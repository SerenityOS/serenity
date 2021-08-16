/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/*
 * @test
 * @bug 6263470
 * @requires (os.family != "mac")
 * @key headful
 * @summary Menu bugs in XAWT
 * @author Vyacheslav.Baranov: area= menu
 * @run main SeparatorsNavigation
 */

/**
 * SeparatorsNavigation.java
 *
 * summary: Creates menu bar with menu consisting of two
 * separators, then tries to navigate it using keyboard.
 */

public class SeparatorsNavigation {
    static class Listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SeparatorsNavigation.pressed = true;
        }
    }

    static Frame f;
    static MenuBar mb;
    static Menu m1;
    static Menu m2;
    static Menu m3;
    static MenuItem i31;
    static Listener l = new Listener();
    static boolean pressed = false;

    public static void main(String args[]) {
        f = new Frame();
        mb = new MenuBar();
        m1 = new Menu("m1");
        m2 = new Menu("m2");
        m3 = new Menu("m3");
        m1.add(new MenuItem("i11"));
        m2.add(new MenuItem("-"));
        m2.add(new MenuItem("-"));
        mb.add(m1);
        mb.add(m2);
        mb.add(m3);
        i31 = new MenuItem("i31");
        m3.add(i31);
        i31.addActionListener(l);
        f.setMenuBar(mb);
        f.setSize(400, 400);
        f.setVisible(true);
        try {
            Robot r = new Robot();
            r.delay(1000);
            r.keyPress(KeyEvent.VK_F10);
            r.delay(10);
            r.keyRelease(KeyEvent.VK_F10);
            r.delay(1000);
            r.keyPress(KeyEvent.VK_DOWN);
            r.delay(10);
            r.keyRelease(KeyEvent.VK_DOWN);
            r.delay(1000);
            r.keyPress(KeyEvent.VK_RIGHT);
            r.delay(10);
            r.keyRelease(KeyEvent.VK_RIGHT);
            r.delay(1000);
            r.keyPress(KeyEvent.VK_RIGHT);
            r.delay(10);
            r.keyRelease(KeyEvent.VK_RIGHT);
            r.delay(1000);
            r.keyPress(KeyEvent.VK_ENTER);
            r.delay(10);
            r.keyRelease(KeyEvent.VK_ENTER);
            r.delay(10000);
        } catch (Exception ex) {
            throw new RuntimeException("Execution interrupted by an " +
                    "exception " + ex.getLocalizedMessage());
        } finally {
            if (f != null) {
                f.setVisible(false);
                f.dispose();
            }
        }
        if (!pressed) {
            throw new RuntimeException("Action was not performed, " +
                    "considering test failed.");
        }
    }
}