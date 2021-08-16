/*
 * Copyright (c) 1998, 2002, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.image.ColorModel;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import sun.java2d.SurfaceData;
import sun.java2d.SunCompositeContext;

/**
 * Bitwise XOR Composite class.
 */

public final class XORComposite implements Composite {

    Color xorColor;
    int xorPixel;
    int alphaMask;

    public XORComposite(Color xorColor, SurfaceData sd) {
        this.xorColor = xorColor;

        SurfaceType sType = sd.getSurfaceType();

        this.xorPixel = sd.pixelFor(xorColor.getRGB());
        this.alphaMask = sType.getAlphaMask();
    }

    public Color getXorColor() {
        return xorColor;
    }

    public int getXorPixel() {
        return xorPixel;
    }

    public int getAlphaMask() {
        return alphaMask;
    }

    public CompositeContext createContext(ColorModel srcColorModel,
                                          ColorModel dstColorModel,
                                          RenderingHints hints) {
        return new SunCompositeContext(this, srcColorModel, dstColorModel);
    }
}
