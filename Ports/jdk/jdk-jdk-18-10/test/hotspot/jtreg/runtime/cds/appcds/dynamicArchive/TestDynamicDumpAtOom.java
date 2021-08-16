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
 * @bug 8266764
 * @summary test dynamic dump with OOM
 * @requires vm.cds
 * @requires vm.gc.Serial & vm.gc == null
 * @comment Test dynamic dump at OOM, currently only works with SerialGC
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @compile ./test-classes/MiniStoreOom.java
 * @build LambHello sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar ministore.jar MiniStoreOom
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. TestDynamicDumpAtOom
 */

import jdk.test.lib.helpers.ClassFileInstaller;
import jdk.test.lib.cds.CDSTestUtils.Result;

public class TestDynamicDumpAtOom extends DynamicArchiveTestBase {
    private static final String mainClass = "MiniStoreOom";
    private static final String jarFile   = "ministore.jar";
    private static void doTest(String topArchiveName) throws Exception {
        dump(topArchiveName,
             "-Xmx64M",
             "-XX:+UseSerialGC",
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-cp",
             jarFile,
             mainClass,
             "1024").assertAbnormalExit(output -> {
                 output.shouldContain("Dynamic dump has failed")
                       .shouldContain("java.lang.OutOfMemoryError: Java heap space");
             });
    }

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    public static void main(String[] args) throws Exception {
        runTest(TestDynamicDumpAtOom::testDefaultBase);
    }
}
