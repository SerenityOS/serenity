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
 * @summary converted from VM Testbase nsk/jdb/dump/dump002.
 * VM Testbase keywords: [quick, jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This is a test for the 'dump' command.
 *  The test works as follows. Upon the debuggee is suspended
 *  on breakpoint, jdb issues 'dump' command for an object
 *  of debugged class and for element in array fields.
 *  The test passes if all checked fields with their values
 *  are listed.
 *  The test consists of two parts:
 *    dump002.java  - test driver, i.e. launches jdb and debuggee,
 *                       writes commands to jdb, reads the jdb output,
 *    dump002a.java - the debugged application.
 * COMMENTS
 *  The test replaces the nsk/jdb/dump/dump001 one.
 *  Test fixed according to test bug:
 *  5045859 TEST_BUG: some JDB tests do not recognize JDB prompt
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.dump.dump002.dump002a
 * @run main/othervm
 *      nsk.jdb.dump.dump002.dump002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.dump.dump002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class dump002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        compoundPromptIdent = COMPOUND_PROMPT_IDENT;
        return new dump002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.dump.dump002";
    static final String TEST_CLASS = PACKAGE_NAME + ".dump002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final String COMPOUND_PROMPT_IDENT = "main";

    static final String[] CHECKED_FIELDS = {
        "_dump002a",
        "iStatic",
        "iPrivate",
        "iProtect",
        "iPublic",
        "iFinal",
        "iTransient",
        "iVolatile",
        "iArray",
        "sStatic",
        "sPrivate",
        "sProtected",
        "sPublic",
        "sFinal",
        "sTransient",
        "sVolatile",
        "sArray",
        "fBoolean",
        "fByte",
        "fChar",
        "fDouble",
        "fFloat",
        "fInt",
        "fLong",
        "fShort"
                                         };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v = new Vector();
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.dump + DEBUGGEE_CLASS + "._dump002a");
        grep = new Paragrep(reply);
        for (int i = 0; i < CHECKED_FIELDS.length; i++) {
            v.setSize(0);
            v.add(CHECKED_FIELDS[i]);
            v.add("null");
            if (grep.find(v) > 0) {
                failure("The field is not dumped : " + CHECKED_FIELDS[i]);
            }
        }

        String checkedField = DEBUGGEE_CLASS + ".iArray[0]";
        reply = jdb.receiveReplyFor(JdbCommand.dump + checkedField);
        checkField(reply, checkedField);

        checkedField = DEBUGGEE_CLASS + ".sArray[0]";
        reply = jdb.receiveReplyFor(JdbCommand.dump + checkedField);
        checkField(reply, checkedField);

        jdb.contToExit(1);
    }

    void checkField (String[] reply, String fieldName) {
        Paragrep grep;
        Vector v = new Vector();

        grep = new Paragrep(reply);
        v.setSize(0);
        v.add(fieldName);
        v.add("null");
        if (grep.find(v) > 0) {
            failure("The field is not dumped : " + fieldName);
        }
    }
}
