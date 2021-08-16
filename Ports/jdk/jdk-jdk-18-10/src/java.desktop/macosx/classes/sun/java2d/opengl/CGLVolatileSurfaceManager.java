/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.ColorModel;

import sun.awt.image.SunVolatileImage;
import sun.awt.image.VolatileSurfaceManager;
import sun.java2d.SurfaceData;

import static sun.java2d.opengl.OGLContext.OGLContextCaps.CAPS_EXT_FBOBJECT;

public class CGLVolatileSurfaceManager extends VolatileSurfaceManager {

    private final boolean accelerationEnabled;

    public CGLVolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        super(vImg, context);

        /*
         * We will attempt to accelerate this image only under the
         * following conditions:
         *   - the image is not bitmask AND the GraphicsConfig supports the FBO
         *     extension
         */
        int transparency = vImg.getTransparency();
        CGLGraphicsConfig gc = (CGLGraphicsConfig) vImg.getGraphicsConfig();
        accelerationEnabled = gc.isCapPresent(CAPS_EXT_FBOBJECT)
                && transparency != Transparency.BITMASK;
    }

    protected boolean isAccelerationEnabled() {
        return accelerationEnabled;
    }

    /**
     * Create a FBO-based SurfaceData object (or init the backbuffer
     * of an existing window if this is a double buffered GraphicsConfig)
     */
    protected SurfaceData initAcceleratedSurface() {
        try {
            CGLGraphicsConfig gc = (CGLGraphicsConfig)vImg.getGraphicsConfig();
            ColorModel cm = gc.getColorModel(vImg.getTransparency());
            int type = vImg.getForcedAccelSurfaceType();
            // if acceleration type is forced (type != UNDEFINED) then
            // use the forced type, otherwise choose FBOBJECT
            if (type == OGLSurfaceData.UNDEFINED) {
                type = OGLSurfaceData.FBOBJECT;
            }
            return CGLSurfaceData.createData(gc, vImg.getWidth(),
                                             vImg.getHeight(), cm, vImg, type);
        } catch (NullPointerException | OutOfMemoryError ignored) {
            return null;
        }
    }

    @Override
    protected boolean isConfigValid(GraphicsConfiguration gc) {
        return ((gc == null) || (gc == vImg.getGraphicsConfig()));
    }

    @Override
    public void initContents() {
        if (vImg.getForcedAccelSurfaceType() != OGLSurfaceData.TEXTURE) {
            super.initContents();
        }
    }
}

