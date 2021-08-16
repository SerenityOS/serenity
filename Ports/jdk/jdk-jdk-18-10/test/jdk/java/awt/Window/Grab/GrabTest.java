/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 7124430
  @summary Tests that SunToolkit.grab API works
  @author anton.tarasov@oracle.com: area=awt.toolkit
  @library ../../regtesthelpers
  @modules java.desktop/sun.awt
  @build Util
  @run main GrabTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class GrabTest {
    private static Frame f;
    private static Frame f1;
    private static Frame frame;
    private static Window w;
    private static Window window1;
    private static Window window2;
    private static Button b;

    private static Robot robot;
    private static sun.awt.SunToolkit tk;

    static volatile boolean ungrabbed;
    static volatile boolean buttonPressed;
    static volatile boolean windowPressed;
    static volatile boolean framePressed;

    static volatile boolean passed = true;

    public static void main(String[] args) {

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    System.out.println(e);
                    if (e instanceof sun.awt.UngrabEvent) {
                        ungrabbed = true;
                    }
                }
            }, sun.awt.SunToolkit.GRAB_EVENT_MASK);

        f = new Frame("Frame");
        f.setBounds(0, 0, 300, 300);
        f.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    System.out.println(e);
                    framePressed = true;
                }
            });

        f1 = new Frame("OtherFrame");
        f1.setBounds(700, 100, 300, 300);

        w = new Window(f);
        w.setLayout(new FlowLayout());
        b = new Button("Press");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println(e);
                    buttonPressed = true;
                }
            });
        w.add(b);
        w.setBounds(400, 100, 300, 300);
        w.setBackground(Color.blue);
        w.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    System.out.println(e);
                    windowPressed = true;
                }
            });

        f.setVisible(true);
        w.setVisible(true);

        frame = new Frame();
        window1 = new Window(frame);
        window1.setSize(200, 200);
        window1.setLocationRelativeTo(null);
        window1.setBackground(Color.blue);

        window2 = new Window(window1);
        window2.setSize(100, 100);
        window2.setLocationRelativeTo(null);
        window2.setBackground(Color.green);

        tk = (sun.awt.SunToolkit)Toolkit.getDefaultToolkit();

        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }

        Util.waitForIdle(robot);

        test();
    }

    public static void test() {
        tk.grab(w);

        // 1. Check that button press doesn't cause ungrab
        Util.clickOnComp(b, robot);
        Util.waitForIdle(robot);
        checkAndThrow(buttonPressed, "Error: Button can not be pressed");
        if (ungrabbed) {
            passed = false;
            tk.grab(w);
            System.err.println("Failure: [1] Press inside of Window (on Button) caused ungrab");
        }

        // 2. Check that press on the window itself doesn't cause ungrab
        Util.clickOnComp(w, robot);
        Util.waitForIdle(robot);
        checkAndThrow(windowPressed, "Error: Window can't be pressed");
        if (ungrabbed) {
            passed = false;
            tk.grab(w);
            System.err.println("Failure: [2] Press inside of Window caused ungrab");
        }

        // 3. Check that press on the frame causes ungrab, event must be dispatched
        Util.clickOnComp(f, robot);
        Util.waitForIdle(robot);
        checkAndThrow(framePressed, "Error: Frame can't be pressed");
        if (!ungrabbed) {
            passed = false;
            System.err.println("Failure: [3] Press inside of Frame didn't cause ungrab");
        }
        ungrabbed = false;
        tk.grab(w);

        // 4. Check that press on the frame's title causes ungrab
        Util.clickOnTitle(f, robot);
        Util.waitForIdle(robot);
        if (!ungrabbed) {
            passed = false;
            System.err.println("Failure: [4] Press inside of Frame's title didn't cause ungrab");
        }
        ungrabbed = false;
        tk.grab(w);


        // 5. Check that press on the other frame's title causes ungrab
        f1.setVisible(true);
        Util.waitForIdle(robot);
        Util.clickOnTitle(f1, robot);
        if (!ungrabbed) {
            passed = false;
            System.err.println("Failure: [5] Press inside of other Frame's title didn't cause ungrab");
        }
        f.requestFocus(); // restore focus
        Util.waitForIdle(robot);
        if (!f.hasFocus()) {
            System.err.println("Error: Frame can't be focused");
        }
        ungrabbed = false;
        tk.grab(w);


        // 6. Check that press on the outside area causes ungrab
        Point loc = f.getLocationOnScreen();
        robot.mouseMove(loc.x + 100, loc.y + f.getSize().height + 10);
        Util.waitForIdle(robot);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.waitForIdle(robot);
        if (!ungrabbed) {
            passed = false;
            System.err.println("Failure: [6] Press on the outside area didn't cause ungrab");
        }
        ungrabbed = false;
        tk.grab(w);


        // 7. Check that disposing the window causes ungrab
        w.dispose();
        Util.waitForIdle(robot);
        if (!ungrabbed) {
            passed = false;
            System.err.println("Failure: [7] Window disposal didn't cause ungrab");
        }
        ungrabbed = false;


        // 8. Check that mouse click on subwindow does not cause ungrab
        frame.setVisible(true);
        window1.setVisible(true);
        window2.setVisible(true);
        Util.waitForIdle(robot);

        tk.grab(window1);

        Util.clickOnComp(window2, robot);
        Util.waitForIdle(robot);

        if (ungrabbed) {
            passed = false;
            System.err.println("Failure: [8] Press on the subwindow caused ungrab");
        }

        if (passed) {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed.");
        }
    }

    public static void checkAndThrow(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }
}
