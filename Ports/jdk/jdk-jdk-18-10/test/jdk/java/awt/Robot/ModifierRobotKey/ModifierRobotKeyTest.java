/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static jdk.test.lib.Asserts.assertNull;
import static jdk.test.lib.Asserts.assertTrue;

/*
 * @test
 * @bug 8155742
 * @key headful
 * @summary Make sure that modifier key mask is set when robot press
 *          some key with one or more modifiers.
 * @library /lib/client
 * @library /test/lib
 * @build ExtendedRobot
 * @run main ModifierRobotKeyTest
 */

public class ModifierRobotKeyTest extends KeyAdapter {

    private boolean focusGained = false;
    private boolean startTest = false;
    private ExtendedRobot robot;
    private Frame frame;
    private Canvas canvas;

    private volatile boolean tempPress = false;

    private int[] textKeys, modifierKeys, inputMasks;
    private boolean[] modifierStatus, textStatus;

    private final static int WAIT_DELAY = 5000;
    private final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        ModifierRobotKeyTest test = new ModifierRobotKeyTest();
        test.doTest();
    }

    public ModifierRobotKeyTest() throws Exception {
        modifierKeys =  new int[4];
        modifierKeys[0] = KeyEvent.VK_SHIFT;
        modifierKeys[1] = KeyEvent.VK_CONTROL;
        modifierKeys[2] = KeyEvent.VK_ALT;
        modifierKeys[3] = KeyEvent.VK_ALT_GRAPH;

        inputMasks = new int[4];
        inputMasks[0] =  InputEvent.SHIFT_MASK;
        inputMasks[1] =  InputEvent.CTRL_MASK;
        inputMasks[2] =  InputEvent.ALT_MASK;
        inputMasks[3] =  InputEvent.ALT_GRAPH_MASK;

        modifierStatus = new boolean[modifierKeys.length];

        textKeys = new int[2];
        textKeys[0] = KeyEvent.VK_A;

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("os x"))
            textKeys[1] = KeyEvent.VK_K;
        else
            textKeys[1] = KeyEvent.VK_I;

        textStatus = new boolean[textKeys.length];

        EventQueue.invokeAndWait( () -> { initializeGUI(); });
    }

    public void keyPressed(KeyEvent event) {
        synchronized (lock) {
            tempPress = true;
            lock.notifyAll();

            if (! startTest) {
                return;
            }
            for (int x = 0; x < inputMasks.length; x++) {
                if ((event.getModifiers() & inputMasks[x]) != 0) {
                    System.out.println("Modifier set: " +
                                      event.getKeyModifiersText(inputMasks[x]));
                    modifierStatus[x] = true;
                }
            }
            for (int x = 0; x < textKeys.length; x++) {
                if (event.getKeyCode() == textKeys[x]) {
                    System.out.println("Text set: " +
                                                 event.getKeyText(textKeys[x]));
                    textStatus[x] = true;
                }
            }
        }
    }

    private void initializeGUI() {
        frame = new Frame("Test frame");
        canvas = new Canvas();
        canvas.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) { focusGained = true; }
        });
        canvas.addKeyListener(this);
        frame.setLayout(new BorderLayout());
        frame.add(canvas);
        frame.setBounds(200, 200, 200, 200);
        frame.setVisible(true);
    }

    public void doTest() throws Exception {
        robot = new ExtendedRobot();
        robot.setAutoDelay(20);
        robot.waitForIdle();

        robot.mouseMove((int) frame.getLocationOnScreen().getX() +
                                                    frame.getSize().width / 2,
                        (int) frame.getLocationOnScreen().getY() +
                                                    frame.getSize().height / 2);
        robot.click(MouseEvent.BUTTON1_MASK);
        robot.waitForIdle();
        assertTrue(focusGained, "FAIL: Canvas gained focus!");

        String error = null;
        exit1:
        for (int i = 0; i < modifierKeys.length; i++) {
            for (int j = 0; j < textKeys.length; j++) {
                if (error != null) {
                    break exit1;
                }
                robot.waitForIdle(100);
                synchronized (lock) {
                    tempPress = false;
                    robot.keyPress(modifierKeys[i]);
                    lock.wait(WAIT_DELAY);
                }
                if (!tempPress) {
                    error ="FAIL: keyPressed triggered for i=" + i;
                }

                synchronized (lock) {
                    resetStatus();
                    startTest = true;
                    robot.keyPress(textKeys[j]);
                    lock.wait(WAIT_DELAY);
                }

                if (!(modifierStatus[i] && textStatus[j])) {
                    error = "FAIL: KeyEvent not proper!"+
                            "Key checked: i=" + i + "; j=" + j+
                            "ModifierStatus = " + modifierStatus[i]+
                            "TextStatus = " + textStatus[j];
                }

                startTest = false;
                robot.keyRelease(textKeys[j]);
                robot.keyRelease(modifierKeys[i]);
            }
        }

        exit2:
        for (int i = 0; i < modifierKeys.length; i++) {
            for (int j = i + 1; j < modifierKeys.length; j++) {
                for (int k = 0; k < textKeys.length; k++) {
                    if (error != null) {
                        break exit2;
                    }
                    robot.waitForIdle(100);
                    synchronized (lock) {
                        tempPress = false;
                        robot.keyPress(modifierKeys[i]);
                        lock.wait(WAIT_DELAY);
                    }

                    if (!tempPress) {
                        error = "FAIL: MultiKeyTest: keyPressed " +
                                                         "triggered for i=" + i;
                    }

                    synchronized (lock) {
                        tempPress = false;
                        robot.keyPress(modifierKeys[j]);
                        lock.wait(WAIT_DELAY);
                    }
                    if (!tempPress) {
                        error = "FAIL: MultiKeyTest keyPressed " +
                                                         "triggered for j=" + j;
                    };

                    synchronized (lock) {
                        resetStatus();
                        startTest = true;
                        robot.keyPress(textKeys[k]);
                        lock.wait(WAIT_DELAY);
                    }
                    if (!(modifierStatus[i] && modifierStatus[j]
                                                              && textStatus[k]))
                    {
                        error = "FAIL: KeyEvent not proper!" +
                               "Key checked: i=" + i + "; j=" + j + "; k=" + k +
                               "Modifier1Status = " + modifierStatus[i] +
                               "Modifier2Status = " + modifierStatus[j] +
                               "TextStatus = " + textStatus[k];
                    }

                    startTest = false;
                    robot.keyRelease(textKeys[k]);
                    robot.keyRelease(modifierKeys[j]);
                    robot.keyRelease(modifierKeys[i]);
                }
            }
        }

        frame.dispose();
        assertNull(error, error);
    }

    private void resetStatus() {
        for (int i = 0; i < modifierStatus.length; i++) {
            modifierStatus[i] = false;
        }
        for (int i = 0; i < textStatus.length; i++) {
            textStatus[i] = false;
        }
    }

}
