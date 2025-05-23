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

import org.qubership.atp.adapter.excel.style.ExcelCellStyle;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public class ExcelRow {
    private static Log log = LogFactory.getLog(ExcelRow.class);
    private Row row = null;
    private ExcelCell currentCell = null;
    private ExcelSheet excelSheet = null;

    public ExcelRow(Row row, ExcelSheet sheet) {
        this.row = row;
        this.excelSheet = sheet;
    }

    public ExcelCell getCurrentCell() {
        return this.currentCell;
    }

    public ExcelCell getCell(int columnNumber) {
        this.validateColumnIndex(columnNumber, true);
        this.currentCell = new ExcelCell(this.getRow().getCell(columnNumber - 1, MissingCellPolicy.CREATE_NULL_AS_BLANK));
        return this.currentCell;
    }

    public int getMaxCellNum() {
        return this.getRow().getLastCellNum();
    }

    public int getRowNum() {
        return this.getRow().getRowNum() + 1;
    }

    public List<ExcelCell> getCellList() {
        return IteratorUtils.toList(this.iterator());
    }

    public ExcelCell createCell(int columnNumber) {
        this.validateColumnIndex(columnNumber);
        this.getRow().createCell(columnNumber - 1);
        return this.getCell(columnNumber);
    }

    public Row getRow() {
        return this.row;
    }

    public void setRow(Row row) {
        this.row = row;
    }

    public void setStyle(ExcelCellStyle excelCellStyle) {
        Iterator i$ = IteratorUtils.toList(this.iterator()).iterator();

        while(i$.hasNext()) {
            ExcelCell cell = (ExcelCell)i$.next();
            cell.setStyle(excelCellStyle);
        }

        this.row.setRowStyle(excelCellStyle.getCellStyle());
    }

    public ExcelCellStyle getStyle() {
        return this.row.getRowStyle() == null ? null : new ExcelCellStyle(this.row.getRowStyle());
    }

    public boolean isCellNull(int columnNumber) {
        this.validateColumnIndex(columnNumber);
        return this.row.getCell(columnNumber - 1) == null;
    }

    public Iterator<ExcelCell> iterator() {
        return new Iterator<ExcelCell>() {
            int index = 1;
            int endIndex = ExcelRow.this.getMaxCellNum();

            public boolean hasNext() {
                return this.index <= this.endIndex;
            }

            public ExcelCell next() {
                return ExcelRow.this.getCell(this.index++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Ask for implementation. It was not implemented due to unusage");
            }
        };
    }

    public String getBookName() {
        return this.excelSheet.getExcelBookName();
    }

    public String getSheetName() {
        return this.excelSheet.getSheetName();
    }

    public void validateColumnIndex(int columnNumber) {
        this.validateColumnIndex(columnNumber, false);
    }

    public void validateColumnIndex(int columnNumber, boolean validateTopRange) {
        if (columnNumber <= 0 || this.excelSheet.getExcelBook().isVerifyTopCellsRage() && validateTopRange && columnNumber > this.getMaxCellNum()) {
            String exception = validateTopRange ? "is out of range: (1.." + this.getMaxCellNum() + ")" : "should be create than 1";
            throw new IllegalArgumentException("Cell index (" + columnNumber + ") " + exception + ". File name is '" + this.getBookName() + "'. Sheet name is '" + this.getSheetName() + "'. Row number is " + this.getRowNum());
        }
    }

    public void dispose() {
        this.currentCell = null;
        this.excelSheet = null;
        this.row = null;
    }
}

