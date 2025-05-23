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
import org.qubership.atp.adapter.excel.exceptions.RecordingInFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelBook {
    private static Log log = LogFactory.getLog(ExcelBook.class);
    private Workbook workbook;
    private File currentFile;
    private ExcelSheet sheet;
    private boolean verifyTopCellsRage;
    public static Locale locale;

    public ExcelBook(String fileName) throws InvalidFormatOfSourceException {
        this(new File(fileName));
    }

    public ExcelBook(File file) throws InvalidFormatOfSourceException {
        this(file, false);
    }

    public ExcelBook(File file, boolean isSXSSFWorkbook) throws InvalidFormatOfSourceException {
        this.workbook = null;
        this.currentFile = null;
        this.sheet = null;
        this.verifyTopCellsRage = true;
        if (file == null) {
            throw new NullPointerException("File is Null. Please define it first");
        } else {
            if (!file.exists()) {
                create(file, isSXSSFWorkbook);
            }

            FileInputStream fis = null;

            try {
                Workbook wb = WorkbookFactory.create(fis = new FileInputStream(file));
                if (wb instanceof XSSFWorkbook && isSXSSFWorkbook) {
                    this.workbook = new SXSSFWorkbook((XSSFWorkbook)wb, 999999);
                } else {
                    this.workbook = wb;
                }

                this.currentFile = file;
            } catch (IOException var13) {
                IOException ex = var13;
                throw new InvalidFormatOfSourceException(String.format("Could not read '%s' file", file.getAbsolutePath()), ex);
            } catch (InvalidFormatException var14) {
                InvalidFormatException ex = var14;
                throw new InvalidFormatOfSourceException(String.format("Invalid format of '%s' file : ", this.currentFile.getName()), ex);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException var12) {
                        IOException e = var12;
                        log.error(e.getMessage());
                    }
                }

            }

        }
    }

    public ExcelSheet createSheet(String sheetName) throws InvalidFormatOfSourceException {
        if (!this.hasSheet(sheetName)) {
            this.workbook.createSheet(sheetName);
        }

        return this.openSheet(sheetName);
    }

    public boolean hasSheet(String newSheetName) {
        return newSheetName != null && this.workbook.getSheet(newSheetName) != null;
    }

    public ExcelSheet openSheet(String newSheetName) throws InvalidFormatOfSourceException {
        return this.openSheet(newSheetName, 1);
    }

    public ExcelSheet openSheet(String newSheetName, String[] headerRowIdentifiers) throws InvalidFormatOfSourceException {
        return this.openSheet(newSheetName, 1, headerRowIdentifiers);
    }

    public ExcelSheet openSheet(String newSheetName, int headerRowIndex) throws InvalidFormatOfSourceException {
        return this.openSheet(newSheetName, headerRowIndex, new String[0]);
    }

    public ExcelSheet openSheet(String newSheetName, int headerRowIndex, String[] headerRowIdentifiers) throws InvalidFormatOfSourceException {
        if (!this.hasSheet(newSheetName)) {
            throw new InvalidFormatOfSourceException(String.format("Sheet '%s' hasn't been found in the '%s' WB", newSheetName, this.currentFile.getName()));
        } else {
            if (this.sheet == null || !this.sheet.getSheetName().equalsIgnoreCase(newSheetName)) {
                this.sheet = null;
                this.sheet = new ExcelSheet(this, newSheetName, headerRowIndex, headerRowIdentifiers);
            }

            this.sheet.calculateHeaders(headerRowIndex, headerRowIdentifiers);
            return this.sheet;
        }
    }

    public File getCurrentFile() {
        return this.currentFile;
    }

    public void applyChangesToFile() throws IOException {
        FileOutputStream fileOut = null;

        try {
            fileOut = new FileOutputStream(this.getCurrentFile());
            this.workbook.write(fileOut);
        } catch (IOException var6) {
            IOException ex = var6;
            if (this.workbook instanceof SXSSFWorkbook) {
                log.error("XSSFWorkbook saving exception. You can use it only once. Otherwise - you should open file again", ex);
            }

            throw new RecordingInFileException("Recording has been attempted in open file. File: " + this.getCurrentFile().getName() + " being used by another process.", ex);
        } finally {
            if (fileOut != null) {
                fileOut.flush();
                fileOut.close();
            }

        }

    }

    public Workbook getWorkbook() {
        return this.workbook;
    }

    public String toString() {
        return this.getCurrentFile().toString();
    }

    public List<ExcelSheet> getAllSheets() {
        return IteratorUtils.toList(this.iterator());
    }

    public ExcelSheet getSheet(int sheetIndex) {
        this.validateSheetIndex(sheetIndex);
        String name = this.getSheetName(sheetIndex);

        try {
            return this.openSheet(name);
        } catch (InvalidFormatOfSourceException var4) {
            return null;
        }
    }

    public ExcelSheet getSheet(String sheetName) throws InvalidFormatOfSourceException {
        return this.openSheet(sheetName);
    }

    public void removeSheet(int sheetIndex) {
        this.validateSheetIndex(sheetIndex);
        this.workbook.removeSheetAt(sheetIndex - 1);
    }

    public void removeSheet(String sheetName) {
        int sheetIndex = this.getSheetIndex(sheetName);
        this.removeSheet(sheetIndex);
    }

    public String getSheetName(int sheetIndex) {
        this.validateSheetIndex(sheetIndex);
        return this.workbook.getSheetName(sheetIndex - 1);
    }

    public int getSheetIndex(String sheetName) {
        return this.workbook.getSheetIndex(sheetName) + 1;
    }

    public int getSheetIndex(ExcelSheet sheet) {
        return this.getSheetIndex(sheet.getSheetName());
    }

    public ExcelSheet cloneSheet(ExcelSheet excelSheet, String newSheetName) throws InvalidFormatOfSourceException {
        return this.cloneSheet(excelSheet.getSheetName(), newSheetName);
    }

    public ExcelSheet cloneSheet(String sheetName, String newSheetName) throws InvalidFormatOfSourceException {
        int sheetIndex = this.getSheetIndex(sheetName);
        return this.cloneSheet(sheetIndex, newSheetName);
    }

    public ExcelSheet cloneSheet(int sheetIndex) throws InvalidFormatOfSourceException {
        this.validateSheetIndex(sheetIndex);
        return this.cloneSheet(sheetIndex, (String)null);
    }

    public ExcelSheet cloneSheet(int sheetIndex, String newSheetName) throws InvalidFormatOfSourceException {
        this.validateSheetIndex(sheetIndex);
        int lastSheetIndex = this.getMaxSheetNum();
        this.workbook.cloneSheet(sheetIndex - 1);
        if (newSheetName != null) {
            this.workbook.setSheetName(lastSheetIndex - 1, newSheetName);
        } else {
            newSheetName = this.getSheetName(lastSheetIndex);
        }

        this.sheet = null;
        this.sheet = new ExcelSheet(this, newSheetName);
        return this.sheet;
    }

    public Iterator<ExcelSheet> iterator() {
        return new Iterator<ExcelSheet>() {
            int index = 1;
            int endIndex = ExcelBook.this.getMaxSheetNum();

            public boolean hasNext() {
                return this.index <= this.endIndex;
            }

            public ExcelSheet next() {
                return ExcelBook.this.getSheet(this.index++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Ask for implementation. It was not implemented due to unusage");
            }
        };
    }

    public int getMaxSheetNum() {
        return this.workbook.getNumberOfSheets();
    }

    private static void create(File file, boolean isSXSSFWorkbook) {
        String fileName = file.getName();
        Object wb;
        if (FilenameUtils.isExtension(fileName, "xls")) {
            wb = new HSSFWorkbook();
        } else {
            if (!FilenameUtils.isExtension(fileName, "xlsx") && !FilenameUtils.isExtension(fileName, "xlsm")) {
                throw new IllegalArgumentException("Unsupported extension of the file: " + file.getName());
            }

            if (isSXSSFWorkbook) {
                wb = new SXSSFWorkbook(new XSSFWorkbook(), 999999);
            } else {
                wb = new XSSFWorkbook();
            }
        }

        FileOutputStream out = null;

        try {
            if (!file.getPath().equals(file.getName())) {
                file.getParentFile().mkdirs();
            }

            out = new FileOutputStream(file);
            ((Workbook)wb).write(out);
        } catch (IOException var14) {
            IOException ex = var14;
            ex.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException var13) {
                    IOException e = var13;
                    e.printStackTrace();
                    log.fatal("Error during reading ExcelBook: " + e.getMessage());
                }
            }

        }

    }

    private void validateSheetIndex(int sheetIndex) {
        if (sheetIndex <= 0 || sheetIndex > this.getMaxSheetNum()) {
            throw new IllegalArgumentException("Sheet index (" + sheetIndex + ") is out of range 1.." + this.getMaxSheetNum());
        }
    }

    public boolean equals(Object object) {
        return !(object instanceof ExcelBook) ? false : this.toString().equals(((ExcelBook)object).toString());
    }

    public void close() throws IOException {
        if (this.sheet != null) {
            this.sheet.close();
        }

        this.workbook.close();
        this.currentFile = null;
        this.sheet = null;
        this.workbook = null;
    }

    public void setVerifyTopCellsRage(boolean verifyTopCellsRage) {
        this.verifyTopCellsRage = verifyTopCellsRage;
    }

    public boolean isVerifyTopCellsRage() {
        return this.verifyTopCellsRage;
    }

    static {
        locale = Locale.ENGLISH;
    }
}

