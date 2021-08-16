/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text;

import java.io.Serializable;

/**
 * An implementation of a gapped buffer similar to that used by
 * emacs.  The underlying storage is a java array of some type,
 * which is known only by the subclass of this class.  The array
 * has a gap somewhere.  The gap is moved to the location of changes
 * to take advantage of common behavior where most changes occur
 * in the same location.  Changes that occur at a gap boundary are
 * generally cheap and moving the gap is generally cheaper than
 * moving the array contents directly to accommodate the change.
 *
 * @author  Timothy Prinzing
 * @see GapContent
 */
@SuppressWarnings("serial") // Data in fields not necessarily serializable
abstract class GapVector implements Serializable {


    /**
     * Creates a new GapVector object.  Initial size defaults to 10.
     */
    public GapVector() {
        this(10);
    }

    /**
     * Creates a new GapVector object, with the initial
     * size specified.
     *
     * @param initialLength the initial size
     */
    public GapVector(int initialLength) {
        array = allocateArray(initialLength);
        g0 = 0;
        g1 = initialLength;
    }

    /**
     * Allocate an array to store items of the type
     * appropriate (which is determined by the subclass).
     *
     * @param  len the length of the array
     * @return the java array of some type
     */
    protected abstract Object allocateArray(int len);

    /**
     * Get the length of the allocated array.
     *
     * @return the length of the array
     */
    protected abstract int getArrayLength();

    /**
     * Access to the array.  The actual type
     * of the array is known only by the subclass.
     *
     * @return the java array of some type
     */
    protected final Object getArray() {
        return array;
    }

    /**
     * Access to the start of the gap.
     *
     * @return the start of the gap
     */
    protected final int getGapStart() {
        return g0;
    }

    /**
     * Access to the end of the gap.
     *
     * @return the end of the gap
     */
    protected final int getGapEnd() {
        return g1;
    }

    // ---- variables -----------------------------------

    /**
     * The array of items.  The type is determined by the subclass.
     */
    private Object array;

    /**
     * start of gap in the array
     */
    private int g0;

    /**
     * end of gap in the array
     */
    private int g1;


    // --- gap management -------------------------------

    /**
     * Replace the given logical position in the storage with
     * the given new items.  This will move the gap to the area
     * being changed if the gap is not currently located at the
     * change location.
     *
     * @param position the location to make the replacement.  This
     *  is not the location in the underlying storage array, but
     *  the location in the contiguous space being modeled.
     * @param rmSize the number of items to remove
     * @param addItems the new items to place in storage.
     * @param addSize the number of items to add
     */
    protected void replace(int position, int rmSize, Object addItems, int addSize) {
        int addOffset = 0;
        if (addSize == 0) {
            close(position, rmSize);
            return;
        } else if (rmSize > addSize) {
            /* Shrink the end. */
            close(position+addSize, rmSize-addSize);
        } else {
            /* Grow the end, do two chunks. */
            int endSize = addSize - rmSize;
            int end = open(position + rmSize, endSize);
            System.arraycopy(addItems, rmSize, array, end, endSize);
            addSize = rmSize;
        }
        System.arraycopy(addItems, addOffset, array, position, addSize);
    }

    /**
     * Delete nItems at position.  Squeezes any marks
     * within the deleted area to position.  This moves
     * the gap to the best place by minimizing it's
     * overall movement.  The gap must intersect the
     * target block.
     */
    void close(int position, int nItems) {
        if (nItems == 0)  return;

        int end = position + nItems;
        int new_gs = (g1 - g0) + nItems;
        if (end <= g0) {
            // Move gap to end of block.
            if (g0 != end) {
                shiftGap(end);
            }
            // Adjust g0.
            shiftGapStartDown(g0 - nItems);
        } else if (position >= g0) {
            // Move gap to beginning of block.
            if (g0 != position) {
                shiftGap(position);
            }
            // Adjust g1.
            shiftGapEndUp(g0 + new_gs);
        } else {
            // The gap is properly inside the target block.
            // No data movement necessary, simply move both gap pointers.
            shiftGapStartDown(position);
            shiftGapEndUp(g0 + new_gs);
        }
    }

    /**
     * Make space for the given number of items at the given
     * location.
     *
     * @return the location that the caller should fill in
     */
    int open(int position, int nItems) {
        int gapSize = g1 - g0;
        if (nItems == 0) {
            if (position > g0)
                position += gapSize;
            return position;
        }

        // Expand the array if the gap is too small.
        shiftGap(position);
        if (nItems >= gapSize) {
            // Pre-shift the gap, to reduce total movement.
            shiftEnd(getArrayLength() - gapSize + nItems);
            gapSize = g1 - g0;
        }

        g0 = g0 + nItems;
        return position;
    }

    /**
     * resize the underlying storage array to the
     * given new size
     */
    void resize(int nsize) {
        Object narray = allocateArray(nsize);
        System.arraycopy(array, 0, narray, 0, Math.min(nsize, getArrayLength()));
        array = narray;
    }

    /**
     * Make the gap bigger, moving any necessary data and updating
     * the appropriate marks.
     *
     * @param  newSize the new capacity
     */
    protected void shiftEnd(int newSize) {
        int oldSize = getArrayLength();
        int oldGapEnd = g1;
        int upperSize = oldSize - oldGapEnd;
        int arrayLength = getNewArraySize(newSize);
        int newGapEnd = arrayLength - upperSize;
        resize(arrayLength);
        g1 = newGapEnd;

        if (upperSize != 0) {
            // Copy array items to new end of array.
            System.arraycopy(array, oldGapEnd, array, newGapEnd, upperSize);
        }
    }

    /**
     * Calculates a new size of the storage array depending on required
     * capacity.
     * @param reqSize the size which is necessary for new content
     * @return the new size of the storage array
     */
    int getNewArraySize(int reqSize) {
        return (reqSize + 1) * 2;
    }

    /**
     * Move the start of the gap to a new location,
     * without changing the size of the gap.  This
     * moves the data in the array and updates the
     * marks accordingly.
     *
     * @param  newGapStart the new start of the gap
     */
    protected void shiftGap(int newGapStart) {
        if (newGapStart == g0) {
            return;
        }
        int oldGapStart = g0;
        int dg = newGapStart - oldGapStart;
        int oldGapEnd = g1;
        int newGapEnd = oldGapEnd + dg;
        int gapSize = oldGapEnd - oldGapStart;

        g0 = newGapStart;
        g1 = newGapEnd;
        if (dg > 0) {
            // Move gap up, move data down.
            System.arraycopy(array, oldGapEnd, array, oldGapStart, dg);
        } else if (dg < 0) {
            // Move gap down, move data up.
            System.arraycopy(array, newGapStart, array, newGapEnd, -dg);
        }
    }

    /**
     * Adjust the gap end downward.  This doesn't move
     * any data, but it does update any marks affected
     * by the boundary change.  All marks from the old
     * gap start down to the new gap start are squeezed
     * to the end of the gap (their location has been
     * removed).
     *
     * @param  newGapStart the new start of the gap
     */
    protected void shiftGapStartDown(int newGapStart) {
        g0 = newGapStart;
    }

    /**
     * Adjust the gap end upward.  This doesn't move
     * any data, but it does update any marks affected
     * by the boundary change. All marks from the old
     * gap end up to the new gap end are squeezed
     * to the end of the gap (their location has been
     * removed).
     *
     * @param  newGapEnd the new end of the gap
     */
    protected void shiftGapEndUp(int newGapEnd) {
        g1 = newGapEnd;
    }

}
