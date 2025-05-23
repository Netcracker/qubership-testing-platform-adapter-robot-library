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

import org.qubership.atp.adapter.excel.ExcelBook;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.configuration.KdtProperties;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.Flag;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import org.qubership.atp.adapter.keyworddriven.executable.TestSuite;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.KDTUtils;
import org.qubership.atp.adapter.utils.excel.ExcelUtils;
import java.io.File;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class BasicFormatTestSuiteReader implements TestSuiteReader {
    private static final Logger log = Logger.getLogger(BasicFormatTestSuiteReader.class);
    public static final String TEST_SUITE_BASIC_FOLDER = Config.getString("BasicFormatTestSuiteReader.TEST_SUITE_BASIC_FOLDER", (String)null);
    public static final String TEST_SUITE_MAIN_SHEET = Config.getString("BasicFormatTestSuiteReader.TEST_SUITE_MAIN_SHEET", "Test Cases");
    public static final String FILE_SEPARATOR = Config.getString("BasicFormatTestSuiteReader.FILE_SEPARATOR", System.getProperty("file.separator"));
    public static final String RUN_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.RUN_COLUMN_NAME", "Run");
    public static final String TEST_CASE_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.TEST_CASE_COLUMN_NAME", "Test Case");
    public static final String DESCRIPTION_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.DESCRIPTION_COLUMN_NAME", "Description");
    public static final String TEST_CASE_PARAMETERS_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.TEST_CASE_PARAMETERS_COLUMN_NAME", "Test Case Parameters");
    private static final String[] HEADERS;
    public static final String TEST_CASE_THREAD_COLUMN_NAME;
    public static final String TEST_CASE_SECTION_COLUMN_NAME;
    public static final String TEST_CASE_TYPE_COLUMN_NAME;
    private ExcelSheet excelSheet;
    private Filter<TestCase> filter;

    public BasicFormatTestSuiteReader(String fileName) throws InvalidFormatOfSourceException {
        this(KDTUtils.checkCriticalFileExistAndExit(TEST_SUITE_BASIC_FOLDER, fileName));
    }

    public BasicFormatTestSuiteReader(File sourceFile) throws InvalidFormatOfSourceException {
        this(ExcelUtils.getBook(sourceFile));
    }

    public BasicFormatTestSuiteReader(ExcelSheet excelSheet) throws InvalidFormatOfSourceException {
        this.excelSheet = excelSheet;
    }

    public BasicFormatTestSuiteReader(ExcelBook excelBook) throws InvalidFormatOfSourceException {
        if (!excelBook.hasSheet(TEST_SUITE_MAIN_SHEET)) {
            throw new InvalidFormatOfSourceException(String.format("Sheet '%s' hasn't been found in the '%s' WB. Available sheets are: %s", TEST_SUITE_MAIN_SHEET, excelBook.getCurrentFile(), excelBook.getAllSheets()));
        } else {
            try {
                this.excelSheet = new ExcelSheet(excelBook, TEST_SUITE_MAIN_SHEET, 1, this.getHeaders());
                ExcelUtils.checkHeaders(this.excelSheet, this.getHeaders());
            } catch (org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException var3) {
                org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException e = var3;
                throw new InvalidFormatOfSourceException(e.getMessage(), e);
            }

            this.filter = ArrayUtils.isNotEmpty(KdtProperties.KDT_TEST_CASES_NAMES) ? Filter.executableByName(KdtProperties.KDT_TEST_CASES_NAMES) : Filter.filterSectionByValidationLevel();
        }
    }

    public TestSuite readTestSuite() throws InvalidFormatOfSourceException {
        log.info("Flow test suite reader started: " + this.getExcelSheet());
        TestSuite ts = new TestSuite(this.getExcelSheet().getExcelBook().getCurrentFile().getName());
        if (this.sourceIsValid()) {
            for(int i = 2; i <= this.getExcelSheet().getRowList().size(); ++i) {
                this.getExcelSheet().setCurrentRow(this.getExcelSheet().getRow(i));
                String tcName = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_COLUMN_NAME);
                String testCaseValidationLevel = ExcelUtils.getCellValue(this.getExcelSheet(), RUN_COLUMN_NAME);
                int validationLevel = this.parseValidationLevel(i, testCaseValidationLevel);
                String tcFilename = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_PARAMETERS_COLUMN_NAME);
                String description = ExcelUtils.getCellValue(this.getExcelSheet(), DESCRIPTION_COLUMN_NAME);
                String threadName = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_THREAD_COLUMN_NAME);
                String sectionName = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_SECTION_COLUMN_NAME);
                if (StringUtils.isBlank(tcName) && StringUtils.isBlank(testCaseValidationLevel) && StringUtils.isBlank(tcFilename) && StringUtils.isBlank(description) && StringUtils.isBlank(threadName) && StringUtils.isBlank(sectionName)) {
                    log.debug(String.format("Row '%s' was skipped because it is empty in file '%s'", i, this.getExcelSheet().getExcelBook().getCurrentFile().getName()));
                } else {
                    FileTestCase testCase = new FileTestCase(tcName, description, tcFilename, threadName, validationLevel);
                    if (this.shouldBeExecuted(testCase)) {
                        this.processFlags(testCase, this.excelSheet);
                        testCase.setParent(ts);
                        if (StringUtils.isNotBlank(sectionName)) {
                            testCase.addExecutableSectionName(sectionName);
                        }

                        BasicFormatReaderFactory.getInstance().newTestCaseReader(tcFilename).fillTestCase(testCase);
                    }
                }
            }
        }

        log.info("Flow test suite reader completed: " + this.getExcelSheet());
        return ts;
    }

    protected void processFlags(Executable executable, ExcelSheet excelSheet) {
        String testCaseType = ExcelUtils.getCellValue(excelSheet, TEST_CASE_TYPE_COLUMN_NAME);
        executable.addFlag(new Flag("TEST_CASE_TYPE", testCaseType));
    }

    protected final int parseValidationLevel(int rowNum, String testCaseValidationLevel) throws InvalidFormatOfSourceException {
        try {
            int validationLevel = ValidationLevel.parseValidationLevel(testCaseValidationLevel);
            return validationLevel;
        } catch (IllegalArgumentException var5) {
            IllegalArgumentException e = var5;
            throw new InvalidFormatOfSourceException(e, "Run column '%s' value '%s' is wrong in row '%s'. Error: %s", new Object[]{RUN_COLUMN_NAME, testCaseValidationLevel, rowNum, e.getMessage()});
        }
    }

    protected boolean shouldBeExecuted(FileTestCase testCase) {
        if (this.filter.match(testCase)) {
            log.debug("Test case '" + testCase + "' will be executed");
            return true;
        } else {
            log.debug("Test case '" + testCase + "' is not specified for execution");
            return false;
        }
    }

    public boolean sourceIsValid() {
        return this.getExcelSheet().getHeaders().values().containsAll(Arrays.asList(this.getHeaders()));
    }

    public ExcelSheet getExcelSheet() {
        return this.excelSheet;
    }

    public String[] getHeaders() {
        return HEADERS;
    }

    static {
        HEADERS = new String[]{TEST_CASE_COLUMN_NAME, TEST_CASE_PARAMETERS_COLUMN_NAME, RUN_COLUMN_NAME};
        TEST_CASE_THREAD_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.TEST_CASE_THREAD_COLUMN_NAME", "Thread");
        TEST_CASE_SECTION_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.TEST_CASE_SECTION_COLUMN_NAME", "Section");
        TEST_CASE_TYPE_COLUMN_NAME = Config.getString("BasicFormatTestSuiteReader.TEST_CASE_TYPE_COLUMN_NAME", "Type");
    }
}

