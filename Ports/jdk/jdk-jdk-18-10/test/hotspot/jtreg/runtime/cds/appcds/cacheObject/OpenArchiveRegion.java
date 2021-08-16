/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test open archive heap regions
 * @requires vm.cds.archived.java.heap
 * @comment This test explicitly chooses the type of GC to be used by sub-processes. It may conflict with the GC type set
 * via the -vmoptions command line option of JTREG. vm.gc==null will help the test case to discard the explicitly passed
 * vm options.
 * @requires (vm.gc=="null")
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../test-classes/Hello.java
 * @run driver OpenArchiveRegion
 */

import jdk.test.lib.process.OutputAnalyzer;

public class OpenArchiveRegion {
    public static void main(String[] args) throws Exception {
        JarBuilder.getOrCreateHelloJar();
        String appJar = TestCommon.getTestJar("hello.jar");
        String appClasses[] = TestCommon.list("Hello");

        // Dump with open archive heap region, requires G1 GC
        OutputAnalyzer output = TestCommon.dump(appJar, appClasses, "-Xlog:cds=debug");
        TestCommon.checkDump(output, "oa0 space:");
        output.shouldNotContain("oa0 space:         0 [");
        output = TestCommon.exec(appJar, "Hello");
        TestCommon.checkExec(output, "Hello World");
        output = TestCommon.exec(appJar, "-XX:+UseSerialGC", "Hello");
        TestCommon.checkExec(output, "Hello World");

        // Dump with open archive heap region disabled when G1 GC is not in use
        output = TestCommon.dump(appJar, appClasses, "-XX:+UseParallelGC");
        TestCommon.checkDump(output);
        output.shouldNotContain("oa0 space:");
        output = TestCommon.exec(appJar, "Hello");
        TestCommon.checkExec(output, "Hello World");
    }
}
