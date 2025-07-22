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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.getCurrentTimestamp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.error.FailedToCreateRamEntity;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.LogRecordsStack;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.entities.UploadScreenshotResponse;
import org.qubership.atp.adapter.common.mocks.ModelMocks;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.crypt.Constants;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.OpenMode;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.CustomLink;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.CompoundLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.MiaLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.TechnicalLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AbstractAdapterTest {

    public static final String MASKED_MESSAGE =
            "Login as \"sysadm1\" with password \"" + Constants.ENCRYPTED_MASK + "\"";
    private static final String CREATE_ER_PATH = "/api/executionrequests/create";
    private static final String PATCH_TR_PATH = "/api/executor/testruns/patch";
    private static final String DELAYED_FINISH_TRS_PATH = "/api/executor/testruns/bulk/finish/delayed";
    private static final String FINISH_TR_PATH_TEMPLATE = "/api/testruns/%s/finish";
    private static final String API_ER_DETAILS_TEMPLATE = "/api/executionrequests/%s/details";
    private static final String DETAILS_MESSAGE = "Details message";

    private AbstractAdapter abstractAdapter;
    private static final List<ContextVariable> contextVariables = Collections.singletonList(
            new ContextVariable("test", "beforeValue", "afterValue"));
    private RequestUtils requestUtils;

    @Before
    public void setUp() {
        abstractAdapter = Mockito.mock(AbstractAdapter.class, Mockito.CALLS_REAL_METHODS);
        TestRunContext context = new TestRunContext();
        context.setTestRunId(UUID.randomUUID().toString());
        abstractAdapter.setContext(context);
        requestUtils = Mockito.mock(RequestUtils.class);
        abstractAdapter.setRequestUtils(requestUtils);
        abstractAdapter.uploadUrlTemplate = "http://localhost:8080/%s/?fileName=%s&contentType=%s&snapshotSource=%s"
                + "&snapshotExternalSource=%s";
    }

    @Test
    public void startExecutionRequest_AllParamsAreFilledProperlyInRequestToRam() throws FailedToCreateRamEntity, IOException {
        ExecutionRequest createdRequest = createExecutionRequest();
        when(requestUtils.postRequest(any(String.class), any(ExecutionRequest.class), any()))
                .thenReturn(createdRequest);
        abstractAdapter.startExecutionRequest(
                createdRequest.getName(),
                createdRequest.getProjectId(),
                createdRequest.getTestPlanId(),
                createdRequest.getExecutionStatus());
        verify(requestUtils, times(1))
                .postRequest(
                        argThat(url -> url.contains(CREATE_ER_PATH)),
                        argThat(er -> {
                            if (er instanceof ExecutionRequest) {
                                ExecutionRequest erToCreate = (ExecutionRequest) er;
                                return erToCreate.getName().equals(createdRequest.getName()) &&
                                        erToCreate.getTestPlanId().equals(createdRequest.getTestPlanId()) &&
                                        erToCreate.getProjectId().equals(createdRequest.getProjectId()) &&
                                        erToCreate.getExecutionStatus().equals(createdRequest.getExecutionStatus()) &&
                                        erToCreate.getStartDate() != null;
                            }
                            return false;
                        }),
                        eq(ExecutionRequest.class));
    }

    @Test
    public void startExecutionRequest_AllRequiredParamsFromResponseAreFilledInContext() throws FailedToCreateRamEntity,
            IOException {
        ExecutionRequest createdRequest = createExecutionRequest();
        when(requestUtils.postRequest(any(String.class), any(ExecutionRequest.class), any()))
                .thenReturn(createdRequest);
        TestRunContext resultContext = abstractAdapter.startExecutionRequest(
                createdRequest.getName(),
                createdRequest.getProjectId(),
                createdRequest.getTestPlanId(),
                createdRequest.getExecutionStatus());
        assertEquals("ER id should be filled in context",
                createdRequest.getUuid().toString(), resultContext.getExecutionRequestId());
        assertEquals("Project id should be filled in context",
                createdRequest.getProjectId().toString(), resultContext.getProjectId());
        assertEquals("Test plan id should be filled in context",
                createdRequest.getTestPlanId().toString(), resultContext.getTestPlanId());
    }

    @Test(expected = FailedToCreateRamEntity.class)
    public void startExecutionRequest_FailedToCreateEntityIsThrownInCaseOfBadResponse() throws FailedToCreateRamEntity,
            IOException {
        ExecutionRequest createdRequest = createExecutionRequest();
        when(requestUtils.postRequest(any(String.class), any(ExecutionRequest.class), any()))
                .thenThrow(new IOException("Ram error"));
        abstractAdapter.startExecutionRequest(
                createdRequest.getName(),
                createdRequest.getProjectId(),
                createdRequest.getTestPlanId(),
                createdRequest.getExecutionStatus());
    }

    @Test
    public void updateTestRun_testRunPatchIsSentToRam() throws IOException, FailedToCreateRamEntity {
        TestRun testRunPatch = new TestRun();
        TestRun patchedTestRun = new TestRun();
        when(requestUtils.patchRequest(any(String.class), any(TestRun.class), any()))
                .thenReturn(patchedTestRun);
        abstractAdapter.updateTestRun(testRunPatch);
        verify(requestUtils, times(1))
                .patchRequest(argThat(url -> url.contains(PATCH_TR_PATH)),
                        eq(testRunPatch),
                        any());
    }

    @Test()
    public void updateTestRun_DetailsReportIsSentInCaseOfPatchError() throws IOException,
            FailedToCreateRamEntity {
        TestRun testRunPatch = new TestRun();
        when(requestUtils.patchRequest(any(String.class), any(TestRun.class), any()))
                .thenThrow(new IOException("RAM error"));
        when(requestUtils.postRequest(any(String.class), any(), any()))
                .thenReturn(new ExecutionRequestDetails());
        abstractAdapter.getContext().setExecutionRequestId(UUID.randomUUID().toString());
        abstractAdapter.getContext().setProjectId(UUID.randomUUID().toString());
        abstractAdapter.updateTestRun(testRunPatch);
        verify(abstractAdapter, times(1))
                .reportDetails(any(String.class), eq(TestingStatuses.FAILED));
    }

    @Test()
    public void finishAllTestRuns_ifDelayedProperEndpointIsInvoked() throws IOException,
            FailedToCreateRamEntity {
        List<UUID> uuids = asList(UUID.randomUUID(), UUID.randomUUID());
        when(requestUtils.postRequest(any(String.class), any(TestRun.class), any()))
                .thenReturn(null);
        abstractAdapter.finishAllTestRuns(uuids, true);
        verify(requestUtils, times(1)).postRequest(
                argThat(url -> url.contains(DELAYED_FINISH_TRS_PATH)),
                argThat(trUuids -> {
                    if (trUuids instanceof List) {
                        List<Object> trUuidsList = (List) trUuids;
                        return uuids.containsAll(trUuidsList);
                    }
                    return false;
                }),
                any()
        );
    }

    @Test()
    public void finishAllTestRuns_ifNotDelayedProperEndpointIsInvoked() throws IOException,
            FailedToCreateRamEntity {
        List<UUID> uuids = asList(UUID.randomUUID(), UUID.randomUUID());
        when(requestUtils.putRequest(any(String.class), any(TestRun.class), any()))
                .thenReturn(null);
        abstractAdapter.finishAllTestRuns(uuids, false);
        for (UUID uuid : uuids) {
            verify(requestUtils, times(1)).putRequest(
                    argThat(url -> url.contains(String.format(FINISH_TR_PATH_TEMPLATE, uuid))),
                    eq(null),
                    any()
            );
        }
    }

    @Test()
    public void finishAllTestRuns_DetailsReportIsSentInCaseOfFinishError() throws IOException,
            FailedToCreateRamEntity {
        List<UUID> uuids = asList(UUID.randomUUID(), UUID.randomUUID());
        when(requestUtils.putRequest(any(String.class), any(), any()))
                .thenThrow(new IOException("RAM error"));
        when(requestUtils.postRequest(any(String.class), any(), any()))
                .thenReturn(new ExecutionRequestDetails());
        abstractAdapter.getContext().setExecutionRequestId(UUID.randomUUID().toString());
        abstractAdapter.getContext().setProjectId(UUID.randomUUID().toString());
        abstractAdapter.finishAllTestRuns(uuids, false);
        verify(abstractAdapter, times(1))
                .reportDetails(any(String.class), eq(TestingStatuses.FAILED));
    }

    @Test()
    public void reportDetails_reportDetailsEndpointIsInvokedProperly() throws IOException,
            FailedToCreateRamEntity {
        when(requestUtils.postRequest(any(String.class), any(), any()))
                .thenReturn(new ExecutionRequestDetails());
        UUID erId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        abstractAdapter.getContext().setExecutionRequestId(erId.toString());
        abstractAdapter.getContext().setProjectId(projectId.toString());
        abstractAdapter.reportDetails(DETAILS_MESSAGE, TestingStatuses.FAILED);
        verify(requestUtils, times(1))
                .postRequest(
                        argThat(url -> url.contains(String.format(API_ER_DETAILS_TEMPLATE, erId))),
                        argThat(details -> {
                            if (details instanceof ExecutionRequestDetails) {
                                ExecutionRequestDetails requestDetails = (ExecutionRequestDetails) details;
                                return requestDetails.getMessage().equals(DETAILS_MESSAGE) &&
                                        requestDetails.getStatus().equals(TestingStatuses.FAILED) &&
                                        requestDetails.getExecutionRequestId().equals(erId) &&
                                        requestDetails.getProjectId().equals(projectId);
                            }
                            return false;
                        }),
                        any());
    }

    @Test
    @Ignore
    public void stopAtpRun_WithEmptyFields_CreateRequestWithoutErrors() throws IOException {
        String trId = abstractAdapter.getContext().getTestRunId();
        ObjectNode expectedRequest = OBJECT_MAPPER.createObjectNode();
        expectedRequest.put(RamConstants.TEST_RUN_ID_KEY, trId);
        expectedRequest.put(RamConstants.TESTING_STATUS_KEY, TestingStatuses.UNKNOWN.toString());
        expectedRequest.put(RamConstants.LOG_COLLECTOR_DATA_KEY, abstractAdapter.getContext().getLogCollectorData());

        abstractAdapter.stopAtpRun(trId);

        verify(requestUtils, times(1)).postRequest(any(), eq(expectedRequest.toString()), any());
    }

    @Test(expected = FailedToCreateRamEntity.class)
    public void reportDetails_FailedToCreateRamEntityIsThrownInCaseOfRamResponseError() throws IOException,
            FailedToCreateRamEntity {
        when(requestUtils.postRequest(any(String.class), any(), any()))
                .thenThrow(new IOException("RAM error"));
        UUID erId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        abstractAdapter.getContext().setExecutionRequestId(erId.toString());
        abstractAdapter.getContext().setProjectId(projectId.toString());
        abstractAdapter.reportDetails(DETAILS_MESSAGE, TestingStatuses.FAILED);
    }

    @Test
    public void testUploadFile_AttachmentStreamParameterSpecified_AttachmentStreamParameterIsUsed() throws IOException {
        Message message = new Message();
        InputStream streamToUpload = new ByteArrayInputStream(new byte[]{1, 2, 3});
        Map<String, Object> attributes = createAttributes(streamToUpload);
        when(requestUtils.postRequestStream(any(String.class), any(), any())).thenReturn(new UploadScreenshotResponse());
        abstractAdapter.uploadFile(attributes, message);
        verify(requestUtils, times(1))
                .postRequestStream(any(String.class), eq(streamToUpload), eq(UploadScreenshotResponse.class));
    }

    @Test
    public void testUploadFile_AttachmentStreamParameterIsAbsent_ScreenshotFileKeyParameterIsUsed() throws IOException {
        Message message = new Message();
        ClassPathResource classPathResource = new ClassPathResource("fileToUpload.txt");
        File file = classPathResource.getFile();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(RamConstants.SCREENSHOT_TYPE_KEY, "image/png");
        attributes.put(RamConstants.SCREENSHOT_NAME_KEY, "image.png");
        attributes.put(RamConstants.SCREENSHOT_SOURCE_KEY, "image.png");
        attributes.put(RamConstants.SCREENSHOT_FILE_KEY, file);
        when(requestUtils.postRequestStream(any(String.class), any(), any())).thenReturn(new UploadScreenshotResponse());
        abstractAdapter.uploadFile(attributes, message);
        verify(requestUtils, times(1))
                .postRequestStream(any(String.class),
                        any(InputStream.class),
                        eq(UploadScreenshotResponse.class));
    }

    @Test
    public void testUploadFile_IOExceptionIsThrownDuringRamInvocation_MethodReturnsNormally() throws IOException {
        // given
        Message message = new Message();
        InputStream streamToUpload = new ByteArrayInputStream(new byte[]{1, 2, 3});
        Map<String, Object> attributes = createAttributes(streamToUpload);
        String exceptionString = "Exception during RAM invoking";
        String expectedErrorString = "Failed to upload file for LR";
        // when
        when(requestUtils.postRequestStream(any(String.class), any(), any()))
                .thenThrow(new IOException(exceptionString));
        abstractAdapter.uploadFile(attributes, message);
        // then
        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(abstractAdapter, times(1)).message(messageArgumentCaptor.capture());
        assertEquals(message.getUuid(), messageArgumentCaptor.getValue().getParentRecordId());
        assertEquals(expectedErrorString, messageArgumentCaptor.getValue().getName());
        assertEquals(TestingStatuses.WARNING.name(), messageArgumentCaptor.getValue().getTestingStatus());
        assertTrue(messageArgumentCaptor.getValue().getMessage().contains(expectedErrorString));
        assertTrue(messageArgumentCaptor.getValue().getMessage().contains(exceptionString));
        verify(abstractAdapter).sendLogRecord(any(), any());
    }

    @Test
    public void testUploadFile_ResponseFromRamIsReturnedAsIs() throws IOException {
        Message message = new Message();
        InputStream streamToUpload = new ByteArrayInputStream(new byte[]{1, 2, 3});
        Map<String, Object> attributes = createAttributes(streamToUpload);
        UploadScreenshotResponse expectedResponse = new UploadScreenshotResponse();
        expectedResponse.setFileId("fileId");
        expectedResponse.setPreview("preview");
        when(requestUtils.postRequestStream(any(String.class), any(), any()))
                .thenReturn(expectedResponse);
        UploadScreenshotResponse actualResponse = abstractAdapter.uploadFile(attributes, message);
        assertEquals("UploadScreenshotResponse should be returned from RAM",
                expectedResponse, actualResponse);
    }

    private Map<String, Object> createAttributes(InputStream streamToUpload) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(RamConstants.SCREENSHOT_TYPE_KEY, "image/png");
        attributes.put(RamConstants.SCREENSHOT_NAME_KEY, "image.png");
        attributes.put(RamConstants.SCREENSHOT_SOURCE_KEY, "image.png");
        attributes.put(RamConstants.ATTACHMENT_STREAM_KEY, streamToUpload);
        return attributes;
    }

    private boolean contentEquals(InputStream expectedStream, InputStream actualStream) {
        try {
            return IOUtils.contentEquals(expectedStream, actualStream);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    @Test
    public void createLogRecord_SetEmptyTypeAction_GenerateValidLogRecord() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        Assert.assertEquals("Type action is default value (TECHNICAL)", TypeAction.TECHNICAL, logRecord.getType());
    }

    @Test
    public void createLogRecord_SetNotEmptyTypeAction_GenerateValidLogRecord() {
        Message message = ModelMocks.generateMessage(TypeAction.BV.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        Assert.assertEquals("Type action is BV", TypeAction.BV, logRecord.getType());
    }

    @Test
    public void createLogRecord_SetNullAsStringLogRecordIdInMessage_GenerateLogRecordWithRandomId() {
        Message message = ModelMocks.generateMessage(TypeAction.BV.toString());
        message.setUuid("null");
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue("ID of LogRecord should be filled", Objects.nonNull(logRecord.getUuid()));
    }

    @Test
    public void createLogRecord_SetCreatedDateStamp_GenerateLogRecordWithCreatedDateStamp() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        long createdDateStamp = new Random().nextLong();
        message.setCreatedDateStamp(createdDateStamp);
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        Assert.assertEquals("Created Date is set", createdDateStamp, logRecord.getCreatedDateStamp());
    }

    @Test
    public void createBvLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.BV.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof BvLogRecord);
    }

    @Test
    public void createMiaLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.MIA.toString());
        message.setIsGroup(true);
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof MiaLogRecord);
    }

    @Test
    public void createUiLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.UI.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof UiLogRecord);
    }

    @Test
    public void createItfLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.ITF.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        assertTrue(logRecord instanceof ItfLogRecord);
    }

    @Test
    public void createRestLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.REST.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof RestLogRecord);
    }

    @Test
    public void createSqlLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.SQL.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof SqlLogRecord);
    }

    @Test
    public void createSshLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.SSH.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof SshLogRecord);
    }

    @Test
    public void createCompoundLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.COMPOUND.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        assertTrue(logRecord instanceof CompoundLogRecord);
    }

    @Test
    public void createTechnicalLogRecordTest() {
        Message message = ModelMocks.generateMessage(TypeAction.TECHNICAL.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        assertTrue(logRecord instanceof TechnicalLogRecord);

    }

    @Test
    public void createLogRecord_SetParentSectionIdAsNull_GenerateLogRecordWithEmptyParentId() {
        Message message = ModelMocks.generateMessage(TypeAction.BV.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        assertTrue("ID of parent LogRecord should be empty", Objects.isNull(logRecord.getParentRecordId()));
    }

    @Test
    public void createLogRecord_maskMessageAndTitle_generateEncryptedMessage() {
        Message message = ModelMocks.generateEncryptedMessage(TypeAction.BV.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        assertEquals("Log record title should be masked",
                MASKED_MESSAGE, logRecord.getName());
        assertEquals("Log record message should be masked", MASKED_MESSAGE, logRecord.getMessage());
    }

    @Test
    public void createLogRecord_SetMessageFieldInMessage_GenerateLogRecordWithFilledMessage() {
        Message message = ModelMocks.generateMessage(TypeAction.BV.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        Assert.assertEquals("Message of LogRecord should be filled", message.getMessage(), logRecord.getMessage());
    }

    @Test
    public void createLogRecord_ConvertValue_KeepsCorrectTestingStatus() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        String expectedStatus = TestingStatuses.FAILED.toString();
        message.setTestingStatus(expectedStatus.toUpperCase());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        Assert.assertEquals("Log record should keep status through conversion", expectedStatus,
                logRecord.getTestingStatus().toString());
    }

    @Test
    public void createLogRecord_ConvertValue_KeepsCorrectTestingStatusUnderscore() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        String expectedStatus = TestingStatuses.NOT_STARTED.toString();
        message.setTestingStatus(expectedStatus);
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        Assert.assertEquals("Log record should keep status through conversion", expectedStatus,
                logRecord.getTestingStatus().toString());
    }

    @Test
    public void createLogRecord_NotEmptyStepContextVariables_GenerateLogRecordWithNotEmptyStepContextVariables() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        message.setStepContextVariables(contextVariables);
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        Assert.assertEquals("Log record should keep step context variables through conversion", contextVariables,
                logRecord.getStepContextVariables());
    }

    @Test
    public void createLogRecord_NotEmptyCustomLinks_GenerateLogRecordWithNotEmptyCustomLinks() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        List<CustomLink> customLinks = new ArrayList<>();
        customLinks.add(new CustomLink("name1", "http://url1", OpenMode.NEW_TAB));
        customLinks.add(new CustomLink("name2", "http://url2", OpenMode.CURRENT_TAB));
        message.setCustomLinks(customLinks);

        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        List<CustomLink> resCustomLinks = logRecord.getCustomLinks();
        assertNotNull(resCustomLinks);
        Assert.assertEquals(2, resCustomLinks.size());
        CustomLink customLink = resCustomLinks.get(0);
        Assert.assertEquals("name1", customLink.getName());
        Assert.assertEquals("http://url1", customLink.getUrl());
        Assert.assertEquals(OpenMode.NEW_TAB, customLink.getOpenMode());
        customLink = resCustomLinks.get(1);
        Assert.assertEquals("name2", customLink.getName());
        Assert.assertEquals("http://url2", customLink.getUrl());
        Assert.assertEquals(OpenMode.CURRENT_TAB, customLink.getOpenMode());
    }

    @Test
    public void updateLogRecordDurationAndEndDate_SetNullEndDate_CalculateDateAndDuration() {
        LogRecord logRecord = new LogRecord();
        logRecord.setStartDate(new Timestamp(System.currentTimeMillis() - 1));
        abstractAdapter.updateLogRecordDurationAndEndDate(logRecord);
        Assert.assertNotNull("End date of log record should be filled", logRecord.getEndDate());
        Assert.assertTrue("Duration of log record should be > 0", logRecord.getDuration() > 0);
    }

    @Test
    public void setAtpCompound_NewCompoundWithWarningStatus_NewStatusMustBeSet() {
        TestRunContext context = abstractAdapter.getContext();
        context.setCompoundAndUpdateCompoundStatuses(ModelMocks.generateAtpCompounds(TestingStatuses.FAILED));
        AtpCompaund newCompound = ModelMocks.generateAtpCompounds(TestingStatuses.WARNING);
        context.setCompoundAndUpdateCompoundStatuses(newCompound);

        AtpCompaund result = context.getAtpCompaund();

        Assert.assertEquals(newCompound.getTestingStatuses(), result.getTestingStatuses());
        Assert.assertEquals(newCompound.getParentSection().getTestingStatuses(),
                result.getParentSection().getTestingStatuses());
    }

    @Test
    public void setAtpCompound_NewCompoundWithUnknownStatus_OldStatusMustBeSet() {
        TestRunContext context = abstractAdapter.getContext();
        AtpCompaund compoundFromContext = ModelMocks.generateAtpCompounds(TestingStatuses.FAILED);
        context.setCompoundAndUpdateCompoundStatuses(compoundFromContext);
        context.setCompoundAndUpdateCompoundStatuses(ModelMocks.generateAtpCompounds(TestingStatuses.UNKNOWN));

        AtpCompaund result = context.getAtpCompaund();

        Assert.assertEquals(compoundFromContext.getTestingStatuses(), result.getTestingStatuses());
        Assert.assertEquals(compoundFromContext.getParentSection().getTestingStatuses(),
                result.getParentSection().getTestingStatuses());
    }

    @Test
    public void closeSection_doNotSendUpdate_whenSectionNotChanged() {
        Message sectionMessage = ModelMocks.generateMessage("UI");
        sectionMessage.setExecutionStatus(ExecutionStatuses.FINISHED.toString());
        sectionMessage.setTestingStatus(TestingStatuses.NOT_STARTED.toString());

        abstractAdapter.openSection(sectionMessage);
        abstractAdapter.closeSection();
        verify(abstractAdapter, times(1)).sendLogRecord(any());
    }

    @Test
    public void closeSection_sendUpdate_whenSectionChanged() throws InterruptedException {
        Message sectionMessage = ModelMocks.generateMessage("UI");
        sectionMessage.setStartDate(getCurrentTimestamp());
        abstractAdapter.openSection(sectionMessage);
        Thread.sleep(1000);
        abstractAdapter.closeSection();
        verify(abstractAdapter, times(2)).sendLogRecord(any());
    }

    @Test
    public void createLogRecord_SectionWithTestingStatusIsNotStarted_KeepsCorrectExecutionStatus() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        ExecutionStatuses expectedStatus = ExecutionStatuses.NOT_STARTED;
        message.setTestingStatus(TestingStatuses.NOT_STARTED.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        Assert.assertEquals("Log record should keep status through conversion", expectedStatus,
                logRecord.getExecutionStatus());
    }

    @Test
    public void createLogRecord_NotSectionWithAnyTestingStatus_KeepsCorrectExecutionStatus() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        ExecutionStatuses expectedStatus = ExecutionStatuses.FINISHED;
        message.setTestingStatus(TestingStatuses.NOT_STARTED.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, false, false);
        Assert.assertEquals("Log record should keep status through conversion", expectedStatus,
                logRecord.getExecutionStatus());
    }

    @Test
    public void createLogRecord_SectionWithAnyTestingStatus_KeepsCorrectExecutionStatus() {
        Message message = ModelMocks.generateMessage(StringUtils.EMPTY);
        ExecutionStatuses expectedStatus = ExecutionStatuses.IN_PROGRESS;
        message.setTestingStatus(TestingStatuses.UNKNOWN.toString());
        LogRecord logRecord = abstractAdapter.createLogRecord(message, true, false);
        Assert.assertEquals("Log record should keep status through conversion", expectedStatus,
                logRecord.getExecutionStatus());
    }

    @Test
    public void test() {
        TestRunContext context = new TestRunContext();
        context.setTestRunId(UUID.randomUUID().toString());

        LogRecord logRecord1 = new LogRecord();
        logRecord1.setUuid(UUID.randomUUID());
        logRecord1.setName("logrecord1");

        LogRecord logRecord2 = new LogRecord();
        logRecord2.setUuid(UUID.randomUUID());
        logRecord2.setName("logrecord2");

        LogRecordsStack stack = new LogRecordsStack();
        stack.push(logRecord1);
        stack.push(logRecord2);

        context.setSections(stack);

        LogRecord res = context.getCurrentSection();
        res.setMessage("message");

        System.out.println(context.getSections().pop().getMessage());
    }

    private ExecutionRequest createExecutionRequest() {
        ExecutionRequest createdRequest = new ExecutionRequest();
        createdRequest.setName("ER name");
        createdRequest.setUuid(UUID.randomUUID());
        createdRequest.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        createdRequest.setTestPlanId(UUID.randomUUID());
        createdRequest.setProjectId(UUID.randomUUID());
        return createdRequest;
    }

    @Test
    public void getUploadFileUrl_GenerateUrlWithSpaceSpecialCharacterInFileName_SpaceCharacterInFileNameParamEncoded() {

        // Arrange
        Map<String, Object> attribute = Mockito.mock(Map.class);
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_NAME_KEY))
                .thenReturn("screenshotNameKey withSpaceCharacter");
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_TYPE_KEY)).thenReturn("screenshotTypeKey");
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY)).thenReturn("screenshotSourceKey");
        Mockito.when(attribute.get(RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY))
                .thenReturn("screenshotExternalSourceKey");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getUuid()).thenReturn("");

        // Act
        String result = abstractAdapter.getUploadFileUrl(attribute, message);

        // Assert
        assertFalse("Result String with URL contains space ' ' character but should be encoded.",
                result.contains(" "));
        assertTrue("Result String with URL doesnt contain correct fileName param",
                result.contains("fileName=screenshotNameKey+withSpaceCharacter&"));
    }
}
