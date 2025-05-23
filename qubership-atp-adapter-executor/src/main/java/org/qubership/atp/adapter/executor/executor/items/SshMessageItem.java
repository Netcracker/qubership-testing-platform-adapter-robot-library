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

package org.qubership.atp.adapter.executor.executor.items;

import org.apache.log4j.Level;

import org.qubership.atp.adapter.executor.executor.AtpRamWriter;
import org.qubership.atp.adapter.report.WebReportItem;
import org.qubership.atp.adapter.report.WebReportWriterWraper;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SshMessageItem extends WebReportItem {
    private org.qubership.atp.adapter.common.entities.Message message;
    private Level level;

    @Override
    public void message(WebReportWriterWraper webReportWriter) {
        webReportWriter.getWebReportWriter().message(message.getName(), level, message.getMessage(), null);
    }

    public void message(AtpRamWriter writer) {
        writer.sshMessage(message, level);
    }
}
