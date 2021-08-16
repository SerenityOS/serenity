/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
import static sun.java2d.loops.SurfaceType.AnyColor;
import static sun.java2d.loops.SurfaceType.GradientPaint;
import static sun.java2d.loops.SurfaceType.LinearGradientPaint;
import static sun.java2d.loops.SurfaceType.OpaqueColor;
import static sun.java2d.loops.SurfaceType.OpaqueGradientPaint;
import static sun.java2d.loops.SurfaceType.OpaqueLinearGradientPaint;
import static sun.java2d.loops.SurfaceType.OpaqueRadialGradientPaint;
import static sun.java2d.loops.SurfaceType.OpaqueTexturePaint;
import static sun.java2d.loops.SurfaceType.RadialGradientPaint;
import static sun.java2d.loops.SurfaceType.TexturePaint;

import java.awt.*;
import sun.awt.*;
import sun.java2d.*;
import sun.java2d.loops.*;

public class XRMaskFill extends MaskFill {
    static void register() {
        GraphicsPrimitive[] primitives = {
                new XRMaskFill(AnyColor, SrcOver, XRSurfaceData.IntRgbX11),
                new XRMaskFill(OpaqueColor, SrcNoEa, XRSurfaceData.IntRgbX11),
                new XRMaskFill(GradientPaint, SrcOver, XRSurfaceData.IntRgbX11),
                new XRMaskFill(OpaqueGradientPaint, SrcNoEa,
                        XRSurfaceData.IntRgbX11),
                new XRMaskFill(LinearGradientPaint, SrcOver,
                        XRSurfaceData.IntRgbX11),
                new XRMaskFill(OpaqueLinearGradientPaint, SrcNoEa,
                        XRSurfaceData.IntRgbX11),
                new XRMaskFill(RadialGradientPaint, SrcOver,
                        XRSurfaceData.IntRgbX11),
                new XRMaskFill(OpaqueRadialGradientPaint, SrcNoEa,
                        XRSurfaceData.IntRgbX11),
                new XRMaskFill(TexturePaint, SrcOver, XRSurfaceData.IntRgbX11),
                new XRMaskFill(OpaqueTexturePaint, SrcNoEa,
                        XRSurfaceData.IntRgbX11),

                new XRMaskFill(AnyColor, SrcOver, XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(OpaqueColor, SrcNoEa, XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(GradientPaint, SrcOver, XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(OpaqueGradientPaint, SrcNoEa,
                        XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(LinearGradientPaint, SrcOver,
                        XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(OpaqueLinearGradientPaint, SrcNoEa,
                        XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(RadialGradientPaint, SrcOver,
                        XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(OpaqueRadialGradientPaint, SrcNoEa,
                        XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(TexturePaint, SrcOver, XRSurfaceData.IntArgbPreX11),
                new XRMaskFill(OpaqueTexturePaint, SrcNoEa,
                        XRSurfaceData.IntArgbPreX11)
                };

        GraphicsPrimitiveMgr.register(primitives);
    }

    protected XRMaskFill(SurfaceType srcType, CompositeType compType,
            SurfaceType surfaceType) {
        super(srcType, compType, surfaceType);
    }

    protected native void maskFill(long xsdo, int x, int y, int w, int h,
            int maskoff, int maskscan, int masklen, byte[] mask);

    public void MaskFill(SunGraphics2D sg2d, SurfaceData sData, Composite comp,
            final int x, final int y, final int w, final int h,
            final byte[] mask, final int maskoff, final int maskscan) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sd = (XRSurfaceData) sData;
            x11sd.validateAsDestination(null, sg2d.getCompClip());

            XRCompositeManager maskBuffer = x11sd.maskBuffer;
            maskBuffer.validateCompositeState(comp, sg2d.transform, sg2d.paint, sg2d);

            int maskPict = maskBuffer.getMaskBuffer().uploadMask(w, h, maskscan, maskoff, mask);
            maskBuffer.XRComposite(XRUtils.None, maskPict, x11sd.picture, x, y, 0, 0, x, y, w, h);
            maskBuffer.getMaskBuffer().clearUploadMask(maskPict, w, h);
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}
