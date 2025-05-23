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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SolutionBuildGetter {

    private static final Object syncObject = new Object();
    private static final ConcurrentHashMap<String, String> solutionBuilds = new ConcurrentHashMap<>();

    /**
     * Returns version of solution build.
     */
    public String getSolutionBuild(String urlString) {
        log.debug("start getSolutionBuild(urlString: {})", urlString);
        if (Strings.isNullOrEmpty(urlString)) {
            return "Unknown solution build";
        }

        String solutionBuild = getCachedSolutionBuild(urlString);
        log.debug("initial solutionBuild = {}", solutionBuild);
        if (isGetSolutionBuildEnabled() && Objects.isNull(solutionBuild)) {
            synchronized (syncObject) {
                solutionBuild = getCachedSolutionBuild(urlString);
                if (Objects.isNull(solutionBuild)) {
                    try {
                        String pageContent = connectAndFetchPage(urlString + "/version.txt");
                        solutionBuild = pageContent.toLowerCase().contains("html")
                                ? "Unknown"
                                : pageContent.replaceFirst(".*?[.\\r\\n]*?build_number:", "").trim();
                    } catch (Exception e) {
                        log.error("Cannot get solution build", e);
                        solutionBuild = "Unknown solution build";
                    }
                    cacheSolutionBuild(urlString, solutionBuild);
                    log.debug("cached solution build = {}", solutionBuild);
                }
            }
        }

        if (Objects.isNull(solutionBuild)) {
            solutionBuild = "Unknown solution build";
        }

        log.debug("end getSolutionBuild(urlString: {})", solutionBuild);
        return solutionBuild;
    }

    private void cacheSolutionBuild(String urlString, String solutionBuild) {
        solutionBuilds.put(urlString, solutionBuild);
    }

    private String getCachedSolutionBuild(String urlString) {
        return solutionBuilds.get(urlString);
    }

    private boolean isGetSolutionBuildEnabled() {
        return Config.getConfig().getBooleanProperty("atp2.get.solution.build.enabled", false);
    }

    /**
     * Connect and fetch page by url.
     *
     * @param urlString the url
     * @return the page
     * @throws IOException the exception
     */
    public String connectAndFetchPage(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();

        int timeout = Config.getConfig()
                .getIntProperty("atp2.get.solution.build.connection.timeout", 30 * 1000);
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);

        InputStream in = con.getInputStream();
        String encoding = con.getContentEncoding();
        encoding = encoding == null ? "UTF-8" : encoding;
        return IOUtils.toString(in, encoding);
    }
}
