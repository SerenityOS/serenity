/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4870984
 * @summary JPDA: Add support for RFE 4856541 - varargs
 * @comment converted from test/jdk/com/sun/jdi/JdbVarargsTest.sh
 *
 * @library /test/lib
 * @run main/othervm JdbVarargsTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class JdbVarargsTestTarg {

    public static void main(String args[]) {
        int ii = 0; // @1 breakpoint

        // Call the varargs method so the bkpt will be hit
        varString(new String[] {"a", "b"});
    }

    static String varString(String... ss) {
        if (ss == null) {
            return "-null-";
        }
        if (ss.length == 0) {
            return "NONE";
        }
        String retVal = "";
        for (int ii = 0; ii < ss.length; ii++) {
            retVal += ss[ii];
        }
        return retVal;
    }

}

public class JdbVarargsTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbVarargsTest().run();
    }

    private JdbVarargsTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbVarargsTestTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("JdbVarargsTest.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        // check that 'methods' shows the ...
        jdb.command(JdbCommand.methods(DEBUGGEE_CLASS));

        // check that we can call with no args
        jdb.command(JdbCommand.eval(DEBUGGEE_CLASS + ".varString();"));

        // check that we can call with var args
        jdb.command(JdbCommand.eval(DEBUGGEE_CLASS + ".varString(\"aa\", \"bb\");"));

        // check that we can stop in ...
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "varString(java.lang.String...)"));

        jdb.command(JdbCommand.cont());

        new OutputAnalyzer(jdb.getJdbOutput())
                .shouldContain("NONE")
                .shouldContain("aabb")
                .shouldContain(DEBUGGEE_CLASS + " varString(java.lang.String...)")
                .shouldMatch("Breakpoint hit:.*varString\\(\\)");
    }
}
