/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 @bug 4009555 7119774
 @summary Test for Component's method getMousePosition()
 @run main ComponentMousePositionTest
 */
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.AWTException;

public class ComponentMousePositionTest {

    public void changeMousePosition() {
        Frame frame = new Frame();
        Panel panel = new Panel();
        Button button = new Button("Button");
        Button overlappedButton = new Button("Overlapped button");
        Dimension BUTTON_DIMENSION = new Dimension(100, 100);
        Dimension FRAME_DIMENSION = new Dimension(200, 200);
        Point POINT_WITHOUT_COMPONENTS = new Point(10, 10);
        Point FIRST_BUTTON_LOCATION = new Point(20, 20);
        Point SECOND_BUTTON_LOCATION = new Point(30, 30);

        button.setSize(BUTTON_DIMENSION);
        button.setLocation(FIRST_BUTTON_LOCATION);
        overlappedButton.setSize(BUTTON_DIMENSION);
        overlappedButton.setLocation(SECOND_BUTTON_LOCATION);

        panel.setLayout(null);
        panel.add(button);
        panel.add(overlappedButton);
        frame.add(panel);
        Robot robot;

        try {
            robot = new Robot();
            frame.setSize(FRAME_DIMENSION);
            frame.setVisible(true);
            robot.delay(2000);

            Point p = button.getLocationOnScreen();
            robot.mouseMove(p.x + button.getWidth()
                    / 2, p.y + button.getHeight() / 2);
            robot.waitForIdle();

            Point pMousePosition = button.getMousePosition();
            if (pMousePosition == null) {
                throw new RuntimeException("Test failed: "
                        + "Component.getMousePosition() returned null result "
                        + "inside Component");
            }

            if (pMousePosition.x != button.getWidth() / 2
                    || pMousePosition.y != button.getHeight() / 2) {
                throw new RuntimeException("Test failed: "
                        + "Component.getMousePosition() returned incorrect "
                        + "result inside Component");
            }

            pMousePosition = overlappedButton.getMousePosition();
            if (pMousePosition != null) {
                throw new RuntimeException("Test failed: Overlapped component "
                        + "did not return null result when a pointer was inside"
                        + " the components bounds.");
            }
            robot.mouseMove(panel.getLocationOnScreen().x
                    + POINT_WITHOUT_COMPONENTS.x,
                    panel.getLocationOnScreen().y
                    + POINT_WITHOUT_COMPONENTS.y);
            robot.waitForIdle();

            pMousePosition = button.getMousePosition();
            if (pMousePosition != null) {
                throw new RuntimeException("FAILED: "
                        + "Component.getMousePosition() returned non-null "
                        + "results outside Component");
            }

        } catch (AWTException e) {
            throw new RuntimeException("FAILED: AWTException by Robot" + e);
        } finally {
            frame.dispose();
        }
    }

    public static void main(String args[]) {
        ComponentMousePositionTest mousePos = new ComponentMousePositionTest();
        mousePos.changeMousePosition();
    }
}
