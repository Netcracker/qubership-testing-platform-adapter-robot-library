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

package org.qubership.atp.adapter.utils.excel;

import org.qubership.atp.adapter.excel.ExcelBook;
import org.qubership.atp.adapter.excel.ExcelRow;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.qubership.atp.adapter.excel.exceptions.DataNotSetException;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.utils.KDTUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;

public class ExcelUtils {
    private static final Logger log = Logger.getLogger(ExcelUtils.class);

    public ExcelUtils() {
    }

    public static ExcelBook getBook(File sourceFile) throws InvalidFormatOfSourceException {
        try {
            ExcelBook excelBook = new ExcelBook(KDTUtils.checkCriticalFileExistAndExit(sourceFile));
            excelBook.setVerifyTopCellsRage(false);
            return excelBook;
        } catch (org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException var2) {
            org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException e = var2;
            throw new InvalidFormatOfSourceException(e.getMessage(), e);
        }
    }

    public static ExcelBook getBook(String filepath) throws InvalidFormatOfSourceException {
        return getBook(new File(filepath));
    }

    public static Map<String, String> readRowWithHeaders(ExcelSheet sheet, ExcelRow row, int startIndex, int endIndex) {
        Map<String, String> map = new HashMap();
        if (row != null) {
            for(int colNum = startIndex; colNum < endIndex; ++colNum) {
                map.put(sheet.getHeaders().get(colNum), row.getCell(colNum).getValue());
            }
        }

        return map;
    }

    public static List<String> readRow(ExcelRow row) {
        return readRow(row, 1);
    }

    public static List<String> readRow(ExcelRow row, int startIndex) {
        return readRow(row, startIndex, row.getCellList().size() + 1);
    }

    public static List<String> readRow(ExcelRow row, int startIndex, int endIndex) {
        List<String> array = new ArrayList();
        if (row != null) {
            for(int colNum = startIndex; colNum < endIndex; ++colNum) {
                array.add(row.getCell(colNum).getValue());
            }
        }

        return array;
    }

    public static Integer getInteger(ExcelSheet sheet, String columnName, Integer defaultValue) throws InvalidFormatOfSourceException {
        String valueByHeaderName = "";

        try {
            valueByHeaderName = sheet.getCellByHeaderName(sheet.getCurrentRow().getRowNum(), columnName).getValue();
            return Integer.valueOf(valueByHeaderName);
        } catch (DataNotSetException var5) {
            DataNotSetException e = var5;
            log.warn(e.getMessage());
        } catch (NumberFormatException var6) {
            log.debug(String.format("Value '%s' of '%s' is not defined or has incorrect format in row '%s' in file '%s'. Value by default is: %s", valueByHeaderName, columnName, sheet.getCurrentRow().getRowNum(), sheet.getExcelBook().getCurrentFile().getName(), defaultValue));
        }

        return defaultValue;
    }

    public static String getCellValue(ExcelSheet sheet, String columnName) {
        return getCellValue(sheet, columnName, "");
    }

    public static String getCellValue(ExcelSheet sheet, String columnName, String defaultValue) {
        try {
            return sheet.getCellByHeaderName(sheet.getCurrentRow().getRowNum(), columnName).getValue();
        } catch (DataNotSetException var4) {
            DataNotSetException e = var4;
            log.warn(e.getMessage());
            return defaultValue;
        }
    }

    public static void checkHeaders(ExcelSheet sheet, String... headers) throws InvalidFormatOfSourceException {
        checkHeaders(sheet, Arrays.asList(headers));
    }

    public static void checkHeaders(ExcelSheet sheet, List<String> expectedHeaders) throws InvalidFormatOfSourceException {
        List missedHeaders = ListUtils.removeAll(expectedHeaders, sheet.getHeaders().values());
        if (missedHeaders.size() > 0) {
            throw new InvalidFormatOfSourceException("Following rows are not present in header on sheet " + sheet + " : " + missedHeaders);
        }
    }
}

