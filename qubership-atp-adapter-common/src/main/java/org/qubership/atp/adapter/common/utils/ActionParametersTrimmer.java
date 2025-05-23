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

package org.qubership.atp.adapter.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ActionParametersTrimmer {

    private static final String ACTION_PARAMETERS_REGEXP = "(\"([\\s\\S\"])*?[^\\\\]\")|('([\\s\\S\"])*?[^\\\\]')";
    private static final Pattern ACTION_PARAMETERS_PATTERN = Pattern.compile(ACTION_PARAMETERS_REGEXP);

    private final int parameterValueSizeLimitChar;

    /**
     * Instantiates a new Action parameters trimmer.
     *
     * @param parameterValueSizeLimitChar the parameter value size limit char
     */
    public ActionParametersTrimmer(Integer parameterValueSizeLimitChar) {
        this.parameterValueSizeLimitChar = parameterValueSizeLimitChar;
    }

    /**
     * Trim action parameters by limit string.
     *
     * @param actionName the action name
     * @return the string
     */
    public String trimActionParametersByLimit(String actionName) {
        String result = actionName;
        if (result != null && result.length() > parameterValueSizeLimitChar) {
            StringBuilder newName = new StringBuilder();
            Matcher matcher = ACTION_PARAMETERS_PATTERN.matcher(result);
            int position = 0;
            while (matcher.find()) {
                int start = matcher.start() + 1;
                newName.append(result, position, start);

                int end = matcher.end() - 1;
                String value = StringUtils.abbreviate(result.substring(start, end), parameterValueSizeLimitChar);
                newName.append(value);

                position = end;
            }
            if (position < result.length()) {
                newName.append(result, position, result.length());
            }

            result = newName.toString();
        }

        return result;
    }
}
