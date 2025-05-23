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

import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RouteGroup extends LinkedList<Route> {
    private static final Log log = LogFactory.getLog(RouteGroup.class);
    private final String name;
    private static final long serialVersionUID = -5456796519418419700L;

    public RouteGroup(String name) {
        this.name = name;
    }

    public void addFirst(Route e) {
        this.logRouteAdding(e);
        super.addFirst(e);
    }

    public void addLast(Route e) {
        this.logRouteAdding(e);
        super.addLast(e);
    }

    public boolean add(Route e) {
        this.logRouteAdding(e);
        return super.add(e);
    }

    public boolean addAll(Collection<? extends Route> c) {
        this.logRouteAdding((Route[])c.toArray(new Route[c.size()]));
        return super.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends Route> c) {
        this.logRouteAdding((Route[])c.toArray(new Route[c.size()]));
        return super.addAll(index, c);
    }

    public void add(int index, Route e) {
        this.logRouteAdding(e);
        super.add(index, e);
    }

    protected void logRouteAdding(Route... c) {
        if (log.isTraceEnabled()) {
            Route[] var2 = c;
            int var3 = c.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Route e = var2[var4];
                log.trace("Route added into " + this.getName() + " group:" + e);
            }
        }

    }

    public String getName() {
        return this.name;
    }
}

