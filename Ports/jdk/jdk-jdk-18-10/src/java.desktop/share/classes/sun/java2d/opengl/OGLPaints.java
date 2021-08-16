/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import static sun.java2d.pipe.BufferedPaints.*;
import static sun.java2d.opengl.OGLContext.OGLContextCaps.*;

abstract class OGLPaints {

    /**
     * Holds all registered implementations, using the corresponding
     * SunGraphics2D.PAINT_* constant as the hash key.
     */
    private static Map<Integer, OGLPaints> impls =
        new HashMap<Integer, OGLPaints>(4, 1.0f);

    static {
        impls.put(SunGraphics2D.PAINT_GRADIENT, new Gradient());
        impls.put(SunGraphics2D.PAINT_LIN_GRADIENT, new LinearGradient());
        impls.put(SunGraphics2D.PAINT_RAD_GRADIENT, new RadialGradient());
        impls.put(SunGraphics2D.PAINT_TEXTURE, new Texture());
    }

    /**
     * Attempts to locate an implementation corresponding to the paint state
     * of the provided SunGraphics2D object.  If no implementation can be
     * found, or if the paint cannot be accelerated under the conditions
     * of the SunGraphics2D, this method returns false; otherwise, returns
     * true.
     */
    static boolean isValid(SunGraphics2D sg2d) {
        OGLPaints impl = impls.get(sg2d.paintState);
        return (impl != null && impl.isPaintValid(sg2d));
    }

    /**
     * Returns true if this implementation is able to accelerate the
     * Paint object associated with, and under the conditions of, the
     * provided SunGraphics2D instance; otherwise returns false.
     */
    abstract boolean isPaintValid(SunGraphics2D sg2d);

/************************* GradientPaint support ****************************/

    private static class Gradient extends OGLPaints {
        private Gradient() {}

        /**
         * There are no restrictions for accelerating GradientPaint, so
         * this method always returns true.
         */
        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            return true;
        }
    }

/************************** TexturePaint support ****************************/

    private static class Texture extends OGLPaints {
        private Texture() {}

        /**
         * Returns true if the given TexturePaint instance can be used by the
         * accelerated OGLPaints.Texture implementation.  A TexturePaint is
         * considered valid if the following conditions are met:
         *   - the texture image dimensions are power-of-two (or the
         *     GL_ARB_texture_non_power_of_two extension is present)
         *   - the texture image can be (or is already) cached in an OpenGL
         *     texture object
         */
        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            TexturePaint paint = (TexturePaint)sg2d.paint;
            OGLSurfaceData dstData = (OGLSurfaceData)sg2d.surfaceData;
            BufferedImage bi = paint.getImage();

            // see if texture-non-pow2 extension is available
            if (!dstData.isTexNonPow2Available()) {
                int imgw = bi.getWidth();
                int imgh = bi.getHeight();

                // verify that the texture image dimensions are pow2
                if ((imgw & (imgw - 1)) != 0 || (imgh & (imgh - 1)) != 0) {
                    return false;
                }
            }

            SurfaceData srcData =
                dstData.getSourceSurfaceData(bi,
                                             SunGraphics2D.TRANSFORM_ISIDENT,
                                             CompositeType.SrcOver, null);
            if (!(srcData instanceof OGLSurfaceData)) {
                // REMIND: this is a hack that attempts to cache the system
                //         memory image from the TexturePaint instance into an
                //         OpenGL texture...
                srcData =
                    dstData.getSourceSurfaceData(bi,
                                                 SunGraphics2D.TRANSFORM_ISIDENT,
                                                 CompositeType.SrcOver, null);
                if (!(srcData instanceof OGLSurfaceData)) {
                    return false;
                }
            }

            // verify that the source surface is actually a texture
            OGLSurfaceData oglData = (OGLSurfaceData)srcData;
            if (oglData.getType() != OGLSurfaceData.TEXTURE) {
                return false;
            }

            return true;
        }
    }

/****************** Shared MultipleGradientPaint support ********************/

    private abstract static class MultiGradient extends OGLPaints {
        protected MultiGradient() {}

        /**
         * Returns true if the given MultipleGradientPaint instance can be
         * used by the accelerated OGLPaints.MultiGradient implementation.
         * A MultipleGradientPaint is considered valid if the following
         * conditions are met:
         *   - the number of gradient "stops" is <= MAX_FRACTIONS
         *   - the destination has support for fragment shaders
         */
        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            MultipleGradientPaint paint = (MultipleGradientPaint)sg2d.paint;
            // REMIND: ugh, this creates garbage; would be nicer if
            // we had a MultipleGradientPaint.getNumStops() method...
            if (paint.getFractions().length > MULTI_MAX_FRACTIONS) {
                return false;
            }

            OGLSurfaceData dstData = (OGLSurfaceData)sg2d.surfaceData;
            OGLGraphicsConfig gc = dstData.getOGLGraphicsConfig();
            if (!gc.isCapPresent(CAPS_EXT_GRAD_SHADER)) {
                return false;
            }

            return true;
        }
    }

/********************** LinearGradientPaint support *************************/

    private static class LinearGradient extends MultiGradient {
        private LinearGradient() {}

        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            LinearGradientPaint paint = (LinearGradientPaint)sg2d.paint;

            if (paint.getFractions().length == 2 &&
                paint.getCycleMethod() != CycleMethod.REPEAT &&
                paint.getColorSpace() != ColorSpaceType.LINEAR_RGB)
            {
                // we can delegate to the optimized two-color gradient
                // codepath, which does not require fragment shader support
                return true;
            }

            return super.isPaintValid(sg2d);
        }
    }

/********************** RadialGradientPaint support *************************/

    private static class RadialGradient extends MultiGradient {
        private RadialGradient() {}
    }
}
