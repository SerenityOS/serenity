/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211736
 * @summary Tests that the breakpoint location and prompt are printed in the debugger output
 * when breakpoint is hit and suspend policy is set to SUSPEND_EVENT_THREAD or SUSPEND_NONE
 *
 * @library /test/lib
 * @run compile -g JdbStopThreadTest.java
 * @run main/othervm JdbStopThreadTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class JdbStopThreadTestTarg {
    public static void main(String[] args) {
        test();
    }

    private static void test() {
        Thread thread = Thread.currentThread();
        print(thread); // @1 breakpoint
        String str = "test";
        print(str); // @2 breakpoint
    }

    public static void print(Object obj) {
        System.out.println(obj);
    }
}

public class JdbStopThreadTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbStopThreadTest().run();
    }

    private JdbStopThreadTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbStopThreadTestTarg.class.getName();
    private static final String PATTERN1_TEMPLATE = "^Breakpoint hit: \"thread=main\", " +
            "JdbStopThreadTestTarg\\.test\\(\\), line=%LINE_NUMBER.*\\R%LINE_NUMBER\\s+print\\(thread\\);.*\\R>\\s";
    private static final String PATTERN2_TEMPLATE = "^Breakpoint hit: \"thread=main\", " +
            "JdbStopThreadTestTarg\\.test\\(\\), line=%LINE_NUMBER.*\\R%LINE_NUMBER\\s+print\\(str\\);.*\\R>\\s";

    @Override
    protected void runCases() {
        // Test suspend policy SUSPEND_EVENT_THREAD
        int bpLine1 = parseBreakpoints(getTestSourcePath("JdbStopThreadTest.java"), 1).get(0);
        jdb.command(JdbCommand.stopThreadAt(DEBUGGEE_CLASS, bpLine1));
        String pattern1 = PATTERN1_TEMPLATE.replaceAll("%LINE_NUMBER", String.valueOf(bpLine1));
        // Run to breakpoint #1
        jdb.command(JdbCommand.run().waitForPrompt(pattern1, true));
        new OutputAnalyzer(jdb.getJdbOutput()).shouldMatch(pattern1);


        // Test suspend policy SUSPEND_NONE
        jdb.command(JdbCommand.thread(1));
        int bpLine2 = parseBreakpoints(getTestSourcePath("JdbStopThreadTest.java"), 2).get(0);
        jdb.command(JdbCommand.stopGoAt(DEBUGGEE_CLASS, bpLine2));
        String pattern2 = PATTERN2_TEMPLATE.replaceAll("%LINE_NUMBER", String.valueOf(bpLine2));
        jdb.command(JdbCommand.cont().allowExit());
        new OutputAnalyzer(jdb.getJdbOutput()).shouldMatch(pattern2);
    }
}
