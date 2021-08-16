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
 * @summary converted from VM Testbase nsk/jdb/where/where005.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  A test case for the 'where' command. The debugged 'where005a'
 *  application throws a null pointer exception in 'func6()' method.
 *  The test passes if the 'where' command produces the correct
 *  stack trace.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/where/where002 test
 *  and replaces it.
 *  Modified due to fix of the bug:
 *  4818762 TEST_BUG: two jdb test incorrectly check debuggee exit code
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.where.where005.where005a
 * @run main/othervm
 *      nsk.jdb.where.where005.where005
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.where.where005;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class where005 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new where005(true).runTest(argv, out);
    }

    public where005 (boolean debuggeeShouldFail) {
        super(debuggeeShouldFail);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.where.where005";
    static final String TEST_CLASS       = PACKAGE_NAME + ".where005";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] FRAMES = new String[][] {
        {DEBUGGEE_CLASS + ".func6", "76"},
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

        reply = jdb.receiveReplyFor(JdbCommand.cont);
        grep = new Paragrep(reply);

        if (grep.find("NullPointerException") == 0) {
            failure("Expected NullPointerException is not thrown");
        } else {
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
        }
        jdb.sendCommand(JdbCommand.quit);
    }
}
