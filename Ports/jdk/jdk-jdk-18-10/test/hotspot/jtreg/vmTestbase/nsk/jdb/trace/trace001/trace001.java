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
 * @summary converted from VM Testbase nsk/jdb/trace/trace001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'trace methods <thread id>' command.
 * The debuggee program (trace001a.java) creates two of additional
 * threads with name like "MyThread-<number>" and starts them. The jdb
 * suspends the debuggee at a moment when the additional threads are
 * waiting for notification for lock objects and then turns on method
 * tracing for these threads by checked command. After expected
 * notification the additional threads invoke checked debuggee's methods.
 * Thus jdb output must have the trace messages of checked methods'
 * entrance and exit. The test passes if jdb output has two 'enter' messages
 * and two 'exit' messages for every checked method.
 * The test consists of two program:
 *   trace001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   trace001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.trace.trace001.trace001a
 * @run main/othervm
 *      nsk.jdb.trace.trace001.trace001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.trace.trace001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class trace001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new trace001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.trace.trace001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".trace001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";
    static final String MYTHREAD        = "MyThread";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + "." + MYTHREAD;

    static final String[] CHECKED_METHODS = {"func1", "func2", "func3"};

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;
        String[] threads;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        threads = jdb.getThreadIds(DEBUGGEE_THREAD);

        if (threads.length != 2) {
            log.complain("jdb should report 2 instance of " + DEBUGGEE_THREAD);
            log.complain("Found: " + threads.length);
            success = false;
        }

        for (int i = 0; i < threads.length; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.trace + "methods " + threads[i]);
        }

        jdb.contToExit(CHECKED_METHODS.length*threads.length*2 + 3);

        reply = jdb.getTotalReply();
        if (!checkTrace(CHECKED_METHODS, reply)) {
            success = false;
        }
    }

    private boolean checkTrace (String[] checkedMethods, String[] reply) {
        Paragrep grep;
        String found;
        int count;
        Vector v = new Vector();
        boolean result = true;

        grep = new Paragrep(reply);
        for (int i = 0; i < checkedMethods.length; i++) {
            v.removeAllElements();
            v.add(DEBUGGEE_THREAD + "." + checkedMethods[i]);
            v.add("Method entered");
            count = grep.find(v);
            if (count != 2) {
                log.complain("Count of method enter is incorrect for the method : " + DEBUGGEE_THREAD + "." + checkedMethods[i]);
                log.complain("Should be 2 trace messages, found : " + count);
                result= false;
            }

            v.removeAllElements();
            v.add(DEBUGGEE_THREAD + "." + checkedMethods[i]);
            v.add("Method exited");
            count = grep.find(v);
            if (count != 2) {
                log.complain("Count of method exit is incorrect for the method : " + DEBUGGEE_THREAD + "." + checkedMethods[i]);
                log.complain("Should be 2 trace messages, found : " + count);
                result= false;
            }
        }
        return result;
    }
}
