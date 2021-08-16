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

import static java.lang.Math.min;
import static java.lang.Math.max;
import static sun.java2d.xr.MaskTileManager.MASK_SIZE;

/**
 * This class implements region tracking, used by the tiled-mask code.
 *
 * @author Clemens Eisserer
 */

public class DirtyRegion implements Cloneable {
    int x, y, x2, y2;

    public DirtyRegion() {
        clear();
    }

    public void clear() {
        x = Integer.MAX_VALUE;
        y = Integer.MAX_VALUE;
        x2 = Integer.MIN_VALUE;
        y2 = Integer.MIN_VALUE;
    }

    public void growDirtyRegion(int x, int y, int x2, int y2) {
        this.x = min(x, this.x);
        this.y = min(y, this.y);
        this.x2 = max(x2, this.x2);
        this.y2 = max(y2, this.y2);
    }

    public int getWidth() {
        return x2 - x;
    }

    public int getHeight() {
        return y2 - y;
    }

    public void growDirtyRegionTileLimit(int x, int y, int x2, int y2) {
        if (x < this.x) {
            this.x = max(x, 0);
        }
        if (y < this.y) {
            this.y = max(y, 0);
        }
        if (x2 > this.x2) {
            this.x2 = min(x2, MASK_SIZE);
        }
        if (y2 > this.y2) {
            this.y2 = min(y2, MASK_SIZE);
        }
    }

    public static DirtyRegion combineRegion(DirtyRegion region1,
                                            DirtyRegion region2) {
        DirtyRegion region = new DirtyRegion();
        region.x = min(region1.x, region2.x);
        region.y = min(region1.y, region2.y);
        region.x2 = max(region1.x2, region2.x2);
        region.y2 = max(region1.y2, region2.y2);
        return region;
    }

    public void setDirtyLineRegion(int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            this.x = x1;
            this.x2 = x2;
        } else {
            this.x = x2;
            this.x2 = x1;
        }

        if (y1 < y2) {
            this.y = y1;
            this.y2 = y2;
        } else {
            this.y = y2;
            this.y2 = y1;
        }
    }

    public void translate(int x, int y) {
        if (this.x != Integer.MAX_VALUE) {
            this.x += x;
            this.x2 += x;
            this.y += y;
            this.y2 += y;
        }
    }

    public String toString() {
        return this.getClass().getName() +
                "(x: " + x + ", y:" + y + ", x2:" + x2 + ", y2:" + y2 + ")";
    }

    public DirtyRegion cloneRegion() {
        try {
            return (DirtyRegion) clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
