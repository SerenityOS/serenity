/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7163696
 * @summary Tests that JScrollBar scrolls to the left
 * @author Sergey Malenkov
 */

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class Test7163696 implements Runnable {

    private static final boolean AUTO = null != System.getProperty("test.src", null);

    public static void main(String[] args) throws Exception {
        new Test7163696().test();
    }

    private JScrollBar bar;

    private void test() throws Exception {
        Robot robot = new Robot();
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            UIManager.setLookAndFeel(info.getClassName());

            SwingUtilities.invokeAndWait(this);
            robot.waitForIdle(); // after creation
            Thread.sleep(1000);

            Point point = this.bar.getLocation();
            SwingUtilities.convertPointToScreen(point, this.bar);
            point.x += this.bar.getWidth() >> 2;
            point.y += this.bar.getHeight() >> 1;
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            robot.waitForIdle(); // before validation
            Thread.sleep(1000);
            SwingUtilities.invokeAndWait(this);

            if (this.bar != null) {
                this.bar = null; // allows to reuse the instance
                if (AUTO) { // error reporting only for automatic testing
                    throw new Error("TEST FAILED");
                }
            }
        }
    }

    public void run() {
        if (this.bar == null) {
            this.bar = new JScrollBar(JScrollBar.HORIZONTAL, 50, 10, 0, 100);
            this.bar.setPreferredSize(new Dimension(400, 20));

            JFrame frame = new JFrame();
            frame.add(this.bar);
            frame.pack();
            frame.setVisible(true);
        }
        else if (40 != this.bar.getValue()) {
            System.out.println("name = " + UIManager.getLookAndFeel().getName());
            System.out.println("value = " + this.bar.getValue());
        }
        else {
            SwingUtilities.getWindowAncestor(this.bar).dispose();
            this.bar = null;
        }
    }
}
