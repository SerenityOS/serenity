/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 5097801 8163270
  @summary Tests that no mouse events are sent to component if robot is
           moving mouse on another screen, Xinerama
  @run main SpuriousMouseEvents
 */
import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Robot;
import java.awt.GraphicsConfiguration;
import java.awt.Frame;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;

public class SpuriousMouseEvents {

    private static volatile boolean testPassed = true;

    public static void main(String args[]) throws AWTException {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2) {
            return;
        }

        Robot r = null;
        try {
            r = new Robot();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create AWT robot" + e);
        }

        for (int i = 1; i >= 0; i--) {
            GraphicsDevice gd = gds[i];
            GraphicsDevice gdo = gds[1 - i];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            GraphicsConfiguration gco = gdo.getDefaultConfiguration();
            Frame f = new Frame("Frame", gc);
            f.setBounds(gc.getBounds().x + 100, gc.getBounds().y + 100, 200, 200);
            f.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent me) {
                    testPassed = false;
                }
            });
            f.setVisible(true);

            r = new Robot(gdo);
            int x = (int) gco.getBounds().x;
            for (int j = x; j < x + 400; j += 10) {
                r.mouseMove(j, 200);
                r.delay(10);
            }
            r.delay(1000);

            f.setVisible(false);
            f.dispose();

            if (!testPassed) {
                break;
            }
        }

        if (!testPassed) {
            throw new RuntimeException("Wrong mouse events are sent");
        }
    }
}
