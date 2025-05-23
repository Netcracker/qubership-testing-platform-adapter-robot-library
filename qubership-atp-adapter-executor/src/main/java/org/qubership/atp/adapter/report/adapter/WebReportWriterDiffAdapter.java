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

package org.qubership.atp.adapter.report.adapter;

import org.qubership.atp.adapter.report.GenericsReportAdapter;
import org.qubership.atp.adapter.report.WebReportItem;
import org.qubership.atp.adapter.report.WebReportWriterDiff;
import org.qubership.atp.adapter.utils.Utils;

public class WebReportWriterDiffAdapter extends GenericsReportAdapter<WebReportWriterDiff> {
    public WebReportWriterDiffAdapter() {
    }

    public void writeItem(WebReportWriterDiff writer, Object item) {
        if (item instanceof WebReportItem.Message) {
            WebReportItem.Message msg = (WebReportItem.Message)item;
            StringBuilder message = new StringBuilder(msg.getMessage());
            if (msg.getThrowable() != null) {
                message.append("<pre>").append(Utils.getStackTrace(msg.getThrowable())).append("</pre>");
            }

            writer.message(msg.getTitle(), msg.getLevel(), message.toString(), msg.getPage());
        } else if (item instanceof WebReportItem.OpenSection) {
            WebReportItem.OpenSection os = (WebReportItem.OpenSection)item;
            writer.openSection(os.getTitle(), os.getMessage(), os.getPage());
        } else if (item instanceof WebReportItem.CloseSection) {
            writer.closeSection();
        } else if (item instanceof WebReportItem.OpenLog) {
            WebReportItem.OpenLog ol = (WebReportItem.OpenLog)item;
            writer.openLog(ol.getLogName(), ol.getDescription());
        } else if (item instanceof WebReportItem.CloseLog) {
            writer.closeLog();
        }

    }
}

