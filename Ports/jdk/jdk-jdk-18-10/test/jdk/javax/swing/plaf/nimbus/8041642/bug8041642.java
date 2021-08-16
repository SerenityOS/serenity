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
 * @bug 8041642 8079450 8140237
 * @summary Incorrect paint of JProgressBar in Nimbus LF
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import java.awt.*;

public class bug8041642 {

    private static JFrame frame;
    private static Point point;
    private static JProgressBar bar;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    frame.setUndecorated(true);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    setup(frame);
                }
            });
            final Robot robot = new Robot();
            robot.delay(300);
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    point = bar.getLocationOnScreen();
                }
            });
            Color color = robot.getPixelColor(point.x + 1, point.y + 7);
            System.out.println(color);
            if (color.getGreen() < 150 || color.getBlue() > 30 ||
                    color.getRed() > 200) {
                throw new RuntimeException("Bar padding color should be green");
            }

        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.dispose();
                }
            });
        }

        System.out.println("ok");
    }

    static void setup(JFrame frame) {
        bar = new JProgressBar();
        bar.setBackground(Color.WHITE);
        bar.setValue(2);
        frame.getContentPane().add(bar, BorderLayout.NORTH);
        frame.getContentPane().setBackground(Color.GREEN);
        frame.setSize(200, 150);
        frame.setLocation(100, 100);
        frame.setVisible(true);
    }
}
