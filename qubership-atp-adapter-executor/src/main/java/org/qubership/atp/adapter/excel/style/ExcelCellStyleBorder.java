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

public enum ExcelCellStyleBorder {
    NONE(0),
    THIN(1),
    MEDIUM(2),
    DASHED(2),
    HAIR(3),
    THICK(7),
    DOUBLE(5),
    DOTTED(6),
    MEDIUM_DASHED(4),
    DASH_DOT(8),
    MEDIUM_DASH_DOT(10),
    DASH_DOT_DOT(11),
    MEDIUM_DASH_DOT_DOT(12),
    SLANTED_DASH_DOT(13);

    private short borderStyle;

    private ExcelCellStyleBorder(int styleIndex) {
        this.borderStyle = (short)styleIndex;
    }

    public short getBorderIndex() {
        return this.borderStyle;
    }
}

