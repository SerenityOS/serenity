/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4070747 4486859
 * @summary Compiler fails to generate local var tbl entry for exception passed to catch
 * @comment converted from test/jdk/com/sun/jdi/GetLocalVariables4Test.sh
 *
 * @library /test/lib
 * @compile -g GetLocalVariables4Test.java
 * @run main/othervm GetLocalVariables4Test
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class GetLocalVariables4Targ {
    public static void main(String[] args) {
        System.out.println("Howdy!");
        int i = 0;
        try {
            i = 16 / i;
        } catch(Exception e) {
            System.out.println("e should be visible");    // @1 breakpoint
        }
        System.out.println("Goodbye from GetLocalVariables4Targ!");
    }
}

public class GetLocalVariables4Test extends JdbTest {
    public static void main(String argv[]) {
        new GetLocalVariables4Test().run();
    }

    private GetLocalVariables4Test() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = GetLocalVariables4Targ.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("GetLocalVariables4Test.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.locals());
        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldContain("e = instance of java.lang.ArithmeticException");
        new OutputAnalyzer(getDebuggeeOutput())
                .shouldContain("Howdy")
                .shouldContain("Goodbye from GetLocalVariables4Targ");
    }
}
