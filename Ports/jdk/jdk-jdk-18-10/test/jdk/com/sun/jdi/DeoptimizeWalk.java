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
 * @bug 4525714
 * @summary jtreg test PopAsynchronousTest fails in build 85 with -Xcomp
 * @comment converted from test/jdk/com/sun/jdi/DeoptimizeWalk.sh
 *
 * @library /test/lib
 * @compile -g DeoptimizeWalk.java
 * @run main/othervm DeoptimizeWalk
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

/*
 * The bug (JDK-4525714) is about failing PopAsynchronousTest test.
 * This is another test of the same issue. The bug occurs when trying
 * to walk the stack of a deoptimized thread. We can do this
 * by running in -Xcomp mode and by doing a step which causes deopt,
 * and then a 'where'. This will cause not all the frames to be shown.
 */

class DeoptimizeWalkTarg {
    static public void main(String[] args) {
        DeoptimizeWalkTarg mine = new DeoptimizeWalkTarg();
        mine.a1(89);
    }

    public void a1(int p1) {
        int v1 = 89;
        System.out.println("a1" + v1);
        a2(89);
    }

    public void a2(int pp) {
        int v2 = 89;
        System.out.println("a2" + v2);
        a3(89);
    }

    public void a3(int pp) {
        int v3 = 89;
        System.out.println("a3");  //@ 1 breakpoint
        a4(22);                  // it passes if this line is commented out
        System.out.println("jj");
    }

    public void a4(int pp) {
        int v4 = 90;
        System.out.println("a4: @1 breakpoint here");
    }
}


public class DeoptimizeWalk extends JdbTest {
    public static void main(String argv[]) {
        new DeoptimizeWalk().run();
    }

    private DeoptimizeWalk() {
        super(new LaunchOptions(DEBUGGEE_CLASS)
                .addDebuggeeOptions(DEBUGGEE_OPTIONS));
    }

    private static final String DEBUGGEE_CLASS = DeoptimizeWalkTarg.class.getName();
    private static final String[] DEBUGGEE_OPTIONS = {"-Xcomp"};

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("DeoptimizeWalk.java", 1);
        jdb.command(JdbCommand.run());

        jdb.command(JdbCommand.where(""));
        jdb.command(JdbCommand.step());
        jdb.command(JdbCommand.where(""));

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                .shouldContain(DEBUGGEE_CLASS + ".main");
        new OutputAnalyzer(getDebuggeeOutput())
                .shouldNotContain("Internal exception:");
    }
}

