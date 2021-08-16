/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
   @bug 8255880
   @key headful
   @summary Swing components, whose internal state changed while a frame was
            iconified, are not redrawn after the frame becomes deiconified.
 */

import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Robot;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class RepaintOnFrameIconifiedStateChangeTest {
    private static final String[][] strsForComps = new String[][] {
        {"JLabel AAA", "JLabel BBB"},
        {"JButton AAA", "JButton BBB"}};
    private static final int lblIndex = 0;
    private static final int btnIndex = 1;

    private static volatile JFrame frame;
    private static volatile JLabel label;
    private static volatile JButton button;
    private static volatile JComponent[] comps = new JComponent[2];
    private static volatile boolean[] compRedrawn = new boolean[2];
    private static volatile boolean compRedrawnFlagCanBeSet = false;

    public static void main(String[] args) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (!toolkit.isFrameStateSupported(JFrame.ICONIFIED) ||
            !toolkit.isFrameStateSupported(JFrame.NORMAL)) {
            System.out.println("ICONIFIED or NORMAL frame states are not" +
                "supported by a toolkit.");
            return;
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Creating GUI...");
                    createGUI();
                }
            });
            Robot robot = new Robot();
            robot.delay(2000);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Minimizing the frame...");
                    frame.setExtendedState(JFrame.ICONIFIED);
                }
            });
            robot.delay(2000);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Changing states of components...");
                    label.setText(strsForComps[lblIndex][1]);
                    button.setText(strsForComps[btnIndex][1]);
                }
            });
            robot.delay(2000);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Restoring the frame...");
                    for (int i = 0; i < compRedrawn.length; i++) {
                        compRedrawn[i] = false;
                    }
                    compRedrawnFlagCanBeSet = true;

                    frame.setExtendedState(JFrame.NORMAL);
                    frame.toFront();
                }
            });
            robot.delay(2000);

            int notRedrawnCompsCount = 0;
            for (int i = 0; i < compRedrawn.length; i++) {
                if (!compRedrawn[i]) {
                    notRedrawnCompsCount++;
                    System.out.println(String.format(
                            "Not redrawn component #%d: '%s'", i, comps[i]));
                }
            }
            if (notRedrawnCompsCount > 0) {
                throw new RuntimeException(String.format(
                        "'%d' components were not redrawn.",
                        notRedrawnCompsCount));
            }
            System.out.println("Test passed.");
        } catch (InterruptedException | InvocationTargetException |
            AWTException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        if (frame != null) {
                            frame.dispose();
                            frame = null;
                        }
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void createGUI() {
        if (!(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)) {
            try {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (UnsupportedLookAndFeelException ulafe) {
                throw new RuntimeException(ulafe);
            }
        }

        frame = new JFrame("RepaintOnFrameIconifiedStateChangeTest");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Container content = frame.getContentPane();
        content.setLayout(new FlowLayout());

        comps[lblIndex] = label = new JLabel(strsForComps[lblIndex][0]) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (compRedrawnFlagCanBeSet) {
                    compRedrawn[lblIndex] = true;
                }
            }
        };
        label.setPreferredSize(new Dimension(150, 50));
        content.add(label);

        comps[btnIndex] = button = new JButton(strsForComps[btnIndex][0]) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (compRedrawnFlagCanBeSet) {
                    compRedrawn[btnIndex] = true;
                }
            }
        };
        button.setPreferredSize(new Dimension(200, 50));
        button.setFocusable(false);
        content.add(button);

        frame.pack();
        frame.setVisible(true);
    }
}
