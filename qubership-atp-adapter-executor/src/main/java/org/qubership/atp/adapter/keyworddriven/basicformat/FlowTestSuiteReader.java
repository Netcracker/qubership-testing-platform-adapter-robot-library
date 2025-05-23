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

import com.google.common.collect.Maps;
import org.qubership.atp.adapter.excel.ExcelCell;
import org.qubership.atp.adapter.excel.ExcelRow;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.TestSuite;
import org.qubership.atp.adapter.utils.excel.ExcelUtils;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class FlowTestSuiteReader extends BasicFormatTestSuiteReader {
    private static final Logger log = Logger.getLogger(FlowTestSuiteReader.class);
    private static final String[] HEADERS;
    public static Pattern EXCEL_FILE_PATTERN;
    private String directory;

    public FlowTestSuiteReader(String sourceFile, String directory) throws InvalidFormatOfSourceException {
        super(sourceFile);
        this.directory = directory;
    }

    public TestSuite readTestSuite() throws InvalidFormatOfSourceException {
        log.info("Flow test suite reader started: " + this.getExcelSheet());
        TestSuite ts = new TestSuite(this.getExcelSheet().getExcelBook().getCurrentFile().getName());
        Map<BasicFormatTestCaseReader, Set<String>> fileSectionMap = this.readSections(new File(this.directory));
        if (this.sourceIsValid()) {
            for(int rowNum = 2; rowNum <= this.getExcelSheet().getMaxRowNum(); ++rowNum) {
                this.getExcelSheet().setCurrentRow(this.getExcelSheet().getRow(rowNum));
                String description = ExcelUtils.getCellValue(this.getExcelSheet(), DESCRIPTION_COLUMN_NAME);
                String runValue = ExcelUtils.getCellValue(this.getExcelSheet(), RUN_COLUMN_NAME);
                String sectionName = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_COLUMN_NAME);
                String threadName = ExcelUtils.getCellValue(this.getExcelSheet(), TEST_CASE_THREAD_COLUMN_NAME);
                Iterator var8 = fileSectionMap.keySet().iterator();

                while(var8.hasNext()) {
                    BasicFormatTestCaseReader reader = (BasicFormatTestCaseReader)var8.next();
                    Set<String> sectionNames = (Set)fileSectionMap.get(reader);
                    if (sectionNames.contains(sectionName)) {
                        FileTestCase testCase = new FileTestCase(sectionName, description, reader.getExcelSheet().getExcelBook().getCurrentFile().getAbsolutePath(), threadName, ValidationLevel.parseValidationLevel(runValue));
                        if (StringUtils.isNotBlank(sectionName)) {
                            testCase.addExecutableSectionName(sectionName);
                        }

                        if (this.shouldBeExecuted(testCase)) {
                            this.processFlags(testCase, this.getExcelSheet());
                            testCase.setParent(ts);
                            reader.fillTestCase(testCase);
                        }
                    }
                }
            }
        }

        log.info("Flow test suite reader completed: " + this.getExcelSheet());
        return ts;
    }

    public String[] getHeaders() {
        return HEADERS;
    }

    private Map<BasicFormatTestCaseReader, Set<String>> readSections(File directory) {
        Map<BasicFormatTestCaseReader, Set<String>> fileSectionMap = Maps.newHashMap();
        IOFileFilter filter = new RegexFileFilter(EXCEL_FILE_PATTERN);
        Collection<File> testCases = FileUtils.listFiles(directory, filter, TrueFileFilter.INSTANCE);
        Iterator var5 = testCases.iterator();

        while(var5.hasNext()) {
            File file = (File)var5.next();

            try {
                BasicFormatTestCaseReader reader = new BasicFormatTestCaseReader(file);
                fileSectionMap.put(reader, this.getSectionNames(reader));
            } catch (InvalidFormatOfSourceException var8) {
                InvalidFormatOfSourceException e = var8;
                log.warn(String.format("Test cases are not loaded from file '%s' of invalid format: %s", file.getAbsolutePath(), e.getMessage()));
            }
        }

        return fileSectionMap;
    }

    private Set<String> getSectionNames(BasicFormatTestCaseReader reader) {
        Set<String> sections = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        ExcelSheet excelSheet = reader.getExcelSheet();
        List<Integer> sectionHeaderIndexes = excelSheet.getHeaderIndexesByName(BasicFormatTestCaseReader.SECTION_COLUMN_NAME);
        if (excelSheet.getHeaderRowIndex() < 0) {
            log.warn("Header is not found in test case file '" + excelSheet.getExcelBook().getCurrentFile().getPath() + "' sheet '" + excelSheet.getSheetName() + "'");
            return sections;
        } else {
            for(int index = excelSheet.getHeaderRowIndex() + 1; index < excelSheet.getMaxRowNum() + 1; ++index) {
                ExcelRow row = excelSheet.getRow(index);
                Iterator var7 = sectionHeaderIndexes.iterator();

                while(var7.hasNext()) {
                    int columnIndex = (Integer)var7.next();
                    ExcelCell cell = row.getCell(columnIndex);
                    if (cell == null) {
                        break;
                    }

                    String sectionName = cell.getValue();
                    if (StringUtils.isNotBlank(sectionName)) {
                        sections.add(sectionName);
                    }
                }
            }

            return sections;
        }
    }

    static {
        HEADERS = new String[]{TEST_CASE_COLUMN_NAME, RUN_COLUMN_NAME};
        EXCEL_FILE_PATTERN = Pattern.compile("^(?!~\\$).+?\\.xlsx?$");
    }
}

