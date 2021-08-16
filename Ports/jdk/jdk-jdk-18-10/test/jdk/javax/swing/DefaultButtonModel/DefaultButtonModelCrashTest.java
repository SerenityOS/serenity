/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8182577
 * @summary  Verifies if moving focus via custom ButtonModel causes crash
 * @key headful
 * @run main DefaultButtonModelCrashTest
 */

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class DefaultButtonModelCrashTest {
    private JFrame frame = null;
    private JPanel panel;
    private volatile Point p = null;

    public static void main(String[] args) throws Exception {
        new DefaultButtonModelCrashTest();
    }

    public DefaultButtonModelCrashTest() throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(200);
            SwingUtilities.invokeAndWait(() -> go());
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.delay(100);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
        } finally {
            if (frame != null) { SwingUtilities.invokeAndWait(()->frame.dispose()); }
        }
    }

    private void go() {

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();
        ButtonModel model = new DefaultButtonModel();

        JCheckBox check = new JCheckBox("a bit broken");
        check.setModel(model);
        panel = new JPanel(new BorderLayout());
        panel.add(new JTextField("Press Tab (twice?)"), BorderLayout.NORTH);
        panel.add(check);
        contentPane.add(panel);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
    }
}
