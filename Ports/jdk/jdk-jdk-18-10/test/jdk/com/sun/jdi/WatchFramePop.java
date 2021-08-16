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
 * @bug 4546478
 * @summary Enabling a watchpoint can kill following NotifyFramePops
 * @comment converted from test/jdk/com/sun/jdi/WatchFramePop.sh
 *
 * @library /test/lib
 * @run main/othervm WatchFramePop
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class WatchFramePopTarg {
    int watchMe;
    static public void main(String[] args) {
        System.out.println("In Main");
        WatchFramePopTarg mine = new WatchFramePopTarg();
        mine.a1();
        System.out.println("Test completed");
    }

    public void a1() {
        a2();                           // @1 breakpoint. We'll do a watch of watchMe here
    }

    public void a2() {
        System.out.println("in a2");
        a3();
    }                                   // line 18

    public void a3() {
        System.out.println("in a3");    // After the watch, we'll run to here, line 21
        a4();                           // We'll do a 'next' to here.  The failure is that this
    }                                   // runs to completion, or asserts with java_g

    public void a4() {
        System.out.println("in a4");
    }

}

public class WatchFramePop extends JdbTest {
    public static void main(String argv[]) {
        new WatchFramePop().run();
    }

    private WatchFramePop() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private static final String DEBUGGEE_CLASS = WatchFramePopTarg.class.getName();
    private static final String SOURCE_FILE = "WatchFramePop.java";

    @Override
    protected void runCases() {
        setBreakpoints(1);
        jdb.command(JdbCommand.run());
        jdb.command(JdbCommand.watch(DEBUGGEE_CLASS, "watchMe"));
        jdb.command(JdbCommand.stopIn(DEBUGGEE_CLASS, "a3"));
        jdb.command(JdbCommand.cont());                             // stops at the bkpt
        jdb.command(JdbCommand.next());                             // The bug is that this next runs to completion
        // In which case, so does jdb
        // so we never get here.

        new OutputAnalyzer(jdb.getJdbOutput())
                .shouldNotContain("The application exited");
    }
}
