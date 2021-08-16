/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.*;
import java.awt.event.*;

/**
 * @test
 * @key headful
 * @bug 7161568
 * @author Alexander Scherbatiy
 * @summary Tests that navigating tabs in the JTAbbedPane does not throw NPE
 * @run main bug7161568
 */
public class bug7161568 {

    private static final int N = 50;
    private static JTabbedPane tabbedPane;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        UIManager.put("TabbedPane.selectionFollowsFocus", Boolean.FALSE);

        try {
            Robot robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    createAndShowUI();
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    tabbedPane.requestFocus();
                }
            });

            robot.waitForIdle();

            for (int i = 0; i < N; i++) {
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_LEFT);
                robot.waitForIdle();
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static void createAndShowUI() {
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(100, 100);

        tabbedPane = new JTabbedPane();

        for (int i = 0; i < N; i++) {
            tabbedPane.addTab("Tab: " + i, new JLabel("Test"));
        }

        tabbedPane.setSelectedIndex(0);

        frame.getContentPane().add(tabbedPane);
        frame.setVisible(true);
    }
}
