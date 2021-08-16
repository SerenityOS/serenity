/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @bug 4478780
 * @key headful
 * @summary Tests that Choice can be accessed and controlled by keyboard.
 */
public class AccessibleChoiceTest {
    //Declare things used in the test, like buttons and labels here
    Frame frame = new Frame("window owner");
    Window win = new Window(frame);
    Choice choice = new Choice();
    Button def = new Button("default owner");
    CountDownLatch go = new CountDownLatch(1);

    public static void main(final String[] args) {
        AccessibleChoiceTest app = new AccessibleChoiceTest();
        app.test();
    }

    private void test() {
        try {
            init();
            start();
        } finally {
            if (frame != null) frame.dispose();
            if (win != null) win.dispose();
        }
    }

    public void init() {
        win.setLayout (new FlowLayout ());
        win.add(def);
        def.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    go.countDown();
                }
            });
        choice.add("One");
        choice.add("Two");
        win.add(choice);
    }

    public void start () {
        frame.setVisible(true);
        win.pack();
        win.setLocation(100, 200);
        win.setVisible(true);

        Robot robot = null;
        try {
            robot = new Robot();
        } catch (Exception ex) {
            throw new RuntimeException("Can't create robot");
        }
        robot.delay(2000);
        robot.setAutoDelay(150);
        robot.setAutoWaitForIdle(true);

        // Focus default button and wait till it gets focus
        Point loc = def.getLocationOnScreen();
        robot.mouseMove(loc.x+2, loc.y+2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        try {
            go.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            throw new RuntimeException("Interrupted !!!");
        }

        if (!def.isFocusOwner()) {
            throw new RuntimeException("Button doesn't have focus");
        }

        // Press Tab key to move focus to Choice
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.delay(500);

        // Press Down key to select next item in the choice(Motif 2.1)
        // If bug exists we won't be able to do so
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac")) {
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        }

        robot.delay(1000);

        // On success second item should be selected
        if (choice.getSelectedItem() != choice.getItem(1)) {
            throw new RuntimeException("Choice can't be controlled by keyboard");
        }
    }
}
