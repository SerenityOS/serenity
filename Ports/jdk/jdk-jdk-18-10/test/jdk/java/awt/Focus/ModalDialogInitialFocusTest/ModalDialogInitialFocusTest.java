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
  @bug        6382750
  @summary   Tests that modal dialog doesn't request extra initial focus on show.
  @run        main ModalDialogInitialFocusTest
*/

import java.awt.*;
import java.awt.event.*;

public class ModalDialogInitialFocusTest {
    Robot robot;

    Dialog dialog = new Dialog((Window)null, "Test Dialog", Dialog.ModalityType.TOOLKIT_MODAL);
    Button button = new Button("button");

    volatile static boolean passed = true;

    public static void main(String[] args) {
        ModalDialogInitialFocusTest app = new ModalDialogInitialFocusTest();
        app.init();
        app.start();
    }

    public void init() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }
    }

    public void start() {

        dialog.setLayout(new FlowLayout());
        dialog.add(button);
        dialog.setBounds(800, 0, 100, 100);

        dialog.addFocusListener(new FocusAdapter() {
                // The only expected FOCUS_GAINED is on the button.
                public void focusGained(FocusEvent e) {
                    passed = false;
                }
            });

        test();
    }

    void test() {
        new Thread(new Runnable() {
                public void run() {
                  dialog.setVisible(true);
                }
            }).start();

        waitTillShown(dialog);

        robot.waitForIdle();

        dialog.dispose();

        if (passed) {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed: dialog requests extra focus on show!");
        }
    }

    void waitTillShown(Component c) {
        while (true) {
            try {
                Thread.sleep(100);
                c.getLocationOnScreen();
                break;
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                break;
            } catch (IllegalComponentStateException e) {
            }
        }
    }
}
