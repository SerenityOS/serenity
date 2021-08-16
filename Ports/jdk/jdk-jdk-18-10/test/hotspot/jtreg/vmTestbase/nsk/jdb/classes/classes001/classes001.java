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
 * @summary converted from VM Testbase nsk/jdb/classes/classes001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'classes' command.
 * The test checks if jdb correctly replies on 'classes' command.
 * The test passes when reply contains full names of all checked classes.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.classes.classes001.classes001a
 * @run main/othervm
 *      nsk.jdb.classes.classes001.classes001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.classes.classes001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class classes001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new classes001().runTest(argv, out);
    }

    static final String PACKAGE_NAME       = "nsk.jdb.classes.classes001";
    static final String TEST_CLASS         = PACKAGE_NAME + ".classes001";
    static final String DEBUGGEE_CLASS     = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final String NOT_VALID_SAMPLE   = "is not a valid";

    static String[] checkedClasses = {
        DEBUGGEE_CLASS,
        DEBUGGEE_CLASS + "$Inner1",
        DEBUGGEE_CLASS + "$Inner2",
        DEBUGGEE_CLASS + "$Inner3",
        DEBUGGEE_CLASS + "$Inner4",
        DEBUGGEE_CLASS + "$Inner5",
        DEBUGGEE_CLASS + "$Inner6",
        DEBUGGEE_CLASS + "$Inner7",
        DEBUGGEE_CLASS + "$Inner8",
        DEBUGGEE_CLASS + "$InnerInt1",
        DEBUGGEE_CLASS + "$InnerInt2",
        DEBUGGEE_CLASS + "$InnerInt3",
        DEBUGGEE_CLASS + "$InnerInt4",
        DEBUGGEE_CLASS + "$InnerInt5",
        PACKAGE_NAME + ".Outer1",
        PACKAGE_NAME + ".Outer2",
        PACKAGE_NAME + ".Outer3",
        PACKAGE_NAME + ".OuterInt1",
        PACKAGE_NAME + ".OuterInt2"
                                      };

    /* ------------------------------------- */

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.classes);

        for (int i = 0; i < checkedClasses.length; i++) {
            if (!checkClass(checkedClasses[i], reply)) {
                success = false;
            }
        }

        jdb.contToExit(1);
    }

    private boolean checkClass (String className, String[] reply) {
        Paragrep grep;
        String found;
        boolean result = true;

        grep = new Paragrep(reply);
        found = grep.findFirst(className);
        if (found.length() == 0) {
            log.complain("Failed to report class " + className);
            result = false;
        }
        return result;
    }
}
