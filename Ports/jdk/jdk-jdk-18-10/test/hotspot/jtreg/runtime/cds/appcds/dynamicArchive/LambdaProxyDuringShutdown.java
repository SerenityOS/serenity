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
 * @bug 8261860
 * @summary VM should not crash if a lambda proxy class is created during
 *          shutdown and its nest host is not linked.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build LambdaProxyDuringShutdownApp sun.hotspot.WhiteBox LambdaVerification
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar lambda_proxy_shutdown.jar LambdaVerification
 *             LambdaProxyDuringShutdownApp MyShutdown Outer Outer$Inner
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. LambdaProxyDuringShutdown
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class LambdaProxyDuringShutdown extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(LambdaProxyDuringShutdown::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("lambda_proxy_shutdown.jar");
        String mainClass = "LambdaProxyDuringShutdownApp";
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appOutput = "Hello from Inner";

        dump(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:class+load=debug,cds=debug,cds+dynamic=info",
            use_whitebox_jar,
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                // Nest host should not be skipped although it is not in the linked state.
                output.shouldNotContain("Skipping Outer: Not linked")
                // Lambda proxy is loaded normally.
                      .shouldMatch("class.load.*Outer[$]Inner[$][$]Lambda[$].*0x.*source:.Outer")
                      .shouldContain(appOutput)
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            use_whitebox_jar,
            "-Xlog:class+load=debug",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                // Only the nest host (Outer) and the Inner class are loaded
                // from the dynamic archive.
                // The lambda proxy is not loaded from the dynamic archive.
                output.shouldMatch("class.load.*Outer.source:.*shared.*objects.*file.*(top)")
                      .shouldMatch("class.load.*Outer[$]Inner[$][$]Lambda[$].*0x.*source:.Outer")
                      .shouldMatch("class.load. Outer[$]Inner.source:.*shared.*objects.*file.*(top)")
                      .shouldContain(appOutput)
                      .shouldHaveExitValue(0);
            });
    }
}
