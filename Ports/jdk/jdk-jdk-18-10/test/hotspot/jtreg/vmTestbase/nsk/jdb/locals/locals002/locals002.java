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
 * @summary converted from VM Testbase nsk/jdb/locals/locals002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This is a test for the 'locals' command.
 *  The test sets breakpoints in 'allKindsOfLocals' and 'allKindsOfArgs'
 *  methods of debugged 'locals002a' class. Once the debuggee is
 *  suspended in method, the test via 'locals' command compares
 *  the values of all visible variables with expected ones.
 *  The test consists of two parts:
 *    locals002.java  - test driver, i.e. launches jdb and debuggee,
 *                       writes commands to jdb, reads the jdb output,
 *    locals002a.java - the debugged application.
 * COMMENTS
 *  The test functionally equals to nsk/jdb/locals/locals001 test
 *  and replaces it.
 *  Test fixed according to test bug:
 *     5045859 TEST_BUG: some JDB tests do not recognize JDB prompt
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.locals.locals002.locals002
 *
 * @comment make sure locals002a is compiled w/ full debug info
 * @clean nsk.jdb.locals.locals002.locals002a
 * @compile -g:lines,source,vars locals002a.java
 *
 * @run main/othervm
 *      nsk.jdb.locals.locals002.locals002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.locals.locals002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class locals002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        compoundPromptIdent = COMPOUND_PROMPT_IDENT;
        return new locals002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.locals.locals002";
    static final String TEST_CLASS = PACKAGE_NAME + ".locals002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final String COMPOUND_PROMPT_IDENT = "main";
    static final int    BREAKPOINT_LINE1 = 84;
    static final int    BREAKPOINT_LINE2 = 100;

    static final String LOCALS[][] = new String[][] {
       { "boolVar"  , "true"         , "false"         },
       { "byteVar"  , "27"           , "12"            },
       { "charVar"  , "V"            , "A"             },
       { "shortVar" , "767"          , "327"           },
       { "intVar"   , "1474"         , "3647"          },
       { "longVar"  , "21345"        , "65789"         },
       { "floatVar" , "3.141"        , "4.852"         },
       { "doubleVar", "2.578"        , "3.8976"        },
       { "objVar"   , "objVarString" , "objArgString"  },
       { "arrVar"   , "int[5]"       , "int[3]"        }

                                                    };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + BREAKPOINT_LINE1);
        jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + BREAKPOINT_LINE2);

        jdb.receiveReplyFor(JdbCommand.cont);
        reply = jdb.receiveReplyFor(JdbCommand.locals);
        grep = new Paragrep(reply);
        for (int i = 0; i < LOCALS.length; i++) {
            v = new Vector();
            v.add(LOCALS[i][0]);
            v.add(LOCALS[i][2]);
            if (grep.find(v) == 0) {
                failure("Cannot find " + LOCALS[0][0] +
                    " with expected value: " + LOCALS[i][2]);
            }
        }

        jdb.receiveReplyFor(JdbCommand.cont);
        reply = jdb.receiveReplyFor(JdbCommand.locals);
        grep = new Paragrep(reply);
        for (int i = 0; i < LOCALS.length; i++) {
            v = new Vector();
            v.add(LOCALS[i][0]);
            v.add(LOCALS[i][1]);
            if (grep.find(v) == 0) {
                failure("Cannot find " + LOCALS[0][0] +
                    " with expected value: " + LOCALS[i][1]);
            }
        }

        jdb.contToExit(1);
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
