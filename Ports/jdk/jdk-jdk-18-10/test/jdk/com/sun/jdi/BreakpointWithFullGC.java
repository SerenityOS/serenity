/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6862295
 * @summary Verify breakpoints still work after a full GC.
 * @comment converted from test/jdk/com/sun/jdi/BreakpointWithFullGC.sh
 *
 * @library /test/lib
 * @compile -g BreakpointWithFullGC.java
 * @run main/othervm BreakpointWithFullGC
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.Jdb;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.ArrayList;
import java.util.List;

class BreakpointWithFullGCTarg {
    public static List<Object> objList = new ArrayList<>();

    private static void init(int numObjs) {
        for (int i = 0; i < numObjs; i++) {
            objList.add(new Object());
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println("top of loop");     // @1 breakpoint
            init(500000);
            objList.clear();
            System.gc();
            System.out.println("bottom of loop");  // @1 breakpoint
        }
        System.out.println("end of test");         // @1 breakpoint
    }
}

public class BreakpointWithFullGC extends JdbTest {
    public static void main(String argv[]) {
        new BreakpointWithFullGC().run();
    }

    private BreakpointWithFullGC() {
        super(new LaunchOptions(DEBUGGEE_CLASS)
                     .addDebuggeeOptions(DEBUGGEE_OPTIONS));
    }

    private static final String DEBUGGEE_CLASS = BreakpointWithFullGCTarg.class.getName();
    // We don't specify "-Xmx" for debuggee as we have full GCs with any value.
    private static final String[] DEBUGGEE_OPTIONS = {"-verbose:gc"};

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("BreakpointWithFullGC.java", 1);

        // get to the first loop breakpoint
        jdb.command(JdbCommand.run());
        // 19 "cont" commands gets us through all the loop breakpoints.
        for (int i = 1; i <= 19; i++) {
            jdb.command(JdbCommand.cont());
        }
        // get to the last breakpoint
        jdb.command(JdbCommand.cont());

        jdb.contToExit(1);

        new OutputAnalyzer(getJdbOutput())
                // make sure we hit the first breakpoint at least once
                .stdoutShouldMatch("System\\..*top of loop")
                // make sure we hit the second breakpoint at least once
                .stdoutShouldMatch("System\\..*bottom of loop")
                // make sure we hit the last breakpoint
                .stdoutShouldMatch("System\\..*end of test");
        new OutputAnalyzer(getDebuggeeOutput())
                // check for error message due to thread ID change
                .stderrShouldNotContain("Exception in thread \"event-handler\" java.lang.NullPointerException");
    }
}
