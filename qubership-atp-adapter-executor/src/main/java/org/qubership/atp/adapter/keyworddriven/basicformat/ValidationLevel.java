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

public enum ValidationLevel {
    no(10),
    full(5),
    smoke(0),
    yes(-10);

    public int lvl;

    private ValidationLevel(int levelNum) {
        this.lvl = levelNum;
    }

    public static int parseValidationLevel(String validationLevelName) throws IllegalArgumentException {
        if (validationLevelName != null && !validationLevelName.trim().isEmpty()) {
            try {
                return valueOf(validationLevelName.toLowerCase()).lvl;
            } catch (IllegalArgumentException var4) {
                IllegalArgumentException e = var4;

                try {
                    return Integer.parseInt(validationLevelName);
                } catch (NumberFormatException var3) {
                    throw new IllegalArgumentException("Unknown validation level = '" + validationLevelName + "'", e);
                }
            }
        } else {
            return smoke.lvl;
        }
    }
}
