/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.awt.windows;

import sun.awt.CustomCursor;
import java.awt.*;
import java.awt.image.*;
import sun.awt.image.ImageRepresentation;
import sun.awt.image.IntegerComponentRaster;
import sun.awt.image.ToolkitImage;

/**
 * A class to encapsulate a custom image-based cursor.
 *
 * @see Component#setCursor
 * @author      ThomasBall
 */
@SuppressWarnings("serial") // JDK-implementation class
final class WCustomCursor extends CustomCursor {

    WCustomCursor(Image cursor, Point hotSpot, String name)
            throws IndexOutOfBoundsException {
        super(cursor, hotSpot, name);
    }

    @Override
    protected void createNativeCursor(Image im, int[] pixels, int w, int h,
                                      int xHotSpot, int yHotSpot) {
        BufferedImage bimage = new BufferedImage(w, h,
                               BufferedImage.TYPE_INT_RGB);
        Graphics g = bimage.getGraphics();
        try {
            if (im instanceof ToolkitImage) {
                ImageRepresentation ir = ((ToolkitImage)im).getImageRep();
                ir.reconstruct(ImageObserver.ALLBITS);
            }
            g.drawImage(im, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        Raster  raster = bimage.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        // REMIND: native code should use ScanStride _AND_ width
        int[] data = ((DataBufferInt)buffer).getData();

        byte[] andMask = new byte[w * h / 8];
        int npixels = pixels.length;
        for (int i = 0; i < npixels; i++) {
            int ibyte = i / 8;
            int omask = 1 << (7 - (i % 8));
            if ((pixels[i] & 0xff000000) == 0) {
                // Transparent bit
                andMask[ibyte] |= omask;
            }
        }

        {
            int     ficW = raster.getWidth();
            if( raster instanceof IntegerComponentRaster ) {
                ficW = ((IntegerComponentRaster)raster).getScanlineStride();
            }
            createCursorIndirect(
                ((DataBufferInt)bimage.getRaster().getDataBuffer()).getData(),
                andMask, ficW, raster.getWidth(), raster.getHeight(),
                xHotSpot, yHotSpot);
        }
    }

    private native void createCursorIndirect(int[] rData, byte[] andMask,
                                             int nScanStride, int width,
                                             int height, int xHotSpot,
                                             int yHotSpot);
    /**
     * Return the current value of SM_CXCURSOR.
     */
    static native int getCursorWidth();

    /**
     * Return the current value of SM_CYCURSOR.
     */
    static native int getCursorHeight();
}
