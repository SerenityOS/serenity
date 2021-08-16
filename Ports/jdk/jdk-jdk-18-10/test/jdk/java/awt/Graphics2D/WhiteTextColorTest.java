/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8056009
 * @summary tests whether Graphics.setColor-calls with Color.white are ignored directly
 *          after pipeline initialization for a certain set of operations.
 * @author ceisserer
 */

public class WhiteTextColorTest extends Frame {
    public static volatile boolean success = false;

    public WhiteTextColorTest() {
        Image dstImg = getGraphicsConfiguration()
                .createCompatibleVolatileImage(30, 20);
        Graphics g = dstImg.getGraphics();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, dstImg.getWidth(null), dstImg.getHeight(null));
        g.setColor(Color.WHITE);
        g.drawString("Test", 0, 15);

        BufferedImage readBackImg = new BufferedImage(dstImg.getWidth(null),
                dstImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
        readBackImg.getGraphics().drawImage(dstImg, 0, 0, null);

        for (int x = 0; x < readBackImg.getWidth(); x++) {
            for (int y = 0; y < readBackImg.getHeight(); y++) {
                int pixel = readBackImg.getRGB(x, y);

                // In case a single white pixel is found, the
                // setColor(Color.WHITE)
                // call before was not ignored and the bug is not present
                if (pixel == 0xFFFFFFFF) {
                    return;
                }
            }
        }

        throw new RuntimeException("Test Failed");
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new WhiteTextColorTest();
            }
        });
    }
}

