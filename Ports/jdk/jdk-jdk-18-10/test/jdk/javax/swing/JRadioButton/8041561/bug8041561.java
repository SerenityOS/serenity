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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * @test
 * @key headful
 * @bug 8041561
 * @author Alexander Scherbatiy
 * @summary Inconsistent opacity behaviour between JCheckBox and JRadioButton
 * @run main bug8041561
 */
public class bug8041561 {

    private static JRadioButton radioButton;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
                        UIManager.setLookAndFeel(new MetalLookAndFeel());
                        createAndShowGUI();
                    } catch (UnsupportedLookAndFeelException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            new Robot().waitForIdle();
            Thread.sleep(500);

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        Point point = radioButton.getLocationOnScreen();
                        int x = (int) point.getX() + radioButton.getWidth() / 2;
                        int y = (int) point.getY() + radioButton.getHeight() / 2;

                        Robot robot = new Robot();
                        Color color = robot.getPixelColor(x, y);
                        if (!Color.BLUE.equals(color)) {
                            throw new RuntimeException("JRadioButton is opaque");
                        }
                    } catch (AWTException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.BLUE);
        radioButton = new JRadioButton();
        radioButton.setOpaque(false);
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLUE);
        panel.add(radioButton);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
