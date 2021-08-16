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

import java.util.*;

/**
 * Growable int array, designed to allow subclasses to emulate
 * the behaviour of value types.
 *
 * @author Clemens Eisserer
 */

public class GrowableIntArray {

    int[] array;
    int size;
    int cellSize;

    public GrowableIntArray(int cellSize, int initialSize) {
        array = new int[initialSize];
        size = 0;
        this.cellSize = cellSize;
    }

    private int getNextCellIndex() {
        int oldSize = size;
        size += cellSize;

        if (size >= array.length) {
            growArray();
        }

        return oldSize;
    }

    /**
     * @return a direct reference to the backing array.
     */
    public int[] getArray() {
        return array;
    }

    /**
     * @return a copy of the backing array.
     */
    public int[] getSizedArray() {
        return Arrays.copyOf(array, getSize());
    }

    /**
     * Returns the index of the next free cell,
     * and grows the backing arrays if required.
     */
    public final int getNextIndex() {
        return getNextCellIndex() / cellSize;
    }

    protected final int getCellIndex(int cellIndex) {
        return cellSize * cellIndex;
    }

    public final int getInt(int cellIndex) {
        return array[cellIndex];
    }

    public final void addInt(int i) {
        int nextIndex = getNextIndex();
        array[nextIndex] = i;
    }

    /**
     * @return The number of stored cells.
     */
    public final int getSize() {
        return size / cellSize;
    }

    public void clear() {
        size = 0;
    }

    protected void growArray() {
        int newSize = Math.max(array.length * 2, 10);
        int[] oldArray = array;
        array = new int[newSize];

        System.arraycopy(oldArray, 0, array, 0, oldArray.length);
    }

}
