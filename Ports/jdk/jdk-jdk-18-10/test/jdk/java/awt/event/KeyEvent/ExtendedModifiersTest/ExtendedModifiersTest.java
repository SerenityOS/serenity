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
 * @bug 8043126 8145116
 * @summary Check whether
 *          1. correct extended modifiers are returned
 *             by KeyEvent.getModifiersEx()
 *          2. InputEvent.getModifiersExText() returns
 *             correct extended modifier keys description
 *
 * @library /lib/client/ ../../helpers/lwcomponents/
 * @library /test/lib
 * @build LWComponent
 * @build LWButton
 * @build LWList
 * @build ExtendedRobot
 * @run main/timeout=600 ExtendedModifiersTest
 */
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import static jdk.test.lib.Asserts.*;
import test.java.awt.event.helpers.lwcomponents.LWButton;
import test.java.awt.event.helpers.lwcomponents.LWList;

public class ExtendedModifiersTest implements KeyListener {

    Frame frame;
    Button button;
    LWButton buttonLW;
    TextField textField;
    TextArea textArea;
    List list;
    LWList listLW;

    private final ExtendedRobot robot;
    private static final int WAIT_DELAY = 5000;
    private static final int KEY_DELAY = 500;
    private final Object lock;

    private boolean keyPressedFlag;
    private int modifiersEx = 0;
    private String exText = "";

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            return;
        }
        modifiersEx = e.getModifiersEx();
        exText = InputEvent.getModifiersExText(modifiersEx);
        keyPressedFlag = true;

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void createGUI() {

        frame = new Frame();
        frame.setTitle("ExtendedModifiersTest");
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
        for (int i = 1; i <= 5; ++i) {
            list.add("item " + i);
        }
        list.addKeyListener(this);
        frame.add(list);

        listLW = new LWList();
        for (int i = 1; i <= 5; ++i) {
            listLW.add("item " + i);
        }
        listLW.addKeyListener(this);
        frame.add(listLW);

        frame.setBackground(Color.gray);
        frame.setSize(500, 100);
        frame.setVisible(true);
        frame.toFront();
    }

    public ExtendedModifiersTest() throws Exception {
        lock = new Object();
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::createGUI);
    }

    private void runScenario(int keys[], int expectedMask) {
        if (keys.length < 1) {
            return;
        }

        for (int k = 0; k < keys.length; ++k) {

            keyPressedFlag = false;
            robot.keyPress(keys[k]);
            robot.delay(KEY_DELAY);

            if (!keyPressedFlag) {
                synchronized (lock) {
                    try {
                        lock.wait(WAIT_DELAY);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (!keyPressedFlag) {
                robot.keyRelease(keys[k]);
                robot.delay(KEY_DELAY);
                assertTrue(false, "key press event was not received");
            }
        }

        int modEx = modifiersEx & expectedMask;

        for (int k = keys.length - 1; k >= 0; --k) {
            robot.keyRelease(keys[k]);
            robot.delay(KEY_DELAY);
        }

        assertEQ(expectedMask, modEx, "invalid extended modifiers");

        for (int k = 0; k < keys.length; ++k) {
            String keyText = KeyEvent.getKeyText(keys[k]).toLowerCase();
            assertTrue(exText.toLowerCase().contains(keyText),
                    "invalid extended modifier keys description");
        }

        System.out.println(exText + " : passed");

        robot.type(KeyEvent.VK_ESCAPE);
        robot.waitForIdle();
    }

    private void doTest() throws Exception {

        ArrayList<Component> components = new ArrayList();
        components.add(button);
        components.add(buttonLW);
        components.add(textField);
        components.add(textArea);
        components.add(list);
        components.add(listLW);

        String OS = System.getProperty("os.name").toLowerCase();
        System.out.println(OS);

        for (Component c : components) {

            String className = c.getClass().getName();
            System.out.println("component class : " + className);

            Point origin = c.getLocationOnScreen();
            int xc = origin.x + c.getWidth() / 2;
            int yc = origin.y + c.getHeight() / 2;
            Point center = new Point(xc, yc);

            robot.waitForIdle();
            robot.glide(origin, center);
            robot.click();
            robot.waitForIdle();

            // 1. shift + control
            runScenario(new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL},
                    InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

            // 2. alt + shift + control
            runScenario(new int[]{KeyEvent.VK_ALT, KeyEvent.VK_SHIFT,
                KeyEvent.VK_CONTROL}, InputEvent.ALT_DOWN_MASK
                    | InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

            // 3. shift
            runScenario(new int[]{KeyEvent.VK_SHIFT},
                    InputEvent.SHIFT_DOWN_MASK);

            // 4. alt + control
            runScenario(new int[]{KeyEvent.VK_ALT, KeyEvent.VK_CONTROL},
                    InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

            // 5. shift + alt
            runScenario(new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_ALT},
                    InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

            if (OS.contains("os x")) {
                // 6. meta
                runScenario(new int[]{KeyEvent.VK_META},
                        InputEvent.META_DOWN_MASK);

                // 7. shift + ctrl + alt + meta
                runScenario(new int[]{KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL,
                    KeyEvent.VK_ALT, KeyEvent.VK_META},
                        InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK
                        | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK);

                // 8. meta + shift + ctrl
                runScenario(new int[]{KeyEvent.VK_META, KeyEvent.VK_SHIFT,
                    KeyEvent.VK_CONTROL}, InputEvent.META_DOWN_MASK
                      | InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

                // 9. meta + shift + alt
                runScenario(new int[]{KeyEvent.VK_META, KeyEvent.VK_SHIFT,
                    KeyEvent.VK_ALT}, InputEvent.META_DOWN_MASK
                      | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

                // 10. meta + ctrl + alt
                runScenario(new int[]{KeyEvent.VK_META, KeyEvent.VK_CONTROL,
                    KeyEvent.VK_ALT}, InputEvent.META_DOWN_MASK
                      | InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
            }
        }

        robot.waitForIdle();
        frame.dispose();
    }

    public static void main(String[] args) throws Exception {
        ExtendedModifiersTest test = new ExtendedModifiersTest();
        test.doTest();
    }
}
