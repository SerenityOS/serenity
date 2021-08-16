/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary CDS dumping with java agent.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds
 * @requires vm.jvmti
 * @build SimpleAgent Hello
 * @run main/othervm DumpingWithJavaAgent
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class DumpingWithJavaAgent {
    public static String appClasses[] = {
        "Hello",
    };
    public static String agentClasses[] = {
        "SimpleAgent",
    };

    public static String warningMessages[] = {
        "This archive was created with AllowArchivingWithJavaAgent",
        "It should be used for testing purposes only and should not be used in a production environment",
    };

    public static String errorMessage =
        "The setting of the AllowArchivingWithJavaAgent is different from the setting in the shared archive.";


    public static String diagnosticOption = "-XX:+AllowArchivingWithJavaAgent";

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("SimpleAgent.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("SimpleAgent.mf"),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("DumpingWithJavaAgent.jar", appClasses);

        // CDS dumping with a java agent with the AllowArchvingWithJavaAgent diagnostic option.
        OutputAnalyzer output = TestCommon.testDump(appJar, TestCommon.list("Hello"),
            "-XX:+UnlockDiagnosticVMOptions", diagnosticOption,
            "-javaagent:" + agentJar);
        TestCommon.checkDump(output);
        output.shouldContain(warningMessages[0]);
        output.shouldContain(warningMessages[1]);
        output.shouldContain("inside SimpleAgent");

        // Using the archive with the AllowArchvingWithJavaAgent diagnostic option.
        output = TestCommon.exec(
            appJar,
            "-Xlog:class+load=trace",
            "-XX:+UnlockDiagnosticVMOptions", diagnosticOption,
            "Hello");
        if (!TestCommon.isUnableToMap(output)) {
            output.shouldHaveExitValue(0);
            output.shouldContain(warningMessages[0]);
            output.shouldContain(warningMessages[1]);
            output.shouldContain("[class,load] Hello source: shared objects file");
        }

        // Using the archive with -Xshare:on without the diagnostic option.
        // VM should exit with an error message.
        output = TestCommon.exec(
            appJar,
            "Hello");
        output.shouldHaveExitValue(1);
        output.shouldContain(errorMessage);

        // Using the archive with -Xshare:auto without the diagnostic option.
        // VM should continue execution with a warning message. The archive
        // will not be used.
        output = TestCommon.execAuto(
            "-cp", appJar,
            "-Xlog:class+load=trace,cds=info",
            "Hello");
        if (!TestCommon.isUnableToMap(output)) {
            output.shouldHaveExitValue(0);
            output.shouldContain(errorMessage);
            output.shouldMatch(".class.load.* Hello source:.*DumpingWithJavaAgent.jar");

        // CDS dumping with a java agent without the AllowArchvingWithJavaAgent diagnostic option.
        // VM will exit with an error message.
        output = TestCommon.dump(appJar, TestCommon.list("Hello"),
            "-javaagent:" + agentJar);
        }
        output.shouldContain("Must enable AllowArchivingWithJavaAgent in order to run Java agent during CDS dumping")
            .shouldHaveExitValue(1);
    }
}
