/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8157163 8159132
 * @summary AWT FileDialog does not inherit icon image from parent Frame
 * @requires os.family=="windows"
 * @run main FileDialogIconTest
 */

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public class FileDialogIconTest {
    private static Frame frame;
    private static Dialog dialog;

    public static void main(final String[] args) throws Exception {
        Robot robot;
        Point p;
        try {
            frame = new Frame();
            frame.setIconImage(createImage());
            frame.setVisible(true);
            robot = new Robot();
            robot.waitForIdle();
            robot.delay(200);

            dialog = new FileDialog(frame, "Dialog");
            dialog.setModal(false);
            dialog.setVisible(true);
            robot.waitForIdle();
            robot.delay(1000);

            p = new Point(20, 20);
            SwingUtilities.convertPointToScreen(p, dialog);
            Color color = robot.getPixelColor(p.x, p.y);
            if (!Color.RED.equals(color)) {
                throw new RuntimeException("Dialog icon was not inherited from " +
                        "owning window. Wrong color: " + color);
            }
        } finally {
            if (dialog != null) { dialog.dispose(); }
            if (frame  != null) { frame.dispose();  }
        }
    }

    private static Image createImage() {
        BufferedImage image = new BufferedImage(64, 64,
                                                  BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return image;
    }

}
