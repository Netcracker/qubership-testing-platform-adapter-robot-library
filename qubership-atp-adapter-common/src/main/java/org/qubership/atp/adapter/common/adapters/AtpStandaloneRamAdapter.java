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

import static org.qubership.atp.adapter.common.RamConstants.CATEGORY_KEY;
import static org.qubership.atp.adapter.common.RamConstants.CONFIG_INFO_ID_KEY;
import static org.qubership.atp.adapter.common.RamConstants.DATA_KEY;
import static org.qubership.atp.adapter.common.RamConstants.FIND_OR_CREATE_PATH;
import static org.qubership.atp.adapter.common.RamConstants.LOG_RECORDS_PATH;
import static org.qubership.atp.adapter.common.RamConstants.NAME_KEY;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.adapter.common.RamConstants.RAM_EXECUTOR_PATH;
import static org.qubership.atp.adapter.common.RamConstants.SAVE_CONFIGS_PATH;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.ram.enums.EngineCategory;
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
public class AtpStandaloneRamAdapter extends AbstractAdapter {

    public AtpStandaloneRamAdapter() {
        this(ExecutionRequestHelper.generateRequestName());
    }

    public AtpStandaloneRamAdapter(TestRunContext context) {
        this(context.getTestRunName());
        this.context = context;
    }

    /**
     * Init adapter.
     */
    public AtpStandaloneRamAdapter(String testRunName) {
        super(testRunName);
    }

    @SneakyThrows
    @Override
    public TestRunContext sendLogRecord(LogRecord logRecordRequest) {
        ObjectNode resp = RequestUtils.postRequest(
                atpRamUrl + RAM_EXECUTOR_PATH + LOG_RECORDS_PATH + FIND_OR_CREATE_PATH,
                new ObjectMapper().writeValueAsString(logRecordRequest));
        context.setLogRecordUuid(resp.get("logRecordId").asText());
        return context;
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
    public TestRunContext updateSsmMetricReports(String logRecordId,
                                                 String problemContextMetricReportId,
                                                 String microservicesReportId) {
        return null;
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
    public TestRunContext updateMessageAndTestingStatus(String logRecordId, String message, String testingStatus) {
        return null;
    }

    @Override
    public TestRunContext updateMessageWithIsGroup(String logRecordId, boolean isGroup) {
        return null;
    }

    @Override
    public TestRunContext sendEnvironmentsInfo(EnvironmentsInfo incomingEnvironmentsInfo) {
        return null;
    }

    @Override
    public TestRunContext sendToolsInfo(ToolsInfo incomingToolsInfo) {
        return null;
    }

    private String sendConfigInfo(Map<String, String> configFiles, String category) {
        String url = atpRamUrl + RAM_EXECUTOR_PATH + LOG_RECORDS_PATH + SAVE_CONFIGS_PATH;
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
        return RequestUtils.postRequestAsString(url, request.toString());
    }
}
