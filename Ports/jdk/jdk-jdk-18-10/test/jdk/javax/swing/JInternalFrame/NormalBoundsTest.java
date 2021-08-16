/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 @bug 7126823
 @summary Verify NormalBounds upon iconify/deiconify sequence
 @run main NormalBoundsTest
 */
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.beans.PropertyVetoException;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

public class NormalBoundsTest {

    private static JFrame mainFrame;
    private static JInternalFrame internalFrame;
    private static Rectangle bounds;

    private static void createUI(String lookAndFeelString) {
        internalFrame = new JInternalFrame("Internal", true, true, true, true);
        internalFrame.setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE);
        internalFrame.setSize(200, 200);

        JDesktopPane desktopPane = new JDesktopPane();
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktopPane.add(internalFrame);

        mainFrame = new JFrame(lookAndFeelString);
        mainFrame.setSize(640, 480);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setContentPane(desktopPane);

        mainFrame.setVisible(true);
        internalFrame.setVisible(true);

    }

    private static int signWOZero(int i) {
        return (i > 0) ? 1 : -1;
    }

    private static void mouseMove(Robot robot, Point startPt, Point endPt) {
        int dx = endPt.x - startPt.x;
        int dy = endPt.y - startPt.y;

        int ax = Math.abs(dx) * 2;
        int ay = Math.abs(dy) * 2;

        int sx = signWOZero(dx);
        int sy = signWOZero(dy);

        int x = startPt.x;
        int y = startPt.y;

        int d = 0;

        if (ax > ay) {
            d = ay - ax / 2;
            while (true) {
                robot.mouseMove(x, y);
                robot.delay(50);

                if (x == endPt.x) {
                    return;
                }
                if (d >= 0) {
                    y = y + sy;
                    d = d - ax;
                }
                x = x + sx;
                d = d + ay;
            }
        } else {
            d = ax - ay / 2;
            while (true) {
                robot.mouseMove(x, y);
                robot.delay(50);

                if (y == endPt.y) {
                    return;
                }
                if (d >= 0) {
                    x = x + sx;
                    d = d - ay;
                }
                y = y + sy;
                d = d + ax;
            }
        }
    }

    private static void drag(Robot r, Point startPt, Point endPt, int button) {
        if (!(button == InputEvent.BUTTON1_MASK
                || button == InputEvent.BUTTON2_MASK
                || button == InputEvent.BUTTON3_MASK)) {
            throw new IllegalArgumentException("invalid mouse button");
        }

        r.mouseMove(startPt.x, startPt.y);
        r.mousePress(button);
        try {
            mouseMove(r, startPt, endPt);
        } finally {
            r.mouseRelease(button);
        }
    }

    private static boolean tryLookAndFeel(String lookAndFeelString) {
        try {
            UIManager.setLookAndFeel(lookAndFeelString);
            return true;
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                InstantiationException | IllegalAccessException e) {
            return false;
        }
    }

    public static void executeTest(Robot robot) throws Exception {

        // Iconize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setIcon(true);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("Iconize InternalFrame Failed");
                }
            }
        });
        robot.waitForIdle();

        // Deiconize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setIcon(false);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("Deiconize InternalFrame"
                            + " Failed");
                }
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Point loc = internalFrame.getLocationOnScreen();
                // Drag Frame
                drag(robot,
                        new Point((int) loc.x + 80, (int) loc.y + 12),
                        new Point((int) loc.x + 100, (int) loc.y + 40),
                        InputEvent.BUTTON1_MASK);
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                bounds = internalFrame.getBounds();
                if (!internalFrame.getNormalBounds().equals(bounds)) {
                    mainFrame.dispose();
                    throw new RuntimeException("Invalid NormalBounds");
                }
            }
        });
        robot.waitForIdle();

        // Regression Test Bug ID: 4424247
        // Maximize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setMaximum(true);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("Maximize InternalFrame Failed");
                }
            }
        });
        robot.waitForIdle();

        // Iconize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setIcon(true);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("Iconize InternalFrame Failed");
                }
            }
        });
        robot.waitForIdle();

        // DeIconize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setIcon(false);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("DeIcoize InternalFrame "
                            + " Failed");
                }
            }
        });
        robot.waitForIdle();

        // Restore/Undo Maximize JInternalFrame
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    internalFrame.setMaximum(false);
                } catch (PropertyVetoException ex) {
                    mainFrame.dispose();
                    throw new RuntimeException("Restore InternalFrame "
                            + " Failed");
                }
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (!internalFrame.getBounds().equals(bounds)) {
                    mainFrame.dispose();
                    throw new RuntimeException("Regression Test Failed");
                }
            }
        });
        robot.waitForIdle();

        mainFrame.dispose();
    }

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            String lookAndFeelString = lookAndFeelItem.getClassName();
            if (tryLookAndFeel(lookAndFeelString)) {
                // create UI
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        createUI(lookAndFeelString);
                    }
                });

                robot.waitForIdle();
                executeTest(robot);
            } else {
                throw new RuntimeException("Setting Look and Feel Failed");
            }
        }

    }
}
