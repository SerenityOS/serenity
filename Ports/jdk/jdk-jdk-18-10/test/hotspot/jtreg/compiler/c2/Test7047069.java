/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @key randomness
 * @bug 7047069
 * @summary Array can dynamically change size when assigned to an object field
 * @modules java.desktop
 *
 * @library /test/lib
 * @run main/othervm -Xbatch compiler.c2.Test7047069
 */

package compiler.c2;

import java.awt.geom.Line2D;
import java.util.Random;
import jdk.test.lib.Utils;

public class Test7047069 {
    static boolean verbose;

    static final int GROW_SIZE = 24;    // Multiple of cubic & quad curve size

    float squareflat;           // Square of the flatness parameter
                    // for testing against squared lengths

    int limit;              // Maximum number of recursion levels

    float hold[] = new float[14];   // The cache of interpolated coords
                    // Note that this must be long enough
                    // to store a full cubic segment and
                    // a relative cubic segment to avoid
                    // aliasing when copying the coords
                    // of a curve to the end of the array.
                    // This is also serendipitously equal
                    // to the size of a full quad segment
                    // and 2 relative quad segments.

    int holdEnd;            // The index of the last curve segment
                    // being held for interpolation

    int holdIndex;          // The index of the curve segment
                    // that was last interpolated.  This
                    // is the curve segment ready to be
                    // returned in the next call to
                    // currentSegment().

    int levels[];           // The recursion level at which
                    // each curve being held in storage
                    // was generated.

    int levelIndex;         // The index of the entry in the
                    // levels array of the curve segment
                    // at the holdIndex

    public static void subdivide(float src[], int srcoff,
                                 float left[], int leftoff,
                                 float right[], int rightoff)
    {
        float x1 = src[srcoff + 0];
        float y1 = src[srcoff + 1];
        float ctrlx = src[srcoff + 2];
        float ctrly = src[srcoff + 3];
        float x2 = src[srcoff + 4];
        float y2 = src[srcoff + 5];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
        x1 = (x1 + ctrlx) / 2f;
        y1 = (y1 + ctrly) / 2f;
        x2 = (x2 + ctrlx) / 2f;
        y2 = (y2 + ctrly) / 2f;
        ctrlx = (x1 + x2) / 2f;
        ctrly = (y1 + y2) / 2f;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx;
            left[leftoff + 5] = ctrly;
        }
        if (right != null) {
            right[rightoff + 0] = ctrlx;
            right[rightoff + 1] = ctrly;
            right[rightoff + 2] = x2;
            right[rightoff + 3] = y2;
        }
    }

    public static double getFlatnessSq(float coords[], int offset) {
        return Line2D.ptSegDistSq(coords[offset + 0], coords[offset + 1],
                                  coords[offset + 4], coords[offset + 5],
                                  coords[offset + 2], coords[offset + 3]);
    }

    public Test7047069() {
        this.squareflat = .0001f * .0001f;
        holdIndex = hold.length - 6;
        holdEnd = hold.length - 2;
        Random rng = Utils.getRandomInstance();
        hold[holdIndex + 0] = (float) (rng.nextDouble() * 100);
        hold[holdIndex + 1] = (float) (rng.nextDouble() * 100);
        hold[holdIndex + 2] = (float) (rng.nextDouble() * 100);
        hold[holdIndex + 3] = (float) (rng.nextDouble() * 100);
        hold[holdIndex + 4] = (float) (rng.nextDouble() * 100);
        hold[holdIndex + 5] = (float) (rng.nextDouble() * 100);
        levelIndex = 0;
        this.limit = 10;
        this.levels = new int[limit + 1];
    }

    /*
     * Ensures that the hold array can hold up to (want) more values.
     * It is currently holding (hold.length - holdIndex) values.
     */
    void ensureHoldCapacity(int want) {
        if (holdIndex - want < 0) {
            int have = hold.length - holdIndex;
            int newsize = hold.length + GROW_SIZE;
            float newhold[] = new float[newsize];
            System.arraycopy(hold, holdIndex,
                     newhold, holdIndex + GROW_SIZE,
                     have);
            if (verbose) System.err.println("old hold = "+hold+"["+hold.length+"]");
            if (verbose) System.err.println("replacement hold = "+newhold+"["+newhold.length+"]");
            hold = newhold;
            if (verbose) System.err.println("new hold = "+hold+"["+hold.length+"]");
            if (verbose) System.err.println("replacement hold still = "+newhold+"["+newhold.length+"]");
            holdIndex += GROW_SIZE;
            holdEnd += GROW_SIZE;
        }
    }

    private boolean next() {
        if (holdIndex >= holdEnd) {
            return false;
        }

        int level = levels[levelIndex];
        while (level < limit) {
            if (getFlatnessSq(hold, holdIndex) < squareflat) {
                break;
            }

            ensureHoldCapacity(4);
            subdivide(hold, holdIndex,
                      hold, holdIndex - 4,
                      hold, holdIndex);
            holdIndex -= 4;

            // Now that we have subdivided, we have constructed
            // two curves of one depth lower than the original
            // curve.  One of those curves is in the place of
            // the former curve and one of them is in the next
            // set of held coordinate slots.  We now set both
            // curves level values to the next higher level.
            level++;
            levels[levelIndex] = level;
            levelIndex++;
            levels[levelIndex] = level;
        }

        // This curve segment is flat enough, or it is too deep
        // in recursion levels to try to flatten any more.  The
        // two coordinates at holdIndex+4 and holdIndex+5 now
        // contain the endpoint of the curve which can be the
        // endpoint of an approximating line segment.
        holdIndex += 4;
        levelIndex--;
        return true;
    }

    public static void main(String argv[]) {
        verbose = (argv.length > 0);
        for (int i = 0; i < 100000; i++) {
            Test7047069 st = new Test7047069();
            while (st.next()) {}
        }
    }
}
