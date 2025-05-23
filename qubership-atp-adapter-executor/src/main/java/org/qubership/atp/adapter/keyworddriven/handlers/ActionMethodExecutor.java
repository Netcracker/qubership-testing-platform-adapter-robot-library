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

package org.qubership.atp.adapter.keyworddriven.handlers;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.qubership.atp.adapter.keyworddriven.ActionExecutionException;
import org.qubership.atp.adapter.keyworddriven.ActionsFactory;
import org.qubership.atp.adapter.keyworddriven.ParametersHandlerException;
import org.qubership.atp.adapter.keyworddriven.databinder.Calculators;
import org.qubership.atp.adapter.keyworddriven.databinder.DataBinder;
import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import org.qubership.atp.adapter.keyworddriven.executable.KeywordParameter;
import org.qubership.atp.adapter.keyworddriven.routing.RouteItem;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public abstract class ActionMethodExecutor implements ActionExecutor {
    private static final Logger log = Logger.getLogger(ActionMethodExecutor.class);
    private Method method;
    private Collection<RouteItem> defaultValues;

    public ActionMethodExecutor() {
    }

    public void execute(Keyword keyword) throws Exception {
        Object[] arglist = this.getActionArguments(keyword);
        Object action;
        if (Modifier.isStatic(this.method.getModifiers())) {
            action = null;
        } else {
            action = ActionsFactory.getAction(this.method.getDeclaringClass());
        }

        if (log.isDebugEnabled()) {
            log.debug("Execute keyword: '" + keyword + "' using method: " + this.toShortString(this.method));
        }

        this.execute(action, this.method, arglist);
    }

    private String toShortString(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName()).append('.').append(method.getName()).append("(").append(StringUtils.join(Collections2.transform(Arrays.asList(method.getParameterTypes()), new Function<Class<?>, String>() {
            @Nullable
            public String apply(@Nullable Class<?> aClass) {
                return aClass.getSimpleName();
            }
        }), ",")).append(")");
        return sb.toString();
    }

    protected abstract void execute(Object var1, Method var2, Object[] var3) throws ActionExecutionException, Exception;

    protected Object[] getActionArguments(Keyword keyword) throws ParametersHandlerException {
        LinkedHashMap<String, KeywordParameter> keywordParameters = keyword.getKeywordParameters();
        Type[] signature = this.method.getGenericParameterTypes();
        Object[] arglist = new Object[signature.length];
        int numberParam = 0;

        for(int i = 0; i < signature.length; ++i) {
            Type paramType = signature[i];
            if (Calculators.getCalculator((Type)paramType) == null) {
                paramType = this.method.getParameterTypes()[i];
            }

            Object param;
            if (this.defaultValues != null && this.defaultValues.size() != 0) {
                param = getParam(keywordParameters, this.defaultValues, (Type)paramType, numberParam);
            } else {
                param = getParam(keywordParameters, (Type)paramType, numberParam);
            }

            arglist[numberParam] = param;
            ++numberParam;
        }

        return arglist;
    }

    protected static Object getParam(LinkedHashMap<String, KeywordParameter> routeParameters, Collection<RouteItem> defaultValues, Type paramType, int numberParam) throws ParametersHandlerException {
        RouteItem item = ((RouteItem[])defaultValues.toArray(new RouteItem[defaultValues.size()]))[numberParam];
        if (item.isParameter()) {
            KeywordParameter par = (KeywordParameter)routeParameters.get(item.getParamName());
            return getCalc(paramType).calculate(par);
        } else {
            return item.getSource();
        }
    }

    protected static Object getParam(LinkedHashMap<String, KeywordParameter> keywordParameters, Type paramType, int numberParam) throws ParametersHandlerException {
        KeywordParameter par = ((KeywordParameter[])keywordParameters.values().toArray(new KeywordParameter[0]))[numberParam];
        return getCalc(paramType).calculate(par);
    }

    public static DataBinder<?> getCalc(Type paramType) throws ParametersHandlerException {
        DataBinder<?> parametersHandler = Calculators.getCalculator(paramType);
        if (parametersHandler == null) {
            throw new ParametersHandlerException("Can't find calculator for param type = " + paramType);
        } else {
            return parametersHandler;
        }
    }

    public Method getMethod() {
        return this.method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Collection<RouteItem> getDefaultValues() {
        return this.defaultValues;
    }

    public void setDefaultValues(Collection<RouteItem> defaultValues) {
        this.defaultValues = defaultValues;
    }
}

