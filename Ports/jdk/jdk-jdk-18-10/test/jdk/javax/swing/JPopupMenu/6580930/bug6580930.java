/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6580930 7184956
 * @summary Swing Popups should overlap taskbar
 * @author Alexander Potochkin
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug6580930
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class bug6580930 {
    private static ExtendedRobot robot;
    private static JFrame frame;
    private static JPopupMenu popup;
    private static Toolkit toolkit;
    private static volatile boolean skipTest = false;
    private static Point loc;
    private static int y;

    private static void createGui() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);

        popup = new JPopupMenu("Menu");
        for (int i = 0; i < 7; i++) {
            popup.add(new JMenuItem("MenuItem"));
        }
        JPanel panel = new JPanel();
        panel.setComponentPopupMenu(popup);
        frame.add(panel);

        frame.setSize(200, 200);
    }


    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JPopupMenu.setDefaultLightWeightPopupEnabled(true);
                    bug6580930.createGui();
                }
            });

            toolkit = Toolkit.getDefaultToolkit();
            robot = new ExtendedRobot();
            robot.setAutoDelay(10);
            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Insets insets = toolkit.getScreenInsets(frame.getGraphicsConfiguration());
                    if (insets.bottom == 0) {
                        System.out.println("This test is only for configurations with taskbar on the bottom");

                        skipTest = true;
                    }

                    Dimension screenSize = toolkit.getScreenSize();
                    frame.setLocation(screenSize.width/2, screenSize.height - frame.getHeight() - insets.bottom + 10);
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();

            if(skipTest) {
                return;
            }

            SwingUtilities.invokeAndWait(() -> loc = frame.getLocationOnScreen());
            robot.waitForIdle();

            robot.mouseMove(loc.x, loc.y);
            showPopup();
            robot.waitForIdle();
            if (!System.getProperty("os.name").startsWith("Mac")
                && isHeavyWeightMenuVisible()) {
                throw new RuntimeException("HeavyWeightPopup is unexpectedly visible");
            }

            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);

            int x = loc.x;
            SwingUtilities.invokeAndWait( () -> y = loc.y + (frame.getHeight() -
                    popup.getPreferredSize().height) + 1);
            robot.waitForIdle();
            robot.mouseMove(x, y);

            showPopup();
            SwingUtilities.invokeAndWait(() -> loc = popup.getLocationOnScreen());
            robot.waitForIdle();

            if (!loc.equals(new Point(x, y))) {
                throw new RuntimeException("Popup is unexpectedly shifted");
            }

            if (!isHeavyWeightMenuVisible()) {
                throw new RuntimeException("HeavyWeightPopup is unexpectedly hidden");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void showPopup() {
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        if (!popup.isShowing()) {
            robot.mousePress(InputEvent.BUTTON2_MASK);
            robot.mouseRelease(InputEvent.BUTTON2_MASK);
            robot.waitForIdle();
            if (!popup.isShowing()) {
                robot.mousePress(InputEvent.BUTTON3_MASK);
                robot.mouseRelease(InputEvent.BUTTON3_MASK);
                robot.waitForIdle();
            }
        }
    }

    private static boolean isHeavyWeightMenuVisible() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.getClass().getSimpleName().equals("HeavyWeightWindow")
                    && window.isVisible()) {
                    return true;
            }
        }
        return false;
    }
}
