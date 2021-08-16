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
 * @summary If the caller class of a lambda proxy class is a hidden class,
 *          the lambda proxy class will not be archived.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @modules java.base/jdk.internal.misc
 * @compile test-classes/LambdaProxyCallerIsHiddenApp.java
 *          ../../../../../../lib/jdk/test/lib/compiler/InMemoryJavaCompiler.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar callerishidden.jar LambdaProxyCallerIsHiddenApp
 *                 jdk/test/lib/compiler/InMemoryJavaCompiler
 *                 jdk/test/lib/compiler/InMemoryJavaCompiler$FileManagerWrapper$1
 *                 jdk/test/lib/compiler/InMemoryJavaCompiler$FileManagerWrapper
 *                 jdk/test/lib/compiler/InMemoryJavaCompiler$MemoryJavaFileObject
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. LambdaProxyCallerIsHidden
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class LambdaProxyCallerIsHidden extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(LambdaProxyCallerIsHidden::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("callerishidden.jar");
        String mainClass = "LambdaProxyCallerIsHiddenApp";

        dump(topArchiveName,
            "-Xlog:class+load,cds+dynamic,cds=debug",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldMatch("Skipping.LambdaHello_0x.*[$][$]Lambda[$].*:.Hidden.class")
                      .shouldMatch("Skipping.LambdaHello.0x.*:.Hidden.class")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-Xlog:class+load,cds+dynamic,cds",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldMatch("class.load.*LambdaHello/0x.*source.*LambdaProxyCallerIsHiddenApp")
                      .shouldMatch("class.load.*LambdaHello_0x.*[$][$]Lambda[$].*source.*LambdaProxyCallerIsHiddenApp")
                      .shouldHaveExitValue(0);
            });
    }
}
