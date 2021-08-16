/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7105883
 * @summary Ensure that JDWP doesn't crash with a null thread group name
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run driver NullThreadGroupNameTest
 */
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.jdi.VMDisconnectedException;
import java.util.concurrent.CountDownLatch;
import java.util.*;

class DebugTarget {
    public final static String DEBUG_THREAD_NAME = "DebugThread";

    public static void main(String[] args) throws Exception {
        DebugThread thread = new DebugThread();
        thread.start();
        thread.runningLatch.await();
        breakpointHere();
        thread.breakpointLatch.countDown();
    }

    public static void breakpointHere() {
        System.out.println("Breakpoint finished!");
    }

    static class DebugThread extends Thread {
        final CountDownLatch runningLatch = new CountDownLatch(1);
        final CountDownLatch breakpointLatch = new CountDownLatch(1);

        public DebugThread() {
            super(new ThreadGroup(null), DEBUG_THREAD_NAME);
        }

        public void run() {
            runningLatch.countDown();
            try {
                breakpointLatch.await();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}

public class NullThreadGroupNameTest extends TestScaffold {

    NullThreadGroupNameTest(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new NullThreadGroupNameTest(args).startTests();
    }

    protected void runTests() throws Exception {
        startTo("DebugTarget", "breakpointHere", "()V");

        ThreadReference thread = findThread(DebugTarget.DEBUG_THREAD_NAME);
        assertThreadGroupName(thread.threadGroup(), "");

        listenUntilVMDisconnect();
    }

    private ThreadReference findThread(String name) {
        for (ThreadReference thread : vm().allThreads()) {
            if (name.equals(thread.name())) {
                return thread;
            }
        }
        throw new NoSuchElementException("Couldn't find " + name);
    }

    private void assertThreadGroupName(ThreadGroupReference threadGroup, String expectedName) {
        try {
            String name = threadGroup.name();
            if (!expectedName.equals(name)) {
                throw new AssertionError("Unexpected thread group name '" + name + "'");
            }
        } catch (VMDisconnectedException vmde) {
            throw new AssertionError("Likely JVM crash with null thread group name", vmde);
        }
    }
}
