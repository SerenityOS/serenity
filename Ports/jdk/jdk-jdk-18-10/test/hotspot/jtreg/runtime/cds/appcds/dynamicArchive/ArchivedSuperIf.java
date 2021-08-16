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
 * @bug 8261532
 * @summary An archived super interface should be accessible by the class which
 *          implements the interface during runtime although the class itself
 *          is not in the CDS archive.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build sun.hotspot.WhiteBox ArchivedSuperIfApp Bar Baz
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar archived_super_if.jar pkg.ArchivedSuperIfApp pkg.Bar pkg.Baz
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. ArchivedSuperIf
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchivedSuperIf extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(ArchivedSuperIf::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("archived_super_if.jar");
        String mainClass = "pkg.ArchivedSuperIfApp";
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        dump(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:class+load=debug,cds+dynamic=info",
            use_whitebox_jar,
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                // Only the inteface Bar is loaded during dump time.
                output.shouldMatch(".class.load. pkg.Bar source:.*archived_super_if.jar")
                      .shouldNotMatch(".class.load. pkg.Baz source:.*archived_super_if.jar")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            use_whitebox_jar,
            "-Xlog:class+load=debug",
            "-cp", appJar, mainClass, "Baz")
            .assertNormalExit(output -> {
                // The interface Bar will be loaded from the archive.
                // The class (Baz) which implements Bar will be loaded from jar.
                output.shouldContain("[class,load] pkg.Bar source: shared objects file (top)")
                      .shouldMatch(".class.load. pkg.Baz source:.*archived_super_if.jar")
                      .shouldHaveExitValue(0);
            });
    }
}
