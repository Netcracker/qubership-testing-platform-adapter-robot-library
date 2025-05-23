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
import org.qubership.atp.adapter.excel.exceptions.InvalidFormatOfSourceException;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.TestCase;
import org.qubership.atp.adapter.testcase.Config;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TextDataSetReader implements ParametersReader<TestCase> {
    private static final Log log = LogFactory.getLog(TestSuiteTextReader.class);
    private static String TEST_DATA_SET_CONTEXT = Config.getString("user.dataset.context", "Context:");
    private final String filePath;

    public TextDataSetReader(String filePath) {
        this.filePath = filePath;
    }

    public void loadParameters(TestCase testCase) {
        String[] fileDataSet = this.filePath.split("/");
        String[] var3 = fileDataSet;
        int var4 = fileDataSet.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String fileNamePart = var3[var5];
            String fileName = fileNamePart.trim().toLowerCase();
            if (StringUtils.isNotBlank(fileName)) {
                if (fileName.endsWith(".txt")) {
                    this.readParamTXT(testCase, fileDataSet);
                } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                    this.readParamExcel(testCase, fileDataSet);
                }
            }
        }

    }

    private void readParamTXT(Executable testCase, String[] dataSetFile) {
        String dataSetFileName = "";
        String path = "";
        String context = "";

        for(int i = 0; i < dataSetFile.length; ++i) {
            if (dataSetFile[i].trim().endsWith(".txt")) {
                dataSetFileName = dataSetFile[i].trim();
                if (i > 0) {
                    for(int j = 0; j < i; ++j) {
                        path = path + dataSetFile[j].trim() + "/";
                    }
                }

                if (i + 1 < dataSetFile.length) {
                    context = dataSetFile[i + 1].trim();
                    break;
                }
            }
        }

        if (StringUtils.isBlank(context)) {
            context = testCase.getName();
        }

        TextReader textReader = new TextReader(path + dataSetFileName);
        List<String> textContent = textReader.getFileContent();
        Map<String, String> parameters = new HashMap();

        for(int i = 0; i < textContent.size(); ++i) {
            String trimCurrentLine = ((String)textContent.get(i)).trim();
            if (trimCurrentLine.startsWith(TEST_DATA_SET_CONTEXT)) {
                String currentContext = trimCurrentLine.substring(trimCurrentLine.indexOf(":") + 1).trim();
                if (currentContext.equals(context)) {
                    for(int j = i; j < textContent.size(); i = j++) {
                        if (((String)textContent.get(j)).contains("=")) {
                            String currentLine = ((String)textContent.get(j)).trim();
                            String[] params = this.parseLine(currentLine);
                            if (StringUtils.isNotBlank(params[0]) && StringUtils.isNotBlank(params[1])) {
                                String paramName = params[0].trim();
                                String paramValue = params[1].trim();
                                parameters.put(paramName, paramValue);
                            }
                        } else if (((String)textContent.get(j)).startsWith(TEST_DATA_SET_CONTEXT) && !((String)textContent.get(j)).substring(((String)textContent.get(j)).indexOf(":") + 1).trim().equals(context)) {
                            break;
                        }
                    }
                }
            }
        }

        Iterator var19 = parameters.entrySet().iterator();

        while(var19.hasNext()) {
            Map.Entry entrySet = (Map.Entry)var19.next();
            testCase.setParam(entrySet.getKey().toString(), entrySet.getValue());
        }

    }

    private String[] parseLine(String currentLine) {
        String[] result = new String[2];
        if (currentLine.contains("=")) {
            result[0] = currentLine.substring(0, currentLine.indexOf("=")).trim();
            result[1] = currentLine.substring(currentLine.indexOf("=") + 1).trim();
        }

        return result;
    }

    private void readParamExcel(Executable testCase, String[] fileDataSet) {
        try {
            String file = "";
            String sheet = "";
            String context = "";
            boolean isFile = false;
            boolean isSheet = false;
            String[] var8 = fileDataSet;
            int var9 = fileDataSet.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                String aFileDataSet = var8[var10];
                String fileNamePart = aFileDataSet.trim();
                if (StringUtils.isNotBlank(fileNamePart)) {
                    String lowerCaseName = fileNamePart.toLowerCase();
                    if (!lowerCaseName.endsWith(".xls") && !lowerCaseName.endsWith(".xlsx")) {
                        if (isSheet && isFile) {
                            context = fileNamePart;
                        } else if (isFile && !isSheet) {
                            sheet = fileNamePart;
                            isSheet = true;
                        } else if (!isFile && !isSheet) {
                            file = file + fileNamePart + "/";
                        }
                    } else {
                        file = file + fileNamePart;
                        isFile = true;
                    }
                }
            }

            if (StringUtils.isBlank(context)) {
                context = testCase.getName();
            }

            ExcelBook book = new ExcelBook(file);
            book.setVerifyTopCellsRage(false);
            if (StringUtils.isBlank(sheet)) {
                sheet = book.getSheet(1).getSheetName();
            }

            BasicFormatParametersReader paramReader = new BasicFormatParametersReader(book, sheet);
            ExcelSheet excelSheet = new ExcelSheet(book, sheet, 1, BasicFormatParametersReader.HEADERS);
            paramReader.loadParameters(context, testCase, excelSheet);
        } catch (InvalidFormatOfSourceException var14) {
            InvalidFormatOfSourceException ex = var14;
            log.error("Incorrect file format : " + Arrays.toString(fileDataSet), ex);
        }

    }
}

