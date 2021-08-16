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
 * @summary converted from VM Testbase nsk/jdb/unmonitor/unmonitor001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * A positive test for the 'unmonitor <monitor#>' command.
 * The jdb sets up line breakpoint at the debugged application. Then
 * the  following set of commands is called:
 *  - three different commands are set as a "monitor" by 'monitor <command>',
 *  - the first "monitored" command (i.e. 'locals') is deleted by 'unmonitor 1' command,
 *  - another two command are set as a "monitor".
 * After resuming the debuggee stops at the breakpoint. All "monitored" commands should
 * be executed.
 * The test passes if only un-deleted "monitored" commands are executed and number
 * of "monitors" equals to 4.
 * The test consists of two program:
 *   unmonitor001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   unmonitor001a.java - the debugged application.
 * COMMENTS
 *  The test was updated due fix of the bug 4686159.
 *  The test was updated due to fix of the RFE 4905231.
 *  Test fixed according to test bug:
 *    5041797 TEST_BUG: race condition in JDB output readirectors
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.unmonitor.unmonitor001.unmonitor001
 *
 * @comment make sure unmonitor001a is compiled w/ full debug info
 * @clean nsk.jdb.unmonitor.unmonitor001.unmonitor001a
 * @compile -g:lines,source,vars unmonitor001a.java
 *
 * @run main/othervm
 *      nsk.jdb.unmonitor.unmonitor001.unmonitor001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.unmonitor.unmonitor001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class unmonitor001 extends JdbTest {

    public static void main (String argv[])  {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new unmonitor001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.unmonitor.unmonitor001";
    static final String TEST_CLASS = PACKAGE_NAME + ".unmonitor001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final int    BREAKPOINT_LINE    = 47;

    static final String[] CHECKED_COMMANDS = {
        JdbCommand.locals,
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

        reply = jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + BREAKPOINT_LINE);

        // set first three monitors
        reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[0]);
        reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[1]);
        reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[2]);

        // delete first monitor
        reply = jdb.receiveReplyFor(JdbCommand.unmonitor + " 1");

        // set first last two monitors
        reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[3]);
        reply = jdb.receiveReplyFor(JdbCommand.monitor + CHECKED_COMMANDS[4]);

        int repliesCount = 4 + 1;
        reply = jdb.receiveReplyFor(JdbCommand.cont, true, repliesCount);

        reply = jdb.receiveReplyFor(JdbCommand.monitor);
        if (!checkMonitors(reply)) {
            success = false;
        }

        // Give some time to evaluate an expression
        jdb.waitForMessage(0, "length() = 12");

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
        for (int i = 1; i < CHECKED_COMMANDS.length; i++) {
            if ((count = grep.find(CHECKED_COMMANDS[i])) != 1) {
                log.complain("Wrong number of monitor command: " + CHECKED_COMMANDS[i]);
                log.complain("    Expected: 1; found: " + count);
                result = false;
            }
        }

        if ((count = grep.find(CHECKED_COMMANDS[0])) > 0) {
            log.complain("Found deleted monitor: " + CHECKED_COMMANDS[0]);
            log.complain("    Expected: 0; found: " + count);
            result = false;
        }

        return result;
    }

    private boolean checkCommands(String[] reply) {
        Paragrep grep;
        String found;
        Vector v = new Vector();;
        boolean result = true;
        int count;

        grep = new Paragrep(reply);

        // check deleted 'locals'
        if ((count = grep.find("Local variables")) > 0) {
            log.complain("Found execution of deleted monitor command: " + CHECKED_COMMANDS[0]);
            result = false;
        }

        // check 'threads'
        v.add("java.lang.Thread");
        v.add("main");
        if ((count = grep.find(v)) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[1]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'methods'
        if ((count = grep.find("runIt(java.lang.String[], java.io.PrintStream)")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[2]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'fields'
        if ((count = grep.find(DEBUGGEE_CLASS + " _unmonitor001a")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[3]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
        // check 'eval'
        if ((count = grep.find("length() = 12")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[4]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        return result;
    }
}
