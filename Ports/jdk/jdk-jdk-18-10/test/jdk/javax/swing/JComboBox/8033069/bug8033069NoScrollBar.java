/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

/*
 * @test
 * @key headful
 * @bug 8033069
 * @summary Checks that JComboBox popup does not close when mouse wheel is
 *          rotated over combo box and over its popup. The case where popup
 *          has no scroll bar.
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug8033069NoScrollBar
 */
public class bug8033069NoScrollBar {

    private static final String[] NO_SCROLL_ITEMS = new String[] {
        "A", "B", "C", "D", "E", "F"
    };

    private final Robot robot;

    private final String[] items;

    private JFrame frame;
    private JComboBox cb1;
    private JComboBox cb2;

    private volatile Point p;
    private volatile Dimension d;

    public static void main(String[] args) throws Exception {
        iterateLookAndFeels(new bug8033069NoScrollBar(NO_SCROLL_ITEMS));
    }

    protected static void iterateLookAndFeels(final bug8033069NoScrollBar test) throws Exception {
        LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        for (LookAndFeelInfo info : lafInfo) {
            try {
                UIManager.setLookAndFeel(info.getClassName());
                System.out.println("Look and Feel: " + info.getClassName());
                test.runTest();
            } catch (UnsupportedLookAndFeelException e) {
                System.out.println("Skipping unsupported LaF: " + info);
            }
        }
    }

    public bug8033069NoScrollBar(String[] items) throws AWTException {
        this.items = items;

        robot = new Robot();
        robot.setAutoDelay(200);
    }

    private void setupUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cb1 = new JComboBox<>(items);
        cb2 = new JComboBox<>(items);
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(cb1);
        panel.add(cb2);

        frame.add(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void disposeUI() {
        if (frame != null) {
            frame.dispose();
        }
    }

    public void runTest() throws Exception {
        try {
            SwingUtilities.invokeAndWait(this::setupUI);

            robot.waitForIdle();
            assertFalse("cb1 popup is visible",
                        Util.invokeOnEDT(cb1::isPopupVisible));

            // Move mouse pointer to the center of the fist combo box
            SwingUtilities.invokeAndWait(() -> {
                p = cb1.getLocationOnScreen();
                d = cb1.getSize();
            });

            robot.mouseMove(p.x + d.width / 2, p.y + d.height / 2);
            // Click it to open popup
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            robot.waitForIdle();
            assertTrue("cb1 popup is not visible",
                       Util.invokeOnEDT(cb1::isPopupVisible));

            robot.mouseWheel(1);
            robot.waitForIdle();
            assertTrue("cb1 popup is not visible after mouse wheel up on combo box",
                       Util.invokeOnEDT(cb1::isPopupVisible));

            robot.mouseWheel(-1);
            robot.waitForIdle();
            assertTrue("cb1 popup is not visible after mouse wheel down on combo box",
                       Util.invokeOnEDT(cb1::isPopupVisible));

            // Move mouse down on the popup
            robot.mouseMove(p.x + d.width / 2, p.y + d.height * 3);

            robot.mouseWheel(1);
            robot.waitForIdle();
            assertTrue("cb1 popup is not visible after mouse wheel up on popup",
                       Util.invokeOnEDT(cb1::isPopupVisible));

            robot.mouseWheel(-1);
            robot.waitForIdle();
            assertTrue("cb1 popup is not visible after mouse wheel down on popup",
                       Util.invokeOnEDT(cb1::isPopupVisible));


            // Move mouse pointer to the center of the second combo box
            SwingUtilities.invokeAndWait(() -> {
                p = cb2.getLocationOnScreen();
                d = cb2.getSize();
            });

            robot.mouseMove(p.x + d.width / 2, p.y + d.height / 2);

            robot.mouseWheel(1);
            robot.waitForIdle();
            assertFalse("cb1 popup is visible after mouse wheel up on cb2",
                        Util.invokeOnEDT(cb1::isPopupVisible));
        } finally {
            SwingUtilities.invokeAndWait(this::disposeUI);
        }

        System.out.println("Test passed");
    }

    private static void assertTrue(String message, boolean value) {
        assertEquals(message, true, value);
    }

    private static void assertFalse(String message, boolean value) {
        assertEquals(message, false, value);
    }

    private static void assertEquals(String message, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new RuntimeException(message);
        }
    }
}
