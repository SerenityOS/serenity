/*
 * Copyright 2009 D.E. Shaw.  All Rights Reserved.
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
 * @bug 6912517
 * @summary JIT bug compiles out (and stops running) code that needs to be run.  Causes NPE.
 *
 * @run main/othervm -Xbatch -XX:CompileThreshold=100 -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedOops
 *      compiler.c2.Test6912517
 */

package compiler.c2;

/**
 * Highlights a bug with the JIT compiler.
 * @author Matt Bruce m b r u c e __\at/__ g m a i l DOT c o m
 */
public class Test6912517 implements Runnable
{
    private final Thread myThread;
    private Thread       myInitialThread;
    private boolean      myShouldCheckThreads;

    /**
     * Sets up the running thread, and starts it.
     */
    public Test6912517(int id)
    {
        myThread = new Thread(this);
        myThread.setName("Runner: " + id);
        myThread.start();
        myShouldCheckThreads = false;
    }

    /**
     * @param shouldCheckThreads the shouldCheckThreads to set
     */
    public void setShouldCheckThreads(boolean shouldCheckThreads)
    {
        myShouldCheckThreads = shouldCheckThreads;
    }

    /**
     * Starts up the two threads with enough delay between them for JIT to
     * kick in.
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
        // let this run for a bit, so the "run" below is JITTed.
        for (int id = 0; id < 20; id++) {
            System.out.println("Starting thread: " + id);
            Test6912517 bug = new Test6912517(id);
            bug.setShouldCheckThreads(true);
            Thread.sleep(2500);
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        long runNumber = 0;
        while (true) {
            // run hot for a little while, give JIT time to kick in to this loop.
            // then run less hot.
            if (runNumber > 15000) {
                try {
                    Thread.sleep(5);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runNumber++;
            ensureProperCallingThread();
        }
    }

    private void ensureProperCallingThread()
    {
        // this should never be null.  but with the JIT bug, it will be.
        // JIT BUG IS HERE ==>>>>>
        if (myShouldCheckThreads) {
            if (myInitialThread == null) {
                myInitialThread = Thread.currentThread();
            }
            else if (myInitialThread != Thread.currentThread()) {
                System.out.println("Not working: " + myInitialThread.getName());
            }
        }
    }
}
