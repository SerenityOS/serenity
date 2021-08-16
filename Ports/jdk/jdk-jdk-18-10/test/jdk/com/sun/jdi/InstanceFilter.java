/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4312961
 * @summary Verify that an instance filter on a MethodEntryRequest works
 *  properly.
 * @author Robert Field/Jim Holmlund
 *
 * @run build TestScaffold VMConnection TargetAdapter TargetListener
 * @run compile -g InstanceFilter.java
 * @run driver InstanceFilter
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

class InstanceFilterTarg {
    static InstanceFilterTarg first = new InstanceFilterTarg();
    static InstanceFilterTarg second = new InstanceFilterTarg();
    static InstanceFilterTarg third = new InstanceFilterTarg();

    public static void main(String args[]) {
        start();
    }

    static void start() {
        first.go();
        second.go();
        third.go();
    }

    void go() {
        one();
        two();
        three();
    }

    void one() {
    }

    void two() {
    }

    void three() {
    }
}

public class InstanceFilter extends TestScaffold {
    ReferenceType targetClass;

    ObjectReference theInstance;
    MethodEntryRequest methodEntryRequest;
    int methodCount = 0;
    // These are the methods for which we expect to get MethodEntryEvents.
    String[] expectedMethods = new String[] { "go", "one", "two", "three"};

    public static void main(String args[]) throws Exception {
        new InstanceFilter(args).startTests();
    }

    InstanceFilter(String args[]) throws Exception {
        super(args);
    }

    /**
     * Override TestScaffold.methodEntered.  This should get called
     * once for each method named in 'expectedMethods', and for
     * the instance that we select to filter on.
     */
    public void methodEntered(MethodEntryEvent event) {
        if (testFailed) {
            return;
        }
        // Find the instance and verify that it is
        // the one we want.
        ObjectReference theThis;
        try {
            theThis = event.thread().frame(0).thisObject();
        } catch (IncompatibleThreadStateException ee) {
            failure("FAILED: Exception occured in methodEntered: " + ee);
            return;
        }
        if (theThis == null) {
            // This happens when the thread has exited or when a
            // static method is called. Setting an instance
            // filter does not prevent this event from being
            // emitted with a this that is null.
            methodEntryRequest.disable();
            return;
        }

        if (!theThis.equals(theInstance)) {
            failure("FAILED: Got a hit on a non-selected instance");
        }

        // fail if we don't get called for each of the expected methods
        {
            String methodStr = event.location().method().name();

            if (methodCount >= expectedMethods.length) {
                failure("FAILED: Got too many methodEntryEvents");
            } else if (methodStr.indexOf(expectedMethods[methodCount]) == -1) {
                failure("FAILED: Expected method: " + expectedMethods[methodCount]);
            }
            methodCount++;
            println("Method: " + methodStr);
        }
    }

    protected void runTests() throws Exception {

        BreakpointEvent bpe = startTo("InstanceFilterTarg", "go", "()V");
        targetClass = bpe.location().declaringType();

        Field field = targetClass.fieldByName("second");
        theInstance = (ObjectReference)(targetClass.getValue(field));

        EventRequestManager mgr = vm().eventRequestManager();
        methodEntryRequest = mgr.createMethodEntryRequest();
        methodEntryRequest.addInstanceFilter(theInstance);
        // Thread filter is needed to prevent MethodEntry events
        // to be emitted by the debugee when a static method
        // is called on any thread.
        methodEntryRequest.addThreadFilter(bpe.thread());
        methodEntryRequest.enable();

        listenUntilVMDisconnect();

        if (!testFailed && methodCount < expectedMethods.length) {
            failure("FAILED: Expected " + expectedMethods.length + " events, only got "
                    + methodCount);
        }
        if (!testFailed) {
            println("InstanceFilter: passed");
        } else {
            throw new Exception("InstanceFilter: failed");
        }
    }
}
