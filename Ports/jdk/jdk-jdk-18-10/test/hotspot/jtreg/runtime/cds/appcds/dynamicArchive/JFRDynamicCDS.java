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
 * @bug 8268470
 * @summary Test dynamic CDS with JFR recording.
 *          Dynamic dump should skip the class such as jdk/jfr/events/FileReadEvent
 *          if one of its super classes has been redefined during JFR startup.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build JFRDynamicCDSApp sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar jfr_dynamic_cds_app.jar JFRDynamicCDSApp JFRDynamicCDSApp$StressEvent
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. JFRDynamicCDS
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class JFRDynamicCDS extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(JFRDynamicCDS::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("jfr_dynamic_cds_app.jar");
        String mainClass = "JFRDynamicCDSApp";
        dump(topArchiveName,
            "-Xlog:class+load,cds=debug",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldHaveExitValue(0)
                      .shouldMatch("Skipping.jdk/jfr/events.*Has.been.redefined");
            });

        run(topArchiveName,
            "-Xlog:class+load=info",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldHaveExitValue(0)
                      .shouldMatch(".class.load. jdk.jfr.events.*source:.*jrt:/jdk.jfr")
                      .shouldContain("[class,load] JFRDynamicCDSApp source: shared objects file (top)");
            });
    }
}
