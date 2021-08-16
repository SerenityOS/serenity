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
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

import java.util.UUID;


/**
 * This class will try to find an unused port and run a JdpTestCase using it.
 * The unused port is needed for jmxremote.port.
 * The problem with busy ports arises when running many automated tests on the same host.
 * Note that jdp.port is a multicast port and thus it can be binded by different processes at the same time.
 */
public abstract class DynamicLauncher {

    final String jdpName = UUID.randomUUID().toString();
    OutputAnalyzer output;
    int jmxPort;

    protected void run() throws Exception {
        int retries = 1;
        boolean tryAgain;

        do {
            tryAgain = false;
            jmxPort = Utils.getFreePort();
            output = runVM();
            try {
                output.shouldNotContain("Port already in use");
            } catch (RuntimeException e) {
                if (retries < 3) {
                    retries++;
                    tryAgain = true;
                }
            }
        } while (tryAgain);
        output.shouldHaveExitValue(0);
        // java.lang.Exception is thrown by JdpTestCase if something goes wrong
        // for instance - see JdpTestCase::shutdown()
        output.shouldNotContain("java.lang.Exception:");
        output.shouldNotContain("Error: Could not find or load main class");
    }

    protected OutputAnalyzer runVM() throws Exception {
        String[] options = this.options();
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(options);
        OutputAnalyzer out = ProcessTools.executeProcess(pb);
        System.out.println(out.getStdout());
        System.err.println(out.getStderr());
        return out;
    }

    protected abstract String[] options();

    protected OutputAnalyzer getProcessOutpoutAnalyzer() {
        return output;
    }
}
