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
/*
 * @test
 * @key headful
 * @bug 4870644 7190539
 * @summary  Default button responds to CTRL-ENTER while popup menu is active.
 * @run main bug4870644
 */
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


public class bug4870644 {
    JButton b1, b2, b3;
    JFrame frame;
    JMenu menu;
    JPopupMenu popup;
    static Robot robot;
    static boolean passed = true;
    private volatile Point p = null;
    private volatile Dimension d = null;
    void blockTillDisplayed(JComponent comp) throws Exception {
        while (p == null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    p = comp.getLocationOnScreen();
                    d = comp.getSize();
                });
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            try {
                SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
                System.out.println("Test for LookAndFeel " + laf.getClassName());
                new bug4870644();
                System.out.println("Test passed for LookAndFeel " + laf.getClassName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public bug4870644() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            JMenuBar menuBar = new JMenuBar();
            menu = new JMenu("Menu");
            menuBar.add(menu);
            JMenuItem menuItem = new JMenuItem("Item");
            menu.add(menuItem);
            menu.add(new JMenuItem("Item 2"));
            frame = new JFrame("test");
            frame.setJMenuBar(menuBar);

            b1 = new JButton("One");
            b2 = new JButton("Two");
            b3 = new JButton("Default");
            b3.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    passed = false;
                }
            });
            frame.getContentPane().add(b1, BorderLayout.NORTH);
            frame.getContentPane().add(b2, BorderLayout.CENTER);
            frame.getContentPane().add(b3, BorderLayout.SOUTH);
            frame.getRootPane().setDefaultButton(b3);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });

        blockTillDisplayed(b1);
        robot.waitForIdle();

        robot.delay(500);
        robot.mouseMove(p.x + d.width-1, p.y + d.height/2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_F10);
        robot.keyRelease(KeyEvent.VK_F10);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            popup = menu.getPopupMenu();
        });

        blockTillDisplayed(popup);
        robot.waitForIdle();
        robot.mouseMove(p.x + d.width-1, p.y + d.height/2);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> frame.dispose());
        if(!passed) {
            String cause = "Default button reacted on \"ctrl ENTER\" while menu is active.";
            throw new RuntimeException(cause);
        }
    }
}
