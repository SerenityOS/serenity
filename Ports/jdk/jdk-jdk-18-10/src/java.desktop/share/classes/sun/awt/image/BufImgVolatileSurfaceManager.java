/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.image;

import sun.java2d.SurfaceData;

/**
 * This SurfaceManager variant manages an unaccelerated volatile surface.
 * This class is created in the event that someone requested a VolatileImage
 * to be created from a BufferedImageGraphicsConfig, which is not platform-
 * or hardware-based, thus the resulting surface and surface manager
 * are unaccelerated.  All we do in this class is implement the abstract
 * methods of VolatileSurfaceManager to return values that indicate that
 * we cannot accelerate surfaces through this SurfaceManager, thus the
 * parent class will handle things through the unaccelerated backup mechanism.
 */
public class BufImgVolatileSurfaceManager extends VolatileSurfaceManager {

    /**
     * This constructor simply defers to the superclass since all of the real
     * functionality of this class is implemented in VolatileSurfaceManager.
     */
    public BufImgVolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        super(vImg, context);
    }

    /**
     * Returns false to indicate that this surface manager cannot accelerate
     * the image.
     */
    protected boolean isAccelerationEnabled() {
        return false;
    }

    /**
     * Returns null to indicate failure in creating the accelerated surface.
     * Note that this method should not ever be called since creation of
     * accelerated surfaces should be preceded by calls to the above
     * isAccelerationEnabled() method.  But we need to override this method
     * since it is abstract in our parent class.
     */
    protected SurfaceData initAcceleratedSurface() {
        return null;
    }
}
