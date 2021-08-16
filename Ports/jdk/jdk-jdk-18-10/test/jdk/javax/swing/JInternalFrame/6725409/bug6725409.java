/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @key headful
 * @bug 6725409
 * @requires (os.family == "windows")
 * @summary Checks that JInternalFrame's system menu
 *          can be localized during run-time
 * @author Mikhail Lapshin
 * @library /lib/client/
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 * @build ExtendedRobot
 * @run main bug6725409
 */

import javax.swing.*;
import java.awt.*;

public class bug6725409 {
    private JFrame frame;
    private JInternalFrame iFrame;
    private TestTitlePane testTitlePane;
    private boolean passed;
    private static ExtendedRobot robot = createRobot();

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(
                    new com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel());
        } catch(UnsupportedLookAndFeelException e) {
            System.out.println("The test is for Windows LaF only");
            return;
        }

        final bug6725409 bug6725409 = new bug6725409();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bug6725409.setupUIStep1();
                }
            });
            sync();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bug6725409.setupUIStep2();
                }
            });
            sync();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bug6725409.test();
                }
            });
            sync();
            bug6725409.checkResult();
        } finally {
            if (bug6725409.frame != null) {
                bug6725409.frame.dispose();
            }
        }
    }

    private void setupUIStep1() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JDesktopPane desktop = new JDesktopPane();
        iFrame = new JInternalFrame("Internal Frame", true, true, true, true);
        iFrame.setSize(200, 100);
        desktop.add(iFrame);
        frame.add(desktop);
        iFrame.setVisible(true);

        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupUIStep2() {
        UIManager.put("InternalFrameTitlePane.restoreButtonText",
                "CUSTOM.restoreButtonText");
        UIManager.put("InternalFrameTitlePane.moveButtonText",
                "CUSTOM.moveButtonText");
        UIManager.put("InternalFrameTitlePane.sizeButtonText",
                "CUSTOM.sizeButtonText");
        UIManager.put("InternalFrameTitlePane.minimizeButtonText",
                "CUSTOM.minimizeButtonText");
        UIManager.put("InternalFrameTitlePane.maximizeButtonText",
                "CUSTOM.maximizeButtonText");
        UIManager.put("InternalFrameTitlePane.closeButtonText",
                "CUSTOM.closeButtonText");
        SwingUtilities.updateComponentTreeUI(frame);
    }

    // The test depends on the order of the menu items in
    // WindowsInternalFrameTitlePane.systemPopupMenu
    private void test() {
        testTitlePane = new TestTitlePane(iFrame);
        passed = true;
        checkMenuItemText(0, "CUSTOM.restoreButtonText");
        checkMenuItemText(1, "CUSTOM.moveButtonText");
        checkMenuItemText(2, "CUSTOM.sizeButtonText");
        checkMenuItemText(3, "CUSTOM.minimizeButtonText");
        checkMenuItemText(4, "CUSTOM.maximizeButtonText");
        // Skip separator
        checkMenuItemText(6, "CUSTOM.closeButtonText");
    }

    private void checkMenuItemText(int index, String text) {
        JMenuItem menuItem = (JMenuItem)
                testTitlePane.getSystemPopupMenu().getComponent(index);
        if (!text.equals(menuItem.getText())) {
            passed = false;
        }
    }

    private void checkResult() {
        if (passed) {
            System.out.println("Test passed");
        } else {
            throw new RuntimeException("Unable to localize " +
                    "JInternalFrame's system menu during run-time");
        }
    }

    private static void sync() {
        robot.waitForIdle();
    }
    private static ExtendedRobot createRobot() {
        try {
             ExtendedRobot robot = new ExtendedRobot();
             return robot;
         }catch(Exception ex) {
             ex.printStackTrace();
             throw new Error("Unexpected Failure");
         }
    }

    // Extend WindowsInternalFrameTitlePane to get access to systemPopupMenu
    private class TestTitlePane extends
            com.sun.java.swing.plaf.windows.WindowsInternalFrameTitlePane {
        private JPopupMenu systemPopupMenu;

        public TestTitlePane(JInternalFrame f) {
            super(f);
        }

        public JPopupMenu getSystemPopupMenu() {
            return systemPopupMenu;
        }

        protected void addSystemMenuItems(JPopupMenu menu) {
            super.addSystemMenuItems(menu);
            systemPopupMenu = menu;
        }
    }
}
