/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067346
 * @summary Submenu has a changed offset on Windows7 with Windows look and feel
 * @requires (os.family == "windows")
 * @run main bug8067346
 */

import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


public class bug8067346 {

    private JMenuBar menuBar;
    private JFrame frame;
    private String[] menuClasses = {"MenuItem", "Menu",
        "CheckBoxMenuItem", "RadioButtonMenuItem"};
    private String MARGIN = ".margin";
    private String CHECKICONOFFSET = ".checkIconOffset";
    private static boolean runTest = true;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                bug8067346 test = new bug8067346();
                try {
                    // set windows look and feel
                    String lnf =
                           "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                    UIManager.setLookAndFeel(lnf);
                } catch (UnsupportedLookAndFeelException e) {
                    runTest = false;
                } catch (ClassNotFoundException e) {
                    runTest = false;
                } catch (InstantiationException e) {
                    runTest = false;
                } catch (IllegalAccessException e) {
                    runTest = false;
                }
                if(runTest) {
                    test.createUI();
                    test.performTest();
                    test.dispose();
                }
            }
        });
    }

    public void createUI() {

        frame = new JFrame();
        menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        JMenu menu, submenu;
        JMenuItem menuItem;

        menu = new JMenu("A Menu");
        menuBar.add(menu);
        menu.addSeparator();

        submenu = new JMenu("A submenu");

        menuItem = new JMenuItem("An item in the submenu");
        submenu.add(menuItem);
        menu.add(submenu);
    }

    public void performTest() {
        try {
            String errorMessage = "Incorrect value for ";
            StringBuilder errorMessageBuilder = new StringBuilder(errorMessage);
            boolean error = false;
            int retVal = testMargin();
            if (retVal != 0) {
                errorMessageBuilder.append(menuClasses[retVal])
                        .append(MARGIN).append("\n");
                error = true;
            }
            retVal = testCheckIconOffset();
            if (retVal != 0) {
                errorMessageBuilder.append(errorMessage)
                        .append(menuClasses[retVal]).append(CHECKICONOFFSET);
            }
            if (error || retVal != 0) {
                throw new RuntimeException(errorMessageBuilder.toString());
            }
        } finally {
            dispose();
        }
    }

    private int testMargin() {

        for (int inx = 0; inx < menuClasses.length; inx++) {
            Insets margin = (Insets) UIManager.get(menuClasses[inx] + MARGIN);
            if (margin != null && margin.bottom == 0 && margin.left == 0
                    && margin.right == 0 && margin.top == 0) {
                return inx + 1;
            }
        }
        return 0;
    }

    private int testCheckIconOffset() {

        for (int inx = 0; inx < menuClasses.length; inx++) {
            Object checkIconOffset = UIManager.get(menuClasses[inx]
                    + CHECKICONOFFSET);
            if (checkIconOffset != null && ((Integer) checkIconOffset) == 0) {
                return inx + 1;
            }
        }
        return 0;
    }

    public void dispose() {
        frame.dispose();
    }
}
