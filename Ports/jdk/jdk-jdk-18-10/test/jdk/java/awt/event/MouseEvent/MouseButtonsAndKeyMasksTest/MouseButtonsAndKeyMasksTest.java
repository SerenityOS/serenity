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


import java.awt.*;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import static jdk.test.lib.Asserts.*;


import test.java.awt.event.helpers.lwcomponents.LWButton;
import test.java.awt.event.helpers.lwcomponents.LWList;


/*
 * @test
 * @key headful
 * @bug 8043126
 * @summary Check whether MouseEvent.getModifiers(), MouseEvent.getModifiersEx()
 *          and KeyEvent.getModifiers() return correct modifiers when pressing
 *          keys Ctrl, Alt, Shift, Meta and mouse buttons sequentially
 *
 * @library /lib/client/ ../../helpers/lwcomponents/
 * @library /test/lib
 * @build LWComponent
 * @build LWButton
 * @build LWList
 * @build ExtendedRobot
 * @run main/timeout=600 MouseButtonsAndKeyMasksTest
 */

public class MouseButtonsAndKeyMasksTest implements MouseListener, KeyListener {

    Frame frame;

    Button    button;
    LWButton  buttonLW;
    TextField textField;
    TextArea  textArea;
    List      list;
    LWList    listLW;

    ExtendedRobot robot;

    private final static int robotDelay = 1500;
    private final static int   keyDelay =  500;
    private final static int  waitDelay = 5000;

    int modMouse = 0, modMouseEx = 0, modKey = 0, modAction = 0;

    boolean mousePressFired = false;
    boolean keyPressFired = false;

    final Object lock;

    MouseButtonsAndKeyMasksTest() throws Exception {
        lock = new Object();
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait( this::createGUI );
    }

    public void createGUI() {

        frame = new Frame();
        frame.setTitle("MouseButtonsAndKeysTest");
        frame.setLayout(new GridLayout(1, 6));

        button = new Button();
        button.addKeyListener(this);
        button.addMouseListener(this);
        frame.add(button);

        buttonLW = new LWButton();
        buttonLW.addKeyListener(this);
        buttonLW.addMouseListener(this);
        frame.add(buttonLW);

        textField = new TextField(5);
        textField.addKeyListener(this);
        textField.addMouseListener(this);
        frame.add(textField);

        textArea = new TextArea(5, 5);
        textArea.addKeyListener(this);
        textArea.addMouseListener(this);
        frame.add(textArea);

        list = new List();
        for (int i = 1; i <= 5; ++i) { list.add("item " + i); }
        list.addKeyListener(this);
        list.addMouseListener(this);
        frame.add(list);

        listLW = new LWList();
        for (int i = 1; i <= 5; ++i) { listLW.add("item " + i); }
        listLW.addKeyListener(this);
        listLW.addMouseListener(this);
        frame.add(listLW);


        frame.setBackground(Color.gray);
        frame.setSize(500, 80);
        frame.setVisible(true);
        frame.toFront();
    }


    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {

        modMouse = e.getModifiers();
        modMouseEx = e.getModifiersEx();
        mousePressFired = true;
        synchronized (lock) { lock.notifyAll(); }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}


    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { return; }

        keyPressFired = true;
        modKey = e.getModifiers();

        synchronized (lock) { lock.notifyAll(); }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    void doTest() throws Exception {

        int buttons[] = new int[]{
            InputEvent.BUTTON1_MASK, InputEvent.BUTTON2_MASK, InputEvent.BUTTON3_MASK};

        int buttonsEx[] = new int[]{
            InputEvent.BUTTON1_DOWN_MASK, InputEvent.BUTTON2_DOWN_MASK, InputEvent.BUTTON3_DOWN_MASK};

        String OS = System.getProperty("os.name").toLowerCase();
        System.out.println(OS);

        int keyMods[], keyModsEx[], keys[];


        if (OS.contains("linux")) {
            keyMods = new int[]{InputEvent.SHIFT_MASK, InputEvent.CTRL_MASK};
            keyModsEx = new int[]{InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK};
            keys = new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL};
        } else if (OS.contains("os x")) {
            keyMods = new int[]{
                InputEvent.SHIFT_MASK, InputEvent.CTRL_MASK, InputEvent.ALT_MASK, InputEvent.META_MASK};
            keyModsEx = new int[]{
                InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.META_DOWN_MASK};
            keys = new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META};
        } else {
            keyMods = new int[]{
                InputEvent.SHIFT_MASK, InputEvent.CTRL_MASK, InputEvent.ALT_MASK};
            keyModsEx = new int[]{
                InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.ALT_DOWN_MASK};
            keys = new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT};
        }


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

            for (int b = 0; b < buttons.length; ++b) {

                int btn = buttons[b];

                for (int k = 0; k < keys.length; ++k) {

                    int key = keys[k];

                    System.out.print(KeyEvent.getKeyText(key) + " + button " + (b + 1));

                    robot.delay(robotDelay);

                    robot.keyPress(key);
                    robot.delay(keyDelay);

                    if (!keyPressFired) {
                        synchronized (lock) {
                            try {
                                lock.wait(waitDelay);
                            } catch (InterruptedException ex) {}
                        }
                    }

                    if (!keyPressFired) {
                        robot.keyRelease(key);
                        assertTrue(false, "key press event was not received");
                    }

                    robot.mousePress(btn);
                    robot.delay(robotDelay);

                    if (!mousePressFired) {
                        synchronized (lock) {
                            try {
                                lock.wait(waitDelay);
                            } catch (InterruptedException ex) {}
                        }
                    }

                    assertTrue(mousePressFired, "mouse press event was not received");

                    robot.mouseRelease(btn);
                    robot.delay(robotDelay);

                    // do checks
                    assertEQ(modMouse & btn, btn, "invalid mouse button mask");
                    assertEQ(modKey & keyMods[k], keyMods[k], "invalid key mask");
                    assertEQ(buttonsEx[b] | keyModsEx[k], modMouseEx, "invalid extended modifiers");

                    mousePressFired  = false;
                    keyPressFired    = false;

                    robot.keyRelease(key);
                    robot.delay(keyDelay);

                    robot.type(KeyEvent.VK_ESCAPE);

                    robot.delay(robotDelay);

                    System.out.println(" - passed");
                }
            }
        }

        robot.waitForIdle();
        frame.dispose();
    }


    public static void main(String[] args) throws Exception {

        MouseButtonsAndKeyMasksTest test = new MouseButtonsAndKeyMasksTest();
        test.doTest();
    }
}
