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
 * @summary converted from VM Testbase nsk/jdb/threadgroup/threadgroup002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *  This is a test for jdb 'threadgroup <threadgroup_name>' command.
 *  The main thread creates 3 threadgroups of 5 threads each.
 *  All threads are locked in their 'run' method on a lock that the main
 *  thread holds. The test passes if jdb correctly switches between
 *  three user-defined threadgroups using 'threadgroup' command.
 * COMMENTS
 *  This test functionally equals to nsk/jdb/threadgroup/threadgroup001
 *  test and replaces it.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.threadgroup.threadgroup002.threadgroup002a
 * @run main/othervm
 *      nsk.jdb.threadgroup.threadgroup002.threadgroup002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.threadgroup.threadgroup002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class threadgroup002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new threadgroup002().runTest(argv, out);
    }

    static final String PACKAGE_NAME     = "nsk.jdb.threadgroup.threadgroup002";
    static final String TEST_CLASS       = PACKAGE_NAME + ".threadgroup002";
    static final String DEBUGGEE_CLASS   = TEST_CLASS + "a";
    static final String FIRST_BREAK      = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK       = DEBUGGEE_CLASS + ".lastBreak";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        jdb.receiveReplyFor(JdbCommand.cont);
        jdb.receiveReplyFor(JdbCommand.threadgroups);

        for (int i = 0; i < threadgroup002a.numThreadGroups; i++) {
            reply = jdb.receiveReplyFor(JdbCommand.threadgroup + threadgroup002a.THREADGROUP_NAME + i);
            grep = new Paragrep(reply);
            count = grep.find("not a valid threadgroup name");
            if (count > 0) {
                failure("jdb cannot switch to valid threadgroup: " + threadgroup002a.THREADGROUP_NAME + i);
            }
        }

        jdb.contToExit(1);
    }
}
