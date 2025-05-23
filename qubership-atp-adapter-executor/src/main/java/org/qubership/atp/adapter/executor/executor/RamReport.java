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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.executor.executor.items.MiaOpenSectionItem;
import org.qubership.atp.adapter.executor.executor.items.RestMessageItem;
import org.qubership.atp.adapter.executor.executor.items.SqlMessageItem;
import org.qubership.atp.adapter.executor.executor.items.SshMessageItem;
import org.qubership.atp.adapter.executor.executor.items.TechMessageItem;
import org.qubership.atp.adapter.executor.executor.items.UiMessageItem;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.report.SourceProvider;
import org.qubership.atp.adapter.report.WebReportItem;

public class RamReport {
    protected static Report report;
    protected static RamReport ramReport;

    protected RamReport(Report newReport) {
        report = newReport;
    }

    protected static RamReport init() {
        if (Objects.isNull(ramReport)) {
            ramReport = new RamReport(Report.getReport());
        }
        return ramReport;
    }

    public static RamReport getReport() {
        return init();
    }

    /**
     * Start Test Run by id.
     * @param testRunId Test Run id.
     */
    public void openLog(String testRunId) {
        this.openLog(testRunId, "");
    }

    /**
     * Start TestRun by id and description.
     */
    public void openLog(String testRunId, String description) {
        report.message(WebReportItem.openLog(testRunId, description));
    }

    /**
     * Open Log Record Section.
     */
    public void openSection(String name) {
        report.openSection(name);
    }

    /**
     * Close current section.
     */
    public void closeSection() {
        report.closeSection();
    }

    /**
     * Open MiaLogRecord Section.
     */
    public void openMiaSection(String stage, ValidationTable table, boolean isGroup) {
        Message message = new Message();
        message.setValidationTable(table);
        message.setName(stage);
        message.setIsGroup(isGroup);
        report.message(new MiaOpenSectionItem(message));
    }

    public File getReportDir() {
        return report.getReportDir();
    }

    /**
     * Save passed Log Record with message.
     */
    public void info(String message) {
        report.info(StringUtils.EMPTY, message);
    }

    /**
     * Save passed Log Record with name and message.
     */
    public void info(String name, String message) {
        report.info(name, message);
    }

    /**
     * Save passed Log Record with name, message and screenshot.
     */
    public void info(String name, String message, SourceProvider page) {
        report.info(name, message, page);
    }

    /**
     * Save passed Log Record with message and validation table.
     */
    public void info(String message, ValidationTable table) {
        Message beanMessage = new Message();
        beanMessage.setMessage(message);
        beanMessage.setValidationTable(table);
        report.message(new TechMessageItem(beanMessage, Level.INFO, null));
    }

    /**
     * Save Log Record with log level.
     * @param message Container with any fields.
     * @param level Log Records status (Level.INFO = Passed, Level.ERROR = Failed, Level.WARN = Warning,
     *              Level.OFF = Skipped).
     * @param error Error. May be null.
     * @param file File to be saved in RAM. May be null.
     */
    public void log(Message message, Level level, @Nullable Throwable error, @Nullable SourceProvider file) {
        if (error != null) {
            message.setMessage(message.getMessage().concat("\n").concat(getErrorMessage(error)));
        }
        report.message(new TechMessageItem(message, level, file));
    }

    /**
     * Save passed SQL Log Record with query, validation table, result and connection info.
     */
    public void sqlInfo(String query, ValidationTable table, Map<String, List<String>> result,
                        Map<String, String> connectionInfo) {
        Message beanMessage = createSqlMessage(query, table, result, connectionInfo, null);
        report.message(new SqlMessageItem(beanMessage, Level.INFO));
    }

    /**
     * Save passed SQL Log Record with query, result and connection info.
     */
    public void sqlInfo(String query, Map<String, List<String>> result,
                        Map<String, String> connectionInfo) {
        Message beanMessage = createSqlMessage(query, null, result, connectionInfo, null);
        report.message(new SqlMessageItem(beanMessage, Level.INFO));
    }

    /**
     * Save passed SQL Log Record with message, query, validation table, result and connection info.
     */
    public void sqlInfo(String title, String message, String query, ValidationTable table,
                        Map<String, List<String>> result, Map<String, String> connectionInfo) {
        Message beanMessage = createSqlMessage(query, table, result, connectionInfo, title);
        beanMessage.setMessage(message);
        report.message(new SqlMessageItem(beanMessage, Level.INFO));
    }

    /**
     * Save passed SSH Log Record with command, validation output and connection info.
     */
    public void sshInfo(String title, String command, String output, Map<String, String> connectionInfo) {
        Message beanMessage = createSshMessage(title, command, output, connectionInfo);
        report.message(new SshMessageItem(beanMessage, Level.INFO));
    }

    /**
     * Save passed UI Log Record with massage, validation table and screenshot.
     */
    public void uiInfo(String message, ValidationTable table, SourceProvider page) {
        uiInfo(StringUtils.EMPTY, message,table,page);
    }

    /**
     * Save passed UI Log Record with massage, validation table and screenshot.
     */
    public void uiInfo(String title, String message, ValidationTable table, SourceProvider page) {
        uiInfo(title,message,table,page, null,StringUtils.EMPTY);
    }

    /**
     * Save passed UI Log Record with massage, validation table, screenshot and browserLogs.
     *
     * @param message ui report message
     * @param table validation table
     * @param page screenshot/snapshot of page
     * @param browserLogs list of browser logs
     */
    public void uiInfo(String title, String message, ValidationTable table, SourceProvider page, List<BrowserConsoleLogsTable> browserLogs, String browserName) {
        Message beanMessage = new Message();
        beanMessage.setMessage(message);
        beanMessage.setName(title);
        beanMessage.setValidationTable(table);
        beanMessage.setBrowserLogs(browserLogs);
        if (!Objects.isNull(browserName)) {
            beanMessage.setBrowserName(browserName);
        }
        report.message(new UiMessageItem(beanMessage, Level.INFO, page));
    }

    /**
     * Save passed UI Log Record with massage, validation table, screenshot and browserLogs.
     *
     * @param beanMessage message object
     * @param page screenshot/snapshot of page
     */
    public void uiInfo(Message beanMessage, SourceProvider page) {
        report.message(new UiMessageItem(beanMessage,Level.INFO,page));
    }

    /**
     * Save passed REST Log Record.
     */
    public void restInfo(Message message) {
        report.message(new RestMessageItem(message, Level.INFO));
    }

    /**
     * Save error Log Record with message.
     */
    public void error(String message) {
        report.error(StringUtils.EMPTY, message);
    }

    /**
     * Save error Log Record with message and Throwable.
     */
    public void error(String message, Throwable error) {
        report.error(StringUtils.EMPTY, message.concat("\n").concat(getErrorMessage(error)));
    }

    /**
     * Save error Log Record with message, Throwable and screenshot.
     */
    public void error(String message, Throwable error, SourceProvider page) {
        report.error(StringUtils.EMPTY, message.concat("\n").concat(getErrorMessage(error)), page);
    }

    /**
     * Save error Log Record with name, message and screenshot.
     */
    public void error(String name, String message, SourceProvider page) {
        report.error(name, message, null, null, page);
    }

    /**
     * Save error Log Record with message and validation table.
     */
    public void error(String message, ValidationTable table) {
        Message beanMessage = new Message();
        beanMessage.setMessage(message);
        beanMessage.setValidationTable(table);
        report.message(new TechMessageItem(beanMessage, Level.ERROR, null));
    }

    /**
     * Save error SQL Log Record with query, message and connection info.
     */
    public void sqlError(String message, String query, Map<String, String> connectionInfo) {
        sqlError(null, message, query, null, null, connectionInfo);
    }

    /**
     * Save error SQL Log Record with query, validation table and connection info.
     */
    public void sqlError(String query, ValidationTable table, Map<String, String> connectionInfo) {
        sqlError(null, StringUtils.EMPTY, query, table, null, connectionInfo);
    }

    /**
     * Save error SQL Log Record with query, connection info and Throwable.
     */
    public void sqlError(String query, Map<String, String> connectionInfo, Throwable error) {
        sqlError(null, getErrorMessage(error), query, null, null, connectionInfo);
    }

    /**
     * Save error SQL Log Record with message, query, validation table, result and connection info.
     */
    public void sqlError(String title, String message, String query, ValidationTable table,
                         Map<String, List<String>> result, Map<String, String> connectionInfo) {
        Message beanMessage = createSqlMessage(query, table, result, connectionInfo, title);
        beanMessage.setMessage(message);
        report.message(new SqlMessageItem(beanMessage, Level.ERROR));
    }

    /**
     * Save error SSH Log Record with command and connection info.
     */
    public void sshError(String command, Map<String, String> connectionInfo) {
        Message beanMessage = createSshMessage(null, command, null, connectionInfo);
        report.message(new SshMessageItem(beanMessage, Level.ERROR));
    }

    /**
     * Save error SSH Log Record with command, output and connection info.
     */
    public void sshError(String title, String message, String command, String output, Map<String, String> connectionInfo) {
        Message beanMessage = createSshMessage(title, command, output, connectionInfo);
        beanMessage.setMessage(message);
        report.message(new SshMessageItem(beanMessage, Level.ERROR));
    }

    /**
     * Save error SSH Log Record with command, connection info and Throwable.
     */
    public void sshError(String command, Map<String, String> connectionInfo, Throwable error) {
        Message beanMessage = createSshMessage(null, command, null, connectionInfo);
        beanMessage.setMessage(getErrorMessage(error));
        report.message(new SshMessageItem(beanMessage, Level.ERROR));
    }

    /**
     * Save error UI Log Record with message, validation table and screenshot.
     */
    public void uiError(String message, ValidationTable table, SourceProvider page) {
        uiError(StringUtils.EMPTY,message, table, page,null, null);
    }

    /**
     * Save error UI Log Record with massage, validation table, screenshot and browserLogs.
     *
     * @param title message title
     * @param message ui report message
     * @param table validation table
     * @param page screenshot/snapshot of page
     * @param browserLogs list of browser logs
     */
    public void uiError(String title, String message, ValidationTable table, SourceProvider page, List<BrowserConsoleLogsTable> browserLogs, String browserName) {
        Message beanMessage = new Message();
        beanMessage.setName(title);
        beanMessage.setMessage(message);
        beanMessage.setValidationTable(table);
        if (!Objects.isNull(browserName)) {
            beanMessage.setBrowserName(browserName);
        }
        beanMessage.setBrowserLogs(browserLogs);
        report.message(new UiMessageItem(beanMessage, Level.ERROR, page));
    }

    /**
     * Save error UI Log Record with massage, validation table, screenshot and browserLogs.
     *
     * @param beanMessage message object
     * @param page screenshot/snapshot of page
     */
    public void uiError(Message beanMessage, SourceProvider page) {
        report.message(new UiMessageItem(beanMessage,Level.ERROR,page));
    }

    /**
     * Save error UI Log Record with massage, validation table and screenshot.
     *
     * @param message ui report message
     * @param error Exception object
     * @param page Screenshot/snapshot
     */
    public void uiError(String message, Throwable error, SourceProvider page) {
        uiError(StringUtils.EMPTY, message.concat("\n").concat(getErrorMessage(error)), null, page, null,null);
    }

    /**
     * Save error REST Log Record.
     */
    public void restError(Message message) {
        report.message(new RestMessageItem(message, Level.ERROR));
    }

    /**
     * Save warn Log Record with message.
     */
    public void warn(String message) {
        report.warn(StringUtils.EMPTY, message, null);
    }

    /**
     * Save warn Log Record with name, message and screenshot.
     */
    public void warn(String name, String message, SourceProvider page) {
        report.warn(name, message, page);
    }

    /**
     * Save warn SSH Log Record with command, output and connection info.
     */
    public void sshWarn(String title, String command, String output, Map<String, String> connectionInfo) {
        Message beanMessage = createSshMessage(title, command, output, connectionInfo);
        report.message(new SshMessageItem(beanMessage, Level.WARN));
    }

    /**
     * Save warn Log Record with message, validation table and screenshot.
     */
    public void uiWarn(String message, ValidationTable table, SourceProvider page) {
        uiWarn(StringUtils.EMPTY, message,table,page,null,null);
    }

    /**
     * Save info UI Log Record with message, validation table, screenshot and browser name.
     *
     * @param message Log record message
     * @param table Validation table
     * @param page Screenshot or snapshot
     * @param browserName Browser pod name
     */
    public void uiWarn(String title, String message, ValidationTable table, SourceProvider page, List<BrowserConsoleLogsTable> browserLogs, String browserName) {
        Message beanMessage = new Message();
        beanMessage.setName(title);
        beanMessage.setMessage(message);
        beanMessage.setValidationTable(table);
        if (!Objects.isNull(browserName)) {
            beanMessage.setBrowserName(browserName);
        }
        beanMessage.setBrowserLogs(browserLogs);
        report.message(new UiMessageItem(beanMessage, Level.WARN, page));
    }

    /**
     * Save warn UI Log Record with massage, validation table, screenshot and browserLogs.
     *
     * @param beanMessage message object
     * @param page screenshot/snapshot of page
     */
    public void uiWarn(Message beanMessage, SourceProvider page) {
        report.message(new UiMessageItem(beanMessage,Level.WARN,page));
    }

    /**
     * Save warn SQL Log Record with message, query, validation table, result and connection info.
     */
    public void sqlWarn(String title, String message, String query, ValidationTable table,
                        Map<String, List<String>> result, Map<String, String> connectionInfo) {
        Message beanMessage = createSqlMessage(query, table, result, connectionInfo, title);
        beanMessage.setMessage(message);
        report.message(new SqlMessageItem(beanMessage, Level.WARN));
    }

    /**
     * Log item.
     */
    public void message(WebReportItem item) {
        report.message(item);
    }

    /**
     * Update current mia section.
     */
    @Deprecated
    public void updateCurrentSectionWithMessageAndTestingStatus(String message,
                                                                TestingStatuses testingStatuses) {
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        TestRunContext context = writer.getAdapter().getContext();
        if (context!=null) {
            String sectionId = context.getAtpCompaund().getSectionId();
            writer.getAdapter().updateMessageAndTestingStatus(sectionId, message, testingStatuses.toString());
        }
    }

    /**
     * Update current mia section with message, testing status and files.
     */
    public void updateCurrentSectionWithMessageTestingStatusAndFiles(String message, TestingStatuses testingStatuses,
                                                                     List<SourceProvider> files) {
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        writer.updateMessageTestingStatusAndFiles(message, testingStatuses, files);
    }

    /**
     * Update current mia (rest) section.
     */
    public void updateCurrentSectionWithMessageTestingStatusRequestAndResponse(String message,
                                                                               TestingStatuses testingStatuses,
                                                                               Request request, Response response) {
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        TestRunContext context = writer.getAdapter().getContext();
        if (context!=null) {
            String sectionId = context.getAtpCompaund().getSectionId();
            writer.getAdapter().updateMessageTestingStatusRequestAndResponse(sectionId, message,
                    testingStatuses.toString(), request, response);
        }
    }

    private Message createSqlMessage(String query, ValidationTable table, Map<String, List<String>> result,
                                     Map<String, String> connectionInfo, String title) {
        Message beanMessage = new Message();
        beanMessage.setCommand(query);
        beanMessage.setName(title);
        beanMessage.setValidationTable(table);
        beanMessage.setResult(result);
        beanMessage.setConnectionInfo(connectionInfo);
        return beanMessage;
    }

    private Message createSshMessage(String title, String command, String output, Map<String, String> connectionInfo) {
        Message beanMessage = new Message();
        beanMessage.setName(title);
        beanMessage.setCommand(command);
        beanMessage.setOutput(output);
        beanMessage.setConnectionInfo(connectionInfo);
        return beanMessage;
    }

    protected String getErrorMessage(Throwable throwable) {
        return throwable.toString().concat("\n").concat(StringUtils.join(throwable.getStackTrace(), '\n'));
    }
}
