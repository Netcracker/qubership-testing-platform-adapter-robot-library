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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static Config config = new Config();
    private File[] configs;
    private Properties props = new Properties();

    private Config() {
        init();
    }

    public static Config getConfig() {
        return config;
    }

    private void init() {
        loadConfigFiles();
        readConfigFiles();
    }

    private void loadConfigFiles() {
        File dir = new File(Paths.get("").toAbsolutePath().toString());
        FileFilter fileFilter = new WildcardFileFilter("*.properties");
        configs = dir.listFiles(fileFilter);
    }

    private void readConfigFiles() {
        for (File file : configs) {
            try {
                FileInputStream fis = new FileInputStream(file);
                props.load(fis);
            } catch (FileNotFoundException fnfe) {
                log.error("File not found", fnfe);
            } catch (IOException io) {
                log.error("Problem with load", io);
            }
        }
    }

    public String getProperty(String key) {
        String propValue = props.getProperty(key);
        return System.getProperty(key, propValue);
    }

    public String getProperty(String key, String defaultValue) {
        String propValue = props.getProperty(key, defaultValue);
        return System.getProperty(key, propValue);
    }

    /**
     * Getting value from config with check for absent or empty String value.
     * @param key - property name
     * @param defaultValue - the value which will be returned in case of absent or empty String value
     * @return - String value from Config or default value in case of absent or empty String value
     */
    public String getNotEmptyStringProperty(String key, String defaultValue) {
        String propValue = props.getProperty(key, defaultValue);
        String value = System.getProperty(key, propValue);
        return value.isEmpty() ? defaultValue : value;
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String propValue = props.getProperty(key);
        String result = System.getProperty(key, propValue);
        return result == null ? defaultValue : Boolean.parseBoolean(result);
    }

    public int getIntProperty(String key, int defaultValue) {
        String propValue = props.getProperty(key);
        String result = System.getProperty(key, propValue);
        return result == null ? defaultValue : Integer.parseInt(result);
    }

    public long getLongProperty(String key, long defaultValue) {
        String propValue = props.getProperty(key);
        String result = System.getProperty(key, propValue);
        return result == null ? defaultValue : Long.parseLong(result);
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
}
