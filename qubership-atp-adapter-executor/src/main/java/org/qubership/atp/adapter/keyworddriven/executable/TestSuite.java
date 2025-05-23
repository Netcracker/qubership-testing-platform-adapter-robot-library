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

package org.qubership.atp.adapter.keyworddriven.executable;

import org.qubership.atp.adapter.keyworddriven.configuration.KdtProperties;
import org.qubership.atp.adapter.tools.tacomponents.context.Context;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextType;

public class TestSuite extends ExecutableImpl {
    public TestSuite(String name) {
        this(name, (Executable)null);
    }

    public TestSuite(String name, Executable parent) {
        super(name, parent);
    }

    public String toString() {
        return this.getName();
    }

    public Object setParam(String key, Object value) {
        if (KdtProperties.KDT_CONTEXT_TYPE_IS_NEW) {
            ContextDataStorage storage = Context.getStorage(ContextType.GLOBAL);
            Object oldValue = storage.getValue(key);
            storage.putValue(key, value);
            return oldValue;
        } else {
            return super.setParam(key, value);
        }
    }
}

