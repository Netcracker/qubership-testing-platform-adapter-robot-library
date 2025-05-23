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

import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.StringKeyword;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestCaseTextReader implements TestCaseReader {
    public static final Log log = LogFactory.getLog(TestCaseTextReader.class);
    FileTestCase testCase;
    List<String> dataList;
    StringKeyword keyword;
    List<StringKeyword> keywordList = new ArrayList();

    public TestCaseTextReader(List<String> dataList) {
        this.dataList = dataList;
    }

    public List<StringKeyword> getKeywordList() {
        return this.keywordList;
    }

    public StringKeyword getKeyword() {
        return this.keyword;
    }

    public List<String> getDataList() {
        return this.dataList;
    }

    public void fillTestCase(TestCase tCase) throws InvalidFormatOfSourceException {
        log.info("[START] Read test case: " + tCase);
        this.testCase = (FileTestCase)tCase;
        Iterator var2 = this.dataList.iterator();

        while(var2.hasNext()) {
            String aDataList = (String)var2.next();
            String currentLine = aDataList.trim();
            if (StringUtils.isNotBlank(currentLine)) {
                TextKeywordsReader keywordsReader = new TextKeywordsReader(this.testCase, currentLine);
                StringKeyword newKeyword = keywordsReader.getKeyword();
                if (newKeyword != null) {
                    this.keyword = newKeyword;
                    this.keywordList.add(this.keyword);
                } else {
                    log.error("Keyword not found :" + currentLine);
                }
            }
        }

        log.info("[END] Read test case: " + tCase);
    }
}

