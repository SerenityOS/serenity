/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8232069 for ZGC
 * @requires vm.cds
 * @requires vm.bits == 64
 * @requires vm.gc.Z
 * @requires vm.gc.Serial
 * @requires vm.gc == null
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/Hello.java
 * @run driver TestZGCWithCDS
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

public class TestZGCWithCDS {
    public final static String HELLO = "Hello World";
    public final static String UNABLE_TO_USE_ARCHIVE = "Unable to use shared archive.";
    public final static String ERR_MSG = "The saved state of UseCompressedOops and UseCompressedClassPointers is different from runtime, CDS will be disabled.";
    public static void main(String... args) throws Exception {
         String helloJar = JarBuilder.build("hello", "Hello");
         System.out.println("0. Dump with ZGC");
         OutputAnalyzer out = TestCommon
                                  .dump(helloJar,
                                        new String[] {"Hello"},
                                        "-XX:+UseZGC",
                                        "-Xlog:cds");
         out.shouldContain("Dumping shared data to file:");
         out.shouldHaveExitValue(0);

         System.out.println("1. Run with same args of dump");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseZGC",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(HELLO);
         out.shouldHaveExitValue(0);

         System.out.println("2. Run with +UseCompressedOops +UseCompressedClassPointers");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:-UseZGC",
                         "-XX:+UseCompressedOops",           // in case turned off by vmoptions
                         "-XX:+UseCompressedClassPointers",  // by jtreg
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(UNABLE_TO_USE_ARCHIVE);
         out.shouldContain(ERR_MSG);
         out.shouldHaveExitValue(1);

         System.out.println("3. Run with -UseCompressedOops -UseCompressedClassPointers");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseSerialGC",
                         "-XX:-UseCompressedOops",
                         "-XX:-UseCompressedClassPointers",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(UNABLE_TO_USE_ARCHIVE);
         out.shouldContain(ERR_MSG);
         out.shouldHaveExitValue(1);

         System.out.println("4. Run with -UseCompressedOops +UseCompressedClassPointers");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseSerialGC",
                         "-XX:-UseCompressedOops",
                         "-XX:+UseCompressedClassPointers",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(HELLO);
         out.shouldHaveExitValue(0);

         System.out.println("5. Run with +UseCompressedOops -UseCompressedClassPointers");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseSerialGC",
                         "-XX:+UseCompressedOops",
                         "-XX:-UseCompressedClassPointers",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(UNABLE_TO_USE_ARCHIVE);
         out.shouldContain(ERR_MSG);
         out.shouldHaveExitValue(1);

         System.out.println("6. Run with +UseCompressedOops +UseCompressedClassPointers");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseSerialGC",
                         "-XX:+UseCompressedOops",
                         "-XX:+UseCompressedClassPointers",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(UNABLE_TO_USE_ARCHIVE);
         out.shouldContain(ERR_MSG);
         out.shouldHaveExitValue(1);

         System.out.println("7. Dump with -UseCompressedOops -UseCompressedClassPointers");
         out = TestCommon
                   .dump(helloJar,
                         new String[] {"Hello"},
                         "-XX:+UseSerialGC",
                         "-XX:-UseCompressedOops",
                         "-XX:+UseCompressedClassPointers",
                         "-Xlog:cds");
         out.shouldContain("Dumping shared data to file:");
         out.shouldHaveExitValue(0);

         System.out.println("8. Run with ZGC");
         out = TestCommon
                   .exec(helloJar,
                         "-XX:+UseZGC",
                         "-Xlog:cds",
                         "Hello");
         out.shouldContain(HELLO);
         out.shouldHaveExitValue(0);
    }
}
