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

import org.qubership.atp.adapter.keyworddriven.routing.Route;
import org.qubership.atp.adapter.keyworddriven.routing.RouteItem;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StringKeyword extends SectionImpl implements Keyword {
    private LinkedList<DataItem> dataItems;
    private SoftReference<LinkedHashMap<String, KeywordParameter>> keywordParams = new SoftReference((Object)null);
    private Map<String, String> optionalProperties = new LinkedHashMap();
    private Route route;
    private int validationLevel;
    private boolean isShot = true;

    public StringKeyword(Executable parent, List<String> keywordDataRaw) {
        super(keywordDataRaw.size() == 0 ? "" : (String)keywordDataRaw.get(0), parent);
        this.dataItems = parseDataRaw(keywordDataRaw);
    }

    protected static LinkedList<DataItem> parseDataRaw(List<String> data) {
        LinkedList<DataItem> res = new LinkedList();
        Iterator var2 = data.iterator();

        while(var2.hasNext()) {
            String d = (String)var2.next();
            res.add(new DataItem(d));
        }

        return res;
    }

    public LinkedList<DataItem> getDataItems() {
        return this.dataItems;
    }

    public String toHtml() {
        return this.toString();
    }

    public String getStringToCompare() {
        String result = "";
        List<DataItem> dataItems = this.getDataItems();

        for(int index = 0; index < dataItems.size(); ++index) {
            String data = ((DataItem)dataItems.get(index)).getData();
            result = result + data;
            if (index != dataItems.size() - 1 && !data.isEmpty()) {
                result = result + "\t";
            }
        }

        return result;
    }

    public String toString() {
        return this.getDataItems().toString();
    }

    public String getOptionalProperty(String key) {
        return (String)this.optionalProperties.get(key);
    }

    public void setOptionalProperty(String key, String value) {
        this.optionalProperties.put(key, value);
    }

    public Route getRoute() {
        return this.route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    protected Map<String, String> getOptionalProperties() {
        return this.optionalProperties;
    }

    public void setOptionalProperties(Map<String, String> map) {
        this.optionalProperties = map;
    }

    public LinkedHashMap<String, KeywordParameter> getKeywordParameters() throws IllegalStateException {
        if (this.route == null) {
            throw new IllegalStateException("Route is not matched yet");
        } else {
            LinkedHashMap<String, KeywordParameter> params = (LinkedHashMap)this.keywordParams.get();
            if (params == null) {
                params = this.defineParams();
                this.keywordParams = new SoftReference(params);
            }

            return params;
        }
    }

    public LinkedHashMap<String, KeywordParameter> defineParams() {
        LinkedHashMap<String, KeywordParameter> params = new LinkedHashMap();
        Iterator var3 = this.dataItems.iterator();

        while(var3.hasNext()) {
            DataItem dataItem = (DataItem)var3.next();
            RouteItem routeItem = dataItem.getRouteItem();
            if (routeItem.isParameter()) {
                String paramName = routeItem.getParamName();
                if (!params.containsKey(paramName)) {
                    params.put(paramName, new KeywordParameter(routeItem));
                }

                ((KeywordParameter)params.get(paramName)).addDataItem(dataItem);
            }
        }

        return params;
    }

    public int getValidationLevel() {
        return this.validationLevel;
    }

    public void setValidationLevel(int lvl) {
        this.validationLevel = lvl;
    }

    public boolean isShot() {
        return this.isShot;
    }

    public void setSnapshotEnable(boolean isShot) {
        this.isShot = isShot;
    }
}

