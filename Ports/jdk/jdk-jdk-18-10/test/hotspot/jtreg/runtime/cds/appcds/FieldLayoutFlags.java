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
 * @summary VM should work even if different field layout options are chosen between dump time and run time.
 * @bug 8233086
 * @requires vm.cds
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.vm.annotation
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build FieldLayoutApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar field_layout.jar
 *     FieldLayoutApp
 *     FieldLayoutApp$TestObject
 *     FieldLayoutApp$Base1
 *     FieldLayoutApp$Base2
 *     FieldLayoutApp$Child1
 *     FieldLayoutApp$Child2
 * @run driver FieldLayoutFlags
 */

import jdk.test.lib.Platform;
import jdk.test.lib.helpers.ClassFileInstaller;

public class FieldLayoutFlags {
    static final String[][] flags = {
        // Dump time                             // Run time
        {},                                      {},   // All defaults. Ensure that the test itself is correct.
        {"-XX:+EnableContended"},                {"-XX:-EnableContended"},
        {"-XX:-EnableContended"},                {"-XX:+EnableContended"},

        {"-XX:ContendedPaddingWidth=128"},       {"-XX:ContendedPaddingWidth=256"},
        {"-XX:ContendedPaddingWidth=256"},       {"-XX:ContendedPaddingWidth=128"},
    };

    static final String appJar = ClassFileInstaller.getJarPath("field_layout.jar");

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < flags.length; i += 2) {
            String[] dumpFlags = flags[i+0];
            String[] runFlags  = flags[i+1];

            System.out.println("====================================================== Cases " + i + " and " + (i + 1));
            logFlags("Dump:", dumpFlags);
            logFlags("Run :", runFlags);

            testDump(dontRestrict(dumpFlags));
            testRun (dontRestrict(runFlags));
        }
    }

    static void logFlags(String which, String[] flags) {
        System.out.print(which);
        String prefix = " ";
        for (String s : flags) {
            System.out.print(prefix);
            System.out.print(s);
            prefix = ", ";
        }
        System.out.println();
    }

    // Don't restrict @Contended to trusted classes, so we can use it in FieldLayoutApp
    static String[] dontRestrict(String args[]) {
        return TestCommon.concat("-XX:-RestrictContended", args);
    }

    static void testDump(String[] dumpFlags) throws Exception {
        String classlist[] = new String[] {
            "FieldLayoutApp",
            "FieldLayoutApp$TestObject",
            "FieldLayoutApp$Base1",
            "FieldLayoutApp$Base2",

            /*
             * Note, the following classes are not archived, and will be loaded
             * dynamically at run time. We check that their field layout is compatible with
             * their super classes, which are archived.
             */
            // "FieldLayoutApp$Child1",
            // "FieldLayoutApp$Child2",
        };

        TestCommon.testDump(appJar, classlist, dumpFlags);
    }

    static void testRun(String[] runFlags) throws Exception {
        String[] cmds = TestCommon.concat(runFlags, "-cp", appJar);
        if (Platform.isDebugBuild()) {
            cmds = TestCommon.concat(cmds, "-XX:+PrintFieldLayout");
        }
        cmds = TestCommon.concat(cmds, "FieldLayoutApp");

        TestCommon.run(cmds).assertNormalExit();
    }
}
