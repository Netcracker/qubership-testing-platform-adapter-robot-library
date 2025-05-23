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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.error.FailedToCreateRamEntity;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.ws.LogRecordDto;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.adapter.common.ws.StartRunResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
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

@RunWith(MockitoJUnitRunner.class)
public class AtpImporterRamAdapterTest {

    private final TestRunContext testRunContext = new TestRunContext();
    private static final UUID erId = UUID.randomUUID();
    private static final UUID logRecordId = UUID.randomUUID();
    private static final UUID projectId = UUID.randomUUID();
    private static final UUID testRunId = UUID.randomUUID();
    private static final UUID testPlanId = UUID.randomUUID();
    private static final String ramInteractionFailureMessage = "Failed to send Entity to RAM";
    private static final IOException ramInteractionFailureException = new IOException(ramInteractionFailureMessage);

    private static final String reportDetailsUrl = "http://localhost:8080"
            + RamConstants.API_PATH
            + RamConstants.V1_PATH
            + RamConstants.ER_PATH
            + "/" + erId
            + RamConstants.DETAILS_PATH;

    private static final ObjectMapper objectMapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private AtpImporterRamAdapter atpImporterRamAdapter;
    private AtpImporterRamAdapter atpBulkImporterRamAdapter;
    private BulkLogRecordSender bulkLogRecordSender;

    @Mock
    private RequestUtils requestUtils;

    @Mock
    private BulkLogRecordSender mockBulkLogRecordSender;

    @Before
    public void setUp() {
        testRunContext.setTestRunId(testRunId.toString());
        testRunContext.setProjectId(projectId.toString());
        testRunContext.setExecutionRequestId(erId.toString());
        bulkLogRecordSender = new BulkLogRecordSender(requestUtils);
        atpImporterRamAdapter = new AtpImporterRamAdapter(testRunContext.getTestRunName(),
                bulkLogRecordSender);
        atpImporterRamAdapter.setContext(testRunContext);
        atpImporterRamAdapter.setRequestUtils(requestUtils);
        atpImporterRamAdapter.uploadUrlTemplate = "http://localhost:8080/%s/?fileName=%s&contentType=%s"
                + "&attachmentSource=%s&snapshotExternalSource=%s";
        atpBulkImporterRamAdapter = new AtpImporterRamAdapter(testRunContext.getTestRunName(), mockBulkLogRecordSender);
        atpBulkImporterRamAdapter.setContext(testRunContext);
        atpBulkImporterRamAdapter.setRequestUtils(requestUtils);
    }

    @Test
    public void startExecutionRequest_startBatchSendingTaskIsInvoked() throws FailedToCreateRamEntity, IOException {
        ExecutionRequest createdRequest = new ExecutionRequest();
        createdRequest.setUuid(erId);
        createdRequest.setProjectId(projectId);
        createdRequest.setTestPlanId(testPlanId);
        when(requestUtils.postRequest(any(String.class), any(ExecutionRequest.class), any()))
                .thenReturn(createdRequest);
        atpBulkImporterRamAdapter.startExecutionRequest(
                "ER name",
                projectId,
                testPlanId,
                ExecutionStatuses.IN_PROGRESS);
        verify(mockBulkLogRecordSender, times(1))
                .startBatchSendingTask(eq(erId), eq(projectId));
    }

    @Test
    public void startAtpRun_startBatchSendingTaskIsInvoked() throws IOException {
        StartRunRequest startRunRequest = StartRunRequest.getRequestBuilder()
                .setProjectId(projectId)
                .setTestPlanId(UUID.randomUUID())
                .setProjectName("Project")
                .setTestPlanName("Test Plan")
                .setTestCaseName("TestCase")
                .setTestRunId(UUID.randomUUID().toString())
                .build();
        StartRunResponse response = new StartRunResponse();
        response.setTestRunId(testRunId);
        response.setExecutionRequestId(erId);
        when(requestUtils.postRequest(any(), any(), any()))
                .thenReturn(response);
        atpBulkImporterRamAdapter.startAtpRun(startRunRequest, testRunContext);
        verify(mockBulkLogRecordSender, times(1))
                .startBatchSendingTask(eq(erId), eq(projectId));
    }

    @Test
    public void finishAllTestRuns_cancelBatchSendingIsInvoked() throws IOException, FailedToCreateRamEntity {
        List<UUID> uuids = asList(UUID.randomUUID(), UUID.randomUUID());
        atpBulkImporterRamAdapter.finishAllTestRuns(uuids, true);
        verify(mockBulkLogRecordSender, times(1))
                .stopBatchSendingTask(eq(erId));
    }

    @Test
    public void sendLogRecord_offerIsInvoked() {
        LogRecord logRecord = new LogRecord();
        logRecord.setUuid(UUID.randomUUID());
        atpBulkImporterRamAdapter.sendLogRecord(logRecord);
        verify(mockBulkLogRecordSender, times(1))
                .offer(argThat(logRecordDto -> logRecordDto.getLogRecordId().equals(logRecord.getUuid())),
                        eq(erId));
    }

    @Test
    public void testUpdateTestRun_ProperRamEndpointIsInvoked() throws FailedToCreateRamEntity, IOException {
        String urlToUpdateTr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH
                + "?projectId=" + projectId;
        TestRun testRunPatch = new TestRun();
        atpImporterRamAdapter.updateTestRun(testRunPatch);
        verify(requestUtils, times(1))
                .patchRequest(eq(urlToUpdateTr), any(TestRun.class), eq(null));
    }

    @Test
    public void testUpdateTestRun_TestRunPatchIsSentToRamAsIs() throws FailedToCreateRamEntity,
            IOException {
        TestRun testRunPatch = new TestRun();
        atpImporterRamAdapter.updateTestRun(testRunPatch);
        verify(requestUtils, times(1))
                .patchRequest(any(String.class), eq(testRunPatch), eq(null));
    }

    @Test
    public void testStartAtpRun_ProperRamEndpointIsInvoked() throws IOException {
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH;
        StartRunRequest startRunRequest = StartRunRequest.getRequestBuilder()
                .setProjectId(projectId)
                .setTestPlanId(UUID.randomUUID())
                .setProjectName("Project")
                .setTestPlanName("Test Plan")
                .setTestCaseName("TestCase")
                .setTestRunId(UUID.randomUUID().toString())
                .build();
        StartRunResponse response = new StartRunResponse();
        response.setTestRunId(UUID.randomUUID());
        response.setExecutionRequestId(UUID.randomUUID());
        when(requestUtils.postRequest(any(), any(), any()))
                .thenReturn(response);
        atpImporterRamAdapter.startAtpRun(startRunRequest, atpImporterRamAdapter.context);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToBeInvoked), eq(startRunRequest), eq(StartRunResponse.class));
    }

    @Test(expected = FailedToCreateRamEntity.class)
    public void testStartAtpRun_RamInvocationFails_FailedToCreateRamEntityIsThrown()
            throws IOException {
        StartRunRequest startRunRequest = StartRunRequest.getRequestBuilder()
                .setProjectName("Project")
                .setTestPlanName("Test Plan")
                .setTestCaseName("TestCase")
                .setTestRunId(testRunId.toString())
                .build();
        StartRunResponse response = new StartRunResponse();
        response.setTestRunId(UUID.randomUUID());
        response.setExecutionRequestId(UUID.randomUUID());
        when(requestUtils.postRequest(any(), any(), any()))
                .thenThrow(new IOException("Failed to start TR in RAM"));
        atpImporterRamAdapter.startAtpRun(startRunRequest, atpImporterRamAdapter.context);
    }

    @Test
    public void testUpdateTestRunStatus_ProperRamEndpointIsInvoked() throws IOException {
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH
                + "?projectId=" + projectId;
        UUID trId = UUID.randomUUID();
        ExecutionStatuses newTrStatus = ExecutionStatuses.FINISHED;
        atpImporterRamAdapter.updateTestRunStatus(newTrStatus, trId.toString());
        verify(requestUtils, times(1))
                .patchRequest(eq(urlToBeInvoked),
                        argThat(testRunPatch -> {
                            if (testRunPatch instanceof TestRun) {
                                TestRun testRun = (TestRun) testRunPatch;
                                return testRun.getExecutionStatus().equals(newTrStatus)
                                        && testRun.getUuid().equals(trId)
                                        && testRun.getTestingStatus() == null;
                            } else {
                                return false;
                            }
                        }),
                        eq(null));
    }

    @Test
    public void testFinishAllTestRuns_properRamEndpointIsInvoked() throws FailedToCreateRamEntity, IOException {
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.TEST_RUN_PATH
                + RamConstants.BULK_PATH
                + RamConstants.FINISH_PATH
                + "?isDelayed=" + true
                + "&projectId=" + projectId;
        UUID trId1 = UUID.randomUUID();
        UUID trId2 = UUID.randomUUID();
        atpImporterRamAdapter.finishAllTestRuns(asList(trId1, trId2), true);
        verify(requestUtils, times(1))
                .postRequest(
                        eq(urlToBeInvoked),
                        argThat(trIds ->
                                trIds instanceof List
                                        && ((List<?>) trIds).size() == 2
                                        && ((List<?>) trIds).contains(trId1)
                                        && ((List<?>) trIds).contains(trId2)),
                        eq(null));
    }

    @Test
    public void testUpdateExecutionRequestStatus_properRamEndpointIsInvoked() throws IOException {
        ExecutionStatuses newErStatus = ExecutionStatuses.FINISHED;
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH
                + "/" + erId
                + RamConstants.UPDATE_EXECUTION_STATUS_PATH
                + "/" + newErStatus.toString()
                + "?projectId=" + projectId;
        atpImporterRamAdapter.updateExecutionRequestStatus(newErStatus, erId.toString());
        verify(requestUtils, times(1))
                .putRequest(eq(urlToBeInvoked), eq(null), eq(null));
    }

    @Test
    public void testUpdateVariablesContext_properRamEndpointIsInvoked() throws IOException {
        List<ContextVariable> contextVariables = new ArrayList<>();
        contextVariables.add(new ContextVariable());
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + "/" + logRecordId
                + RamConstants.CONTEXT_VARIABLES_PATH
                + "?projectId=" + projectId;
        atpImporterRamAdapter.updateContextVariables(logRecordId.toString(), contextVariables);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToBeInvoked), eq(contextVariables), eq(null));
    }

    @Test
    public void testUpdateStepVariablesContext_properRamEndpointIsInvoked() throws IOException {
        List<ContextVariable> contextVariables = new ArrayList<>();
        contextVariables.add(new ContextVariable());
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + "/" + logRecordId
                + RamConstants.STEP_CONTEXT_VARIABLES_PATH
                + "?projectId=" + projectId;
        atpImporterRamAdapter.updateStepContextVariables(logRecordId.toString(), contextVariables);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToBeInvoked), eq(contextVariables), eq(null));
    }

    @Test
    public void testReportEnvironmentsInfo_properRamEndpointIsInvoked() throws IOException {
        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        UUID erId = UUID.randomUUID();
        environmentsInfo.setExecutionRequestId(erId);
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH
                + RamConstants.ENVIRONMENTS_INFO_PATH
                + "?projectId=" + projectId;
        atpImporterRamAdapter.reportEnvironmentsInfo(environmentsInfo);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToBeInvoked), eq(environmentsInfo), eq(null));
    }

    @Test
    public void testReportToolsInfo_properRamEndpointIsInvoked() throws IOException {
        ToolsInfo toolsInfo = new ToolsInfo();
        String urlToBeInvoked = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH
                + RamConstants.TOOLS_INFO_PATH
                + "?projectId=" + projectId;
        atpImporterRamAdapter.reportToolsInfo(toolsInfo);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToBeInvoked), eq(toolsInfo), eq(null));
    }

    @Test
    public void testUpdateExecutionRequestStatus_RamInvocationFails_DetailsAreReportedToRam() throws IOException {
        ExecutionStatuses newErStatus = ExecutionStatuses.FINISHED;
        String urlToUpdateErStatus = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.ER_PATH
                + "/" + erId
                + RamConstants.UPDATE_EXECUTION_STATUS_PATH
                + "/" + newErStatus.toString()
                + "?projectId=" + projectId;
        atpImporterRamAdapter.getContext().setExecutionRequestId(erId.toString());
        when(requestUtils.putRequest(eq(urlToUpdateErStatus), eq(null), eq(null)))
                .thenThrow(ramInteractionFailureException);
        atpImporterRamAdapter.updateExecutionRequestStatus(newErStatus, erId.toString());
        InOrder inOrder = inOrder(requestUtils);
        inOrder.verify(requestUtils, times(1))
                .putRequest(eq(urlToUpdateErStatus), eq(null), eq(null));
        inOrder.verify(requestUtils, times(1))
                .postRequest(eq(reportDetailsUrl),
                        argThat(this::isProperDetailsMessage_erIdStatusProjectIdAndMessageAreSet),
                        eq(ExecutionRequestDetails.class));
    }

    @Test
    public void testSendLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        LogRecord logRecord = new LogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
    }

    @Test
    public void testSendUiLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        LogRecord logRecord = new UiLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
    }

    @Test
    public void testSendSshLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        SshLogRecord logRecord = new SshLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendSshLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testSendSqlLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        SqlLogRecord logRecord = new SqlLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendSqlLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testSendRestLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        RestLogRecord logRecord = new RestLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendRestLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testSendItfLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        ItfLogRecord logRecord = new ItfLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendItfLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testSendBvLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        BvLogRecord logRecord = new BvLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendBvLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testSendMiaLogRecord_ProperRamEndpointIsInvoked() throws IOException {
        MiaLogRecord logRecord = new MiaLogRecord();
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.sendMiaLogRecord(logRecord);
        bulkLogRecordSender.stopBatchSendingTask(erId);
        LogRecordDto lrDto = atpImporterRamAdapter.createLogRecordDto(logRecord);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch).contains(lrDto))),
                        eq(null));
        Assert.assertEquals(logRecord.getClass(), lrDto.getLogRecordType());
    }

    @Test
    public void testUpdateTestingStatus_ProperLogRecordIsSent() throws IOException {
        UUID logRecordId = UUID.randomUUID();
        TestingStatuses newTestingStatus = TestingStatuses.PASSED;
        String urlToSendLr = "http://localhost:8080"
                + RamConstants.API_PATH
                + RamConstants.V1_PATH
                + RamConstants.LOG_RECORD_PATH
                + RamConstants.BULK_PATH
                + "?projectId=" + projectId;
        bulkLogRecordSender.startBatchSendingTask(erId, projectId);
        atpImporterRamAdapter.updateTestingStatus(logRecordId.toString(), newTestingStatus.toString());
        bulkLogRecordSender.stopBatchSendingTask(erId);
        verify(requestUtils, times(1))
                .postRequest(eq(urlToSendLr),
                        argThat(batch -> (batch instanceof List
                                && ((List<?>) batch).size() == 1
                                && ((List<?>) batch)
                                .stream()
                                .map(lrDto -> (LogRecordDto) lrDto)
                                .map(this::extractLrFromDto)
                                .anyMatch(lr -> lr.getUuid().equals(logRecordId)
                                        && lr.getTestingStatus().equals(newTestingStatus)
                                        && lr.getLastUpdated() != null))),
                        eq(null));
    }

    private LogRecord extractLrFromDto(LogRecordDto logRecordDto) {
        try {
            return objectMapper.readValue(logRecordDto.getLogRecordJsonString(),
                    LogRecord.class);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private boolean isProperDetailsMessage_erIdStatusProjectIdAndMessageAreSet(Object erDetails) {
        return erDetails instanceof ExecutionRequestDetails
                && ((ExecutionRequestDetails) erDetails).getExecutionRequestId().equals(erId)
                && ((ExecutionRequestDetails) erDetails).getStatus().equals(TestingStatuses.FAILED)
                && ((ExecutionRequestDetails) erDetails).getMessage()
                .contains(ramInteractionFailureMessage)
                && ((ExecutionRequestDetails) erDetails).getProjectId().equals(projectId);
    }

    @Test
    public void getUploadFileUrl_GenerateUrlWithSpaceSpecialCharacterInFileName_SpaceCharacterInFileNameParamEncoded() {

        // Arrange
        Map<String, Object> attribute = Mockito.mock(Map.class);
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_NAME_KEY))
                .thenReturn("screenshotNameKey withSpaceCharacter");
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_TYPE_KEY)).thenReturn("screenshotTypeKey");
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY)).thenReturn("screenshotSourceKey");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getUuid()).thenReturn("");

        // Act
        String result = atpImporterRamAdapter.getUploadFileUrl(attribute, message);

        // Assert
        assertFalse("Result String with URL contains space ' ' character but should be encoded.",
                result.contains(" "));
        assertTrue("Result String with URL doesnt contain correct fileName param",
                result.contains("&contentType=screenshotNameKey+withSpaceCharacter&"));
    }
}
