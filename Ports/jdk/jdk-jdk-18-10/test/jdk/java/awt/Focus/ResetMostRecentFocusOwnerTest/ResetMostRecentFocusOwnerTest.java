/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug      8013773
  @summary  Tests that disabled component is not retained as most recent focus owner.
  @library  ../../regtesthelpers
  @build    Util
  @run      main ResetMostRecentFocusOwnerTest
*/

import java.awt.AWTEvent;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import test.java.awt.regtesthelpers.Util;

public class ResetMostRecentFocusOwnerTest {

    public static void main(String[] args) {
        ResetMostRecentFocusOwnerTest app = new ResetMostRecentFocusOwnerTest();
        app.start();
    }

    public void start() {

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                System.err.println(e);
            }
        }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_FOCUS_EVENT_MASK);

        boolean gained = false;
        final Robot robot = Util.createRobot();

        JFrame frame1 = new JFrame("Main Frame");
        final JButton b1 = new JButton("button1");
        frame1.add(b1);
        frame1.pack();
        frame1.setLocation(100, 300);

        Util.showWindowWait(frame1);

        final JFrame frame2 = new JFrame("Test Frame");
        final JButton b2 = new JButton("button2");
        frame2.add(b2);
        frame2.pack();
        frame2.setLocation(300, 300);

        b2.setEnabled(false);
        b2.requestFocus();

        Util.showWindowWait(frame2);

        robot.delay(500);

        //
        // It's expeced that the focus is restored to <button1>.
        // If not, click <button1> to set focus on it.
        //
        if (!b1.hasFocus()) {
            gained = Util.trackFocusGained(b1, new Runnable() {
                public void run() {
                    Util.clickOnComp(b1, robot);
                }
            }, 5000, false);

            if (!gained) {
                throw new RuntimeException("Unexpected state: focus is not on <button1>");
            }
        }

        robot.delay(500);

        //
        // Click <button2>, check that focus is set on the parent frame.
        //
        gained = false;
        gained = Util.trackFocusGained(frame2, new Runnable() {
            public void run() {
                Util.clickOnComp(b2, robot);
            }
        }, 5000, false);

        if (!gained) {
            throw new RuntimeException("Test failed: focus wasn't set to <frame2>");
        }

        System.out.println("Test passed.");
    }
}
