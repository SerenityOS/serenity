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
  @bug 6390326 8204946
  @key headful
  @summary REGRESSION: Broken mouse behaviour of menus partially outside the main window.
  @run main MenuDragEvents
*/

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;

public class MenuDragEvents
{
    //Declare things used in the test, like buttons and labels here
    boolean mouseDragged = false;
    boolean mouseEntered = false;
    boolean mouseReleased = false;
    boolean actionReceived = false;

    public static void main(String[] args) {
        MenuDragEvents test = new MenuDragEvents();
        test.doTest();
    }

    public void doTest ()
    {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent event) {
                    int id = event.getID();
                    if (id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_EXITED) {
                        System.err.println(event);
                    }
                }
            }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        JMenuBar mb = new JMenuBar();

        JMenu m = new JMenu("A Menu");
        mb.add(m);

        JMenuItem i = new JMenuItem("A menu item",KeyEvent.VK_A);
        m.add(i);

        m = new JMenu("Another Menu");
        mb.add(m);
        i = new JMenuItem("Yet another menu item",KeyEvent.VK_Y);
        m.add(i);
        i.addMenuDragMouseListener(new MenuDragMouseListener() {
                public void menuDragMouseDragged(MenuDragMouseEvent e) {
                    System.err.println(e);
                    mouseDragged = true;
                }
                public void menuDragMouseEntered(MenuDragMouseEvent e) {
                    System.err.println(e);
                    mouseEntered = true;
                }
                public void menuDragMouseReleased(MenuDragMouseEvent e) {
                    System.err.println(e);
                    mouseReleased = true;
                }
                // perhaps we need to test mouse exited too
                // but this doesn't work even with tiger
                public void menuDragMouseExited(MenuDragMouseEvent e) {
                    System.err.println(e);
                }
            });

        i.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    System.err.println(ae);
                    actionReceived = true;
                }
            });

        JFrame frame = new JFrame("Menu");
        frame.setLayout (new BorderLayout ());
        frame.setJMenuBar(mb);
        frame.setSize(200, 200);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        Robot r = null;
        try {
            r = new Robot();
            r.setAutoDelay(50);
        }
        catch (AWTException ae) {
            throw new RuntimeException(ae);
        }

        r.waitForIdle();

        Point loc = m.getLocationOnScreen();
        loc.x += m.getWidth() / 2;
        loc.y += m.getHeight() / 2;
        r.mouseMove(loc.x, loc.y);
        r.mousePress(InputEvent.BUTTON1_MASK);

        r.waitForIdle();
        r.delay(1000);

        Point loc2 = i.getLocationOnScreen();
        loc2.x += i.getWidth() / 2;
        loc2.y += i.getHeight() / 2;

        // move from menu on menubar to menu item
        dragMouse(r, loc, loc2);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
        r.waitForIdle();
        r.delay(1000);

        if (!mouseEntered || !mouseDragged || !mouseReleased || !actionReceived) {
            throw new RuntimeException("we expected to receive both mouseEntered and MouseDragged ("
                                       + mouseEntered + ", " + mouseDragged + ", " + mouseReleased
                                       + ", " + actionReceived + ")");
        }

        System.out.println("Test passed");

        // dispose off the frame
        frame.dispose();
    }// doTest()

    void dragMouse(Robot r, Point from, Point to) {
        final int n_step = 10;
        int step_x = (to.x - from.x) / n_step;
        int step_y = (to.y - from.y) / n_step;
        int x = from.x;
        int y = from.y;
        for (int idx = 0; idx < n_step; idx++) {
            x += step_x;
            y += step_y;
            r.mouseMove(x, y);
            r.delay(10);
        }
        if (x != to.x || y != to.y) {
            r.mouseMove(to.x, to.y);
            r.delay(10);
        }
    }

}// class MenuDragEvents
