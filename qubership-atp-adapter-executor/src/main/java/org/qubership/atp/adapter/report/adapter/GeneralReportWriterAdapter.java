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

import org.qubership.atp.adapter.report.GeneralReportWriter;
import org.qubership.atp.adapter.report.GenericsReportAdapter;
import org.qubership.atp.adapter.report.WebReportItem;
import org.qubership.atp.adapter.testcase.Config;

public class GeneralReportWriterAdapter extends GenericsReportAdapter<GeneralReportWriter> {
    public GeneralReportWriterAdapter() {
    }

    public void writeItem(GeneralReportWriter writer, Object item) {
        try {
            if (item instanceof WebReportItem.Message) {
                writer.report((WebReportItem.Message)item, Thread.currentThread());
            } else if (item instanceof WebReportItem.OpenLog) {
                writer.newScenario((WebReportItem.OpenLog)item, Thread.currentThread());
            }
        } finally {
            writer.reportScenarioDetails();
            if (Config.getBoolean("report.junit.enabled")) {
                writer.reportXmlJunitDetails();
            }

        }

    }
}
