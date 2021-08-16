/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996 - 1997, All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998, All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by Taligent, Inc., a wholly-owned subsidiary
 * of IBM. These materials are provided under terms of a License
 * Agreement between Taligent and Sun. This technology is protected
 * by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.awt.font;

/*
 * one info for each side of each glyph
 * separate infos for grow and shrink case
 * !!! this doesn't really need to be a separate class.  If we keep it
 * separate, probably the newJustify code from TextLayout belongs here as well.
 */

class TextJustifier {
    private GlyphJustificationInfo[] info;
    private int start;
    private int limit;

    static boolean DEBUG = false;

    /**
     * Initialize the justifier with an array of infos corresponding to each
     * glyph. Start and limit indicate the range of the array to examine.
     */
    TextJustifier(GlyphJustificationInfo[] info, int start, int limit) {
        this.info = info;
        this.start = start;
        this.limit = limit;

        if (DEBUG) {
            System.out.println("start: " + start + ", limit: " + limit);
            for (int i = start; i < limit; i++) {
                GlyphJustificationInfo gji = info[i];
                System.out.println("w: " + gji.weight + ", gp: " +
                                   gji.growPriority + ", gll: " +
                                   gji.growLeftLimit + ", grl: " +
                                   gji.growRightLimit);
            }
        }
    }

    public static final int MAX_PRIORITY = 3;

    /**
     * Return an array of deltas twice as long as the original info array,
     * indicating the amount by which each side of each glyph should grow
     * or shrink.
     *
     * Delta should be positive to expand the line, and negative to compress it.
     */
    public float[] justify(float delta) {
        float[] deltas = new float[info.length * 2];

        boolean grow = delta > 0;

        if (DEBUG)
            System.out.println("delta: " + delta);

        // make separate passes through glyphs in order of decreasing priority
        // until justifyDelta is zero or we run out of priorities.
        int fallbackPriority = -1;
        for (int p = 0; delta != 0; p++) {
            /*
             * special case 'fallback' iteration, set flag and recheck
             * highest priority
             */
            boolean lastPass = p > MAX_PRIORITY;
            if (lastPass)
                p = fallbackPriority;

            // pass through glyphs, first collecting weights and limits
            float weight = 0;
            float gslimit = 0;
            float absorbweight = 0;
            for (int i = start; i < limit; i++) {
                GlyphJustificationInfo gi = info[i];
                if ((grow ? gi.growPriority : gi.shrinkPriority) == p) {
                    if (fallbackPriority == -1) {
                        fallbackPriority = p;
                    }

                    if (i != start) { // ignore left of first character
                        weight += gi.weight;
                        if (grow) {
                            gslimit += gi.growLeftLimit;
                            if (gi.growAbsorb) {
                                absorbweight += gi.weight;
                            }
                        } else {
                            gslimit += gi.shrinkLeftLimit;
                            if (gi.shrinkAbsorb) {
                                absorbweight += gi.weight;
                            }
                        }
                    }

                    if (i + 1 != limit) { // ignore right of last character
                        weight += gi.weight;
                        if (grow) {
                            gslimit += gi.growRightLimit;
                            if (gi.growAbsorb) {
                                absorbweight += gi.weight;
                            }
                        } else {
                            gslimit += gi.shrinkRightLimit;
                            if (gi.shrinkAbsorb) {
                                absorbweight += gi.weight;
                            }
                        }
                    }
                }
            }

            // did we hit the limit?
            if (!grow) {
                gslimit = -gslimit; // negative for negative deltas
            }
            boolean hitLimit = (weight == 0) || (!lastPass && ((delta < 0) == (delta < gslimit)));
            boolean absorbing = hitLimit && absorbweight > 0;

            // predivide delta by weight
            float weightedDelta = 0;
            if (weight != 0) { // not used if weight == 0
                weightedDelta = delta / weight;
            }

            float weightedAbsorb = 0;
            if (hitLimit && absorbweight != 0) {
                weightedAbsorb = (delta - gslimit) / absorbweight;
            }

            if (DEBUG) {
                System.out.println("pass: " + p +
                    ", d: " + delta +
                    ", l: " + gslimit +
                    ", w: " + weight +
                    ", aw: " + absorbweight +
                    ", wd: " + weightedDelta +
                    ", wa: " + weightedAbsorb +
                    ", hit: " + (hitLimit ? "y" : "n"));
            }

            // now allocate this based on ratio of weight to total weight
            int n = start * 2;
            for (int i = start; i < limit; i++) {
                GlyphJustificationInfo gi = info[i];
                if ((grow ? gi.growPriority : gi.shrinkPriority) == p) {
                    if (i != start) { // ignore left
                        float d;
                        if (hitLimit) {
                            // factor in sign
                            d = grow ? gi.growLeftLimit : -gi.shrinkLeftLimit;
                            if (absorbing) {
                                // sign factored in already
                               d += gi.weight * weightedAbsorb;
                            }
                        } else {
                            // sign factored in already
                            d = gi.weight * weightedDelta;
                        }

                        deltas[n] += d;
                    }
                    n++;

                    if (i + 1 != limit) { // ignore right
                        float d;
                        if (hitLimit) {
                            d = grow ? gi.growRightLimit : -gi.shrinkRightLimit;
                            if (absorbing) {
                                d += gi.weight * weightedAbsorb;
                            }
                        } else {
                            d = gi.weight * weightedDelta;
                        }

                        deltas[n] += d;
                    }
                    n++;
                } else {
                    n += 2;
                }
            }

            if (!lastPass && hitLimit && !absorbing) {
                delta -= gslimit;
            } else {
                delta = 0; // stop iteration
            }
        }

        if (DEBUG) {
            float total = 0;
            for (int i = 0; i < deltas.length; i++) {
                total += deltas[i];
                System.out.print(deltas[i] + ", ");
                if (i % 20 == 9) {
                    System.out.println();
                }
            }
            System.out.println("\ntotal: " + total);
            System.out.println();
        }

        return deltas;
    }
}
