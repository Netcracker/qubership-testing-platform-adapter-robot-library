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
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.error.FailedToCreateRamEntity;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.EngineCategory;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.ws.LogRecordDto;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.MiaLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter reports to ram-results-importer service.
 * It uses batching for logrecords that can be tuned using logrecord.batch.size and logrecord.batch.timeout parameters.
 * Default values: logrecord.batch.size = 50, logrecord.batch.timeout = 10000 (millis).
 * Logrecord batching is managed by {@link BulkLogRecordSender}. All logrecords should be submitted to be sent after
 * {@link AtpImporterRamAdapter#startExecutionRequest(String, UUID, UUID, ExecutionStatuses)}
 * or {@link AtpImporterRamAdapter#startAtpRun(StartRunRequest, TestRunContext)}
 * invocation and before
 * {@link AtpImporterRamAdapter#finishAllTestRuns(List, boolean)}
 */
@Slf4j
public class AtpImporterRamAdapter extends AbstractAdapter {

    private final BulkLogRecordSender bulkLogRecordSender;

    private String atpRamImporterUrl;
    protected String uploadUrlTemplate;

    /**
     * Init adapter.
     *
     * @param testRunName - name of test run to be reported
     */
    public AtpImporterRamAdapter(String testRunName) {
        this(testRunName, BulkLogRecordSender
                .BulkLogRecordSenderHolder.HOLDER_INSTANCE);
    }

    public AtpImporterRamAdapter(TestRunContext context) {
        this(context.getTestRunName(), BulkLogRecordSender
                .BulkLogRecordSenderHolder.HOLDER_INSTANCE);
        this.context = context;
    }

    /*For test use only*/
    AtpImporterRamAdapter(String testRunName, BulkLogRecordSender bulkLogRecordSender) {
        super(testRunName);
        atpRamImporterUrl = Config.getConfig().getProperty(RamConstants.ATP_RAM_IMPORTER_URL_KEY, "http://localhost"
                + ":8080");
        uploadUrlTemplate = atpRamImporterUrl
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ATTACHMENT_PATH
                + "/%s?projectId=%s&fileName=%s&contentType=%s&attachmentSource=%s";
        this.bulkLogRecordSender = bulkLogRecordSender;
    }

    @Override
    public TestRunContext startExecutionRequest(
            String executionRequestName,
            UUID projectId,
            UUID testPlanId,
            ExecutionStatuses executionStatuses) throws FailedToCreateRamEntity {
        TestRunContext context = super.startExecutionRequest(
                executionRequestName,
                projectId,
                testPlanId,
                executionStatuses);
        bulkLogRecordSender.startBatchSendingTask(
                UUID.fromString(context.getExecutionRequestId()),
                UUID.fromString(context.getProjectId()));
        return context;
    }

    @Override
    protected String getCreateErUrl() {
        return atpRamImporterUrl
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH;
    }

    /**
     * {@inheritDoc}
     *
     * @throws FailedToCreateRamEntity in case of ER details reporting to RAM fails.
     *                                 This is unrecoverable for reporting error. Client should handle it.
     */
    @Override
    public void updateTestRun(TestRun testRunPatch) throws FailedToCreateRamEntity {
        try {
            log.debug("Patching TR with id {}. ", testRunPatch.getUuid());
            requestUtils.patchRequest(atpRamImporterUrl
                            + RamConstants.API_PATH
                            + RamConstants.V1_PATH
                            + RamConstants.TEST_RUN_PATH
                            + "?projectId=" + context.getProjectId(),
                    testRunPatch, null);
        } catch (IOException ioException) {
            log.error("Failed to update TR {} in RAM", context.getTestRunId(), ioException);
            reportDetails("Failed to update TR " + testRunPatch.getUuid() + " in RAM."
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
    }

    @Override
    protected String getEmailSendUrl(String executionRequestId) {
        return atpRamImporterUrl + RamConstants.API_PATH + RamConstants.V1_PATH + "/email/" + executionRequestId;
    }

    @Override
    protected String getCreateReportingUrl(String executionRequestId) {
        return atpRamImporterUrl + RamConstants.API_PATH + RamConstants.V1_PATH + RamConstants.ER_PATH + "/" + executionRequestId + "/emailReporting";
    }

    /**
     * Sends request to start TR.
     * In case TR creation failure in RAM, throws FailedToCreateRamEntity exception
     * (unrecoverable; client should handle it).
     */
    @SneakyThrows
    @Override
    public TestRunContext startAtpRun(StartRunRequest request, TestRunContext initialContext) {
        try {
            super.startAtpRun(request, initialContext);
            bulkLogRecordSender.startBatchSendingTask(
                    UUID.fromString(context.getExecutionRequestId()),
                    UUID.fromString(context.getProjectId()));
            return context;
        } catch (Exception ioException) { //Here IOException can be thrown which is hidden.
            log.error("Failed to create TR for ER {}", request.getAtpExecutionRequestId(), ioException);
            throw new FailedToCreateRamEntity(
                    String.format("Failed to create TR for ER %s", request.getAtpExecutionRequestId()),
                    ioException);
        }
    }

    @Override
    protected String getStartAtpRunUrl() {
        return atpRamImporterUrl
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH;
    }

    @SneakyThrows
    @Override
    public TestRunContext updateTestRunStatus(ExecutionStatuses statuses, String trId) {
        TestRun testRunPatch = new TestRun();
        testRunPatch.setTestingStatus(null);//in order to save stored value
        testRunPatch.setUuid(UUID.fromString(trId));
        testRunPatch.setExecutionStatus(statuses);
        this.updateTestRun(testRunPatch);
        return this.context;
    }

    /**
     * Stop all provided test runs in RAM. Also cancels LR batch sending task.
     * Further LRs submitting to adapter will result in {@link IllegalStateException}
     *
     * @param isDelayed if true finish date and duration is not updated in RAM.
     * @throws FailedToCreateRamEntity in case of ER details reporting to RAM fails.
     *                                 This is unrecoverable for reporting error. Client should handle it.
     */
    @Override
    public void finishAllTestRuns(List<UUID> testRunUuids, boolean isDelayed) throws FailedToCreateRamEntity {
        try {
            log.debug("Delayed finishing TRs {}", testRunUuids);
            requestUtils.postRequest(atpRamImporterUrl
                            + RamConstants.API_PATH
                            + RamConstants.V1_PATH
                            + RamConstants.TEST_RUN_PATH
                            + RamConstants.BULK_PATH
                            + RamConstants.FINISH_PATH
                            + "?isDelayed=" + isDelayed
                            + "&projectId=" + context.getProjectId(),
                    testRunUuids, null);
        } catch (IOException ioException) {
            log.error("Failed to finish TRs {} in RAM", testRunUuids, ioException);
            reportDetails("Failed to stop TRs in RAM due to "
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        } finally {
            bulkLogRecordSender.stopBatchSendingTask(UUID.fromString(context.getExecutionRequestId()));
        }
    }

    /**
     * Updates ER status.
     */
    @SneakyThrows
    @Override
    public TestRunContext updateExecutionRequestStatus(ExecutionStatuses status, String erId) {
        log.debug("updateExecutionRequestStatus [statuses={}, erId={}]", status, erId);
        try {
            requestUtils.putRequest(atpRamImporterUrl
                    + RamConstants.API_PATH
                    + RamConstants.V1_PATH
                    + RamConstants.ER_PATH
                    + "/" + erId
                    + RamConstants.UPDATE_EXECUTION_STATUS_PATH
                    + "/" + status.toString()
                    + "?projectId=" + context.getProjectId(), null, null);
        } catch (IOException ioException) {
            log.error("Failed to update ER {} status", erId, ioException);
            reportDetails("Failed to update ER " + erId + " status"
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
        return this.context;
    }

    @Override
    protected String getReportDetailsUrl(String executionRequestId) {
        return atpRamImporterUrl
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH
                + "/" + executionRequestId
                + RamConstants.DETAILS_PATH;
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles, EngineCategory category) {
        return null;
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles,
                                 org.qubership.atp.ram.enums.EngineCategory category) {
        return null;
    }

    @Override
    public TestRunContext updateMessageAndTestingStatus(String logRecordId, String message, String testingStatus) {
        return null;
    }

    @SneakyThrows
    @Override
    public void updateContextVariables(String logRecordId, List<ContextVariable> contextVariables) {
        encryptContextVariables(contextVariables);
        try {
            requestUtils.postRequest(atpRamImporterUrl
                    + RamConstants.API_PATH
                    + RamConstants.V1_PATH
                    + RamConstants.LOG_RECORD_PATH
                    + "/" + logRecordId
                    + RamConstants.CONTEXT_VARIABLES_PATH
                    + "?projectId=" + context.getProjectId(), contextVariables, null);
        } catch (IOException ioException) {
            log.error("Failed to update context variables for LR {}", logRecordId, ioException);
            reportDetails("Failed to context variables for LR " + logRecordId + ". "
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
    }

    @SneakyThrows
    @Override
    public void updateStepContextVariables(String logRecordId, List<ContextVariable> stepContextVariables) {
        encryptContextVariables(stepContextVariables);
        try {
            requestUtils.postRequest(atpRamImporterUrl
                    + RamConstants.API_PATH
                    + RamConstants.V1_PATH
                    + RamConstants.LOG_RECORD_PATH
                    + "/" + logRecordId
                    + RamConstants.STEP_CONTEXT_VARIABLES_PATH
                    + "?projectId=" + context.getProjectId(), stepContextVariables, null);
        } catch (IOException ioException) {
            log.error("Failed to update step context variables for LR {}", logRecordId, ioException);
            reportDetails("Failed to step context variables for LR " + logRecordId + ". "
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
    }

    @Override
    public TestRunContext updateMessageWithIsGroup(String logRecordId, boolean isGroup) {
        return null;
    }

    @Override
    public TestRunContext sendLogRecord(LogRecord logRecordRequest) {
        try {
            final LogRecordDto logRecordDto = createLogRecordDto(logRecordRequest);
            bulkLogRecordSender.offer(logRecordDto, UUID.fromString(context.getExecutionRequestId()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize LR {}", logRecordRequest);
            try {
                reportDetails("Failed to send LR " + logRecordRequest.getUuid() + " in RAM."
                        + ExceptionUtils.getStackTrace(e), TestingStatuses.FAILED);
            } catch (FailedToCreateRamEntity failedToCreateRamEntity) {
                log.error("Failed to report send LR {} error details to RAM", logRecordRequest.getUuid(),
                        failedToCreateRamEntity);
            }
        }
        return this.context;
    }

    protected LogRecordDto createLogRecordDto(LogRecord logRecordRequest) throws JsonProcessingException {
        LogRecordDto logRecordDto = new LogRecordDto();
        logRecordDto.setLogRecordId(logRecordRequest.getUuid());
        logRecordDto.setLogRecordType(logRecordRequest.getClass());
        logRecordDto.setLogRecordJsonString(RamConstants.OBJECT_MAPPER.writeValueAsString(logRecordRequest));
        logRecordDto.setProjectId(UUID.fromString(context.getProjectId()));
        return logRecordDto;
    }

    @Override
    protected void updateTestRunTestingStatus(TestingStatuses statuses, String trId) {
        String url = atpRamImporterUrl
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH
                + "/" + trId
                + RamConstants.UPDATE_TESTING_STATUS_PATH
                + "/" + statuses
                + "?projectId=" + context.getProjectId();
        RequestUtils.putRequest(url);
    }

    @Override
    public TestRunContext updateSsmMetricReports(String logRecordId, String problemContextMetricReportId, String microservicesReportId) {
        return null;
    }



    @SneakyThrows
    protected String getUploadFileUrl(Map<String, Object> attribute, Message message) {
        String fileName = (String) attribute.get(RamConstants.SCREENSHOT_NAME_KEY);
        String contentType = (String) attribute.get(RamConstants.SCREENSHOT_TYPE_KEY);
        String attachmentSource = (String) attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY);
        return String.format(
                uploadUrlTemplate,
                message.getUuid(),
                context.getProjectId(),
                URLEncoder.encode(fileName, RamConstants.UTF8_CHARSET),
                contentType,
                URLEncoder.encode(StringUtils.defaultIfEmpty(attachmentSource, ""), RamConstants.UTF8_CHARSET));
    }

    @Override
    public TestRunContext sendUiLogRecord(UiLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendRestLogRecord(RestLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendSqlLogRecord(SqlLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendSshLogRecord(SshLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendMiaLogRecord(MiaLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendItfLogRecord(ItfLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public TestRunContext sendBvLogRecord(BvLogRecord logRecordRequest) {
        return sendLogRecord(logRecordRequest);
    }

    @Override
    public void sendBrowserLogs(List<BrowserConsoleLogsTable> logRecordRequest, String uuid) {
        try {
            log.debug("Sending browser logs with LR id {}. ", uuid);
            requestUtils.postRequest(atpRamImporterUrl
                            + RamConstants.API_PATH
                            + RamConstants.V1_PATH
                            + RamConstants.LOG_RECORD_PATH
                            + "/" + uuid
                            + RamConstants.BROWSER_LOGS_PATH
                            + "?projectId=" + context.getProjectId(),
                    logRecordRequest, null);
        } catch (IOException ioException) {
            log.error("Failed to send browser logs with LR id {} in RAM", uuid, ioException);
        }
    }

    @Override
    public TestRunContext updateTestingStatus(String logRecordId, String status) {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.fromString(logRecordId));
        logRecord.setLastUpdated(new Date());
        logRecord.setTestingStatus(TestingStatuses.findByValue(status));
        return this.sendLogRecord(logRecord);
    }

    @SneakyThrows
    @Override
    public TestRunContext sendEnvironmentsInfo(EnvironmentsInfo environmentsInfo) {
        try {
            requestUtils.postRequest(atpRamImporterUrl
                    + RamConstants.API_PATH
                    + RamConstants.V1_PATH
                    + RamConstants.ER_PATH
                    + RamConstants.ENVIRONMENTS_INFO_PATH
                    + "?projectId=" + context.getProjectId(), environmentsInfo, null);
        } catch (IOException error) {
            log.error("Failed to report Environments Info for ER {}", environmentsInfo.getExecutionRequestId(), error);
            reportDetails("Failed to report Environments Info for ER "
                            + environmentsInfo.getExecutionRequestId() + ". " + ExceptionUtils.getStackTrace(error),
                    TestingStatuses.FAILED);
        }
        return context;
    }

    @SneakyThrows
    @Override
    public TestRunContext sendToolsInfo(ToolsInfo toolsInfo) {
        try {
            requestUtils.postRequest(atpRamImporterUrl
                    + RamConstants.API_PATH
                    + RamConstants.V1_PATH
                    + RamConstants.ER_PATH
                    + RamConstants.TOOLS_INFO_PATH
                    + "?projectId=" + context.getProjectId(), toolsInfo, null);
        } catch (IOException error) {
            log.error("Failed to report Tools Info for ER {}", context.getExecutionRequestId(), error);
            reportDetails("Failed to report Tools Info for ER " + context.getExecutionRequestId() + ". "
                    + ExceptionUtils.getStackTrace(error), TestingStatuses.FAILED);
        }
        return context;
    }
}
