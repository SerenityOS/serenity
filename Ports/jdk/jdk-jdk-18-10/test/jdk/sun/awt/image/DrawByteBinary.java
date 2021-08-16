/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6800846
 *
 * @summary Test verifes that images with short palette are rendered
 *          withourt artifacts.
 *
 * @run     main DrawByteBinary
 */


import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import static java.awt.image.BufferedImage.*;


public class DrawByteBinary {

    public static void main(String args[]) {
        int w = 100, h = 30;
        int x = 10;
        byte[] arr = {(byte)0xff, (byte)0x0, (byte)0x00};

        IndexColorModel newCM = new IndexColorModel(1, 2, arr, arr, arr);
        BufferedImage orig = new BufferedImage(w, h, TYPE_BYTE_BINARY, newCM);
        Graphics2D g2d = orig.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.black);
        g2d.drawLine(x, 0, x, h);
        g2d.dispose();

        IndexColorModel origCM = (IndexColorModel)orig.getColorModel();
        BufferedImage test = new BufferedImage(w, h, TYPE_BYTE_BINARY,origCM);
        g2d = test.createGraphics();
        g2d.drawImage(orig, 0, 0, null);
        g2d.dispose();

        int y = h / 2;

        // we expect white color outside the line
        if (test.getRGB(x - 1, y) != 0xffffffff) {
            throw new RuntimeException("Invalid color outside the line.");
        }

        // we expect black color on the line
        if (test.getRGB(x, y) != 0xff000000) {
            throw new RuntimeException("Invalid color on the line.");
        }
    }
}
