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

import static org.qubership.atp.adapter.common.RamConstants.DEFAULT_ER_NAME;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.atp.adapter.common.RamConstants;

public class ExecutionRequestHelper {
    private static final Logger log = LoggerFactory.getLogger(ExecutionRequestHelper.class);
    private static SolutionBuildGetter solutionBuildGetter = new SolutionBuildGetter();

    /**
     * returns prepared Execution Request name.
     */
    public static String generateRequestName() {
        String erTemplate = Config.getConfig().getProperty("atp.project.er.name", DEFAULT_ER_NAME);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String res = erTemplate + " " + now.format(formatter);
        System.setProperty("atp2.ram.er.name", res);
        return res;
    }

    /**
     * Returns ER name with hostName.
     */
    public static String generateRequestNameByEngineHost() {
        String erTemplate = Config.getConfig().getProperty("atp.project.er.name", DEFAULT_ER_NAME);
        String hostName = getHostName();
        CRC32 crc32 = new CRC32();
        crc32.update(hostName.getBytes());
        String res = erTemplate + "_" + crc32.getValue();
        System.setProperty("atp2.ram.er.name", res);
        return res;
    }

    /**
     * Get host name.
     */
    public static String getHostName() {
        String hostName = "Unknown";
        try {
            InetAddress host = InetAddress.getLocalHost();
            hostName = host.getHostName();
        } catch (UnknownHostException uhe) {
            log.error("Unable get host name", uhe);
        }
        return hostName;
    }

    public static String getOsUser() {
        return System.getProperty("user.name", "Unknown");
    }

    /**
     * Parse response and return test run and record info.
     */
    public static Map<String, String> parseResponse(String response) {
        Map<String, String> result = new HashMap<>();
        Pattern testRunIdPattern = Pattern.compile("<testRunId>(.*)</testRunId>");
        Matcher testRunIdMatcher = testRunIdPattern.matcher(response);
        if (testRunIdMatcher.find()) {
            result.put(RamConstants.TEST_RUN_ID_KEY, testRunIdMatcher.group(1));
        }
        Pattern recordIdPattern = Pattern.compile("<recordId>(.*)</recordId>");
        Matcher recordIdMatcher = recordIdPattern.matcher(response);
        if (recordIdMatcher.find()) {
            result.put(RamConstants.RECORD_ID_KEY, recordIdMatcher.group(1));
        }
        return result;
    }

    public static String getUuidString() {
        return UUID.randomUUID().toString();
    }

    public static Timestamp getCurrentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Returns version of solution build.
     */
    public static String getSolutionBuild(String urlString) {
        return solutionBuildGetter.getSolutionBuild(urlString);
    }
}

