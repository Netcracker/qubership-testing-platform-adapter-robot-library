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
import org.qubership.atp.adapter.keyworddriven.executable.StringKeyword;
import org.qubership.atp.adapter.keyworddriven.routing.Route;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TextKeywordsReader {
    private static final Log log = LogFactory.getLog(TextKeywordsReader.class);
    public static final Pattern PATTERN_PARAMETERS = Pattern.compile("(x?) ( \" ( ( \\\\. | [^\"\\\\] )* ) \" ) | ( ' ( \\\\. | [^'\\\\] )* ' ) | ( \\$\\{ ([^}])* } )", 4);
    private final StringKeyword keyword;
    private Executable parent;

    public TextKeywordsReader(Executable parent, String string) {
        List<String> stringKeywordList = this.fillKeyword(parent.getTestCase(), string);
        if (stringKeywordList != null) {
            this.keyword = new StringKeyword(parent, this.deleteQuote(stringKeywordList));
            this.keyword.setName(string);
        } else {
            this.keyword = null;
        }

    }

    private List<String> deleteQuote(List<String> keywords) {
        List<String> repStringList = new ArrayList(keywords.size());
        Iterator var3 = keywords.iterator();

        while(var3.hasNext()) {
            String item = (String)var3.next();
            repStringList.add(trimQuotes(item));
        }

        return repStringList;
    }

    public static String trimQuotes(String item) {
        return (!item.startsWith("\"") || !item.endsWith("\"")) && (!item.startsWith("'") || !item.endsWith("'")) ? item : item.substring(1, item.length() - 1);
    }

    public Executable getParent() {
        return this.parent;
    }

    public StringKeyword getKeyword() {
        return this.keyword;
    }

    public List<String> fillKeyword(Executable parent, String str) {
        List<String> keys = new ArrayList();
        this.parent = parent;
        if (StringUtils.isNotBlank(str)) {
            if (!Route.IS_SPACE_DELIM_ENABLED) {
                str = replaceSpaceOnTab(str.trim());
            }

            keys = BasicFormatKeywordsReader.parseKeyword(str.trim());
        }

        return (List)keys;
    }

    public static String replaceSpaceOnTab(String str) {
        Matcher matcher = PATTERN_PARAMETERS.matcher(str);
        StringBuilder result = new StringBuilder(str.length() * 2);

        int begin;
        for(begin = 0; matcher.find(); begin = matcher.end()) {
            result.append(str.substring(begin, matcher.start()).replace(" ", "\t"));
            result.append(matcher.group());
        }

        result.append(str.substring(begin).replace(" ", "\t"));
        return result.toString();
    }
}

