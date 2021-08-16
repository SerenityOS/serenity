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
 * @summary converted from VM Testbase nsk/jdb/uncaught_exception/uncaught_exception002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This goal of this test is to verify that when the debugee
 *  throws an uncaught exception, jdb halts the execution at the point
 *  where the exception was thrown. The test verifies the following:
 *    - the locals variables at this point can be printed out,
 *    - "Exception occured" message is printed out.
 *  The test consists of two parts:
 *    uncaught_exception002.java  - test driver, i.e. launches jdb and debuggee,
 *                       writes commands to jdb, reads the jdb output,
 *    uncaught_exception002a.java - the debugged application.
 * COMMENTS
 *  The test is functionally equal to
 *  nsk/jdb/uncaught_exception/uncaught_exception002 test and replaces it.
 *  Modified due to fix of the bug:
 *  4818762 TEST_BUG: two jdb test incorrectly check debuggee exit code
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.uncaught_exception.uncaught_exception002.uncaught_exception002
 *
 * @comment make sure uncaught_exception002a is compiled w/ full debug info
 * @clean nsk.jdb.uncaught_exception.uncaught_exception002.uncaught_exception002a
 * @compile -g:lines,source,vars uncaught_exception002a.java
 *
 * @run main/othervm
 *      nsk.jdb.uncaught_exception.uncaught_exception002.uncaught_exception002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.uncaught_exception.uncaught_exception002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class uncaught_exception002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new uncaught_exception002(true).runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.uncaught_exception.uncaught_exception002";
    static final String TEST_CLASS = PACKAGE_NAME + ".uncaught_exception002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final String[] FRAMES = new String[] {
        DEBUGGEE_CLASS + ".func",
        DEBUGGEE_CLASS + ".runIt",
        DEBUGGEE_CLASS + ".main"                };

    public uncaught_exception002 (boolean debuggeeShouldFail) {
        super(debuggeeShouldFail);
    }

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.receiveReplyFor(JdbCommand.cont);
        jdb.receiveReplyFor(JdbCommand.locals);
        jdb.contToExit(1);

        reply = jdb.getTotalReply();
        grep = new Paragrep(reply);

        v = new Vector();
        v.add("Exception occurred");
        v.add(PACKAGE_NAME + ".TenMultipleException");
        if (grep.find(v) == 0) {
            failure("jdb does not reported of TenMultipleException occurence");
        }

        v = new Vector();
        v.add("localVar");
        v.add("1234");
        if (grep.find(v) == 0) {
            failure("Local variable of stack frame the exception was thrown " +
                "is not accessible");
        }
    }

    private boolean checkStop () {
        Paragrep grep;
        String[] reply;
        String found;
        Vector v;
        boolean result = true;

        return result;
    }
}
