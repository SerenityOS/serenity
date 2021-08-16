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
 * @summary converted from VM Testbase nsk/jdb/suspend/suspend001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * A positive test case for the 'suspend <thread id>' command.
 * The debugged application (suspend001a.java) creates two addional threads.
 * First thead is of Suspended class, second one is of MyThread class.
 * The jdb stops debuggee at a moment after addional thread have started and
 * before their completion. Then jdb calls 'suspend' command with id of
 * the Suspended thread and resumes debuggee. The suspended thread should
 * not modify special int 'notSuspended' variable of the suspend001a class.
 * The test passes if suspend001a.notSuspended variable equals to 1, i.e.
 * only not-suspended MyThread thread had modified it.
 * The test consists of two program:
 *   suspend001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   suspend001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.suspend.suspend001.suspend001a
 * @run main/othervm
 *      nsk.jdb.suspend.suspend001.suspend001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.suspend.suspend001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class suspend001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new suspend001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.suspend.suspend001";
    static final String TEST_CLASS = PACKAGE_NAME + ".suspend001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK    = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK     = DEBUGGEE_CLASS + ".breakHere";

    static final String SUSPENDED       = "Suspended";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + "." + SUSPENDED;
    static final String DEBUGGEE_RESULT = DEBUGGEE_CLASS + ".notSuspended";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;
        String[] threads;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);
        while (true) {
            threads = jdb.getThreadIds(DEBUGGEE_THREAD);
            if (threads.length != 1) {
                log.complain("jdb should report 1 instance of " + DEBUGGEE_THREAD);
                log.complain("Found: " + threads.length);
                success = false;
                break;
            }

            reply = jdb.receiveReplyFor(JdbCommand.suspend + threads[0]);

            reply = jdb.receiveReplyFor(JdbCommand.cont);
            if (!jdb.isAtBreakpoint(reply)) {
                log.complain("Debugge does not reached second breakHere breakpoint");
                success = false;
                break;
            }

            reply = jdb.receiveReplyFor(JdbCommand.eval + DEBUGGEE_RESULT);
            grep = new Paragrep(reply);
            found = grep.findFirst(DEBUGGEE_RESULT + " =" );
            if (found.length() > 0 && found.indexOf(DEBUGGEE_RESULT + " = null") < 0) {
                if (found.indexOf(DEBUGGEE_RESULT + " = 1") < 0) {
                   log.complain("Wrong value of " + DEBUGGEE_RESULT);
                   log.complain(found);
                   success = false;
                }
            } else {
                log.complain("TEST BUG: not found value for " + DEBUGGEE_RESULT);
                success = false;
            }

            break;
        }

        jdb.contToExit(2);
    }
}
