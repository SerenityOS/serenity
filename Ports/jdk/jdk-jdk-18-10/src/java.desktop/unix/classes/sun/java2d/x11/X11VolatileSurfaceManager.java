/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GraphicsConfiguration;
import java.awt.ImageCapabilities;
import java.awt.Transparency;
import java.awt.image.ColorModel;

import sun.awt.X11GraphicsConfig;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.VolatileSurfaceManager;
import sun.java2d.SurfaceData;

/**
 * X11 platform implementation of the VolatileSurfaceManager class.
 * The class attempts to create and use a pixmap-based SurfaceData
 * object (X11PixmapSurfaceData).
 * If this object cannot be created or re-created as necessary, the
 * class falls back to a system memory based SurfaceData object
 * (BufImgSurfaceData) that will be used until the accelerated
 * SurfaceData can be restored.
 */
public class X11VolatileSurfaceManager extends VolatileSurfaceManager {

    private boolean accelerationEnabled;

    public X11VolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        super(vImg, context);

        // We only accelerated opaque vImages currently
        accelerationEnabled = X11SurfaceData.isAccelerationEnabled() &&
            (vImg.getTransparency() == Transparency.OPAQUE);

        if ((context != null) && !accelerationEnabled) {
            // if we're wrapping a backbuffer drawable, we must ensure that
            // the accelerated surface is initialized up front, regardless
            // of whether acceleration is enabled. But we need to set
            // the  accelerationEnabled field to true to reflect that this
            // SM is actually accelerated.
            accelerationEnabled = true;
            sdAccel = initAcceleratedSurface();
            sdCurrent = sdAccel;

            if (sdBackup != null) {
                // release the system memory backup surface, as we won't be
                // needing it in this case
                sdBackup = null;
            }
        }
    }

    protected boolean isAccelerationEnabled() {
        return accelerationEnabled;
    }

    /**
     * Create a pixmap-based SurfaceData object
     */
    protected SurfaceData initAcceleratedSurface() {
        SurfaceData sData;

        try {
            X11GraphicsConfig gc = (X11GraphicsConfig)vImg.getGraphicsConfig();
            ColorModel cm = gc.getColorModel();
            long drawable = 0;
            if (context instanceof Long) {
                drawable = ((Long)context).longValue();
            }
            sData = X11SurfaceData.createData(gc,
                                              vImg.getWidth(),
                                              vImg.getHeight(),
                                              cm, vImg, drawable,
                                              Transparency.OPAQUE,
                                              false);
        } catch (NullPointerException ex) {
            sData = null;
        } catch (OutOfMemoryError er) {
            sData = null;
        }

        return sData;
    }

    protected boolean isConfigValid(GraphicsConfiguration gc) {
        // REMIND: we might be too paranoid here, requiring that
        // the GC be exactly the same as the original one.  The
        // real answer is one that guarantees that pixmap copies
        // will be correct (which requires like bit depths and
        // formats).
        return ((gc == null) || (gc == vImg.getGraphicsConfig()));
    }

    /**
     * Need to override the default behavior because Pixmaps-based
     * images are accelerated but not volatile.
     */
    @Override
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        if (isConfigValid(gc) && isAccelerationEnabled()) {
            // accelerated but not volatile
            return new ImageCapabilities(true);
        }
        // neither accelerated nor volatile
        return new ImageCapabilities(false);
    }
}
