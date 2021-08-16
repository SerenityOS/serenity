/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204931 8227392 8224825 8233910
 * @summary test alpha colors are blended with background.
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Robot;
import javax.swing.SwingUtilities;

public class AlphaColorTest extends Component {

    private Color color;

    public AlphaColorTest(Color c) {
       this.color = c;
    }

    public void paint(Graphics g) {
        g.setColor(color);
        g.fillRect(0, 0, getSize().width, getSize().height);
    }

    public Dimension getPreferredSize() {
        return getSize();
    }

    public Dimension getSize() {
        return new Dimension(200, 200);
    }

    public static void main(String args[]) throws Exception {
        SwingUtilities.invokeAndWait(() -> createAndShowGUI());
        Robot robot = new Robot();
        robot.delay(5000);
        robot.waitForIdle();
        Color c = robot.getPixelColor(frame.getX() + 100, frame.getY() + 100);
        int red = c.getRed();
        frame.dispose();
        // Should be 126-128, but be tolerant of gamma correction.
        if (red < 122 || red > 132) {
            throw new RuntimeException("Color is not as expected. Got " + c);
        }
     }

    static Frame frame;
    private static void createAndShowGUI() {
        frame = new Frame("Alpha Color Test") {
            @Override
            public void paint(Graphics g) {
                g.setColor(Color.black);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paint(g);
            }
        };
        Color color = new Color(255, 255, 255, 127);
        frame.add("Center", new AlphaColorTest(color));
        frame.setUndecorated(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
