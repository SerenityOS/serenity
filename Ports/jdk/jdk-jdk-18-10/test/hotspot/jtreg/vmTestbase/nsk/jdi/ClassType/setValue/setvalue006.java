/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.setValue;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method:<br>
 * <code>com.sun.jdi.ClassType.setValue()</code><br>
 * does not throw <i>ClassNotLoadedException</i> - when a debugger
 * part of the test attempts to set null value to the debuggee field
 * which field type has not yet been loaded through the appropriate
 * class loader.<p>
 *
 * The test works as follows. The debuggee part has two static
 * fields: <i>dummySFld</i> of non-loaded type <i>setvalue006tDummyType</i> and
 * <i>finDummySFld</i> of non-loaded type <i>setvalue006tFinDummyType</i>.
 * Debugger part tries to provoke the exception by setting null values to
 * these fields. The test makes sure that appropriate class has not
 * been loaded by the debuggee VM through the JDI method
 * <i>VirtualMachine.classesByName()</i> which should return matching
 * list of <b>loaded</b> classes only.
 */
public class setvalue006 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.setValue.setvalue006t";

    // tested static fields in debuggee class and theirs types
    static final int FLD_NUM = 2;
    static final String DEBUGGEE_FLDS[][] = {
        {"dummySFld", "nsk.jdi.ClassType.setValue.setvalue006tDummyType"},
        {"finDummySFld", "nsk.jdi.ClassType.setValue.setvalue006tFinDummyType"}
    };

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setvalue006().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "setvalue006t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);
            ClassType clsType = (ClassType) rType;

            for (int i=0; i<FLD_NUM; i++) {
                Field fld =
                    rType.fieldByName(DEBUGGEE_FLDS[i][0]);
                // field with the given name is not found
                if (fld == null)
                    throw new Failure("needed debuggee field "
                        + DEBUGGEE_FLDS[i][0] + " not found");

                // Make sure that the reference type is not loaded by the debuggee VM
                if ((debuggee.classByName(DEBUGGEE_FLDS[i][1])) != null) {
                    log.display("Skipping the check: actually, the tested reference type\n\t\""
                        + DEBUGGEE_FLDS[i][1]
                        + "\"\nhas been already loaded by the debuggee VM, unable to test an assertion");
                    continue;
                }

                log.display("\n" + (i+1)
                    + ") Trying to set null value for the field: "
                    + fld.name() + "\tsignature: " + fld.signature()
                    + "\n\tof non-loaded type: "
                    + fld.typeName() + "\n\tin the class type \""
                    + clsType + "\" ...");

                // Check the tested assersion
                try {
                    clsType.setValue(fld, null);

                    log.display("CHECK PASSED: ClassNotLoadedException was not thrown as expected"
                        + "\n\twhen attempted to set null value for the field: "
                        + fld.name() + "\tsignature: " + fld.signature()
                        + "\n\tof non-loaded type: "
                        + fld.typeName() + "\n\tin the class type \""
                        + clsType + "\"");
                } catch(ClassNotLoadedException ee) {
                    log.complain("TEST FAILED: " + ee + " was thrown"
                        + "\n\twhen attempted to set null value for field: "
                        + fld.name() + "\tsignature: " + fld.signature()
                        + "\n\tof non-loaded type: "
                        + fld.typeName() + "\n\tin the class type \""
                        + clsType + "\"");
                    tot_res = Consts.TEST_FAILED;
                } catch(Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: caught unexpected "
                        + ue + "\n\tinstead of ClassNotLoadedException"
                        + "\n\twhen attempted to set null value for field: "
                        + fld.name() + "\tsignature: " + fld.signature()
                        + "\n\tof non-loaded type: "
                        + fld.typeName() + "\n\tin the class type \""
                        + clsType + "\"");
                    tot_res = Consts.TEST_FAILED;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

        return quitDebuggee();
    }

    private int quitDebuggee() {
        log.display("\nFinal resumption of debuggee VM");
        vm.resume();
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.complain("TEST FAILED: debuggee process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("\nDebuggee process finished with the status: "
                + debStat);

        return tot_res;
    }
}
