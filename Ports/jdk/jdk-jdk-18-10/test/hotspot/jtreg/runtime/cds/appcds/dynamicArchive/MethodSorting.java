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
 * @summary When HelloA and HelloB are copied into the dynamic archive, the Symbols
 *          for their method's names will have a different sorting order. This requires
 *          that the dumped InstanceKlass to re-sort their "methods" array and re-layout the vtables/itables.
 *          A regression test for an earlier bug in DynamicArchiveBuilder::relocate_buffer_to_target().
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build MethodSortingApp
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar method_sorting.jar
 *             MethodSortingApp
 *             MethodSortingApp$HelloA
 *             MethodSortingApp$HelloA1
 *             MethodSortingApp$HelloB
 *             MethodSortingApp$HelloB1
 *             MethodSortingApp$InterfaceA
 *             MethodSortingApp$InterfaceB
 *             MethodSortingApp$ImplementorA
 *             MethodSortingApp$ImplementorA1
 *             MethodSortingApp$ImplementorB
 *             MethodSortingApp$ImplementorB1
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. MethodSorting
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class MethodSorting extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(MethodSorting::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("method_sorting.jar");
        String mainClass = "MethodSortingApp";

        dumpAndRun(topArchiveName, "-Xlog:cds+dynamic=debug", "-cp", appJar, mainClass);
    }
}
