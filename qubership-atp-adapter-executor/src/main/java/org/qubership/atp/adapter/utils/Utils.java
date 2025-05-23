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

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Utils {
    private static final Logger log = Logger.getLogger(Utils.class);
    protected static final Pattern XML_PROLOG_JUNK_PATTERN = Pattern.compile("^([\\W]+)<");
    private static HtmlCompressor compressor = new HtmlCompressor();

    public Utils() {
    }

    public static String readFileToString(File file) {
        try {
            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException var2) {
            IOException e = var2;
            log.error("Failed to read file: " + file.getAbsolutePath(), e);
            return "";
        }
    }

    public static String readFileToString(String filePath) {
        return readFileToString(new File(filePath));
    }

    public static void writeStringToFile(File file, String data) {
        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException var3) {
            IOException e = var3;
            log.error("Failed to write to file: " + file.getAbsolutePath(), e);
        }

    }

    public static String sanitizeFilename(String filename) {
        return sanitizeFilename(filename, "_");
    }

    public static String sanitizeFilename(String filename, String replacement) {
        return filename.replaceAll("[|:*?\"<>/\\\\]", replacement);
    }

    public static void writeStringToFile(String filePath, String data) {
        writeStringToFile(new File(filePath), data);
    }

    public static String minimizeHTML(String htmlSource) {
        return compressor.compress(htmlSource);
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();

        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty("omit-xml-declaration", "yes");
            t.setOutputProperty("indent", "no");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException var3) {
            System.out.println("nodeToString Transformer Exception");
        }

        return sw.toString();
    }

    public static Document parseXml(String xmlString) {
        Matcher junkMatcher = XML_PROLOG_JUNK_PATTERN.matcher(xmlString.trim());
        xmlString = junkMatcher.replaceFirst("<");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException var7) {
            ParserConfigurationException e = var7;
            log.fatal("ParserConfigurationException", e);
            throw new RuntimeException("ParserConfigurationException", e);
        }

        if (xmlString.length() > 0) {
            try {
                return builder.parse(new InputSource(new StringReader(xmlString)));
            } catch (SAXException var5) {
                SAXException e = var5;
                log.error("Failed to parse XML", e);
            } catch (IOException var6) {
                IOException e = var6;
                log.error("Failed to parse XML", e);
            }
        }

        return builder.newDocument();
    }

    public static Document parseXml(File xmlFile) {
        return parseXml(readFileToString(xmlFile));
    }

    public static String writeXmlToString(Node node) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(node);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerConfigurationException var4) {
            TransformerConfigurationException e = var4;
            log.error("Failed to output XML to String", e);
        } catch (TransformerFactoryConfigurationError var5) {
            TransformerFactoryConfigurationError e = var5;
            log.error("Failed to output XML to String", e);
        } catch (TransformerException var6) {
            TransformerException e = var6;
            log.error("Failed to output XML to String", e);
        }

        return "";
    }

    public static String evaluateXPath(String expression, Node node) {
        try {
            return XPathFactory.newInstance().newXPath().evaluate(expression, node);
        } catch (XPathExpressionException var3) {
            log.error("Failed to extract xpath value from xml: xpath=" + expression);
            return "";
        }
    }

    public static Properties readPropertiesFile(File propertiesFile) {
        Properties p = new Properties();
        InputStream is = null;

        try {
            is = new FileInputStream(propertiesFile);
            p.load(is);
        } catch (IOException var7) {
            IOException e = var7;
            if (!propertiesFile.exists()) {
                log.error("Properties file '" + propertiesFile.getName() + "' doesn't exist");
            } else {
                log.error("Failed to read properties file: " + propertiesFile.getName(), e);
            }
        } finally {
            close(is);
        }

        return p;
    }

    public static Properties readPropertiesFromStream(InputStream propStream) {
        Properties p = new Properties();

        try {
            p.load(propStream);
        } catch (IOException var3) {
            IOException e = var3;
            log.error("Failed to read properties from InputStream", e);
        }

        return p;
    }

    public static String readResourceToString(ClassLoader classLoader, String resourcePath) {
        InputStream str = classLoader.getResourceAsStream(resourcePath);

        String var5;
        try {
            if (str == null) {
                return "";
            }

            BufferedInputStream data = new BufferedInputStream(str);
            byte[] buf = new byte[data.available()];
            data.read(buf);
            var5 = new String(buf);
        } catch (IOException var9) {
            IOException e = var9;
            log.error("Failed to read resource: " + resourcePath, e);
            return "";
        } finally {
            close(str);
        }

        return var5;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException var3) {
            InterruptedException e = var3;
            log.error("Exception caugth while sleeping", e);
        }

    }

    public static void sleepSeconds(int seconds) {
        sleep((long)(1000 * seconds));
    }

    public static void close(Closeable... closeables) {
        if (closeables != null) {
            Closeable[] var1 = closeables;
            int var2 = closeables.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                Closeable c = var1[var3];

                try {
                    if (c != null) {
                        c.close();
                    }
                } catch (IOException var6) {
                    IOException e = var6;
                    log.error("Failed to close " + c.getClass().getSimpleName(), e);
                }
            }

        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];

        int read;
        while((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }

    }

    public static void unzip(File zipFile, File extractToDir) {
        try {
            ZipFile archive = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> e = archive.entries();

            while(true) {
                while(e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry)e.nextElement();
                    File file = new File(extractToDir, entry.getName());
                    if (entry.isDirectory() && !file.exists()) {
                        file.mkdirs();
                    } else {
                        if (!file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }

                        if (!file.isDirectory()) {
                            InputStream is = null;
                            OutputStream os = null;

                            try {
                                is = archive.getInputStream(entry);
                                os = new FileOutputStream(file);
                                copy(is, os);
                            } finally {
                                close(is, os);
                            }
                        }
                    }
                }

                return;
            }
        } catch (IOException var12) {
            IOException e = var12;
            log.error("Failed to unzip :" + zipFile + " to dir: " + extractToDir, e);
        }
    }

    public static void unzip(ZipInputStream input, File extractToDir) {
        ZipEntry entry = null;

        try {
            while((entry = input.getNextEntry()) != null) {
                File file = new File(extractToDir, entry.getName());
                if (entry.isDirectory() && !file.exists()) {
                    file.mkdirs();
                } else {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }

                    if (!file.isDirectory()) {
                        OutputStream os = null;

                        try {
                            os = new FileOutputStream(file);
                            copy(input, os);
                        } finally {
                            close(os);
                        }
                    }
                }
            }
        } catch (IOException var14) {
            IOException e = var14;
            if (entry != null) {
                log.error("Failed to unzip :" + entry.getName() + " to dir: " + extractToDir, e);
            }
        } finally {
            close(input);
        }

    }

    public static final void zip(File file, File zipFile) {
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            if (file.isDirectory()) {
                zip(file.listFiles(), file, zos);
            } else if (file.isFile()) {
                zip(new File[]{file}, file.getParentFile(), zos);
            }
        } catch (IOException var7) {
            IOException e = var7;
            log.error("Failed to zip directory: " + file, e);
        } finally {
            close(zos);
        }

    }

    private static final void zip(File[] files, File baseDir, ZipOutputStream zos) throws IOException {
        File[] var3 = files;
        int var4 = files.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];
            if (file.isDirectory()) {
                zip(file.listFiles(), baseDir, zos);
            } else {
                FileInputStream is = new FileInputStream(file);

                try {
                    ZipEntry entry = new ZipEntry(file.getPath().substring(baseDir.getPath().length() + 1));
                    zos.putNextEntry(entry);
                    copy(is, zos);
                    zos.closeEntry();
                } finally {
                    close(is);
                }
            }
        }

    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String normalizeString(String s) {
        return s == null ? "" : s.replace(' ', ' ').trim();
    }

    public static String convertGlobMetaCharsToRegexpMetaChars(String glob) {
        return glob.replaceAll("([\\.\\^\\$\\+\\(\\)\\{\\}\\[\\]\\\\\\|])", "\\\\$1").replaceAll("\\?", "(.|[\n\r])").replaceAll("\\*", "(.|[\n\r])*");
    }

    public static String getRegexp(String expression) {
        return expression.startsWith("regexp:") ? expression.replaceFirst("regexp:", "") : "^" + convertGlobMetaCharsToRegexpMetaChars(expression) + "$";
    }

    public static Pattern preparePattern(String expression) {
        return Pattern.compile(getRegexp(expression));
    }

    static {
        compressor.setRemoveComments(false);
    }
}

