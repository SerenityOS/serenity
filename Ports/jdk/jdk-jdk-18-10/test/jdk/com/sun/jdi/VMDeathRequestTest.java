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
 * @bug 4419314
 * @summary VMDeathRequestTest checks to see that
 * VMDisconnectedException is never thrown before VMDisconnectEvent.
 *
 * Failure mode for this test is throwing VMDisconnectedException
 * on vm.eventQueue().remove();
 * Does not use a scaffold since we don't want that hiding the exception.
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g HelloWorld.java
 * @run build VMDeathRequestTest
 * @run driver VMDeathRequestTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;


    /********** test program **********/

public class VMDeathRequestTest extends TestScaffold {
    boolean requestedVMDeathOccurred = false;
    boolean defaultVMDeathOccurred = false;
    Object syncer = new Object();
    boolean disconnected = false;
    VMDeathRequest deathRequest;
    EventSet currentEventSet;

    VMDeathRequestTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new VMDeathRequestTest(args).startTests();
    }

    /********** event handlers **********/

    public void eventSetReceived(EventSet set) {
        currentEventSet = set;
    }

    public void vmDied(VMDeathEvent event) {
        if (event.request() == deathRequest) {
            requestedVMDeathOccurred = true;
            println("Got requested VMDeathEvent");
            if (currentEventSet.suspendPolicy() !=
                                   EventRequest.SUSPEND_ALL) {
                failure("failure: wrong suspend policy");
            }
        } else if (event.request() == null) {
            defaultVMDeathOccurred = true;
            println("Got default VMDeathEvent");
        } else {
            failure("failure: Unexpected type of VMDeathEvent occurred");
        }
    }

    public void vmDisconnected(VMDisconnectEvent event) {
        println("Got VMDisconnectEvent");
        disconnected = true;
        synchronized (syncer) {
            syncer.notifyAll();
        }
    }

    /**
     * Turn off default VMDeath handling
     */
    protected void createDefaultVMDeathRequest() {
    }

    /********** test core **********/

    protected void runTests() throws Exception {

        startToMain("HelloWorld");

        deathRequest = eventRequestManager().createVMDeathRequest();
        deathRequest.enable();

        /*
         * Static tests
         */
        List reqs = eventRequestManager().vmDeathRequests();
        if (reqs.size() != 1 || reqs.get(0) != deathRequest) {
            failure("failure: vmDeathRequests()");
        }
        if (!vm().canRequestVMDeathEvent()) {
            failure("failure: canRequestVMDeathEvent() returned false");
        }

        /*
         * Event tests
         */
        addListener(this);
        synchronized (syncer) {
            vm().resume();
            while (!disconnected) {
                try {
                    syncer.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        /*
         * Failure analysis
         */
        if (!requestedVMDeathOccurred) {
            failure("failure: didn't get requested VMDeathEvent");
        }
        if (!defaultVMDeathOccurred) {
            failure("failure: didn't get default VMDeathEvent");
        }

        if (!testFailed) {
            println("VMDeathRequestTest: passed");
        } else {
            throw new Exception("VMDeathRequestTest: failed");
        }
    }
}
