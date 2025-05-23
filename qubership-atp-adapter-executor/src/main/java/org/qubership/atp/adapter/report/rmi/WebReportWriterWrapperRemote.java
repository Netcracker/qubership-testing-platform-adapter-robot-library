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

package org.qubership.atp.adapter.report.rmi;

import org.qubership.atp.adapter.report.InterruptScenarioException;
import org.qubership.atp.adapter.report.ReportWriter;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.apache.log4j.Logger;

public class WebReportWriterWrapperRemote implements ReportWriter {
    private static Logger logger = Logger.getLogger("org.qubership.atp.adapter.report.rmi.WebReportWriterWrapperRemote");
    private static WebReportWriterServer server = null;
    private static WebReportWriterClient client = null;

    public WebReportWriterWrapperRemote() {
    }

    public static void stopServer() {
        if (server != null) {
            try {
                server.stop();
            } catch (RemoteException var1) {
                RemoteException e = var1;
                throw new InterruptScenarioException(e);
            } catch (NotBoundException var2) {
                NotBoundException e = var2;
                throw new InterruptScenarioException(e);
            }
        }

    }

    public static WebReportWriterServer initServer() {
        if (server == null) {
            server = new WebReportWriterServer();
        }

        if (!server.isStarted()) {
            try {
                server.start();
            } catch (NotBoundException var1) {
                NotBoundException e = var1;
                throw new InterruptScenarioException(e);
            } catch (RemoteException var2) {
                RemoteException e = var2;
                throw new InterruptScenarioException(e);
            }
        }

        return server;
    }

    public static WebReportWriterClient getClient() {
        if (client == null) {
            client = new WebReportWriterClient();
            logger.info("RMI Client started. Communicating via address: " + RemoteParametersStorage.getBindAddress());
        }

        return client;
    }

    public RemoteToLocalReportAdapter getWebReportAdapterRemote() {
        getClient();
        return WebReportWriterWrapperRemote.WebReportWriterClient.getAdapter();
    }

    public static class WebReportWriterServer {
        private static Registry registry;
        private boolean started = false;

        protected WebReportWriterServer() {
            if (RemoteParametersStorage.getHost() == null || RemoteParametersStorage.getPort() == null || RemoteParametersStorage.getServerName() == null) {
                throw new InterruptScenarioException("One or more required parameters were not specified: 'host', 'port', 'serverName'");
            }
        }

        public void start() throws IllegalArgumentException, NotBoundException, RemoteException {
            if (!this.started) {
                try {
                    RemoteReportAdapter writer = new LocalToRemoteReportAdapter(Thread.currentThread().getName());
                    registry = LocateRegistry.createRegistry(Integer.parseInt(RemoteParametersStorage.getPort(), 10));
                    registry.rebind(RemoteParametersStorage.getServerName(), writer);
                    this.started = true;
                    WebReportWriterWrapperRemote.logger.info("RMI Server started. Listening at address: " + RemoteParametersStorage.getBindAddress());
                } catch (ConnectException var2) {
                    throw new InterruptScenarioException("Failed to start RMI server on given address: " + RemoteParametersStorage.getBindAddress());
                }
            }
        }

        public void stop() throws RemoteException, NotBoundException {
            if (registry != null) {
                registry.unbind(RemoteParametersStorage.getBindAddress());
                this.started = false;
                WebReportWriterWrapperRemote.logger.info("RMI Server stopped.");
            }

        }

        public boolean isStarted() {
            return this.started;
        }

        public static Registry getRegistry() {
            return registry;
        }
    }

    public static class WebReportWriterClient {
        private static RemoteToLocalReportAdapter adapter;

        protected WebReportWriterClient() {
            getAdapter();
        }

        public static RemoteToLocalReportAdapter getAdapter() {
            if (adapter == null) {
                try {
                    adapter = new RemoteToLocalReportAdapter((RemoteReportAdapter)LocateRegistry.getRegistry(RemoteParametersStorage.getHost(), new Integer(RemoteParametersStorage.getPort())).lookup(RemoteParametersStorage.getServerName()));
                } catch (NotBoundException var1) {
                    NotBoundException e = var1;
                    throw new InterruptScenarioException("Unable to find RMI server on given address: " + RemoteParametersStorage.getBindAddress(), e);
                } catch (ConnectException var2) {
                    ConnectException e = var2;
                    throw new InterruptScenarioException("Client was unable to connect to the server. Exception: " + e.getMessage());
                } catch (RemoteException var3) {
                    RemoteException e = var3;
                    throw new InterruptScenarioException(e);
                }
            }

            return adapter;
        }
    }
}

