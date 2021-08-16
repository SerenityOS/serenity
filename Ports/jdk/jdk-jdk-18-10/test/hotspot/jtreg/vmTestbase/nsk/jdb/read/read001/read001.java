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
 * @summary converted from VM Testbase nsk/jdb/read/read001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 * A positive test for the 'read <filename>' command.
 * The jdb stops the debugged application on enter in read001a.lastBreak()
 * method. Then the tested command is called for the 'jdb.scenario' file.
 * The test passes if correct replies for all commands containing in
 * 'jdb.scenario' are found in jdb stdout stream.
 * The test consists of two program:
 *   read001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   read001a.java - the debugged application.
 * COMMENTS
 * Only limited number of commands is used in 'jdb.scenario' file because
 * the jdb does not wait until end of reply for current command and
 * immediatly executes the next command from 'jdb.scenario'.
 * Fixed test according to test bug:
 *     4665075 TTY: error messages for commands from scenario file of 'read' command
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.read.read001.read001
 *
 * @comment make sure read001a is compiled w/ full debug info
 * @clean nsk.jdb.read.read001.read001a
 * @compile -g:lines,source,vars read001a.java
 *
 * @run main/othervm
 *      nsk.jdb.read.read001.read001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.read.read001;

import nsk.share.*;
import nsk.share.jdb.*;
import jdk.test.lib.Utils;

import java.io.*;
import java.util.*;

public class read001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new read001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.read.read001";
    static final String TEST_CLASS = PACKAGE_NAME + ".read001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK    = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK     = DEBUGGEE_CLASS + ".lastBreak";

    static final String SCENARIO_FILE = "jdb.scenario";
    static final int SCENARIO_COMMANDS_COUNT = 5;

    protected void runCases() {
        String[] reply;
        String srcdir = Utils.TEST_SRC;

        // stop in lastBreak() method
        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        // return to testedInstanceMethod()
        reply = jdb.receiveReplyFor(JdbCommand.step);

        String command = JdbCommand.read + srcdir + File.separator + SCENARIO_FILE;
        int count = SCENARIO_COMMANDS_COUNT + 1;
        reply = jdb.receiveReplyFor(command, true, count);

        if (!checkCommands(reply)) {
            success = false;
        }

        jdb.contToExit(1);
    }

    private boolean checkCommands(String[] reply) {
        Paragrep grep;
        String found;
        Vector v = new Vector();
        boolean result = true;
        int count;

        grep = new Paragrep(reply);

        // check 'threads'
        log.display("Check reply for command: classes");
        if ((count = grep.find("nsk.jdb.read.read001.read001aTestedClass")) != 1) {
            log.complain("Wrong number of execution of command: classes");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'threads'
        log.display("Check reply for command: threads");
        if ((count = grep.find("TestedThreadInstance")) != 1) {
            log.complain("Wrong number of execution of command: threads");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'methods'
        log.display("Check reply for command: methods");
        if ((count = grep.find("testedInstanceMethod()")) != 1) {
            log.complain("Wrong number of execution of command: methods nsk.jdb.read.read001a");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'fields'
        log.display("Check reply for command: fields");
        if ((count = grep.find("testedStaticFieldBoolean")) != 1) {
            log.complain("Wrong number of execution of command: fields nsk.jdb.read.read001.presentedInstance");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'locals'
        log.display("Check reply for command: locals");
        if ((count = grep.find("testedLocalVarInt")) != 1) {
            log.complain("Wrong number of execution of command: locals");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

/*
        // check 'eval'
        log.display("Check reply for command: eval");
        if ((count = grep.find("556600")) != 1) {
            log.complain("Wrong number of execution of command: "
                        + "eval nsk.jdb.read.read001a.staticInt+instanceInt*3");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'print'
        log.display("Check reply for command: print");
        if ((count = grep.find("staticString = \"static string of read001a class\"")) != 1) {
            log.complain("Wrong number of execution of command: "
                        + "print nsk.jdb.read.read001.read001a.staticString"
                        );
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }

        // check 'dump'
        log.display("Check reply for command: dump");
        if ((count = grep.find("instance of nsk.jdb.read.read001.read001a")) != 1) {
            log.complain("Wrong number of execution of command: "
                        + "dump nsk.jdb.read.read001.read001a._read001a");
            log.complain("    Expected: 1; found: " + count);
            result = false;
        }
 */

        return result;
    }
}
