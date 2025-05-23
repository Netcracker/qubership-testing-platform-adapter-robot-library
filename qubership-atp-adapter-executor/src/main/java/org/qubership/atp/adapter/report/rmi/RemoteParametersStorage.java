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

import org.qubership.atp.adapter.testcase.Config;

public class RemoteParametersStorage {
    private static ThreadLocal<String> serverName = new ThreadLocal();
    private static ThreadLocal<String> port = new ThreadLocal();
    private static ThreadLocal<String> host = new ThreadLocal();
    private static ThreadLocal<Boolean> isRemote = new ThreadLocal();
    private static ThreadLocal<Boolean> isServer = new ThreadLocal();

    public RemoteParametersStorage() {
    }

    public static void setIsRemote(boolean isRemote) {
        RemoteParametersStorage.isRemote.set(isRemote);
    }

    public static void setServerName(String serverName) {
        RemoteParametersStorage.serverName.set(serverName);
    }

    public static void setPort(String port) {
        RemoteParametersStorage.port.set(port);
    }

    public static void setHost(String host) {
        RemoteParametersStorage.host.set(host);
    }

    public static void setIsServer(boolean isServer) {
        RemoteParametersStorage.isServer.set(isServer);
    }

    public static String getBindAddress() {
        return "rmi://" + getHost() + ":" + getPort() + "/" + getServerName();
    }

    public static boolean getIsServer() {
        if (isServer.get() == null) {
            setIsServer(Config.getBoolean("report.is.server", false));
        }

        return (Boolean)isServer.get();
    }

    public static boolean getIsRemote() {
        if (isRemote.get() == null) {
            setIsRemote(Config.getBoolean("report.remote", false));
        }

        return (Boolean)isRemote.get();
    }

    public static String getServerName() {
        if (serverName.get() == null) {
            setServerName(Config.getString("report.remote.server", "Server"));
        }

        return (String)serverName.get();
    }

    public static String getPort() {
        if (port.get() == null) {
            setPort(Config.getString("report.remote.port", "8081"));
        }

        return (String)port.get();
    }

    public static String getHost() {
        if (host.get() == null) {
            setHost(Config.getString("report.remote.host", "localhost"));
        }

        return (String)host.get();
    }
}

