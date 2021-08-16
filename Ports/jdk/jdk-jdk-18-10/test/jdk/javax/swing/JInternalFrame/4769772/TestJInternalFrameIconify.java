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

/*
 * @test
 * @key headful
 * @bug 4769772
 * @summary JInternalFrame.setIcon(true) before JDesktopPane.add(JIF) causes wrong state
 * @run main TestJInternalFrameIconify
 */
import java.beans.PropertyVetoException;
import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Robot;
import javax.swing.SwingUtilities;

public class TestJInternalFrameIconify {

    private static JDesktopPane desktopPane;
    private static JFrame frame;
    private static Robot robot;
    private static volatile String errorMessage = "";

    public static void main(String[] args) throws Exception {
        robot = new java.awt.Robot();
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            String lookAndFeelString = lookAndFeelItem.getClassName();
            if (tryLookAndFeel(lookAndFeelString)) {
                createUI(lookAndFeelString);
                robot.waitForIdle();
                executeTest(lookAndFeelString);
            }
        }
        if (!"".equals(errorMessage)) {
            throw new RuntimeException(errorMessage);
        }
    }

    private static boolean tryLookAndFeel(String lookAndFeelString) {
        try {
            UIManager.setLookAndFeel(lookAndFeelString);
            return true;
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                InstantiationException | IllegalAccessException e) {
            errorMessage += e.getMessage() + "\n";
            System.err.println("Caught Exception: " + e.getMessage());
            return false;
        }
    }

    private static void createUI(String lookAndFeelString) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame(lookAndFeelString);
                desktopPane = new JDesktopPane();
                frame.getContentPane().add(desktopPane);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                JInternalFrame f = new JInternalFrame("Child ", true, true,
                        true, true);
                f.setSize(200, 300);
                f.setLocation(20, 20);
                try {
                    f.setIcon(true);
                } catch (PropertyVetoException ex) {
                    errorMessage += ex.getMessage() + "\n";
                }
                desktopPane.add(f);
                f.setVisible(true);

                frame.setSize(500, 500);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });

    }

    private static void executeTest(String lookAndFeelString) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    JInternalFrame internalFrames[]
                            = desktopPane.getAllFrames();
                    if (internalFrames[0].isShowing()) {
                        errorMessage += "Test Failed for "
                                + lookAndFeelString + " look and feel\n";
                        System.err.println(errorMessage);
                    }
                } finally {
                    frame.dispose();
                }
            }
        });
    }
}
