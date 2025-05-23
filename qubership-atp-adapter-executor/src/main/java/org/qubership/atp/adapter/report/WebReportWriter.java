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
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class WebReportWriter extends AbstractWebReportWriter {
    private static final Log log = LogFactory.getLog(WebReportWriter.class);
    public static final String SUFFIX_KEY = "suffix";
    public static final String REPORT_DIR_KEY = "report.dir";
    /** @deprecated */
    @Deprecated
    public static final String REPORT_DIR_ROOT_KEY = "report.dir.root";
    public static final String REPORT_CHANGING_NAMES_THREADS_FOLDER = "report.changing.names.threads.folder";
    public static final String REPORT_THREAD_LOCAL_KEY = "report.thread.local";
    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat timeFormat;
    private static final String LOG_REPORT_SNAPSHOTS = "report.log.snapshots";
    private static final String SPLIT_REPORT = "report.split.enable";
    private static final boolean SHOW_PAGE_SOURCES;
    private static ReportThreadLocal report;
    private static ArrayList<File> snapshotFiles;
    private static ThreadLocal<String> reportFiles;
    private String reportEncoding;
    private Stack<String> currentLogCallStack = new Stack();
    private String currentLogName = null;
    private int msgId = 0;
    private int maxPriorityReported = 20000;
    private Stack<Long> timeStack = new Stack();
    private File reportFile;
    private long reportFileLength;
    private long indexPosition;
    private File reportDir;
    private boolean logReportSnapshots = false;
    private String description = "";
    private List<Logger> customLoggers;
    private int logId = 0;
    private int pageId = 0;
    private int reportCounter = 0;
    private boolean isInit = false;
    private static String reportJarDir;
    private String threadName = Thread.currentThread().getName();

    public String getThreadName() {
        return this.threadName;
    }

    public WebReportWriter(Properties p) {
        this.createReportDir(p);
        this.customLoggers = new ArrayList();
        this.customLoggers.add(Logger.getLogger("common"));
    }

    private void createReportDir(Properties p) {
        this.logReportSnapshots = Config.getBoolean("report.log.snapshots", false);
        this.reportEncoding = Config.getString("report.encoding", "utf-8");
        changeEncoding(this.reportEncoding);
        String reportDirname = this.getReportDirName(p);
        this.reportDir = new File(this.getReportDirRoot(p), Utils.sanitizeFilename(reportDirname));
    }

    protected void prepareIndexFile() {
        this.indexFile = new File(this.reportDir, "list.html");
        if (!this.indexFile.exists()) {
            Utils.writeStringToFile(this.indexFile, "<head>\n  <base target=\"report\"/>\n  <title>Reports list</title>\n  <script type=\"text/javascript\" src=\"template/list.js\"></script>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"template/list.css\">\n</head>\n<body  onload=\"var parentWindow = parent.window; var parentDoc = parentWindow.document;\nvar indexFrame = parentDoc.getElementById('index');\nvar reportFrame = parentDoc.getElementById('report');\nvar countDiv = document.getElementsByTagName('div').length; if (countDiv > 16.5)\n{countDiv = 16.5}; \tindexFrame.style.height = countDiv*2+3 + '%';\nreportFrame.style.height = 100-(countDiv*2+3) + '%';\"/>");
        }

    }

    protected String getReportDirName(Properties p) {
        String reportDirname = super.getReportDirName(p);
        String threadName = Thread.currentThread().getName();
        if (!"main".equals(threadName) && WebReportWriter.ReportThreadLocal.isReportThreadLocal) {
            reportDirname = reportDirname + "_" + threadName + "_" + Thread.currentThread().getId();
        }

        if (Config.getBoolean("report.split.enable")) {
            reportDirname = reportDirname + "_" + this.reportCounter;
            ++this.reportCounter;
        }

        return reportDirname;
    }

    public String getResourcesLocationInJar() {
        return reportJarDir;
    }

    public static void init(Properties p) {
        report.set(new WebReportWriter(p));
    }

    private static WebReportWriter reportToThreadlocal(WebReportWriter report) {
        report = new WebReportWriter((Properties)null);
        WebReportWriter.report.set(report);
        return report;
    }

    public static WebReportWriter getWebReportWriter() {
        WebReportWriter report = WebReportWriter.report.get();
        if (report == null) {
            report = reportToThreadlocal(report);
        } else if (report != null && !report.getThreadName().equals(Thread.currentThread().getName()) && Config.getBoolean("report.changing.names.threads.folder")) {
            report = reportToThreadlocal(report);
        }

        return report;
    }

    public synchronized void openLog(String logName) {
        this.openLog(logName, "Unspecified");
    }

    public synchronized void openLog(String logName, String description) {
        if (Boolean.parseBoolean(Config.getString("report.split.enable"))) {
            this.createReportDir((Properties)null);
        }

        if (!this.isInit || Boolean.parseBoolean(Config.getString("report.split.enable"))) {
            this.prepareReportTemplate();
            this.indexFile = new File(this.reportDir, "list.html");
            if (!this.indexFile.exists()) {
                Utils.writeStringToFile(this.indexFile, "<head>\n  <base target=\"report\"/>\n  <title>Reports list</title>\n  <script type=\"text/javascript\" src=\"template/list.js\"></script>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"template/list.css\">\n</head>\n<body  onload=\"var parentWindow = parent.window; var parentDoc = parentWindow.document;\nvar parentFrameSet = parentDoc.getElementsByTagName('frameset')[1];\nvar countDiv = document.getElementsByTagName('div').length; if (countDiv > 16.5)\n{countDiv = 16.5}; parentFrameSet.setAttribute('rows', 28 + countDiv * 16 +'px, *');\nparent.window.frames['settings'].document.getElementsByName('expand_problem')[0].checked = false\"/>");
            }

            this.isInit = true;
        }

        this.description = description;
        String filename = "report" + String.format("%04d", this.logId++) + ".xml";
        this.currentLogName = logName;
        this.currentLogCallStack.clear();
        this.timeStack.clear();
        this.reportFile = new File(this.reportDir, filename);
        this.reportFile.delete();
        reportFiles.set(this.reportFile.getAbsolutePath());
        this.appendReportFile("<?xml version=\"1.0\" encoding=\"" + this.reportEncoding + "\"?>\n" + "<?xml-stylesheet type=\"text/xsl\" href=\"template/tree.xsl\"?>\n" + "<LOG date=\"" + dateFormat.format(new Date()) + "\" t=\"" + timeFormat.format(new Date()) + "\">\n" + "<logName id='" + this.logId + "'><![CDATA[" + logName + "]]></logName>\n");
        this.reportFileLength = this.reportFile.length();
        this.timeStack.push(System.currentTimeMillis());
        this.msgId = 0;
        this.maxPriorityReported = 20000;
        this.indexPosition = this.indexFile.length();
    }

    public void closeLog() {
    }

    private void appendTail() {
        if (this.reportFile != null) {
            this.reportFileLength = this.reportFile.length();
            int i = 0;

            for(int size = this.currentLogCallStack.size(); i < size; ++i) {
                this.appendReportFile(this.formatMessageClose());
            }

            this.appendReportFile(this.formatMessageTime((Long)this.timeStack.lastElement(), System.currentTimeMillis()) + "</LOG>");
            FileChannel indexFileChannel = null;

            try {
                indexFileChannel = (new RandomAccessFile(this.indexFile, "rw")).getChannel();
                byte[] indexBytes = ("<div id='" + this.logId + "' class=\"" + Level.toLevel(this.maxPriorityReported).toString().toLowerCase() + "\">" + "<a href=\"" + this.reportFile.getName() + "\" " + (this.description == null ? "" : (this.description.isEmpty() ? "" : "TITLE=\"" + this.description.replaceAll("\"", "'") + "\" ")) + " onclick=\"highlight(this);\">" + this.currentLogName + "</a>" + "</div>").getBytes();
                indexFileChannel.write(ByteBuffer.wrap(indexBytes), this.indexPosition);
                indexFileChannel.truncate(this.indexPosition + (long)indexBytes.length);
            } catch (IOException var6) {
                log.error("Unable to write to index file", var6);
            } finally {
                Utils.close(new Closeable[]{indexFileChannel});
            }

        }
    }

    public synchronized void openSection(String sectionName, String message, SourceProvider page, LinkedHashMap<Object, Object> addValues) {
        this.currentLogCallStack.push(sectionName);
        this.appendReportFile(this.formatMessageOpen(sectionName, (Level)null, message, page, addValues), this.reportFileLength);
        Iterator i$ = this.customLoggers.iterator();

        while(i$.hasNext()) {
            Logger logger = (Logger)i$.next();
            logger.info(this.currentLogCallStack.toString() + " Section - " + sectionName);
        }

        this.timeStack.push(System.currentTimeMillis());
        this.appendTail();
    }

    public synchronized void closeSection() {
        if (!this.currentLogCallStack.isEmpty()) {
            this.currentLogCallStack.pop();
            this.appendReportFile(this.formatMessageTime((Long)this.timeStack.pop(), System.currentTimeMillis()) + this.formatMessageClose(), this.reportFileLength);
            this.appendTail();
        }
    }

    public synchronized void message(String title, Level level, String message, SourceProvider page) {
        this.message(title, level, message, page, (LinkedHashMap)null);
    }

    public synchronized void message(String title, Level level, String message, SourceProvider page, LinkedHashMap<Object, Object> addValues) {
        this.maxPriorityReported = Math.max(this.maxPriorityReported, level.toInt());
        this.appendReportFile(this.formatMessageOpen(title, level, message, page, addValues) + this.formatMessageClose(), this.reportFileLength);
        String msg = this.currentLogCallStack.toString() + " Title: " + (title == null ? "" : title) + ", Message: " + (message == null ? "" : message);
        Iterator i$ = this.customLoggers.iterator();

        while(i$.hasNext()) {
            Logger customerLogger = (Logger)i$.next();
            customerLogger.log(level, msg);
        }

        this.appendTail();
    }

    private File createSnapshot(SourceProvider page) {
        String pageFilename = "page" + String.format("%04d", this.pageId++);
        File pageFileHTML = new File(new File(this.reportDir, "pages"), pageFilename + "." + page.getExtension());
        if (this.logReportSnapshots) {
            snapshotFiles.add(pageFileHTML);
        }

        Utils.writeStringToFile(pageFileHTML, page.getSource());
        return pageFileHTML;
    }

    public ArrayList<File> getSnapshots() {
        return (ArrayList)snapshotFiles.clone();
    }

    private String formatMessageOpen(String title, Level level, String message, SourceProvider page, LinkedHashMap<Object, Object> addValues) {
        StringBuilder result = new StringBuilder();
        result.append("<MSG id=\"").append(this.msgId++).append("\"");
        result.append(" t=\"").append(timeFormat.format(new Date())).append("\"");
        if (level != null) {
            result.append(" status=\"").append(level.toString().toLowerCase()).append("\"");
        }

        if (page != null) {
            if (SHOW_PAGE_SOURCES) {
                page.setReportType(ReportType.SNAPSHOT.toString());
                File snapShotFile = this.createSnapshot(page);
                result.append(" snapshot=\"").append(snapShotFile.getName()).append("\"");
                page.setReportType(ReportType.SCREENSHOT.toString());
                if (page.getReportType().equals(ReportType.SCREENSHOT.toString())) {
                    File screenShotFile = this.createSnapshot(page);
                    result.append(" screenshot=\"").append(screenShotFile.getName()).append("\"");
                }
            } else {
                result.append(" snapshot=\"").append(this.createSnapshot(page).getName()).append("\"");
            }
        }

        if (addValues != null) {
            Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]*");
            Iterator i$ = addValues.keySet().iterator();

            while(i$.hasNext()) {
                Object keyO = i$.next();
                String key;
                if (!(key = NON_ALPHANUMERIC.matcher(keyO.toString()).replaceAll("")).isEmpty()) {
                    result.append(String.format(" %s=\"%s\"", key, addValues.get(keyO).toString()));
                }
            }
        }

        result.append(">");
        result.append("<title><![CDATA[").append(title != null && title.length() > 0 ? title : "[message]").append("]]></title>");
        result.append("<message><![CDATA[").append(message).append("]]></message>");
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

    private void appendReportFile(String data, long position) {
        if (this.reportFile != null) {
            FileChannel fileChannel = null;
            byte[] dataBytes = data.getBytes();

            try {
                fileChannel = (new RandomAccessFile(this.reportFile, "rw")).getChannel();
                if (position > 0L) {
                    fileChannel.write(ByteBuffer.wrap(dataBytes), position);
                    fileChannel.truncate(position + (long)dataBytes.length);
                } else {
                    fileChannel.write(ByteBuffer.wrap(dataBytes), fileChannel.size());
                }
            } catch (IOException var15) {
                IOException e = var15;
                log.error("Unable to write to " + this.reportFile.getName(), e);
            } finally {
                try {
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                } catch (IOException var14) {
                    IOException e = var14;
                    log.error("Unable to close " + this.reportFile.getName(), e);
                }

            }

        }
    }

    public File getReportDir() {
        return this.reportDir;
    }

    public static String getReportFilePath() {
        getWebReportWriter();
        if (reportFiles.get() == null) {
            return "";
        } else {
            getWebReportWriter();
            return ((String)reportFiles.get()).replaceAll("report[0-9]*.xml", "report.html");
        }
    }

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        SHOW_PAGE_SOURCES = Config.getBoolean("report.show.pagesources");
        report = new ReportThreadLocal();
        snapshotFiles = new ArrayList();
        reportFiles = new ThreadLocal();
        reportJarDir = "config/report/";
    }

    private static class ReportThreadLocal extends ThreadLocal<WebReportWriter> {
        private static boolean isReportThreadLocal = Config.getBoolean("report.thread.local", true);
        private WebReportWriter report;

        private ReportThreadLocal() {
        }

        public WebReportWriter get() {
            return isReportThreadLocal ? (WebReportWriter)super.get() : this.report;
        }

        public void set(WebReportWriter value) {
            if (isReportThreadLocal) {
                super.set(value);
            } else {
                this.report = value;
            }

        }
    }
}

