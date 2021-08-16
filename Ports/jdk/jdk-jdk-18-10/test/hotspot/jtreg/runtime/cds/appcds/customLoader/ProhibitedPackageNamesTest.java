/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Make sure prohibited packages cannot be stored into archive for custom loaders.
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../ClassListFormatBase.java ../test-classes/Hello.java test-classes/InProhibitedPkg.java
 * @run driver ProhibitedPackageNamesTest
 */

public class ProhibitedPackageNamesTest extends ClassListFormatBase {
    static {
        // Uncomment the following line to run only one of the test cases
        // ClassListFormatBase.RUN_ONLY_TEST = "TESTCASE PPN1";
    }

    public static void main(String[] args) throws Throwable {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String customJarPath = JarBuilder.build("ProhibitedPackageNames_custom", "java/InProhibitedPkg");

        dumpShouldPass(
            "TESTCASE PPN1: prohibited package name without loader:",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                // Without "loader:" keyword.
                "java/InProhibitedPkg id: 2 super: 1 source: " + customJarPath
            ),
            "Prohibited package for non-bootstrap classes: java/InProhibitedPkg.class");
    }
}
