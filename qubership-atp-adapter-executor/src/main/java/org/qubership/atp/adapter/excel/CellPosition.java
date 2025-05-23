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

public class CellPosition implements Comparable {
    private int rowNum;
    private int colNum;

    public CellPosition(int rowNum, int colNum) {
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public int getRowNum() {
        return this.rowNum;
    }

    public int getColNum() {
        return this.colNum;
    }

    public int compareTo(Object o) {
        if (o != null && o instanceof CellPosition) {
            CellPosition position = (CellPosition)o;
            int colDiff = position.getColNum() - this.getColNum();
            int rowDiff = position.getRowNum() - this.getRowNum();
            if (colDiff == 0 && rowDiff == 0) {
                return 0;
            } else {
                return rowDiff >= 0 && colDiff >= 0 ? 1 : -1;
            }
        } else {
            return -1;
        }
    }
}

