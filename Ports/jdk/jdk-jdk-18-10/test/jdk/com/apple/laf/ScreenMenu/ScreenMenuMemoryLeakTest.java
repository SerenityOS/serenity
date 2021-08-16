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

/**
 * @test
 * @key headful
 * @bug 8158325 8180821 8202878
 * @summary Memory leak in com.apple.laf.ScreenMenu: removed JMenuItems are still referenced
 * @requires (os.family == "mac")
 * @library /javax/swing/regtesthelpers
 * @build Util
 * @run main/timeout=300/othervm -Xmx16m ScreenMenuMemoryLeakTest
 */

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

public class ScreenMenuMemoryLeakTest {

    private static WeakReference<JMenuItem> sMenuItem;
    private static JFrame sFrame;
    private static JMenu sMenu;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                showUI();
            }
        });

        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                removeMenuItemFromMenu();
            }
        });

        Util.generateOOME();
        JMenuItem menuItem = sMenuItem.get();
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                sFrame.dispose();
            }
        });
        if (menuItem != null) {
            throw new RuntimeException("The menu item should have been GC-ed");
        }
    }

    private static void showUI() {
        sFrame = new JFrame();
        sFrame.add(new JLabel("Some dummy content"));

        JMenuBar menuBar = new JMenuBar();

        sMenu = new JMenu("Menu");
        JMenuItem item = new JMenuItem("Item");
        sMenu.add(item);

        sMenuItem = new WeakReference<>(item);

        menuBar.add(sMenu);

        sFrame.setJMenuBar(menuBar);

        sFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        sFrame.pack();
        sFrame.setVisible(true);
    }

    private static void removeMenuItemFromMenu() {
        JMenuItem menuItem = sMenuItem.get();
        Objects.requireNonNull(menuItem, "The menu item should still be available at this point");
        sMenu.remove(menuItem);
    }
}
