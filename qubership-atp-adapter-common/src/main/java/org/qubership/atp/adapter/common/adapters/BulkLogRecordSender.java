/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.qubership.atp.adapter.common.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.NotThreadSafe;

import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.ws.LogRecordDto;
import lombok.extern.slf4j.Slf4j;

/**
 * Class manages LR batches sending to RAM importer.
 * For each ER {@link BulkLogRecordSender#startBatchSendingTask(UUID, UUID)}
 * should be invoked to start scheduled task that forms and sends batches.
 * After ER is finished {@link BulkLogRecordSender#stopBatchSendingTask(UUID)} should be
 * invoked to cancel scheduled task. Task automatically cancels after
 * {@link RamConstants#LOGRECORD_BATCHING_SENDER_TASK_TIMEOUT}
 */
@NotThreadSafe
@Slf4j
public class BulkLogRecordSender {

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<UUID, ScheduledFuture<Void>> scheduledSendingTasks =
            new ConcurrentHashMap<>();// one per ER
    private final int batchSize;
    private final long timeout; //in millis
    private final int senderTaskTimeout; //in hours
    private final String atpRamImporterUrl;
    protected RequestUtils requestUtils = new RequestUtils();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<LogRecordDto>> lrsToErQueue =
            new ConcurrentHashMap<>();

    private BulkLogRecordSender() {
        atpRamImporterUrl = Config.getConfig().getProperty(RamConstants.ATP_RAM_IMPORTER_URL_KEY,
                "http://localhost:8080");
        batchSize = Config.getConfig().getIntProperty(RamConstants.ATP_RAM_IMPORTER_LOGRECORD_BATCH_SIZE_KEY,
                RamConstants.DEFAULT_LOGRECORD_BATCH_SIZE);
        timeout = Config.getConfig().getLongProperty(RamConstants.ATP_RAM_IMPORTER_LOGRECORD_BATCH_TIMEOUT_KEY,
                RamConstants.DEFAULT_LOGRECORD_BATCHING_TIMEOUT);
        senderTaskTimeout = RamConstants.LOGRECORD_BATCHING_SENDER_TASK_TIMEOUT;
        log.debug("BulkLogRecordSender is created with batch size of {} and timeout {}", batchSize, timeout);
    }

    /*
     * For test use only
     * */
    BulkLogRecordSender(RequestUtils requestUtils) {
        this();
        this.requestUtils = requestUtils;
    }

    public static class BulkLogRecordSenderHolder {

        public static final BulkLogRecordSender HOLDER_INSTANCE = new BulkLogRecordSender();
    }

    public static BulkLogRecordSender getInstance() {
        return BulkLogRecordSenderHolder.HOLDER_INSTANCE;
    }

    /**
     * Schedules batch sending task for specified ER.
     */
    public void startBatchSendingTask(UUID erId, UUID projectId) {
        log.debug("Starting batch sending task for project {}", erId);
        scheduledSendingTasks.computeIfAbsent(erId, uuid -> {
            lrsToErQueue.put(uuid, new ConcurrentLinkedQueue<>());
            ScheduledFuture scheduledFuture = schedule(uuid, projectId);
            scheduleSenderCancellation(scheduledFuture, senderTaskTimeout);
            return scheduledFuture;
        });
    }

    /**
     * Schedules batch sending task for specified ER, waiting for the last task
     * invocation to ensure all offered LR are sent.
     */
    public void stopBatchSendingTask(UUID erId) {
        log.debug("Canceling batch sending task for ER {}", erId);
        scheduledSendingTasks.computeIfPresent(//blocks erId mapping so we can not update it anywhere else
                erId,
                (uuid, scheduledFuture) -> {
                    while (lrsToErQueue.get(erId).size() > 0) {
                        log.debug("Waiting for LR queue to be empty to cancel sending task for ER {}", erId);
                        try {
                            Thread.sleep(timeout); //ensure the last LRs are sent
                        } catch (InterruptedException e) {
                            log.error("Thread is interrupted while waiting for LR batch sender cancellation");
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    scheduledFuture.cancel(false);
                    log.debug("Sending batch LR task for ER {} is cancelled", erId);
                    lrsToErQueue.remove(uuid);
                    return null;//this removes erId mapping
                });
    }

    /**
     * All invocations should be done between startBatchSending() and cancelBatchSending() for each ER id.
     */
    public void offer(LogRecordDto logRecordDto, UUID erId) {
        log.debug("LR {} in project {} and ER {} is submitted to be sent", logRecordDto.getLogRecordId(),
                logRecordDto.getProjectId(), erId);
        ConcurrentLinkedQueue<LogRecordDto> concurrentLinkedQueue = lrsToErQueue.get(erId);
        if (concurrentLinkedQueue == null) {
            throw new IllegalStateException("Can not offer LR " + logRecordDto.getLogRecordId()
                    + " for er " + erId + " before batch sending is started");
        }
        concurrentLinkedQueue.offer(logRecordDto);
    }

    private ScheduledFuture<?> schedule(UUID erId, UUID projectId) {
        return executor.scheduleAtFixedRate(() -> {
            log.debug("Start LR batches sending for ER {} in project {}", erId, projectId);
            final ConcurrentLinkedQueue<LogRecordDto> lrsQueue = lrsToErQueue.get(erId);
            final List<LogRecordDto> batch = new ArrayList<>(batchSize);
            LogRecordDto lrDto;
            int batchCounter = 0;
            while ((lrDto = lrsQueue.poll()) != null) {
                batch.add(lrDto);
                if (batch.size() >= batchSize) {
                    sendLrBatch(batch, projectId);
                    log.debug("{} full batch is sent", ++batchCounter);
                    batch.clear();
                }
            }
            if (batch.size() != 0) {
                sendLrBatch(batch, projectId);
                log.debug("{} batch is sent with {} LRs", ++batchCounter, batch.size());
                batch.clear();
            }
        }, 0, timeout, TimeUnit.MILLISECONDS);
    }

    private void sendLrBatch(List<LogRecordDto> batch, UUID projectId) {
        try {
            requestUtils.postRequest(atpRamImporterUrl
                            + RamConstants.API_PATH
                            + RamConstants.V1_PATH
                            + RamConstants.LOG_RECORD_PATH
                            + RamConstants.BULK_PATH
                            + "?projectId=" + projectId,
                    new ArrayList<>(batch), null);
        } catch (IOException ioException) {
            log.error("Failed to send LRs {} in RAM", batch, ioException);
        }
    }

    private void scheduleSenderCancellation(ScheduledFuture scheduledFuture, int delayHours) {
        executor.schedule(() -> scheduledFuture.cancel(false), delayHours, TimeUnit.HOURS);
    }
}
