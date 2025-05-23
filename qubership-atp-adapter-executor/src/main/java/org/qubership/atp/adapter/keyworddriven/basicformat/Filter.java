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

package org.qubership.atp.adapter.keyworddriven.basicformat;

import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.keyworddriven.executor.KeywordExecutor;
import org.apache.commons.lang3.ArrayUtils;

public abstract class Filter<T> {
    private Filter() {
    }

    public static <T> Filter or(final Filter<T>... filters) {
        return new Filter<T>() {
            public boolean match(T object) {
                Filter[] var2 = filters;
                int var3 = var2.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    Filter<T> filter = var2[var4];
                    if (filter.match(object)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public static <T extends Executable> Filter<T> executableByName(final String... names) {
        return new Filter<T>() {
            public boolean match(T executable) {
                return ArrayUtils.contains(names, executable.getName());
            }
        };
    }

    public static <T> Filter<T> noFilter() {
        return new Filter<T>() {
            public boolean match(T o) {
                return true;
            }
        };
    }

    public static <T extends Section> Filter<T> filterSectionByValidationLevel() {
        return new Filter<T>() {
            public boolean match(T section) {
                int level = section.getValidationLevel();
                if (level > KeywordExecutor.validationLevel) {
                    section.log().debug(String.format("Section '%s' was skipped because validation level of it is bigger than execution level (%d > %d)", section.getName(), level, KeywordExecutor.validationLevel));
                    return false;
                } else {
                    return true;
                }
            }
        };
    }

    public abstract boolean match(T var1);
}

