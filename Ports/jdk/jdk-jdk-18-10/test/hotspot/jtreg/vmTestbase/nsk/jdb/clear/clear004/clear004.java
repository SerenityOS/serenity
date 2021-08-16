/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jdb/clear/clear004.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  A positive test case for the 'clear <class_id:line>' command.
 *  The test sets 3 breakpoints and then clears one after the first
 *  breakpoint is reached. The tests verifies that jdb does not
 *  halt execution at the cleared breakpoint.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/clear/clear004 test and
 *  replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.clear.clear004.clear004a
 * @run main/othervm
 *      nsk.jdb.clear.clear004.clear004
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.clear.clear004;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class clear004 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new clear004().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.clear.clear004";
    static final String TEST_CLASS       = PACKAGE_NAME + ".clear004";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";
    static final String[] BREAKPOINTS = new String[]
        { DEBUGGEE_CLASS + ":63",
          DEBUGGEE_CLASS + ":67",
          DEBUGGEE_CLASS + ":71" };
    static final String REMOVED_SAMPLE   = "Removed:";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        for (int i = 0; i < BREAKPOINTS.length; i++) {
           log.display("Setting breakpoint at " + BREAKPOINTS[i]);
           reply = jdb.receiveReplyFor(JdbCommand.stop_at + BREAKPOINTS[i]);
        }

        if (!checkClear (BREAKPOINTS[1])) {
            success = false;
        }

        jdb.contToExit(3);

        grep = new Paragrep(jdb.getTotalReply());
        count = grep.find(Jdb.BREAKPOINT_HIT);
        if (count != 3) {
            log.complain("Should hit 3 breakpoints.");
            log.complain("Breakpoint hit count reported: " + count);
            success = false;
        }

        if (!checkBreakpoint (BREAKPOINTS[1], grep)) {
            success = false;
        }

    }

    private boolean checkBreakpoint (String breakpoint, Paragrep grep) {
        String found;
        boolean result = true;
        int count;
        Vector v;

        v = new Vector();
        v.add(Jdb.BREAKPOINT_HIT);
        v.add(breakpoint);

        found = grep.findFirst(v);
        if (found.length() > 0) {
            log.complain("Wrong hit at removed breakpoint at:" + breakpoint);
            result = false;
        }
        return result;
    }

    private boolean checkClear (String breakpoint) {
        Paragrep grep;
        String found;
        String[] reply;
        boolean result = true;
        int count;
        Vector v;

        v = new Vector();
        v.add(REMOVED_SAMPLE);
        v.add(breakpoint);

        log.display("Clearing breakpoint at: " + breakpoint);
        reply = jdb.receiveReplyFor(JdbCommand.clear + breakpoint);
        grep = new Paragrep(reply);

        found = grep.findFirst(v);
        if (found.length() == 0) {
            log.complain("Failed to clear breakpoint at: " + breakpoint);
            result = false;
        }
        return result;
    }
}
