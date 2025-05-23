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

package org.qubership.atp.adapter.keyworddriven.routing;

import org.qubership.atp.adapter.testcase.Config;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.math.NumberUtils;

public class RouteItem {
    private static boolean escapeConstants = Boolean.parseBoolean(Config.getString("kdt.quote.constants", "true"));
    private static final String paramNameGroup = "^\\[(.*)\\]";
    private static final String cellPatternGroup = "(?:\\((.*)\\))?";
    private static final String cellCountGroup = "(?:\\{(.*)\\})?";
    private static final Pattern PARAM_PATTERN = Pattern.compile("^^\\[(.*)\\](?:\\((.*)\\))?(?:\\{(.*)\\})?$");
    private int cellCount = 1;
    private Pattern cellContentPattern = Pattern.compile("(?s).*?");
    private boolean isParameter = false;
    private String paramName;
    private Object source;

    public RouteItem(Object source) {
        this.source = source;
        if (source instanceof String) {
            this.parseMaskItem((String)source);
        }

    }

    protected void parseMaskItem(String source) {
        Matcher m = PARAM_PATTERN.matcher(source);
        if (m.matches()) {
            this.isParameter = true;
            this.paramName = m.group(1).trim();
            if (m.group(3) != null) {
                this.cellCount = NumberUtils.toInt(m.group(3).trim(), 0);
            }

            if (m.group(2) != null) {
                this.cellContentPattern = Pattern.compile("(" + m.group(2).trim() + ")");
            } else {
                this.cellContentPattern = Pattern.compile("(.*)");
            }
        } else {
            this.cellContentPattern = Pattern.compile(escapeConstants ? Pattern.quote(source) : source);
        }

    }

    public String toString() {
        return String.valueOf(this.source);
    }

    public int getOccupiedCellsCount() {
        return this.cellCount;
    }

    public Pattern getCellContentPattern() {
        return this.cellContentPattern;
    }

    public boolean isParameter() {
        return this.isParameter;
    }

    public Object getSource() {
        return this.source;
    }

    public String getParamName() {
        return this.paramName;
    }
}

