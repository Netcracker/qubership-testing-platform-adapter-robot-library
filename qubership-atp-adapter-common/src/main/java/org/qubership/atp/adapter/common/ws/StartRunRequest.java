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

package org.qubership.atp.adapter.common.ws;

import static org.qubership.atp.adapter.common.RamConstants.UNKNOWN;
import static org.qubership.atp.adapter.common.utils.ExecutionRequestHelper.generateRequestName;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.models.MetaInfo;
import lombok.Data;

@Data
public class StartRunRequest {

    private String projectName;
    private UUID projectId;
    private String testPlanName;
    private UUID testPlanId;
    private String testSuiteName;
    private String testCaseName;
    private UUID testCaseId;
    private String executionRequestName;
    private UUID atpExecutionRequestId;
    private String testRunName;
    private String testRunId;
    private boolean isFinalTestRun;
    private UUID initialTestRunId;
    private Timestamp startDate;
    private String taHost;
    private String qaHost;
    private String executor;
    private UUID executorId;
    private String solutionBuild;
    private String mailList;
    private UUID testScopeId;
    private UUID environmentId;
    private MetaInfo metaInfo;
    private String labelTemplateId;
    private UUID widgetConfigTemplateId;
    private String dataSetListId;
    private String dataSetId;
    private int threads;
    private boolean autoSyncCasesWithJira;
    private boolean autoSyncRunsWithJira;
    private TestScopeSections testScopeSection;
    private int order;
    private Set<UUID> labelIds;
    private Set<UUID> flagIds;

    private StartRunRequest() {
    }

    public static RequestBuilder getRequestBuilder() {
        return new StartRunRequest().new RequestBuilder();
    }

    public class RequestBuilder {

        private RequestBuilder() {
        }

        public StartRunRequest build() {
            validateFields();
            return StartRunRequest.this;
        }

        private void validateFields() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(projectName), "ProjectName is required!");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(testPlanName), "TestPlanName is required!");
            testSuiteName = Strings.isNullOrEmpty(testSuiteName) ? "Single Test Runs" : testSuiteName;
            executionRequestName = Strings.isNullOrEmpty(executionRequestName)
                    ? generateRequestName() : executionRequestName;
            testRunName = Strings.isNullOrEmpty(testRunName) ? testCaseName + LocalDateTime.now().toString() :
                    testRunName;
            taHost = Strings.isNullOrEmpty(taHost) ? UNKNOWN : taHost;
            qaHost = Strings.isNullOrEmpty(qaHost) ? UNKNOWN : qaHost;
            executor = Strings.isNullOrEmpty(executor) ? UNKNOWN : executor;
            solutionBuild = Strings.isNullOrEmpty(solutionBuild) ? UNKNOWN : solutionBuild;
            mailList = Strings.nullToEmpty(mailList);
            dataSetListId = Strings.nullToEmpty(dataSetListId);
            dataSetId = Strings.nullToEmpty(dataSetId);
            testRunId = Strings.nullToEmpty(testRunId);
        }

        public RequestBuilder setProjectName(String projectName) {
            StartRunRequest.this.projectName = projectName;
            return this;
        }

        public RequestBuilder setTestPlanName(String testPlanName) {
            StartRunRequest.this.testPlanName = testPlanName;
            return this;
        }

        public RequestBuilder setTestSuiteName(String testSuiteName) {
            StartRunRequest.this.testSuiteName = testSuiteName;
            return this;
        }

        public RequestBuilder setTestCaseName(String testCaseName) {
            StartRunRequest.this.testCaseName = testCaseName;
            return this;
        }

        public RequestBuilder setExecutionRequestName(String executionRequestName) {
            StartRunRequest.this.executionRequestName = executionRequestName;
            return this;
        }

        public RequestBuilder setTestRunName(String testRunName) {
            StartRunRequest.this.testRunName = testRunName;
            return this;
        }

        public RequestBuilder setTestRunId(String testRunId) {
            StartRunRequest.this.testRunId = testRunId;
            return this;
        }

        public RequestBuilder setStartDate(Timestamp startDate) {
            StartRunRequest.this.startDate = startDate;
            return this;
        }

        public RequestBuilder setTaHost(String taHost) {
            StartRunRequest.this.taHost = taHost;
            return this;
        }

        public RequestBuilder setQaHost(String qaHost) {
            StartRunRequest.this.qaHost = qaHost;
            return this;
        }

        public RequestBuilder setDataSetListId(String dataSetListId) {
            StartRunRequest.this.dataSetListId = dataSetListId;
            return this;
        }

        public RequestBuilder setDataSetId(String dataSetId) {
            StartRunRequest.this.dataSetId = dataSetId;
            return this;
        }

        public RequestBuilder setExecutor(String executor) {
            StartRunRequest.this.executor = executor;
            return this;
        }

        public RequestBuilder setExecutorId(UUID executorId) {
            StartRunRequest.this.executorId = executorId;
            return this;
        }

        public RequestBuilder setSolutionBuild(String solutionBuild) {
            StartRunRequest.this.solutionBuild = solutionBuild;
            return this;
        }

        public RequestBuilder setTestScopeId(UUID testScopeId) {
            StartRunRequest.this.testScopeId = testScopeId;
            return this;
        }

        public RequestBuilder setEnvironmentId(UUID environmentId) {
            StartRunRequest.this.environmentId = environmentId;
            return this;
        }

        /**
         * Set mailList to request.
         */
        public RequestBuilder setMailList(String mailList) {
            if (!Strings.isNullOrEmpty(mailList)) {
                StartRunRequest.this.mailList = mailList.replace(";", ",");
            }
            return this;
        }

        public RequestBuilder setTestPlanId(UUID testPlanId) {
            StartRunRequest.this.testPlanId = testPlanId;
            return this;
        }

        public RequestBuilder setProjectId(UUID projectId) {
            StartRunRequest.this.projectId = projectId;
            return this;
        }

        /**
         * Set AtpExecutionRequestId to request.
         */
        public RequestBuilder setAtpExecutionRequestId(UUID atpExecutionRequestId) {
            StartRunRequest.this.atpExecutionRequestId = atpExecutionRequestId;
            return this;
        }

        /**
         * Set test case ID.
         * Ignore, when test case ID = 'null'
         *
         * @param testCaseId for setting
         * @return builder
         */
        public RequestBuilder setTestCaseId(UUID testCaseId) {
            if (Objects.nonNull(testCaseId)) {
                StartRunRequest.this.testCaseId = testCaseId;
            }
            return this;
        }

        public RequestBuilder setLabelTemplateId(String labelTemplateId) {
            StartRunRequest.this.labelTemplateId = labelTemplateId;
            return this;
        }

        public RequestBuilder setMetaInfo(MetaInfo metaInfo) {
            StartRunRequest.this.metaInfo = metaInfo;
            return this;
        }

        public RequestBuilder setThreads(int threads) {
            StartRunRequest.this.threads = threads;
            return this;
        }

        public RequestBuilder setAutoSyncCasesWithJira(boolean autoSyncCasesWithJira) {
            StartRunRequest.this.autoSyncCasesWithJira = autoSyncCasesWithJira;
            return this;
        }

        public RequestBuilder setAutoSyncRunsWithJira(boolean autoSyncRunsWithJira) {
            StartRunRequest.this.autoSyncRunsWithJira = autoSyncRunsWithJira;
            return this;
        }

        public RequestBuilder setWidgetConfigTemplateId(UUID widgetConfigTemplateId) {
            StartRunRequest.this.widgetConfigTemplateId = widgetConfigTemplateId;
            return this;
        }

        public RequestBuilder setTestScopeSection(TestScopeSections testScopeSection) {
            StartRunRequest.this.testScopeSection = testScopeSection;
            return this;
        }

        public RequestBuilder setOrder(int order) {
            StartRunRequest.this.order = order;
            return this;
        }

        public RequestBuilder setLabelIds(Set<UUID> labelIds) {
            StartRunRequest.this.labelIds = labelIds;
            return this;
        }

        public RequestBuilder setFlagIds(Set<UUID> flagIds) {
            StartRunRequest.this.flagIds = flagIds;
            return this;
        }

        public RequestBuilder setIsFinalTestRun(boolean isFinalTestRun) {
            StartRunRequest.this.isFinalTestRun = isFinalTestRun;
            return this;
        }

        public RequestBuilder setInitialTestRunId(UUID initialTestRunId) {
            StartRunRequest.this.initialTestRunId = initialTestRunId;
            return this;
        }
    }
}
