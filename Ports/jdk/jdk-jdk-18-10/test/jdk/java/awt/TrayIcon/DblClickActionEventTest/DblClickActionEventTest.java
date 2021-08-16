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
  @bug 6284070
  @summary Tests that ActionEvent is generated when a tray icon is double-clicked
  @library ../../regtesthelpers
  @build Sysout
  @author artem.ananiev: area=awt.tray
  @run applet/manual=yesno DblClickActionEventTest.html
*/

import java.applet.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.Sysout;

public class DblClickActionEventTest extends Applet {
    boolean traySupported;

    public void init() {
        this.setLayout(new BorderLayout());

        String[] instructions;
        traySupported = SystemTray.isSupported();
        if (traySupported) {
            String clickInstruction;
            if (Platform.isOSX()) {
                clickInstruction = "right";
            } else {
                clickInstruction = "left";
            }
            instructions = new String[]{
                    "When the test starts an icon is added to the SystemTray area.",
                    " Double-click on it with a " + clickInstruction + " button and make sure that",
                    "  ACTION_PERFORMED event is sent to Java (all the clicks and",
                    "  action events are shown below these instructions).",
                    "Then, if your system allows the tray icon to get focus (for",
                    "  example, windows 2000 or windows XP), double-click on the",
                    "  icon with SPACE button and single-click with RETURN button.",
                    "  Both of them must also trigger ACTION_PERFORMED event.",
                    "If you see ACTION_PERFORMED events after each of your actions",
                    "  (either mouse clicks or key presses), press PASS, else FAIL"
            };
        } else {
            instructions = new String[]{
                    "The test cannot be run because SystemTray is not supported.",
                    "Simply press PASS button."
            };
        }
        Sysout.createDialogWithInstructions(instructions);
    }

    public void start() {
        setSize(200, 200);
        setVisible(true);
        validate();

        if (!traySupported) {
            return;
        }

        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.RED);
        g.fillRect(6, 6, 20, 20);
        g.dispose();

        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon icon = new TrayIcon(img);
        icon.setImageAutoSize(true);
        icon.addActionListener(ev -> Sysout.println(ev.toString()));
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                Sysout.println(ev.toString());
            }
        }
        );

        try {
            tray.add(icon);
        } catch (AWTException e) {
            Sysout.println(e.toString());
            Sysout.println("!!! The test coudn't be performed !!!");
        }
    }
}

