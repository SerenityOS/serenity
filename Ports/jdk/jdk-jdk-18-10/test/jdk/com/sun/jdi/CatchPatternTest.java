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
 * @bug 4671838
 * @summary TTY: surprising ExceptionSpec.resolveEventRequest() wildcard results
 * @comment converted from test/jdk/com/sun/jdi/CatchPatternTest.sh
 *
 * @library /test/lib
 * @build CatchPatternTest
 * @run main/othervm CatchPatternTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class CatchPatternTestTarg {
    public void bark(int i) {
        System.out.println(" bark: " + i);
        switch (i) {
        case 0:
            throw new IllegalArgumentException("IllegalArgumentException");
        case 1:
            throw new ArithmeticException("ArithmeticException");
        case 2:
            throw new IllegalMonitorStateException("IllegalMonitorStateException");
        case 3:
            throw new IndexOutOfBoundsException("IndexOutOfBoundsException");
        default:
            throw new Error("should not happen");
        }
    }
    public void loop(int max) {
        for (int i = 0; i <= max; i++) {
            try {
                bark(i);
            } catch(RuntimeException re) {
                System.out.println(" loop: " + re.getMessage() +
                       " caught and ignored.");
            }
        }
    }
    public void partOne() {
        loop(2);
        System.out.println("partOne completed");
    }
    public void partTwo() {
        loop(3);
        System.out.println("partTwo completed");
    }
    public static void main(String[] args) {
        System.out.println("Howdy!");
        CatchPatternTestTarg my = new CatchPatternTestTarg();
        my.partOne();
        my.partTwo();
        System.out.println("Goodbye from CatchPatternTestTarg!");
    }
}

public class CatchPatternTest extends JdbTest {
    public static void main(String argv[]) {
        new CatchPatternTest().run();
    }

    private CatchPatternTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = CatchPatternTestTarg.class.getName();

    @Override
    protected void runCases() {
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "main"));
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "partTwo"));
        jdb.command(JdbCommand.run());

        jdb.command(JdbCommand.ignore(JdbCommand.ExType.uncaught, "java.lang.Throwable"));
        // Instead of matching java.lang.I* we use two more specific
        // patterns here. The reason is to avoid matching IncompatibleClassChangeError
        // (or the subclass NoSuchMethodError) thrown by the
        // java.lang.invoke infrastructure.
        jdb.command(JdbCommand.catch_(JdbCommand.ExType.all, "java.lang.Il*"));
        jdb.command(JdbCommand.catch_(JdbCommand.ExType.all, "java.lang.Ind*"));
        jdb.command(JdbCommand.cont());
        jdb.command(JdbCommand.cont());
        jdb.command(JdbCommand.cont());
        jdb.command(JdbCommand.ignore(JdbCommand.ExType.all, "java.lang.Ind*"));
        jdb.command(JdbCommand.ignore(JdbCommand.ExType.all, "java.lang.Il*"));

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldContain("Exception occurred: java.lang.IllegalArgumentException")
                .shouldContain("Exception occurred: java.lang.IllegalMonitorStateException")
                .shouldNotContain("Exception occurred: ArithmeticException")
                .shouldNotContain("Exception occurred: IndexOutOfBoundsException")
                .shouldNotContain("Exception occurred: IllegalStateException")
                .shouldNotContain("should not happen");
        new OutputAnalyzer(getDebuggeeOutput())
                .shouldContain("partOne completed")
                .shouldContain("partTwo completed");
    }

}
