/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072900
 * @summary Mouse events are captured by the wrong menu in OS X
 * @author Anton Nashatyrev
 * @run main WrongSelectionOnMouseOver
 */

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static javax.swing.UIManager.getInstalledLookAndFeels;

public class WrongSelectionOnMouseOver implements Runnable {

    CountDownLatch firstMenuSelected = new CountDownLatch(1);
    CountDownLatch secondMenuMouseEntered = new CountDownLatch(1);
    CountDownLatch secondMenuSelected = new CountDownLatch(1);

    JMenu m1, m2;

    private UIManager.LookAndFeelInfo laf;
    JFrame frame1;
    JFrame frame2;
    private Point menu1location;
    private Point menu2location;

    public WrongSelectionOnMouseOver(UIManager.LookAndFeelInfo laf) throws Exception {
        this.laf = laf;
    }

    private void createUI() throws Exception {
        System.out.println("Testing UI: " + laf);
        UIManager.setLookAndFeel(laf.getClassName());

        {
            frame1 = new JFrame("Frame1");
            JMenuBar mb = new JMenuBar();
            m1 = new JMenu("File");
            JMenuItem i1 = new JMenuItem("Save");
            JMenuItem i2 = new JMenuItem("Load");

            m1.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    firstMenuSelected.countDown();
                    System.out.println("Menu1: menuSelected");
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                    System.out.println("Menu1: menuDeselected");
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                    System.out.println("Menu1: menuCanceled");
                }
            });

            frame1.setJMenuBar(mb);
            mb.add(m1);
            m1.add(i1);
            m1.add(i2);

            frame1.setLayout(new FlowLayout());
            frame1.setBounds(200, 200, 200, 200);

            frame1.setVisible(true);
        }

        {
            frame2 = new JFrame("Frame2");
            JMenuBar mb = new JMenuBar();
            m2 = new JMenu("File");
            JMenuItem i1 = new JMenuItem("Save");
            JMenuItem i2 = new JMenuItem("Load");

            m2.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    secondMenuMouseEntered.countDown();
                    System.out.println("WrongSelectionOnMouseOver.mouseEntered");
                }
            });

            m2.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    secondMenuSelected.countDown();
                    System.out.println("Menu2: menuSelected");
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                    System.out.println("Menu2: menuDeselected");
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                    System.out.println("Menu2: menuCanceled");
                }
            });

            frame2.setJMenuBar(mb);
            mb.add(m2);
            m2.add(i1);
            m2.add(i2);

            frame2.setLayout(new FlowLayout());
            frame2.setBounds(450, 200, 200, 200);

            frame2.setVisible(true);
        }
    }

    public void disposeUI() {
        frame1.dispose();
        frame2.dispose();
    }

    @Override
    public void run() {
        try {
            if (frame1 == null) {
                createUI();
            } else {
                disposeUI();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void test() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(100);

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            menu1location = m1.getLocationOnScreen();
            menu2location = m2.getLocationOnScreen();
        });

        robot.mouseMove((int) menu1location.getX() + 5,
                (int) menu1location.getY() + 5);
        robot.mousePress(MouseEvent.BUTTON1_MASK);
        robot.mouseRelease(MouseEvent.BUTTON1_MASK);

        if (!firstMenuSelected.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Menu has not been selected.");
        };

        robot.mouseMove((int) menu2location.getX() + 5,
                (int) menu2location.getY() + 5);

        if (!secondMenuMouseEntered.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("MouseEntered event missed for the second menu");
        };

        if (secondMenuSelected.await(1, TimeUnit.SECONDS)) {
            throw new RuntimeException("The second menu has been selected");
        };
    }

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            WrongSelectionOnMouseOver test = new WrongSelectionOnMouseOver(laf);
            SwingUtilities.invokeAndWait(test);
            test.test();
            SwingUtilities.invokeAndWait(test);
        }
        System.out.println("Test passed");
    }
}
