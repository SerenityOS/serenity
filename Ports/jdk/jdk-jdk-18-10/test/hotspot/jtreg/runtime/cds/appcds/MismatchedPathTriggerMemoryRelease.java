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
 * @test MismatchedPathTriggerMemoryRelease
 * @summary Mismatched path at runtime will cause reserved memory released
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run main MismatchedPathTriggerMemoryRelease
 */

import jdk.test.lib.process.OutputAnalyzer;

public class MismatchedPathTriggerMemoryRelease {
    private static String ERR_MSGS[] = {
        "UseSharedSpaces: shared class paths mismatch (hint: enable -Xlog:class+path=info to diagnose the failure)",
        "UseSharedSpaces: Unable to map shared spaces"};
    private static String RELEASE_SPACE_MATCH = "Released shared space .* 0(x|X)[0-9a-fA-F]+$";
    private static String OS_RELEASE_MSG = "os::release_memory failed";

    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();

        OutputAnalyzer dumpOutput = TestCommon.dump(
            appJar, new String[] {"Hello"}, "-XX:SharedBaseAddress=0");
        TestCommon.checkDump(dumpOutput, "Loading classes to share");

        // Normal exit
        OutputAnalyzer execOutput = TestCommon.exec(appJar, "Hello");
        TestCommon.checkExec(execOutput, "Hello World");

        // mismatched jar
        execOutput = TestCommon.exec("non-exist.jar",
                                     "-Xshare:auto",
                                     "-Xlog:os,cds=debug",
                                     "-XX:NativeMemoryTracking=detail",
                                     "-XX:SharedBaseAddress=0",
                                     "Hello");
        execOutput.shouldHaveExitValue(1);
        for (String err : ERR_MSGS) {
            execOutput.shouldContain(err);
        }
        execOutput.shouldMatch(RELEASE_SPACE_MATCH);
        execOutput.shouldNotContain(OS_RELEASE_MSG); // os::release only log release failed message
    }
}
