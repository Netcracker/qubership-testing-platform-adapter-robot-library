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

package org.qubership.atp.adapter.common.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ActionParametersTrimmerTest {

    private ActionParametersTrimmer actionParametersTrimmer;

    @Before
    public void setUp() {
        actionParametersTrimmer = new ActionParametersTrimmer(12);
    }

    @Test
    public void trimActionParametersByLimit() {
        String bigValue = "\"big value here big value herebig value here big value here\"";
        String bigValueWithQuotes = "\"big value here big value here \\\"big value here big value here\"";
        String bigValueWithQuotes2 = "\"big value here big value here 'big value here' big value here\"";
        String arrayWithBigValue =
                "(\"small value\", \"big value here big value here 'big value here' big value here\")";

        String step = "Store value " + bigValue + " and " + bigValueWithQuotes + " and " + bigValueWithQuotes2 + "and "
                + arrayWithBigValue;

        String expected =
                "Store value \"big value...\" and \"big value...\" and \"big value...\"and (\"small value\", \"big value...\")";
        String result = actionParametersTrimmer.trimActionParametersByLimit(step);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void trimActionParametersByLimit2() {
        String step = "Store value \"MRC with \\\\\\\"Installation\\\\\\\"\"";
        String result = actionParametersTrimmer.trimActionParametersByLimit(step);
        String expected = "Store value \"MRC with ...\"";
        Assert.assertEquals(expected, result);
    }
}
