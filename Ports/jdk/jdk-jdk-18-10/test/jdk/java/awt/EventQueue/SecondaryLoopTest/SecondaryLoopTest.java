/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 6949936
  @author Artem Ananiev: area=eventqueue
  @run main/timeout=30 SecondaryLoopTest
*/

import java.awt.*;

/**
 * Unit test for java.awt.SecondaryLoop implementation
 */
public class SecondaryLoopTest {

    private static volatile boolean loopStarted;
    private static volatile boolean doubleEntered;
    private static volatile boolean loopActive;
    private static volatile boolean eventDispatched;

    public static void main(String[] args) throws Exception {
        test(true, true);
        test(true, false);
        test(false, true);
        test(false, false);
    }

    private static void test(final boolean enterEDT, final boolean exitEDT) throws Exception {
        System.out.println("Running test(" + enterEDT + ", " + exitEDT + ")");
        System.err.flush();
        loopStarted = true;
        Runnable enterRun = new Runnable() {
            @Override
            public void run() {
                Toolkit tk = Toolkit.getDefaultToolkit();
                EventQueue eq = tk.getSystemEventQueue();
                final SecondaryLoop loop = eq.createSecondaryLoop();
                doubleEntered = false;
                eventDispatched = false;
                Runnable eventRun = new Runnable() {
                    @Override
                    public void run() {
                        // Let the loop enter
                        sleep(1000);
                        if (loop.enter()) {
                            doubleEntered = true;
                        }
                        eventDispatched = true;
                    }
                };
                EventQueue.invokeLater(eventRun);
                Runnable exitRun = new Runnable() {
                    @Override
                    public void run() {
                        // Let the loop enter and eventRun finish
                        sleep(2000);
                        if (doubleEntered) {
                            // Hopefully, we get here if the loop is entered twice
                            loop.exit();
                        }
                        loop.exit();
                    }
                };
                if (exitEDT) {
                    EventQueue.invokeLater(exitRun);
                } else {
                    new Thread(exitRun).start();
                }
                if (!loop.enter()) {
                    loopStarted = false;
                }
                loopActive = eventDispatched;
            }
        };
        if (enterEDT) {
            EventQueue.invokeAndWait(enterRun);
        } else {
            enterRun.run();
        }
        // Print all the flags before we fail with exception
        System.out.println("    loopStarted = " + loopStarted);
        System.out.println("    doubleEntered = " + doubleEntered);
        System.out.println("    loopActive = " + loopActive);
        System.out.flush();
        if (!loopStarted) {
            throw new RuntimeException("Test FAILED: the secondary loop is not started");
        }
        if (doubleEntered) {
            throw new RuntimeException("Test FAILED: the secondary loop is started twice");
        }
        if (!loopActive) {
            throw new RuntimeException("Test FAILED: the secondary loop exited immediately");
        }
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

}
