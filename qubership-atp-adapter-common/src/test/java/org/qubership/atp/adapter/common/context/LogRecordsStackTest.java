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

package org.qubership.atp.adapter.common.context;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;

public class LogRecordsStackTest {

    @Test
    public void testIsCurrentLogRecordChanged_shouldBeFalse_whenLogRecordsAreSame() {
        LogRecord logRecord = new LogRecord();
        logRecord.setName("record 1");
        logRecord.setTestingStatus(TestingStatuses.NOT_STARTED);

        LogRecordsStack stack = new LogRecordsStack();
        stack.push(logRecord);
        Assert.assertFalse("Current log record should not be changed", stack.isCurrentLogRecordChanged());
    }

    @Test
    public void testIsCurrentLogRecordChanged_shouldBeTrue_whenLogRecordsAreDifferent() {
        LogRecord logRecord = new LogRecord();
        logRecord.setName("record 1");
        logRecord.setTestingStatus(TestingStatuses.NOT_STARTED);

        LogRecordsStack stack = new LogRecordsStack();
        stack.push(logRecord);
        logRecord.setTestingStatus(TestingStatuses.PASSED);
        Assert.assertTrue("Current log record should be changed", stack.isCurrentLogRecordChanged());
    }

    @Test
    public void testIsCurrentLogRecordChanged_shouldReturnCorrectResult_whenMultipleLogRecords() {
        LogRecord logRecord = new LogRecord();
        logRecord.setName("record 1");
        logRecord.setTestingStatus(TestingStatuses.NOT_STARTED);

        LogRecord logRecord2 = new LogRecord();
        logRecord2.setName("record 2");
        logRecord2.setTestingStatus(TestingStatuses.FAILED);
        logRecord2.setExecutionStatus(ExecutionStatuses.FINISHED);

        LogRecord logRecord3 = new LogRecord();
        logRecord3.setName("record 3");
        logRecord3.setExecutionStatus(ExecutionStatuses.TERMINATED);

        LogRecordsStack stack = new LogRecordsStack();
        stack.push(logRecord);
        stack.push(logRecord2);
        stack.push(logRecord3);
        logRecord.setTestingStatus(TestingStatuses.PASSED);
        logRecord3.setDuration(10);

        Assert.assertTrue("Current log record should be changed", stack.isCurrentLogRecordChanged());
        stack.pop();
        Assert.assertFalse("Current log record should not be changed", stack.isCurrentLogRecordChanged());
        stack.pop();
        Assert.assertTrue("Current log record should be changed", stack.isCurrentLogRecordChanged());
        stack.pop();
        Assert.assertTrue("Stack should be empty", stack.isEmpty());
    }
}
