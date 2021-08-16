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
 * @summary converted from VM Testbase nsk/jdb/thread/thread002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  This is a test for jdb 'thread <thread id>' command.
 *  The debugee starts 5 'MyThreads' that are all suspended on the lock
 *  that the main thread posseses. The 'threads' command is issued
 *  at this point to obtain ids' od auxiliary threads. Then for each id
 *  the 'thread' command is issued. The test checks that jdb correctly
 *  switches between threads using 'eval <expr>' command. The expression
 *  must return a string containing the name of current thread.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/threads/threads001 test
 *  and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.thread.thread002.thread002a
 * @run main/othervm
 *      nsk.jdb.thread.thread002.thread002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.thread.thread002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class thread002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new thread002().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.thread.thread002";
    static final String TEST_CLASS       = PACKAGE_NAME + ".thread002";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    static final String THREAD_NAME      = "MyThread";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        jdb.receiveReplyFor(JdbCommand.cont);

        String[] threadIds = jdb.getThreadIds(PACKAGE_NAME + "." + THREAD_NAME);

        for (int i = 0; i < thread002a.numThreads; i++) {
            jdb.receiveReplyFor(JdbCommand.thread + threadIds[i]);
            jdb.receiveReplyFor(JdbCommand.print + DEBUGGEE_CLASS + ".holder[" + i + "].name");
        }

        jdb.contToExit(1);

        reply = jdb.getTotalReply();
        grep = new Paragrep(reply);
        for (int i = 0; i < threadIds.length; i++) {
            count = grep.find(THREAD_NAME + "#" + i);
            if (count != 1) {
                 failure("jdb failed to switch to thread: " + threadIds[i]);
            }
        }
    }
}
