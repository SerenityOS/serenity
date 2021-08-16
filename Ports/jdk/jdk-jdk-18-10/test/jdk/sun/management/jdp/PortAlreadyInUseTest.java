/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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


import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

/**
 * This test used for test development and it is not meant to be run by JTReg.
 * <p/>
 * This test exercises a retry mechanism to avoid a test failure because of
 * starting the VM on a busy jmxremote.port.
 * <p/>
 * To run this test you'll need to add this VM option: -Dtest.jdk=<path-to-jdk>
 */
public class PortAlreadyInUseTest extends DynamicLauncher {

    ServerSocket socket;
    final Logger log = Logger.getLogger("PortAlreadyInUse");

    protected void go() throws Exception {
        jmxPort = Utils.getFreePort();
        occupyPort();

        log.info("Attempting to start a VM using the same port.");
        OutputAnalyzer out = this.runVM();
        out.shouldContain("Port already in use");
        log.info("Failed as expected.");

        log.info("Trying again using retries.");
        this.run();
    }

    private void occupyPort() throws IOException {
        socket = new ServerSocket(jmxPort);
        log.info("Occupying port " + String.valueOf(jmxPort));
    }

    protected String[] options() {
        String[] options = {
                "-Dcom.sun.management.jmxremote.authenticate=false",
                "-Dcom.sun.management.jmxremote.ssl=false",
                "-Dcom.sun.management.jmxremote=true",
                "-Dcom.sun.management.jmxremote.port=" + String.valueOf(jmxPort),
                "-version"
        };
        return options;
    }

    public static void main(String[] args) throws Exception {
        PortAlreadyInUseTest test = new PortAlreadyInUseTest();
        test.go();
    }

}
