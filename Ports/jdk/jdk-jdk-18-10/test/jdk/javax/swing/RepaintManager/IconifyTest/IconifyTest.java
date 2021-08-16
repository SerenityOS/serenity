/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4665214
 * @summary Makes sure that RepaintManager doesn't attempt to repaint
 *          a frame when it is iconified.
 * @author Scott Violet
 * @run main IconifyTest
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class IconifyTest {
    private static volatile boolean windowIconifiedIsCalled = false;
    private static volatile boolean frameIsRepainted = false;
    static JFrame frame;
    static JButton button;

    public static void main(String[] args) throws Throwable {
        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();
                button = new JButton("HI");
                frame.getContentPane().add(button);
                frame.addWindowListener(new WindowAdapter() {
                    public void windowIconified(WindowEvent e) {
                        windowIconifiedIsCalled = true;
                        RepaintManager rm = RepaintManager.currentManager(null);
                        rm.paintDirtyRegions();
                        button.repaint();
                        if (!rm.getDirtyRegion(button).isEmpty()) {
                            frameIsRepainted = true;
                        }
                    }
                });
                frame.pack();
                frame.setVisible(true);
            }
        });
        robot.waitForIdle();
        robot.delay(1000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setExtendedState(Frame.ICONIFIED);
            }
        });
        robot.waitForIdle();
        robot.delay(1000);

        if (!windowIconifiedIsCalled) {
            throw new Exception("Test failed: window was not iconified.");
        }
        if (frameIsRepainted) {
            throw new Exception("Test failed: frame was repainted when window was iconified.");
        }
    }
}
