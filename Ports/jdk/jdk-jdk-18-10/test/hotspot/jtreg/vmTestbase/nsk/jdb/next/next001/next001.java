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
 * @summary converted from VM Testbase nsk/jdb/next/next001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'next' command.
 * The debuggee program (next001a.java) creates two additional
 * threads of MyThread type and starts them. The jdb sets up breakpoint
 * inside the method 'func2' which is invoked in these addional threads.
 * When breakpoint is hitted the checked command is called at the line
 * in which debuggee's method ('func3') is invoked. After this,
 * the stack trace of the current method is checked by 'where' command.
 * The test passes if only calling method ('func2') presents in stack
 * trace, but not the called method ('func3').
 * The test consists of two program:
 *   next001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   next001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.next.next001.next001a
 * @run main/othervm
 *      nsk.jdb.next.next001.next001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.next.next001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class next001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new next001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.next.next001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".next001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";
    static final String MYTHREAD        = "MyThread";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + "." + MYTHREAD;

    static final String[] checkedMethods = {"func1", "func2", "func3"};

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;
        String[] threads;

        jdb.setBreakpointInMethod(LAST_BREAK);

        int breakCount = 0;
        int nextCount = 0;
        for (int i = 0; i < next001a.numThreads; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.cont);
            if (jdb.isAtBreakpoint(reply, LAST_BREAK)) {
                breakCount++;
                reply = jdb.receiveReplyFor(JdbCommand.step); // to get out of lastBreak;

                reply = jdb.receiveReplyFor(JdbCommand.next);
                if (!checkNext()) {
                    success = false;
                } else {
                    nextCount++;
                }
            }
        }

        jdb.contToExit(1);

        if (nextCount != next001a.numThreads) {
            log.complain("Wrong number of 'next' events: " + nextCount);
            log.complain("Must be equal to : " + next001a.numThreads);
            success = false;
        }
    }


    private boolean checkNext () {
        Paragrep grep;
        String found;
        int count;
        boolean result = true;
        String[] reply;

        reply = jdb.receiveReplyFor(JdbCommand.where);

        grep = new Paragrep(reply);
        count = grep.find(DEBUGGEE_THREAD + "." + checkedMethods[2]);
        if (count > 0) {
            log.complain("Debuggee is suspended in wrong method after 'next' command: " + DEBUGGEE_THREAD + "." + checkedMethods[2]);
            result= false;
        }

        count = grep.find(DEBUGGEE_THREAD + "." + checkedMethods[1]);
        if (count != 1) {
            log.complain("Checked method does not exist in thread stack trace: " + DEBUGGEE_THREAD + "." + checkedMethods[1]);
            result= false;
        }

        return result;
    }
}
