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
import static org.qubership.atp.adapter.common.RamConstants.DATA_KEY;
import static org.qubership.atp.adapter.common.RamConstants.NAME_KEY;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;
import static org.qubership.atp.adapter.common.RamConstants.UUID_KEY;
import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.getCurrentTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.kafka.client.KafkaConfigurator;
import org.qubership.atp.adapter.common.kafka.pool.KafkaPoolManagementService;
import org.qubership.atp.adapter.common.kafka.pool.ProducerType;
import org.qubership.atp.adapter.common.protos.KafkaEnvironmentsInfo;
import org.qubership.atp.adapter.common.protos.KafkaLogRecord;
import org.qubership.atp.adapter.common.protos.KafkaLogRecordMessageParameter;
import org.qubership.atp.adapter.common.protos.KafkaLogRecordScriptReport;
import org.qubership.atp.adapter.common.utils.ActionParametersTrimmer;
import org.qubership.atp.adapter.common.utils.BuildKafkaParamsUtils;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.crypt.CryptoTools;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.enums.EngineCategory;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.CustomLink;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.models.StepLinkMetaInfo;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.logrecords.BvLogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.MiaLogRecord;
import org.qubership.atp.ram.models.logrecords.RestLogRecord;
import org.qubership.atp.ram.models.logrecords.SqlLogRecord;
import org.qubership.atp.ram.models.logrecords.SshLogRecord;
import org.qubership.atp.ram.models.logrecords.UiLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AtpKafkaRamAdapter extends AbstractAdapter {

    private final String topicName;
    private final String lrContextTopicName;
    private final String lrStepContextTopicName;
    private final String lrMessageParametersTopicName;
    private final String envInfoTopicName;
    private final String toolsInfoTopicName;
    private final String browserLogsTopicName;
    private final String consoleLogsTopicName;
    private final String topicNameConfigFiles;
    private final KafkaPoolManagementService kafkaPoolManagementService;
    protected ActionParametersTrimmer actionParametersTrimmer;

    public AtpKafkaRamAdapter() {
        this(ExecutionRequestHelper.generateRequestName());
    }

    public AtpKafkaRamAdapter(TestRunContext context) {
        this(context.getTestRunName());
        this.context = context;
    }

    /**
     * Init adapter.
     */
    public AtpKafkaRamAdapter(String testRunName) {
        super(testRunName);
        Config cfg = Config.getConfig();
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                cfg.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, RamConstants.DEFAULT_BOOTSTRAP_SERVERS));
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                cfg.getIntProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, RamConstants.DEFAULT_MAX_REQUEST_SIZE));
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                cfg.getProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, RamConstants.COMPRESSION_TYPE));

        topicName = cfg.getNotEmptyStringProperty(RamConstants.MESSAGE_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_MESSAGE_TOPIC_NAME);
        int lrTopicPartitionsNumber = Integer.parseInt(cfg.getProperty(RamConstants.LR_TOPIC_PARTITIONS_NUMBER,
                RamConstants.DEFAULT_LR_TOPIC_PARTITIONS_NUMBER));
        short lrTopicReplicationFactor = Short.parseShort(cfg.getProperty(RamConstants.LR_TOPIC_REPLICATION_FACTOR,
                RamConstants.DEFAULT_LR_TOPIC_REPLICATION_FACTOR));
        envInfoTopicName = cfg.getProperty(RamConstants.KAFKA_ENV_INFO_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_ENV_INFO_TOPIC_NAME);
        toolsInfoTopicName = cfg.getProperty(RamConstants.KAFKA_TOOLS_INFO_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_TOOLS_INFO_TOPIC_NAME);
        topicNameConfigFiles = cfg.getProperty(RamConstants.KAFKA_CONFIG_FILES_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_CONFIG_FILES_TOPIC_NAME);
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, lrTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(topicName, lrTopicPartitionsNumber);

        browserLogsTopicName = cfg.getNotEmptyStringProperty(RamConstants.BROWSER_LOG_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_BROWSER_LOG_TOPIC_NAME);
        consoleLogsTopicName = cfg.getNotEmptyStringProperty(RamConstants.CONSOLE_LOG_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_CONSOLE_LOG_TOPIC_NAME);
        lrContextTopicName = cfg.getNotEmptyStringProperty(RamConstants.LR_CONTEXT_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_LR_CONTEXT_TOPIC_NAME);
        lrStepContextTopicName = cfg.getNotEmptyStringProperty(RamConstants.LR_STEP_CONTEXT_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_LR_STEP_CONTEXT_TOPIC_NAME);
        lrMessageParametersTopicName = cfg.getNotEmptyStringProperty(RamConstants.LR_MESSAGE_PARAMETERS_TOPIC_NAME_KEY,
                RamConstants.DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_NAME);
        configureLrContextTopic(cfg, props);
        configureLrStepContextTopic(cfg, props);
        configureLrMessageParametersTopic(cfg, props);
        configureBrowserLogTopic(cfg, props);
        configureConsoleLogTopic(cfg, props);

        kafkaPoolManagementService = KafkaPoolManagementService.getInstance();

        int parameterValueSizeLimit = cfg.getIntProperty(RamConstants.ACTION_PARAMETER_VALUE_SIZE_LIMIT_TO_TRIM_CHARS,
                RamConstants.DEFAULT_ACTION_PARAMETER_VALUE_SIZE_LIMIT_TO_TRIM_CHARS);
        actionParametersTrimmer = new ActionParametersTrimmer(parameterValueSizeLimit);
    }

    protected void configureLrContextTopic(Config cfg, Properties props) {
        int lrContextTopicPartitionsNumber =
                Integer.parseInt(cfg.getNotEmptyStringProperty(RamConstants.LR_CONTEXT_TOPIC_PARTITIONS_NUMBER,
                        RamConstants.DEFAULT_LR_CONTEXT_TOPIC_PARTITIONS_NUMBER));
        short lrContextTopicReplicationFactor =
                Short.parseShort(cfg.getNotEmptyStringProperty(RamConstants.LR_CONTEXT_TOPIC_REPLICATION_FACTOR,
                        RamConstants.DEFAULT_LR_CONTEXT_TOPIC_REPLICATION_FACTOR));
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, lrContextTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(lrContextTopicName, lrContextTopicPartitionsNumber);
    }

    protected void configureLrStepContextTopic(Config cfg, Properties props) {
        int lrContextTopicPartitionsNumber =
                Integer.parseInt(cfg.getNotEmptyStringProperty(RamConstants.LR_STEP_CONTEXT_TOPIC_PARTITIONS_NUMBER,
                        RamConstants.DEFAULT_LR_STEP_CONTEXT_TOPIC_PARTITIONS_NUMBER));
        short lrContextTopicReplicationFactor =
                Short.parseShort(cfg.getNotEmptyStringProperty(RamConstants.LR_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR,
                        RamConstants.DEFAULT_LR_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR));
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, lrContextTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(lrStepContextTopicName, lrContextTopicPartitionsNumber);
    }

    protected void configureLrMessageParametersTopic(Config cfg, Properties props) {
        int lrMessageParametersTopicPartitionsNumber =
                Integer.parseInt(cfg.getNotEmptyStringProperty(
                        RamConstants.LR_MESSAGE_PARAMETERS_TOPIC_PARTITIONS_NUMBER,
                        RamConstants.DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_PARTITIONS_NUMBER));
        short lrMessageParametersTopicReplicationFactor =
                Short.parseShort(cfg.getNotEmptyStringProperty(
                        RamConstants.LR_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR,
                        RamConstants.DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR));
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, lrMessageParametersTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(lrMessageParametersTopicName, lrMessageParametersTopicPartitionsNumber);
    }

    protected void configureBrowserLogTopic(Config cfg, Properties props) {
        int browserLogTopicPartitionsNumber =
                Integer.parseInt(cfg.getNotEmptyStringProperty(RamConstants.BROWSER_LOG_TOPIC_PARTITION_NUMBER,
                        RamConstants.DEFAULT_BROWSER_LOG_TOPIC_PARTITIONS_NUMBER));
        short browserLogTopicReplicationFactor =
                Short.parseShort((cfg.getNotEmptyStringProperty(RamConstants.BROWSER_LOG_TOPIC_REPLICATION_FACTOR,
                        RamConstants.DEFAULT_BROWSER_LOG_TOPIC_REPLICATION_FACTOR)));
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, browserLogTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(browserLogsTopicName, browserLogTopicPartitionsNumber);
    }

    protected void configureConsoleLogTopic(Config cfg, Properties props) {
        int consoleLogTopicPartitionsNumber =
                Integer.parseInt(cfg.getNotEmptyStringProperty(RamConstants.CONSOLE_LOG_TOPIC_PARTITION_NUMBER,
                        RamConstants.DEFAULT_CONSOLE_LOG_TOPIC_PARTITIONS_NUMBER));
        short consoleLogTopicReplicationFactor =
                Short.parseShort((cfg.getNotEmptyStringProperty(RamConstants.CONSOLE_LOG_TOPIC_REPLICATION_FACTOR,
                        RamConstants.DEFAULT_CONSOLE_LOG_TOPIC_REPLICATION_FACTOR)));
        KafkaConfigurator kafkaConfigurator = new KafkaConfigurator(props, consoleLogTopicReplicationFactor);
        kafkaConfigurator.createOrUpdate(consoleLogsTopicName, consoleLogTopicPartitionsNumber);
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles,
                                 org.qubership.atp.adapter.common.utils.EngineCategory category) {
        return sendConfigFiles(configFiles, category.getName());
    }

    @Override
    public String saveConfigInfo(Map<String, String> configFiles, EngineCategory category) {
        return sendConfigFiles(configFiles, category.getName());
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp endDate, long duration) {
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .setLastUpdated(getCurrentTimestamp().getTime())
                .setExecutionStatus(executionStatus);
        if (Objects.nonNull(endDate)) {
            logRecord.setEndDate(endDate.getTime());
            logRecord.setDuration(duration);
        }
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecordId,
                logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Updating logrecord {} execution status. New status is {}, end date is {}, duration is {}.",
                logRecordId, executionStatus, endDate, duration);
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate) {
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .setLastUpdated(getCurrentTimestamp().getTime())
                .setExecutionStatus(executionStatus);
        if (Objects.nonNull(startDate)) {
            logRecord.setStartDate(startDate.getTime());
        }
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Updating logrecord {} execution status. New status is {} and start date {}.", logRecordId,
                executionStatus, startDate);
    }

    @Override
    public void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate, String name) {
        String maskedName = actionParametersTrimmer.trimActionParametersByLimit(CryptoTools.maskEncryptedData(name));
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .setName(maskedName)
                .setLastUpdated(getCurrentTimestamp().getTime())
                .setExecutionStatus(executionStatus);
        if (Objects.nonNull(startDate)) {
            logRecord.setStartDate(startDate.getTime());
        }
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Updating logrecord {} with name [{}] execution status. New status is {} and start date {}.",
                logRecordId, maskedName, executionStatus, startDate);
    }

    @Override
    public void updateContextVariables(String logRecordId, List<ContextVariable> contextVariables) {
        if (Objects.isNull(contextVariables) || contextVariables.isEmpty()) {
            log.warn("Context variables is empty for Log Record {}. Do not update log record.", logRecordId);
            return;
        }
        encryptContextVariables(contextVariables);
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .addAllContextVariables(BuildKafkaParamsUtils.buildContextVariables(contextVariables));

        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(lrContextTopicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Updating Log Record {} context variables. New variables {}.", logRecordId,
                contextVariables);
    }

    @Override
    public void sendBrowserLogs(List<BrowserConsoleLogsTable> browserLogs, String uuid) {
        log.debug("start sendBrowserLogs(browserLogs: {}) with uuid [{}].", browserLogs, uuid);

        if (Objects.isNull(browserLogs)) {
            log.warn("Browser not contain any logs with specified level");
            return;
        }
        List<KafkaLogRecord.BrowserConsoleLogsTable> logsTable = new ArrayList<>();
        browserLogs.forEach(log -> {
                    if (Objects.isNull(log.getFileName())) {
                        log.setFileName("");
                    }
                    KafkaLogRecord.BrowserConsoleLogsTable.Builder builder = KafkaLogRecord.BrowserConsoleLogsTable.newBuilder()
                            .setMessage(log.getMessage())
                            .setLevel(log.getLevel())
                            .setTimestamp(log.getTimestamp().getTime())
                            .setFileName(log.getFileName());
                    logsTable.add(builder.build());
                }
        );

        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(uuid)
                .addAllBrowserConsoleLogTable(logsTable);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(browserLogsTopicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        log.debug("end sendLogRecord()");
    }

    @Override
    public void sendScriptConsoleLogs(List<ScriptConsoleLog> scriptConsoleLogs,
                                      String preScript,
                                      String postScript,
                                      String logRecordId) {
        log.debug("start sendScriptConsoleLogs(consoleLogs: {}) with uuid [{}].", scriptConsoleLogs, logRecordId);

        if (Objects.isNull(scriptConsoleLogs)) {
            log.warn("Script execution not contain any logs with specified level");
            return;
        }
        List<KafkaLogRecordScriptReport.ScriptConsoleLog> logsTable = new ArrayList<>();
        scriptConsoleLogs.forEach(log -> {
            KafkaLogRecordScriptReport.ScriptConsoleLog.Builder builder = KafkaLogRecordScriptReport.ScriptConsoleLog.newBuilder()
                            .setMessage(log.getMessage())
                            .setLevel(log.getLevel())
                            .setTimestamp(log.getTimestamp());
                    logsTable.add(builder.build());
                }
        );
        KafkaLogRecordScriptReport.ScriptConsoleReport.Builder scriptConsoleReportBuilder = KafkaLogRecordScriptReport.ScriptConsoleReport.newBuilder();
        scriptConsoleReportBuilder.addAllScriptConsoleLogs(logsTable);
        scriptConsoleReportBuilder.setPreScript(preScript);
        scriptConsoleReportBuilder.setPostScript(postScript);
        scriptConsoleReportBuilder.setLogRecordId(logRecordId);
        KafkaLogRecordScriptReport.ScriptConsoleReport scriptConsoleReport = scriptConsoleReportBuilder.build();
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(consoleLogsTopicName, logRecordId,
                        scriptConsoleReport),
                ProducerType.PROTOBUF);
        log.debug("end sendLogRecord()");
    }



    @Override
    public void updateStepContextVariables(String logRecordId, List<ContextVariable> contextVariables) {
        if (Objects.isNull(contextVariables) || contextVariables.isEmpty()) {
            log.warn("Step context variables is empty for Log Record {}. Do not update log record.", logRecordId);
            return;
        }
        encryptContextVariables(contextVariables);
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .addAllStepContextVariables(BuildKafkaParamsUtils.buildContextVariables(contextVariables));

        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(lrStepContextTopicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Updating logrecord {} step context variables. New variables {}.", logRecordId,
                contextVariables);
    }

    /**
     * Sends message parameters to kafka topic.
     * @param logRecordId logRecordId
     * @param messageParameters message parameters list
     * @param createdDate created date
     */
    private void updateMessageParameters(String logRecordId, List<MessageParameter> messageParameters,
                                        Timestamp createdDate) {
        if (Objects.isNull(messageParameters) || messageParameters.isEmpty()) {
            log.warn("Message parameters is empty for Log Record {}. Do not update log record.", logRecordId);
            return;
        }

        KafkaLogRecordMessageParameter.LogRecordMessageParameter.Builder logRecordMessageParameter =
                KafkaLogRecordMessageParameter.LogRecordMessageParameter.newBuilder()
                .setId(logRecordId)
                .setCreatedDate(createdDate.getTime())
                .addAllMessageParameters(BuildKafkaParamsUtils.buildMessageParameters(messageParameters));

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(
                lrMessageParametersTopicName, logRecordId, logRecordMessageParameter.build()), ProducerType.PROTOBUF);
        log.debug("Updating logrecord {} message parameters. New variables {}.", logRecordId, messageParameters);
    }

    protected void sendContextVariablesAndMessageParametersIfExists(LogRecord logRecordRequest) {
        List<ContextVariable> contextVariables = logRecordRequest.getContextVariables();
        if (Objects.nonNull(contextVariables) && !contextVariables.isEmpty()) {
            updateContextVariables(logRecordRequest.getUuid().toString(), contextVariables);
        }

        List<ContextVariable> stepContextVariables = logRecordRequest.getStepContextVariables();
        if (Objects.nonNull(stepContextVariables) && !stepContextVariables.isEmpty()) {
            updateStepContextVariables(logRecordRequest.getUuid().toString(), stepContextVariables);
        }

        List<MessageParameter> messageParameters = logRecordRequest.getMessageParameters();
        if (Objects.nonNull(messageParameters) && !messageParameters.isEmpty()) {
            updateMessageParameters(logRecordRequest.getUuid().toString(), messageParameters,
                    logRecordRequest.getCreatedDate());
        }
    }

    @Override
    public void updateMessageTestingStatusRequestAndResponse(String logRecordId, String message,
                                                             String testingStatus, Request request,
                                                             Response response) {
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .setLastUpdated(getCurrentTimestamp().getTime())
                .setTestingStatus(testingStatus)
                .setMessage(message);
        BuildKafkaParamsUtils.buildRequest(request, logRecord);
        BuildKafkaParamsUtils.buildResponse(response, logRecord);

        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("Update message [{}], testing status [{}], request and response for section [{}]", message,
                testingStatus, logRecordId);
    }

    @Override
    public TestRunContext updateMessageAndTestingStatus(String logRecordId, String message, String testingStatus) {
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, KafkaLogRecord.LogRecord.newBuilder()
                        .setUuid(logRecordId)
                        .setLastUpdated(getCurrentTimestamp().getTime())
                        .setTestingStatus(testingStatus)
                        .setMessage(message)
                        .build()), ProducerType.PROTOBUF);
        log.debug("Update message [{}] and testing status [{}] for section [{}]", message, testingStatus, logRecordId);
        return context;
    }

    @Override
    public TestRunContext updateMessageWithIsGroup(String logRecordId, boolean isGroup) {
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, KafkaLogRecord.LogRecord.newBuilder()
                        .setUuid(logRecordId)
                        .setLastUpdated(getCurrentTimestamp().getTime())
                        .setIsGroup(isGroup)
                        .build()), ProducerType.PROTOBUF);
        log.debug("Update isGroup [{}] for section [{}]", isGroup, logRecordId);
        return context;
    }

    @Override
    public TestRunContext updateTestingStatus(String logRecordId, String status) {
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, KafkaLogRecord.LogRecord.newBuilder()
                        .setUuid(logRecordId)
                        .setLastUpdated(getCurrentTimestamp().getTime())
                        .setTestingStatus(status)
                        .build()), ProducerType.PROTOBUF);
        log.debug("Update and testing status [{}] for section [{}]", status, logRecordId);
        return context;
    }

    /**
     * Update SSM Metric reports.
     */
    @Override
    public TestRunContext updateSsmMetricReports(String logRecordId, String problemContextMetricReportId,
                                                 String microservicesReportId) {
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, KafkaLogRecord.LogRecord.newBuilder()
                        .setUuid(logRecordId)
                        .setSsmMetricReports(KafkaLogRecord.SsmMetricReports.newBuilder()
                                .setProblemContextReportId(problemContextMetricReportId)
                                .setMicroservicesReportId(microservicesReportId).build())
                        .build()), ProducerType.PROTOBUF);
        log.debug("Update and ssm metric reports [{}, {}] for section [{}]", problemContextMetricReportId,
                microservicesReportId, logRecordId);
        return context;
    }

    @Override
    public void sendMessageStatusAndFiles(String logRecordId, String message, TestingStatuses status,
                                          List<FileMetadata> files) {
        KafkaLogRecord.LogRecord.Builder logRecord = KafkaLogRecord.LogRecord.newBuilder()
                .setUuid(logRecordId)
                .setLastUpdated(getCurrentTimestamp().getTime())
                .setMessage(message)
                .setTestingStatus(status.toString());
        if (!CollectionUtils.isEmpty(files)) {
            logRecord.addAllFileMetadata(
                    files.stream().map(metadata ->
                            KafkaLogRecord.FileMetadata.newBuilder()
                                    .setFileType(metadata.getType().toString())
                                    .setFileName(metadata.getFileName())
                                    .build()).collect(Collectors.toList())
            );
            log.debug("Update message [{}], status [{}] and file metadata [{}] for section [{}]", message, status,
                    files, logRecordId);
        } else {
            log.debug("Update message [{}] and status [{}] for section [{}]", message, status, logRecordId);
        }
        kafkaPoolManagementService.sendProducerRecord(
                new ProducerRecord(topicName, logRecordId, logRecord.build()), ProducerType.PROTOBUF);
        log.debug("end sendLogRecord()");
    }

    @Override
    public TestRunContext sendLogRecord(LogRecord logRecordRequest) {
        log.debug("start sendLogRecord(logRecordRequest: {})", logRecordRequest);
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);
        log.debug("end sendLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendUiLogRecord(UiLogRecord logRecordRequest) {
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.checkAndFillProperty(UiLogRecord::getScreenId,
                KafkaLogRecord.LogRecord.Builder::setScreenId,
                in -> in, logRecordRequest, logRecord);
        BuildKafkaParamsUtils.checkAndFillProperty(UiLogRecord::getPreview,
                KafkaLogRecord.LogRecord.Builder::setPreview,
                in -> in, logRecordRequest, logRecord);
        BuildKafkaParamsUtils.checkAndFillProperty(UiLogRecord::getBrowserName,
                KafkaLogRecord.LogRecord.Builder::setBrowserName,
                in -> in, logRecordRequest, logRecord);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);
        return context;
    }

    @Override
    public TestRunContext sendRestLogRecord(RestLogRecord logRecordRequest) {
        log.debug("start sendRestLogRecord(logRecordRequest: {})", logRecordRequest);
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.buildRequest(logRecordRequest.getRequest(), logRecord);
        BuildKafkaParamsUtils.buildResponse(logRecordRequest.getResponse(), logRecord);
        logRecord.setIsPreScriptPresent(logRecordRequest.isPreScriptPresent());
        logRecord.setIsPostScriptPresent(logRecordRequest.isPostScriptPresent());
        BuildKafkaParamsUtils.checkAndFillProperty(RestLogRecord::getProtocolType,
                KafkaLogRecord.LogRecord.Builder::setProtocolType, in -> in,
                logRecordRequest, logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);
        log.debug("end sendRestLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendEnvironmentsInfo(EnvironmentsInfo incomingEnvironmentsInfo) {
        KafkaEnvironmentsInfo.EnvironmentsInfo environmentsInfo =
                BuildKafkaParamsUtils.createKafkaEnvironmentsInfo(incomingEnvironmentsInfo);
        log.debug("Sending environment info {} to Kafka topic {}.", incomingEnvironmentsInfo, envInfoTopicName);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(envInfoTopicName, environmentsInfo),
                ProducerType.PROTOBUF);
        return context;
    }

    @Override
    public TestRunContext sendToolsInfo(ToolsInfo incomingToolsInfo) {
        if (incomingToolsInfo.getUuid() == null) {
            incomingToolsInfo.setUuid(UUID.randomUUID());
        }
        KafkaEnvironmentsInfo.ToolsInfo toolsInfo = BuildKafkaParamsUtils.createToolsInfo(incomingToolsInfo);
        log.debug("Sending tools info {}  with environments info id {} to Kafka topic {}.", toolsInfo,
                context.getEnvironmentInfoId(), toolsInfoTopicName);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(toolsInfoTopicName, toolsInfo),
                ProducerType.PROTOBUF);
        return context;
    }

    private void sendEnvironmentsInfoToKafka(EnvironmentsInfo environmentsInfo) {
        try {
            kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(envInfoTopicName,
                    OBJECT_MAPPER.writeValueAsString(environmentsInfo)), ProducerType.PROTOBUF);
        } catch (JsonProcessingException e) {
            log.error("Environments info with id {} can't be serialized.", environmentsInfo.getUuid());
        }
    }

    @Override
    public TestRunContext sendSqlLogRecord(SqlLogRecord logRecordRequest) {
        log.debug("start sendSqlLogRecord(logRecordRequest: {})", logRecordRequest);
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        logRecord.setCommand(logRecordRequest.getCommand());

        if (MapUtils.isNotEmpty(logRecordRequest.getResult())) {
            List<KafkaLogRecord.SqlResult> sqlResults = new ArrayList<>();
            logRecordRequest.getResult().forEach((key, value) -> {
                KafkaLogRecord.SqlResult.Builder builder = KafkaLogRecord.SqlResult.newBuilder();
                builder.setKey(key);
                if (CollectionUtils.isNotEmpty(value)) {
                    builder.addAllValue(value);
                }
                sqlResults.add(builder.build());
            });
            logRecord.addAllResult(sqlResults);
        }
        BuildKafkaParamsUtils.setConnectionInfoForLogRecord(logRecordRequest.getConnectionInfo(), logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);

        log.debug("end sendSqlLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendSshLogRecord(SshLogRecord logRecordRequest) {
        log.debug("start sendSshLogRecord(logRecordRequest: {})", logRecordRequest);

        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.checkAndFillProperty(SshLogRecord::getCommand,
                KafkaLogRecord.LogRecord.Builder::setCommand,
                in -> in, logRecordRequest, logRecord);
        BuildKafkaParamsUtils.checkAndFillProperty(SshLogRecord::getOutput,
                KafkaLogRecord.LogRecord.Builder::setOutput,
                in -> in, logRecordRequest, logRecord);
        BuildKafkaParamsUtils.setConnectionInfoForLogRecord(logRecordRequest.getConnectionInfo(), logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);

        log.debug("end sendSshLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendMiaLogRecord(MiaLogRecord logRecordRequest) {
        log.debug("start sendMiaLogRecord(logRecordRequest: {})", logRecordRequest);

        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.buildRequest(logRecordRequest.getRequest(), logRecord);
        BuildKafkaParamsUtils.buildResponse(logRecordRequest.getResponse(), logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord<>(topicName,
                        logRecord.getUuid(), logRecord.build()),
                ProducerType.PROTOBUF);
        if (Objects.nonNull(logRecordRequest.getIsGroup()) && logRecordRequest.getIsGroup()) {
            log.debug("Log Record [{}] is group.", logRecordRequest.getUuid());
            kafkaPoolManagementService.sendProducerRecord(new ProducerRecord<>(topicName,
                            logRecord.getUuid(), KafkaLogRecord.LogRecord.newBuilder()
                            .setUuid(logRecordRequest.getUuid().toString())
                            .setIsGroup(logRecordRequest.getIsGroup())
                            .setLastUpdated(logRecord.getCreatedDate())
                            .build()),
                    ProducerType.PROTOBUF);
        }
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);

        log.debug("end sendMiaLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendItfLogRecord(ItfLogRecord logRecordRequest) {
        log.debug("start sendItfLogRecord(logRecordRequest: {})", logRecordRequest);
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.checkAndFillProperty(ItfLogRecord::getLinkToTool, KafkaLogRecord.LogRecord.Builder::setLinkToTool, in -> in,
                logRecordRequest, logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);

        log.debug("end sendItfLogRecord()");
        return context;
    }

    @Override
    public TestRunContext sendBvLogRecord(BvLogRecord logRecordRequest) {
        log.debug("start sendBvLogRecord(logRecordRequest: {})", logRecordRequest);
        KafkaLogRecord.LogRecord.Builder logRecord = createKafkaLogRecord(logRecordRequest);
        BuildKafkaParamsUtils.checkAndFillProperty(BvLogRecord::getLinkToTool,
                KafkaLogRecord.LogRecord.Builder::setLinkToTool, in -> in,
                logRecordRequest, logRecord);

        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicName, logRecord.getUuid(),
                        logRecord.build()),
                ProducerType.PROTOBUF);
        sendContextVariablesAndMessageParametersIfExists(logRecordRequest);

        log.debug("end sendBvLogRecord()");
        return context;
    }

    protected KafkaLogRecord.LogRecord.Builder createKafkaLogRecord(LogRecord logRecordRequest) {
        log.debug("start createKafkaLogRecord(logRecordRequest: {})", logRecordRequest);
        Timestamp finishDate = logRecordRequest.getEndDate();
        Set<String> configInfo = logRecordRequest.getConfigInfoId();
        Set<String> validationLabels = logRecordRequest.getValidationLabels();
        String name = actionParametersTrimmer.trimActionParametersByLimit(logRecordRequest.getName());

        KafkaLogRecord.LogRecord.Builder builder = KafkaLogRecord.LogRecord.newBuilder()
                .setTestRunId(logRecordRequest.getTestRunId().toString())
                .setUuid(logRecordRequest.getUuid().toString())
                .setIsSection(logRecordRequest.isSection())
                .setClassName(logRecordRequest.getClass().getName())
                .setIsCompaund(logRecordRequest.isCompaund())
                .setName(name)
                .setMessage(logRecordRequest.getMessage())
                .setExecutionStatus(logRecordRequest.getExecutionStatus().name())
                .setTestingStatus(logRecordRequest.getTestingStatus().name())
                .setCreatedDate(logRecordRequest.getCreatedDate().getTime())
                .setCreatedDateStamp(logRecordRequest.getCreatedDateStamp())
                .setType(logRecordRequest.getType().name());

        if (Objects.nonNull(logRecordRequest.getMetaInfo())) {
            KafkaLogRecord.MetaInfo.Builder metaInfoBuilder = KafkaLogRecord.MetaInfo.newBuilder();
            UUID scenarioId = logRecordRequest.getMetaInfo().getScenarioId();
            if (Objects.nonNull(scenarioId)) {
                metaInfoBuilder.setScenarioId(scenarioId.toString());
            }
            UUID definitionId = logRecordRequest.getMetaInfo().getDefinitionId();
            if (Objects.nonNull(definitionId)) {
                metaInfoBuilder.setDefinitionId(definitionId.toString());
            }
            String scenarioHashSum = logRecordRequest.getMetaInfo().getScenarioHashSum();
            if (Objects.nonNull(scenarioHashSum)) {
                metaInfoBuilder.setScenarioHashSum(scenarioHashSum);
            }
            Integer line = logRecordRequest.getMetaInfo().getLine();
            if (Objects.nonNull(line)) {
                metaInfoBuilder.setLine(line);
            }

            StepLinkMetaInfo editorMetaInfo = logRecordRequest.getMetaInfo().getEditorMetaInfo();
            if (Objects.nonNull(editorMetaInfo)) {
                KafkaLogRecord.StepLinkMetaInfo.Builder stepLinkMetaInfoOrBuilder = KafkaLogRecord.StepLinkMetaInfo.newBuilder();
                stepLinkMetaInfoOrBuilder.setEngineType(editorMetaInfo.getEngineType().toString());
                stepLinkMetaInfoOrBuilder.setValue((String) editorMetaInfo.getValue());
                metaInfoBuilder.setEditorMetaInfo(stepLinkMetaInfoOrBuilder.build());
            }
            metaInfoBuilder.setHidden(logRecordRequest.getMetaInfo().isHidden());
            builder.setMetaInfo(metaInfoBuilder.build());
        }

        if (Objects.nonNull(logRecordRequest.getTable())) {
            List<KafkaLogRecord.Row> rows = logRecordRequest.getTable()
                    .getRows()
                    .stream()
                    .map(row -> KafkaLogRecord.Row.newBuilder()
                            .addAllCells(row
                                    .getCells()
                                    .stream()
                                    .map(cell -> KafkaLogRecord.Cell.newBuilder()
                                            .setValue(cell.getValue())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
            builder.setTable(KafkaLogRecord.Table.newBuilder().addAllRows(rows));
        }

        if (Objects.nonNull(logRecordRequest.getParentRecordId())) {
            builder.setParentRecordId(logRecordRequest.getParentRecordId().toString());
        }

        if (Objects.nonNull(logRecordRequest.getStartDate())) {
            builder.setStartDate(logRecordRequest.getStartDate().getTime());
        }
        if (Objects.nonNull(finishDate)) {
            builder.setEndDate(finishDate.getTime());
        }
        builder.setDuration(logRecordRequest.getDuration());

        if (Objects.nonNull(configInfo)) {
            builder.addAllConfigInfoId(configInfo);
        }
        if (Objects.nonNull(validationLabels)) {
            builder.addAllValidationLabels(validationLabels);
        }
        ValidationTable table = logRecordRequest.getValidationTable();
        if (Objects.nonNull(table)) {
            BuildKafkaParamsUtils.buildValidationTable(table, builder);
        }

        log.debug("Server: {} lrId {}", logRecordRequest.getServer(), logRecordRequest.getUuid());
        if (!StringUtils
                .isEmpty(logRecordRequest.getServer())) {
            builder.setServer(logRecordRequest.getServer());
        }

        if (!Strings.isNullOrEmpty(context.getAtpLogRecordId())) {
            builder.setAtpSource(
                    KafkaLogRecord.ATPSource.newBuilder()
                            .setAtpObjectId(context.getAtpLogRecordId())
                            .build());
        }

        List<FileMetadata> fileMetadataList = logRecordRequest.getFileMetadata();
        if (!CollectionUtils.isEmpty(fileMetadataList)) {
            builder.addAllFileMetadata(
                    fileMetadataList.stream().map(metadata ->
                            KafkaLogRecord.FileMetadata.newBuilder()
                                    .setFileType(metadata.getType().toString())
                                    .setFileName(metadata.getFileName())
                                    .build()).collect(Collectors.toList())
            );
        }

        List<CustomLink> customLinks = logRecordRequest.getCustomLinks();
        if (CollectionUtils.isNotEmpty(customLinks)) {
            customLinks.forEach(cl -> builder.addCustomLinks(KafkaLogRecord.CustomLink
                    .newBuilder()
                    .setName(cl.getName())
                    .setUrl(cl.getUrl())
                    .setOpenMode(cl.getOpenMode().name())
                    .build()));
        }

        BuildKafkaParamsUtils.checkAndFillProperty(LogRecord::getLinkToSvp, KafkaLogRecord.LogRecord.Builder::setLinkToSvp,
                in -> in, logRecordRequest, builder);

        log.debug("Log Record for kafka {}", builder.build());
        return builder;
    }

    private String sendConfigFiles(Map<String, String> configFiles, String category) {
        ArrayNode response = OBJECT_MAPPER.createArrayNode();
        configFiles.forEach((key, value) -> {
            ObjectNode configFile = OBJECT_MAPPER.createObjectNode();
            String uuid = UUID.randomUUID().toString();
            configFile.put(UUID_KEY, uuid);
            configFile.put(NAME_KEY, key);
            configFile.put(DATA_KEY, value);
            configFile.put(CATEGORY_KEY, category);
            kafkaPoolManagementService.sendProducerRecord(new ProducerRecord(topicNameConfigFiles, configFile),
                    ProducerType.JSON);
            response.add(uuid);
        });
        return response.toString();
    }
}
