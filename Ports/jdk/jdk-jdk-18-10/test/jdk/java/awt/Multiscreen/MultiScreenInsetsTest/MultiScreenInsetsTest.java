/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8020443
  @summary Frame is not created on the specified GraphicsDevice with two
monitors
  @author Oleg Pekhovskiy
  @library /test/lib
  @build jdk.test.lib.Platform
  @run main MultiScreenInsetsTest
 */

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import jdk.test.lib.Platform;

public class MultiScreenInsetsTest {
    private static final int SIZE = 100;

    public static void main(String[] args) throws InterruptedException {
        if (!Platform.isLinux()) {
            System.out.println("This test is for Linux only..." +
                               "skipping!");
            return;
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2) {
            System.out.println("It's a multi-screen test... skipping!");
            return;
        }

        for (int screen = 0; screen < gds.length; ++screen) {
            GraphicsDevice gd = gds[screen];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

            Frame frame = new Frame(gc);
            frame.setLocation(bounds.x + (bounds.width - SIZE) / 2,
                              bounds.y + (bounds.height - SIZE) / 2);
            frame.setSize(SIZE, SIZE);
            frame.setUndecorated(true);
            frame.setVisible(true);

            // Maximize Frame to reach the struts
            frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
            Thread.sleep(2000);

            Rectangle frameBounds = frame.getBounds();
            frame.dispose();
            if (bounds.x + insets.left != frameBounds.x
                || bounds.y + insets.top != frameBounds.y
                || bounds.width - insets.right - insets.left != frameBounds.width
                || bounds.height - insets.bottom - insets.top != frameBounds.height) {
                throw new RuntimeException("Test FAILED! Wrong screen #" +
                                           screen + " insets: " + insets);
            }
        }
        System.out.println("Test PASSED!");
    }
}
