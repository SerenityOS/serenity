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
 * @summary converted from VM Testbase nsk/jdb/where/where006.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  A test to exercise the functionality of the 'where <thread_id>'
 *  and the 'where all' commands.
 *  The debugee creates 5 threads that are all suspended on locks
 *  that the main thread posseses. The 'where <thread_id>' command
 *  is used to get stack traces of all 5 suspended threads. The test
 *  passes if contents of the stack trace match to expected output.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/where/where003 test
 *  and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.where.where006.where006a
 * @run main/othervm
 *      nsk.jdb.where.where006.where006
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.where.where006;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class where006 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new where006().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.where.where006";
    static final String TEST_CLASS       = PACKAGE_NAME + ".where006";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] FRAMES = new String[][] {
        {PACKAGE_NAME + ".MyThread.func5", "111"},
        {PACKAGE_NAME + ".MyThread.func4", "103"},
        {PACKAGE_NAME + ".MyThread.func3", "99"},
        {PACKAGE_NAME + ".MyThread.func2", "95"},
        {PACKAGE_NAME + ".MyThread.func1", "91"},
        {PACKAGE_NAME + ".MyThread.run", "85"},
                                                };
    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        jdb.receiveReplyFor(JdbCommand.cont);

        String[] threadIds = jdb.getThreadIds(PACKAGE_NAME + ".MyThread");
        reply = jdb.receiveReplyFor(JdbCommand.where + "all");
        for (int i = 0; i < where006a.numThreads; i++) {
            checkFrames(threadIds[i], reply, 5);
        }

        for (int i = 0; i < where006a.numThreads; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.where + threadIds[i]);
            checkFrames(threadIds[i], reply, 1);
        }

        jdb.contToExit(1);
    }

    void checkFrames (String threadId, String[] reply, int expectedVal) {
        Paragrep grep;
        int count;
        Vector v;
        String found;

        grep = new Paragrep(reply);
        for (int j = 0; j < FRAMES.length; j++) {
            count = grep.find(FRAMES[j][0]);
            if (count != expectedVal) {
                failure("Unexpected number of occurencies of the stack frame: " + FRAMES[j][0] +
                    " for thread " + threadId +
                    "\n\t Expected number of occurence: " + expectedVal +", got : " + count);
                if (count > 0) {
                    found = grep.findFirst(FRAMES[j][0]);
                    if (found.indexOf(FRAMES[j][1]) < 0) {
                        failure("Unexpected location in the stack frame: " + FRAMES[j][0] +
                            " for thread " + threadId +
                            "\n\t Expected location: " + FRAMES[j][1] + ", got :\n\t" + found);
                    }
                }
            }
        }
    }
}
