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

package org.qubership.atp.adapter.keyworddriven.configuration;

import org.qubership.atp.adapter.keyworddriven.context.ContextUtils;
import org.qubership.atp.adapter.testcase.Config;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class KdtProperties {
    private static final Logger LOG = Logger.getLogger(KdtProperties.class);
    @OptionalProperty
    public static final String KDT_TEST_CASE_FILTER_BY_NAME = "kdt.test.cases";
    public static final String[] KDT_TEST_CASES_NAMES = StringUtils.isEmpty(Config.getString("kdt.test.cases")) ? null : Config.getStringArray("kdt.test.cases");
    @OptionalProperty
    public static final String KDT_SERVER_VERSION_LOCATION_PROPERTY = "kdt.server.version.location";
    public static final String KDT_SERVER_VERSION_LOCATION;
    @OptionalProperty
    public static final String KDT_UI_ELEMENTS_PARSER_TYPE_PROPERTY = "kdt.ui.elements.parser.type";
    public static final String KDT_UI_ELEMENTS_PARSER_TYPE;
    @OptionalProperty
    public static final String KDT_REPORT_FOLDERS_WITH_NAMES_PROPERTY = "kdt.report.folders.with.names";
    public static final boolean KDT_REPORT_FOLDERS_WITH_NAMES;
    @OptionalProperty
    public static final String KDT_RESOURCE_RELEASE_AFTER_TESTCASE_PROPERTY = "kdt.release.resources.after.testcase";
    public static final boolean KDT_RESOURCE_RELEASE_AFTER_TESTCASE;
    @OptionalProperty
    public static final String KDT_FIX_BROWSER_STATE_BEFORE_EACH_TEST_PROPERTY = "kdt.fix.browser.state.before.test";
    public static final boolean KDT_FIX_BROWSER_STATE_BEFORE_EACH_TEST;
    @OptionalProperty
    public static final String KDT_CONTEXT_TYPE_PROPERTY = "kdt.context.type.new";
    public static final boolean KDT_CONTEXT_TYPE_IS_NEW;
    @OptionalProperty
    public static final String KDT_ROUTES_MATCHING_STRATEGY_PROPERTY = "kdt.routes.matching.strategy";
    public static final boolean KDT_ROUTES_MATCHING_STRATEGY_LAZY;
    @OptionalProperty
    public static final String REPLACE_PARAMETERS_ON_READ_PROPERTY = "kdt.replace.parameters.on.read";
    public static final boolean REPLACE_PARAMETERS_ON_READ;
    @OptionalProperty
    public static final String KDT_WARNINGS_TO_REPORT_PROPERTY = "kdt.print.warnings.to.report";
    public static final boolean KDT_WARNINGS_TO_REPORT;
    @OptionalProperty
    public static final String KDT_HIGHLIGHT_UI_ELEMENT = "kdt.highlight.calculated.element";
    @OptionalProperty
    public static final String KDT_PARAMETERS_BINDER = "kdt.parameters.binder.supports.escaping";

    public static boolean highlightEnabled() {
        return ContextUtils.getBoolean("kdt.highlight.calculated.element", false);
    }

    public static boolean parametersBinderSupportsEscaping() {
        return Config.getBoolean("kdt.parameters.binder.supports.escaping", true);
    }

    private KdtProperties() {
    }

    static {
        LOG.info("KDT test cases to be executed: " + (KDT_TEST_CASES_NAMES == null ? "ALL" : ArrayUtils.toString(KDT_TEST_CASES_NAMES)));
        KDT_SERVER_VERSION_LOCATION = Config.getString("kdt.server.version.location", "/version.txt");
        LOG.info("KDT server version location = " + KDT_SERVER_VERSION_LOCATION);
        KDT_UI_ELEMENTS_PARSER_TYPE = Config.getString("kdt.ui.elements.parser.type", "NEW");
        KDT_REPORT_FOLDERS_WITH_NAMES = Boolean.valueOf(Config.getString("kdt.report.folders.with.names", "false"));
        KDT_RESOURCE_RELEASE_AFTER_TESTCASE = Boolean.valueOf(Config.getString("kdt.release.resources.after.testcase", "false"));
        KDT_FIX_BROWSER_STATE_BEFORE_EACH_TEST = Boolean.valueOf(Config.getString("kdt.fix.browser.state.before.test", "true"));
        KDT_CONTEXT_TYPE_IS_NEW = Boolean.valueOf(Config.getString("kdt.context.type.new", "true"));
        KDT_ROUTES_MATCHING_STRATEGY_LAZY = "LAZY".equalsIgnoreCase(Config.getString("kdt.routes.matching.strategy", "LAZY"));
        KDT_WARNINGS_TO_REPORT = Boolean.valueOf(Config.getString("kdt.print.warnings.to.report", "true"));
        REPLACE_PARAMETERS_ON_READ = Boolean.valueOf(Config.getString("kdt.replace.parameters.on.read", "true"));
    }
}

