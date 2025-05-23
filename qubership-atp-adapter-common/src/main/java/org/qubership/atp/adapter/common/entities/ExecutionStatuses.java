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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Deprecated
public enum ExecutionStatuses {
    NOT_STARTED,
    IN_PROGRESS,
    @JsonProperty("Finished")
    FINISHED,
    @JsonProperty("Terminated")
    TERMINATED,
    TERMINATED_BY_TIMEOUT,
    SUSPENDED,
    RESUMING;

    static {
        NOT_STARTED.name = "Not Started";
        NOT_STARTED.id = 1;
        IN_PROGRESS.name = "In Progress";
        IN_PROGRESS.id = 2;
        FINISHED.name = "Finished";
        FINISHED.id = 3;
        TERMINATED.name = "Terminated";
        TERMINATED.id = 4;
        TERMINATED_BY_TIMEOUT.name = "Terminated by timeout";
        TERMINATED_BY_TIMEOUT.id = 6;
        SUSPENDED.name = "Suspended";
        SUSPENDED.id = 7;
        RESUMING.name = "Resuming";
        RESUMING.id = 8;
    }


    private String name;
    private int id;

    /**
     * Find {@link ExecutionStatuses} by executionRequestId contains word.
     */
    @JsonCreator
    public static ExecutionStatuses findByValue(String strValue) {
        for (ExecutionStatuses v : values()) {
            if (v.getName().equalsIgnoreCase(strValue)) {
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
