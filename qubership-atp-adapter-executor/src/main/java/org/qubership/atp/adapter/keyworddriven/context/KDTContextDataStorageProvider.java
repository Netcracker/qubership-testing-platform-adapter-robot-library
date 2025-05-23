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

import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executor.KeywordExecutor;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorageProvider;
import javax.annotation.Nullable;

public class KDTContextDataStorageProvider implements ContextDataStorageProvider {
    private static ThreadLocal<Executable> executable = new ThreadLocal();
    private static KDTLocalContextDataStorage storage = new KDTLocalContextDataStorage();

    public KDTContextDataStorageProvider() {
    }

    public static void setExecutable(Executable executable) {
        KDTContextDataStorageProvider.executable.set(executable);
    }

    static Executable get() {
        Executable e = (Executable)executable.get();
        if (e == null) {
            e = KeywordExecutor.getKeyword();
        }

        if (e == null) {
            throw new NullPointerException("Executable context is unknown. Executable should be defined in KeywordExecutor or in KDTContextDataStorageProvider.");
        } else {
            return (Executable)e;
        }
    }

    @Nullable
    public ContextDataStorage getContextDataStorage() {
        return storage;
    }
}
