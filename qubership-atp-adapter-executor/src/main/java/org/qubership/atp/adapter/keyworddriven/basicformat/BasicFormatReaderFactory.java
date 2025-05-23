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

import com.google.inject.Inject;
import org.qubership.atp.adapter.excel.ExcelBook;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import java.io.File;
import java.io.IOException;

public class BasicFormatReaderFactory {
    @Inject
    private static BasicFormatReaderFactory instance = null;

    protected BasicFormatReaderFactory() {
    }

    public static BasicFormatReaderFactory getInstance() {
        if (instance == null) {
            Class var0 = BasicFormatReaderFactory.class;
            synchronized(BasicFormatReaderFactory.class) {
                if (instance == null) {
                    instance = new BasicFormatReaderFactory();
                }
            }
        }

        return instance;
    }

    public TestSuiteReader newTestSuiteReader(String fileName) throws InvalidFormatOfSourceException {
        return (TestSuiteReader)(fileName.endsWith(".txt") ? new TestSuiteTextReader(fileName) : new BasicFormatTestSuiteReader(fileName));
    }

    public TestSuiteReader newFlowTestSuiteReader(String fileName, String directory) throws InvalidFormatOfSourceException {
        return new FlowTestSuiteReader(fileName, directory);
    }

    public TestCaseReader newTestCaseReader(String filename) throws InvalidFormatOfSourceException {
        return new BasicFormatTestCaseReader(filename);
    }

    public BasicFormatKeywordsReader newKeywordsReader(ExcelBook book, String sheetName) throws InvalidFormatOfSourceException {
        return new BasicFormatKeywordsReader(book, sheetName);
    }

    /** @deprecated */
    @Deprecated
    public TestCaseReader newPlainTextReader(String filename) throws IOException {
        return new PlainTextReader(new File(filename));
    }

    public ParametersReader getDataSetReader(String dataSetFile) {
        return new TextDataSetReader(dataSetFile);
    }
}
