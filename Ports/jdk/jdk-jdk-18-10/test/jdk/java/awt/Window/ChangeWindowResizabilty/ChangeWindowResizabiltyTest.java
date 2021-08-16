/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @key headful
   @bug      8166897 8167652
   @summary  Some font overlap in the Optionpane dialog.
   @run      main ChangeWindowResizabiltyTest
*/

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Robot;
import java.awt.Point;

public class ChangeWindowResizabiltyTest {
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        for(int i = 0; i < 10; i++) {
            Dialog dialog = new Dialog((Frame) null);
            dialog.setLocation(100, 100);
            Component panel = new Panel();
            panel.setPreferredSize(new Dimension(200, 100));
            dialog.add(panel);
            dialog.pack();
            dialog.setVisible(true);
            robot.waitForIdle();
            robot.delay(200);

            Point frameLoc = dialog.getLocationOnScreen();
            Point contentLoc = panel.getLocationOnScreen();

            System.out.println("Decor location " + frameLoc);
            System.out.println("Content location " + contentLoc);

            dialog.setResizable(false);
            robot.waitForIdle();
            robot.delay(200);

            Point l = dialog.getLocationOnScreen();
            if (!l.equals(frameLoc)) {
                dialog.dispose();
                throw new RuntimeException("Decorated frame location moved " +
                        "after setResizable(false)" + l);
            }

            l = panel.getLocationOnScreen();
            if (!l.equals(contentLoc)) {
                dialog.dispose();
                throw new RuntimeException("Content location moved after " +
                        "setResizable(false)" + l);
            }

            if (panel.getLocationOnScreen().y <
                      dialog.getLocationOnScreen().y + dialog.getInsets().top) {
                dialog.dispose();
                throw new RuntimeException(
                            "Wrong content position after setResizable(false)");
            }

            dialog.setResizable(true);
            robot.waitForIdle();
            robot.delay(200);

            l = dialog.getLocationOnScreen();
            if (!l.equals(frameLoc)) {
                dialog.dispose();
                throw new RuntimeException("Decorated frame location moved " +
                        "after setResizable(true)" + l);
            }

            l = panel.getLocationOnScreen();
            if (!l.equals(contentLoc)) {
                dialog.dispose();
                throw new RuntimeException("Content location moved after " +
                        "setResizable(true)" + l);
            }
            if (panel.getLocationOnScreen().y <
                      dialog.getLocationOnScreen().y + dialog.getInsets().top) {
                dialog.dispose();
                throw new RuntimeException(
                             "Wrong content position after setResizable(true)");
            }

            dialog.dispose();
        }
    }
}
