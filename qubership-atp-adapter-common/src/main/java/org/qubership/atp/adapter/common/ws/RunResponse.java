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

package org.qubership.atp.adapter.common.ws;

public class RunResponse {

    private String executionRequestId;
    private String testRunId;
    private String logRecordUuid;

    public String getExecutionRequestId() {
        return executionRequestId;
    }

    public void setExecutionRequestId(String executionRequestId) {
        this.executionRequestId = executionRequestId;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public void setTestRunId(String testRunId) {
        this.testRunId = testRunId;
    }

    public String getLogRecordUuid() {
        return logRecordUuid;
    }

    public void setLogRecordUuid(String logRecordUuid) {
        this.logRecordUuid = logRecordUuid;
    }
}
