/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159168 8161913
 * @summary [hidpi] Window.setShape() works incorrectly on HiDPI
 * @run main/othervm -Dsun.java2d.uiScale=2 SetShapeTest
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.Robot;
import javax.swing.SwingUtilities;

public class SetShapeTest {

    private static Window window;
    private static Frame background;

    public static void main(String[] args) throws Exception {
        createUI();
        Robot robot = new Robot();
        robot.waitForIdle();
        Rectangle rect = window.getBounds();
        rect.x += rect.width - 10;
        rect.y += rect.height - 10;
        robot.delay(1000);
        Color c = robot.getPixelColor(rect.x, rect.y);
        try {
            if (!c.equals(Color.RED)) {
                throw new RuntimeException("Test Failed");
            }
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    background.dispose();
                    window.dispose();
                }
            });
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                background = new Frame();
                background.setUndecorated(true);
                background.setBackground(Color.blue);
                background.setSize(300, 300);
                background.setLocation(100, 100);
                background.setVisible(true);
                window = new Window(background);
                window.setBackground(Color.red);
                window.add(new Panel(), BorderLayout.CENTER);
                window.setLocation(200, 200);
                window.setSize(100, 100);
                Area a = new Area();
                a.add(new Area(new Rectangle2D.Double(0, 0, 100, 100)));
                window.setShape(a);
                window.setVisible(true);
                window.toFront();
            }
        });
    }
}
