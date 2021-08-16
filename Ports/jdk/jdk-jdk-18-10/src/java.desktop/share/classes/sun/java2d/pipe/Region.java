/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import sun.java2d.loops.TransformHelper;

import static java.lang.Double.isNaN;

/**
 * This class encapsulates a definition of a two dimensional region which
 * consists of a number of Y ranges each containing multiple X bands.
 * <p>
 * A rectangular Region is allowed to have a null band list in which
 * case the rectangular shape is defined by the bounding box parameters
 * (lox, loy, hix, hiy).
 * <p>
 * The band list, if present, consists of a list of rows in ascending Y
 * order, ending at endIndex which is the index beyond the end of the
 * last row.  Each row consists of at least 3 + 2n entries (n >= 1)
 * where the first 3 entries specify the Y range as start, end, and
 * the number of X ranges in that Y range.  These 3 entries are
 * followed by pairs of X coordinates in ascending order:
 * <pre>
 * bands[rowstart+0] = Y0;        // starting Y coordinate
 * bands[rowstart+1] = Y1;        // ending Y coordinate - endY > startY
 * bands[rowstart+2] = N;         // number of X bands - N >= 1
 *
 * bands[rowstart+3] = X10;       // starting X coordinate of first band
 * bands[rowstart+4] = X11;       // ending X coordinate of first band
 * bands[rowstart+5] = X20;       // starting X coordinate of second band
 * bands[rowstart+6] = X21;       // ending X coordinate of second band
 * ...
 * bands[rowstart+3+N*2-2] = XN0; // starting X coord of last band
 * bands[rowstart+3+N*2-1] = XN1; // ending X coord of last band
 *
 * bands[rowstart+3+N*2] = ...    // start of next Y row
 * </pre>
 */
public final class Region {
    private static final int INIT_SIZE = 50;
    private static final int GROW_SIZE = 50;

    public static final Region EMPTY_REGION = new Region(0, 0, 0, 0);
    public static final Region WHOLE_REGION = new Region(
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);

    private int lox;
    private int loy;
    private int hix;
    private int hiy;

    int endIndex;
    int[] bands;

    private static native void initIDs();

    static {
        initIDs();
    }

    /**
     * Adds the dimension {@code dim} to the coordinate
     * {@code start} with appropriate clipping.  If
     * {@code dim} is non-positive then the method returns
     * the start coordinate.  If the sum overflows an integer
     * data type then the method returns {@code Integer.MAX_VALUE}.
     */
    public static int dimAdd(int start, int dim) {
        if (dim <= 0) return start;
        if ((dim += start) < start) return Integer.MAX_VALUE;
        return dim;
    }

    /**
     * Adds the delta {@code dv} to the value {@code v} with
     * appropriate clipping to the bounds of Integer resolution.
     * If the answer would be greater than {@code Integer.MAX_VALUE}
     * then {@code Integer.MAX_VALUE} is returned.
     * If the answer would be less than {@code Integer.MIN_VALUE}
     * then {@code Integer.MIN_VALUE} is returned.
     * Otherwise the sum is returned.
     */
    public static int clipAdd(int v, int dv) {
        int newv = v + dv;
        if ((newv > v) != (dv > 0)) {
            newv = (dv < 0) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return newv;
    }

    /**
     * Returns the closest {@code int} to the argument, with ties rounding to
     * negative infinity.
     * <p>
     * Special cases:
     * <ul><li>If the argument is NaN, the result is 0.
     * <li>If the argument is negative infinity or any value less than or
     * equal to the value of {@code Integer.MIN_VALUE}, the result is
     * equal to the value of {@code Integer.MIN_VALUE}.
     * <li>If the argument is positive infinity or any value greater than or
     * equal to the value of {@code Integer.MAX_VALUE}, the result is
     * equal to the value of {@code Integer.MAX_VALUE}.</ul>
     *
     * @param   coordinate a floating-point value to be rounded to an integer
     * @return  the value of the argument rounded to the nearest
     *          {@code int} value.
     */
    public static int clipRound(final double coordinate) {
        final double newv = coordinate - 0.5;
        if (newv < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (newv > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(newv);
    }

    /**
     * Multiply the scale factor {@code sv} and the value {@code v} with
     * appropriate clipping to the bounds of Integer resolution. If the answer
     * would be greater than {@code Integer.MAX_VALUE} then {@code
     * Integer.MAX_VALUE} is returned. If the answer would be less than {@code
     * Integer.MIN_VALUE} then {@code Integer.MIN_VALUE} is returned. Otherwise
     * the multiplication is returned.
     */
    public static int clipScale(final int v, final double sv) {
        if (sv == 1.0) {
            return v;
        }
        final double newv = v * sv;
        if (newv < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (newv > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.round(newv);
    }

    private Region(int lox, int loy, int hix, int hiy) {
        this.lox = lox;
        this.loy = loy;
        this.hix = hix;
        this.hiy = hiy;
    }

    private Region(int lox, int loy, int hix, int hiy, int[] bands, int end) {
        this.lox = lox;
        this.loy = loy;
        this.hix = hix;
        this.hiy = hiy;
        this.bands = bands;
        this.endIndex = end;
    }

    /**
     * Returns a Region object covering the pixels which would be
     * touched by a fill or clip operation on a Graphics implementation
     * on the specified Shape object under the optionally specified
     * AffineTransform object.
     *
     * @param s a non-null Shape object specifying the geometry enclosing
     *          the pixels of interest
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration, or
     *          {@code null} if untransformed coordinates are desired
     */
    public static Region getInstance(Shape s, AffineTransform at) {
        return getInstance(WHOLE_REGION, false, s, at);
    }

    /**
     * Returns a Region object covering the pixels which would be
     * touched by a fill or clip operation on a Graphics implementation
     * on the specified Shape object under the optionally specified
     * AffineTransform object further restricted by the specified
     * device bounds.
     * <p>
     * Note that only the bounds of the specified Region are used to
     * restrict the resulting Region.
     * If devBounds is non-rectangular and clipping to the specific
     * bands of devBounds is needed, then an intersection of the
     * resulting Region with devBounds must be performed in a
     * subsequent step.
     *
     * @param devBounds a non-null Region specifying some bounds to
     *          clip the geometry to
     * @param s a non-null Shape object specifying the geometry enclosing
     *          the pixels of interest
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration, or
     *          {@code null} if untransformed coordinates are desired
     */
    public static Region getInstance(Region devBounds,
                                     Shape s, AffineTransform at)
    {
        return getInstance(devBounds, false, s, at);
    }

    /**
     * Returns a Region object covering the pixels which would be
     * touched by a fill or clip operation on a Graphics implementation
     * on the specified Shape object under the optionally specified
     * AffineTransform object further restricted by the specified
     * device bounds.
     * If the normalize parameter is true then coordinate normalization
     * is performed as per the 2D Graphics non-antialiasing implementation
     * of the VALUE_STROKE_NORMALIZE hint.
     * <p>
     * Note that only the bounds of the specified Region are used to
     * restrict the resulting Region.
     * If devBounds is non-rectangular and clipping to the specific
     * bands of devBounds is needed, then an intersection of the
     * resulting Region with devBounds must be performed in a
     * subsequent step.
     *
     * @param devBounds a non-null Region specifying some bounds to
     *          clip the geometry to
     * @param normalize a boolean indicating whether or not to apply
     *          normalization
     * @param s a non-null Shape object specifying the geometry enclosing
     *          the pixels of interest
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration, or
     *          {@code null} if untransformed coordinates are desired
     */
    public static Region getInstance(Region devBounds, boolean normalize,
                                     Shape s, AffineTransform at)
    {
        // Optimize for empty shapes to avoid involving the SpanIterator
        if (s instanceof RectangularShape &&
                ((RectangularShape)s).isEmpty())
        {
            return EMPTY_REGION;
        }

        int[] box = new int[4];
        ShapeSpanIterator sr = new ShapeSpanIterator(normalize);
        try {
            sr.setOutputArea(devBounds);
            sr.appendPath(s.getPathIterator(at));
            sr.getPathBox(box);
            return Region.getInstance(box, sr);
        } finally {
            sr.dispose();
        }
    }

    /**
     * Returns a Region object with a rectangle of interest specified by the
     * indicated rectangular area in lox, loy, hix, hiy and edges array, which
     * is located relative to the rectangular area. Edges array - 0,1 are y
     * range, 2N,2N+1 are x ranges, 1 per y range.
     *
     * @see TransformHelper
     */
    static Region getInstance(final int lox, final int loy, final int hix,
                              final int hiy, final int[] edges) {
        final int y1 = edges[0];
        final int y2 = edges[1];
        if (hiy <= loy || hix <= lox || y2 <= y1) {
            return EMPTY_REGION;
        }
        // rowsNum * (3 + 1 * 2)
        final int[] bands = new int[(y2 - y1) * 5];
        int end = 0;
        int index = 2;
        for (int y = y1; y < y2; ++y) {
            final int spanlox = Math.max(clipAdd(lox, edges[index++]), lox);
            final int spanhix = Math.min(clipAdd(lox, edges[index++]), hix);
            if (spanlox < spanhix) {
                final int spanloy = Math.max(clipAdd(loy, y), loy);
                final int spanhiy = Math.min(clipAdd(spanloy, 1), hiy);
                if (spanloy < spanhiy) {
                    bands[end++] = spanloy;
                    bands[end++] = spanhiy;
                    bands[end++] = 1; // 1 span per row
                    bands[end++] = spanlox;
                    bands[end++] = spanhix;
                }
            }
        }
        return end != 0 ? new Region(lox, loy, hix, hiy, bands, end)
                        : EMPTY_REGION;
    }

    /**
     * Returns a Region object with a rectangle of interest specified
     * by the indicated Rectangle object.
     * <p>
     * This method can also be used to create a simple rectangular
     * region.
     */
    public static Region getInstance(Rectangle r) {
        return Region.getInstanceXYWH(r.x, r.y, r.width, r.height);
    }

    /**
     * Returns a Region object with a rectangle of interest specified
     * by the indicated rectangular area in x, y, width, height format.
     * <p>
     * This method can also be used to create a simple rectangular
     * region.
     */
    public static Region getInstanceXYWH(int x, int y, int w, int h) {
        return Region.getInstanceXYXY(x, y, dimAdd(x, w), dimAdd(y, h));
    }

    /**
     * Returns a Region object with a rectangle of interest specified
     * by the indicated span array.
     * <p>
     * This method can also be used to create a simple rectangular
     * region.
     */
    public static Region getInstance(int[] box) {
        return new Region(box[0], box[1], box[2], box[3]);
    }

    /**
     * Returns a Region object with a rectangle of interest specified
     * by the indicated rectangular area in lox, loy, hix, hiy format.
     * <p>
     * This method can also be used to create a simple rectangular
     * region.
     */
    public static Region getInstanceXYXY(int lox, int loy, int hix, int hiy) {
        return new Region(lox, loy, hix, hiy);
    }

    /**
     * Returns a Region object with a rectangle of interest specified by the
     * indicated rectangular area in lox, loy, hix, hiy format.
     * <p/>
     * Appends the list of spans returned from the indicated SpanIterator. Each
     * span must be at a higher starting Y coordinate than the previous data or
     * it must have a Y range equal to the highest Y band in the region and a
     * higher X coordinate than any of the spans in that band.
     */
    public static Region getInstance(int[] box, SpanIterator si) {
        Region ret = new Region(box[0], box[1], box[2], box[3]);
        ret.appendSpans(si);
        return ret;
    }

    /**
     * Appends the list of spans returned from the indicated
     * SpanIterator.  Each span must be at a higher starting
     * Y coordinate than the previous data or it must have a
     * Y range equal to the highest Y band in the region and a
     * higher X coordinate than any of the spans in that band.
     */
    private void appendSpans(SpanIterator si) {
        int[] box = new int[6];

        while (si.nextSpan(box)) {
            appendSpan(box);
        }

        endRow(box);
        calcBBox();
    }

    /**
     * Returns a Region object that represents the same list of rectangles as
     * the current Region object, scaled by the specified sx, sy factors.
     */
    public Region getScaledRegion(final double sx, final double sy) {
        if (sx == 0 || sy == 0 || this == EMPTY_REGION) {
            return EMPTY_REGION;
        }
        if ((sx == 1.0 && sy == 1.0) || (this == WHOLE_REGION)) {
            return this;
        }

        int tlox = clipScale(lox, sx);
        int tloy = clipScale(loy, sy);
        int thix = clipScale(hix, sx);
        int thiy = clipScale(hiy, sy);
        Region ret = new Region(tlox, tloy, thix, thiy);
        int[] bands = this.bands;
        if (bands != null) {
            int end = endIndex;
            int[] newbands = new int[end];
            int i = 0; // index for source bands
            int j = 0; // index for translated newbands
            int ncol;
            while (i < end) {
                int y1, y2;
                newbands[j++] = y1   = clipScale(bands[i++], sy);
                newbands[j++] = y2   = clipScale(bands[i++], sy);
                newbands[j++] = ncol = bands[i++];
                int savej = j;
                if (y1 < y2) {
                    while (--ncol >= 0) {
                        int x1 = clipScale(bands[i++], sx);
                        int x2 = clipScale(bands[i++], sx);
                        if (x1 < x2) {
                            newbands[j++] = x1;
                            newbands[j++] = x2;
                        }
                    }
                } else {
                    i += ncol * 2;
                }
                // Did we get any non-empty bands in this row?
                if (j > savej) {
                    newbands[savej-1] = (j - savej) / 2;
                } else {
                    j = savej - 3;
                }
            }
            if (j <= 5) {
                if (j < 5) {
                    // No rows or bands were generated...
                    ret.lox = ret.loy = ret.hix = ret.hiy = 0;
                } else {
                    // Only generated one single rect in the end...
                    ret.loy = newbands[0];
                    ret.hiy = newbands[1];
                    ret.lox = newbands[3];
                    ret.hix = newbands[4];
                }
                // ret.endIndex and ret.bands were never initialized...
                // ret.endIndex = 0;
                // ret.newbands = null;
            } else {
                // Generated multiple bands and/or multiple rows...
                ret.endIndex = j;
                ret.bands = newbands;
            }
        }
        return ret;
    }


    /**
     * Returns a Region object that represents the same list of
     * rectangles as the current Region object, translated by
     * the specified dx, dy translation factors.
     */
    public Region getTranslatedRegion(int dx, int dy) {
        if ((dx | dy) == 0) {
            return this;
        }
        int tlox = lox + dx;
        int tloy = loy + dy;
        int thix = hix + dx;
        int thiy = hiy + dy;
        if ((tlox > lox) != (dx > 0) ||
            (tloy > loy) != (dy > 0) ||
            (thix > hix) != (dx > 0) ||
            (thiy > hiy) != (dy > 0))
        {
            return getSafeTranslatedRegion(dx, dy);
        }
        Region ret = new Region(tlox, tloy, thix, thiy);
        int[] bands = this.bands;
        if (bands != null) {
            int end = endIndex;
            ret.endIndex = end;
            int[] newbands = new int[end];
            ret.bands = newbands;
            int i = 0;
            int ncol;
            while (i < end) {
                newbands[i] = bands[i] + dy; i++;
                newbands[i] = bands[i] + dy; i++;
                newbands[i] = ncol = bands[i]; i++;
                while (--ncol >= 0) {
                    newbands[i] = bands[i] + dx; i++;
                    newbands[i] = bands[i] + dx; i++;
                }
            }
        }
        return ret;
    }

    private Region getSafeTranslatedRegion(int dx, int dy) {
        int tlox = clipAdd(lox, dx);
        int tloy = clipAdd(loy, dy);
        int thix = clipAdd(hix, dx);
        int thiy = clipAdd(hiy, dy);
        Region ret = new Region(tlox, tloy, thix, thiy);
        int[] bands = this.bands;
        if (bands != null) {
            int end = endIndex;
            int[] newbands = new int[end];
            int i = 0; // index for source bands
            int j = 0; // index for translated newbands
            int ncol;
            while (i < end) {
                int y1, y2;
                newbands[j++] = y1   = clipAdd(bands[i++], dy);
                newbands[j++] = y2   = clipAdd(bands[i++], dy);
                newbands[j++] = ncol = bands[i++];
                int savej = j;
                if (y1 < y2) {
                    while (--ncol >= 0) {
                        int x1 = clipAdd(bands[i++], dx);
                        int x2 = clipAdd(bands[i++], dx);
                        if (x1 < x2) {
                            newbands[j++] = x1;
                            newbands[j++] = x2;
                        }
                    }
                } else {
                    i += ncol * 2;
                }
                // Did we get any non-empty bands in this row?
                if (j > savej) {
                    newbands[savej-1] = (j - savej) / 2;
                } else {
                    j = savej - 3;
                }
            }
            if (j <= 5) {
                if (j < 5) {
                    // No rows or bands were generated...
                    ret.lox = ret.loy = ret.hix = ret.hiy = 0;
                } else {
                    // Only generated one single rect in the end...
                    ret.loy = newbands[0];
                    ret.hiy = newbands[1];
                    ret.lox = newbands[3];
                    ret.hix = newbands[4];
                }
                // ret.endIndex and ret.bands were never initialized...
                // ret.endIndex = 0;
                // ret.newbands = null;
            } else {
                // Generated multiple bands and/or multiple rows...
                ret.endIndex = j;
                ret.bands = newbands;
            }
        }
        return ret;
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the specified Rectangle.  The return value
     * may be this same object if no clipping occurs.
     */
    public Region getIntersection(Rectangle r) {
        return getIntersectionXYWH(r.x, r.y, r.width, r.height);
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the specified rectangular area.  The return
     * value may be this same object if no clipping occurs.
     */
    public Region getIntersectionXYWH(int x, int y, int w, int h) {
        return getIntersectionXYXY(x, y, dimAdd(x, w), dimAdd(y, h));
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the specified Rectangle2D. The return value
     * may be this same object if no clipping occurs.
     */
    public Region getIntersection(final Rectangle2D r) {
        if (r instanceof Rectangle) {
            return getIntersection((Rectangle) r);
        }
        return getIntersectionXYXY(r.getMinX(), r.getMinY(), r.getMaxX(),
                                   r.getMaxY());
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the specified rectangular area. The return
     * value may be this same object if no clipping occurs.
     */
    public Region getIntersectionXYXY(double lox, double loy, double hix,
                                      double hiy) {
        if (isNaN(lox) || isNaN(loy) || isNaN(hix) || isNaN(hiy)) {
            return EMPTY_REGION;
        }
        return getIntersectionXYXY(clipRound(lox), clipRound(loy),
                                   clipRound(hix), clipRound(hiy));
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the specified rectangular area.  The return
     * value may be this same object if no clipping occurs.
     */
    public Region getIntersectionXYXY(int lox, int loy, int hix, int hiy) {
        if (isInsideXYXY(lox, loy, hix, hiy)) {
            return this;
        }
        Region ret = new Region((lox < this.lox) ? this.lox : lox,
                                (loy < this.loy) ? this.loy : loy,
                                (hix > this.hix) ? this.hix : hix,
                                (hiy > this.hiy) ? this.hiy : hiy);
        if (bands != null) {
            ret.appendSpans(this.getSpanIterator());
        }
        return ret;
    }

    /**
     * Returns a Region object that represents the intersection of this
     * object with the specified Region object.
     * <p>
     * If {@code A} and {@code B} are both Region Objects and
     * {@code C = A.getIntersection(B);} then a point will
     * be contained in {@code C} iff it is contained in both
     * {@code A} and {@code B}.
     * <p>
     * The return value may be this same object or the argument
     * Region object if no clipping occurs.
     */
    public Region getIntersection(Region r) {
        if (this.isInsideQuickCheck(r)) {
            return this;
        }
        if (r.isInsideQuickCheck(this)) {
            return r;
        }
        Region ret = new Region((r.lox < this.lox) ? this.lox : r.lox,
                                (r.loy < this.loy) ? this.loy : r.loy,
                                (r.hix > this.hix) ? this.hix : r.hix,
                                (r.hiy > this.hiy) ? this.hiy : r.hiy);
        if (!ret.isEmpty()) {
            ret.filterSpans(this, r, INCLUDE_COMMON);
        }
        return ret;
    }

    /**
     * Returns a Region object that represents the union of this
     * object with the specified Region object.
     * <p>
     * If {@code A} and {@code B} are both Region Objects and
     * {@code C = A.getUnion(B);} then a point will
     * be contained in {@code C} iff it is contained in either
     * {@code A} or {@code B}.
     * <p>
     * The return value may be this same object or the argument
     * Region object if no augmentation occurs.
     */
    public Region getUnion(Region r) {
        if (r.isEmpty() || r.isInsideQuickCheck(this)) {
            return this;
        }
        if (this.isEmpty() || this.isInsideQuickCheck(r)) {
            return r;
        }
        Region ret = new Region((r.lox > this.lox) ? this.lox : r.lox,
                                (r.loy > this.loy) ? this.loy : r.loy,
                                (r.hix < this.hix) ? this.hix : r.hix,
                                (r.hiy < this.hiy) ? this.hiy : r.hiy);
        ret.filterSpans(this, r, INCLUDE_A | INCLUDE_B | INCLUDE_COMMON);
        return ret;
    }

    /**
     * Returns a Region object that represents the difference of the
     * specified Region object subtracted from this object.
     * <p>
     * If {@code A} and {@code B} are both Region Objects and
     * {@code C = A.getDifference(B);} then a point will
     * be contained in {@code C} iff it is contained in
     * {@code A} but not contained in {@code B}.
     * <p>
     * The return value may be this same object or the argument
     * Region object if no clipping occurs.
     */
    public Region getDifference(Region r) {
        if (!r.intersectsQuickCheck(this)) {
            return this;
        }
        if (this.isInsideQuickCheck(r)) {
            return EMPTY_REGION;
        }
        Region ret = new Region(this.lox, this.loy, this.hix, this.hiy);
        ret.filterSpans(this, r, INCLUDE_A);
        return ret;
    }

    /**
     * Returns a Region object that represents the exclusive or of this
     * object with the specified Region object.
     * <p>
     * If {@code A} and {@code B} are both Region Objects and
     * {@code C = A.getExclusiveOr(B);} then a point will
     * be contained in {@code C} iff it is contained in either
     * {@code A} or {@code B}, but not if it is contained in both.
     * <p>
     * The return value may be this same object or the argument
     * Region object if either is empty.
     */
    public Region getExclusiveOr(Region r) {
        if (r.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return r;
        }
        Region ret = new Region((r.lox > this.lox) ? this.lox : r.lox,
                                (r.loy > this.loy) ? this.loy : r.loy,
                                (r.hix < this.hix) ? this.hix : r.hix,
                                (r.hiy < this.hiy) ? this.hiy : r.hiy);
        ret.filterSpans(this, r, INCLUDE_A | INCLUDE_B);
        return ret;
    }

    private static final int INCLUDE_A      = 1;
    private static final int INCLUDE_B      = 2;
    private static final int INCLUDE_COMMON = 4;

    private void filterSpans(Region ra, Region rb, int flags) {
        int[] abands = ra.bands;
        int[] bbands = rb.bands;
        if (abands == null) {
            abands = new int[] {ra.loy, ra.hiy, 1, ra.lox, ra.hix};
        }
        if (bbands == null) {
            bbands = new int[] {rb.loy, rb.hiy, 1, rb.lox, rb.hix};
        }
        int[] box = new int[6];
        int acolstart = 0;
        int ay1 = abands[acolstart++];
        int ay2 = abands[acolstart++];
        int acolend = abands[acolstart++];
        acolend = acolstart + 2 * acolend;
        int bcolstart = 0;
        int by1 = bbands[bcolstart++];
        int by2 = bbands[bcolstart++];
        int bcolend = bbands[bcolstart++];
        bcolend = bcolstart + 2 * bcolend;
        int y = loy;
        while (y < hiy) {
            if (y >= ay2) {
                if (acolend < ra.endIndex) {
                    acolstart = acolend;
                    ay1 = abands[acolstart++];
                    ay2 = abands[acolstart++];
                    acolend = abands[acolstart++];
                    acolend = acolstart + 2 * acolend;
                } else {
                    if ((flags & INCLUDE_B) == 0) break;
                    ay1 = ay2 = hiy;
                }
                continue;
            }
            if (y >= by2) {
                if (bcolend < rb.endIndex) {
                    bcolstart = bcolend;
                    by1 = bbands[bcolstart++];
                    by2 = bbands[bcolstart++];
                    bcolend = bbands[bcolstart++];
                    bcolend = bcolstart + 2 * bcolend;
                } else {
                    if ((flags & INCLUDE_A) == 0) break;
                    by1 = by2 = hiy;
                }
                continue;
            }
            int yend;
            if (y < by1) {
                if (y < ay1) {
                    y = Math.min(ay1, by1);
                    continue;
                }
                // We are in a set of rows that belong only to A
                yend = Math.min(ay2, by1);
                if ((flags & INCLUDE_A) != 0) {
                    box[1] = y;
                    box[3] = yend;
                    int acol = acolstart;
                    while (acol < acolend) {
                        box[0] = abands[acol++];
                        box[2] = abands[acol++];
                        appendSpan(box);
                    }
                }
            } else if (y < ay1) {
                // We are in a set of rows that belong only to B
                yend = Math.min(by2, ay1);
                if ((flags & INCLUDE_B) != 0) {
                    box[1] = y;
                    box[3] = yend;
                    int bcol = bcolstart;
                    while (bcol < bcolend) {
                        box[0] = bbands[bcol++];
                        box[2] = bbands[bcol++];
                        appendSpan(box);
                    }
                }
            } else {
                // We are in a set of rows that belong to both A and B
                yend = Math.min(ay2, by2);
                box[1] = y;
                box[3] = yend;
                int acol = acolstart;
                int bcol = bcolstart;
                int ax1 = abands[acol++];
                int ax2 = abands[acol++];
                int bx1 = bbands[bcol++];
                int bx2 = bbands[bcol++];
                int x = Math.min(ax1, bx1);
                if (x < lox) x = lox;
                while (x < hix) {
                    if (x >= ax2) {
                        if (acol < acolend) {
                            ax1 = abands[acol++];
                            ax2 = abands[acol++];
                        } else {
                            if ((flags & INCLUDE_B) == 0) break;
                            ax1 = ax2 = hix;
                        }
                        continue;
                    }
                    if (x >= bx2) {
                        if (bcol < bcolend) {
                            bx1 = bbands[bcol++];
                            bx2 = bbands[bcol++];
                        } else {
                            if ((flags & INCLUDE_A) == 0) break;
                            bx1 = bx2 = hix;
                        }
                        continue;
                    }
                    int xend;
                    boolean appendit;
                    if (x < bx1) {
                        if (x < ax1) {
                            xend = Math.min(ax1, bx1);
                            appendit = false;
                        } else {
                            xend = Math.min(ax2, bx1);
                            appendit = ((flags & INCLUDE_A) != 0);
                        }
                    } else if (x < ax1) {
                        xend = Math.min(ax1, bx2);
                        appendit = ((flags & INCLUDE_B) != 0);
                    } else {
                        xend = Math.min(ax2, bx2);
                        appendit = ((flags & INCLUDE_COMMON) != 0);
                    }
                    if (appendit) {
                        box[0] = x;
                        box[2] = xend;
                        appendSpan(box);
                    }
                    x = xend;
                }
            }
            y = yend;
        }
        endRow(box);
        calcBBox();
    }

    /**
     * Returns a Region object that represents the bounds of the
     * intersection of this object with the bounds of the specified
     * Region object.
     * <p>
     * The return value may be this same object if no clipping occurs
     * and this Region is rectangular.
     */
    public Region getBoundsIntersection(Rectangle r) {
        return getBoundsIntersectionXYWH(r.x, r.y, r.width, r.height);
    }

    /**
     * Returns a Region object that represents the bounds of the
     * intersection of this object with the bounds of the specified
     * rectangular area in x, y, width, height format.
     * <p>
     * The return value may be this same object if no clipping occurs
     * and this Region is rectangular.
     */
    public Region getBoundsIntersectionXYWH(int x, int y, int w, int h) {
        return getBoundsIntersectionXYXY(x, y, dimAdd(x, w), dimAdd(y, h));
    }

    /**
     * Returns a Region object that represents the bounds of the
     * intersection of this object with the bounds of the specified
     * rectangular area in lox, loy, hix, hiy format.
     * <p>
     * The return value may be this same object if no clipping occurs
     * and this Region is rectangular.
     */
    public Region getBoundsIntersectionXYXY(int lox, int loy,
                                            int hix, int hiy)
    {
        if (this.bands == null &&
            this.lox >= lox && this.loy >= loy &&
            this.hix <= hix && this.hiy <= hiy)
        {
            return this;
        }
        return new Region((lox < this.lox) ? this.lox : lox,
                          (loy < this.loy) ? this.loy : loy,
                          (hix > this.hix) ? this.hix : hix,
                          (hiy > this.hiy) ? this.hiy : hiy);
    }

    /**
     * Returns a Region object that represents the intersection of
     * this object with the bounds of the specified Region object.
     * <p>
     * The return value may be this same object or the argument
     * Region object if no clipping occurs and the Regions are
     * rectangular.
     */
    public Region getBoundsIntersection(Region r) {
        if (this.encompasses(r)) {
            return r;
        }
        if (r.encompasses(this)) {
            return this;
        }
        return new Region((r.lox < this.lox) ? this.lox : r.lox,
                          (r.loy < this.loy) ? this.loy : r.loy,
                          (r.hix > this.hix) ? this.hix : r.hix,
                          (r.hiy > this.hiy) ? this.hiy : r.hiy);
    }

    /**
     * Appends a single span defined by the 4 parameters
     * spanlox, spanloy, spanhix, spanhiy.
     * This span must be at a higher starting Y coordinate than
     * the previous data or it must have a Y range equal to the
     * highest Y band in the region and a higher X coordinate
     * than any of the spans in that band.
     */
    private void appendSpan(int[] box) {
        int spanlox, spanloy, spanhix, spanhiy;
        if ((spanlox = box[0]) < lox) spanlox = lox;
        if ((spanloy = box[1]) < loy) spanloy = loy;
        if ((spanhix = box[2]) > hix) spanhix = hix;
        if ((spanhiy = box[3]) > hiy) spanhiy = hiy;
        if (spanhix <= spanlox || spanhiy <= spanloy) {
            return;
        }

        int curYrow = box[4];
        if (endIndex == 0 || spanloy >= bands[curYrow + 1]) {
            if (bands == null) {
                bands = new int[INIT_SIZE];
            } else {
                needSpace(5);
                endRow(box);
                curYrow = box[4];
            }
            bands[endIndex++] = spanloy;
            bands[endIndex++] = spanhiy;
            bands[endIndex++] = 0;
        } else if (spanloy == bands[curYrow] &&
                   spanhiy == bands[curYrow + 1] &&
                   spanlox >= bands[endIndex - 1]) {
            if (spanlox == bands[endIndex - 1]) {
                bands[endIndex - 1] = spanhix;
                return;
            }
            needSpace(2);
        } else {
            throw new InternalError("bad span");
        }
        bands[endIndex++] = spanlox;
        bands[endIndex++] = spanhix;
        bands[curYrow + 2]++;
    }

    private void needSpace(int num) {
        if (endIndex + num >= bands.length) {
            int[] newbands = new int[bands.length + GROW_SIZE];
            System.arraycopy(bands, 0, newbands, 0, endIndex);
            bands = newbands;
        }
    }

    private void endRow(int[] box) {
        int cur = box[4];
        int prev = box[5];
        if (cur > prev) {
            int[] bands = this.bands;
            if (bands[prev + 1] == bands[cur] &&
                bands[prev + 2] == bands[cur + 2])
            {
                int num = bands[cur + 2] * 2;
                cur += 3;
                prev += 3;
                while (num > 0) {
                    if (bands[cur++] != bands[prev++]) {
                        break;
                    }
                    num--;
                }
                if (num == 0) {
                    // prev == box[4]
                    bands[box[5] + 1] = bands[prev + 1];
                    endIndex = prev;
                    return;
                }
            }
        }
        box[5] = box[4];
        box[4] = endIndex;
    }

    private void calcBBox() {
        int[] bands = this.bands;
        if (endIndex <= 5) {
            if (endIndex == 0) {
                lox = loy = hix = hiy = 0;
            } else {
                loy = bands[0];
                hiy = bands[1];
                lox = bands[3];
                hix = bands[4];
                endIndex = 0;
            }
            this.bands = null;
            return;
        }
        int lox = this.hix;
        int hix = this.lox;
        int hiyindex = 0;

        int i = 0;
        while (i < endIndex) {
            hiyindex = i;
            int numbands = bands[i + 2];
            i += 3;
            if (lox > bands[i]) {
                lox = bands[i];
            }
            i += numbands * 2;
            if (hix < bands[i - 1]) {
                hix = bands[i - 1];
            }
        }

        this.lox = lox;
        this.loy = bands[0];
        this.hix = hix;
        this.hiy = bands[hiyindex + 1];
    }

    /**
     * Returns the lowest X coordinate in the Region.
     */
    public int getLoX() {
        return lox;
    }

    /**
     * Returns the lowest Y coordinate in the Region.
     */
    public int getLoY() {
        return loy;
    }

    /**
     * Returns the highest X coordinate in the Region.
     */
    public int getHiX() {
        return hix;
    }

    /**
     * Returns the highest Y coordinate in the Region.
     */
    public int getHiY() {
        return hiy;
    }

    /**
     * Returns the width of this Region clipped to the range (0 - MAX_INT).
     */
    public int getWidth() {
        if (hix < lox) return 0;
        int w;
        if ((w = hix - lox) < 0) {
            w = Integer.MAX_VALUE;
        }
        return w;
    }

    /**
     * Returns the height of this Region clipped to the range (0 - MAX_INT).
     */
    public int getHeight() {
        if (hiy < loy) return 0;
        int h;
        if ((h = hiy - loy) < 0) {
            h = Integer.MAX_VALUE;
        }
        return h;
    }

    /**
     * Returns true iff this Region encloses no area.
     */
    public boolean isEmpty() {
        return (hix <= lox || hiy <= loy);
    }

    /**
     * Returns true iff this Region represents a single simple
     * rectangular area.
     */
    public boolean isRectangular() {
        return (bands == null);
    }

    /**
     * Returns true iff this Region contains the specified coordinate.
     */
    public boolean contains(int x, int y) {
        if (x < lox || x >= hix || y < loy || y >= hiy) return false;
        if (bands == null) return true;
        int i = 0;
        while (i < endIndex) {
            if (y < bands[i++]) {
                return false;
            }
            if (y >= bands[i++]) {
                int numspans = bands[i++];
                i += numspans * 2;
            } else {
                int end = bands[i++];
                end = i + end * 2;
                while (i < end) {
                    if (x < bands[i++]) return false;
                    if (x < bands[i++]) return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Returns true iff this Region lies inside the indicated
     * rectangular area specified in x, y, width, height format
     * with appropriate clipping performed as per the dimAdd method.
     */
    public boolean isInsideXYWH(int x, int y, int w, int h) {
        return isInsideXYXY(x, y, dimAdd(x, w), dimAdd(y, h));
    }

    /**
     * Returns true iff this Region lies inside the indicated
     * rectangular area specified in lox, loy, hix, hiy format.
     */
    public boolean isInsideXYXY(int lox, int loy, int hix, int hiy) {
        return (this.lox >= lox && this.loy >= loy &&
                this.hix <= hix && this.hiy <= hiy);

    }

    /**
     * Quickly checks if this Region lies inside the specified
     * Region object.
     * <p>
     * This method will return false if the specified Region
     * object is not a simple rectangle.
     */
    public boolean isInsideQuickCheck(Region r) {
        return (r.bands == null &&
                r.lox <= this.lox && r.loy <= this.loy &&
                r.hix >= this.hix && r.hiy >= this.hiy);
    }

    /**
     * Quickly checks if this Region intersects the specified
     * rectangular area specified in lox, loy, hix, hiy format.
     * <p>
     * This method tests only against the bounds of this region
     * and does not bother to test if the rectangular region
     * actually intersects any bands.
     */
    public boolean intersectsQuickCheckXYXY(int lox, int loy,
                                            int hix, int hiy)
    {
        return (hix > this.lox && lox < this.hix &&
                hiy > this.loy && loy < this.hiy);
    }

    /**
     * Quickly checks if this Region intersects the specified
     * Region object.
     * <p>
     * This method tests only against the bounds of this region
     * and does not bother to test if the rectangular region
     * actually intersects any bands.
     */
    public boolean intersectsQuickCheck(Region r) {
        return (r.hix > this.lox && r.lox < this.hix &&
                r.hiy > this.loy && r.loy < this.hiy);
    }

    /**
     * Quickly checks if this Region surrounds the specified
     * Region object.
     * <p>
     * This method will return false if this Region object is
     * not a simple rectangle.
     */
    public boolean encompasses(Region r) {
        return (this.bands == null &&
                this.lox <= r.lox && this.loy <= r.loy &&
                this.hix >= r.hix && this.hiy >= r.hiy);
    }

    /**
     * Quickly checks if this Region surrounds the specified
     * rectangular area specified in x, y, width, height format.
     * <p>
     * This method will return false if this Region object is
     * not a simple rectangle.
     */
    public boolean encompassesXYWH(int x, int y, int w, int h) {
        return encompassesXYXY(x, y, dimAdd(x, w), dimAdd(y, h));
    }

    /**
     * Quickly checks if this Region surrounds the specified
     * rectangular area specified in lox, loy, hix, hiy format.
     * <p>
     * This method will return false if this Region object is
     * not a simple rectangle.
     */
    public boolean encompassesXYXY(int lox, int loy, int hix, int hiy) {
        return (this.bands == null &&
                this.lox <= lox && this.loy <= loy &&
                this.hix >= hix && this.hiy >= hiy);
    }

    /**
     * Gets the bbox of the available spans, clipped to the OutputArea.
     */
    public void getBounds(int[] pathbox) {
        pathbox[0] = lox;
        pathbox[1] = loy;
        pathbox[2] = hix;
        pathbox[3] = hiy;
    }

    /**
     * Clips the indicated bbox array to the bounds of this Region.
     */
    public void clipBoxToBounds(int[] bbox) {
        if (bbox[0] < lox) bbox[0] = lox;
        if (bbox[1] < loy) bbox[1] = loy;
        if (bbox[2] > hix) bbox[2] = hix;
        if (bbox[3] > hiy) bbox[3] = hiy;
    }

    /**
     * Gets an iterator object to iterate over the spans in this region.
     */
    public RegionIterator getIterator() {
        return new RegionIterator(this);
    }

    /**
     * Gets a span iterator object that iterates over the spans in this region
     */
    public SpanIterator getSpanIterator() {
        return new RegionSpanIterator(this);
    }

    /**
     * Gets a span iterator object that iterates over the spans in this region
     * but clipped to the bounds given in the argument (xlo, ylo, xhi, yhi).
     */
    public SpanIterator getSpanIterator(int[] bbox) {
        SpanIterator result = getSpanIterator();
        result.intersectClipBox(bbox[0], bbox[1], bbox[2], bbox[3]);
        return result;
    }

    /**
     * Returns a SpanIterator that is the argument iterator filtered by
     * this region.
     */
    public SpanIterator filter(SpanIterator si) {
        if (bands == null) {
            si.intersectClipBox(lox, loy, hix, hiy);
        } else {
            si = new RegionClipSpanIterator(this, si);
        }
        return si;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Region[[");
        sb.append(lox);
        sb.append(", ");
        sb.append(loy);
        sb.append(" => ");
        sb.append(hix);
        sb.append(", ");
        sb.append(hiy);
        sb.append(']');
        if (bands != null) {
            int col = 0;
            while (col < endIndex) {
                sb.append("y{");
                sb.append(bands[col++]);
                sb.append(',');
                sb.append(bands[col++]);
                sb.append("}[");
                int end = bands[col++];
                end = col + end * 2;
                while (col < end) {
                    sb.append("x(");
                    sb.append(bands[col++]);
                    sb.append(", ");
                    sb.append(bands[col++]);
                    sb.append(')');
                }
                sb.append(']');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return (isEmpty() ? 0 : (lox * 3 + loy * 5 + hix * 7 + hiy * 9));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Region)) {
            return false;
        }
        Region r = (Region) o;
        if (this.isEmpty()) {
            return r.isEmpty();
        } else if (r.isEmpty()) {
            return false;
        }
        if (r.lox != this.lox || r.loy != this.loy ||
            r.hix != this.hix || r.hiy != this.hiy)
        {
            return false;
        }
        if (this.bands == null) {
            return (r.bands == null);
        } else if (r.bands == null) {
            return false;
        }
        if (this.endIndex != r.endIndex) {
            return false;
        }
        int[] abands = this.bands;
        int[] bbands = r.bands;
        for (int i = 0; i < endIndex; i++) {
            if (abands[i] != bbands[i]) {
                return false;
            }
        }
        return true;
    }
}
