/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.ForceEarlyReturn;

import nsk.share.Wicket;
import java.io.PrintStream;
import nsk.share.Consts;
import java.lang.reflect.*;

public class ForceEarlyReturn001 {
    native static boolean doForceEarlyReturnObject(Thread thread, Object valToRet);
    native static boolean doForceEarlyReturnInt(Thread thread, int valToRet);
    native static boolean doForceEarlyReturnLong(Thread thread, long valToRet);
    native static boolean doForceEarlyReturnFloat(Thread thread, float valToRet);
    native static boolean doForceEarlyReturnDouble(Thread thread, double valToRet);
    native static boolean doForceEarlyReturnVoid(Thread thread);

    native static boolean suspendThread(Thread thread);
    native static boolean resumeThread(Thread thread);

    private PrintStream out;

    private int runIt(String argv[], PrintStream _out) {
        out = _out;

        EarlyReturnThread001 thread = null;

        // Object

        out.println("\n>>>> Object:\n");

        thread = new EarlyReturnThread001(MethodType.Object, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnObject(thread, new Object())) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }


        // Int

        out.println("\n>>>> Int:\n");

        thread = new EarlyReturnThread001(MethodType.Int, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnInt(thread, 1)) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }


        // Long

        out.println("\n>>>> Long:\n");

        thread = new EarlyReturnThread001(MethodType.Long, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnLong(thread, 1L)) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }


        // Float

        out.println("\n>>>> Float:\n");

        thread = new EarlyReturnThread001(MethodType.Float, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnFloat(thread, 1)) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }

        // Double

        out.println("\n>>>> Double:\n");

        thread = new EarlyReturnThread001(MethodType.Double, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {}

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnDouble(thread, 1)) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }


        // Void

        out.println("\n>>>> Void:\n");

        thread = new EarlyReturnThread001(MethodType.Void, out);

        thread.start();
        thread.startingBarrier.waitFor();

        synchronized(thread.barrier) {}

        out.println("Suspending thread...");
        if (!suspendThread(thread)) {
            out.println("Failed to suspend the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is suspended.");

        out.println("Repeatedly suspending the thread...");
        if (suspendThread(thread)) {
            out.println("Repeatedly suspended the thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is already suspended.");

        out.println("Forcing thread to early return...");
        if (!doForceEarlyReturnVoid(thread)) {
            out.println("Failed to force thread to early return.");
            thread.stopThread();
            return Consts.TEST_FAILED;
        }
        out.println("Force early return succeeded.");
        thread.stopThread();

        out.println("Resuming thread...");
        if (!resumeThread(thread)) {
            out.println("Failed to resume thread.");
            return Consts.TEST_FAILED;
        }
        out.println("Thread is resumed.");

        try {
            thread.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }

        if (thread.state != Consts.TEST_PASSED) {
            out.println("Thread execution failed.");
            return Consts.TEST_FAILED;
        }

        return Consts.TEST_PASSED;
    }

    /* */
    public static int run(String argv[], PrintStream out) {
        if (new ForceEarlyReturn001().runIt(argv, out) == Consts.TEST_PASSED) {
            out.println("TEST PASSED");
            return Consts.TEST_PASSED;
        } else {
            out.println("TEST FAILED");
            return Consts.TEST_FAILED;
        }
    }

    /* */
    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }
}
