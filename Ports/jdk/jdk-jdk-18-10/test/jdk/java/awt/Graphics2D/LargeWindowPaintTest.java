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

/*
 * @test
 * @bug 8240654
 * @summary Test painting a large window works
 * @key headful
 * @requires (os.family == "windows")
 * @requires vm.gc.Z
 * @run main/othervm -Dsun.java2d.uiScale=1 LargeWindowPaintTest
 * @run main/othervm -Dsun.java2d.uiScale=1 -Dsun.java2d.d3d=false LargeWindowPaintTest
 * @run main/othervm -XX:+UseZGC -Dsun.java2d.uiScale=1 LargeWindowPaintTest
 * @run main/othervm -XX:+UseZGC -Dsun.java2d.uiScale=1 -Dsun.java2d.d3d=false LargeWindowPaintTest
 */

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class LargeWindowPaintTest extends JPanel {

    static volatile JFrame frame = null;
    static volatile LargeWindowPaintTest comp = null;
    static Color color = Color.red;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("Large Window Paint Test");
            frame.add(comp = new LargeWindowPaintTest());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });

        Thread.sleep(2000);
        Robot robot = new Robot();
        robot.setAutoDelay(500);
        robot.waitForIdle();
        Rectangle r = comp.getBounds();
        System.out.println("Component bounds = " + r);
        Color c = robot.getPixelColor((int)r.getWidth()-100, (int)r.getHeight()-100);

        SwingUtilities.invokeAndWait(() -> frame.dispose());

        if (!c.equals(color)) {
            throw new RuntimeException("Color was " + c + " expected " + color);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(color);
        g.fillRect(0, 0, getSize().width, getSize().height);
    };
}
