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

import com.google.common.eventbus.EventBus;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.tools.tacomponents.context.Context;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextRecord;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextType;
import org.qubership.atp.adapter.tools.tacomponents.context.DefaultContextDataStorage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KDTLocalContextDataStorage implements ContextDataStorage {
    protected static final Map<Executable, ContextDataStorage> map = Collections.synchronizedMap(new WeakHashMap());
    private EventBus eventBus = new EventBus();

    public KDTLocalContextDataStorage() {
    }

    protected ContextDataStorage getStorage() {
        return this.getStorage(KDTContextDataStorageProvider.get());
    }

    protected ContextDataStorage getStorage(Executable keyword) {
        synchronized(map) {
            return (ContextDataStorage)map.computeIfAbsent(keyword, (k) -> {
                return new DefaultContextDataStorage();
            });
        }
    }

    protected <T> T get(Executable e, Function<ContextDataStorage, T> getFromContext) {
        if (e == null) {
            return null;
        } else {
            T value = getFromContext.apply(this.getStorage(e));
            if (value == null && e.getParent() != null) {
                return this.get(e.getParent(), getFromContext);
            } else {
                return value == null && e.getParent() == null ? getFromContext.apply(Context.getStorage(ContextType.GLOBAL)) : value;
            }
        }
    }

    public <T> ContextRecord<T> putValue(@Nonnull final String key, @Nullable final T value) {
        return (ContextRecord)this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, ContextRecord<T>>() {
            @Nullable
            public ContextRecord<T> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.putValue(key, value);
            }
        });
    }

    public <T> void putValues(@Nonnull Map<String, T> map) {
        map.forEach(this::putValue);
    }

    @Nullable
    public <T> T getValue(@Nonnull final String key) {
        return this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, T>() {
            @Nullable
            public T apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getValue(key);
            }
        });
    }

    @Nullable
    public <T> T getValue(@Nonnull final String key, @Nonnull T defaultValue) {
        Object value = this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, T>() {
            @Nullable
            public T apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getValue(key);
            }
        });
        return value != null ? (T) value : defaultValue;
    }

    @Nonnull
    public Map<String, Object> getValues() {
        return (Map)this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, Map<String, Object>>() {
            @Nullable
            public Map<String, Object> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getValues();
            }
        });
    }

    public <T> ContextRecord<T> putRecord(@Nonnull final String key, @Nonnull final ContextRecord<T> record) {
        return (ContextRecord)this.get(KDTContextDataStorageProvider.get(), new Function<ContextDataStorage, ContextRecord<T>>() {
            @Nullable
            public ContextRecord<T> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.putRecord(key, record);
            }
        });
    }

    public <T> void putRecords(@Nonnull Map<String, ContextRecord<T>> map) {
        this.getStorage().putRecords(map);
    }

    @Nullable
    public <T> ContextRecord<T> getRecord(@Nonnull final String key) {
        return (ContextRecord)this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, ContextRecord<T>>() {
            @Nullable
            public ContextRecord<T> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getRecord(key);
            }
        });
    }

    @Nonnull
    public <T> ContextRecord<T> getRecord(@Nonnull final String key, @Nonnull final ContextRecord<T> defaultRecord) {
        return (ContextRecord)this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, ContextRecord<T>>() {
            @Nullable
            public ContextRecord<T> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getRecord(key, defaultRecord);
            }
        });
    }

    @Nonnull
    public Map<String, ContextRecord<?>> getRecords() {
        return (Map)this.get(KDTContextDataStorageProvider.get(), new com.google.common.base.Function<ContextDataStorage, Map<String, ContextRecord<?>>>() {
            @Nullable
            public Map<String, ContextRecord<?>> apply(@Nullable ContextDataStorage contextDataStorage) {
                return contextDataStorage.getRecords();
            }
        });
    }

    public void removeRecord(@Nonnull String key) {
        this.getStorage().removeRecord(key);
    }

    public void clear() {
        this.getStorage().clear();
    }

    @Nonnull
    public EventBus getEventBus() {
        return this.eventBus;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        throw new UnsupportedOperationException("writeExternal does not supported in KDT because no ATP integration here");
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("readExternal does not supported in KDT because no ATP integration here");
    }
}

