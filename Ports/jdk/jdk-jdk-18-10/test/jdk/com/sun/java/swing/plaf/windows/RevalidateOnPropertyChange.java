/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Robot;
import java.awt.Toolkit;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * @test
 * @key headful
 * @bug 8075255
 * @summary tests if the desktop property changes this will force the UIs to
 *          update all known Frames
 * @requires (os.family == "windows")
 * @modules java.desktop/java.awt:open java.desktop/sun.awt
 */
public final class RevalidateOnPropertyChange {

    private static JFrame frame;
    private static JButton button;
    private static volatile Dimension sizeAfter;
    private static volatile Dimension sizeBefore;
    private static volatile boolean flag;

    public static void main(String[] args) throws Exception {
        System.setProperty("swing.useSystemFontSettings", "true");
        UIManager.put("Application.useSystemFontSettings", true);

        // this functionality is supported on windows in "Windows and Metal L&F"
        test("com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
             "win.defaultGUI.font",
             new Font(Font.DIALOG, FontUIResource.BOLD, 40));
        test("javax.swing.plaf.metal.MetalLookAndFeel",
             "win.ansiVar.font.height", 70);
    }

    /**
     * Emulates the property change via reflection.
     */
    static void test(String laf, String prop, Object value) throws Exception {
        Class cls = Toolkit.class;
        Method setDesktopProperty =
                cls.getDeclaredMethod("setDesktopProperty", String.class,
                                      Object.class);
        setDesktopProperty.setAccessible(true);

        UIManager.setLookAndFeel(laf);
        EventQueue.invokeAndWait(RevalidateOnPropertyChange::createGUI);

        Robot r = new Robot();
        r.waitForIdle();

        EventQueue.invokeAndWait(() -> {
            sizeBefore = button.getSize();
        });

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.addPropertyChangeListener(prop, evt -> flag = true);
        setDesktopProperty.invoke(toolkit, prop, value);
        r.waitForIdle();

        EventQueue.invokeAndWait(() -> {
            sizeAfter = button.getSize();
            frame.dispose();
        });

        if (!flag) {
            throw new RuntimeException("The listener was not notified");
        }
        if (sizeAfter.equals(sizeBefore)) {
            throw new RuntimeException("Size was not changed :" + sizeAfter);
        }
    }

    private static void createGUI() {
        frame = new JFrame();
        button = new JButton(UIManager.getLookAndFeel().getName());
        frame.setLayout(new FlowLayout());
        frame.getContentPane().add(button);
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
