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
 * @run driver ClassListFormatE
 */

public class ClassListFormatE extends ClassListFormatBase {
    static {
        // Uncomment the following line to run only one of the test cases
        // ClassListFormatBase.RUN_ONLY_TEST = "TESTCASE E1";
    }

    public static void main(String[] args) throws Throwable {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String customJarPath = JarBuilder.build("ClassListFormatE", "CustomLoadee",
                                                "CustomLoadee2", "CustomInterface2_ia",
                                                "CustomInterface2_ib");

        //----------------------------------------------------------------------
        // TESTGROUP E: super class and interfaces
        //----------------------------------------------------------------------
        dumpShouldFail(
            "TESTCASE E1: missing interfaces: keyword",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomLoadee2 id: 1 super: 1 source: " + customJarPath
            ),
            "Class CustomLoadee2 implements the interface CustomInterface2_ia, but no interface has been specified in the input line");

        dumpShouldFail(
            "TESTCASE E2: missing one interface",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomInterface2_ia id: 2 super: 1 source: " + customJarPath,
                "CustomInterface2_ib id: 3 super: 1 source: " + customJarPath,
                "CustomLoadee2 id: 4 super: 1 interfaces: 2 source: " + customJarPath
            ),
            "The interface CustomInterface2_ib implemented by class CustomLoadee2 does not match any of the specified interface IDs");

        dumpShouldFail(
            "TESTCASE E3: specifying an interface that's not implemented by the class",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomInterface2_ia id: 2 super: 1 source: " + customJarPath,
                "CustomLoadee id: 2 super: 1 interfaces: 2 source: " + customJarPath
            ),
            "The number of interfaces (1) specified in class list does not match the class file (0)");

        dumpShouldFail(
            "TESTCASE E4: repeating an ID in the interfaces: keyword",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomInterface2_ia id: 2 super: 1 source: " + customJarPath,
                "CustomInterface2_ib id: 3 super: 1 source: " + customJarPath,
                "CustomLoadee2 id: 4 super: 1 interfaces: 2 2 3 source: " + customJarPath
            ),
            "The number of interfaces (3) specified in class list does not match the class file (2)");

        dumpShouldFail(
            "TESTCASE E5: wrong super class",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomInterface2_ia id: 2 super: 1 source: " + customJarPath,
                "CustomInterface2_ib id: 3 super: 1 source: " + customJarPath,
                "CustomLoadee id: 4 super: 1 source: " + customJarPath,
                "CustomLoadee2 id: 5 super: 4 interfaces: 2 3 source: " + customJarPath
            ),
            "The specified super class CustomLoadee (id 4) does not match actual super class java.lang.Object");
    }
}
