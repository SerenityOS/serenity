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
 * @summary converted from VM Testbase nsk/jdb/step_up/step_up001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'step up' command.
 * The debuggee program (step_up001a.java) creates two additional
 * threads of MyThread type and starts them. The jdb sets up breakpoint
 * inside the method 'func2' which is invoked in these addional threads.
 * When breakpoint is hitted the checked command is called. After this,
 * the stack trace of the current method is checked by 'where' command.
 * The test passes if only caller method ('func1') presents in stack
 * trace, but neither of 'func2' nor 'func3' methods.
 * The test consists of two program:
 *   step_up001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   step_up001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.step_up.step_up001.step_up001a
 * @run main/othervm
 *      nsk.jdb.step_up.step_up001.step_up001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.step_up.step_up001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class step_up001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new step_up001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.step_up.step_up001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".step_up001";
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
        int stepupCount = 0;
        for (int i = 0; i < step_up001a.numThreads; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.cont);
            if (jdb.isAtBreakpoint(reply, LAST_BREAK)) {
                breakCount++;
                reply = jdb.receiveReplyFor(JdbCommand.step); // to get out of lastBreak;

                reply = jdb.receiveReplyFor(JdbCommand.step_up);
                if (!checkSteppedUp()) {
                    success = false;
                } else {
                    stepupCount++;
                }
            }
        }

        jdb.contToExit(1);

        if (stepupCount != step_up001a.numThreads) {
            log.complain("Wrong number of step up events: " + stepupCount);
            log.complain("Must be equal to : " + step_up001a.numThreads);
            success = false;
        }
    }


    private boolean checkSteppedUp () {
        Paragrep grep;
        String found;
        int count;
        boolean result = true;
        String[] reply;

        reply = jdb.receiveReplyFor(JdbCommand.where);

        grep = new Paragrep(reply);
        for (int i = 1 /* !!! */; i < checkedMethods.length; i++) {
            count = grep.find(DEBUGGEE_THREAD + "." + checkedMethods[i]);
            if (count > 0) {
                log.complain("Wrong method in thread stack trace: " + DEBUGGEE_THREAD + "." + checkedMethods[i]);
                result= false;
            }
        }

        count = grep.find(DEBUGGEE_THREAD + "." + checkedMethods[0]);
        if (count != 1) {
            log.complain("Checked method does not exist in thread stack trace: " + DEBUGGEE_THREAD + "." + checkedMethods[0]);
            result= false;
        }

        return result;
    }
}
