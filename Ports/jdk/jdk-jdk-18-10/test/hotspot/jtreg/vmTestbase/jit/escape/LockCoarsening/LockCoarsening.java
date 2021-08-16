/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jit.escape.LockCoarsening;

import nsk.share.TestFailure;

public class LockCoarsening {
    // JVM option '-XX:+EliminateLocks' specified
    public static boolean eliminateLocks = false;
    // Number of chances Thread 2 has to acquire the lock
    public static int numChances = 16;

    // Signals to Threads_2 that Thread_1 started execuition
    public static volatile boolean start;
    // Signals to Thread_2 to stop execution
    public static volatile boolean done;
    // Thread_2 has acquired the lock
    public static volatile boolean acquiredLock;
    // Actually running the test
    public static volatile boolean realrun;
    // Thread 2 'acquire lock chance' number
    public static volatile int currentChance;

    static Thread_2 t2;

    public static void main(String[] args) {
        parseArgs(args);

        Thread.currentThread().getThreadGroup().setMaxPriority(Thread.MAX_PRIORITY);

        currentChance = 1;

        do {
            System.out.println("Chance " + currentChance + ":");

            Thread_1 t1 = new Thread_1();
            t1.getThreadGroup().setMaxPriority(Thread.MAX_PRIORITY);
            t1.setPriority(Thread.MIN_PRIORITY);
            t1.start();

            try {
                t1.join();
            } catch (InterruptedException e) {
            }

            System.out.println();

            // if thread 2 hasn't acquired lock and we are not eliminating them, give it one more try...
        } while (!eliminateLocks && !acquiredLock && ++currentChance <= numChances);

        System.out.println("Thread 2 has acquired lock: " + acquiredLock);

        boolean failed = false;

        if (!eliminateLocks) {
            if (!acquiredLock) {
                failed = true;

                throw new TestFailure("acquiredLock == false, though, '-XX:-EliminateLocks' specified");
            }
        } else {
            if (acquiredLock) {
                failed = true;

                throw new TestFailure("acquiredLock == true, though, '-XX:+EliminateLocks' specified");
            }
        }

        if (!failed)
            System.out.println("TEST PASSED");
        else
            throw new TestFailure("TEST FAILED");
    }

    private static void parseArgs(String[] args) {
        eliminateLocks = false;

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            String val;

            if (arg.equals("-eliminateLocks")) {
                eliminateLocks = true;
            } else if (arg.equals("-numChances")) {
                if (++i >= args.length)
                    throw new TestFailure("'numChances' parameter requires an integer value");
                val = args[i];
                try {
                    numChances = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    throw new TestFailure("invalid value for 'numChances'");
                }
            } else {
                System.out.println("Invalid argument: " + args);

            }
        }
    }

    /**
     * Thread that enters synchronized parts which are subject of
     * lock coarsening
     */
    public static class Thread_1 extends Thread {
        public void run() {
            Dummy lock = new Dummy();

            // An ugly-terrible hack to force JIT to compile Thread_1.doit():
            // 1: call method from a static method of another class within a loop
            System.out.println("**** Compilation warm-up *****");
            realrun = false;
            Helper.callMethod(this, lock);

            // 2: call method normally
            System.out.println("**** Starting real run ****");
            realrun = true;
            this.doit(lock);
        }

        public final void doit(Dummy _lock) {
            Dummy lock = new Dummy();

            start = false;
            done = false;
            acquiredLock = false;

            /*Thread_2*/
            t2 = new Thread_2(lock);
            t2.getThreadGroup().setMaxPriority(Thread.MAX_PRIORITY);
            t2.setPriority(Thread.MAX_PRIORITY);
            t2.start();

            //waiting for the Thread_2 to invoke lock.wait()
            while (t2.getState() != Thread.State.WAITING) { }
            start = true;

            // The following code is subject to lock coarsening if eliminateLocks == true
            {
                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                synchronized (lock) {
                    lock.foo();
                }

                // Footer
                synchronized (lock) {
                    lock.foo();

                    done = true;

                    lock.notify();
                }
            }
            try {
                t2.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Thread that tries to acquire lock during lock coarsening.
     * If it unable to do it then lock coarsening occurred.
     */
    private static class Thread_2 extends Thread {
        private Dummy lock;

        public Thread_2(Dummy lock) {
            this.lock = lock;
        }

        public void run() {
            Dummy lock = this.lock;

            synchronized (lock) {
                if (!done) {
                    while (!start) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted!");
                        }
                    }

                    if (!done) {
                        done = true;

                        acquiredLock = true;

                        if (realrun) {
                            System.out.println("Acquired lock at " + lock.counter + " iteration of " + currentChance + " chance");
                        } else if (eliminateLocks) {
                            //forcibly stop warm-up as we see that lock coarsening occurs
                            Helper.allowExec = true;
                        }

                    }
                }
            }
        }
    }

    /**
     *  Helper class to make method Thread_1.doit() be compiled.
     */
    public static class Helper {
        public static volatile boolean allowExec = false;
        private static int iterations = 10000;

        public static void callMethod(Thread_1 t, Dummy lock) {
            for (int i = 0; i < iterations; ++i) {
                t.doit(lock);
                if (allowExec)
                    break;
            }
        }
    }

    /**
     *  Class to count number of synchronized statement.
     *  If test fails Dummy.counter shows iteration when lock coarsening did not happen
     */
    public static class Dummy {
        public volatile int counter = 0;

        public void foo() {
            if (done)
                return;

            while (t2.getState() != Thread.State.BLOCKED && t2.getState() != Thread.State.WAITING) {
                this.notifyAll();

                Thread.yield();
            }

            this.notifyAll();

            while (t2.getState() != Thread.State.BLOCKED) {
                Thread.yield();
            }

            ++counter;

            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
        }
    }
}
