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

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.Locale;

/*
 * @test
 * @key headful
 * @bug 8022401 8160623
 * @summary Wrong key char
 * @author Alexandr Scherbatiy
 * @run main KeyCharTest
 */
public class KeyCharTest {

    private static volatile int eventsCount = 0;

    static {
        Locale.setDefault(Locale.ENGLISH);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            @Override
            public void eventDispatched(AWTEvent event) {
                eventsCount++;
                char delete = ((KeyEvent) event).getKeyChar();
                if (delete != '\u007f') {
                    throw new RuntimeException("Key char is not delete: '" + delete + "'");
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    public static void main(String[] args) throws Exception {


        Frame frame = new Frame();
        frame.setSize(300, 300);
        frame.setVisible(true);
        Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.waitForIdle();


        robot.keyPress(KeyEvent.VK_DELETE);
        robot.keyRelease(KeyEvent.VK_DELETE);
        robot.waitForIdle();

        frame.dispose();

        if (eventsCount != 3) {
            throw new RuntimeException("Wrong number of key events: " + eventsCount);
        }
    }
}
