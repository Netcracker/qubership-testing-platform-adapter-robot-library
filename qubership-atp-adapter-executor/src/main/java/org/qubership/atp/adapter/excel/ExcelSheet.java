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

package org.qubership.atp.adapter.excel;

import org.qubership.atp.adapter.excel.exceptions.DataNotSetException;
import org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;

public class ExcelSheet {
    private static Log log = LogFactory.getLog(ExcelSheet.class);
    public static final int DEFAULT_HEADER_INDEX = 1;
    private Sheet currentSheet;
    private String currentSheetName;
    private ExcelBook excelBook;
    private ExcelRow currentRow;
    private int headerRowIndex;
    private Map<Integer, String> headers;

    public ExcelSheet(ExcelBook excelBook, String sheetName, int currentRowIndex, String... headerRowIdentifiers) throws InvalidFormatOfSourceException {
        this.currentSheet = null;
        this.currentSheetName = null;
        this.excelBook = null;
        this.currentRow = null;
        this.headerRowIndex = -1;
        this.headers = new LinkedHashMap();
        this.excelBook = excelBook;
        this.currentSheetName = sheetName;
        if (!excelBook.hasSheet(sheetName)) {
            throw new InvalidFormatOfSourceException(String.format("Sheet '%s' hasn't been found in the '%s' WB", sheetName, this.getExcelBookName()));
        } else {
            this.setCurrentSheet(excelBook.getWorkbook().getSheet(sheetName));
            this.calculateHeaders(currentRowIndex, headerRowIdentifiers);
        }
    }

//    public ExcelSheet(ExcelBook excelBook, String sheetName, int currentRowIndex) throws InvalidFormatOfSourceException {
//        this(excelBook, sheetName, currentRowIndex);
//    }

    public ExcelSheet(ExcelBook excelBook) throws InvalidFormatOfSourceException {
        this(excelBook, excelBook.getSheetName(1));
    }

    public ExcelSheet(ExcelBook excelBook, String sheetName) throws InvalidFormatOfSourceException {
        this(excelBook, sheetName, 1);
    }

    public ExcelSheet(ExcelBook excelBook, int sheetNum) throws InvalidFormatOfSourceException {
        this(excelBook, excelBook.getSheetName(sheetNum));
    }

    public void calculateHeaders(int currentRowIndex, String... headerRowIdentifiers) throws InvalidFormatOfSourceException {
        this.headers.clear();
        if (this.getMaxRowNum() > 0) {
            this.currentRow = this.getRow(currentRowIndex);
            if (headerRowIdentifiers.length > 0) {
                currentRowIndex = this.getRowNumberByContent(headerRowIdentifiers);
            }

            if (currentRowIndex != -1 && this.getRow(currentRowIndex) != null) {
                this.setHeaderRowIndex(currentRowIndex);
                Iterator i$ = IteratorUtils.toList(this.getCurrentRow().iterator()).iterator();

                while(i$.hasNext()) {
                    ExcelCell cell = (ExcelCell)i$.next();
                    String cellValue = cell.getValue();
                    if (StringUtils.isNotBlank(cellValue)) {
                        this.headers.put(cell.getPosition().getColNum(), cellValue);
                    }
                }

            } else {
                throw new InvalidFormatOfSourceException(String.format("Header row on '%s' sheet hasn't been defined. File name is '%s'.", this.getSheetName(), this.getExcelBookName()));
            }
        }
    }

    public ExcelRow getCurrentRow() {
        return this.currentRow;
    }

    public void setCurrentRow(ExcelRow currentRow) {
        this.currentRow = currentRow;
    }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    public Map<Integer, String> getHeaders() {
        return this.headers;
    }

    public List<Integer> getHeaderIndexesByName(String headerName) {
        return this.getHeaderIndexesByName(headerName, -1);
    }

    public List<Integer> getHeaderIndexesByName(String headerName, int startIndex) {
        List<Integer> headerIndexes = new ArrayList();
        Iterator i$ = this.headers.keySet().iterator();

        while(i$.hasNext()) {
            int key = (Integer)i$.next();
            if (((String)this.headers.get(key)).equalsIgnoreCase(headerName) && key >= startIndex) {
                headerIndexes.add(key);
            }
        }

        return headerIndexes;
    }

    public ExcelCell getCellByIndexAndContent(int headerIndex, String content) {
        Iterator<ExcelRow> rowIterator = this.iterator();

        ExcelRow excelRow;
        do {
            if (!rowIterator.hasNext()) {
                log.error("Cell by header index " + headerIndex + " and with value " + content + " is not found");
                return null;
            }

            excelRow = (ExcelRow)rowIterator.next();
        } while(excelRow.isCellNull(headerIndex) || !excelRow.getCell(headerIndex).getValue().equals(content));

        return excelRow.getCell(headerIndex);
    }

    public ExcelRow getRow(int rowNumber) {
        this.validateRowIndex(rowNumber, true);
        if (this.isRowNull(rowNumber)) {
            this.currentSheet.createRow(rowNumber - 1);
        }

        this.currentRow = new ExcelRow(this.currentSheet.getRow(rowNumber - 1), this);
        return this.currentRow;
    }

    public ExcelCell getCellByHeaderName(int rowNumber, String headerName) {
        return this.getCellByHeaderName(rowNumber, headerName, -1);
    }

    public ExcelCell getCellByHeaderName(int rowNumber, String headerName, int startIndex) {
        this.validateRowIndex(rowNumber, true);
        List<Integer> indexes = this.getHeaderIndexesByName(headerName, startIndex);
        if (indexes.size() != 0 && (Integer)indexes.get(0) != -1) {
            return this.getRow(rowNumber).getCell((Integer)indexes.get(0));
        } else {
            String basicMessage = String.format("Error during operation with headers. Excel file name is '%s', excel sheet name is '%s'. Header row index = %s", this.getExcelBookName(), this.getSheetName(), this.headerRowIndex);
            if (this.getHeaderRowIndex() <= -1) {
                throw new DataNotSetException(String.format("%s. Header row is not defined!", basicMessage));
            } else {
                throw new DataNotSetException(String.format("%s. Header name '%s' doesn't exist in header row: '%s'", basicMessage, headerName, this.headers.values().toString()));
            }
        }
    }

    public boolean isColumnHidden(int columnIndex) {
        this.validateColumnIndex(columnIndex);
        return this.currentSheet.isColumnHidden(columnIndex - 1);
    }

    public void setColumnHidden(int columnIndex, boolean isHidden) {
        this.validateColumnIndex(columnIndex);
        this.currentSheet.setColumnHidden(columnIndex - 1, true);
    }

    public int getRowNumberByContent(String[] content) {
        Iterator i$ = IteratorUtils.toList(this.iterator()).iterator();

        ExcelRow row;
        int valuesCount;
        do {
            if (!i$.hasNext()) {
                return -1;
            }

            row = (ExcelRow)i$.next();
            valuesCount = 0;
            boolean isEmpty = false;
            Iterator iterator = IteratorUtils.toList(row.iterator()).iterator();

            while(iterator.hasNext()) {
                ExcelCell cell = (ExcelCell)iterator.next();
                String cellValue = cell.getValue();

                for(int i = 0; i < content.length; ++i) {
                    String contentVal = content[i];
                    if (StringUtils.equalsIgnoreCase(cellValue, contentVal)) {
                        if (StringUtils.isNotEmpty(cellValue)) {
                            ++valuesCount;
                        } else if (StringUtils.isEmpty(cellValue) && !isEmpty) {
                            isEmpty = true;
                            ++valuesCount;
                        }
                    }
                }
            }
        } while(valuesCount < content.length);

        return row.getRowNum();
    }

    public boolean isRowNull(int rowNumber) {
        this.validateRowIndex(rowNumber);
        return this.currentSheet.getRow(rowNumber - 1) == null;
    }

    public int getMaxRowNum() {
        int amountOfRows = this.currentSheet.getLastRowNum();
        return amountOfRows == 0 && this.isRowNull(1) ? 0 : amountOfRows + 1;
    }

    public ExcelRow createRow(int rowNumber) {
        this.validateRowIndex(rowNumber);
        if (rowNumber < this.getMaxRowNum()) {
            this.getCurrentSheet().shiftRows(rowNumber - 1, this.getMaxRowNum(), 1);
        }

        this.currentSheet.createRow(rowNumber - 1);
        return this.getRow(rowNumber);
    }

    public void setAutoSizeColumnByIndex(int columnIndex) {
        this.validateColumnIndex(columnIndex);
        this.currentSheet.autoSizeColumn(columnIndex - 1);
    }

    public void createFreezePane(int endRowIndex, int endColumnIndex) {
        if (endRowIndex != 0) {
            this.validateRowIndex(endRowIndex, true);
        }

        if (endColumnIndex != 0) {
            this.validateColumnIndex(endColumnIndex);
        }

        this.currentSheet.createFreezePane(endColumnIndex, endRowIndex);
    }

    public int getHeaderRowIndex() {
        return this.headerRowIndex;
    }

    public String toString() {
        return this.getExcelBookName() + "$" + this.getSheetName();
    }

    public int getMaxCellNumForThisSheet() {
        int currentMaxCell = 0;
        Iterator i$ = IteratorUtils.toList(this.iterator()).iterator();

        while(i$.hasNext()) {
            ExcelRow row = (ExcelRow)i$.next();
            int currentLastCellNum = row.getRow() == null ? 0 : row.getMaxCellNum();
            if (currentMaxCell < currentLastCellNum) {
                currentMaxCell = currentLastCellNum;
            }
        }

        return currentMaxCell;
    }

    public List<ExcelRow> getRowList() {
        return IteratorUtils.toList(this.iterator());
    }

    public Iterator<ExcelRow> iterator() {
        return new Iterator<ExcelRow>() {
            int index = 1;
            int endIndex = ExcelSheet.this.getMaxRowNum();

            public boolean hasNext() {
                return this.index <= this.endIndex;
            }

            public ExcelRow next() {
                return ExcelSheet.this.getRow(this.index++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Ask for implementation. It was not implemented due to unusage");
            }
        };
    }

    private void setCurrentSheet(Sheet currentSheet) {
        this.currentSheet = currentSheet;
    }

    public Sheet getCurrentSheet() {
        return this.currentSheet;
    }

    public String getSheetName() {
        return this.currentSheetName;
    }

    public void setColumnWidthByIndex(int columnIndex, int width) {
        this.validateColumnIndex(columnIndex);
        this.currentSheet.setColumnWidth(columnIndex - 1, width);
    }

    public void setDefaultColumnWidth(int width) {
        this.currentSheet.setDefaultColumnWidth(width);
    }

    public void setDefaultRowHeight(int height) {
        this.currentSheet.setDefaultRowHeight((short)height);
    }

    public int addMergedRegion(int firstRow, int endRow, int firstCol, int endCol) {
        this.validateRowIndex(firstRow, true);
        this.validateRowIndex(endRow, true);
        this.validateColumnIndex(firstCol);
        this.validateColumnIndex(endCol);
        if (firstRow <= endRow && firstCol <= endCol) {
            return this.currentSheet.addMergedRegion(new CellRangeAddress(firstRow - 1, endRow - 1, firstCol - 1, endCol - 1));
        } else {
            String messageText = firstRow > endRow ? "First Row index (" + firstRow + ") should not be bigger than End Row index (" + endRow + ")" : "First Cell index (" + firstCol + ") should not be bigger than End Cell index (" + endCol + ")";
            throw new IllegalArgumentException(messageText);
        }
    }

    public void setAutoFilter(int firstRow, int endRow, int firstCol, int endCol) {
        this.validateRowIndex(firstRow, true);
        this.validateRowIndex(endRow, true);
        this.validateColumnIndex(firstCol);
        this.validateColumnIndex(endCol);
        if (firstRow <= endRow && firstCol <= endCol) {
            this.currentSheet.setAutoFilter(new CellRangeAddress(firstRow - 1, endRow - 1, firstCol - 1, endCol - 1));
        } else {
            String messageText = firstRow > endRow ? "First Row index (" + firstRow + ") should not be bigger than End Row index (" + endRow + ")" : "First Cell index (" + firstCol + ") should not be bigger than End Cell index (" + endCol + ")";
            throw new IllegalArgumentException(messageText);
        }
    }

    public boolean equals(Object object) {
        return !(object instanceof ExcelSheet) ? false : this.toString().equals(((ExcelSheet)object).toString());
    }

    public ExcelBook getExcelBook() {
        return this.excelBook;
    }

    public String getExcelBookName() {
        return this.getExcelBook().toString();
    }

    public void close() {
        this.headers.clear();
        this.currentSheet = null;
        this.currentRow.dispose();
        this.currentRow = null;
        this.excelBook = null;
    }

    private void validateRowIndex(int rowNumber) {
        this.validateRowIndex(rowNumber, false);
    }

    private void validateRowIndex(int rowNumber, boolean verifyTopLimit) {
        if (rowNumber <= 0 || verifyTopLimit && rowNumber > this.getMaxRowNum()) {
            String exception = verifyTopLimit ? "is out of range: (1.." + this.getMaxRowNum() + ")" : "should be create than 1";
            throw new IllegalArgumentException("Row index (" + rowNumber + ") " + exception + ". File name is '" + this.getExcelBookName() + "'. Sheet name is '" + this.getSheetName());
        }
    }

    private void validateColumnIndex(int columnNumber) {
        if (columnNumber <= 0 || this.getExcelBook().isVerifyTopCellsRage() && columnNumber > this.getMaxCellNumForThisSheet()) {
            throw new IllegalArgumentException("Column index (" + columnNumber + ") is out of range: (1.." + this.getMaxCellNumForThisSheet() + "). File name is '" + this.getExcelBookName() + "'. Sheet name is '" + this.getSheetName());
        }
    }

    public void flushRows() throws IOException {
        if (this.currentSheet instanceof SXSSFSheet) {
            ((SXSSFSheet)this.currentSheet).flushRows();
        } else {
            throw new UnsupportedOperationException("Could not release memory. This sheet is not SXSSFSheet's instance");
        }
    }

    public void trackColumnForAutoSizing(int columnNumber) {
        if (this.currentSheet instanceof SXSSFSheet) {
            ((SXSSFSheet)this.currentSheet).trackColumnForAutoSizing(columnNumber - 1);
        } else {
            throw new UnsupportedOperationException("This function available only for SXSSFSheet sheet");
        }
    }

    public void untrackColumnForAutoSizing(int columnNumber) {
        if (this.currentSheet instanceof SXSSFSheet) {
            ((SXSSFSheet)this.currentSheet).untrackColumnForAutoSizing(columnNumber - 1);
        } else {
            throw new UnsupportedOperationException("This function available only for SXSSFSheet sheet");
        }
    }
}
