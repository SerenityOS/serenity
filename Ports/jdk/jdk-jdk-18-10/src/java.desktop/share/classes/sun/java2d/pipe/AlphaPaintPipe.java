/*
 * Copyright (c) 1997, 2002, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.lang.ref.WeakReference;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.PaintContext;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import sun.awt.image.BufImgSurfaceData;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.MaskBlit;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.GraphicsPrimitiveMgr;

/**
 * This class implements a CompositePipe that renders path alpha tiles
 * into a destination according to the Composite attribute of a
 * SunGraphics2D.
 */
public class AlphaPaintPipe implements CompositePipe {
    static WeakReference<Raster> cachedLastRaster;
    static WeakReference<ColorModel> cachedLastColorModel;
    static WeakReference<SurfaceData> cachedLastData;

    static class TileContext {
        SunGraphics2D sunG2D;
        PaintContext paintCtxt;
        ColorModel paintModel;
        WeakReference<Raster> lastRaster;
        WeakReference<SurfaceData> lastData;
        MaskBlit lastMask;
        Blit     lastBlit;
        SurfaceData dstData;

        public TileContext(SunGraphics2D sg, PaintContext pc) {
            sunG2D = sg;
            paintCtxt = pc;
            paintModel = pc.getColorModel();
            dstData = sg.getSurfaceData();
            synchronized (AlphaPaintPipe.class) {
                if (cachedLastColorModel != null &&
                    cachedLastColorModel.get() == paintModel)
                {
                    this.lastRaster = cachedLastRaster;
                    this.lastData = cachedLastData;
                }
            }
        }
    }

    public Object startSequence(SunGraphics2D sg, Shape s, Rectangle devR,
                                int[] abox) {
        PaintContext paintContext =
            sg.paint.createContext(sg.getDeviceColorModel(),
                                   devR,
                                   s.getBounds2D(),
                                   sg.cloneTransform(),
                                   sg.getRenderingHints());
        return new TileContext(sg, paintContext);
    }

    public boolean needTile(Object context, int x, int y, int w, int h) {
        return true;
    }

    private static final int TILE_SIZE = 32;

    public void renderPathTile(Object ctx,
                               byte[] atile, int offset, int tilesize,
                               int x, int y, int w, int h) {
        TileContext context = (TileContext) ctx;
        PaintContext paintCtxt = context.paintCtxt;
        SunGraphics2D sg = context.sunG2D;
        SurfaceData dstData = context.dstData;
        SurfaceData srcData = null;
        Raster lastRas = null;
        if (context.lastData != null && context.lastRaster != null) {
            srcData = context.lastData.get();
            lastRas = context.lastRaster.get();
            if (srcData == null || lastRas == null) {
                srcData = null;
                lastRas = null;
            }
        }
        ColorModel paintModel = context.paintModel;

        for (int rely = 0; rely < h; rely += TILE_SIZE) {
            int ty = y + rely;
            int th = Math.min(h-rely, TILE_SIZE);
            for (int relx = 0; relx < w; relx += TILE_SIZE) {
                int tx = x + relx;
                int tw = Math.min(w-relx, TILE_SIZE);

                Raster srcRaster = paintCtxt.getRaster(tx, ty, tw, th);
                if ((srcRaster.getMinX() != 0) || (srcRaster.getMinY() != 0)) {
                    srcRaster = srcRaster.createTranslatedChild(0, 0);
                }
                if (lastRas != srcRaster) {
                    lastRas = srcRaster;
                    context.lastRaster = new WeakReference<>(lastRas);
                    // REMIND: This will fail for a non-Writable raster!
                    BufferedImage bImg =
                        new BufferedImage(paintModel,
                                          (WritableRaster) srcRaster,
                                          paintModel.isAlphaPremultiplied(),
                                          null);
                    srcData = BufImgSurfaceData.createData(bImg);
                    context.lastData = new WeakReference<>(srcData);
                    context.lastMask = null;
                    context.lastBlit = null;
                }

                if (atile == null) {
                    if (context.lastBlit == null) {
                        CompositeType comptype = sg.imageComp;
                        if (CompositeType.SrcOverNoEa.equals(comptype) &&
                            paintModel.getTransparency() == Transparency.OPAQUE)
                        {
                            comptype = CompositeType.SrcNoEa;
                        }
                        context.lastBlit =
                            Blit.getFromCache(srcData.getSurfaceType(),
                                              comptype,
                                              dstData.getSurfaceType());
                    }
                    context.lastBlit.Blit(srcData, dstData,
                                          sg.composite, null,
                                          0, 0, tx, ty, tw, th);
                } else {
                    if (context.lastMask == null) {
                        CompositeType comptype = sg.imageComp;
                        if (CompositeType.SrcOverNoEa.equals(comptype) &&
                            paintModel.getTransparency() == Transparency.OPAQUE)
                        {
                            comptype = CompositeType.SrcNoEa;
                        }
                        context.lastMask =
                            MaskBlit.getFromCache(srcData.getSurfaceType(),
                                                  comptype,
                                                  dstData.getSurfaceType());
                    }

                    int toff = offset + rely * tilesize + relx;
                    context.lastMask.MaskBlit(srcData, dstData,
                                              sg.composite, null,
                                              0, 0, tx, ty, tw, th,
                                              atile, toff, tilesize);
                }
            }
        }
    }

    public void skipTile(Object context, int x, int y) {
        return;
    }

    public void endSequence(Object ctx) {
        TileContext context = (TileContext) ctx;
        if (context.paintCtxt != null) {
            context.paintCtxt.dispose();
        }
        synchronized (AlphaPaintPipe.class) {
            if (context.lastData != null) {
                cachedLastRaster = context.lastRaster;
                if (cachedLastColorModel == null ||
                    cachedLastColorModel.get() != context.paintModel)
                {
                    // Avoid creating new WeakReference if possible
                    cachedLastColorModel =
                        new WeakReference<>(context.paintModel);
                }
                cachedLastData = context.lastData;
            }
        }
    }
}
