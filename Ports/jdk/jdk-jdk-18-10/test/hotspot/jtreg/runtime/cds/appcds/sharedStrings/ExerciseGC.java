/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Exercise GC with shared strings
 * @requires vm.cds.archived.java.heap
 * @library /test/hotspot/jtreg/runtime/cds/appcds /test/lib
 * @build HelloStringGC sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver ExerciseGC
 */
public class ExerciseGC {
    public static void main(String[] args) throws Exception {
        SharedStringsUtils.run(args, ExerciseGC::test);
    }
    public static void test(String[] args) throws Exception {
        SharedStringsUtils.buildJarAndWhiteBox("HelloStringGC");

        SharedStringsUtils.dumpWithWhiteBox(TestCommon.list("HelloStringGC"),
            "SharedStringsBasic.txt", "-Xlog:cds,cds+hashtables");

        SharedStringsUtils.runWithArchiveAndWhiteBox("HelloStringGC",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyBeforeGC");
    }
}
