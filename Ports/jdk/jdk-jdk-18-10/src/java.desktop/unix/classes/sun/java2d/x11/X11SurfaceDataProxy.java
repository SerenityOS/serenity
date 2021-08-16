/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;

import sun.awt.X11GraphicsConfig;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.SurfaceDataProxy;
import sun.java2d.loops.CompositeType;

/**
 * The proxy class contains the logic for when to replace a
 * SurfaceData with a cached X11 Pixmap and the code to create
 * the accelerated surfaces.
 */
public abstract class X11SurfaceDataProxy extends SurfaceDataProxy
    implements Transparency
{
    public static SurfaceDataProxy createProxy(SurfaceData srcData,
                                               X11GraphicsConfig dstConfig)
    {
        if (srcData instanceof X11SurfaceData) {
            // srcData must be a VolatileImage which either matches
            // our visual or not - either way we do not cache it...
            return UNCACHED;
        }

        ColorModel cm = srcData.getColorModel();
        int transparency = cm.getTransparency();

        if (transparency == Transparency.OPAQUE) {
            return new Opaque(dstConfig);
        } else if (transparency == Transparency.BITMASK) {
            // 4673490: updateBitmask() only handles ICMs with 8-bit indices
            if ((cm instanceof IndexColorModel) && cm.getPixelSize() == 8) {
                return new Bitmask(dstConfig);
            }
            // The only other ColorModel handled by updateBitmask() is
            // a DCM where the alpha bit, and only the alpha bit, is in
            // the top 8 bits
            if (cm instanceof DirectColorModel) {
                DirectColorModel dcm = (DirectColorModel) cm;
                int colormask = (dcm.getRedMask() |
                                 dcm.getGreenMask() |
                                 dcm.getBlueMask());
                int alphamask = dcm.getAlphaMask();

                if ((colormask & 0xff000000) == 0 &&
                    (alphamask & 0xff000000) != 0)
                {
                    return new Bitmask(dstConfig);
                }
            }
        }

        // For whatever reason, this image is not a good candidate for
        // caching in a pixmap so we return the non-caching (non-)proxy.
        return UNCACHED;
    }

    X11GraphicsConfig x11gc;

    public X11SurfaceDataProxy(X11GraphicsConfig x11gc) {
        this.x11gc = x11gc;
    }

    @Override
    public SurfaceData validateSurfaceData(SurfaceData srcData,
                                           SurfaceData cachedData,
                                           int w, int h)
    {
        if (cachedData == null) {
            try {
                // Bitmask will be created lazily during the blit phase
                cachedData = X11SurfaceData.createData(x11gc, w, h,
                                                       x11gc.getColorModel(),
                                                       null, 0,
                                                       getTransparency(), true);
           } catch (OutOfMemoryError oome) {
           }
        }
        return cachedData;
    }

    /**
     * Proxy for opaque source images.
     * This proxy can accelerate unscaled Src copies.
     */
    public static class Opaque extends X11SurfaceDataProxy {
        public Opaque(X11GraphicsConfig x11gc) {
            super(x11gc);
        }

        public int getTransparency() {
            return Transparency.OPAQUE;
        }

        @Override
        public boolean isSupportedOperation(SurfaceData srcData,
                                            int txtype,
                                            CompositeType comp,
                                            Color bgColor)
        {
            return (txtype < SunGraphics2D.TRANSFORM_TRANSLATESCALE &&
                    (CompositeType.SrcOverNoEa.equals(comp) ||
                     CompositeType.SrcNoEa.equals(comp)));
        }
    }

    /**
     * Proxy for bitmask transparent source images.
     * This proxy can accelerate unscaled Src copies or
     * unscaled SrcOver copies that use an opaque bgColor.
     */
    public static class Bitmask extends X11SurfaceDataProxy {
        public Bitmask(X11GraphicsConfig x11gc) {
            super(x11gc);
        }

        public int getTransparency() {
            return Transparency.BITMASK;
        }

        @Override
        public boolean isSupportedOperation(SurfaceData srcData,
                                            int txtype,
                                            CompositeType comp,
                                            Color bgColor)
        {
            // These could probably be combined into a single
            // nested if, but the logic is easier to follow this way.

            // we don't have X11 scale loops, so always use
            // software surface in case of scaling
            if (txtype >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
                return false;
            }

            if (bgColor != null &&
                bgColor.getTransparency() != Transparency.OPAQUE)
            {
                return false;
            }

            // for transparent images SrcNoEa+bgColor has the
            // same effect as SrcOverNoEa+bgColor, so we allow
            // copying from pixmap SD using accelerated blitbg loops:
            // SrcOver will be changed to SrcNoEa in DrawImage.blitSD
            if (CompositeType.SrcOverNoEa.equals(comp) ||
                (CompositeType.SrcNoEa.equals(comp) &&
                 bgColor != null))
            {
                return true;
            }

            return false;
        }
    }
}
