/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug        6426132
  @summary    Modal blocked window shouldn't steal focus when shown, or brought to front.
  @library    ../../regtesthelpers
  @build      Util
  @run        main ModalBlockedStealsFocusTest
*/

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;
import test.java.awt.regtesthelpers.Util;

public class ModalBlockedStealsFocusTest {
    Frame frame = new Frame("Blocked Frame");
    Dialog dialog = new Dialog(frame, "Modal Dialog", Dialog.ModalityType.TOOLKIT_MODAL);
    AtomicBoolean lostFocus = new AtomicBoolean(false);

    public static void main(String[] args) {
        ModalBlockedStealsFocusTest app = new ModalBlockedStealsFocusTest();
        app.start();
    }

    public void start() {
        if ("sun.awt.motif.MToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
            System.out.println("The test is not for MToolkit.");
            return;
        }

        dialog.setBounds(800, 0, 200, 100);
        frame.setBounds(800, 150, 200, 100);

        dialog.addWindowFocusListener(new WindowAdapter() {
                public void windowLostFocus(WindowEvent e) {
                    System.out.println(e.toString());
                    synchronized (lostFocus) {
                        lostFocus.set(true);
                        lostFocus.notifyAll();
                    }
                }
            });

        new Thread(new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                }
            }).start();

        Util.waitTillShown(dialog);
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        // Test 1. Show a modal blocked frame, check that it doesn't steal focus.

        frame.setVisible(true);

        if (Util.waitForCondition(lostFocus, 2000L)) {
            throw new TestFailedException("the modal blocked frame stole focus on its showing!");
        }

        // Test 2. Brought a modal blocked frame to front, check that it doesn't steal focus.

        frame.toFront();

        if (Util.waitForCondition(lostFocus, 2000L)) {
            throw new TestFailedException("the modal blocked frame stole focus on its bringing to front!");
        } else {
            System.out.println("Test passed");
        }
    }
}

class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}
