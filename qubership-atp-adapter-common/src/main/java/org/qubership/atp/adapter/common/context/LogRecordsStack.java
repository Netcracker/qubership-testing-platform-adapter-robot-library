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

import java.util.Stack;

import org.qubership.atp.ram.models.LogRecord;

public class LogRecordsStack extends Stack<LogRecord> {

    Stack<Integer> logRecordsOriginalHashCodes;

    public LogRecordsStack() {
        super();
        logRecordsOriginalHashCodes = new Stack<>();
    }

    @Override
    public LogRecord push(LogRecord item) {
        super.push(item);
        logRecordsOriginalHashCodes.push(item.hashCode());
        return item;
    }

    @Override
    public synchronized LogRecord pop() {
        logRecordsOriginalHashCodes.pop();
        return super.pop();
    }

    public synchronized boolean isCurrentLogRecordChanged() {
        if (super.empty()) {
            return false;
        }
        LogRecord currentSection = peek();
        int originalHashCode = logRecordsOriginalHashCodes.peek();
        return originalHashCode != currentSection.hashCode();
    }
}
