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

package org.qubership.atp.adapter.common.entities;

@Deprecated
public enum TestingStatuses {

    PASSED,
    FAILED,
    WARNING,
    STOPPED,
    SKIPPED,
    BLOCKED,
    UNKNOWN,
    NOT_STARTED;

    static {
        PASSED.name = "Passed";
        PASSED.id = 2;
        FAILED.name = "Failed";
        FAILED.id = 4;
        WARNING.name = "Warning";
        WARNING.id = 3;
        UNKNOWN.name = "Unknown";
        UNKNOWN.id = 1;
        NOT_STARTED.name = "Not Started";
        NOT_STARTED.id = 1;
        STOPPED.name = "Stopped";
        STOPPED.id = 5;

        SKIPPED.name = "Skipped";
        SKIPPED.id = 1;

        BLOCKED.name = "Blocked";
        BLOCKED.id = 1;


    }

    private String name;
    private int id;

    /**
     * Find {@link TestingStatuses} by name contains word.
     */
    public static TestingStatuses findByValue(String strValue) {
        for (TestingStatuses v : values()) {
            if (v.toString().equalsIgnoreCase(strValue)) {
                return v;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
