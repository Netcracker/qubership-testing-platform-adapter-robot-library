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
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.ExceptionUtils;
import org.qubership.atp.adapter.utils.KDTUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SectionExecutor implements Executor {
    private static final Log log = LogFactory.getLog(SectionExecutor.class);
    private List<ExecuteListener> executeListeners = new ArrayList();

    public SectionExecutor() {
    }

    public void execute(Executable executable) throws Exception {
        Section section = (Section)executable;
        KDTUtils.replaceParametersInDescription(section);
        if (!this.skip(section)) {
            Report.getReport().message(section);

            try {
                this.executeChildren(section);
            } finally {
                Report.getReport().message(section);
            }

        }
    }

    public void prepare(Executable executable) {
        Iterator var2 = executable.getChildren().iterator();

        while(var2.hasNext()) {
            Executable child = (Executable)var2.next();
            child.prepare();
        }

    }

    public void addExecuteListener(ExecuteListener listener) {
        if (Config.getBoolean("kdt.check.listeners.are.already.registered", true) && this.executeListeners.contains(listener)) {
            log.warn(String.format("Try to register already registered '%s' to '%s'", listener, this));
        } else {
            this.executeListeners.add(listener);
        }
    }

    public void executeBefore(Executable executable) {
        Iterator var2 = this.executeListeners.iterator();

        while(var2.hasNext()) {
            ExecuteListener listener = (ExecuteListener)var2.next();
            listener.beforeExecute(executable);
        }

    }

    public void executeAfter(Executable executable) {
        Iterator var2 = this.executeListeners.iterator();

        while(var2.hasNext()) {
            ExecuteListener listener = (ExecuteListener)var2.next();
            listener.afterExecute(executable);
        }

    }

    private boolean skip(Executable executable) {
        Section section = (Section)executable;
        if (section.getValidationLevel() > KeywordExecutor.validationLevel) {
            section.log().debug(String.format("Section '%s' was skipped because validation level of it is bigger than execution level ( %s > %s )", section.getFullName(), section.getValidationLevel(), KeywordExecutor.validationLevel));
            return true;
        } else {
            return false;
        }
    }

    protected void executeChildren(Section section) throws Exception {
        Iterator<Executable> childen = section.getChildren().iterator();
        Executable current = null;

        try {
            while(childen.hasNext()) {
                current = (Executable)childen.next();
                current.execute();
            }
        } catch (Exception var9) {
            Exception e = var9;
            if (!ExceptionUtils.isHandled(e)) {
                TestCaseException handledException = ExceptionUtils.handle(e, "Error occurred during execution section: " + current + ".\n Error: " + e.getMessage());
                Report.getReport().message(handledException);
                throw handledException;
            }

            throw e;
        } finally {
            while(childen.hasNext()) {
                Report.getReport().message(new BlockedExecutable((Executable)childen.next()));
            }

        }

    }
}

