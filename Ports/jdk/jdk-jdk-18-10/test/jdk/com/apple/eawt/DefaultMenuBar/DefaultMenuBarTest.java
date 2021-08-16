/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007267
 * @summary [macosx] com.apple.eawt.Application.setDefaultMenuBar is not working
 * @requires (os.family == "mac")
 * @author leonid.romanov@oracle.com
 * @modules java.desktop/sun.awt
 *          java.desktop/com.apple.eawt
 * @run main DefaultMenuBarTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.lang.reflect.Method;


public class DefaultMenuBarTest {
    static KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_MASK);

    static volatile int listenerCallCounter = 0;
    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        Robot robot = new Robot();
        robot.setAutoDelay(100);

        robot.keyPress(KeyEvent.VK_META);
        robot.keyPress(ks.getKeyCode());
        robot.keyRelease(ks.getKeyCode());
        robot.keyRelease(KeyEvent.VK_META);

        robot.waitForIdle();

        if (listenerCallCounter != 1) {
            throw new Exception("Test failed: ActionListener either wasn't called or was called more than once");
        }
    }

    private static void createAndShowGUI() {
        JMenu menu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("Open");

        newItem.setAccelerator(ks);
        newItem.addActionListener(
            new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    listenerCallCounter++;
                }
            }
        );
        menu.add(newItem);

        JMenuBar defaultMenu = new JMenuBar();
        defaultMenu.add(menu);

        // Application.getApplication().setDefaultMenuBar(defaultMenu);
        try {
            Class appClass = Class.forName("com.apple.eawt.Application");
            if (appClass != null) {
                Method method = appClass.getMethod("getApplication");
                if (method != null) {
                    Object app = method.invoke(null, new Object[]{});
                    if (app != null) {
                        method = appClass.getMethod("setDefaultMenuBar", new Class[]{JMenuBar.class});
                        if (method != null) {
                            method.invoke(app, new Object[]{defaultMenu});
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
