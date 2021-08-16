/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.5 98/07/23
 * @key headful
 * @bug 4064202 4253466
 * @summary Test for Win32 NPE when MenuItem with null label added.
 * @author fred.ecks
 * @run main/othervm NullMenuLabelTest
 */

import java.awt.*;

public class NullMenuLabelTest {

    public static void main(String[] args) {
        Frame frame = new Frame("Test Frame");
        frame.pack();
        frame.setVisible(true);
        MenuBar menuBar = new MenuBar();
        frame.setMenuBar(menuBar);
        Menu menu = new Menu(null);
        menuBar.add(menu);
        menu.add(new MenuItem(null));
        // If we got this far, the test succeeded
        frame.setVisible(false);
        frame.dispose();
    }

} // class NullMenuLabelTest
