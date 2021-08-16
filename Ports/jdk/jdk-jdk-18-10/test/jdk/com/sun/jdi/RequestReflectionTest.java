/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4425840
 * @summary RequestReflectionTest checks to see that reflective
 * accessors on EventRequests return what they are given.
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g RequestReflectionTest.java
 * @run driver RequestReflectionTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;


    /********** target program **********/

class RequestReflectionTarg {
    int foo = 9;

    public static void main(String args[]) {
        System.out.println("Why, hello there...");
        (new RequestReflectionTarg()).bar();
    }

    void bar() {
        ++foo;
    }
}

    /********** test program **********/

public class RequestReflectionTest extends TestScaffold {

    RequestReflectionTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new RequestReflectionTest(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main():
         */
        BreakpointEvent bpe = startToMain("RequestReflectionTarg");
        ReferenceType targ = bpe.location().declaringType();
        ThreadReference thread = bpe.thread();

        Field fooField = targ.fieldByName("foo");
        if (fooField == null) {
            throw new Exception("test error: cannot find field foo");
        }
        List meths = targ.methodsByName("bar");
        if (meths.size() != 1) {
            throw new Exception("test error: should be one bar()");
        }
        Method barMethod = (Method)meths.get(0);

        List exClasses = vm().classesByName("java.lang.Exception");
        if (exClasses.size() != 1) {
            throw new Exception(
               "test error: should be one java.lang.Exception");
        }
        ReferenceType exceptionClass = (ReferenceType)exClasses.get(0);
        EventRequestManager erm = eventRequestManager();

        StepRequest sr =
            erm.createStepRequest(thread, StepRequest.STEP_MIN,
                                  StepRequest.STEP_OUT);
        sr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        sr.enable();
        if (!sr.thread().equals(thread)) {
            throw new Exception(
                    "RequestReflectionTest fail: exceptions do not match " +
                    thread + " != " + sr.thread());
        }
        if (sr.size() != StepRequest.STEP_MIN) {
            throw new Exception(
                    "RequestReflectionTest fail: size does not match " +
                    sr.size() + " != " + StepRequest.STEP_MIN);
        }
        if (sr.depth() != StepRequest.STEP_OUT) {
            throw new Exception(
                    "RequestReflectionTest fail: depth does not match " +
                    sr.depth() + " != " + StepRequest.STEP_OUT);
        }
        if (sr.suspendPolicy() != EventRequest.SUSPEND_NONE) {
            throw new Exception(
                    "RequestReflectionTest fail: wrong suspend policy " +
                    sr.suspendPolicy());
        }
        if (!sr.isEnabled()) {
            throw new Exception(
                    "RequestReflectionTest fail: should be enabled");
        }
        sr.disable();

        sr = erm.createStepRequest(thread, StepRequest.STEP_LINE,
                                  StepRequest.STEP_INTO);
        sr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        if (sr.size() != StepRequest.STEP_LINE) {
            throw new Exception(
                    "RequestReflectionTest fail: size does not match " +
                    sr.size() + " != " + StepRequest.STEP_LINE);
        }
        if (sr.depth() != StepRequest.STEP_INTO) {
            throw new Exception(
                    "RequestReflectionTest fail: depth does not match " +
                    sr.depth() + " != " + StepRequest.STEP_INTO);
        }
        if (sr.suspendPolicy() != EventRequest.SUSPEND_ALL) {
            throw new Exception(
                    "RequestReflectionTest fail: wrong suspend policy " +
                    sr.suspendPolicy());
        }
        if (sr.isEnabled()) {
            throw new Exception(
                    "RequestReflectionTest fail: should not be enabled");
        }

        AccessWatchpointRequest awr =
            erm.createAccessWatchpointRequest(fooField);
        if (!awr.field().equals(fooField)) {
            throw new Exception(
                    "RequestReflectionTest fail: fields do not match " +
                    fooField + " != " + awr.field());
        }
        if (awr.suspendPolicy() != EventRequest.SUSPEND_ALL) {
            throw new Exception(
                    "RequestReflectionTest fail: wrong suspend policy " +
                    awr.suspendPolicy());
        }
        if (awr.isEnabled()) {
            throw new Exception(
                    "RequestReflectionTest fail: should not be enabled");
        }
        BreakpointRequest bpr =
            erm.createBreakpointRequest(barMethod.location());
        bpr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        bpr.enable();
        if (!bpr.location().method().equals(barMethod)) {
            throw new Exception(
                    "RequestReflectionTest fail: methodss do not match " +
                    barMethod + " != " + bpr.location().method());
        }
        if (bpr.suspendPolicy() != EventRequest.SUSPEND_NONE) {
            throw new Exception(
                    "RequestReflectionTest fail: wrong suspend policy " +
                    bpr.suspendPolicy());
        }
        if (!bpr.isEnabled()) {
            throw new Exception(
                    "RequestReflectionTest fail: should be enabled");
        }
        ExceptionRequest exr =
            erm.createExceptionRequest(exceptionClass, true, false);
        exr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        exr.enable();
        exr.disable();
        if (!exr.exception().equals(exceptionClass)) {
            throw new Exception(
                    "RequestReflectionTest fail: not java.lang.Exception " +
                    exr.exception());
        }
        if (exr.suspendPolicy() != EventRequest.SUSPEND_EVENT_THREAD) {
            throw new Exception(
                    "RequestReflectionTest fail: wrong suspend policy " +
                    exr.suspendPolicy());
        }
        if (exr.isEnabled()) {
            throw new Exception(
                    "RequestReflectionTest fail: should not be enabled");
        }

        listenUntilVMDisconnect();

        println("RequestReflectionTest: passed");
    }
}
