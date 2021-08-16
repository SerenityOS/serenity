/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug        6271849
  @summary    Tests that component in modal excluded Window which parent is blocked responses to mouse clicks.
  @modules java.desktop/sun.awt
  @run        main ModalExcludedWindowClickTest
*/

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

public class ModalExcludedWindowClickTest {
    Robot robot;
    Frame frame = new Frame("Frame");
    Window w = new Window(frame);
    Dialog d = new Dialog ((Dialog)null, "NullParentDialog", true);
    Button button = new Button("Button");
    boolean actionPerformed = false;

    public static void main (String args[]) {
        ModalExcludedWindowClickTest app = new ModalExcludedWindowClickTest();
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

        if ("sun.awt.motif.MToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
            System.out.println("No testing on MToolkit.");
            return;
        }

        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    actionPerformed = true;
                    System.out.println(e.paramString());
                }
            });

        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    frame.setSize(200, 200);
                    frame.setVisible(true);

                    w.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
                    w.add(button);
                    w.setSize(200, 200);
                    w.setLocation(230, 230);
                    w.setVisible(true);

                    d.setSize(200, 200);
                    d.setLocation(0, 230);
                    d.setVisible(true);

                }
            });

        waitTillShown(d);

        test();
    }

    void test() {
        clickOn(button);
        waitForIdle();
        if (!actionPerformed) {
            throw new RuntimeException("Test failed!");
        }
        System.out.println("Test passed.");
    }

    void clickOn(Component c) {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();

        System.out.println("Clicking " + c);

        if (c instanceof Frame) {
            robot.mouseMove(p.x + (int)(d.getWidth()/2), p.y + ((Frame)c).getInsets().top/2);
        } else {
            robot.mouseMove(p.x + (int)(d.getWidth()/2), p.y + (int)(d.getHeight()/2));
        }
        waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        waitForIdle();
    }
    void waitTillShown(Component c) {
        while (true) {
            try {
                Thread.sleep(100);
                Point p = c.getLocationOnScreen();
                if (p != null)
                    break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IllegalComponentStateException e) {}
        }
    }
    void waitForIdle() {
        try {
            robot.waitForIdle();
            EventQueue.invokeAndWait( new Runnable() {
                    public void run() {} // Dummy implementation
                });
        } catch(InterruptedException ie) {
            System.out.println("waitForIdle, non-fatal exception caught:");
            ie.printStackTrace();
        } catch(InvocationTargetException ite) {
            System.out.println("waitForIdle, non-fatal exception caught:");
            ite.printStackTrace();
        }

        // wait longer...
        robot.delay(200);
    }
}
