/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

/**
 * @test
 * @key headful
 * @bug 4449139
 * @summary test MouseWheelEvent generation by Scrollbar component
 */

public final class ScrollbarMouseWheelTest
        implements MouseWheelListener, WindowListener {

    final static String tk = Toolkit.getDefaultToolkit().getClass().getName();
    final static int REPS = 5;
    // There is a bug on Windows: 4616935.
    // Wheel events comes to every component in the hierarchy so we should
    // check a platform.
    // There are two scrollbars within one Panel and both accept 5 clicks, so
    // Panel would accept 5*2 clicks on Windows.
    final static int PANEL_REPS = tk.equals("sun.awt.windows.WToolkit")? REPS * 2: REPS;

    Scrollbar sb1;
    Scrollbar sb2;
    Panel pnl;
    class Sema {
        boolean flag;
        boolean getVal() { return flag;}
        void setVal(boolean b) { flag = b;}
    }
    Sema sema = new Sema();

    Robot robot;

    int sb1upevents, sb2upevents, pnlupevents;
    int sb1downevents, sb2downevents, pnldownevents;

    public static void main(final String[] args) {
        new ScrollbarMouseWheelTest().test();
    }

    public void test() {
        // Move mouse to upper-right area
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.out.println("Problem creating Robot.  FAIL.");
            throw new RuntimeException("Problem creating Robot.  FAIL.");

        }

        robot.setAutoDelay(500);
        robot.setAutoWaitForIdle(true);

        // Show test Frame
        Frame frame = new Frame("ScrollbarMouseWheelTest");
        frame.addWindowListener(this);
        pnl = new Panel();
        pnl.setLayout(new GridLayout(1, 2));
        pnl.addMouseWheelListener(this);
        sb1 = new Scrollbar();
        sb1.addMouseWheelListener(this);
        pnl.add(sb1);
        sb2 = new Scrollbar();
        pnl.add(sb2);
        frame.add(pnl);
        frame.setSize(200, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();

        // When Frame is active, start testing (handled in windowActivated())
        while (true) {
            synchronized (sema) {
                if (sema.getVal()) {
                    break;
                }
            }
        }
        // up on sb1
        testComp(sb1, true);
        // down on sb1
        testComp(sb1, false);
        // up on sb2
        testComp(sb2, true);
        // down on sb2
        testComp(sb2, false);
        frame.dispose();
        System.out.println("Test done.");
        if (sb1upevents == REPS &&
                sb2upevents == 0 &&
                pnlupevents == PANEL_REPS &&
                sb1downevents == REPS &&
                sb2downevents == 0 &&
                pnldownevents == PANEL_REPS) {
            System.out.println("PASSED.");
        } else {
            System.out.println("Test Failed:" +
                                       "\n\tsb1upevents =" + sb1upevents +
                                       "\n\tsb2upevents = " + sb2upevents +
                                       "\n\tpnlupevents = " + pnlupevents +
                                       "\n\tsb1downevents =" + sb1downevents +
                                       "\n\tsb2downevents = " + sb2downevents +
                                       "\n\tpnldownevents = " + pnldownevents);
            throw new RuntimeException("Test FAILED.");
        }
    }

    public void testComp(Component comp, boolean up) {
        Point loc = comp.getLocationOnScreen();
        robot.mouseMove(loc.x + comp.getWidth() / 2,
                        loc.y + comp.getHeight() / 2);
        for (int loop = 0; loop < REPS; loop++) {
            System.out.println("Robot.mouseWheel() on " + comp.getName());
            robot.mouseWheel(up ? -1 : 1);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent mwe) {
        Component src = mwe.getComponent();
        System.out.println("mouseWheelMoved() on " + src.getName());
        if (mwe.getWheelRotation() == -1) {
            if (src == sb1) {
                sb1upevents++;
            } else if (src == sb2) {
                sb2upevents++;
            } else if (src == pnl) {
                pnlupevents++;
            } else {
                System.out.println("weird source component");
            }
        } else if (mwe.getWheelRotation() == 1) {
            if (src == sb1) {
                sb1downevents++;
            } else if (src == sb2) {
                sb2downevents++;
            } else if (src == pnl) {
                pnldownevents++;
            } else {
                System.out.println("weird source component");
            }
        } else {
            System.out.println("weird wheel rotation");
        }
    }

    public void windowActivated(WindowEvent we) {
        synchronized (sema) {
            sema.setVal(true);
        }
    }

    public void windowClosed(WindowEvent we) {}
    public void windowClosing(WindowEvent we) {}
    public void windowDeactivated(WindowEvent we) {}
    public void windowDeiconified(WindowEvent we) {}
    public void windowIconified(WindowEvent we) {}
    public void windowOpened(WindowEvent we) {}
}// class ScrollbarMouseWheelTest
