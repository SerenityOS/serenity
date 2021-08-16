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
 * @summary converted from VM Testbase nsk/jdb/redefine/redefine001.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DECSRIPTION
 * A positive test for the 'redefine <class id> <class file name>' command.
 * The debuggee program invokes three times method 'foo()' of RedefinedClass
 * class. This class in redefined with checked command each time before last
 * two invocations. If redefinitions occurs then the value returned by 'foo()'
 * method must be different from one returned previous invocation.
 * The test passes if method 'foo()' of the RedefinedClass returns expected
 * values.
 * The test consists of three program:
 *   redefine001.java - launches jdb and debuggee, writes commands to jdb, reads the jdb output,
 *   redefine001a.java - the debugged application.
 *   RedefinedClass.java - the class to be redefined.
 *   newclass_g/RedefinedClass.java - the redefining class.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.redefine.redefine001.redefine001a
 *
 * @comment compile newclass_g/RedefinedClass.java to newclass_g
 * @run driver
 *      ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      -d ${test.classes}/newclass_g
 *      -g:lines,source,vars
 *      -cp ${test.class.path}
 *      ${test.src}/newclass_g/RedefinedClass.java
 *
 * @run main/othervm
 *      nsk.jdb.redefine.redefine001.redefine001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.redefine.redefine001;

import nsk.share.*;
import nsk.share.jdb.*;
import nsk.share.classload.ClassLoadUtils;

import java.io.*;
import java.util.*;

public class redefine001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new redefine001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.redefine.redefine001";
    static final String TEST_CLASS = PACKAGE_NAME + ".redefine001";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    static final String REDEFINED_CLASS    = PACKAGE_NAME + ".RedefinedClass";
    static final String BEFORE_REDEFINITION = "BEFORE_REDEFINITION";
    static final String FIRST_REDEFINITION  = "AFTER_REDEFINITION";
    static final String SECOND_REDEFINITION = BEFORE_REDEFINITION;

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        reply = jdb.receiveReplyFor(JdbCommand.step); // to get out of lastBreak()

        reply = jdb.receiveReplyFor(JdbCommand.eval + DEBUGGEE_CLASS + ".flag");
        grep = new Paragrep(reply);
        if (grep.find(BEFORE_REDEFINITION) == 0) {
            log.complain("Wrong value of redefine001a.flag before redefinition: " + (reply.length > 0? reply[0]: ""));
            success = false;
        }

        String className = RedefinedClass.class.getName();
        String pathToRedefFile1 = ClassLoadUtils.getRedefineClassFileName("newclass_g", className);
        if (new File(pathToRedefFile1).exists()) {
            reply = jdb.receiveReplyFor(JdbCommand.redefine + REDEFINED_CLASS + " " + pathToRedefFile1);

            reply = jdb.receiveReplyFor(JdbCommand.cont);

            reply = jdb.receiveReplyFor(JdbCommand.eval + DEBUGGEE_CLASS + ".flag");
            grep = new Paragrep(reply);
            if (grep.find(FIRST_REDEFINITION) == 0) {
                log.complain("Wrong value of redefine001a.flag after first redefinition: " + (reply.length > 0? reply[0]: ""));
                success = false;
            }
        } else {
            log.complain("File does not exists: " + pathToRedefFile1);
            success = false;
        }

        String pathToRedefFile2 = ClassLoadUtils.getClassPathFileName(className);
        if (new File(pathToRedefFile2).exists()) {
            reply = jdb.receiveReplyFor(JdbCommand.redefine + REDEFINED_CLASS + " " + pathToRedefFile2);

            reply = jdb.receiveReplyFor(JdbCommand.cont);

            reply = jdb.receiveReplyFor(JdbCommand.eval + DEBUGGEE_CLASS + ".flag");
            grep = new Paragrep(reply);
            if (grep.find(SECOND_REDEFINITION) == 0) {
                log.complain("Wrong value of redefine001a.flag after second redefinition: " + (reply.length > 0? reply[0]: ""));
                success = false;
            }
        } else {
            log.complain("File does not exists: " + pathToRedefFile2);
            success = false;
        }

        jdb.contToExit(2);
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
