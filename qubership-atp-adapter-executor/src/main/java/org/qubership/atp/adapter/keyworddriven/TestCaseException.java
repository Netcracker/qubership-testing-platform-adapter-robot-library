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

package org.qubership.atp.adapter.keyworddriven;

import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import org.qubership.atp.adapter.keyworddriven.executor.KeywordExecutor;

public class TestCaseException extends Exception {
    private static final long serialVersionUID = -7935665405480044065L;
    private final Keyword keyword = KeywordExecutor.getKeyword();

    public TestCaseException() {
    }

    public TestCaseException(String message) {
        super(message);
    }

    public TestCaseException(Throwable e) {
        super(e);
    }

    public TestCaseException(String message, Throwable e) {
        super(message, e);
    }

    public Keyword getKeyword() {
        return this.keyword;
    }
}
