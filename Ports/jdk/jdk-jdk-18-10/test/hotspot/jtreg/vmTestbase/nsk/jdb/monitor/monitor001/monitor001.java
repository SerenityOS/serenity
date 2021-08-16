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
 * @summary converted from VM Testbase nsk/jdb/monitor/monitor001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * A positive test for the 'monitor <command>' command.
 * The jdb sets up line breakpoint at the debugged application. Then four different
 * commands are set as a "monitor" by 'monitor <command>' command. After resuming the
 * debuggee stops at the breakpoint. All "monitored" commands should be executed.
 * The test passes if correct replies for all "monitored" commands are found in jdb stdout
 * stream.
 * The test consists of two program:
 *   monitor001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   monitor001a.java - the debugged application.
 * COMMENTS
 *  The test was updated due fix of the bug 4686159.
 *  Test fixed according to test bug:
 *  5041797 TEST_BUG: race condition in JDB output readirectors
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.monitor.monitor001.monitor001a
 * @run main/othervm
 *      nsk.jdb.monitor.monitor001.monitor001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.monitor.monitor001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class monitor001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new monitor001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.monitor.monitor001";
    static final String TEST_CLASS = PACKAGE_NAME + ".monitor001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final int    LINE_NUMBER        = 47;

    static final String[] CHECKED_COMMANDS = {
        JdbCommand.threads,
        JdbCommand.methods + DEBUGGEE_CLASS,
        JdbCommand.fields  + DEBUGGEE_CLASS,
        JdbCommand.eval + "(new java.lang.String(\"Hello, World\")).length()"
                                             };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        reply = jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + LINE_NUMBER);

        for (int i = 0; i < CHECKED_COMMANDS.length; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[i]);
        }

        int repliesCount = CHECKED_COMMANDS.length + 1;
        reply = jdb.receiveReplyFor(JdbCommand.cont, true, repliesCount);

        reply = jdb.receiveReplyFor(JdbCommand.monitor);
        if (!checkMonitors(reply)) {
            success = false;
        }

        jdb.contToExit(1);

        reply = jdb.getTotalReply();
        if (!checkCommands(reply)) {
            success = false;
        }
    }

    private boolean checkMonitors(String[] reply) {
        Paragrep grep;
        String found;
        Vector v;
        boolean result = true;
        int count;

        grep = new Paragrep(reply);
        for (int i = 0; i < CHECKED_COMMANDS.length; i++) {
            if ((count = grep.find(CHECKED_COMMANDS[i])) != 1) {
                log.complain("Wrong number of monitor command: " + CHECKED_COMMANDS[i]);
                log.complain("    Expected: 1; found: " + count);
                result = false;
            }
        }

        return result;
    }

    private boolean checkCommands(String[] reply) {
        Paragrep grep;
        String found;
        Vector v = new Vector();
        boolean result = true;
        int count;

        grep = new Paragrep(reply);

        // check 'threads'
        v.add("java.lang.Thread");
        v.add("main");
        if ((count = grep.find(v)) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[0]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'methods'
        if ((count = grep.find("runIt(java.lang.String[], java.io.PrintStream)")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[1]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'fields'
        if ((count = grep.find(DEBUGGEE_CLASS + " _monitor001a")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[2]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'eval'
        if ((count = grep.find("length() = 12")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[3]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        return result;
    }
}
