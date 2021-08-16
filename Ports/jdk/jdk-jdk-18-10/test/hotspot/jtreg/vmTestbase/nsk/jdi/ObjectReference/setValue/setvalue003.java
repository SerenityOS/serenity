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

package nsk.jdi.ObjectReference.setValue;

import com.sun.jdi.*;

import java.util.Iterator;
import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.ObjectReference.setValue()</code>
 * properly throws <i>InvalidTypeException</i> - if the Field is valid
 * for this ObjectReference, but there is no a widening reference
 * conversion from this object to the field's type.<br>
 * The debuggee part of the test has set of static and instance fields
 * of different types. The debugger part provokes the <i>InvalidTypeException</i>
 * trying to set value of:
 * <li>type not matched with the field's one, and without the widening
 * reference conversion as well
 * <li>boolean type which can be converted only to boolean.<br>
 */
public class setvalue003 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ObjectReference.setValue.setvalue003t";

    // name of debuggee's main thread
    static final String DEBUGGEE_THRNAME = "setvalue003tThr";

    // debuggee's local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR = "setvalue003tPipe";

    static final int ATTEMPTS = 5;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int FLDS_NUM = 34;
    // list of debuggee's fields used to set values and ones
    // used only to get their wrong non-conversionable values,
    // see section "5.1.2 Widening Primitive Conversion" in the JLS
    static final String DEBUGGEE_FLDS[][] = {
        {"byteFld", "sStrFld"},
        {"shortFld", "intFld"},
        {"intFld", "longFld"},
        {"longFld", "floatFld"},
        {"floatFld", "doubleFld"},
        {"doubleFld", "sBooleanFld"},
        {"charFld", "strFld"},
        {"booleanFld", "byteFld"},
        {"strFld", "sCharFld"},
        {"sByteFld", "sIntFld"},
        {"sShortFld", "doubleFld"},
        {"sIntFld", "sStrFld"},
        {"sLongFld", "sFloatFld"},
        {"sFloatFld", "sDoubleFld"},
        {"sDoubleFld", "strFld"},
        {"sCharFld", "sDoubleFld"},
        {"sBooleanFld", "doubleFld"},
        {"sStrFld", "sDoubleFld"},
    // see section "5.1.1 Identity Conversions" in the JLS:
    // "the only permitted conversion that involves the type boolean is
    // the identity conversion from boolean to boolean"
        {"byteFld", "booleanFld"},
        {"shortFld", "booleanFld"},
        {"intFld", "booleanFld"},
        {"longFld", "booleanFld"},
        {"floatFld", "booleanFld"},
        {"doubleFld", "booleanFld"},
        {"charFld", "booleanFld"},
        {"strFld", "booleanFld"},
        {"sByteFld", "booleanFld"},
        {"sShortFld", "booleanFld"},
        {"sIntFld", "booleanFld"},
        {"sLongFld", "booleanFld"},
        {"sFloatFld", "booleanFld"},
        {"sDoubleFld", "booleanFld"},
        {"sCharFld", "booleanFld"},
        {"sStrFld", "booleanFld"}
    };

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private ThreadReference thrRef = null;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setvalue003().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ObjectReference objRef;
        ReferenceType rType;
        String cmd;
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "setvalue003t.err> ");
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: Method Debugee.threadByName() returned null for debuggee's thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }
        thrRef.suspend();
        while(!thrRef.isSuspended()) {
            num++;
            if (num > ATTEMPTS) {
                log.complain("TEST FAILED: Unable to suspend debuggee's thread");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            log.display("Waiting for debuggee's thread suspension ...");
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                ie.printStackTrace();
                log.complain("TEST FAILED: caught: " + ie);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

// Check the tested assertion
        try {
            objRef = findObjRef(DEBUGGEE_LOCALVAR);
            rType = objRef.referenceType();

            // provoke the InvalidTypeException
            for (int i=0; i<FLDS_NUM; i++) {
                Field fld = rType.fieldByName(DEBUGGEE_FLDS[i][0]);
                try {
                    log.display("\nTrying to set value for the field \""
                        + fld.name() + "\"\n\tfrom the object reference \""
                        + objRef
                        + "\"\n\tusing not matched value's type \""
                        + objRef.getValue(rType.fieldByName(DEBUGGEE_FLDS[i][1])).type() + "\" ...");
                    objRef.setValue(fld,
                        objRef.getValue(rType.fieldByName(DEBUGGEE_FLDS[i][1])));
                    log.complain("TEST FAILED: expected InvalidTypeException was not thrown"
                        + "\n\twhen attempted to set value for the "
                        + fld.type() + " field \""
                        + fld.name() + "\"\n\tfrom the debuggee's object reference \""
                        + objRef
                        + "\"\n\tusing not matched value's type \""
                        + objRef.getValue(rType.fieldByName(DEBUGGEE_FLDS[i][1])).type() + "\"");
                    tot_res = Consts.TEST_FAILED;
                } catch (InvalidTypeException ie) {
                    log.display("CHECK PASSED: caught expected " + ie);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.complain("TEST FAILED: ObjectReference.setValue(): caught unexpected "
                        + e + "\n\tinstead of expected InvalidTypeException"
                        + "\n\twhen attempted to set value for the "
                        + fld.type() + " field \""
                        + fld.name() + "\"\n\tfrom the debuggee's object reference \""
                        + objRef
                        + "\"\n\tusing not matched value's type \""
                        + objRef.getValue(rType.fieldByName(DEBUGGEE_FLDS[i][1])).type() + "\"");
                    tot_res = Consts.TEST_FAILED;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

// Finish the test
        return quitDebuggee();
    }

    private ObjectReference findObjRef(String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar = stackFr.visibleVariableByName(varName);

                    if (locVar == null) continue;

                    // return reference to the debuggee class' object
                    return stackFr.thisObject();
                } catch(AbsentInformationException e) {
                    // this is not needed stack frame, ignoring
                } catch(NativeMethodException ne) {
                    // current method is native, also ignoring
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tot_res = Consts.TEST_FAILED;
            throw new Failure("findObjRef: caught unexpected exception: " + e);
        }
        throw new Failure("findObjRef: needed debuggee's stack frame not found");
    }

    private int quitDebuggee() {
        if (thrRef != null) {
            if (thrRef.isSuspended())
                thrRef.resume();
        }
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.complain("TEST FAILED: debuggee's process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("Debuggee's process finished with the status: "
                + debStat);

        return tot_res;
    }

}
