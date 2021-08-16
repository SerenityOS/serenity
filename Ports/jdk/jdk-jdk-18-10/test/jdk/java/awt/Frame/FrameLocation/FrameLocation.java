/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6895647
  @summary X11 Frame locations should be what we set them to
  @author anthony.petrov@oracle.com: area=awt.toplevel
  @run main FrameLocation
 */

import java.awt.*;

public class FrameLocation {
    private static final int X = 250;
    private static final int Y = 250;

    public static void main(String[] args) {
        Frame f = new Frame("test");
        f.setBounds(X, Y, 250, 250); // the size doesn't matter
        f.setVisible(true);

        for (int i = 0; i < 10; i++) {
            // 2 seconds must be enough for the WM to show the window
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }

            // Check the location
            int x = f.getX();
            int y = f.getY();

            if (x != X || y != Y) {
                throw new RuntimeException("The frame location is wrong! Current: " + x + ", " + y + ";  expected: " + X + ", " + Y);
            }

            // Emulate what happens when setGraphicsConfiguration() is called
            synchronized (f.getTreeLock()) {
                f.removeNotify();
                f.addNotify();
            }
        }

        f.dispose();
    }
}

