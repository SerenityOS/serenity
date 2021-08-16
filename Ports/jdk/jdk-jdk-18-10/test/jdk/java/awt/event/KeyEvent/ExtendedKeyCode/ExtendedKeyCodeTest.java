/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

/*
 * @test
 * @key headful
 * @bug 8007156 8025126
 * @summary Extended key code is not set for a key event
 * @author Alexandr Scherbatiy
 * @library /lib/client
 * @build ExtendedRobot
 * @run main ExtendedKeyCodeTest
 */
public class ExtendedKeyCodeTest {

    private static volatile boolean setExtendedKeyCode = true;
    private static volatile int eventsCount = 0;

    public static void main(String[] args) throws Exception {
        ExtendedRobot robot = new ExtendedRobot();
        robot.setAutoDelay(50);

        Frame frame = new Frame();
        frame.setSize(300, 300);

        frame.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                eventsCount++;
                setExtendedKeyCode = setExtendedKeyCode && (e.getExtendedKeyCode()
                        == KeyEvent.getExtendedKeyCodeForChar(e.getKeyChar()));
            }

            @Override
            public void keyReleased(KeyEvent e) {
                eventsCount++;
                setExtendedKeyCode = setExtendedKeyCode && (e.getExtendedKeyCode()
                        == KeyEvent.getExtendedKeyCodeForChar(e.getKeyChar()));
            }
        });

        frame.setVisible(true);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_D);
        robot.keyRelease(KeyEvent.VK_D);
        robot.waitForIdle();

        frame.dispose();

        if (eventsCount != 2 || !setExtendedKeyCode) {
            throw new RuntimeException("Wrong extended key code");
        }

        frame = new Frame();
        frame.setSize(300, 300);
        setExtendedKeyCode = false;

        frame.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                setExtendedKeyCode = e.getExtendedKeyCode() == KeyEvent.VK_LEFT;
            }
        });

        frame.setVisible(true);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_LEFT);
        robot.keyRelease(KeyEvent.VK_LEFT);
        robot.waitForIdle();
        frame.dispose();

        if (!setExtendedKeyCode) {
            throw new RuntimeException("Wrong extended key code!");
        }
    }
}
