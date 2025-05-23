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

import static org.qubership.atp.adapter.report.WebReportItem.CloseSection;
import static org.qubership.atp.adapter.report.WebReportItem.Message;
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
        if (wr instanceof AtpRamWriterWraper) {
            AtpRamWriter writer = ((AtpRamWriterWraper) wr).getAtpRamWriter();
            if (item instanceof OpenLog) {
                OpenLog i = (OpenLog) item;
                writer.openLog(i.getLogName(), i.getDescription());
            }
            if (item instanceof CreateContextItem) {
                CreateContextItem i = (CreateContextItem) item;
                writer.createContext(i.getTestRunId());
            }
            if (item instanceof CloseLog) {
                CloseLog i = (CloseLog) item;
                writer.closeLog(i);
            }
            if (item instanceof OpenSection) {
                OpenSection i = (OpenSection) item;
                writer.openSection(i);
            }
            if (item instanceof Message) {
                WebReportItem.Message msg = (WebReportItem.Message) item;
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
            if (item instanceof CloseSection) {
                CloseSection i = (CloseSection) item;
                writer.closeSection(i);
            }
            if (item instanceof UiMessageItem) {
                UiMessageItem i = (UiMessageItem) item;
                i.message(writer);
            }
            if (item instanceof TechMessageItem) {
                TechMessageItem i = (TechMessageItem) item;
                i.message(writer);
            }
            if (item instanceof ItfOpenSectionItem) {
                ItfOpenSectionItem i = (ItfOpenSectionItem) item;
                i.openSection(writer);
            }
            if (item instanceof BvMessageItem) {
                BvMessageItem i = (BvMessageItem) item;
                i.message(writer);
            }
            if (item instanceof RestMessageItem) {
                RestMessageItem i = (RestMessageItem) item;
                i.message(writer);
            }
            if (item instanceof MiaOpenSectionItem) {
                MiaOpenSectionItem i = (MiaOpenSectionItem) item;
                i.openSection(writer);
            }
            if (item instanceof SqlMessageItem) {
                SqlMessageItem i = (SqlMessageItem) item;
                i.message(writer);
            }
            if (item instanceof SshMessageItem) {
                SshMessageItem i = (SshMessageItem) item;
                i.message(writer);
            }
            if (item instanceof ActionMessage) {
                //needed to support screenshot
                ActionMessage msg = (ActionMessage)item;
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
