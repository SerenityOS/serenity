/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary At run time, it is OK to append new elements to the classpath that was used at dump time.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../test-classes/Hello.java
 * @compile ../test-classes/HelloMore.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. AppendClasspath
 */

import java.io.File;

public class AppendClasspath extends DynamicArchiveTestBase {

    public static void main(String[] args) throws Exception {
        runTest(AppendClasspath::testDefaultBase);
    }

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    private static void doTest(String topArchiveName) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appJar2 = JarBuilder.build("AppendClasspath_HelloMore", "HelloMore");

        // Dump an archive with a specified JAR file in -classpath
        dump(topArchiveName,
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-cp", appJar, "Hello")
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });

        // runtime with classpath containing the one used in dump time,
        // i.e. the dump time classpath is a prefix of the runtime classpath.
        run(topArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar + File.pathSeparator + appJar2,
            "HelloMore")
            .assertNormalExit(output -> {
                    output.shouldContain("Hello source: shared objects file")
                          .shouldContain("Hello World ... More")
                          .shouldHaveExitValue(0);
                });

        // reverse the order of the 2 jar files so that the dump time classpath
        // is no longer a prefix of the runtime classpath. The Hello class
        // should be loaded from the jar file.
        run(topArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar2 + File.pathSeparator + appJar,
            "HelloMore")
            .assertAbnormalExit(output -> {
                    output.shouldContain("shared class paths mismatch")
                          .shouldHaveExitValue(1);
                });

    }
}
