/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 6583251 8217377
 * @summary One more ClassCastException in Swing with TrayIcon
 * @author Alexander Potochkin
 * @run main bug6583251
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class bug6583251 {
    private static JFrame frame;
    private static JPopupMenu menu;

    private static void createGui() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        menu = new JPopupMenu();
        menu.add(new JMenuItem("item"));
        panel.setComponentPopupMenu(menu);
        frame.add(panel);

        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        if (SystemTray.isSupported()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGui();
                }
            });

            Robot robot = new Robot();
            robot.waitForIdle();
            menu.show(frame, 0, 0);
            robot.waitForIdle();

            TrayIcon trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
            MouseEvent ev = new MouseEvent(
                    new JButton(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 0, 0, 1, false);
            ev.setSource(trayIcon);
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ev);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.dispose();
                }
            });

        } else {
            System.out.println("SystemTray not supported. " + "Skipping the test.");
        }
    }
}
