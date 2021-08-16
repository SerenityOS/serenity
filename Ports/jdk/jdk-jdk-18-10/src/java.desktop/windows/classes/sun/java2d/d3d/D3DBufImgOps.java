/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.d3d;

import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.LookupOp;
import java.awt.image.RescaleOp;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.pipe.BufferedBufImgOps;
import static sun.java2d.d3d.D3DContext.D3DContextCaps.*;

class D3DBufImgOps extends BufferedBufImgOps {

    /**
     * This method is called from D3DDrawImage.transformImage() only.  It
     * validates the provided BufferedImageOp to determine whether the op
     * is one that can be accelerated by the D3D pipeline.  If the operation
     * cannot be completed for any reason, this method returns false;
     * otherwise, the given BufferedImage is rendered to the destination
     * using the provided BufferedImageOp and this method returns true.
     */
    static boolean renderImageWithOp(SunGraphics2D sg, BufferedImage img,
                                     BufferedImageOp biop, int x, int y)
    {
        // Validate the provided BufferedImage (make sure it is one that
        // is supported, and that its properties are acceleratable)
        if (biop instanceof ConvolveOp) {
            if (!isConvolveOpValid((ConvolveOp)biop)) {
                return false;
            }
        } else if (biop instanceof RescaleOp) {
            if (!isRescaleOpValid((RescaleOp)biop, img)) {
                return false;
            }
        } else if (biop instanceof LookupOp) {
            if (!isLookupOpValid((LookupOp)biop, img)) {
                return false;
            }
        } else {
            // No acceleration for other BufferedImageOps (yet)
            return false;
        }

        SurfaceData dstData = sg.surfaceData;
        if (!(dstData instanceof D3DSurfaceData) ||
            (sg.interpolationType == AffineTransformOp.TYPE_BICUBIC) ||
            (sg.compositeState > SunGraphics2D.COMP_ALPHA))
        {
            return false;
        }

        SurfaceData srcData =
            dstData.getSourceSurfaceData(img, SunGraphics2D.TRANSFORM_ISIDENT,
                                         CompositeType.SrcOver, null);
        if (!(srcData instanceof D3DSurfaceData)) {
            // REMIND: this hack tries to ensure that we have a cached texture
            srcData =
                dstData.getSourceSurfaceData(img, SunGraphics2D.TRANSFORM_ISIDENT,
                                             CompositeType.SrcOver, null);
            if (!(srcData instanceof D3DSurfaceData)) {
                return false;
            }
        }

        // Verify that the source surface is actually a texture and that
        // shaders are supported
        D3DSurfaceData d3dSrc = (D3DSurfaceData)srcData;
        D3DGraphicsDevice gd =
            (D3DGraphicsDevice)d3dSrc.getDeviceConfiguration().getDevice();
        if (d3dSrc.getType() != D3DSurfaceData.TEXTURE ||
            !gd.isCapPresent(CAPS_LCD_SHADER))
        {
            return false;
        }

        int sw = img.getWidth();
        int sh = img.getHeight();
        D3DBlitLoops.IsoBlit(srcData, dstData,
                             img, biop,
                             sg.composite, sg.getCompClip(),
                             sg.transform, sg.interpolationType,
                             0, 0, sw, sh,
                             x, y, x+sw, y+sh,
                             true);

        return true;
    }
}
