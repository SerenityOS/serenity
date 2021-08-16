/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * This test is useful for finding out whether a Thread can have a
 * private variable indicate whether or not it is finished, and to illustrate
 * the ease with which Threads terminate each other.
 *
 * @test
 */

public class CancellableThreadTest {
    public static final int THREADPAIRS = Integer.parseInt(System.getProperty("test.threadpairs", "128"));

    public static void main(String args[]) {
        Thread[] threads = new Thread[THREADPAIRS];
        Canceller[] cancellers = new Canceller[THREADPAIRS];

        System.out.println("Running with " + THREADPAIRS + " thread pairs");

        for (int i = 0; i < THREADPAIRS; i++) {
            cancellers[i] = new Canceller(i);
            threads[i] = new Thread(cancellers[i]);
            threads[i].start();
        }

        for (int i = 0; i < THREADPAIRS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
            }

            if (cancellers[i].failed) {
                throw new RuntimeException(" Test failed in " + i + " th pair. See error messages above.");
            }
        }
    }
}

class Canceller implements Runnable {

    private final CancellableTimer timer;

    public final String name;
    public volatile boolean failed = false;
    private volatile boolean hasBeenNotified = false;

    public Canceller(int index) {
        this.name = "Canceller #" + index;
        timer = new CancellableTimer(index, this);
    }

    public void setHasBeenNotified() {
        hasBeenNotified = true;
    }

    /**
     * This method contains the "action" of this Canceller Thread.
     * It starts a CancellableTimer, waits, and then interrupts the
     * CancellableTimer after the CancellableTimer notifies it.  It then
     * tries to join the CancellableTimer to itself and reports on whether
     * it was successful in doing so.
     */
    public void run() {
        Thread timerThread = new Thread(timer);

        try {
            synchronized(this) {
                timerThread.start();
                wait();
            }
        } catch (InterruptedException e) {
            System.err.println(name + " was interrupted during wait()");
            failed = true;
        }

        if (!hasBeenNotified) {
            System.err.println(name + ".hasBeenNotified is not true as expected");
            failed = true;
        }

        synchronized (timer) {
            timerThread.interrupt();
        }

        try {
            timerThread.join();
        } catch (InterruptedException ie) {
            System.err.println(name + " was interrupted while joining " +
                    timer.name);
            failed = true;
        }

        if (timerThread.isAlive()) {
            System.err.println(timer.name + " is still alive after " + name +
                    " attempted to join it.");
            failed = true;
        }
    }
}

/**
 * This non-public class is the Thread which the Canceller Thread deliberately
 * interrupts and then joins to itself after this Thread has slept for a few milliseconds.
 */

class CancellableTimer implements Runnable {

    public final String name;
    private final Canceller myCanceller;

    public CancellableTimer(int index, Canceller aCanceller) {
        this.name = "CancellableTimer #" + index;
        this.myCanceller = aCanceller;
    }

    /**
     * This is where this CancellableTimer does its work. It notifies its
     * Canceller, waits for the Canceller to interrupt it, then catches the
     * InterruptedException, and sleeps for a few milliseconds before exiting.
     */
    public void run() {
        try {
            synchronized (this) {
                synchronized (myCanceller) {
                    myCanceller.setHasBeenNotified();
                    myCanceller.notify();
                }
                wait();
            }
        } catch (InterruptedException first) {
            // isInterrupted should've been cleared here and we should not register a
            // second interrupt
            if (Thread.currentThread().isInterrupted()) {
                System.err.println(name + " should not register an interrupt here");
                myCanceller.failed = true;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                System.err.println(name + " was interrupted when sleeping");
                myCanceller.failed = true;
            }
        }
    }
}
