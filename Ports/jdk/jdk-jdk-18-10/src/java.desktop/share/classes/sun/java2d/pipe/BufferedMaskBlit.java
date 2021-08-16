/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.Composite;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.MaskBlit;
import sun.java2d.loops.SurfaceType;
import static sun.java2d.pipe.BufferedOpCodes.*;

/**
 * The MaskBlit operation is expressed as:
 *   dst = ((src <MODE> dst) * pathA) + (dst * (1 - pathA))
 *
 * The OGL/D3D implementation of the MaskBlit operation differs from the above
 * equation because it is not possible to perform such a complex operation in
 * OpenGL/Direct3D (without the use of advanced techniques like fragment
 * shaders and multitexturing).  Therefore, the BufferedMaskBlit operation
 * is expressed as:
 *   dst = (src * pathA) <SrcOver> dst
 *
 * This simplified formula is only equivalent to the "true" MaskBlit equation
 * in the following situations:
 *   - <MODE> is SrcOver
 *   - <MODE> is Src, extra alpha == 1.0, and the source surface is opaque
 *
 * Therefore, we register BufferedMaskBlit primitives for only the SurfaceType
 * and CompositeType restrictions mentioned above.  In addition for the Src
 * case, we must override the composite with a SrcOver (no extra alpha)
 * instance, so that we set up the OpenGL/Direct3D blending mode to match the
 * BufferedMaskBlit equation.
 */
public abstract class BufferedMaskBlit extends MaskBlit {

    private static final int ST_INT_ARGB     = 0;
    private static final int ST_INT_ARGB_PRE = 1;
    private static final int ST_INT_RGB      = 2;
    private static final int ST_INT_BGR      = 3;

    private final RenderQueue rq;
    private final int srcTypeVal;
    private Blit blitop;

    protected BufferedMaskBlit(RenderQueue rq,
                               SurfaceType srcType,
                               CompositeType compType,
                               SurfaceType dstType)
    {
        super(srcType, compType, dstType);
        this.rq = rq;
        if (srcType == SurfaceType.IntArgb) {
            this.srcTypeVal = ST_INT_ARGB;
        } else if (srcType == SurfaceType.IntArgbPre) {
            this.srcTypeVal = ST_INT_ARGB_PRE;
        } else if (srcType == SurfaceType.IntRgb) {
            this.srcTypeVal = ST_INT_RGB;
        } else if (srcType == SurfaceType.IntBgr) {
            this.srcTypeVal = ST_INT_BGR;
        } else {
            throw new InternalError("unrecognized source surface type");
        }
    }

    @Override
    public void MaskBlit(SurfaceData src, SurfaceData dst,
                         Composite comp, Region clip,
                         int srcx, int srcy,
                         int dstx, int dsty,
                         int width, int height,
                         byte[] mask, int maskoff, int maskscan)
    {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (mask == null) {
            // no mask involved; delegate to regular blit loop
            if (blitop == null) {
                blitop = Blit.getFromCache(src.getSurfaceType(),
                                           CompositeType.AnyAlpha,
                                           this.getDestType());
            }
            blitop.Blit(src, dst,
                        comp, clip,
                        srcx, srcy, dstx, dsty,
                        width, height);
            return;
        }

        AlphaComposite acomp = (AlphaComposite)comp;
        if (acomp.getRule() != AlphaComposite.SRC_OVER) {
            comp = AlphaComposite.SrcOver;
        }

        rq.lock();
        try {
            validateContext(dst, comp, clip);

            RenderBuffer buf = rq.getBuffer();
            int totalBytesRequired = 20 + (width * height * 4);

            /*
             * REMIND: we should fix this so that it works with tiles that
             *         are larger than the entire buffer, but the native
             *         OGL/D3DMaskBlit isn't even prepared for tiles larger
             *         than 32x32 pixels, so there's no urgency here...
             */
            rq.ensureCapacity(totalBytesRequired);

            // enqueue parameters and tile pixels
            int newpos = enqueueTile(buf.getAddress(), buf.position(),
                                     src, src.getNativeOps(), srcTypeVal,
                                     mask, mask.length, maskoff, maskscan,
                                     srcx, srcy, dstx, dsty,
                                     width, height);

            buf.position(newpos);
        } finally {
            rq.unlock();
        }
    }

    private native int enqueueTile(long buf, int bpos,
                                   SurfaceData srcData,
                                   long pSrcOps, int srcType,
                                   byte[] mask, int masklen,
                                   int maskoff, int maskscan,
                                   int srcx, int srcy, int dstx, int dsty,
                                   int width, int height);

    /**
     * Validates the context state using the given destination surface
     * and composite/clip values.
     */
    protected abstract void validateContext(SurfaceData dstData,
                                            Composite comp, Region clip);
}
