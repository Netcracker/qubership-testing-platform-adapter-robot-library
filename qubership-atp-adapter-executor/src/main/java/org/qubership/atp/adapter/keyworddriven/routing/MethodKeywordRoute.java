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

import org.qubership.atp.adapter.keyworddriven.databinder.Calculators;
import org.qubership.atp.adapter.keyworddriven.handlers.ActionMethodExecutor;
import org.qubership.atp.adapter.keyworddriven.handlers.DefaultActionExecutor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MethodKeywordRoute extends Route {
    private static final Log log = LogFactory.getLog(MethodKeywordRoute.class);

    public MethodKeywordRoute(String[] keywordMask, Class<?> actionClass, String methodName, Class<?>... signature) {
        this((String)null, keywordMask, (Class)null, actionClass, methodName, signature, (Object[])null);
    }

    public MethodKeywordRoute(String description, String[] keywordMask, Class<?> actionClass, String methodName, Class<?>... signature) {
        this(description, keywordMask, (Class)null, actionClass, methodName, signature, (Object[])null);
    }

    public MethodKeywordRoute(String[] keywordMask, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        this((String)null, keywordMask, (Class)null, actionClass, methodName, signature, defaultValues);
    }

    public MethodKeywordRoute(String description, String[] keywordMask, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        this(description, keywordMask, (Class)null, actionClass, methodName, signature, defaultValues);
    }

    public MethodKeywordRoute(boolean isDeprecated, String description, String[] keywordMask, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        this(isDeprecated, description, keywordMask, (Class)null, actionClass, methodName, signature, defaultValues);
    }

    public MethodKeywordRoute(String[] keywordMask, Class<? extends ActionMethodExecutor> handler, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        this((String)null, keywordMask, handler, actionClass, methodName, signature, defaultValues);
    }

    public MethodKeywordRoute(String[] keywordMask, Class<? extends ActionMethodExecutor> handler, Class<?> actionClass, String methodName, Class<?>[] signature) {
        this((String)null, keywordMask, handler, actionClass, methodName, signature, (Object[])null);
    }

    public MethodKeywordRoute(String description, String[] keywordMask, Class<? extends ActionMethodExecutor> handler, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        this(false, (String)null, keywordMask, handler, actionClass, methodName, signature, (Object[])null);
    }

    public MethodKeywordRoute(boolean deprecated, String description, String[] keywordMask, Class<? extends ActionMethodExecutor> handler, Class<?> actionClass, String methodName, Class<?>[] signature, Object[] defaultValues) {
        super(description, Route.parseMaskInRouteItems(keywordMask), handler == null ? DefaultActionExecutor.class : handler, deprecated);

        try {
            Method method = getMethod(actionClass, methodName, signature);
            ((ActionMethodExecutor)this.getExecutor()).setMethod(method);
            ArrayList<RouteItem> defaultValuesParsed = this.parseDefaultValues(defaultValues, signature);
            ((ActionMethodExecutor)this.getExecutor()).setDefaultValues(defaultValuesParsed);
            this.checkCalcs(method, defaultValuesParsed);
        } catch (SecurityException var11) {
            throw new IllegalArgumentException(String.format("There is no access to method '%s.%s(%s)'. Route: %s", actionClass.getCanonicalName(), methodName, toString(signature), this.toString()));
        } catch (NoSuchMethodException var12) {
            throw new IllegalArgumentException(String.format("Method '%s.%s(%s)' does not exist. Route: %s", actionClass.getCanonicalName(), methodName, toString(signature), this.toString()));
        }
    }

    protected void checkCalcs(Method method, ArrayList<RouteItem> defaultValues) {
        if (method != null) {
            Type[] signature = method.getGenericParameterTypes();

            for(int i = 0; i < signature.length; ++i) {
                if ((defaultValues == null || ((RouteItem)defaultValues.get(i)).isParameter()) && Calculators.getCalculator(signature[i]) == null && Calculators.getCalculator(method.getParameterTypes()[i]) == null) {
                    log.error("Can't find calculator for param type = " + signature[i] + " Route:" + this);
                }
            }

        }
    }

    protected static ActionMethodExecutor getExecutor(Class<? extends ActionMethodExecutor> clazz, Method method, Collection<RouteItem> defaultValues) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ActionMethodExecutor res = (ActionMethodExecutor)clazz.newInstance();
        res.setMethod(method);
        res.setDefaultValues(defaultValues);
        return res;
    }

    protected ArrayList<RouteItem> parseDefaultValues(Object[] defaultValues2, Class<?>[] signature) throws IllegalArgumentException {
        if (defaultValues2 == null) {
            return null;
        } else {
            ArrayList<RouteItem> res = Route.parseMaskInRouteItems(defaultValues2);
            if (res.size() != signature.length) {
                throw new IllegalArgumentException("Default values array should have the same size as signature of method. Route:" + this);
            } else {
                return res;
            }
        }
    }

    protected static Method getMethod(Class<?> clazz, String methodName, Class<?>[] signature) throws NoSuchMethodException {
        return clazz.getMethod(methodName, signature);
    }

    private static String toString(Class<?>... classes) {
        StringBuilder sb = new StringBuilder();
        Class[] var2 = classes;
        int var3 = classes.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Class clazz = var2[var4];
            sb.append(clazz.getCanonicalName()).append(", ");
        }

        if (sb.length() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb.toString();
    }

    public String toString() {
        return super.toString() + "{javaMethod=" + ((ActionMethodExecutor)this.getExecutor()).getMethod() + '}';
    }
}

