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
import org.qubership.atp.adapter.excel.ExcelRow;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.FileTestCase;
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.keyworddriven.executable.SectionImpl;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.KDTUtils;
import org.qubership.atp.adapter.utils.excel.ExcelUtils;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class BasicFormatTestCaseReader implements TestCaseReader {
    /** @deprecated */
    @Deprecated
    public static final String DEFAULT_DATE_FORMAT = Config.getString("var.format.date.mask", "yyyy-MM-dd HH:mm:ss");
    public static final TimeZone DATE_FORMAT_TIMEZONE;
    private static Log log;
    public static final String TEST_CASE_BASIC_FOLDER;
    public static final String TEST_CASE_MAIN_SHEET;
    public static final String YES;
    /** @deprecated */
    @Deprecated
    public static final String NO;
    public static final String SECTION_COLUMN_NAME;
    public static final String RUN_COLUMN_NAME;
    public static final String DESCRIPTION_COLUMN_NAME;
    public static final String PARAMETERS_COLUMN_NAME;
    public static final String KEYWORDS_COLUMN_NAME;
    public static final String[] HEADERS;
    private static final DateFormat dateFormat;
    private static final PatternLayout appenderLayout;
    private static final String LOG_NAME = "logs/%s-%s.log";
    private ExcelSheet excelSheet;
    private List<Integer> sectionHeaderIndexes;

    public BasicFormatTestCaseReader(String fileName) throws InvalidFormatOfSourceException {
        this(KDTUtils.checkCriticalFileExistAndExit(TEST_CASE_BASIC_FOLDER, fileName));
    }

    public BasicFormatTestCaseReader(File sourceFile) throws InvalidFormatOfSourceException {
        this(ExcelUtils.getBook(sourceFile));
    }

    public BasicFormatTestCaseReader(ExcelSheet excelSheet) {
        this.sectionHeaderIndexes = new ArrayList();
        this.excelSheet = excelSheet;
    }

    public BasicFormatTestCaseReader(ExcelBook excelBook) throws InvalidFormatOfSourceException {
        this.sectionHeaderIndexes = new ArrayList();
        if (!excelBook.hasSheet(TEST_CASE_MAIN_SHEET)) {
            throw new InvalidFormatOfSourceException(String.format("Sheet '%s' hasn't been found in the '%s' WB. Available sheets are: %s", TEST_CASE_MAIN_SHEET, excelBook.getCurrentFile(), excelBook.getAllSheets()));
        } else {
            try {
                this.excelSheet = new ExcelSheet(excelBook, TEST_CASE_MAIN_SHEET, 1, this.getHeaders());
                ExcelUtils.checkHeaders(this.excelSheet, this.getHeaders());
            } catch (org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException var3) {
                org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException e = var3;
                throw new InvalidFormatOfSourceException(e.getMessage(), e);
            }

            this.sectionHeaderIndexes.addAll(this.excelSheet.getHeaderIndexesByName(SECTION_COLUMN_NAME));
        }
    }

    public void fillTestCase(TestCase tc) throws InvalidFormatOfSourceException {
        log.info("[START] Read test case: " + tc);
        FileTestCase testCase = (FileTestCase)tc;
        this.setLogger(testCase);
        if (!testCase.getExecutableSectionName().isEmpty()) {
            Iterator var3 = testCase.getExecutableSectionName().iterator();

            while(var3.hasNext()) {
                String sectionName = (String)var3.next();
                this.loadSection(testCase, this.excelSheet, sectionName);
            }
        } else {
            this.loadSubSection(testCase, this.excelSheet.getHeaderRowIndex() + 1, -1, this.excelSheet, this.excelSheet.getMaxRowNum() + 1);
        }

        this.loadParameters(testCase, this.excelSheet);
        this.loadParametersFromAllTabs(testCase, this.excelSheet);
        log.info("[END] Read test case: " + tc);
    }

    private void setLogger(FileTestCase testCase) {
        String logFileName = String.format("logs/%s-%s.log", testCase.getName(), dateFormat.format(new Date()));
        FileAppender appender = new FileAppender(appenderLayout, logFileName, false);

        Logger logger = Logger.getLogger(testCase.getName());
//        logger.addAppender(appender);
        logger.setLevel(Level.ALL);
        testCase.setLog(logger);
    }

    private void loadSection(FileTestCase testCase, ExcelSheet excelSheet, String sectionName) throws InvalidFormatOfSourceException {
        if (StringUtils.isBlank(sectionName)) {
            log.error(String.format("Section is blank in %s test case", testCase.getName()));
        } else {
            int firstIndex = 0;
            int lastIndex = 0;
            int rowsCount = excelSheet.getRowList().size();
            Iterator var7 = this.sectionHeaderIndexes.iterator();

            while(var7.hasNext()) {
                Integer sectionIndex = (Integer)var7.next();

                for(int currentRowNum = excelSheet.getHeaderRowIndex() + 1; currentRowNum <= rowsCount && excelSheet.getRow(currentRowNum) != null; ++currentRowNum) {
                    boolean isCurrentRowMatches = excelSheet.getCurrentRow().getCell(sectionIndex).getValue().equalsIgnoreCase(sectionName);
                    if (isCurrentRowMatches) {
                        if (firstIndex == 0) {
                            firstIndex = excelSheet.getCurrentRow().getRowNum();
                            lastIndex = firstIndex - 1;
                        }

                        ++lastIndex;
                    }

                    if (firstIndex != 0 && !isCurrentRowMatches || isCurrentRowMatches && excelSheet.getCurrentRow().getRowNum() == excelSheet.getMaxRowNum()) {
                        this.loadSubSection(testCase, firstIndex, -1, excelSheet, lastIndex + 1);
                        return;
                    }
                }
            }

            log.error(String.format("Section '%s' not found", sectionName));
        }
    }

    private boolean hasSubSections(int headerIndex, int rowNum) {
        if (headerIndex + 1 < this.sectionHeaderIndexes.size()) {
            return this.excelSheet.getRow(rowNum).getCell((Integer)this.sectionHeaderIndexes.get(headerIndex + 1)).getValue().length() > 0;
        } else {
            return false;
        }
    }

    private boolean hasRunnable(int headerIndex, int rowNum) {
        String sectionName = this.excelSheet.getRow(rowNum).getCell((Integer)this.sectionHeaderIndexes.get(headerIndex)).getValue();

        ExcelRow row;
        for(String currentSectionName = sectionName; rowNum <= this.excelSheet.getMaxRowNum() && StringUtils.isNotBlank(currentSectionName) && sectionName.equalsIgnoreCase(currentSectionName); currentSectionName = row == null ? null : row.getCell((Integer)this.sectionHeaderIndexes.get(headerIndex)).getValue()) {
            String runValue = this.excelSheet.getRow(rowNum).getCell((Integer)this.excelSheet.getHeaderIndexesByName(RUN_COLUMN_NAME).get(0)).getValue();
            if (YES.equalsIgnoreCase(runValue)) {
                return true;
            }

            ++rowNum;
            row = rowNum <= this.excelSheet.getMaxRowNum() ? this.excelSheet.getRow(rowNum) : null;
        }

        return false;
    }

    private void loadSubSection(Executable parent, int rowNum, int headerIndex, ExcelSheet subSheet, int endRow) throws InvalidFormatOfSourceException {
        if (this.sectionHeaderIndexes.size() > headerIndex + 1) {
            String previos = "";
            boolean isChildrenFound = false;

            for(int currentRowNum = rowNum; currentRowNum < endRow; ++currentRowNum) {
                subSheet.setCurrentRow(subSheet.getRow(currentRowNum));
                int nextHeaderIndex = headerIndex + 1;
                String parentName = headerIndex == -1 ? parent.getName() : subSheet.getCurrentRow().getCell((Integer)this.sectionHeaderIndexes.get(headerIndex)).getValue();
                String name = subSheet.getCurrentRow().getCell((Integer)this.sectionHeaderIndexes.get(nextHeaderIndex)).getValue();
                if (parentName.equals(parent.getName()) && name.length() > 0) {
                    isChildrenFound = true;
                    boolean hasChildren = this.hasSubSections(nextHeaderIndex, currentRowNum);
                    boolean hasRunnables = this.hasRunnable(nextHeaderIndex, currentRowNum);
                    if (!previos.equals(name)) {
                        previos = name;
                    } else if (hasChildren) {
                        continue;
                    }

                    if ((subSheet.getCellByHeaderName(subSheet.getCurrentRow().getRowNum(), RUN_COLUMN_NAME, (Integer)this.sectionHeaderIndexes.get(nextHeaderIndex)).getValue().equalsIgnoreCase(YES) || hasChildren) && hasRunnables) {
                        subSheet.getRow(currentRowNum);
                        SectionImpl section = new SectionImpl(name, parent);
                        this.loadParameters(section, subSheet);
                        if (hasChildren) {
                            this.loadSubSection(section, currentRowNum, nextHeaderIndex, subSheet, endRow);
                        } else {
                            section.setDescription((Integer)subSheet.getHeaderIndexesByName(DESCRIPTION_COLUMN_NAME).get(0) >= 0 ? subSheet.getCellByHeaderName(subSheet.getCurrentRow().getRowNum(), DESCRIPTION_COLUMN_NAME, (Integer)this.sectionHeaderIndexes.get(nextHeaderIndex)).getValue() : name);
                            this.loadKeywords(section, subSheet);
                        }
                    }
                } else if (isChildrenFound) {
                    break;
                }
            }

        }
    }

    protected void loadKeywords(SectionImpl parent, ExcelSheet sheet) throws InvalidFormatOfSourceException {
        String keywordsSheet = ExcelUtils.getCellValue(sheet, KEYWORDS_COLUMN_NAME);
        if (keywordsSheet.length() != 0) {
            BasicFormatReaderFactory.getInstance().newKeywordsReader(sheet.getExcelBook(), keywordsSheet).readKeywords(parent);
        }
    }

    protected void loadParameters(Section section, ExcelSheet tcSheet) throws InvalidFormatOfSourceException {
        String paramSheetName = ExcelUtils.getCellValue(tcSheet, PARAMETERS_COLUMN_NAME);
        ParametersReader paramReader = ExcelParametersReader.get(tcSheet.getExcelBook(), paramSheetName);
        paramReader.loadParameters(section);
    }

    protected void loadParametersFromAllTabs(Section section, ExcelSheet testCaseSheet) throws InvalidFormatOfSourceException {
        Set<String> processedTabs = new HashSet();

        for(int index = this.excelSheet.getHeaderRowIndex() + 1; index < this.excelSheet.getMaxRowNum() + 1; ++index) {
            testCaseSheet.setCurrentRow(testCaseSheet.getRow(index));
            String paramSheetName = ExcelUtils.getCellValue(testCaseSheet, PARAMETERS_COLUMN_NAME);
            if (!processedTabs.contains(paramSheetName)) {
                processedTabs.add(paramSheetName);
                ParametersReader paramReader = ExcelParametersReader.get(testCaseSheet.getExcelBook(), paramSheetName);
                paramReader.loadParameters(section);
            }
        }

    }

    public boolean sourceIsValid() {
        return this.excelSheet.getHeaders().values().containsAll(Arrays.asList(this.getHeaders()));
    }

    public String[] getHeaders() {
        return HEADERS;
    }

    public ExcelSheet getExcelSheet() {
        return this.excelSheet;
    }

    static {
        String timeZoneId = Config.getString("var.format.date.timezone");
        DATE_FORMAT_TIMEZONE = timeZoneId.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneId);
        log = LogFactory.getLog(BasicFormatTestCaseReader.class);
        TEST_CASE_BASIC_FOLDER = Config.getString("BasicFormatTestCaseReader.TEST_CASE_BASIC_FOLDER", BasicFormatTestSuiteReader.TEST_SUITE_BASIC_FOLDER);
        TEST_CASE_MAIN_SHEET = Config.getString("BasicFormatTestCaseReader.TEST_CASE_MAIN_SHEET", "Test Case");
        YES = Config.getString("BasicFormatTestCaseReader.YES", "Yes");
        NO = Config.getString("BasicFormatTestCaseReader.NO", "No");
        SECTION_COLUMN_NAME = Config.getString("BasicFormatTestCaseReader.SECTION_COLUMN_NAME", "Sections");
        RUN_COLUMN_NAME = Config.getString("BasicFormatTestCaseReader.RUN_COLUMN_NAME", "Run");
        DESCRIPTION_COLUMN_NAME = Config.getString("BasicFormatTestCaseReader.DESCRIPTION_COLUMN_NAME", "Description");
        PARAMETERS_COLUMN_NAME = Config.getString("BasicFormatTestCaseReader.PARAMETERS_COLUMN_NAME", "Parameters");
        KEYWORDS_COLUMN_NAME = Config.getString("BasicFormatTestCaseReader.KEYWORDS_COLUMN_NAME", "Keywords");
        HEADERS = new String[]{SECTION_COLUMN_NAME, PARAMETERS_COLUMN_NAME, KEYWORDS_COLUMN_NAME};
        dateFormat = new SimpleDateFormat("dd_MMM_yyyy__hh_mm_ssaa");
        appenderLayout = new PatternLayout("%d %-5p [%c{1}] %m%n");
    }
}

