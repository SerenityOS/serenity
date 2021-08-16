/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013581
 * @summary [macosx] Key Bindings break with awt GraphicsEnvironment setFullScreenWindow
 * @author leonid.romanov@oracle.com
 * @run main bug8013581
 */

import java.awt.*;
import java.awt.event.*;

public class bug8013581 {
    private static Frame frame;
    private static volatile int listenerCallCounter = 0;

    public static void main(String[] args) throws Exception {
        final GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        final GraphicsDevice[] devices = ge.getScreenDevices();

        final Robot robot = new Robot();
        robot.setAutoDelay(50);

        createAndShowGUI();
        robot.waitForIdle();

        Exception error = null;
        for (final GraphicsDevice device : devices) {
            if (!device.isFullScreenSupported()) {
                continue;
            }

            device.setFullScreenWindow(frame);
            sleep(robot);

            robot.keyPress(KeyEvent.VK_A);
            robot.keyRelease(KeyEvent.VK_A);
            robot.waitForIdle();

            device.setFullScreenWindow(null);
            sleep(robot);

            if (listenerCallCounter != 2) {
                error = new Exception("Test failed: KeyListener called " + listenerCallCounter + " times instead of 2!");
                break;
            }

            listenerCallCounter = 0;
        }

        frame.dispose();

        if (error != null) {
            throw error;
        }
     }

    private static void createAndShowGUI() {
        frame = new Frame("Test");
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                listenerCallCounter++;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                listenerCallCounter++;
            }
        });

        frame.setUndecorated(true);
        frame.setVisible(true);
    }

    private static void sleep(Robot robot) {
        robot.waitForIdle();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }
}
