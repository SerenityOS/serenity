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

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;

/*
 * @test
 * @key headful
 * @bug 8172269 8244557
 * @summary Tests JTabbedPane background for SCROLL_TAB_LAYOUT
 * @run main TestBackgroundScrollPolicy
 */

public class TestBackgroundScrollPolicy {
    private static Robot ROBOT;

    public static void main(String[] args) throws Exception {
        ROBOT = new Robot();
        ROBOT.setAutoWaitForIdle(true);
        ROBOT.setAutoDelay(100);
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            System.out.println("Testing L&F: " + laf.getClassName());
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            try {
                SwingUtilities.invokeAndWait(() -> createGUI());
                ROBOT.waitForIdle();
                ROBOT.delay(1000);
                SwingUtilities.invokeAndWait(() -> test(laf));
                ROBOT.delay(2000);
            } finally {
                if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
            ROBOT.delay(1000);
        }
    }

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored) {
            System.out.println("Unsupported L&F: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addOpaqueError(UIManager.LookAndFeelInfo laf, boolean opaque) {
        throw new RuntimeException(laf.getClassName() + " background color wrong for opaque=" + opaque);
    }

    private static JFrame frame;
    private static JTabbedPane pane;

    public static void createGUI() {
        pane = new JTabbedPane();
        pane.setOpaque(true);
        pane.setBackground(Color.RED);
        for (int i = 0; i < 3; i++) {
            pane.addTab("Tab " + i, new JLabel("Content area " + i));
        }
        pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        pane.repaint();
        frame = new JFrame();
        frame.getContentPane().setBackground(Color.BLUE);
        frame.add(pane);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
    }

    public static void test(UIManager.LookAndFeelInfo laf) {
        Point point = new Point(pane.getWidth() - 2, 2);
        SwingUtilities.convertPointToScreen(point, pane);
        Color actual = ROBOT.getPixelColor(point.x, point.y);

        boolean opaque = pane.isOpaque();
        Color expected = opaque
                ? pane.getBackground()
                : frame.getContentPane().getBackground();

        if (!expected.equals(actual)){
            System.out.println("expected " + expected + " actual " + actual);
            addOpaqueError(laf, opaque);
        }

    }
}

