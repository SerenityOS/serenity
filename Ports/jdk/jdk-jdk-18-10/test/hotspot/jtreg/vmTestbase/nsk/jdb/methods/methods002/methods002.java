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
 * @summary converted from VM Testbase nsk/jdb/methods/methods002.
 * VM Testbase keywords: [jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This is a test for the 'methods' command.
 *  The test checks the following cases
 *    - class with various method modifiers,
 *    - class with various constructors,
 *    - class with various overloaded methods,
 *    - abstract class and interface,
 *    - implementing class,
 *    - class with inherited methods,
 *    - class with inherited and overrided methods.
 *  The test passes if signatures for all expected methods are listed
 *  in reply on command for every case.
 *  The test consists of two parts:
 *    methods002.java  - test driver, i.e. launches jdb and debuggee,
 *                       writes commands to jdb, reads the jdb output,
 *    methods002a.java - the debugged application.
 * COMMENTS
 *  This test replaces the nsk/jdb/methods/methods001 one.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdb.methods.methods002.methods002a
 * @run main/othervm
 *      nsk.jdb.methods.methods002.methods002
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.methods.methods002;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class methods002 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new methods002().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.methods.methods002";
    static final String TEST_CLASS = PACKAGE_NAME + ".methods002";
    static final String DEBUGGEE_CLASS = TEST_CLASS + "a";
    static final String FIRST_BREAK        = DEBUGGEE_CLASS + ".main";
    static final String LAST_BREAK         = DEBUGGEE_CLASS + ".lastBreak";

    protected void runCases() {
        String[] reply;
        Paragrep grep;
        int count;
        Vector v;
        String found;

        jdb.setBreakpointInMethod(LAST_BREAK);
        reply = jdb.receiveReplyFor(JdbCommand.cont);

        // Case for class with method modifiers
        String testedClass1 = TEST_CLASS + "a";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass1);
        for (int i = 1; i <= 33; i++) {
            checkMethod( reply, "m" + intToString(i), 1, testedClass1, testedClass1 );
        }
        for (int i = 1; i <= 3; i++) {
            checkMethod( reply, "f" + intToString(i), 1, testedClass1, testedClass1 );
        }

        // Case for class with many constructors
        String testedClass2 = TEST_CLASS + "b";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass2);
        String[] reply1 = toStringArray(reply);  // on Windows reply is single string with embedded \r\n symbols
        checkMethod( reply1, "<init>", 4, testedClass2, testedClass2 );

        // Case for class with overloaded methods
        String testedClass3 = TEST_CLASS + "c";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass3);
        reply1 = toStringArray(reply);   // // on Windows reply is single string with embedded \r\n symbols
        checkMethod( reply1, "m01", 3, testedClass3, testedClass3 );

        // Case for abstract class
        String testedClass4 = TEST_CLASS + "d";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass4);
        checkMethod( reply, "m01", 1, testedClass4, testedClass4 );

        // Case for interface
        String testedClass5 = TEST_CLASS + "i";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass5);
        checkMethod( reply, "i01", 1, testedClass5, testedClass5 );

        // Case for class with implemented method
        String testedClass6 = TEST_CLASS + "e";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass6);
        checkMethod( reply, "m01", 1, testedClass4, testedClass6 );
        checkMethod( reply, "i01", 1, testedClass5, testedClass6 );
        checkMethod( reply, "m01", 1, testedClass6, testedClass6 );

        // Case for class with inherited methods
        String testedClass7 = TEST_CLASS + "f";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass7);
        for (int i = 1; i <= 33; i++) {
            checkMethod( reply, "m" + intToString(i), 1, testedClass1, testedClass7 );
        }
        for (int i = 1; i <= 3; i++) {
            checkMethod( reply, "f" + intToString(i), 1, testedClass1, testedClass7 );
        }

        // Case for class with inherited and overrided methods
        String testedClass8 = TEST_CLASS + "g";
        reply = jdb.receiveReplyFor(JdbCommand.methods + testedClass8);
        for (int i = 1; i <= 33; i++) {
            checkMethod( reply, "m" + intToString(i), 1, testedClass8, testedClass8 );
        }
        for (int i = 1; i <= 33; i++) {
            checkMethod( reply, "m" + intToString(i), 1, testedClass1, testedClass8 );
        }
        for (int i = 1; i <= 3; i++) {
            checkMethod( reply, "f" + intToString(i), 1, testedClass1, testedClass8 );
        }

        jdb.contToExit(1);
    }

    private void checkMethod (
        String[] reply,       /* reply on 'methods' command */
        String methodName,    /* method name */
        int expOccur,         /* expected number of occurences of the method */
        String ownerClass,    /* name of class defining method */
        String testedClass    /* name of tested class */
                             ) {

        Paragrep grep = new Paragrep(reply);
        Vector v = new Vector();

        v.add(ownerClass);
        v.add(methodName);
        int j = grep.find(v);
        if (j != expOccur) {
            failure("Wrong number of occurences of method " + methodName +
                "\n\t of class " + ownerClass +
                "\n\t in class " + testedClass +
                "\n\t expected number: " + expOccur + " got: " + j);
        }
    }

    private String[] toStringArray (String[] arr) {
        Vector v = new Vector();
        for (int i = 0; i < arr.length; i++) {
            StringTokenizer st = new StringTokenizer(arr[i], "\r\n");
            while (st.hasMoreTokens()) {
                v.add(st.nextToken());
            }
        }
        Object[] objects = v.toArray();
        String[] results = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            results[i] = (String)objects[i];
        }
        return results;
    }

    private String intToString (int i) {
        String s = String.valueOf(i);
        if (s.length()==1)
            s = "0" + s;
        return s;
    }
}
