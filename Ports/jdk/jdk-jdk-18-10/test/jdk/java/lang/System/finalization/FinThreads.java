/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4026895
   @summary Ensure that System.runFinalization does not run finalizers in the
            thread that invokes it
 */


public class FinThreads {

    static Thread mainThread;

    static Object lock = new Object();    /* Protects following two fields */
    static Thread finalizerThread = null;
    static Thread finalizedBy = null;


    static class Foo {

        boolean catchFinalizer = false;

        /* Instances are only created in an auxiliary thread, in order to
           guard against stray references from the current thread's stack
         */
        public static void create(final boolean catchFinalizer)
            throws InterruptedException
        {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    new Foo(catchFinalizer);
                }});
            t.start();
            t.join();
        }

        public Foo(boolean catchFinalizer) {
            this.catchFinalizer = catchFinalizer;
        }

        public void finalize() throws InterruptedException {
            if (catchFinalizer) {
                boolean gotFinalizer = false;
                synchronized (lock) {
                    if (finalizerThread == null) {
                        finalizerThread = Thread.currentThread();
                        gotFinalizer = true;
                    }
                }
                if (gotFinalizer) {
                    System.err.println("Caught finalizer thread; sleeping...");
                    Thread.sleep(Long.MAX_VALUE);
                }
            } else {
                synchronized (lock) {
                    finalizedBy = Thread.currentThread();
                }
                System.err.println("Test object finalized by " + finalizedBy);
            }
        }

    }


    static void alarm(final Thread sleeper, final long delay)
        throws InterruptedException
    {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(delay);
                    System.err.println("Waking " + sleeper);
                    sleeper.interrupt();
                } catch (InterruptedException x) { }
            }});
        t.setDaemon(true);
        t.start();
    }


    public static void main(String[] args) throws Exception {

        mainThread = Thread.currentThread();

        /* Find the finalizer thread and put it to sleep */
        for (;;) {
            Foo.create(true);
            System.gc();
            synchronized (lock) {
                if (finalizerThread != null) break;
            }
        }

        /* Now create a finalizable object and run finalization */
        alarm(finalizerThread, 5000);
        Foo.create(false);
        for (;;) {
            System.gc();
            System.runFinalization();
            synchronized (lock) {
                if (finalizedBy != null) break;
            }
        }

        if (finalizedBy == mainThread)
            throw new Exception("Finalizer run in main thread");

    }

}
