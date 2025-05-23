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

import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PatternConverter {
    private static final Log log = LogFactory.getLog(PatternConverter.class);

    private PatternConverter() {
    }

    public static String convert(Pattern pattern) {
        String str = pattern.pattern();
        int indexE = -2;
        StringBuilder sb = new StringBuilder(str.length());

        int indexQ;
        String automaton;
        while((indexQ = str.indexOf("\\Q", indexE + 2)) > -1) {
            sb.append(str.substring(indexE + 2, indexQ));
            indexE = str.indexOf("\\E", indexQ);
            automaton = str.substring(indexQ + 2, indexE).replace("\"", "\"\\\"\"");
            sb.append("\"").append(automaton).append("\"");
        }

        sb.append(str.substring(indexE + 2));
        automaton = sb.toString();
        log.trace(String.format("Convert route java regexp to automaton regexp '%s' > '%s'", str, automaton));
        return automaton;
    }
}
