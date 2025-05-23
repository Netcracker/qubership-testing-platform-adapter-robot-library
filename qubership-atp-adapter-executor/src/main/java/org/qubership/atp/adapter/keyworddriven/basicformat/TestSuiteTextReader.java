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
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import org.qubership.atp.adapter.keyworddriven.executable.TestSuite;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.KDTUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestSuiteTextReader implements TestSuiteReader {
    private static Log log = LogFactory.getLog(TestSuiteTextReader.class);
    private static String TEST_SUITE_WORD = Config.getString("user.testsuite.name", "Test Suite:");
    private static String TEST_CASE_WORD = Config.getString("user.testcase.name", "Test Case:");
    private static String TEST_PARAMETERS_WORD = Config.getString("user.parameters.name", "Parameters:");
    private static String TEST_KEYWORD_WORD = Config.getString("user.keyword.name", "Keyword:");
    private static String TEST_DATA_SET_WORD = Config.getString("user.dataset.name", "Data Set:");
    private List<String> lst = new ArrayList();
    private List<TestCase> listTestCases = new ArrayList();
    private String fileLocation = "";
    private String fileName = "";

    private void initialisationList(String str) {
        log.info("[START] Loading test suite from: " + str);
        TextReader reader = new TextReader(str);
        this.lst = reader.getFileContent();
        this.fileName = reader.getName();
        this.fileLocation = reader.getPath();
        log.info("[END] Loading test suite from: " + str);
    }

    public TestSuiteTextReader(String str) {
        this.initialisationList(str);
    }

    public TestSuiteTextReader(File file) {
        this.initialisationList(file.getPath());
    }

    public TestSuiteTextReader(Path path) {
        this.initialisationList(path.toFile().getPath());
    }

    public TestSuite readTestSuite() throws InvalidFormatOfSourceException {
        log.info("[START] Text test suite reader started: " + this.fileLocation);
        TestSuite testSuite = new TestSuite(this.fileName);
        this.listTestCases = this.parserList(testSuite);
        log.info("[END] Text test suite reader completed: " + this.fileLocation);
        return testSuite;
    }

    private void fillParameter(TestCase testCase) throws InvalidFormatOfSourceException {
        if (testCase != null) {
            boolean isParam = false;
            String testCaseName = "";
            Iterator var5 = this.lst.iterator();

            while(true) {
                while(var5.hasNext()) {
                    String line = (String)var5.next();
                    String currentLine = line.trim();
                    if (currentLine.startsWith(TEST_CASE_WORD)) {
                        testCaseName = currentLine.substring(currentLine.indexOf(":") + 1).trim();
                    }

                    if (currentLine.startsWith(TEST_PARAMETERS_WORD) && testCase.getName().equals(testCaseName)) {
                        isParam = true;
                    } else if (currentLine.startsWith(TEST_DATA_SET_WORD) && testCase.getName().equals(testCaseName)) {
                        String dataSetFile = currentLine.substring(currentLine.indexOf(":") + 1);
                        BasicFormatReaderFactory.getInstance().getDataSetReader(dataSetFile).loadParameters(testCase);
                    }

                    if (isParam && currentLine.contains("=")) {
                        String[] stringLine = currentLine.split("=");
                        String paramName;
                        String paramValue;
                        if (stringLine[0].trim().contains(":")) {
                            paramName = stringLine[0].substring(stringLine[0].indexOf(":") + 1).trim();
                            paramValue = stringLine[1].trim();
                        } else {
                            paramName = stringLine[0].trim();
                            paramValue = stringLine[1].trim();
                        }

                        if (StringUtils.isNotBlank(paramName) && StringUtils.isNotBlank(paramValue)) {
                            testCase.setParam(paramName, paramValue);
                        }
                    } else if (StringUtils.isBlank(currentLine) || !currentLine.startsWith(TEST_PARAMETERS_WORD)) {
                        isParam = false;
                    }
                }

                KDTUtils.loadConfigParams(testCase);
                break;
            }
        }

    }

    public List<TestCase> parserList(Executable parent) throws InvalidFormatOfSourceException {
        if (this.lst == null) {
            throw new InvalidFormatOfSourceException(String.format("No strings read from file. May be file does not exist / file structure is wrong? File name: %s", this.fileLocation));
        } else {
            List<TestCase> listCases = new ArrayList();

            for(int i = 0; i < this.lst.size(); ++i) {
                String currentLine = ((String)this.lst.get(i)).trim();
                if (currentLine.startsWith(TEST_SUITE_WORD)) {
                    String testSuiteName = currentLine.substring(currentLine.indexOf(":") + 1).trim();
                    parent.setName(testSuiteName);
                } else if (currentLine.startsWith(TEST_CASE_WORD)) {
                    List<String> tmpTC = new ArrayList();
                    String tcName = currentLine.substring(currentLine.indexOf(":") + 1).trim();
                    FileTestCase testCase = new FileTestCase(tcName, "", this.fileLocation);
                    this.fillParameter(testCase);
                    boolean isSpecialBlock = false;
                    String nameOfBlock = "";

                    for(int j = i; j < this.lst.size(); ++j) {
                        String currentScenarioLine = ((String)this.lst.get(j)).trim();
                        if (currentScenarioLine.startsWith(TEST_DATA_SET_WORD)) {
                            isSpecialBlock = true;
                        } else if (currentScenarioLine.startsWith(TEST_PARAMETERS_WORD)) {
                            isSpecialBlock = true;
                        } else if (!currentScenarioLine.startsWith(TEST_KEYWORD_WORD) && !currentScenarioLine.startsWith(TEST_CASE_WORD)) {
                            if (isSpecialBlock && (StringUtils.isBlank(currentScenarioLine) || !currentScenarioLine.contains("="))) {
                                isSpecialBlock = false;
                            }
                        } else {
                            isSpecialBlock = true;
                            nameOfBlock = currentScenarioLine.substring(currentScenarioLine.indexOf(":") + 1).trim();
                        }

                        if (currentScenarioLine.length() > 0) {
                            if (currentScenarioLine.startsWith(TEST_CASE_WORD) && tmpTC.size() > 0 || j == this.lst.size() - 1) {
                                if (StringUtils.isNotBlank(currentScenarioLine) && !isSpecialBlock && nameOfBlock.equals(testCase.getName()) && !currentScenarioLine.startsWith(TEST_CASE_WORD)) {
                                    tmpTC.add(currentScenarioLine);
                                }

                                listCases.add(testCase);
                                i = j == 0 ? 0 : j - 1;
                                break;
                            }

                            if (!isSpecialBlock && nameOfBlock.equals(testCase.getName())) {
                                tmpTC.add(currentScenarioLine);
                            }
                        }
                    }

                    testCase.setParent(parent);
                    TestCaseTextReader TestCaseReader = new TestCaseTextReader(tmpTC);
                    TestCaseReader.fillTestCase(testCase);
                    listCases.add(testCase);
                }
            }

            return listCases;
        }
    }

    public List<TestCase> getListTestCases() {
        return this.listTestCases;
    }
}

