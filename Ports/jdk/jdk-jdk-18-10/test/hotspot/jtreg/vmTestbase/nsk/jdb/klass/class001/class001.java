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
 * @summary converted from VM Testbase nsk/jdb/class/class001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'class <class_id>' command.
 * The test checks if jdb correctly replies on request for
 * given debuggee class via 'class' command.
 * The test fails if :
 *   - string "is not a valid" found in reply,
 *   - reply does not contain full name of a checked class.
 * Otherwise the test passes.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.klass.class001.class001a
 * @run main/othervm
 *      nsk.jdb.klass.class001.class001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.klass.class001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class class001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new class001().runTest(argv, out);
    }

    static final String PACKAGE_NAME       = "nsk.jdb.klass.class001";
    static final String TEST_CLASS         = PACKAGE_NAME + ".class001";
    static final String DEBUGGEE_CLASS     = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final String NOT_VALID_SAMPLE   = "is not a valid";

    static String[] checkedClasses = {
        DEBUGGEE_CLASS,
        DEBUGGEE_CLASS + "$InnerInt1",
        DEBUGGEE_CLASS + "$Inner2",
        DEBUGGEE_CLASS + "$Inner3",
        DEBUGGEE_CLASS + "$Inner4",
        DEBUGGEE_CLASS + "$Inner5",
        DEBUGGEE_CLASS + "$Inner6",
        PACKAGE_NAME + ".Outer1"
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

        for (int i = 0; i < checkedClasses.length; i++) {
            if (!checkClass(checkedClasses[i])) {
                success = false;
            }
        }

        jdb.contToExit(1);
    }

    private boolean checkClass (String className) {
        String[] reply;
        Paragrep grep;
        int count;
        String found;
        boolean result = true;

        reply = jdb.receiveReplyFor(JdbCommand._class + className);

        grep = new Paragrep(reply);
        found = grep.findFirst(NOT_VALID_SAMPLE);
        if (found.length() > 0) {
            log.complain("Failed to report class " + className);
            result = false;
        }
        found = grep.findFirst(className);
        if (found.length() == 0) {
            log.complain("Failed to report class " + className);
            result = false;
        }
        return result;
    }
}
