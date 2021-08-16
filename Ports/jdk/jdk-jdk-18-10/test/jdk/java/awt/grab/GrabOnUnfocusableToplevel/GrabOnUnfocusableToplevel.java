/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6539458
  @summary JPopupMenu does not display if invoker is instance of JWindow
  @author oleg.sukhodolsky area=awt.grab
  @library ../../regtesthelpers
  @build Util
  @run main GrabOnUnfocusableToplevel
*/

/**
 * GrabOnUnfocusableToplevel.java
 *
 * summary: JPopupMenu does not display if invoker is instance of JWindow
 */

import java.awt.Robot;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;

import test.java.awt.regtesthelpers.Util;

public class GrabOnUnfocusableToplevel {
    public static void main(String[] args) {
        Robot r = Util.createRobot();
        JWindow w = new JWindow();
        w.setSize(100, 100);
        w.setVisible(true);
        Util.waitForIdle(r);

        final JPopupMenu menu = new JPopupMenu();
        JButton item = new JButton("A button in popup");

        menu.add(item);

        w.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent me) {
                menu.show(me.getComponent(), me.getX(), me.getY());

                System.out.println("Showing menu at " + menu.getLocationOnScreen() +
                                   " isVisible: " + menu.isVisible() +
                                   " isValid: " + menu.isValid());
                }
            });

        Util.clickOnComp(w, r);
        Util.waitForIdle(r);

        if (!menu.isVisible()) {
            throw new RuntimeException("menu was not shown");
        }

        menu.hide();
        System.out.println("Test passed.");
    }
}
