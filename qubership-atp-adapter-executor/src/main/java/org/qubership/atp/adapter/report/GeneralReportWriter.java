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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GeneralReportWriter implements ReportWriter {
    private static final Log LOG = LogFactory.getLog(GeneralReportWriter.class);
    public static final String REPORT_ENCODING = "report.encoding";
    private static final String HEAD1 = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + Config.getString("report.encoding", "utf-8") + "\">" + "</head>" + "<body style=\"font-size:12px;font-style:normal;font-family:arial,verbana,times;\">";
    private static final String HEAD3 = "<br/><table bordercolor=\"black\" bgcolor=\"#F6FFF2\" border='2' cellpadding='2' cellspacing='0' style=\"font-size:12px;font-style:normal;font-family:arial,verbana,times;\"><tr class='pass'><td width='75px'><b>Status</b></td><td width='300px'><b>Test Case</b></td><td width='600px'><b>Description</b></td><td width='85px'><b>Execution time</b></td><td width='35px'><b>Cause</b></td></tr>";
    public static final String SUFFIX_KEY = "suffix";
    public static final String REPORT_DIR_KEY = "report.dir";
    public static final String REPORT_DIR_ROOT_KEY = "report.dir.root";
    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat timeFormat;
    private static String SERVER_URL_KEY;
    private static String SERVER_ALIAS_KEY;
    private static String serverInfo;
    private final long startTime = System.currentTimeMillis();
    private final Map<Thread, List<ScenarioInfo>> scenarios = new LinkedHashMap();
    private final List<ScenarioInfo> scenariosOrdered = new LinkedList();
    private File webReportDir = getWebReportDir();
    private static int failedRequestsCount;

    public static String getEnvValue(String key, String def) {
        String value = System.getProperty(key, "");
        if (value.length() == 0) {
            value = Config.getString(key, def);
            if (value == null || value.length() == 0) {
                value = def;
            }
        }

        return value;
    }

    public static File getWebReportDir() {
        Iterator<ReportWriter> iterator = Report.getReport().writerIterator();

        ReportWriter reportWriter;
        do {
            if (!iterator.hasNext()) {
                String suffix = Config.getString("suffix", "");
                if (suffix.length() > 0) {
                    suffix = "_" + suffix;
                }

                String reportDirname = Config.getString("report.dir");
                if (reportDirname.length() > 0) {
                    reportDirname = reportDirname + suffix;
                }

                String reportRootDir = Config.getString("report.dir.root");
                if (reportRootDir.length() == 0) {
                    reportRootDir = "results";
                }

                return new File(reportRootDir, reportDirname);
            }

            reportWriter = (ReportWriter)iterator.next();
        } while(!(reportWriter instanceof WebReportWriter));

        return ((WebReportWriter)reportWriter).getReportDir();
    }

    public static String doHttpsGet(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init((KeyManager[])null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        URL targetUrl = new URL(url);
        URLConnection con = targetUrl.openConnection();
        Reader reader = new InputStreamReader(con.getInputStream());
        StringBuilder returnBuild = new StringBuilder();

        while(true) {
            int ch = ((Reader)reader).read();
            if (ch == -1) {
                return returnBuild.toString();
            }

            returnBuild.append((char)ch);
        }
    }

    public static String getServerVersionInfo(String serverURL) {
        try {
            if (serverURL != null && !serverURL.isEmpty()) {
                return doHttpsGet(serverURL + "/version.txt");
            }

            return "param 'server' is incorrect";
        } catch (KeyManagementException var2) {
            LOG.error("KeyManagementException", var2);
        } catch (NoSuchAlgorithmException var3) {
            LOG.error("NoSuchAlgorithmException", var3);
        } catch (ConnectException var4) {
            if (failedRequestsCount >= 10) {
                return "Failed to get connection to server" + serverURL;
            }

            LOG.error("Failed to get connection to server " + serverURL, var4);
            ++failedRequestsCount;
        } catch (IOException var5) {
            IOException e = var5;
            LOG.error("Failed to get server version information", e);
        }

        return "";
    }

    public GeneralReportWriter() {
    }

    public synchronized void report(WebReportItem.Message message, Thread thread) {
        synchronized(this.scenarios) {
            if (!this.scenarios.isEmpty()) {
                List<ScenarioInfo> scenarioList = (List)this.scenarios.get(thread);
                ((ScenarioInfo)scenarioList.get(scenarioList.size() - 1)).setLevel(message.getLevel().toInt());
                Throwable throwable = message.getThrowable();
                String cause;
                if (throwable == null) {
                    cause = message.getMessage();
                } else if (!WebReportItem.printReportThrowableMessageShort && throwable.getMessage() != null) {
                    cause = throwable.getCause() != null && throwable.getCause().getCause() != null ? throwable.getCause().getCause().toString() : throwable.getMessage();
                } else if (WebReportItem.printReportThrowableMessageShort && throwable.getMessage() != null) {
                    cause = throwable.getMessage();
                } else {
                    cause = "<pre>" + Utils.getStackTrace(throwable) + "</pre>";
                }

                ((ScenarioInfo)scenarioList.get(scenarioList.size() - 1)).setCause(cause);
                ((ScenarioInfo)scenarioList.get(scenarioList.size() - 1)).setEndTime(System.currentTimeMillis());
            } else {
                this.newScenario(new WebReportItem.OpenLog(message.getTitle(), message.getMessage()), thread);
                this.report(message, thread);
            }

        }
    }

    public synchronized void newScenario(WebReportItem.OpenLog message, Thread thread) {
        synchronized(this.scenarios) {
            List<ScenarioInfo> scenarioList = (List)this.scenarios.get(thread);
            if (scenarioList == null) {
                scenarioList = new ArrayList();
                this.scenarios.put(thread, scenarioList);
            } else {
                ((ScenarioInfo)((List)scenarioList).get(((List)scenarioList).size() - 1)).setEndTime(System.currentTimeMillis());
            }

            ScenarioInfo soinfo = new ScenarioInfo(message.getLogName(), message.getDescription());
            ((List)scenarioList).add(soinfo);
            this.scenariosOrdered.add(soinfo);
        }
    }

    public synchronized void reportScenarioDetails() {
        StringBuilder sb = new StringBuilder();
        int info = 0;
        int warn = 0;
        int error = 0;
        Iterator i$ = this.scenariosOrdered.iterator();

        while(i$.hasNext()) {
            ScenarioInfo si = (ScenarioInfo)i$.next();
            sb.append("<tr>");
            sb.append("<td bgcolor=\"").append(si.getLevel()[0]).append("\" >").append("<b><font color=\"").append(si.getLevel()[1]).append("\" >").append(si.getLevel()[2]).append("</font></b>").append("</td>");
            sb.append("<td>").append("<b>").append(si.getName()).append("</b>").append("</td>");
            sb.append("<td>").append("<b>").append(si.getDescription()).append("</b>").append("</td>");
            sb.append("<td>").append("<b>").append(si.getExecutionTime()).append("</b>").append("</td>");
            sb.append("<td>").append("&#160;").append(si.getCause()).append("</b>").append("</td>");
            sb.append("</tr>");
            if (!"Execution Info".equals(si.getName()) && !"Prerequisites".equals(si.getName())) {
                switch (si.level) {
                    case 30000:
                        ++warn;
                        break;
                    case 40000:
                        ++error;
                        break;
                    default:
                        ++info;
                }
            }
        }

        sb.append("</table>").append("</body>").append("</html>");
        int all = info + warn + error;
        StringBuilder headerPart = new StringBuilder();
        long execTime = (System.currentTimeMillis() - this.startTime) / 1000L;
        headerPart.append("<b>Report Time: </b>").append("date=\"").append(dateFormat.format(new Date())).append("\" t=\"").append(timeFormat.format(new Date())).append("\"").append("<br/>");
        headerPart.append("<b>Duration: </b>").append(String.format("%02d hours %02d minutes", execTime / 3600L, execTime % 3600L / 60L)).append("<br/>");
        if (Integer.parseInt(getEnvValue(Config.getString(SERVER_ALIAS_KEY) + ".threads", "1")) > 1) {
            headerPart.append("<b>Threads: </b>").append(getEnvValue(Config.getString(SERVER_ALIAS_KEY) + ".threads", "1")).append("<br/>");
        }

        if (info > 0) {
            headerPart.append("<b>Passed cases: </b>").append(info).append("<br/>");
        }

        if (warn > 0) {
            headerPart.append("<b>Cases with warnings: </b>").append(warn).append("<br/>");
        }

        if (error > 0) {
            headerPart.append("<b>Failed cases: </b>").append(error).append("<br/>");
        }

        if (all > 0) {
            headerPart.append("<b>Executed scenarios:</b>  ").append(all).append("<br/>");
            headerPart.append("<b>Failure rate:</b>  ").append(Math.abs(error * 100 / all)).append("%");
        } else {
            headerPart.append("<b>Failure rate:</b>  0%");
        }

        FileOutputStream fos = null;
        if (GeneralReportWriter.serverInfo.isEmpty()) {
            StringBuilder serverInfo = new StringBuilder();
            serverInfo.append("<b>Server: </b>").append("<a target=\"_blank\" href=\"" + Config.getString(SERVER_URL_KEY) + "\">").append(Config.getString(SERVER_URL_KEY)).append("</a>").append("<br /> <b>Build:</b>").append("<a target=\"_blank\" href=\"" + Config.getString(SERVER_URL_KEY) + "/version.jsp\">").append(getServerVersionInfo(Config.getString(SERVER_URL_KEY))).append("</a>").append("<br>");
            setServerInfo(serverInfo.toString());
        }

        this.generateReportFile((new StringBuilder(HEAD1)).append(GeneralReportWriter.serverInfo).append(headerPart).append("<br/><table bordercolor=\"black\" bgcolor=\"#F6FFF2\" border='2' cellpadding='2' cellspacing='0' style=\"font-size:12px;font-style:normal;font-family:arial,verbana,times;\"><tr class='pass'><td width='75px'><b>Status</b></td><td width='300px'><b>Test Case</b></td><td width='600px'><b>Description</b></td><td width='85px'><b>Execution time</b></td><td width='35px'><b>Cause</b></td></tr>").append(sb.toString()).toString(), "GeneralReport.html");
    }

    public synchronized void reportXmlJunitDetails() {
        StringBuilder sb = new StringBuilder();
        int testCount = 0;

        for(Iterator i$ = this.scenariosOrdered.iterator(); i$.hasNext(); sb.append("</testcase>")) {
            ScenarioInfo si = (ScenarioInfo)i$.next();
            ++testCount;
            sb.append(String.format("<testcase classname=\"%s\" name=\"%s\">", StringEscapeUtils.escapeXml("Test Scope"), StringEscapeUtils.escapeXml(si.name)));
            if (si.level == 40000) {
                if ("true".equals(Config.getString("report.junit.ci.url"))) {
                    sb.append("<failure type=\"\"  message=\"" + Config.getString("report.detailed.path") + this.getFolderByScenarioName(si.getName()) + "/report.html\"/>");
                } else {
                    sb.append("<failure type=\"\">" + StringEscapeUtils.escapeXml(si.getCause()) + "</failure>");
                }
            }
        }

        this.generateReportFile("<testsuite tests=\"" + testCount + "\">" + sb.toString() + "</testsuite>", "JUnitReport.xml");
    }

    public String getFolderByScenarioName(String name) {
        String[] arr$ = getWebReportDir().list();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String folder = arr$[i$];
            if (folder.contains(name)) {
                return folder;
            }
        }

        return "";
    }

    private void generateReportFile(String text, String reportFileName) {
        FileOutputStream fos = null;

        try {
            if (!getWebReportDir().exists()) {
                getWebReportDir().mkdirs();
            }

            File reportFile;
            synchronized(reportFile = new File(getWebReportDir(), reportFileName)) {
                fos = new FileOutputStream(reportFile, false);
                fos.write(text.getBytes());
            }
        } catch (IOException var12) {
            IOException e = var12;
            LOG.error("Failed to clone or open file: " + getWebReportDir(), e);
        } finally {
            Utils.close(new Closeable[]{fos});
        }

    }

    public static void setServerInfo(String newServerInfo) {
        serverInfo = newServerInfo;
    }

    public static String getServerInfo() {
        return serverInfo;
    }

    public static void setServerUrlKey(String serverUrlKey) {
        SERVER_URL_KEY = serverUrlKey;
    }

    public static String getServerUrlKey() {
        return SERVER_URL_KEY;
    }

    public static void setServerAliasKey(String serverAliasKey) {
        SERVER_ALIAS_KEY = serverAliasKey;
    }

    public static String getServerAliasKey() {
        return SERVER_ALIAS_KEY;
    }

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        SERVER_URL_KEY = "server.url";
        SERVER_ALIAS_KEY = "server.alias";
        serverInfo = "";
        failedRequestsCount = 0;
        WebReportWriter.changeEncoding(Config.getString("report.encoding", "utf-8"));
    }

    private static class ScenarioInfo {
        private final String name;
        private String description;
        private String cause;
        private int level;
        private long start;
        private long end;
        private boolean hasError = false;

        ScenarioInfo(String scenarioName, String description) {
            this.name = scenarioName;
            this.level = 0;
            this.description = description;
            this.start = System.currentTimeMillis();
            this.end = this.start;
        }

        void setLevel(int newLevel) {
            this.level = newLevel > this.level ? newLevel : this.level;
        }

        public String getName() {
            return this.name;
        }

        public String getDescription() {
            return this.description;
        }

        public String getCause() {
            return this.hasError ? this.cause : "";
        }

        public void setCause(String cause) {
            if (!this.hasError && (this.level == 40000 || this.level == 50000)) {
                this.hasError = true;
                this.cause = cause;
            }

        }

        public String[] getLevel() {
            switch (this.level) {
                case 30000:
                    return new String[]{"#FFD700", "black", "Warning"};
                case 40000:
                case 50000:
                    return new String[]{"red", "black", "Failed"};
                default:
                    return new String[]{"#32CD32", "white", "Passed"};
            }
        }

        public void setEndTime(long endTime) {
            this.end = endTime;
        }

        public String getExecutionTime() {
            long s = (this.end - this.start) / 1000L;
            return String.format("%02d:%02d:%02d", s / 3600L, s % 3600L / 60L, s % 60L);
        }
    }
}

