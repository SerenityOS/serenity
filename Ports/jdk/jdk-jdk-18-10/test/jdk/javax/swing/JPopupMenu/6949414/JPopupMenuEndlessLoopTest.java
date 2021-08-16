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

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 6949414 6424606
 * @summary JMenu.buildMenuElementArray() endless loop
 * @run main/timeout=5 JPopupMenuEndlessLoopTest
 */
public class JPopupMenuEndlessLoopTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {

            JPopupMenu popup = new JPopupMenu("Popup Menu");
            JMenu menu = new JMenu("Menu");
            menu.add(new JMenuItem("Menu Item"));
            popup.add(menu);
            menu.doClick();
            MenuElement[] elems = MenuSelectionManager
                    .defaultManager().getSelectedPath();

            if (elems == null || elems.length == 0) {
                throw new RuntimeException("Empty Selection");
            }

            if (elems[0] != popup || elems[1] != menu) {
                throw new RuntimeException("Necessary menus are not selected!");
            }
        });
    }
}
