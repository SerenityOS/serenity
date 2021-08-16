/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @summary Test that it is possible to type "Alt code" on Windows.
 * @requires (os.family == "windows")
 * @library /lib/client
 * @build ExtendedRobot
 * @run main AltPlusNumberKeyCombinationsTest
 */

public class AltPlusNumberKeyCombinationsTest {

    private Frame frame;
    private TextField tf;
    private TextArea ta;

    private static int delay = 500;
    private static String euroChar = "\u20AC";
    private static String accChar = "\u00E3";

    private boolean passed = true;
    private ExtendedRobot robot;

    public AltPlusNumberKeyCombinationsTest() {
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    initializeGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }
    }

    private void initializeGUI() {
        frame = new Frame("AltPlusNumberKeyCombinationsTest");
        frame.setLayout(new FlowLayout());

        tf = new TextField(15);
        frame.add(tf);

        ta = new TextArea(8, 20);
        frame.add(ta);

        frame.setSize(250,400);
        frame.setVisible(true);
    }


    private void doTest() throws Exception {
        robot = new ExtendedRobot();
        robot.setAutoDelay(100);

        robot.waitForIdle(delay);

        robot.mouseMove((int) ta.getLocationOnScreen().x + ta.getSize().width / 2,
                        (int) ta.getLocationOnScreen().y + ta.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_RIGHT);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle(delay);

        if (! "".equals(ta.getText())) {
            System.err.println("FAIL: Symbol typed in the text area when ALT + RIGHT ARROW keys typed");
            passed = false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    ta.setText("");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }

        // getLockingKeyState works on Windows;
        // Alt code only make sense on Windows;
        // so don't check availability of this and just use it.
        if( Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) ) {
            robot.keyPress(KeyEvent.VK_NUM_LOCK);
            robot.keyRelease(KeyEvent.VK_NUM_LOCK);
        }

        robot.mouseMove((int) tf.getLocationOnScreen().x + tf.getSize().width / 2,
                        (int) tf.getLocationOnScreen().y + tf.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_NUMPAD0);
        robot.keyRelease(KeyEvent.VK_NUMPAD0);
        robot.keyPress(KeyEvent.VK_NUMPAD1);
        robot.keyRelease(KeyEvent.VK_NUMPAD1);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD8);
        robot.keyRelease(KeyEvent.VK_NUMPAD8);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle(delay);

        if (! euroChar.equals(tf.getText())) {
            System.err.println("FAIL: Euro symbol not typed in the text field when " +
                               "ALT + NUMPAD 1,2,8 keys typed");
            passed = false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    tf.setText("");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }

        robot.mouseMove((int) ta.getLocationOnScreen().x + ta.getSize().width / 2,
                        (int) ta.getLocationOnScreen().y + ta.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_NUMPAD0);
        robot.keyRelease(KeyEvent.VK_NUMPAD0);
        robot.keyPress(KeyEvent.VK_NUMPAD1);
        robot.keyRelease(KeyEvent.VK_NUMPAD1);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD8);
        robot.keyRelease(KeyEvent.VK_NUMPAD8);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle(delay);

        if (! euroChar.equals(ta.getText())) {
            System.err.println("FAIL: Euro symbol not typed in the text area when " +
                               "ALT + NUMPAD 1,2,8 keys typed");
            passed = false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    ta.setText("");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }

        robot.mouseMove((int) tf.getLocationOnScreen().x + tf.getSize().width / 2,
                        (int) tf.getLocationOnScreen().y + tf.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_NUMPAD0);
        robot.keyRelease(KeyEvent.VK_NUMPAD0);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD7);
        robot.keyRelease(KeyEvent.VK_NUMPAD7);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle(delay);

        if (! accChar.equals(tf.getText())) {
            System.err.println("FAIL: Symbol not typed in the text field when " +
                               "ALT + NUMPAD 2,2,7 keys typed");
            passed = false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    tf.setText("");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }

        robot.mouseMove((int) ta.getLocationOnScreen().x + ta.getSize().width / 2,
                        (int) ta.getLocationOnScreen().y + ta.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_NUMPAD0);
        robot.keyRelease(KeyEvent.VK_NUMPAD0);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD2);
        robot.keyRelease(KeyEvent.VK_NUMPAD2);
        robot.keyPress(KeyEvent.VK_NUMPAD7);
        robot.keyRelease(KeyEvent.VK_NUMPAD7);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.waitForIdle(delay);

        if (! accChar.equals(ta.getText())) {
            System.err.println("FAIL: Symbol not typed in the text field when " +
                               "ALT + NUMPAD 2,2,7 keys typed");
            passed = false;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    ta.setText("");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed;", e.getCause());
        }

        if (! passed) {
            System.err.println("Test failed");
            captureScreenAndSave();
            throw new RuntimeException("Test failed");
        } else {
            System.out.println("Test passed");
        }
    }

    public static void main(String[] args) {
        if (System.getProperty("os.name").indexOf("Win") == -1) {
            System.out.println("This test is supposed to be run only on Windows! Marking as passed..");
            return;
        }
        try {
            AltPlusNumberKeyCombinationsTest test = new AltPlusNumberKeyCombinationsTest();
            test.doTest();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Fail; "+e.getMessage());
        }
    }

    /**
     * Do screen capture and save it as image
     */
    private static void captureScreenAndSave() {

        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle rectangle = new Rectangle(0, 0, screenSize.width, screenSize.height);
            System.out.println("About to screen capture - " + rectangle);
            java.awt.image.BufferedImage image = robot.createScreenCapture(rectangle);
            javax.imageio.ImageIO.write(image, "jpg", new java.io.File("ScreenImage.jpg"));
            robot.delay(3000);
        } catch (Throwable t) {
            System.out.println("WARNING: Exception thrown while screen capture!");
            t.printStackTrace();
        }
    }
}
