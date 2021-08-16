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
 * @bug 4749692
 * @summary REGRESSION: jdb rejects the syntax catch java.lang.IndexOutOfBoundsException
 * @comment converted from test/jdk/com/sun/jdi/CatchAllTest.sh
 *
 * @library /test/lib
 * @build CatchAllTest
 * @run main/othervm CatchAllTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class CatchAllTestTarg {
    public void bar() {
        System.out.println("bar");        // @1 breakpoint
    }

    public static void main(String[] args) {
        CatchAllTestTarg my = new CatchAllTestTarg();
        my.bar();
    }
}

public class CatchAllTest extends JdbTest {
    public static void main(String argv[]) {
        new CatchAllTest().run();
    }

    private CatchAllTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = CatchAllTestTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("CatchAllTest.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        final String IOOB = "java.lang.IndexOutOfBoundsException";
        jdb.command(JdbCommand.catch_(IOOB));
        jdb.command(JdbCommand.catch_(""));
        jdb.command(JdbCommand.ignore(""));
        jdb.command(JdbCommand.ignore(IOOB));
        jdb.command(JdbCommand.catch_(JdbCommand.ExType.all, IOOB));
        jdb.command(JdbCommand.ignore(JdbCommand.ExType.all, IOOB));
        jdb.command(JdbCommand.catch_(JdbCommand.ExType.caught, IOOB));
        jdb.command(JdbCommand.ignore(JdbCommand.ExType.caught, IOOB));
        jdb.command(JdbCommand.catch_(JdbCommand.ExType.uncaught, IOOB));
        jdb.command(JdbCommand.ignore(JdbCommand.ExType.uncaught, IOOB));

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldNotContain("Usage: catch")
                .shouldNotContain("Usage: ignore");
    }

}
