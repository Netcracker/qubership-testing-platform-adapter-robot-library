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

package org.qubership.atp.adapter.keyworddriven.executable;

import org.qubership.atp.adapter.utils.KDTUtils;
import java.io.File;
import java.util.List;

public class ExcelKeyword extends StringKeyword {
    private String sourceSheetName;
    private String sourceFileName;
    private int rowNum;

    public ExcelKeyword(Executable parent, List<String> keywordDataRaw) {
        super(parent, keywordDataRaw);
    }

    public String toHtml() {
        return this.getDataItems() + "<br>[Book:&nbsp;" + KDTUtils.htmlLink(new File(this.getSourceFileName())) + ",&nbsp;Sheet:&nbsp;" + this.getSourceSheetName() + ",&nbsp;RowNum:&nbsp;" + this.getRowNum() + "]";
    }

    public String getSourceFileName() {
        return this.sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceSheetName() {
        return this.sourceSheetName;
    }

    public void setSourceSheetName(String sourceSheetName) {
        this.sourceSheetName = sourceSheetName;
    }

    public String toString() {
        return this.getDataItems() + " (" + this.getSourceFileName() + "$" + this.getSourceSheetName() + "#" + this.getRowNum() + ")";
    }

    public int getRowNum() {
        return this.rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }
}

