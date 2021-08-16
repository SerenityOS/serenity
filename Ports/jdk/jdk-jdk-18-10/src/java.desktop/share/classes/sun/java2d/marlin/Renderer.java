/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin;

import static sun.java2d.marlin.OffHeapArray.SIZE_INT;
import jdk.internal.misc.Unsafe;

final class Renderer implements DPathConsumer2D, MarlinConst {

    static final boolean DISABLE_RENDER = false;

    static final boolean ENABLE_BLOCK_FLAGS = MarlinProperties.isUseTileFlags();
    static final boolean ENABLE_BLOCK_FLAGS_HEURISTICS = MarlinProperties.isUseTileFlagsWithHeuristics();

    private static final int ALL_BUT_LSB = 0xFFFFFFFE;
    private static final int ERR_STEP_MAX = 0x7FFFFFFF; // = 2^31 - 1

    private static final double POWER_2_TO_32 = 0x1.0p32d;

    // use double to make tosubpix methods faster (no int to double conversion)
    static final double SUBPIXEL_SCALE_X = SUBPIXEL_POSITIONS_X;
    static final double SUBPIXEL_SCALE_Y = SUBPIXEL_POSITIONS_Y;
    static final int SUBPIXEL_MASK_X = SUBPIXEL_POSITIONS_X - 1;
    static final int SUBPIXEL_MASK_Y = SUBPIXEL_POSITIONS_Y - 1;

    static final double RDR_OFFSET_X = 0.5d / SUBPIXEL_SCALE_X;
    static final double RDR_OFFSET_Y = 0.5d / SUBPIXEL_SCALE_Y;

    // number of subpixels corresponding to a tile line
    private static final int SUBPIXEL_TILE
        = TILE_H << SUBPIXEL_LG_POSITIONS_Y;

    // 2176 pixels (height) x 8 subpixels = 68K
    static final int INITIAL_BUCKET_ARRAY
        = INITIAL_PIXEL_HEIGHT * SUBPIXEL_POSITIONS_Y;

    // crossing capacity = edges count / 4 ~ 1024
    static final int INITIAL_CROSSING_COUNT = INITIAL_EDGES_COUNT >> 2;

    // common to all types of input path segments.
    // OFFSET as bytes
    // only integer values:
    public static final long OFF_CURX_OR  = 0;
    public static final long OFF_ERROR    = OFF_CURX_OR  + SIZE_INT;
    public static final long OFF_BUMP_X   = OFF_ERROR    + SIZE_INT;
    public static final long OFF_BUMP_ERR = OFF_BUMP_X   + SIZE_INT;
    public static final long OFF_NEXT     = OFF_BUMP_ERR + SIZE_INT;
    public static final long OFF_YMAX     = OFF_NEXT     + SIZE_INT;

    // size of one edge in bytes
    public static final int SIZEOF_EDGE_BYTES = (int)(OFF_YMAX + SIZE_INT);

    // curve break into lines
    // cubic error in subpixels to decrement step
    private static final double CUB_DEC_ERR_SUBPIX
        = MarlinProperties.getCubicDecD2() * (SUBPIXEL_POSITIONS_X / 8.0d); // 1.0 / 8th pixel
    // cubic error in subpixels to increment step
    private static final double CUB_INC_ERR_SUBPIX
        = MarlinProperties.getCubicIncD1() * (SUBPIXEL_POSITIONS_X / 8.0d); // 0.4 / 8th pixel
    // scale factor for Y-axis contribution to quad / cubic errors:
    public static final double SCALE_DY = ((double) SUBPIXEL_POSITIONS_X) / SUBPIXEL_POSITIONS_Y;

    // TestNonAARasterization (JDK-8170879): cubics
    // bad paths (59294/100000 == 59,29%, 94335 bad pixels (avg = 1,59), 3966 warnings (avg = 0,07)
// 2018
    // 1.0 / 0.2: bad paths (67194/100000 == 67,19%, 117394 bad pixels (avg = 1,75 - max =  9), 4042 warnings (avg = 0,06)

    // cubic bind length to decrement step
    public static final double CUB_DEC_BND
        = 8.0d * CUB_DEC_ERR_SUBPIX;
    // cubic bind length to increment step
    public static final double CUB_INC_BND
        = 8.0d * CUB_INC_ERR_SUBPIX;

    // cubic countlg
    public static final int CUB_COUNT_LG = 2;
    // cubic count = 2^countlg
    private static final int CUB_COUNT = 1 << CUB_COUNT_LG;
    // cubic count^2 = 4^countlg
    private static final int CUB_COUNT_2 = 1 << (2 * CUB_COUNT_LG);
    // cubic count^3 = 8^countlg
    private static final int CUB_COUNT_3 = 1 << (3 * CUB_COUNT_LG);
    // cubic dt = 1 / count
    private static final double CUB_INV_COUNT = 1.0d / CUB_COUNT;
    // cubic dt^2 = 1 / count^2 = 1 / 4^countlg
    private static final double CUB_INV_COUNT_2 = 1.0d / CUB_COUNT_2;
    // cubic dt^3 = 1 / count^3 = 1 / 8^countlg
    private static final double CUB_INV_COUNT_3 = 1.0d / CUB_COUNT_3;

    // quad break into lines
    // quadratic error in subpixels
    private static final double QUAD_DEC_ERR_SUBPIX
        = MarlinProperties.getQuadDecD2() * (SUBPIXEL_POSITIONS_X / 8.0d); // 0.5 / 8th pixel

    // TestNonAARasterization (JDK-8170879): quads
    // bad paths (62916/100000 == 62,92%, 103818 bad pixels (avg = 1,65), 6514 warnings (avg = 0,10)
// 2018
    // 0.50px  = bad paths (62915/100000 == 62,92%, 103810 bad pixels (avg = 1,65), 6512 warnings (avg = 0,10)

    // quadratic bind length to decrement step
    public static final double QUAD_DEC_BND
        = 8.0d * QUAD_DEC_ERR_SUBPIX;

//////////////////////////////////////////////////////////////////////////////
//  SCAN LINE
//////////////////////////////////////////////////////////////////////////////
    // crossings ie subpixel edge x coordinates
    private int[] crossings;
    // auxiliary storage for crossings (merge sort)
    private int[] aux_crossings;

    // indices into the segment pointer lists. They indicate the "active"
    // sublist in the segment lists (the portion of the list that contains
    // all the segments that cross the next scan line).
    private int edgeCount;
    private int[] edgePtrs;
    // auxiliary storage for edge pointers (merge sort)
    private int[] aux_edgePtrs;

    // max used for both edgePtrs and crossings (stats only)
    private int activeEdgeMaxUsed;

    // crossings ref (dirty)
    private final IntArrayCache.Reference crossings_ref;
    // edgePtrs ref (dirty)
    private final IntArrayCache.Reference edgePtrs_ref;
    // merge sort initial arrays (large enough to satisfy most usages) (1024)
    // aux_crossings ref (dirty)
    private final IntArrayCache.Reference aux_crossings_ref;
    // aux_edgePtrs ref (dirty)
    private final IntArrayCache.Reference aux_edgePtrs_ref;

//////////////////////////////////////////////////////////////////////////////
//  EDGE LIST
//////////////////////////////////////////////////////////////////////////////
    private int edgeMinY = Integer.MAX_VALUE;
    private int edgeMaxY = Integer.MIN_VALUE;
    private double edgeMinX = Double.POSITIVE_INFINITY;
    private double edgeMaxX = Double.NEGATIVE_INFINITY;

    // edges [ints] stored in off-heap memory
    private final OffHeapArray edges;

    private int[] edgeBuckets;
    private int[] edgeBucketCounts; // 2*newedges + (1 if pruning needed)
    // used range for edgeBuckets / edgeBucketCounts
    private int buckets_minY;
    private int buckets_maxY;

    // edgeBuckets ref (clean)
    private final IntArrayCache.Reference edgeBuckets_ref;
    // edgeBucketCounts ref (clean)
    private final IntArrayCache.Reference edgeBucketCounts_ref;

    // Flattens using adaptive forward differencing. This only carries out
    // one iteration of the AFD loop. All it does is update AFD variables (i.e.
    // X0, Y0, D*[X|Y], COUNT; not variables used for computing scanline crossings).
    private void quadBreakIntoLinesAndAdd(double x0, double y0,
                                          final Curve c,
                                          final double x2, final double y2)
    {
        int count = 1; // dt = 1 / count

        // maximum(ddX|Y) = norm(dbx, dby) * dt^2 (= 1)
        double maxDD = Math.abs(c.dbx) + Math.abs(c.dby) * SCALE_DY;

        final double _DEC_BND = QUAD_DEC_BND;

        while (maxDD >= _DEC_BND) {
            // divide step by half:
            maxDD /= 4.0d; // error divided by 2^2 = 4

            count <<= 1;
            if (DO_STATS) {
                rdrCtx.stats.stat_rdr_quadBreak_dec.add(count);
            }
        }

        final int nL = count; // line count

        if (count > 1) {
            final double icount = 1.0d / count; // dt
            final double icount2 = icount * icount; // dt^2

            final double ddx = c.dbx * icount2;
            final double ddy = c.dby * icount2;
            double dx = c.bx * icount2 + c.cx * icount;
            double dy = c.by * icount2 + c.cy * icount;

            // we use x0, y0 to walk the line
            for (double x1 = x0, y1 = y0; --count > 0; dx += ddx, dy += ddy) {
                x1 += dx;
                y1 += dy;

                addLine(x0, y0, x1, y1);
                x0 = x1;
                y0 = y1;
            }
        }
        addLine(x0, y0, x2, y2);

        if (DO_STATS) {
            rdrCtx.stats.stat_rdr_quadBreak.add(nL);
        }
    }

    // x0, y0 and x3,y3 are the endpoints of the curve. We could compute these
    // using c.xat(0),c.yat(0) and c.xat(1),c.yat(1), but this might introduce
    // numerical errors, and our callers already have the exact values.
    // Another alternative would be to pass all the control points, and call
    // c.set here, but then too many numbers are passed around.
    private void curveBreakIntoLinesAndAdd(double x0, double y0,
                                           final Curve c,
                                           final double x3, final double y3)
    {
        int count            = CUB_COUNT;
        final double icount  = CUB_INV_COUNT;   // dt
        final double icount2 = CUB_INV_COUNT_2; // dt^2
        final double icount3 = CUB_INV_COUNT_3; // dt^3

        // the dx and dy refer to forward differencing variables, not the last
        // coefficients of the "points" polynomial
        double dddx, dddy, ddx, ddy, dx, dy;
        dddx = 2.0d * c.dax * icount3;
        dddy = 2.0d * c.day * icount3;
        ddx = dddx + c.dbx * icount2;
        ddy = dddy + c.dby * icount2;
        dx = c.ax * icount3 + c.bx * icount2 + c.cx * icount;
        dy = c.ay * icount3 + c.by * icount2 + c.cy * icount;

        int nL = 0; // line count

        final double _DEC_BND = CUB_DEC_BND;
        final double _INC_BND = CUB_INC_BND;
        final double _SCALE_DY = SCALE_DY;

        // we use x0, y0 to walk the line
        for (double x1 = x0, y1 = y0; count > 0; ) {
            // inc / dec => ratio ~ 5 to minimize upscale / downscale but minimize edges

            // double step:
            // can only do this on even "count" values, because we must divide count by 2
            while ((count % 2 == 0)
                    && ((Math.abs(ddx) + Math.abs(ddy) * _SCALE_DY) <= _INC_BND)) {
                dx = 2.0d * dx + ddx;
                dy = 2.0d * dy + ddy;
                ddx = 4.0d * (ddx + dddx);
                ddy = 4.0d * (ddy + dddy);
                dddx *= 8.0d;
                dddy *= 8.0d;

                count >>= 1;
                if (DO_STATS) {
                    rdrCtx.stats.stat_rdr_curveBreak_inc.add(count);
                }
            }

            // divide step by half:
            while ((Math.abs(ddx) + Math.abs(ddy) * _SCALE_DY) >= _DEC_BND) {
                dddx /= 8.0d;
                dddy /= 8.0d;
                ddx = ddx / 4.0d - dddx;
                ddy = ddy / 4.0d - dddy;
                dx = (dx - ddx) / 2.0d;
                dy = (dy - ddy) / 2.0d;

                count <<= 1;
                if (DO_STATS) {
                    rdrCtx.stats.stat_rdr_curveBreak_dec.add(count);
                }
            }
            if (--count == 0) {
                break;
            }

            x1 += dx;
            y1 += dy;
            dx += ddx;
            dy += ddy;
            ddx += dddx;
            ddy += dddy;

            addLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
        addLine(x0, y0, x3, y3);

        if (DO_STATS) {
            rdrCtx.stats.stat_rdr_curveBreak.add(nL + 1);
        }
    }

    private void addLine(double x1, double y1, double x2, double y2) {
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_addLine.start();
        }
        if (DO_STATS) {
            rdrCtx.stats.stat_rdr_addLine.add(1);
        }
        int or = 1; // orientation of the line. 1 if y increases, 0 otherwise.
        if (y2 < y1) {
            or = 0;
            double tmp = y2;
            y2 = y1;
            y1 = tmp;
            tmp = x2;
            x2 = x1;
            x1 = tmp;
        }

        // convert subpixel coordinates [double] into pixel positions [int]

        // The index of the pixel that holds the next HPC is at ceil(trueY - 0.5)
        // Since y1 and y2 are biased by -0.5 in tosubpixy(), this is simply
        // ceil(y1) or ceil(y2)
        // upper integer (inclusive)
        final int firstCrossing = FloatMath.max(FloatMath.ceil_int(y1), boundsMinY);

        // note: use boundsMaxY (last Y exclusive) to compute correct coverage
        // upper integer (exclusive)
        final int lastCrossing  = FloatMath.min(FloatMath.ceil_int(y2), boundsMaxY);

        /* skip horizontal lines in pixel space and clip edges
           out of y range [boundsMinY; boundsMaxY] */
        if (firstCrossing >= lastCrossing) {
            if (DO_MONITORS) {
                rdrCtx.stats.mon_rdr_addLine.stop();
            }
            if (DO_STATS) {
                rdrCtx.stats.stat_rdr_addLine_skip.add(1);
            }
            return;
        }

        // edge min/max X/Y are in subpixel space (half-open interval):
        // note: Use integer crossings to ensure consistent range within
        // edgeBuckets / edgeBucketCounts arrays in case of NaN values (int = 0)
        if (firstCrossing < edgeMinY) {
            edgeMinY = firstCrossing;
        }
        if (lastCrossing > edgeMaxY) {
            edgeMaxY = lastCrossing;
        }

        final double slope = (x1 - x2) / (y1 - y2);

        if (slope >= 0.0d) { // <==> x1 < x2
            if (x1 < edgeMinX) {
                edgeMinX = x1;
            }
            if (x2 > edgeMaxX) {
                edgeMaxX = x2;
            }
        } else {
            if (x2 < edgeMinX) {
                edgeMinX = x2;
            }
            if (x1 > edgeMaxX) {
                edgeMaxX = x1;
            }
        }

        // local variables for performance:
        final int _SIZEOF_EDGE_BYTES = SIZEOF_EDGE_BYTES;

        final OffHeapArray _edges = edges;

        // get free pointer (ie length in bytes)
        final int edgePtr = _edges.used;

        // use substraction to avoid integer overflow:
        if (_edges.length - edgePtr < _SIZEOF_EDGE_BYTES) {
            // suppose _edges.length > _SIZEOF_EDGE_BYTES
            // so doubling size is enough to add needed bytes
            // note: throw IOOB if neededSize > 2Gb:
            final long edgeNewSize = ArrayCacheConst.getNewLargeSize(
                                        _edges.length,
                                        edgePtr + _SIZEOF_EDGE_BYTES);

            if (DO_STATS) {
                rdrCtx.stats.stat_rdr_edges_resizes.add(edgeNewSize);
            }
            _edges.resize(edgeNewSize);
        }


        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_INT = 4L;
        long addr   = _edges.address + edgePtr;

        // The x value must be bumped up to its position at the next HPC we will evaluate.
        // "firstcrossing" is the (sub)pixel number where the next crossing occurs
        // thus, the actual coordinate of the next HPC is "firstcrossing + 0.5"
        // so the Y distance we cover is "firstcrossing + 0.5 - trueY".
        // Note that since y1 (and y2) are already biased by -0.5 in tosubpixy(), we have
        // y1 = trueY - 0.5
        // trueY = y1 + 0.5
        // firstcrossing + 0.5 - trueY = firstcrossing + 0.5 - (y1 + 0.5)
        //                             = firstcrossing - y1
        // The x coordinate at that HPC is then:
        // x1_intercept = x1 + (firstcrossing - y1) * slope
        // The next VPC is then given by:
        // VPC index = ceil(x1_intercept - 0.5), or alternately
        // VPC index = floor(x1_intercept - 0.5 + 1 - epsilon)
        // epsilon is hard to pin down in floating point, but easy in fixed point, so if
        // we convert to fixed point then these operations get easier:
        // long x1_fixed = x1_intercept * 2^32;  (fixed point 32.32 format)
        // curx = next VPC = fixed_floor(x1_fixed - 2^31 + 2^32 - 1)
        //                 = fixed_floor(x1_fixed + 2^31 - 1)
        //                 = fixed_floor(x1_fixed + 0x7FFFFFFF)
        // and error       = fixed_fract(x1_fixed + 0x7FFFFFFF)
        final double x1_intercept = x1 + (firstCrossing - y1) * slope;

        // inlined scalb(x1_intercept, 32):
        final long x1_fixed_biased = ((long) (POWER_2_TO_32 * x1_intercept))
                                     + 0x7FFFFFFFL;
        // curx:
        // last bit corresponds to the orientation
        _unsafe.putInt(addr, (((int) (x1_fixed_biased >> 31L)) & ALL_BUT_LSB) | or);
        addr += SIZE_INT;
        _unsafe.putInt(addr,  ((int)  x1_fixed_biased) >>> 1);
        addr += SIZE_INT;

        // inlined scalb(slope, 32):
        final long slope_fixed = (long) (POWER_2_TO_32 * slope);

        // last bit set to 0 to keep orientation:
        _unsafe.putInt(addr, (((int) (slope_fixed >> 31L)) & ALL_BUT_LSB));
        addr += SIZE_INT;
        _unsafe.putInt(addr,  ((int)  slope_fixed) >>> 1);
        addr += SIZE_INT;

        final int[] _edgeBuckets      = edgeBuckets;
        final int[] _edgeBucketCounts = edgeBucketCounts;

        final int _boundsMinY = boundsMinY;

        // each bucket is a linked list. this method adds ptr to the
        // start of the "bucket"th linked list.
        final int bucketIdx = firstCrossing - _boundsMinY;

        // pointer from bucket
        _unsafe.putInt(addr, _edgeBuckets[bucketIdx]);
        addr += SIZE_INT;
        // y max (exclusive)
        _unsafe.putInt(addr,  lastCrossing);

        // Update buckets:
        // directly the edge struct "pointer"
        _edgeBuckets[bucketIdx]       = edgePtr;
        _edgeBucketCounts[bucketIdx] += 2; // 1 << 1
        // last bit means edge end
        _edgeBucketCounts[lastCrossing - _boundsMinY] |= 0x1;

        // update free pointer (ie length in bytes)
        _edges.used += _SIZEOF_EDGE_BYTES;

        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_addLine.stop();
        }
    }

// END EDGE LIST
//////////////////////////////////////////////////////////////////////////////

    // Cache to store RLE-encoded coverage mask of the current primitive
    final MarlinCache cache;

    // Bounds of the drawing region, at subpixel precision.
    private int boundsMinX, boundsMinY, boundsMaxX, boundsMaxY;

    // Current winding rule
    private int windingRule;

    // Current drawing position, i.e., final point of last segment
    private double x0, y0;

    // Position of most recent 'moveTo' command
    private double sx0, sy0;

    // per-thread renderer context
    final RendererContext rdrCtx;
    // dirty curve
    private final Curve curve;

    // clean alpha array (zero filled)
    private int[] alphaLine;

    // alphaLine ref (clean)
    private final IntArrayCache.Reference alphaLine_ref;

    private boolean enableBlkFlags = false;
    private boolean prevUseBlkFlags = false;

    /* block flags (0|1) */
    private int[] blkFlags;

    // blkFlags ref (clean)
    private final IntArrayCache.Reference blkFlags_ref;

    Renderer(final RendererContext rdrCtx) {
        this.rdrCtx = rdrCtx;
        this.curve = rdrCtx.curve;
        this.cache = rdrCtx.cache;

        this.edges = rdrCtx.newOffHeapArray(INITIAL_EDGES_CAPACITY); // 96K

        edgeBuckets_ref      = rdrCtx.newCleanIntArrayRef(INITIAL_BUCKET_ARRAY); // 64K
        edgeBucketCounts_ref = rdrCtx.newCleanIntArrayRef(INITIAL_BUCKET_ARRAY); // 64K

        edgeBuckets      = edgeBuckets_ref.initial;
        edgeBucketCounts = edgeBucketCounts_ref.initial;

        // 4096 pixels large
        alphaLine_ref = rdrCtx.newCleanIntArrayRef(INITIAL_AA_ARRAY); // 16K
        alphaLine     = alphaLine_ref.initial;

        crossings_ref     = rdrCtx.newDirtyIntArrayRef(INITIAL_CROSSING_COUNT); // 2K
        aux_crossings_ref = rdrCtx.newDirtyIntArrayRef(INITIAL_CROSSING_COUNT); // 2K
        edgePtrs_ref      = rdrCtx.newDirtyIntArrayRef(INITIAL_CROSSING_COUNT); // 2K
        aux_edgePtrs_ref  = rdrCtx.newDirtyIntArrayRef(INITIAL_CROSSING_COUNT); // 2K

        crossings     = crossings_ref.initial;
        aux_crossings = aux_crossings_ref.initial;
        edgePtrs      = edgePtrs_ref.initial;
        aux_edgePtrs  = aux_edgePtrs_ref.initial;

        blkFlags_ref = rdrCtx.newCleanIntArrayRef(INITIAL_ARRAY); // 1K = 1 tile line
        blkFlags     = blkFlags_ref.initial;
    }

    Renderer init(final int pix_boundsX, final int pix_boundsY,
                  final int pix_boundsWidth, final int pix_boundsHeight,
                  final int windingRule)
    {
        this.windingRule = windingRule;

        // bounds as half-open intervals: minX <= x < maxX and minY <= y < maxY
        this.boundsMinX =  pix_boundsX << SUBPIXEL_LG_POSITIONS_X;
        this.boundsMaxX =
            (pix_boundsX + pix_boundsWidth) << SUBPIXEL_LG_POSITIONS_X;
        this.boundsMinY =  pix_boundsY << SUBPIXEL_LG_POSITIONS_Y;
        this.boundsMaxY =
            (pix_boundsY + pix_boundsHeight) << SUBPIXEL_LG_POSITIONS_Y;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("boundsXY = [" + boundsMinX + " ... "
                                + boundsMaxX + "[ [" + boundsMinY + " ... "
                                + boundsMaxY + "[");
        }

        // see addLine: ceil(boundsMaxY) => boundsMaxY + 1
        // +1 for edgeBucketCounts
        final int edgeBucketsLength = (boundsMaxY - boundsMinY) + 1;

        if (edgeBucketsLength > INITIAL_BUCKET_ARRAY) {
            if (DO_STATS) {
                rdrCtx.stats.stat_array_renderer_edgeBuckets
                    .add(edgeBucketsLength);
                rdrCtx.stats.stat_array_renderer_edgeBucketCounts
                    .add(edgeBucketsLength);
            }
            edgeBuckets = edgeBuckets_ref.getArray(edgeBucketsLength);
            edgeBucketCounts = edgeBucketCounts_ref.getArray(edgeBucketsLength);
        }

        edgeMinY = Integer.MAX_VALUE;
        edgeMaxY = Integer.MIN_VALUE;
        edgeMinX = Double.POSITIVE_INFINITY;
        edgeMaxX = Double.NEGATIVE_INFINITY;

        // reset used mark:
        edgeCount = 0;
        activeEdgeMaxUsed = 0;
        edges.used = 0;

        return this; // fluent API
    }

    /**
     * Disposes this renderer and recycle it clean up before reusing this instance
     */
    void dispose() {
        if (DO_STATS) {
            rdrCtx.stats.stat_rdr_activeEdges.add(activeEdgeMaxUsed);
            rdrCtx.stats.stat_rdr_edges.add(edges.used);
            rdrCtx.stats.stat_rdr_edges_count.add(edges.used / SIZEOF_EDGE_BYTES);
            rdrCtx.stats.hist_rdr_edges_count.add(edges.used / SIZEOF_EDGE_BYTES);
            rdrCtx.stats.totalOffHeap += edges.length;
        }
        // Return arrays:
        crossings = crossings_ref.putArray(crossings);
        aux_crossings = aux_crossings_ref.putArray(aux_crossings);

        edgePtrs = edgePtrs_ref.putArray(edgePtrs);
        aux_edgePtrs = aux_edgePtrs_ref.putArray(aux_edgePtrs);

        alphaLine = alphaLine_ref.putArray(alphaLine, 0, 0); // already zero filled
        blkFlags  = blkFlags_ref.putArray(blkFlags, 0, 0); // already zero filled

        if (edgeMinY != Integer.MAX_VALUE) {
            // if context is maked as DIRTY:
            if (rdrCtx.dirty) {
                // may happen if an exception if thrown in the pipeline processing:
                // clear completely buckets arrays:
                buckets_minY = 0;
                buckets_maxY = boundsMaxY - boundsMinY;
            }
            // clear only used part
            edgeBuckets = edgeBuckets_ref.putArray(edgeBuckets, buckets_minY,
                                                                buckets_maxY);
            edgeBucketCounts = edgeBucketCounts_ref.putArray(edgeBucketCounts,
                                                             buckets_minY,
                                                             buckets_maxY + 1);
        } else {
            // unused arrays
            edgeBuckets = edgeBuckets_ref.putArray(edgeBuckets, 0, 0);
            edgeBucketCounts = edgeBucketCounts_ref.putArray(edgeBucketCounts, 0, 0);
        }

        // At last: resize back off-heap edges to initial size
        if (edges.length != INITIAL_EDGES_CAPACITY) {
            // note: may throw OOME:
            edges.resize(INITIAL_EDGES_CAPACITY);
        }
        if (DO_CLEAN_DIRTY) {
            // Force zero-fill dirty arrays:
            edges.fill(BYTE_0);
        }
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_endRendering.stop();
        }
        // recycle the RendererContext instance
        DMarlinRenderingEngine.returnRendererContext(rdrCtx);
    }

    private static double tosubpixx(final double pix_x) {
        return SUBPIXEL_SCALE_X * pix_x;
    }

    private static double tosubpixy(final double pix_y) {
        // shift y by -0.5 for fast ceil(y - 0.5):
        return SUBPIXEL_SCALE_Y * pix_y - 0.5d;
    }

    @Override
    public void moveTo(final double pix_x0, final double pix_y0) {
        closePath();
        final double sx = tosubpixx(pix_x0);
        final double sy = tosubpixy(pix_y0);
        this.sx0 = sx;
        this.sy0 = sy;
        this.x0 = sx;
        this.y0 = sy;
    }

    @Override
    public void lineTo(final double pix_x1, final double pix_y1) {
        final double x1 = tosubpixx(pix_x1);
        final double y1 = tosubpixy(pix_y1);
        addLine(x0, y0, x1, y1);
        x0 = x1;
        y0 = y1;
    }

    @Override
    public void curveTo(final double pix_x1, final double pix_y1,
                        final double pix_x2, final double pix_y2,
                        final double pix_x3, final double pix_y3)
    {
        final double xe = tosubpixx(pix_x3);
        final double ye = tosubpixy(pix_y3);
        curve.set(x0, y0,
                tosubpixx(pix_x1), tosubpixy(pix_y1),
                tosubpixx(pix_x2), tosubpixy(pix_y2),
                xe, ye);
        curveBreakIntoLinesAndAdd(x0, y0, curve, xe, ye);
        x0 = xe;
        y0 = ye;
    }

    @Override
    public void quadTo(final double pix_x1, final double pix_y1,
                       final double pix_x2, final double pix_y2)
    {
        final double xe = tosubpixx(pix_x2);
        final double ye = tosubpixy(pix_y2);
        curve.set(x0, y0,
                tosubpixx(pix_x1), tosubpixy(pix_y1),
                xe, ye);
        quadBreakIntoLinesAndAdd(x0, y0, curve, xe, ye);
        x0 = xe;
        y0 = ye;
    }

    @Override
    public void closePath() {
        if (x0 != sx0 || y0 != sy0) {
            addLine(x0, y0, sx0, sy0);
            x0 = sx0;
            y0 = sy0;
        }
    }

    @Override
    public void pathDone() {
        closePath();
    }

    @Override
    public long getNativeConsumer() {
        throw new InternalError("Renderer does not use a native consumer.");
    }

    private void _endRendering(final int ymin, final int ymax) {
        if (DISABLE_RENDER) {
            return;
        }

        // Get X bounds as true pixel boundaries to compute correct pixel coverage:
        final int bboxx0 = bbox_spminX;
        final int bboxx1 = bbox_spmaxX;

        final boolean windingRuleEvenOdd = (windingRule == WIND_EVEN_ODD);

        // Useful when processing tile line by tile line
        final int[] _alpha = alphaLine;

        // local vars (performance):
        final MarlinCache _cache = cache;
        final OffHeapArray _edges = edges;
        final int[] _edgeBuckets = edgeBuckets;
        final int[] _edgeBucketCounts = edgeBucketCounts;

        int[] _crossings = this.crossings;
        int[] _edgePtrs  = this.edgePtrs;

        // merge sort auxiliary storage:
        int[] _aux_crossings = this.aux_crossings;
        int[] _aux_edgePtrs  = this.aux_edgePtrs;

        // copy constants:
        final long _OFF_ERROR    = OFF_ERROR;
        final long _OFF_BUMP_X   = OFF_BUMP_X;
        final long _OFF_BUMP_ERR = OFF_BUMP_ERR;

        final long _OFF_NEXT     = OFF_NEXT;
        final long _OFF_YMAX     = OFF_YMAX;

        final int _ALL_BUT_LSB   = ALL_BUT_LSB;
        final int _ERR_STEP_MAX  = ERR_STEP_MAX;

        // unsafe I/O:
        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long    addr0  = _edges.address;
        long addr;
        final int _SUBPIXEL_LG_POSITIONS_X = SUBPIXEL_LG_POSITIONS_X;
        final int _SUBPIXEL_LG_POSITIONS_Y = SUBPIXEL_LG_POSITIONS_Y;
        final int _SUBPIXEL_MASK_X = SUBPIXEL_MASK_X;
        final int _SUBPIXEL_MASK_Y = SUBPIXEL_MASK_Y;
        final int _SUBPIXEL_POSITIONS_X = SUBPIXEL_POSITIONS_X;

        final int _MIN_VALUE = Integer.MIN_VALUE;
        final int _MAX_VALUE = Integer.MAX_VALUE;

        // Now we iterate through the scanlines. We must tell emitRow the coord
        // of the first non-transparent pixel, so we must keep accumulators for
        // the first and last pixels of the section of the current pixel row
        // that we will emit.
        // We also need to accumulate pix_bbox, but the iterator does it
        // for us. We will just get the values from it once this loop is done
        int minX = _MAX_VALUE;
        int maxX = _MIN_VALUE;

        int y = ymin;
        int bucket = y - boundsMinY;

        int numCrossings = this.edgeCount;
        int edgePtrsLen = _edgePtrs.length;
        int crossingsLen = _crossings.length;
        int _arrayMaxUsed = activeEdgeMaxUsed;
        int ptrLen = 0, newCount, ptrEnd;

        int bucketcount, i, j, ecur;
        int cross, lastCross;
        int x0, x1, tmp, sum, prev, curx, curxo, crorientation, err;
        int pix_x, pix_xmaxm1, pix_xmax;

        int low, high, mid, prevNumCrossings;
        boolean useBinarySearch;

        final int[] _blkFlags = blkFlags;
        final int _BLK_SIZE_LG = BLOCK_SIZE_LG;
        final int _BLK_SIZE = BLOCK_SIZE;

        final boolean _enableBlkFlagsHeuristics = ENABLE_BLOCK_FLAGS_HEURISTICS && this.enableBlkFlags;

        // Use block flags if large pixel span and few crossings:
        // ie mean(distance between crossings) is high
        boolean useBlkFlags = this.prevUseBlkFlags;

        final int stroking = rdrCtx.stroking;

        int lastY = -1; // last emited row


        // Iteration on scanlines
        for (; y < ymax; y++, bucket++) {
            // --- from former ScanLineIterator.next()
            bucketcount = _edgeBucketCounts[bucket];

            // marker on previously sorted edges:
            prevNumCrossings = numCrossings;

            // bucketCount indicates new edge / edge end:
            if (bucketcount != 0) {
                if (DO_STATS) {
                    rdrCtx.stats.stat_rdr_activeEdges_updates.add(numCrossings);
                }

                // last bit set to 1 means that edges ends
                if ((bucketcount & 0x1) != 0) {
                    // eviction in active edge list
                    // cache edges[] address + offset
                    addr = addr0 + _OFF_YMAX;

                    for (i = 0, newCount = 0; i < numCrossings; i++) {
                        // get the pointer to the edge
                        ecur = _edgePtrs[i];
                        // random access so use unsafe:
                        if (_unsafe.getInt(addr + ecur) > y) {
                            _edgePtrs[newCount++] = ecur;
                        }
                    }
                    // update marker on sorted edges minus removed edges:
                    prevNumCrossings = numCrossings = newCount;
                }

                ptrLen = bucketcount >> 1; // number of new edge

                if (ptrLen != 0) {
                    if (DO_STATS) {
                        rdrCtx.stats.stat_rdr_activeEdges_adds.add(ptrLen);
                        if (ptrLen > 10) {
                            rdrCtx.stats.stat_rdr_activeEdges_adds_high.add(ptrLen);
                        }
                    }
                    ptrEnd = numCrossings + ptrLen;

                    if (edgePtrsLen < ptrEnd) {
                        if (DO_STATS) {
                            rdrCtx.stats.stat_array_renderer_edgePtrs.add(ptrEnd);
                        }
                        this.edgePtrs = _edgePtrs
                            = edgePtrs_ref.widenArray(_edgePtrs, numCrossings,
                                                      ptrEnd);

                        edgePtrsLen = _edgePtrs.length;
                        // Get larger auxiliary storage:
                        aux_edgePtrs_ref.putArray(_aux_edgePtrs);

                        // use ArrayCache.getNewSize() to use the same growing
                        // factor than widenArray():
                        if (DO_STATS) {
                            rdrCtx.stats.stat_array_renderer_aux_edgePtrs.add(ptrEnd);
                        }
                        this.aux_edgePtrs = _aux_edgePtrs
                            = aux_edgePtrs_ref.getArray(
                                ArrayCacheConst.getNewSize(numCrossings, ptrEnd)
                            );
                    }

                    // cache edges[] address + offset
                    addr = addr0 + _OFF_NEXT;

                    // add new edges to active edge list:
                    for (ecur = _edgeBuckets[bucket];
                         numCrossings < ptrEnd; numCrossings++)
                    {
                        // store the pointer to the edge
                        _edgePtrs[numCrossings] = ecur;
                        // random access so use unsafe:
                        ecur = _unsafe.getInt(addr + ecur);
                    }

                    if (crossingsLen < numCrossings) {
                        // Get larger array:
                        crossings_ref.putArray(_crossings);

                        if (DO_STATS) {
                            rdrCtx.stats.stat_array_renderer_crossings
                                .add(numCrossings);
                        }
                        this.crossings = _crossings
                            = crossings_ref.getArray(numCrossings);

                        // Get larger auxiliary storage:
                        aux_crossings_ref.putArray(_aux_crossings);

                        if (DO_STATS) {
                            rdrCtx.stats.stat_array_renderer_aux_crossings
                                .add(numCrossings);
                        }
                        this.aux_crossings = _aux_crossings
                            = aux_crossings_ref.getArray(numCrossings);

                        crossingsLen = _crossings.length;
                    }
                    if (DO_STATS) {
                        // update max used mark
                        if (numCrossings > _arrayMaxUsed) {
                            _arrayMaxUsed = numCrossings;
                        }
                    }
                } // ptrLen != 0
            } // bucketCount != 0


            if (numCrossings != 0) {
                /*
                 * thresholds to switch to optimized merge sort
                 * for newly added edges + final merge pass.
                 */
                if ((ptrLen < 10) || (numCrossings < 40)) {
                    if (DO_STATS) {
                        rdrCtx.stats.hist_rdr_crossings.add(numCrossings);
                        rdrCtx.stats.hist_rdr_crossings_adds.add(ptrLen);
                    }

                    /*
                     * threshold to use binary insertion sort instead of
                     * straight insertion sort (to reduce minimize comparisons).
                     */
                    useBinarySearch = (numCrossings >= 20);

                    // if small enough:
                    lastCross = _MIN_VALUE;

                    for (i = 0; i < numCrossings; i++) {
                        // get the pointer to the edge
                        ecur = _edgePtrs[i];

                        /* convert subpixel coordinates into pixel
                            positions for coming scanline */
                        /* note: it is faster to always update edges even
                           if it is removed from AEL for coming or last scanline */

                        // random access so use unsafe:
                        addr = addr0 + ecur; // ecur + OFF_F_CURX

                        // get current crossing:
                        curx = _unsafe.getInt(addr);

                        // update crossing with orientation at last bit:
                        cross = curx;

                        // Increment x using DDA (fixed point):
                        curx += _unsafe.getInt(addr + _OFF_BUMP_X);

                        // Increment error:
                        err  =  _unsafe.getInt(addr + _OFF_ERROR)
                              + _unsafe.getInt(addr + _OFF_BUMP_ERR);

                        // Manual carry handling:
                        // keep sign and carry bit only and ignore last bit (preserve orientation):
                        _unsafe.putInt(addr,               curx - ((err >> 30) & _ALL_BUT_LSB));
                        _unsafe.putInt(addr + _OFF_ERROR, (err & _ERR_STEP_MAX));

                        if (DO_STATS) {
                            rdrCtx.stats.stat_rdr_crossings_updates.add(numCrossings);
                        }

                        // insertion sort of crossings:
                        if (cross < lastCross) {
                            if (DO_STATS) {
                                rdrCtx.stats.stat_rdr_crossings_sorts.add(i);
                            }

                            /* use binary search for newly added edges
                               in crossings if arrays are large enough */
                            if (useBinarySearch && (i >= prevNumCrossings)) {
                                if (DO_STATS) {
                                    rdrCtx.stats.stat_rdr_crossings_bsearch.add(i);
                                }
                                low = 0;
                                high = i - 1;

                                do {
                                    // note: use signed shift (not >>>) for performance
                                    // as indices are small enough to exceed Integer.MAX_VALUE
                                    mid = (low + high) >> 1;

                                    if (_crossings[mid] < cross) {
                                        low = mid + 1;
                                    } else {
                                        high = mid - 1;
                                    }
                                } while (low <= high);

                                for (j = i - 1; j >= low; j--) {
                                    _crossings[j + 1] = _crossings[j];
                                    _edgePtrs [j + 1] = _edgePtrs[j];
                                }
                                _crossings[low] = cross;
                                _edgePtrs [low] = ecur;

                            } else {
                                j = i - 1;
                                _crossings[i] = _crossings[j];
                                _edgePtrs[i] = _edgePtrs[j];

                                while ((--j >= 0) && (_crossings[j] > cross)) {
                                    _crossings[j + 1] = _crossings[j];
                                    _edgePtrs [j + 1] = _edgePtrs[j];
                                }
                                _crossings[j + 1] = cross;
                                _edgePtrs [j + 1] = ecur;
                            }

                        } else {
                            _crossings[i] = lastCross = cross;
                        }
                    }
                } else {
                    if (DO_STATS) {
                        rdrCtx.stats.stat_rdr_crossings_msorts.add(numCrossings);
                        rdrCtx.stats.hist_rdr_crossings_ratio
                            .add((1000 * ptrLen) / numCrossings);
                        rdrCtx.stats.hist_rdr_crossings_msorts.add(numCrossings);
                        rdrCtx.stats.hist_rdr_crossings_msorts_adds.add(ptrLen);
                    }

                    // Copy sorted data in auxiliary arrays
                    // and perform insertion sort on almost sorted data
                    // (ie i < prevNumCrossings):

                    lastCross = _MIN_VALUE;

                    for (i = 0; i < numCrossings; i++) {
                        // get the pointer to the edge
                        ecur = _edgePtrs[i];

                        /* convert subpixel coordinates into pixel
                            positions for coming scanline */
                        /* note: it is faster to always update edges even
                           if it is removed from AEL for coming or last scanline */

                        // random access so use unsafe:
                        addr = addr0 + ecur; // ecur + OFF_F_CURX

                        // get current crossing:
                        curx = _unsafe.getInt(addr);

                        // update crossing with orientation at last bit:
                        cross = curx;

                        // Increment x using DDA (fixed point):
                        curx += _unsafe.getInt(addr + _OFF_BUMP_X);

                        // Increment error:
                        err  =  _unsafe.getInt(addr + _OFF_ERROR)
                              + _unsafe.getInt(addr + _OFF_BUMP_ERR);

                        // Manual carry handling:
                        // keep sign and carry bit only and ignore last bit (preserve orientation):
                        _unsafe.putInt(addr,               curx - ((err >> 30) & _ALL_BUT_LSB));
                        _unsafe.putInt(addr + _OFF_ERROR, (err & _ERR_STEP_MAX));

                        if (DO_STATS) {
                            rdrCtx.stats.stat_rdr_crossings_updates.add(numCrossings);
                        }

                        if (i >= prevNumCrossings) {
                            // simply store crossing as edgePtrs is in-place:
                            // will be copied and sorted efficiently by mergesort later:
                            _crossings[i]     = cross;

                        } else if (cross < lastCross) {
                            if (DO_STATS) {
                                rdrCtx.stats.stat_rdr_crossings_sorts.add(i);
                            }

                            // (straight) insertion sort of crossings:
                            j = i - 1;
                            _aux_crossings[i] = _aux_crossings[j];
                            _aux_edgePtrs[i] = _aux_edgePtrs[j];

                            while ((--j >= 0) && (_aux_crossings[j] > cross)) {
                                _aux_crossings[j + 1] = _aux_crossings[j];
                                _aux_edgePtrs [j + 1] = _aux_edgePtrs[j];
                            }
                            _aux_crossings[j + 1] = cross;
                            _aux_edgePtrs [j + 1] = ecur;

                        } else {
                            // auxiliary storage:
                            _aux_crossings[i] = lastCross = cross;
                            _aux_edgePtrs [i] = ecur;
                        }
                    }

                    // use Mergesort using auxiliary arrays (sort only right part)
                    MergeSort.mergeSortNoCopy(_crossings,     _edgePtrs,
                                              _aux_crossings, _aux_edgePtrs,
                                              numCrossings,   prevNumCrossings);
                }

                // reset ptrLen
                ptrLen = 0;
                // --- from former ScanLineIterator.next()


                /* note: bboxx0 and bboxx1 must be pixel boundaries
                   to have correct coverage computation */

                // right shift on crossings to get the x-coordinate:
                curxo = _crossings[0];
                x0    = curxo >> 1;
                if (x0 < minX) {
                    minX = x0; // subpixel coordinate
                }

                x1 = _crossings[numCrossings - 1] >> 1;
                if (x1 > maxX) {
                    maxX = x1; // subpixel coordinate
                }


                // compute pixel coverages
                prev = curx = x0;
                // to turn {0, 1} into {-1, 1}, multiply by 2 and subtract 1.
                // last bit contains orientation (0 or 1)
                crorientation = ((curxo & 0x1) << 1) - 1;

                if (windingRuleEvenOdd) {
                    sum = crorientation;

                    // Even Odd winding rule: take care of mask ie sum(orientations)
                    for (i = 1; i < numCrossings; i++) {
                        curxo = _crossings[i];
                        curx  =  curxo >> 1;
                        // to turn {0, 1} into {-1, 1}, multiply by 2 and subtract 1.
                        // last bit contains orientation (0 or 1)
                        crorientation = ((curxo & 0x1) << 1) - 1;

                        if ((sum & 0x1) != 0) {
                            // TODO: perform line clipping on left-right sides
                            // to avoid such bound checks:
                            x0 = (prev > bboxx0) ? prev : bboxx0;

                            if (curx < bboxx1) {
                                x1 = curx;
                            } else {
                                x1 = bboxx1;
                                // skip right side (fast exit loop):
                                i = numCrossings;
                            }

                            if (x0 < x1) {
                                x0 -= bboxx0; // turn x0, x1 from coords to indices
                                x1 -= bboxx0; // in the alpha array.

                                pix_x      =  x0      >> _SUBPIXEL_LG_POSITIONS_X;
                                pix_xmaxm1 = (x1 - 1) >> _SUBPIXEL_LG_POSITIONS_X;

                                if (pix_x == pix_xmaxm1) {
                                    // Start and end in same pixel
                                    tmp = (x1 - x0); // number of subpixels
                                    _alpha[pix_x    ] += tmp;
                                    _alpha[pix_x + 1] -= tmp;

                                    if (useBlkFlags) {
                                        // flag used blocks:
                                        // note: block processing handles extra pixel:
                                        _blkFlags[pix_x    >> _BLK_SIZE_LG] = 1;
                                    }
                                } else {
                                    tmp = (x0 & _SUBPIXEL_MASK_X);
                                    _alpha[pix_x    ]
                                        += (_SUBPIXEL_POSITIONS_X - tmp);
                                    _alpha[pix_x + 1]
                                        += tmp;

                                    pix_xmax = x1 >> _SUBPIXEL_LG_POSITIONS_X;

                                    tmp = (x1 & _SUBPIXEL_MASK_X);
                                    _alpha[pix_xmax    ]
                                        -= (_SUBPIXEL_POSITIONS_X - tmp);
                                    _alpha[pix_xmax + 1]
                                        -= tmp;

                                    if (useBlkFlags) {
                                        // flag used blocks:
                                        // note: block processing handles extra pixel:
                                        _blkFlags[pix_x    >> _BLK_SIZE_LG] = 1;
                                        _blkFlags[pix_xmax >> _BLK_SIZE_LG] = 1;
                                    }
                                }
                            }
                        }

                        sum += crorientation;
                        prev = curx;
                    }
                } else {
                    // Non-zero winding rule: optimize that case (default)
                    // and avoid processing intermediate crossings
                    for (i = 1, sum = 0;; i++) {
                        sum += crorientation;

                        if (sum != 0) {
                            // prev = min(curx)
                            if (prev > curx) {
                                prev = curx;
                            }
                        } else {
                            // TODO: perform line clipping on left-right sides
                            // to avoid such bound checks:
                            x0 = (prev > bboxx0) ? prev : bboxx0;

                            if (curx < bboxx1) {
                                x1 = curx;
                            } else {
                                x1 = bboxx1;
                                // skip right side (fast exit loop):
                                i = numCrossings;
                            }

                            if (x0 < x1) {
                                x0 -= bboxx0; // turn x0, x1 from coords to indices
                                x1 -= bboxx0; // in the alpha array.

                                pix_x      =  x0      >> _SUBPIXEL_LG_POSITIONS_X;
                                pix_xmaxm1 = (x1 - 1) >> _SUBPIXEL_LG_POSITIONS_X;

                                if (pix_x == pix_xmaxm1) {
                                    // Start and end in same pixel
                                    tmp = (x1 - x0); // number of subpixels
                                    _alpha[pix_x    ] += tmp;
                                    _alpha[pix_x + 1] -= tmp;

                                    if (useBlkFlags) {
                                        // flag used blocks:
                                        // note: block processing handles extra pixel:
                                        _blkFlags[pix_x    >> _BLK_SIZE_LG] = 1;
                                    }
                                } else {
                                    tmp = (x0 & _SUBPIXEL_MASK_X);
                                    _alpha[pix_x    ]
                                        += (_SUBPIXEL_POSITIONS_X - tmp);
                                    _alpha[pix_x + 1]
                                        += tmp;

                                    pix_xmax = x1 >> _SUBPIXEL_LG_POSITIONS_X;

                                    tmp = (x1 & _SUBPIXEL_MASK_X);
                                    _alpha[pix_xmax    ]
                                        -= (_SUBPIXEL_POSITIONS_X - tmp);
                                    _alpha[pix_xmax + 1]
                                        -= tmp;

                                    if (useBlkFlags) {
                                        // flag used blocks:
                                        // note: block processing handles extra pixel:
                                        _blkFlags[pix_x    >> _BLK_SIZE_LG] = 1;
                                        _blkFlags[pix_xmax >> _BLK_SIZE_LG] = 1;
                                    }
                                }
                            }
                            prev = _MAX_VALUE;
                        }

                        if (i == numCrossings) {
                            break;
                        }

                        curxo = _crossings[i];
                        curx  =  curxo >> 1;
                        // to turn {0, 1} into {-1, 1}, multiply by 2 and subtract 1.
                        // last bit contains orientation (0 or 1)
                        crorientation = ((curxo & 0x1) << 1) - 1;
                    }
                }
            } // numCrossings > 0

            // even if this last row had no crossings, alpha will be zeroed
            // from the last emitRow call. But this doesn't matter because
            // maxX < minX, so no row will be emitted to the MarlinCache.
            if ((y & _SUBPIXEL_MASK_Y) == _SUBPIXEL_MASK_Y) {
                lastY = y >> _SUBPIXEL_LG_POSITIONS_Y;

                // convert subpixel to pixel coordinate within boundaries:
                minX = FloatMath.max(minX, bboxx0) >> _SUBPIXEL_LG_POSITIONS_X;
                maxX = FloatMath.min(maxX, bboxx1) >> _SUBPIXEL_LG_POSITIONS_X;

                if (maxX >= minX) {
                    // note: alpha array will be zeroed by copyAARow()
                    // +1 because alpha [pix_minX; pix_maxX[
                    // fix range [x0; x1[
                    // note: if x1=bboxx1, then alpha is written up to bboxx1+1
                    // inclusive: alpha[bboxx1] ignored, alpha[bboxx1+1] == 0
                    // (normally so never cleared below)
                    copyAARow(_alpha, lastY, minX, maxX + 1, useBlkFlags);

                    // speculative for next pixel row (scanline coherence):
                    if (_enableBlkFlagsHeuristics) {
                        // Use block flags if large pixel span and few crossings:
                        // ie mean(distance between crossings) is larger than
                        // 1 block size;

                        // fast check width:
                        maxX -= minX;

                        // if stroking: numCrossings /= 2
                        // => shift numCrossings by 1
                        // condition = (width / (numCrossings - 1)) > blockSize
                        useBlkFlags = (maxX > _BLK_SIZE) && (maxX >
                            (((numCrossings >> stroking) - 1) << _BLK_SIZE_LG));

                        if (DO_STATS) {
                            tmp = FloatMath.max(1,
                                    ((numCrossings >> stroking) - 1));
                            rdrCtx.stats.hist_tile_generator_encoding_dist
                                .add(maxX / tmp);
                        }
                    }
                } else {
                    _cache.clearAARow(lastY);
                }
                minX = _MAX_VALUE;
                maxX = _MIN_VALUE;
            }
        } // scan line iterator

        // Emit final row
        y--;
        y >>= _SUBPIXEL_LG_POSITIONS_Y;

        // convert subpixel to pixel coordinate within boundaries:
        minX = FloatMath.max(minX, bboxx0) >> _SUBPIXEL_LG_POSITIONS_X;
        maxX = FloatMath.min(maxX, bboxx1) >> _SUBPIXEL_LG_POSITIONS_X;

        if (maxX >= minX) {
            // note: alpha array will be zeroed by copyAARow()
            // +1 because alpha [pix_minX; pix_maxX[
            // fix range [x0; x1[
            // note: if x1=bboxx1, then alpha is written up to bboxx1+1
            // inclusive: alpha[bboxx1] ignored then cleared and
            // alpha[bboxx1+1] == 0 (normally so never cleared after)
            copyAARow(_alpha, y, minX, maxX + 1, useBlkFlags);
        } else if (y != lastY) {
            _cache.clearAARow(y);
        }

        // update member:
        edgeCount = numCrossings;
        prevUseBlkFlags = useBlkFlags;

        if (DO_STATS) {
            // update max used mark
            activeEdgeMaxUsed = _arrayMaxUsed;
        }
    }

    boolean endRendering() {
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_endRendering.start();
        }
        if (edgeMinY == Integer.MAX_VALUE) {
            return false; // undefined edges bounds
        }

        // bounds as half-open intervals
        final int spminX = FloatMath.max(FloatMath.ceil_int(edgeMinX - 0.5d), boundsMinX);
        final int spmaxX = FloatMath.min(FloatMath.ceil_int(edgeMaxX - 0.5d), boundsMaxX);

        // edge Min/Max Y are already rounded to subpixels within bounds:
        final int spminY = edgeMinY;
        final int spmaxY = edgeMaxY;

        buckets_minY = spminY - boundsMinY;
        buckets_maxY = spmaxY - boundsMinY;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("edgesXY = [" + edgeMinX + " ... " + edgeMaxX
                                + "[ [" + edgeMinY + " ... " + edgeMaxY + "[");
            MarlinUtils.logInfo("spXY    = [" + spminX + " ... " + spmaxX
                                + "[ [" + spminY + " ... " + spmaxY + "[");
        }

        // test clipping for shapes out of bounds
        if ((spminX >= spmaxX) || (spminY >= spmaxY)) {
            return false;
        }

        // half open intervals
        // inclusive:
        final int pminX =  spminX                    >> SUBPIXEL_LG_POSITIONS_X;
        // exclusive:
        final int pmaxX = (spmaxX + SUBPIXEL_MASK_X) >> SUBPIXEL_LG_POSITIONS_X;
        // inclusive:
        final int pminY =  spminY                    >> SUBPIXEL_LG_POSITIONS_Y;
        // exclusive:
        final int pmaxY = (spmaxY + SUBPIXEL_MASK_Y) >> SUBPIXEL_LG_POSITIONS_Y;

        // store BBox to answer ptg.getBBox():
        this.cache.init(pminX, pminY, pmaxX, pmaxY);

        // Heuristics for using block flags:
        if (ENABLE_BLOCK_FLAGS) {
            enableBlkFlags = this.cache.useRLE;
            prevUseBlkFlags = enableBlkFlags && !ENABLE_BLOCK_FLAGS_HEURISTICS;

            if (enableBlkFlags) {
                // ensure blockFlags array is large enough:
                // note: +2 to ensure enough space left at end
                final int blkLen = ((pmaxX - pminX) >> BLOCK_SIZE_LG) + 2;
                if (blkLen > INITIAL_ARRAY) {
                    blkFlags = blkFlags_ref.getArray(blkLen);
                }
            }
        }

        // memorize the rendering bounding box:
        /* note: bbox_spminX and bbox_spmaxX must be pixel boundaries
           to have correct coverage computation */
        // inclusive:
        bbox_spminX = pminX << SUBPIXEL_LG_POSITIONS_X;
        // exclusive:
        bbox_spmaxX = pmaxX << SUBPIXEL_LG_POSITIONS_X;
        // inclusive:
        bbox_spminY = spminY;
        // exclusive:
        bbox_spmaxY = spmaxY;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("pXY       = [" + pminX + " ... " + pmaxX
                                + "[ [" + pminY + " ... " + pmaxY + "[");
            MarlinUtils.logInfo("bbox_spXY = [" + bbox_spminX + " ... "
                                + bbox_spmaxX + "[ [" + bbox_spminY + " ... "
                                + bbox_spmaxY + "[");
        }

        // Prepare alpha line:
        // add 2 to better deal with the last pixel in a pixel row.
        final int width = (pmaxX - pminX) + 2;

        // Useful when processing tile line by tile line
        if (width > INITIAL_AA_ARRAY) {
            if (DO_STATS) {
                rdrCtx.stats.stat_array_renderer_alphaline.add(width);
            }
            alphaLine = alphaLine_ref.getArray(width);
        }

        // process first tile line:
        endRendering(pminY);

        return true;
    }

    private int bbox_spminX, bbox_spmaxX, bbox_spminY, bbox_spmaxY;

    void endRendering(final int pminY) {
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_endRendering_Y.start();
        }

        final int spminY       = pminY << SUBPIXEL_LG_POSITIONS_Y;
        final int fixed_spminY = FloatMath.max(bbox_spminY, spminY);

        // avoid rendering for last call to nextTile()
        if (fixed_spminY < bbox_spmaxY) {
            // process a complete tile line ie scanlines for 32 rows
            final int spmaxY = FloatMath.min(bbox_spmaxY, spminY + SUBPIXEL_TILE);

            // process tile line [0 - 32]
            cache.resetTileLine(pminY);

            // Process only one tile line:
            _endRendering(fixed_spminY, spmaxY);
        }
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_endRendering_Y.stop();
        }
    }

    void copyAARow(final int[] alphaRow,
                   final int pix_y, final int pix_from, final int pix_to,
                   final boolean useBlockFlags)
    {
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_copyAARow.start();
        }
        if (useBlockFlags) {
            if (DO_STATS) {
                rdrCtx.stats.hist_tile_generator_encoding.add(1);
            }
            cache.copyAARowRLE_WithBlockFlags(blkFlags, alphaRow, pix_y, pix_from, pix_to);
        } else {
            if (DO_STATS) {
                rdrCtx.stats.hist_tile_generator_encoding.add(0);
            }
            cache.copyAARowNoRLE(alphaRow, pix_y, pix_from, pix_to);
        }
        if (DO_MONITORS) {
            rdrCtx.stats.mon_rdr_copyAARow.stop();
        }
    }
}
