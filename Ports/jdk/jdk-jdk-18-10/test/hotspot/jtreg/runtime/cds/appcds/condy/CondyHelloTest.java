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
 */

/*
 * @test
 * @summary Make sure CDS works with a minimal test case that uses a CONSTANT_Dynamic constant-pool entry
 * @requires (vm.cds)
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build CondyHello
 * @build sun.hotspot.WhiteBox CondyHelloTest CondyHelloApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar condy_hello.jar CondyHello CondyHelloApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver CondyHelloTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class CondyHelloTest {

    static String classes[] = {
        "CondyHello",
        "CondyHelloApp",
    };

    public static void main(String[] args) throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("condy_hello.jar");

        TestCommon.dump(appJar, TestCommon.list(classes), use_whitebox_jar);

        TestCommon.run("-XX:+UnlockDiagnosticVMOptions",
                       "-XX:+WhiteBoxAPI",
                       "-cp", appJar,
                       use_whitebox_jar,
                       "CondyHelloApp")
          .assertNormalExit();
    }
}
