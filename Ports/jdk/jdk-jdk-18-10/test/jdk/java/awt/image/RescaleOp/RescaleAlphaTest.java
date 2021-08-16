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
/**
 * @test
 * @bug 8080287 8136354
 * @run main RescaleAlphaTest
 * @summary RescaleOp with scaleFactor/alpha should copy alpha to destination
 * channel
 */
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.Color;
import java.awt.Frame;
import java.io.IOException;

public class RescaleAlphaTest {

    BufferedImage bimg = null, bimg1;
    int w = 10, h = 10;
    float scaleFactor = 0.5f;
    float offset = 0.0f;

    public static void main(String[] args) throws Exception {
        RescaleAlphaTest test = new RescaleAlphaTest();
        test.startTest();
    }

    private void startTest() throws Exception {

        // Test with source image with alpha channel

        bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bimg.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, w, h);

        RescaleOp res = new RescaleOp(scaleFactor, offset, null);
        bimg1 = res.filter(bimg, null);

        // check if destination image has alpha channel copied from src
        checkForAlpha(bimg1);

        // Test with source image without alpha channel
        bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        g2d = bimg.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, w, h);


        res = new RescaleOp(scaleFactor, offset, null);

        // Create destination image with alpha channel
        bimg1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bimg1 = res.filter(bimg, bimg1);

        // check if filtered destination image has alpha channel
        checkForAlpha(bimg1);

    }

    private void checkForAlpha(BufferedImage bi) throws IOException {
        int argb = bi.getRGB(w/2, h/2);
        if ((argb >>> 24) != 255) {
            throw new
            RuntimeException("Wrong alpha in destination image.RescaleOp with alpha failed.");
        }
    }
}
