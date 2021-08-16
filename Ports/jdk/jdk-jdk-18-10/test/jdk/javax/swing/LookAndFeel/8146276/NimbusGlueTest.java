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
 * @test
 * @key headful
 * @bug 8146276
 * @summary Right aligned toolbar component does not appear
 * @run main NimbusGlueTest
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Robot;
import javax.swing.UnsupportedLookAndFeelException;

public class NimbusGlueTest {

    private static JFrame frame;
    private static Robot robot;
    private static volatile String errorMessage = "";
    private static JToolBar bar;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            String lookAndFeelString = lookAndFeelItem.getClassName();
            if (tryLookAndFeel(lookAndFeelString)) {
                createUI();
                performTest();
                robot.waitForIdle();
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

    private static void performTest() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    int width = 0;
                    for (Component comp : bar.getComponents()) {
                        width += comp.getWidth();
                    }
                    if (width > 600) {
                        errorMessage = "Test Failed";
                    }
                } finally {
                    frame.dispose();
                }

            }
        });
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();
                bar = new JToolBar();
                bar.add(createButton(1));
                bar.add(createButton(2));
                bar.add(Box.createHorizontalGlue());
                bar.add(createButton(3));
                frame.add(bar, BorderLayout.NORTH);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(600, 400);
                frame.setVisible(true);
            }
        });
    }

    private static JButton createButton(int id) {
        JButton b = new JButton("B: " + id);
        b.setPreferredSize(new Dimension(60, b.getPreferredSize().height));
        return b;
    }
}
