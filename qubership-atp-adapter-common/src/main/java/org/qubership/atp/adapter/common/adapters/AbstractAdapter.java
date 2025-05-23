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

import static org.qubership.atp.adapter.common.RamConstants.NULL_VALUE;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.getCurrentTimestamp;
import static org.qubership.atp.ram.enums.ExecutionStatuses.FINISHED;
import static org.qubership.atp.ram.enums.ExecutionStatuses.IN_PROGRESS;
import static org.qubership.atp.ram.enums.ExecutionStatuses.findByValue;
import static java.util.Objects.isNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.error.AdapterMethodIsNotSupported;
import org.qubership.atp.adapter.common.adapters.error.FailedToCreateRamEntity;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.CompoundLogRecordContainer;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.entities.UploadScreenshotResponse;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.utils.Utils;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.adapter.common.ws.StartRunResponse;
import org.qubership.atp.crypt.CryptoTools;
import org.qubership.atp.ram.dto.request.UpdateLogRecordContextVariablesRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordExecutionStatusRequest;
import org.qubership.atp.ram.dto.request.UpdateLogRecordFields;
import org.qubership.atp.ram.enums.EngineCategory;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequest;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunStatistic;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.CompoundLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.MiaLogRecord;
import org.qubership.atp.ram.models.logrecords.RbmLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.TechnicalLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import lombok.SneakyThrows;
import net.sf.json.JSONObject;

public abstract class AbstractAdapter implements AtpRamAdapter {

    private static final Logger log = LoggerFactory.getLogger(AbstractAdapter.class);

    private static final Map<TypeAction, Class<? extends LogRecord>> logRecordTypeByTypeAction;

    static {
        Map<TypeAction, Class<? extends LogRecord>> map = new HashMap<>();
        map.put(TypeAction.UI, UiLogRecord.class);
        map.put(TypeAction.ITF, ItfLogRecord.class);
        map.put(TypeAction.BV, BvLogRecord.class);
        map.put(TypeAction.MIA, MiaLogRecord.class);
        map.put(TypeAction.REST, RestLogRecord.class);
        map.put(TypeAction.SQL, SqlLogRecord.class);
        map.put(TypeAction.SSH, SshLogRecord.class);
        map.put(TypeAction.COMPOUND, CompoundLogRecord.class);
        map.put(TypeAction.TECHNICAL, TechnicalLogRecord.class);
        map.put(TypeAction.TRANSPORT, RestLogRecord.class);
        logRecordTypeByTypeAction = new HashMap<>(map);
    }

    protected TestRunContext context;
    protected String atpRamUrl;
    protected String atpRamReceiverUrl;
    protected RequestUtils requestUtils;
    protected String uploadUrlTemplate;

    protected void setRequestUtils(RequestUtils requestUtils) {
        this.requestUtils = requestUtils;
    }

    /**
     * Init adapter.
     */
    public AbstractAdapter(String testRunName) {
        Config cfg = Config.getConfig();
        Properties props = new Properties();
        atpRamUrl = cfg.getProperty(RamConstants.ATP_RAM_URL_KEY, "http://localhost:8080");
        atpRamReceiverUrl = cfg.getProperty(RamConstants.ATP_RAM_RECEIVER_URL_KEY, "http://atp-ram-receiver:8080");
        uploadUrlTemplate = atpRamReceiverUrl
                + RamConstants.RAM_RECEIVER_API_PATH
                + RamConstants.LOG_RECORDS_PATH
                + RamConstants.UPLOAD_PATH
                + "/%s"//LR id
                + RamConstants.STREAM_PATH
                + "?fileName=%s&contentType=%s&snapshotSource=%s&snapshotExternalSource=%s";
        this.requestUtils = new RequestUtils();
    }

    @Override
    public void setContext(TestRunContext context) {
        this.context = context;
    }

    @Override
    public TestRunContext getContext() {
        return context;
    }

    @Override
    public TestRunContext startExecutionRequest(
            String executionRequestName,
            UUID projectId,
            UUID testPlanId,
            ExecutionStatuses executionStatuses) throws FailedToCreateRamEntity {
        log.debug("Creating ER {} in project {} and test plan {}",
                executionRequestName, projectId, testPlanId);
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName(executionRequestName);
        executionRequest.setProjectId(projectId);
        executionRequest.setTestPlanId(testPlanId);
        executionRequest.setExecutionStatus(executionStatuses);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        return createExecutionRequest(executionRequest);

    }

    @Override
    public TestRunContext startExecutionRequest(String executionRequestName, UUID projectId, UUID testPlanId,
                                                ExecutionStatuses executionStatuses, String jointExecutionKey,
                                                Integer jointExecutionTimeout,
                                                Integer jointExecutionCount) throws FailedToCreateRamEntity {
        log.debug("Creating ER {} with joint properties in project {} and test plan {}",
                executionRequestName, projectId, testPlanId);
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setName(executionRequestName);
        executionRequest.setProjectId(projectId);
        executionRequest.setTestPlanId(testPlanId);
        executionRequest.setExecutionStatus(executionStatuses);
        executionRequest.setStartDate(new Timestamp(System.currentTimeMillis()));
        executionRequest.setJointExecutionKey(jointExecutionKey);
        executionRequest.setJointExecutionTimeout(jointExecutionTimeout);
        executionRequest.setJointExecutionCount(jointExecutionCount);
        return createExecutionRequest(executionRequest);
    }

    private TestRunContext createExecutionRequest(ExecutionRequest executionRequest) throws FailedToCreateRamEntity {
        try {
            ExecutionRequest createdExecutionRequest = requestUtils.postRequest(getCreateErUrl(), executionRequest,
                    ExecutionRequest.class);
            log.debug("Created ER {} with id {} in project {} and test plan {}",
                    createdExecutionRequest.getName(),
                    createdExecutionRequest.getUuid(),
                    createdExecutionRequest.getProjectId(),
                    createdExecutionRequest.getTestPlanId());
            context = context != null ? context : new TestRunContext();
            context.setExecutionRequestName(createdExecutionRequest.getName());
            context.setExecutionRequestId(createdExecutionRequest.getUuid().toString());
            context.setProjectId(createdExecutionRequest.getProjectId().toString());
            context.setTestPlanId(createdExecutionRequest.getTestPlanId().toString());
            return context;
        } catch (IOException ioException) {
            log.error("Failed to create ER!", ioException);
            throw new FailedToCreateRamEntity(
                    String.format("Failed to create ER with name %s", executionRequest),
                    ioException);
        }
    }

    /**
     * Url to create ER in RAM.
     */
    protected String getCreateErUrl() {
        return atpRamUrl
                + RamConstants.API_PATH
                + RamConstants.EXECUTION_REQUESTS_PATH
                + RamConstants.CREATE_PATH;
    }

    @Override
    public ExecutionRequestDetails reportDetails(String message, TestingStatuses status)
            throws FailedToCreateRamEntity {
        try {
            String executionRequestId = context.getExecutionRequestId();
            log.debug("Sending details to RAM {} for ER {}", message, executionRequestId);
            ExecutionRequestDetails details = new ExecutionRequestDetails();
            details.setMessage(message);
            details.setStatus(status);
            details.setExecutionRequestId(UUID.fromString(executionRequestId));
            details.setProjectId(UUID.fromString(context.getProjectId()));
            return requestUtils.postRequest(
                    getReportDetailsUrl(executionRequestId),
                    details,
                    ExecutionRequestDetails.class);
        } catch (IOException ioException) {
            log.error("Unable to report details to RAM", ioException);
            throw new FailedToCreateRamEntity("Unable to report details to RAM", ioException);
        }
    }

    protected String getReportDetailsUrl(String executionRequestId) {
        return atpRamUrl
                + RamConstants.API_PATH
                + RamConstants.EXECUTION_REQUESTS_PATH
                + "/" + executionRequestId
                + RamConstants.DETAILS_PATH;
    }

    /**
     * Updates TR finish date by current time. May be useful if we can't track
     * the last step in test run.
     */
    @Override
    public void updateTestRun(TestRun testRunPatch) throws FailedToCreateRamEntity {
        try {
            log.debug("Patching TR with id {}. ", testRunPatch.getUuid());
            requestUtils.patchRequest(atpRamUrl
                            + RamConstants.RAM_EXECUTOR_PATH
                            + RamConstants.TEST_RUNS_PATH
                            + RamConstants.PATCH_PATH,
                    testRunPatch, null);
        } catch (IOException ioException) {
            log.error("Failed to update TR {} in RAM", context.getTestRunId(), ioException);
            reportDetails("Failed to update TR " + testRunPatch.getUuid() + " in RAM."
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
    }

    /**
     * Stop all provided test runs in RAM w/o finish date update.
     */
    public void finishAllTestRuns(List<UUID> testRunUuids, boolean isDelayed) throws FailedToCreateRamEntity {
        try {
            if (isDelayed) {
                log.debug("Delayed finishing TRs {}", testRunUuids);
                requestUtils.postRequest(atpRamUrl
                        + RamConstants.RAM_EXECUTOR_PATH
                        + RamConstants.TEST_RUNS_PATH
                        + RamConstants.BULK_PATH
                        + RamConstants.FINISH_PATH
                        + RamConstants.DELAYED_PATH, testRunUuids, null);
            } else {
                log.debug("Finishing TRs {}", testRunUuids);
                for (UUID testRunUuid : testRunUuids) {
                    requestUtils.putRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.TEST_RUNS_PATH
                            + "/" + testRunUuid.toString()
                            + RamConstants.FINISH_PATH, null, null);
                }
            }
        } catch (IOException ioException) {
            log.error("Failed to finish TRs {} in RAM", testRunUuids, ioException);
            reportDetails("Failed to stop TRs in RAM due to "
                    + ExceptionUtils.getStackTrace(ioException), TestingStatuses.FAILED);
        }
    }

    @SneakyThrows
    @Override
    public TestRunContext startAtpRun(StartRunRequest request, TestRunContext initialContext) {
        this.context = initialContext;

        context.setMailList(request.getMailList());
        String url = getStartAtpRunUrl();
        log.debug("Starting TR {} in RAM. RAM_URL {}", request, url);
        StartRunResponse response = requestUtils.postRequest(url, request, StartRunResponse.class);
        context.setTestRunId(response.getTestRunId().toString());
        context.setExecutionRequestId(response.getExecutionRequestId().toString());
        context.setProjectId(request.getProjectId().toString());
        if (Objects.nonNull(request.getTestPlanId())) {
            context.setTestPlanId(request.getTestPlanId().toString());
        }
        log.info("Started TR {}", response);
        return context;
    }

    protected String getStartAtpRunUrl() {
        return atpRamUrl
                + RamConstants.RAM_EXECUTOR_PATH
                + RamConstants.TEST_RUNS_PATH
                + RamConstants.CREATE_PATH;
    }

    @Override
    public abstract String saveConfigInfo(Map<String, String> configFiles,
                                          org.qubership.atp.adapter.common.utils.EngineCategory category);

    @Override
    public abstract String saveConfigInfo(Map<String, String> configFiles, EngineCategory category);

    @Override
    public TestRunContext updateAtpRun(TestRunContext context) {
        this.context = context;
        String url = atpRamUrl
                + RamConstants.RAM_EXECUTOR_PATH
                + RamConstants.TEST_RUNS_PATH
                + RamConstants.UPDATE_OR_CREATE_PATH;
        ObjectNode testRunRequest = OBJECT_MAPPER.createObjectNode();
        testRunRequest.put(RamConstants.TEST_RUN_ID_KEY, this.context.getTestRunId());
        testRunRequest.put(RamConstants.URL_TO_BROWSER_OR_LOGS_KEY, this.context.getUrlToBrowserOrLogs().toString());

        RequestUtils.postRequest(url, testRunRequest.toString());
        log.debug("TestRun with id: {} was updated with url to browser: {}",
                this.context.getTestRunId(),
                this.context.getUrlToBrowserOrLogs());
        return this.context;
    }

    @Override
    public TestRunContext stopAtpRun(String testRunId) {
        ObjectNode response = RequestUtils
                .postRequest(atpRamUrl
                                + RamConstants.RAM_EXECUTOR_PATH
                                + RamConstants.TEST_RUNS_PATH
                                + RamConstants.STOP_PATH,
                        createRequestToStopTestRun(testRunId).toString());
        if (Objects.nonNull(response)) {
            log.debug("TestRun with id: {} was stopped with status: {}", testRunId, response);
        }
        return null;
    }

    @Override
    public TestRunContext stopTestRun(String testRunId) throws Exception {
        ObjectNode response = requestUtils
                .postRequest(
                        atpRamUrl
                                + RamConstants.RAM_EXECUTOR_PATH
                                + RamConstants.TEST_RUNS_PATH
                                + RamConstants.STOP_PATH,
                        createRequestToStopTestRun(testRunId),
                        ObjectNode.class);
        if (Objects.nonNull(response)) {
            log.debug("TestRun with id: {} was stopped with status: {}", testRunId, response.asText());
        }
        return null;
    }

    private ObjectNode createRequestToStopTestRun(String testRunId) {
        ObjectNode testRunRequest = OBJECT_MAPPER.createObjectNode();
        testRunRequest.put(RamConstants.TEST_RUN_ID_KEY, testRunId);
        if (this.context != null) {
            if (Objects.nonNull(this.context.getUrlToBrowserOrLogs())) {
                testRunRequest.put(RamConstants.URL_TO_BROWSER_OR_LOGS_KEY,
                        this.context.getUrlToBrowserOrLogs().toString());
            }
            testRunRequest.put(RamConstants.TESTING_STATUS_KEY, Objects.isNull(context.getTestingStatus())
                    ? TestingStatuses.UNKNOWN.toString() : String.valueOf(context.getTestingStatus()));
            testRunRequest.put(RamConstants.LOG_COLLECTOR_DATA_KEY, context.getLogCollectorData());
        } else {
            log.warn("Context for Test Run {} not provided to stop request", testRunId);
        }
        return testRunRequest;
    }

    /**
     * Added compounds in context and update start date if his child is the first.
     *
     * @param compound This is the added compound.
     * @return Context.
     */
    @Override
    public TestRunContext openCompoundSection(Message compound, boolean isStep) {
        CompoundLogRecordContainer logRecordRequest = OBJECT_MAPPER.convertValue(compound,
                CompoundLogRecordContainer.class);
        logRecordRequest.setStep(isStep);
        logRecordRequest.setLastInSection(compound.isLastInSection());
        context.setLogRecordUuid(logRecordRequest.getUuid().toString());
        context.addSection(logRecordRequest);
        log.debug("Added section [{}] with name [{}] in context.", logRecordRequest.getUuid(),
                logRecordRequest.getName());
        return context;
    }

    //todo add test for this + add checking parent ID for other methods
    private TestRunContext addSection(Message section, boolean isStep) {
        try {
            LogRecord logRecordRequest = createLogRecord(section, true, isStep);
            log.debug("Try to create log record (section) with ID {}, TestRunId {} and status {}",
                    logRecordRequest.getUuid(), logRecordRequest.getTestRunId(),
                    logRecordRequest.getTestingStatus());
            sendLogRecord(logRecordRequest);
            log.debug("Log record created (section) with ID {}, TestRunId {} and status {}",
                    logRecordRequest.getUuid(), logRecordRequest.getTestRunId(),
                    logRecordRequest.getTestingStatus());
            if (!isStep) {
                context.setLastSectionInStep(logRecordRequest.getUuid().toString());
            }
            context.setLogRecordUuid(logRecordRequest.getUuid().toString());
            context.addSection(logRecordRequest);
        } catch (Exception e) {
            log.error("Unable add section", e);
        }
        return context;
    }

    LogRecord createLogRecord(Message message, boolean isSection, boolean isStepFromAtpCompound) {
        try {
            if (NULL_VALUE.equalsIgnoreCase(message.getUuid())) {
                message.setUuid(null);
            }
            message.setMessage(CryptoTools.maskEncryptedData(message.getMessage()));
            message.setName(CryptoTools.maskEncryptedData(message.getName()));
            LogRecord logRecord;
            if (message.getType() != null) {
                Class<? extends LogRecord> logRecordType =
                        logRecordTypeByTypeAction.get(TypeAction.valueOf(message.getType()));
                if (logRecordType != null) {
                    logRecord = OBJECT_MAPPER.convertValue(message, logRecordType);
                    logRecord.setStepContextVariables(message.getStepContextVariables());
                    logRecord.setMessageParameters(message.getMessageParameters());
                    setParamsForLogRecord(logRecord, isSection, isStepFromAtpCompound);
                    return logRecord;
                }
            } else {
                message.setType(TypeAction.TECHNICAL.name());
            }
            logRecord = OBJECT_MAPPER.convertValue(message, LogRecord.class);
            logRecord.setStepContextVariables(message.getStepContextVariables());
            logRecord.setMessageParameters(message.getMessageParameters());
            setParamsForLogRecord(logRecord, isSection, isStepFromAtpCompound);
            return logRecord;
        } catch (Exception e) {
            log.error("Unable prepare log record", e);
            return null;
        }
    }

    @Override
    public TestRunContext openSection(String name, String message, String status) {
        Message section = new Message();
        section.setName(name);
        section.setMessage(message);
        section.setTestingStatus(status);
        return addSection(section, false);
    }

    @Override
    public TestRunContext openSection(Message section) {
        return addSection(section, false);
    }

    @Override
    public TestRunContext openSection(Message message, boolean isStep) {
        return addSection(message, isStep);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      String status, String type, boolean isStepFromAtpCompound, boolean hidden) {
        return openSection(name, message, parentSectionId, sectionId, status, type, isStepFromAtpCompound, hidden,
                Collections.emptySet());
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      String status, String type, boolean isStepFromAtpCompound, boolean hidden,
                                      Set<String> validationLabels) {
        Message section = new Message();
        section.setName(name);
        section.setMessage(message);
        section.setTestingStatus(status);
        section.setParentRecordId(parentSectionId);
        section.setUuid(sectionId);
        section.setType(type);
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setHidden(hidden);
        section.setMetaInfo(metaInfo);
        section.setValidationLabels(isNull(validationLabels) ? Collections.emptySet() : validationLabels);
        log.debug("openSection before addSection {}", section);
        return addSection(section, isStepFromAtpCompound);
    }

    @Override
    public TestRunContext openSection(String name, String message,
                                      String parentSectionId, String sectionId, String status) {
        return openSection(name, message, parentSectionId, sectionId, status, Collections.emptySet());
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      String status, Set<String> validationLabels) {
        Message section = new Message();
        section.setName(name);
        section.setMessage(message);
        section.setTestingStatus(status);
        section.setParentRecordId(parentSectionId);
        section.setUuid(sectionId);
        section.setValidationLabels(isNull(validationLabels) ? Collections.emptySet() : validationLabels);
        return addSection(section, false);
    }

    @Override
    public TestRunContext openSection(String name, String message,
                                      String parentSectionId, String sectionId, String status, String type) {
        return openSection(name, message, parentSectionId, sectionId, status, type, false, false);
    }

    @Override
    public TestRunContext openSection(String name, String message,
                                      String parentSectionId, String sectionId, String status, String type,
                                      boolean hidden) {
        return openSection(name, message, parentSectionId, sectionId, status, type, false, hidden);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      boolean isCompound, String status) {
        return openSection(name, message, parentSectionId, sectionId, isCompound, status, null, false, false);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      boolean isCompound, String status, String type) {
        return openSection(name, message, parentSectionId, sectionId, isCompound, status, type,
                false, false);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      boolean isCompound, String status, String type, boolean hidden) {
        return openSection(name, message, parentSectionId, sectionId, isCompound, status, type, false, hidden);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      boolean isCompound, String status, String type, boolean isStepFromAtpCompound,
                                      boolean hidden) {
        Message section = setParamsForMessageBean(sectionId, name, message, status, Collections.emptyMap(),
                Collections.emptySet(), type, hidden, new Message());
        section.setParentRecordId(parentSectionId);
        return addSection(section, isStepFromAtpCompound);
    }

    @Override
    public TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                                      boolean isCompound, String status, String type, boolean isStepFromAtpCompound,
                                      boolean hidden, Set<String> validationLabels) {
        Message section = setParamsForMessageBean(sectionId, name, message, status, Collections.emptyMap(),
                Collections.emptySet(), type, hidden, new Message());
        section.setParentRecordId(parentSectionId);
        section.setValidationLabels(isNull(validationLabels) ? Collections.emptySet() : validationLabels);
        return addSection(section, isStepFromAtpCompound);
    }

    @Override
    public TestRunContext openItfSection(Message messageBean, JSONObject validationTable) {
        try {
            if (Strings.isNullOrEmpty(messageBean.getType())) {
                messageBean.setType(TypeAction.ITF.toString());
            }
            ItfLogRecord itfLogRecord =
                    (ItfLogRecord) createLogRecord(messageBean, true, false);

            log.debug("Configured log record (ITF section) with ID {}", itfLogRecord.getUuid());

            if (Objects.nonNull(validationTable)) {
                try {
                    itfLogRecord.setValidationTable(Utils.parseValidationTableFromJson(validationTable));
                } catch (IOException ex) {
                    log.error("Failed to parse validation table for LogRecord: {} from json: {}.",
                            itfLogRecord.getUuid(), validationTable, ex);
                }
            }
            sendItfLogRecord(itfLogRecord);
            context.setLastSectionInStep(itfLogRecord.getUuid().toString());
            context.setLogRecordUuid(itfLogRecord.getUuid().toString());
            context.addSection(itfLogRecord);
        } catch (Exception e) {
            log.error("Unable create ITF section", e);
        }
        return context;
    }

    @Override
    public TestRunContext openMiaSection(Message messageBean) {
        try {
            if (Strings.isNullOrEmpty(messageBean.getType())) {
                messageBean.setType(TypeAction.MIA.toString());
            }
            MiaLogRecord miaLogRecord =
                    (MiaLogRecord) createLogRecord(messageBean, true, false);

            log.debug("Configured log record (MIA section): {}", miaLogRecord);
            sendMiaLogRecord(miaLogRecord);

            context.setLogRecordUuid(miaLogRecord.getUuid().toString());
            context.addSection(miaLogRecord);
            context.setLastSectionInStep(miaLogRecord.getUuid().toString());
        } catch (Exception e) {
            log.error("Unable create MIA section", e);
        }
        return context;
    }

    @Override
    public TestRunContext closeSection() {
        LogRecord section = context.getCurrentSection();
        log.debug("Close section with id {}", section.getUuid());
        try {
            section.setExecutionStatus(FINISHED);
            updateLogRecordDurationAndEndDate(section);
            final boolean isCurrentSectionChanged = context.isCurrentSectionChanged();
            log.debug("Section {} before remove with sectionId {}", section, section.getUuid());
            context.removeSection();
            log.debug("Close section with ID {}", section.getUuid());

            TestingStatuses status = section.getTestingStatus();
            if (section instanceof CompoundLogRecordContainer) {
                boolean isCompoundStatusUpdate = context.isParentStatusUpdate();

                TestingStatuses parentCompoundStatus = context.getSections().empty()
                        ? null : context.getSections().peek().getTestingStatus();
                context.setParentStatusUpdate(
                        Objects.nonNull(parentCompoundStatus) && !parentCompoundStatus.equals(status));
                if (isCompoundStatusUpdate || ((CompoundLogRecordContainer) section).isStep()) {
                    updateSectionStatus(status.name());
                    context = updateTestingStatus(section.getUuid().toString(), status.toString());
                }
                return context;
            }
            if (isCurrentSectionChanged) {
                return sendLogRecord(section, status.name());
            }
        } catch (Exception e) {
            log.error("Unable close section: {}", section, e);
        }
        return context;
    }

    void updateLogRecordDurationAndEndDate(LogRecord logRecord) {
        if (Objects.nonNull(logRecord.getStartDate())) {
            if (Objects.isNull(logRecord.getEndDate())) {
                logRecord.setEndDate(getCurrentTimestamp());
            }
            logRecord.setDuration(logRecord.getEndDate().getTime() - logRecord.getStartDate().getTime());
            log.debug("Update duration {} and end date {} for log record {}", logRecord.getDuration(),
                    logRecord.getEndDate(), logRecord.getUuid());
        } else {
            log.warn("Unable calculate duration and end date for log record {}", logRecord.getUuid());
        }
    }

    @Override
    public TestRunContext message(Message message) {
        try {
            if (Strings.isNullOrEmpty(message.getUuid()) || NULL_VALUE.equalsIgnoreCase(message.getUuid())) {
                message.setUuid(UUID.randomUUID().toString());
            }
            if (hasFile(message)) {
                uploadFirstFile(message);
            }

            LogRecord logRecord = createLogRecord(message, false, false);

            if (logRecord instanceof UiLogRecord) {
                log.debug("Configured log record (Ui Log Record) with ID {} message {}", logRecord.getUuid(),
                        logRecord.getMessage());
                updateSectionStatus(message.getTestingStatus());
                if (context.getAtpCompaund() != null) {
                    context.getAtpCompaund().setBrowserName(message.getBrowserName());
                }
                if (CollectionUtils.isNotEmpty(message.getBrowserLogs())) {
                    sendBrowserLogs(message.getBrowserLogs(), message.getUuid());
                }
                return sendUiLogRecord((UiLogRecord) logRecord);
            }

            log.debug("Configured log record (message) with ID {} message {}", logRecord.getUuid(),
                    logRecord.getMessage());
            return sendLogRecord(logRecord, message.getTestingStatus());
        } catch (Exception e) {
            log.error("Unable create message", e);
        }
        return context;
    }

    @Override
    public TestRunContext message(String sectionId, String name, String message, String status) {
        return message(sectionId, name,
                message, status,
                Collections.emptyMap(), null);
    }

    @Override
    public TestRunContext message(String sectionId, String name, String message, String status, Map attributes) {
        return message(sectionId, name, message, status, attributes, null);
    }

    @Override
    public TestRunContext message(String sectionId, String name,
                                  String message, String status,
                                  Map attributes, String type) {
        return message(sectionId, name, message, status, attributes, Collections.emptySet(), type);
    }

    @Override
    public TestRunContext message(String sectionId, String name, String message, String status, Map attributes,
                                  Set<String> configInfo, String type) {
        Message messageBean = setParamsForMessageBean(sectionId, name, message, status, attributes, configInfo, type,
                false, new Message());
        return message(messageBean);
    }

    @Override
    public TestRunContext message(String sectionId, String name, String message, String status, Map attributes,
                                  Set<String> configInfo, String type, Boolean hidden) {
        Message messageBean = setParamsForMessageBean(sectionId, name, message, status, attributes, configInfo, type,
                hidden, new Message());
        return message(messageBean);
    }

    private boolean hasFile(Message message) {
        return !CollectionUtils.isEmpty(message.getAttributes());
    }

    public static Message setParamsForMessageBean(String sectionId, String name, String message, String status,
                                                  Map attributes, Set<String> configInfo, String type, boolean hidden,
                                                  Message messageBean) {
        messageBean.setUuid(sectionId);
        messageBean.setName(name);
        messageBean.setMessage(message);
        messageBean.setTestingStatus(status);
        messageBean.setAttributes(Collections.singletonList(attributes));
        messageBean.setConfigInfoId(configInfo);
        messageBean.setType(type);
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setHidden(hidden);
        messageBean.setMetaInfo(metaInfo);
        return messageBean;
    }

    @Override
    public TestRunContext bvMessage(Message message) {
        try {
            if (Strings.isNullOrEmpty(message.getType())) {
                message.setType(TypeAction.BV.toString());
            }

            BvLogRecord logRecord = (BvLogRecord) createLogRecord(message, false, false);

            log.debug("Configure log record (BV message) with ID {}", logRecord.getUuid());
            updateSectionAndTestRunStatus(message.getTestingStatus());
            sendBvLogRecord(logRecord);
        } catch (Exception e) {
            log.error("Unable create log record (BV message) ", e);
        }
        return context;
    }

    @Override
    public TestRunContext restMessage(Message message, JSONObject validationTable) {
        try {
            if (Strings.isNullOrEmpty(message.getType())) {
                message.setType(TypeAction.REST.toString());
            }

            RestLogRecord logRecord = (RestLogRecord) createLogRecord(message, false, false);

            log.debug("Configure log record (rest message) with ID {}", logRecord.getUuid());
            if (Objects.nonNull(validationTable)) {
                try {
                    logRecord.setValidationTable(Utils.parseValidationTableFromJson(validationTable));
                } catch (IOException ex) {
                    log.error("Failed to parse validation table for LogRecord: {} from json: {}.",
                            logRecord.getUuid(), validationTable, ex);
                }
            }
            updateSectionAndTestRunStatus(message.getTestingStatus());
            sendRestLogRecord(logRecord);
        } catch (Exception e) {
            log.error("Unable create log record (rest message) ", e);
        }
        return context;
    }

    @Override
    public TestRunContext restMessage(Message message) {
        return restMessage(message, null);
    }

    @Override
    public TestRunContext sqlMessage(Message message) {
        try {
            if (Strings.isNullOrEmpty(message.getType())) {
                message.setType(TypeAction.SQL.toString());
            }

            SqlLogRecord logRecord = (SqlLogRecord) createLogRecord(message, false, false);

            log.debug("Configure log record (sql message) with ID {}", logRecord.getUuid());
            updateSectionAndTestRunStatus(message.getTestingStatus());
            sendSqlLogRecord(logRecord);
        } catch (Exception e) {
            log.error("Unable create log record (sql message)", e);
        }
        return context;
    }

    @Override
    public TestRunContext sshMessage(Message message) {
        try {
            if (Strings.isNullOrEmpty(message.getType())) {
                message.setType(TypeAction.SSH.toString());
            }

            SshLogRecord logRecord = (SshLogRecord) createLogRecord(message, false, false);

            log.debug("Configure log record (ssh message) with ID {}", logRecord.getUuid());
            updateSectionAndTestRunStatus(message.getTestingStatus());
            sendSshLogRecord(logRecord);
        } catch (Exception e) {
            log.error("Unable create log record (ssh message)", e);
        }
        return context;
    }

    public TestRunContext reportEnvironmentsInfo(EnvironmentsInfo incomingEnvironmentsInfo) {
        if (incomingEnvironmentsInfo.getExecutionRequestId() == null && context.getExecutionRequestId() != null) {
            incomingEnvironmentsInfo.setExecutionRequestId(UUID.fromString(context.getExecutionRequestId()));
        }
        if (incomingEnvironmentsInfo.getName() == null && context.getExecutionRequestName() != null) {
            incomingEnvironmentsInfo.setName(context.getExecutionRequestName());
        }
        return sendEnvironmentsInfo(incomingEnvironmentsInfo);
    }

    public abstract TestRunContext sendEnvironmentsInfo(EnvironmentsInfo incomingEnvironmentsInfo);

    public TestRunContext reportToolsInfo(ToolsInfo incomingToolsInfo) {
        sendToolsInfo(incomingToolsInfo);
        EnvironmentsInfo environmentsInfo = new EnvironmentsInfo();
        environmentsInfo.setUuid(context.getEnvironmentInfoId());
        environmentsInfo.setToolsInfoUuid(incomingToolsInfo.getUuid());
        return reportEnvironmentsInfo(environmentsInfo);
    }

    public abstract TestRunContext sendToolsInfo(ToolsInfo incomingToolsInfo);

    @Override
    public void updateMessageTestingStatusAndFiles(Message message) {
        message.setUuid(Strings.isNullOrEmpty(context.getCurrentSectionId())
                ? context.getAtpCompaund().getSectionId() : context.getCurrentSectionId());

        if (hasFile(message)) {
            uploadAllFiles(message);
        } else {
            log.debug("List of files is empty for Log Record {}", message.getUuid());
        }

        TestingStatuses status = TestingStatuses.findByValue(message.getTestingStatus());
        if (Strings.isNullOrEmpty(context.getCurrentSectionId()) || Objects.isNull(context.getCurrentSection())) {
            sendMessageStatusAndFiles(message.getUuid(), message.getMessage(), status, message.getFileMetadata());
        } else {
            LogRecord currentLogRecord = context.getCurrentSection();
            currentLogRecord.setMessage(message.getMessage());
            currentLogRecord.setTestingStatus(status);
            if (currentLogRecord.getFileMetadata() == null) {
                currentLogRecord.setFileMetadata(new LinkedList<>());
            }
            currentLogRecord.getFileMetadata().addAll(message.getFileMetadata());
        }
        updateSectionAndTestRunStatus(message.getTestingStatus());
    }

    public void uploadFileForLogRecord(String logRecordId, InputStream fileContent, String fileName) {
        try {
            String url = String.format(
                    uploadUrlTemplate,
                    logRecordId,
                    URLEncoder.encode(fileName, RamConstants.UTF8_CHARSET),
                    "text/html",
                    URLEncoder.encode(fileName, RamConstants.UTF8_CHARSET),
                    "");
            UploadScreenshotResponse response = requestUtils.postRequestStream(
                    url, fileContent, UploadScreenshotResponse.class);
            log.debug("File '{}' was uploaded for Log Record {}. Response {}", fileName, logRecordId, response);
        } catch (Exception exception) {
            String errorString = "Failed to upload file for LR";
            Message message = new Message();
            message.setUuid(logRecordId);
            createWarningLogRecord(exception, message, errorString);
        }
    }

    @SneakyThrows
    protected String getUploadFileUrl(Map<String, Object> attribute, Message message) {
        String fileName = (String) attribute.get(RamConstants.SCREENSHOT_NAME_KEY);
        String contentType = (String) attribute.get(RamConstants.SCREENSHOT_TYPE_KEY);
        String snapshotSource = (String) attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY);
        String snapshotExternalSource = (String) attribute.get(RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY);
        return String.format(
                uploadUrlTemplate,
                message.getUuid(),
                URLEncoder.encode(fileName, RamConstants.UTF8_CHARSET),
                contentType,
                URLEncoder.encode(StringUtils.defaultIfEmpty(snapshotSource, ""), RamConstants.UTF8_CHARSET),
                URLEncoder.encode(StringUtils.defaultIfEmpty(snapshotExternalSource, ""), RamConstants.UTF8_CHARSET));
    }

    /**
     * Upload one file to RAM.
     *
     * @param message Message with attributes.
     */
    protected void uploadFirstFile(Message message) {
        Map<String, Object> attributes = message.getAttributes().get(0);
        UploadScreenshotResponse response = uploadFile(attributes, message);

        String contentType = (String) attributes.get(RamConstants.SCREENSHOT_TYPE_KEY);
        if (Objects.isNull(contentType) || contentType.equals(RamConstants.CONTENT_TYPE)) {
            message.setType(TypeAction.UI.name());
            message.setScreenId(response.getFileId());
            message.setPreview(response.getPreview());
        }
    }

    /**
     * Upload all files to RAM.
     *
     * @param message Message with id and attributes.
     */
    protected void uploadAllFiles(Message message) {
        List<Map<String, Object>> attributes = message.getAttributes();
        attributes.forEach(attribute -> uploadFile(attribute, message));
    }

    protected UploadScreenshotResponse uploadFile(Map<String, Object> attribute, Message message) {
        String contentType = (String) attribute.get(RamConstants.SCREENSHOT_TYPE_KEY);
        String logRecordId = message.getUuid();
        log.debug("Try to upload a file with type {} for LogRecord: {}", contentType, logRecordId);
        UploadScreenshotResponse response = new UploadScreenshotResponse();

        if (attribute.containsKey(RamConstants.ATTACHMENT_STREAM_KEY)) {
            try {
                InputStream requestStream = (InputStream) attribute.get(RamConstants.ATTACHMENT_STREAM_KEY);
                response = requestUtils.postRequestStream(
                        getUploadFileUrl(attribute, message),
                        requestStream,
                        UploadScreenshotResponse.class);
            } catch (Exception exception) {
                message.setFileMetadata(new ArrayList<>());
                String errorString = "Failed to upload file for LR";
                createWarningLogRecord(exception, message, errorString);
            }
        } else {
            File file = (File) attribute.get(RamConstants.SCREENSHOT_FILE_KEY);
            if (file != null) {
                try (InputStream requestStream = Files.newInputStream(file.toPath())) {
                    response = requestUtils.postRequestStream(
                            getUploadFileUrl(attribute, message),
                            requestStream,
                            UploadScreenshotResponse.class);
                } catch (Exception exception) {
                    message.setFileMetadata(new ArrayList<>());
                    String errorString = "Failed to upload screenshot for LR";
                    createWarningLogRecord(exception, message, errorString);
                }
            } else {
                log.error("Can't find file taken from `{}` attribute field. Can't upload file.",
                        RamConstants.SCREENSHOT_FILE_KEY);
            }
        }
        if (Strings.isNullOrEmpty(response.getFileId())) {
            log.error("File id is NULL for Log Record: [{}]. Response {}", logRecordId, response);
        } else {
            log.debug("Uploaded file with id {} for LR {}", response.getFileId(), logRecordId);
        }
        return response;
    }

    private void createWarningLogRecord(Exception exception, Message parentMessage, String errorString) {
        String logRecordId = parentMessage.getUuid();
        log.error(errorString + " {}", logRecordId, exception);
        Message errorMessage = new Message();
        errorMessage.setParentRecordId(logRecordId);
        errorMessage.setName(errorString);
        errorMessage.setMessage(String.format(errorString + " %s and with error message: %s",
                logRecordId,
                exception.getMessage()));
        errorMessage.setTestingStatus(TestingStatuses.WARNING.name());
        TestingStatuses parentMessageStatus = TestingStatuses.findByValue(parentMessage.getTestingStatus());
        parentMessage.setTestingStatus(parentMessageStatus == null ? TestingStatuses.WARNING.name()
                : TestingStatuses.compareAndGetPriority(parentMessageStatus, TestingStatuses.WARNING).name());
        message(errorMessage);
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp endDate, long duration) {
        UpdateLogRecordExecutionStatusRequest request = new UpdateLogRecordExecutionStatusRequest(
                findByValue(executionStatus),
                null,
                null,
                endDate,
                duration
        );
        try {
            RequestUtils.putRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_EXECUTION_STATUS_PATH + "/",
                    OBJECT_MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string with execution status [{}], end date [{}] and duration [{}]",
                    executionStatus, endDate, duration, e);
        }
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate) {
        UpdateLogRecordExecutionStatusRequest request = new UpdateLogRecordExecutionStatusRequest(
                findByValue(executionStatus),
                null,
                startDate,
                null,
                0
        );
        try {
            RequestUtils.putRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_EXECUTION_STATUS_PATH,
                    OBJECT_MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string with execution status [{}] and start date [{}]",
                    executionStatus, startDate, e);
        }
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate, String name) {
        UpdateLogRecordExecutionStatusRequest request = new UpdateLogRecordExecutionStatusRequest(
                findByValue(executionStatus),
                CryptoTools.maskEncryptedData(name),
                startDate,
                null,
                0
        );
        try {
            RequestUtils.putRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_EXECUTION_STATUS_PATH,
                    OBJECT_MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string with execution status [{}] and start date [{}]",
                    executionStatus, startDate, e);
        }
    }

    @Override
    public void updateContextVariables(String logRecordId, List<ContextVariable> contextVariables) {
        encryptContextVariables(contextVariables);
        UpdateLogRecordContextVariablesRequest request =
                new UpdateLogRecordContextVariablesRequest(contextVariables);
        try {
            RequestUtils.postRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_CONTEXT_VARIABLES_PATH,
                    OBJECT_MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string with context variables [{}]",
                    contextVariables);
        }
    }

    @Override
    public void updateStepContextVariables(String logRecordId, List<ContextVariable> contextVariables) {
        encryptContextVariables(contextVariables);
        UpdateLogRecordContextVariablesRequest request =
                new UpdateLogRecordContextVariablesRequest(contextVariables);
        try {
            RequestUtils.postRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_STEP_CONTEXT_VARIABLES_PATH,
                    OBJECT_MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string with context variables [{}]",
                    contextVariables);
        }
    }

    protected void encryptContextVariables(List<ContextVariable> contextVariables) {
        if (contextVariables == null) {
            return;
        }
        contextVariables.forEach(contVar -> {
            contVar.setBeforeValue(CryptoTools.maskEncryptedData(contVar.getBeforeValue()));
            contVar.setAfterValue(CryptoTools.maskEncryptedData(contVar.getAfterValue()));
        });
    }

    public void updateMessageTestingStatusRequestAndResponse(String logRecordId, String message,
                                                             String testingStatus,
                                                             org.qubership.atp.ram.models.logrecords.parts.Request request,
                                                             Response response) {
        UpdateLogRecordFields body = new UpdateLogRecordFields(
                TestingStatuses.findByValue(testingStatus), message, null, request, response);
        try {
            RequestUtils.postRequest(atpRamUrl
                            + RamConstants.RAM_EXECUTOR_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_PATH,
                    OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string for Log Record {} with testing status [{}], message [{}], "
                    + "request [{}], response [{}]", logRecordId, testingStatus, message, request, response);
        }
    }

    private void setParamsForLogRecord(LogRecord logRecord, boolean isSection, boolean isStepFromAtpCompound) {
        if (logRecord.getUuid() == null) {
            logRecord.setUuid(UUID.randomUUID());
        }
        logRecord.setSection(isSection);
        if (!isStepFromAtpCompound && Objects.isNull(logRecord.getParentRecordId())) {
            if (!Strings.isNullOrEmpty(context.getCurrentSectionId())) {
                logRecord.setParentRecordId(UUID.fromString(context.getCurrentSectionId()));
            } else if (Objects.nonNull(context.getAtpCompaund())
                    && !Strings.isNullOrEmpty(context.getAtpCompaund().getSectionId())) {
                logRecord.setParentRecordId(UUID.fromString(context.getAtpCompaund().getSectionId()));
            }
        }
        log.debug("Set parent {} for logRecord with id {}", logRecord.getParentRecordId(), logRecord.getUuid());
        if (logRecord.getTestingStatus() == null) {
            logRecord.setTestingStatus(TestingStatuses.UNKNOWN);
        }
        Timestamp currentDate = getCurrentTimestamp();
        logRecord.setCreatedDate(currentDate);
        if (logRecord.getCreatedDateStamp() == 0) {
            logRecord.setCreatedDateStamp(System.nanoTime());
        }
        if (Objects.isNull(logRecord.getStartDate())
                && !TestingStatuses.NOT_STARTED.equals(logRecord.getTestingStatus())) {
            logRecord.setStartDate(currentDate);
        }
        if (!logRecord.isSection()) {
            updateLogRecordDurationAndEndDate(logRecord);
            logRecord.setExecutionStatus(FINISHED);
        } else if (Objects.isNull(logRecord.getExecutionStatus())) {
            logRecord.setExecutionStatus(TestingStatuses.NOT_STARTED.equals(logRecord.getTestingStatus())
                    ? ExecutionStatuses.NOT_STARTED : IN_PROGRESS);
        }
        logRecord.setTestRunId(UUID.fromString(context.getTestRunId()));
        logRecord.setServer(context.getQaHost());

        if (logRecord.getMessage() == null) {
            logRecord.setMessage("");
        }
    }

    /**
     * Send provided LR and update parent section and TR status according to provided status.
     */
    public TestRunContext sendLogRecord(LogRecord logRecordRequest, String status) {
        TestRunContext result = sendLogRecord(logRecordRequest);
        log.debug("Sending log record {}", logRecordRequest);
        updateSectionAndTestRunStatus(status);
        return result;
    }

    public abstract TestRunContext sendLogRecord(LogRecord logRecordRequest);

    public abstract TestRunContext sendUiLogRecord(UiLogRecord logRecordRequest);

    public abstract TestRunContext sendRestLogRecord(RestLogRecord logRecordRequest);

    public abstract TestRunContext sendSqlLogRecord(SqlLogRecord logRecordRequest);

    public abstract TestRunContext sendSshLogRecord(SshLogRecord logRecordRequest);

    public abstract TestRunContext sendMiaLogRecord(MiaLogRecord logRecordRequest);

    public abstract TestRunContext sendItfLogRecord(ItfLogRecord logRecordRequest);

    public abstract TestRunContext sendBvLogRecord(BvLogRecord logRecordRequest);

    public void sendBrowserLogs(List<BrowserConsoleLogsTable> browserLogs, String uuid) {
        try {
            RequestUtils.postRequest(atpRamUrl
                            + RamConstants.API_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + uuid
                            + RamConstants.CREATE_BROWSER_CONSOLE_LOG,
                    OBJECT_MAPPER.writeValueAsString(browserLogs));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string for logRecord '{}' with browserConsoleLogsTable [{}]",
                    uuid, browserLogs, e);
        }
    }

    public void sendScriptConsoleLogs(List<ScriptConsoleLog> consoleLogs,
                                      String preScript,
                                      String postScript,
                                      String uuid) {
        throw new AdapterMethodIsNotSupported("sendConsoleLogs");
    }

    /**
     * Send provided LR with provided status.
     */
    public abstract TestRunContext updateTestingStatus(String logRecordId, String status);

    /**
     * Updates provided LR in RAM with provided testing status and files metadata.
     */
    public void sendMessageStatusAndFiles(String logRecordId, String message, TestingStatuses status,
                                          List<FileMetadata> files) {
        UpdateLogRecordFields body = new UpdateLogRecordFields(status, message, files, null, null);
        try {
            RequestUtils.postRequest(atpRamUrl
                            + RamConstants.RAM_EXECUTOR_PATH
                            + RamConstants.LOG_RECORDS_PATH
                            + "/" + logRecordId
                            + RamConstants.UPDATE_PATH,
                    OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            log.error("Can not write request as string for Log Record {} with testing status [{}], message [{}], "
                    + "files [{}]", logRecordId, status, message, files);
        }
    }

    @Override
    public void sendRamReportImmediately(String executionRequestUuid) {
        log.debug("sendRamReportImmediately [executionRequestUuid={}]", executionRequestUuid);
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.put(RamConstants.EXECUTION_REQUEST_UUID_KEY, executionRequestUuid);
        params.put(RamConstants.RECIPIENTS_KEY, context.getMailList());
        String output;
        try {
            final Content postResult = RequestUtils.getHttpExecutor()
                    .execute(Request.Post(atpRamUrl + "/api/mail/send/er/report")
                            .bodyString(params.toString(), ContentType.APPLICATION_JSON)).returnContent();
            output = postResult.asString();
            if (log.isDebugEnabled()) {
                log.debug("Send report response: " + output);
            }
            final Content getResult = RequestUtils.getHttpExecutor()
                    .execute(Request.Get(atpRamUrl
                            + RamConstants.RAM_EXECUTOR_PATH
                            + RamConstants.EXECUTION_REQUESTS_PATH
                            + "/" + executionRequestUuid
                            + RamConstants.STOP_PATH)).returnContent();
            output = getResult.asString();
            if (log.isDebugEnabled()) {
                log.debug("Stop ER response: " + output);
            }
        } catch (Exception io) {
            log.error("Error due sending report: ", io);
        }
    }

    @Override
    public TestRunContext updateExecutionRequestStatus(ExecutionStatuses statuses, String erId) {
        log.debug("updateExecutionRequestStatus [statuses={}, erId={}]", statuses, erId);
        RequestUtils.putRequest(
                atpRamUrl
                        + RamConstants.API_PATH
                        + RamConstants.EXECUTION_REQUESTS_PATH
                        + "/" + erId
                        + RamConstants.UPD_EXECUTION_STATUS_PATH
                        + "/" + statuses.toString()
        );
        return this.context;
    }

    @Override
    public TestRunContext updateTestRunStatus(ExecutionStatuses statuses, String trId) {
        ObjectNode updateStatusRequest = RequestUtils.buildUpdateStatusRequest(
                statuses,
                RamConstants.TEST_RUN_ID_KEY,
                trId);
        String url = atpRamUrl
                + RamConstants.RAM_EXECUTOR_PATH
                + RamConstants.TEST_RUNS_PATH
                + RamConstants.UPDATE_OR_CREATE_PATH;
        RequestUtils.postRequest(url, updateStatusRequest.toString());
        return this.context;
    }

    protected void updateTestRunTestingStatus(TestingStatuses statuses, String trId) {
        String url = atpRamUrl
                + RamConstants.API_PATH
                + RamConstants.TEST_RUNS_PATH
                + "/" + trId
                + RamConstants.UPDATE_TESTING_STATUSES_PATH
                + "/" + statuses;
        RequestUtils.putRequest(url);
    }

    @SneakyThrows
    @Override
    public void upsertTestRunStatisticLabelReportParam(String trId,
                                                       String paramKey,
                                                       TestRunStatistic.ReportLabelParameterData paramData) {
        String url = atpRamUrl + RamConstants.API_PATH + RamConstants.TEST_RUNS_PATH + "/" + trId
                + RamConstants.STATISTIC_PATH + RamConstants.REPORT_LABELS_PARAMS_PATH + "/" + paramKey;

        ObjectNode bodyNode = OBJECT_MAPPER.createObjectNode();
        bodyNode.put(RamConstants.IS_PASSED_KEY, paramData.isPassed());

        String body = bodyNode.toString();

        RequestUtils.postRequest(url, body);
    }

    private void updateSectionStatus(String status) {
        LogRecord section = context.getCurrentSection();
        TestingStatuses recordStatus = TestingStatuses.findByValue(status);

        if (isNull(section)) {
            context.setTestingStatus(recordStatus);
            return;
        }

        log.debug("Changing log record [{}] status, current status is {} with id {}, new status is {} with id {}",
                section.getUuid(), section.getTestingStatus(), section.getTestingStatus().getId(), recordStatus,
                recordStatus.getId());

        if (section.getTestingStatus().getId() < recordStatus.getId()) {
            section.setTestingStatus(recordStatus);
            setCompaundStatus(context.getAtpCompaund(), recordStatus);
        }
    }

    private void updateSectionTestRunStatus(String status) {
        TestingStatuses recordStatus = TestingStatuses.findByValue(status);

        log.debug("Changing test run status, current status is {} with id {}, new status is {} with id {}",
                context.getTestingStatus(), context.getTestingStatus().getId(), recordStatus, recordStatus.getId());

        if (context.getTestingStatus().getId() < recordStatus.getId()) {
            context.setTestingStatus(recordStatus);
            try {
                log.debug("Setting test run with id {} to status {}", context.getTestRunId(),
                        context.getTestingStatus());
                updateTestRunTestingStatus(context.getTestingStatus(), context.getTestRunId());
            } catch (Exception e) {
                log.error("Cant set log record status", e);
            }
        }
    }

    private void updateSectionAndTestRunStatus(String status) {
        updateSectionStatus(status);
        updateSectionTestRunStatus(status);
    }

    private void setCompaundStatus(AtpCompaund compaund, TestingStatuses recordStatus) {
        if (Objects.nonNull(compaund)) {
            if (isNull(compaund.getTestingStatuses()) || compaund.getTestingStatuses().getId() < recordStatus.getId()) {
                log.debug("Setting compaund with id {} to status {}", compaund.getSectionId(),
                        recordStatus);
                compaund.setTestingStatuses(recordStatus);
            }
            setCompaundStatus(compaund.getParentSection(), recordStatus);
        }
    }


    @Override
    public void sendEmail(String executionRequestId, String recipients, String subject, String templateId) {
        log.info("sendRamReport [executionRequestUuid={}]", executionRequestId);
        String output;
        try {
            output = RequestUtils
                    .postRequest(getEmailSendUrl(executionRequestId),
                            createSendMailRequestBody(recipients, subject, templateId).toString())
                    .toString();
            if (log.isDebugEnabled()) {
                log.debug("Send report response: " + output);
            }
        } catch (Exception io) {
            log.error("Error due sending report: ", io);
        }
    }

    @Override
    public void createReporting(String executionRequestId, String projectId, String recipients, String subject) {
        RequestUtils.postRequest(getCreateReportingUrl(executionRequestId),
                createCreateReportingRequestBody(recipients, subject, executionRequestId, projectId).toString());
    }

    protected String getEmailSendUrl(String executionRequestId) {
        return atpRamUrl + "/api/email/" + executionRequestId;
    }

    protected String getCreateReportingUrl(String executionRequestId) {
        return atpRamUrl + "/api" + RamConstants.EXECUTION_REQUESTS_PATH + "/" + executionRequestId + "/emailReporting";
    }

    protected ObjectNode createCreateReportingRequestBody(String recipients, String subject,
                                                          String executionRequestId, String projectId) {
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        ArrayNode recipientsNode = OBJECT_MAPPER.createArrayNode();
        if (StringUtils.isNotBlank(recipients)) {
            Arrays.stream(recipients.split(",")).forEach(str -> recipientsNode.add(str));
        }
        params.set(RamConstants.RECIPIENTS_KEY, recipientsNode);
        params.put(RamConstants.SUBJECT_KEY, subject);
        params.put(RamConstants.EXECUTION_REQUEST_ID_KEY, executionRequestId);
        params.put(RamConstants.PROJECT_ID_KEY, projectId);
        return params;
    }

    protected ObjectNode createSendMailRequestBody(String recipients, String subject, String templateId) {
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.put(RamConstants.RECIPIENTS_KEY, recipients);
        params.put(RamConstants.TEMPLATE_ID_KEY, templateId);
        params.put(RamConstants.SUBJECT_KEY, subject);
        return params;
    }

    public abstract TestRunContext updateSsmMetricReports(String logRecordId, String problemContextMetricReportId,
                                                          String microservicesReportId);

    public void close() {
        if (this.context != null && this.context.getTestRunId() != null) {
            TestRunContextHolder.removeContext(this.context.getTestRunId());
        } else {
            log.warn("TestRunContext wasn't closed, because Test Run id is null for Execution Request id {}.",
                    this.context != null ? this.context.getExecutionRequestId() : "null");
            log.trace(String.valueOf(this.context));
        }
    };
}
