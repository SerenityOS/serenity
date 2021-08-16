/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 @test
 @key headful
 @bug 6176814 8132766
 @summary Metalworks frame maximizes after the move
 @run main MaximizedFrameTest
 */

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MaximizedFrameTest {

    final static int ITERATIONS_COUNT = 5;
    private static JFrame frame;
    private static Point tempMousePosition;
    private static Component titleComponent;

    public void init() {
        JFrame.setDefaultLookAndFeelDecorated(true);

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException ex) {
            throw new RuntimeException("Test Failed. MetalLookAndFeel not set "
                    + "for frame");
        }

        frame = new JFrame("JFrame Maximization Test");
        frame.pack();
        frame.setSize(450, 260);
        frame.setVisible(true);
    }

    public void getTitleComponent() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                JLayeredPane lPane = frame.getLayeredPane();
                boolean titleFound = false;

                for (int j = 0; j < lPane.getComponentsInLayer(
                    JLayeredPane.FRAME_CONTENT_LAYER.intValue()).length; j++) {

                    titleComponent = lPane.getComponentsInLayer(
                    JLayeredPane.FRAME_CONTENT_LAYER.intValue())[j];

                    if (titleComponent.getClass().getName().equals(
                        "javax.swing.plaf.metal.MetalTitlePane")) {

                        titleFound = true;
                        break;
                    }
                }

                if (!titleFound) {
                    try {
                        dispose();
                    } catch (Exception ex) {
                        Logger.getLogger(MaximizedFrameTest.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }
                    throw new RuntimeException("Test Failed. Unable to "
                            + "determine title component");
                }
            }
        });
    }

    public void doMaximizeFrameTest() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Point framePosition = frame.getLocationOnScreen();

                tempMousePosition = new Point(framePosition.x
                        + frame.getWidth() / 2, framePosition.y
                                + titleComponent.getHeight() / 2);
            }
        });

        try {
            Robot robot = new Robot();
            robot.mouseMove(tempMousePosition.x, tempMousePosition.y);
            robot.waitForIdle();

            for (int iteration = 0; iteration < ITERATIONS_COUNT; iteration++) {
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();

                // Moving a mouse pointer less than a few pixels
                // leads to rising a double click event.
                // We have to use exceeded the AWT_MULTICLICK_SMUDGE
                // const value (which is 4 by default on GNOME) to test that.
                tempMousePosition.x += 5;
                robot.mouseMove(tempMousePosition.x, tempMousePosition.y);
                robot.waitForIdle();
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();

                if (frame.getExtendedState() != 0) {
                    dispose();
                    throw new RuntimeException("Test failed. JFrame was "
                            + "maximized. ExtendedState is : "
                            + frame.getExtendedState());
                }
            }
        } catch (AWTException e) {
            dispose();
            throw new RuntimeException("Test Failed. AWTException thrown.");
        }
        System.out.println("Test passed.");
    }

    private void dispose() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (null != frame) {
                    frame.dispose();
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {

        MaximizedFrameTest maximizedFrameTest = new MaximizedFrameTest();
        maximizedFrameTest.init();
        maximizedFrameTest.getTitleComponent();
        maximizedFrameTest.doMaximizeFrameTest();
        maximizedFrameTest.dispose();
    }
}
