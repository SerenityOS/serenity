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

/*
 * @test
 * @key headful
 * @bug 4523758
 * @summary Directly check that torn-off combo works
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug4523758
 */
/*
 * There is another regression test for this bug created by ssi with a
 * fix. It is purely AWT and designed to verify the (absence of) underlying Component issue.
 * This functional test does test, well, functionality of the swing control.
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class bug4523758 {

    private static JFrame frame;
    private JToolBar tools;
    private JComboBox combo;

    private boolean passed = true;
    private boolean itemStateChanged = false;
    private Object itemLock = new Object();

    private static int delay = 500;
    private static final int WAIT_EVENT_DELAY = 60000;

    public bug4523758() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    initializeGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize GUI");
        }
    }

    private void initializeGUI() {
        frame = new JFrame("bug4523758");
        tools = new JToolBar();
        frame.getContentPane().add(tools, BorderLayout.NORTH);
        combo = new JComboBox(new Object[] { "Red", "Orange", "Yellow",
            "Green", "Blue", "Indigo", "Violet"});
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                itemStateChanged = true;
                synchronized (itemLock) {
                    try {
                        itemLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }
        });
        tools.add(combo);
        frame.setSize(250,400);
        frame.setLocation(700, 0);
        frame.setVisible(true);
    }

    private void doTest() throws Exception {
        ExtendedRobot robot = new ExtendedRobot();
        robot.waitForIdle(1000);

        final Point cl = combo.getLocationOnScreen();
        final Dimension cs = combo.getSize();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        robot.waitForIdle(delay);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize((int) cl.x - 700 + cs.width,
                              (int) cl.y + cs.height - 5);
                frame.setVisible(true);
            }
        });

        robot.waitForIdle(delay*2);
        Point comboLocation = combo.getLocationOnScreen();
        Dimension comboSize = combo.getSize();

        robot.mouseMove((int) comboLocation.x + comboSize.width / 2,
                        (int) comboLocation.y + 5);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(100);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);

        robot.mouseMove((int) comboLocation.x + comboSize.width / 2,
                        (int) comboLocation.y + 60);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(100);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);

        if (! itemStateChanged) {
            synchronized (itemLock) {
                try {
                    itemLock.wait(WAIT_EVENT_DELAY);
                } catch (Exception e) {
                }
            }
        }
        if (! itemStateChanged) {
            System.err.println("FAIL: ItemEvent not triggered when mouse clicked on combo box drop down");
            passed = false;
        }

        if (! passed) {
            System.err.println("Test failed!");
            captureScreenAndSave();
            throw new RuntimeException("FAIL");
        } else {
            System.out.println("Test passed!");
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            bug4523758 test = new bug4523758();
            test.doTest();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("FAIL");
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    /**
     * Do screen capture and save it as image
     */
    private static void captureScreenAndSave() {

        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle rectangle = new Rectangle(0, 0, screenSize.width, screenSize.height);
            System.out.println("About to screen capture - " + rectangle);
            java.awt.image.BufferedImage image = robot.createScreenCapture(rectangle);
            javax.imageio.ImageIO.write(image, "jpg", new java.io.File("ScreenImage.jpg"));
            robot.delay(3000);
        } catch (Throwable t) {
            System.out.println("WARNING: Exception thrown while screen capture!");
            t.printStackTrace();
        }
    }
}
