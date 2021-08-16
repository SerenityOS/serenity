/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.awt.image.ColorModel;
import java.awt.image.Raster;

/**
 * A simple class that provides RenderedImage functionality
 * given a Raster and a ColorModel.
 */
public class SingleTileRenderedImage extends SimpleRenderedImage {

    Raster ras;

    /**
     * Constructs a SingleTileRenderedImage based on a Raster
     * and a ColorModel.
     *
     * @param ras A Raster that will define tile (0, 0) of the image.
     * @param colorModel A ColorModel that will serve as the image's
     *           ColorModel.
     */
    public SingleTileRenderedImage(Raster ras, ColorModel colorModel) {
        this.ras = ras;

        this.tileGridXOffset = this.minX = ras.getMinX();
        this.tileGridYOffset = this.minY = ras.getMinY();
        this.tileWidth = this.width = ras.getWidth();
        this.tileHeight = this.height = ras.getHeight();
        this.sampleModel = ras.getSampleModel();
        this.colorModel = colorModel;
    }

    /**
     * Returns the image's Raster as tile (0, 0).
     */
    public Raster getTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("tileX != 0 || tileY != 0");
        }
        return ras;
    }
}
