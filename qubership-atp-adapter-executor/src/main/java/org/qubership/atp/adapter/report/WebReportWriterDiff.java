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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class WebReportWriterDiff extends AbstractWebReportWriter {
    private static final Log LOG = LogFactory.getLog(WebReportWriterDiff.class);
    public static final String SUFFIX_KEY = "suffix";
    public static final String REPORT_DIR_KEY = "report.dir";
    /** @deprecated */
    @Deprecated
    public static final String REPORT_DIR_ROOT_KEY = "report.dir.root";
    public static final String REPORT_THREAD_LOCAL_KEY = "report.thread.local";
    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;
    private static AtomicInteger PAGE_COUNTER;
    private static AtomicInteger LOG_COUNTER;
    private static ReportThreadLocal REPORT;
    private static String REPORT_JAR_DIR;
    private String reportEncoding;
    private File reportDir;
    private List<Logger> customLoggers;
    private String originContent;
    private String originName;
    private boolean isInit = false;

    public WebReportWriterDiff() {
        this.init();
        this.indexFile = new File(this.reportDir, "list.html");
    }

    public void init() {
        Properties p = Utils.readPropertiesFile(new File(Config.getTestPropertiesFilePath()));
        this.reportEncoding = Config.getString("report.encoding", "utf-8");
        changeEncoding(this.reportEncoding);
        String reportDirName = this.getReportDirName(p);
        this.reportDir = new File(this.getReportDir(), Utils.sanitizeFilename(reportDirName));
        this.customLoggers = new ArrayList();
        this.customLoggers.add(Logger.getLogger("common"));
    }

    protected void prepareIndexFile() {
        this.indexFile = new File(this.reportDir, "list.html");
        if (!this.indexFile.exists()) {
            Utils.writeStringToFile(this.indexFile, "<head>\n  <base target=\"report\"/>\n  <title>Reports list</title>\n  <script type=\"text/javascript\" src=\"template/list.js\"></script>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"template/list.css\">\n</head>\n<body  onload=\"var parentWindow = parent.window; var parentDoc = parentWindow.document;\nvar parentFrameSet1 = parentDoc.getElementsByName('index')[0];\nvar parentFrameSet2 = parentDoc.getElementsByName('report')[0];\nvar countDiv = document.getElementsByTagName('div').length; if (countDiv > 16.5)\n{countDiv = 16.5};  parentFrameSet1.setAttribute('height',  countDiv * 2 +'%, *');\nparentFrameSet2.setAttribute('height',  100 - countDiv * 2 +'%, *');\nparent.window.frames['settings'].document.getElementsByName('expand_problem')[0].checked = false\"/>");
        }

        File snapshotFile = new File(this.reportDir, "template\\snapshot.html");
        if (!snapshotFile.exists()) {
            Utils.writeStringToFile(snapshotFile, "<!DOCTYPE html>\n<html>\n\t<head>\n\t\t<base target=\"_blank\" href=\"" + Config.getString(Config.getString("server.alias") + ".server") + "\"/>\n\t</head>\n\t<body>\n\t\t" + "<div style=\"position:absolute; right:5px; width:99%; height: 97%;\">\n\t\t" + "\t<iframe id=\"snapshot\" height=\"100%\" width=\"100%\" seamless =\"\"></iframe>\n\t\t" + "</div>\n\t</body>\n</html>");
        }

    }

    public String getResourcesLocationInJar() {
        return REPORT_JAR_DIR;
    }

    private ReportLog log() {
        ReportLog log = REPORT.get();
        if (log == null) {
            log = new ReportLog();
            REPORT.set(log);
        }

        return log;
    }

    public synchronized void openLog(String logName) {
        this.openLog(logName, "Unspecified");
    }

    public synchronized void openLog(String logName, String description) {
        synchronized(this) {
            if (!this.isInit) {
                this.isInit = true;
                this.prepareReportTemplate();
            }
        }

        this.log().description = description;
        String filename;
        synchronized(this) {
            filename = String.format("report%04d", LOG_COUNTER.incrementAndGet()) + ".xml";
        }

        this.log().currentLogName = logName;
        this.log().currentLogCallStack.clear();
        this.log().timeStack.clear();
        this.log().reportFile = new File(this.reportDir, filename);
        this.log().reportFile.delete();
        this.appendReportFile("<?xml version=\"1.0\" encoding=\"" + this.reportEncoding + "\"?>\n" + "<?xml-stylesheet type=\"text/xsl\" href=\"template/tree.xsl\"?>\n" + "<LOG date=\"" + DATE_FORMAT.format(new Date()) + "\" t=\"" + TIME_FORMAT.format(new Date()) + "\">\n" + "<logName><![CDATA[" + logName + "]]></logName>\n");
        this.log().reportFileLength = this.log().reportFile.length();
        this.log().timeStack.push(System.currentTimeMillis());
        this.log().msgId = 0;
        this.log().maxPriorityReported = 20000;
        synchronized(this.indexFile) {
            this.log().indexPosition = this.indexFile.length();
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
            indexFileChannel.write(ByteBuffer.wrap(indexBytes), this.log().indexPosition);
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
            logger.info(this.log().currentLogCallStack.toString() + "Section - " + sectionName);
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

    private String createSnapshot(SourceProvider page) {
        Long startTime = System.currentTimeMillis();
        String pageFilename = "page" + String.format("%04d", PAGE_COUNTER.incrementAndGet());
        File pageFileHTML = new File(new File(this.reportDir, "pages"), pageFilename + "." + page.getExtension());
        if (this.originName == null) {
            this.originName = pageFileHTML.getName();
            this.originContent = page.getSource();
            Utils.writeStringToFile(pageFileHTML, this.originContent);
        } else if ("xml".equals(page.getExtension())) {
            Utils.writeStringToFile(pageFileHTML, page.getSource());
        } else {
            DiffMatchPatch dfp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> difs = dfp.diff_main(this.originContent, page.getSource());
            String textDiff = dfp.diff_toDelta(difs);
            if (textDiff.length() >= page.getSource().length()) {
                this.originName = pageFileHTML.getName();
                this.originContent = page.getSource();
                Utils.writeStringToFile(pageFileHTML, this.originContent);
            } else {
                File pageFileDiff = new File(new File(this.reportDir, "pages"), pageFilename + ".diff");
                Utils.writeStringToFile(pageFileDiff, textDiff);
                pageFileHTML = pageFileDiff;
            }
        }

        return pageFileHTML.getName();
    }

    private String formatMessageOpen(String title, Level level, String message, SourceProvider page) {
        StringBuilder result = new StringBuilder();
        result.append("<MSG id=\"").append(this.log().msgId++).append("\"");
        result.append(" t=\"").append(TIME_FORMAT.format(new Date())).append("\"");
        if (level != null) {
            result.append(" status=\"").append(level.toString().toLowerCase()).append("\"");
        }

        String savedTitle;
        if (page != null) {
            savedTitle = this.createSnapshot(page);
            result.append(" snapshot=\"").append(savedTitle).append("\"");
            result.append(" baseversion=\"").append(this.originName).append("\"");
        }

        result.append(">");
        savedTitle = null;
        int reportMaxLength = Config.getInt("report.title.max.size", 200);
        if (title != null && title.length() > reportMaxLength) {
            savedTitle = "Title: </br>" + StringEscapeUtils.escapeHtml(title) + "</br>";
            title = "Response is too big. Full text of respons in description";
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
        return "<TIME t=\"" + (finishTime - startTime) / 1000L + "\" />";
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
            } catch (ClosedByInterruptException var17) {
                ClosedByInterruptException e = var17;
                LOG.error("Current thread was interrupted", e);
            } catch (IOException var18) {
                IOException e = var18;
                LOG.error("Unable to write to " + this.log().reportFile.getName(), e);
            } finally {
                try {
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                } catch (IOException var16) {
                    IOException e = var16;
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
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);
        PAGE_COUNTER = new AtomicInteger(0);
        LOG_COUNTER = new AtomicInteger(0);
        REPORT = new ReportThreadLocal();
        REPORT_JAR_DIR = "config/report_diff/";
    }

    private static class ReportThreadLocal extends ThreadLocal<ReportLog> {
        private static boolean IS_REPORT_THREAD_LOCAL = Config.getBoolean("report.thread.local", true);
        private ReportLog struct;

        private ReportThreadLocal() {
        }

        public ReportLog get() {
            return IS_REPORT_THREAD_LOCAL ? (ReportLog)super.get() : this.struct;
        }

        public void set(ReportLog value) {
            if (IS_REPORT_THREAD_LOCAL) {
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
        private long indexPosition;

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

