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

package org.qubership.atp.adapter.common;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.adapter.common.adapters.error.FailedToCreateRamEntity;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.ram.enums.EngineCategory;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.EnvironmentsInfo;
import org.qubership.atp.ram.models.ExecutionRequestDetails;
import org.qubership.atp.ram.models.TestRun;
import org.qubership.atp.ram.models.TestRunStatistic.ReportLabelParameterData;
import org.qubership.atp.ram.models.ToolsInfo;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import net.sf.json.JSONObject;

public interface AtpRamAdapter {

    TestRunContext startExecutionRequest(
            String executionRequestName,
            UUID projectId,
            UUID testPlanId,
            ExecutionStatuses executionStatuses) throws FailedToCreateRamEntity;

    TestRunContext startExecutionRequest(
            String executionRequestName,
            UUID projectId,
            UUID testPlanId,
            ExecutionStatuses executionStatuses,
            String jointExecutionKey,
            Integer jointExecutionTimeout,
            Integer jointExecutionCount) throws FailedToCreateRamEntity;

    ExecutionRequestDetails reportDetails(String message, TestingStatuses status) throws FailedToCreateRamEntity;

    void updateTestRun(TestRun testRunPatch) throws FailedToCreateRamEntity;

    void finishAllTestRuns(List<UUID> testRunUuids, boolean isDelayed) throws FailedToCreateRamEntity;

    TestRunContext startAtpRun(StartRunRequest request, TestRunContext context);

    /**
     * @deprecated for test run update use {@link AtpRamAdapter#updateTestRun(org.qubership.atp.ram.models.TestRun)}
     */
    @Deprecated
    TestRunContext updateAtpRun(TestRunContext context);

    String saveConfigInfo(Map<String, String> configFiles,
                          org.qubership.atp.adapter.common.utils.EngineCategory category);

    String saveConfigInfo(Map<String, String> configFiles, EngineCategory category);

    @Deprecated
    TestRunContext stopAtpRun(String testRunId);
    TestRunContext stopTestRun(String testRunId) throws Exception;

    TestRunContext openSection(Message section);

    TestRunContext openSection(Message section, boolean isStep);

    TestRunContext openSection(String name, String message, String status);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId, String status);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId, String status,
                               Set<String> validationLabels);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId, String status,
                               String type);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId, String status,
                               String type, boolean hidden);

    @Deprecated
    TestRunContext openSection(String name, String message, String parentSectionId,
                               String sectionId, String status, String type, boolean isStepFromAtpCompound,
                               boolean hidden);

    TestRunContext openSection(String name, String message, String parentSectionId,
                               String sectionId, String status, String type, boolean isStepFromAtpCompound,
                               boolean hidden, Set<String> validationLabels);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                               boolean isCompaund, String status);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                               boolean isCompaund, String status, String type);

    TestRunContext openSection(String name, String message, String parentSectionId, String sectionId,
                               boolean isCompaund, String status, String type, boolean hidden);

    @Deprecated
    TestRunContext openSection(String name, String message, String parentSectionId,
                               String sectionId, boolean isCompaund, String status, String type,
                               boolean isStepFromAtpCompound, boolean hidden);

    TestRunContext openSection(String name, String message, String parentSectionId,
                               String sectionId, boolean isCompaund, String status, String type,
                               boolean isStepFromAtpCompound, boolean hidden, Set<String> validationLabels);

    TestRunContext openItfSection(Message message, JSONObject validationTable);

    TestRunContext openMiaSection(Message message);

    TestRunContext closeSection();

    TestRunContext message(String sectionId,
                           String name, String message, String status, Map attributes);

    TestRunContext message(String sectionId,
                           String name, String message, String status, Map attributes, String type);

    TestRunContext message(String sectionId,
                           String name, String message, String status, Map attributes, Set<String> configInfo,
                           String type);

    TestRunContext message(String sectionId,
                           String name, String message, String status, Map attributes, Set<String> configInfo,
                           String type, Boolean hidden);

    TestRunContext message(Message message);

    TestRunContext message(String sectionId,
                           String name, String message, String status);

    TestRunContext bvMessage(Message message);

    TestRunContext restMessage(Message message);

    TestRunContext restMessage(Message message, JSONObject validationTable);

    TestRunContext sqlMessage(Message message);

    TestRunContext sshMessage(Message message);

    void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp endDate, long duration);

    @Deprecated
    void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate);

    void updateExecutionStatus(String logRecordId, String executionStatus, Timestamp startDate, String name);

    void updateContextVariables(String logRecordId, List<ContextVariable> contextVariables);

    void updateStepContextVariables(String logRecordId, List<ContextVariable> contextVariables);

    void updateMessageTestingStatusRequestAndResponse(String logRecordId, String message,
                                                      String testingStatus, Request request,
                                                      Response response);

    void updateMessageTestingStatusAndFiles(Message message);
    void uploadFileForLogRecord(String logRecordId, InputStream fileContent, String fileName);

    @Deprecated
    TestRunContext updateMessageAndTestingStatus(String logRecordId, String message, String testingStatus);

    TestRunContext updateMessageWithIsGroup(String logRecordId, boolean isGroup);

    TestRunContext updateTestingStatus(String logRecordId, String status);

    void sendEmail(String executionRequestId, String recipients, String subject, String templateId);

    void createReporting(String executionRequestId, String projectId, String recipients, String subject);

    TestRunContext updateSsmMetricReports(String logRecordId, String problemContextMetricReportId,
                                          String microservicesReportId);

    TestRunContext reportEnvironmentsInfo(EnvironmentsInfo environmentsInfo);

    TestRunContext reportToolsInfo(ToolsInfo toolsInfo);

    void sendRamReportImmediately(String executionRequestUuid);

    void setContext(TestRunContext context);

    TestRunContext getContext();

    TestRunContext updateExecutionRequestStatus(ExecutionStatuses statuses, String erId);

    TestRunContext updateTestRunStatus(ExecutionStatuses statuses, String trId);

    TestRunContext openCompoundSection(Message compound, boolean isStep);

    void upsertTestRunStatisticLabelReportParam(String trId, String paramKey, ReportLabelParameterData paramData);

    void close();
}
