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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.qubership.atp.adapter.keyworddriven.basicformat.flags.TestCaseType;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.resources.ResourceFactory;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

public class BeforeAfterTestSuiteExecutor extends PooledTestSuiteExecutor {
    private static final Logger log = Logger.getLogger(PooledTestSuiteExecutor.class);

    public BeforeAfterTestSuiteExecutor() {
    }

    public void execute(Executable suite) throws Exception {
        Preconditions.checkState(CollectionUtils.isNotEmpty(suite.getChildren()), "Test suite has no tests to execute: " + suite);
        log.info("[START] test suite execution: " + suite);

        try {
            log.info("[START] Prerequisites execution");
            this.execute(Collections2.filter(suite.getChildren(), by(TestCaseType.PRE_CONDITION)));
            log.info("[START] Prerequisites execution");
            log.info("[START] test cases execution");
            this.execute(Collections2.filter(suite.getChildren(), by(TestCaseType.TEST_CASE)));
            log.info("[END] test cases execution");
            log.info("[START] Post requisites execution");
            this.execute(Collections2.filter(suite.getChildren(), by(TestCaseType.POST_CONDITION)));
            log.info("[END] Post requisites execution");
        } finally {
            ResourceFactory.getInstance().releaseResourcesAll();
            log.info("[END] test suite execution: " + suite.getName());
        }

    }

    protected static Predicate<Executable> by(final TestCaseType testCaseType) {
        return new Predicate<Executable>() {
            public boolean apply(@Nullable Executable input) {
                return testCaseType == TestCaseType.matches(input);
            }
        };
    }
}

