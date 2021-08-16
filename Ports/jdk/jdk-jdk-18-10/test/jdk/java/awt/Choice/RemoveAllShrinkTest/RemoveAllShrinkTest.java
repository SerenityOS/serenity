/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4851798 8041896 8225126
  @summary Tests Choice List shrinks after removeAll
  @run main RemoveAllShrinkTest
*/

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

public class RemoveAllShrinkTest {

    public static void main(String[] args) {
        Frame f = new Frame();
        try {
            Choice choice = new Choice();

            for (int i = 0; i < 10; ++i) {
                choice.addItem("Item " + i);
            }

            f.add(choice, BorderLayout.NORTH);
            Panel panel = new Panel();
            panel.setBackground(Color.RED);
            f.add(panel);

            f.setSize(200, 200);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            f.toFront();

            choice.removeAll();

            try {
                Robot robot = new Robot();
                robot.setAutoWaitForIdle(true);
                robot.setAutoDelay(50);

                robot.waitForIdle();
                Thread.sleep(200);

                Point pt = choice.getLocationOnScreen();
                robot.mouseMove(pt.x + choice.getWidth() - choice.getHeight() / 2,
                        pt.y + choice.getHeight() / 2);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);

                Thread.sleep(400);

                Point pt1 = panel.getLocationOnScreen();

                Color color = robot.getPixelColor(pt1.x + panel.getWidth() / 2,
                        pt1.y + panel.getHeight() / 2);

                if (!color.equals(Color.RED)) {
                    throw new RuntimeException("RemoveAllShrinkTest failed. " + color);
                }
            } catch (Exception e) {
                throw new RuntimeException("The test was not completed.\n\n" + e);
            }
        } finally {
            f.dispose();
        }
    }
}

