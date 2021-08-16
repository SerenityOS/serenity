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
 * @summary converted from VM Testbase nsk/jdb/reenter/reenter001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * A positive test case for the 'reenter' command.
 * The debugged application (reenter001a.java) starts additional thread of MyThread
 * class. The 'run()' method of the this class invokes recursively a number of
 * int methods. The jdb sets breakpoint in the last called 'func5()' method
 * to suspend the debugged VM when all the methods, i.e. from 'func1()' to
 * 'func5()', are in MyThread's stack. Then jdb steps up three frame on stack
 * by 'up 3' command. At this moment frame with 'func2()' is current.
 * Then the 'reenter' command is called. The test passes if after execution of
 * tested command the 'func2()' frame reentered, i.e. top frame of the stack.
 * The test consists of two program:
 *   reenter001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   reenter001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.reenter.reenter001.reenter001a
 * @run main/othervm
 *      nsk.jdb.reenter.reenter001.reenter001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.reenter.reenter001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class reenter001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new reenter001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.reenter.reenter001";
    static final String TEST_CLASS = PACKAGE_NAME + ".reenter001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";

    static final String MYTHREAD        = "MyThread";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + "." + MYTHREAD;
    static final String[] CHECKED_METHODS = {"func1", "func2", "func3", "func4", "func5"};

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        while (true) {
            String[] threads = jdb.getThreadIds(DEBUGGEE_THREAD);
            if (threads.length != 1) {
                log.complain("jdb should report 1 instance of " + DEBUGGEE_THREAD);
                log.complain("Found: " + threads.length);
                success = false;
                break;
            }

            reply = jdb.receiveReplyFor(JdbCommand.thread + threads[0]);

            reply = jdb.receiveReplyFor(JdbCommand.step); // to get out of lastBreak()

            reply = jdb.receiveReplyFor(JdbCommand.where);
            if (!checkStack(reply, "func5", "[1]", "lastBreak", false)) {
                success = false;
            }

            reply = jdb.receiveReplyFor(JdbCommand.up + " 3");

            reply = jdb.receiveReplyFor(JdbCommand.where);
            if (!checkStack(reply, "func2", "[4]", "func3", false)) {
                success = false;
            }

            reply = jdb.receiveReplyFor(JdbCommand.reenter);

            reply = jdb.receiveReplyFor(JdbCommand.where);
            if (!checkStack(reply, "func2", "[1]", "func3", true)) {
                success = false;
            }

            reply = jdb.receiveReplyFor(JdbCommand.locals);
            grep = new Paragrep(reply);
            if (grep.find("Internal exception") > 0) {
                log.complain("Internal exception was thrown while 'locals' command");
                for (int i = 0; i < reply.length; i++) {
                    log.complain(reply[i]);
                }
                success = false;
            }

            break;
        }

        jdb.contToExit(2);
    }

    private boolean checkStack (String[] reply, String shouldBe, String frameNum, String shouldNotBe, boolean reenter ) {
        Paragrep grep;
        String found;
        Vector v = new Vector();
        boolean result = true;
        int count;

        grep = new Paragrep(reply);
        v.add(frameNum);
        v.add(DEBUGGEE_THREAD + "." + shouldBe);
        if ((count = grep.find(v)) != 1) {
            log.complain("Contents of stack trace is incorrect " + ( reenter? "after 'reenter' command": ""));
            log.complain("Searched for: " + DEBUGGEE_THREAD + "." + shouldBe);
            log.complain("Count : " + count);
            result = false;
        }

        if (grep.find(DEBUGGEE_THREAD + "." + shouldNotBe) > 0) {
            log.complain("Contents of stack trace is incorrect " + ( reenter? "after 'reenter' command": ""));
            log.complain("Found wrong frame: " + DEBUGGEE_THREAD + "." + shouldNotBe);
            result = false;
        }

        return result;
    }
}
