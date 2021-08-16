/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdb/pop_exception/pop_exception001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 *         Test  for cr 6972759 "Step over not working after thrown exception and Pop"
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.pop_exception.pop_exception001.pop_exception001a
 * @run main/othervm
 *      nsk.jdb.pop_exception.pop_exception001.pop_exception001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.pop_exception.pop_exception001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class pop_exception001 extends JdbTest {

    static final String PACKAGE_NAME    = "nsk.jdb.pop_exception.pop_exception001";
    static final String TEST_CLASS      = PACKAGE_NAME + ".pop_exception001";
    static final String DEBUGGEE_CLASS  = TEST_CLASS + "a";
    static final String FIRST_BREAK     = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK      = DEBUGGEE_CLASS + ".lastBreak";

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new pop_exception001().runTest(argv, out);
    }



    protected void runCases() {

        jdb.receiveReplyFor(JdbCommand._catch + "java.lang.NullPointerException");
        jdb.receiveReplyFor(JdbCommand.cont);
        //exception
        jdb.receiveReplyFor(JdbCommand.pop);
        jdb.receiveReplyFor(JdbCommand.pop);
        jdb.receiveReplyFor(JdbCommand.ignore + "java.lang.NullPointerException");
        jdb.receiveReplyFor(JdbCommand.step);
        jdb.receiveReplyFor(JdbCommand.next);
        jdb.receiveReplyFor(JdbCommand.next);
        String[] reply = jdb.receiveReplyFor(JdbCommand.next);

        jdb.quit();
        checkJdbReply(reply);
    }

    private void checkJdbReply(String[] jdbReply) {
        String replyString = "";
        for(String s: jdbReply) {
            replyString += s;
            if(s.contains("line=")){
                if(!s.contains("line=" + pop_exception001a.expectedFinish)) {
                    throw new Failure("FAILED: Expected location: line=" + pop_exception001a.expectedFinish + "\n found: " + s);
                } else {
                    return;
                }
            }
        }
        throw new Failure("FAILED: Couldn't determinate finish position: " + replyString);
    }
}
