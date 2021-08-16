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

import java.io.*;
import java.lang.reflect.*;
import nsk.share.Wicket;
import nsk.share.Consts;

interface IA {}
interface IB extends IA {}
interface IC {}
class A implements IA {}
class B extends A implements IB, IC {}

class EarlyReturnThread002 extends Thread {
    public volatile int state = Consts.TEST_PASSED;
    public volatile boolean earlyReturned = true;
    public volatile boolean stop = false;
    public volatile Object result = null;

    private boolean complain = true;
    private PrintStream out = System.out;

    public Wicket startingBarrier = new Wicket();
    public Object barrier = new Object();

    private Method methodToExecute;

    public void stopThread() {
        stop = true;
    }


    public EarlyReturnThread002 (
            Method _method
            , PrintStream _out
            )
    {
        methodToExecute = _method;
        out = _out;
    }

    public void setEarlyReturnedStatus(boolean status) {
        earlyReturned = status;
    }

    public void run() {
        try {
            out.println("run(): before method invoke");
            out.println("Method name: "+methodToExecute.getName());
            result = methodToExecute.invoke(this);
            out.println("run(): after method invoke");
        }
        catch (Throwable e) {
            out.println(e.getClass().getName()+": "+e.getMessage());
            state = Consts.TEST_FAILED;
        }
    }

    public void checkResults() {
        if (earlyReturned) {
            out.println("TEST FAILED: a tested frame wasn't returned");
            state = Consts.TEST_FAILED;
            complain = false;
        }
    }

    public Object method1 () {
        synchronized(barrier) {
            out.println("Actually invoked method1");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new Object();
    }

    public A method2 () {
        synchronized(barrier) {
            out.println("Actually invoked method2");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new A();
    }
    public B method3 () {
        synchronized(barrier) {
            out.println("Actually invoked method3");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B();
    }
    public IA method4 () {
        synchronized(barrier) {
            out.println("Actually invoked method4");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new A();
    }
    public IB method5 () {
        synchronized(barrier) {
            out.println("Actually invoked method5");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B();
    }
    public IC method6 () {
        synchronized(barrier) {
            out.println("Actually invoked method6");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B();
    }
    public Object[] method7 () {
        synchronized(barrier) {
            out.println("Actually invoked method7");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new A[0];
    }
    public Object[][] method8 () {
        synchronized(barrier) {
            out.println("Actually invoked method8");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0][0];
    }

    public IC[] method9 () {
        synchronized(barrier) {
            out.println("Actually invoked method9");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0];
    }

    public IC[][] method10 () {
        synchronized(barrier) {
            out.println("Actually invoked method10");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0][0];
    }

    public int[] method11 () {
        synchronized(barrier) {
            out.println("Actually invoked method11");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new int[0];
    }
    public int[][] method12 () {
        synchronized(barrier) {
            out.println("Actually invoked method12");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new int[0][0];
    }
    public A[] method13 () {
        synchronized(barrier) {
            out.println("Actually invoked method13");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0];
    }
    public A[][] method14 () {
        synchronized(barrier) {
            out.println("Actually invoked method14");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0][0];
    }
    public B[] method15 () {
        synchronized(barrier) {
            out.println("Actually invoked method15");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0];
    }
    public B[][] method16 () {
        synchronized(barrier) {
            out.println("Actually invoked method16");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0][0];
    }
    public IA[] method17 () {
        synchronized(barrier) {
            out.println("Actually invoked method17");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0];
    }
    public IA[][] method18 () {
        synchronized(barrier) {
            out.println("Actually invoked method18");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new A[0][0];
    }
    public IB[] method19 () {
        synchronized(barrier) {
            out.println("Actually invoked method19");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0];
    }
    public IB[][] method20 () {
        synchronized(barrier) {
            out.println("Actually invoked method20");
            startingBarrier.unlock();
        }

        // loop until the main thread forces early return
        int i = 0; int n = 1000;
        while (!stop) {
            if (n <= 0) { n = 1000; }
            if (i > n) { i = 0; n--; }
            i++;
        }

        checkResults();
        return new B[0][0];
    }
}

public class ForceEarlyReturn002 {
    private PrintStream out;

    private int runIt(String argv[], PrintStream _out) {
        out = _out;

        Object[] params = {
                new Object()
                , new A()
                , new B()
                , new Object[1]
                , new Object[1][1]
                , new int[1]
                , new int[1][1]
                , new A[1]
                , new A[1][1]
                , new B[1]
                , new B[1][1]
        };

        for (Method method : EarlyReturnThread002.class.getMethods()) {
            if (!method.getName().startsWith("method"))
                continue;

            for (Object param : params) {
                out.println("\nITERATION:\n");
                out.println("\tOriginal return type: \""+method.getReturnType().getName()+"\"");
                out.println("\t  Forced return type: \""+param.getClass().getName()+"\"\n");


                EarlyReturnThread002 thread = new EarlyReturnThread002(method, out);

                thread.start();
                thread.startingBarrier.waitFor();

                synchronized(thread.barrier) {
                }

                out.println("Suspending thread...");
                if (!ForceEarlyReturn001.suspendThread(thread)) {
                    out.println("Failed to suspend the thread.");
                    return Consts.TEST_FAILED;
                }
                out.println("Thread is suspended.\n");

                out.println("Repeatedly suspending the thread...");
                if (ForceEarlyReturn001.suspendThread(thread)) {
                    out.println("Repeatedly suspended the thread.\n");
                    return Consts.TEST_FAILED;
                }
                out.println("Thread is already suspended.\n");

                out.println("Forcing thread to early return...");
                boolean result = ForceEarlyReturn001.doForceEarlyReturnObject(thread, param);
                boolean isAssignable = method.getReturnType().isAssignableFrom(param.getClass());

                if (result) {
                    out.println("Force early return SUCCEEDED.\n");
                } else {
                    out.println("FAILED to force thread to early return.\n");
                }

                // In case ForceEarlyReturn invocation failed, a thread being
                // modified should know about that when it analyses its status
                thread.setEarlyReturnedStatus(result);

                thread.stopThread();

                out.println("Resuming thread...");
                if (!ForceEarlyReturn001.resumeThread(thread)) {
                    out.println("Failed to resume thread.");
                    return Consts.TEST_FAILED;
                }
                out.println("Thread is resumed.\n");

                try {
                    thread.join();
                } catch (InterruptedException e) {
                    out.println("TEST INCOMPLETE: caught " + e);
                    return Consts.TEST_FAILED;
                }
                out.println("  Original return type: \""+method.getReturnType().getName()+"\"");
                out.println("Actually returned type: \""
                        +(thread.result !=null ? thread.result.getClass().getName() : "null") +"\"\n");
                out.println(param.getClass().getName() + (isAssignable ? " => " : " <> ") + method.getReturnType().getName());

                if (result ^ isAssignable) {
                    return Consts.TEST_FAILED;
                }

                if (thread.state != Consts.TEST_PASSED) {
                    out.println("Thread execution failed.");
                    return Consts.TEST_FAILED;
                }
            }
        }

        return Consts.TEST_PASSED;
    }

    /* */
    public static int run(String argv[], PrintStream out) {
        if (new ForceEarlyReturn002().runIt(argv, out) == Consts.TEST_PASSED) {
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
