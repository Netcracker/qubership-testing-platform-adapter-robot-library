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

public class SectionImpl extends ExecutableImpl implements Section {
    private int validationLevel;
    private String description;

    public SectionImpl(String name, Executable parent) {
        super(name, parent);
        this.validationLevel = ValidationLevel.smoke.lvl;
    }

    public SectionImpl(String name, String description, Executable parent) {
        super(name, parent);
        this.validationLevel = ValidationLevel.smoke.lvl;
        this.setDescription(description);
    }

    public String toString() {
        return this.getFullName().replaceAll("[\r\n]+", " | ");
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description == null ? "" : this.description;
    }

    public String getFullName() {
        return !(this instanceof TestCase) && this.getParent() != null && !(this.getParent() instanceof TestCase) ? ((Section)this.getParent()).getFullName() + "\n" + this.getName() : this.getName();
    }

    public int getValidationLevel() {
        return this.validationLevel;
    }

    public void setValidationLevel(int lvl) {
        this.validationLevel = lvl;
    }
}

