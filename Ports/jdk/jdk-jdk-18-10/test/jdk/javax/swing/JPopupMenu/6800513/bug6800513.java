/*
 * Copyright 2012 Red Hat, Inc.  All Rights Reserved.
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6800513
 * @summary GTK-LaF renders menus incompletely
 * @author Mario Torre
 * @modules java.desktop/javax.swing:open
 * @library ../../regtesthelpers/
 * @build Util
 * @run main bug6800513
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class bug6800513 {

    private static JPopupMenu popupMenu;
    private static JMenu menu;
    private static JFrame frame;
    private static Robot robot;

    public static void testFrame(final boolean defaultLightWeightPopupEnabled,
            String expectedPopupClass) throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JPopupMenu.setDefaultLightWeightPopupEnabled(defaultLightWeightPopupEnabled);
                createAndShowUI();
            }
        });

        robot.waitForIdle();

        clickOnMenu();

        robot.waitForIdle();

        Field getPopup = JPopupMenu.class.getDeclaredField("popup");
        getPopup.setAccessible(true);
        Popup popup = (Popup) getPopup.get(popupMenu);

        if (popup == null) {
            throw new Exception("popup is null!");
        }

        String className = popup.getClass().getName();
        if (!className.equals(expectedPopupClass)) {
            throw new Exception("popup class is: " + className +
                    ", expected: " + expectedPopupClass);
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
                popupMenu = null;
            }
        });

        robot.waitForIdle();
    }


    public static void clickOnMenu() throws Exception {
        Rectangle bounds = Util.invokeOnEDT(new Callable<Rectangle>() {
            @Override
            public Rectangle call() throws Exception {
                return new Rectangle(menu.getLocationOnScreen(), menu.getSize());
            }
        });

        robot.setAutoDelay(100);

        robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static class PopupListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.toString().contains("visible") && ((Boolean) evt.getNewValue() == true)) {
                popupMenu = (JPopupMenu) evt.getSource();
            }
        }
    }

    public static void createAndShowUI() {
        frame = new JFrame();

        JMenuBar menuBar = new JMenuBar();
        menu = new JMenu("Menu");

        menu.add(new JMenuItem("Menu Item #1"));
        menu.add(new JMenuItem("Menu Item #2"));
        menu.add(new JMenuItem("Menu Item #3"));

        menuBar.add(menu);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(menuBar);
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);

        PopupListener listener = new PopupListener();
        menu.getPopupMenu().addPropertyChangeListener(listener);

        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        testFrame(false, "javax.swing.PopupFactory$HeavyWeightPopup");

        testFrame(true, "javax.swing.PopupFactory$LightWeightPopup");
    }
}
