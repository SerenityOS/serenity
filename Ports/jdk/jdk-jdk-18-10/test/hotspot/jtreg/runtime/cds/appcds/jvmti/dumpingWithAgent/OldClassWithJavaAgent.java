/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8261090
 * @summary Dump old class with java agent.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds
 * @requires vm.jvmti
 * @compile ../../test-classes/OldSuper.jasm
 * @compile SimpleAgent.java
 * @run main/othervm OldClassWithJavaAgent
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class OldClassWithJavaAgent {
    public static String appClasses[] = {"OldSuper"};
    public static String agentClasses[] = {"SimpleAgent"};
    public static String diagnosticOption = "-XX:+AllowArchivingWithJavaAgent";
    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("SimpleAgent.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("SimpleAgent.mf"),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("OldClassWithJavaAgent.jar", appClasses);
        OutputAnalyzer output = TestCommon.testDump(appJar, TestCommon.list("OldSuper"),
            "-Xlog:cds=debug,class+load",
            "-XX:+UnlockDiagnosticVMOptions", diagnosticOption,
            "-javaagent:" + agentJar + "=OldSuper");

        // The java agent will load and link the class. We will skip old classes
        // which have been linked during CDS dump.
        output.shouldContain("Skipping OldSuper: Old class has been linked");
    }
}
