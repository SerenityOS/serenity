/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8035668
 * @library /test/lib
 * @summary Test checks case when target application finishes execution and jstat didn't complete work.
            jstat is started with interval = 100 (jstat -compiler 100) and monitored application finishes
            after 500ms. This shouldn't cause crash or hang in target application or in jstat.
 * @modules java.management
 * @build JStatInterval
 * @run main JStatInterval
 */

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JStatInterval {
    private static final String READY = "READY";
    private static final String ERROR = "!ERROR";

    public static class Application {
        public static void main(String[] args) {
            try {
                System.out.println(READY);
                System.out.flush();
                int exitCode = System.in.read();
                Thread.sleep(500);
                System.exit(exitCode);
            } catch (Exception e) {
                System.out.println(ERROR);
                System.out.flush();
                throw new Error(e);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-cp",
            System.getProperty("test.class.path"),
            "-XX:+UsePerfData",
            Application.class.getName()
        );
        AtomicBoolean error = new AtomicBoolean(false);
        Process app = ProcessTools.startProcess(
            "application",
            pb,
            line -> {
                if (line.equals(READY)) {
                    return true;
                } else if (line.equals(ERROR)) {
                    error.set(true);
                    return true;
                }
                return false;
            },
            10,
            TimeUnit.SECONDS
        );
        if (error.get()) {
            throw new Error("Unable to start the monitored application.");
        }

        String pidStr = String.valueOf(app.pid());
        JDKToolLauncher l = JDKToolLauncher.createUsingTestJDK("jstat");
        l.addVMArgs(Utils.getTestJavaOpts());
        l.addToolArg("-compiler");
        l.addToolArg(pidStr);
        l.addToolArg("100");

        ProcessBuilder jstatDef = new ProcessBuilder(l.getCommand());
        Process jstat = ProcessTools.startProcess(
            "jstat",
            jstatDef,
            line -> {
                if (line.trim().toLowerCase().startsWith("compiled")) {
                    return true;
                }
                return false;
            },
            10,
            TimeUnit.SECONDS
        );

        app.getOutputStream().write(0);
        app.getOutputStream().flush();

        if (app.waitFor() != 0) {
            throw new Error("Error detected upon exiting the monitored application with active jstat");
        }
        if (jstat.waitFor() != 0) {
            throw new Error("Error detected in jstat when monitored application has exited prematurely");
        }
    }
}
