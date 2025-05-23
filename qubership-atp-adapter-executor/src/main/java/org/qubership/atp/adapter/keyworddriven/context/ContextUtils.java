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

package org.qubership.atp.adapter.keyworddriven.context;

import org.qubership.atp.adapter.tools.tacomponents.context.Context;

public class ContextUtils {
    private ContextUtils() {
    }

    public static int getInt(String key, int defaultValue) {
        Object value = Context.getValue(key);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Integer) {
            return (Integer)value;
        } else {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException var4) {
                return defaultValue;
            }
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = Context.getValue(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(value.toString());
        }
    }
}

