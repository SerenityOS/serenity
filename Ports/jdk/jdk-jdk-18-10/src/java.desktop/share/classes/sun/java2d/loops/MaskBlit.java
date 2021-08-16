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
import java.lang.ref.WeakReference;

import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;

/**
 * MaskBlit
 * 1) copies rectangle of pixels from one surface to another
 * 2) performs compositing of colors based upon a Composite
 *    parameter
 * 3) blends result of composite with destination using an
 *    alpha coverage mask
 * 4) the mask may be null in which case it should be treated
 *    as if it were an array of all opaque values (0xff)
 *
 * precise behavior is undefined if the source surface
 * and the destination surface are the same surface
 * with overlapping regions of pixels
 */

public class MaskBlit extends GraphicsPrimitive
{
    public static final String methodSignature = "MaskBlit(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    private static RenderCache blitcache = new RenderCache(20);

    public static MaskBlit locate(SurfaceType srctype,
                                  CompositeType comptype,
                                  SurfaceType dsttype)
    {
        return (MaskBlit)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    public static MaskBlit getFromCache(SurfaceType src,
                                        CompositeType comp,
                                        SurfaceType dst)
    {
        Object o = blitcache.get(src, comp, dst);
        if (o != null) {
            return (MaskBlit) o;
        }
        MaskBlit blit = locate(src, comp, dst);
        if (blit == null) {
            System.out.println("mask blit loop not found for:");
            System.out.println("src:  "+src);
            System.out.println("comp: "+comp);
            System.out.println("dst:  "+dst);
        } else {
            blitcache.put(src, comp, dst, blit);
        }
        return blit;
    }

    protected MaskBlit(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public MaskBlit(long pNativePrim,
                    SurfaceType srctype,
                    CompositeType comptype,
                    SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    /**
     * All MaskBlit implementors must have this invoker method
     */
    public native void MaskBlit(SurfaceData src, SurfaceData dst,
                                Composite comp, Region clip,
                                int srcx, int srcy,
                                int dstx, int dsty,
                                int width, int height,
                                byte[] mask, int maskoff, int maskscan);

    static {
        GraphicsPrimitiveMgr.registerGeneral(new MaskBlit(null, null, null));
    }

    protected GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                              CompositeType comptype,
                                              SurfaceType dsttype)
    {
        /*
        new Throwable().printStackTrace();
        System.out.println("Constructing general maskblit for:");
        System.out.println("src:  "+srctype);
        System.out.println("comp: "+comptype);
        System.out.println("dst:  "+dsttype);
        */

        if (CompositeType.Xor.equals(comptype)) {
            throw new InternalError("Cannot construct MaskBlit for " +
                                    "XOR mode");
        }

        General ob = new General(srctype, comptype, dsttype);
        setupGeneralBinaryOp(ob);
        return ob;
    }

    private static class General
        extends MaskBlit
        implements GeneralBinaryOp
    {
        Blit convertsrc;
        Blit convertdst;
        MaskBlit performop;
        Blit convertresult;

        WeakReference<SurfaceData> srcTmp;
        WeakReference<SurfaceData> dstTmp;

        public General(SurfaceType srctype,
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
            this.performop = (MaskBlit) genericop;
            this.convertresult = resconverter;
        }

        public synchronized void MaskBlit(SurfaceData srcData,
                                          SurfaceData dstData,
                                          Composite comp,
                                          Region clip,
                                          int srcx, int srcy,
                                          int dstx, int dsty,
                                          int width, int height,
                                          byte[] mask, int offset, int scan)
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

            performop.MaskBlit(src, dst, comp, opclip,
                               sx, sy, dx, dy, width, height,
                               mask, offset, scan);

            if (convertresult != null) {
                // assert: convertdst != null
                convertTo(convertresult, dst, dstData, clip,
                          dstx, dsty, width, height);
            }
        }
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceMaskBlit(this);
    }

    private static class TraceMaskBlit extends MaskBlit {
        MaskBlit target;

        public TraceMaskBlit(MaskBlit target) {
            // We need to have the same NativePrim as our
            // target in case we are used with a TransformHelper
            super(target.getNativePrim(),
                  target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void MaskBlit(SurfaceData src, SurfaceData dst,
                             Composite comp, Region clip,
                             int srcx, int srcy, int dstx, int dsty,
                             int width, int height,
                             byte[] mask, int maskoff, int maskscan)
        {
            tracePrimitive(target);
            target.MaskBlit(src, dst, comp, clip,
                            srcx, srcy, dstx, dsty, width, height,
                            mask, maskoff, maskscan);
        }
    }
}
