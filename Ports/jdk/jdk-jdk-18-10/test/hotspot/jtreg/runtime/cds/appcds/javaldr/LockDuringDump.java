/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8249276
 * @summary When dumping the CDS archive, try to lock some objects. These objects should be archived
 *          without the locking bits in the markWord.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @requires vm.cds
 * @requires vm.jvmti
 * @modules java.instrument
 * @run driver LockDuringDump
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class LockDuringDump {
    public static String appClasses[] = {
        LockDuringDumpApp.class.getName(),
    };
    public static String agentClasses[] = {
        LockDuringDumpAgent.class.getName(),
    };

    private static final String MANIFEST =
        "Manifest-Version: 1.0\nPremain-Class: LockDuringDumpAgent\n";

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("LockDuringDumpAgent.jar",
                                        ClassFileInstaller.Manifest.fromString(MANIFEST),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("LockDuringDumpApp.jar", appClasses);

        for (int i = 0; i < 3; i++) {
            // i = 0 -- dump without agent
            // i = 1 -- dump with agent

            String agentArg   = (i == 0) ? "-showversion" : "-javaagent:" + agentJar;
            String agentArg2  = (i == 0) ? "-showversion" : "-XX:+AllowArchivingWithJavaAgent";

            OutputAnalyzer out =
                TestCommon.testDump(appJar, TestCommon.list(LockDuringDumpApp.class.getName()),
                                    "-XX:+UnlockDiagnosticVMOptions",
                                    agentArg, agentArg2);
            if (i != 0) {
                out.shouldContain("Let's hold the lock on the literal string");
            }

            TestCommon.run(
                "-cp", appJar,
                "-XX:+UnlockDiagnosticVMOptions", agentArg2,
                LockDuringDumpApp.class.getName())
              .assertNormalExit("I am able to lock the literal string");
        }
    }
}

