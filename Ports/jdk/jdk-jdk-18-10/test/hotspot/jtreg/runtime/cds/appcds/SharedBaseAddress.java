/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test SharedBaseAddress
 * @summary Test variety of values for SharedBaseAddress, in AppCDS mode,
 *          making sure VM handles normal values as well as edge values
 *          w/o a crash.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run main/timeout=240 SharedBaseAddress
 */

import jdk.test.lib.process.OutputAnalyzer;

public class SharedBaseAddress {

    // shared base address test table
    private static final String[] testTable = {
        "1g", "8g", "64g","512g", "4t",
        "32t", "128t", "0",
        "1", "64k", "64M", "320g",
        "0x800001000"  // Default base address + 1 page - probably valid but unaligned to metaspace alignment, see JDK 8247522
    };

    public static void main(String[] args) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();

        for (String testEntry : testTable) {
            System.out.println("sharedBaseAddress = " + testEntry);

            // Note: some platforms may restrict valid values for SharedBaseAddress; the VM should print
            // a warning and use the default value instead. Similar, ASLR may prevent the given address
            // from being used; this too should handled gracefully by using the default base address.
            OutputAnalyzer dumpOutput = TestCommon.dump(
                appJar, new String[] {"Hello"}, "-XX:SharedBaseAddress=" + testEntry);
            TestCommon.checkDump(dumpOutput, "Loading classes to share");

            OutputAnalyzer execOutput = TestCommon.exec(appJar, "Hello");
            TestCommon.checkExec(execOutput, "Hello World");
        }
    }
}
