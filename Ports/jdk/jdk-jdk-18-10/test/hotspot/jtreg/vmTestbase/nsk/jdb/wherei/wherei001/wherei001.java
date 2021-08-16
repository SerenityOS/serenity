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
 * @summary converted from VM Testbase nsk/jdb/wherei/wherei001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'wherei <thread id>' command.
 * The test checks if jdb correctly reports stack trace for
 * every checked thread id.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.wherei.wherei001.wherei001a
 * @run main/othervm
 *      nsk.jdb.wherei.wherei001.wherei001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.wherei.wherei001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class wherei001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new wherei001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.wherei.wherei001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".wherei001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + ".MyThread";

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

        if (threads.length != 5) {
            log.complain("jdb should report 5 instance of " + DEBUGGEE_THREAD);
            log.complain("Found: " + threads.length);
            success = false;
        }

        for (int i = 0; i < threads.length; i++) {
            if (!checkStack(threads[i])) {
                success = false;
            }
        }

        jdb.contToExit(1);
    }

    private boolean checkStack (String threadId) {
        Paragrep grep;
        String[] reply;
        String found;
        int count;
        Vector v;
        boolean result = true;
        String[] func = { "func5", "func4", "func3", "func2", "func1", "run" };

        reply = jdb.receiveReplyFor(JdbCommand.wherei + threadId);

        grep = new Paragrep(reply);
        for (int i = 0; i < func.length; i++) {
            count = grep.find(DEBUGGEE_THREAD + "." + func[i]);
            if (count != 1) {
                log.complain("Contents of stack trace is incorrect for thread " + threadId);
                log.complain("Searched for: " + DEBUGGEE_THREAD + "." + func[i]);
                log.complain("Count : " + count);
                result= false;
            }
        }
        return result;
    }
}
