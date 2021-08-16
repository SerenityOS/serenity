/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;


public class DebugdUtils {
    private static final String GOLDEN = "Debugger attached";
    private String serverID;
    private int registryPort;
    private boolean disableRegistry;
    private String serverName;
    private Process debugdProcess;

    public DebugdUtils() {
        this.serverID = null;
        this.registryPort = 0;
        this.disableRegistry = false;
        this.serverName = null;
        debugdProcess = null;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public void setDisableRegistry(boolean disableRegistry) {
        this.disableRegistry = disableRegistry;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void attach(long pid) throws IOException {
        JDKToolLauncher jhsdbLauncher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        jhsdbLauncher.addVMArgs(Utils.getTestJavaOpts());
        jhsdbLauncher.addToolArg("debugd");
        jhsdbLauncher.addToolArg("--pid");
        jhsdbLauncher.addToolArg(Long.toString(pid));
        if (serverID != null) {
            jhsdbLauncher.addToolArg("--serverid");
            jhsdbLauncher.addToolArg(serverID);
        }
        if (registryPort != 0) {
            jhsdbLauncher.addToolArg("--registryport");
            jhsdbLauncher.addToolArg(Integer.toString(registryPort));
        }
        if (disableRegistry) {
            jhsdbLauncher.addToolArg("--disable-registry");
        }
        if (serverName != null) {
            jhsdbLauncher.addToolArg("--servername");
            jhsdbLauncher.addToolArg(serverName);
        }
        debugdProcess = (new ProcessBuilder(jhsdbLauncher.getCommand())).start();

        // Wait until debug server attached
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(debugdProcess.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(GOLDEN)) {
                    break;
                }
            }
        }
    }

    public void detach() throws InterruptedException {
        if (debugdProcess != null) {
            debugdProcess.destroy();
            debugdProcess.waitFor();
        }
    }
}
