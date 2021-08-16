/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       6182359
  @summary   Tests that Window having non-focusable owner can't be a focus owner.
  @library   ../../regtesthelpers
  @build     Util
  @run       main NonfocusableOwnerTest
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class NonfocusableOwnerTest {
    Robot robot = Util.createRobot();
    Frame frame;
    Dialog dialog;
    Window window1;
    Window window2;
    Button button = new Button("button");

    public static void main(String[] args) {
        NonfocusableOwnerTest test = new NonfocusableOwnerTest();
        test.start();
    }

    public void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    System.out.println(e.toString());
                }
            }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_FOCUS_EVENT_MASK | WindowEvent.WINDOW_EVENT_MASK);

        frame = new Frame("Frame");
        frame.setName("Frame-owner");
        frame.setBounds(100, 0, 100, 100);
        dialog = new Dialog(frame, "Dialog");
        dialog.setName("Dialog-owner");
        dialog.setBounds(100, 0, 100, 100);

        window1 = new Window(frame);
        window1.setName("1st child");
        window1.setBounds(100, 300, 100, 100);
        window2 = new Window(window1);
        window2.setName("2nd child");
        window2.setBounds(100, 500, 100, 100);

        test1(frame, window1);
        test2(frame, window1, window2);
        test3(frame, window1, window2);

        window1 = new Window(dialog);
        window1.setBounds(100, 300, 100, 100);
        window1.setName("1st child");
        window2 = new Window(window1);
        window2.setName("2nd child");
        window2.setBounds(100, 500, 100, 100);

        test1(dialog, window1);
        test2(dialog, window1, window2);
        test3(dialog, window1, window2);

        System.out.println("Test passed.");
    }

    void test1(Window owner, Window child) {
        System.out.println("* * * STAGE 1 * * *\nWindow owner: " + owner);

        owner.setFocusableWindowState(false);
        owner.setVisible(true);

        child.add(button);
        child.setVisible(true);

        Util.waitTillShown(child);

        Util.clickOnComp(button, robot);
        if (button == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) {
            throw new RuntimeException("Test Failed.");
        }
        child.dispose();
        owner.dispose();
    }

    void test2(Window owner, Window child1, Window child2) {
        System.out.println("* * * STAGE 2 * * *\nWindow nowner: " + owner);

        owner.setFocusableWindowState(false);
        owner.setVisible(true);

        child1.setFocusableWindowState(true);
        child1.setVisible(true);

        child2.add(button);
        child2.setVisible(true);

        Util.waitTillShown(child2);

        Util.clickOnComp(button, robot);
        if (button == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) {
            throw new RuntimeException("Test failed.");
        }
        child2.dispose();
        child1.dispose();
        owner.dispose();
    }

    void test3(Window owner, Window child1, Window child2) {
        System.out.println("* * * STAGE 3 * * *\nWidow owner: " + owner);

        owner.setFocusableWindowState(true);
        owner.setVisible(true);

        child1.setFocusableWindowState(false);
        child1.setVisible(true);

        child2.setFocusableWindowState(true);
        child2.add(button);
        child2.setVisible(true);

        Util.waitTillShown(child2);

        Util.clickOnComp(button, robot);
        System.err.println("focus owner: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        if (button != KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) {
            throw new RuntimeException("Test failed.");
        }
        child1.dispose();
        child2.dispose();
        owner.dispose();
    }
}
