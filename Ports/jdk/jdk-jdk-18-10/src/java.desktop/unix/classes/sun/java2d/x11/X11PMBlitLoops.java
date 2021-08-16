/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.x11;

import sun.awt.SunToolkit;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.GraphicsPrimitiveMgr;
import sun.java2d.loops.GraphicsPrimitiveProxy;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.Blit;
import sun.java2d.loops.MaskBlit;
import sun.java2d.pipe.Region;
import sun.java2d.SurfaceData;
import java.awt.Composite;
import java.awt.image.IndexColorModel;

/**
 * X11PMBlitLoops
 *
 * This class accelerates Blits between two surfaces of types *PM.  Since
 * the onscreen surface is of that type and some of the offscreen surfaces
 * may be of that type (if they were created via X11OffScreenImage), then
 * this type of Blit will accelerated double-buffer copies between those
 * two surfaces.
*/
public class X11PMBlitLoops extends Blit {

    public static void register()
    {
        GraphicsPrimitive[] primitives = {
            new X11PMBlitLoops(X11SurfaceData.IntBgrX11,
                               X11SurfaceData.IntBgrX11, false),
            new X11PMBlitLoops(X11SurfaceData.IntRgbX11,
                               X11SurfaceData.IntRgbX11, false),
            new X11PMBlitLoops(X11SurfaceData.ThreeByteBgrX11,
                               X11SurfaceData.ThreeByteBgrX11, false),
            new X11PMBlitLoops(X11SurfaceData.ThreeByteRgbX11,
                               X11SurfaceData.ThreeByteRgbX11, false),
            new X11PMBlitLoops(X11SurfaceData.ByteIndexedOpaqueX11,
                               X11SurfaceData.ByteIndexedOpaqueX11, false),
            new X11PMBlitLoops(X11SurfaceData.ByteGrayX11,
                               X11SurfaceData.ByteGrayX11, false),
            new X11PMBlitLoops(X11SurfaceData.Index8GrayX11,
                               X11SurfaceData.Index8GrayX11, false),
            new X11PMBlitLoops(X11SurfaceData.UShort555RgbX11,
                               X11SurfaceData.UShort555RgbX11, false),
            new X11PMBlitLoops(X11SurfaceData.UShort565RgbX11,
                               X11SurfaceData.UShort565RgbX11, false),
            new X11PMBlitLoops(X11SurfaceData.UShortIndexedX11,
                               X11SurfaceData.UShortIndexedX11, false),

            // 1-bit transparent to opaque loops
            new X11PMBlitLoops(X11SurfaceData.IntBgrX11_BM,
                               X11SurfaceData.IntBgrX11, true),
            new X11PMBlitLoops(X11SurfaceData.IntRgbX11_BM,
                               X11SurfaceData.IntRgbX11, true),
            new X11PMBlitLoops(X11SurfaceData.ThreeByteBgrX11_BM,
                               X11SurfaceData.ThreeByteBgrX11, true),
            new X11PMBlitLoops(X11SurfaceData.ThreeByteRgbX11_BM,
                               X11SurfaceData.ThreeByteRgbX11, true),
            new X11PMBlitLoops(X11SurfaceData.ByteIndexedX11_BM,
                               X11SurfaceData.ByteIndexedOpaqueX11, true),
            new X11PMBlitLoops(X11SurfaceData.ByteGrayX11_BM,
                               X11SurfaceData.ByteGrayX11, true),
            new X11PMBlitLoops(X11SurfaceData.Index8GrayX11_BM,
                               X11SurfaceData.Index8GrayX11, true),
            new X11PMBlitLoops(X11SurfaceData.UShort555RgbX11_BM,
                               X11SurfaceData.UShort555RgbX11, true),
            new X11PMBlitLoops(X11SurfaceData.UShort565RgbX11_BM,
                               X11SurfaceData.UShort565RgbX11, true),
            new X11PMBlitLoops(X11SurfaceData.UShortIndexedX11_BM,
                               X11SurfaceData.UShortIndexedX11, true),

            new X11PMBlitLoops(X11SurfaceData.IntRgbX11,
                               X11SurfaceData.IntArgbPreX11, true),
            new X11PMBlitLoops(X11SurfaceData.IntRgbX11,
                               X11SurfaceData.IntArgbPreX11, false),
            new X11PMBlitLoops(X11SurfaceData.IntRgbX11_BM,
                               X11SurfaceData.IntArgbPreX11, true),

            new X11PMBlitLoops(X11SurfaceData.IntBgrX11,
                               X11SurfaceData.FourByteAbgrPreX11, true),
            new X11PMBlitLoops(X11SurfaceData.IntBgrX11,
                               X11SurfaceData.FourByteAbgrPreX11, false),
            new X11PMBlitLoops(X11SurfaceData.IntBgrX11_BM,
                               X11SurfaceData.FourByteAbgrPreX11, true),



            // delegate loops
            new DelegateBlitLoop(X11SurfaceData.IntBgrX11_BM,
                                 X11SurfaceData.IntBgrX11),
            new DelegateBlitLoop(X11SurfaceData.IntRgbX11_BM,
                                 X11SurfaceData.IntRgbX11),
            new DelegateBlitLoop(X11SurfaceData.ThreeByteBgrX11_BM,
                                 X11SurfaceData.ThreeByteBgrX11),
            new DelegateBlitLoop(X11SurfaceData.ThreeByteRgbX11_BM,
                                 X11SurfaceData.ThreeByteRgbX11),
            new DelegateBlitLoop(X11SurfaceData.ByteIndexedX11_BM,
                                 X11SurfaceData.ByteIndexedOpaqueX11),
            new DelegateBlitLoop(X11SurfaceData.ByteGrayX11_BM,
                                 X11SurfaceData.ByteGrayX11),
            new DelegateBlitLoop(X11SurfaceData.Index8GrayX11_BM,
                                 X11SurfaceData.Index8GrayX11),
            new DelegateBlitLoop(X11SurfaceData.UShort555RgbX11_BM,
                                 X11SurfaceData.UShort555RgbX11),
            new DelegateBlitLoop(X11SurfaceData.UShort565RgbX11_BM,
                                 X11SurfaceData.UShort565RgbX11),
            new DelegateBlitLoop(X11SurfaceData.UShortIndexedX11_BM,
                                 X11SurfaceData.UShortIndexedX11),

        };
        GraphicsPrimitiveMgr.register(primitives);
    }

    public X11PMBlitLoops(SurfaceType srcType, SurfaceType dstType,
                          boolean over) {
        super(srcType,
              over ? CompositeType.SrcOverNoEa : CompositeType.SrcNoEa,
              dstType);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy,
                     int dx, int dy,
                     int w, int h)
    {
        SunToolkit.awtLock();
        try {
            X11SurfaceData x11sd = (X11SurfaceData)dst;
            // pass null clip region here since we clip manually in native code
            // also use false for needExposures since we clip to the pixmap
            long xgc = x11sd.getBlitGC(null, false);
            nativeBlit(src.getNativeOps(), dst.getNativeOps(), xgc, clip,
                       sx, sy, dx, dy, w, h);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    /**
     * Blit
     * This native method is where all of the work happens in the
     * accelerated Blit.
     */
    private native void nativeBlit(long srcData, long dstData,
                                   long xgc, Region clip,
                                   int sx, int sy, int dx, int dy,
                                   int w, int h);

    /**
     * This loop is used to render from a BITMASK Sw surface data
     * to the Hw cached copies managed by SurfaceDataProxies.
     * It first uses a delegate opaque Blit to perform the copy of
     * the pixel data and then updates the X11 clipping bitmask from
     * the transparent pixels in the source.
     */
    static class DelegateBlitLoop extends Blit {
        SurfaceType dstType;

        /**
         * @param realDstType SurfaceType for which the loop should be
         * registered
         * @param delegateDstType SurfaceType which will be used
         * for finding delegate loop
         */
        public DelegateBlitLoop(SurfaceType realDstType, SurfaceType delegateDstType) {
            super(SurfaceType.Any, CompositeType.SrcNoEa, realDstType);
            this.dstType = delegateDstType;
        }

        public void Blit(SurfaceData src, SurfaceData dst,
                         Composite comp, Region clip,
                         int sx, int sy, int dx, int dy, int w, int h)
        {
            Blit blit = Blit.getFromCache(src.getSurfaceType(),
                                          CompositeType.SrcNoEa,
                                          dstType);
            blit.Blit(src, dst, comp, clip, sx, sy, dx, dy, w, h);
            updateBitmask(src, dst,
                          src.getColorModel() instanceof IndexColorModel);
        }
    }

    private static native void updateBitmask(SurfaceData src,
                                             SurfaceData dst,
                                             boolean isICM);
}
