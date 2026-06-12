/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Level;
import org.qubership.atp.adapter.report.adapter.GeneralReportWriterAdapter;
import org.qubership.atp.adapter.report.adapter.WebReportWriterAdapter;
import org.qubership.atp.adapter.report.adapter.WebReportWriterDiffAdapter;
import org.qubership.atp.adapter.report.adapter.WebReportWriterJointAdapter;
import org.qubership.atp.adapter.report.rmi.RemoteParametersStorage;
import org.qubership.atp.adapter.report.rmi.WebReportWriterWrapperRemote;
import org.qubership.atp.adapter.testcase.Config;

public class Report {
    private static Report instance;
    private final Queue<ReportWriter> reportWriters = new ConcurrentLinkedQueue<>();
    private final Queue<ReportAdapter> reportAdapters = new ConcurrentLinkedQueue<>();
    private static final ThreadLocal<Boolean> init = new ThreadLocal<>();

    private Report() {
    }

    private static void init() {
        if (init.get() == null || !(Boolean)init.get()) {
            if (RemoteParametersStorage.getIsRemote()) {
                if (RemoteParametersStorage.getIsServer()) {
                    WebReportWriterWrapperRemote.initServer();
                } else {
                    innerGetInstance().addAdapter((new WebReportWriterWrapperRemote()).getWebReportAdapterRemote());
                }
            }

            init.set(true);
        }
    }

    private static Report innerGetInstance() {
        if (instance == null) {
            instance = new Report();
        }

        return instance;
    }

    public static Report getReport() {
        init();
        return innerGetInstance();
    }

    public Iterator<ReportWriter> writerIterator() {
        return this.reportWriters.iterator();
    }

    public boolean addWriter(ReportWriter e) {
        return this.reportWriters.add(e);
    }

    public boolean removeWriter(ReportWriter o) {
        return this.reportWriters.remove(o);
    }

    public void removeAllWriters() {
        this.reportWriters.clear();
    }

    public Iterator<ReportAdapter> adapterIterator() {
        return this.reportAdapters.iterator();
    }

    public boolean addAdapter(ReportAdapter e) {
        return this.reportAdapters.add(e);
    }

    public boolean removeAdapter(ReportAdapter o) {
        return this.reportAdapters.remove(o);
    }

    public void removeAllAdapters() {
        this.reportAdapters.clear();
    }

    public void message(Object item) {
        for (ReportAdapter adapter : this.reportAdapters) {
            for (ReportWriter report : this.reportWriters) {
                adapter.write(report, item);
            }
        }
    }

    public void info(String title) {
        this.info(title, title, null);
    }

    public void info(String title, String message) {
        this.info(title, message, null);
    }

    public void info(String title, String message, SourceProvider page) {
        this.info(title, message, null, page);
    }

    public void info(String title, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
        this.message(title, Level.INFO, message, addValues, null, page);
    }

    public void warn(String title) {
        this.warn(title, title, null);
    }

    public void warn(String title, String message) {
        this.warn(title, message, null);
    }

    public void warn(String title, String message, SourceProvider page) {
        this.warn(title, message, null, page);
    }

    public void warn(String title, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
        this.message(title, Level.WARN, message, addValues, null, page);
    }

    public void error(String title) {
        this.error(title, title, null);
    }

    public void error(String title, String message) {
        this.error(title, message, null);
    }

    public void error(String title, Throwable t) {
        this.error(title, t, null);
    }

    public void error(String title, Throwable t, SourceProvider page) {
        this.error(title, "", null, t, page);
    }

    public void error(String title, String message, SourceProvider page) {
        this.error(title, message, null, null, page);
    }

    public void error(String title, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
        this.error(title, message, addValues, null, page);
    }

    public void error(String title, String message, LinkedHashMap<Object, Object> addValues, Throwable t, SourceProvider page) {
        this.message(title, Level.ERROR, message, addValues, t, page);
    }

    private void message(String title, Level level, String message, Throwable throwable, SourceProvider page) {
        this.message(title, level, message, null, throwable, page);
    }

    private void message(String title, Level level, String message, LinkedHashMap<Object, Object> addValues, Throwable throwable, SourceProvider page) {
        this.message(WebReportItem.message(title, level, message, addValues, throwable, page));
    }

    public void openLog(String logName) {
        this.openLog(logName, "");
    }

    public void openLog(String logName, String description) {
        this.message(WebReportItem.openLog(logName, description));
    }

    public void closeLog() {
        this.message(WebReportItem.closeLog());
    }

    public void openSection(String sectionName) {
        this.openSection(sectionName, (String)null, (SourceProvider)null);
    }

    public void openSection(String sectionName, String message, SourceProvider page) {
        this.openSection(sectionName, message, null, page);
    }

    public void openSection(String sectionName, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
        this.message(WebReportItem.openSection(sectionName, message, addValues, page));
    }

    public void closeSection() {
        this.message(WebReportItem.closeSection());
    }

    public File getReportDir() {
        AbstractWebReportWriter abstractWebReportWriter = null;
        for (ReportWriter reportWriter : this.reportWriters) {
            if (reportWriter instanceof WebReportWriterWraper webReportWriterWraper) {
                abstractWebReportWriter = webReportWriterWraper.getWebReportWriter();
            } else if (reportWriter instanceof AbstractWebReportWriter writer) {
                abstractWebReportWriter = writer;
            }
        }
        return abstractWebReportWriter == null ? null : abstractWebReportWriter.getReportDir();
    }

    static {
        if (Config.getBoolean("use.diff.report", false)) {
            getReport().addWriter(new WebReportWriterDiff());
            getReport().addAdapter(new WebReportWriterDiffAdapter());
        } else if (Config.getBoolean("report.thread.joint", false)) {
            getReport().addWriter(new WebReportWriterJoint());
            getReport().addAdapter(new WebReportWriterJointAdapter());
        } else {
            getReport().addWriter(new WebReportWriterWraper());
            getReport().addAdapter(new WebReportWriterAdapter());
        }

        if (Config.getBoolean("report.general.enabled", true)) {
            getReport().addWriter(new GeneralReportWriter());
            getReport().addAdapter(new GeneralReportWriterAdapter());
        }
    }
}

