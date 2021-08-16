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

/**
 * Represents a single tile, used to store the rectangles covering the area
 * of the mask where the tile is located.
 *
 * @author Clemens Eisserer
 */
public class MaskTile {
    GrowableRectArray rects;
    DirtyRegion dirtyArea;

    public MaskTile()
    {
        rects = new GrowableRectArray(128);
        dirtyArea = new DirtyRegion();
    }

    public void calculateDirtyAreas()
    {
        for (int i=0; i < rects.getSize(); i++) {
            int x = rects.getX(i);
            int y = rects.getY(i);
            dirtyArea.growDirtyRegion(x, y,
                                      x + rects.getWidth(i),
                                      y + rects.getHeight(i));
        }
    }

    public void reset() {
        rects.clear();
        dirtyArea.clear();
    }

    public void translate(int x, int y) {
        if (rects.getSize() > 0) {
            dirtyArea.translate(x, y);
        }
        rects.translateRects(x, y);
    }

    public GrowableRectArray getRects() {
        return rects;
    }

    public DirtyRegion getDirtyArea() {
        return dirtyArea;
    }
}
