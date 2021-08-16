/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import sun.awt.image.BufImgSurfaceData;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;

/**
 * BlitBg
 * 1) copies rectangle of pixels from one surface to another
 * 2) performs compositing of colors based upon a Composite
 *    parameter
 * 3) assumes that non-opaque pixels are to be blended with
 *    the indicated Bg color before compositing with the
 *    destination
 *
 * precise behavior is undefined if the source surface
 * and the destination surface are the same surface
 * with overlapping regions of pixels
 */
public class BlitBg extends GraphicsPrimitive
{
    public static final String methodSignature = "BlitBg(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    private static RenderCache blitcache = new RenderCache(20);

    public static BlitBg locate(SurfaceType srctype,
                                CompositeType comptype,
                                SurfaceType dsttype)
    {
        return (BlitBg)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    public static BlitBg getFromCache(SurfaceType src,
                                      CompositeType comp,
                                      SurfaceType dst)
    {
        Object o = blitcache.get(src, comp, dst);
        if (o != null) {
            return (BlitBg) o;
        }
        BlitBg blit = locate(src, comp, dst);
        if (blit == null) {
            System.out.println("blitbg loop not found for:");
            System.out.println("src:  "+src);
            System.out.println("comp: "+comp);
            System.out.println("dst:  "+dst);
        } else {
            blitcache.put(src, comp, dst, blit);
        }
        return blit;
    }

    protected BlitBg(SurfaceType srctype,
                     CompositeType comptype,
                     SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public BlitBg(long pNativePrim,
                  SurfaceType srctype,
                  CompositeType comptype,
                  SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    /**
     * All BlitBg implementors must have this invoker method
     */
    public native void BlitBg(SurfaceData src, SurfaceData dst,
                              Composite comp, Region clip,
                              int bgColor,
                              int srcx, int srcy,
                              int dstx, int dsty,
                              int width, int height);

    static {
        GraphicsPrimitiveMgr.registerGeneral(new BlitBg(null, null, null));
    }

    protected GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                              CompositeType comptype,
                                              SurfaceType dsttype)
    {
        /*
        System.out.println("Constructing general blitbg for:");
        System.out.println("src:  "+srctype);
        System.out.println("comp: "+comptype);
        System.out.println("dst:  "+dsttype);
        */
        return new General(srctype, comptype, dsttype);
    }

    private static class General extends BlitBg {
        CompositeType compositeType;

        public General(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
            compositeType = comptype;
        }

        @Override
        public void BlitBg(SurfaceData srcData,
                           SurfaceData dstData,
                           Composite comp,
                           Region clip,
                           int bgArgb,
                           int srcx, int srcy,
                           int dstx, int dsty,
                           int width, int height)
        {
            ColorModel dstModel = dstData.getColorModel();
            boolean bgHasAlpha = (bgArgb >>> 24) != 0xff;
            if (!dstModel.hasAlpha() && bgHasAlpha) {
                dstModel = ColorModel.getRGBdefault();
            }
            WritableRaster wr =
                dstModel.createCompatibleWritableRaster(width, height);
            boolean isPremult = dstModel.isAlphaPremultiplied();
            BufferedImage bimg =
                new BufferedImage(dstModel, wr, isPremult, null);
            SurfaceData tmpData = BufImgSurfaceData.createData(bimg);
            Color bgColor = new Color(bgArgb, bgHasAlpha);
            SunGraphics2D sg2d = new SunGraphics2D(tmpData, bgColor, bgColor,
                                                   defaultFont);
            FillRect fillop = FillRect.locate(SurfaceType.AnyColor,
                                              CompositeType.SrcNoEa,
                                              tmpData.getSurfaceType());
            Blit combineop = Blit.getFromCache(srcData.getSurfaceType(),
                                               CompositeType.SrcOverNoEa,
                                               tmpData.getSurfaceType());
            Blit blitop = Blit.getFromCache(tmpData.getSurfaceType(), compositeType,
                                            dstData.getSurfaceType());
            fillop.FillRect(sg2d, tmpData, 0, 0, width, height);
            combineop.Blit(srcData, tmpData, AlphaComposite.SrcOver, null,
                           srcx, srcy, 0, 0, width, height);
            blitop.Blit(tmpData, dstData, comp, clip,
                        0, 0, dstx, dsty, width, height);
        }

        private static Font defaultFont = new Font("Dialog", Font.PLAIN, 12);
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceBlitBg(this);
    }

    private static class TraceBlitBg extends BlitBg {
        BlitBg target;

        public TraceBlitBg(BlitBg target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        @Override
        public void BlitBg(SurfaceData src, SurfaceData dst,
                           Composite comp, Region clip,
                           int bgColor,
                           int srcx, int srcy, int dstx, int dsty,
                           int width, int height)
        {
            tracePrimitive(target);
            target.BlitBg(src, dst, comp, clip, bgColor,
                          srcx, srcy, dstx, dsty, width, height);
        }
    }
}
