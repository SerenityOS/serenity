/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @bug 7153700
 * @summary Check for mouseMoved event for java.awt.TrayIcon
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client ../
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main MouseMovedTest
 */

public class MouseMovedTest {
    static volatile boolean moved;

    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
            return;
        }

        if (SystemTrayIconHelper.isOel7()) {
            return;
        }

        moved = false;

        TrayIcon icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "Test icon");
        icon.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent event) {
                moved = true;
                System.out.println("Mouse moved");
            }
        });
        SystemTray.getSystemTray().add(icon);

        ExtendedRobot robot = new ExtendedRobot();
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        if (System.getProperty("os.name").toLowerCase().startsWith("win"))
            robot.glide(size.width / 2, size.height-15, size.width, size.height-15, 1, 3);
        else
            robot.glide(size.width / 2, 13, size.width, 13, 1, 3);
        robot.mouseMove(size.width/2, size.height/2);

        if (!moved)
            throw new RuntimeException("Mouse moved action did not trigger");
    }
}
