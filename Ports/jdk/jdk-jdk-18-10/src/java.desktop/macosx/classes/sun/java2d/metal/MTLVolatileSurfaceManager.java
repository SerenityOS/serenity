/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.metal;

import sun.awt.image.SunVolatileImage;
import sun.awt.image.VolatileSurfaceManager;
import sun.java2d.SurfaceData;

import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import sun.java2d.pipe.hw.AccelSurface;

public class MTLVolatileSurfaceManager extends VolatileSurfaceManager {

    private final boolean accelerationEnabled;

    public MTLVolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        super(vImg, context);

        /*
         * We will attempt to accelerate this image only
         * if the image is not bitmask
         */
        int transparency = vImg.getTransparency();
        accelerationEnabled = transparency != Transparency.BITMASK;
    }

    protected boolean isAccelerationEnabled() {
        return accelerationEnabled;
    }

    /**
     * Create a SurfaceData object (or init the backbuffer
     * of an existing window if this is a double buffered GraphicsConfig)
     */
    protected SurfaceData initAcceleratedSurface() {
        try {
            MTLGraphicsConfig gc =
                (MTLGraphicsConfig)vImg.getGraphicsConfig();
            ColorModel cm = gc.getColorModel(vImg.getTransparency());
            int type = vImg.getForcedAccelSurfaceType();
            // if acceleration type is forced (type != UNDEFINED) then
            // use the forced type, otherwise choose RT_TEXTURE
            if (type == AccelSurface.UNDEFINED) {
                type = AccelSurface.RT_TEXTURE;
            }
            return MTLSurfaceData.createData(gc,
                                             vImg.getWidth(),
                                             vImg.getHeight(),
                                             cm, vImg, type);
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
        if (vImg.getForcedAccelSurfaceType() != AccelSurface.TEXTURE) {
            super.initContents();
        }
    }
}
