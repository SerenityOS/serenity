/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
  test
  @bug 6299866
  @summary Tests that no NPE is thrown when the tray icon is disposed from the
  handler of action event caused by clicking on this icon.
  @library ../../regtesthelpers
  @build Sysout
  @author artem.ananiev: area=awt.tray
  @run applet/manual=yesno DisposeInActionEventTest.html
*/

import java.applet.*;

import java.awt.*;
import java.awt.image.*;

import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.Sysout;

public class DisposeInActionEventTest extends Applet {
    private boolean traySupported;

    private SystemTray systemTray;
    private TrayIcon trayIcon;

    public void init() {
        this.setLayout(new BorderLayout());

        String[] instructions;
        traySupported = SystemTray.isSupported();
        if (!traySupported) {
            instructions = new String[]{
                    "The test cannot be run because SystemTray is not supported.",
                    "Simply press PASS button."
            };
        } else {
            String clickInstruction;
            if (Platform.isOSX()) {
                clickInstruction = "right";
            } else {
                clickInstruction = "left";
            }
            instructions = new String[]{
                    "When the test starts, it adds the icon to the tray aread. If you",
                    "  don't see a tray icon, please, make sure that the tray area",
                    "  (also called Taskbar Status Area on MS Windows, Notification",
                    "  Area on Gnome or System Tray on KDE) is visible.",
                    "Double-click with " + clickInstruction + " button on the tray icon to trigger the",
                    "  action event. Brief information about action events is printed",
                    "  below. After each action event the tray icon is removed from",
                    "  the tray and then added back in a second.",
                    "The test performs some automatic checks when removing the icon. If",
                    "  something is wrong the corresponding message is displayed below.",
                    "  Repeat double-clicks several times. If no 'Test FAILED' messages",
                    "  are printed, press PASS button else FAIL button."
            };
        }
        Sysout.createDialogWithInstructions(instructions);
    }

    @Override
    public void start() {
        setSize(200, 200);
        setVisible(true);
        validate();

        if (!traySupported) return;

        System.setProperty("sun.awt.exception.handler", "DisposeInActionEventTest$EDTExceptionHandler");

        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.RED);
        g.fillRect(6, 6, 20, 20);
        g.dispose();

        systemTray = SystemTray.getSystemTray();
        trayIcon = new TrayIcon(img);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(ev -> {
            Sysout.println(ev.toString());
            systemTray.remove(trayIcon);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    systemTray.add(trayIcon);
                } catch (AWTException | InterruptedException e) {
                    Sysout.println(e.toString());
                    Sysout.println("!!! The test coudn't be performed !!!");
                }
            }
            ).start();
        }
        );

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            Sysout.println(e.toString());
            Sysout.println("!!! The test coudn't be performed !!!");
        }
    }
}
