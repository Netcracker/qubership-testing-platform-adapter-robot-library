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

package org.qubership.atp.adapter.common.utils;

import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.getCurrentTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.qubership.atp.adapter.common.protos.KafkaLogRecordMessageParameter;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import org.qubership.atp.adapter.common.protos.KafkaEnvironmentsInfo;
import org.qubership.atp.adapter.common.protos.KafkaLogRecord;
import org.qubership.atp.ram.enums.BvStatus;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.SsmMetricReports;
import org.qubership.atp.ram.models.SystemInfo;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.WdShells;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.RequestHeader;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildKafkaParamsUtils {

    /**
     * Build context variables for KafkaLogRecord.
     *
     * @param contextVariables List of {@link ContextVariable}.
     * @return List of {@link KafkaLogRecord.ContextVariable}.
     */
    public static List<KafkaLogRecord.ContextVariable> buildContextVariables(List<ContextVariable> contextVariables) {
        return contextVariables.stream()
                .map(variable -> {
                    KafkaLogRecord.ContextVariable.Builder contextVariable = KafkaLogRecord.ContextVariable.newBuilder()
                            .setName(variable.getName());
                    checkAndFillProperty(ContextVariable::getBeforeValue,
                            KafkaLogRecord.ContextVariable.Builder::setBeforeValue, in -> in, variable,
                            contextVariable);
                    checkAndFillProperty(ContextVariable::getAfterValue,
                            KafkaLogRecord.ContextVariable.Builder::setAfterValue, in -> in, variable,
                            contextVariable);
                    return contextVariable.build();
                }).collect(Collectors.toList());
    }

    /**
     * Build message parameters for KafkaLogRecordMessageParameter.
     * @param messageParameters List of {@link MessageParameter}.
     * @return List of {@link KafkaLogRecordMessageParameter.MessageParameter}.
     */
    public static List<KafkaLogRecordMessageParameter.MessageParameter> buildMessageParameters(
            List<MessageParameter> messageParameters) {
        return messageParameters.stream()
                .map(parameter -> {
                    KafkaLogRecordMessageParameter.MessageParameter.Builder messageParameter =
                            KafkaLogRecordMessageParameter.MessageParameter.newBuilder()
                                    .setName(parameter.getName())
                                    .setValue(parameter.getValue());
                    return messageParameter.build();
                }).collect(Collectors.toList());
    }

    /**
     * Build validation table for KafkaLogRecord.
     *
     * @param table   {@link ValidationTable}.
     * @param builder Kafka LogRecord builder.
     */
    public static void buildValidationTable(ValidationTable table, KafkaLogRecord.LogRecord.Builder builder) {
        List<KafkaLogRecord.ValidationTableLine> validationTableLineList = new ArrayList<>();
        table.getSteps().forEach(step -> {
            KafkaLogRecord.ValidationTableLine.Builder validationTableLine =
                    KafkaLogRecord.ValidationTableLine.newBuilder()
                            .setName(step.getName())
                            .setStatus(step.getStatus().name());
            checkAndFillProperty(ValidationTableLine::getActualResult,
                    KafkaLogRecord.ValidationTableLine.Builder::setActualResult, in -> in,
                    step, validationTableLine);
            if (Objects.nonNull(step.getExpectedResult())) {
                validationTableLine.setExpectedResult(step.getExpectedResult());
            }
            validationTableLine.setBvStatus(step.getBvStatus() == null
                    ? BvStatus.UNDEFINED.name() : step.getBvStatus().name());
            validationTableLineList.add(validationTableLine.build());
        });
        KafkaLogRecord.ValidationTable validationTable = KafkaLogRecord.ValidationTable.newBuilder()
                .addAllSteps(validationTableLineList)
                .build();
        builder.setValidationTable(validationTable);
    }

    /**
     * Build request for KafkaLogRecord.
     *
     * @param request {@link Request}.
     * @param builder Kafka LogRecord builder.
     */
    public static void buildRequest(Request request, KafkaLogRecord.LogRecord.Builder builder) {
        if (Objects.nonNull(request)) {
            KafkaLogRecord.Request.Builder kafkaRequestBuilder = KafkaLogRecord.Request.newBuilder();
            checkAndFillProperty(Request::getEndpoint, KafkaLogRecord.Request.Builder::setEndpoint,
                    in -> in, request, kafkaRequestBuilder);
            checkAndFillProperty(Request::getMethod, KafkaLogRecord.Request.Builder::setMethod,
                    in -> in, request, kafkaRequestBuilder);
            checkAndFillProperty(Request::getBody, KafkaLogRecord.Request.Builder::setBody,
                    in -> in, request, kafkaRequestBuilder);
            checkAndFillProperty(Request::getTimestamp, KafkaLogRecord.Request.Builder::setTimestamp,
                    Timestamp::getTime, request, kafkaRequestBuilder);
            if (MapUtils.isNotEmpty(request.getHeaders())) {
                kafkaRequestBuilder.addAllHeaders(buildMapForLogRecord(request.getHeaders()));
            }
            if (CollectionUtils.isNotEmpty(request.getHeadersList())) {
                kafkaRequestBuilder.addAllHeadersList(buildListHeadersForLogRecord(request.getHeadersList()));
            }
            kafkaRequestBuilder.setHtmlBody(request.isHtmlBody());
            builder.setRequest(kafkaRequestBuilder.build());
        }
    }

    /**
     * Build response for KafkaLogRecord.
     *
     * @param response {@link Response}.
     * @param builder  Kafka LogRecord builder.
     */
    public static void buildResponse(Response response, KafkaLogRecord.LogRecord.Builder builder) {
        if (Objects.nonNull(response)) {
            KafkaLogRecord.Response.Builder kafkaResponseBuilder = KafkaLogRecord.Response.newBuilder();
            checkAndFillProperty(Response::getEndpoint, KafkaLogRecord.Response.Builder::setEndpoint,
                    in -> in, response, kafkaResponseBuilder);
            checkAndFillProperty(Response::getCode, KafkaLogRecord.Response.Builder::setCode,
                    in -> in, response, kafkaResponseBuilder);
            checkAndFillProperty(Response::getBody, KafkaLogRecord.Response.Builder::setBody,
                    in -> in, response, kafkaResponseBuilder);
            checkAndFillProperty(Response::getTimestamp, KafkaLogRecord.Response.Builder::setTimestamp,
                    Timestamp::getTime, response, kafkaResponseBuilder);
            if (MapUtils.isNotEmpty(response.getHeaders())) {
                kafkaResponseBuilder.addAllHeaders(buildMapForLogRecord(response.getHeaders()));
            }
            if (CollectionUtils.isNotEmpty(response.getHeadersList())) {
                kafkaResponseBuilder.addAllHeadersList(buildListHeadersForLogRecord(response.getHeadersList()));
            }
            builder.setResponse(kafkaResponseBuilder.build());
        }
    }

    private static List<KafkaLogRecord.RequestHeader> buildListHeadersForLogRecord(List<RequestHeader> list) {
        log.debug("start buildListHeadersForLogRecord(list: {})", list);
        List<KafkaLogRecord.RequestHeader> listHeaders = new ArrayList<>();
        list.forEach(element -> {
            listHeaders.add(KafkaLogRecord.RequestHeader.newBuilder()
                    .setName(element.getName())
                    .setValue(element.getValue())
                    .setDescription(element.getDescription())
                    .build()
            );
        });
        log.debug("end buildListHeadersForLogRecord(): {}", listHeaders);
        return listHeaders;
    }

    /**
     * Create Kafka Environments info.
     *
     * @param environmentsInfo {@link EnvironmentsInfo}.
     * @return {@link KafkaEnvironmentsInfo.EnvironmentsInfo}.
     */
    public static KafkaEnvironmentsInfo.EnvironmentsInfo createKafkaEnvironmentsInfo(
            EnvironmentsInfo environmentsInfo) {
        String envInfoUuid = environmentsInfo.getUuid() != null ? environmentsInfo.getUuid().toString() :
                UUID.randomUUID().toString();
        KafkaEnvironmentsInfo.EnvironmentsInfo.Builder builder =
                KafkaEnvironmentsInfo.EnvironmentsInfo.newBuilder()
                        .setUuid(envInfoUuid);

        if (Objects.nonNull(environmentsInfo.getExecutionRequestId())) {
            builder.setExecutionRequestId(String.valueOf(environmentsInfo.getExecutionRequestId()));
        }

        checkAndFillProperty(EnvironmentsInfo::getName,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setName, in -> in, environmentsInfo, builder);
        checkAndFillProperty(EnvironmentsInfo::getToolsInfoUuid,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setToolsInfo,
                java.util.UUID::toString, environmentsInfo, builder);

        long startDate = Objects.isNull(environmentsInfo.getStartDate())
                ? getCurrentTimestamp().getTime() : environmentsInfo.getStartDate().getTime();
        builder.setStartDate(startDate);
        long endDate = getCurrentTimestamp().getTime();
        builder.setEndDate(endDate);
        builder.setDuration(endDate - startDate);

        buildEnvironmentsListIfExists(environmentsInfo, builder, EnvironmentsInfo::getQaSystemInfoList,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::addAllQaSystemInfoList);
        buildEnvironmentsListIfExists(environmentsInfo, builder, EnvironmentsInfo::getTaSystemInfoList,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::addAllTaSystemInfoList);

        checkAndFillProperty(EnvironmentsInfo::getEnvironmentId,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setEnvironmentId,
                java.util.UUID::toString, environmentsInfo, builder);
        checkAndFillProperty(EnvironmentsInfo::getTaToolsGroupId,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setTaToolsGroupId,
                java.util.UUID::toString, environmentsInfo, builder);
        checkAndFillProperty(EnvironmentsInfo::getMandatoryChecksReportId,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setMandatoryChecksReportId,
                java.util.UUID::toString, environmentsInfo, builder);
        buildSsmMetricReportsIfExists(environmentsInfo, builder, EnvironmentsInfo::getSsmMetricReports,
                KafkaEnvironmentsInfo.EnvironmentsInfo.Builder::setSsmMetricReports);
        return builder.build();
    }

    /**
     * Create Kafka Tools info.
     *
     * @param toolsInfo {@link ToolsInfo}.
     * @return {@link KafkaEnvironmentsInfo.ToolsInfo}.
     */
    public static KafkaEnvironmentsInfo.ToolsInfo createToolsInfo(ToolsInfo toolsInfo) {
        KafkaEnvironmentsInfo.ToolsInfo.Builder toolsInfoBuilder = KafkaEnvironmentsInfo.ToolsInfo.newBuilder();
        checkAndFillProperty(ToolsInfo::getUuid, KafkaEnvironmentsInfo.ToolsInfo.Builder::setUuid,
                java.util.UUID::toString, toolsInfo, toolsInfoBuilder);
        checkAndFillProperty(ToolsInfo::getDealer, KafkaEnvironmentsInfo.ToolsInfo.Builder::setDealer,
                in -> in, toolsInfo, toolsInfoBuilder);
        checkAndFillProperty(ToolsInfo::getDealerLogsUrl, KafkaEnvironmentsInfo.ToolsInfo.Builder::setDealerLogsUrl,
                in -> in, toolsInfo, toolsInfoBuilder);
        checkAndFillProperty(ToolsInfo::getTool, KafkaEnvironmentsInfo.ToolsInfo.Builder::setTool,
                in -> in, toolsInfo, toolsInfoBuilder);
        checkAndFillProperty(ToolsInfo::getToolLogsUrl, KafkaEnvironmentsInfo.ToolsInfo.Builder::setToolLogsUrl,
                in -> in, toolsInfo, toolsInfoBuilder);
        //buildWdShells(toolsInfo, toolsInfoBuilder);
        return toolsInfoBuilder.build();
    }

    /**
     * Build wdShells for tools info.
     *
     * @param toolsInfo        {@link ToolsInfo}.
     * @param toolsInfoBuilder Kafka LogRecord builder.
     */
    public static void buildWdShells(ToolsInfo toolsInfo, KafkaEnvironmentsInfo.ToolsInfo.Builder toolsInfoBuilder) {
        if (toolsInfo.getWdShells() != null) {
            List<WdShells> wdShells = toolsInfo.getWdShells();

            List<KafkaEnvironmentsInfo.WdShells> wdShellsList = wdShells.stream().map(wdShell ->
                    KafkaEnvironmentsInfo.WdShells.newBuilder()
                            .setName(wdShell.getName())
                            .setVersion(wdShell.getVersion())
                            .build()
            ).collect(Collectors.toList());

            toolsInfoBuilder.addAllWdShells(wdShellsList);
        }
    }

    /**
     * Set connection info for Kafka LogRecord.
     *
     * @param connectionInfo Map of connection info params.
     * @param logRecord      Kafka LogRecord builder.
     */
    public static void setConnectionInfoForLogRecord(Map<String, String> connectionInfo,
                                                     KafkaLogRecord.LogRecord.Builder logRecord) {
        log.debug("start setConnectionInfoForLogRecord(connectionInfo: {})", connectionInfo);
        if (MapUtils.isNotEmpty(connectionInfo)) {
            List<KafkaLogRecord.Map> connectionInfos = BuildKafkaParamsUtils.buildMapForLogRecord(connectionInfo);
            logRecord.addAllConnectionInfo(connectionInfos);
        }
        log.debug("end setConnectionInfoForLogRecord()");
    }

    /**
     * Set param for Kafka builder if param isn't null.
     *
     * @param getProperty   Method for get new param.
     * @param setProperty   Method for set new param in builder.
     * @param transformType Method for transform new param (if not needed, you can specify it like this 'in -> in').
     * @param sourceT       Object from take new param.
     * @param targetT       Object to put new param.
     */
    public static <SourceT, SourceTypeT, TargetT, TargetTypeT> void checkAndFillProperty(
            Function<SourceT, SourceTypeT> getProperty, BiConsumer<TargetT, TargetTypeT> setProperty,
            Function<SourceTypeT, TargetTypeT> transformType, SourceT sourceT, TargetT targetT) {
        if (getProperty.apply(sourceT) != null) {
            setProperty.accept(targetT, transformType.apply(getProperty.apply(sourceT)));
        }
    }

    private static List<KafkaLogRecord.Map> buildMapForLogRecord(Map<String, String> map) {
        log.debug("start buildMapForLogRecord(map: {})", map);
        List<KafkaLogRecord.Map> maps = new ArrayList<>();
        map.forEach((key, value) -> {
            maps.add(
                    KafkaLogRecord.Map.newBuilder()
                            .setKey(key).setValue(StringUtils.defaultIfEmpty(value, ""))
                            .build()
            );
        });
        log.debug("end buildMapForLogRecord(): {}", maps);
        return maps;
    }

    private static void buildSsmMetricReportsIfExists(
            EnvironmentsInfo environmentsInfo,
            KafkaEnvironmentsInfo.EnvironmentsInfo.Builder envInfoBuilder,
            Function<EnvironmentsInfo, SsmMetricReports> getSsmMetricReports,
            BiConsumer<KafkaEnvironmentsInfo.EnvironmentsInfo.Builder, KafkaEnvironmentsInfo.SsmMetricReports> setSsmMetricReports) {
        if (getSsmMetricReports.apply(environmentsInfo) != null) {
            KafkaEnvironmentsInfo.SsmMetricReports.Builder systemBuilder = KafkaEnvironmentsInfo.SsmMetricReports.newBuilder();
            SsmMetricReports metricReports = getSsmMetricReports.apply(environmentsInfo);
            checkAndFillProperty(SsmMetricReports::getMicroservicesReportId, KafkaEnvironmentsInfo.SsmMetricReports.Builder::setMicroservicesReportId,
                    java.util.UUID::toString, metricReports, systemBuilder);
            checkAndFillProperty(SsmMetricReports::getProblemContextReportId, KafkaEnvironmentsInfo.SsmMetricReports.Builder::setProblemContextReportId,
                    java.util.UUID::toString, metricReports, systemBuilder);
            KafkaEnvironmentsInfo.SsmMetricReports build = systemBuilder.build();
            setSsmMetricReports.accept(envInfoBuilder, build);
        }
    }

    private static void buildEnvironmentsListIfExists(
            EnvironmentsInfo environmentsInfo,
            KafkaEnvironmentsInfo.EnvironmentsInfo.Builder envInfoBuilder,
            Function<EnvironmentsInfo, List<SystemInfo>> getSystems,
            BiConsumer<KafkaEnvironmentsInfo.EnvironmentsInfo.Builder, List<KafkaEnvironmentsInfo.System>> setSystems) {
        if (!CollectionUtils.isEmpty(getSystems.apply(environmentsInfo))) {
            List<SystemInfo> systems = getSystems.apply(environmentsInfo);
            List<KafkaEnvironmentsInfo.System> environmentList = buildSystemList(systems);
            setSystems.accept(envInfoBuilder, environmentList);
        }
    }

    private static List<KafkaEnvironmentsInfo.System> buildSystemList(List<SystemInfo> systemsInfo) {
        List<KafkaEnvironmentsInfo.System> systemList = new ArrayList<>();
        for (SystemInfo system : systemsInfo) {
            KafkaEnvironmentsInfo.System.Builder systemBuilder = KafkaEnvironmentsInfo.System.newBuilder();
            checkAndFillProperty(SystemInfo::getName, KafkaEnvironmentsInfo.System.Builder::setName,
                    in -> in, system, systemBuilder);
            checkAndFillProperty(SystemInfo::getStatus, KafkaEnvironmentsInfo.System.Builder::setStatus,
                    Enum::name, system, systemBuilder);
            checkAndFillProperty(SystemInfo::getVersion, KafkaEnvironmentsInfo.System.Builder::setVersion,
                    in -> in, system, systemBuilder);
            checkAndFillProperty(SystemInfo::getUrls, KafkaEnvironmentsInfo.System.Builder::addAllUrls,
                    in -> in, system, systemBuilder);
            checkAndFillProperty(SystemInfo::getMonitoringSystem,
                    KafkaEnvironmentsInfo.System.Builder::setMonitoringSystem,
                    Enum::name, system, systemBuilder);
            systemList.add(systemBuilder.build());
        }
        return systemList;
    }
}
