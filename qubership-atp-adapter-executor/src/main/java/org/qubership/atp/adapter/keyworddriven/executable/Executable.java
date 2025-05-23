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

import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.executor.Executor;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public interface Executable {
    Executable getParent();

    void setParent(Executable var1);

    List<Executable> getChildren();

    String getName();

    void setName(String var1);

    void prepare();

    void execute() throws Exception;

    Executor getExecutor() throws TestCaseException;

    TestCase getTestCase();

    Object getParam(String var1);

    Object getParam(String var1, Object var2);

    Object setParam(String var1, Object var2);

    Map<String, Object> getNormalPriorityParams();

    void setLog(Logger var1);

    Logger log();

    void addFlag(Flag var1);

    Flag getFlag(String var1);

    void removeFlag(String var1);

    boolean hasFlag(String var1);

    List<Flag> getEnabledFlags();

    boolean isFlagInherited(String var1);
}
