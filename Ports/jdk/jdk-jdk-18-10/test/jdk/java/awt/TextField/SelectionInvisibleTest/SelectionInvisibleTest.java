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

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.event.InputEvent;

/**
 * @test
 * @key headful
 * @bug 8036110
 * @author Alexander Scherbatiy
 * @summary In TextField can only select text visible or to the left
 * @run main SelectionInvisibleTest
 */

public class SelectionInvisibleTest {

    private static final String TEXT = "One Two Three Four Five Six Seven Eight Nine ";
    private static final String LAST_WORD = "Ten";

    public static void main(String[] args) throws Exception {

        Frame frame = new Frame();
        frame.setSize(300, 200);
        TextField textField = new TextField(TEXT + LAST_WORD, 30);
        Panel panel = new Panel(new FlowLayout());
        panel.add(textField);
        frame.add(panel);
        frame.setVisible(true);

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        robot.waitForIdle();

        Point point = textField.getLocationOnScreen();
        int x = point.x + textField.getWidth() / 2;
        int y = point.y + textField.getHeight() / 2;
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        robot.mousePress(InputEvent.BUTTON1_MASK);
        int N = 10;
        int dx = textField.getWidth() / N;
        for (int i = 0; i < N; i++) {
            x += dx;
            robot.mouseMove(x, y);
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        if (!textField.getSelectedText().endsWith(LAST_WORD)) {
            throw new RuntimeException("Last word is not selected!");
        }
    }
}
