/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @bug        5082319
  @summary    Tests that focus request for already focused component doesn't block key events.
  @run main FocusSubRequestTest
*/

import java.awt.*;
import java.awt.event.*;

public class FocusSubRequestTest {
    Frame frame = new Frame("Test Frame");
    Button button = new Button("button");
    boolean passed = false;
    Robot robot;

    public static void main(final String[] args) {
        FocusSubRequestTest app = new FocusSubRequestTest();
        app.init();
        app.start();
    }

    public void init() {
        frame.add(button);
        button.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    System.out.println("FocusSubRequestTest: focusGained for: " + e.getSource());
                    ((Component)e.getSource()).requestFocus();
                }
            });

        button.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    System.out.println("FocusSubRequestTest: keyPressed for: " + e.getSource());
                    passed = true;
                }
            });

        try {
            robot = new Robot();
            robot.setAutoDelay(100);
        } catch(Exception e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }
    }

    public void start() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        waitTillShown(button);
        frame.toFront();

        robot.delay(100);
        robot.keyPress(KeyEvent.VK_K);
        robot.keyRelease(KeyEvent.VK_K);

        robot.waitForIdle();

        if(passed) {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed.");
        }
    }

    private void waitTillShown(Component component) {
        Point p = null;
        while (p == null) {
            try {
                p = component.getLocationOnScreen();
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
