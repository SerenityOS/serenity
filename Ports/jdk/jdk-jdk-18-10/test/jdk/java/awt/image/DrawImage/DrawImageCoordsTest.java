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

/*
 * @test
 * @bug 8028539
 * @summary Test that drawing a scaled image terminates.
 * @run main/othervm/timeout=60 DrawImageCoordsTest
*/

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class DrawImageCoordsTest {

    public static void main(String[] args) {

        /* Create an image to draw, filled in solid red. */
        BufferedImage srcImg =
             new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics srcG = srcImg.createGraphics();
        srcG.setColor(Color.red);
        int w = srcImg.getWidth(null);
        int h = srcImg.getHeight(null);
        srcG.fillRect(0, 0, w, h);

        /* Create a destination image */
        BufferedImage dstImage =
           new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D dstG = dstImage.createGraphics();
        /* draw image under a scaling transform that overflows int */
        AffineTransform tx = new AffineTransform(0.5, 0, 0, 0.5,
                                                  0, 5.8658460197478485E9);
        dstG.setTransform(tx);
        dstG.drawImage(srcImg, 0, 0, null );
        /* draw image under the same overflowing transform, cancelling
         * out the 0.5 scale on the graphics
         */
        dstG.drawImage(srcImg, 0, 0, 2*w, 2*h, null);
        if (Color.red.getRGB() == dstImage.getRGB(w/2, h/2)) {
             throw new RuntimeException("Unexpected color: clipping failed.");
        }
        System.out.println("Test Thread Completed");
    }
}
