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

package org.qubership.atp.adapter.executor.executor;

import static org.qubership.atp.adapter.common.RamConstants.SCREENSHOT_FILE_KEY;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;

import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.providers.RamAdapterProvider;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.adapter.executor.executor.utils.AttachmentCreator;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.adapter.report.ReportWriter;
import org.qubership.atp.adapter.report.SourceProvider;
import org.qubership.atp.adapter.report.WebReportItem;
import net.sf.json.JSONObject;

public class AtpRamWriter implements ReportWriter {

    private static final Logger log = LoggerFactory.getLogger(AtpRamWriter.class);

    private static final ReportThreadLocal report = new ReportThreadLocal();
    private static final String SECTION_NAME = "Section";
    private static final String SCREENSHOT_NAME_REGEX = "^screen-thread.*\\.png$";
    private AtpRamAdapter adapter;
    private String testRunId;
    private String executionRequestUuid;
    private String logName = StringUtils.EMPTY;
    private int parentSectionsCount;

    private TestRunContext context;

    /**
     * Initialize and return RAM writer.
     *
     * @return writer {@link AtpRamWriter}
     */
    public static AtpRamWriter getAtpRamWriter() {
        AtpRamWriter report = AtpRamWriter.report.get();
        if (report == null) {
            report = new AtpRamWriter();
            AtpRamWriter.report.set(report);
        }
        return report;
    }

    /**
     * Returns existing adapter or creates new AtpRamAdapter.
     *
     * @return {@link AtpRamAdapter}
     */
    public AtpRamAdapter getAdapter() {
        if (adapter == null) {
            adapter = RamAdapterProvider.getNewAdapter(logName);
        }
        return adapter;
    }

    /**
     * Open log with log name.
     */
    public synchronized void openLog(String testRunId) {
        openLog(testRunId, "");
    }

    /**
     * Open log with log name and description.
     */
    public synchronized void openLog(String testRunId, String description) {
        try {
            context = TestRunContextHolder.getContext(testRunId);
            String nullValue = "null";
            UUID executionRequestId = Strings.isNullOrEmpty(context.getAtpExecutionRequestId())
                    || nullValue.equalsIgnoreCase(context.getAtpExecutionRequestId()) ? null
                    : UUID.fromString(context.getAtpExecutionRequestId());
            UUID projectId = Strings.isNullOrEmpty(context.getProjectId())
                    || nullValue.equalsIgnoreCase(context.getProjectId()) ? null
                    : UUID.fromString(context.getProjectId());
            UUID testplanId = Strings.isNullOrEmpty(context.getTestPlanId())
                    || nullValue.equalsIgnoreCase(context.getTestPlanId()) ? null
                    : UUID.fromString(context.getTestPlanId());
            UUID testcaseId = Strings.isNullOrEmpty(context.getTestCaseId())
                    || nullValue.equalsIgnoreCase(context.getTestCaseId()) ? null
                    : UUID.fromString(context.getTestCaseId());


            StartRunRequest startRunRequest = StartRunRequest.getRequestBuilder()
                    .setProjectName(context.getProjectName())
                    .setTestPlanName(context.getTestPlanName())
                    .setTestCaseName(context.getTestCaseName())
                    .setTestSuiteName(context.getTestSuiteName())
                    .setExecutionRequestName(context.getExecutionRequestName())
                    .setTestRunName(context.getTestRunName())
                    .setTestRunId(testRunId)
                    .setIsFinalTestRun(context.isFinalTestRun())
                    .setInitialTestRunId(context.getInitialTestRunId())
                    .setSolutionBuild(ExecutionRequestHelper.getSolutionBuild(context.getQaHost()))
                    .setQaHost(context.getQaHost())
                    .setStartDate(ExecutionRequestHelper.getCurrentTimestamp())
                    .setExecutor(ExecutionRequestHelper.getOsUser())
                    .setTaHost(ExecutionRequestHelper.getHostName())
                    .setAtpExecutionRequestId(executionRequestId)
                    .setProjectId(projectId)
                    .setTestPlanId(testplanId)
                    .setTestCaseId(testcaseId)
                    .setLabelTemplateId(context.getLabelTemplateId())
                    .setDataSetId(context.getDataSetId())
                    .setDataSetListId(context.getDataSetListId())
                    .setThreads(context.getThreads())
                    .setExecutorId(context.getExecutorId())
                    .setAutoSyncCasesWithJira(context.isAutoSyncCasesWithJira())
                    .setAutoSyncRunsWithJira(context.isAutoSyncRunsWithJira())
                    .setTestScopeSection(context.getTestScopeSection())
                    .setOrder(context.getOrder())
                    .setLabelIds(context.getLabelIds())
                    .setFlagIds(context.getFlagIds())
                    .build();
            context = getAdapter().startAtpRun(startRunRequest, context);
            this.testRunId = context.getTestRunId();
            this.logName = context.getTestRunName();
            System.setProperty(logName + ".ram2.run.id", testRunId);
            this.executionRequestUuid = context.getExecutionRequestId();
        } catch (Exception e) {
            log.error("Cannot open Log " + executionRequestUuid, e);
        }
    }

    /**
     * Create TestRunContext for AtpRamWriter and adapter.
     * @param testRunId Test Run id.
     */
    public synchronized void createContext(String testRunId) {
        if (TestRunContextHolder.hasContext(testRunId)) {
            context = TestRunContextHolder.getContext(testRunId);
            getAdapter().setContext(context);
            log.debug("Context has been created for Test Run with id [{}].", testRunId);
        } else {
            log.warn("Context hasn't been created for Test Run with id [{}], because context wasn't find by this id.",
                    testRunId);
        }
    }

    /**
     * Stop TR.
     */
    public void closeLog(WebReportItem item) {
        getAdapter().stopAtpRun(testRunId);
    }

    /**
     * Open section.
     */
    public void openSection(WebReportItem.OpenSection item) {
        try {
            if (isContextEmpty()) {
                log.warn("Context is empty, unable open section {}", item.getTitle());
                return;
            }
            Message messageBean = new Message();
            messageBean.setName(Strings.isNullOrEmpty(item.getTitle()) ? SECTION_NAME : item.getTitle());
            messageBean.setMessage(item.getMessage());
            messageBean.setTestingStatus(TestingStatuses.UNKNOWN.toString());
            messageBean.setSection(true);
            writeLogRecordWithParentSections(getAdapter()::openSection, messageBean);
        } catch (Exception e) {
            log.error("Cannot open section", e);
        }
    }

    /**
     * Open section by Message.
     */
    public void openSection(Message messageBean) {
        try {
            if (isContextEmpty()) {
                log.warn("Context is empty, unable open section {}", messageBean.getName());
                return;
            }
            messageBean.setSection(true);
            messageBean.setName(Strings.isNullOrEmpty(messageBean.getName()) ? SECTION_NAME : messageBean.getName());
            writeLogRecordWithParentSections(getAdapter()::openSection, messageBean);
        } catch (Exception e) {
            log.error("Cannot open section", e);
        }
    }

    public void openItfSection(Message messageBean, JSONObject validationTable) {
        try {
            if (isContextEmpty()) {
                return;
            }
            if (Objects.isNull(messageBean.getName())) {
                messageBean.setName("Itf section");
            }
            messageBean.setTestingStatus(RamConstants.UNKNOWN);
            messageBean.setType(TypeAction.ITF.name());
            messageBean.setSection(true);

            AtpCompaund compaund = context.getAtpCompaund();
            if (Objects.nonNull(compaund) && context.getSections().isEmpty()) {
                log.debug("Compound: {} with parentSection {}", compaund, compaund.getParentSection());
                parentSectionsCount = writeParentSections(compaund);
                this.context = getAdapter().openItfSection(messageBean, validationTable);
            }
        } catch (Exception e) {
            log.error("Cannot open section", e);
        }
    }

    /**
     * Close section.
     */
    public void closeSection() {
        closeSection(new WebReportItem.CloseSection());
    }

    /**
     * Close section.
     */
    public void closeSection(WebReportItem.CloseSection item) {
        try {
            if (isContextEmpty()) {
                return;
            }
            context = getAdapter().closeSection();
            if (!context.getSections().isEmpty() && parentSectionsCount != 0
                    && context.getSections().size() == parentSectionsCount) {
                closeParentSections(parentSectionsCount);
            }
        } catch (Exception e) {
            log.error("Cannot close section", e);
        }
    }

    protected void fillStepStack(Stack<AtpCompaund> stack, AtpCompaund compaund) {
        if (!Strings.isNullOrEmpty(compaund.getSectionId())) {
            stack.push(compaund);
        }
        if (compaund.getParentSection() != null
                && !Strings.isNullOrEmpty(compaund.getParentSection().getSectionId())) {
            fillStepStack(stack, compaund.getParentSection());
        }
    }

    /**
     * Add message section.
     */
    public void message(String title, Level level, String message, SourceProvider page) {
        message(title, level, message, page, null);
    }

    /**
     * Add message section.
     *
     * @param title message title
     * @param level log level
     * @param message message body
     * @param page screenshot or snapshot
     * @param table validation table
     */
    public void message(String title, Level level, String message, SourceProvider page, ValidationTable table) {
        message(title, level, message, page, table,null);
    }

    public void message(String title, Level level, String message, SourceProvider page, ValidationTable table, List<BrowserConsoleLogsTable> browserLogs) {
        try {
            if (isContextEmpty()) {
                return;
            }
            Message messageBean = setMessageParams(title, level, message, page);
            messageBean.setValidationTable(table);
            if(!CollectionUtils.isNotEmpty(browserLogs)) {
                messageBean.setBrowserLogs(browserLogs);
            }
            writeLogRecordWithParentSections(getAdapter()::message, messageBean);
            deleteScreenshotFromPod(messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    public void message(Message message, Level level, SourceProvider page) {
        try {
            if (isContextEmpty()) {
                return;
            }
            message = setMessageParams(message, level, page);
            writeLogRecordWithParentSections(getAdapter()::message, message);
            deleteScreenshotFromPod(message);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    /**
     * Delete screenshot from pod.
     * @param message message bean
     */
    private void deleteScreenshotFromPod(Message message) {
        try {
            if (CollectionUtils.isNotEmpty(message.getAttributes())) {
                message.getAttributes().forEach(
                        x -> {
                            File screenFile = (File)x.get(SCREENSHOT_FILE_KEY);
                            if (screenFile.getName().matches(SCREENSHOT_NAME_REGEX) && screenFile.exists())
                            {
                                screenFile.delete();
                            }
                        }
                );
            }
        } catch (Exception e) {
            log.error("Error during screenshot deleting. Message: {}", message.getMessage(),e);
        }
    }

    /**
     * Add message section by Message.
     */
    public void message(Message messageBean) {
        try {
            if (isContextEmpty()) {
                return;
            }
            writeLogRecordWithParentSections(getAdapter()::message, messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    public void restMessage(Message messageBean, Level level) {
        try {
            if (isContextEmpty()) {
                return;
            }
            setMessageParams(messageBean, level, TypeAction.REST.name());

            writeLogRecordWithParentSections(getAdapter()::restMessage, messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    /**
     * Upload file for Log Record.
     * @param logRecordId Log Record uuid.
     * @param fileContent {@link InputStream} file content.
     * @param fileName file name.
     */
    public void uploadFileForLogRecord(String logRecordId, InputStream fileContent, String fileName) {
        try {
            if (isContextEmpty()) {
                return;
            }
            getAdapter().uploadFileForLogRecord(logRecordId, fileContent, fileName);

        } catch (Exception e) {
            log.error("Cannot upload file '{}' for Log Record {}", fileName, logRecordId, e);
        }
    }

    public void sqlMessage(Message messageBean, Level level) {
        try {
            if (isContextEmpty()) {
                return;
            }
            setMessageParams(messageBean, level, TypeAction.SQL.name());

            writeLogRecordWithParentSections(getAdapter()::sqlMessage, messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    public void sshMessage(Message messageBean, Level level) {
        try {
            if (isContextEmpty()) {
                return;
            }
            setMessageParams(messageBean, level, TypeAction.SSH.name());

            writeLogRecordWithParentSections(getAdapter()::sshMessage, messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    public void bvMessage(Message messageBean, Level level) {
        try {
            if (isContextEmpty()) {
                return;
            }
            setMessageParams(messageBean, level, TypeAction.BV.name());

            writeLogRecordWithParentSections(getAdapter()::bvMessage, messageBean);
        } catch (Exception e) {
            log.error("Cannot log message", e);
        }
    }

    public void writeLogRecordWithParentSections(Function<Message, TestRunContext> writeLogRecord, Message message) {
        AtpCompaund compaund = context.getAtpCompaund();
        if (Objects.nonNull(compaund)) {
            if (message.isSection() && context.getSections().isEmpty()) {
                log.debug("Compound: {} with parentSection {}", compaund, compaund.getParentSection());
                parentSectionsCount = writeParentSections(compaund);

                this.context = writeLogRecord.apply(message);
            } else if (!message.isSection() && context.getSections().isEmpty()) {
                log.debug("Compound: {} with parentSection {}", compaund, compaund.getParentSection());
                parentSectionsCount = writeParentSections(compaund);

                this.context = writeLogRecord.apply(message);
                closeParentSections(parentSectionsCount);
            } else {
                this.context = writeLogRecord.apply(message);
            }
        } else {
            this.context = writeLogRecord.apply(message);
        }
    }

    public void updateMessageTestingStatusAndFiles(String message, TestingStatuses testingStatuses,
                                                   List<SourceProvider> files) {
        Message messageBean = new Message();
        messageBean.setMessage(message);
        messageBean.setTestingStatus(testingStatuses.toString());

        List<FileMetadata> fileMetadataList = new ArrayList<>();
        List<Map<String, Object>> atp2messages = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            files.forEach(file -> {
                Map<String, Object> atp2message = new HashMap<>();
                try {
                    NttAttachment attachment = createAttachment(file, atp2message);
                    if (attachment.getFileMetadata() != null) {
                        fileMetadataList.add(attachment.getFileMetadata());
                    }
                    atp2messages.add(atp2message);
                } catch (Exception e) {
                    log.error("Cannot create attachment", e);
                }
            });
            messageBean.setAttributes(atp2messages);
        }
        messageBean.setFileMetadata(fileMetadataList);
        getAdapter().updateMessageTestingStatusAndFiles(messageBean);
    }

    private Message setMessageParams(String title,Level level,String message,SourceProvider page) {
        Message beanMessage = new Message();
        beanMessage.setMessage(Strings.nullToEmpty(message));
        beanMessage.setName(Strings.isNullOrEmpty(title) ? "Message" : title);
        return setMessageParams(beanMessage, level, page);
    }

    private Message setMessageParams(Message message, Level level, SourceProvider page) {
        setParentSection(message);
        message.setTestingStatus(getStatus(level));
        message.setMessage(Strings.nullToEmpty(message.getMessage()));
        message.setName(Strings.isNullOrEmpty(message.getName()) ? "Message" : message.getName());
        message.setServer(this.context.getQaHost());

        Map<String, Object> atp2message = new HashMap<>();
        if (page != null) {
            try {
                NttAttachment attachment = createAttachment(page, atp2message);
                if (attachment.getFileMetadata() != null) {
                    message.setFileMetadata(Collections.singletonList(attachment.getFileMetadata()));
                }
                if (!Strings.isNullOrEmpty(this.context.getQaHost())
                        && !Strings.isNullOrEmpty(this.context.getQaExternalHost())) {
                    atp2message.put(RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY, attachment.getSnapshotSource()
                            .replaceFirst(this.context.getQaHost(), this.context.getQaExternalHost()));
                }
                message.setAttributes(Collections.singletonList(atp2message));
            } catch (Exception e) {
                log.error("Cannot create attachment", e);
            }
        }
        return message;
    }

    private NttAttachment createAttachment(SourceProvider file, Map<String, Object> atp2message) throws Exception {
        NttAttachment attachment = AttachmentCreator.create(file);
        atp2message.put(RamConstants.SCREENSHOT_NAME_KEY, attachment.getFileName());
        atp2message.put(SCREENSHOT_FILE_KEY, attachment.getFileSource());
        atp2message.put(RamConstants.SCREENSHOT_TYPE_KEY, attachment.getContentType());
        atp2message.put(RamConstants.SCREENSHOT_SOURCE_KEY, attachment.getSnapshotSource());
        return attachment;
    }

    //if the context.sections not empty we set for curr message curr section (not parent compaund section)
    private void setParentSection(Message message) {
        if (!isEmpty(context.getSections())) {
            message.setParentRecordId(context.getSections().peek().getUuid().toString());
        }
    }

    private void setMessageParams(Message message, Level level, String type) {
        if (Objects.isNull(message.getName())) {
            message.setName("Message");
        }
        message.setTestingStatus(getStatus(level));
        message.setType(type);
    }

    private boolean isContextEmpty() {
        if (Strings.isNullOrEmpty(context.getTestRunId())) {
            log.warn("There is no TestRun in TestRunContext");
            return true;
        }
        return false;
    }

    public TestRunContext getContext() {
        return context;
    }

    /**
     * Writes parent section.
     *
     * @param compaund Step from ATP {@link AtpCompaund}
     * @return number parent sections for Step.
     */
    public int writeParentSections(AtpCompaund compaund) {
        int parentSectionsCount = 0;
        if (compaund != null) {
            Stack<AtpCompaund> compaunds = new Stack<>();
            fillStepStack(compaunds, compaund);
            while (!compaunds.isEmpty()) {
                AtpCompaund atpCompaund = compaunds.pop();

                String parentId = atpCompaund.getParentSection() == null
                        ? "" : atpCompaund.getParentSection().getSectionId();
                String id = atpCompaund.getSectionId();
                TestingStatuses statuses = atpCompaund.getTestingStatuses() == null
                        ? TestingStatuses.UNKNOWN : atpCompaund.getTestingStatuses();
                if (Strings.isNullOrEmpty(id)) {
                    continue;
                }
                TypeAction type = atpCompaund.getType() == null ? TypeAction.TECHNICAL : atpCompaund.getType();

                Message message = new Message(id, parentId, atpCompaund.getSectionName(), "",
                        statuses.toString(), String.valueOf(type), compaund.isHidden());
                message.setLastInSection(atpCompaund.isLastInSection());
                message.setStartDate(atpCompaund.getStartDate());
                if (compaunds.isEmpty()) {
                    getAdapter().openCompoundSection(message, true);
                } else {
                    getAdapter().openCompoundSection(message, false);
                }
                parentSectionsCount++;
            }
        }
        return parentSectionsCount;
    }

    private void closeParentSections(int parentSectionsCount) {
        log.debug("CloseParentSections with parentSectionsCount {}", parentSectionsCount);
        if (parentSectionsCount > 0) {
            for (int i = 1; i <= parentSectionsCount; i++) {
                getAdapter().closeSection();
            }
        }
    }

    private String getStatus(Level level) {
        if (level.equals(Level.INFO)) {
            return "PASSED";
        } else if (level.equals(Level.ERROR)) {
            return "FAILED";
        } else if (level.equals(Level.WARN)) {
            return "WARNING";
        } else if (level.equals(Level.OFF)) {
            return "SKIPPED";
        } else if ("BLOCKED".equalsIgnoreCase(level.toString())) {
            return "BLOCKED";
        } else if ("STOPPED".equalsIgnoreCase(level.toString())) {
            return "STOPPED";
        } else {
            log.debug("Level of message: {}", level);
            return "UNKNOWN";
        }
    }

    public void openMiaSection(Message message) {
        try {
            if (isContextEmpty()) {
                return;
            }
            if (Objects.isNull(message.getName())) {
                message.setName("MIA section");
            }
            message.setTestingStatus(RamConstants.UNKNOWN);
            message.setType(TypeAction.MIA.name());
            message.setSection(true);
            writeLogRecordWithParentSections(getAdapter()::openMiaSection, message);
        } catch (Exception e) {
            log.error("Cannot open section", e);
        }
    }



    private static class ReportThreadLocal extends ThreadLocal<AtpRamWriter> {

        private static boolean isReportThreadLocal = true;
        private AtpRamWriter report;

        @Override
        public AtpRamWriter get() {
            if (isReportThreadLocal) {
                return super.get();
            } else {
                return report;
            }
        }

        @Override
        public void set(AtpRamWriter value) {
            if (isReportThreadLocal) {
                super.set(value);
            } else {
                report = value;
            }
        }
    }
}
