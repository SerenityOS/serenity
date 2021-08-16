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
 * @summary converted from VM Testbase nsk/jdb/eval/eval001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'eval <expr>' command.
 * The test checks if jdb correctly prints values for the following
 * expressions:
 *  - static field,
 *  - instance field,
 *  - element of array field,
 *  - return value of a method,
 *  - arithmetic expression of local variables,
 *  - return value of public method of the java.lang.String class.
 * The test passes when all printed values are equal to expected ones.
 * The test consists of two program:
 *   eval001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   eval001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.eval.eval001.eval001
 *
 * @comment make sure eval001a is compiled w/ full debug info
 * @clean nsk.jdb.eval.eval001.eval001a
 * @compile -g:lines,source,vars eval001a.java
 *
 * @run main/othervm
 *      nsk.jdb.eval.eval001.eval001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.eval.eval001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class eval001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new eval001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.eval.eval001";
    static final String TEST_CLASS = PACKAGE_NAME + ".eval001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] checkedExpr = {
        { DEBUGGEE_CLASS + ".myStaticField", "-2147483648" },
        { DEBUGGEE_CLASS + "._eval001a.myInstanceField", "9223372036854775807" },
        { DEBUGGEE_CLASS + "._eval001a.myArrayField[0][0].toString()", "ABCDE" },
        { DEBUGGEE_CLASS + "._eval001a.myMethod()", "2147483647" },
        { "myClass.toString().equals(\"abcde\")", "true"},
        { "i + j + k", "777"},
        { "new java.lang.String(\"Hello, World\").length()", "12"},
        { DEBUGGEE_CLASS + "._eval001a.testPrimitiveArray(test)", "1.0" }
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

        for (int i = 0; i < checkedExpr.length; i++) {
            if (!checkValue(checkedExpr[i][0], checkedExpr[i][1])) {
                success = false;
            }
        }

        jdb.contToExit(1);
    }

    private boolean checkValue (String expr, String value) {
        Paragrep grep;
        String[] reply;
        String found;
        Vector v;
        boolean result = true;

        reply = jdb.receiveReplyFor(JdbCommand.eval + expr);
        grep = new Paragrep(reply);
        found = grep.findFirst(value);
        if (found.length() <= 0) {
            log.complain("jdb failed to report value of expression: " + expr);
            log.complain("expected : " + value + " ;\nreported: " + (reply.length > 0? reply[0]: ""));
            result = false;
        }
        return result;
    }
}
