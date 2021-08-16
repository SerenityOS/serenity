/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/*
 * @test
 * @key headful
 * @bug 8046495
 * @summary Verifies that mouse/key events has always increasing 'when' timestamps
 * @author Anton Nashatyrev
 * @run main EventWhenTest
 */
public class EventWhenTest {

    private static volatile int eventsCount = 0;
    private static volatile boolean failed = false;

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            long lastWhen = 0;

            @Override
            public void eventDispatched(AWTEvent event) {
                long curWhen;
                if (event instanceof KeyEvent) {
                    curWhen = ((KeyEvent) event).getWhen();
                } else if (event instanceof MouseEvent) {
                    curWhen = ((MouseEvent) event).getWhen();
                } else {
                    return;
                }

                eventsCount++;

                if (curWhen < lastWhen) {
                    System.err.println("FAILED: " + curWhen + " < " + lastWhen +
                        " for " + event);
                    failed = true;
                } else {
                    lastWhen = curWhen;
                }
            }
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
    }

    public static void main(String[] args) throws Exception {

        Frame frame = new Frame();

        try {
            Button b = new Button("Button");
            frame.setBounds(300, 300, 300, 300);
            frame.add(b);
            frame.setVisible(true);

            Robot robot = new Robot();
            robot.waitForIdle();
            robot.mouseMove((int)frame.getLocationOnScreen().getX() + 150,
                    (int)frame.getLocationOnScreen().getY() + 150);

            eventsCount = 0;
            System.out.println("Clicking mouse...");
            for (int i = 0; i < 300 && !failed; i++) {
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                Thread.sleep(10);
                b.setLabel("Click: " + i);
            }

            if (eventsCount == 0) {
                throw new RuntimeException("No events were received");
            }

            if (failed) {
                throw new RuntimeException("Test failed.");
            }
            System.out.println("Clicking mouse done: " + eventsCount + " events.");

            b.requestFocusInWindow();
            robot.waitForIdle();

            eventsCount = 0;
            System.out.println("Typing a key...");
            for (int i = 0; i < 300 && !failed; i++) {
                robot.keyPress(KeyEvent.VK_A);
                robot.keyRelease(KeyEvent.VK_A);
                Thread.sleep(10);
                b.setLabel("Type: " + i);
            }
            System.out.println("Key typing done: " + eventsCount + " events.");

            if (eventsCount == 0) {
                throw new RuntimeException("No events were received");
            }

            if (failed) {
                throw new RuntimeException("Test failed.");
            }

            System.out.println("Success!");
        } finally {
            frame.dispose();
        }
    }
}
