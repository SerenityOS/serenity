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
 * @summary converted from VM Testbase nsk/jdb/clear/clear002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'clear <class_id>.<method>' command.
 * The test sets 3 breakpoints and then clears two of them after the
 * first breakpoint is reached. The tests verifies that jdb does not
 * halt execution at the cleared breakpoint.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.clear.clear002.clear002a
 * @run main/othervm
 *      nsk.jdb.clear.clear002.clear002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.clear.clear002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class clear002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new clear002().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.clear.clear002";
    static final String TEST_CLASS       = PACKAGE_NAME + ".clear002";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";
    static final String METHOD_TO_STOP   = DEBUGGEE_CLASS + ".func5";
    static final String METHOD1_TO_CLEAR = DEBUGGEE_CLASS + ".func4";
    static final String METHOD2_TO_CLEAR = DEBUGGEE_CLASS + "$A.func7";
    static final String REMOVED_SAMPLE   = "Removed:";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        log.display("Setting breakpoint in method: " + METHOD1_TO_CLEAR);
        jdb.setBreakpointInMethod(METHOD1_TO_CLEAR);

        log.display("Setting breakpoint in method: " + METHOD2_TO_CLEAR);
        jdb.setBreakpointInMethod(METHOD2_TO_CLEAR);

        log.display("Setting breakpoint in method: " + METHOD_TO_STOP);
        jdb.setBreakpointInMethod(METHOD_TO_STOP);

        if (!checkClear (METHOD1_TO_CLEAR)) {
            success = false;
        }

        if (!checkClear (METHOD2_TO_CLEAR)) {
            success = false;
        }

        jdb.contToExit(2);

        grep = new Paragrep(jdb.getTotalReply());
        count = grep.find(Jdb.BREAKPOINT_HIT);
        if (count != 2) {
            log.complain("Should hit 2 breakpoints.");
            log.complain("Breakpoint hit count reported: " + count);
            success = false;
        }

        if (!checkBreakpoint (METHOD1_TO_CLEAR, grep)) {
            success = false;
        }

        if (!checkBreakpoint (METHOD2_TO_CLEAR, grep)) {
            success = false;
        }
    }

    private boolean checkBreakpoint (String methodName, Paragrep grep) {
        String found;
        boolean result = true;
        int count;
        Vector v;

        v = new Vector();
        v.add(Jdb.BREAKPOINT_HIT);
        v.add(methodName);

        found = grep.findFirst(v);
        if (found.length() > 0) {
            log.complain("Wrong hit at removed breakpoint in method:" + methodName);
            result = false;
        }
        return result;
    }

    private boolean checkClear (String methodName) {
        Paragrep grep;
        String found;
        String[] reply;
        boolean result = true;
        int count;
        Vector v;

        v = new Vector();
        v.add(REMOVED_SAMPLE);
        v.add(methodName);

        log.display("Clearing breakpoint in method:" + methodName);
        reply = jdb.receiveReplyFor(JdbCommand.clear + methodName);
        grep = new Paragrep(reply);

        found = grep.findFirst(v);
        if (found.length() == 0) {
            log.complain("Failed to clear breakpoint in method: " + methodName);
            result = false;
        }
        return result;
    }
}
