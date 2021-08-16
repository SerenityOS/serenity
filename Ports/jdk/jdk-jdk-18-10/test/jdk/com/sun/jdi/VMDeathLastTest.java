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
 * @bug 4420844 4449394
 * @summary Checks that no events are sent after VMDeath, and test vm.canBeModified
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g HelloWorld.java
 * @run build VMDeathLastTest
 * @run driver VMDeathLastTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** test program **********/

public class VMDeathLastTest extends TestScaffold {
    Object syncer = new Object();
    boolean vmDead = false;
    boolean disconnected = false;

    VMDeathLastTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new VMDeathLastTest(args).startTests();
    }

    /********** event handlers **********/

    public void methodEntered(MethodEntryEvent event) {
        if (vmDead) {
            failure("Failure: Got MethodEntryEvent after VM Dead");
        }
    }

    public void classPrepared(ClassPrepareEvent event) {
        if (vmDead) {
            failure("Failure: Got ClassPrepareEvent after VM Dead");
        }
    }

    public void threadDied(ThreadDeathEvent event) {
        if (vmDead) {
            failure("Failure: Got ThreadDeathEvent after VM Dead");
        }
    }

    public void vmDied(VMDeathEvent event) {
        println("Got VMDeathEvent");
        vmDead = true;
    }

    public void vmDisconnected(VMDisconnectEvent event) {
        println("Got VMDisconnectEvent");
        if (!vmDead) {
            failure("Test failure: didn't get VMDeath");
        }
        disconnected = true;
        synchronized (syncer) {
            syncer.notifyAll();
        }
    }

    /**
     * Turn off default VMDeath handling.
     */
    protected void createDefaultVMDeathRequest() {
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        startToMain("HelloWorld");
        if (!vm().canBeModified()) {
            failure("VM says it is read-only");
        }
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Set event requests
         */
        erm.createMethodEntryRequest().enable();
        erm.createClassPrepareRequest().enable();
        erm.createThreadDeathRequest().enable();

        /*
         * resume the target listening for events
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
         * deal with results of test
         */
        if (!testFailed) {
            println("VMDeathLastTest: passed");
        } else {
            throw new Exception("VMDeathLastTest: failed");
        }
    }
}
