/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 @bug 6180449 8160764
 @summary TextArea scrolls to its left when selecting the text from the end.
 @run main TextAreaScrolling
 */

import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.event.InputEvent;

public class TextAreaScrolling {
    Frame mainFrame;
    TextArea textArea;
    Robot robot;

    TextAreaScrolling() {
        try {
            robot = new Robot();
        } catch (Exception ex) {
            throw new RuntimeException("Robot Creation Failed");
        }

        mainFrame = new Frame();
        mainFrame.setSize(200, 200);
        mainFrame.setLocation(200, 200);

        textArea = new TextArea();
        textArea.setText("1234 5678");
        textArea.setSelectionStart(3);
        textArea.setSelectionEnd(4);
        mainFrame.add(textArea);
        mainFrame.setVisible(true);
        textArea.requestFocusInWindow();
    }

    public void dispose() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    public void performTest() {
        robot.waitForIdle();
        robot.delay(1000);
        Point loc = textArea.getLocationOnScreen();
        Rectangle textAreaBounds = new Rectangle();
        textArea.getBounds(textAreaBounds);

        // Move mouse at center in first row of TextArea.
        robot.mouseMove(loc.x + textAreaBounds.width / 2, loc.y + 5);
        robot.waitForIdle();
        robot.delay(500);

        // Perform selection by scrolling to left from end of char sequence.
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseMove(loc.x - 5, loc.y + 5);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.delay(500);

        // Perform double click on beginning word of TextArea
        robot.mouseMove(loc.x + 5, loc.y + 5);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.delay(500);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(500);
        robot.waitForIdle();

        if (textArea.getSelectedText().contentEquals("5678")) {
            dispose();
            throw new RuntimeException ("TextArea over scrolled towards left. "
                + "Expected selected text: '1234 ' and for mac '1234' "
                + "Actual selected text: 5678");
        }
    }

    public static void main(String argv[]) throws RuntimeException {
        TextAreaScrolling test = new TextAreaScrolling();
        test.performTest();
        test.dispose();
    }
}
