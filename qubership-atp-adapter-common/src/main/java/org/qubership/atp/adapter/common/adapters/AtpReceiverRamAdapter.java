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

import static org.qubership.atp.adapter.common.RamConstants.API_PATH;
import static org.qubership.atp.adapter.common.RamConstants.ATP_LOGGER_URL_KEY;
import static org.qubership.atp.adapter.common.RamConstants.ATP_RAM_URL_KEY;
import static org.qubership.atp.adapter.common.RamConstants.CATEGORY_KEY;
import static org.qubership.atp.adapter.common.RamConstants.CONFIG_INFO_ID_KEY;
import static org.qubership.atp.adapter.common.RamConstants.CREATE_PATH;
import static org.qubership.atp.adapter.common.RamConstants.DATA_KEY;
import static org.qubership.atp.adapter.common.RamConstants.FIND_OR_CREATE_PATH;
import static org.qubership.atp.adapter.common.RamConstants.FINISH_DATE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.IS_COMPAUND_KEY;
import static org.qubership.atp.adapter.common.RamConstants.IS_SECTION_KEY;
import static org.qubership.atp.adapter.common.RamConstants.LOG_RECORD_ID_KEY;
import static org.qubership.atp.adapter.common.RamConstants.MESSAGE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.NAME_KEY;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.adapter.common.RamConstants.PARENT_RECORD_ID_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SAVE_CONFIGS_PATH;
import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_FILE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_NAME_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_SOURCE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_TYPE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.SERVER;
import static org.qubership.atp.adapter.common.RamConstants.START_DATE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.TESTING_STATUS_KEY;
import static org.qubership.atp.adapter.common.RamConstants.TEST_RUN_ID_KEY;
import static org.qubership.atp.adapter.common.RamConstants.TOOL_CONFIG_INFO_PATH;
import static org.qubership.atp.adapter.common.RamConstants.TYPE_ACTION_KEY;
import static org.qubership.atp.adapter.common.RamConstants.V1_PATH;
import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.getCurrentTimestamp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.crypt.CryptoTools;
import org.qubership.atp.ram.enums.EngineCategory;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.MiaLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import lombok.SneakyThrows;

@Deprecated
public class AtpReceiverRamAdapter extends AbstractAdapter {

    private static final String EXECUTION_REQUEST_UPDATE_STATUS_PATH = "/er/updateStatus";
    private static final String TEST_RUN_UPDATE_STATUS_PATH = "/tr/updateOrCreate";

    private static final Logger log = LoggerFactory.getLogger(AtpReceiverRamAdapter.class);
    private static final Executor executor = Executors.newCachedThreadPool();

    private final String atpLoggerUrl;

    private String logRecordUrl;
    private String testRunUrl;

    public AtpReceiverRamAdapter(TestRunContext context) {
        this(context.getTestRunName());
        this.context = context;
    }

    public AtpReceiverRamAdapter() {
        this(ExecutionRequestHelper.generateRequestName());
    }

    /**
     * Init adapter.
     */
    public AtpReceiverRamAdapter(String testRunName) {
        super(testRunName);
        Config cfg = Config.getConfig();
        atpLoggerUrl = cfg.getProperty(ATP_LOGGER_URL_KEY, "http://localhost:8081");
        atpRamUrl = cfg.getProperty(ATP_RAM_URL_KEY, "http://localhost:8080");
        logRecordUrl = atpLoggerUrl + "/lr" + FIND_OR_CREATE_PATH;
        testRunUrl = atpLoggerUrl + "/tr" + CREATE_PATH;
        if (Strings.isNullOrEmpty(testRunName)) {
            log.warn("Cannot get context, testRunName is empty");
        } else {
            context = TestRunContextHolder.getContext(testRunName);
        }
    }

    @SneakyThrows
    @Override
    public TestRunContext startAtpRun(StartRunRequest request, TestRunContext initialContext) {
        this.context = initialContext;
        context.setMailList(request.getMailList());
        ObjectNode output = sendRequest(testRunUrl, OBJECT_MAPPER.writeValueAsString(request));
        context.setTestRunId(output.get("testRunId").asText());
        context.setExecutionRequestId(output.get("executionRequestId").asText());
        return context;
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles,
                                 org.qubership.atp.adapter.common.utils.EngineCategory category) {
        return sendConfigInfo(configFiles, category.getName());
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles, EngineCategory category) {
        return sendConfigInfo(configFiles, category.getName());
    }

    @Override
    public TestRunContext updateAtpRun(TestRunContext initialContext) {
        this.context = initialContext;
        String url = atpLoggerUrl + "/tr/updateOrCreate";
        ObjectNode testRunRequest = OBJECT_MAPPER.createObjectNode();
        testRunRequest.put("testRunId", this.context.getTestRunId());
        testRunRequest.put("urlToBrowserOrLogs", this.context.getUrlToBrowserOrLogs().toString());
        sendRequest(url, testRunRequest.toString());
        log.debug("TestRun with id: {} was updated with url to browser: {}",
                this.context.getTestRunId(),
                this.context.getUrlToBrowserOrLogs());
        return this.context;
    }

    @Override
    public TestRunContext stopAtpRun(String testRunId) {
        String url = atpLoggerUrl + "/tr/stop";
        ObjectNode testRunRequest = OBJECT_MAPPER.createObjectNode();
        testRunRequest.put("testRunId", testRunId);
        testRunRequest.put("urlToBrowserOrLogs", this.context.getUrlToBrowserOrLogs().toString());
        String output = sendRequest(url, testRunRequest.toString()).toString();
        log.debug("TestRun with id: " + testRunId + " was stopped with status: " + output);
        return null;
    }

    @Override
    public TestRunContext closeSection() {
        context.removeSection();
        return context;
    }

    @Override
    public TestRunContext message(String sectionId,
                                  String name, String message, String status, Map attributes, Set<String> configInfo,
                                  String type) {
        String uuid = UUID.randomUUID().toString();
        if (!attributes.isEmpty()) {
            upload(uuid, (String) attributes.get(SCREENSHOT_NAME_KEY), (File) attributes.get(SCREENSHOT_FILE_KEY),
                    (String) attributes.get(SCREENSHOT_TYPE_KEY), (String) attributes.get(SCREENSHOT_SOURCE_KEY),
                    (String) attributes.get(SCREENSHOT_EXTERNAL_SOURCE_KEY));
        }
        ObjectNode logRecordRequest = OBJECT_MAPPER.createObjectNode();
        logRecordRequest.put(TEST_RUN_ID_KEY, context.getTestRunId());
        logRecordRequest.put(LOG_RECORD_ID_KEY, uuid);
        logRecordRequest.put(PARENT_RECORD_ID_KEY, context.getCurrentSectionId());
        logRecordRequest.put(IS_SECTION_KEY, false);
        logRecordRequest.put(IS_COMPAUND_KEY, false);
        logRecordRequest.put(NAME_KEY, CryptoTools.maskEncryptedData(name));
        logRecordRequest.put(MESSAGE_KEY, message);
        logRecordRequest.put(TESTING_STATUS_KEY, status);
        logRecordRequest.put(START_DATE_KEY, context.getStartDate().toString());
        logRecordRequest.put(FINISH_DATE_KEY, getCurrentTimestamp().toString());
        logRecordRequest.put(CONFIG_INFO_ID_KEY, configInfo.toString());
        logRecordRequest.put(TYPE_ACTION_KEY, type);
        logRecordRequest.put(SERVER, context.getQaHost());
        if (!Strings.isNullOrEmpty(context.getAtpLogRecordId())) {
            ObjectNode atpSource = OBJECT_MAPPER.createObjectNode();
            atpSource.put("atpObjectId", context.getAtpLogRecordId());
            logRecordRequest.set("atpSource", atpSource);
        }
        sendRequest(logRecordUrl, logRecordRequest.toString());
        return context;
    }

    @Override
    public TestRunContext updateMessageAndTestingStatus(String logRecordId, String message, String testingStatus) {
        return null;
    }

    @Override
    public TestRunContext updateMessageWithIsGroup(String logRecordId, boolean isGroup) {
        return null;
    }

    @Override
    public TestRunContext sendEnvironmentsInfo(EnvironmentsInfo environmentsInfo) {
        return null;
    }

    @Override
    public TestRunContext sendToolsInfo(ToolsInfo toolsInfo) {
        return null;
    }

    @Override
    public TestRunContext reportToolsInfo(ToolsInfo toolsInfo) {
        return null;
    }

    @Override
    public TestRunContext sendLogRecord(LogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendUiLogRecord(UiLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendRestLogRecord(RestLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendSqlLogRecord(SqlLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendSshLogRecord(SshLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendMiaLogRecord(MiaLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendItfLogRecord(ItfLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public TestRunContext sendBvLogRecord(BvLogRecord logRecordRequest) {
        return null;
    }

    @Override
    public void sendBrowserLogs(List<BrowserConsoleLogsTable> logRecordRequest, String uuid) {}

    @Override
    public TestRunContext updateTestingStatus(String logRecordId, String status) {
        return null;
    }

    @Override
    public void sendRamReportImmediately(String executionRequestUuid) {
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.put("executionRequestUuid", executionRequestUuid);
        params.put("recipients", context.getMailList());
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
                    .execute(Request.Get(atpLoggerUrl + "/er/" + executionRequestUuid + "/stop")).returnContent();
            output = getResult.asString();
            if (log.isDebugEnabled()) {
                log.debug("Stop ER response: " + output);
            }
        } catch (Exception io) {
            log.error("Error due sending report: ", io);
        }
    }

    /**
     * Returns current TestRunContext.
     */
    @Override
    public TestRunContext getContext() {
        return context;
    }

    @Override
    public void setContext(TestRunContext context) {
        this.context = context;
    }

    @Override
    public TestRunContext updateExecutionRequestStatus(ExecutionStatuses statuses, String erId) {
        ObjectNode updateStatusRequest = RequestUtils.buildUpdateStatusRequest(
                statuses,
                RamConstants.ATP_EXECUTION_REQUEST_ID_KEY,
                erId);
        sendRequest(
                this.atpLoggerUrl + EXECUTION_REQUEST_UPDATE_STATUS_PATH,
                updateStatusRequest.toString());
        return this.context;
    }

    @Override
    public TestRunContext updateTestRunStatus(ExecutionStatuses statuses, String trId) {
        ObjectNode updateStatusRequest = RequestUtils.buildUpdateStatusRequest(
                statuses,
                TEST_RUN_ID_KEY,
                trId);
        sendRequest(
                atpLoggerUrl + TEST_RUN_UPDATE_STATUS_PATH,
                updateStatusRequest.toString());
        return this.context;
    }

    @Override
    public TestRunContext updateSsmMetricReports(String logRecordId, String problemContextMetricReportId, String microservicesReportId) {
        return null;
    }

    /**
     * Sending request to the specified url.
     */
    private ObjectNode sendRequest(String url, String request) {
        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        try {
            final Content postResult = RequestUtils.getHttpExecutor()
                    .execute(Request.Post(url)
                            .bodyString(request, ContentType.APPLICATION_JSON)).returnContent();
            String output = postResult.asString();
            if (log.isDebugEnabled()) {
                log.debug("LOGGER RESPONSE: " + output);
            }
            result = OBJECT_MAPPER.readValue(output, ObjectNode.class);
        } catch (JsonParseException jpe) {
            log.error("Cannot parse response from ATP Report Receiver", jpe);
        } catch (IOException io) {
            log.error("Error due sending request", io);
        }
        return result;
    }

    private void upload(String uuid, String fileName, File file, String contentType, String snapshotSource, String snapshotExternalSource) {
        if (!Objects.nonNull(file)) {
            return;
        }
        Runnable task = () -> {
            try (InputStream stream = new FileInputStream(file)) {
                final Content postResult = RequestUtils.getHttpExecutor()
                        .execute(Request.Post(atpLoggerUrl + "/lr/upload/" + uuid + "/stream?fileName="
                                + fileName + "&contentType=" + contentType + "&snapshotSource="
                                + URLEncoder.encode(StringUtils.defaultIfEmpty(snapshotSource, ""), "UTF-8")
                                + "&snapshotExternalSource=" + URLEncoder.encode(
                                StringUtils.defaultIfEmpty(snapshotExternalSource, ""), "UTF-8"))
                                .bodyStream(stream, ContentType.APPLICATION_JSON)).returnContent();
                String output = postResult.asString();
                log.debug("GridFS FileId: {} ", output);
            } catch (Exception io) {
                log.error("Error due upload attach", io);
            }
        };
        executor.execute(task);
    }

    private String sendConfigInfo(Map<String, String> configFiles, String category) {
        String url = atpLoggerUrl + API_PATH + V1_PATH + TOOL_CONFIG_INFO_PATH + SAVE_CONFIGS_PATH;
        ObjectNode request = OBJECT_MAPPER.createObjectNode();
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        configFiles.forEach((key, value) -> {
            ObjectNode configFile = OBJECT_MAPPER.createObjectNode();
            configFile.put(NAME_KEY, key);
            configFile.put(DATA_KEY, value);
            configFile.put(CATEGORY_KEY, category);
            array.add(configFile);
        });
        request.set(CONFIG_INFO_ID_KEY, array);
        String response = RequestUtils.postRequestAsString(url, request.toString());
        log.debug("Configuration files were saved. Response: {}", response);
        return response;
    }
}
