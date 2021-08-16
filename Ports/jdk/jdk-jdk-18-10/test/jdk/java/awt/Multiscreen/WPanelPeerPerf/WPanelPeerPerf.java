/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 @key headful
 @bug 5085626
 @summary Exponential performance regression in AWT components (multiple mon)
 @run main WPanelPeerPerf
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This test must be run on a multi-screen system.
 * This test works by moving a Frame back and forth between the screens a few
 * times.  When the bug is active, the first move will overwhelm the EDT with
 * recursive display change calls.  The test fails if it takes too long to
 * service the setLocation() calls and send componentMoved() events.
 */
public class WPanelPeerPerf {

    private static final int NESTED_PANELS = 25;
    private static final int ITERATIONS_PER_SCREEN = 3;
    private static final int MAX_WAIT_PER_SCREEN = 2500;
    private static final int PAUSE_BETWEEN_MOVES = 500;


    private static Object showLock = new Object();

    private static Counter instance = null;
    public static Counter getCounter() {
        if (instance == null) {
            instance = new Counter();
        }
        return instance;
    }

    private static class Counter {
        int counter;
        Counter() { counter = 0; }
    }

    // This one is very slow!
    public static void testAWT() {
        // fail if only on one screen
        int numScreens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        if (numScreens < 2) {
            System.err.println("Test must be run on a multiscreen system");
            return;
        }
        final Frame frame = new Frame("AWT WPanelPeerPerf");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                System.exit(0);
            }
            public void windowOpened(WindowEvent e) {
                synchronized(showLock) {
                    showLock.notify();
                }
            }
        });
        frame.setLayout(new BorderLayout());
        Label label = new Label("Hello world");
        frame.add(label, BorderLayout.NORTH);
        Panel panel = new Panel(new BorderLayout());
        Panel currentPanel = panel;
        for (int i = 0; i < NESTED_PANELS; i++) {
            Panel newPanel = new Panel(new BorderLayout());
            currentPanel.add(newPanel, BorderLayout.CENTER);
            currentPanel = newPanel;
        }
        currentPanel.add(new Label("WPanelPeerPerf"));
        frame.add(panel, BorderLayout.CENTER);
        Button btn = new Button("OK");
        frame.add(btn, BorderLayout.SOUTH);
        frame.pack();

        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                System.out.println("Frame moved: ");
                Counter ctr = getCounter();
                synchronized(ctr) {
                    ctr.counter++;
                    ctr.notify();
                }
            }
        });
        synchronized(showLock) {
            try {
                frame.setVisible(true);
                showLock.wait();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Problem with showLock");
            }
        }
        runTest(frame);
    }

    public static void runTest(Frame theFrame) {
        System.out.println("Running test");
        GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        Point[] points = new Point[devs.length];

        for (int i = 0; i < points.length; i++) {
            Rectangle bounds = devs[i].getDefaultConfiguration().getBounds();
            points[i] = new Point(bounds.x + (bounds.width / 2),
                    bounds.y + (bounds.height / 2));
            System.out.println("Added point:" + points[i]);
        }

        final Frame localFrame = theFrame;

        for (int n = 0; n < ITERATIONS_PER_SCREEN; n++) {
            for (int i = 0; i < points.length; i++) {
                final Point contextPoint = points[i];
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        localFrame.setLocation(contextPoint);
                    }
                });
                try {
                    Thread.sleep(PAUSE_BETWEEN_MOVES);
                }
                catch (InterruptedException e) {
                    System.out.println("Interrupted during iteration");
                }
            }
        }
        Counter ctr = getCounter();
        synchronized(ctr) {
            try {
                if (ctr.counter < ITERATIONS_PER_SCREEN * devs.length) {
                    // If test hasn't finished, wait for maximum time
                    // If we get interrupted, test fails
                    ctr.wait((long)(ITERATIONS_PER_SCREEN * MAX_WAIT_PER_SCREEN * devs.length));
                    System.out.println("after wait");
                    if (ctr.counter < ITERATIONS_PER_SCREEN * devs.length) {
                        throw new RuntimeException("Waited too long for all the componentMoved()s");
                    }
                }
            }
            catch(InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Wait interrupted - ???");
            }
            System.out.println("Counter reads: " + ctr.counter);
        }

    }
    public static void main(String[] args) {
        testAWT();
    }
}
