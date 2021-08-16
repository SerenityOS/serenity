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
 * Class to efficiently store rectangles.
 *
 * @author Clemens Eisserer
 */
public class GrowableRectArray extends GrowableIntArray {

    private static final int RECT_SIZE = 4;

    public GrowableRectArray(int initialSize) {
        super(RECT_SIZE, initialSize);
    }

    public final void pushRectValues(int x, int y, int width, int height) {
        int currSize = size;
        size += RECT_SIZE;

        if (size >= array.length) {
            growArray();
        }

        array[currSize] = x;
        array[currSize + 1] = y;
        array[currSize + 2] = width;
        array[currSize + 3] = height;
    }

    public final void setX(int index, int x) {
        array[getCellIndex(index)] = x;
    }

    public final void setY(int index, int y) {
        array[getCellIndex(index) + 1] = y;
    }

    public final void setWidth(int index, int width) {
        array[getCellIndex(index) + 2] = width;
    }

    public final void setHeight(int index, int height) {
        array[getCellIndex(index) + 3] = height;
    }

    public final int getX(int index) {
        return array[getCellIndex(index)];
    }

    public final int getY(int index) {
        return array[getCellIndex(index) + 1];
    }

    public final int getWidth(int index) {
        return array[getCellIndex(index) + 2];
    }

    public final int getHeight(int index) {
        return array[getCellIndex(index) + 3];
    }

    public final void translateRects(int x, int y) {
        for (int i = 0; i < getSize(); i++) {
            setX(i, getX(i) + x);
            setY(i, getY(i) + y);
        }
    }
}
