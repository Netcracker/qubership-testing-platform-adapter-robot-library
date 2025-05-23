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

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.configuration.KdtProperties;
import org.qubership.atp.adapter.keyworddriven.context.KDTContextDataStorageProvider;
import org.qubership.atp.adapter.keyworddriven.executor.Executor;
import org.qubership.atp.adapter.keyworddriven.executor.ExecutorFactory;
import org.qubership.atp.adapter.report.InterruptScenarioException;
import org.qubership.atp.adapter.tools.tacomponents.context.Context;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextDataStorage;
import org.qubership.atp.adapter.tools.tacomponents.context.ContextType;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

public abstract class ExecutableImpl implements Executable {
    private Map<String, Object> executeParam = new LinkedHashMap();
    private String name;
    private List<Executable> childrens = new ArrayList();
    private WeakReference<Executable> parent = null;
    private Logger logger;
    private Map<String, Flag> flags = Maps.newHashMapWithExpectedSize(2);

    public ExecutableImpl(String name, Executable parent) {
        this.name = name;
        this.setParent(parent);
    }

    public void setParent(Executable parent) {
        this.parent = new WeakReference(parent);
        if (parent != null) {
            parent.getChildren().add(this);
        }

    }

    public List<Executable> getChildren() {
        return this.childrens;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Executable getParent() {
        return (Executable)this.parent.get();
    }

    public void execute() throws Exception {
        try {
            this.getExecutor().executeBefore(this);
            this.getExecutor().execute(this);
        } catch (Exception e) {
            this.log().error("Failed to execute: " + this, e);
            throw e;
        } finally {
            this.getExecutor().executeAfter(this);
        }

    }

    public void prepare() {
        try {
            this.getExecutor().prepare(this);
        } catch (InterruptScenarioException var2) {
            InterruptScenarioException e = var2;
            throw e;
        } catch (Exception var3) {
            Exception e = var3;
            this.log().error(this.getClass().toString(), e);
        }

    }

    public Executor getExecutor() throws TestCaseException {
        return ExecutorFactory.getInstance().getExecutor(this.getClass());
    }

    public TestCase getTestCase() {
        if (this instanceof TestCase) {
            return (TestCase)this;
        } else {
            return this.getParent() == null ? null : this.getParent().getTestCase();
        }
    }

    public Object getParam(String key) {
        Object result;
        if (KdtProperties.KDT_CONTEXT_TYPE_IS_NEW) {
            KDTContextDataStorageProvider.setExecutable(this);
            result = Context.getValue(key);
            return result == null ? Context.getValue(key, ContextType.GLOBAL) : result;
        } else {
            result = this.executeParam.get(key);
            return result == null && this.getParent() != null ? this.getParent().getParam(key) : result;
        }
    }

    public Object getParam(String key, Object defaultValue) {
        Object result = this.getParam(key);
        return result == null ? defaultValue : result;
    }

    public Object setParam(String key, Object value) {
        if (KdtProperties.KDT_CONTEXT_TYPE_IS_NEW) {
            KDTContextDataStorageProvider.setExecutable(this);
            ContextDataStorage storage = Context.getStorage(ContextType.LOCAL);
            Object oldValue = storage.getValue(key);
            storage.putValue(key, value);
            return oldValue;
        } else {
            return this.executeParam.put(key, value);
        }
    }

    public Map<String, Object> getNormalPriorityParams() {
        if (KdtProperties.KDT_CONTEXT_TYPE_IS_NEW) {
            KDTContextDataStorageProvider.setExecutable(this);
            ContextDataStorage storage = Context.getStorage(ContextType.LOCAL);
            return storage.getValues();
        } else {
            return this.executeParam;
        }
    }

    public Logger log() {
        if (this.getParent() == null && this.logger == null) {
            return Logger.getLogger(this.getClass());
        } else {
            return this.logger == null ? this.getParent().log() : this.logger;
        }
    }

    public void setLog(Logger logger) {
        this.logger = logger;
    }

    public void addFlag(Flag flag) {
        if (!this.flags.containsKey(flag.getName())) {
            this.flags.put(flag.getName(), flag);
        }

    }

    public Flag getFlag(String name) {
        Flag flag = (Flag)this.flags.get(name);
        if (flag != null) {
            if (flag.isEnabled()) {
                return flag;
            }
        } else if (this.getParent() != null) {
            return this.getParent().getFlag(name);
        }

        return null;
    }

    public void removeFlag(String name) {
        this.flags.remove(name);
    }

    public boolean hasFlag(String name) {
        return this.getFlag(name) != null;
    }

    public List<Flag> getEnabledFlags() {
        List<Flag> resultFlags = new ArrayList(this.flags.size());
        if (this.getParent() != null) {
            resultFlags.addAll(CollectionUtils.subtract(this.getParent().getEnabledFlags(), this.flags.values()));
        }

        resultFlags.addAll(Collections2.filter(this.flags.values(), Flag::isEnabled));
        return resultFlags;
    }

    public boolean isFlagInherited(String name) {
        if (this.getParent() == null) {
            return false;
        } else {
            boolean parentHas = this.getParent().hasFlag(name);
            Flag _flag = (Flag)this.flags.get(name);
            return _flag == null && parentHas;
        }
    }
}

