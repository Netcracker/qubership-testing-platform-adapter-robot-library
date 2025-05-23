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

package org.qubership.atp.adapter.report;

import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.Utils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class WebReportWriterJoint extends AbstractWebReportWriter {
    private static final Log LOG = LogFactory.getLog(WebReportWriterJoint.class);
    public static final String SUFFIX_KEY = "suffix";
    public static final String REPORT_DIR_KEY = "report.dir";
    /** @deprecated */
    @Deprecated
    public static final String REPORT_DIR_ROOT_KEY = "report.dir.root";
    public static final String REPORT_THREAD_LOCAL_KEY = "report.thread.local";
    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat timeFormat;
    private static final String LOG_REPORT_SNAPSHOTS = "report.log.snapshots";
    private static final String REPORT_JAR_DIR = "config/report/";
    private static ReportThreadLocal report;
    private static ArrayList<File> snapshotFiles;
    private static final boolean SHOW_PAGE_SOURCES;
    private File reportDir;
    private String reportEncoding;
    private boolean logReportSnapshots = false;
    private List<Logger> customLoggers;
    private boolean isInit = false;

    public WebReportWriterJoint() {
        this.createReportDir();
        this.customLoggers = new ArrayList();
        this.customLoggers.add(Logger.getLogger("common"));
    }

    private void createReportDir() {
        Properties p = Utils.readPropertiesFile(new File(Config.getTestPropertiesFilePath()));
        this.logReportSnapshots = Config.getBoolean("report.log.snapshots", false);
        this.reportEncoding = Config.getString("report.encoding", "utf-8");
        changeEncoding(this.reportEncoding);
        String reportDirName = this.getReportDirName(p);
        this.reportDir = new File(this.getReportDirRoot(p), Utils.sanitizeFilename(reportDirName));
    }

    protected void prepareIndexFile() {
        this.indexFile = new File(this.reportDir, "list.html");
        if (!this.indexFile.exists()) {
            Utils.writeStringToFile(this.indexFile, "<head>\n  <base target=\"report\"/>\n  <title>Reports list</title>\n  <script type=\"text/javascript\" src=\"template/list.js\"></script>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"template/list.css\">\n</head>\n<body  onload=\"var parentWindow = parent.window; var parentDoc = parentWindow.document;\nvar indexFrame = parentDoc.getElementById('index');\nvar reportFrame = parentDoc.getElementById('report');\nvar countDiv = document.getElementsByTagName('div').length; if (countDiv > 16.5)\n{countDiv = 16.5}; \tindexFrame.style.height = countDiv*2+3 + '%';\nreportFrame.style.height = 100-(countDiv*2+3) + '%';\"/>");
        }

    }

    public String getResourcesLocationInJar() {
        return "config/report/";
    }

    private ReportLog log() {
        ReportLog log = report.get();
        if (log == null) {
            log = new ReportLog();
            report.set(log);
        }

        return log;
    }

    public synchronized void openLog(String logName) {
        this.openLog(logName, "Unspecified");
    }

    public synchronized void openLog(String logName, String description) {
        synchronized(this) {
            if (!this.isInit) {
                this.prepareReportTemplate();
                this.isInit = true;
            }
        }

        this.log().description = description;
        String fileName;
        synchronized(this) {
            fileName = "report" + String.format("%04d", ResourcesManager.getLogCounter()) + ".xml";
        }

        this.log().currentLogName = logName;
        this.log().currentLogCallStack.clear();
        this.log().timeStack.clear();
        this.log().reportFile = new File(this.reportDir, fileName);
        this.log().reportFile.delete();
        this.appendReportFile("<?xml version=\"1.0\" encoding=\"" + this.reportEncoding + "\"?>\n" + "<?xml-stylesheet type=\"text/xsl\" href=\"template/tree.xsl\"?>\n" + "<LOG date=\"" + dateFormat.format(new Date()) + "\" t=\"" + timeFormat.format(new Date()) + "\">\n" + "<logName><![CDATA[" + logName + "]]></logName>\n");
        this.log().reportFileLength = this.log().reportFile.length();
        this.log().timeStack.push(System.currentTimeMillis());
        this.log().msgId = 0;
        this.log().maxPriorityReported = 20000;
        synchronized(this.indexFile) {
            this.log().indexFileLength = this.indexFile.length();
            this.writeIndex();
        }
    }

    public void closeLog() {
    }

    private void writeIndex() {
        FileChannel indexFileChannel = null;

        try {
            indexFileChannel = (new RandomAccessFile(this.indexFile, "rw")).getChannel();
            byte[] indexBytes = ("<div class=\"" + (new Formatter()).format("%1$5s", Level.toLevel(this.log().maxPriorityReported).toString().toLowerCase()) + "\">" + "<a href=\"" + this.log().reportFile.getName() + "\" " + (this.log().description == null ? "" : (this.log().description.isEmpty() ? "" : "TITLE=\"" + this.log().description.replaceAll("\"", "'") + "\" ")) + " onclick=\"highlight(this);\">" + this.log().currentLogName + "</a>" + "</div>").getBytes();
            indexFileChannel.write(ByteBuffer.wrap(indexBytes), this.log().indexFileLength);
        } catch (ClosedByInterruptException var7) {
            ClosedByInterruptException e = var7;
            LOG.error("Current thread was interrupted", e);
        } catch (IOException var8) {
            IOException e = var8;
            LOG.error("Unable to write to index file", e);
        } finally {
            Utils.close(new Closeable[]{indexFileChannel});
        }

    }

    private void appendTail() {
        if (this.log().reportFile != null) {
            this.log().reportFileLength = this.log().reportFile.length();
            int i = 0;

            for(int size = this.log().currentLogCallStack.size(); i < size; ++i) {
                this.appendReportFile(this.formatMessageClose());
            }

            this.appendReportFile(this.formatMessageTime((Long)this.log().timeStack.lastElement(), System.currentTimeMillis()) + "</LOG>");
            synchronized(this.indexFile) {
                this.writeIndex();
            }
        }
    }

    public synchronized void openSection(String sectionName, String message, SourceProvider page) {
        this.log().currentLogCallStack.push(sectionName);
        this.appendReportFile(this.formatMessageOpen(sectionName, (Level)null, message, page), this.log().reportFileLength);
        Iterator i$ = this.customLoggers.iterator();

        while(i$.hasNext()) {
            Logger logger = (Logger)i$.next();
            logger.info(this.log().currentLogCallStack.toString() + " Section - " + sectionName);
        }

        this.log().timeStack.push(System.currentTimeMillis());
        this.appendTail();
    }

    public synchronized void closeSection() {
        if (!this.log().currentLogCallStack.isEmpty()) {
            this.log().currentLogCallStack.pop();
            this.appendReportFile(this.formatMessageTime((Long)this.log().timeStack.pop(), System.currentTimeMillis()) + this.formatMessageClose(), this.log().reportFileLength);
            this.appendTail();
        }
    }

    public synchronized void message(String title, Level level, String message, SourceProvider page) {
        this.log().maxPriorityReported = Math.max(this.log().maxPriorityReported, level.toInt());
        this.appendReportFile(this.formatMessageOpen(title, level, message, page) + this.formatMessageClose(), this.log().reportFileLength);
        String msg = this.log().currentLogCallStack.toString() + " Title: " + (title == null ? "" : title) + ", Message: " + (message == null ? "" : message);
        Iterator i$ = this.customLoggers.iterator();

        while(i$.hasNext()) {
            Logger customerLogger = (Logger)i$.next();
            customerLogger.log(level, msg);
        }

        this.appendTail();
    }

    private File createSnapshot(SourceProvider page) {
        String pageFileName = "page" + String.format("%04d", ResourcesManager.getPageCounter());
        File pageFileHTML = new File(new File(this.reportDir, "pages"), pageFileName + "." + page.getExtension());
        if (this.logReportSnapshots) {
            snapshotFiles.add(pageFileHTML);
        }

        Utils.writeStringToFile(pageFileHTML, page.getSource());
        return pageFileHTML;
    }

    public ArrayList<File> getSnapshots() {
        return (ArrayList)snapshotFiles.clone();
    }

    private String formatMessageOpen(String title, Level level, String message, SourceProvider page) {
        StringBuilder result = new StringBuilder();
        result.append("<MSG id=\"").append(this.log().msgId++).append("\"");
        result.append(" t=\"").append(timeFormat.format(new Date())).append("\"");
        if (level != null) {
            result.append(" status=\"").append(level.toString().toLowerCase()).append("\"");
        }

        if (page != null) {
            if (SHOW_PAGE_SOURCES) {
                page.setReportType("snapshot");
                File snapShotFile = this.createSnapshot(page);
                result.append(" snapshot=\"").append(snapShotFile.getName()).append("\"");
                page.setReportType("screenshot");
                if (page.getReportType().toLowerCase().equals("screenshot")) {
                    File screenShotFile = this.createSnapshot(page);
                    result.append(" screenshot=\"").append(screenShotFile.getName()).append("\"");
                }
            } else {
                result.append(" snapshot=\"").append(this.createSnapshot(page).getName()).append("\"");
            }
        }

        result.append(">");
        String savedTitle = null;
        int reportMaxLength = Config.getInt("report.title.max.size", 200);
        if (title != null && title.length() > reportMaxLength) {
            savedTitle = "Title: </br>" + StringEscapeUtils.escapeHtml(title) + "</br>";
            title = "Response is too big. Full text of response in description";
        }

        result.append("<title><![CDATA[").append(title != null && title.length() > 0 ? title : "[message]").append("]]></title>");
        if (message != null && message.length() > 0) {
            if (savedTitle != null) {
                result.append("<message>").append("<![CDATA[").append(savedTitle).append("Description: </br>").append(message).append("]]></message>");
            } else {
                result.append("<message><![CDATA[").append(message).append("]]></message>");
            }
        }

        return result.toString();
    }

    private String formatMessageClose() {
        return "</MSG>\n";
    }

    private String formatMessageTime(long startTime, long finishTime) {
        return "<TIME t=\"" + this.getSecondsToTimeFormat(startTime, finishTime) + "\" />";
    }

    private String getSecondsToTimeFormat(long startTime, long finishTime) {
        int secs = Math.round((float)((finishTime - startTime) / 1000L));
        if (secs <= 0) {
            return secs + " second(s)";
        } else {
            int hours = secs / 3600;
            int remainder = secs % 3600;
            int minutes = remainder / 60;
            int seconds = remainder % 60;
            StringBuilder result = new StringBuilder();
            if (hours > 0) {
                result.append((hours < 10 ? "0" : "") + hours).append(":");
            }

            if (minutes > 0 || hours > 0) {
                result.append((minutes < 10 ? "0" : "") + minutes).append(":");
            }

            if (hours <= 0 && minutes <= 0) {
                result.append(seconds);
                if (seconds == 1) {
                    result.append(" second");
                } else {
                    result.append(" seconds");
                }
            } else {
                result.append((seconds < 10 ? "0" : "") + seconds);
            }

            return result.toString();
        }
    }

    public void addLogger(Logger logger) {
        this.customLoggers.add(logger);
    }

    private void appendReportFile(String data) {
        this.appendReportFile(data, -1L);
    }

    private synchronized void appendReportFile(String data, long position) {
        if (this.log().reportFile != null) {
            FileChannel fileChannel = null;
            byte[] dataBytes = data.getBytes();

            try {
                fileChannel = (new RandomAccessFile(this.log().reportFile, "rw")).getChannel();
                if (position > 0L) {
                    fileChannel.write(ByteBuffer.wrap(dataBytes), position);
                    fileChannel.truncate(position + (long)dataBytes.length);
                } else {
                    fileChannel.write(ByteBuffer.wrap(dataBytes), fileChannel.size());
                }
            } catch (IOException var15) {
                IOException e = var15;
                LOG.error("Unable to write to " + this.log().reportFile.getName(), e);
            } finally {
                try {
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                } catch (IOException var14) {
                    IOException e = var14;
                    LOG.error("Unable to close " + this.log().reportFile.getName(), e);
                }

            }

        }
    }

    public File getReportDir() {
        return this.reportDir;
    }

    public Thread getCurrentThread() {
        return Thread.currentThread();
    }

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        report = new ReportThreadLocal();
        snapshotFiles = new ArrayList();
        SHOW_PAGE_SOURCES = Config.getBoolean("report.show.pagesources");
    }

    private static class ReportThreadLocal extends ThreadLocal<ReportLog> {
        private static boolean isReportThreadLocal = Config.getBoolean("report.thread.local", true);
        private ReportLog struct;

        private ReportThreadLocal() {
        }

        public ReportLog get() {
            return isReportThreadLocal ? (ReportLog)super.get() : this.struct;
        }

        public void set(ReportLog value) {
            if (isReportThreadLocal) {
                super.set(value);
            } else {
                this.struct = value;
            }

        }
    }

    private class ReportLog {
        private Stack<String> currentLogCallStack;
        private String currentLogName;
        private int msgId;
        private int maxPriorityReported;
        private Stack<Long> timeStack;
        private String description;
        private File reportFile;
        private long reportFileLength;
        private long indexFileLength;

        private ReportLog() {
            this.currentLogCallStack = new Stack();
            this.currentLogName = null;
            this.msgId = 0;
            this.maxPriorityReported = 20000;
            this.timeStack = new Stack();
            this.description = "";
        }
    }
}

