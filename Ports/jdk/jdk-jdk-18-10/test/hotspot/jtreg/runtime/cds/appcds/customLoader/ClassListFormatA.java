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
 * @summary Tests the format checking of class list format.
 *
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../test-classes/Hello.java test-classes/CustomLoadee.java test-classes/CustomLoadee2.java
 *          test-classes/CustomInterface2_ia.java test-classes/CustomInterface2_ib.java
 * @run driver ClassListFormatA
 */

public class ClassListFormatA extends ClassListFormatBase {
    static {
        // Uncomment the following line to run only one of the test cases
        // ClassListFormatBase.RUN_ONLY_TEST = "TESTCASE A1";
    }

    public static void main(String[] args) throws Throwable {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String customJarPath = JarBuilder.build("ClassListFormatA", "CustomLoadee",
                                            "CustomLoadee2", "CustomInterface2_ia", "CustomInterface2_ib");
        //----------------------------------------------------------------------
        // TESTGROUP A: general bad input
        //----------------------------------------------------------------------
        dumpShouldFail(
            "TESTCASE A1: bad input - interface: instead of interfaces:",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: 1",
                "CustomLoadee interface: 1"
            ),
            "Unknown input:");

        dumpShouldFail(
            "TESTCASE A2: bad input - negative IDs not allowed",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: -1"
            ),
            "Error: negative integers not allowed");

        dumpShouldFail(
            "TESTCASE A3: bad input - bad ID (not an integer)",
            appJar, classlist(
                "Hello",
                "java/lang/Object id: xyz"
            ),
            "Error: expected integer");

        if (false) {
              // FIXME - classFileParser.cpp needs fixing.
            dumpShouldFail(
                "TESTCASE A4: bad input - bad ID (integer too big)",
                appJar, classlist(
                    "Hello",
                    "java/lang/Object id: 2147483648" // <- this is 0x80000000
                ),
                "Error: expected integer");

              // FIXME
            dumpShouldFail(
                "TESTCASE A5: bad input - bad ID (integer too big)",
                appJar, classlist(
                    "Hello",
                    "java/lang/Object id: 21474836489" // bigger than 32-bit!
                ),
                "Error: expected integer");
        }

        // Good input:
        dumpShouldPass(
            "TESTCASE A6: extraneous spaces, tab characters and trailing new line characters",
            appJar, classlist(
                "Hello   ",                   // trailing spaces
                "java/lang/Object\tid:\t1",   // \t instead of ' '
                "CustomLoadee id: 2 super: 1 source: " + customJarPath,
                "CustomInterface2_ia id: 3 super: 1 source: " + customJarPath + " ",
                "CustomInterface2_ib id: 4 super: 1 source: " + customJarPath + "\t\t\r" ,
                "CustomLoadee2 id: 5 super: 1 interfaces: 3 4 source: " + customJarPath      // preceding spaces
            ));

        int _max_allowed_line = 4096; // Must match ClassListParser::_max_allowed_line in C code.
        int _line_buf_extra = 10;     // Must match ClassListParser::_line_buf_extra in C code.
        StringBuffer sbuf = new StringBuffer();
        for (int i=0; i<_max_allowed_line+1; i++) {
          sbuf.append("x");
        }

        dumpShouldFail(
            "TESTCASE A7: bad input - line too long",
            appJar, classlist(
                sbuf.toString()
            ),
            "input line too long (must be no longer than " + _max_allowed_line + " chars");

        for (int i=0; i<_line_buf_extra + 1000; i++) {
          sbuf.append("X");
        }

        dumpShouldFail(
            "TESTCASE A8: bad input - line too long: try to overflow C buffer",
            appJar, classlist(
                sbuf.toString()
            ),
            "input line too long (must be no longer than " + _max_allowed_line + " chars");
    }
}
