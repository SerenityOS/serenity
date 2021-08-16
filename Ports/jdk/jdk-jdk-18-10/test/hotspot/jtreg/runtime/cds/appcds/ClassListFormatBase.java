/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;

/**
 * Base class for DumpClassListWithLF, customerLoader/ClassListFormat[A,B,C...].java
 */
public class ClassListFormatBase {
    protected static String RUN_ONLY_TEST = null;

    static void dumpShouldFail(String caseHelp, String appJar, String[] appClasses,
                               String... expected_errors) throws Throwable {
        if (RUN_ONLY_TEST != null && !caseHelp.startsWith(RUN_ONLY_TEST)) {
            System.out.println("Skipped via RUN_ONLY_TEST: " + caseHelp);
            return;
        }
        System.out.println("------------------------------");
        System.out.println(caseHelp);
        System.out.println("------------------------------");

        try {
            OutputAnalyzer output = TestCommon.dump(appJar, appClasses, "-Xlog:cds+lambda=debug");
            output.shouldHaveExitValue(1);
            for (String s : expected_errors) {
                output.shouldContain(s);
            }
        } catch (Throwable t) {
            System.out.println("FAILED CASE: " + caseHelp);
            throw t;
        }
    }

    static void dumpShouldPass(String caseHelp, String appJar, String[] appClasses,
                               String... expected_msgs) throws Throwable {
        if (RUN_ONLY_TEST != null && !caseHelp.startsWith(RUN_ONLY_TEST)) {
            System.out.println("Skipped via RUN_ONLY_TEST: " + caseHelp);
            return;
        }
        System.out.println("------------------------------");
        System.out.println(caseHelp);
        System.out.println("------------------------------");

        try {
            OutputAnalyzer output = TestCommon.dump(appJar, appClasses, "-Xlog:cds", "-Xlog:cds+lambda=debug");
            output.shouldHaveExitValue(0);
            output.shouldContain("Dumping");
            for (String s : expected_msgs) {
                output.shouldContain(s);
            }
        } catch (Throwable t) {
            System.out.println("FAILED CASE: " + caseHelp);
            throw t;
        }
    }

    static String[] classlist(String... args) {
        return TestCommon.list(args);
    }
}
