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

import org.qubership.atp.adapter.testcase.Config;
import org.qubership.atp.adapter.utils.Utils;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractWebReportWriter implements ReportWriter {
    private static final Log LOG = LogFactory.getLog(AbstractWebReportWriter.class);
    public static final String REPORT_DIR_ROOT_KEY = "report.dir.root";
    public static final String REPORT_ENCODING = "report.encoding";
    protected File indexFile;

    public AbstractWebReportWriter() {
    }

    protected void prepareReportTemplate() {
        File reportDir = this.getReportDir();
        if (reportDir.exists()) {
            try {
                FileUtils.cleanDirectory(reportDir);
            } catch (IOException var19) {
                LOG.warn("Failed to clean existing report dir: " + reportDir.getPath());
            }
        }

        File pagesDir = new File(reportDir, "pages");
        pagesDir.mkdirs();

        try {
            String s = this.getResourcesLocationInJar();
            URLConnection connection = this.getClass().getClassLoader().getResource(s).openConnection();
            if (connection instanceof JarURLConnection) {
                JarFile archive = ((JarURLConnection)connection).getJarFile();
                Enumeration<? extends JarEntry> e = archive.entries();

                label119:
                while(true) {
                    while(true) {
                        JarEntry entry;
                        String entryName;
                        do {
                            do {
                                if (!e.hasMoreElements()) {
                                    break label119;
                                }

                                entry = (JarEntry)e.nextElement();
                                entryName = entry.getName();
                            } while(!entryName.startsWith(s));
                        } while(entryName.equals(s));

                        File file = new File(reportDir, entry.getName().substring(s.length()));
                        if (entry.isDirectory()) {
                            if (!file.exists()) {
                                file.mkdirs();
                            }
                        } else {
                            if (!file.getParentFile().exists()) {
                                file.getParentFile().mkdirs();
                            }

                            InputStream is = null;
                            OutputStream os = null;

                            try {
                                is = archive.getInputStream(entry);
                                os = new FileOutputStream(file);
                                byte[] buffer = new byte[8192];

                                int read;
                                while((read = is.read(buffer)) != -1) {
                                    ((OutputStream)os).write(buffer, 0, read);
                                }
                            } finally {
                                Utils.close(new Closeable[]{is, os});
                            }
                        }
                    }
                }
            } else {
                FileUtils.copyDirectory(new File(connection.getURL().toURI()), reportDir);
            }
        } catch (IOException var21) {
            IOException e = var21;
            LOG.error("Unable to find report files", e);
            return;
        } catch (URISyntaxException var22) {
            URISyntaxException e = var22;
            LOG.error("Unable to find report files", e);
            return;
        }

        this.prepareIndexFile();
    }

    protected abstract void prepareIndexFile();

    protected String getReportDirRoot(Properties p) {
        String reportRootDir = p == null ? Config.getString("report.dir.root") : p.getProperty("report.dir.root", Config.getString("report.dir.root"));
        if (reportRootDir.length() == 0) {
            reportRootDir = "results";
        }

        return reportRootDir;
    }

    protected String getReportDirName(Properties p) {
        String suffix = p == null ? "" : p.getProperty("suffix", "");
        if (suffix.length() > 0) {
            suffix = "_" + suffix;
        }

        String reportDirname = p == null ? Config.getString("report.dir") : p.getProperty("report.dir", Config.getString("report.dir"));
        if (reportDirname.isEmpty()) {
            reportDirname = "report" + suffix + "_" + (new SimpleDateFormat("yyMMdd_HHmmss_SSS")).format(new Date());
        } else {
            reportDirname = reportDirname + suffix;
        }

        return reportDirname;
    }

    public static void changeEncoding(String encoding) {
        try {
            Class<Charset> c = Charset.class;
            Field defaultCharsetField = c.getDeclaredField("defaultCharset");
            defaultCharsetField.setAccessible(true);
            defaultCharsetField.set((Object)null, Charset.forName(encoding));
        } catch (NoSuchFieldException var3) {
            NoSuchFieldException ex = var3;
            LOG.warn("Can't set encoding", ex);
        } catch (IllegalAccessException var4) {
            IllegalAccessException ex = var4;
            LOG.warn("Can't set encoding", ex);
        }

    }

    public abstract String getResourcesLocationInJar();

    public abstract File getReportDir();
}

