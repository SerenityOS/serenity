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
 * @bug 8139213
 * @summary Mac OS Aqua X LAF: JOptionPane truncates the first button
 * @run main OptionPaneTest
 */
import java.awt.Component;
import java.awt.Insets;
import java.awt.Robot;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class OptionPaneTest {

    private volatile static boolean testFailed;
    private static JDialog dialog;
    private static Robot robot;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    JOptionPane optionPane = new JOptionPane("JOptionPane",
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.DEFAULT_OPTION,
                            null,
                            new String[]{"3", "2", "1"},
                            null);
                    dialog = optionPane.createDialog("JOptionPane");
                    int width = 0;
                    Component[] comps = optionPane.getComponents();
                    for (Component comp : comps) {
                        if (comp instanceof JPanel) {
                            Component[] child = ((JPanel) comp).getComponents();
                            for (Component c : child) {
                                if (c instanceof JButton) {
                                    width += c.getWidth();
                                }
                            }
                        }
                    }
                    Insets in = optionPane.getInsets();
                    width += in.left + in.right;
                    if (width > optionPane.getWidth()) {
                        testFailed = true;
                    }
                } finally {
                    dialog.dispose();
                }
            }
        });
        robot.waitForIdle();
        if (testFailed) {
            throw new RuntimeException("Test Failed");
        }
    }
}
