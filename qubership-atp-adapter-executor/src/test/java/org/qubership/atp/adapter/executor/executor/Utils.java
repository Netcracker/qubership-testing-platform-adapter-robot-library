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

package org.qubership.atp.adapter.executor.executor;

import java.util.UUID;

import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;

public class Utils {
    public static final UUID logRecordUuid = UUID.randomUUID();
    public static final UUID testRunUuid = UUID.randomUUID();
    public static final String compaundName = "Compaund";
    public static final UUID compaundId = UUID.randomUUID();
    public static final String stepName = "Step";
    public static final UUID stepId = UUID.randomUUID();

    public static TestRunContext createTestRunContext(boolean withSection) {
        TestRunContext testRunContext = new TestRunContext();
        if (withSection) {
            LogRecord section = new LogRecord();
            section.setUuid(logRecordUuid);
            testRunContext.addSection(section);
        }
        testRunContext.setProjectName("test");
        testRunContext.setTestRunId(testRunUuid.toString());
        testRunContext.setTestRunName("test");
        testRunContext.setTestPlanName("test");
        testRunContext.setTestCaseName("test");
        return testRunContext;
    }

    public static AtpCompaund createAtpCompaund(boolean stepIsLast) {
        AtpCompaund atpCompaund = new AtpCompaund();
        atpCompaund.setSectionId(stepId.toString());
        atpCompaund.setSectionName(stepName);
        atpCompaund.setLastInSection(stepIsLast);

        AtpCompaund parent = new AtpCompaund();
        parent.setSectionId(compaundId.toString());
        parent.setSectionName(compaundName);
        parent.setType(TypeAction.COMPOUND);

        atpCompaund.setParentSection(parent);
        return atpCompaund;
    }

    public static LogRecord createLogRecord(String name, UUID id, UUID parentId) {
        LogRecord logRecord = new LogRecord();
        logRecord.setName(name);
        logRecord.setUuid(id);
        logRecord.setParentRecordId(parentId);
        return logRecord;
    }
}
