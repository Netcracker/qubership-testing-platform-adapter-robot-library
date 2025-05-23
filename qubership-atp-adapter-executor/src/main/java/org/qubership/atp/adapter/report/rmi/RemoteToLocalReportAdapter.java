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

package org.qubership.atp.adapter.report.rmi;

import org.qubership.atp.adapter.report.ReportAdapter;
import org.qubership.atp.adapter.report.ReportWriter;
import java.rmi.RemoteException;
import org.apache.log4j.Logger;

public class RemoteToLocalReportAdapter implements ReportAdapter {
    private RemoteReportAdapter remoteReportAdapter;

    public RemoteToLocalReportAdapter(RemoteReportAdapter remoteReportAdapter) {
        this.remoteReportAdapter = remoteReportAdapter;
    }

    public void write(ReportWriter writer, Object item) {
        try {
            if (!RemoteParametersStorage.getIsServer()) {
                this.remoteReportAdapter.write(item);
            }
        } catch (RemoteException var4) {
            RemoteException e = var4;
            Logger.getLogger(RemoteToLocalReportAdapter.class).error(e);
        }

    }
}
