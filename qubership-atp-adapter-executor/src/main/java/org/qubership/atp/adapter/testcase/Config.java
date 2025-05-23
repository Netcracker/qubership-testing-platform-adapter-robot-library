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

package org.qubership.atp.adapter.testcase;

import org.qubership.atp.adapter.utils.Utils;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.XMLUnit;

public class Config {
    private static final Log log = LogFactory.getLog(Config.class);
    private static String propertiesFilePath = "test.properties";
    /** @deprecated */
    @Deprecated
    public static final String PROPERTIES_FILE;
    private static final Pattern PARAMETRIZATION_PATTERN;
    private static Properties config;
    private static final DocumentBuilder builder;
    private static final TimeZone serverTimeZone;
    private static String tempDirPathname;

    public Config() {
    }

    public static Properties getConfig() {
        return config;
    }

    public static String getTestPropertiesFilePath() {
        return propertiesFilePath;
    }

    public static void setTestPropertiesFilePath(String _propertiesFilePath) {
        config = readParameterizedPropertiesFile(new File(_propertiesFilePath));
        propertiesFilePath = _propertiesFilePath;
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        return getInt(key, 0);
    }

    public static int getInt(String key, int defaultValue) {
        if (!getString(key).trim().isEmpty()) {
            try {
                return Integer.parseInt(getString(key));
            } catch (NumberFormatException var3) {
                NumberFormatException e = var3;
                log.warn(String.format("'%s' value should be a number. Actual value is '%s'", key, getString(key)), e);
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return !getString(key).trim().isEmpty() ? Boolean.parseBoolean(getString(key)) : defaultValue;
    }

    public static void setString(String key, String value) {
        config.setProperty(key.trim(), value.trim());
    }

    public static Map<String, String> getStringsByPrefix(String prefix) {
        Map<String, String> result = new HashMap();
        Set<Object> keys = config.keySet();
        Iterator i$ = keys.iterator();

        while(true) {
            String key;
            do {
                Object objectKey;
                do {
                    if (!i$.hasNext()) {
                        return result;
                    }

                    objectKey = i$.next();
                } while(!(objectKey instanceof String));

                key = (String)objectKey;
            } while(!key.equals(prefix) && !key.startsWith(prefix + "."));

            result.put(key, getString(key));
        }
    }

    public static String[] getStringArray(String key) {
        return getString(key).split(",");
    }

    public static void setTempDirPathname(String tempDirPathname) {
        Config.tempDirPathname = tempDirPathname;
    }

    public static String getTempDirPathname() {
        return tempDirPathname;
    }

    public static String getConfigDirPathname() {
        return "config";
    }

    public static DocumentBuilder getDocumentBuilder() {
        return builder;
    }

    public static TimeZone getServerTimezone() {
        return serverTimeZone;
    }

    private static Properties readParameterizedPropertiesFile(File propertiesFile) {
        Properties p = new Properties();
        if (!propertiesFile.exists()) {
            log.debug("'" + propertiesFile.getAbsolutePath() + "' file has not been found");
            return p;
        } else {
            BufferedReader lines = null;

            try {
                lines = new BufferedReader(new InputStreamReader(new FileInputStream(propertiesFile), "UTF-8"));

                String line;
                while((line = lines.readLine()) != null) {
                    line = line.trim();
                    if (!line.startsWith("#") && line.length() != 0) {
                        Matcher m = PARAMETRIZATION_PATTERN.matcher(line);
                        StringBuffer resultLine = new StringBuffer();

                        while(m.find()) {
                            String key = m.group(1);
                            String replacement;
                            if (p.containsKey(key)) {
                                replacement = p.getProperty(key);
                            } else {
                                replacement = m.group(0);
                                log.error("Substitution was not made: " + replacement);
                            }

                            replacement = replacement.replace("\\", "\\\\\\\\").replace("$", "\\$");
                            m.appendReplacement(resultLine, replacement);
                        }

                        m.appendTail(resultLine);
                        line = resultLine.toString();
                        String[] keyAndValue = line.split("[=|:]", 2);
                        if (keyAndValue.length != 2) {
                            keyAndValue = line.split("[ |\t|\f]", 2);
                        }

                        if (keyAndValue.length >= 2) {
                            p.put(keyAndValue[0].trim(), keyAndValue[1].trim());
                        }
                    }
                }
            } catch (IOException var11) {
                IOException e = var11;
                log.fatal("Failed to read properties file: " + propertiesFile.getName(), e);
                throw new RuntimeException("IOException", e);
            } finally {
                Utils.close(new Closeable[]{lines});
            }

            return p;
        }
    }

    public static Properties loadSystemProperties() {
        Enumeration<?> properties = System.getProperties().propertyNames();

        while(properties.hasMoreElements()) {
            String element = (String)properties.nextElement();
            config.setProperty(element, System.getProperty(element));
        }

        return config;
    }

    static {
        PROPERTIES_FILE = propertiesFilePath;
        PARAMETRIZATION_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");
        config = readParameterizedPropertiesFile(new File(propertiesFilePath));
        tempDirPathname = "tmp";

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException var2) {
            ParserConfigurationException e = var2;
            log.fatal("ParserConfigurationException", e);
            throw new RuntimeException("ParserConfigurationException", e);
        }

        try {
            Class.forName("org.custommonkey.xmlunit.XMLUnit");
            XMLUnit.setCompareUnmatched(false);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setNormalizeWhitespace(true);
            XMLUnit.setNormalize(true);
        } catch (ClassNotFoundException var1) {
        }

        String timeZoneId = getString("nc.timezone");
        serverTimeZone = timeZoneId.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneId);
    }
}

