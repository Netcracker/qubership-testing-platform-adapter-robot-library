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

package org.qubership.atp.adapter.keyworddriven.basicformat.flags;

import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.Flag;

public enum TestCaseType {
    PRE_CONDITION(new String[]{"before"}),
    TEST_CASE(new String[]{"test", ""}),
    POST_CONDITION(new String[]{"after"});

    private final String[] names;
    public static final String TEST_CASE_TYPE_FLAG = "TEST_CASE_TYPE";

    private TestCaseType(String... names) {
        this.names = names;
    }

    public static TestCaseType matches(Executable input) {
        Flag type = input.getFlag("TEST_CASE_TYPE");
        if (type == null) {
            return TEST_CASE;
        } else {
            TestCaseType[] var2 = values();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                TestCaseType testCaseType = var2[var4];
                String[] var6 = testCaseType.names;
                int var7 = var6.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    String flag = var6[var8];
                    if (flag.equalsIgnoreCase(type.getValue())) {
                        return testCaseType;
                    }
                }
            }

            return TEST_CASE;
        }
    }
}

