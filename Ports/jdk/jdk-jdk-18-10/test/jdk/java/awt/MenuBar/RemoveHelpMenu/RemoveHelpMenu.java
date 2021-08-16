/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 6475361
 * @author Sergey Bylokhov
 */
public final class RemoveHelpMenu {

    public static void main(final String[] args) {
        final Frame frame = new Frame("RemoveHelpMenu Test");
        try {
            frame.pack();
            // peer exists
            test1(getMenuBar(frame));
            test2(getMenuBar(frame));
            test3(getMenuBar(frame));
            test4(getMenuBar(frame));
        } finally {
            frame.dispose();
        }
        // peer is null
        test1(getMenuBar(frame));
        test2(getMenuBar(frame));
        test3(getMenuBar(frame));
        test4(getMenuBar(frame));
    }

    private static MenuBar getMenuBar(final Frame frame) {
        final MenuBar menuBar = new MenuBar();
        frame.setMenuBar(menuBar);
        return menuBar;
    }

    private static void checkHelpMenu(final Menu menu, final boolean expected) {
        final boolean actual = menu.toString().contains("isHelpMenu=true");
        if (actual != expected) {
            throw new RuntimeException("Incorrect menu type");
        }
    }

    private static void checkMenuCount(final MenuBar bar, final int expected) {
        final int actual = bar.getMenuCount();
        if (actual != expected) {
            throw new RuntimeException("Incorrect menus count");
        }
    }

    private static void checkCurrentMenu(final MenuBar bar, final Menu menu) {
        if (bar.getHelpMenu() != menu) {
            throw new RuntimeException("Wrong HelpMenu");
        }
    }

    private static void test1(final MenuBar menuBar) {
        checkCurrentMenu(menuBar, null);
        checkMenuCount(menuBar, 0);
    }

    private static void test2(final MenuBar menuBar) {
        final Menu helpMenu = new Menu("Help Menu");
        menuBar.setHelpMenu(helpMenu);
        checkCurrentMenu(menuBar, helpMenu);
        checkMenuCount(menuBar, 1);
        checkHelpMenu(helpMenu, true);

        menuBar.remove(helpMenu);
        checkCurrentMenu(menuBar, null);
        checkMenuCount(menuBar, 0);
        checkHelpMenu(helpMenu, false);
    }

    private static void test3(final MenuBar menuBar) {
        final Menu helpMenu1 = new Menu("Help Menu1");
        final Menu helpMenu2 = new Menu("Help Menu2");
        menuBar.setHelpMenu(helpMenu1);
        checkCurrentMenu(menuBar, helpMenu1);
        checkMenuCount(menuBar, 1);
        checkHelpMenu(helpMenu1, true);
        checkHelpMenu(helpMenu2, false);

        menuBar.setHelpMenu(helpMenu2);
        checkCurrentMenu(menuBar, helpMenu2);
        checkMenuCount(menuBar, 1);
        checkHelpMenu(helpMenu1, false);
        checkHelpMenu(helpMenu2, true);

        menuBar.remove(helpMenu2);
        checkCurrentMenu(menuBar, null);
        checkMenuCount(menuBar, 0);
        checkHelpMenu(helpMenu1, false);
        checkHelpMenu(helpMenu2, false);
    }

    private static void test4(final MenuBar menuBar) {
        final Menu helpMenu = new Menu("Help Menu");
        menuBar.setHelpMenu(helpMenu);
        checkCurrentMenu(menuBar, helpMenu);
        checkMenuCount(menuBar, 1);
        checkHelpMenu(helpMenu, true);

        menuBar.setHelpMenu(null);
        checkCurrentMenu(menuBar, null);
        checkMenuCount(menuBar, 0);
        checkHelpMenu(helpMenu, false);
    }
}
