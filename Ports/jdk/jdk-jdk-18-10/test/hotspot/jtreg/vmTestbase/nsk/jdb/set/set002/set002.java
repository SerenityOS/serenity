/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdb/set/set002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'set <lvalue> = <expr>' command.
 * The test checks if jdb correctly sets value for the following
 * fields and variables:
 *  - element of array field,
 *  - local variable
 * Probably needs to be merged with set001
 * The jdb suspends the debuggee inside the method runIt and then tries
 * to set new values for the fields and variables using checked command.
 * Then the debuggee checks if modified fields/variables have expected
 * values. If not, then special errorMessage variable is appended with
 * the info of wrong values. The test passes when length of errorMessage
 * is equal to 0, and fails otherwise.
 * The test consists of two programs:
 *   set002.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   set002a.java - the debugged application.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.set.set002.set002
 *
 * @comment make sure set002a is compiled w/ full debug info
 * @clean nsk.jdb.set.set002.set002a
 * @compile -g:lines,source,vars set002a.java
 *
 * @run main/othervm
 *      nsk.jdb.set.set002.set002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.set.set002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class set002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;

        return new set002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.set.set002";
    static final String TEST_CLASS = PACKAGE_NAME + ".set002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final String ERROR_MESSAGE = DEBUGGEE_CLASS + ".errorMessage";

    static final String[][] checkedExpr = {  //not broken, can be run safely even if 4660158 is not fixed
        { DEBUGGEE_CLASS + "._set002a.myArrayField[0][0].line", "\"ABCDE\"" },
        { "localInt", "java.lang.Integer.MIN_VALUE"}
                                          };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        // to get out of lastBreak()
        reply = jdb.receiveReplyFor(JdbCommand.step);

        // set values
        for (int i = 0; i < checkedExpr.length; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.set + checkedExpr[i][0] + " = " + checkedExpr[i][1]);
        }

        reply = jdb.receiveReplyFor(JdbCommand.cont);
        // check value of debuggeeResult
        reply = jdb.receiveReplyFor(JdbCommand.eval + ERROR_MESSAGE);

        //if everything is OK reply will look like this
        //  nsk.jdb.set.set002.set002a.errorMessage = ""
        if (!reply[0].contains("\"\"")) {
            log.complain("jdb failed to set value for expression(s): ");
            for (int i = 0; i < reply.length; i++) {
                log.complain(reply[i]);
            }
            success = false;
        }

        jdb.contToExit(1);
    }
}
