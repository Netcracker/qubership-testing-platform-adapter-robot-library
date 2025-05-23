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

package org.qubership.atp.adapter.keyworddriven.executor;

import org.qubership.atp.adapter.keyworddriven.TestCaseException;
import org.qubership.atp.adapter.keyworddriven.basicformat.ValidationLevel;
import org.qubership.atp.adapter.keyworddriven.dataitem.Processor;
import org.qubership.atp.adapter.keyworddriven.dataitem.ProcessorStorage;
import org.qubership.atp.adapter.keyworddriven.executable.DataItem;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import org.qubership.atp.adapter.keyworddriven.handlers.PreparedActionExecutor;
import org.qubership.atp.adapter.keyworddriven.routing.KeywordRouteTable;
import org.qubership.atp.adapter.keyworddriven.routing.Route;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.KDTUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class KeywordExecutor extends SectionExecutor {
    private static final ThreadLocal<Keyword> currentKeyword = new ThreadLocal();
    public static final boolean VALIDATE_SCENARIO = Boolean.parseBoolean(Config.getString("kdt.validate.scenarios"));
    public static final int SEVERITY_LEVEL = Integer.parseInt(Config.getString("kdt.severity.level", "0"));
    public static int validationLevel = ValidationLevel.parseValidationLevel(Config.getString("kdt.validation.level"));

    public KeywordExecutor() {
    }

    public void prepare(Executable executable) {
        Keyword keyword = (Keyword)executable;
        Route route = keyword.getRoute() == null ? KeywordRouteTable.searchRoute(keyword) : keyword.getRoute();
        if (route == null) {
            Report.getReport().warn("No routes found for keyword: " + keyword);
            if (VALIDATE_SCENARIO && !this.skip(keyword)) {
                KDTUtils.criticalMessAndExit(String.format("No routes found for keyword %s\nExecution is interrupted", keyword));
            }

        } else {
            KeywordRouteTable.routeToDataAssign(keyword, route);
            if (keyword.getRoute().getActionExecutor() instanceof PreparedActionExecutor) {
                ((PreparedActionExecutor)keyword.getRoute().getActionExecutor()).prepare(keyword);
            }

            super.prepare(keyword);
        }
    }

    public void execute(Executable executable) throws Exception {
        Keyword keyword = (Keyword)executable;
        keyword.log().info("Keyword : " + keyword);
        keyword.log().debug("Route : " + keyword.getRoute());
        currentKeyword.set(keyword);
        if (!this.skip(executable)) {
            this.execute(keyword);
        }
    }

    protected boolean skip(Executable executable) {
        Keyword keyword = (Keyword)executable;
        boolean skip = keyword.getValidationLevel() > validationLevel;
        if (skip && keyword.log().isDebugEnabled()) {
            keyword.log().info(String.format("Keyword '%s' was skipped because validation level of it is bigger than execution level ( %s > %s )", keyword, keyword.getValidationLevel(), validationLevel));
        }

        return skip;
    }

    protected void execute(Keyword keyword) throws Exception {
        this.processDataItems(keyword);
        replaceParametersInKeyword(keyword);
        KDTUtils.replaceParametersInDescription(keyword);
        if (keyword.getRoute() == null) {
            routeNotFound(keyword);
        } else {
            try {
                keyword.getRoute().getActionExecutor().execute(keyword);
            } catch (InvocationTargetException var3) {
                InvocationTargetException e = var3;
                this.throwTCE(keyword, e.getTargetException());
            } catch (Exception e) {
                Report.getReport().error((String)null, "Error during keyword execution: " + keyword + ":" + e.getMessage(), new KDTUtils.StringSource(ExceptionUtils.getStackTrace(e)));
                this.throwTCE(keyword, e);
            }

            if (keyword.getChildren().size() > 0) {
                super.execute(keyword);
            }

        }
    }

    protected void processDataItems(Keyword keyword) {
        LinkedList<DataItem> dataItems = keyword.getDataItems();

        for(int i = 0; i < dataItems.size(); ++i) {
            DataItem item = (DataItem)dataItems.get(i);
            Iterator var5 = ProcessorStorage.getStorage().getProcessors().iterator();

            while(var5.hasNext()) {
                Processor dataItemProcessor = (Processor)var5.next();
                dataItems.set(i, dataItemProcessor.process(item));
            }
        }

    }

    protected void throwTCE(Keyword keyword, Throwable e) throws TestCaseException {
        Route route = keyword.getRoute();
        TestCaseException throwE = org.qubership.atp.adapter.utils.ExceptionUtils.handle(e, "Error occurred during execution keyword: " + keyword + ".\n Route: " + route + ".\n Error: " + e.getMessage());
        Report.getReport().message(throwE);
        if (SEVERITY_LEVEL < keyword.getValidationLevel()) {
            keyword.log().warn("Exception in keyword was wrote in report as warning. Test case should continue");
        } else {
            throw throwE;
        }
    }

    protected static void replaceParametersInKeyword(Keyword keyword) {
        Iterator var1 = keyword.getDataItems().iterator();

        while(var1.hasNext()) {
            DataItem item = (DataItem)var1.next();
            String value = KDTUtils.replaceParametersInString(keyword, item.getData());
            item.setData(value);
        }

    }

    protected static void routeNotFound(Keyword keyword) {
        String KEYWORD_WAS_SKIPPED_DESC = "Keyword was skipped because no matches found for it. Keyword : '%s'.\n<br/>Possible routes:<br/>\n";
        String KEYWORD_WAS_SKIPPED_TITLE = "Keyword '%s' was skipped";
        String title = String.format("Keyword '%s' was skipped", keyword.getName());
        String description = String.format("Keyword was skipped because no matches found for it. Keyword : '%s'.\n<br/>Possible routes:<br/>\n", keyword.getDataItems());

        Route route;
        for(Iterator var5 = KeywordRouteTable.getRoutesByName(keyword.getName()).iterator(); var5.hasNext(); description = description + route.getRouteItems() + "\n<br/>") {
            route = (Route)var5.next();
        }

        Report.getReport().error(title, description);
        if (VALIDATE_SCENARIO) {
            KDTUtils.criticalMessAndExit(description);
        }

    }

    public static Keyword getKeyword() {
        return (Keyword)currentKeyword.get();
    }

    public static void removeInstance() {
        currentKeyword.remove();
    }
}

