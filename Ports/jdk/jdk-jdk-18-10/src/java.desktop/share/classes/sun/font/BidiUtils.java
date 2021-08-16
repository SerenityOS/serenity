/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright IBM Corp. 1999-2000 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

package sun.font;

import java.text.Bidi;

public final class BidiUtils {



    /**
     * Return the level of each character into the levels array starting at start.
     * This is a convenience method for clients who prefer to use an explicit levels
     * array instead of iterating over the runs.
     *
     * @param levels the array to receive the character levels
     * @param start the starting offset into the array
     * @throws IndexOutOfBoundsException if {@code start} is less than 0 or
     * {@code start + getLength()} is greater than {@code levels.length}.
     */
    public static void getLevels(Bidi bidi, byte[] levels, int start) {
        int limit = start + bidi.getLength();

        if (start < 0 || limit > levels.length) {
            throw new IndexOutOfBoundsException("levels.length = " + levels.length +
                " start: " + start + " limit: " + limit);
        }

        int runCount = bidi.getRunCount();
        int p = start;
        for (int i = 0; i < runCount; ++i) {
            int rlimit = start + bidi.getRunLimit(i);
            byte rlevel = (byte)bidi.getRunLevel(i);

            while (p < rlimit) {
                levels[p++] = rlevel;
            }
        }
    }

    /**
     * Return an array containing the resolved bidi level of each character, in logical order.
     * @return an array containing the level of each character, in logical order.
     */
    public static byte[] getLevels(Bidi bidi) {
        byte[] levels = new byte[bidi.getLength()];
        getLevels(bidi, levels, 0);
        return levels;
    }

    static final char NUMLEVELS = 62;

    /**
     * Given level data, compute a a visual to logical mapping.
     * The leftmost (or topmost) character is at visual index zero.  The
     * logical index of the character is derived from the visual index
     * by the expression {@code li = map[vi];}.
     * @param levels the levels array
     * @return the mapping array from visual to logical
     */
    public static int[] createVisualToLogicalMap(byte[] levels) {
        int len = levels.length;
        int[] mapping = new int[len];

        byte lowestOddLevel = (byte)(NUMLEVELS + 1);
        byte highestLevel = 0;

        // initialize mapping and levels

        for (int i = 0; i < len; i++) {
            mapping[i] = i;

            byte level = levels[i];
            if (level > highestLevel) {
                highestLevel = level;
            }

            if ((level & 0x01) != 0 && level < lowestOddLevel) {
                lowestOddLevel = level;
            }
        }

        while (highestLevel >= lowestOddLevel) {
            int i = 0;
            for (;;) {
                while (i < len && levels[i] < highestLevel) {
                    i++;
                }
                int begin = i++;

                if (begin == levels.length) {
                    break; // no more runs at this level
                }

                while (i < len && levels[i] >= highestLevel) {
                    i++;
                }
                int end = i - 1;

                while (begin < end) {
                    int temp = mapping[begin];
                    mapping[begin] = mapping[end];
                    mapping[end] = temp;
                    ++begin;
                    --end;
                }
            }

            --highestLevel;
        }

        return mapping;
    }

    /**
     * Return the inverse position map.  The source array must map one-to-one (each value
     * is distinct and the values run from zero to the length of the array minus one).
     * For example, if {@code values[i] = j}, then {@code inverse[j] = i}.
     * @param values the source ordering array
     * @return the inverse array
     */
    public static int[] createInverseMap(int[] values) {
        if (values == null) {
            return null;
        }

        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[values[i]] = i;
        }

        return result;
    }


    /**
     * Return an array containing contiguous values from 0 to length
     * having the same ordering as the source array. If this would be
     * a canonical ltr ordering, return null.  The data in values[] is NOT
     * required to be a permutation, but elements in values are required
     * to be distinct.
     * @param values an array containing the discontiguous values
     * @return the contiguous values
     */
    public static int[] createContiguousOrder(int[] values) {
        if (values != null) {
            return computeContiguousOrder(values, 0, values.length);
        }

        return null;
    }

    /**
     * Compute a contiguous order for the range start, limit.
     */
    private static int[] computeContiguousOrder(int[] values, int start,
                                                int limit) {

        int[] result = new int[limit-start];
        for (int i=0; i < result.length; i++) {
            result[i] = i + start;
        }

        // now we'll sort result[], with the following comparison:
        // result[i] lessthan result[j] iff values[result[i]] < values[result[j]]

        // selection sort for now;  use more elaborate sorts if desired
        for (int i=0; i < result.length-1; i++) {
            int minIndex = i;
            int currentValue = values[result[minIndex]];
            for (int j=i; j < result.length; j++) {
                if (values[result[j]] < currentValue) {
                    minIndex = j;
                    currentValue = values[result[minIndex]];
                }
            }
            int temp = result[i];
            result[i] = result[minIndex];
            result[minIndex] = temp;
        }

        // shift result by start:
        if (start != 0) {
            for (int i=0; i < result.length; i++) {
                result[i] -= start;
            }
        }

        // next, check for canonical order:
        int k;
        for (k=0; k < result.length; k++) {
            if (result[k] != k) {
                break;
            }
        }

        if (k == result.length) {
            return null;
        }

        // now return inverse of result:
        return createInverseMap(result);
    }

    /**
     * Return an array containing the data in the values array from start up to limit,
     * normalized to fall within the range from 0 up to limit - start.
     * If this would be a canonical ltr ordering, return null.
     * NOTE: This method assumes that values[] is a logical to visual map
     * generated from levels[].
     * @param values the source mapping
     * @param levels the levels corresponding to the values
     * @param start the starting offset in the values and levels arrays
     * @param limit the limiting offset in the values and levels arrays
     * @return the normlized map
     */
    public static int[] createNormalizedMap(int[] values, byte[] levels,
                                           int start, int limit) {

        if (values != null) {
            if (start != 0 || limit != values.length) {
                // levels optimization
                boolean copyRange, canonical;
                byte primaryLevel;

                if (levels == null) {
                    primaryLevel = (byte) 0x0;
                    copyRange = true;
                    canonical = true;
                }
                else {
                    if (levels[start] == levels[limit-1]) {
                        primaryLevel = levels[start];
                        canonical = (primaryLevel & (byte)0x1) == 0;

                        // scan for levels below primary
                        int i;
                        for (i=start; i < limit; i++) {
                            if (levels[i] < primaryLevel) {
                                break;
                            }
                            if (canonical) {
                                canonical = levels[i] == primaryLevel;
                            }
                        }

                        copyRange = (i == limit);
                    }
                    else {
                        copyRange = false;

                        // these don't matter;  but the compiler cares:
                        primaryLevel = (byte) 0x0;
                        canonical = false;
                    }
                }

                if (copyRange) {
                    if (canonical) {
                        return null;
                    }

                    int[] result = new int[limit-start];
                    int baseValue;

                    if ((primaryLevel & (byte)0x1) != 0) {
                        baseValue = values[limit-1];
                    } else {
                        baseValue = values[start];
                    }

                    if (baseValue == 0) {
                        System.arraycopy(values, start, result, 0, limit-start);
                    }
                    else {
                        for (int j=0; j < result.length; j++) {
                            result[j] = values[j+start] - baseValue;
                        }
                    }

                    return result;
                }
                else {
                    return computeContiguousOrder(values, start, limit);
                }
            }
            else {
                return values;
            }
        }

        return null;
    }

    /**
     * Reorder the objects in the array into visual order based on their levels.
     * This is a utility function to use when you have a collection of objects
     * representing runs of text in logical order, each run containing text
     * at a single level.  The elements in the objects array will be reordered
     * into visual order assuming each run of text has the level provided
     * by the corresponding element in the levels array.
     * @param levels an array representing the bidi level of each object
     * @param objects the array of objects to be reordered into visual order
     */
    public static void reorderVisually(byte[] levels, Object[] objects) {
        int len = levels.length;

        byte lowestOddLevel = (byte)(NUMLEVELS + 1);
        byte highestLevel = 0;

        // initialize mapping and levels

        for (int i = 0; i < len; i++) {
            byte level = levels[i];
            if (level > highestLevel) {
                highestLevel = level;
            }

            if ((level & 0x01) != 0 && level < lowestOddLevel) {
                lowestOddLevel = level;
            }
        }

        while (highestLevel >= lowestOddLevel) {
            int i = 0;
            for (;;) {
                while (i < len && levels[i] < highestLevel) {
                    i++;
                }
                int begin = i++;

                if (begin == levels.length) {
                    break; // no more runs at this level
                }

                while (i < len && levels[i] >= highestLevel) {
                    i++;
                }
                int end = i - 1;

                while (begin < end) {
                    Object temp = objects[begin];
                    objects[begin] = objects[end];
                    objects[end] = temp;
                    ++begin;
                    --end;
                }
            }

            --highestLevel;
        }
    }
}
