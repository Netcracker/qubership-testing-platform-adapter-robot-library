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

package org.qubership.atp.adapter.keyworddriven.executable;

import org.qubership.atp.adapter.keyworddriven.basicformat.ValidationLevel;
import java.util.LinkedHashSet;
import java.util.Set;

public class FileTestCase extends TestCaseImpl {
    private String sourceFileName;
    private Set<String> executableSectionNames;
    private String threadName;

    public FileTestCase(String name, String description, String sourceFileName) {
        this(name, description, sourceFileName, (String)null);
    }

    public FileTestCase(String name, String description, String sourceFileName, String threadName) {
        this(name, description, sourceFileName, (Executable)null, threadName);
    }

    public FileTestCase(String name, String description, String sourceFileName, String threadName, int level) {
        this(name, description, sourceFileName, (Executable)null, threadName, level);
    }

    public FileTestCase(String name, String description, String sourceFileName, Executable parent, String threadName) {
        this(name, description, sourceFileName, parent, threadName, ValidationLevel.smoke.lvl);
    }

    public FileTestCase(String name, String description, String sourceFileName, Executable parent, String threadName, int level) {
        super(name, description, parent);
        this.executableSectionNames = new LinkedHashSet();
        this.sourceFileName = sourceFileName;
        this.threadName = threadName;
        this.setValidationLevel(level);
    }

    public String getSourceFileName() {
        return this.sourceFileName;
    }

    public Set<String> getExecutableSectionName() {
        return this.executableSectionNames;
    }

    public void addExecutableSectionName(String executableSectionName) {
        this.executableSectionNames.add(executableSectionName);
    }

    public String getThreadName() {
        return this.threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
}

