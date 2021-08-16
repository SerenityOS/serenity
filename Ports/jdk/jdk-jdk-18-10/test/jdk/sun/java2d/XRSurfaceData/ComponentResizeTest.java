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

import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * @test
 * @bug 8039345
 * @author Prasanta Sadhukhan
 * @run main/manual ComponentResizeTest
 * @summary Resizes JFrame so that component drawn inside it gets repainted
 * without leaving any trails
 */
public class ComponentResizeTest {

    private static JFrame demoFrame;

    public static void testresize() throws Exception {
        Thread.sleep(5000);
        for (int i = 0; i < 20; i++) {
            SwingUtilities.invokeLater(() -> {
                demoFrame.setSize(demoFrame.getWidth() + 5, demoFrame.getHeight() + 5);
            });
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showMessageDialog(
                    (Component) null,
                    "The test creates a transparent JFrame and resizes the JFrame. Please verify JFrame is transparent and components (like JButton, checkbox) move without leaving any trails",
                    "information", JOptionPane.INFORMATION_MESSAGE);
            createAndShowGUI();
        });

        try {
            testresize();
        } finally {
            SwingUtilities.invokeLater(() -> {
                demoFrame.dispose();
            });
        }

        SwingUtilities.invokeAndWait(() -> {
            int confirm = JOptionPane.showConfirmDialog(
                    (Component) null,
                    "Did the component resize work without leaving any trails?",
                    "alert", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("Test passed");
            } else {
                System.out.println("Test failed");
                throw new RuntimeException("Component resize leaves trail");
            }
        });
    }

    private static void createAndShowGUI() {
        demoFrame = new JFrame();
        demoFrame.setSize(300, 300);
        demoFrame.setLayout(new FlowLayout());
        demoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        demoFrame.setUndecorated(true);
        demoFrame.setBackground(new Color(0f, 0, 0, 0.1f));
        JCheckBox b = new JCheckBox("Whatever");
        demoFrame.paintAll(null);
        b.setOpaque(true);
        demoFrame.add(b);
        demoFrame.add(new JButton());
        demoFrame.setVisible(true);
    }
}
