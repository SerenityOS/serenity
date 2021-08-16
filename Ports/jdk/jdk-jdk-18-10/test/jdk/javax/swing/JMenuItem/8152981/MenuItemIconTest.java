/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8152981 8159135
 * @summary Double icons with JMenuItem setHorizontalTextPosition on Win 10
 * @requires (os.family == "windows")
 * @run main MenuItemIconTest
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MenuItemIconTest {

    private static JFrame frame;
    private static Robot robot;
    private static String errorMessage = "";
    private static JMenuItem menuItem;
    private static final int IMAGE_WIDTH_AND_HEIGHT = 25;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        String name = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(name);
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException e) {
            throw new RuntimeException("Test Failed");
        }
        createUI();
        robot.waitForIdle();
        executeTest();
        if (!"".equals(errorMessage)) {
            throw new RuntimeException(errorMessage);
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            frame.setTitle("Test");
            JMenuBar menuBar = new JMenuBar();
            ImageIcon icon = createIcon();
            menuItem = new JMenuItem("Command", icon);
            menuItem.setHorizontalTextPosition(SwingConstants.LEFT);
            menuBar.add(menuItem);
            frame.setJMenuBar(menuBar);
            frame.setPreferredSize(new Dimension(500, 500));
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        });
    }

    private static void checkPixeclColor(int x, int y) {
        robot.delay(2000);
        robot.mouseMove(x, y);
        Color c = robot.getPixelColor(x, y);
        if (Color.RED.equals(c)) {
            errorMessage = "Test Failed";
        }
        robot.delay(5000);
        frame.dispose();
    }

    protected static ImageIcon createIcon() {
        BufferedImage bi = new BufferedImage(IMAGE_WIDTH_AND_HEIGHT,
                IMAGE_WIDTH_AND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        g.setColor(Color.RED);
        g.fillOval(0, 0, IMAGE_WIDTH_AND_HEIGHT, IMAGE_WIDTH_AND_HEIGHT);
        return new ImageIcon(bi);
    }

    private static void executeTest() throws Exception {
        Point point = menuItem.getLocationOnScreen();
        checkPixeclColor(point.x + IMAGE_WIDTH_AND_HEIGHT / 2,
                point.y + IMAGE_WIDTH_AND_HEIGHT / 2);
    }
}

