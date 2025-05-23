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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ContextRecord<T> implements Serializable {
    private static final long serialVersionUID = -1049577909010590824L;
    @Nonnull
    private String key;
    private T value;

    public ContextRecord(@Nonnull String key, @Nullable T value) {
        this.key = key;
        this.value = value;
    }

    @Nonnull
    public String getKey() {
        return this.key;
    }

    void setKey(@Nonnull String newKey) {
        this.key = newKey;
    }

    @Nullable
    public T getValue() {
        return this.value;
    }

    public void setValue(@Nullable T newValue) {
        this.value = newValue;
    }

    public void setVisible(boolean visible) {
        if (visible) {
            this.setVisible();
        } else {
            this.setInvisible();
        }

    }

    public void setVisible() {
        ContextDataStorage dsLocal = ContextType.LOCAL.getStorage();
        ContextDataStorage dsGlobal = ContextType.GLOBAL.getStorage();
        Preconditions.checkNotNull(dsLocal);
        Preconditions.checkNotNull(dsGlobal);
        dsLocal.removeRecord(this.getKey());
        dsGlobal.putRecord(this.getKey(), this);
    }

    public void setInvisible() {
        ContextDataStorage dsLocal = ContextType.LOCAL.getStorage();
        ContextDataStorage dsGlobal = ContextType.GLOBAL.getStorage();
        Preconditions.checkNotNull(dsLocal);
        Preconditions.checkNotNull(dsGlobal);
        dsGlobal.removeRecord(this.getKey());
        dsLocal.putRecord(this.getKey(), this);
    }

    @Nonnull
    public static <T> Map<String, T> convertRecordsToValues(@Nonnull Map<String, ContextRecord<T>> originalMap) {
        Map<String, T> resultMap = new HashMap();
        Iterator var2 = originalMap.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, ContextRecord<T>> originalEntry = (Map.Entry)var2.next();
            resultMap.put(originalEntry.getKey(), (T)((ContextRecord)originalEntry.getValue()).getValue());
        }

        return resultMap;
    }
}

