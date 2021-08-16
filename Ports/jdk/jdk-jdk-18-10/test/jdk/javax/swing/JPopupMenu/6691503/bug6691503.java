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
 * @test
 * @key headful
 * @bug 6691503
 * @summary Checks that there is no opportunity for a malicious applet
 * to show a popup menu which has whole screen size.
 * a heaviweight popup menu is shown from an applet.
 * @author Mikhail Lapshin
 * @run main/othervm -Djava.security.manager=allow bug6691503
 */

import javax.swing.*;
import java.awt.*;

public class bug6691503 {
    private JPopupMenu popupMenu;
    private JFrame frame;
    private boolean isAlwaysOnTop1 = false;
    private boolean isAlwaysOnTop2 = true;

    public static void main(String[] args) {
        bug6691503 test = new bug6691503();
        test.setupUI();
        test.testApplication();
        test.testApplet();
        test.checkResult();
        test.stopEDT();
    }

    private void setupUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame = new JFrame();
                frame.setVisible(true);
                popupMenu = new JPopupMenu();
                JMenuItem click = new JMenuItem("Click");
                popupMenu.add(click);
            }
        });
    }

    private void testApplication() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                popupMenu.show(frame, 0, 0);
                Window popupWindow = (Window)
                        (popupMenu.getParent().getParent().getParent().getParent());
                isAlwaysOnTop1 = popupWindow.isAlwaysOnTop();
                System.out.println(
                        "Application: popupWindow.isAlwaysOnTop() = " + isAlwaysOnTop1);
                popupMenu.setVisible(false);
            }
        });
    }

    private void testApplet() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.setSecurityManager(new SecurityManager());
                popupMenu.show(frame, 0, 0);
                Window popupWindow = (Window)
                        (popupMenu.getParent().getParent().getParent().getParent());
                isAlwaysOnTop2 = popupWindow.isAlwaysOnTop();
                System.out.println(
                        "Applet: popupWindow.isAlwaysOnTop() = " + isAlwaysOnTop2);
                popupMenu.setVisible(false);
            }
        });
    }

    private void checkResult() {
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }
        if (!isAlwaysOnTop1 || isAlwaysOnTop2) {
            throw new RuntimeException("Malicious applet can show always-on-top " +
                    "popup menu which has whole screen size");
        }
        System.out.println("Test passed");
    }

    private void stopEDT() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
    }
}
