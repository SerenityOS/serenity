/*
 * Copyright 2021 JetBrains s.r.o.
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

package sun.font;

import sun.java2d.SurfaceData;

import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.image.Raster;

/**
 * SurfaceData view for a color glyph from glyph cache
 */
class ColorGlyphSurfaceData extends SurfaceData {
    ColorGlyphSurfaceData() {
        super(State.UNTRACKABLE);
        initOps();
    }

    private native void initOps();

    native void setCurrentGlyph(long imgPtr);

    @Override
    public SurfaceData getReplacement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Raster getRaster(int x, int y, int w, int h) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rectangle getBounds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getDestination() {
        throw new UnsupportedOperationException();
    }
}
