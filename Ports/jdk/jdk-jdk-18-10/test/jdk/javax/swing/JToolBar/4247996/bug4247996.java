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
 * @bug 4247996 4260485
 * @summary Test that rollover toolbar doesn't corrupt buttons
 * @author Peter Zhelezniakov
 * @run main bug4247996
 */
import java.awt.*;
import javax.swing.*;

public class bug4247996 {

    private static JButton button;
    private static JToggleButton toogleButton;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        Point point = getButtonCenter();
        robot.mouseMove(point.x, point.y);
        robot.waitForIdle();

        checkButtonsSize();

    }

    private static void checkButtonsSize() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                if (!button.getSize().equals(toogleButton.getSize())) {
                    throw new RuntimeException("Button sizes are different!");
                }
            }
        });
    }

    private static Point getButtonCenter() throws Exception {
        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                Point p = button.getLocationOnScreen();
                Dimension size = button.getSize();
                result[0] = new Point(p.x + size.width / 2, p.y + size.height / 2);
            }
        });
        return result[0];
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);

        JButton rButton = new JButton("Rollover");
        rButton.setRolloverEnabled(true);
        JToolBar nrToolbar = new JToolBar();
        nrToolbar.add(rButton);
        nrToolbar.remove(rButton);

        if (!rButton.isRolloverEnabled()) {
            throw new Error("Failed (bug 4260485): "
                    + "toolbar overrode button's rollover property");
        }

        JToolBar rToolbar = new JToolBar();
        rToolbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        rToolbar.add(button = new JButton("Test"));
        rToolbar.add(toogleButton = new JToggleButton("Test"));

        frame.getContentPane().add(rToolbar, BorderLayout.NORTH);
        frame.setVisible(true);
    }
}
