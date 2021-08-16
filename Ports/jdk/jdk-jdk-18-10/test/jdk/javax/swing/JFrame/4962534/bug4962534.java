/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4962534
  @summary JFrame dances very badly
  @run main bug4962534
 */

import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.*;

public class bug4962534 {

    Robot robot;
    volatile Point framePosition;
    volatile Point newFrameLocation;
    static JFrame frame;
    Rectangle gcBounds;
    Component titleComponent;
    JLayeredPane lPane;
    volatile boolean titleFound = false;
    public static Object LOCK = new Object();

    public static void main(final String[] args) throws Exception {
        try {
            bug4962534 app = new bug4962534();
            app.init();
            app.start();
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Init failed. " + ex.getMessage());
        }
    }//End  init()

    public void start() {
        try {
            setJLayeredPaneEDT();
            setTitleComponentEDT();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Test failed. " + ex.getMessage());
        }

        if (!titleFound) {
            throw new RuntimeException("Test Failed. Unable to determine title's size.");
        }

        Random r = new Random();

        for (int iteration = 0; iteration < 10; iteration++) {
            try {
                setFramePosEDT();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Test failed.");
            }
            try {
                robot = new Robot();
                robot.setAutoDelay(70);

                robot.waitForIdle();

                robot.mouseMove(framePosition.x + getJFrameWidthEDT() / 2,
                        framePosition.y + titleComponent.getHeight() / 2);
                robot.mousePress(InputEvent.BUTTON1_MASK);

                robot.waitForIdle();

                gcBounds =
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getConfigurations()[0].getBounds();

                robot.mouseMove(framePosition.x + getJFrameWidthEDT() / 2,
                        framePosition.y + titleComponent.getHeight() / 2);

                robot.waitForIdle();

                int multier = gcBounds.height / 2 - 10; //we will not go out the borders
                for (int i = 0; i < 10; i++) {
                    robot.mouseMove(gcBounds.width / 2 - (int) (r.nextDouble() * multier), gcBounds.height / 2 - (int) (r.nextDouble() * multier));
                }
                robot.mouseRelease(InputEvent.BUTTON1_MASK);

                robot.waitForIdle();

            } catch (AWTException e) {
                throw new RuntimeException("Test Failed. AWTException thrown." + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Test Failed.");
            }
            System.out.println("Mouse  lies in " + MouseInfo.getPointerInfo().getLocation());
            boolean frameIsOutOfScreen = false;
            try {
                setNewFrameLocationEDT();
                System.out.println("Now Frame lies in " + newFrameLocation);
                frameIsOutOfScreen = checkFrameIsOutOfScreenEDT();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Test Failed.");
            }

            if (frameIsOutOfScreen) {
                throw new RuntimeException("Test failed. JFrame is out of screen.");
            }

        } //for iteration
        System.out.println("Test passed.");
    }// start()

    private void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(
                    "javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame = new JFrame("JFrame Dance Test");
        frame.pack();
        frame.setSize(450, 260);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setJLayeredPaneEDT() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                lPane = frame.getLayeredPane();
                System.out.println("JFrame's LayeredPane " + lPane);
            }
        });
    }

    private void setTitleComponentEDT() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < lPane.getComponentsInLayer(JLayeredPane.FRAME_CONTENT_LAYER.intValue()).length; j++) {
                    titleComponent = lPane.getComponentsInLayer(JLayeredPane.FRAME_CONTENT_LAYER.intValue())[j];
                    if (titleComponent.getClass().getName().equals("javax.swing.plaf.metal.MetalTitlePane")) {
                        titleFound = true;
                        break;
                    }
                }
            }
        });
    }

    private void setFramePosEDT() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                framePosition = frame.getLocationOnScreen();
            }
        });
    }

    private boolean checkFrameIsOutOfScreenEDT() throws Exception {

        final boolean[] result = new boolean[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (newFrameLocation.x > gcBounds.width || newFrameLocation.x < 0
                    || newFrameLocation.y > gcBounds.height || newFrameLocation.y
                    < 0) {
                result[0] = true;
            }
            }
        });
        return result[0];
    }

    private void setNewFrameLocationEDT() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                newFrameLocation = new Point(frame.getLocationOnScreen().x
                        + frame.getWidth() / 2, frame.getLocationOnScreen().y + titleComponent.getHeight() / 2);
            }
        });
    }

    private int getJFrameWidthEDT() throws Exception {

        final int[] result = new int[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                result[0] = frame.getWidth();
            }
        });

        return result[0];
    }
}// class
