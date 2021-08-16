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
 * @summary converted from VM Testbase nsk/jdb/print/print002.
 * VM Testbase keywords: [quick, jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * This is a test for the 'print <expr>' command.
 * The test checks if jdb correctly prints values for the following
 * expressions:
 *  - arithmetic expression of local variables,
 *  - boolean expression of local variables,
 *  - string field deeply nested in class hierarchy.
 * The test passes when all printed values are equal to expected ones.
 * The test consists of two program:
 *   print002.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   print002a.java - the debugged application.
 * COMMENTS
 * The test replaces the nsk/jdb/print/print001 one.
 * Test was fixed according to test bug:
 *     4862693 NSK testcase nsk/jdb/print/print002 fails with operation not yet supported
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.print.print002.print002
 *
 * @comment make sure print002a is compiled w/ full debug info
 * @clean nsk.jdb.print.print002.print002a
 * @compile -g:lines,source,vars print002a.java
 *
 * @run main/othervm
 *      nsk.jdb.print.print002.print002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.print.print002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class print002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new print002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.print.print002";
    static final String TEST_CLASS = PACKAGE_NAME + ".print002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final String[][] checkedExpr = {
        { "i + j", "8"},
        { "j - i", "4"},
        { "j * i", "12"},
        { "j / i", "3"},
//        { "j % i", "0"},
//        { "i++",   "2"},
//        { "++i",   "3"},
//        { "j--",   "6"},
//        { "--j",   "5"},
//        { "!b1 ",   "false"},
//        { "b2 && b1", "false"},
//        { "b2 || b1", "true"},
        { "a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.s", "foo" }
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

        reply = jdb.receiveReplyFor(JdbCommand.print + expr);
        grep = new Paragrep(reply);
        found = grep.findFirst(value);
        if (found.length() <= 0) {
            log.complain("jdb failed to report value of expression: " + expr);
            log.complain("\t expected : " + value + " ;\n\t reported: " + (reply.length > 0? reply[0]: ""));
            result = false;
        }
        return result;
    }
}
