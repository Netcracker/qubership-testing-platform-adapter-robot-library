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

package org.qubership.atp.adapter.utils;

import org.qubership.atp.adapter.keyworddriven.basicformat.BasicFormatTestSuiteReader;
import org.qubership.atp.adapter.keyworddriven.basicformat.StringValueSubstitution;
import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.keyworddriven.executable.Keyword;
import org.qubership.atp.adapter.keyworddriven.executable.Section;
import org.qubership.atp.adapter.keyworddriven.executor.KeywordExecutor;
import org.qubership.atp.adapter.report.InterruptScenarioException;
import org.qubership.atp.adapter.report.Report;
import org.qubership.atp.adapter.report.SourceProvider;
import org.qubership.atp.adapter.report.WebReportWriter;
import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.wd.shell.browser.ReportType;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KDTUtils {
    public static final String STORE_TEST_CASE_PARAMETERS_SHEET = "Stored Parameters";
    private static final Log log = LogFactory.getLog(KDTUtils.class);
    public static final String HTML_STYLES = "<style type='text/css'>\r\ntable.colored {border: 0px; } table.colored th {background-color: #CCCCCC; } table.colored td {white-space: pre-wrap; } table.colored .errorBorder { border-color: coral;} table.colored .errorBackGround { background-color: #F78181;} table.colored .warnBorder { border-color: #FFFF00;} table.colored .warnBackGround { background-color: #FFFF00;} table.colored .successBorder { border-color: #81F781;} table.colored .successBackGround { background-color: #81F781;} table.colored .normal { border-width: 0px;} </style> ";
    private static final boolean PRINT_PARAM_PAGE = Boolean.parseBoolean(Config.getString("kdt.print.parampages", "false"));

    public KDTUtils() {
    }

    public static String prepareFilePath(String file) {
        String FILE_SEPARATOR = System.getProperty("file.separator");
        if (!FILE_SEPARATOR.equals("\\")) {
            file = file.replaceAll("\\\\", FILE_SEPARATOR);
        }

        return file;
    }

    public static void criticalMessAndExit(String errorMess) {
        InterruptScenarioException e = new InterruptScenarioException(errorMess);
        Logger.getLogger("KDT").fatal(e);
        throw e;
    }

    public static void errorMess(String errorMess) {
        Logger.getLogger("KDT").error(Utils.getStackTrace(new Exception(errorMess)));
    }

    public static File checkCriticalFileExistAndExit(File file) {
        if (!file.exists()) {
            criticalMessAndExit("File '" + file.getPath() + "' not found.\n" + Arrays.toString(Thread.currentThread().getStackTrace()));
        }

        return file;
    }

    public static File checkCriticalFileExistAndExit(String path, String filePath) {
        return path != null ? checkCriticalFileExistAndExit(new File(path, prepareFilePath(filePath))) : checkCriticalFileExistAndExit(new File(prepareFilePath(filePath)));
    }

    public static File checkCriticalFileExistAndExit(String filePath) {
        return checkCriticalFileExistAndExit(new File(prepareFilePath(filePath)));
    }

    public static boolean setStringsByPrefix(String prefix) {
        Map<String, String> configMap = Config.getStringsByPrefix(prefix);
        if (configMap.size() <= 0) {
            return false;
        } else {
            Iterator var3 = configMap.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry)var3.next();
                String key = ((String)entry.getKey()).replaceFirst(prefix + ".", "");
                Config.setString(key, (String)entry.getValue());
            }

            return true;
        }
    }

    public static File getWebReportDir() {
        return WebReportWriter.getWebReportWriter().getReportDir();
    }

    public static String getFileNameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf(BasicFormatTestSuiteReader.FILE_SEPARATOR) + 1, filePath.length());
    }

    public static String getRepTitle() {
        return KeywordExecutor.getKeyword() != null ? getRepTitle(KeywordExecutor.getKeyword()) : null;
    }

    public static String getRepTitle(Keyword keyword) {
        if (keyword != null) {
            return StringUtils.isEmpty(keyword.getDescription()) ? keyword.getName() : keyword.getDescription() + " (" + keyword.getName() + ")";
        } else {
            return null;
        }
    }

    public static String getRepDesc() {
        return getRepDesc(KeywordExecutor.getKeyword());
    }

    public static String getRepDesc(Keyword keyword) {
        return keyword != null ? keyword.toHtml() : "";
    }

    public static String getRepDesc(String additionalInfo) {
        return getRepDesc() + (additionalInfo == null ? "" : "" + additionalInfo);
    }

    public static String getRepDesc(Keyword e, String additionalInfo) {
        return getRepDesc(e) + (additionalInfo == null ? "" : "" + additionalInfo);
    }

    public static Properties getAllConfigContent() {
        try {
            Field config = Config.class.getDeclaredField("config");
            config.setAccessible(true);
            return (Properties)config.get((Object)null);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException var1) {
            Exception e = var1;
            log.error(KDTUtils.class.toString(), e);
            return null;
        }
    }

    public static String htmlCell(String cellValue, HtmlClass clazz) {
        return "<td class='" + clazz + "'>" + cellValue + "</td>";
    }

    public static String htmlRow(boolean success, Object... cellValues) {
        StringBuilder buf = new StringBuilder("<tr>");
        Object[] var3 = cellValues;
        int var4 = cellValues.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Object value = var3[var5];
            buf.append("<td").append(success ? ">" : " class='errorBackGround'>").append(value).append("</td>");
        }

        buf.append("</tr>");
        return buf.toString();
    }

    public static String htmlRow(HtmlClass clazz, Object... cellValues) {
        StringBuilder buf = (new StringBuilder("<tr class='")).append(clazz).append("'>");
        Object[] var3 = cellValues;
        int var4 = cellValues.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Object value = var3[var5];
            buf.append("<td class='").append(clazz).append("'>").append(value).append("</td>");
        }

        buf.append("</tr>");
        return buf.toString();
    }

    public static String htmlRow(Object... cellValues) {
        return htmlRow(true, cellValues);
    }

    public static String htmlTable(String... content) {
        return htmlTable(KDTUtils.HtmlClass.normal, content);
    }

    public static String htmlTable(HtmlClass clazz, String... content) {
        StringBuilder buf = (new StringBuilder("<style type='text/css'>\r\ntable.colored {border: 0px; } table.colored th {background-color: #CCCCCC; } table.colored td {white-space: pre-wrap; } table.colored .errorBorder { border-color: coral;} table.colored .errorBackGround { background-color: #F78181;} table.colored .warnBorder { border-color: #FFFF00;} table.colored .warnBackGround { background-color: #FFFF00;} table.colored .successBorder { border-color: #81F781;} table.colored .successBackGround { background-color: #81F781;} table.colored .normal { border-width: 0px;} </style> ")).append("<table class='").append(clazz).append("'><tbody>");
        String[] var3 = content;
        int var4 = content.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String row = var3[var5];
            buf.append(row);
        }

        buf.append("</tbody></table>");
        return buf.toString();
    }

    public static String htmlLink(File f) {
        return "<a href =\"" + getHref(f) + "\" target=\"_blank\">" + f.getName() + "</a>";
    }

    public static String htmlBold(String string) {
        return "<span style=\"font-weight: bold;\">" + string + "</span>";
    }

    public static String getHrefFromTypeId(BigInteger typeId) {
        return typeId == null ? "" : "/admintool.main.nc?typeId=" + typeId;
    }

    /** @deprecated */
    @Deprecated
    public static String getHref(File f) {
        String host = getHostname();
        if (!host.equals("UNKNOWN-HOST")) {
            return "file://" + f.getAbsolutePath();
        } else {
            try {
                String relativePath = getRelativePath(f.getAbsolutePath(),
                        (new File("../../../QubershipWebApp/")).getAbsolutePath(), File.separator);
                return Config.getString("server.url") + File.separator + relativePath;
            } catch (Throwable var3) {
                Throwable e = var3;
                log.error("Can't get relative link to file " + f.getAbsolutePath(), e);
                return "";
            }
        }
    }

    public static String getHostname() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.trim().equals("")) {
            return hostname;
        } else {
            hostname = System.getenv("COMPUTERNAME");
            return hostname != null && !hostname.trim().equals("") ? hostname : "UNKNOWN-HOST";
        }
    }

    public static String getRelativePath(String targetPath, String basePath, String pathSeparator) {
        String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
        String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);
        switch (pathSeparator) {
            case "/":
                normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
                normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);
                break;
            case "\\":
                normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
                normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);
                break;
            default:
                throw new RuntimeException("Unrecognised dir separator '" + pathSeparator + "'");
        }

        String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));
        StringBuilder common = new StringBuilder();

        int commonIndex;
        for(commonIndex = 0; commonIndex < target.length && commonIndex < base.length && target[commonIndex].equals(base[commonIndex]); ++commonIndex) {
            common.append(target[commonIndex]).append(pathSeparator);
        }

        if (commonIndex == 0) {
            throw new RuntimeException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath + "'");
        } else {
            boolean baseIsFile = true;
            File baseResource = new File(normalizedBasePath);
            if (baseResource.exists()) {
                baseIsFile = baseResource.isFile();
            } else if (basePath.endsWith(pathSeparator)) {
                baseIsFile = false;
            }

            StringBuilder relative = new StringBuilder();
            if (base.length != commonIndex) {
                int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

                for(int i = 0; i < numDirsUp; ++i) {
                    relative.append("..").append(pathSeparator);
                }
            }

            relative.append(normalizedTargetPath.substring(common.length()));
            return relative.toString();
        }
    }

    public static String getHrefFromId(BigInteger objectID) {
        return getHrefFromId(objectID, "");
    }

    public static String getHrefFromId(BigInteger id, String tab) {
        tab = tab.trim().replace(" ", "+");
        String link = id == null ? "#" : "/ncobject.jsp?id=" + id;
        if (tab.length() != 0) {
            link = link + "&tab=" + (tab.charAt(0) != '_' ? "_" : "") + tab;
        }

        return link;
    }

    public static String htmlLink(String href, String linkName) {
        String color = "#6666FF";
        if (linkName == null || linkName.length() == 0) {
            linkName = "N/A";
            color = "#CC3333";
        }

        return "<a style=\"color: " + color + "; text-decoration: underline; font-weight: bold;\"target=\"_blank\" href=\"" + href + "\">" + linkName + "</a>";
    }

    public static SourceProvider getPropertiesPage() {
        return getPropertiesPage(KeywordExecutor.getKeyword(), false);
    }

    public static SourceProvider getPropertiesPage(Executable section) {
        return getPropertiesPage(section, false);
    }

    public static SourceProvider getPropertiesPage(Executable section, boolean showFinalOnly) {
        if (PRINT_PARAM_PAGE) {
            if (showFinalOnly) {
                return new StringSource(mapToTable(getFinalPropTable(section)));
            } else {
                StringBuilder source = new StringBuilder();
                source.append("<H1></H1>");

                for(Executable current = section; current != null; current = current.getParent()) {
                    if (current.getNormalPriorityParams().size() > 0) {
                        if (current instanceof Section) {
                            source.append(htmlBold(((Section)current).getFullName()));
                        } else {
                            source.append(htmlBold(current.getName()));
                        }

                        source.append(mapToTable(current.getNormalPriorityParams()));
                    }
                }

                source.append("<H1>Final param table</H1>");
                source.append(mapToTable(getFinalPropTable(section)));
                return new StringSource(source.toString());
            }
        } else {
            return null;
        }
    }

    public static String mapToTable(Map<?, ?> map) {
        StringBuilder buf = new StringBuilder();
        buf.append("<tr>").append("<th>Key</th>").append("<th>Value</th>").append("</tr>");

        Object key;
        String value;
        for(Iterator var2 = map.keySet().iterator(); var2.hasNext(); buf.append(htmlRow(key.toString(), value))) {
            key = var2.next();
            Object objValue = map.get(key);
            if (objValue instanceof Map) {
                value = mapToTable((Map)objValue);
            } else {
                value = String.valueOf(objValue).replaceAll(" ", "&nbsp;");
            }
        }

        return htmlTable(KDTUtils.HtmlClass.colored, buf.toString());
    }

    public static String listToTable(List<String[]> list) {
        StringBuilder buf = new StringBuilder();
        Iterator var2 = list.iterator();

        while(var2.hasNext()) {
            String[] row = (String[])var2.next();
            buf.append("<tr>");
            String[] var4 = row;
            int var5 = row.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String cell = var4[var6];
                buf.append(htmlCell(StringEscapeUtils.escapeHtml4(cell), KDTUtils.HtmlClass.normal));
            }

            buf.append("</tr>");
        }

        return htmlTable(KDTUtils.HtmlClass.colored, buf.toString());
    }

    public static Map<String, Object> getFinalPropTable(Executable section) {
        return getFinalPropTableNormal(section);
    }

    private static Map<String, Object> getFinalPropTableNormal(Executable section) {
        Map<String, Object> finalPropTable = section.getParent() == null ? new HashMap() : getFinalPropTableNormal(section.getParent());
        ((Map)finalPropTable).putAll(section.getNormalPriorityParams());
        return (Map)finalPropTable;
    }

    public static <T extends Section> void replaceParametersInDescription(T section) {
        section.setDescription(replaceParametersInString(section, section.getDescription()));
        section.setName(replaceParametersInString(section, section.getName()));
    }

    public static String replaceParametersInString(Executable section, String data) {
        return replaceParametersInString(section, data, Report.getReport());
    }

    public static String replaceParametersInString(Executable section, String data, Report report) {
        return StringValueSubstitution.replaceParametersInString(section, data, report);
    }

    public static SourceProvider getSourceProviderFromException(Throwable t) {
        SourceProvider result = null;

        for(Throwable cause = t; cause != null && result == null; cause = cause.getCause()) {
            result = getSourceProviderFromObject(cause);
        }

        return result;
    }

    public static SourceProvider getSourceProviderFromObject(Object object) {
        if (object != null) {
            Method[] var1 = object.getClass().getDeclaredMethods();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Method method = var1[var3];
                if (SourceProvider.class.isAssignableFrom(method.getReturnType())) {
                    try {
                        return (SourceProvider)method.invoke(object);
                    } catch (Throwable var6) {
                    }
                }
            }
        }

        return null;
    }

    public static void loadConfigParams(Executable executable) {
        Properties p = getAllConfigContent();
        if (p != null) {
            Iterator var2 = p.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<Object, Object> prop = (Map.Entry)var2.next();
                String key = String.valueOf(prop.getKey());
                String value = String.valueOf(prop.getValue());
                executable.setParam(key, value);
                if (log.isTraceEnabled()) {
                    log.trace("Config param set: " + key + "=" + value);
                }
            }
        }

    }

    public static String prettifyToHtml(String source) {
        return String.format("<script src=\"https://cdn.rawgit.com/google/code-prettify/main/loader/run_prettify.js\">"
                + "</script><pre class=\"prettyprint\">%s</pre>", StringEscapeUtils.escapeHtml4(source));
    }

    public static class StringSource implements SourceProvider {
        private String source;

        public StringSource(String source) {
            this.source = source;
        }

        public String getSource() {
            return this.source;
        }

        public String getExtension() {
            return "html";
        }

        public String getReportType() {
            return ReportType.SNAPSHOT.toString();
        }

        public void setReportType(String s) {
        }
    }

    public static enum HtmlClass {
        errorBorder,
        errorBackGround,
        warnBorder,
        warnBackGround,
        successBorder,
        successBackGround,
        colored,
        normal;

        private HtmlClass() {
        }
    }

    public static class HtmlTableBuilder {
        private StringBuilder buf;
        private Level status;

        private HtmlTableBuilder() {
            this.status = Level.INFO;
        }

        public static HtmlTableBuilder newColoredTable() {
            return newTable(KDTUtils.HtmlClass.colored);
        }

        public static HtmlTableBuilder newColoredTable(String additionalCssStyles) {
            return newTable(KDTUtils.HtmlClass.colored, additionalCssStyles);
        }

        public static HtmlTableBuilder newTable(HtmlClass clazz) {
            return newTable(clazz, "");
        }

        public static HtmlTableBuilder newTable(HtmlClass clazz, String additionalCssStyles) {
            HtmlTableBuilder thiz = new HtmlTableBuilder();
            thiz.buf = new StringBuilder("<style type='text/css'>\r\ntable.colored {border: 0px; } table.colored th {background-color: #CCCCCC; } table.colored td {white-space: pre-wrap; } table.colored .errorBorder { border-color: coral;} table.colored .errorBackGround { background-color: #F78181;} table.colored .warnBorder { border-color: #FFFF00;} table.colored .warnBackGround { background-color: #FFFF00;} table.colored .successBorder { border-color: #81F781;} table.colored .successBackGround { background-color: #81F781;} table.colored .normal { border-width: 0px;} </style> ");
            if (StringUtils.isNotEmpty(additionalCssStyles)) {
                thiz.buf.append("<style type='text/css'>").append(additionalCssStyles).append("</style>");
            }

            thiz.buf.append("<table class='").append(clazz).append("'><tbody>");
            return thiz;
        }

        public HtmlTableBuilder addHeaderRow(Object... cellValues) {
            return this.addHeaderRow(true, cellValues);
        }

        public HtmlTableBuilder addHeaderRowAndEscape(Object... cellValues) {
            return this.addHeaderRow(false, cellValues);
        }

        public HtmlTableBuilder addHeaderRow(boolean doNotEscape, Object... cellValues) {
            this.buf.append("<tr>");
            Object[] var3 = cellValues;
            int var4 = cellValues.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Object value = var3[var5];
                this.buf.append("<th>").append(this.escapeValue(value, doNotEscape)).append("</th>");
            }

            this.buf.append("</tr>");
            return this;
        }

        protected String escapeValue(Object value, boolean doNotEscape) {
            return doNotEscape ? String.valueOf(value) : StringEscapeUtils.escapeHtml4(String.valueOf(value));
        }

        public HtmlTableBuilder addRow(HtmlClass clazz, Object... cellValues) {
            return this.addRow(true, clazz, cellValues);
        }

        public HtmlTableBuilder addRowAndEscape(HtmlClass clazz, Object... cellValues) {
            return this.addRow(false, clazz, cellValues);
        }

        public HtmlTableBuilder addRow(boolean doNotEscape, HtmlClass clazz, Object... cellValues) {
            if ((clazz == KDTUtils.HtmlClass.errorBorder || clazz == KDTUtils.HtmlClass.errorBackGround) && Level.ERROR.isGreaterOrEqual(this.status)) {
                this.status = Level.ERROR;
            } else if ((clazz == KDTUtils.HtmlClass.warnBorder || clazz == KDTUtils.HtmlClass.warnBackGround) && Level.WARN.isGreaterOrEqual(this.status)) {
                this.status = Level.WARN;
            }

            this.buf.append("<tr class='").append(clazz).append("'>");
            Object[] var4 = cellValues;
            int var5 = cellValues.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Object value = var4[var6];
                this.buf.append("<td class='").append(clazz).append("'>").append(this.escapeValue(value, doNotEscape)).append("</td>");
            }

            this.buf.append("</tr>");
            return this;
        }

        public String build() {
            return this.buf.append("</tbody></table>").toString();
        }

        public Level getStatus() {
            return this.status;
        }

        /** @deprecated */
        @Deprecated
        public String toString() {
            return this.buf.toString();
        }
    }
}

