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

package org.qubership.atp.adapter.keyworddriven.executor;

import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.testcase.Config;
import java.util.Collection;

public abstract class ExecutorFactory {
    public static String EXECUTOR_FACTORY = "executor.factory";
    private static ExecutorFactory instance;

    public static ExecutorFactory getInstance() throws TestCaseException {
        if (instance == null) {
            String executorFactoryName = Config.getString(EXECUTOR_FACTORY, BasicExecutorFactory.class.getName());

            Class executorFactoryClazz;
            try {
                executorFactoryClazz = Class.forName(executorFactoryName);
            } catch (ClassNotFoundException var8) {
                throw new TestCaseException("Execute factory '" + executorFactoryName + "' not found");
            }

            Class var2 = ExecutorFactory.class;
            synchronized(ExecutorFactory.class) {
                if (instance == null) {
                    try {
                        instance = (ExecutorFactory)executorFactoryClazz.newInstance();
                    } catch (InstantiationException var5) {
                        InstantiationException e = var5;
                        throw new TestCaseException("Factory " + executorFactoryName + " instantiation failed", e);
                    } catch (IllegalAccessException var6) {
                        IllegalAccessException e = var6;
                        throw new TestCaseException("Factory " + executorFactoryName + " instantiation failed", e);
                    }
                }
            }
        }

        return instance;
    }

    protected ExecutorFactory() {
    }

    public abstract Collection<Executor> getExecutors();

    public abstract Executor getExecutor(Class<? extends Executable> var1) throws TestCaseException;

    public abstract Executor addExecutor(Class<? extends Executable> var1, Executor var2);
}

