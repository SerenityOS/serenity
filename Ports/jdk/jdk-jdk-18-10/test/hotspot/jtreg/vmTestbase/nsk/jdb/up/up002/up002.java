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
 * @summary converted from VM Testbase nsk/jdb/up/up002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  This tests the jdb 'up' command. The test sets a breakpoint
 *  at 'func5' method in debugged 'nsk.jdb.up.up002a' class and then
 *  runs the debugee.  Once, an execution suspends at 'func5' method,
 *  the 'up' command is issued to the debugger several times.
 *  The output is processed by counting the number of times
 *  each method name appears in the stack trace. The test passes
 *  if the obtained count matches the expected one.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/up/up001 test and
 *  replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.up.up002.up002a
 * @run main/othervm
 *      nsk.jdb.up.up002.up002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.up.up002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class up002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new up002().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.up.up002";
    static final String TEST_CLASS       = PACKAGE_NAME + ".up002";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] FRAMES = new String[][] {
        {"[1]", DEBUGGEE_CLASS + ".func5"},
        {"[2]", DEBUGGEE_CLASS + ".func4"},
        {"[3]", DEBUGGEE_CLASS + ".func3"},
        {"[4]", DEBUGGEE_CLASS + ".func2"},
        {"[5]", DEBUGGEE_CLASS + ".func1"},
        {"[6]", DEBUGGEE_CLASS + ".runIt"},
        {"[7]", DEBUGGEE_CLASS + ".main"}
                                                };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.receiveReplyFor(JdbCommand.stop_in + DEBUGGEE_CLASS + ".func5");
        jdb.receiveReplyFor(JdbCommand.cont);

        for (int i = 0; i < FRAMES.length; i++) {
            jdb.receiveReplyFor(JdbCommand.where);
            jdb.receiveReplyFor(JdbCommand.up);
        }

        jdb.contToExit(1);

        reply = jdb.getTotalReply();
        grep = new Paragrep(reply);

        for (int i = 1; i < (FRAMES.length-1); i++) {
            v = new Vector();
            v.add(FRAMES[i][0]);
            v.add(FRAMES[i][1]);
            count = grep.find(v);
            if (count != (i+1)) {
                failure("Unexpected number of the stack frame: " + FRAMES[i][1] +
                    "\n\texpected value : " + (i+1) + ", got : " + count);
            }
        }

    }

}
