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
 * @summary converted from VM Testbase nsk/jdb/exclude/exclude001.
 * VM Testbase keywords: [jpda, jdb, quarantine]
 * VM Testbase comments: 8191037
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'exclude' command.
 * The debuggee program (exclude001a.java) starts three
 * addional threads of MyThread class. The 'run' method of these
 * threads invokes java.lang.System.currentTimeMillis() and
 * sun.util.calendar.Gregorian() methods.
 * There are three test cases:
 *  - block all exclude filter;
 *  - modified exclude filter allowing tracing events for java.* methods,
 *    which is set with 'exclude javax.*,sun.*,com.sun.*,jdk.*' command;
 *  - modified exclude filter allowing tracing events for sun.* methods,
 *    which is set with 'exclude java.*,javax.*,com.sun.*,jdk.*' command.
 *  - non-modified, predefined exclude filter;
 *  - modified exclude filter allowing tracing events for java.* methods,
 *    which is set with 'exclude javax.*,sun.*,com.sun.*' command;
 *  - modified exclude filter allowing tracing events for all system methods,
 *    which is set with 'exclude none' command.
 * For each test case the correspondent MyThread thread is started and
 * suspended at a breakpoint. Then method tracing is turned on with
 * 'trace methods <thread id>' command with correspondent exclude filtering.
 * The test passes if debuggee suspends on method enter/exit of only
 * filtered classes, i.e. in comply with exclude filter previously set.
 * The test consists of two program:
 *   exclude001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   exclude001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.exclude.exclude001.exclude001a
 * @run main/othervm/timeout=600
 *      nsk.jdb.exclude.exclude001.exclude001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=10
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.exclude.exclude001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class exclude001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new exclude001().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.exclude.exclude001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".exclude001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";
    static final String MYTHREAD        = "MyThread";
    static final String DEBUGGEE_THREAD = PACKAGE_NAME + "." + MYTHREAD;

    static final String JAVA_CORE_METHOD = "java.lang.System.currentTimeMillis";
    static final String SUN_METHOD   = "sun.util.calendar.Gregorian";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;
        String[] threads;

        String oldExclude = "";
        boolean javaTraced = false;
        boolean comTraced = false;
        boolean nskTraced = false;

        jdb.setBreakpointInMethod(LAST_BREAK);

        // getting predefined 'exclude' value
        reply = jdb.receiveReplyFor(JdbCommand.exclude);
        if (reply.length == 0) {
            log.complain("Predefined excluded lists of classes is empty");
            success = false;
        } else {

            oldExclude = reply[0];

            for (int testCase = 0; testCase < exclude001a.numThreads; testCase++) {
                String expectedPrompt = MYTHREAD + "-" + testCase + "[1]";
                reply = jdb.receiveReplyFor(JdbCommand.cont);

                if (jdb.isAtBreakpoint(reply, LAST_BREAK)) {

                    threads = jdb.getThreadIds(DEBUGGEE_THREAD);

                    if (threads.length != 1) {
                        log.complain("jdb should report 1 instance of " + DEBUGGEE_THREAD);
                        log.complain("Found: " + threads.length);
                        success = false;
                    } else {

                        reply = jdb.receiveReplyFor(JdbCommand.step); // to get out of lastBreak;

                        switch (testCase) {
                        case 0: // block all
                                reply = jdb.receiveReplyFor(JdbCommand.exclude + "java.*,javax.*,sun.*,com.sun.*,jdk.*");

                                break;
                        case 1: // allow java.*
                                reply = jdb.receiveReplyFor(JdbCommand.exclude + "javax.*,sun.*,com.sun.*,jdk.*");
                                break;
                        case 2: // allow sun.*
                                reply = jdb.receiveReplyFor(JdbCommand.exclude + "java.*,javax.*,com.sun.*,jdk.*");
                                break;
                        }

                        reply = jdb.receiveReplyFor(JdbCommand.trace + "methods " + threads[0]);

                        while (true) {
                            reply = jdb.receiveReplyForWithMessageWait(JdbCommand.cont, expectedPrompt);

                            grep = new Paragrep(reply);
                            count = grep.find(JAVA_CORE_METHOD);
                            if (count > 0) {
                                if (testCase != 0) {
                                    javaTraced = true;
                                } else {
                                    log.complain("Trace message for excluded method: " + JAVA_CORE_METHOD);
                                }
                            }

                            count = grep.find(SUN_METHOD);
                            if (count > 0) {
                                if (testCase == 2) {
                                    comTraced = true;
                                } else {
                                    log.complain("Trace message for excluded method: " + SUN_METHOD);
                                }
                            }

                            count = grep.find(DEBUGGEE_THREAD + ".run");
                            if (count > 0) {
                                nskTraced = true;

                                reply = jdb.receiveReplyFor(JdbCommand.exclude + oldExclude);
                                reply = jdb.receiveReplyFor(JdbCommand.untrace + "methods "+ threads[0]);
                                break;
                            }
                        }
                    }
                }
            }
        }

        jdb.contToExit(2);

        if (!javaTraced) {
            log.complain("There were no tracing events for " + JAVA_CORE_METHOD + "() method while turned off filter");
            success = false;
        }
        if (!comTraced) {
            log.complain("There were no tracing events for " + SUN_METHOD + "() method while turned off filter");
            success = false;
        }
        if (!nskTraced) {
            log.complain("There were no tracing events for " + DEBUGGEE_THREAD + ".run() method ");
            success = false;
        }
    }
}
