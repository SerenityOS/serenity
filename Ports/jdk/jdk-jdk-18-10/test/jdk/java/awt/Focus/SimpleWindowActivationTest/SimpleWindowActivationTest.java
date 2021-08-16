/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug       6385277
 * @summary   Tests that override redirect window gets activated on click.
 * @author    anton.tarasov@sun.com: area=awt.focus
 * @library   ../../regtesthelpers
 * @build     Util
 * @run       main SimpleWindowActivationTest
 */
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Callable;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

public class SimpleWindowActivationTest {

    private static Frame frame;
    private static Window window;
    private static Button fbutton;
    private static Button wbutton;
    private static Label label;
    private static Robot robot;

    public static void main(String[] args) throws Exception {

        if ("sun.awt.motif.MToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
            System.out.println("No testing on Motif. Test passed.");
            return;
        }

        robot = new Robot();
        robot.setAutoDelay(50);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent e) {
                System.out.println(e);
            }
        }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_FOCUS_EVENT_MASK);

        createAndShowWindow();
        robot.waitForIdle();

        createAndShowFrame();
        robot.waitForIdle();

        // click on Frame
        clickOn(getClickPoint(frame));

        if (!frame.isFocused()) {
            throw new RuntimeException("Error: a frame couldn't be focused by click.");
        }

        //click on Label in Window
        clickOn(getClickPoint(label));

        if (!window.isFocused()) {
            throw new RuntimeException("Test failed: the window couldn't be activated by click!");
        }

        // bring focus back to the frame
        clickOn(getClickPoint(fbutton));

        if (!frame.isFocused()) {
            throw new RuntimeException("Error: a frame couldn't be focused by click.");
        }

        // Test 2. Verifies that clicking on a component of unfocusable Window
        //         won't activate it.

        window.setFocusableWindowState(false);
        robot.waitForIdle();


        clickOn(getClickPoint(label));

        if (window.isFocused()) {
            throw new RuntimeException("Test failed: unfocusable window got activated by click!");
        }
        System.out.println("Test passed.");

    }

    private static void createAndShowWindow() {

        frame = new Frame("Test Frame");
        window = new Window(frame);
        wbutton = new Button("wbutton");
        label = new Label("label");

        window.setBounds(800, 200, 300, 100);
        window.setLayout(new FlowLayout());
        window.add(wbutton);
        window.add(label);
        window.setVisible(true);

    }

    private static void createAndShowFrame() {
        fbutton = new Button("fbutton");

        frame.setBounds(800, 0, 300, 100);
        frame.setLayout(new FlowLayout());
        frame.add(fbutton);
        frame.setVisible(true);

    }

    static void clickOn(Point point) {

        robot.mouseMove(point.x, point.y);
        robot.waitForIdle();

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.waitForIdle();
    }

    static Point getClickPoint(Component c) {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();
        return new Point(p.x + (int) (d.getWidth() / 2), p.y + (int) (d.getHeight() / 2));
    }

    static Point getClickPoint(Frame frame) {
        Point p = frame.getLocationOnScreen();
        Dimension d = frame.getSize();
        return new Point(p.x + (int) (d.getWidth() / 2), p.y + (frame.getInsets().top / 2));
    }
}
