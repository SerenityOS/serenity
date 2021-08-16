/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8054386
 * @summary java debugging test for CDS
 * @requires vm.cds
 * @modules jdk.jdi
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @library /test/lib
 * @library ..
 * @build TestScaffold VMConnection TargetListener TargetAdapter
 * @build CDSJDITest
 * @run compile -g ../FieldWatchpoints.java
 * @run main CDSFieldWatchpoints
 */

/*
 * Launch the JDI FieldWatchpoints test, which will setup field watchpoints in
 * FieldWatchpointsDebugee. FieldWatchpointsDebugee is first dumped into the
 * CDS archive, so this will test debugging a class in the archive.
 */

public class CDSFieldWatchpoints extends CDSJDITest {
    static String jarClasses[] = {
        // FieldWatchpointsDebugee. A, and B are the only classes we need in the archive.
        // FieldWatchpointsDebugee will be launched by FieldWatchpoints as the debuggee
        // application. Note, compiling FieldWatchpoints.java above will cause
        // FieldWatchpointsDebugee to be compiled since it is also in FieldWatchpoints.java.
        "FieldWatchpointsDebugee", "A", "B",
    };
    static String testname = "FieldWatchpoints";

    public static void main(String[] args) throws Exception {
        runTest(testname, jarClasses);
    }
}
