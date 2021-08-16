/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;

/*
 * @test
 * @summary Check that BufferedImage constructors and methods do not throw
 *          unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessBufferedImage
 */

public class HeadlessBufferedImage {

    public static void main(String args[]) {
        BufferedImage bi;
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_3BYTE_BGR);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_4BYTE_ABGR);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_BYTE_BINARY);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_BYTE_GRAY);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_BYTE_INDEXED);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB_PRE);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_BGR);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_USHORT_565_RGB);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_USHORT_GRAY);
        bi = new BufferedImage(300, 300, BufferedImage.TYPE_USHORT_555_RGB);
        bi.getType();
        bi.getColorModel();
        bi.getRaster();
        bi.getAlphaRaster();
        bi.getRGB(1, 1);
        bi.getWidth();
        bi.getHeight();
        bi.getSource();
        bi.flush();
        bi.getGraphics();
        bi.createGraphics();
        BufferedImage bi2 = bi.getSubimage(10, 10, 200, 200);
        bi.isAlphaPremultiplied();
        bi.coerceData(true);
        bi.coerceData(false);
        bi.toString();
        bi.getSources();
        bi.getPropertyNames();
        bi.getMinX();
        bi.getMinY();
        bi.getSampleModel();
        bi.getNumXTiles();
        bi.getNumYTiles();
        bi.getMinTileX();
        bi.getMinTileY();
        bi.getTileWidth();
        bi.getTileHeight();
        bi.getTileGridXOffset();
        bi.getTileGridYOffset();
        bi.getData();
    }
}
