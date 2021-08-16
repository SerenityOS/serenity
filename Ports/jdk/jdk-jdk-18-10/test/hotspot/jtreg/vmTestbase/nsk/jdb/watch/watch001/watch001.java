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
 * @summary converted from VM Testbase nsk/jdb/watch/watch001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'watch access <class id>.<field name>' command.
 * There are two test cases:
 *  - access watch set for the fields defined in class,
 *  - access watch set for the fields defined in inner class.
 * The debugged application invokes the methods in which all checked fields
 * participate in assigned expressions.
 * The test passes jdb correctly reports on access event for all checked fields.
 * Correct report message in jdb stdout should contain full name of the field
 * and "access encountered" words.
 * The test consists of two program:
 *   watch001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   watch001a.java - the debugged application.
 * COMMENTS
 * Test fixed according to test bug:
 *     5045859 TEST_BUG: some JDB tests do not recognize JDB prompt
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.watch.watch001.watch001a
 * @run main/othervm
 *      nsk.jdb.watch.watch001.watch001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.watch.watch001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class watch001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        compoundPromptIdent = COMPOUND_PROMPT_IDENT;
        return new watch001().runTest(argv, out);
    }

    static final String PACKAGE_NAME       = "nsk.jdb.watch.watch001";
    static final String TEST_CLASS         = PACKAGE_NAME + ".watch001";
    static final String DEBUGGEE_CLASS     = TEST_CLASS + "a";
    static final String DEBUGGEE_CLASS2    = DEBUGGEE_CLASS + "$CheckedFields";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".breakHere";
    static final String COMPOUND_PROMPT_IDENT = "main";

    static String[] checkedFields  = { "fS0", "FS1" };
    static String[] checkedFields2 = { "FP0", "FU1", "FR0", "FT1", "FV0" };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);

        reply = jdb.receiveReplyFor(JdbCommand.fields + DEBUGGEE_CLASS);

        reply = jdb.receiveReplyFor(JdbCommand.fields + DEBUGGEE_CLASS2);

        watchFields (DEBUGGEE_CLASS, checkedFields);
        watchFields (DEBUGGEE_CLASS2, checkedFields2);

        jdb.contToExit(checkedFields.length + checkedFields2.length + 2);

        reply = jdb.getTotalReply();
        if (!checkFields (DEBUGGEE_CLASS, reply, checkedFields)) {
            success = false;
        }
        if (!checkFields (DEBUGGEE_CLASS2, reply, checkedFields2)) {
            success = false;
        }
    }

    private void watchFields (String className, String[] checkedFields) {
        String[] reply;

        for (int i = 0; i < checkedFields.length; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.watch + " access " + className + "." + checkedFields[i]);
        }

    }

    private boolean checkFields (String className, String[] reply, String[] checkedFields) {
        Paragrep grep;
        String found;
        boolean result = true;
        int count;
        Vector v = new Vector();

        grep = new Paragrep(reply);
        v.add("access encountered");
        for (int i = 0; i < checkedFields.length; i++) {
            v.removeAllElements();
            v.add("access encountered");
            v.add(className + "." + checkedFields[i]);

            found = grep.findFirst(v);
            if (found.length() == 0) {
                log.complain("Failed to report access to field " + className + "." + checkedFields[i]);
                result = false;
            }
        }
        return result;
    }
}
