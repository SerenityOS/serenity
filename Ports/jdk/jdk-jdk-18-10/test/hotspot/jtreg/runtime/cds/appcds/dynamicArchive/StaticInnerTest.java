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
 * @summary Outer class is directly referenced during dump time but not during
 *          runtime. This test makes sure the nest host of a lambda proxy class
 *          could be loaded from the archive during runtime though it isn't being
 *          referenced directly.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build StaticInnerApp sun.hotspot.WhiteBox LambdaVerification
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar static_inner_app.jar StaticInnerApp HelloStaticInner HelloStaticInner$InnerHello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. StaticInnerTest
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class StaticInnerTest extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(StaticInnerTest::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("static_inner_app.jar");
        String mainClass = "StaticInnerApp";

        dump(topArchiveName,
            "-Xlog:class+load=info,class+nestmates=trace,cds+dynamic=info",
            "-cp", appJar, mainClass, "dump")
            .assertNormalExit(output -> {
                output.shouldContain("Archiving hidden HelloStaticInner$InnerHello$$Lambda$")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-Xlog:class+load=info",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                output.shouldHaveExitValue(0)
                      .shouldContain("HelloStaticInner source: shared objects file (top)")
                      .shouldMatch(".class.load. HelloStaticInner[$]InnerHello[$][$]Lambda[$].*/0x.*source:.*shared.*objects.*file.*(top)");
            });
    }
}
