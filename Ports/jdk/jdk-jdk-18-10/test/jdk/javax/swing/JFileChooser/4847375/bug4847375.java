/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4847375 8171363
 * @requires (os.family == "windows")
 * @summary JFileChooser Create New Folder button is disabled incorrectly
 * @author Pavel Porvatov
 * @modules java.desktop/sun.awt
 *          java.desktop/sun.awt.shell:+open
 */

import sun.awt.OSInfo;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

public class bug4847375 {
    private final String newFolderToolTipText;

    private final String lookAndFeel;

    public static void main(String[] args) throws Exception {
        if (OSInfo.getOSType() != OSInfo.OSType.WINDOWS) {
            System.out.println("The test is suitable only for Windows OS. Skipped.");

            return;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug4847375("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

                new bug4847375("javax.swing.plaf.metal.MetalLookAndFeel");
            }
        });
    }

    private static Object[][] DIRECTORIES = new Object[][]{
            {"getDesktop", Boolean.TRUE},
            {"getDrives", Boolean.FALSE}, // My computer
            {"getRecent", Boolean.TRUE},
            {"getNetwork", Boolean.FALSE},
            {"getPersonal", Boolean.TRUE},
    };

    private bug4847375(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;

        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            fail("Cannot set LookAndFeel", e);
        }

        JFileChooser fileChooser = new JFileChooser();

        // Find button NewFolder
        newFolderToolTipText = UIManager.getString("FileChooser.newFolderToolTipText", fileChooser.getLocale());

        if (newFolderToolTipText == null || newFolderToolTipText.length() == 0) {
            fail("Cannot find NewFolderButton in FileChooser (tooltip doesn't exist)");

            return;
        }

        JButton newFolderButton = findNewFolderButton(fileChooser);

        if (newFolderButton == null) {
            fail("Cannot find NewFolderButton in FileChooser");

            return;
        }

        for (Object[] objects : DIRECTORIES) {
            String getterName = (String) objects[0];
            Boolean enabledNewFolder = (Boolean) objects[1];

            fileChooser.setCurrentDirectory(getWin32Folder(getterName));

            if (newFolderButton.isEnabled() != enabledNewFolder) {
                fail("Enabled state of NewFolderButton should be " + enabledNewFolder +
                        " for Win32ShellFolderManager2." + getterName + "()");
            }
        }
    }

    private JButton findNewFolderButton(Container container) {
        JButton result = null;

        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponent(i);

            if (c instanceof JButton && newFolderToolTipText.equals(((JButton) c).getToolTipText())) {
                if (result != null) {
                    fail("Two or more NewFolderButton found in FileChooser");
                }

                result = (JButton) c;
            }

            if (c instanceof Container) {
                JButton button = findNewFolderButton((Container) c);

                if (result == null) {
                    result = button;
                } else {
                    if (button != null) {
                        fail("Two or more NewFolderButton found in FileChooser");
                    }
                }
            }
        }

        return result;
    }

    private ShellFolder getWin32Folder(String getterName) {
        try {
            Class win32ShellFolderManager2 = Class.forName("sun.awt.shell.Win32ShellFolderManager2");

            Method method = win32ShellFolderManager2.getDeclaredMethod(getterName);
            method.setAccessible(true);

            return (ShellFolder) method.invoke(null);
        } catch (Exception e) {
            fail("Cannot call '" + getterName + "' in the Win32ShellFolderManager2 class", e);

            return null;
        }
    }

    private void fail(String s) {
        throw new RuntimeException("Test failed: " + s);
    }

    private void fail(String s, Throwable e) {
        throw new RuntimeException("Test failed for LookAndFeel " + lookAndFeel + ": " + s, e);
    }
}
