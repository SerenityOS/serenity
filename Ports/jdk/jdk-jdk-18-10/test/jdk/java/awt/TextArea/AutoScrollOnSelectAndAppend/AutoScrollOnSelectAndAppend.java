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
 * @bug 5003402 8151588
 * @summary TextArea must scroll automatically when calling append and select,
 *          even when not in focus.
 * @run main AutoScrollOnSelectAndAppend
 */

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AutoScrollOnSelectAndAppend {

    Frame frame;
    TextArea textArea;
    Button buttonHoldFocus;
    Robot robot;
    int test;
    int selectScrollPos1;
    int selectScrollPos2;
    String selectionText;

    public void composeTextArea() {
        String filler = "";
        // Add 10 rows of text for first selection auto scroll test.
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                filler = filler + i + i + "\n";
            }
        }
        selectScrollPos1 = filler.length();
        String text = filler + "FirstScroll\n";

        // Add 10 more rows of text for second selection auto scroll test.
        filler = "";
        for (int i = 2; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                filler = filler + i + i + "\n";
            }
        }
        text = text + filler;
        selectScrollPos2 = text.length();
        text = text + "SecondScroll\n";

        // Add 10 more rows of text for append text auto scroll test.
        filler = "";
        for (int i = 4; i < 6; i++) {
            for (int j = 0; j < 5; j++) {
                filler = filler + i + i + "\n";
            }
        }
        text = text + filler;
        textArea.setText(text);

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() % 2 == 0) {
                    if (!(textArea.getSelectedText().contains(selectionText))) {
                        dispose();
                        throw new RuntimeException("Test No: " + test +
                            ": TextArea is not auto scrolled to show the" +
                            " select/append text.");
                    }
                }
            }
        });
    }

    public AutoScrollOnSelectAndAppend() {
        try {
            robot = new Robot();
        } catch (Exception ex) {
            throw new RuntimeException("Robot Creation Failed.");
        }
        frame = new Frame();
        frame.setSize(200, 200);
        frame.setLayout(new FlowLayout());

        textArea = new TextArea(5, 20);
        composeTextArea();
        frame.add(textArea);

        buttonHoldFocus = new Button("HoldFocus");
        frame.add(buttonHoldFocus);

        frame.setVisible(true);
        robot.waitForIdle();

        // Move mouse cursor on first row of text area.
        Point loc = textArea.getLocationOnScreen();
        robot.mouseMove(loc.x + 8, loc.y + 8);
        robot.waitForIdle();
    }

    public void doubleClick() {
        // Delay to make sure auto scroll is finished.
        robot.waitForIdle();
        robot.delay(500);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(100);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
    }

    public void setFocusOnButton() {
        buttonHoldFocus.requestFocusInWindow();
        robot.waitForIdle();
    }

    public void selectAutoScrollTest1() {
        test = 1;
        setFocusOnButton();
        textArea.select(selectScrollPos1, selectScrollPos1);
        selectionText = "11";
        doubleClick();
    }

    public void selectAutoScrollTest2() {
        test = 2;
        setFocusOnButton();
        textArea.select(selectScrollPos2, selectScrollPos2);
        selectionText = "33";
        doubleClick();
    }

    public void appendAutoScrollTest() {
        test = 3;
        setFocusOnButton();
        selectionText = "55";
        textArea.append("appendScroll");
        doubleClick();
    }

    public void dispose() {
        frame.dispose();
    }

    public static void main(String[] args) {
        AutoScrollOnSelectAndAppend test = new AutoScrollOnSelectAndAppend();
        test.selectAutoScrollTest1();
        test.selectAutoScrollTest2();
        test.appendAutoScrollTest();
        test.dispose();
    }
}
