/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import sun.awt.SunHints;
import java.util.*;

/* This is the java implementation of the native code from
 * src/share/native/sun/java2d/loops/ProcessPath.[c,h]
 * This code is written to be as much similar to the native
 * as it possible. So, it sometimes does not follow java naming conventions.
 *
 * It's important to keep this code synchronized with native one.  See more
 * comments, description and high level scheme of the rendering process in the
 * ProcessPath.c
 */

public class ProcessPath {

    /* Public interfaces and methods for drawing and filling general paths */

    public abstract static class DrawHandler {
        public int xMin;
        public int yMin;
        public int xMax;
        public int yMax;
        public float xMinf;
        public float yMinf;
        public float xMaxf;
        public float yMaxf;

        public int strokeControl;

        public DrawHandler(int xMin, int yMin, int xMax, int yMax,
                           int strokeControl)
        {
            setBounds(xMin, yMin, xMax, yMax, strokeControl);
        }

        public void setBounds(int xMin, int yMin, int xMax, int yMax)
        {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;

            /*                Setting up fractional clipping box
             *
             * We are using following float -> int mapping:
             *
             *      xi = floor(xf + 0.5)
             *
             * So, fractional values that hit the [xmin, xmax) integer interval
             * will be situated inside the [xmin-0.5, xmax - 0.5) fractional
             * interval. We are using EPSF constant to provide that upper
             * boundary is not included.
             */
            xMinf = xMin - 0.5f;
            yMinf = yMin - 0.5f;
            xMaxf = xMax - 0.5f - EPSF;
            yMaxf = yMax - 0.5f - EPSF;
        }

        public void setBounds(int xMin, int yMin, int xMax, int yMax,
                              int strokeControl)
        {
            this.strokeControl = strokeControl;
            setBounds(xMin, yMin, xMax, yMax);
        }

        public void adjustBounds(int bxMin, int byMin, int bxMax, int byMax)
        {
            if (xMin > bxMin) bxMin = xMin;
            if (xMax < bxMax) bxMax = xMax;
            if (yMin > byMin) byMin = yMin;
            if (yMax < byMax) byMax = yMax;
            setBounds(bxMin, byMin, bxMax, byMax);
        }

        public DrawHandler(int xMin, int yMin, int xMax, int yMax) {
            this(xMin, yMin, xMax, yMax, SunHints.INTVAL_STROKE_DEFAULT);
        }

        public abstract void drawLine(int x0, int y0, int x1, int y1);

        public abstract void drawPixel(int x0, int y0);

        public abstract void drawScanline(int x0, int x1, int y0);
    }

    public interface EndSubPathHandler {
        public void processEndSubPath();
    }

    public static final int PH_MODE_DRAW_CLIP = 0;
    public static final int PH_MODE_FILL_CLIP = 1;

    public abstract static class ProcessHandler implements EndSubPathHandler {
        DrawHandler dhnd;
        int clipMode;

        public ProcessHandler(DrawHandler dhnd,
                              int clipMode) {
            this.dhnd = dhnd;
            this.clipMode = clipMode;
        }

        public abstract void processFixedLine(int x1, int y1,
                                              int x2, int y2, int [] pixelInfo,
                                              boolean checkBounds,
                                              boolean endSubPath);
    }

    public static EndSubPathHandler noopEndSubPathHandler =
        new EndSubPathHandler() {
            public void processEndSubPath() { }
        };

    public static boolean fillPath(DrawHandler dhnd, Path2D.Float p2df,
                                   int transX, int transY)
    {
        FillProcessHandler fhnd = new FillProcessHandler(dhnd);
        if (!doProcessPath(fhnd, p2df, transX, transY)) {
            return false;
        }
        FillPolygon(fhnd, p2df.getWindingRule());
        return true;
    }

    public static boolean drawPath(DrawHandler dhnd,
                                   EndSubPathHandler endSubPath,
                                   Path2D.Float p2df,
                                   int transX, int transY)
    {
        return doProcessPath(new DrawProcessHandler(dhnd, endSubPath),
                             p2df, transX, transY);
    }

    public static boolean drawPath(DrawHandler dhnd,
                                   Path2D.Float p2df,
                                   int transX, int transY)
    {
        return doProcessPath(new DrawProcessHandler(dhnd,
                                                    noopEndSubPathHandler),
                             p2df, transX, transY);
    }

    /* Private implementation of the rendering process */

    /* Boundaries used for skipping huge path segments */
    private static final float UPPER_BND = Float.MAX_VALUE/4.0f;
    private static final float LOWER_BND = -UPPER_BND;

    /* Precision (in bits) used in forward differencing */
    private static final int FWD_PREC = 7;

    /* Precision (in bits) used for the rounding in the midpoint test */
    private static final int MDP_PREC = 10;

    private static final int MDP_MULT = 1 << MDP_PREC;
    private static final int MDP_HALF_MULT = MDP_MULT >> 1;

    /* Boundaries used for clipping large path segments (those are inside
     * [UPPER/LOWER]_BND boundaries)
     */
    private static final int UPPER_OUT_BND = 1 << (30 - MDP_PREC);
    private static final int LOWER_OUT_BND = -UPPER_OUT_BND;


    /* Calculation boundaries. They are used for switching to the more slow but
     * allowing larger input values method of calculation of the initial values
     * of the scan converted line segments inside the FillPolygon
     */
    private static final float CALC_UBND = 1 << (30 - MDP_PREC);
    private static final float CALC_LBND = -CALC_UBND;


    /* Following constants are used for providing open boundaries of the
     * intervals
     */
    public static final int EPSFX = 1;
    public static final float EPSF = ((float)EPSFX)/MDP_MULT;

    /* Bit mask used to separate whole part from the fraction part of the
     * number
     */
    private static final int MDP_W_MASK = -MDP_MULT;

    /* Bit mask used to separate fractional part from the whole part of the
     * number
     */
    private static final int MDP_F_MASK = MDP_MULT - 1;

    /*
     *                  Constants for the forward differencing
     *                      of the cubic and quad curves
     */

    /* Maximum size of the cubic curve (calculated as the size of the bounding
     * box of the control points) which could be rendered without splitting
     */
    private static final int MAX_CUB_SIZE = 256;

    /* Maximum size of the quad curve (calculated as the size of the bounding
     * box of the control points) which could be rendered without splitting
     */
    private static final int MAX_QUAD_SIZE = 1024;

    /* Default power of 2 steps used in the forward differencing. Here DF prefix
     * stands for DeFault. Constants below are used as initial values for the
     * adaptive forward differencing algorithm.
     */
    private static final int DF_CUB_STEPS = 3;
    private static final int DF_QUAD_STEPS = 2;

    /* Shift of the current point of the curve for preparing to the midpoint
     * rounding
     */
    private static final int DF_CUB_SHIFT = FWD_PREC + DF_CUB_STEPS*3 -
                                            MDP_PREC;
    private static final int DF_QUAD_SHIFT = FWD_PREC + DF_QUAD_STEPS*2 -
                                             MDP_PREC;

    /* Default amount of steps of the forward differencing */
    private static final int DF_CUB_COUNT = (1<<DF_CUB_STEPS);
    private static final int DF_QUAD_COUNT = (1<<DF_QUAD_STEPS);

    /* Default boundary constants used to check the necessity of the restepping
     */
    private static final int DF_CUB_DEC_BND = 1<<DF_CUB_STEPS*3 + FWD_PREC + 2;
    private static final int DF_CUB_INC_BND = 1<<DF_CUB_STEPS*3 + FWD_PREC - 1;
    private static final int DF_QUAD_DEC_BND = 1<<DF_QUAD_STEPS*2 +
                                                  FWD_PREC + 2;
    private static final int DF_QUAD_INC_BND = 1<<DF_QUAD_STEPS*2 +
                                                  FWD_PREC - 1;

    /* Multipliers for the coefficients of the polynomial form of the cubic and
     * quad curves representation
     */
    private static final int CUB_A_SHIFT = FWD_PREC;
    private static final int CUB_B_SHIFT = (DF_CUB_STEPS + FWD_PREC + 1);
    private static final int CUB_C_SHIFT = (DF_CUB_STEPS*2 + FWD_PREC);

    private static final int CUB_A_MDP_MULT = (1<<CUB_A_SHIFT);
    private static final int CUB_B_MDP_MULT = (1<<CUB_B_SHIFT);
    private static final int CUB_C_MDP_MULT = (1<<CUB_C_SHIFT);

    private static final int QUAD_A_SHIFT = FWD_PREC;
    private static final int QUAD_B_SHIFT = (DF_QUAD_STEPS + FWD_PREC);

    private static final int QUAD_A_MDP_MULT = (1<<QUAD_A_SHIFT);
    private static final int QUAD_B_MDP_MULT = (1<<QUAD_B_SHIFT);

    /* Clipping macros for drawing and filling algorithms */
    private static float CLIP(float a1, float b1, float a2, float b2,
                              double t) {
        return (float)(b1 + (t - a1)*(b2 - b1) / (a2 - a1));
    }

    private static int CLIP(int a1, int b1, int a2, int b2, double t) {
        return (int)(b1 + (t - a1)*(b2 - b1) / (a2 - a1));
    }


    private static final int CRES_MIN_CLIPPED = 0;
    private static final int CRES_MAX_CLIPPED = 1;
    private static final int CRES_NOT_CLIPPED = 3;
    private static final int CRES_INVISIBLE = 4;

    private static boolean IS_CLIPPED(int res) {
        return res == CRES_MIN_CLIPPED || res == CRES_MAX_CLIPPED;
    }

    /* This is java implementation of the macro from ProcessGeneralPath.c.
     * To keep the logic of the java code similar to the native one
     * array and set of indexes are used to point out the data.
     */
    private static int TESTANDCLIP(float LINE_MIN, float LINE_MAX, float[] c,
                                   int a1, int b1, int a2, int b2) {
        double t;
        int res = CRES_NOT_CLIPPED;
        if (c[a1] < (LINE_MIN) || c[a1] > (LINE_MAX)) {
            if (c[a1] < (LINE_MIN)) {
                if (c[a2] < (LINE_MIN)) {
                    return CRES_INVISIBLE;
                };
                res = CRES_MIN_CLIPPED;
                t = (LINE_MIN);
            } else {
                if (c[a2] > (LINE_MAX)) {
                    return CRES_INVISIBLE;
                };
                res = CRES_MAX_CLIPPED;
                t = (LINE_MAX);
            }
            c[b1] = CLIP(c[a1], c[b1], c[a2], c[b2], t);
            c[a1] = (float)t;
        }
        return res;
    }

    /* Integer version of the method above */
    private static int TESTANDCLIP(int LINE_MIN, int LINE_MAX, int[] c,
                                   int a1, int b1, int a2, int b2) {
        double t;
        int res = CRES_NOT_CLIPPED;
        if (c[a1] < (LINE_MIN) || c[a1] > (LINE_MAX)) {
            if (c[a1] < (LINE_MIN)) {
                if (c[a2] < (LINE_MIN)) {
                    return CRES_INVISIBLE;
                };
                res = CRES_MIN_CLIPPED;
                t = (LINE_MIN);
            } else {
                if (c[a2] > (LINE_MAX)) {
                    return CRES_INVISIBLE;
                };
                res = CRES_MAX_CLIPPED;
                t = (LINE_MAX);
            }
            c[b1] = CLIP(c[a1], c[b1], c[a2], c[b2], t);
            c[a1] = (int)t;
        }
        return res;
    }



    /* Following method is used for clipping and clumping filled shapes.
     * An example of this process is shown on the picture below:
     *                      ----+          ----+
     *                    |/    |        |/    |
     *                    +     |        +     |
     *                   /|     |        I     |
     *                  / |     |        I     |
     *                  | |     |  ===>  I     |
     *                  \ |     |        I     |
     *                   \|     |        I     |
     *                    +     |        +     |
     *                    |\    |        |\    |
     *                    | ----+        | ----+
     *                 boundary       boundary
     *
     * We can only perform clipping in case of right side of the output area
     * because all segments passed out the right boundary don't influence on the
     * result of scan conversion algorithm (it correctly handles half open
     * contours).
     *
     * This is java implementation of the macro from ProcessGeneralPath.c.
     * To keep the logic of the java code similar to the native one
     * array and set of indexes are used to point out the data.
     *
     */
    private static int CLIPCLAMP(float LINE_MIN, float LINE_MAX, float[] c,
                                 int a1, int b1, int a2, int b2,
                                 int a3, int b3) {
        c[a3] = c[a1];
        c[b3] = c[b1];
        int res = TESTANDCLIP(LINE_MIN, LINE_MAX, c, a1, b1, a2, b2);
        if (res == CRES_MIN_CLIPPED) {
            c[a3] = c[a1];
        } else if (res == CRES_MAX_CLIPPED) {
            c[a3] = c[a1];
            res = CRES_MAX_CLIPPED;
        } else if (res == CRES_INVISIBLE) {
            if (c[a1] > LINE_MAX) {
                res =  CRES_INVISIBLE;
            } else {
                c[a1] = LINE_MIN;
                c[a2] = LINE_MIN;
                res = CRES_NOT_CLIPPED;
            }
        }
        return res;
    }

    /* Integer version of the method above */
    private static int CLIPCLAMP(int LINE_MIN, int LINE_MAX, int[] c,
                                 int a1, int b1, int a2, int b2,
                                 int a3, int b3) {
        c[a3] = c[a1];
        c[b3] = c[b1];
        int res = TESTANDCLIP(LINE_MIN, LINE_MAX, c, a1, b1, a2, b2);
        if (res == CRES_MIN_CLIPPED) {
            c[a3] = c[a1];
        } else if (res == CRES_MAX_CLIPPED) {
            c[a3] = c[a1];
            res = CRES_MAX_CLIPPED;
        } else if (res == CRES_INVISIBLE) {
            if (c[a1] > LINE_MAX) {
                res =  CRES_INVISIBLE;
            } else {
                c[a1] = LINE_MIN;
                c[a2] = LINE_MIN;
                res = CRES_NOT_CLIPPED;
            }
        }
        return res;
    }

    private static class DrawProcessHandler extends ProcessHandler {

        EndSubPathHandler processESP;

        public DrawProcessHandler(DrawHandler dhnd,
                                  EndSubPathHandler processESP) {
            super(dhnd, PH_MODE_DRAW_CLIP);
            this.dhnd = dhnd;
            this.processESP = processESP;
        }

        public void processEndSubPath() {
            processESP.processEndSubPath();
        }

        void PROCESS_LINE(int fX0, int fY0, int fX1, int fY1,
                          boolean checkBounds, int[] pixelInfo) {
            int X0 = fX0 >> MDP_PREC;
            int Y0 = fY0 >> MDP_PREC;
            int X1 = fX1 >> MDP_PREC;
            int Y1 = fY1 >> MDP_PREC;

           /* Handling lines having just one pixel */
            if (((X0^X1) | (Y0^Y1)) == 0) {
                if (checkBounds &&
                    (dhnd.yMin > Y0  ||
                     dhnd.yMax <= Y0 ||
                     dhnd.xMin > X0  ||
                     dhnd.xMax <= X0)) return;

                if (pixelInfo[0] == 0) {
                    pixelInfo[0] = 1;
                    pixelInfo[1] = X0;
                    pixelInfo[2] = Y0;
                    pixelInfo[3] = X0;
                    pixelInfo[4] = Y0;
                    dhnd.drawPixel(X0, Y0);
                } else if ((X0 != pixelInfo[3] || Y0 != pixelInfo[4]) &&
                           (X0 != pixelInfo[1] || Y0 != pixelInfo[2])) {
                    dhnd.drawPixel(X0, Y0);
                    pixelInfo[3] = X0;
                    pixelInfo[4] = Y0;
                }
                return;
            }

            if (!checkBounds ||
                (dhnd.yMin <= Y0  &&
                 dhnd.yMax > Y0 &&
                 dhnd.xMin <= X0  &&
                 dhnd.xMax > X0))
            {
                /* Switch off first pixel of the line before drawing */
                if (pixelInfo[0] == 1 &&
                    ((pixelInfo[1] == X0 && pixelInfo[2] == Y0) ||
                     (pixelInfo[3] == X0 && pixelInfo[4] == Y0)))
                {
                    dhnd.drawPixel(X0, Y0);
                }
            }

            dhnd.drawLine(X0, Y0, X1, Y1);

            if (pixelInfo[0] == 0) {
                pixelInfo[0] = 1;
                pixelInfo[1] = X0;
                pixelInfo[2] = Y0;
                pixelInfo[3] = X0;
                pixelInfo[4] = Y0;
            }

            /* Switch on last pixel of the line if it was already
             * drawn during rendering of the previous segments
             */
            if ((pixelInfo[1] == X1 && pixelInfo[2] == Y1) ||
                (pixelInfo[3] == X1 && pixelInfo[4] == Y1))
            {
                if (checkBounds &&
                    (dhnd.yMin > Y1  ||
                     dhnd.yMax <= Y1 ||
                     dhnd.xMin > X1  ||
                     dhnd.xMax <= X1)) {
                    return;
                }

                dhnd.drawPixel(X1, Y1);
            }
            pixelInfo[3] = X1;
            pixelInfo[4] = Y1;
        }

        void PROCESS_POINT(int fX, int fY, boolean checkBounds,
                           int[] pixelInfo) {
            int _X = fX>> MDP_PREC;
            int _Y = fY>> MDP_PREC;
            if (checkBounds &&
                (dhnd.yMin > _Y  ||
                 dhnd.yMax <= _Y ||
                 dhnd.xMin > _X  ||
                 dhnd.xMax <= _X)) return;
            /*
             *  (_X,_Y) should be inside boundaries
             *
             *  assert(dhnd.yMin <= _Y &&
             *         dhnd.yMax >  _Y &&
             *         dhnd.xMin <= _X &&
             *         dhnd.xMax >  _X);
             *
             */
            if (pixelInfo[0] == 0) {
                pixelInfo[0] = 1;
                pixelInfo[1] = _X;
                pixelInfo[2] = _Y;
                pixelInfo[3] = _X;
                pixelInfo[4] = _Y;
                dhnd.drawPixel(_X, _Y);
            } else if ((_X != pixelInfo[3] || _Y != pixelInfo[4]) &&
                       (_X != pixelInfo[1] || _Y != pixelInfo[2])) {
                dhnd.drawPixel(_X, _Y);
                pixelInfo[3] = _X;
                pixelInfo[4] = _Y;
            }
        }

        /*                  Drawing line with subpixel endpoints
         *
         * (x1, y1), (x2, y2) -  fixed point coordinates of the endpoints
         *                       with MDP_PREC bits for the fractional part
         *
         * pixelInfo          -  structure which keeps drawing info for avoiding
         *                       multiple drawing at the same position on the
         *                       screen (required for the XOR mode of drawing)
         *
         *                          pixelInfo[0]   - state of the drawing
         *                                           0 - no pixel drawn between
         *                                           moveTo/close of the path
         *                                           1 - there are drawn pixels
         *
         *                          pixelInfo[1,2] - first pixel of the path
         *                                           between moveTo/close of the
         *                                           path
         *
         *                          pixelInfo[3,4] - last drawn pixel between
         *                                           moveTo/close of the path
         *
         * checkBounds        - flag showing necessity of checking the clip
         *
         */
        public void  processFixedLine(int x1, int y1, int x2, int y2,
                                      int[] pixelInfo, boolean checkBounds,
                                      boolean endSubPath)  {

            /* Checking if line is inside a (X,Y),(X+MDP_MULT,Y+MDP_MULT) box */
            int c = ((x1 ^ x2) | (y1 ^ y2));
            int rx1, ry1, rx2, ry2;
            if ((c & MDP_W_MASK) == 0) {
                /* Checking for the segments with integer coordinates having
                 * the same start and end points
                 */
                if (c == 0) {
                    PROCESS_POINT(x1 + MDP_HALF_MULT, y1 + MDP_HALF_MULT,
                                  checkBounds, pixelInfo);
                }
                return;
            }

            if (x1 == x2 || y1 == y2) {
                rx1 = x1 + MDP_HALF_MULT;
                rx2 = x2 + MDP_HALF_MULT;
                ry1 = y1 + MDP_HALF_MULT;
                ry2 = y2 + MDP_HALF_MULT;
            } else {
                /* Neither dx nor dy can be zero because of the check above */
                int dx = x2 - x1;
                int dy = y2 - y1;

                /* Floor of x1, y1, x2, y2 */
                int fx1 = x1 & MDP_W_MASK;
                int fy1 = y1 & MDP_W_MASK;
                int fx2 = x2 & MDP_W_MASK;
                int fy2 = y2 & MDP_W_MASK;

                /* Processing first endpoint */
                if (fx1 == x1 || fy1 == y1) {
                    /* Adding MDP_HALF_MULT to the [xy]1 if f[xy]1 == [xy]1
                     * will not affect the result
                     */
                    rx1 = x1 + MDP_HALF_MULT;
                    ry1 = y1 + MDP_HALF_MULT;
                } else {
                    /* Boundary at the direction from (x1,y1) to (x2,y2) */
                    int bx1 = (x1 < x2) ? fx1 + MDP_MULT : fx1;
                    int by1 = (y1 < y2) ? fy1 + MDP_MULT : fy1;

                    /* intersection with column bx1 */
                    int cross = y1 + ((bx1 - x1)*dy)/dx;
                    if (cross >= fy1 && cross <= fy1 + MDP_MULT) {
                        rx1 = bx1;
                        ry1 = cross + MDP_HALF_MULT;
                    } else {
                        /* intersection with row by1 */
                        cross = x1 + ((by1 - y1)*dx)/dy;
                        rx1 = cross + MDP_HALF_MULT;
                        ry1 = by1;
                    }
                }

                /* Processing second endpoint */
                if (fx2 == x2 || fy2 == y2) {
                    /* Adding MDP_HALF_MULT to the [xy]2 if f[xy]2 == [xy]2
                     * will not affect the result
                     */
                    rx2 = x2 + MDP_HALF_MULT;
                    ry2 = y2 + MDP_HALF_MULT;
                } else {
                    /* Boundary at the direction from (x2,y2) to (x1,y1) */
                    int bx2 = (x1 > x2) ? fx2 + MDP_MULT : fx2;
                    int by2 = (y1 > y2) ? fy2 + MDP_MULT : fy2;

                    /* intersection with column bx2 */
                    int cross = y2 + ((bx2 - x2)*dy)/dx;
                    if (cross >= fy2 && cross <= fy2 + MDP_MULT) {
                        rx2 = bx2;
                        ry2 = cross + MDP_HALF_MULT;
                    } else {
                        /* intersection with row by2 */
                        cross = x2 + ((by2 - y2)*dx)/dy;
                        rx2 = cross + MDP_HALF_MULT;
                        ry2 = by2;
                    }
                }
            }
            PROCESS_LINE(rx1, ry1, rx2, ry2, checkBounds, pixelInfo);
        }
    }

    /* Performing drawing of the monotonic in X and Y quadratic curves with
     * sizes less than MAX_QUAD_SIZE by using forward differencing method of
     * calculation. See comments to the DrawMonotonicQuad in the
     * ProcessGeneralPath.c
     */
    private static void DrawMonotonicQuad(ProcessHandler hnd,
                                          float[] coords,
                                          boolean checkBounds,
                                          int[] pixelInfo) {

        int x0 = (int)(coords[0]*MDP_MULT);
        int y0 = (int)(coords[1]*MDP_MULT);

        int xe = (int)(coords[4]*MDP_MULT);
        int ye = (int)(coords[5]*MDP_MULT);

        /* Extracting fractional part of coordinates of first control point */
        int px = (x0 & (~MDP_W_MASK)) << DF_QUAD_SHIFT;
        int py = (y0 & (~MDP_W_MASK)) << DF_QUAD_SHIFT;

        /* Setting default amount of steps */
        int count = DF_QUAD_COUNT;

        /* Setting default shift for preparing to the midpoint rounding */
        int shift =  DF_QUAD_SHIFT;

        int ax = (int)((coords[0] - 2*coords[2] +
                         coords[4])*QUAD_A_MDP_MULT);
        int ay = (int)((coords[1] - 2*coords[3] +
                         coords[5])*QUAD_A_MDP_MULT);

        int bx = (int)((-2*coords[0] + 2*coords[2])*QUAD_B_MDP_MULT);
        int by = (int)((-2*coords[1] + 2*coords[3])*QUAD_B_MDP_MULT);

        int ddpx = 2*ax;
        int ddpy = 2*ay;

        int dpx = ax + bx;
        int dpy = ay + by;

        int x1, y1;

        int x2 = x0;
        int y2 = y0;

        int maxDD = Math.max(Math.abs(ddpx),Math.abs(ddpy));

        int dx = xe - x0;
        int dy = ye - y0;

        int x0w = x0 & MDP_W_MASK;
        int y0w = y0 & MDP_W_MASK;

        /* Perform decreasing step in 2 times if slope of the first forward
         * difference changes too quickly (more than a pixel per step in X or Y
         * direction).  We can perform adjusting of the step size before the
         * rendering loop because the curvature of the quad curve remains the
         * same along all the curve
         */
        while (maxDD > DF_QUAD_DEC_BND) {
            dpx = (dpx<<1) - ax;
            dpy = (dpy<<1) - ay;
            count <<= 1;
            maxDD >>= 2;
            px <<=2;
            py <<=2;
            shift += 2;
        }

        while(count-- > 1) {
            px += dpx;
            py += dpy;

            dpx += ddpx;
            dpy += ddpy;

            x1 = x2;
            y1 = y2;

            x2 = x0w + (px >> shift);
            y2 = y0w + (py >> shift);

            /* Checking that we are not running out of the endpoint and bounding
             * violating coordinate.  The check is pretty simple because the
             * curve passed to the DrawCubic already split into the
             * monotonic in X and Y pieces
             */

            /* Bounding x2 by xe */
            if (((xe-x2)^dx) < 0) {
                x2 = xe;
            }

            /* Bounding y2 by ye */
            if (((ye-y2)^dy) < 0) {
                y2 = ye;
            }

            hnd.processFixedLine(x1, y1, x2, y2, pixelInfo, checkBounds, false);
        }

        /* We are performing one step less than necessary and use actual
         * (xe,ye) endpoint of the curve instead of calculated. This prevent us
         * from running above the curve endpoint due to the accumulated errors
         * during the iterations.
         */

        hnd.processFixedLine(x2, y2, xe, ye, pixelInfo, checkBounds, false);
    }

    /*
     * Checking size of the quad curves and split them if necessary.
     * Calling DrawMonotonicQuad for the curves of the appropriate size.
     * Note: coords array could be changed
     */
    private static void ProcessMonotonicQuad(ProcessHandler hnd,
                                             float[] coords,
                                             int[] pixelInfo) {

        float[] coords1 = new float[6];
        float tx, ty;
        float xMin, yMin, xMax, yMax;

        xMin = xMax = coords[0];
        yMin = yMax = coords[1];
        for (int i = 2; i < 6; i += 2) {
            xMin = (xMin > coords[i])? coords[i] : xMin;
            xMax = (xMax < coords[i])? coords[i] : xMax;
            yMin = (yMin > coords[i + 1])? coords[i + 1] : yMin;
            yMax = (yMax < coords[i + 1])? coords[i + 1] : yMax;
        }

        if (hnd.clipMode == PH_MODE_DRAW_CLIP) {

           /* In case of drawing we could just skip curves which are
            * completely out of bounds
            */
           if (hnd.dhnd.xMaxf < xMin || hnd.dhnd.xMinf > xMax ||
               hnd.dhnd.yMaxf < yMin || hnd.dhnd.yMinf > yMax) {
               return;
           }
        } else {

            /* In case of filling we could skip curves which are above,
             * below and behind the right boundary of the visible area
             */

            if (hnd.dhnd.yMaxf < yMin || hnd.dhnd.yMinf > yMax ||
                hnd.dhnd.xMaxf < xMin)
            {
                return;
            }

            /* We could clamp x coordinates to the corresponding boundary
             * if the curve is completely behind the left one
             */

            if (hnd.dhnd.xMinf > xMax) {
                coords[0] = coords[2] = coords[4] = hnd.dhnd.xMinf;
            }
        }

        if (xMax - xMin > MAX_QUAD_SIZE || yMax - yMin > MAX_QUAD_SIZE) {
            coords1[4] = coords[4];
            coords1[5] = coords[5];
            coords1[2] = (coords[2] + coords[4])/2.0f;
            coords1[3] = (coords[3] + coords[5])/2.0f;
            coords[2] = (coords[0] + coords[2])/2.0f;
            coords[3] = (coords[1] + coords[3])/2.0f;
            coords[4] = coords1[0] = (coords[2] + coords1[2])/2.0f;
            coords[5] = coords1[1] = (coords[3] + coords1[3])/2.0f;

            ProcessMonotonicQuad(hnd, coords, pixelInfo);

            ProcessMonotonicQuad(hnd, coords1, pixelInfo);
        } else {
            DrawMonotonicQuad(hnd, coords,
                              /* Set checkBounds parameter if curve intersects
                               * boundary of the visible area. We know that the
                               * curve is visible, so the check is pretty
                               * simple
                               */
                              hnd.dhnd.xMinf >= xMin ||
                              hnd.dhnd.xMaxf <= xMax ||
                              hnd.dhnd.yMinf >= yMin ||
                              hnd.dhnd.yMaxf <= yMax,
                              pixelInfo);
        }
    }

    /*
     * Split quadratic curve into monotonic in X and Y parts. Calling
     * ProcessMonotonicQuad for each monotonic piece of the curve.
     * Note: coords array could be changed
     */
    private static void ProcessQuad(ProcessHandler hnd, float[] coords,
                                    int[] pixelInfo) {
        /* Temporary array for holding parameters corresponding to the extreme
         * in X and Y points
         */
        double[] params = new double[2];
        int cnt = 0;
        double param;

        /* Simple check for monotonicity in X before searching for the extreme
         * points of the X(t) function. We first check if the curve is
         * monotonic in X by seeing if all of the X coordinates are strongly
         * ordered.
         */
        if ((coords[0] > coords[2] || coords[2] > coords[4]) &&
            (coords[0] < coords[2] || coords[2] < coords[4]))
        {
            /* Searching for extreme points of the X(t) function  by solving
             * dX(t)
             * ----  = 0 equation
             *  dt
             */
            double ax = coords[0] - 2*coords[2] + coords[4];
            if (ax != 0) {
                /* Calculating root of the following equation
                 * ax*t + bx = 0
                 */
                double bx = coords[0] - coords[2];

                param = bx/ax;
                if (param < 1.0 && param > 0.0) {
                    params[cnt++] = param;
                }
            }
        }

        /* Simple check for monotonicity in Y before searching for the extreme
         * points of the Y(t) function. We first check if the curve is
         * monotonic in Y by seeing if all of the Y coordinates are strongly
         * ordered.
         */
        if ((coords[1] > coords[3] || coords[3] > coords[5]) &&
            (coords[1] < coords[3] || coords[3] < coords[5]))
        {
            /* Searching for extreme points of the Y(t) function by solving
             * dY(t)
             * ----- = 0 equation
             *  dt
             */
            double ay = coords[1] - 2*coords[3] + coords[5];

            if (ay != 0) {
                /* Calculating root of the following equation
                 * ay*t + by = 0
                 */
                double by = coords[1] - coords[3];

                param = by/ay;
                if (param < 1.0 && param > 0.0) {
                    if (cnt > 0) {
                        /* Inserting parameter only if it differs from
                         * already stored
                         */
                        if (params[0] >  param) {
                            params[cnt++] = params[0];
                            params[0] = param;
                        } else if (params[0] <  param) {
                            params[cnt++] = param;
                        }
                    } else {
                        params[cnt++] = param;
                    }
                }
            }
        }

        /* Processing obtained monotonic parts */
        switch(cnt) {
            case 0:
                break;
            case 1:
                ProcessFirstMonotonicPartOfQuad(hnd, coords, pixelInfo,
                                                (float)params[0]);
                break;
            case 2:
                ProcessFirstMonotonicPartOfQuad(hnd, coords, pixelInfo,
                                                (float)params[0]);
                param = params[1] - params[0];
                if (param > 0) {
                    ProcessFirstMonotonicPartOfQuad(hnd, coords, pixelInfo,
                                           /* Scale parameter to match with
                                            * rest of the curve
                                            */
                                           (float)(param/(1.0 - params[0])));
                }
                break;
        }

        ProcessMonotonicQuad(hnd,coords,pixelInfo);
    }

    /*
     * Bite the piece of the quadratic curve from start point till the point
     * corresponding to the specified parameter then call ProcessQuad for the
     * bitten part.
     * Note: coords array will be changed
     */
    private static void ProcessFirstMonotonicPartOfQuad(ProcessHandler hnd,
                                                        float[] coords,
                                                        int[] pixelInfo,
                                                        float t) {
        float[] coords1 = new float[6];

        coords1[0] = coords[0];
        coords1[1] = coords[1];
        coords1[2] = coords[0] + t*(coords[2] - coords[0]);
        coords1[3] = coords[1] + t*(coords[3] - coords[1]);
        coords[2] = coords[2] + t*(coords[4] - coords[2]);
        coords[3] = coords[3] + t*(coords[5] - coords[3]);
        coords[0] = coords1[4] = coords1[2] + t*(coords[2] - coords1[2]);
        coords[1] = coords1[5] = coords1[3] + t*(coords[3] - coords1[3]);

        ProcessMonotonicQuad(hnd, coords1, pixelInfo);
    }

    /* Performing drawing of the monotonic in X and Y cubic curves with sizes
     * less than MAX_CUB_SIZE by using forward differencing method of
     * calculation.  See comments to the DrawMonotonicCubic in the
     * ProcessGeneralPath.c
     */
    private static void DrawMonotonicCubic(ProcessHandler hnd,
                                           float[] coords,
                                           boolean checkBounds,
                                           int[] pixelInfo) {
        int x0 = (int)(coords[0]*MDP_MULT);
        int y0 = (int)(coords[1]*MDP_MULT);

        int xe = (int)(coords[6]*MDP_MULT);
        int ye = (int)(coords[7]*MDP_MULT);

        /* Extracting fractional part of coordinates of first control point */
        int px = (x0 & (~MDP_W_MASK)) << DF_CUB_SHIFT;
        int py = (y0 & (~MDP_W_MASK)) << DF_CUB_SHIFT;

        /* Setting default boundary values for checking first and second forward
         * difference for the necessity of the restepping. See comments to the
         * boundary values in ProcessQuad for more info.
         */
        int incStepBnd = DF_CUB_INC_BND;
        int decStepBnd = DF_CUB_DEC_BND;

        /* Setting default amount of steps */
        int count = DF_CUB_COUNT;

        /* Setting default shift for preparing to the midpoint rounding */
        int shift =  DF_CUB_SHIFT;

        int ax = (int)((-coords[0] + 3*coords[2] - 3*coords[4] +
                 coords[6])*CUB_A_MDP_MULT);
        int ay = (int)((-coords[1] + 3*coords[3] - 3*coords[5] +
                 coords[7])*CUB_A_MDP_MULT);

        int bx = (int)((3*coords[0] - 6*coords[2] +
                 3*coords[4])*CUB_B_MDP_MULT);
        int by = (int)((3*coords[1] - 6*coords[3] +
                 3*coords[5])*CUB_B_MDP_MULT);

        int cx = (int)((-3*coords[0] + 3*coords[2])*(CUB_C_MDP_MULT));
        int cy = (int)((-3*coords[1] + 3*coords[3])*(CUB_C_MDP_MULT));

        int dddpx = 6*ax;
        int dddpy = 6*ay;

        int ddpx = dddpx + bx;
        int ddpy = dddpy + by;

        int dpx = ax + (bx>>1) + cx;
        int dpy = ay + (by>>1) + cy;

        int x1, y1;

        int x2 = x0;
        int y2 = y0;

        /* Calculating whole part of the first point of the curve */
        int x0w = x0 & MDP_W_MASK;
        int y0w = y0 & MDP_W_MASK;

        int dx = xe - x0;
        int dy = ye - y0;

        while (count > 0) {
            /* Perform decreasing step in 2 times if necessary */
            while (Math.abs(ddpx) > decStepBnd ||
                   Math.abs(ddpy) > decStepBnd) {
                ddpx = (ddpx<<1) - dddpx;
                ddpy = (ddpy<<1) - dddpy;
                dpx = (dpx<<2) - (ddpx>>1);
                dpy = (dpy<<2) - (ddpy>>1);
                count <<=1;
                decStepBnd <<=3;
                incStepBnd <<=3;
                px <<=3;
                py <<=3;
                shift += 3;
            }

            /* Perform increasing step in 2 times if necessary.
             * Note: we could do it only in even steps
             */

            while ((count & 1) == 0 && shift > DF_CUB_SHIFT &&
                   Math.abs(dpx) <= incStepBnd &&
                   Math.abs(dpy) <= incStepBnd) {
                dpx = (dpx>>2) + (ddpx>>3);
                dpy = (dpy>>2) + (ddpy>>3);
                ddpx = (ddpx + dddpx)>>1;
                ddpy = (ddpy + dddpy)>>1;
                count >>=1;
                decStepBnd >>=3;
                incStepBnd >>=3;
                px >>=3;
                py >>=3;
                shift -= 3;
            }

            count--;

            /* Performing one step less than necessary and use actual (xe,ye)
             * curve's endpoint instead of calculated. This prevent us from
             * running above the curve endpoint due to the accumulated errors
             * during the iterations.
             */
            if (count > 0) {
                px += dpx;
                py += dpy;

                dpx += ddpx;
                dpy += ddpy;
                ddpx += dddpx;
                ddpy += dddpy;

                x1 = x2;
                y1 = y2;

                x2 = x0w + (px >> shift);
                y2 = y0w + (py >> shift);

                /* Checking that we are not running out of the endpoint and
                 * bounding violating coordinate.  The check is pretty simple
                 * because the curve passed to the DrawCubic already split
                 * into the monotonic in X and Y pieces
                 */

                /* Bounding x2 by xe */
                if (((xe-x2)^dx) < 0) {
                    x2 = xe;
                }

                /* Bounding y2 by ye */
                if (((ye-y2)^dy) < 0) {
                    y2 = ye;
                }

                hnd.processFixedLine(x1, y1, x2, y2, pixelInfo, checkBounds,
                                     false);
            } else {
                hnd.processFixedLine(x2, y2, xe, ye, pixelInfo, checkBounds,
                                     false);
            }
        }
    }

    /*
     * Checking size of the cubic curves and split them if necessary.
     * Calling DrawMonotonicCubic for the curves of the appropriate size.
     * Note: coords array could be changed
     */
    private static void ProcessMonotonicCubic(ProcessHandler hnd,
                                              float[] coords,
                                              int[] pixelInfo) {

        float[] coords1 = new float[8];
        float tx, ty;
        float xMin, xMax;
        float yMin, yMax;

        xMin = xMax = coords[0];
        yMin = yMax = coords[1];

        for (int i = 2; i < 8; i += 2) {
            xMin = (xMin > coords[i])? coords[i] : xMin;
            xMax = (xMax < coords[i])? coords[i] : xMax;
            yMin = (yMin > coords[i + 1])? coords[i + 1] : yMin;
            yMax = (yMax < coords[i + 1])? coords[i + 1] : yMax;
        }

        if (hnd.clipMode == PH_MODE_DRAW_CLIP) {
            /* In case of drawing we could just skip curves which are
             * completely out of bounds
             */
            if (hnd.dhnd.xMaxf < xMin || hnd.dhnd.xMinf > xMax ||
                hnd.dhnd.yMaxf < yMin || hnd.dhnd.yMinf > yMax) {
                return;
            }
        } else {

            /* In case of filling we could skip curves which are above,
             * below and behind the right boundary of the visible area
             */

            if (hnd.dhnd.yMaxf < yMin || hnd.dhnd.yMinf > yMax ||
                hnd.dhnd.xMaxf < xMin)
            {
                return;
            }

            /* We could clamp x coordinates to the corresponding boundary
             * if the curve is completely behind the left one
             */

            if (hnd.dhnd.xMinf > xMax) {
                coords[0] = coords[2] = coords[4] = coords[6] =
                    hnd.dhnd.xMinf;
            }
        }

        if (xMax - xMin > MAX_CUB_SIZE || yMax - yMin > MAX_CUB_SIZE) {
            coords1[6] = coords[6];
            coords1[7] = coords[7];
            coords1[4] = (coords[4] + coords[6])/2.0f;
            coords1[5] = (coords[5] + coords[7])/2.0f;
            tx = (coords[2] + coords[4])/2.0f;
            ty = (coords[3] + coords[5])/2.0f;
            coords1[2] = (tx + coords1[4])/2.0f;
            coords1[3] = (ty + coords1[5])/2.0f;
            coords[2] =  (coords[0] + coords[2])/2.0f;
            coords[3] =  (coords[1] + coords[3])/2.0f;
            coords[4] = (coords[2] + tx)/2.0f;
            coords[5] = (coords[3] + ty)/2.0f;
            coords[6]=coords1[0]=(coords[4] + coords1[2])/2.0f;
            coords[7]=coords1[1]=(coords[5] + coords1[3])/2.0f;

            ProcessMonotonicCubic(hnd, coords, pixelInfo);

            ProcessMonotonicCubic(hnd, coords1, pixelInfo);
        } else {
            DrawMonotonicCubic(hnd, coords,
                               /* Set checkBounds parameter if curve intersects
                                * boundary of the visible area. We know that
                                * the curve is visible, so the check is pretty
                                * simple
                                */
                                hnd.dhnd.xMinf > xMin ||
                                hnd.dhnd.xMaxf < xMax ||
                                hnd.dhnd.yMinf > yMin ||
                                hnd.dhnd.yMaxf < yMax,
                                pixelInfo);
        }
    }

    /*
     * Split cubic curve into monotonic in X and Y parts. Calling
     * ProcessMonotonicCubic for each monotonic piece of the curve.
     *
     * Note: coords array could be changed
     */
    private static void ProcessCubic(ProcessHandler hnd,
                                     float[] coords,
                                     int[] pixelInfo) {
        /* Temporary array for holding parameters corresponding to the extreme
         * in X and Y points
         */
        double[] params = new double[4];
        double[] eqn = new double[3];
        double[] res = new double[2];
        int cnt = 0;

        /* Simple check for monotonicity in X before searching for the extreme
         * points of the X(t) function. We first check if the curve is
         * monotonic in X by seeing if all of the X coordinates are strongly
         * ordered.
         */
        if ((coords[0] > coords[2] || coords[2] > coords[4] ||
             coords[4] > coords[6]) &&
            (coords[0] < coords[2] || coords[2] < coords[4] ||
             coords[4] < coords[6]))
        {
            /* Searching for extreme points of the X(t) function  by solving
             * dX(t)
             * ----  = 0 equation
             *  dt
             */
            eqn[2] = -coords[0] + 3*coords[2] - 3*coords[4] + coords[6];
            eqn[1] = 2*(coords[0] - 2*coords[2] + coords[4]);
            eqn[0] = -coords[0] + coords[2];

            int nr = QuadCurve2D.solveQuadratic(eqn, res);

            /* Following code also correctly works in degenerate case of
             * the quadratic equation (nr = -1) because we do not need
             * splitting in this case.
             */
            for (int i = 0; i < nr; i++) {
                if (res[i] > 0 && res[i] < 1) {
                    params[cnt++] = res[i];
                }
            }
        }

        /* Simple check for monotonicity in Y before searching for the extreme
         * points of the Y(t) function. We first check if the curve is
         * monotonic in Y by seeing if all of the Y coordinates are strongly
         * ordered.
         */
        if ((coords[1] > coords[3] || coords[3] > coords[5] ||
             coords[5] > coords[7]) &&
            (coords[1] < coords[3] || coords[3] < coords[5] ||
             coords[5] < coords[7]))
        {
            /* Searching for extreme points of the Y(t) function by solving
             * dY(t)
             * ----- = 0 equation
             *  dt
             */
            eqn[2] = -coords[1] + 3*coords[3] - 3*coords[5] + coords[7];
            eqn[1] = 2*(coords[1] - 2*coords[3] + coords[5]);
            eqn[0] = -coords[1] + coords[3];

            int nr = QuadCurve2D.solveQuadratic(eqn, res);

            /* Following code also correctly works in degenerate case of
             * the quadratic equation (nr = -1) because we do not need
             * splitting in this case.
             */
            for (int i = 0; i < nr; i++) {
                if (res[i] > 0 && res[i] < 1) {
                    params[cnt++] = res[i];
                }
            }
        }

        if (cnt > 0) {
            /* Sorting parameter values corresponding to the extreme points
             * of the curve
             */
            Arrays.sort(params, 0, cnt);

            /* Processing obtained monotonic parts */
            ProcessFirstMonotonicPartOfCubic(hnd, coords, pixelInfo,
                                             (float)params[0]);
            for (int i = 1; i < cnt; i++) {
                double param = params[i] - params[i-1];
                if (param > 0) {
                    ProcessFirstMonotonicPartOfCubic(hnd, coords, pixelInfo,
                        /* Scale parameter to match with rest of the curve */
                        (float)(param/(1.0 - params[i - 1])));
                }
            }
        }

        ProcessMonotonicCubic(hnd,coords,pixelInfo);
    }

    /*
     * Bite the piece of the cubic curve from start point till the point
     * corresponding to the specified parameter then call ProcessCubic for the
     * bitten part.
     * Note: coords array will be changed
     */
    private static void ProcessFirstMonotonicPartOfCubic(ProcessHandler hnd,
                                                         float[] coords,
                                                         int[] pixelInfo,
                                                         float t)
    {
        float[] coords1 = new float[8];
        float tx, ty;

        coords1[0] = coords[0];
        coords1[1] = coords[1];
        tx = coords[2] + t*(coords[4] - coords[2]);
        ty = coords[3] + t*(coords[5] - coords[3]);
        coords1[2] =  coords[0] + t*(coords[2] - coords[0]);
        coords1[3] =  coords[1] + t*(coords[3] - coords[1]);
        coords1[4] = coords1[2] + t*(tx - coords1[2]);
        coords1[5] = coords1[3] + t*(ty - coords1[3]);
        coords[4] = coords[4] + t*(coords[6] - coords[4]);
        coords[5] = coords[5] + t*(coords[7] - coords[5]);
        coords[2] = tx + t*(coords[4] - tx);
        coords[3] = ty + t*(coords[5] - ty);
        coords[0]=coords1[6]=coords1[4] + t*(coords[2] - coords1[4]);
        coords[1]=coords1[7]=coords1[5] + t*(coords[3] - coords1[5]);

        ProcessMonotonicCubic(hnd, coords1, pixelInfo);
    }

    /* Note:
     * For more easy reading of the code below each java version of the macros
     * from the ProcessPath.c preceded by the commented origin call
     * containing verbose names of the parameters
     */
    private static void ProcessLine(ProcessHandler hnd, float x1, float y1,
                                    float x2, float y2, int[] pixelInfo) {
        float xMin, yMin, xMax, yMax;
        int X1, Y1, X2, Y2, X3, Y3, res;
        boolean clipped = false;
        float x3,y3;
        float[] c = new float[]{x1, y1, x2, y2, 0, 0};

        boolean lastClipped;

        xMin = hnd.dhnd.xMinf;
        yMin = hnd.dhnd.yMinf;
        xMax = hnd.dhnd.xMaxf;
        yMax = hnd.dhnd.yMaxf;

        //
        // TESTANDCLIP(yMin, yMax, y1, x1, y2, x2, res);
        //
        res = TESTANDCLIP(yMin, yMax, c, 1, 0, 3, 2);
        if (res == CRES_INVISIBLE) return;
        clipped = IS_CLIPPED(res);
        //
        // TESTANDCLIP(yMin, yMax, y2, x2, y1, x1, res);
        //
        res = TESTANDCLIP(yMin, yMax, c, 3, 2, 1, 0);
        if (res == CRES_INVISIBLE) return;
        lastClipped = IS_CLIPPED(res);
        clipped = clipped || lastClipped;

        if (hnd.clipMode == PH_MODE_DRAW_CLIP) {
            //
            // TESTANDCLIP(xMin, xMax, x1, y1, x2, y2, res);
            //
            res = TESTANDCLIP(xMin, xMax, c, 0, 1, 2, 3);
            if (res == CRES_INVISIBLE) return;
            clipped = clipped || IS_CLIPPED(res);
            //
            // TESTANDCLIP(xMin, xMax, x2, y2, x1, y1, res);
            //
            res = TESTANDCLIP(xMin, xMax, c, 2, 3, 0, 1);
            if (res == CRES_INVISIBLE) return;
            lastClipped = lastClipped || IS_CLIPPED(res);
            clipped = clipped || lastClipped;
            X1 = (int)(c[0]*MDP_MULT);
            Y1 = (int)(c[1]*MDP_MULT);
            X2 = (int)(c[2]*MDP_MULT);
            Y2 = (int)(c[3]*MDP_MULT);

            hnd.processFixedLine(X1, Y1, X2, Y2, pixelInfo,
                                 clipped, /* enable boundary checking in
                                             case of clipping to avoid
                                             entering out of bounds which
                                             could happens during rounding
                                           */
                                 lastClipped /* Notify pProcessFixedLine
                                                that
                                                this is the end of the
                                                subpath (because of exiting
                                                out of boundaries)
                                              */
                                 );
        } else {
            /* Clamping starting from first vertex of the processed
             * segment
             *
             * CLIPCLAMP(xMin, xMax, x1, y1, x2, y2, x3, y3, res);
             */
            res = CLIPCLAMP(xMin, xMax, c, 0, 1, 2, 3, 4, 5);
            X1 = (int)(c[0]*MDP_MULT);
            Y1 = (int)(c[1]*MDP_MULT);

            /* Clamping only by left boundary */
            if (res == CRES_MIN_CLIPPED) {
                X3 = (int)(c[4]*MDP_MULT);
                Y3 = (int)(c[5]*MDP_MULT);
                hnd.processFixedLine(X3, Y3, X1, Y1, pixelInfo,
                                     false, lastClipped);

            } else if (res == CRES_INVISIBLE) {
                return;
            }

            /* Clamping starting from last vertex of the processed
             * segment
             *
             * CLIPCLAMP(xMin, xMax, x2, y2, x1, y1, x3, y3, res);
             */
            res = CLIPCLAMP(xMin, xMax, c, 2, 3, 0, 1, 4, 5);

            /* Checking if there was a clip by right boundary */
            lastClipped = lastClipped || (res == CRES_MAX_CLIPPED);

            X2 = (int)(c[2]*MDP_MULT);
            Y2 = (int)(c[3]*MDP_MULT);
            hnd.processFixedLine(X1, Y1, X2, Y2, pixelInfo,
                                 false, lastClipped);

            /* Clamping only by left boundary */
            if (res == CRES_MIN_CLIPPED) {
                X3 = (int)(c[4]*MDP_MULT);
                Y3 = (int)(c[5]*MDP_MULT);
                hnd.processFixedLine(X2, Y2, X3, Y3, pixelInfo,
                                     false, lastClipped);
            }
        }
    }

    private static boolean doProcessPath(ProcessHandler hnd,
                                         Path2D.Float p2df,
                                         float transXf, float transYf) {
        float[] coords = new float[8];
        float[] tCoords = new float[8];
        float[] closeCoord = new float[] {0.0f, 0.0f};
        float[] firstCoord = new float[2];
        int[] pixelInfo = new int[5];
        boolean subpathStarted = false;
        boolean skip = false;
        float lastX, lastY;
        pixelInfo[0] = 0;

        /* Adjusting boundaries to the capabilities of the
         * ProcessPath code
         */
        hnd.dhnd.adjustBounds(LOWER_OUT_BND, LOWER_OUT_BND,
                              UPPER_OUT_BND, UPPER_OUT_BND);

        /* Adding support of the KEY_STROKE_CONTROL rendering hint.
         * Now we are supporting two modes: "pixels at centers" and
         * "pixels at corners".
         * First one is disabled by default but could be enabled by setting
         * VALUE_STROKE_PURE to the rendering hint. It means that pixel at the
         * screen (x,y) has (x + 0.5, y + 0.5) float coordinates.
         *
         * Second one is enabled by default and means straightforward mapping
         * (x,y) --> (x,y)
         */
        if (hnd.dhnd.strokeControl == SunHints.INTVAL_STROKE_PURE) {
            closeCoord[0] = -0.5f;
            closeCoord[1] = -0.5f;
            transXf -= 0.5;
            transYf -= 0.5;
        }

        PathIterator pi = p2df.getPathIterator(null);

        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    /* Performing closing of the unclosed segments */
                    if (subpathStarted && !skip) {
                        if (hnd.clipMode == PH_MODE_FILL_CLIP) {
                            if (tCoords[0] != closeCoord[0] ||
                                tCoords[1] != closeCoord[1])
                            {
                                ProcessLine(hnd, tCoords[0], tCoords[1],
                                            closeCoord[0], closeCoord[1],
                                            pixelInfo);
                            }
                        }
                        hnd.processEndSubPath();
                    }

                    tCoords[0] = coords[0] + transXf;
                    tCoords[1] = coords[1] + transYf;

                    /* Checking SEG_MOVETO coordinates if they are out of the
                     * [LOWER_BND, UPPER_BND] range.  This check also handles
                     * NaN and Infinity values. Skipping next path segment in
                     * case of invalid data.
                     */

                    if (tCoords[0] < UPPER_BND &&
                        tCoords[0] > LOWER_BND &&
                        tCoords[1] < UPPER_BND &&
                        tCoords[1] > LOWER_BND)
                    {
                        subpathStarted = true;
                        skip = false;
                        closeCoord[0] = tCoords[0];
                        closeCoord[1] = tCoords[1];
                    } else {
                        skip = true;
                    }
                    pixelInfo[0] = 0;
                    break;
                case PathIterator.SEG_LINETO:
                    lastX = tCoords[2] = coords[0] + transXf;
                    lastY = tCoords[3] = coords[1] + transYf;

                    /* Checking SEG_LINETO coordinates if they are out of the
                     * [LOWER_BND, UPPER_BND] range.  This check also handles
                     * NaN and Infinity values. Ignoring current path segment
                     * in case  of invalid data. If segment is skipped its
                     * endpoint (if valid) is used to begin new subpath.
                     */

                    if (lastX < UPPER_BND &&
                        lastX > LOWER_BND &&
                        lastY < UPPER_BND &&
                        lastY > LOWER_BND)
                    {
                        if (skip) {
                            tCoords[0] = closeCoord[0] = lastX;
                            tCoords[1] = closeCoord[1] = lastY;
                            subpathStarted = true;
                            skip = false;
                        } else {
                            ProcessLine(hnd, tCoords[0], tCoords[1],
                                        tCoords[2], tCoords[3], pixelInfo);
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                    break;
                case PathIterator.SEG_QUADTO:
                    tCoords[2] = coords[0] + transXf;
                    tCoords[3] = coords[1] + transYf;
                    lastX = tCoords[4] = coords[2] + transXf;
                    lastY = tCoords[5] = coords[3] + transYf;

                    /* Checking SEG_QUADTO coordinates if they are out of the
                     * [LOWER_BND, UPPER_BND] range.  This check also handles
                     * NaN and Infinity values. Ignoring current path segment
                     * in case  of invalid endpoints's data.  Equivalent to
                     * the SEG_LINETO if endpoint coordinates are valid but
                     * there are invalid data among other coordinates
                     */

                    if (lastX < UPPER_BND &&
                        lastX > LOWER_BND &&
                        lastY < UPPER_BND &&
                        lastY > LOWER_BND)
                    {
                        if (skip) {
                            tCoords[0] = closeCoord[0] = lastX;
                            tCoords[1] = closeCoord[1] = lastY;
                            subpathStarted = true;
                            skip = false;
                        } else {
                            if (tCoords[2] < UPPER_BND &&
                                tCoords[2] > LOWER_BND &&
                                tCoords[3] < UPPER_BND &&
                                tCoords[3] > LOWER_BND)
                            {
                                ProcessQuad(hnd, tCoords, pixelInfo);
                            } else {
                                ProcessLine(hnd, tCoords[0], tCoords[1],
                                            tCoords[4], tCoords[5],
                                            pixelInfo);
                            }
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                    break;
                case PathIterator.SEG_CUBICTO:
                    tCoords[2] = coords[0] + transXf;
                    tCoords[3] = coords[1] + transYf;
                    tCoords[4] = coords[2] + transXf;
                    tCoords[5] = coords[3] + transYf;
                    lastX = tCoords[6] = coords[4] + transXf;
                    lastY = tCoords[7] = coords[5] + transYf;

                    /* Checking SEG_CUBICTO coordinates if they are out of the
                     * [LOWER_BND, UPPER_BND] range.  This check also handles
                     * NaN and Infinity values. Ignoring current path segment
                     * in case  of invalid endpoints's data.  Equivalent to
                     * the SEG_LINETO if endpoint coordinates are valid but
                     * there are invalid data among other coordinates
                     */

                    if (lastX < UPPER_BND &&
                        lastX > LOWER_BND &&
                        lastY < UPPER_BND &&
                        lastY > LOWER_BND)
                    {
                        if (skip) {
                            tCoords[0] = closeCoord[0] = tCoords[6];
                            tCoords[1] = closeCoord[1] = tCoords[7];
                            subpathStarted = true;
                            skip = false;
                        } else {
                            if (tCoords[2] < UPPER_BND &&
                                tCoords[2] > LOWER_BND &&
                                tCoords[3] < UPPER_BND &&
                                tCoords[3] > LOWER_BND &&
                                tCoords[4] < UPPER_BND &&
                                tCoords[4] > LOWER_BND &&
                                tCoords[5] < UPPER_BND &&
                                tCoords[5] > LOWER_BND)
                            {
                                ProcessCubic(hnd, tCoords, pixelInfo);
                            } else {
                                ProcessLine(hnd, tCoords[0], tCoords[1],
                                            tCoords[6], tCoords[7],
                                            pixelInfo);
                            }
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                    break;
                case PathIterator.SEG_CLOSE:
                    if (subpathStarted && !skip) {
                        skip = false;
                        if (tCoords[0] != closeCoord[0] ||
                            tCoords[1] != closeCoord[1])
                        {
                            ProcessLine(hnd, tCoords[0], tCoords[1],
                                        closeCoord[0], closeCoord[1],
                                        pixelInfo);

                            /* Storing last path's point for using in following
                             * segments without initial moveTo
                             */
                            tCoords[0] = closeCoord[0];
                            tCoords[1] = closeCoord[1];
                        }
                        hnd.processEndSubPath();
                    }
                    break;
            }
            pi.next();
        }

        /* Performing closing of the unclosed segments */
        if (subpathStarted & !skip) {
            if (hnd.clipMode == PH_MODE_FILL_CLIP) {
                if (tCoords[0] != closeCoord[0] ||
                    tCoords[1] != closeCoord[1])
                {
                    ProcessLine(hnd, tCoords[0], tCoords[1],
                                closeCoord[0], closeCoord[1],
                                pixelInfo);
                }
            }
            hnd.processEndSubPath();
        }
        return true;
    }

    private static class Point {
        public int x;
        public int y;
        public boolean lastPoint;
        public Point prev;
        public Point next;
        public Point nextByY;
        public Edge edge;
        public Point(int x, int y, boolean lastPoint) {
            this.x = x;
            this.y = y;
            this.lastPoint = lastPoint;
        }
    };

    private static class Edge {
        int x;
        int dx;
        Point p;
        int  dir;
        Edge prev;
        Edge next;

        public Edge(Point p, int x, int dx, int dir) {
            this.p = p;
            this.x = x;
            this.dx = dx;
            this.dir = dir;
        }
    };

    /* Size of the default buffer in the FillData structure. This buffer is
     * replaced with heap allocated in case of large paths.
     */
    private static final int DF_MAX_POINT = 256;

    /* Following class accumulates points of the non-continuous flattened
     * general path during iteration through the origin path's segments . The
     * end of the each subpath is marked as lastPoint flag set at the last
     * point
     */
    private static class FillData {
        List<Point>  plgPnts;
        public int  plgYMin;
        public int  plgYMax;

        public FillData() {
            plgPnts = new Vector<Point>(DF_MAX_POINT);
        }

        public void addPoint(int x, int y, boolean lastPoint) {
            if (plgPnts.size() == 0) {
                plgYMin = plgYMax = y;
            } else {
                plgYMin = (plgYMin > y)?y:plgYMin;
                plgYMax = (plgYMax < y)?y:plgYMax;
            }

            plgPnts.add(new Point(x, y, lastPoint));
        }

        public boolean isEmpty() {
            return plgPnts.size() == 0;
        }

        public boolean isEnded() {
            return plgPnts.get(plgPnts.size() - 1).lastPoint;
        }

        public boolean setEnded() {
            return plgPnts.get(plgPnts.size() - 1).lastPoint = true;
        }
    }

    private static class ActiveEdgeList {
        Edge head;

        public boolean isEmpty() {
            return (head == null);
        }

        public void insert(Point pnt, int cy) {
            Point np = pnt.next;
            int X1 = pnt.x, Y1 = pnt.y;
            int X2 = np.x, Y2 = np.y;
            Edge ne;
            if (Y1 == Y2) {
                /* Skipping horizontal segments */
                return;
            } else {
                int dX = X2 - X1;
                int dY = Y2 - Y1;
                int stepx, x0, dy, dir;

                if (Y1 < Y2) {
                    x0 = X1;
                    dy = cy - Y1;
                    dir = -1;
                } else { // (Y1 > Y2)
                    x0 = X2;
                    dy = cy - Y2;
                    dir = 1;
                }

                /* We need to worry only about dX because dY is in denominator
                 * and abs(dy) < MDP_MULT (cy is a first scanline of the scan
                 * converted segment and we subtract y coordinate of the
                 * nearest segment's end from it to obtain dy)
                 */
                if (dX > CALC_UBND || dX < CALC_LBND)  {
                    stepx = (int)((((double)dX)*MDP_MULT)/dY);
                    x0 = x0 + (int)((((double)dX)*dy)/dY);
                } else {
                    stepx = (dX<<MDP_PREC)/dY;
                    x0 += (dX*dy)/dY;
                }

                ne = new Edge(pnt, x0, stepx, dir);
            }

            ne.next = head;
            ne.prev = null;
            if (head != null) {
                head.prev = ne;
            }
            head = pnt.edge = ne;
        }

        public void delete(Edge e) {
            Edge prevp = e.prev;
            Edge nextp = e.next;
            if (prevp != null) {
                prevp.next = nextp;
            } else {
                head = nextp;
            }
            if (nextp != null) {
                nextp.prev = prevp;
            }
        }

        /**
         * Bubble sorting in the ascending order of the linked list.  This
         * implementation stops processing the list if there were no changes
         * during the previous pass.
         *
         * We could not use O(N) Radix sort here because in most cases list of
         * edges almost sorted.  So, bubble sort (O(N^2)) is working much
         * better.  Note, in case of array of edges Shell sort is more
         * efficient.
         */
        public void sort() {
            Edge p, q, r, s = null, temp;
            boolean wasSwap = true;

            // r precedes p and s points to the node up to which
            // comparisons are to be made
            while (s != head.next && wasSwap) {
                r = p = head;
                q = p.next;
                wasSwap = false;
                while (p != s) {
                    if (p.x >= q.x) {
                        wasSwap = true;
                        if (p == head) {
                            temp = q.next;
                            q.next = p;
                            p.next = temp;
                            head = q;
                            r = q;
                        } else {
                            temp = q.next;
                            q.next = p;
                            p.next = temp;
                            r.next = q;
                            r = q;
                        }
                    } else {
                        r = p;
                        p = p.next;
                    }
                    q = p.next;
                    if (q == s) s = p;
                }
            }

            // correction of the back links in the double linked edge list
            p = head;
            q = null;
            while (p != null) {
                p.prev = q;
                q = p;
                p = p.next;
            }
        }
    }

    private static void FillPolygon(FillProcessHandler hnd,
                                    int fillRule) {
        int k, y, n;
        boolean drawing;
        Edge active;
        int rightBnd = hnd.dhnd.xMax - 1;
        FillData fd = hnd.fd;
        int yMin = fd.plgYMin;
        int yMax = fd.plgYMax;
        int hashSize = ((yMax - yMin)>>MDP_PREC) + 4;

        /* Because of support of the KEY_STROKE_CONTROL hint we are performing
         * shift of the coordinates at the higher level
         */
        int hashOffset = ((yMin - 1) & MDP_W_MASK);

        /* Winding counter */
        int counter;

        /* Calculating mask to be applied to the winding counter */
        int counterMask =
            (fillRule == PathIterator.WIND_NON_ZERO)? -1:1;

        int pntOffset;
        List<Point> pnts = fd.plgPnts;
        n = pnts.size();

        if (n <=1) return;

        Point[] yHash = new Point[hashSize];

        /* Creating double linked list (prev, next links) describing path order
         * and hash table with points which fall between scanlines. nextByY
         * link is used for the points which are between same scanlines.
         * Scanlines are passed through the centers of the pixels.
         */
        Point curpt = pnts.get(0);
        curpt.prev = null;
        for (int i = 0; i < n - 1; i++) {
            curpt = pnts.get(i);
            Point nextpt = pnts.get(i + 1);
            int curHashInd = (curpt.y - hashOffset - 1) >> MDP_PREC;
            curpt.nextByY = yHash[curHashInd];
            yHash[curHashInd] = curpt;
            curpt.next = nextpt;
            nextpt.prev = curpt;
        }

        Point ept = pnts.get(n - 1);
        int curHashInd = (ept.y - hashOffset - 1) >> MDP_PREC;
        ept.nextByY = yHash[curHashInd];
        yHash[curHashInd] = ept;

        ActiveEdgeList activeList = new ActiveEdgeList();

        for (y=hashOffset + MDP_MULT,k = 0;
             y<=yMax && k < hashSize; y += MDP_MULT, k++)
        {
            for(Point pt = yHash[k];pt != null; pt=pt.nextByY) {
                /* pt.y should be inside hashed interval
                 * assert(y-MDP_MULT <= pt.y && pt.y < y);
                 */
                if (pt.prev != null && !pt.prev.lastPoint) {
                    if (pt.prev.edge != null && pt.prev.y <= y) {
                        activeList.delete(pt.prev.edge);
                        pt.prev.edge = null;
                    } else  if (pt.prev.y > y) {
                        activeList.insert(pt.prev, y);
                    }
                }

                if (!pt.lastPoint && pt.next != null) {
                    if (pt.edge != null && pt.next.y <= y) {
                        activeList.delete(pt.edge);
                        pt.edge = null;
                    } else if (pt.next.y > y) {
                        activeList.insert(pt, y);
                    }
                }
            }

            if (activeList.isEmpty()) continue;

            activeList.sort();

            counter = 0;
            drawing = false;
            int xl, xr;
            xl = xr = hnd.dhnd.xMin;
            Edge curEdge = activeList.head;
            while (curEdge != null) {
                counter += curEdge.dir;
                if ((counter & counterMask) != 0 && !drawing) {
                    xl = (curEdge.x + MDP_MULT - 1)>>MDP_PREC;
                    drawing = true;
                }

                if ((counter & counterMask) == 0 && drawing) {
                    xr = (curEdge.x - 1) >> MDP_PREC;
                    if (xl <= xr) {
                        hnd.dhnd.drawScanline(xl, xr, y >> MDP_PREC);
                    }
                    drawing = false;
                }

                curEdge.x += curEdge.dx;
                curEdge = curEdge.next;
            }

            /* Performing drawing till the right boundary (for correct
             * rendering shapes clipped at the right side)
             */
            if (drawing && xl <= rightBnd) {

                /* Support of the strokeHint was added into the
                 * draw and fill methods of the sun.java2d.pipe.LoopPipe
                 */
                hnd.dhnd.drawScanline(xl, rightBnd, y  >> MDP_PREC);
            }
        }
    }

    private static class FillProcessHandler extends ProcessHandler {

        FillData fd;

        /* Note: For more easy reading of the code below each java version of
         * the macros from the ProcessPath.c preceded by the commented
         * origin call containing verbose names of the parameters
         */
        public void  processFixedLine(int x1, int y1, int x2, int y2,
                                      int[] pixelInfo, boolean checkBounds,
                                      boolean endSubPath)
        {
            int outXMin, outXMax, outYMin, outYMax;
            int res;

            /* There is no need to round line coordinates to the forward
             * differencing precision anymore. Such a rounding was used for
             * preventing the curve go out the endpoint (this sometimes does
             * not help). The problem was fixed in the forward differencing
             * loops.
             */
            if (checkBounds) {
                boolean lastClipped;

                /* This function is used only for filling shapes, so there is no
                 * check for the type of clipping
                 */
                int[] c = new int[]{x1, y1, x2, y2, 0, 0};
                outXMin = (int)(dhnd.xMinf * MDP_MULT);
                outXMax = (int)(dhnd.xMaxf * MDP_MULT);
                outYMin = (int)(dhnd.yMinf * MDP_MULT);
                outYMax = (int)(dhnd.yMaxf * MDP_MULT);

                /*
                 * TESTANDCLIP(outYMin, outYMax, y1, x1, y2, x2, res);
                 */
                res = TESTANDCLIP(outYMin, outYMax, c, 1, 0, 3, 2);
                if (res == CRES_INVISIBLE) return;

                /*
                 * TESTANDCLIP(outYMin, outYMax, y2, x2, y1, x1, res);
                 */
                res = TESTANDCLIP(outYMin, outYMax, c, 3, 2, 1, 0);
                if (res == CRES_INVISIBLE) return;
                lastClipped = IS_CLIPPED(res);

                /* Clamping starting from first vertex of the processed
                 * segment
                 *
                 * CLIPCLAMP(outXMin, outXMax, x1, y1, x2, y2, x3, y3, res);
                 */
                res = CLIPCLAMP(outXMin, outXMax, c, 0, 1, 2, 3, 4, 5);

                /* Clamping only by left boundary */
                if (res == CRES_MIN_CLIPPED) {
                    processFixedLine(c[4], c[5], c[0], c[1], pixelInfo,
                                     false, lastClipped);

                } else if (res == CRES_INVISIBLE) {
                    return;
                }

                /* Clamping starting from last vertex of the processed
                 * segment
                 *
                 * CLIPCLAMP(outXMin, outXMax, x2, y2, x1, y1, x3, y3, res);
                 */
                res = CLIPCLAMP(outXMin, outXMax, c, 2, 3, 0, 1, 4, 5);

                /* Checking if there was a clip by right boundary */
                lastClipped = lastClipped || (res == CRES_MAX_CLIPPED);

                processFixedLine(c[0], c[1], c[2], c[3], pixelInfo,
                                 false, lastClipped);

                /* Clamping only by left boundary */
                if (res == CRES_MIN_CLIPPED) {
                    processFixedLine(c[2], c[3], c[4], c[5], pixelInfo,
                                     false, lastClipped);
                }

                return;
            }

            /* Adding first point of the line only in case of empty or just
             * finished path
             */
            if (fd.isEmpty() || fd.isEnded()) {
                fd.addPoint(x1, y1, false);
            }

            fd.addPoint(x2, y2, false);

            if (endSubPath) {
                fd.setEnded();
            }
        }

        FillProcessHandler(DrawHandler dhnd) {
            super(dhnd, PH_MODE_FILL_CLIP);
            this.fd = new FillData();
        }

        public void processEndSubPath() {
            if (!fd.isEmpty()) {
                fd.setEnded();
            }
        }
    }
}
