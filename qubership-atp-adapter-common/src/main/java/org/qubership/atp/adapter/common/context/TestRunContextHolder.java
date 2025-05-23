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

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class TestRunContextHolder {
    private static final ConcurrentHashMap<String, TestRunContext> holder = new ConcurrentHashMap<>();

    /**
     * Return context for TR.
     */
    public static synchronized TestRunContext getContext(String testRunId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testRunId), "TestRunId is required!");
        TestRunContext res = holder.get(testRunId);
        if (res != null) {
            return res;
        }
        res = new TestRunContext();
        res.setTestRunId(testRunId);
        holder.put(testRunId, res);
        return res;
    }

    /**
     * Return true if context for TR exist.
     */
    public static synchronized boolean hasContext(String testRunId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testRunId), "TestRunId is required!");
        return holder.containsKey(testRunId);
    }

    /**
     * Return context for TR by name and uuid.
     */
    public static synchronized TestRunContext getContext(String testRunName, String testRunId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testRunId), "TestRunId is required!");
        TestRunContext res = holder.get(testRunId);
        if (res != null) {
            return res;
        }
        res = new TestRunContext();
        res.setTestRunId(testRunId);
        res.setTestRunName(testRunName);
        holder.put(testRunId, res);
        return res;
    }

    public static synchronized void putContext(String testRunId, TestRunContext context) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testRunId));
        holder.put(testRunId, context);
    }

    public static synchronized void removeContext(String testRunId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testRunId));
        holder.remove(testRunId);
    }
}
