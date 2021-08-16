/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Redefine shared class. GC should not cause crash with cached resolved_references.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes /test/hotspot/jtreg/runtime/cds/appcds/jvmti
 * @requires vm.cds.archived.java.heap
 * @requires vm.jvmti
 * @build sun.hotspot.WhiteBox
 *        RedefineClassApp
 *        InstrumentationClassFileTransformer
 *        InstrumentationRegisterClassFileTransformer
 * @run driver RedefineClassTest
 */

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.File;
import java.util.List;
import jdk.test.lib.Asserts;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefineClassTest {
    public static String bootClasses[] = {
        "RedefineClassApp$Intf",
        "RedefineClassApp$Bar",
        "sun.hotspot.WhiteBox",
    };
    public static String appClasses[] = {
        "RedefineClassApp",
        "RedefineClassApp$Foo",
    };
    public static String sharedClasses[] = TestCommon.concat(bootClasses, appClasses);

    public static String agentClasses[] = {
        "InstrumentationClassFileTransformer",
        "InstrumentationRegisterClassFileTransformer",
        "Util",
    };

    public static void main(String[] args) throws Throwable {
        runTest();
    }

    public static void runTest() throws Throwable {
        String bootJar =
            ClassFileInstaller.writeJar("RedefineClassBoot.jar", bootClasses);
        String appJar =
            ClassFileInstaller.writeJar("RedefineClassApp.jar", appClasses);
        String agentJar =
            ClassFileInstaller.writeJar("InstrumentationAgent.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("InstrumentationAgent.mf"),
                                        agentClasses);

        String bootCP = "-Xbootclasspath/a:" + bootJar;

        String agentCmdArg;
        agentCmdArg = "-javaagent:" + agentJar;

        TestCommon.testDump(appJar, sharedClasses, bootCP, "-Xlog:gc+region=trace");

        OutputAnalyzer out = TestCommon.execAuto("-cp", appJar,
                bootCP,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-Xlog:cds=info",
                agentCmdArg,
               "RedefineClassApp", bootJar, appJar);
        out.reportDiagnosticSummary();

        CDSOptions opts = (new CDSOptions()).setXShareMode("auto");
        TestCommon.checkExec(out, opts);
    }
}

