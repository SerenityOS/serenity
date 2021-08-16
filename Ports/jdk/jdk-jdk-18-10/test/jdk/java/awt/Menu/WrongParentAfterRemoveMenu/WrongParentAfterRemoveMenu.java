/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.PopupMenu;
import java.awt.Window;

/**
 * @test
 * @bug 8165769 8198001
 * @key headful
 */
public final class WrongParentAfterRemoveMenu {

    public static void main(final String[] args) {
        testMenuBar();
        testComponent();
        testFrame();
    }

    private static void testFrame() {
        // peer exists
        Frame frame = new Frame();
        try {
            frame.pack();
            PopupMenu popupMenu = new PopupMenu();
            frame.add(popupMenu);
            checkParent(popupMenu, frame);
            frame.remove(popupMenu);
            checkParent(popupMenu, null);
        } finally {
            frame.dispose();
        }
        // peer is null
        frame = new Frame();
        PopupMenu popupMenu = new PopupMenu();
        frame.add(popupMenu);
        checkParent(popupMenu, frame);
        frame.remove(popupMenu);
        checkParent(popupMenu, null);
    }

    private static void testComponent() {
        // peer exists
        Window w = new Window(null);
        try {
            w.pack();
            PopupMenu popupMenu = new PopupMenu();
            w.add(popupMenu);
            checkParent(popupMenu, w);
            w.remove(popupMenu);
            checkParent(popupMenu, null);
        } finally {
            w.dispose();
        }
        // peer is null
        w = new Window(null);
        PopupMenu popupMenu = new PopupMenu();
        w.add(popupMenu);
        checkParent(popupMenu, w);
        w.remove(popupMenu);
        checkParent(popupMenu, null);
    }

    private static void testMenuBar() {
        // peer exists
        MenuBar mb = new MenuBar();
        try {
            mb.addNotify();
            Menu m1 = new Menu();
            Menu m2 = new Menu();
            m1.add(m2);
            mb.add(m1);
            checkParent(m1, mb);
            checkParent(m2, m1);
            m1.remove(m2);
            checkParent(m2, null);
            mb.remove(m1);
            checkParent(m1, null);
        } finally {
            mb.removeNotify();
        }
        // peer is null
        mb = new MenuBar();
        Menu m1 = new Menu();
        Menu m2 = new Menu();
        m1.add(m2);
        mb.add(m1);
        checkParent(m1, mb);
        checkParent(m2, m1);
        m1.remove(m2);
        checkParent(m2, null);
        mb.remove(m1);
        checkParent(m1, null);
    }

    private static void checkParent(final Menu menu, final Object parent) {
        if (menu.getParent() != parent) {
            System.err.println("Expected: " + parent);
            System.err.println("Actual: " + menu.getParent());
            throw new RuntimeException("Wrong parent");
        }
    }
}
