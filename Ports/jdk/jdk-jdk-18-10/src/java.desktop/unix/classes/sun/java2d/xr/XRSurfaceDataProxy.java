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

import java.awt.Color;
import java.awt.Transparency;
import sun.java2d.SurfaceData;
import sun.java2d.SurfaceDataProxy;
import sun.java2d.loops.CompositeType;

/**
 * The proxy class contains the logic if to replace a SurfaceData with a
 * cached X11 Pixmap and the code to create the accelerated surfaces.
 */
public class XRSurfaceDataProxy extends SurfaceDataProxy implements Transparency {

    public static SurfaceDataProxy createProxy(SurfaceData srcData,
            XRGraphicsConfig dstConfig) {

        /*Don't cache already native surfaces*/
        if (srcData instanceof XRSurfaceData) {
            return UNCACHED;
        }

        return new XRSurfaceDataProxy(dstConfig, srcData.getTransparency());
    }

    XRGraphicsConfig xrgc;
    int transparency;

    public XRSurfaceDataProxy(XRGraphicsConfig x11gc) {
        this.xrgc = x11gc;
    }

    @Override
    public SurfaceData validateSurfaceData(SurfaceData srcData,
            SurfaceData cachedData, int w, int h) {
        if (cachedData == null) {
            try {
                cachedData = XRSurfaceData.createData(xrgc, w, h,
                                                      xrgc.getColorModel(), null, 0,
                                                      getTransparency(), true);
            } catch (OutOfMemoryError oome) {
            }
        }
        return cachedData;
    }

    public XRSurfaceDataProxy(XRGraphicsConfig x11gc, int transparency) {
        this.xrgc = x11gc;
        this.transparency = transparency;
    }

    //TODO: Is that really ok?
    @Override
    public boolean isSupportedOperation(SurfaceData srcData, int txtype,
            CompositeType comp, Color bgColor) {
        return (bgColor == null || transparency == Transparency.TRANSLUCENT);
    }

    public int getTransparency() {
        return transparency;
    }
}
