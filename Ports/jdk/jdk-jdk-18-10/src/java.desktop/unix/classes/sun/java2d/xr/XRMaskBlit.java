/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

import static sun.java2d.loops.CompositeType.SrcNoEa;
import static sun.java2d.loops.CompositeType.SrcOver;

import java.awt.Composite;

import sun.awt.*;
import sun.java2d.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.Region;

/**
 * For XRender there is no "blit", everything is just a fill with Repeat or Not.
 * So basically this just quite the same as MaskFill.
 *
 * @author Clemens Eisserer
 */
public class XRMaskBlit extends MaskBlit {
    static void register() {
        GraphicsPrimitive[] primitives = {
                new XRMaskBlit(XRSurfaceData.IntArgbPreX11, SrcOver,
                               XRSurfaceData.IntArgbPreX11),
                new XRMaskBlit(XRSurfaceData.IntRgbX11, SrcOver,
                               XRSurfaceData.IntRgbX11),
                new XRMaskBlit(XRSurfaceData.IntArgbPreX11, SrcNoEa,
                               XRSurfaceData.IntRgbX11),
                new XRMaskBlit(XRSurfaceData.IntRgbX11, SrcNoEa,
                               XRSurfaceData.IntArgbPreX11)
                };
        GraphicsPrimitiveMgr.register(primitives);
    }

    public XRMaskBlit(SurfaceType srcType, CompositeType compType,
            SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
    }

    protected native void maskBlit(long srcXsdo, long dstxsdo, int srcx,
            int srcy, int dstx, int dsty, int w, int h, int maskoff,
            int maskscan, int masklen, byte[] mask);

    public void MaskBlit(SurfaceData src, SurfaceData dst, Composite comp,
            Region clip, int srcx, int srcy, int dstx, int dsty, int width,
            int height, byte[] mask, int maskoff, int maskscan) {
        if (width <= 0 || height <= 0) {
            return;
        }

        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sd = (XRSurfaceData) src;
            x11sd.validateAsSource(null, XRUtils.RepeatNone, XRUtils.FAST);

            XRCompositeManager maskBuffer = x11sd.maskBuffer;
            XRSurfaceData x11dst = (XRSurfaceData) dst;
            x11dst.validateAsDestination(null, clip);

            int maskPict = maskBuffer.getMaskBuffer().
                         uploadMask(width, height, maskscan, maskoff, mask);
            maskBuffer.XRComposite(x11sd.getPicture(), maskPict, x11dst.getPicture(),
                                  srcx, srcy, 0, 0, dstx, dsty, width, height);
            maskBuffer.getMaskBuffer().clearUploadMask(maskPict, width, height);
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}
