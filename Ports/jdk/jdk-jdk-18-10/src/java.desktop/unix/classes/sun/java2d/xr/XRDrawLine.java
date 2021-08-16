/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Bresenham line-drawing implementation decomposing line segments
 * into a series of rectangles.
 * This is required, because xrender doesn't support line primitives directly.
 * The code here is an almost 1:1 port of the existing C-source contained in
 * sun/java2d/loop/DrawLine.c and sun/java2d/loop/LoopMacros.h
 */
package sun.java2d.xr;

public class XRDrawLine {
    static final int BIG_MAX = ((1 << 29) - 1);
    static final int BIG_MIN = (-(1 << 29));

    static final int OUTCODE_TOP = 1;
    static final int OUTCODE_BOTTOM = 2;
    static final int OUTCODE_LEFT = 4;
    static final int OUTCODE_RIGHT = 8;

    int x1, y1, x2, y2;
    int ucX1, ucY1, ucX2, ucY2;

    DirtyRegion region = new DirtyRegion();

    protected void rasterizeLine(GrowableRectArray rectBuffer, int _x1,
            int _y1, int _x2, int _y2, int cxmin, int cymin, int cxmax,
            int cymax, boolean clip, boolean overflowCheck) {
        float diagF;
        int error;
        int steps;
        int errminor, errmajor;
        boolean xmajor;
        int dx, dy, ax, ay;

        initCoordinates(_x1, _y1, _x2, _y2, overflowCheck);

        dx = x2 - x1;
        dy = y2 - y1;
        ax = Math.abs(dx);
        ay = Math.abs(dy);
        xmajor = (ax >= ay);
        diagF = ((float) ax) / ay;

        if (clip
                && !clipCoordinates(cxmin, cymin, cxmax, cymax, xmajor, dx, dy,
                        ax, ay)) {
            // whole line was clipped away
            return;
        }

        region.setDirtyLineRegion(x1, y1, x2, y2);
        int xDiff = region.x2 - region.x;
        int yDiff = region.y2 - region.y;

        if (xDiff == 0 || yDiff == 0) {
            // horizontal / diagonal lines can be represented by a single
            // rectangle
            rectBuffer.pushRectValues(region.x, region.y, region.x2 - region.x
                    + 1, region.y2 - region.y + 1);
            return;
        }

        // Setup bresenham
        if (xmajor) {
            errmajor = ay * 2;
            errminor = ax * 2;
            ax = -ax; /* For clipping adjustment below */
            steps = x2 - x1;
        } else {
            errmajor = ax * 2;
            errminor = ay * 2;
            ay = -ay; /* For clipping adjustment below */
            steps = y2 - y1;
        }

        if ((steps = (Math.abs(steps) + 1)) == 0) {
            return;
        }

        error = -(errminor / 2);

        if (y1 != ucY1) {
            int ysteps = y1 - ucY1;
            if (ysteps < 0) {
                ysteps = -ysteps;
            }
            error += ysteps * ax * 2;
        }

        if (x1 != ucX1) {
            int xsteps = x1 - ucX1;
            if (xsteps < 0) {
                xsteps = -xsteps;
            }
            error += xsteps * ay * 2;
        }
        error += errmajor;
        errminor -= errmajor;

        int xStep = (dx > 0 ? 1 : -1);
        int yStep = (dy > 0 ? 1 : -1);
        int orthogonalXStep = xmajor ? xStep : 0;
        int orthogonalYStep = !xmajor ? yStep : 0;

        /*
         * For lines which proceed in one direction faster, we try to generate
         * rectangles instead of points. Otherwise we try to avoid the extra
         * work...
         */
        if (diagF <= 0.9 || diagF >= 1.1) {
            lineToRects(rectBuffer, steps, error, errmajor, errminor, xStep,
                    yStep, orthogonalXStep, orthogonalYStep);
        } else {
            lineToPoints(rectBuffer, steps, error, errmajor, errminor, xStep,
                    yStep, orthogonalXStep, orthogonalYStep);
        }
    }

    private void lineToPoints(GrowableRectArray rectBuffer, int steps,
            int error, int errmajor, int errminor, int xStep, int yStep,
            int orthogonalXStep, int orthogonalYStep) {
        int x = x1, y = y1;

        do {
            rectBuffer.pushRectValues(x, y, 1, 1);

            // "Traditional" Bresenham line drawing
            if (error < 0) {
                error += errmajor;
                x += orthogonalXStep;
                y += orthogonalYStep;
            } else {
                error -= errminor;
                x += xStep;
                y += yStep;
            }
        } while (--steps > 0);
    }

    private void lineToRects(GrowableRectArray rectBuffer, int steps,
            int error, int errmajor, int errminor, int xStep, int yStep,
            int orthogonalXStep, int orthogonalYStep) {
        int x = x1, y = y1;
        int rectX = Integer.MIN_VALUE, rectY = 0;
        int rectW = 0, rectH = 0;

        do {
            // Combine the resulting rectangles
            // for steps performed in a single direction.
            if (y == rectY) {
                if (x == (rectX + rectW)) {
                    rectW++;
                } else if (x == (rectX - 1)) {
                    rectX--;
                    rectW++;
                }
            } else if (x == rectX) {
                if (y == (rectY + rectH)) {
                    rectH++;
                } else if (y == (rectY - 1)) {
                    rectY--;
                    rectH++;
                }
            } else {
                // Diagonal step: add the previous rectangle to the list,
                // iff it was "real" (= not initialized before the first
                // iteration)
                if (rectX != Integer.MIN_VALUE) {
                    rectBuffer.pushRectValues(rectX, rectY, rectW, rectH);
                }
                rectX = x;
                rectY = y;
                rectW = rectH = 1;
            }

            // "Traditional" Bresenham line drawing
            if (error < 0) {
                error += errmajor;
                x += orthogonalXStep;
                y += orthogonalYStep;
            } else {
                error -= errminor;
                x += xStep;
                y += yStep;
            }
        } while (--steps > 0);

        // Add last rectangle which isn't handled by the combination-code
        // anymore
        rectBuffer.pushRectValues(rectX, rectY, rectW, rectH);
    }

    private boolean clipCoordinates(int cxmin, int cymin, int cxmax, int cymax,
            boolean xmajor, int dx, int dy, int ax, int ay) {
        int outcode1, outcode2;

        outcode1 = outcode(x1, y1, cxmin, cymin, cxmax, cymax);
        outcode2 = outcode(x2, y2, cxmin, cymin, cxmax, cymax);

        while ((outcode1 | outcode2) != 0) {
            long xsteps = 0, ysteps = 0;

            if ((outcode1 & outcode2) != 0) {
                return false;
            }

            if (outcode1 != 0) {
                if ((outcode1 & (OUTCODE_TOP | OUTCODE_BOTTOM)) != 0) {
                    if ((outcode1 & OUTCODE_TOP) != 0) {
                        y1 = cymin;
                    } else {
                        y1 = cymax;
                    }
                    ysteps = y1 - ucY1;
                    if (ysteps < 0) {
                        ysteps = -ysteps;
                    }
                    xsteps = 2 * ysteps * ax + ay;
                    if (xmajor) {
                        xsteps += ay - ax - 1;
                    }
                    xsteps = xsteps / (2 * ay);
                    if (dx < 0) {
                        xsteps = -xsteps;
                    }
                    x1 = ucX1 + (int) xsteps;
                } else if ((outcode1 & (OUTCODE_LEFT | OUTCODE_RIGHT)) != 0) {
                    if ((outcode1 & OUTCODE_LEFT) != 0) {
                        x1 = cxmin;
                    } else {
                        x1 = cxmax;
                    }
                    xsteps = x1 - ucX1;
                    if (xsteps < 0) {
                        xsteps = -xsteps;
                    }
                    ysteps = 2 * xsteps * ay + ax;
                    if (!xmajor) {
                        ysteps += ax - ay - 1;
                    }
                    ysteps = ysteps / (2 * ax);
                    if (dy < 0) {
                        ysteps = -ysteps;
                    }
                    y1 = ucY1 + (int) ysteps;
                }
                outcode1 = outcode(x1, y1, cxmin, cymin, cxmax, cymax);
            } else {
                if ((outcode2 & (OUTCODE_TOP | OUTCODE_BOTTOM)) != 0) {
                    if ((outcode2 & OUTCODE_TOP) != 0) {
                        y2 = cymin;
                    } else {
                        y2 = cymax;
                    }
                    ysteps = y2 - ucY2;
                    if (ysteps < 0) {
                        ysteps = -ysteps;
                    }
                    xsteps = 2 * ysteps * ax + ay;
                    if (xmajor) {
                        xsteps += ay - ax;
                    } else {
                        xsteps -= 1;
                    }
                    xsteps = xsteps / (2 * ay);
                    if (dx > 0) {
                        xsteps = -xsteps;
                    }
                    x2 = ucX2 + (int) xsteps;
                } else if ((outcode2 & (OUTCODE_LEFT | OUTCODE_RIGHT)) != 0) {
                    if ((outcode2 & OUTCODE_LEFT) != 0) {
                        x2 = cxmin;
                    } else {
                        x2 = cxmax;
                    }
                    xsteps = x2 - ucX2;
                    if (xsteps < 0) {
                        xsteps = -xsteps;
                    }
                    ysteps = 2 * xsteps * ay + ax;
                    if (xmajor) {
                        ysteps -= 1;
                    } else {
                        ysteps += ax - ay;
                    }
                    ysteps = ysteps / (2 * ax);
                    if (dy > 0) {
                        ysteps = -ysteps;
                    }
                    y2 = ucY2 + (int) ysteps;
                }
                outcode2 = outcode(x2, y2, cxmin, cymin, cxmax, cymax);
            }
        }

        return true;
    }

    private void initCoordinates(int x1, int y1, int x2, int y2,
            boolean checkOverflow) {
        /*
         * Part of calculating the Bresenham parameters for line stepping
         * involves being able to store numbers that are twice the magnitude of
         * the biggest absolute difference in coordinates. Since we want the
         * stepping parameters to be stored in jints, we then need to avoid any
         * absolute differences more than 30 bits. Thus, we need to preprocess
         * the coordinates to reduce their range to 30 bits regardless of
         * clipping. We need to cut their range back before we do the clipping
         * because the Bresenham stepping values need to be calculated based on
         * the "unclipped" coordinates.
         *
         * Thus, first we perform a "pre-clipping" stage to bring the
         * coordinates within the 30-bit range and then we proceed to the
         * regular clipping procedure, pretending that these were the original
         * coordinates all along. Since this operation occurs based on a
         * constant "pre-clip" rectangle of +/- 30 bits without any
         * consideration for the final clip, the rounding errors that occur here
         * will depend only on the line coordinates and be invariant with
         * respect to the particular device/user clip rectangles in effect at
         * the time. Thus, rendering a given large-range line will be consistent
         * under a variety of clipping conditions.
         */
        if (checkOverflow
                && (OverflowsBig(x1) || OverflowsBig(y1) || OverflowsBig(x2) || OverflowsBig(y2))) {
            /*
             * Use doubles to get us into range for "Big" arithmetic.
             *
             * The math of adjusting an endpoint for clipping can involve an
             * intermediate result with twice the number of bits as the original
             * coordinate range. Since we want to maintain as much as 30 bits of
             * precision in the resulting coordinates, we will get roundoff here
             * even using IEEE double-precision arithmetic which cannot carry 60
             * bits of mantissa. Since the rounding errors will be consistent
             * for a given set of input coordinates the potential roundoff error
             * should not affect the consistency of our rendering.
             */
            double x1d = x1;
            double y1d = y1;
            double x2d = x2;
            double y2d = y2;
            double dxd = x2d - x1d;
            double dyd = y2d - y1d;

            if (x1 < BIG_MIN) {
                y1d = y1 + (BIG_MIN - x1) * dyd / dxd;
                x1d = BIG_MIN;
            } else if (x1 > BIG_MAX) {
                y1d = y1 - (x1 - BIG_MAX) * dyd / dxd;
                x1d = BIG_MAX;
            }
            /* Use Y1d instead of _y1 for testing now as we may have modified it */
            if (y1d < BIG_MIN) {
                x1d = x1 + (BIG_MIN - y1) * dxd / dyd;
                y1d = BIG_MIN;
            } else if (y1d > BIG_MAX) {
                x1d = x1 - (y1 - BIG_MAX) * dxd / dyd;
                y1d = BIG_MAX;
            }
            if (x2 < BIG_MIN) {
                y2d = y2 + (BIG_MIN - x2) * dyd / dxd;
                x2d = BIG_MIN;
            } else if (x2 > BIG_MAX) {
                y2d = y2 - (x2 - BIG_MAX) * dyd / dxd;
                x2d = BIG_MAX;
            }
            /* Use Y2d instead of _y2 for testing now as we may have modified it */
            if (y2d < BIG_MIN) {
                x2d = x2 + (BIG_MIN - y2) * dxd / dyd;
                y2d = BIG_MIN;
            } else if (y2d > BIG_MAX) {
                x2d = x2 - (y2 - BIG_MAX) * dxd / dyd;
                y2d = BIG_MAX;
            }

            x1 = (int) x1d;
            y1 = (int) y1d;
            x2 = (int) x2d;
            y2 = (int) y2d;
        }

        this.x1 = ucX1 = x1;
        this.y1 = ucY1 = y1;
        this.x2 = ucX2 = x2;
        this.y2 = ucY2 = y2;
    }

    private boolean OverflowsBig(int v) {
        return ((v) != (((v) << 2) >> 2));
    }

    private int out(int v, int vmin, int vmax, int cmin, int cmax) {
        return ((v < vmin) ? cmin : ((v > vmax) ? cmax : 0));
    }

    private int outcode(int x, int y, int xmin, int ymin, int xmax, int ymax) {
        return out(y, ymin, ymax, OUTCODE_TOP, OUTCODE_BOTTOM)
                | out(x, xmin, xmax, OUTCODE_LEFT, OUTCODE_RIGHT);
    }
}
