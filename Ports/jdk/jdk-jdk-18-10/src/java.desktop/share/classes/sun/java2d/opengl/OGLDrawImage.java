/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.opengl;

import java.awt.Color;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.TransformBlit;
import sun.java2d.pipe.DrawImage;

public class OGLDrawImage extends DrawImage {

    @Override
    protected void renderImageXform(SunGraphics2D sg, Image img,
                                    AffineTransform tx, int interpType,
                                    int sx1, int sy1, int sx2, int sy2,
                                    Color bgColor)
    {
        // punt to the MediaLib-based transformImage() in the superclass if:
        //     - bicubic interpolation is specified
        //     - a background color is specified and will be used
        //     - the source surface is neither a texture nor render-to-texture
        //       surface, and a non-default interpolation hint is specified
        //       (we can only control the filtering for texture->surface
        //       copies)
        //         REMIND: we should tweak the sw->texture->surface
        //         transform case to handle filtering appropriately
        //         (see 4841762)...
        //     - an appropriate TransformBlit primitive could not be found
        if (interpType != AffineTransformOp.TYPE_BICUBIC) {
            SurfaceData dstData = sg.surfaceData;
            SurfaceData srcData =
                dstData.getSourceSurfaceData(img,
                                             SunGraphics2D.TRANSFORM_GENERIC,
                                             sg.imageComp,
                                             bgColor);

            if (srcData != null &&
                !isBgOperation(srcData, bgColor) &&
                (srcData.getSurfaceType() == OGLSurfaceData.OpenGLTexture ||
                 srcData.getSurfaceType() == OGLSurfaceData.OpenGLSurfaceRTT ||
                 interpType == AffineTransformOp.TYPE_NEAREST_NEIGHBOR))
            {
                SurfaceType srcType = srcData.getSurfaceType();
                SurfaceType dstType = dstData.getSurfaceType();
                TransformBlit blit = TransformBlit.getFromCache(srcType,
                                                                sg.imageComp,
                                                                dstType);

                if (blit != null) {
                    blit.Transform(srcData, dstData,
                                   sg.composite, sg.getCompClip(),
                                   tx, interpType,
                                   sx1, sy1, 0, 0, sx2-sx1, sy2-sy1);
                    return;
                }
            }
        }

        super.renderImageXform(sg, img, tx, interpType,
                               sx1, sy1, sx2, sy2, bgColor);
    }

    @Override
    public void transformImage(SunGraphics2D sg, BufferedImage img,
                               BufferedImageOp op, int x, int y)
    {
        if (op != null) {
            if (op instanceof AffineTransformOp) {
                AffineTransformOp atop = (AffineTransformOp) op;
                transformImage(sg, img, x, y,
                               atop.getTransform(),
                               atop.getInterpolationType());
                return;
            } else {
                if (OGLBufImgOps.renderImageWithOp(sg, img, op, x, y)) {
                    return;
                }
            }
            img = op.filter(img, null);
        }
        copyImage(sg, img, x, y, null);
    }
}
