/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.adapter.keyworddriven.configuration.DefaultConfiguration;
import org.qubership.atp.adapter.keyworddriven.configuration.KdtProperties;
import org.qubership.atp.adapter.keyworddriven.executable.DataItem;
import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import org.qubership.atp.adapter.keyworddriven.routing.annotation.RouteAlias;
import org.qubership.atp.adapter.utils.KDTUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeywordRouteTable {
    public static final String DEFAULT_GROUP_NAME = "Other";
    private static final LinkedHashMap<String, RouteGroup> routeGroups = new LinkedHashMap<>();
    @Inject
    public static KeywordMapper mapper;

    public KeywordRouteTable() {
    }

    @Nullable
    public static Route searchRoute(Keyword keyword) {
        return searchRoute(keyword.getStringToCompare());
    }

    @Nullable
    public static Route searchRoute(String keyword) {
        int rating;
        Iterator var3;
        List routeGroup;
        Iterator var5;
        Route route;
        if (KdtProperties.KDT_ROUTES_MATCHING_STRATEGY_LAZY) {
            Route result = null;
            rating = -1;
            var3 = routeGroups.values().iterator();

            while(var3.hasNext()) {
                routeGroup = (List)var3.next();
                var5 = routeGroup.iterator();

                while(var5.hasNext()) {
                    route = (Route)var5.next();
                    if (route.getRouteMask().matcher(keyword).matches() && route.getRating() > rating) {
                        result = route;
                        rating = route.getRating();
                    }
                }
            }

            return result;
        } else {
            List<Route> result = new ArrayList();
            rating = -1;
            var3 = routeGroups.values().iterator();

            while(var3.hasNext()) {
                routeGroup = (List)var3.next();
                var5 = routeGroup.iterator();

                while(var5.hasNext()) {
                    route = (Route)var5.next();
                    if (route.getRouteMask().matcher(keyword).matches()) {
                        if (route.getRating() > rating) {
                            result.clear();
                            result.add(route);
                            rating = route.getRating();
                        } else if (route.getRating() == rating) {
                            result.add(route);
                        }
                    }
                }
            }

            if (result.isEmpty()) {
                log.error("No one route is found for keyword '{}'", keyword);
                return null;
            } else if (result.size() == 1) {
                return result.getFirst();
            } else {
                log.error("More than one route is found for keyword '{}'. Matched routes: {}", keyword, result);
                return null;
            }
        }
    }

    public static void calculateRoutesRating() {
        log.info("Routes calculation started");
        for (RouteGroup routes : routeGroups.values()) {
            List<Route> routeGroup = (List) routes;
            for (Route route : routeGroup) {
                int rating = 0;
                for (RouteGroup group : routeGroups.values()) {
                    List<Route> routeGroup2 = (List) group;
                    for (Route route2 : routeGroup2) {
                        if (route != route2 && route.isSubsetOf(route2)) {
                            ++rating;
                        }
                    }
                }
                route.setRating(rating);
            }
        }
        log.info("Routes calculation completed");
    }

    public static ArrayList<Route> getRoutesByName(String name) {
        ArrayList<Route> list = new ArrayList<>();
        for (RouteGroup routes : routeGroups.values()) {
            List<Route> routeGroup = (List) routes;
            for (Route route : routeGroup) {
                if (route.getName().equalsIgnoreCase(name)) {
                    list.add(route);
                }
            }
        }
        return list;
    }

    public static void routeToDataAssign(Keyword keyword, Route route) {
        if (mapper == null) {
            KDTUtils.criticalMessAndExit("KeywordMapper is not specified. Default implementation is activated by adding parameter 'library.actions.*' with value = '" + DefaultConfiguration.class + "'");
        }
        mapper.assignData(keyword, route);
        checkAssignRouteAndExit(keyword);
    }

    private static void checkAssignRouteAndExit(Keyword keyword) {
        Route route = keyword.getRoute();
        for (DataItem dataItem : keyword.getDataItems()) {
            if (dataItem.getRouteItem() == null) {
                KDTUtils.criticalMessAndExit("Fatal error in assigning route to keyword.\nRoute: " + route.toString() + "\nRoute Mask: " + route.getRouteMask() + "\nKeyword: " + keyword + "\nKeyword Compare String: " + keyword.getStringToCompare());
            }
        }
    }

    public static boolean matches(Keyword keyword, Route route) {
        return route.getRouteMask().matcher(keyword.getStringToCompare()).matches();
    }

    /** @deprecated */
    public static void register(Route route) {
        registerLast(route);
    }

    public static void registerLast(Route route) {
        registerLast(null, route);
    }

    public static void registerLast(String groupName, Route route) {
        getRouteGroup(groupName).add(route);
    }

    public static void clear() {
        routeGroups.clear();
    }

    public static List<Route> getRouteGroup(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            groupName = "Other";
        }
        if (!routeGroups.containsKey(groupName)) {
            routeGroups.put(groupName, new RouteGroup(groupName));
        }

        return routeGroups.get(groupName);
    }

    public static LinkedHashMap<String, RouteGroup> getRouteGroups() {
        return routeGroups;
    }

    public static void registerAnnotatedRoutes(String actionsPackage) {
        log.debug("[START] Register routes from package: {}", actionsPackage);
        Reflections reflections = new Reflections((new ConfigurationBuilder()).filterInputsBy((new FilterBuilder()).includePackage(actionsPackage)).setUrls(ClasspathHelper.forPackage(actionsPackage)).setScanners(new SubTypesScanner(), new MethodAnnotationsScanner()));
        Set<Method> annotated = reflections.getMethodsAnnotatedWith(org.qubership.atp.adapter.keyworddriven.routing.annotation.Route.class);
        annotated.addAll(reflections.getMethodsAnnotatedWith(RouteAlias.class));
        CollectionUtils.filter(annotated, (arg) -> {
            return Modifier.isPublic(arg.getModifiers());
        });
        registerMethods(annotated.toArray(new Method[0]));
        log.debug("[END] Register routes from package: {}", actionsPackage);
    }

    public static void registerAnnotatedRoutes(Class<?> actionClass) {
        log.debug("[START] Register routes from class: {}", actionClass);
        registerMethods(actionClass.getMethods());
        log.debug("[END] Register routes from class: {}", actionClass);
    }

    private static void registerMethods(Method... methods) {
        for (Method method : methods) {
            registerRouteAnnotation(method);
            registerRouteAliasAnnotation(method);
        }
    }

    private static void registerRouteAnnotation(Method method) {
        org.qubership.atp.adapter.keyworddriven.routing.annotation.Route annotation = method.getAnnotation(org.qubership.atp.adapter.keyworddriven.routing.annotation.Route.class);
        register(method, getGroupName(method), annotation, null);
    }

    private static void registerRouteAliasAnnotation(Method method) {
        RouteAlias routeAlias = method.getAnnotation(RouteAlias.class);
        if (routeAlias != null) {
            String groupName = getGroupName(method);
            for (org.qubership.atp.adapter.keyworddriven.routing.annotation.Route annotation : routeAlias.value()) {
                register(method, groupName, annotation, routeAlias.description());
            }
        }

    }

    private static void register(Method method, String groupName, org.qubership.atp.adapter.keyworddriven.routing.annotation.Route annotation, String description) {
        if (annotation != null) {
            description = StringUtils.isEmpty(annotation.description()) ? description : annotation.description();
            registerLast(groupName, new MethodKeywordRoute(method.isAnnotationPresent(Deprecated.class), description, annotation.value(), method.getDeclaringClass(), method.getName(), method.getParameterTypes(), null));
        }

    }

    private static String getGroupName(Method method) {
        org.qubership.atp.adapter.keyworddriven.routing.annotation.RouteGroup groupAnnotation =
                method.getDeclaringClass()
                        .getAnnotation(org.qubership.atp.adapter.keyworddriven.routing.annotation.RouteGroup.class);
        return groupAnnotation == null ? null : groupAnnotation.value();
    }
}

