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
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.testcase.Config;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public abstract class ExcelParametersReader implements ParametersReader<Section> {
    public static final String EXCEL_PARAMETERS_READER = "kdt.excel.parameters.reader";
    private static final Logger log = Logger.getLogger(ExcelParametersReader.class);
    protected final ExcelBook excelBook;
    protected final String sheetName;

    public static ExcelParametersReader get(ExcelBook book, String sheetName) throws InvalidFormatOfSourceException {
        String reader = Config.getString("kdt.excel.parameters.reader");
        return (ExcelParametersReader)(StringUtils.isEmpty(reader) ? new BasicFormatParametersReader(book, sheetName) : getCustomParametersReader(book, sheetName, reader));
    }

    private static ExcelParametersReader getCustomParametersReader(ExcelBook book, String sheetName, String reader) throws InvalidFormatOfSourceException {
        try {
            Class<? extends ExcelParametersReader> c = Class.forName(reader).asSubclass(ExcelParametersReader.class);
            Constructor ctor = c.getConstructor(book.getClass(), sheetName.getClass());
            return (ExcelParametersReader)ctor.newInstance(book, sheetName);
        } catch (ClassNotFoundException var5) {
            ClassNotFoundException e = var5;
            throw new InvalidFormatOfSourceException("kdt.excel.parameters.reader class is not found: " + reader, e);
        } catch (ClassCastException var6) {
            ClassCastException e = var6;
            throw new InvalidFormatOfSourceException("kdt.excel.parameters.reader class '" + reader + "' does not extend " + ExcelParametersReader.class, e);
        } catch (NoSuchMethodException var7) {
            NoSuchMethodException e = var7;
            throw new InvalidFormatOfSourceException("kdt.excel.parameters.reader class '" + reader + "' does not have constructor with book / sheetName parameters", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException var8) {
            ReflectiveOperationException e = var8;
            throw new InvalidFormatOfSourceException("Can not create kdt.excel.parameters.reader instance '" + reader + "'. Error: " + ((ReflectiveOperationException)e).getMessage(), e);
        }
    }

    public ExcelParametersReader(ExcelBook excelBook, String sheetName) {
        this.excelBook = excelBook;
        this.sheetName = sheetName;
    }
}

