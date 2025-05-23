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

import org.qubership.atp.adapter.keyworddriven.executable.StringKeyword;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** @deprecated */
@Deprecated
public class PlainTextReader implements TestCaseReader {
    private static Log log = LogFactory.getLog(PlainTextReader.class);
    private String text;

    public PlainTextReader(String text) {
        this.text = text;
    }

    public PlainTextReader(File file) throws IOException {
        this.text = FileUtils.readFileToString(file);
    }

    public void fillTestCase(TestCase testCase) {
        String[] rows = this.text.split(System.getProperty("line.separator"));
        String[] var3 = rows;
        int var4 = rows.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String row = var3[var5];
            if (StringUtils.isNotBlank(row)) {
                List<String> data = BasicFormatKeywordsReader.parseKeyword(row.replaceAll("\t", " ").trim());
                if (data == null) {
                    log.warn("Skipped row " + row);
                } else {
                    new StringKeyword(testCase, data);
                }
            }
        }

    }
}

