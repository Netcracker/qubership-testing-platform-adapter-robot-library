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

package org.qubership.atp.adapter.robot;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.adapter.robot.utils.ScreenShotHelper;


public class RamListenerTest {
    @Test
    public void extractFileName() {
        String messageText = "</td></tr><tr><td colspan=\"3\"><a href=\"selenium-screenshot-1.png\"><img src=\"selenium-screenshot-1.png\" width=\"800px\"></a>";
        ScreenShotHelper helper = new ScreenShotHelper();
        String imgName = helper.extractImage(messageText);
        Assert.assertEquals("selenium-screenshot-1.png", imgName);
    }
}
