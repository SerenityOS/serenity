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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import sun.java2d.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;

/**
 * Class used for re-routing transformed blits to the accelerated loops.
 */

public class XRDrawImage extends DrawImage {

    @Override
    protected void renderImageXform(SunGraphics2D sg, Image img,
            AffineTransform tx, int interpType, int sx1, int sy1, int sx2,
            int sy2, Color bgColor) {
        SurfaceData dstData = sg.surfaceData;
        SurfaceData srcData = dstData.getSourceSurfaceData(img,
                SunGraphics2D.TRANSFORM_GENERIC, sg.imageComp, bgColor);

        if (sg.composite instanceof AlphaComposite) {
            int compRule = ((AlphaComposite) sg.composite).getRule();
            float extraAlpha = ((AlphaComposite) sg.composite).getAlpha();

            if (srcData != null && !isBgOperation(srcData, bgColor)
                && interpType <= AffineTransformOp.TYPE_BILINEAR
                && (XRUtils.isMaskEvaluated(XRUtils.j2dAlphaCompToXR(compRule))
                    || (XRUtils.isTransformQuadrantRotated(tx))
                    && extraAlpha == 1.0f))
            {
                SurfaceType srcType = srcData.getSurfaceType();
                SurfaceType dstType = dstData.getSurfaceType();

                TransformBlit blit = TransformBlit.getFromCache(srcType,
                        sg.imageComp, dstType);
                if (blit != null) {
                    blit.Transform(srcData, dstData, sg.composite,
                          sg.getCompClip(), tx, interpType, sx1, sy1, 0, 0, sx2
                                - sx1, sy2 - sy1);
                    return;
                }
            }
        }

        super.renderImageXform(sg, img, tx, interpType, sx1, sy1, sx2, sy2,
                bgColor);
    }
}
