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

public class DataItem {
    private final String sourceData;
    private String data;
    private RouteItem routeItem;

    public DataItem(String sourceData) {
        this.sourceData = sourceData;
        this.data = sourceData;
        this.routeItem = null;
    }

    public String toString() {
        return this.data;
    }

    public RouteItem getRouteItem() {
        return this.routeItem;
    }

    public void setRouteItem(RouteItem routeItem) {
        this.routeItem = routeItem;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSourceData() {
        return this.sourceData;
    }
}
