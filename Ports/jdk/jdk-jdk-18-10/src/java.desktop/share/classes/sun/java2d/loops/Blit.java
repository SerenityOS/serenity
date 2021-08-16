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

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.ref.WeakReference;

import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.SpanIterator;

/**
 * Blit
 * 1) copies rectangle of pixels from one surface to another
 * 2) performs compositing of colors based upon a Composite
 *    parameter
 *
 * precise behavior is undefined if the source surface
 * and the destination surface are the same surface
 * with overlapping regions of pixels
 */

public class Blit extends GraphicsPrimitive
{
    public static final String methodSignature = "Blit(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    private static RenderCache blitcache = new RenderCache(20);

    public static Blit locate(SurfaceType srctype,
                              CompositeType comptype,
                              SurfaceType dsttype)
    {
        return (Blit)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    public static Blit getFromCache(SurfaceType src,
                                    CompositeType comp,
                                    SurfaceType dst)
    {
        Object o = blitcache.get(src, comp, dst);
        if (o != null) {
            return (Blit) o;
        }

        Blit blit = locate(src, comp, dst);
        if (blit == null) {
            System.out.println("blit loop not found for:");
            System.out.println("src:  "+src);
            System.out.println("comp: "+comp);
            System.out.println("dst:  "+dst);
        } else {
            blitcache.put(src, comp, dst, blit);
        }
        return blit;
    }

    protected Blit(SurfaceType srctype,
                   CompositeType comptype,
                   SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public Blit(long pNativePrim,
                SurfaceType srctype,
                CompositeType comptype,
                SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    /**
     * All Blit implementors must have this invoker method
     */
    public native void Blit(SurfaceData src, SurfaceData dst,
                            Composite comp, Region clip,
                            int srcx, int srcy,
                            int dstx, int dsty,
                            int width, int height);

    static {
        GraphicsPrimitiveMgr.registerGeneral(new Blit(null, null, null));
    }

    protected GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                              CompositeType comptype,
                                              SurfaceType dsttype)
    {
        /*
        System.out.println("Constructing general blit for:");
        System.out.println("src:  "+srctype);
        System.out.println("comp: "+comptype);
        System.out.println("dst:  "+dsttype);
        */

        if (comptype.isDerivedFrom(CompositeType.Xor)) {
            GeneralXorBlit gxb = new GeneralXorBlit(srctype,
                                                    comptype,
                                                    dsttype);
            setupGeneralBinaryOp(gxb);
            return gxb;
        } else if (comptype.isDerivedFrom(CompositeType.AnyAlpha)) {
            return new GeneralMaskBlit(srctype, comptype, dsttype);
        } else {
            return AnyBlit.instance;
        }
    }

    private static class AnyBlit extends Blit {
        public static AnyBlit instance = new AnyBlit();

        public AnyBlit() {
            super(SurfaceType.Any, CompositeType.Any, SurfaceType.Any);
        }

        public void Blit(SurfaceData srcData,
                         SurfaceData dstData,
                         Composite comp,
                         Region clip,
                         int srcx, int srcy,
                         int dstx, int dsty,
                         int width, int height)
        {
            ColorModel srcCM = srcData.getColorModel();
            ColorModel dstCM = dstData.getColorModel();
            // REMIND: Should get RenderingHints from sg2d
            CompositeContext ctx = comp.createContext(srcCM, dstCM,
                                                      new RenderingHints(null));
            Raster srcRas = srcData.getRaster(srcx, srcy, width, height);
            WritableRaster dstRas =
                (WritableRaster) dstData.getRaster(dstx, dsty, width, height);

            if (clip == null) {
                clip = Region.getInstanceXYWH(dstx, dsty, width, height);
            }
            int[] span = {dstx, dsty, dstx+width, dsty+height};
            SpanIterator si = clip.getSpanIterator(span);
            srcx -= dstx;
            srcy -= dsty;
            while (si.nextSpan(span)) {
                int w = span[2] - span[0];
                int h = span[3] - span[1];
                Raster tmpSrcRas = srcRas.createChild(srcx + span[0], srcy + span[1],
                                                      w, h, 0, 0, null);
                WritableRaster tmpDstRas = dstRas.createWritableChild(span[0], span[1],
                                                                      w, h, 0, 0, null);
                ctx.compose(tmpSrcRas, tmpDstRas, tmpDstRas);
            }
            ctx.dispose();
        }
    }

    private static class GeneralMaskBlit extends Blit {
        MaskBlit performop;

        public GeneralMaskBlit(SurfaceType srctype,
                               CompositeType comptype,
                               SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
            performop = MaskBlit.locate(srctype, comptype, dsttype);
        }

        public void Blit(SurfaceData srcData,
                         SurfaceData dstData,
                         Composite comp,
                         Region clip,
                         int srcx, int srcy,
                         int dstx, int dsty,
                         int width, int height)
        {
            performop.MaskBlit(srcData, dstData, comp, clip,
                               srcx, srcy, dstx, dsty,
                               width, height,
                               null, 0, 0);
        }
    }

    private static class GeneralXorBlit
        extends Blit
        implements GeneralBinaryOp
    {
        Blit convertsrc;
        Blit convertdst;
        Blit performop;
        Blit convertresult;

        WeakReference<SurfaceData> srcTmp;
        WeakReference<SurfaceData> dstTmp;

        public GeneralXorBlit(SurfaceType srctype,
                              CompositeType comptype,
                              SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
        }

        public void setPrimitives(Blit srcconverter,
                                  Blit dstconverter,
                                  GraphicsPrimitive genericop,
                                  Blit resconverter)
        {
            this.convertsrc = srcconverter;
            this.convertdst = dstconverter;
            this.performop = (Blit) genericop;
            this.convertresult = resconverter;
        }

        public synchronized void Blit(SurfaceData srcData,
                                      SurfaceData dstData,
                                      Composite comp,
                                      Region clip,
                                      int srcx, int srcy,
                                      int dstx, int dsty,
                                      int width, int height)
        {
            SurfaceData src, dst;
            Region opclip;
            int sx, sy, dx, dy;

            if (convertsrc == null) {
                src = srcData;
                sx = srcx;
                sy = srcy;
            } else {
                SurfaceData cachedSrc = null;
                if (srcTmp != null) {
                    cachedSrc = srcTmp.get();
                }
                src = convertFrom(convertsrc, srcData, srcx, srcy,
                                  width, height, cachedSrc);
                sx = 0;
                sy = 0;
                if (src != cachedSrc) {
                    srcTmp = new WeakReference<>(src);
                }
            }

            if (convertdst == null) {
                dst = dstData;
                dx = dstx;
                dy = dsty;
                opclip = clip;
            } else {
                // assert: convertresult != null
                SurfaceData cachedDst = null;
                if (dstTmp != null) {
                    cachedDst = dstTmp.get();
                }
                dst = convertFrom(convertdst, dstData, dstx, dsty,
                                  width, height, cachedDst);
                dx = 0;
                dy = 0;
                opclip = null;
                if (dst != cachedDst) {
                    dstTmp = new WeakReference<>(dst);
                }
            }

            performop.Blit(src, dst, comp, opclip,
                           sx, sy, dx, dy,
                           width, height);

            if (convertresult != null) {
                // assert: convertdst != null
                convertTo(convertresult, dst, dstData, clip,
                          dstx, dsty, width, height);
            }
        }
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceBlit(this);
    }

    private static class TraceBlit extends Blit {
        Blit target;

        public TraceBlit(Blit target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void Blit(SurfaceData src, SurfaceData dst,
                         Composite comp, Region clip,
                         int srcx, int srcy, int dstx, int dsty,
                         int width, int height)
        {
            tracePrimitive(target);
            target.Blit(src, dst, comp, clip,
                        srcx, srcy, dstx, dsty, width, height);
        }
    }
}
