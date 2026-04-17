/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.adapter.keyworddriven.databinder;

import java.lang.reflect.Type;
import java.util.HashMap;

import org.qubership.atp.adapter.keyworddriven.executable.KeywordParameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Calculators {
    private static final HashMap<Type, DataBinder<?>> calculators = new HashMap<>();

    public Calculators() {
    }

    public static DataBinder<?> getCalculator(Type type) {
        return calculators.get(type);
    }

    public static void addCalculator(DataBinder<?> calculator) {
        try {
            Type type = calculator.getClass().getMethod("calculate", KeywordParameter.class).getGenericReturnType();
            addCalculator(type, calculator);
        } catch (SecurityException | NoSuchMethodException e) {
            log.error(Calculators.class.toString(), e);
        }
    }

    protected static void addCalculator(Type type, DataBinder<?> calculator) {
        calculators.put(type, calculator);
    }
}

