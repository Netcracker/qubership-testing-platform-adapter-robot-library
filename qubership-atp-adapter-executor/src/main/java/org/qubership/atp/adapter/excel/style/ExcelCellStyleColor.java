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

import org.apache.poi.ss.usermodel.IndexedColors;

public enum ExcelCellStyleColor {
    BLACK(IndexedColors.BLACK.getIndex()),
    WHITE(IndexedColors.WHITE.getIndex()),
    RED(IndexedColors.RED.getIndex()),
    BRIGHT_GREEN(IndexedColors.BRIGHT_GREEN.getIndex()),
    BLUE(IndexedColors.BLUE.getIndex()),
    YELLOW(IndexedColors.YELLOW.getIndex()),
    PINK(IndexedColors.PINK.getIndex()),
    TURQUOISE(IndexedColors.TURQUOISE.getIndex()),
    DARK_RED(IndexedColors.DARK_RED.getIndex()),
    GREEN(IndexedColors.GREEN.getIndex()),
    DARK_BLUE(IndexedColors.DARK_BLUE.getIndex()),
    DARK_YELLOW(IndexedColors.DARK_YELLOW.getIndex()),
    VIOLET(IndexedColors.VIOLET.getIndex()),
    TEAL(IndexedColors.TEAL.getIndex()),
    GREY_25_PERCENT(IndexedColors.GREY_25_PERCENT.getIndex()),
    GREY_50_PERCENT(IndexedColors.GREY_50_PERCENT.getIndex()),
    CORNFLOWER_BLUE(IndexedColors.CORNFLOWER_BLUE.getIndex()),
    MAROON(IndexedColors.MAROON.getIndex()),
    LEMON_CHIFFON(IndexedColors.LEMON_CHIFFON.getIndex()),
    ORCHID(IndexedColors.ORCHID.getIndex()),
    CORAL(IndexedColors.CORAL.getIndex()),
    ROYAL_BLUE(IndexedColors.ROYAL_BLUE.getIndex()),
    LIGHT_CORNFLOWER_BLUE(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()),
    SKY_BLUE(IndexedColors.SKY_BLUE.getIndex()),
    LIGHT_TURQUOISE(IndexedColors.LIGHT_TURQUOISE.getIndex()),
    LIGHT_GREEN(IndexedColors.LIGHT_GREEN.getIndex()),
    LIGHT_YELLOW(IndexedColors.LIGHT_YELLOW.getIndex()),
    PALE_BLUE(IndexedColors.PALE_BLUE.getIndex()),
    ROSE(IndexedColors.ROSE.getIndex()),
    LAVENDER(IndexedColors.LAVENDER.getIndex()),
    TAN(IndexedColors.TAN.getIndex()),
    LIGHT_BLUE(IndexedColors.LIGHT_BLUE.getIndex()),
    AQUA(IndexedColors.AQUA.getIndex()),
    LIME(IndexedColors.LIME.getIndex()),
    GOLD(IndexedColors.GOLD.getIndex()),
    LIGHT_ORANGE(IndexedColors.LIGHT_ORANGE.getIndex()),
    ORANGE(IndexedColors.ORANGE.getIndex()),
    BLUE_GREY(IndexedColors.BLUE_GREY.getIndex()),
    GREY_40_PERCENT(IndexedColors.GREY_40_PERCENT.getIndex()),
    DARK_TEAL(IndexedColors.DARK_TEAL.getIndex()),
    SEA_GREEN(IndexedColors.SEA_GREEN.getIndex()),
    DARK_GREEN(IndexedColors.DARK_GREEN.getIndex()),
    OLIVE_GREEN(IndexedColors.OLIVE_GREEN.getIndex()),
    BROWN(IndexedColors.BROWN.getIndex()),
    PLUM(IndexedColors.PLUM.getIndex()),
    INDIGO(IndexedColors.INDIGO.getIndex()),
    GREY_80_PERCENT(IndexedColors.GREY_80_PERCENT.getIndex()),
    AUTOMATIC(IndexedColors.AUTOMATIC.getIndex());

    private short index;

    private ExcelCellStyleColor(int idx) {
        this.index = (short)idx;
    }

    public short getColorIndex() {
        return this.index;
    }
}

