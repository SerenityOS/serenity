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
 * @bug 4689395
 * @summary converted from VM Testbase nsk/jdb/regression/b4689395.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *     Regression test for the bug
 *         4689395 (P4/S3) "step over" after a class is redefined acts like "step out"
 *         Release summary: 1.4
 *         Hardware version: generic
 *         O/S version (unbundled products): 5.8
 *     The test consists of two java-files:
 *         b4689395.java  - launches jdb and debuggee, writes commands to jdb,
 *                          reads the jdb output;
 *         b4689395a.java - the debugged application.
 *     The debugged application (b4689395a.java) defines method minor() that
 *     prints four lines into System.out. b4689395 sets a breakpoint on the 54th
 *     line
 *         System.out.println("A breakpoint is here.");
 *     and then redefines b4689395a with newclass/b4689395a. Those classes differ
 *     just in the 30th line (period is omitted in newclass/b4689395a). After that
 *     the debuggee invokes 'next' command. The test fails if
 *     b4689395.ERROR_MESSAGE message appears in output, otherwise the test passes.
 * COMMENTS
 *     The test reproduces the bug on Solsparc.
 *     java version "1.4.1-beta"
 *     Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.1-beta-b14)
 *     Java HotSpot(TM) Client VM (build 1.4.1-beta-b14, mixed mode)
 *     Command line
 *         ../jdk1.4.1-b14/solsparc/bin/java b4689395 -arch=sparc
 *         -waittime=2 -debugee.vmkind=java
 *         -jdb=../jdk1.4.1-b14/solsparc/bin/jdb
 *         -workdir=../b4689395 -jdb.option= -debugee.vmkeys=
 *     #launcher > Compound prompt found: main[1]
 *     #Test cases starts.
 *     #Sending command: stop at b4689395a:54
 *     #
 *     #launcher > Compound prompt found: main[1]
 *     #Sending command: cont
 *     #
 *     #launcher > Compound prompt found: main[1]
 *     #Sending command: redefine b4689395a b4689395/newclass/b4689395a.class
 *     #
 *     #launcher > Compound prompt found: main[1]
 *     #Sending command: next
 *     #
 *     #launcher > Compound prompt found: main[1]
 *     ## ERROR: 'ERROR_M' is not expected to be printed after 'next' command.
 *     #Sending command: cont
 *     #
 *     #Test cases ends.
 *     #Waiting for jdb exits
 *     #jdb normally exited
 *     ## ERROR: TEST FAILED
 *     #
 *     #
 *     ##>
 *     ##>  SUMMARY: Following errors occured
 *     ##>      during test execution:
 *     ##>
 *     ## ERROR: 'ERROR_M' is not expected to be printed after 'next' command.
 *     ## ERROR: TEST FAILED
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @build nsk.jdb.regression.b4689395.b4689395
 *        nsk.jdb.regression.b4689395.b4689395a
 *
 * @comment compile newclass/b4689395a.java to newclass
 * @run driver
 *      ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      -d ${test.classes}/newclass
 *      -cp ${test.class.path}
 *      ${test.src}/newclass/b4689395a.java
 *
 * @run main/othervm
 *      nsk.jdb.regression.b4689395.b4689395
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.regression.b4689395;

import nsk.share.*;
import nsk.share.jdb.*;
import nsk.share.classload.ClassLoadUtils;

import java.io.*;
import java.util.*;

public class b4689395 extends JdbTest {
        final static String TEST_CLASS     = b4689395.class.getName();
        final static String DEBUGGEE_CLASS = TEST_CLASS + "a";
        final static String FIRST_BREAK    = DEBUGGEE_CLASS + ".main";
        final static String ERROR_MESSAGE  = "ERROR_M";
        final static int    LINE_NUMBER    = 54;
        private String classFile;

        public static void main (String argv[]) {
                System.exit(run(argv, System.out) + JCK_STATUS_BASE);
        }

        public static int run(String argv[], PrintStream out) {
                debuggeeClass =  DEBUGGEE_CLASS;
                firstBreak = FIRST_BREAK;
                return new b4689395().runTest(argv, out);
        }

        public b4689395() {
                classFile = ClassLoadUtils.getRedefineClassFileName(DEBUGGEE_CLASS);
                if (classFile == null)
                        throw new TestFailure("Unable to find redefine class file in classpath for: " + DEBUGGEE_CLASS);
        }

        protected void runCases() {
                String[] reply;
                reply = jdb.receiveReplyFor(JdbCommand.stop_at + DEBUGGEE_CLASS + ":" + LINE_NUMBER);
                reply = jdb.receiveReplyFor(JdbCommand.cont);

                if (new File(classFile).exists()) {
                        reply = jdb.receiveReplyFor(JdbCommand.redefine + DEBUGGEE_CLASS
                                        + " " + classFile);
                        reply = jdb.receiveReplyFor(JdbCommand.next);

                        Paragrep grep = new Paragrep(reply);
                        if (grep.find(ERROR_MESSAGE) != 0) {
                                log.complain("'" + ERROR_MESSAGE + "' is not expected to be "
                                                + "printed after 'next' command.");
                                success = false;
                        }
                } else {
                        log.complain("File does not exist: " + classFile);
                        success = false;
                }

                jdb.contToExit(1);
        }
}
