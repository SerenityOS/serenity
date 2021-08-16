/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4846413
 * @summary Checks if No tooltip modification when no KeyStroke modifier
 * @library ../../regtesthelpers
 * @build Util
 * @author Konstantin Eremin
 * @run main bug4846413
 */
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.plaf.metal.MetalToolTipUI;

public class bug4846413 {

    private static volatile boolean isTooltipAdded;
    private static JButton button;

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

        Point movePoint = getButtonPoint();
        robot.mouseMove(movePoint.x, movePoint.y);
        robot.waitForIdle();

        long timeout = System.currentTimeMillis() + 9000;
        while (!isTooltipAdded && (System.currentTimeMillis() < timeout)) {
            try {Thread.sleep(500);} catch (Exception e) {}
        }

        checkToolTip();
    }

    private static void checkToolTip() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                JToolTip tooltip = (JToolTip) Util.findSubComponent(
                        JFrame.getFrames()[0], "JToolTip");

                if (tooltip == null) {
                    throw new RuntimeException("Tooltip has not been found!");
                }

                MetalToolTipUI tooltipUI = (MetalToolTipUI) MetalToolTipUI.createUI(tooltip);
                tooltipUI.installUI(tooltip);

                if (!"-Insert".equals(tooltipUI.getAcceleratorString())) {
                    throw new RuntimeException("Tooltip acceleration is not properly set!");
                }

            }
        });
    }

    private static Point getButtonPoint() throws Exception {
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
        frame.setLocationRelativeTo(null);

        button = new JButton("Press me");
        button.setToolTipText("test");
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, true), "someCommand");
        button.getActionMap().put("someCommand", null);
        frame.getContentPane().add(button);

        JLayeredPane layeredPane = (JLayeredPane) Util.findSubComponent(
                frame, "JLayeredPane");
        layeredPane.addContainerListener(new ContainerAdapter() {

            @Override
            public void componentAdded(ContainerEvent e) {
                isTooltipAdded = true;
            }
        });

        frame.setVisible(true);
    }
}
