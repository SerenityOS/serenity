/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6394084
 * @summary Redefine class can't handle addition of 64 bit constants in JDK1.5.0_05
 * @comment converted from test/jdk/com/sun/jdi/RedefineIntConstantToLong.sh
 *
 * @library /test/lib
 * @compile -g RedefineIntConstantToLong.java
 * @run main/othervm RedefineIntConstantToLong
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

final class RedefineIntConstantToLongTarg {

    public long m1(int i) {
        long r=0;
        r = m2(i * 2); // @1 commentout
        // @1 uncomment      r =m2(i * 2L);
        return r;
    }

    public long m2(int j) {
        System.out.println(System.getProperty("line.separator") +
                           "**** public long m2(int j) with value: " + j);
        return j;
    }

    public long m2(long j) {
        System.out.println(System.getProperty("line.separator") +
                           "**** public long m2(long j) with value: " + j);
        return j;
    }

    public void doit() throws Exception {
        long r1 = 0;
        long r2;
        r1 = m1(1000);
        r2 = 0;         // @1 breakpoint
        r2 = m1(1000);
        if (r1 != r2) { // @1 breakpoint
             throw new Exception("FAILURE: Expected value: " + r1 + " Actual value: " + r2);
        } else {
             System.out.println("SUCCESS: Expected value: " + r1 + " Actual value: " + r2);
        }
    }

    public static void main(String args[]) throws Exception {
        new RedefineIntConstantToLongTarg().doit();
    }
}

public class RedefineIntConstantToLong extends JdbTest {

    public static void main(String argv[]) {
        new RedefineIntConstantToLong().run();
    }

    private RedefineIntConstantToLong() {
        super(RedefineIntConstantToLongTarg.class.getName(), "RedefineIntConstantToLong.java");
    }

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        redefineClass(1, "-g");
        setBreakpoints(1);
        jdb.command(JdbCommand.cont());
        jdb.command(JdbCommand.where(""));
        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldNotContain("FAILURE:");
    }
}
