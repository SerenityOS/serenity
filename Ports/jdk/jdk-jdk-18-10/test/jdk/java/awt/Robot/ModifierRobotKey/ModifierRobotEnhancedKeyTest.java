/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertTrue;

/*
 * NOTE: this is no intentionally a manual test (i.e. has no test tag) because
 * even on Windows, the various tested key-combination may get partially
 * intercepted by other applications and this can leave the whole system in an
 * inconsistent state. For example on my Windows machine with Intel Graphics
 * the key combinations "Ctl-Alt-F11" and "Ctl-Alt-F12" triggers some Intel
 * Graphics utilities by default. If the test is run in such an environment,
 * some key events of the test are intercepted by those utilities with the
 * result that the test fails and no more keyboard input will be possible at
 * all.
 *
 * To execute the test add a '@' before the 'test' keyword below to make it a tag.
 */

/*
 * test 8155742
 *
 * @summary Make sure that modifier key mask is set when robot press
 *          some key with one or more modifiers.
 * @library /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @key headful
 * @run main/timeout=600 ModifierRobotEnhancedKeyTest
 */

public class ModifierRobotEnhancedKeyTest extends KeyAdapter {

    private boolean focusGained = false;
    private boolean startTest = false;
    private ExtendedRobot robot;
    private Frame frame;
    private Canvas canvas;

    private volatile boolean tempPress = false;

    private int[] textKeys, modifierKeys, inputMasks;
    private boolean[] modifierStatus, textStatus;

    private final static int waitDelay = 5000;
    private Object tempLock = new Object();
    private Object keyLock = new Object();

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows")) {
            System.out.println("*** this test is for windows only because some of the tested key combinations " +
                               "might be caught by the os and therefore don't reach the canvas ***");
            return;
        }

        ModifierRobotEnhancedKeyTest test = new ModifierRobotEnhancedKeyTest();
        test.doTest();
    }

    public ModifierRobotEnhancedKeyTest() throws Exception {
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

        textKeys = new int[6];
        textKeys[0] = KeyEvent.VK_A;
        textKeys[1] = KeyEvent.VK_S;
        textKeys[2] = KeyEvent.VK_DELETE;
        textKeys[3] = KeyEvent.VK_HOME;
        textKeys[4] = KeyEvent.VK_F12;
        textKeys[5] = KeyEvent.VK_LEFT;

        textStatus = new boolean[textKeys.length];

        EventQueue.invokeAndWait( () -> { initializeGUI(); });
    }

    public void keyPressed(KeyEvent event) {

        tempPress = true;
        synchronized (tempLock) { tempLock.notifyAll(); }

        if (! startTest) {
            return;
        }
        for (int x = 0; x < inputMasks.length; x++) {
            if ((event.getModifiers() & inputMasks[x]) != 0) {
                System.out.println("Modifier set: " + event.getKeyModifiersText(inputMasks[x]));
                modifierStatus[x] = true;
            }
        }
        for (int x = 0; x < textKeys.length; x++) {
            if (event.getKeyCode() == textKeys[x]) {
                System.out.println("Text set: " + event.getKeyText(textKeys[x]));
                textStatus[x] = true;
            }
        }

        synchronized (keyLock) { keyLock.notifyAll(); }
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
        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    public void doTest() throws Exception {
        robot = new ExtendedRobot();

        robot.mouseMove((int) frame.getLocationOnScreen().getX() + frame.getSize().width / 2,
                        (int) frame.getLocationOnScreen().getY() + frame.getSize().height / 2);
        robot.click(MouseEvent.BUTTON1_MASK);
        robot.waitForIdle();

        assertTrue(focusGained, "FAIL: Canvas gained focus!");

        for (int i = 0; i < modifierKeys.length; i++) {
            for (int j = 0; j < textKeys.length; j++) {
                tempPress = false;
                robot.keyPress(modifierKeys[i]);
                robot.waitForIdle();
                if (! tempPress) {
                    synchronized (tempLock) { tempLock.wait(waitDelay); }
                }
                assertTrue(tempPress, "FAIL: keyPressed triggered for i=" + i);

                resetStatus();
                startTest = true;
                robot.keyPress(textKeys[j]);
                robot.waitForIdle();
                if (! modifierStatus[i] || ! textStatus[j]) {
                    synchronized (keyLock) { keyLock.wait(waitDelay); }
                }


                assertTrue(modifierStatus[i] && textStatus[j],
                        "FAIL: KeyEvent not proper!"+
                        "Key checked: i=" + i + "; j=" + j+
                        "ModifierStatus = " + modifierStatus[i]+
                        "TextStatus = " + textStatus[j]);
                startTest = false;
                robot.keyRelease(textKeys[j]);
                robot.waitForIdle();
                robot.keyRelease(modifierKeys[i]);
                robot.waitForIdle();
            }
        }

        for (int i = 0; i < modifierKeys.length; i++) {
            for (int j = i + 1; j < modifierKeys.length; j++) {
                for (int k = 0; k < textKeys.length; k++) {
                    tempPress = false;
                    robot.keyPress(modifierKeys[i]);
                    robot.waitForIdle();
                    if (! tempPress) {
                        synchronized (tempLock) { tempLock.wait(waitDelay); }
                    }

                    assertTrue(tempPress, "FAIL: MultiKeyTest: keyPressed triggered for i=" + i);

                    tempPress = false;
                    robot.keyPress(modifierKeys[j]);
                    robot.waitForIdle();
                    if (! tempPress) {
                        synchronized (tempLock) { tempLock.wait(waitDelay); }
                    }
                    assertTrue(tempPress, "FAIL: MultiKeyTest keyPressed triggered for j=" + j);

                    resetStatus();
                    startTest = true;
                    robot.keyPress(textKeys[k]);
                    robot.waitForIdle();
                    if (! modifierStatus[i] || ! modifierStatus[j] || ! textStatus[k]) {
                        synchronized (keyLock) {
                            keyLock.wait(waitDelay);
                        }
                    }
                    assertTrue(modifierStatus[i] && modifierStatus[j] && textStatus[k],
                            "FAIL: KeyEvent not proper!"+
                            "Key checked: i=" + i + "; j=" + j + "; k=" + k+
                            "Modifier1Status = " + modifierStatus[i]+
                            "Modifier2Status = " + modifierStatus[j]+
                            "TextStatus = " + textStatus[k]);

                    startTest = false;
                    robot.keyRelease(textKeys[k]);
                    robot.waitForIdle();
                    robot.keyRelease(modifierKeys[j]);
                    robot.waitForIdle();
                    robot.keyRelease(modifierKeys[i]);
                    robot.waitForIdle();
                }
            }
        }

        frame.dispose();
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
