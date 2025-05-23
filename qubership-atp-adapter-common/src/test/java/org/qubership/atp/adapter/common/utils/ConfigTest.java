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

public class ConfigTest {

    @Before
    public void setUp() throws Exception {
        System.clearProperty("atp.url");
    }

    @Test
    public void getConfig() {
        Assert.assertNotNull(Config.getConfig());
    }

    @Test
    public void getPropertyWithoutDefaultValue() {
        String value = "http://localhost:8081";
        Assert.assertEquals(value, Config.getConfig().getProperty("atp.url"));
        Assert.assertNull(Config.getConfig().getProperty("atp2.url"));
    }

    @Test
    public void getPropertyWithDefaultValue() {
        String defValue = "http://localhost:8888";
        Assert.assertNotEquals(defValue, Config.getConfig().getProperty("atp.url", defValue));
        Assert.assertEquals(defValue, Config.getConfig().getProperty("atp2.url", defValue));
    }

    @Test
    public void allPropertiesFilesShouldBeLoaded() {
        Assert.assertNotNull(Config.getConfig().getProperty("atp3.url"));
    }

    @Test
    public void samePropertiesInFilesShouldExist() {
        System.out.println(Config.getConfig().getProperty("atp4.url"));
    }

    @Test
    public void setProperty() {
        String key = "prop.should.be.empty";
        String value = "newValue";
        Assert.assertNull(Config.getConfig().getProperty(key));
        Config.getConfig().setProperty(key, value);
        Assert.assertEquals(value, Config.getConfig().getProperty(key));
    }

    @Test
    public void systemPropertyShouldBePriority() {
        String value = "http://localhost:8081";
        String newValue = "http://localhost:8181";
        Assert.assertEquals(Config.getConfig().getProperty("atp.url"), value);
        System.setProperty("atp.url", newValue);
        System.out.println(Config.getConfig().getProperty("atp.url"));
        Assert.assertEquals(newValue, Config.getConfig().getProperty("atp.url"));
    }
}
