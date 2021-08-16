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
 * @summary converted from VM Testbase nsk/jdb/use/use001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test case for the 'use [source file path]' command.
 * The test checks if jdb correctly sets and shows source file path.
 * Working directory is added to the current path using 'use' command
 * with argument composed of old value and current working directory.
 * The test passes if working directory is encountered in reply message
 * returned by subsequent 'use' command.
 * The test consists of two program:
 *   use001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   use001a.java - the debugged application.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.use.use001.use001a
 * @run main/othervm
 *      nsk.jdb.use.use001.use001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.use.use001;

import nsk.share.*;
import nsk.share.jdb.*;
import jdk.test.lib.Utils;

import java.io.*;
import java.util.*;

public class use001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new use001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.use.use001";
    static final String TEST_CLASS = PACKAGE_NAME + ".use001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        String path = "";
        String srcdir = Utils.TEST_SRC;

//        jdb.setBreakpointInMethod(LAST_BREAK);
//        reply = jdb.receiveReplyFor(JdbCommand.cont);

        while (true) {
            reply = jdb.receiveReplyFor(JdbCommand.use);
/*
            if (reply.length == 0) {
                log.complain("Empty reply on first 'use' command");
                success = false;
                break;
            }
 */
            if (reply.length > 0) {
                path = reply[0];
            }
/*
            grep = new Paragrep(reply);
            count = grep.find(srcdir);
            if (count == 0) {
                log.complain("Not found current source directory in source file path");
                success = false;
                break;
            }
 */
            reply = jdb.receiveReplyFor(JdbCommand.use + srcdir + File.pathSeparator + path);
            if (reply.length == 0) {
                log.complain("Empty reply on third 'use' command");
                success = false;
                break;
            }

            reply = jdb.receiveReplyFor(JdbCommand.use);
            grep = new Paragrep(reply);
            if (grep.find(srcdir) == 0) {
                log.complain("Current source directory should be in source file path");
                success = false;
                break;
            }

            break;
        }

        jdb.contToExit(1);
    }
}
