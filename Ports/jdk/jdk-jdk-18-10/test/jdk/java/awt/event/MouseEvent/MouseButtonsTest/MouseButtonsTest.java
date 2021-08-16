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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import test.java.awt.event.helpers.lwcomponents.LWButton;
import test.java.awt.event.helpers.lwcomponents.LWList;

import java.util.ArrayList;

import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @key headful
 * @bug 8043126
 * @summary Check whether getButton() returns correct mouse button
 *          number when the mouse buttons are pressed and getModifiers()
 *          returns correct modifiers
 *
 * @library /lib/client/ ../../helpers/lwcomponents/
 * @library /test/lib
 * @build LWComponent
 * @build LWButton
 * @build LWList
 * @build ExtendedRobot
 * @run main/timeout=600 MouseButtonsTest
 */

public class MouseButtonsTest implements MouseListener {

    private Frame frame;

    private Button    button;
    private LWButton  buttonLW;
    private TextField textField;
    private TextArea  textArea;
    private List      list;
    private LWList    listLW;

    private int buttonPressedNumber = 0;
    private int buttonReleasedNumber = 0;
    private int modifiers = 0;


    private final ExtendedRobot robot;

    private final static int robotDelay = 1000;
    private final static int waitDelay  = 3500;

    private boolean released = false;
    private boolean pressed = false;
    private final Object lock;


    MouseButtonsTest() throws Exception {
        lock = new Object();
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait( this::createGUI );
    }

    public void createGUI() {

        frame = new Frame();
        frame.setTitle("MouseButtonsTest");
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
    public void mousePressed(MouseEvent e) {

        assertFalse(e.getButton() == MouseEvent.NOBUTTON, "invalid button");

        buttonPressedNumber = e.getButton();
        modifiers = e.getModifiers();

        pressed = true;

        synchronized (lock) {
            try {
                lock.notifyAll();
            } catch (Exception ex) {}
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        assertFalse(e.getButton() == MouseEvent.NOBUTTON, "invalid button");

        buttonReleasedNumber = e.getButton();
        modifiers = e.getModifiers();

        released = true;

        synchronized (lock) {
            try {
                lock.notifyAll();
            } catch (Exception ex) {}
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}


    void doTest() throws Exception {

        int masks[] = new int[]{
            InputEvent.BUTTON1_MASK, InputEvent.BUTTON2_MASK, InputEvent.BUTTON3_MASK};

        int buttons[] = new int[]{
            MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3};

        ArrayList<Component> components = new ArrayList();
        components.add(button);
        components.add(buttonLW);
        components.add(textField);
        components.add(textArea);
        components.add(list);
        components.add(listLW);

        for (Component c: components) {

            System.out.println(c.getClass().getName() + ":");

            Point origin = c.getLocationOnScreen();
            int xc = origin.x + c.getWidth() / 2;
            int yc = origin.y + c.getHeight() / 2;
            Point center = new Point(xc, yc);

            robot.delay(robotDelay);
            robot.glide(origin, center);
            robot.click();
            robot.delay(robotDelay);

            for (int i = 0; i < masks.length; ++i) {

                pressed  = false;
                released = false;

                int mask = masks[i];
                robot.mousePress(mask);
                robot.delay(robotDelay);

                if (!pressed) {
                    synchronized (lock) {
                        try {
                            lock.wait(waitDelay);
                        } catch (InterruptedException ex) {}
                    }
                }

                assertTrue(pressed, "mouse press event was not received");
                assertEQ((modifiers & mask), mask, "invalid mask modifiers");

                robot.mouseRelease(mask);
                robot.delay(robotDelay);

                if (!released) {
                    synchronized (lock) {
                        try {
                            lock.wait(waitDelay);
                        } catch (InterruptedException ex) {}
                    }
                }

                assertTrue(released, "mouse release event was not received");
                assertEQ((modifiers & mask), mask, "invalid mask modifiers");

                assertEquals(buttonPressedNumber,  buttons[i]);
                assertEquals(buttonReleasedNumber, buttons[i]);

                robot.type(KeyEvent.VK_ESCAPE);
                robot.delay(robotDelay);

                System.out.println("button " + buttons[i] + " - passed");
            }
        }

        robot.waitForIdle();
        frame.dispose();
    }


    public static void main(String[] args) throws Exception {

        MouseButtonsTest test = new MouseButtonsTest();
        test.doTest();
    }
}
