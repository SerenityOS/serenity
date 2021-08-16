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
 * @bug 4422141 4695338
 * @summary TTY: .length field for arrays in print statements in jdb not recognized
 *          TTY: dump <ArrayReference> command not implemented.
 * @comment converted from test/jdk/com/sun/jdi/ArrayLengthDumpTest.sh
 *
 * @library /test/lib
 * @build ArrayLengthDumpTest
 * @run main/othervm ArrayLengthDumpTest
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


class ArrayLengthDumpTarg {
    static final int [] i = {0,1,2,3,4,5,6};
    String [] s = {"zero", "one", "two", "three", "four"};
    String [][] t = {s, s, s, s, s, s, s, s, s, s, s};
    int length = 5;

    private void bar() {
    }

    private void foo() {
        ArrayLengthDumpTarg u[] = { new ArrayLengthDumpTarg(),
                                    new ArrayLengthDumpTarg(),
                                    new ArrayLengthDumpTarg(),
                                    new ArrayLengthDumpTarg(),
                                    new ArrayLengthDumpTarg(),
                                    new ArrayLengthDumpTarg() };
        int k = u.length;
        System.out.println("        u.length is: " + k);
        k = this.s.length;
        System.out.println("   this.s.length is: " + k);
        k = this.t.length;
        System.out.println("   this.t.length is: " + k);
        k = this.t[1].length;
        System.out.println("this.t[1].length is: " + k);
        k = i.length;
        System.out.println("        i.length is: " + k);
        bar();                      // @1 breakpoint
    }

    public static void main(String[] args) {
        ArrayLengthDumpTarg my = new ArrayLengthDumpTarg();
        my.foo();
    }
}

public class ArrayLengthDumpTest extends JdbTest {
    public static void main(String argv[]) {
        new ArrayLengthDumpTest().run();
    }

    public ArrayLengthDumpTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = ArrayLengthDumpTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("ArrayLengthDumpTest.java", 1);

        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        List<String> reply = new LinkedList<>();
        reply.addAll(jdb.command(JdbCommand.dump("this")));
        reply.addAll(jdb.command(JdbCommand.dump("this.s.length")));
        reply.addAll(jdb.command(JdbCommand.dump("this.s")));
        reply.addAll(jdb.command(JdbCommand.dump("this.t.length")));
        reply.addAll(jdb.command(JdbCommand.dump("this.t[1].length")));
        reply.addAll(jdb.command(JdbCommand.dump("ArrayLengthDumpTarg.i.length")));
        reply.addAll(jdb.command(JdbCommand.dump("this.length")));

        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                // Test the fix for 4690242:
                .shouldNotContain("No instance field or method with the name length in")
                .shouldNotContain("No static field or method with the name length")
                // Test the fix for 4695338:
                .shouldContain("\"zero\", \"one\", \"two\", \"three\", \"four\"");

        jdb.contToExit(1);
    }

}
