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

import org.qubership.atp.adapter.keyworddriven.handlers.ActionExecutor;
import org.qubership.atp.adapter.testcase.Config;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class Route {
    private static final Log log = LogFactory.getLog(Route.class);
    public static final boolean IS_SPACE_DELIM_ENABLED = Boolean.parseBoolean(Config.getString("kdt.enable.llk"));
    private final ArrayList<RouteItem> routeItems;
    private final ActionExecutor executor;
    private Pattern routeMask;
    private Automaton maskAutomaton;
    private int rating;
    private final String description;
    private String delim;
    private final boolean deprecated;

    public Route(ArrayList<RouteItem> routeItems, Class<? extends ActionExecutor> executor) {
        this((String)null, routeItems, executor);
    }

    public Route(String description, ArrayList<RouteItem> routeItems, Class<? extends ActionExecutor> executor) {
        this((String)null, routeItems, executor, false);
    }

    public Route(String description, ArrayList<RouteItem> routeItems, Class<? extends ActionExecutor> executor, boolean deprecated) {
        this.rating = 0;
        this.routeItems = routeItems;
        this.executor = getExecutor(executor);
        this.description = description == null ? "" : description;
        this.deprecated = deprecated;
        this.delim = IS_SPACE_DELIM_ENABLED ? "(\t| +)" : "\t";

        try {
            this.routeMask = this.getRouteMask(routeItems);
        } catch (PatternSyntaxException var7) {
            PatternSyntaxException e = var7;
            throw new IllegalArgumentException(String.format("Route '%s' has wrong format of '%s'. Error: %s", description, routeItems.toString(), e.getMessage()), e);
        }

        try {
            this.maskAutomaton = (new RegExp(PatternConverter.convert(this.routeMask))).toAutomaton(true);
        } catch (IllegalArgumentException var6) {
            IllegalArgumentException e = var6;
            throw new IllegalArgumentException(String.format("Route '%s' has wrong format of pattern '%s'. Error: %s", routeItems.toString(), this.routeMask.pattern(), e.getMessage()), e);
        }
    }

    public ActionExecutor getActionExecutor() {
        return this.executor;
    }

    public String getName() {
        return ((RouteItem)this.routeItems.get(0)).toString();
    }

    public Pattern getRouteMask() {
        return this.routeMask;
    }

    private Pattern getRouteMask(ArrayList<RouteItem> routeItems) {
        StringBuilder clued = new StringBuilder();
        String tab = "";

        for(int index = 0; index < routeItems.size(); ++index) {
            RouteItem item = (RouteItem)routeItems.get(index);
            if (index > 0) {
                tab = this.delim;
            }

            String addon;
            if (item.isParameter()) {
                addon = "(" + tab + item.getCellContentPattern() + ")?";
            } else {
                addon = tab + item.getCellContentPattern().toString();
            }

            clued.append(addon);
        }

        return Pattern.compile(clued.toString(), 34);
    }

    public String getDelim() {
        return this.delim;
    }

    public ArrayList<RouteItem> getRouteItems() {
        return this.routeItems;
    }

    protected ActionExecutor getExecutor() {
        return this.executor;
    }

    public String toString() {
        return String.valueOf(this.getRouteItems());
    }

    public RouteItem getConstantRouteItem(String content) {
        Iterator var2 = this.routeItems.iterator();

        RouteItem routeItem;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            routeItem = (RouteItem)var2.next();
        } while(routeItem.isParameter() || !routeItem.getCellContentPattern().matcher(content).matches());

        return routeItem;
    }

    public static ArrayList<RouteItem> parseMaskInRouteItems(Object[] sourceMask) {
        ArrayList<RouteItem> res = new ArrayList();
        Object[] var2 = sourceMask;
        int var3 = sourceMask.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Object itemSource = var2[var4];
            if (!(itemSource instanceof String) || !((String)itemSource).isEmpty()) {
                RouteItem routeItem = new RouteItem(itemSource);
                res.add(routeItem);
            }
        }

        return res;
    }

    protected static ActionExecutor getExecutor(Class<? extends ActionExecutor> clazz) {
        try {
            return (ActionExecutor)clazz.newInstance();
        } catch (Throwable var2) {
            Throwable e = var2;
            log.error("Error route instantiation:" + e);
            return null;
        }
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isSubsetOf(Route route) {
        return this.maskAutomaton.subsetOf(route.maskAutomaton);
    }

    public int getRating() {
        return this.rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public boolean deprecated() {
        return this.deprecated;
    }
}

