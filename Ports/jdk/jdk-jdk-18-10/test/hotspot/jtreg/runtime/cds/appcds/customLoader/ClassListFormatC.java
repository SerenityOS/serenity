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
 * @summary Tests the format checking of hotspot/src/closed/share/vm/classfile/classListParser.cpp.
 *
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../test-classes/Hello.java test-classes/CustomLoadee.java test-classes/CustomLoadee2.java
 *          test-classes/CustomInterface2_ia.java test-classes/CustomInterface2_ib.java
 * @run driver ClassListFormatC
 */

public class ClassListFormatC extends ClassListFormatBase {
    static {
        // Uncomment the following line to run only one of the test cases
        // ClassListFormatBase.RUN_ONLY_TEST = "TESTCASE C1";
    }

    public static void main(String[] args) throws Throwable {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String customJarPath = JarBuilder.build("ClassListFormatC", "CustomLoadee",
                                                "CustomLoadee2", "CustomInterface2_ia",
                                                "CustomInterface2_ib");

        //----------------------------------------------------------------------
        // TESTGROUP C: if source IS NOT specified
        //----------------------------------------------------------------------
        dumpShouldFail(
            "TESTCASE C1: if source: is NOT specified, must NOT specify super:",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomLoadee super: 1"
            ),
            "If source location is not specified, super class must not be specified");

        dumpShouldFail(
            "TESTCASE C2: if source: is NOT specified, must NOT specify interface:",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomLoadee interfaces: 1"
            ),
            "If source location is not specified, interface(s) must not be specified");
    }
}
