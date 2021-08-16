/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.*;
import javax.swing.*;

/**
 * @test
 * @key headful
 * @bug 8162591
 * @summary tests gradients with start/endpoints exceeding Short.MAX coordinates
 * @author ceisserer
 */

public class HugeGradientTest extends Frame {
        public static volatile boolean success = false;

        public HugeGradientTest() {
                Image dstImg = getGraphicsConfiguration()
                        .createCompatibleVolatileImage(30, 30);
                Graphics2D g = (Graphics2D) dstImg.getGraphics();

                g.setPaint(new LinearGradientPaint(0f, Short.MAX_VALUE, 0f, Short.MAX_VALUE +31,
                        new float[]{0f, 1f}, new Color[]{Color.BLACK, Color.RED}));
                g.translate(0, -Short.MAX_VALUE);
                g.fillRect (0, 0, Short.MAX_VALUE*2 , Short.MAX_VALUE*2);

                BufferedImage readBackImg = new BufferedImage(dstImg.getWidth(null),
                dstImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
                readBackImg.getGraphics().drawImage(dstImg, 0, 0, null);

                for (int x = 0; x < readBackImg.getWidth(); x++) {
                        for (int y = 0; y < readBackImg.getHeight(); y++) {
                                int redVal = (readBackImg.getRGB(x, y) & 0x00FF0000) >> 16;

                                if (redVal > 127) {
                                        return;
                                }
                        }
                }

                throw new RuntimeException("Test Failed");
        }

        public static void main(String[] args) throws Exception {
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                new HugeGradientTest();
                        }
                });
        }
}
