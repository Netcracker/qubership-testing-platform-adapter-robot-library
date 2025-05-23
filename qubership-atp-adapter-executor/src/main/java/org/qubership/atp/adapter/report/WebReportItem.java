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

package org.qubership.atp.adapter.report;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.Utils;

public abstract class WebReportItem implements Serializable {
    public static Boolean printReportThrowableMessageShort = Config.getBoolean("report.throwable.message.short");

    public WebReportItem() {
    }

    public static Message message(String title, Level level, String message, Throwable throwable, SourceProvider page) {
        return message(title, level, message, (LinkedHashMap)null, throwable, page);
    }

    public static Message message(String title, Level level, String message, LinkedHashMap<Object, Object> addValues, Throwable throwable, SourceProvider page) {
        return new Message(title, level, message, addValues, throwable, page);
    }

    public static OpenLog openLog(String logName, String description) {
        return new OpenLog(logName, description);
    }

    public static OpenSection openSection(String title, String message, SourceProvider page) {
        return openSection(title, message, (LinkedHashMap)null, page);
    }

    public static OpenSection openSection(String title, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
        return new OpenSection(title, message, addValues, page);
    }

    public static CloseSection closeSection() {
        return new CloseSection();
    }

    public static CloseLog closeLog() {
        return new CloseLog();
    }

    public String getNewConvertedFieldValue(String value) {
        try {
            if (value == null) {
                return "";
            } else {
                String changedValue = value.replace("<", "&#60;");
                String regFull = "&#60;(table>|a>|tr>|p>|td>|pre|th>|span>|style>|tbody>|br|a\\s|th\\s|table\\s|div\\s|td\\s|tr\\s|span\\s|style\\s|tbody\\s|p\\s>|/table|/tr|/td|/pre|/th|/a|/div|/span|/style|/tbody|/p)";
                Pattern pattern = Pattern.compile(regFull);
                Matcher matcher = pattern.matcher(changedValue);
                StringBuffer stringBuffer = new StringBuffer();

                while(matcher.find()) {
                    matcher.appendReplacement(stringBuffer, "<");
                    stringBuffer.append(matcher.group().substring(5));
                }

                matcher.appendTail(stringBuffer);
                return stringBuffer.toString();
            }
        } catch (Exception var7) {
            return value;
        }
    }

    public String getNewFieldValue(String value) {
        return value == null ? "" : value.replace("<", " < ").replace(">", " > ");
    }

    public abstract void message(WebReportWriterWraper var1);

    public static class CloseLog extends WebReportItem {
        public CloseLog() {
        }

        public void message(WebReportWriterWraper webReportWriter) {
        }
    }

    public static class CloseSection extends WebReportItem {
        public CloseSection() {
        }

        public void message(WebReportWriterWraper webReportWriter) {
            webReportWriter.getWebReportWriter().closeSection();
        }
    }

    public static class OpenSection extends WebReportItem {
        private String title;
        private String message;
        private LinkedHashMap<Object, Object> addValues;
        private SourceProvider page;

        public OpenSection(String title, String message, SourceProvider page) {
            this.title = this.getNewFieldValue(title);
            this.message = this.getNewConvertedFieldValue(message);
            this.addValues = new LinkedHashMap();
            this.page = page;
        }

        public OpenSection(String title, String message, LinkedHashMap<Object, Object> addValues, SourceProvider page) {
            this.title = this.getNewFieldValue(title == null ? null : title.replace("</div>", "&lt;/div&gt;"));
            this.message = this.getNewConvertedFieldValue(message);
            this.addValues = addValues == null ? new LinkedHashMap() : addValues;
            this.page = page;
        }

        public String getTitle() {
            return this.title;
        }

        public String getMessage() {
            return this.message;
        }

        public LinkedHashMap<Object, Object> getAddValues() {
            return this.addValues;
        }

        public SourceProvider getPage() {
            return this.page;
        }

        public void message(WebReportWriterWraper webReportWriter) {
            webReportWriter.getWebReportWriter().openSection(this.getTitle(), this.getMessage(), this.getPage(), this.getAddValues());
        }
    }

    public static class OpenLog extends WebReportItem {
        private String logName;
        private String description;

        public OpenLog(String logName, String description) {
            this.logName = this.getNewFieldValue(logName);
            this.description = this.getNewConvertedFieldValue(description);
        }

        public String getLogName() {
            return this.logName;
        }

        public String getDescription() {
            return this.description;
        }

        public void message(WebReportWriterWraper webReportWriter) {
            webReportWriter.getWebReportWriter().openLog(this.getLogName(), this.getDescription());
        }
    }

    public static class Message extends WebReportItem {
        private String title;
        private Level level;
        private String message;
        private SourceProvider page;
        private Throwable throwable;
        private LinkedHashMap<Object, Object> addValues;

        public Message(String title, Level level, String message, Throwable throwable, SourceProvider page) {
            this.title = this.getNewFieldValue(title);
            this.level = level;
            this.message = this.getNewConvertedFieldValue(message);
            this.addValues = new LinkedHashMap();
            this.page = page;
            this.throwable = throwable;
        }

        public Message(String title, Level level, String message, LinkedHashMap<Object, Object> addValues, Throwable throwable, SourceProvider page) {
            this.title = this.getNewFieldValue(title);
            this.level = level;
            this.message = this.getNewConvertedFieldValue(message);
            this.addValues = addValues == null ? new LinkedHashMap() : addValues;
            this.page = page;
            this.throwable = throwable;
        }

        public String getTitle() {
            return this.title;
        }

        public Level getLevel() {
            return this.level;
        }

        public String getMessage() {
            return this.message;
        }

        public LinkedHashMap<Object, Object> getAddValues() {
            return this.addValues;
        }

        public SourceProvider getPage() {
            return this.page;
        }

        public Throwable getThrowable() {
            return this.throwable;
        }

        public Object getAddValue(String key, Object defaultValue) {
            return this.getAddValues().containsKey(key) ? this.getAddValues().get(key) : defaultValue;
        }

        public void message(WebReportWriterWraper webReportWriter) {
            String message = this.message;
            if (this.throwable != null) {
                message = message + "<pre>";
                if (printReportThrowableMessageShort && this.throwable.getMessage() != null) {
                    message = message + this.throwable.getMessage();
                } else {
                    message = message + Utils.getStackTrace(this.throwable);
                }

                message = message + "</pre>";
            }

            webReportWriter.getWebReportWriter().message(this.getTitle(), this.getLevel(), message, this.getPage(), this.getAddValues());
        }
    }
}
