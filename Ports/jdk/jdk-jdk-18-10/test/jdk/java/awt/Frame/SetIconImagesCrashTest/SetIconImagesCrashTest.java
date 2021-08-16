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
 * @test
 * @key headful
 * @bug 8166980
 * @summary Test to check Window.setIconImages() does not result in crash when
 * a frame is shown
 * @run main/othervm SetIconImagesCrashTest
 * @run main/othervm -Dsun.java2d.uiScale=2 SetIconImagesCrashTest
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.Frame;
import java.util.List;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class SetIconImagesCrashTest {

    public static void main(String[] args) throws Exception {

        List<BufferedImage> imageList = new ArrayList<BufferedImage>();
        imageList.add(new BufferedImage(200, 200,
                                BufferedImage.TYPE_BYTE_BINARY));

        for (int i = 0; i < 10; i++) {
            Frame f = new Frame();
            test(f, imageList);
        }
    }

    public static void test(final Window window,
            final List<BufferedImage> imageList) throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                for (BufferedImage image : imageList) {
                    Graphics graphics = image.getGraphics();
                    graphics.setColor(Color.RED);
                    graphics.fillRect(
                            0, 0, image.getWidth(), image.getHeight());
                    graphics.dispose();
                }

                window.setIconImages(imageList);
                window.setSize(200, 200);
                window.setVisible(true);
            }
        });

        while (!window.isVisible()) {
            Thread.sleep((long) (20));
        }

        Thread.sleep((long) (50));

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                window.setVisible(false);
                window.dispose();
            }
        });
    }
}

