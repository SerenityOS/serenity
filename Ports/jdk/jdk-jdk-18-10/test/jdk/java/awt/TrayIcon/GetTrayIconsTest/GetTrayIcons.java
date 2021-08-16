/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @summary Check the getTrayIcons method of the SystemTray
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main GetTrayIcons
 */

public class GetTrayIcons {

    Image image;

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported())
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        else
            new GetTrayIcons().doTest();
    }

    GetTrayIcons() {
        image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
    }

    void doTest() throws Exception {
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon[] icons = tray.getTrayIcons();
        if (icons == null || icons.length > 0)
            throw new RuntimeException("FAIL: getTrayIcons() returned incorrect " +
                    "value when no icons are added " + icons);

        TrayIcon icon = new TrayIcon(image);
        tray.add(icon);

        icons = tray.getTrayIcons();
        if (icons == null || icons.length != 1)
            throw new RuntimeException("FAIL: getTrayIcons() returned incorrect value " +
                    "when one icon present " + icons);

        icon = new TrayIcon(image);
        tray.add(icon);

        icons = tray.getTrayIcons();
        if (icons == null || icons.length != 2)
            throw new RuntimeException("FAIL: getTrayIcons() returned incorrect value " +
                    "when two icons present " + icons);

        icons = tray.getTrayIcons();
        if (icons != null) {
            for (int i = 0; i < icons.length; i++) {
                tray.remove(icons[i]);
            }

            TrayIcon[] newList = tray.getTrayIcons();

            if (newList == null || newList.length != 0)
                throw new RuntimeException("FAIL: Incorrect value returned by getTrayIcons " +
                        "after icons are added and then removed " + newList);
        }
    }
}
