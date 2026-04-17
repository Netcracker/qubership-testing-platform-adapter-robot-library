/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.adapter.tools.tacomponents.context.events;

import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class PutValueEvent<T> extends DataStorageEvent {
    @Nonnull
    private final String key;
    private final T value;

    public PutValueEvent(@Nonnull String key, @Nullable T value, @Nonnull ContextDataStorage dataStorage) {
        super(dataStorage);
        this.key = key;
        this.value = value;
    }

    @Nonnull
    public String getKey() {
        return this.key;
    }

    @Nullable
    public T getValue() {
        return this.value;
    }
}
