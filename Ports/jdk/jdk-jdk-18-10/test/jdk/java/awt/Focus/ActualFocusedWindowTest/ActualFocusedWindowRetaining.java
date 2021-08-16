/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
  @bug      4823903
  @summary  Tests actual focused window retaining.
  @library  ../../regtesthelpers
  @build    Util
  @run      main ActualFocusedWindowRetaining
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class ActualFocusedWindowRetaining {
    public static Frame frame = new Frame("Other Frame");
    public static Frame owner = new Frame("Test Frame");
    public static Button otherButton1 = new Button("Other Button 1");
    public static Button otherButton2 = new Button("Other Button 2");
    public static Button otherButton3 = new Button("Other Button 3");
    public static Button testButton1 = new Button("Test Button 1");
    public static Button testButton2 = new Button("Test Button 2");
    public static Button testButton3 = new Button("Test Button 3");
    public static Window window1 = new TestWindow(owner, otherButton2, testButton2, 800, 200);
    public static Window window2 = new TestWindow(owner, otherButton3, testButton3, 800, 300);
    public static int step;
    public static Robot robot = Util.createRobot();

    public static void main(String[] args) {
        ActualFocusedWindowRetaining a = new ActualFocusedWindowRetaining();
        a.start();
    }

    public void start () {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    Object src = e.getSource();
                    Class cls = src.getClass();

                    if (cls == TestWindow.class) {
                        System.out.println(e.paramString() + " on <" + (src == window1 ? "Window 1" : "Window 2") + ">");
                    } else if (cls == Frame.class) {
                        System.out.println(e.paramString() + " on <" + ((Frame)src).getTitle() + ">");
                    } else if (cls == Button.class) {
                        System.out.println(e.paramString() + " on <" + ((Button)src).getLabel() + ">");
                    } else {
                        System.out.println(e.paramString() + " on <Non-testing component>");
                    }
                }
            }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);

        frame.setSize(new Dimension(400, 100));
        frame.setLocation(800, 400);
        frame.setVisible(true);
        frame.toFront();

        owner.setLayout(new FlowLayout());
        owner.add(testButton1);
        owner.add(otherButton1);
        owner.pack();
        owner.setLocation(800, 100);
        owner.setSize(new Dimension(400, 100));
        owner.setVisible(true);
        owner.toFront();
        Util.waitTillShown(owner);

        window1.setVisible(true);
        window2.setVisible(true);
        window1.toFront();
        window2.toFront();
        // Wait longer...
        Util.waitTillShown(window1);
        Util.waitTillShown(window2);

        test();

        frame.dispose();
        owner.dispose();
    }

    public void test() {
        Button[] butArr = new Button[] {testButton3, testButton2, testButton1};
        Window[] winArr = new Window[] {window2, window1, owner};

        step = 1;
        for (int i = 0; i < 3; i++) {
            clickInSeriesCheckFocus(null, butArr[i], frame);
            clickOwnerCheckFocus(winArr[i], butArr[i]);
            step++;
        }

        step = 4;
        clickInSeriesCheckFocus(testButton3, testButton1, frame);
        clickOwnerCheckFocus(owner, testButton1);

        step = 5;
        clickInSeriesCheckFocus(testButton3, testButton2, frame);
        clickOwnerCheckFocus(window1, testButton2);

        step = 6;
        clickInSeriesCheckFocus(testButton1, testButton2, frame);
        clickOwnerCheckFocus(window1, testButton2);

        step = 7;
        clickInSeriesCheckFocus(testButton1, testButton2, frame);
        window1.setVisible(false);
        Util.waitForIdle(robot);
        clickOwnerCheckFocus(owner, testButton1);

        step = 8;
        window1.setVisible(true);
        Util.waitTillShown(window1);
        clickInSeriesCheckFocus(null, testButton2, frame);
        clickOwnerCheckFocus(window1, testButton2);
    }

    boolean checkFocusOwner(Component comp) {
        return (comp == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
    }

    boolean checkFocusedWindow(Window win) {
        return (win == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow());
    }

    void clickOwnerCheckFocus(Window focusedWindow, Component focusedComp) {
        Util.clickOnTitle(owner, robot);
        robot.delay(500);

        if (!checkFocusedWindow(focusedWindow)) {
            stopTest("Test failed: actual focused window didn't get a focus");
        }
        if (!checkFocusOwner(focusedComp)) {
            stopTest("Test failed: actual focus owner didn't get a focus");
        }
    }

    void clickInSeriesCheckFocus(Component comp1, Component comp2, Frame frame) {
        if (comp1 != null) {
            clickOnCheckFocusOwner(comp1);
        }
        if (comp2 != null) {
            clickOnCheckFocusOwner(comp2);
        }
        clickOnCheckFocusedWindow(frame);
    }

    void clickOnCheckFocusOwner(Component c) {
        Util.clickOnComp(c, robot);
        robot.delay(500);

        if (!checkFocusOwner(c)) {
            stopTest("Error: can't bring a focus on Component by clicking on it");
        }
    }

    void clickOnCheckFocusedWindow(Frame f) {
        Util.clickOnTitle(f, robot);
        robot.delay(500);

        if (!checkFocusedWindow(f)) {
            stopTest("Error: can't bring a focus on Frame by clicking on it");
        }
    }

    void stopTest(String msg) {
        throw new RuntimeException(new String("Step " + step + ": " + msg));
    }
}

class TestWindow extends Window {
    TestWindow(Frame owner, Button otherButton, Button testButton, int x, int y) {
        super(owner);

        setLayout(new FlowLayout());
        setLocation(x, y);
        add(testButton);
        add(otherButton);
        pack();
        setBackground(Color.green);
    }
}
