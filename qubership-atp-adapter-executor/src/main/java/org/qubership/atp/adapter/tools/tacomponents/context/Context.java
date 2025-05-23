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

package org.qubership.atp.adapter.tools.tacomponents.context;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Context {
    private static ContextType defaultContextType;
    private static ContextType globalContextType;

    public Context() {
    }

    @Nonnull
    private static ContextDataStorage getDefaultStorage() {
        return defaultContextType.getStorage();
    }

    @Nonnull
    private static ContextDataStorage getGlobalStorage() {
        return globalContextType.getStorage();
    }

    @Nonnull
    public static ContextDataStorage getStorage(@Nonnull ContextType contextType) {
        return contextType.getStorage();
    }

    public static <T> ContextRecord<T> putValue(@Nonnull String key, @Nonnull T value) {
        return getDefaultStorage().putValue(key, value);
    }

    public static <T> ContextRecord<T> putValue(@Nonnull String key, @Nonnull T value, @Nonnull ContextType contextType) {
        return getStorage(contextType).putValue(key, value);
    }

    @Nullable
    public static <T> T getValue(@Nonnull String key) {
        return getDefaultStorage().getValue(key) == null ? getGlobalStorage().getValue(key) : getDefaultStorage().getValue(key);
    }

    @Nullable
    public static <T> T getValue(@Nonnull String key, @Nonnull T defaultValue) {
        return getDefaultStorage().getValue(key, defaultValue) == null ? getGlobalStorage().getValue(key, defaultValue) : getDefaultStorage().getValue(key, defaultValue);
    }

    @Nullable
    public static <T> T getValue(@Nonnull String key, @Nonnull ContextType contextType) {
        return getStorage(contextType).getValue(key);
    }

    @Nullable
    public static <T> T getValue(@Nonnull String key, @Nonnull T defaultValue, @Nonnull ContextType contextType) {
        return getStorage(contextType).getValue(key, defaultValue);
    }

    public static Map<String, Object> getValues() {
        return getDefaultStorage().getValues();
    }

    public static Map<String, Object> getValues(@Nonnull ContextType contextType) {
        return getStorage(contextType).getValues();
    }

    public static void remove(@Nonnull String key) {
        getDefaultStorage().removeRecord(key);
    }

    public static void remove(@Nonnull String key, @Nonnull ContextType contextType) {
        getStorage(contextType).removeRecord(key);
    }

    static {
        defaultContextType = ContextType.LOCAL;
        globalContextType = ContextType.GLOBAL;
    }
}

