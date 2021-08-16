/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8149636
 * @summary TextArea over scrolls to right when selecting text towards right.
 * @requires os.family == "windows"
 * @run main OverScrollTest
 */

import java.awt.Frame;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.event.InputEvent;

public class OverScrollTest {
    Frame mainFrame;
    TextArea textArea;
    Robot robot;

    OverScrollTest() {
        try {
            robot = new Robot();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        mainFrame = new Frame();
        mainFrame.setSize(400, 200);
        mainFrame.setLocation(200, 200);
        mainFrame.setLayout(new FlowLayout());

        textArea = new TextArea(2, 10);
        textArea.setSize(300, 100);
        textArea.setText("123456 789123");
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
        Point loc = textArea.getLocationOnScreen();
        Rectangle textAreaBounds = new Rectangle();
        textArea.getBounds(textAreaBounds);

        // Move mouse at center in first row of TextArea.
        robot.mouseMove(loc.x + textAreaBounds.width / 2, loc.y + 5);

        // Perform selection by scrolling to right from end of char sequence.
        robot.mousePress(InputEvent.BUTTON1_MASK);
        for (int i = 0; i < textAreaBounds.width; i += 15) {
            robot.mouseMove(i + loc.x + textAreaBounds.width / 2, loc.y + 5);
            robot.delay(10);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        // Perform double click on beginning word of TextArea
        robot.mouseMove(loc.x + 5, loc.y + 5);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(100);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        dispose();
        if (!textArea.getSelectedText().contains("123456")) {
            throw new RuntimeException ("TextArea over scrolled towards right. "
                + "Selected text should contain: '123456' "
                + "Actual selected test: '" + textArea.getSelectedText() + "'");
        }
    }

    public static void main(String argv[]) throws RuntimeException {
        OverScrollTest test = new OverScrollTest();
        test.performTest();
    }
}
