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

import org.qubership.atp.adapter.keyworddriven.routing.RouteItem;
import java.util.ArrayList;

public class KeywordParameter {
    private final RouteItem routeItem;
    private ArrayList<DataItem> dataItems;

    public KeywordParameter(RouteItem routeItem) {
        this.routeItem = routeItem;
        this.dataItems = new ArrayList();
    }

    public ArrayList<DataItem> getDataItems() {
        return this.dataItems;
    }

    public void addDataItem(DataItem dataItem) {
        this.dataItems.add(dataItem);
    }

    public RouteItem getRouteItem() {
        return this.routeItem;
    }

    public String toString() {
        return "KeywordParameter " + this.getDataItems();
    }
}

