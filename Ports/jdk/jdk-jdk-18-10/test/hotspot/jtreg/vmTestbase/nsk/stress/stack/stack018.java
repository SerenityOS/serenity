/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/stack/stack018.
 * VM testbase keywords: [stress, diehard, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     This test provokes multiple stack overflows by invocations via
 *     reflection -- repeatedly multiple times, and in multiple threads.
 *     Recursive method is invoked for the given fixed depth of recursion
 *     (though, for a large depth). The test measures a number of recursive
 *     invocations until stack overflow, and then tries to reproduce similar
 *     stack overflows 10 times in each of 10 threads -- each time by trying
 *     to invoke the same recursive method for the given fixed depth
 *     of invocations (which is 10 times that crucial depth just measured).
 *     The test is deemed passed, if VM have not crashed, and
 *     if exception other than due to stack overflow was not
 *     thrown.
 * COMMENTS
 *     This test crashes HS versions 2.0, 1.3, and 1.4 on both
 *     Solaris and Win32 platforms.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires (vm.opt.DeoptimizeALot != true & vm.compMode != "Xcomp" & vm.pageSize == 4096)
 * @library /vmTestbase
 * @build nsk.share.Terminator
 * @run main/othervm/timeout=900 -Xss220K nsk.stress.stack.stack018 -eager
 */

package nsk.stress.stack;


import nsk.share.Terminator;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class stack018 extends Thread {
    private final static int THREADS = 10;
    private final static int CYCLES = 10;
    private final static int STEP = 100;
    private final static int RESERVE = 100;

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        verbose = false;
        boolean eager = false;
        for (int i = 0; i < args.length; i++)
            if (args[i].toLowerCase().equals("-verbose"))
                verbose = true;
            else if (args[i].toLowerCase().equals("-eager"))
                eager = true;
        if (!eager)
            Terminator.appoint(Terminator.parseAppointment(args));
        stack018.out = out;
        stack018 test = new stack018();
        return test.doRun();
    }

    private static boolean verbose;
    private static PrintStream out;

    private void display(Object message) {
        if (!verbose)
            return;
        synchronized (out) {
            out.println(message.toString());
        }
    }

    private int doRun() {
        //
        // Measure maximal recursion depth until stack overflow:
        //
        int maxDepth = 0;
        for (depthToTry = 0; ; depthToTry += STEP)
            try {
                invokeRecurse(depthToTry);
                maxDepth = depthToTry;
            } catch (Throwable exception) {
                Throwable target = getTargetException(exception);
                if ((target instanceof StackOverflowError) ||
                        (target instanceof OutOfMemoryError))
                    break; // OK.
                target.printStackTrace(out);
                if (target instanceof ThreadDeath)
                    throw (ThreadDeath) target;
                return 2;
            }
        out.println("Maximal recursion depth: " + maxDepth);

        //
        // Run the tested threads:
        //
        stack018 threads[] = new stack018[THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new stack018();
            threads[i].setName("Thread: " + (i + 1) + "/" + THREADS);
            threads[i].depthToTry = RESERVE * maxDepth;
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++)
            if (threads[i].isAlive())
                try {
                    threads[i].join();
                } catch (InterruptedException exception) {
                    exception.printStackTrace(out);
                    return 2;
                }

        //
        // Check if unexpected exceptions were thrown:
        //
        int exitCode = 0;
        for (int i = 0; i < threads.length; i++)
            if (threads[i].thrown != null) {
                out.println("# " + threads[i].getName()
                        + ": " + threads[i].thrown);
                exitCode = 2;
            }

        if (exitCode != 0)
            out.println("# TEST FAILED");
        return exitCode;
    }

    private int depthToTry = 0;
    private Throwable thrown = null;

    public void run() {
        String threadName = Thread.currentThread().getName();
        for (int i = 1; i <= CYCLES; i++)
            try {
                display(threadName + ", iteration: " + i + "/" + CYCLES);
                invokeRecurse(depthToTry);
                throw new Error("TEST_RFE: try deeper invocations!");

            } catch (Throwable exception) {
                Throwable target = getTargetException(exception);
                if ((target instanceof StackOverflowError) ||
                        (target instanceof OutOfMemoryError))
                    continue; // OK.
                if (target instanceof ThreadDeath)
                    throw (ThreadDeath) target;
                thrown = target;
                break;
            }
    }

    private static Throwable getTargetException(Throwable exception) {
        Throwable target;
        //
        // Unwrap deep chain of exceptions to find StackOverflowError:
        //
        for (
                target = exception;
                target instanceof InvocationTargetException;
                target = ((InvocationTargetException) target).getTargetException()
                )
            ;
        return target;
    }

    private Method method = null;
    private Object params[] = null;

    private void invokeRecurse(int depth) throws Exception {
        if (method == null) {
            //
            // Optimization trick: allocate once, use everywhere.
            //
            method = stack018.class.getMethod("recurse");
            params = new Object[]{};
        }
        this.depth = depth; // actual parameter
        method.invoke(this, params);
    }

    private int depth = 0; // actual parameter for recurse()

    public void recurse() throws Exception {
        if (depth > 0)
            //
            // Self-invoke via reflection:
            //
            invokeRecurse(depth - 1);
    }
}
