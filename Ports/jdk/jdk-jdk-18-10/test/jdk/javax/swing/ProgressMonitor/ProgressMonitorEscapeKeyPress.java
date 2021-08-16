/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8065861
 * @summary Test to check pressing Escape key sets 'canceled' property of ProgressMonitor
 * @run main ProgressMonitorEscapeKeyPress
 */

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

public class ProgressMonitorEscapeKeyPress {

    static ProgressMonitor monitor;
    static int counter = 0;
    static TestThread robotThread;
    static JFrame frame;


    public static void main(String[] args) throws Exception {

        createTestUI();

        monitor = new ProgressMonitor(frame, "Progress", null, 0, 100);

        robotThread = new TestThread();
        robotThread.start();

        for (counter = 0; counter <= 100; counter += 10) {
            Thread.sleep(1000);

            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (!monitor.isCanceled()) {
                        monitor.setProgress(counter);
                        System.out.println("Progress bar is in progress");
                    }
                }
            });

            if (monitor.isCanceled()) {
                break;
            }
        }

        disposeTestUI();

        if (counter >= monitor.getMaximum()) {
            throw new RuntimeException("Escape key did not cancel the ProgressMonitor");
        }
    }

     private static void createTestUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
           @Override
           public void run() {
                frame = new JFrame("Test");
                frame.setSize(300, 300);
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
              }});
     }


     private static void disposeTestUI() throws Exception {
           SwingUtilities.invokeAndWait(() -> {
               frame.dispose();
           });
       }
}


class TestThread extends Thread {

    Robot testRobot;

    TestThread() throws AWTException {
        super();
        testRobot = new Robot();
    }

    @Override
    public void run() {
        try {
            // Sleep for 5 seconds - so that the ProgressMonitor starts
            Thread.sleep(5000);

            // Press and release Escape key
            testRobot.keyPress(KeyEvent.VK_ESCAPE);
            testRobot.delay(20);
            testRobot.keyRelease(KeyEvent.VK_ESCAPE);
        } catch (InterruptedException ex) {
            throw new RuntimeException("Exception in TestThread");
        }
    }
}

