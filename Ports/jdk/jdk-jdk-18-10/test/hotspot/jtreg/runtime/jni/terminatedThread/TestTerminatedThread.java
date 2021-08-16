/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.management.*;

/*
 * @test
 * @bug     8205878 8206954
 * @requires os.family != "windows"
 * @comment Calling pthread_getcpuclockid() with invalid pid leads to undefined
 * behavior in musl libc (see 8240187).
 * @requires !vm.musl
 * @summary Basic test of Thread and ThreadMXBean queries on a natively
 *          attached thread that has failed to detach before terminating.
 * @comment The native code only supports POSIX so no windows testing
 * @run main/othervm/native TestTerminatedThread
 */

public class TestTerminatedThread {

    static native Thread createTerminatedThread();

    static {
        System.loadLibrary("terminatedThread");
    }

    private static ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Throwable {

        Thread t = createTerminatedThread();

        if (!t.isAlive())
            throw new Error("Thread is only supposed to terminate at native layer!");

        // Now invoke the various functions on this thread to
        // make sure the VM handles it okay. The focus is on
        // functions with an underlying native OS implementation.
        // Generally as long as we don't crash or throw unexpected
        // exceptions then the test passes. In some cases we know exactly
        // what a function should return and so can check that.

        System.out.println("Working with thread: " + t +
                           ",  in state: " + t.getState());

        System.out.println("Calling suspend ...");
        t.suspend();
        System.out.println("Calling resume ...");
        t.resume();
        System.out.println("Calling getStackTrace ...");
        StackTraceElement[] stack = t.getStackTrace();
        System.out.println(java.util.Arrays.toString(stack));
        if (stack.length != 0)
            throw new Error("Terminated thread should have empty java stack trace");
        System.out.println("Calling setName(\"NewName\") ...");
        t.setName("NewName");
        System.out.println("Calling interrupt ...");
        t.interrupt();
        System.out.println("Calling stop ...");
        t.stop();

        // Now the ThreadMXBean functions

        if (mbean.isThreadCpuTimeSupported() &&
            mbean.isThreadCpuTimeEnabled() ) {
            System.out.println("Calling getThreadCpuTime ...");
            long t1 = mbean.getThreadCpuTime(t.getId());
            if (t1 != -1) {
                // At least on PPC, we know threads can still be around a short
                // instant. In some stress scenarios we seem to grab times of
                // new threads started with the same thread id. In these cases
                // we get here.
                System.out.println("Unexpected: thread still reports CPU time: " + t1);
            } else {
                System.out.println("Okay: getThreadCpuTime() reported -1 as expected");
            }
        } else {
            System.out.println("Skipping Thread CPU time test as it's not supported");
        }

        System.out.println("Calling getThreadUserTime ...");
        long t1 = mbean.getThreadUserTime(t.getId());
        if (t1 != -1) {
            // At least on PPC, we know threads can still be around a short
            // instant. In some stress scenarios we seem to grab times of
            // new threads started with the same thread id. In these cases
            // we get here.
            System.out.println("Unexpected: thread still reports User time: " + t1);
        } else {
            System.out.println("Okay: getThreadUserTime() reported -1 as expected");
        }

        System.out.println("Calling getThreadInfo ...");
        ThreadInfo info = mbean.getThreadInfo(t.getId());
        System.out.println(info);

        System.out.println("Calling getThreadInfo with stack ...");
        info = mbean.getThreadInfo(t.getId(), Integer.MAX_VALUE);
        System.out.println(info);
    }
}
