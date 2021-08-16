/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdb/step/step002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * The test for the 'step' command.
 * The test checks the following cases:
 *   - step inside current method,
 *   - step into called method,
 *   - step up to calling method.
 * The test works as follows. The jdb sets breakpoint at the line
 * where new value is assigned to local variable. The jdb issues
 * 'step' command. Then the test checks whether a step is done by
 * requesting a value of local variable using 'eval' command.
 * The next two cases are checked with a pair of 'step' and
 * 'where' commands. The test checks a stack trace after step
 * using 'where' command.
 * The test consists of two program:
 *   step002.java  - test driver, i.e. launches jdb and debuggee,
 *                   writes commands to jdb, reads the jdb output,
 *   step002a.java - the debugged application.
 * COMMENTS
 * This test replaces the nsk/jdb/step/step002 one.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.step.step002.step002
 *
 * @comment make sure step002a is compiled w/ full debug info
 * @clean nsk.jdb.step.step002.step002a
 * @compile -g:lines,source,vars step002a.java
 *
 * @run main/othervm
 *      nsk.jdb.step.step002.step002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.step.step002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class step002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new step002().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.step.step002";
    static final String TEST_CLASS      = PACKAGE_NAME + ".step002";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";
    static final int    BREAKPOINT_LINE = 50;

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;
        String[] threads;

        reply = jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + BREAKPOINT_LINE);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        // case #1 : step inside frame;
        reply = jdb.receiveReplyFor(JdbCommand.step);
        reply = jdb.receiveReplyFor(JdbCommand.eval + "intVar");
        grep = new Paragrep(reply);
        if (grep.find("1234") == 0) {
            failure("CASE #1 FAILED: Wrong location after step inside current method");
        }

        // case #1 : step into called frame;
        reply = jdb.receiveReplyFor(JdbCommand.step);
        reply = jdb.receiveReplyFor(JdbCommand.where);
        grep = new Paragrep(reply);
        if (grep.find("foo") == 0) {
            failure("CASE #2 FAILED: Wrong location after step into called method");
        }

        // case #1 : step out to calling frame;
        reply = jdb.receiveReplyFor(JdbCommand.step);
        reply = jdb.receiveReplyFor(JdbCommand.where);
        grep = new Paragrep(reply);
        if (grep.find("foo") > 0) {
            failure("CASE #2 FAILED: Wrong location after step up to calling method");
        }
        if (grep.find("runIt") == 0) {
            failure("CASE #2 FAILED: Wrong location after step up to calling method");
        }

        jdb.contToExit(1);
    }
}
