/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234386
 * @requires (os.family == "mac")
 * @summary [macos] NPE was thrown at expanding Choice from maximized frame
 * @run main ChoiceFromMaximizedFrame
 */

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

public class ChoiceFromMaximizedFrame
{
    static public void main(String args[]) throws Exception
    {
        Frame f = new Frame("ChoiceTest");
        try {
            Panel p =new Panel(new BorderLayout());
            Choice choice = new Choice();
            choice.addItem("aaa");
            choice.addItem("bbb");
            p.add("North",choice);
            p.add("Center",new Canvas());
            f.add(p);

            GraphicsConfiguration gc = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int x = bounds.x + insets.left;
            int y = bounds.y + insets.top;
            int width = bounds.width - insets.left - insets.right;
            int height = bounds.height - insets.top - insets.bottom;
            Rectangle rect = new Rectangle(x, y, width, height);
            f.pack();
            f.setBounds(rect);
            f.setVisible(true);

            Robot robot = new Robot();
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
            robot.delay(1000);
        } finally {
            f.dispose();
        }
    }
}
