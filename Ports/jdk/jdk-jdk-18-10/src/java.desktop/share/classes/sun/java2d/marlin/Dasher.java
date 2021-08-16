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

import java.util.Arrays;
import sun.java2d.marlin.TransformingPathConsumer2D.CurveBasicMonotonizer;
import sun.java2d.marlin.TransformingPathConsumer2D.CurveClipSplitter;

/**
 * The <code>Dasher</code> class takes a series of linear commands
 * (<code>moveTo</code>, <code>lineTo</code>, <code>close</code> and
 * <code>end</code>) and breaks them into smaller segments according to a
 * dash pattern array and a starting dash phase.
 *
 * <p> Issues: in J2Se, a zero length dash segment as drawn as a very
 * short dash, whereas Pisces does not draw anything.  The PostScript
 * semantics are unclear.
 *
 */
final class Dasher implements DPathConsumer2D, MarlinConst {

    /* huge circle with radius ~ 2E9 only needs 12 subdivision levels */
    static final int REC_LIMIT = 16;
    static final double CURVE_LEN_ERR = MarlinProperties.getCurveLengthError(); // 0.01 initial
    static final double MIN_T_INC = 1.0d / (1 << REC_LIMIT);

    static final double EPS = 1e-6d;

    // More than 24 bits of mantissa means we can no longer accurately
    // measure the number of times cycled through the dash array so we
    // punt and override the phase to just be 0 past that point.
    static final double MAX_CYCLES = 16000000.0d;

    private DPathConsumer2D out;
    private double[] dash;
    private int dashLen;
    private double startPhase;
    private boolean startDashOn;
    private int startIdx;

    private boolean starting;
    private boolean needsMoveTo;

    private int idx;
    private boolean dashOn;
    private double phase;

    // The starting point of the path
    private double sx0, sy0;
    // the current point
    private double cx0, cy0;

    // temporary storage for the current curve
    private final double[] curCurvepts;

    // per-thread renderer context
    final RendererContext rdrCtx;

    // flag to recycle dash array copy
    boolean recycleDashes;

    // We don't emit the first dash right away. If we did, caps would be
    // drawn on it, but we need joins to be drawn if there's a closePath()
    // So, we store the path elements that make up the first dash in the
    // buffer below.
    private double[] firstSegmentsBuffer; // dynamic array
    private int firstSegidx;

    // dashes ref (dirty)
    final DoubleArrayCache.Reference dashes_ref;
    // firstSegmentsBuffer ref (dirty)
    final DoubleArrayCache.Reference firstSegmentsBuffer_ref;

    // Bounds of the drawing region, at pixel precision.
    private double[] clipRect;

    // the outcode of the current point
    private int cOutCode = 0;

    private boolean subdivide = DO_CLIP_SUBDIVIDER;

    private final LengthIterator li = new LengthIterator();

    private final CurveClipSplitter curveSplitter;

    private double cycleLen;
    private boolean outside;
    private double totalSkipLen;

    /**
     * Constructs a <code>Dasher</code>.
     * @param rdrCtx per-thread renderer context
     */
    Dasher(final RendererContext rdrCtx) {
        this.rdrCtx = rdrCtx;

        dashes_ref = rdrCtx.newDirtyDoubleArrayRef(INITIAL_ARRAY); // 1K

        firstSegmentsBuffer_ref = rdrCtx.newDirtyDoubleArrayRef(INITIAL_ARRAY); // 1K
        firstSegmentsBuffer     = firstSegmentsBuffer_ref.initial;

        // we need curCurvepts to be able to contain 2 curves because when
        // dashing curves, we need to subdivide it
        curCurvepts = new double[8 * 2];

        this.curveSplitter = rdrCtx.curveClipSplitter;
    }

    /**
     * Initialize the <code>Dasher</code>.
     *
     * @param out an output <code>DPathConsumer2D</code>.
     * @param dash an array of <code>double</code>s containing the dash pattern
     * @param dashLen length of the given dash array
     * @param phase a <code>double</code> containing the dash phase
     * @param recycleDashes true to indicate to recycle the given dash array
     * @return this instance
     */
    Dasher init(final DPathConsumer2D out, final double[] dash, final int dashLen,
                double phase, final boolean recycleDashes)
    {
        this.out = out;

        // Normalize so 0 <= phase < dash[0]
        int sidx = 0;
        dashOn = true;

        // note: BasicStroke constructor checks dash elements and sum > 0
        double sum = 0.0d;
        for (int i = 0; i < dashLen; i++) {
            sum += dash[i];
        }
        this.cycleLen = sum;

        double cycles = phase / sum;
        if (phase < 0.0d) {
            if (-cycles >= MAX_CYCLES) {
                phase = 0.0d;
            } else {
                int fullcycles = FloatMath.floor_int(-cycles);
                if ((fullcycles & dashLen & 1) != 0) {
                    dashOn = !dashOn;
                }
                phase += fullcycles * sum;
                while (phase < 0.0d) {
                    if (--sidx < 0) {
                        sidx = dashLen - 1;
                    }
                    phase += dash[sidx];
                    dashOn = !dashOn;
                }
            }
        } else if (phase > 0.0d) {
            if (cycles >= MAX_CYCLES) {
                phase = 0.0d;
            } else {
                int fullcycles = FloatMath.floor_int(cycles);
                if ((fullcycles & dashLen & 1) != 0) {
                    dashOn = !dashOn;
                }
                phase -= fullcycles * sum;
                double d;
                while (phase >= (d = dash[sidx])) {
                    phase -= d;
                    sidx = (sidx + 1) % dashLen;
                    dashOn = !dashOn;
                }
            }
        }

        this.dash = dash;
        this.dashLen = dashLen;
        this.phase = phase;
        this.startPhase = phase;
        this.startDashOn = dashOn;
        this.startIdx = sidx;
        this.starting = true;
        this.needsMoveTo = false;
        this.firstSegidx = 0;

        this.recycleDashes = recycleDashes;

        if (rdrCtx.doClip) {
            this.clipRect = rdrCtx.clipRect;
        } else {
            this.clipRect = null;
            this.cOutCode = 0;
        }
        return this; // fluent API
    }

    /**
     * Disposes this dasher:
     * clean up before reusing this instance
     */
    void dispose() {
        if (DO_CLEAN_DIRTY) {
            // Force zero-fill dirty arrays:
            Arrays.fill(curCurvepts, 0.0d);
        }
        // Return arrays:
        if (recycleDashes) {
            dash = dashes_ref.putArray(dash);
        }
        firstSegmentsBuffer = firstSegmentsBuffer_ref.putArray(firstSegmentsBuffer);
    }

    double[] copyDashArray(final float[] dashes) {
        final int len = dashes.length;
        final double[] newDashes;
        if (len <= MarlinConst.INITIAL_ARRAY) {
            newDashes = dashes_ref.initial;
        } else {
            if (DO_STATS) {
                rdrCtx.stats.stat_array_dasher_dasher.add(len);
            }
            newDashes = dashes_ref.getArray(len);
        }
        for (int i = 0; i < len; i++) { newDashes[i] = dashes[i]; }
        return newDashes;
    }

    @Override
    public void moveTo(final double x0, final double y0) {
        if (firstSegidx != 0) {
            out.moveTo(sx0, sy0);
            emitFirstSegments();
        }
        this.needsMoveTo = true;
        this.idx = startIdx;
        this.dashOn = this.startDashOn;
        this.phase = this.startPhase;
        this.cx0 = x0;
        this.cy0 = y0;

        // update starting point:
        this.sx0 = x0;
        this.sy0 = y0;
        this.starting = true;

        if (clipRect != null) {
            final int outcode = Helpers.outcode(x0, y0, clipRect);
            this.cOutCode = outcode;
            this.outside = false;
            this.totalSkipLen = 0.0d;
        }
    }

    private void emitSeg(double[] buf, int off, int type) {
        switch (type) {
        case 4:
            out.lineTo(buf[off], buf[off + 1]);
            return;
        case 8:
            out.curveTo(buf[off    ], buf[off + 1],
                        buf[off + 2], buf[off + 3],
                        buf[off + 4], buf[off + 5]);
            return;
        case 6:
            out.quadTo(buf[off    ], buf[off + 1],
                       buf[off + 2], buf[off + 3]);
            return;
        default:
        }
    }

    private void emitFirstSegments() {
        final double[] fSegBuf = firstSegmentsBuffer;

        for (int i = 0, len = firstSegidx; i < len; ) {
            int type = (int)fSegBuf[i];
            emitSeg(fSegBuf, i + 1, type);
            i += (type - 1);
        }
        firstSegidx = 0;
    }

    // precondition: pts must be in relative coordinates (relative to x0,y0)
    private void goTo(final double[] pts, final int off, final int type,
                      final boolean on)
    {
        final int index = off + type;
        final double x = pts[index - 4];
        final double y = pts[index - 3];

        if (on) {
            if (starting) {
                goTo_starting(pts, off, type);
            } else {
                if (needsMoveTo) {
                    needsMoveTo = false;
                    out.moveTo(cx0, cy0);
                }
                emitSeg(pts, off, type);
            }
        } else {
            if (starting) {
                // low probability test (hotspot)
                starting = false;
            }
            needsMoveTo = true;
        }
        this.cx0 = x;
        this.cy0 = y;
    }

    private void goTo_starting(final double[] pts, final int off, final int type) {
        int len = type - 1; // - 2 + 1
        int segIdx = firstSegidx;
        double[] buf = firstSegmentsBuffer;

        if (segIdx + len  > buf.length) {
            if (DO_STATS) {
                rdrCtx.stats.stat_array_dasher_firstSegmentsBuffer
                    .add(segIdx + len);
            }
            firstSegmentsBuffer = buf
                = firstSegmentsBuffer_ref.widenArray(buf, segIdx,
                                                     segIdx + len);
        }
        buf[segIdx++] = type;
        len--;
        // small arraycopy (2, 4 or 6) but with offset:
        System.arraycopy(pts, off, buf, segIdx, len);
        firstSegidx = segIdx + len;
    }

    @Override
    public void lineTo(final double x1, final double y1) {
        final int outcode0 = this.cOutCode;

        if (clipRect != null) {
            final int outcode1 = Helpers.outcode(x1, y1, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1);

            if (orCode != 0) {
                final int sideCode = outcode0 & outcode1;

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        // subdivide curve => callback with subdivided parts:
                        boolean ret = curveSplitter.splitLine(cx0, cy0, x1, y1,
                                                              orCode, this);
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode1;
                    skipLineTo(x1, y1);
                    return;
                }
            }

            this.cOutCode = outcode1;

            if (this.outside) {
                this.outside = false;
                // Adjust current index, phase & dash:
                skipLen();
            }
        }
        _lineTo(x1, y1);
    }

    private void _lineTo(final double x1, final double y1) {
        final double dx = x1 - cx0;
        final double dy = y1 - cy0;

        double len = dx * dx + dy * dy;
        if (len == 0.0d) {
            return;
        }
        len = Math.sqrt(len);

        // The scaling factors needed to get the dx and dy of the
        // transformed dash segments.
        final double cx = dx / len;
        final double cy = dy / len;

        final double[] _curCurvepts = curCurvepts;
        final double[] _dash = dash;
        final int _dashLen = this.dashLen;

        int _idx = idx;
        boolean _dashOn = dashOn;
        double _phase = phase;

        double leftInThisDashSegment, rem;

        while (true) {
            leftInThisDashSegment = _dash[_idx] - _phase;
            rem = len - leftInThisDashSegment;

            if (rem <= EPS) {
                _curCurvepts[0] = x1;
                _curCurvepts[1] = y1;

                goTo(_curCurvepts, 0, 4, _dashOn);

                // Advance phase within current dash segment
                _phase += len;

                // compare values using epsilon:
                if (Math.abs(rem) <= EPS) {
                    _phase = 0.0d;
                    _idx = (_idx + 1) % _dashLen;
                    _dashOn = !_dashOn;
                }
                break;
            }

            _curCurvepts[0] = cx0 + leftInThisDashSegment * cx;
            _curCurvepts[1] = cy0 + leftInThisDashSegment * cy;

            goTo(_curCurvepts, 0, 4, _dashOn);

            len = rem;
            // Advance to next dash segment
            _idx = (_idx + 1) % _dashLen;
            _dashOn = !_dashOn;
            _phase = 0.0d;
        }
        // Save local state:
        idx = _idx;
        dashOn = _dashOn;
        phase = _phase;
    }

    private void skipLineTo(final double x1, final double y1) {
        final double dx = x1 - cx0;
        final double dy = y1 - cy0;

        double len = dx * dx + dy * dy;
        if (len != 0.0d) {
            len = Math.sqrt(len);
        }

        // Accumulate skipped length:
        this.outside = true;
        this.totalSkipLen += len;

        // Fix initial move:
        this.needsMoveTo = true;
        this.starting = false;

        this.cx0 = x1;
        this.cy0 = y1;
    }

    public void skipLen() {
        double len = this.totalSkipLen;
        this.totalSkipLen = 0.0d;

        final double[] _dash = dash;
        final int _dashLen = this.dashLen;

        int _idx = idx;
        boolean _dashOn = dashOn;
        double _phase = phase;

        // -2 to ensure having 2 iterations of the post-loop
        // to compensate the remaining phase
        final long fullcycles = (long)Math.floor(len / cycleLen) - 2L;

        if (fullcycles > 0L) {
            len -= cycleLen * fullcycles;

            final long iterations = fullcycles * _dashLen;
            _idx = (int) (iterations + _idx) % _dashLen;
            _dashOn = (iterations + (_dashOn ? 1L : 0L) & 1L) == 1L;
        }

        double leftInThisDashSegment, rem;

        while (true) {
            leftInThisDashSegment = _dash[_idx] - _phase;
            rem = len - leftInThisDashSegment;

            if (rem <= EPS) {
                // Advance phase within current dash segment
                _phase += len;

                // compare values using epsilon:
                if (Math.abs(rem) <= EPS) {
                    _phase = 0.0d;
                    _idx = (_idx + 1) % _dashLen;
                    _dashOn = !_dashOn;
                }
                break;
            }

            len = rem;
            // Advance to next dash segment
            _idx = (_idx + 1) % _dashLen;
            _dashOn = !_dashOn;
            _phase = 0.0d;
        }
        // Save local state:
        idx = _idx;
        dashOn = _dashOn;
        phase = _phase;
    }

    // preconditions: curCurvepts must be an array of length at least 2 * type,
    // that contains the curve we want to dash in the first type elements
    private void somethingTo(final int type) {
        final double[] _curCurvepts = curCurvepts;
        if (pointCurve(_curCurvepts, type)) {
            return;
        }
        final LengthIterator _li = li;
        final double[] _dash = dash;
        final int _dashLen = this.dashLen;

        _li.initializeIterationOnCurve(_curCurvepts, type);

        int _idx = idx;
        boolean _dashOn = dashOn;
        double _phase = phase;

        // initially the current curve is at curCurvepts[0...type]
        int curCurveoff = 0;
        double prevT = 0.0d;
        double t;
        double leftInThisDashSegment = _dash[_idx] - _phase;

        while ((t = _li.next(leftInThisDashSegment)) < 1.0d) {
            if (t != 0.0d) {
                Helpers.subdivideAt((t - prevT) / (1.0d - prevT),
                                    _curCurvepts, curCurveoff,
                                    _curCurvepts, 0, type);
                prevT = t;
                goTo(_curCurvepts, 2, type, _dashOn);
                curCurveoff = type;
            }
            // Advance to next dash segment
            _idx = (_idx + 1) % _dashLen;
            _dashOn = !_dashOn;
            _phase = 0.0d;
            leftInThisDashSegment = _dash[_idx];
        }

        goTo(_curCurvepts, curCurveoff + 2, type, _dashOn);

        _phase += _li.lastSegLen();

        // compare values using epsilon:
        if (_phase + EPS >= _dash[_idx]) {
            _phase = 0.0d;
            _idx = (_idx + 1) % _dashLen;
            _dashOn = !_dashOn;
        }
        // Save local state:
        idx = _idx;
        dashOn = _dashOn;
        phase = _phase;

        // reset LengthIterator:
        _li.reset();
    }

    private void skipSomethingTo(final int type) {
        final double[] _curCurvepts = curCurvepts;
        if (pointCurve(_curCurvepts, type)) {
            return;
        }
        final LengthIterator _li = li;

        _li.initializeIterationOnCurve(_curCurvepts, type);

        // In contrary to somethingTo(),
        // just estimate properly the curve length:
        final double len = _li.totalLength();

        // Accumulate skipped length:
        this.outside = true;
        this.totalSkipLen += len;

        // Fix initial move:
        this.needsMoveTo = true;
        this.starting = false;
    }

    private static boolean pointCurve(final double[] curve, final int type) {
        for (int i = 2; i < type; i++) {
            if (curve[i] != curve[i-2]) {
                return false;
            }
        }
        return true;
    }

    // Objects of this class are used to iterate through curves. They return
    // t values where the left side of the curve has a specified length.
    // It does this by subdividing the input curve until a certain error
    // condition has been met. A recursive subdivision procedure would
    // return as many as 1<<limit curves, but this is an iterator and we
    // don't need all the curves all at once, so what we carry out a
    // lazy inorder traversal of the recursion tree (meaning we only move
    // through the tree when we need the next subdivided curve). This saves
    // us a lot of memory because at any one time we only need to store
    // limit+1 curves - one for each level of the tree + 1.
    // NOTE: the way we do things here is not enough to traverse a general
    // tree; however, the trees we are interested in have the property that
    // every non leaf node has exactly 2 children
    static final class LengthIterator {
        // Holds the curves at various levels of the recursion. The root
        // (i.e. the original curve) is at recCurveStack[0] (but then it
        // gets subdivided, the left half is put at 1, so most of the time
        // only the right half of the original curve is at 0)
        private final double[][] recCurveStack; // dirty
        // sidesRight[i] indicates whether the node at level i+1 in the path from
        // the root to the current leaf is a left or right child of its parent.
        private final boolean[] sidesRight; // dirty
        private int curveType;
        // lastT and nextT delimit the current leaf.
        private double nextT;
        private double lenAtNextT;
        private double lastT;
        private double lenAtLastT;
        private double lenAtLastSplit;
        private double lastSegLen;
        // the current level in the recursion tree. 0 is the root. limit
        // is the deepest possible leaf.
        private int recLevel;
        private boolean done;

        // the lengths of the lines of the control polygon. Only its first
        // curveType/2 - 1 elements are valid. This is an optimization. See
        // next() for more detail.
        private final double[] curLeafCtrlPolyLengths = new double[3];

        LengthIterator() {
            this.recCurveStack = new double[REC_LIMIT + 1][8];
            this.sidesRight = new boolean[REC_LIMIT];
            // if any methods are called without first initializing this object
            // on a curve, we want it to fail ASAP.
            this.nextT = Double.MAX_VALUE;
            this.lenAtNextT = Double.MAX_VALUE;
            this.lenAtLastSplit = Double.MIN_VALUE;
            this.recLevel = Integer.MIN_VALUE;
            this.lastSegLen = Double.MAX_VALUE;
            this.done = true;
        }

        /**
         * Reset this LengthIterator.
         */
        void reset() {
            // keep data dirty
            // as it appears not useful to reset data:
            if (DO_CLEAN_DIRTY) {
                final int recLimit = recCurveStack.length - 1;
                for (int i = recLimit; i >= 0; i--) {
                    Arrays.fill(recCurveStack[i], 0.0d);
                }
                Arrays.fill(sidesRight, false);
                Arrays.fill(curLeafCtrlPolyLengths, 0.0d);
                Arrays.fill(nextRoots, 0.0d);
                Arrays.fill(flatLeafCoefCache, 0.0d);
                flatLeafCoefCache[2] = -1.0d;
            }
        }

        void initializeIterationOnCurve(final double[] pts, final int type) {
            // optimize arraycopy (8 values faster than 6 = type):
            System.arraycopy(pts, 0, recCurveStack[0], 0, 8);
            this.curveType = type;
            this.recLevel = 0;
            this.lastT = 0.0d;
            this.lenAtLastT = 0.0d;
            this.nextT = 0.0d;
            this.lenAtNextT = 0.0d;
            goLeft(); // initializes nextT and lenAtNextT properly
            this.lenAtLastSplit = 0.0d;
            if (recLevel > 0) {
                this.sidesRight[0] = false;
                this.done = false;
            } else {
                // the root of the tree is a leaf so we're done.
                this.sidesRight[0] = true;
                this.done = true;
            }
            this.lastSegLen = 0.0d;
        }

        // 0 == false, 1 == true, -1 == invalid cached value.
        private int cachedHaveLowAcceleration = -1;

        private boolean haveLowAcceleration(final double err) {
            if (cachedHaveLowAcceleration == -1) {
                final double len1 = curLeafCtrlPolyLengths[0];
                final double len2 = curLeafCtrlPolyLengths[1];
                // the test below is equivalent to !within(len1/len2, 1, err).
                // It is using a multiplication instead of a division, so it
                // should be a bit faster.
                if (!Helpers.within(len1, len2, err * len2)) {
                    cachedHaveLowAcceleration = 0;
                    return false;
                }
                if (curveType == 8) {
                    final double len3 = curLeafCtrlPolyLengths[2];
                    // if len1 is close to 2 and 2 is close to 3, that probably
                    // means 1 is close to 3 so the second part of this test might
                    // not be needed, but it doesn't hurt to include it.
                    final double errLen3 = err * len3;
                    if (!(Helpers.within(len2, len3, errLen3) &&
                          Helpers.within(len1, len3, errLen3))) {
                        cachedHaveLowAcceleration = 0;
                        return false;
                    }
                }
                cachedHaveLowAcceleration = 1;
                return true;
            }

            return (cachedHaveLowAcceleration == 1);
        }

        // we want to avoid allocations/gc so we keep this array so we
        // can put roots in it,
        private final double[] nextRoots = new double[4];

        // caches the coefficients of the current leaf in its flattened
        // form (see inside next() for what that means). The cache is
        // invalid when it's third element is negative, since in any
        // valid flattened curve, this would be >= 0.
        private final double[] flatLeafCoefCache = new double[]{0.0d, 0.0d, -1.0d, 0.0d};

        // returns the t value where the remaining curve should be split in
        // order for the left subdivided curve to have length len. If len
        // is >= than the length of the uniterated curve, it returns 1.
        double next(final double len) {
            final double targetLength = lenAtLastSplit + len;
            while (lenAtNextT < targetLength) {
                if (done) {
                    lastSegLen = lenAtNextT - lenAtLastSplit;
                    return 1.0d;
                }
                goToNextLeaf();
            }
            lenAtLastSplit = targetLength;
            final double leaflen = lenAtNextT - lenAtLastT;
            double t = (targetLength - lenAtLastT) / leaflen;

            // cubicRootsInAB is a fairly expensive call, so we just don't do it
            // if the acceleration in this section of the curve is small enough.
            if (!haveLowAcceleration(0.05d)) {
                // We flatten the current leaf along the x axis, so that we're
                // left with a, b, c which define a 1D Bezier curve. We then
                // solve this to get the parameter of the original leaf that
                // gives us the desired length.
                final double[] _flatLeafCoefCache = flatLeafCoefCache;

                if (_flatLeafCoefCache[2] < 0.0d) {
                    double x =     curLeafCtrlPolyLengths[0],
                           y = x + curLeafCtrlPolyLengths[1];
                    if (curveType == 8) {
                        double z = y + curLeafCtrlPolyLengths[2];
                        _flatLeafCoefCache[0] = 3.0d * (x - y) + z;
                        _flatLeafCoefCache[1] = 3.0d * (y - 2.0d * x);
                        _flatLeafCoefCache[2] = 3.0d * x;
                        _flatLeafCoefCache[3] = -z;
                    } else if (curveType == 6) {
                        _flatLeafCoefCache[0] = 0.0d;
                        _flatLeafCoefCache[1] = y - 2.0d * x;
                        _flatLeafCoefCache[2] = 2.0d * x;
                        _flatLeafCoefCache[3] = -y;
                    }
                }
                double a = _flatLeafCoefCache[0];
                double b = _flatLeafCoefCache[1];
                double c = _flatLeafCoefCache[2];
                double d = t * _flatLeafCoefCache[3];

                // we use cubicRootsInAB here, because we want only roots in 0, 1,
                // and our quadratic root finder doesn't filter, so it's just a
                // matter of convenience.
                final int n = Helpers.cubicRootsInAB(a, b, c, d, nextRoots, 0, 0.0d, 1.0d);
                if (n == 1 && !Double.isNaN(nextRoots[0])) {
                    t = nextRoots[0];
                }
            }
            // t is relative to the current leaf, so we must make it a valid parameter
            // of the original curve.
            t = t * (nextT - lastT) + lastT;
            if (t >= 1.0d) {
                t = 1.0d;
                done = true;
            }
            // even if done = true, if we're here, that means targetLength
            // is equal to, or very, very close to the total length of the
            // curve, so lastSegLen won't be too high. In cases where len
            // overshoots the curve, this method will exit in the while
            // loop, and lastSegLen will still be set to the right value.
            lastSegLen = len;
            return t;
        }

        double totalLength() {
            while (!done) {
                goToNextLeaf();
            }
            // reset LengthIterator:
            reset();

            return lenAtNextT;
        }

        double lastSegLen() {
            return lastSegLen;
        }

        // go to the next leaf (in an inorder traversal) in the recursion tree
        // preconditions: must be on a leaf, and that leaf must not be the root.
        private void goToNextLeaf() {
            // We must go to the first ancestor node that has an unvisited
            // right child.
            final boolean[] _sides = sidesRight;
            int _recLevel = recLevel;
            _recLevel--;

            while(_sides[_recLevel]) {
                if (_recLevel == 0) {
                    recLevel = 0;
                    done = true;
                    return;
                }
                _recLevel--;
            }

            _sides[_recLevel] = true;
            // optimize arraycopy (8 values faster than 6 = type):
            System.arraycopy(recCurveStack[_recLevel++], 0,
                             recCurveStack[_recLevel], 0, 8);
            recLevel = _recLevel;
            goLeft();
        }

        // go to the leftmost node from the current node. Return its length.
        private void goLeft() {
            final double len = onLeaf();
            if (len >= 0.0d) {
                lastT = nextT;
                lenAtLastT = lenAtNextT;
                nextT += (1 << (REC_LIMIT - recLevel)) * MIN_T_INC;
                lenAtNextT += len;
                // invalidate caches
                flatLeafCoefCache[2] = -1.0d;
                cachedHaveLowAcceleration = -1;
            } else {
                Helpers.subdivide(recCurveStack[recLevel],
                                   recCurveStack[recLevel + 1],
                                   recCurveStack[recLevel], curveType);

                sidesRight[recLevel] = false;
                recLevel++;
                goLeft();
            }
        }

        // this is a bit of a hack. It returns -1 if we're not on a leaf, and
        // the length of the leaf if we are on a leaf.
        private double onLeaf() {
            final double[] curve = recCurveStack[recLevel];
            final int _curveType = curveType;
            double polyLen = 0.0d;

            double x0 = curve[0], y0 = curve[1];
            for (int i = 2; i < _curveType; i += 2) {
                final double x1 = curve[i], y1 = curve[i + 1];
                final double len = Helpers.linelen(x0, y0, x1, y1);
                polyLen += len;
                curLeafCtrlPolyLengths[(i >> 1) - 1] = len;
                x0 = x1;
                y0 = y1;
            }

            final double lineLen = Helpers.linelen(curve[0], curve[1], x0, y0);

            if ((polyLen - lineLen) < CURVE_LEN_ERR || recLevel == REC_LIMIT) {
                return (polyLen + lineLen) / 2.0d;
            }
            return -1.0d;
        }
    }

    @Override
    public void curveTo(final double x1, final double y1,
                        final double x2, final double y2,
                        final double x3, final double y3)
    {
        final int outcode0 = this.cOutCode;

        if (clipRect != null) {
            final int outcode1 = Helpers.outcode(x1, y1, clipRect);
            final int outcode2 = Helpers.outcode(x2, y2, clipRect);
            final int outcode3 = Helpers.outcode(x3, y3, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1 | outcode2 | outcode3);
            if (orCode != 0) {
                final int sideCode = outcode0 & outcode1 & outcode2 & outcode3;

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        // subdivide curve => callback with subdivided parts:
                        boolean ret = curveSplitter.splitCurve(cx0, cy0, x1, y1, x2, y2, x3, y3,
                                                               orCode, this);
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode3;
                    skipCurveTo(x1, y1, x2, y2, x3, y3);
                    return;
                }
            }

            this.cOutCode = outcode3;

            if (this.outside) {
                this.outside = false;
                // Adjust current index, phase & dash:
                skipLen();
            }
        }
        _curveTo(x1, y1, x2, y2, x3, y3);
    }

    private void _curveTo(final double x1, final double y1,
                          final double x2, final double y2,
                          final double x3, final double y3)
    {
        final double[] _curCurvepts = curCurvepts;

        // monotonize curve:
        final CurveBasicMonotonizer monotonizer
            = rdrCtx.monotonizer.curve(cx0, cy0, x1, y1, x2, y2, x3, y3);

        final int nSplits = monotonizer.nbSplits;
        final double[] mid = monotonizer.middle;

        for (int i = 0, off = 0; i <= nSplits; i++, off += 6) {
            // optimize arraycopy (8 values faster than 6 = type):
            System.arraycopy(mid, off, _curCurvepts, 0, 8);

            somethingTo(8);
        }
    }

    private void skipCurveTo(final double x1, final double y1,
                             final double x2, final double y2,
                             final double x3, final double y3)
    {
        final double[] _curCurvepts = curCurvepts;
        _curCurvepts[0] = cx0; _curCurvepts[1] = cy0;
        _curCurvepts[2] = x1;  _curCurvepts[3] = y1;
        _curCurvepts[4] = x2;  _curCurvepts[5] = y2;
        _curCurvepts[6] = x3;  _curCurvepts[7] = y3;

        skipSomethingTo(8);

        this.cx0 = x3;
        this.cy0 = y3;
    }

    @Override
    public void quadTo(final double x1, final double y1,
                       final double x2, final double y2)
    {
        final int outcode0 = this.cOutCode;

        if (clipRect != null) {
            final int outcode1 = Helpers.outcode(x1, y1, clipRect);
            final int outcode2 = Helpers.outcode(x2, y2, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1 | outcode2);
            if (orCode != 0) {
                final int sideCode = outcode0 & outcode1 & outcode2;

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        // subdivide curve => call lineTo() with subdivided curves:
                        boolean ret = curveSplitter.splitQuad(cx0, cy0, x1, y1,
                                                              x2, y2, orCode, this);
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode2;
                    skipQuadTo(x1, y1, x2, y2);
                    return;
                }
            }

            this.cOutCode = outcode2;

            if (this.outside) {
                this.outside = false;
                // Adjust current index, phase & dash:
                skipLen();
            }
        }
        _quadTo(x1, y1, x2, y2);
    }

    private void _quadTo(final double x1, final double y1,
                         final double x2, final double y2)
    {
        final double[] _curCurvepts = curCurvepts;

        // monotonize quad:
        final CurveBasicMonotonizer monotonizer
            = rdrCtx.monotonizer.quad(cx0, cy0, x1, y1, x2, y2);

        final int nSplits = monotonizer.nbSplits;
        final double[] mid = monotonizer.middle;

        for (int i = 0, off = 0; i <= nSplits; i++, off += 4) {
            // optimize arraycopy (8 values faster than 6 = type):
            System.arraycopy(mid, off, _curCurvepts, 0, 8);

            somethingTo(6);
        }
    }

    private void skipQuadTo(final double x1, final double y1,
                            final double x2, final double y2)
    {
        final double[] _curCurvepts = curCurvepts;
        _curCurvepts[0] = cx0; _curCurvepts[1] = cy0;
        _curCurvepts[2] = x1;  _curCurvepts[3] = y1;
        _curCurvepts[4] = x2;  _curCurvepts[5] = y2;

        skipSomethingTo(6);

        this.cx0 = x2;
        this.cy0 = y2;
    }

    @Override
    public void closePath() {
        if (cx0 != sx0 || cy0 != sy0) {
            lineTo(sx0, sy0);
        }
        if (firstSegidx != 0) {
            if (!dashOn || needsMoveTo) {
                out.moveTo(sx0, sy0);
            }
            emitFirstSegments();
        }
        moveTo(sx0, sy0);
    }

    @Override
    public void pathDone() {
        if (firstSegidx != 0) {
            out.moveTo(sx0, sy0);
            emitFirstSegments();
        }
        out.pathDone();

        // Dispose this instance:
        dispose();
    }

    @Override
    public long getNativeConsumer() {
        throw new InternalError("Dasher does not use a native consumer");
    }
}

