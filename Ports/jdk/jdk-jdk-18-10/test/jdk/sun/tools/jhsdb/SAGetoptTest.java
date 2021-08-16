/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary unit test for SAGetopt function
 * @modules jdk.hotspot.agent/sun.jvm.hotspot
 * @compile -XDignore.symbol.file SAGetoptTest.java
 * @run main SAGetoptTest
 */

import sun.jvm.hotspot.SAGetopt;

public class SAGetoptTest {

    private static boolean a_opt;
    private static boolean b_opt;
    private static boolean c_opt;
    private static boolean e_opt;
    private static boolean mixed_opt;

    private static String  d_value;
    private static String  exe_value;
    private static String  core_value;

    private static void initArgValues() {
        a_opt = false;
        b_opt = false;
        c_opt = false;
        e_opt = false;
        mixed_opt = false;

        d_value = "";
        exe_value = "";
        core_value = "";
    }


    private static void optionsTest(String[] args) {
        initArgValues();

        SAGetopt sg = new SAGetopt(args);

        String[] longOpts = {"exe=","core=","mixed"};
        String shortOpts = "abcd:e";
        String s;

        while((s = sg.next(shortOpts, longOpts)) != null) {
            if (s.equals("a")) {
                a_opt = true;
                continue;
            }

            if (s.equals("b")) {
                b_opt = true;
                continue;
            }

            if (s.equals("c")) {
                c_opt = true;
                continue;
            }

            if (s.equals("e")) {
                e_opt = true;
                continue;
            }

            if (s.equals("mixed")) {
                mixed_opt = true;
                continue;
            }

            if (s.equals("d")) {
                d_value = sg.getOptarg();
                continue;
            }

            if (s.equals("exe")) {
                exe_value = sg.getOptarg();
                continue;
            }

            if (s.equals("core")) {
                core_value = sg.getOptarg();
                continue;
            }
        }
    }

    private static void badOptionsTest(int setNumber, String[] args, String expectedMessage) {
        String msg = null;
        try {
            optionsTest(args);
        } catch(RuntimeException ex) {
            msg = ex.getMessage();
        }

        if (msg == null || !msg.equals(expectedMessage)) {
            if (msg != null) {
                System.err.println("Unexpected error '" + msg + "'");
            }
            throw new RuntimeException("Bad option test " + setNumber + " failed");
        }
    }

    public static void main(String[] args) {
        String[] optionSet1 = {"-abd", "bla", "-c"};
        optionsTest(optionSet1);
        if (!a_opt || !b_opt || !d_value.equals("bla") || !c_opt) {
            throw new RuntimeException("Good optionSet 1 failed");
        }

        String[] optionSet2 = {"-e", "--mixed"};
        optionsTest(optionSet2);
        if (!e_opt || !mixed_opt) {
            throw new RuntimeException("Good optionSet 2 failed");
        }

        String[] optionSet3 = {"--exe=bla", "--core", "bla_core", "--mixed"};
        optionsTest(optionSet3);
        if (!exe_value.equals("bla") || !core_value.equals("bla_core") || !mixed_opt) {
            throw new RuntimeException("Good optionSet 3 failed");
        }

        // Bad options test
        String[] optionSet4 = {"-abd", "-c"};
        badOptionsTest(4, optionSet4, "Argument is expected for 'd'");

        String[] optionSet5 = {"-exe", "bla", "--core"};
        badOptionsTest(5, optionSet5, "Invalid option 'x'");

        String[] optionSet6 = {"--exe", "--core", "bla_core"};
        badOptionsTest(6, optionSet6, "Argument is expected for 'exe'");

        String[] optionSet7 = {"--exe"};
        badOptionsTest(7, optionSet7, "Argument is expected for 'exe'");
    }
  }
