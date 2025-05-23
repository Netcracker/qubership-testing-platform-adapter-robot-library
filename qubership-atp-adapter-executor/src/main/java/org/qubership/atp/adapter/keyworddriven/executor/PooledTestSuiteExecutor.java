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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.qubership.atp.adapter.keyworddriven.configuration.KdtProperties;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.resources.ResourceFactory;
import org.qubership.atp.adapter.keyworddriven.resources.Resources;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.ExceptionUtils;
import org.qubership.atp.adapter.utils.ReportUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PooledTestSuiteExecutor extends SectionExecutor {
    private static final Logger log = Logger.getLogger(PooledTestSuiteExecutor.class);
    public static int threadLimit = Integer.parseInt(Config.getString("kdt.threads.count", "1"));

    public PooledTestSuiteExecutor() {
    }

    public void execute(Executable suite) throws Exception {
        Preconditions.checkState(CollectionUtils.isNotEmpty(suite.getChildren()), "Test suite has no tests to execute: " + suite);
        log.info("[START] test suite execution: " + suite);

        try {
            this.execute((Collection)suite.getChildren());
        } finally {
            ResourceFactory.getInstance().releaseResourcesAll();
            log.info("[END] test suite execution: " + suite.getName());
        }

    }

    protected void execute(Collection<Executable> testCases) throws InterruptedException {
        log.info("Test cases to be executed: " + Iterables.transform(testCases, new Function<Executable, String>() {
            @Nullable
            public String apply(@Nullable Executable input) {
                return input.getName();
            }
        }).toString());
        if (testCases.size() == 0) {
            log.warn("No one test case is specified for execution!");
        } else {
            ExecutorService threadPool = Executors.newFixedThreadPool(threadLimit);
            List<Runnable> threads = this.prepareThreadList((Iterable)testCases);
            Iterator var4 = threads.iterator();

            while(var4.hasNext()) {
                Runnable thread = (Runnable)var4.next();
                threadPool.execute(thread);
            }

            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    /** @deprecated */
    @Deprecated
    protected List<Runnable> prepareThreadList(Executable executable) {
        return this.prepareThreadList((Iterable)executable.getChildren());
    }

    protected List<Runnable> prepareThreadList(Iterable<Executable> testCases) {
        List<Runnable> threads = new ArrayList();
        Map<String, List<Executable>> threadNameMap = new LinkedHashMap();
        Iterator var4 = testCases.iterator();

        while(true) {
            while(var4.hasNext()) {
                Executable child = (Executable)var4.next();
                if (child instanceof FileTestCase) {
                    FileTestCase ftc = (FileTestCase)child;
                    if (StringUtils.isNotBlank(ftc.getThreadName())) {
                        if (threadNameMap.get(ftc.getThreadName()) != null) {
                            ((List)threadNameMap.get(ftc.getThreadName())).add(ftc);
                        } else {
                            ArrayList<Executable> list = new ArrayList();
                            list.add(ftc);
                            threadNameMap.put(ftc.getThreadName(), list);
                        }
                        continue;
                    }
                }

                threads.add(new ExecutableRunnable(new Executable[]{child}));
            }

            var4 = threadNameMap.values().iterator();

            while(var4.hasNext()) {
                List<Executable> list = (List)var4.next();
                threads.add(new ExecutableRunnable((Executable[])list.toArray(new Executable[0])));
            }

            return threads;
        }
    }

    private static void handleException(Executable executable, Throwable e) {
        executable.log().error("Error occurred during test case execution: '" + executable + "'", e);
        if (!ExceptionUtils.isHandled(e)) {
            Report.getReport().error("Error occurred during test case execution: " + executable, e);
        }

    }

    public static class ExecutableRunnable implements Runnable {
        private Executable[] executables;

        public ExecutableRunnable(Executable... executables) {
            this.executables = executables;
        }

        protected Executable get(int index) {
            return this.executables[index];
        }

        public void run() {
            Executable[] var1 = this.executables;
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Executable executable = var1[var3];

                try {
                    ReportUtils.setReportFolderName(executable.getName());
                    executable.execute();
                } catch (Throwable var9) {
                    Throwable e = var9;
                    PooledTestSuiteExecutor.handleException(executable, e);
                } finally {
                    this.releaseResources();
                }
            }

            Resources.releaseResourcesForCurrentThreadSilently();
        }

        private void releaseResources() {
            if (KdtProperties.KDT_RESOURCE_RELEASE_AFTER_TESTCASE) {
                Resources.releaseResourcesForCurrentThreadSilently();
            }

            Throwable e;
            try {
                Executable[] var7 = this.executables;
                int var2 = var7.length;

                for(int var3 = 0; var3 < var2; ++var3) {
                    Executable executable = var7[var3];
                    if (executable.getParent() != null) {
                        executable.getParent().getChildren().remove(executable);
                    }
                }
            } catch (Throwable var6) {
                e = var6;
                PooledTestSuiteExecutor.log.error("Can't release testcase from parent", e);
            }

            try {
                KeywordExecutor.removeInstance();
            } catch (Throwable var5) {
                e = var5;
                PooledTestSuiteExecutor.log.error("Can't remove KeywordRequest instance", e);
            }

        }
    }
}

