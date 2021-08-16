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

package sun.text;

public final class UCompactIntArray implements Cloneable {
    /**
     * Default constructor for UCompactIntArray, the default value of the
     * compact array is 0.
     */
    public UCompactIntArray() {
        values = new int[16][];
        indices = new short[16][];
        blockTouched = new boolean[16][];
        planeTouched = new boolean[16];
    }

    public UCompactIntArray(int defaultValue) {
        this();
        this.defaultValue = defaultValue;
    }

    /**
     * Get the mapped value of a Unicode character.
     * @param index the character to get the mapped value with
     * @return the mapped value of the given character
     */
    public int elementAt(int index) {
        int plane = (index & PLANEMASK) >> PLANESHIFT;
        if (!planeTouched[plane]) {
            return defaultValue;
        }
        index &= CODEPOINTMASK;
        return values[plane][(indices[plane][index >> BLOCKSHIFT] & 0xFFFF)
                       + (index & BLOCKMASK)];
    }


    /**
     * Set a new value for a Unicode character.
     * Set automatically expands the array if it is compacted.
     * @param index the character to set the mapped value with
     * @param value the new mapped value
     */
    public void setElementAt(int index, int value) {
        if (isCompact) {
            expand();
        }
        int plane = (index & PLANEMASK) >> PLANESHIFT;
        if (!planeTouched[plane]) {
            initPlane(plane);
        }
        index &= CODEPOINTMASK;
        values[plane][index] = value;
        blockTouched[plane][index >> BLOCKSHIFT] = true;
    }


    /**
     * Compact the array.
     */
    public void compact() {
        if (isCompact) {
            return;
        }
        for (int plane = 0; plane < PLANECOUNT; plane++) {
            if (!planeTouched[plane]) {
                continue;
            }
            int limitCompacted = 0;
            int iBlockStart = 0;
            short iUntouched = -1;

            for (int i = 0; i < indices[plane].length; ++i, iBlockStart += BLOCKCOUNT) {
                indices[plane][i] = -1;
                if (!blockTouched[plane][i] && iUntouched != -1) {
                    // If no values in this block were set, we can just set its
                    // index to be the same as some other block with no values
                    // set, assuming we've seen one yet.
                    indices[plane][i] = iUntouched;
                } else {
                    int jBlockStart = limitCompacted * BLOCKCOUNT;
                    if (i > limitCompacted) {
                        System.arraycopy(values[plane], iBlockStart,
                                         values[plane], jBlockStart, BLOCKCOUNT);
                    }
                    if (!blockTouched[plane][i]) {
                        // If this is the first untouched block we've seen, remember it.
                        iUntouched = (short)jBlockStart;
                    }
                    indices[plane][i] = (short)jBlockStart;
                    limitCompacted++;
                }
            }

            // we are done compacting, so now make the array shorter
            int newSize = limitCompacted * BLOCKCOUNT;
            int[] result = new int[newSize];
            System.arraycopy(values[plane], 0, result, 0, newSize);
            values[plane] = result;
            blockTouched[plane] = null;
        }
        isCompact = true;
    }


    // --------------------------------------------------------------
    // private
    // --------------------------------------------------------------
    /**
     * Expanded takes the array back to a 0x10ffff element array
     */
    private void expand() {
        int i;
        if (isCompact) {
            int[]   tempArray;
            for (int plane = 0; plane < PLANECOUNT; plane++) {
                if (!planeTouched[plane]) {
                    continue;
                }
                blockTouched[plane] = new boolean[INDEXCOUNT];
                tempArray = new int[UNICODECOUNT];
                for (i = 0; i < UNICODECOUNT; ++i) {
                    tempArray[i] = values[plane][indices[plane][i >> BLOCKSHIFT]
                                                & 0xffff + (i & BLOCKMASK)];
                    blockTouched[plane][i >> BLOCKSHIFT] = true;
                }
                for (i = 0; i < INDEXCOUNT; ++i) {
                    indices[plane][i] = (short)(i<<BLOCKSHIFT);
                }
                values[plane] = tempArray;
            }
            isCompact = false;
        }
    }

    private void initPlane(int plane) {
        values[plane] = new int[UNICODECOUNT];
        indices[plane] = new short[INDEXCOUNT];
        blockTouched[plane] = new boolean[INDEXCOUNT];
        planeTouched[plane] = true;

        if (planeTouched[0] && plane != 0) {
            System.arraycopy(indices[0], 0, indices[plane], 0, INDEXCOUNT);
        } else {
            for (int i = 0; i < INDEXCOUNT; ++i) {
                indices[plane][i] = (short)(i<<BLOCKSHIFT);
            }
        }
        for (int i = 0; i < UNICODECOUNT; ++i) {
            values[plane][i] = defaultValue;
        }
    }

    public int getKSize() {
        int size = 0;
        for (int plane = 0; plane < PLANECOUNT; plane++) {
            if (planeTouched[plane]) {
                size += (values[plane].length * 4 + indices[plane].length * 2);
            }
        }
        return size / 1024;
    }

    private static final int PLANEMASK = 0x30000;
    private static final int PLANESHIFT = 16;
    private static final int PLANECOUNT = 0x10;
    private static final int CODEPOINTMASK  = 0xffff;

    private static final int UNICODECOUNT = 0x10000;
    private static final int BLOCKSHIFT = 7;
    private static final int BLOCKCOUNT = (1<<BLOCKSHIFT);
    private static final int INDEXSHIFT = (16-BLOCKSHIFT);
    private static final int INDEXCOUNT = (1<<INDEXSHIFT);
    private static final int BLOCKMASK = BLOCKCOUNT - 1;

    private int defaultValue;
    private int values[][];
    private short indices[][];
    private boolean isCompact;
    private boolean[][] blockTouched;
    private boolean[] planeTouched;
};
