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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import org.qubership.atp.adapter.tools.tacomponents.context.events.ClearValuesEvent;
import org.qubership.atp.adapter.tools.tacomponents.context.events.PutAllValuesEvent;
import org.qubership.atp.adapter.tools.tacomponents.context.events.PutValueEvent;
import org.qubership.atp.adapter.tools.tacomponents.context.events.RemoveValueEvent;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultContextDataStorage implements ContextDataStorage {
    private static final long serialVersionUID = 6784082206451114536L;
    private static final transient Function<ContextRecord<?>, Object> RECORD_TRANSFORMER = new Function<ContextRecord<?>, Object>() {
        @Nullable
        public Object apply(@Nonnull ContextRecord<?> input) {
            return input.getValue();
        }
    };
    private final Map<String, ContextRecord<?>> records = new HashMap();
    private final transient Map<String, Object> proxyView;
    private final transient EventBus eventBus;

    public DefaultContextDataStorage() {
        this.proxyView = Maps.transformValues(this.records, RECORD_TRANSFORMER);
        this.eventBus = new EventBus(this.toString());
    }

    public <T> DefaultContextDataStorage(@Nullable Map<String, T> rawMap) {
        this.proxyView = Maps.transformValues(this.records, RECORD_TRANSFORMER);
        this.eventBus = new EventBus(this.toString());
        if (rawMap != null) {
            this.putValues(rawMap);
        }

    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.records.clear();
        this.records.putAll((Map)in.readObject());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.records);
    }

    public <T> ContextRecord<T> putValue(@Nonnull String key, @Nullable T value) {
        ContextRecord<T> newRecord = new ContextRecord(key, value);
        this.records.put(key, newRecord);
        this.eventBus.post(new PutValueEvent(key, value, this));
        return newRecord;
    }

    public <T> void putValues(@Nonnull Map<String, T> rawMap) {
        Iterator var2 = rawMap.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, T> entry = (Map.Entry)var2.next();
            if (entry.getKey() != null) {
                this.putValue((String)entry.getKey(), entry.getValue());
            }
        }

    }

    @Nullable
    public <T> T getValue(@Nonnull String key) {
        ContextRecord record = (ContextRecord)this.records.get(key);
        return record == null ? null : (T) record.getValue();
    }

    @Nullable
    public <T> T getValue(@Nonnull String key, @Nonnull T defaultValue) {
        ContextRecord record = (ContextRecord)this.records.get(key);
        return record == null ? defaultValue : (T) record.getValue();
    }

    @Nonnull
    public Map<String, Object> getValues() {
        return this.proxyView;
    }

    public <T> ContextRecord<T> putRecord(@Nonnull String key, @Nonnull ContextRecord<T> record) {
        record.setKey(key);
        this.records.put(key, record);
        this.eventBus.post(new PutValueEvent(key, record.getValue(), this));
        return record;
    }

    public <T> void putRecords(@Nonnull Map<String, ContextRecord<T>> recordMap) {
        this.records.putAll(recordMap);
        Map<String, T> rawValues = ContextRecord.convertRecordsToValues(recordMap);
        this.eventBus.post(new PutAllValuesEvent(rawValues, this));
    }

    @Nullable
    public <T> ContextRecord<T> getRecord(@Nonnull String key) {
        return (ContextRecord)this.records.get(key);
    }

    @Nonnull
    public <T> ContextRecord<T> getRecord(@Nonnull String key, @Nonnull ContextRecord<T> defaultRecord) {
        ContextRecord record = (ContextRecord)this.records.get(key);
        return record == null ? defaultRecord : record;
    }

    @Nonnull
    public Map<String, ContextRecord<?>> getRecords() {
        return this.records;
    }

    public void removeRecord(@Nonnull String key) {
        this.records.remove(key);
        this.eventBus.post(new RemoveValueEvent(key, this));
    }

    public void clear() {
        this.records.clear();
        this.eventBus.post(new ClearValuesEvent(this));
    }

    @Nonnull
    public EventBus getEventBus() {
        return this.eventBus;
    }
}

