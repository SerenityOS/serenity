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
 *
 */

/*
 * @test
 * @summary Run /serviceability/jvmti/RedefineClasses/RedefineRunningMethods in AppCDS mode to
 *          make sure class redefinition works with CDS.
 * @requires vm.cds
 * @requires vm.jvmti
 * @library /test/lib /test/hotspot/jtreg/serviceability/jvmti/RedefineClasses /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver RedefineClassHelper
 * @build sun.hotspot.WhiteBox RedefineRunningMethods_SharedHelper
 * @run driver RedefineRunningMethods_Shared
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefineRunningMethods_Shared {
    public static String shared_classes[] = {
        "RedefineRunningMethods_Shared",
        "RedefineRunningMethods_SharedHelper",
        "RedefineRunningMethods",
        "RedefineRunningMethods$1",
        "RedefineRunningMethods$2",
        "RedefineRunningMethods$3",
        "RedefineRunningMethods_B",
        "RedefineClassHelper",
        "jdk/test/lib/compiler/InMemoryJavaCompiler",
        "jdk/test/lib/compiler/InMemoryJavaCompiler$FileManagerWrapper",
        "jdk/test/lib/compiler/InMemoryJavaCompiler$FileManagerWrapper$1",
        "jdk/test/lib/compiler/InMemoryJavaCompiler$MemoryJavaFileObject"
    };

    public static void main(String[] args) throws Exception {
        String wbJar = ClassFileInstaller.writeJar("WhiteBox.jar", "sun.hotspot.WhiteBox");
        String appJar = ClassFileInstaller.writeJar("RedefineRunningMethods_Shared.jar", shared_classes);
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        OutputAnalyzer output;
        TestCommon.testDump(appJar, shared_classes,
                            // command-line arguments ...
                            use_whitebox_jar);

        // RedefineRunningMethods.java contained this:
        // @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+iklass+add=trace,redefine+class+iklass+purge=trace RedefineRunningMethods
        output = TestCommon.exec(appJar,
                                 // command-line arguments ...
                                 use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 // These arguments are expected by RedefineRunningMethods
                                 "-javaagent:redefineagent.jar",
                                 "-Xlog:redefine+class+iklass+add=trace,redefine+class+iklass+purge=trace",
                                 "RedefineRunningMethods_SharedHelper");
        TestCommon.checkExec(output);
    }
}
