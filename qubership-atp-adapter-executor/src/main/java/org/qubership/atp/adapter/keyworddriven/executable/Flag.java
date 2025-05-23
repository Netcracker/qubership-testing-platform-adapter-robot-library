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

public class Flag {
    private String fullText;
    private String name;
    private String value;
    private boolean isEnabled;

    public String getFullText() {
        return this.fullText;
    }

    public static Flag parse(String fullText) {
        String[] keyValue = fullText.split("=", 2);
        Flag flag = new Flag(keyValue[0], keyValue.length < 2 ? "" : keyValue[1]);
        flag.fullText = fullText;
        return flag;
    }

    public Flag(String name) {
        this(true, name, "");
    }

    public Flag(String name, String value) {
        this(true, name, value);
    }

    public Flag(String name, boolean isEnabled) {
        this(isEnabled, name, "");
    }

    public Flag(boolean isEnabled, String name, String value) {
        this.isEnabled = isEnabled;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void enable() {
        this.setEnabled(true);
    }

    public void disable() {
        this.setEnabled(false);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public boolean isDisabled() {
        return !this.isEnabled();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Flag flag = (Flag)o;
            return this.name.equals(flag.name);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        return this.name + (this.value.isEmpty() ? "" : "=" + this.value);
    }
}

