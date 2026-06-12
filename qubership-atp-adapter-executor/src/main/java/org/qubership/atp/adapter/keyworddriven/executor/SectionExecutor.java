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

package org.qubership.atp.adapter.keyworddriven.executor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.executable.BlockedExecutable;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.ExceptionUtils;
import org.qubership.atp.adapter.utils.KDTUtils;

public class SectionExecutor implements Executor {
    private static final Log log = LogFactory.getLog(SectionExecutor.class);
    private final List<ExecuteListener> executeListeners = new ArrayList<>();

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
        for (Executable child : executable.getChildren()) {
            child.prepare();
        }
    }

    public void addExecuteListener(ExecuteListener listener) {
        if (Config.getBoolean("kdt.check.listeners.are.already.registered", true) && this.executeListeners.contains(listener)) {
            log.warn("Try to register already registered '%s' to '%s'".formatted(listener, this));
        } else {
            this.executeListeners.add(listener);
        }
    }

    public void executeBefore(Executable executable) {
        for (ExecuteListener listener : this.executeListeners) {
            listener.beforeExecute(executable);
        }
    }

    public void executeAfter(Executable executable) {
        for (ExecuteListener listener : this.executeListeners) {
            listener.afterExecute(executable);
        }
    }

    private boolean skip(Executable executable) {
        Section section = (Section)executable;
        if (section.getValidationLevel() > KeywordExecutor.validationLevel) {
            section.log().debug("Section '%s' was skipped because validation level of it is bigger than execution level ( %s > %s )".formatted(section.getFullName(), section.getValidationLevel(), KeywordExecutor.validationLevel));
            return true;
        } else {
            return false;
        }
    }

    protected void executeChildren(Section section) throws Exception {
        Iterator<Executable> children = section.getChildren().iterator();
        Executable current = null;
        try {
            while(children.hasNext()) {
                current = children.next();
                current.execute();
            }
        } catch (Exception e) {
            if (!ExceptionUtils.isHandled(e)) {
                TestCaseException handledException = ExceptionUtils.handle(e, "Error occurred during execution section: " + current + ".\n Error: " + e.getMessage());
                Report.getReport().message(handledException);
                throw handledException;
            }
            throw e;
        } finally {
            while(children.hasNext()) {
                Report.getReport().message(new BlockedExecutable(children.next()));
            }
        }
    }
}

