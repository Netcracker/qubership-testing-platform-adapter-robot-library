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

package org.qubership.atp.adapter.keyworddriven.routing;

import org.qubership.atp.adapter.keyworddriven.executable.DataItem;
import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import java.util.ArrayList;
import java.util.LinkedList;

public class CellKeywordMapper implements KeywordMapper {
    public CellKeywordMapper() {
    }

    public void assignData(Keyword keyword, Route route) {
        LinkedList<DataItem> dataItems = keyword.getDataItems();
        ArrayList<RouteItem> routeItems = route.getRouteItems();
        int lastConstantIndex = 0;

        for(int index = 0; index < routeItems.size(); ++index) {
            if (dataItems.size() <= index) {
                dataItems.add(new DataItem(""));
            }

            DataItem dataItem = (DataItem)dataItems.get(index);
            RouteItem constRouteItem = route.getConstantRouteItem(dataItem.getData());
            RouteItem routeItem = (RouteItem)routeItems.get(index);
            if (constRouteItem != null && index <= routeItems.indexOf(constRouteItem)) {
                if (lastConstantIndex == index - 1 & routeItem.isParameter()) {
                    dataItem = new DataItem("");
                    dataItems.add(index, dataItem);
                }

                dataItem.setRouteItem(routeItem);
                lastConstantIndex = index;
            } else if (!routeItem.isParameter()) {
                dataItem.setRouteItem(routeItem);
            } else {
                int i = index;

                for(int max = routeItem.getOccupiedCellsCount(); i < max + index || max == 0 && i < dataItems.size(); ++i) {
                    ((DataItem)dataItems.get(i)).setRouteItem(routeItem);
                }
            }
        }

        keyword.setRoute(route);
    }
}

