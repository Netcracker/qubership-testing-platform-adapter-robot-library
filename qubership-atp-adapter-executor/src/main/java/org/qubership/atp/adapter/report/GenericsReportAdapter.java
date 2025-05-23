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

package org.qubership.atp.adapter.report;

import java.lang.reflect.ParameterizedType;

public abstract class GenericsReportAdapter<T extends ReportWriter> implements ReportAdapterWithSupport {
    public GenericsReportAdapter() {
    }

    public final void write(ReportWriter writer, Object item) {
        if (this.isSupported(writer)) {
            this.writeItem((T) writer, item);
        }
    }

    protected abstract void writeItem(T var1, Object var2);

    public boolean isSupported(ReportWriter writer) {
        Class<?> thisClass = this.getClass();

        for(Class<?> breakClass = GenericsReportAdapter.class; thisClass.getSuperclass() != breakClass; thisClass = thisClass.getSuperclass()) {
        }

        Class<?> Clazz = (Class)((ParameterizedType)thisClass.getGenericSuperclass()).getActualTypeArguments()[0];
        return Clazz.isInstance(writer);
    }
}

