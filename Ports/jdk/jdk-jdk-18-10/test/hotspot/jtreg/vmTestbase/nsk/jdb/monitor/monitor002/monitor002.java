/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JDB problem running monitor command
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * Make sure 'monitor unmonitor 1' does not cause ConcurrentModificationException
 * in the debugger.
 * The jdb sets up line breakpoint at the debugged application. Then one command
 * 'monitor unmonitor 1' is set. After resuming the debuggee stops at the breakpoint.
 * The test passes if correct reply for "unmonitor 1" commanda is found in jdb stdout
 * stream.
 * The test consists of two program:
 *   monitor002.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   monitor002a.java - the debugged application.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.monitor.monitor002.monitor002a
 * @run main/othervm
 *      nsk.jdb.monitor.monitor002.monitor002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.monitor.monitor002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class monitor002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new monitor002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.monitor.monitor002";
    static final String TEST_CLASS = PACKAGE_NAME + ".monitor002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final int    LINE_NUMBER        = 47;

    static final String[] CHECKED_COMMANDS = {
        JdbCommand.unmonitor + "1"
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
        if (reply.length != 1) {
            log.complain("Expected no active monitors after exectuting monitored command: " + CHECKED_COMMANDS[0]);
            success = false;
        }

        jdb.contToExit(1);

        reply = jdb.getTotalReply();

        if (!checkCommands(reply)) {
            success = false;
        }
    }

    private boolean checkCommands(String[] reply) {
        Paragrep grep;
        boolean result = true;
        int count;

        grep = new Paragrep(reply);

        if ((count = grep.find("Unmonitoring 1:  unmonitor 1")) != 1) {
            log.complain("Wrong number of execution of monitored command: " + CHECKED_COMMANDS[0]);
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        return result;
    }
}
