/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @summary Check if a per-pixel translucent window shows up with correct translucency
 * @author mrkam
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main PerPixelTranslucentCanvas
 * @run main/othervm -Dsun.java2d.uiScale=1.5 PerPixelTranslucentCanvas
 */

public class PerPixelTranslucentCanvas extends Common {

    JPanel center;
    static Color OVAL_COLOR = Color.BLUE;

    public static void main(String[] ignored) throws Exception {
        FG_COLOR = new Color(200, 0, 0, 100);
        BG_COLOR = Color.GREEN;
        for (Class<Window> windowClass: WINDOWS_TO_TEST)
            new PerPixelTranslucentCanvas(windowClass).doTest();
    }

    public PerPixelTranslucentCanvas(Class windowClass) throws Exception {
        super(windowClass);
    }

    @Override
    public void createSwingComponents() {
        Container contentPane = RootPaneContainer.class.cast(window).getContentPane();
        BorderLayout bl = new BorderLayout(10, 10);
        contentPane.setLayout(bl);

        JLabel label = new JLabel("North", new ImageIcon(
                new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)), SwingConstants.CENTER);
        contentPane.add(label, BorderLayout.NORTH);

        JButton button = new JButton("West");
        contentPane.add(button, BorderLayout.WEST);

        center = new JPanel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(OVAL_COLOR);
                g.fillOval(0, 0, getWidth(), getHeight());
            }
        };
        contentPane.add(center, BorderLayout.CENTER);

        JTextField jTextField = new JTextField("South");
        contentPane.add(jTextField, BorderLayout.SOUTH);
    }

    @Override
    public void doTest() throws Exception {
        robot.waitForIdle(delay);

        Rectangle bounds = center.getBounds();
        Point loc = center.getLocationOnScreen();

        final int x = loc.x + bounds.width / 2;
        final int y = loc.y + bounds.height / 2;

        Color color = robot.getPixelColor(x, y);
        if (OVAL_COLOR.getRGB() != color.getRGB())
            throw new RuntimeException("bounds = " + bounds + "\n" +
                    "loc = " + loc + "\n" +
                    "background loc = " + background.getX() + ", " + background.getY() + "\n" +
                    "so middle point over background is " + (x - background.getX()) + ", " + (y - background.getY()) + "\n" +
                    "Oval is not opaque in the middle point (" + x + ", " + y + ", " + color + ")");

        color = robot.getPixelColor(loc.x - 5, loc.y - 5);
        if (FG_COLOR.getRGB() == color.getRGB())
            throw new RuntimeException("Background is not translucent (" + color + ")");

        EventQueue.invokeAndWait(this::dispose);
        robot.waitForIdle();
    }
}
