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

package org.qubership.atp.adapter.executor.executor;

import static org.qubership.atp.adapter.report.WebReportItem.CloseSection;
import static org.qubership.atp.adapter.report.WebReportItem.OpenLog;
import static org.qubership.atp.adapter.report.WebReportItem.OpenSection;

import org.apache.log4j.Level;
import org.qubership.atp.adapter.executor.executor.items.BvMessageItem;
import org.qubership.atp.adapter.executor.executor.items.CreateContextItem;
import org.qubership.atp.adapter.executor.executor.items.ItfOpenSectionItem;
import org.qubership.atp.adapter.executor.executor.items.MiaOpenSectionItem;
import org.qubership.atp.adapter.executor.executor.items.RestMessageItem;
import org.qubership.atp.adapter.executor.executor.items.SqlMessageItem;
import org.qubership.atp.adapter.executor.executor.items.SshMessageItem;
import org.qubership.atp.adapter.executor.executor.items.TechMessageItem;
import org.qubership.atp.adapter.executor.executor.items.UiMessageItem;
import org.qubership.atp.adapter.keyworddriven.actions.view.ActionMessage;
import org.qubership.atp.adapter.report.ReportAdapter;
import org.qubership.atp.adapter.report.ReportWriter;
import org.qubership.atp.adapter.report.SourceProvider;
import org.qubership.atp.adapter.report.WebReportItem;
import org.qubership.atp.adapter.report.WebReportItem.CloseLog;
import org.qubership.atp.adapter.utils.Utils;

public class AtpRamWriterAdapter implements ReportAdapter {

    @Override
    public void write(ReportWriter wr, Object item) {
        if (wr instanceof AtpRamWriterWraper wrapper) {
            AtpRamWriter writer = wrapper.getAtpRamWriter();
            if (item instanceof OpenLog i) {
                writer.openLog(i.getLogName(), i.getDescription());
            }
            if (item instanceof CreateContextItem i) {
                writer.createContext(i.getTestRunId());
            }
            if (item instanceof CloseLog i) {
                writer.closeLog(i);
            }
            if (item instanceof OpenSection i) {
                writer.openSection(i);
            }
            if (item instanceof WebReportItem.Message msg) {
                StringBuilder sb = new StringBuilder();
                String message = msg.getMessage();
                if (message != null) {
                    sb.append(message);
                }

                if (msg.getThrowable() != null) {
                    sb.append("<pre>").append(Utils.getStackTrace(msg.getThrowable())).append("</pre>");
                }
                String title = msg.getTitle();
                Level level = msg.getLevel();
                String message1 = sb.toString();
                SourceProvider page = msg.getPage();

                writer.message(title, level, message1, page);
            }
            if (item instanceof CloseSection i) {
                writer.closeSection(i);
            }
            if (item instanceof UiMessageItem i) {
                i.message(writer);
            }
            if (item instanceof TechMessageItem i) {
                i.message(writer);
            }
            if (item instanceof ItfOpenSectionItem i) {
                i.openSection(writer);
            }
            if (item instanceof BvMessageItem i) {
                i.message(writer);
            }
            if (item instanceof RestMessageItem i) {
                i.message(writer);
            }
            if (item instanceof MiaOpenSectionItem i) {
                i.openSection(writer);
            }
            if (item instanceof SqlMessageItem i) {
                i.message(writer);
            }
            if (item instanceof SshMessageItem i) {
                i.message(writer);
            }
            if (item instanceof ActionMessage msg) {
                StringBuilder sb = new StringBuilder();
                String message = msg.getMessage();
                if (message != null) {
                    sb.append(message);
                }
                String title = msg.getTitle();
                Level level = msg.getLevel();
                String message1 = sb.toString();
                SourceProvider page = msg.getPage();
                writer.message(title, level, message1, page);
            }
        }
    }
}
