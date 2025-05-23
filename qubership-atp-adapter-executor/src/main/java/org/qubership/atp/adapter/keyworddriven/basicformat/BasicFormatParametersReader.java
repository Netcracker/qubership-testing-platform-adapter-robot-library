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
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.KDTUtils;
import org.qubership.atp.adapter.utils.excel.ExcelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class BasicFormatParametersReader extends ExcelParametersReader {
    private static final Logger log = Logger.getLogger(BasicFormatParametersReader.class);
    public static final String CONTEXT_COLUMN_NAME = Config.getString("BasicFormatParametersReader.CONTEXT_COLUMN_NAME", "Context");
    public static final String PARAMETER_COLUMN_NAME = Config.getString("BasicFormatParametersReader.PARAMETER_COLUMN_NAME", "Parameter");
    public static final String VALUE_COLUMN_NAME = Config.getString("BasicFormatParametersReader.VALUE_COLUMN_NAME", "Value");
    public static final String[] HEADERS;

    public BasicFormatParametersReader(ExcelBook excelBook, String sheetName) {
        super(excelBook, sheetName);
    }

    public void loadParameters(Section section) throws InvalidFormatOfSourceException {
        if (!StringUtils.isEmpty(this.sheetName)) {
            if (!this.excelBook.hasSheet(this.sheetName)) {
                throw new InvalidFormatOfSourceException(String.format("Sheet '%s' hasn't been found in the '%s' WB. Available sheets are: %s", this.sheetName, this.excelBook.getCurrentFile(), this.excelBook.getAllSheets()));
            }

            try {
                ExcelSheet excelSheet = new ExcelSheet(this.excelBook, this.sheetName, 1, HEADERS);
                ExcelUtils.checkHeaders(excelSheet, this.getHeaders());
                this.loadParameters(section.getName(), section, excelSheet);
                this.loadParameters(section.getFullName(), section, excelSheet);
            } catch (org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException var4) {
                org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException e = var4;
                throw new InvalidFormatOfSourceException("Can not load parameters from book '" + this.excelBook.getCurrentFile() + "' from sheet '" + this.sheetName + "' Error: " + e.getMessage(), e);
            }
        }

        KDTUtils.loadConfigParams(section);
    }

    protected String[] getHeaders() {
        return HEADERS;
    }

    public void loadParameters(String expectedContext, Executable section, ExcelSheet paramSheet) throws org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException {
        if (log.isTraceEnabled()) {
            log.trace("Search for parameters with context = " + expectedContext.replaceAll("[\r\n]", " ") + " on " + paramSheet);
        }

        int rowsCount = paramSheet.getRowList().size();

        for(int rowNum = 2; rowNum <= rowsCount; ++rowNum) {
            paramSheet.setCurrentRow(paramSheet.getRow(rowNum));
            String context = ExcelUtils.getCellValue(paramSheet, CONTEXT_COLUMN_NAME);
            if (context.length() != 0 && context.equals(expectedContext)) {
                String paramName = ExcelUtils.getCellValue(paramSheet, PARAMETER_COLUMN_NAME);
                String paramValue = ExcelUtils.getCellValue(paramSheet, VALUE_COLUMN_NAME);
                if (KdtProperties.REPLACE_PARAMETERS_ON_READ) {
                    paramValue = KDTUtils.replaceParametersInString(section, paramValue);
                }

                section.setParam(paramName, paramValue);
                if (log.isTraceEnabled()) {
                    log.trace("Param set: " + paramName + "=" + paramValue);
                }
            }
        }

    }

    static {
        HEADERS = new String[]{CONTEXT_COLUMN_NAME, PARAMETER_COLUMN_NAME, VALUE_COLUMN_NAME};
    }
}

