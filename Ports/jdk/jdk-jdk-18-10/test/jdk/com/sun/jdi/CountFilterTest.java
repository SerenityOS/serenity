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
 * @bug 4321339
 * @summary Check correct processing of filters after a count filter
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g CountFilterTest.java
 * @run driver CountFilterTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class CountFilterTarg {

    void thisOne() {
    }

    void butNotThisOne() {
    }

    void norThisOne() {
    }

    void butThisOne() {
    }

    public static void main(String[] args){
        CountFilterTarg cft = new CountFilterTarg();
        System.out.println("Hi! Hi! Hello...");
        cft.thisOne();
        cft.butNotThisOne();
        cft.norThisOne();
        cft.butThisOne();
        System.out.println("Goodbye from CountFilterTarg!");
    }
}

    /********** test program **********/

public class CountFilterTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    EventRequestManager erm;
    Map whereMap = new HashMap();

    CountFilterTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new CountFilterTest(args).startTests();
    }

    /********** event handlers **********/

    public void breakpointReached(BreakpointEvent event) {
        println("Got BreakpointEvent");
        locatableEvent(event, event.location());
    }

    public void methodEntered(MethodEntryEvent event) {
        println("Got MethodEntryEvent");
        locatableEvent(event, event.location());
    }

    public void methodExited(MethodExitEvent event) {
        println("Got MethodExitEvent");
        locatableEvent(event, event.location());
    }

    public void locatableEvent(Event event, Location loc) {
        String got = loc.method().name();
        String expected = (String)whereMap.get(event.request());
        if (!got.equals(expected)) {
            failure("FAIL: expected event in " + expected +
                    " but it occurred in " + got);
        }
    }

    /********** test assist*****/

    BreakpointRequest breakpointAtMethod(String methodName)
                                           throws Exception {
        List meths = targetClass.methodsByName(methodName);
        if (meths.size() != 1) {
            throw new Exception("test error: should be one " +
                                methodName);
        }
        Method meth = (Method)meths.get(0);
        return erm.createBreakpointRequest(meth.location());
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("CountFilterTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        erm = vm().eventRequestManager();
        ThreadReference otherThread = null;

        /* find a thread that isn't mainThread */
        for (Iterator it = vm().allThreads().iterator();
                       it.hasNext(); ) {
            ThreadReference tr = (ThreadReference)it.next();
            if (!tr.equals(mainThread)) {
                otherThread = tr;
                break;
            }
        }
        if (otherThread == null) {
            throw new Exception("test error: couldn't find " +
                                "other thread");
        }

        /*
         * Set event requests
         */
        MethodEntryRequest meRequest =
            erm.createMethodEntryRequest();
        meRequest.addClassFilter("CountFilterTarg");
        meRequest.addCountFilter(5);  // incl constructor
        meRequest.enable();
        whereMap.put(meRequest, "butThisOne");

        MethodExitRequest mxRequest =
            erm.createMethodExitRequest();
        mxRequest.addCountFilter(2);
        mxRequest.addClassFilter("borp");
        mxRequest.enable();
        whereMap.put(mxRequest, "nowhere (from method exit)");

        BreakpointRequest thisOneRequest =
            breakpointAtMethod("thisOne");
        thisOneRequest.addCountFilter(1);
        thisOneRequest.addThreadFilter(mainThread);
        thisOneRequest.enable();
        whereMap.put(thisOneRequest, "thisOne");

        BreakpointRequest butNotThisOneRequest =
            breakpointAtMethod("butNotThisOne");
        butNotThisOneRequest.addCountFilter(1);
        butNotThisOneRequest.addThreadFilter(otherThread);
        butNotThisOneRequest.enable();
        whereMap.put(butNotThisOneRequest,
                     "nowhere (post filter)");

        BreakpointRequest norThisOneRequest =
            breakpointAtMethod("norThisOne");
        norThisOneRequest.addThreadFilter(otherThread);
        norThisOneRequest.addCountFilter(1);
        norThisOneRequest.enable();
        whereMap.put(norThisOneRequest,
                     "nowhere (pre filter)");

        BreakpointRequest butThisOneRequest =
            breakpointAtMethod("butThisOne");
        butThisOneRequest.addThreadFilter(mainThread);
        butThisOneRequest.addCountFilter(1);
        butThisOneRequest.enable();
        whereMap.put(butThisOneRequest, "butThisOne");

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("CountFilterTest: passed");
        } else {
            throw new Exception("CountFilterTest: failed");
        }
    }
}
