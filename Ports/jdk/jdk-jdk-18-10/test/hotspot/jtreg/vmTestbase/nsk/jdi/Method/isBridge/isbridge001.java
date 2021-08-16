/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Method.isBridge;

import com.sun.jdi.*;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test exercises the JDI method<br>
 * <b>com.sun.jdi.Method.isBridge()</b></br>
 * It verifies that 'bridge methods' generated during translation of
 * generic classes are determinated properly by the method.
 * Here are the rules from the spec "Adding Generics to the Java
 * Programming Language: Public Draft Specification, Version 2.0"
 * followed by the test:<br><pre>
 *
 *     6.2 Translation of Methods
 *     ...
 *     <li> If C.m is directly overridden by a method D.m in D, and the
 *       erasure of the return type or argument types of D.m differs from
 *       the erasure of the corresponding types in C.m, a bridge method
 *       needs to be generated.
 *
 *     <li> A bridge method also needs to be generated if C.m is not
 *       directly overridden in D, unless C.m is abstract.
 * </pre><br>
 * The test works as follows. Debuggee contains several dummy superclasses
 * and subclasses. Some of their methods fall under the rules above.
 * Debugger verifies that the JDI Method.isBridge() returns true for all
 * generated bridge methods and false for the non-bridge ones.
 * The list of the class methods is obtained through the JDI
 * ReferenceType.methods() which must return each method declared directly
 * in this type including any synthetic methods created by the compiler.
 */
public class isbridge001 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.Method.isBridge.isbridge001t";

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String[][][] methods = {
// nsk.jdi.Method.isBridge.isbridge001aa
       {{"<init>",
            "()V",
            "false",
            "0"},
        {"isbridge001aMeth",
            "(Ljava/lang/Double;)V",
            "false",
            "0"}},

// nsk.jdi.Method.isBridge.isbridge001bb
       {{"<init>",
            "()V",
            "false",
            "0"},
        {"isbridge001bMeth",
            "(Ljava/lang/Number;)Ljava/lang/Number;",
            "false",
            "0"},
        {"isbridge001bMeth2",
            "(Ljava/lang/Number;I)V",
            "false",
            "0"}},

// nsk.jdi.Method.isBridge.isbridge001bb2
       {{"<init>",
            "()V",
            "false",
            "0"},
        {"isbridge001bMeth",
            "(Ljava/lang/Byte;)Ljava/lang/Byte;",
            "false",
            "0"},
        {"isbridge001bMeth2",
            "(Ljava/lang/Byte;I)V",
            "false",
            "0"},
        {"isbridge001bMeth",
            "(Ljava/lang/Number;)Ljava/lang/Number;",
            "true",
            "0"},
        {"isbridge001bMeth2",
            "(Ljava/lang/Number;I)V",
            "true",
            "0"}},

// nsk.jdi.Method.isBridge.isbridge001dd
       {{"<init>",
            "()V",
            "false",
            "0"}},

// nsk.jdi.Method.isBridge.isbridge001dd2
       {{"<init>",
            "()V",
            "false",
            "0"},
        {"isbridge001dMeth",
            "(Ljava/lang/Boolean;Ljava/lang/Character;)Ljava/lang/String;",
            "false",
            "0"},
         {"isbridge001dMeth",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            "true",
            "0"}}
    };

    static final String[] classes = {
        "nsk.jdi.Method.isBridge.isbridge001aa",
        "nsk.jdi.Method.isBridge.isbridge001bb",
        "nsk.jdi.Method.isBridge.isbridge001bb2",
        "nsk.jdi.Method.isBridge.isbridge001dd",
        "nsk.jdi.Method.isBridge.isbridge001dd2",
    };

    static final int CLS_NUM = classes.length;

    private IOPipe pipe;
    private Log log;
    private Debugee debuggee;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new isbridge001().runThis(argv, out);
    }

    private int runThis(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "isbridge001t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            tot_res = Consts.TEST_FAILED;
            log.complain("TEST BUG: unknown debuggee's command: "
                + cmd);
            return quitDebuggee();
        }

        for (int i=0; i<CLS_NUM; i++) {
            ReferenceType rType;
            List clsMethods;

            log.display("\n>>>>>> Class " + classes[i]);
            if ((rType = debuggee.classByName(classes[i])) == null) {
                tot_res = Consts.TEST_FAILED;
                log.complain("TEST FAILURE: Method Debugee.classByName() returned null for "
                    + classes[i] + "\n");
                return quitDebuggee();
            }
            try {
                clsMethods = rType.methods();
            } catch (Exception e) {
                tot_res = Consts.TEST_FAILED;
                log.complain("TEST FAILURE: ReferenceType.methods: caught " + e
                    + "\n");
                return quitDebuggee();
            }

            Iterator iterator = clsMethods.iterator();
            while(iterator.hasNext()) {
                Method meth = (Method) iterator.next();
                log.display("\t--> method " + meth
                    + "\n\t\tname " + meth.name()
                    + "\n\t\treturn type " + meth.returnTypeName()
                    + "\n\t\tsignature " + meth.signature());

                int idx = findMethod(i, meth.signature());
                if (idx != -1) {
                    try {
                        int methCounter = Integer.parseInt(methods[i][idx][3]);
                        methCounter++;
                        methods[i][idx][3] = String.valueOf(methCounter);
                    } catch (NumberFormatException e) {
                        tot_res = Consts.TEST_FAILED;
                        log.complain("TEST BUG: Integer.parseInt: caught " + e);
                        return quitDebuggee();
                    }
                    boolean bridge = Boolean.valueOf(methods[i][idx][2]);
                    if (bridge == meth.isBridge())
                        log.display("\tCHECK PASSED: Method.isBridge() returns "
                            + meth.isBridge() + " as expected\n");
                    else {
                        tot_res = Consts.TEST_FAILED;
                        log.complain("TEST FAILED: Method.isBridge() returns "
                            + meth.isBridge()
                            + "\n\tfor the method: " + meth.name()
                            + " " + meth.signature()
                            + "\n\tExpected: "+ methods[i][idx][2] + "\n");
                    }
                }
            }

            log.display("<<<<<<");
        }

        for (int i=0; i<CLS_NUM; i++) {
            int j = 0;

            while(true) {
                try {
                    if (methods[i][j][3].equals("0")) {
                        tot_res = Consts.TEST_FAILED;
                        log.complain("TEST FAILED: Method " + methods[i][j][0]
                            + " " + methods[i][j][1]
                            + "\n\tfrom the class " + classes[i]
                            + "\n\twas not returned by the ReferenceType.methods()\n");
                    }

                    j++;
                } catch(ArrayIndexOutOfBoundsException e) {
                    break;
                }
            }
        }

        return quitDebuggee();
    }

    private int quitDebuggee() {
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            tot_res = Consts.TEST_FAILED;
            log.complain("TEST FAILED: debuggee's process finished with status: "
                + debStat);
        } else
            log.display("Debuggee's process finished with status: "
                + debStat);

        return tot_res;
    }

    private int findMethod(int clsIdx, String signature) {
        int i=0;

        while(true) {
            try {
                if (signature.equals(methods[clsIdx][i][1]))
                    return i; // the tested method found

                i++;
            } catch(ArrayIndexOutOfBoundsException e) {
                break;
            }
        }

        return -1; // the tested method not found
    }
}
