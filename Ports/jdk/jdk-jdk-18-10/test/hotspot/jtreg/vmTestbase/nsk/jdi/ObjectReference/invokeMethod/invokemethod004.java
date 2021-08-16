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

package nsk.jdi.ObjectReference.invokeMethod;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.ObjectReference.invokeMethod()</code>
 * properly throws <i>IncompatibleThreadStateException</i> when
 * debugger part of the test attempts to invoke several
 * debuggee methods when the debuggee thread has been suspended
 * by the ThreadReference.suspend() instead of by an event.
 */
public class invokemethod004 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod004t";

    // name of debuggee main thread
    static final String DEBUGGEE_THRNAME = "invokemethod004tThr";

    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR =
        "invokemethod004tdummyCls";

    // tested debuggee methods and type reference numbers
    static final int METH_NUM = 10;
    static final String DEBUGGEE_METHODS[] = {
        "byteMeth",
        "shortMeth",
        "intMeth",
        "longMeth",
        "floatMeth",
        "doubleMeth",
        "charMeth",
        "booleanMeth",
        "strMeth",
        "voidMeth"
    };

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int ATTEMPTS = 5;

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private ThreadReference thrRef = null;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new invokemethod004().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        String cmd;
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "invokemethod004t.err> ");
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }
        thrRef.suspend();
        while(!thrRef.isSuspended()) {
            num++;
            if (num > ATTEMPTS) {
                log.complain("TEST FAILED: unable to suspend debuggee thread");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            log.display("Waiting for debuggee thread suspension ...");
            try {
                Thread.currentThread().sleep(1000);
            } catch(InterruptedException ie) {
                ie.printStackTrace();
                log.complain("TEST FAILED: caught: " + ie);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

// Check the tested assersion
        try {
            ObjectReference objRef = findObjRef(DEBUGGEE_LOCALVAR);
            ReferenceType rType = objRef.referenceType(); // debuggee dummy class

            for (int i=0; i<METH_NUM; i++) {
                List<Method> methList = rType.methodsByName(DEBUGGEE_METHODS[i]);
                if (methList.isEmpty()) {
                    log.complain("TEST FAILURE: the expected debuggee method \""
                        + DEBUGGEE_METHODS[i]
                        + "\" not found through the JDI method ReferenceType.methodsByName()");
                    tot_res = Consts.TEST_FAILED;
                    continue;
                }
                Method meth = methList.get(0);

                try {
                    log.display("\nTrying to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType
                        + "\"\n\tusing the debuggee object reference \""
                        + objRef + "\" and the thread reference which has not been suspended by an event ...");

                    objRef.invokeMethod(thrRef, meth, Collections.<Value>emptyList(), 0);

                    log.complain("TEST FAILED: expected IncompatibleThreadStateException was not thrown"
                        + "\n\twhen attempted to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType
                        + "\"\n\tusing the debuggee object reference \""
                        + objRef + "\" and the thread reference which has not been suspended by an event.");
                    tot_res = Consts.TEST_FAILED;
                } catch (IncompatibleThreadStateException is) {
                    log.display("CHECK PASSED: caught expected " + is);
                } catch (Exception ee) {
                    ee.printStackTrace();
                    log.complain("TEST FAILED: ObjectReference.invokeMethod(): caught unexpected "
                        + ee + "\n\tinstead of expected IncompatibleThreadStateException"
                        + "\n\twhen attempted to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType
                        + "\"\n\tusing the debuggee object reference \""
                        + objRef + "\" and the thread reference which has not been suspended by an event.");
                    tot_res = Consts.TEST_FAILED;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
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

                    // dummy debuggee class
                    return (ObjectReference)stackFr.getValue(locVar);
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
        throw new Failure("findObjRef: needed debuggee stack frame not found");
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
            log.complain("TEST FAILED: debuggee process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("\nDebuggee process finished with the status: "
                + debStat);

        return tot_res;
    }

}
