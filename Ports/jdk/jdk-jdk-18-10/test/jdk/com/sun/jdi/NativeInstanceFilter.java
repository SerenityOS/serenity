/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6426034
 * @summary Instance filter doesn't filter event if it occurs in native method
 * @author Keith McGuigan
 *
 * @run build TestScaffold VMConnection
 * @compile -XDignore.symbol.file NativeInstanceFilterTarg.java
 * @run driver NativeInstanceFilter
 */

/*
 *  This test tests instance filters for events generated from a native method
 */

import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

public class NativeInstanceFilter extends TestScaffold {

    static int unfilteredEvents = 0;
    EventSet eventSet = null;
    ObjectReference instance = null;

    public static void main(String args[]) throws Exception {
        new NativeInstanceFilter(args).startTests();
    }

    public NativeInstanceFilter(String args[]) {
        super(args);
    }

    static EventRequestManager requestManager = null;
    static MethodExitRequest request = null;
    static ThreadReference mainThread = null;

    protected void runTests() throws Exception {
        String[] args = new String[2];
        args[0] = "-connect";
        args[1] = "com.sun.jdi.CommandLineLaunch:main=NativeInstanceFilterTarg";

        connect(args);
        waitForVMStart();

        BreakpointEvent bpe = resumeTo("NativeInstanceFilterTarg", "main", "([Ljava/lang/String;)V");
        mainThread = bpe.thread();
        requestManager = vm().eventRequestManager();

        request = requestManager.createMethodExitRequest();
        request.addThreadFilter(mainThread);
        request.enable();

        try {
            addListener(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            failure("failure: Could not add listener");
            throw new Exception("NativeInstanceFilter: failed");
        }

        vm().resume();

        waitForVMDisconnect();

        if (unfilteredEvents != 1) {
            throw new Exception(
                "Failed: Event from native frame not filtered out.");
        }
        System.out.println("Passed: Event filtered out.");
    }

    public void eventSetReceived(EventSet set) {
        this.eventSet = set;
    }

    public void methodExited(MethodExitEvent event) {
        String name = event.method().name();
        if (instance == null && name.equals("latch")) {
            // Grab the instance (return value) and set up as filter
            System.out.println("Setting up instance filter");
            instance = (ObjectReference)event.returnValue();
            requestManager.deleteEventRequest(request);
            request = requestManager.createMethodExitRequest();
            request.addInstanceFilter(instance);
            request.addThreadFilter(mainThread);
            request.enable();
        } else if (instance != null && name.equals("intern")) {
            // If not for the filter, this will be called twice
            System.out.println("method exit event (String.intern())");
            ++unfilteredEvents;
        }
        eventSet.resume();
    }
}
