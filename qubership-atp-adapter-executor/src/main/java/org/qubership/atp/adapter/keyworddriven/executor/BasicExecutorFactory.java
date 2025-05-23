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
import org.qubership.atp.adapter.keyworddriven.executable.ExcelKeyword;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.SectionImpl;
import org.qubership.atp.adapter.keyworddriven.executable.StringKeyword;
import org.qubership.atp.adapter.keyworddriven.executable.TestCaseImpl;
import org.qubership.atp.adapter.keyworddriven.executable.TestSuite;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BasicExecutorFactory extends ExecutorFactory {
    protected Map<Class<? extends Executable>, Executor> executors;

    protected BasicExecutorFactory() {
        KeywordExecutor keywordExecutor = new KeywordExecutor();
        TestCaseExecutor testCaseExecutor = new TestCaseExecutor();
        this.executors = new HashMap();
        this.addExecutor(TestSuite.class, new BeforeAfterTestSuiteExecutor());
        this.addExecutor(FileTestCase.class, testCaseExecutor);
        this.addExecutor(SectionImpl.class, new SectionExecutor());
        this.addExecutor(StringKeyword.class, keywordExecutor);
        this.addExecutor(ExcelKeyword.class, keywordExecutor);
        this.addExecutor(TestCaseImpl.class, testCaseExecutor);
    }

    public Executor getExecutor(Class<? extends Executable> clazz) throws TestCaseException {
        try {
            Executor exe = (Executor)this.executors.get(clazz);
            if (exe == null) {
                throw new TestCaseException("Executor for " + clazz + " class not found");
            } else {
                return exe;
            }
        } catch (ClassCastException var3) {
            ClassCastException e = var3;
            throw new TestCaseException("Executor for " + clazz + " class has inapropriate type : " + ((Executor)this.executors.get(clazz)).getClass(), e);
        }
    }

    public Executor addExecutor(Class<? extends Executable> clazz, Executor exe) {
        return (Executor)this.executors.put(clazz, exe);
    }

    public Collection<Executor> getExecutors() {
        return this.executors.values();
    }
}

