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

import org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.excel.style.ExcelCellStyle;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatPart;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;

public class ExcelCell {
    private static Log log = LogFactory.getLog(ExcelCell.class);
    private Cell cell;
    private String value;
    private boolean isChanged = false;
    private CellPosition position;

    public ExcelCell(Cell cell) {
        this.cell = cell;
        this.position = new CellPosition(cell.getRowIndex() + 1, cell.getColumnIndex() + 1);
    }

    public void setValue(String value) {
        this.isChanged = true;
        this.value = (String)(new WeakReference(value)).get();
        this.applyChanges();
    }

    public String getValue() {
        try {
            return (String)(new WeakReference(this.getValueUnSafe())).get();
        } catch (InvalidFormatOfSourceException var2) {
            return (String)(new WeakReference("")).get();
        }
    }

    public String getValueUnSafe() throws InvalidFormatOfSourceException {
        if (this.value != null) {
            return this.value;
        } else {
            DataFormatter form = new DataFormatter(ExcelBook.locale);
            String result = "";
            if (this.cell != null) {
                try {
                    FormulaEvaluator evaluator = this.cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    String formula = this.cell.toString();
                    this.cell = evaluator.evaluateInCell(this.cell);
                    String dataFormatString = this.cell.getCellStyle().getDataFormatString();
                    if (this.cell.getCellType() == 0 && DateUtil.isValidExcelDate(this.cell.getNumericCellValue()) && this.getCellFormatTypeByDataFormatString(dataFormatString.replaceAll(";.*", "")) == CellFormatType.DATE) {
                        if (formula.equals(this.cell.toString())) {
                            result = CellFormat.getInstance(dataFormatString).apply(this.cell).text;
                        } else {
                            result = CellFormat.getInstance(dataFormatString).apply(this.cell).text;
                            this.cell.setCellFormula(formula);
                        }
                    } else {
                        result = form.formatCellValue(this.cell, evaluator);
                    }
                } catch (Exception var6) {
                    Exception e = var6;
                    if (this.cell.getCellType() == 5) {
                        if (this.cell instanceof XSSFCell) {
                            result = ((XSSFCell)this.cell).getErrorCellString();
                        } else {
                            result = "####";
                        }
                    }

                    throw new InvalidFormatOfSourceException("Cell parsing error\tSheet: " + this.cell.getSheet().getSheetName() + ", " + "\tRow: " + (this.cell.getRowIndex() + 1) + ", " + "\tCell: " + (this.cell.getColumnIndex() + 1) + ", " + "\tValue: " + result, e);
                }
            }

            return result;
        }
    }

    public CellPosition getPosition() {
        return this.position;
    }

    public boolean isChanged() {
        return this.isChanged;
    }

    public void applyChanges() {
        this.cell.setCellType(1);
        this.cell.setCellValue((String)(new WeakReference(this.value)).get());
    }

    public Cell getCell() {
        return this.cell;
    }

    public ExcelCellStyle createExcelCellStyle() {
        return new ExcelCellStyle(this);
    }

    public ExcelCellStyle getStyle() {
        return new ExcelCellStyle(this.cell.getCellStyle());
    }

    public void setStyle(ExcelCellStyle excelCellStyle) {
        this.cell.setCellStyle((CellStyle)(new WeakReference(excelCellStyle.getCellStyle())).get());
    }

    private CellFormatType getCellFormatTypeByDataFormatString(String dataFormatString) {
        try {
            Method getCellFormatType = CellFormatPart.class.getDeclaredMethod("getCellFormatType", (Class[])null);
            getCellFormatType.setAccessible(true);
            return (CellFormatType)getCellFormatType.invoke(new CellFormatPart(dataFormatString.replaceAll(";", "")));
        } catch (InvocationTargetException var3) {
            InvocationTargetException e = var3;
            e.printStackTrace();
        } catch (IllegalAccessException var4) {
            IllegalAccessException e = var4;
            e.printStackTrace();
        } catch (NoSuchMethodException var5) {
            NoSuchMethodException e = var5;
            e.printStackTrace();
        }

        log.error("CellFormatType can't be taken. General value will be returned.");
        return CellFormatType.GENERAL;
    }
}

