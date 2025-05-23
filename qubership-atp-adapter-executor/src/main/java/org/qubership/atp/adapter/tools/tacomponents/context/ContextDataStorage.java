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

import com.google.common.eventbus.EventBus;
import java.io.Externalizable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ContextDataStorage extends Externalizable {
    <T> ContextRecord<T> putValue(@Nonnull String var1, @Nullable T var2);

    <T> void putValues(@Nonnull Map<String, T> var1);

    @Nullable
    <T> T getValue(@Nonnull String var1);

    @Nullable
    <T> T getValue(@Nonnull String var1, @Nonnull T var2);

    @Nonnull
    Map<String, Object> getValues();

    <T> ContextRecord<T> putRecord(@Nonnull String var1, @Nonnull ContextRecord<T> var2);

    <T> void putRecords(@Nonnull Map<String, ContextRecord<T>> var1);

    @Nullable
    <T> ContextRecord<T> getRecord(@Nonnull String var1);

    @Nonnull
    <T> ContextRecord<T> getRecord(@Nonnull String var1, @Nonnull ContextRecord<T> var2);

    @Nonnull
    Map<String, ContextRecord<?>> getRecords();

    void removeRecord(@Nonnull String var1);

    void clear();

    @Nonnull
    EventBus getEventBus();
}

