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
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ContextStorageManager {
    private static ContextStorageManager instance;
    @Nonnull
    private final Map<ContextType, ContextDataStorage> storages = new ConcurrentHashMap();

    private ContextStorageManager() {
        ContextType[] var1 = ContextType.values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ContextType contextType = var1[var3];
            this.getStorages().put(contextType, new DefaultContextDataStorage());
        }

    }

    public static synchronized ContextStorageManager getInstance() {
        if (instance == null) {
            instance = new ContextStorageManager();
        }

        return instance;
    }

    @Nullable
    public ContextDataStorage getDataStorage(@Nonnull ContextType contextType) {
        return (ContextDataStorage)this.getStorages().get(contextType);
    }

    @Nonnull
    Map<ContextType, ContextDataStorage> getStorages() {
        return this.storages;
    }
}

