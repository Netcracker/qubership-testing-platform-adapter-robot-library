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

package org.qubership.atp.adapter.excel.style;

import org.qubership.atp.adapter.excel.ExcelCell;
import org.qubership.atp.adapter.excel.ExcelRow;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelCellStyle {
    private CellStyle cellStyle;
    private Font font;

    public ExcelCellStyle() {
        this.cellStyle = null;
        this.font = null;
    }

    public ExcelCellStyle(ExcelCell excelCell) {
        Workbook wb = excelCell.getCell().getSheet().getWorkbook();
        this.cellStyle = wb.createCellStyle();
        this.font = wb.createFont();
    }

    public ExcelCellStyle(ExcelSheet excelSheet) {
        Workbook wb = excelSheet.getCurrentSheet().getWorkbook();
        this.cellStyle = wb.createCellStyle();
        this.font = wb.createFont();
    }

    public ExcelCellStyle(ExcelRow excelRow) {
        Workbook wb = excelRow.getRow().getSheet().getWorkbook();
        this.cellStyle = wb.createCellStyle();
        this.font = wb.createFont();
    }

    public ExcelCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
    }

    public void setForegroundColor(ExcelCellStyleColor color) {
        this.setForegroundColor(color.getColorIndex());
    }

    public void setForegroundColor(short index) {
        this.cellStyle.setFillForegroundColor(index);
        this.cellStyle.setFillPattern((short)1);
    }

    public void setBorderStyle(ExcelCellStyleBorder border) {
        this.setBorderStyle(border.getBorderIndex());
    }

    public void setBorderStyle(short index) {
        this.cellStyle.setBorderTop(index);
        this.cellStyle.setBorderRight(index);
        this.cellStyle.setBorderBottom(index);
        this.cellStyle.setBorderLeft(index);
    }

    public void setBorderColor(ExcelCellStyleColor color) {
        this.setBorderColor(color.getColorIndex());
    }

    public void setBorderColor(short index) {
        this.cellStyle.setTopBorderColor(index);
        this.cellStyle.setRightBorderColor(index);
        this.cellStyle.setBottomBorderColor(index);
        this.cellStyle.setLeftBorderColor(index);
    }

    public void setAlignment(ExcelCellStyleAlignment alignment) {
        this.setAlignment(alignment.getAlignmentIndex());
    }

    public void setAlignment(short index) {
        this.cellStyle.setAlignment(index);
    }

    public void setVerticalAlignment(ExcelCellStyleVerticalAlignment verticalAlignment) {
        this.setVerticalAlignment(verticalAlignment.getVerticalAlignmentIndex());
    }

    public void setVerticalAlignment(short index) {
        this.cellStyle.setVerticalAlignment(index);
    }

    public void setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
    }

    public CellStyle getCellStyle() {
        return this.cellStyle;
    }

    public short getForegroundColorIndex() {
        return this.cellStyle.getFillForegroundColor();
    }

    public short getBorderIndex() {
        return this.cellStyle.getBorderTop();
    }

    public short getBorderColorIndex() {
        return this.cellStyle.getTopBorderColor();
    }

    public short getAlignmentIndex() {
        return this.cellStyle.getAlignment();
    }

    public short getVerticalAlignmentIndex() {
        return this.cellStyle.getVerticalAlignment();
    }

    public void setFontColor(ExcelCellStyleColor color) {
        this.font.setColor(color.getColorIndex());
        this.cellStyle.setFont(this.font);
    }

    public void setWrapText(boolean wrapped) {
        this.cellStyle.setWrapText(wrapped);
    }

    public boolean getWrapText() {
        return this.cellStyle.getWrapText();
    }
}

