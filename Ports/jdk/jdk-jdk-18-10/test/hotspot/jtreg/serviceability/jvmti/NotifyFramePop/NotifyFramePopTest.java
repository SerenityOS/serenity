/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies NotifyFramePop request is cleared if JVMTI_EVENT_FRAME_POP is disabled
 * @requires vm.jvmti
 * @library /test/lib
 * @compile NotifyFramePopTest.java
 * @run main/othervm/native -agentlib:NotifyFramePopTest NotifyFramePopTest
 */

import jtreg.SkippedException;

public class NotifyFramePopTest {
    static {
        try {
            System.loadLibrary("NotifyFramePopTest");
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Could not load NotifyFramePopTest library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ex;
        }
    }

    public static void main(String args[]) {
        if (!canGenerateFramePopEvents()) {
            throw new SkippedException("FramePop event is not supported");
        }

        // Sanity testing that FRAME_POP works.
        test("sanity", true, () -> {
            setFramePopNotificationMode(true);
            notifyFramePop(null);
        });

        // Request notification and then disable FRAME_POP event notification.
        // This should not prevent the notification for the frame being cleared
        // when we return from the method.
        test("requestAndDisable", false, () -> {
            setFramePopNotificationMode(true);
            notifyFramePop(null);
            setFramePopNotificationMode(false);
        });

        // Ensure there is no pending event
        test("ensureCleared", false, () -> {
            setFramePopNotificationMode(true);
        });

        log("Test PASSED");
    }

    private native static boolean canGenerateFramePopEvents();
    private native static void setFramePopNotificationMode(boolean enabled);
    private native static void notifyFramePop(Thread thread);
    private native static boolean framePopReceived();

    private static void log(String msg) {
        System.out.println(msg);
    }

    private interface Test {
        void test();
    }

    private static void test(String name, boolean framePopExpected, Test theTest) {
        log("test: " + name);
        theTest.test();
        boolean actual = framePopReceived();
        if (framePopExpected != actual) {
            throw new RuntimeException("unexpected notification:"
                    + " FramePop expected: " + (framePopExpected ? "yes" : "no")
                    + ", actually received: " + (actual ? "yes" : "no"));
        }
        log("  - OK (" + (actual ? "received" : "NOT received") + ")");
    }

}
