/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import test.java.awt.event.helpers.lwcomponents.LWButton;
import test.java.awt.event.helpers.lwcomponents.LWList;

import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @key headful
 * @bug 8043126
 * @summary Check whether correct modifiers set when multiple mouse buttons were pressed;
 *          check number of received events.
 *
 * @library /lib/client/ ../../helpers/lwcomponents/
 * @library /test/lib
 * @build LWComponent
 * @build LWButton
 * @build LWList
 * @build ExtendedRobot
 * @run main/timeout=600 MultipleMouseButtonsTest
 */


public class MultipleMouseButtonsTest implements MouseListener {

    private final static int robotDelay = 1000;

    private final ExtendedRobot robot;
    private final Object lock = new Object();

    private Frame frame;

    private Button    button;
    private LWButton  buttonLW;
    private TextField textField;
    private TextArea  textArea;
    private List      list;
    private LWList    listLW;

    private int eventCount;
    private int testCount;
    private boolean pressed = false;
    private int modifiers = 0;
    private int modifiersEx = 0;

    private boolean countEvents = false;


    public void createGUI() {

        frame = new Frame("MultipleMouseButtonTest");
        frame.setLayout(new GridLayout(1, 6));

        button = new Button();
        button.addMouseListener(this);
        frame.add(button);

        buttonLW = new LWButton();
        buttonLW.addMouseListener(this);
        frame.add(buttonLW);

        textField = new TextField(5);
        textField.addMouseListener(this);
        frame.add(textField);

        textArea = new TextArea(5, 5);
        textArea.addMouseListener(this);
        frame.add(textArea);

        list = new List();
        for (int i = 1; i <= 5; ++i) { list.add("item " + i); }
        list.addMouseListener(this);
        frame.add(list);

        listLW = new LWList();
        for (int i = 1; i <= 5; ++i) { listLW.add("item " + i); }
        listLW.addMouseListener(this);
        frame.add(listLW);

        frame.setBackground(Color.gray);
        frame.setSize(500, 100);
        frame.setVisible(true);
        frame.toFront();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited (MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {

        if (!countEvents) { return; }

        ++eventCount;

        pressed = true;
        modifiers = e.getModifiers();
        modifiersEx = e.getModifiersEx();

        synchronized (lock) { lock.notifyAll(); }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (countEvents) {
            ++eventCount;
        }
    }

    MultipleMouseButtonsTest() throws Exception {
        this.robot = new ExtendedRobot();
        EventQueue.invokeAndWait( this::createGUI );
    }

    void doTest() throws Exception {

        int masks[] = new int[]{InputEvent.BUTTON1_MASK, InputEvent.BUTTON2_MASK, InputEvent.BUTTON3_MASK};
        int masksEx[] = new int[]{InputEvent.BUTTON1_DOWN_MASK, InputEvent.BUTTON2_DOWN_MASK, InputEvent.BUTTON3_DOWN_MASK};

        robot.waitForIdle();

        ArrayList<Component> components = new ArrayList();
        components.add(button);
        components.add(buttonLW);
        components.add(textField);
        components.add(textArea);
        components.add(list);
        components.add(listLW);

        for (Component c: components) {

            System.out.println(c.getClass().getName() + ": ");

            Point origin = c.getLocationOnScreen();

            int xc = origin.x + c.getWidth() / 2;
            int yc = origin.y + c.getHeight() / 2;
            Point center = new Point(xc, yc);

            robot.delay(robotDelay);
            robot.mouseMove(origin);
            robot.delay(robotDelay);
            robot.glide(origin, center);
            robot.delay(robotDelay);
            robot.click();
            robot.delay(robotDelay);

            testCount = 0;
            eventCount = 0;

            for (int i = 0; i < masks.length; ++i) {

                for (int k = 0; k < masks.length; ++k) {
                    if (k == i) { continue; }

                    countEvents = false;
                    robot.mousePress(masks[i]);
                    robot.delay(robotDelay);

                    countEvents = true;

                    pressed = false;

                    robot.mousePress(masks[k]);
                    robot.delay(robotDelay);
                    ++testCount;

                    if (!pressed) {
                        synchronized (lock) {
                            try {
                                lock.wait(3 * robotDelay);
                            } catch (InterruptedException ex) {}
                        }
                    }

                    assertTrue(pressed, "mouse press event was not received");

                    assertEQ(modifiers & masks[k], masks[k], "invalid modifiers");
                    assertEQ(modifiersEx & masksEx[i], masksEx[i], "invalid extended modifiers");

                    robot.mouseRelease(masks[k]);
                    robot.delay(robotDelay);
                    ++testCount;

                    countEvents = false;

                    robot.mouseRelease(masks[i]);
                    robot.delay(robotDelay);

                    robot.type(KeyEvent.VK_ESCAPE);
                    robot.delay(robotDelay);
                } //k
            } //i

            assertEquals(testCount, eventCount, "different amount of sent and received events");
            System.out.println("passed");
        } //component

        robot.waitForIdle();
        frame.dispose();
    }

    public static void main(String[] args) throws Exception {

        MultipleMouseButtonsTest test = new MultipleMouseButtonsTest();
        test.doTest();
    }
}
