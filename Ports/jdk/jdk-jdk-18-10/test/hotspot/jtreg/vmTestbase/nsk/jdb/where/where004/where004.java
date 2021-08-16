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
 * @summary converted from VM Testbase nsk/jdb/where/where004.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  A test case for the 'where' command. The test sets a breakpoint
 *  at func5() in debugged `where004a` class and then runs debugee.
 *  Once, execution comes to a halt at func5(), the 'where' command
 *  is issued to the debugger. The test passes if items printed in
 *  stack trace are equal to expected ones.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/where/where001 test
 *  and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.where.where004.where004a
 * @run main/othervm
 *      nsk.jdb.where.where004.where004
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.where.where004;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class where004 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new where004().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.where.where004";
    static final String TEST_CLASS       = PACKAGE_NAME + ".where004";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] FRAMES = new String[][] {
        {DEBUGGEE_CLASS + ".func5", "71"},
        {DEBUGGEE_CLASS + ".func4", "67"},
        {DEBUGGEE_CLASS + ".func3", "63"},
        {DEBUGGEE_CLASS + ".func2", "59"},
        {DEBUGGEE_CLASS + ".func1", "55"},
        {DEBUGGEE_CLASS + ".runIt", "48"},
        {DEBUGGEE_CLASS + ".main",  "39"}
                                                };
    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.receiveReplyFor(JdbCommand.stop_in + DEBUGGEE_CLASS + ".func5");
        jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.where);
        grep = new Paragrep(reply);

        for (int i = 0; i < FRAMES.length; i++) {
            v = new Vector();
            v.add(FRAMES[i][0]);
            v.add(FRAMES[i][1]);
            count = grep.find(v);
            if (count != 1) {
                failure("Unexpected number or location of the stack frame: " + FRAMES[i][0] +
                    "\n\texpected value : 1, got one: " + count);
            }
        }

        jdb.contToExit(1);
    }
}
