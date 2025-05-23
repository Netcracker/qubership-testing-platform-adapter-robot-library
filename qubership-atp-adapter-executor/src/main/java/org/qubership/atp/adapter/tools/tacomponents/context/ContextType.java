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

import com.google.common.base.Preconditions;
import org.qubership.atp.adapter.tools.tacomponents.context.threading.ScopeModel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum ContextType {
    GLOBAL(new GlobalContextProvider(ScopeModel.PRIMITIVE)),
    LOCAL(new LocalContextProvider(ScopeModel.PRIMITIVE));

    private ContextDataStorageProvider contextProvider;
    @Nonnull
    private static ScopeModel scopeModel = ScopeModel.PRIMITIVE;

    private ContextType(@Nonnull ContextDataStorageProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public static void setScopeModel(@Nonnull ScopeModel model) {
        resetProviders(model);
    }

    public static void resetProviders(@Nonnull ScopeModel scopeModel) {
        ContextType.scopeModel = scopeModel;
        GLOBAL.setContextProvider(produceContextProvider(GLOBAL, scopeModel));
        LOCAL.setContextProvider(produceContextProvider(LOCAL, scopeModel));
    }

    public void setContextProvider(@Nonnull ContextDataStorageProvider provider) {
        Preconditions.checkNotNull(this.contextProvider, "Context storage provider cannot be null!");
        this.contextProvider = provider;
    }

    @Nonnull
    public static ScopeModel getScopeModel() {
        return scopeModel;
    }

    @Nullable
    ContextDataStorage getStorage() {
        return this.contextProvider.getContextDataStorage();
    }

    @Nonnull
    public static ContextProvider produceContextProvider(@Nonnull ContextType contextType, @Nonnull ScopeModel model) {
        return (ContextProvider)(contextType == GLOBAL ? new GlobalContextProvider(model) : new LocalContextProvider(model));
    }
}

