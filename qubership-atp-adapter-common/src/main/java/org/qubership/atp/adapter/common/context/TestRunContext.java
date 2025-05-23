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

package org.qubership.atp.adapter.common.context;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Strings;
import org.qubership.atp.ram.enums.TestScopeSections;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TestRunContext implements Cloneable, Serializable {

    private String testRunId;
    private String testRunName;
    private boolean isFinalTestRun;
    private UUID initialTestRunId;
    private String executionRequestName;
    private String executionRequestId;
    private String initialExecutionRequestId;
    private UUID environmentInfoId;
    private String projectName;
    private String projectId;
    private String testPlanName;
    private String testPlanId;
    private String testSuiteName;
    private String testCaseName;
    private String testCaseId;
    private String testScopeId;
    private String labelTemplateId;
    private UUID widgetConfigTemplateId;
    private String atpExecutionRequestId;
    private String atpTestRunId;
    private String atpLogRecordId; //TODO remove it
    private String qaHost;
    private String qaExternalHost;
    private String taHost;
    private String mailList;
    private Timestamp startDate;
    private AtpCompaund atpCompaund;
    private String logRecordUuid;
    private LogRecordsStack sections;
    private String currentSectionId;
    private HashSet<String> urlToBrowserOrLogs;
    private String environmentId;
    private TestingStatuses testingStatus;
    private String lineNumber;
    private String scenarioHashSum;
    private String entityId;
    @Deprecated
    private String lastSectionInStep;
    private String dataSetListId;
    private String dataSetId;
    private String executor;
    private UUID executorId;
    private int threads;
    private boolean parentStatusUpdate;
    private boolean autoSyncCasesWithJira;
    private boolean autoSyncRunsWithJira;
    private TestScopeSections testScopeSection;
    private int order;
    private Set<UUID> labelIds;
    private Set<UUID> flagIds;
    private String logCollectorData;

    public TestRunContext() {
        this.testingStatus = TestingStatuses.UNKNOWN;
    }

    public String getMailList() {
        return Strings.nullToEmpty(mailList);
    }

    /**
     * Get start date.
     *
     * @return date as {@link Timestamp}
     */
    public Timestamp getStartDate() {
        return Objects.isNull(startDate) ? new Timestamp(System.currentTimeMillis()) : startDate;
    }

    /**
     * Sets provided testingStatus only if its id is greater than existing status id.
     */
    public void setTestingStatus(TestingStatuses testingStatus) {
        if (this.testingStatus.getId() < testingStatus.getId()) {
            this.testingStatus = testingStatus;
        }
    }

    /**
     * Sets provided testingStatus hard.
     */
    public void setTestingStatusHard(TestingStatuses testingStatus) {
        this.testingStatus = testingStatus;
    }

    /**
     * Method to add url to urlBrowserOrLogs list.
     */
    public void addUrlToBrowserOrLogs(String urlToBrowserOrLogs) {
        if (this.urlToBrowserOrLogs == null) {
            this.urlToBrowserOrLogs = new HashSet<>();
        }
        this.urlToBrowserOrLogs.add(urlToBrowserOrLogs);
    }

    /**
     * Add section to context.
     */
    public String addSection(LogRecord section) {
        if (sections == null) {
            sections = new LogRecordsStack();
        }
        sections.push(section);
        currentSectionId = section.getUuid().toString();
        return currentSectionId;
    }

    public String getCurrentSectionId() {
        return Strings.nullToEmpty(currentSectionId);
    }

    /**
     * Returns current section.
     */
    public LogRecord getCurrentSection() {
        if (sections == null || sections.empty()) {
            return null;
        }
        return sections.peek();
    }

    /**
     * Check whether hash code of current section was changed after it was added to stack.
     *
     * @return returns true if log record's hash code was changed
     */
    public boolean isCurrentSectionChanged() {
        if (sections == null) {
            return false;
        }
        return sections.isCurrentLogRecordChanged();
    }

    public Stack<LogRecord> getSections() {
        if (Objects.isNull(sections)) {
            sections = new LogRecordsStack();
        }
        return sections;
    }

    /**
     * Remove section from context.
     */
    public String removeSection() {
        if (sections == null) {
            sections = new LogRecordsStack();
            currentSectionId = StringUtils.EMPTY;
        }
        if (!sections.empty()) {
            sections.pop();
        }
        updateCurrentSectionId();
        return currentSectionId;
    }

    private void updateCurrentSectionId() {
        if (sections.empty()) {
            currentSectionId = StringUtils.EMPTY;
        } else {
            currentSectionId = sections.peek().getUuid().toString();
        }
    }

    @Override
    public TestRunContext clone() {

        try {
            return (TestRunContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone is not supported for TestRunContext super class");
        }
    }

    /**
     * Update compound statuses in context.
     *
     * @param newAtpCompound compound from request.
     */
    public void setCompoundAndUpdateCompoundStatuses(AtpCompaund newAtpCompound) {
        if (nonNull(this.atpCompaund)) {
            HashMap<String, TestingStatuses> compoundStatusesMap = compoundStatusesFromContextToMap();
            AtpCompaund currentCompound = newAtpCompound;
            while (nonNull(currentCompound)) {
                TestingStatuses compoundStatusFromContext = compoundStatusesMap.get(currentCompound.getSectionId());
                if (nonNull(compoundStatusFromContext)
                        && (isNull(currentCompound.getTestingStatuses())
                        || TestingStatuses.UNKNOWN.equals(currentCompound.getTestingStatuses())
                        || TestingStatuses.NOT_STARTED.equals(currentCompound.getTestingStatuses()))) {
                    currentCompound.setTestingStatuses(compoundStatusFromContext);
                    log.trace("Set new status {} for compound {}", currentCompound.getTestingStatuses(),
                            currentCompound.getSectionId());
                }
                currentCompound = currentCompound.getParentSection();
            }
        }
        this.atpCompaund = newAtpCompound;
    }

    private HashMap<String, TestingStatuses> compoundStatusesFromContextToMap() {
        HashMap<String, TestingStatuses> result = new HashMap<>();
        AtpCompaund currentCompound = this.atpCompaund;
        while (nonNull(currentCompound)) {
            result.put(currentCompound.getSectionId(), currentCompound.getTestingStatuses());
            currentCompound = currentCompound.getParentSection();
        }
        return result;
    }
}
