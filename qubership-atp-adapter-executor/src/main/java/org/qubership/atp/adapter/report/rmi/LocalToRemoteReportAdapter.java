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

import org.qubership.atp.adapter.report.WebReportItem;
import org.qubership.atp.adapter.report.WebReportWriterDiff;
import org.qubership.atp.adapter.report.WebReportWriterWraper;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.Utils;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LocalToRemoteReportAdapter extends UnicastRemoteObject implements RemoteReportAdapter {
    private WebReportWriterWraper wraper = new WebReportWriterWraper();
    private WebReportWriterDiff writer = new WebReportWriterDiff();
    private String threadName;

    public LocalToRemoteReportAdapter(String threadName) throws RemoteException {
        this.threadName = threadName;
    }

    public void write(Object item) throws RemoteException {
        if (item instanceof WebReportItem) {
            String oldThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName(this.threadName);
            if (Config.getBoolean("use.diff.report", false)) {
                if (item instanceof WebReportItem.Message) {
                    WebReportItem.Message msg = (WebReportItem.Message)item;
                    StringBuilder message = new StringBuilder(msg.getMessage());
                    if (msg.getThrowable() != null) {
                        message.append("<pre>").append(Utils.getStackTrace(msg.getThrowable())).append("</pre>");
                    }

                    this.writer.message(msg.getTitle(), msg.getLevel(), message.toString(), msg.getPage());
                }
            } else {
                ((WebReportItem)item).message(this.wraper);
            }

            Thread.currentThread().setName(oldThreadName);
        }

    }
}

