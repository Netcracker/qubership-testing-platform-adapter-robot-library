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

package org.qubership.atp.adapter.keyworddriven.actions.view;

import org.qubership.atp.adapter.keyworddriven.executor.KeywordExecutor;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.report.SourceProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

public class ActionMessage {
    private String title;
    private String message;
    private SourceProvider page;
    private Level level;

    private ActionMessage(String title, String message, SourceProvider page, Level level) {
        this.title = title;
        this.message = message;
        this.page = page;
        this.level = level;
    }

    public String getTitle() {
        return this.title;
    }

    public String getMessage() {
        return this.message;
    }

    public SourceProvider getPage() {
        return this.page;
    }

    public Level getLevel() {
        return this.level;
    }

    public static void message(String title, String message, SourceProvider page, Level level) {
        Report.getReport().message(new ActionMessage(title, message, page, level));
    }

    public static void message(String message, SourceProvider page, Level level) {
        String title = "";
        if (KeywordExecutor.getKeyword() != null) {
            title = StringUtils.isBlank(KeywordExecutor.getKeyword().getDescription()) ? KeywordExecutor.getKeyword().getFullName() : KeywordExecutor.getKeyword().getDescription();
        }

        message(title, message, page, level);
    }

    public static void message(String message, SourceProvider page) {
        message(message, page, Level.INFO);
    }

    public static void message(String message) {
        message(message, (SourceProvider)null);
    }

    public static void message(SourceProvider snapshot) {
        message((String)null, snapshot);
    }

    public static void message() {
        message((String)null, (SourceProvider)null);
    }
}

