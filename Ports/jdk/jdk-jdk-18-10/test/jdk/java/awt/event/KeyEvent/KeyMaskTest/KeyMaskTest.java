/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043126
 * @summary Check whether KeyEvent.getModifiers() returns correct modifiers
 *          when Ctrl, Alt or Shift keys are pressed.
 *
 * @library /lib/client/ ../../helpers/lwcomponents/
 * @library /test/lib
 * @build LWComponent
 * @build LWButton
 * @build LWList
 * @build ExtendedRobot
 * @run main/timeout=600 KeyMaskTest
 */


import java.awt.*;
import java.awt.event.InputEvent;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.ArrayList;

import static jdk.test.lib.Asserts.*;

import test.java.awt.event.helpers.lwcomponents.LWButton;
import test.java.awt.event.helpers.lwcomponents.LWList;



public class KeyMaskTest extends KeyAdapter {

    Frame frame;

    Button    button;
    LWButton  buttonLW;
    TextField textField;
    TextArea  textArea;
    List      list;
    LWList    listLW;

    int buttonPressedNumber;
    int buttonReleasedNumber;

    ExtendedRobot robot;

    private final static int robotDelay = 1500;
    private final static int waitDelay  = 3500;

    final Object lock;

    private boolean keyPressReceived = false;
    private int keyCode = -1;

    KeyMaskTest() throws Exception {
        lock = new Object();
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait( this::createGUI );
    }

    public void createGUI() {

        frame = new Frame();
        frame.setTitle("KeyMaskTest");
        frame.setLayout(new GridLayout(1, 6));

        button = new Button();
        button.addKeyListener(this);
        frame.add(button);

        buttonLW = new LWButton();
        buttonLW.addKeyListener(this);
        frame.add(buttonLW);

        textField = new TextField(5);
        textField.addKeyListener(this);
        frame.add(textField);

        textArea = new TextArea(5, 5);
        textArea.addKeyListener(this);
        frame.add(textArea);

        list = new List();
        for (int i = 1; i <= 5; ++i) { list.add("item " + i); }
        list.addKeyListener(this);
        frame.add(list);

        listLW = new LWList();
        for (int i = 1; i <= 5; ++i) { listLW.add("item " + i); }
        listLW.addKeyListener(this);
        frame.add(listLW);


        frame.setBackground(Color.gray);
        frame.setSize(500, 100);
        frame.setVisible(true);
        frame.toFront();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        keyPressReceived = true;

        int code = e.getKeyCode();

        assertEQ(code, keyCode, "wrong key code");

        int mask = 0;

        if (code == KeyEvent.VK_SHIFT) {
            mask = InputEvent.SHIFT_MASK;
        } else if (code == KeyEvent.VK_CONTROL) {
            mask = InputEvent.CTRL_MASK;
        } else if (code == KeyEvent.VK_ALT) {
            mask = InputEvent.ALT_MASK;
        } else if (code == KeyEvent.VK_META) {
            mask = InputEvent.META_MASK;
        }

        int mod = e.getModifiers() & mask;
        assertEQ(mod, mask, "invalid key mask");

        synchronized (lock) { lock.notifyAll(); }
    }


    void doTest() throws Exception {

        ArrayList<Component> components = new ArrayList();
        components.add(button);
        components.add(buttonLW);
        components.add(textField);
        components.add(textArea);
        components.add(list);
        components.add(listLW);

        int keys[];
        String OS = System.getProperty("os.name").toLowerCase();
        System.out.println(OS);
        if (OS.contains("os x")) {
            keys = new int[] {KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META};
        } else {
            keys = new int[] {KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT};
        }

        for (Component c: components) {

            System.out.print(c.getClass().getName() + ": ");

            Point origin = c.getLocationOnScreen();
            int xc = origin.x + c.getWidth() / 2;
            int yc = origin.y + c.getHeight() / 2;
            Point center = new Point(xc, yc);

            robot.delay(robotDelay);
            robot.glide(origin, center);
            robot.click();
            robot.delay(robotDelay);

            for (int k = 0; k < keys.length; ++k) {

                keyPressReceived = false;

                keyCode = keys[k];

                robot.type(keyCode);

                robot.delay(robotDelay);

                if (!keyPressReceived) {
                    synchronized (lock) {
                        try {
                            lock.wait(waitDelay);
                        } catch (InterruptedException e) {}
                    }
                }

                assertTrue(keyPressReceived, "key press event was not received");
            }

            System.out.println("passed");
        }

        robot.waitForIdle();
        frame.dispose();
    }


    public static void main(String[] args) throws Exception {

        KeyMaskTest test = new KeyMaskTest();
        test.doTest();
    }
}
