/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * @test
 * @key headful
 * @bug 8020708 8032568 8194943
 * @author Alexander Scherbatiy
 * @summary NLS: mnemonics missing in SwingSet2/JInternalFrame demo
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug8020708
 */
public class bug8020708 {

    private static final Locale[] SUPPORTED_LOCALES = {
        Locale.ENGLISH,
        new Locale("de"),
        new Locale("es"),
        new Locale("fr"),
        new Locale("it"),
        new Locale("ja"),
        new Locale("ko"),
        new Locale("pt", "BR"),
        new Locale("sv"),
        new Locale("zh", "CN"),
        new Locale("zh", "TW")
    };
    private static final String[] LOOK_AND_FEELS = {
        "Nimbus",
        "Windows",
        "Motif"
    };
    private static JInternalFrame internalFrame;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        for (Locale locale : SUPPORTED_LOCALES) {
            for (String laf : LOOK_AND_FEELS) {
                Locale.setDefault(locale);
                if (!installLookAndFeel(laf)) {
                    continue;
                }
                testInternalFrameMnemonic(locale);
            }
        }
    }

    static void testInternalFrameMnemonic(Locale locale) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(250);
        robot.setAutoWaitForIdle(true);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame("Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setUndecorated(true);
                frame.setLocationRelativeTo(null);
                frame.setSize(300, 200);

                JDesktopPane desktop = new JDesktopPane();
                internalFrame = new JInternalFrame("Test");
                internalFrame.setSize(200, 100);
                internalFrame.setClosable(true);
                desktop.add(internalFrame);
                internalFrame.setVisible(true);
                internalFrame.setMaximizable(true);

                frame.getContentPane().add(desktop);
                frame.setVisible(true);
            }
        });

        robot.waitForIdle();

        Point clickPoint = Util.getCenterPoint(internalFrame);
        robot.mouseMove(clickPoint.x, clickPoint.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(500);

        Util.hitKeys(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_ESCAPE);
        robot.delay(500);

        int keyCode = KeyEvent.VK_C;
        String mnemonic = UIManager
                .getString("InternalFrameTitlePane.closeButton.mnemonic");
        try {
            keyCode = Integer.parseInt(mnemonic);
        } catch (NumberFormatException e) {
        }
        System.out.println("keyCode " + keyCode);
        Util.hitKeys(robot, keyCode);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (internalFrame.isVisible()) {
                    throw new RuntimeException("Close mnemonic does not work in "+UIManager.getLookAndFeel() + " for locale " + locale);
                }
                frame.dispose();
            }
        });
        robot.delay(500);
    }

    static final boolean installLookAndFeel(String lafName) throws Exception {
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo info : infos) {
            if (info.getClassName().contains(lafName)) {
                UIManager.setLookAndFeel(info.getClassName());
                return true;
            }
        }
        return false;
    }
}
