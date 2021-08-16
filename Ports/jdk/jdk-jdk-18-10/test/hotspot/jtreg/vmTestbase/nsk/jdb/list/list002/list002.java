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
 * @summary converted from VM Testbase nsk/jdb/list/list002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This is a test for the 'list' command.
 *  It lists the source of given method and the source on a given line.
 *  The test passes if it find expected strings in jdb's output.
 *  The test consists of two parts:
 *    list002.java  - test driver, i.e. launches jdb and debuggee,
 *                    writes commands to jdb, reads the jdb output,
 *    list002a.java - the debugged application.
 * COMMENTS
 *  The test functionally equals to nsk/jdb/list/list001 test
 *  and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.list.list002.list002a
 * @run driver jdk.test.lib.FileInstaller list002a.java src/nsk/jdb/list/list002/list002a.java
 * @run main/othervm
 *      nsk.jdb.list.list002.list002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -jdb.option="-sourcepath src/"
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.list.list002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class list002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new list002().runTest(argv, out);
    }

    static final String PACKAGE_NAME    = "nsk.jdb.list.list002";
    static final String TEST_CLASS      = PACKAGE_NAME + ".list002";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".runIt";
    static final int    LINE_NUMBER     = 38;

    final static String METHOD_SOURCE[] = new String[] {
        "public int runIt(String args[], PrintStream out) {",
        "JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);",
        "Log log = new Log(out, argumentHandler);",
        "log.display(\"Debuggee PASSED\");",
        "return list002.PASSED;"
                                                    };

    final static String LINE_SOURCE =
        "System.exit(list002.JCK_STATUS_BASE + _list002a.runIt(args, System.out));";

    final static String SOURCE_NOT_FOUND = "Source file not found";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.list + "runIt");
        grep = new Paragrep(reply);
        if (grep.find(SOURCE_NOT_FOUND) > 0) {
            failure(reply[0]);
        } else {
            for (int i = 0; i < METHOD_SOURCE.length; i++) {
                if (grep.find(METHOD_SOURCE[i]) == 0) {
                    failure("Line is not found in method sources:\n\t"+
                        METHOD_SOURCE[i]);
                }
            }
        }

        reply = jdb.receiveReplyFor(JdbCommand.list + LINE_NUMBER);
        grep = new Paragrep(reply);
        if (grep.find(SOURCE_NOT_FOUND) > 0) {
            failure(reply[0]);
        } else {
            if (grep.find(LINE_SOURCE) == 0) {
                failure("Line " + LINE_NUMBER + " is not found:\n\t"+
                    LINE_SOURCE);
            }
        }

        jdb.contToExit(1);
    }
}
