/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.*;
import java.awt.image.*;
import java.awt.print.PageFormat;
import java.nio.ByteBuffer;

import sun.java2d.*;
import sun.java2d.loops.SurfaceType;

public class CPrinterSurfaceData extends OSXSurfaceData{
    public static final String DESC_INT_RGB_PQ = "Integer RGB Printer Quartz";
//    public static final String DESC_INT_ARGB_PQ = "Integer ARGB Printer Quartz";

//    public static final SurfaceType IntArgbPQ = SurfaceType.IntArgb.deriveSubType(DESC_INT_ARGB_PQ);
    public static final SurfaceType IntRgbPQ = SurfaceType.IntRgb.deriveSubType(DESC_INT_RGB_PQ);

    static SurfaceData createData(PageFormat pf, long context) {
        return new CPrinterSurfaceData(CPrinterGraphicsConfig.getConfig(pf), context);
    }

    private CPrinterSurfaceData(GraphicsConfiguration gc, long context) {
        super(IntRgbPQ, gc.getColorModel(), gc, gc.getBounds());
        initOps(context, this.fGraphicsStates, this.fGraphicsStatesObject, gc.getBounds().width, gc.getBounds().height);
    }

    public SurfaceData getReplacement() {
        return this;
    }

    private native void initOps(long context, ByteBuffer byteParameters, Object[] objectParameters, int width, int height);

    public void enableFlushing() {
        _flush();
    }
    native void _flush();

    public Object getDestination() {
        // this should never get called for the printer surface (see BufferStrategyPaintManager for one case of usage)
        return null;
    }

    public Raster getRaster(int x, int y, int w, int h) {
        BufferedImage dstImage = new BufferedImage(x + w, y + h, BufferedImage.TYPE_INT_ARGB_PRE);
        return dstImage.getRaster();
    }

    public BufferedImage copyArea(SunGraphics2D sg2d, int x, int y, int w, int h, BufferedImage dstImage) {
        // create the destination image if needed
        if (dstImage == null) {
            dstImage = getDeviceConfiguration().createCompatibleImage(w, h);
        }

        // copy
        Graphics g = dstImage.createGraphics();
        BufferedImage thisImage = getCompositingImage(w, h);
        g.drawImage(thisImage, 0, 0, w, h, x, y, x+w, y+h, null);
        g.dispose();

        return dstImage;
    }

    public boolean xorSurfacePixels(SunGraphics2D sg2d, BufferedImage srcPixels, int x, int y, int w, int h, int colorXOR) {
        throw new InternalError("not implemented yet");
    }
}
