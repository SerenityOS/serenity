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
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method:<br>
 * <code>com.sun.jdi.ClassType.setValue()</code><br>
 * properly throws <i>InvalidTypeException</i> - if
 * the value's type does not match type of the specified field.<p>
 *
 * Debugger part of the test tries to provoke the exception by setting
 * values of static fields of different primitive types and own <i>setvalue007tDummyType</i>
 * which are not assignment compatible with the field type, and not
 * convertible without loss of information as well.
 */
public class setvalue007 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.setValue.setvalue007t";

    static final int FLD_NUM = 30;
    static final String DEBUGGEE_FLDS[][] = {
    // list of debuggee static fields used to set values and ones
    // used to get their wrong non-conversionable values,
    // see section "5.1.2 Widening Primitive Conversion" in the JLS
        {"dummySFld", "doubleSFld"},
        {"byteSFld", "strSFld"},
        {"byteSFld", "shortSFld"},
        {"byteSFld", "intSFld"},
        {"byteSFld", "longSFld"},
        {"byteSFld", "floatSFld"},
        {"byteSFld", "doubleSFld"},
        {"shortSFld", "intSFld"},
        {"shortSFld", "longSFld"},
        {"shortSFld", "floatSFld"},
        {"shortSFld", "doubleSFld"},
        {"intSFld", "longSFld"},
        {"intSFld", "floatSFld"},
        {"intSFld", "doubleSFld"},
        {"longSFld", "floatSFld"},
        {"longSFld", "doubleSFld"},
        {"floatSFld", "doubleSFld"},
        {"doubleSFld", "dummySFld"},
        {"charSFld", "strSFld"},
        {"booleanSFld", "byteSFld"},
        {"strSFld", "charSFld"},
    // see section "5.1.1 Identity Conversions" in the JLS:
    // "the only permitted conversion that involves the type boolean is
    // the identity conversion from boolean to boolean"
        {"byteSFld", "booleanSFld"},
        {"shortSFld", "booleanSFld"},
        {"intSFld", "booleanSFld"},
        {"longSFld", "booleanSFld"},
        {"floatSFld", "booleanSFld"},
        {"doubleSFld", "booleanSFld"},
        {"charSFld", "booleanSFld"},
        {"strSFld", "booleanSFld"},
        {"dummySFld", "booleanSFld"}
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
        return new setvalue007().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "setvalue007t.err> ");
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
                Value wrongVal = rType.getValue(
                    rType.fieldByName(DEBUGGEE_FLDS[i][1]));

                log.display("\n" + (i+1)
                    + ") Trying to set value: " + wrongVal
                    + "\tof type: " + wrongVal.type()
                    + "\n\tto field: "
                    + fld.name() + "\tof type: " + fld.typeName()
                    + "\n\tin the class \""
                    + clsType + "\" ...");

                // Check the tested assersion
                try {
                    clsType.setValue(fld, wrongVal);

                    log.complain("TEST FAILED: expected InvalidTypeException was not thrown"
                        + "\n\twhen attempted to set value: " + wrongVal
                        + "\tof type: " + wrongVal.type()
                        + "\n\tto field: " + fld.name()
                        + "\tof type: " + fld.typeName()
                        + "\n\tin the class \""
                        + clsType + "\""
                        + "\"\nNow the field " + fld.name()
                        + " has value: " + rType.getValue(fld));
                    tot_res = Consts.TEST_FAILED;
                } catch(InvalidTypeException ee) {
                    log.display("CHECK PASSED: caught expected " + ee);
                } catch(Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: caught unexpected "
                        + ue + "\n\tinstead of InvalidTypeException"
                        + "\n\twhen attempted to set value: " + wrongVal
                        + "\tof type: " + wrongVal.type()
                        + "\n\tto field: " + fld.name()
                        + "\tof type: " + fld.typeName()
                        + "\n\tin the class \""
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
