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
 * @test
 * @summary AppCDS handling of package.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/PackageTest.java
 * @compile test-classes/JimageClassPackage.java
 * @run driver SharedPackages
 */

import jdk.test.lib.process.OutputAnalyzer;

public class SharedPackages {
    public static void main(String[] args) throws Exception {
        JarBuilder.build("pkg", "p/PackageTest", "JimageClassPackage");

        String appJar = TestCommon.getTestJar("pkg.jar");
        TestCommon.testDump(appJar, TestCommon.list("p/PackageTest",
                                                    "java/util/Dictionary",
                                                    "sun/tools/javac/Main",
                                                    "jdk/nio/zipfs/ZipInfo",
                                                    "java/net/URL",
                                                    "com/sun/jndi/dns/DnsName"));

        OutputAnalyzer output;

        // Test 1: shared class from Jar on the -cp
        output = TestCommon.exec(appJar, "-verbose:class", "p.PackageTest");
        TestCommon.checkExec(output, "Expected package");
        if (!TestCommon.isUnableToMap(output))
            output.shouldContain("Package is not sealed");

        // Test 2: shared classes from "modules" jimage
        output = TestCommon.exec(appJar, "-verbose:class",
                                 "JimageClassPackage");
        if (!TestCommon.isUnableToMap(output)) {
            output.shouldNotContain("Unexpected package");
            output.shouldNotContain("Package is not sealed");
        }

        // Test 3: shared class from Jar on the -Xbootclasspath/a
        TestCommon.dump(
            appJar, TestCommon.list("p/PackageTest"), "-Xbootclasspath/a:" + appJar);
        output = TestCommon.exec(appJar, "-Xbootclasspath/a:" + appJar, "p.PackageTest");
        if (!TestCommon.isUnableToMap(output)) {
            output.shouldNotContain("Unexpected package");
            output.shouldContain("Package is not sealed");
        }
    }
}
