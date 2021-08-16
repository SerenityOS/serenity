/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8165246 8010319
 * @summary Test has_previous_versions flag and processing during class unloading.
 * @requires vm.jvmti
 * @requires vm.opt.final.ClassUnloading
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run driver RedefineClassHelper
 * @run driver RedefinePreviousVersions test
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

// package access top-level classes to avoid problem with RedefineClassHelper
// and nested types.

class RedefinePreviousVersions_B { }

class RedefinePreviousVersions_Running {
    public static volatile boolean stop = false;
    public static volatile boolean running = false;
    static void localSleep() {
        try {
            Thread.sleep(10); // sleep for 10 ms
        } catch(InterruptedException ie) {
        }
    }

    public static void infinite() {
        running = true;
        while (!stop) { localSleep(); }
    }
}



public class RedefinePreviousVersions {

    public static String newB =
                "class RedefinePreviousVersions_B {" +
                "}";

    public static String newRunning =
        "class RedefinePreviousVersions_Running {" +
        "    public static volatile boolean stop = true;" +
        "    public static volatile boolean running = true;" +
        "    static void localSleep() { }" +
        "    public static void infinite() { }" +
        "}";

    public static void main(String[] args) throws Exception {

        if (args.length > 0) {

            // java -javaagent:redefineagent.jar -Xlog:stuff RedefinePreviousVersions
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder( "-javaagent:redefineagent.jar",
               "-Xlog:redefine+class+iklass+add=trace,redefine+class+iklass+purge=trace",
               "RedefinePreviousVersions");
            new OutputAnalyzer(pb.start())
              .shouldContain("Class unloading: has_previous_versions = false")
              .shouldContain("Class unloading: has_previous_versions = true")
              .shouldHaveExitValue(0);
            return;
        }

        // Redefine a class and create some garbage
        // Since there are no methods running, the previous version is never added to the
        // previous_version_list and the flag _has_previous_versions should stay false
        RedefineClassHelper.redefineClass(RedefinePreviousVersions_B.class, newB);

        for (int i = 0; i < 10 ; i++) {
            String s = new String("some garbage");
            System.gc();
        }

        // Start a class that has a method running
        new Thread() {
            public void run() {
                RedefinePreviousVersions_Running.infinite();
            }
        }.start();

        while (!RedefinePreviousVersions_Running.running) {
            Thread.sleep(10); // sleep for 10 ms
        }

        // Since a method of newRunning is running, this class should be added to the previous_version_list
        // of Running, and _has_previous_versions should return true at class unloading.
        RedefineClassHelper.redefineClass(RedefinePreviousVersions_Running.class, newRunning);

        for (int i = 0; i < 10 ; i++) {
            String s = new String("some garbage");
            System.gc();
        }

        // purge should clean everything up, except Xcomp it might not.
        RedefinePreviousVersions_Running.stop = true;

        for (int i = 0; i < 10 ; i++) {
            String s = new String("some garbage");
            System.gc();
        }
    }
}
