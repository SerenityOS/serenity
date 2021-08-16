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
 * @summary converted from VM Testbase nsk/jdb/caught_exception/caught_exception002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  A positive test case for the 'catch caught <exception>' command.
 *  When the debugee throws an exception that is caught somewhere
 *  up in the program stack, jdb does not halt execution at the point
 *  the exception was thrown but continues to execute the code in the catch
 *  clause and proceed on with the rest of the program.
 *  To halt execution at the throw point for exceptions
 *  that are being caught - the 'catch' command has to be used.
 *  The test throws ten exceptions that are on jdb's catch list
 *  and verifies that jdb halts execution at the throw point each
 *  time an exception is thrown.
 * COMMENTS
 *  This test functionally equals to
 *  nsk/jdb/caught_exception/caught_exception001 test and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.caught_exception.caught_exception002.caught_exception002a
 * @run main/othervm
 *      nsk.jdb.caught_exception.caught_exception002.caught_exception002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.caught_exception.caught_exception002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class caught_exception002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new caught_exception002().runTest(argv, out);
    }

    static final String PACKAGE_NAME       = "nsk.jdb.caught_exception.caught_exception002";
    static final String TEST_CLASS         = PACKAGE_NAME + ".caught_exception002";
    static final String DEBUGGEE_CLASS     = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final int MAX = 10;

    /* ------------------------------------- */

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        for (int i = 0; i < MAX; i++) {
            reply = jdb.receiveReplyFor(JdbCommand._catch + " caught " + PACKAGE_NAME + ".MyException" + i);
        }

        for (int i = 0; i < MAX; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.cont);
            checkCatch(reply, i);
        }

        jdb.contToExit(1);
    }

    private void checkCatch (String[] reply, int i) {
        Paragrep grep;
        int count;
        String found;
        Vector v = new Vector();

        grep = new Paragrep(reply);
        v.add("Exception occurred");
        v.add(PACKAGE_NAME + ".MyException" + i);
        count = grep.find(v);
        if (count != 1) {
            log.complain("Failed to report catch of MyException" + i + " : " + count);
            success = false;
        }
    }
}
