/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165555
 * @summary VM crash after creating Robot second time and accessing key codes in
 *          single JVM mode.
 * @run main RobotCrash
 */
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;

public class RobotCrash implements Runnable {

    private Frame frame;

    public void robotKeyPressTest() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            frame = new Frame();
            frame.setSize(300, 300);
            frame.setVisible(true);
        });

        Robot robot = new Robot();
        robot.waitForIdle();
        Point pt = frame.getLocationOnScreen();
        robot.mouseMove(((int) pt.getX() + frame.getWidth()) / 2,
                ((int) pt.getY() + frame.getHeight()) / 2);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.waitForIdle();
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            frame.dispose();
        });
    }

    @Override
    public void run() {
        try {
            robotKeyPressTest();
        } catch (Exception e) {
            throw new RuntimeException("Test Failed" + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < 10; i++) {
            Thread t1 = new Thread(new RobotCrash());
            t1.start();
            t1.join();
            Thread t2 = new Thread(new RobotCrash());
            t2.start();
            t2.join();
            Thread.sleep(1000);
        }
    }
}
