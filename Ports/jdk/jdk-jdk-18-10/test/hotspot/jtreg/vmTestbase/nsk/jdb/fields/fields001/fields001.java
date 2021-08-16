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
 * @summary converted from VM Testbase nsk/jdb/fields/fields001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'fields <class id>' command.
 * There are three test cases:
 *  - request for the fields defined in class,
 *  - request for the fields defined in inner class,
 *  - request for the fields inherited from super class.
 * The test passes when reply contains names of all checked fields
 * for a given debuggee's class.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.fields.fields001.fields001a
 * @run main/othervm
 *      nsk.jdb.fields.fields001.fields001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.fields.fields001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class fields001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new fields001().runTest(argv, out);
    }

    static final String PACKAGE_NAME       = "nsk.jdb.fields.fields001";
    static final String TEST_CLASS         = PACKAGE_NAME + ".fields001";
    static final String DEBUGGEE_CLASS     = TEST_CLASS + "a";
    static final String DEBUGGEE_CLASS1    = DEBUGGEE_CLASS + "$Inner";
    static final String DEBUGGEE_CLASS2    = DEBUGGEE_CLASS + "$Extender";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";
    static final String NOT_VALID_SAMPLE   = "is not a valid";

    static String[] checkedFields1 = {
        "i_st",    "o_st",
        "i_pv",    "o_pv",
        "i_pt",    "o_pt",
        "i_pb",    "o_pb",
        "i_fn",    "o_fn",
        "i_tr",    "o_tr",
        "i_vl",    "o_vl",
        "i_a",     "o_a",
        "i_aa",    "o_aa",
        "i_aaa",   "o_aaa"
                                      };

    static String[] checkedFields2 = {
        "ii_pv",    "oi_pv",
        "ii_pt",    "oi_pt",
        "ii_pb",    "oi_pb",
        "ii_fn",    "oi_fn",
        "ii_tr",    "oi_tr",
        "ii_vl",    "oi_vl",
        "ii_a",     "oi_a",
        "ii_aa",    "oi_aa",
        "ii_aaa",   "oi_aaa"
                                      };

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.fields + DEBUGGEE_CLASS);
        if (!checkFields(DEBUGGEE_CLASS, reply, checkedFields1)) {
            success = false;
        }

        reply = jdb.receiveReplyFor(JdbCommand.fields + DEBUGGEE_CLASS1);
        if (!checkFields(DEBUGGEE_CLASS1, reply, checkedFields2)) {
            success = false;
        }

        reply = jdb.receiveReplyFor(JdbCommand.fields + DEBUGGEE_CLASS2);
        if (!checkFields(DEBUGGEE_CLASS2, reply, checkedFields2)) {
            success = false;
        }

        jdb.contToExit(1);
    }

    private boolean checkFields (String className, String[] reply, String[] checkedFields) {
        Paragrep grep;
        String found;
        boolean result = true;
        int count;

        grep = new Paragrep(reply);
        for (int i = 0; i < checkedFields.length; i++) {
            count = grep.find(checkedFields[i]);
            if (count == 0) {
                log.complain("Failed to report field " + checkedFields[i] + " for class " + className);
                result = false;
            }
        }
        return result;
    }
}
