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

package org.qubership.atp.adapter.tools.tacomponents.context.threading;

import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorageProvider;
import org.qubership.atp.adapter.tools.tacomponents.context.DefaultContextDataStorage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class ContextThreadGroup extends ThreadGroup implements ContextDataStorageProvider {
    @Nonnull
    private ContextDataStorage contextDataStorage = new DefaultContextDataStorage();

    ContextThreadGroup(@Nonnull String name) {
        super(name);
    }

    ContextThreadGroup(ThreadGroup parent, @Nonnull String name) {
        super(parent, name);
    }

    @Nonnull
    public ContextDataStorage getContextDataStorage() {
        return this.contextDataStorage;
    }

    public void setContextDataStorage(@Nullable ContextDataStorage contextDataStorage) {
        this.contextDataStorage = (ContextDataStorage)(contextDataStorage == null ? new DefaultContextDataStorage() : contextDataStorage);
    }
}

