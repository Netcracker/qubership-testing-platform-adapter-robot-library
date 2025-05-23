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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public interface Keyword extends Section {
    LinkedList<DataItem> getDataItems();

    String toHtml();

    String getStringToCompare();

    String getOptionalProperty(String var1);

    void setOptionalProperty(String var1, String var2);

    void setOptionalProperties(Map<String, String> var1);

    Route getRoute();

    void setRoute(Route var1);

    LinkedHashMap<String, KeywordParameter> getKeywordParameters() throws IllegalStateException;

    boolean isShot();

    void setSnapshotEnable(boolean var1);
}

