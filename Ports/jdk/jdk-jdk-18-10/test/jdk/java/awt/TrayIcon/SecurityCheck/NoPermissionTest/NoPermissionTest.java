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
 * @summary Check for SecurityException occurrence if no permissions for system tray granted
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main/othervm/policy=tray.policy -Djava.security.manager NoPermissionTest
 */

public class NoPermissionTest {

    public static void main(String[] args) {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported on this platform. Marking the test passed");
        } else {

            BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics gr = im.createGraphics();
            gr.setColor(Color.white);
            gr.fillRect(0, 0, 16, 16);

            try {
                SystemTray.getSystemTray();
                throw new RuntimeException("FAIL: SecurityException not thrown by getSystemTray method");
            } catch (SecurityException ex) {
                if (!ex.getMessage().matches(".+java.awt.AWTPermission.+accessSystemTray.*"))
                    throw new RuntimeException("FAIL: Security exception thrown due to unexpected reason");
            }

            try {
                TrayIcon icon = new TrayIcon(im, "Caption");
                throw new RuntimeException("FAIL: SecurityException not thrown by TrayIcon constructor");
            } catch (SecurityException ex) {
                if (!ex.getMessage().matches(".+java.awt.AWTPermission.+accessSystemTray.*"))
                    throw new RuntimeException("FAIL: Security exception thrown due to unexpected reason");
            }
        }
    }

}
