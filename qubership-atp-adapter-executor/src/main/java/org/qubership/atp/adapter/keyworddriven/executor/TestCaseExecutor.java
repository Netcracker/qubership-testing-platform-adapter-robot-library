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

package org.qubership.atp.adapter.keyworddriven.executor;

import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.executable.BlockedExecutable;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.utils.ExceptionUtils;
import java.util.Iterator;

public class TestCaseExecutor extends SectionExecutor {
    public TestCaseExecutor() {
    }

    public void execute(Executable executable) throws Exception {
        Report.getReport().message(executable);
        Iterator<Executable> iterator = executable.getChildren().iterator();

        try {
            while(iterator.hasNext()) {
                ((Executable)iterator.next()).execute();
            }
        } catch (Exception var8) {
            Exception e = var8;
            if (!ExceptionUtils.isHandled(e)) {
                TestCaseException handledException = ExceptionUtils.handle(e, "Error during test case execution");
                Report.getReport().message(handledException);
                throw handledException;
            }

            throw e;
        } finally {
            while(iterator.hasNext()) {
                Report.getReport().message(new BlockedExecutable((Executable)iterator.next()));
            }

            Report.getReport().message(executable);
        }

    }
}

