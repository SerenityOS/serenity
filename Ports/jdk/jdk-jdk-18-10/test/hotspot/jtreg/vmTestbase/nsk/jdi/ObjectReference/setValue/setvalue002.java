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

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Field;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.NativeMethodException;

import java.util.Iterator;
import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.ObjectReference.setValue()</code>
 * properly throws <i>IllegalArgumentException</i> - if the
 * field is not valid for this object's class.<br>
 * The debuggee part of the test contains two classes: the main one
 * <i>setvalue002t</i> and the dummy one <i>DummyClass</i>. Each of
 * them has the same set of static and instance fields.<br>
 * The debugger part provokes the IllegalArgumentException trying to
 * set value of:
 * <li>an object reference to the debuggee's class <i>setvalue002t</i>
 * and Field's value from an object reference to the debuggee's class
 * <i>DummyClass</i>
 * <li>an object reference to the debuggee's class <i>DummyClass</i>
 * and Field's value from an object reference to the debuggee's class
 * <i>setvalue002t</i>.<br>
 */
public class setvalue002 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ObjectReference.setValue.setvalue002t";

    // name of debuggee's main thread
    static final String DEBUGGEE_THRNAME = "setvalue002tThr";

    // debuggee's local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR = "dummyCls";

    static final int ATTEMPTS = 5;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private ThreadReference thrRef = null;
    private ObjectReference[] objRef = new ObjectReference[2];
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setvalue002().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType[] rType = new ReferenceType[2];
        String cmd;
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "setvalue002t.err> ");
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
                Thread.currentThread().sleep(1000);
            } catch(InterruptedException ie) {
                log.complain("TEST FAILED: caught: " + ie);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

// Check the tested assersion
        try {
            findObjRefs(DEBUGGEE_LOCALVAR);
            rType[0] = objRef[0].referenceType();
            rType[1] = objRef[1].referenceType();

            // provoke the IllegalArgumentException using an object reference
            // to debuggee's main class "setvalue002t" and Field's value from
            // an object reference to debuggee's dummy class "DummyClass"
            provokeException(objRef[0], rType[0], objRef[1], rType[1]);

            // provoke the IllegalArgumentException using an object reference
            // to debuggee's dummy class "DummyClass" and Field's value from
            // an object reference to debuggee's main class "setvalue002t"
            provokeException(objRef[1], rType[1], objRef[0], rType[0]);
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

// Finish the test
        thrRef.resume();
        return quitDebuggee();
    }

    private void findObjRefs(String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar = stackFr.visibleVariableByName(varName);
                    if (locVar == null)
                        continue;
                    // main debuggee class
                    objRef[0] = stackFr.thisObject();
                    // dummy debuggee class
                    objRef[1] = (ObjectReference)stackFr.getValue(locVar);
                    return;
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

    private void provokeException(ObjectReference objRef,
            ReferenceType refType,
            ObjectReference fldObjRef, ReferenceType fldRefType) {
        List fields;

        try {
            fields = fldRefType.allFields();
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: allFields for "
                + fldObjRef + ": caught: " + e);
            tot_res = Consts.TEST_FAILED;
            throw new Failure("provokeException: caught unexpected exception");
        }

        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            Field fld = (Field) iter.next();
            try {
                log.display("\nTrying to set value for the field \""
                    + fld.name() + "\"\n\tfrom the debuggee's object reference \""
                    + objRef
                    + "\"\n\tusing not valid Field's value from the other object reference \""
                    + fldObjRef + "\" ...");
                objRef.setValue(fld, objRef.getValue(refType.fieldByName(fld.name())));
                log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                    + "\n\twhen attempted to set value for the field \""
                    + fld.name() + "\"\n\tfrom the debuggee's object reference \""
                    + objRef
                    + "\n\tusing not valid Field's value from the other object reference \""
                    + fldObjRef + "\"");
                tot_res = Consts.TEST_FAILED;
            } catch (IllegalArgumentException ee) {
                log.display("CHECK PASSED: caught expected " + ee);
            } catch (Exception e) {
                e.printStackTrace();
                log.complain("TEST FAILED: ObjectReference.setValue(): caught unexpected "
                    + e + "\n\tinstead of expected IllegalArgumentException"
                    + "\n\twhen attempted to set value for the field \""
                    + fld.name() + "\"\n\tfrom the debuggee's object reference \""
                    + objRef
                    + "\n\tusing not valid Field's value from the other object reference \""
                    + fldObjRef + "\"");
                tot_res = Consts.TEST_FAILED;
            }
        }
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
