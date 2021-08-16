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
 * @summary Test the remove method of the TrayIcon
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main TrayIconRemoveTest
 */

public class TrayIconRemoveTest {

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                               "Marking the test passed");
        } else {
            new TrayIconRemoveTest().doTest();
        }
    }

    private void doTest() throws Exception {
        Image image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        SystemTray tray = SystemTray.getSystemTray();
        tray.remove(null);

        TrayIcon icon1 = new TrayIcon(image);
        tray.add(icon1);

        tray.remove(icon1);

        TrayIcon[] icons = tray.getTrayIcons();
        if (icons.length != 0)
            throw new RuntimeException("FAIL: There are icons still present even after " +
                    "removing the added icon" + "\n"+
                    "No. of icons present: " + icons.length);

        TrayIcon icon2 = new TrayIcon(image);
        tray.remove(icon2);

        TrayIcon icon3 = new TrayIcon(image);
        tray.add(icon3);

        TrayIcon newIcon = new TrayIcon(image);
        tray.remove(newIcon);

        tray.remove(null);
    }
}
