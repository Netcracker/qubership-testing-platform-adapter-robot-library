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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.ws.LogRecordDto;
import org.qubership.atp.ram.models.LogRecord;

@RunWith(MockitoJUnitRunner.class)
public class BulkLogRecordSenderTest {

    private BulkLogRecordSender bulkLogRecordSender;
    private final UUID testRunId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID testPlanId = UUID.randomUUID();
    private final UUID executionRequestId = UUID.randomUUID();

    @Mock
    private RequestUtils requestUtils;

    @Before
    public void setUp() {
        bulkLogRecordSender = BulkLogRecordSender.getInstance();
        bulkLogRecordSender.requestUtils = requestUtils;
    }

    @Test
    public void startBatchSendingTask_scheduledTaskIsStarted_offeredLrIsSent() throws IOException,
            InterruptedException {
        bulkLogRecordSender.startBatchSendingTask(executionRequestId, projectId);
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        bulkLogRecordSender.offer(lrDto, executionRequestId);
        Thread.sleep(RamConstants.DEFAULT_LOGRECORD_BATCHING_TIMEOUT * 2);
        verify(requestUtils, atLeast(1))
                .postRequest(any(), argThat(batch ->
                        (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))), eq(null));
    }

    @Test
    public void startBatchSendingTask_scheduledTaskIsStarted_batchIsBoundedByBatchSizeParam() throws IOException,
            InterruptedException {
        bulkLogRecordSender.startBatchSendingTask(executionRequestId, projectId);
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        for (int i = 0; i < RamConstants.DEFAULT_LOGRECORD_BATCH_SIZE * 2; i++) {
            bulkLogRecordSender.offer(lrDto, executionRequestId);
        }
        Thread.sleep(RamConstants.DEFAULT_LOGRECORD_BATCHING_TIMEOUT * 2);
        verify(requestUtils, never())
                .postRequest(any(), argThat(batch ->
                        (batch instanceof List
                                && ((List<?>) batch).size() > RamConstants.DEFAULT_LOGRECORD_BATCH_SIZE
                                && ((List<?>) batch).contains(lrDto))), eq(null));
    }

    @Test
    public void offer_multithreadedLrOffering_allLrsAreSent() throws IOException,
            InterruptedException {
        bulkLogRecordSender.startBatchSendingTask(executionRequestId, projectId);
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(1);
        for (int j = 0; j < threadCount; j++) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < RamConstants.DEFAULT_LOGRECORD_BATCH_SIZE; i++) {
                    bulkLogRecordSender.offer(lrDto, executionRequestId);
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(RamConstants.DEFAULT_LOGRECORD_BATCHING_TIMEOUT * 2);
        AtomicInteger lrCounter = new AtomicInteger(0);
        verify(requestUtils, atLeast(5))
                .postRequest(any(),
                        argThat(batch -> {
                            if (batch instanceof List) {
                                lrCounter.getAndAdd(((List<?>) batch).size());
                                return true;
                            } else
                                return false;
                        }),
                        eq(null));
        assertEquals(threadCount * RamConstants.DEFAULT_LOGRECORD_BATCH_SIZE, lrCounter.get());
    }

    @Test
    public void stopBatchSendingTask_stopTaskIsCalledBeforeAllLrsAreSent_allLrsAreSent() throws IOException,
            InterruptedException {
        bulkLogRecordSender.startBatchSendingTask(executionRequestId, projectId);
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        bulkLogRecordSender.offer(lrDto, executionRequestId);
        bulkLogRecordSender.stopBatchSendingTask(executionRequestId);
        Thread.sleep(RamConstants.DEFAULT_LOGRECORD_BATCHING_TIMEOUT * 2);
        verify(requestUtils, atLeast(1))
                .postRequest(any(), argThat(batch ->
                        (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))), eq(null));
    }

    @Test(expected = IllegalStateException.class)
    public void offer_scheduledIsNotStarted_exceptionIsThrown() throws IOException{
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        bulkLogRecordSender.offer(lrDto, executionRequestId);
    }

    @Test(expected = IllegalStateException.class)
    public void offer_scheduledTaskIsStopped_exceptionIsThrown() throws IOException{
        bulkLogRecordSender.startBatchSendingTask(executionRequestId, projectId);
        bulkLogRecordSender.stopBatchSendingTask(executionRequestId);
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        LogRecordDto lrDto = createLogRecordDto(logRecord);
        bulkLogRecordSender.offer(lrDto, executionRequestId);
    }

    private LogRecordDto createLogRecordDto(LogRecord logRecordRequest) throws JsonProcessingException {
        LogRecordDto logRecordDto = new LogRecordDto();
        logRecordDto.setLogRecordId(logRecordRequest.getUuid());
        logRecordDto.setLogRecordType(logRecordRequest.getClass());
        logRecordDto.setLogRecordJsonString(RamConstants.OBJECT_MAPPER.writeValueAsString(logRecordRequest));
        logRecordDto.setProjectId(projectId);
        return logRecordDto;
    }
}
