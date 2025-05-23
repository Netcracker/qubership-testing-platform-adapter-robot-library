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

package org.qubership.atp.adapter.executor.executor.sourceproviders;

import org.qubership.atp.adapter.report.ReportType;
import org.qubership.atp.adapter.report.SourceProvider;

public class TestSnapshot implements SourceProvider {

    private String source;

    public TestSnapshot(String source) {
        this.source = source;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getExtension() {
        return "html";
    }

    @Override
    public String getReportType() {
        return ReportType.SNAPSHOT.toString();
    }

    @Override
    public void setReportType(String s) {

    }
}
