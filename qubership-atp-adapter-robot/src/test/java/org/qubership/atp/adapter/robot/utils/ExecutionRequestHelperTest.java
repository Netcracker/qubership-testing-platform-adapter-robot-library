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

package org.qubership.atp.adapter.robot.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;

public class ExecutionRequestHelperTest {

    @Test
    public void prepareErName() {
        String requestName = ExecutionRequestHelper.generateRequestName();
        Pattern p = Pattern.compile("(Default)\\s(ER)\\s(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4}) (\\d{1,2}):(\\d{2})");
        Matcher m = p.matcher(requestName);
        Assert.assertTrue(m.matches());
    }
}
