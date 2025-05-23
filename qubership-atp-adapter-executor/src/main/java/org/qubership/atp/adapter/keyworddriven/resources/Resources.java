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

package org.qubership.atp.adapter.keyworddriven.resources;

import org.apache.log4j.Logger;

public class Resources {
    private static final Logger log = Logger.getLogger(Resources.class);

    private Resources() {
    }

    public static <T> T get(Class<T> clazz) {
        return ResourceFactory.getInstance().get(clazz);
    }

    public static void releaseResourcesForCurrentThreadSilently() {
        try {
            ResourceFactory.getInstance().releaseResourcesForCurrentThread();
        } catch (Throwable var1) {
            Throwable e = var1;
            log.error("Failed to release resources for thread: " + Thread.currentThread(), e);
        }

    }
}

