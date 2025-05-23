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

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import org.qubership.atp.adapter.excel.ExcelBook;
import org.qubership.atp.adapter.excel.ExcelCell;
import org.qubership.atp.adapter.excel.ExcelRow;
import org.qubership.atp.adapter.excel.ExcelSheet;
import org.qubership.atp.adapter.keyworddriven.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.ExcelKeyword;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.routing.KeywordRouteTable;
import org.qubership.atp.adapter.keyworddriven.routing.Route;
import org.qubership.atp.adapter.keyworddriven.routing.RouteItem;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.excel.ExcelUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class BasicFormatKeywordsReader {
    public static final String ACTION_COLUMN_NAME = Config.getString("BasicFormatKeywordsReader.ACTION_COLUMN_NAME", "Action");
    public static final String DESCRIPTION_COLUMN_NAME = Config.getString("BasicFormatKeywordsReader.DESCRIPTION_COLUMN_NAME", "Description");
    public static final String LEVEL_COLUMN_NAME = Config.getString("BasicFormatKeywordsReader.LEVEL_COLUMN_NAME", "Run");
    public static final String[] HEADERS;
    public static final int KEY_COLUMN_INDEX;
    private ExcelSheet excelSheet;

    public BasicFormatKeywordsReader(ExcelBook excelBook, String sheetName) throws InvalidFormatOfSourceException {
        try {
            this.excelSheet = excelBook.openSheet(sheetName, this.getHeaders());
        } catch (org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException var4) {
            org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException e = var4;
            throw new InvalidFormatOfSourceException(e.getMessage(), e);
        }

        ExcelUtils.checkHeaders(this.excelSheet, this.getHeaders());
    }

    public BasicFormatKeywordsReader(ExcelSheet excelSheet) throws InvalidFormatOfSourceException {
        this.excelSheet = excelSheet;
        ExcelUtils.checkHeaders(excelSheet, this.getHeaders());
    }

    public List<ExcelKeyword> readKeywords(Executable parent) throws InvalidFormatOfSourceException {
        return this.readKeywords(parent, parent.getName());
    }

    public List<ExcelKeyword> readKeywords(Executable parent, String key) throws InvalidFormatOfSourceException {
        List<ExcelKeyword> result = new ArrayList();
        ExcelCell cell = this.excelSheet.getCellByIndexAndContent(KEY_COLUMN_INDEX, key);
        if (cell == null) {
            return result;
        } else {
            for(int index = cell.getPosition().getRowNum(); index <= this.excelSheet.getMaxRowNum(); ++index) {
                if (this.excelSheet.getRow(index) != null && this.excelSheet.getCurrentRow().getCell(KEY_COLUMN_INDEX).getValue().equals(key)) {
                    List<String> dataItems = this.parseKeyword(this.excelSheet.getCurrentRow());
                    ExcelKeyword keyword = null;
                    if (dataItems.size() == 1 && Route.IS_SPACE_DELIM_ENABLED) {
                        List<String> data = parseKeyword((String)dataItems.get(0));
                        keyword = data == null ? null : new ExcelKeyword(parent, data);
                    }

                    if (keyword == null) {
                        keyword = new ExcelKeyword(parent, dataItems);
                    }

                    keyword.setOptionalProperties(this.parseOptionalProperties(this.excelSheet.getCurrentRow()));
                    keyword.setSourceFileName(this.excelSheet.getExcelBook().getCurrentFile().getAbsolutePath());
                    keyword.setSourceSheetName(this.excelSheet.getSheetName());
                    keyword.setRowNum(this.excelSheet.getCurrentRow().getRowNum());
                    keyword.setDescription(String.valueOf(keyword.getOptionalProperty(DESCRIPTION_COLUMN_NAME)));
                    String validationLevelName = keyword.getOptionalProperty(LEVEL_COLUMN_NAME);
                    if (StringUtils.isBlank(validationLevelName) && Boolean.parseBoolean(Config.getString("report.snapshot.filter", "false"))) {
                        keyword.setSnapshotEnable(false);
                    }

                    keyword.setValidationLevel(ValidationLevel.parseValidationLevel(validationLevelName));
                    this.processFlags(keyword, this.excelSheet);
                    result.add(keyword);
                }
            }

            return result;
        }
    }

    protected void processFlags(Executable keyword, ExcelSheet excelSheet) {
    }

    protected Map<String, String> parseOptionalProperties(ExcelRow row) {
        int lastIndex = (Integer)this.excelSheet.getHeaderIndexesByName(ACTION_COLUMN_NAME).get(0);
        return ExcelUtils.readRowWithHeaders(this.excelSheet, row, 1, lastIndex);
    }

    protected List<String> parseKeyword(ExcelRow row) {
        int firstIndex = (Integer)this.excelSheet.getHeaderIndexesByName(ACTION_COLUMN_NAME).get(0);
        List<String> list = ExcelUtils.readRow(row, firstIndex);
        list.removeAll(Collections.singleton(""));
        if (list.isEmpty()) {
            list.add("");
        }

        return list;
    }

    public static List<String> parseKeyword(String keyword) {
        Route route = KeywordRouteTable.searchRoute(keyword);
        if (route == null) {
            return null;
        } else {
            ArrayList<String> dataItems = new ArrayList();
            Pattern pattern = buildNamedPattern(route.getRouteItems(), route);
            Matcher m = pattern.matcher(keyword);
            m.find();
            dataItems.addAll(m.namedGroups().values());

            for(int i = 0; i < dataItems.size(); ++i) {
                String item = dataItems.get(i) == null ? "" : (String)dataItems.get(i);
                dataItems.set(i, item.trim());
            }

            return dataItems;
        }
    }

    private static Pattern buildNamedPattern(List<RouteItem> routeItems, Route route) {
        StringBuilder clued = new StringBuilder("^");

        for(int index = 0; index < routeItems.size(); ++index) {
            RouteItem item = (RouteItem)routeItems.get(index);
            String tab = index > 0 ? route.getDelim() : "";
            clued.append("(?<").append(String.valueOf(index)).append(">").append(tab).append(item.getCellContentPattern()).append(")");
            if (item.isParameter()) {
                clued.append("?");
            }
        }

        clued.append("$");
        return Pattern.compile(clued.toString(), 34);
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
        HEADERS = new String[]{ACTION_COLUMN_NAME, DESCRIPTION_COLUMN_NAME};
        KEY_COLUMN_INDEX = Integer.parseInt(Config.getString("BasicFormatKeywordsReader.KEY_COLUMN_INDEX", "1"));
    }
}

