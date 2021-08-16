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
 * @summary converted from VM Testbase nsk/jdb/interrupt/interrupt001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'interrupt <thread id>' command.
 * The debuggee program (interrupt001a.java) creates a number of additional
 * threads with name like "MyThread-<number>" and starts them. The jdb
 * suspends the debuggee at a moment when the additional threads are
 * waiting for notification for lock objects and then tries to interrupt them.
 * If these threads are interrupted then a value of the special
 * "notInterrupted" variable should not be modified.
 * Value of "notInterrupted" variable is checked by "eval <expr>" command.
 * The test passes if the value is equal to 0 and fails otherwise..
 * COMMENTS
 * Modified due to fix of the bug:
 *  4760826 TTY: 'interrupt <threadID>' command sometimes does not work in Mantis
 * Modified due to fix of the bug:
 *  4974992 Missed prompt with jdb testcase nsk/jdb/interrupt/interrupt001
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.interrupt.interrupt001.interrupt001a
 * @run main/othervm
 *      nsk.jdb.interrupt.interrupt001.interrupt001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.interrupt.interrupt001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class interrupt001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        return new interrupt001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.interrupt.interrupt001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".interrupt001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".breakHere";
    static final String MYTHREAD        = "MyThread";
    static final String DEBUGGEE_THREAD = DEBUGGEE_CLASS + "$" + MYTHREAD;
    static final String DEBUGGEE_RESULT = DEBUGGEE_CLASS + ".notInterrupted.get()";

    static int numThreads = nsk.jdb.interrupt.interrupt001.interrupt001a.numThreads;

    /*
     * Pattern for finding the thread ID in a line like the following:
     *   (nsk.jdb.interrupt.interrupt001.interrupt001a$MyThread)651 Thread-0          cond. waiting
     * Note we can't match on DEBUGGEE_THREAD because it includes a $, which Pattern
     * uses to match the end of a line.
     */
    private static Pattern tidPattern = Pattern.compile("\\(.+" + MYTHREAD + "\\)(\\S+)");

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        String found;
        String[] threads;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        threads = jdb.getThreadIds(DEBUGGEE_THREAD);

        if (threads.length != numThreads) {
            log.complain("jdb should report " + numThreads + " instance of " + DEBUGGEE_THREAD);
            log.complain("Found: " + threads.length);
            success = false;
        }

        pauseTillAllThreadsWaiting(threads);

        for (int i = 0; i < threads.length; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.interrupt + threads[i]);
        }

        reply = jdb.receiveReplyFor(JdbCommand.threads);
        reply = jdb.receiveReplyFor(JdbCommand.cont, true);

        reply = jdb.receiveReplyFor(JdbCommand.eval + DEBUGGEE_RESULT);
        grep = new Paragrep(reply);
        found = grep.findFirst(DEBUGGEE_RESULT + " =" );
        if (found.length() > 0) {
            if (found.indexOf(DEBUGGEE_RESULT + " = 0") < 0) {
               log.complain("Not all " + MYTHREAD + "s were interrupted.");
               log.complain(found);
               success = false;
            }
        } else {
            log.complain("TEST BUG: not found value for " + DEBUGGEE_RESULT);
        }

        jdb.contToExit(1);
    }

    private void pauseTillAllThreadsWaiting(String[] threads) {
        String[] reply;
        boolean tidswaiting = false;

        Set<String> tids = new HashSet<>(Arrays.asList(threads));
        Set<String> waitingTids = null;

        do {
            String[] thrdsRply = (String[])jdb.receiveReplyFor(JdbCommand.threads);
            waitingTids = Arrays.asList(thrdsRply).stream()
                .filter((r)-> r.endsWith("waiting"))
                .map((r)->{
                    Matcher m = tidPattern.matcher(r);
                    if (m.find()) {
                        return m.group(1);
                    }
                    return null;
                })
                .filter((r)-> r != null)
                .collect(Collectors.toSet());

            // If all Tids are waiting set allWorkersAreWaiting to true so
            // the main test thread will get out of its breakpoint loop
            // and continue with the test.
            if (waitingTids.containsAll(tids)) {
                reply = jdb.receiveReplyFor(JdbCommand.set + DEBUGGEE_CLASS + ".allWorkersAreWaiting=true");
                tidswaiting = true;
            } else {
                reply = jdb.receiveReplyFor(JdbCommand.cont);
            }
        } while (!tidswaiting);
    }
}
