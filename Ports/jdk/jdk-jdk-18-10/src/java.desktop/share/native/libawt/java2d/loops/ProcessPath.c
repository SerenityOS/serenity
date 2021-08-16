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

#include <math.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>

#include "jni.h"
#include "j2d_md.h"
#include "java_awt_geom_PathIterator.h"

#include "ProcessPath.h"

/*
 * This framework performs filling and drawing of paths with sub-pixel
 * precision. Also, it performs clipping by the specified view area.
 *
 * Drawing of the shapes is performed not pixel by pixel but segment by segment
 * except several pixels near endpoints of the drawn line. This approach saves
 * lot's of cpu cycles especially in case of large primitives (like ovals with
 * sizes more than 50) and helps in achieving appropriate visual quality. Also,
 * such method of drawing is useful for the accelerated pipelines where
 * overhead of the per-pixel drawing could eliminate all benefits of the
 * hardware acceleration.
 *
 * Filling of the path was  taken from
 *
 * [Graphics Gems, edited by Andrew S Glassner. Academic Press 1990,
 * ISBN 0-12-286165-5 (Concave polygon scan conversion), 87-91]
 *
 * and modified to work with sub-pixel precision and non-continuous paths.
 * It's also speeded up by using hash table by rows of the filled objects.
 *
 * Here is high level scheme showing the rendering process:
 *
 *                   doDrawPath   doFillPath
 *                         \         /
 *                         ProcessPath
 *                              |
 *                      CheckPathSegment
 *                              |
 *                      --------+------
 *                      |             |
 *                      |             |
 *                      |             |
 *                  _->ProcessCurve   |
 *                 /    / |           |
 *                 \___/  |           |
 *                        |           |
 *                    DrawCurve     ProcessLine
 *                         \         /
 *                          \       /
 *                           \     /
 *                            \   /
 *                        ------+------
 *             (filling) /             \ (drawing)
 *                      /               \
 *               Clipping and        Clipping
 *                clamping                \
 *                   |                     \
 *           StoreFixedLine          ProcessFixedLine
 *                   |                     /    \
 *                   |                    /      \
 *             FillPolygon       PROCESS_LINE   PROCESS_POINT
 *
 *
 *
 *  CheckPathSegment  - rough checking and skipping path's segments  in case of
 *                      invalid or huge coordinates of the control points to
 *                      avoid calculation problems with NaNs and values close
 *                      to the FLT_MAX
 *
 * ProcessCurve - (ProcessQuad, ProcessCubic) Splitting the curve into
 *                monotonic parts having appropriate size (calculated as
 *                boundary box of the control points)
 *
 * DrawMonotonicCurve - (DrawMonotonicQuad, DrawMonotonicCubic) flattening
 *                      monotonic curve using adaptive forward differencing
 *
 * StoreFixedLine - storing segment from the flattened path to the
 *                  FillData structure. Performing clipping and clamping if
 *                  necessary.
 *
 * PROCESS_LINE, PROCESS_POINT - Helpers for calling appropriate primitive from
 *                               DrawHandler structure
 *
 * ProcessFixedLine - Drawing line segment with subpixel precision.
 *
 */

#define PROCESS_LINE(hnd, fX0, fY0, fX1, fY1, checkBounds, pixelInfo)       \
    do {                                                                    \
        jint X0 = (fX0) >> MDP_PREC;                                        \
        jint Y0 = (fY0) >> MDP_PREC;                                        \
        jint X1 = (fX1) >> MDP_PREC;                                        \
        jint Y1 = (fY1) >> MDP_PREC;                                        \
        jint res;                                                           \
                                                                            \
        /* Checking bounds and clipping if necessary.                       \
         * REMIND: It's temporary solution to avoid OOB in rendering code.  \
         * Current approach uses float equations which are unreliable for   \
         * clipping and makes assumptions about the line biases of the      \
         * rendering algorithm. Also, clipping code should be moved down    \
         * into only those output renderers that need it.                   \
         */                                                                 \
        if (checkBounds) {                                                  \
            jfloat xMinf = hnd->dhnd->xMinf + 0.5f;                         \
            jfloat yMinf = hnd->dhnd->yMinf + 0.5f;                         \
            jfloat xMaxf = hnd->dhnd->xMaxf + 0.5f;                         \
            jfloat yMaxf = hnd->dhnd->yMaxf + 0.5f;                         \
            TESTANDCLIP(yMinf, yMaxf, Y0, X0, Y1, X1, jint, res);           \
            if (res == CRES_INVISIBLE) break;                               \
            TESTANDCLIP(yMinf, yMaxf, Y1, X1, Y0, X0, jint, res);           \
            if (res == CRES_INVISIBLE) break;                               \
            TESTANDCLIP(xMinf, xMaxf, X0, Y0, X1, Y1, jint, res);           \
            if (res == CRES_INVISIBLE) break;                               \
            TESTANDCLIP(xMinf, xMaxf, X1, Y1, X0, Y0, jint, res);           \
            if (res == CRES_INVISIBLE) break;                               \
        }                                                                   \
                                                                            \
        /* Handling lines having just one pixel      */                     \
        if (((X0^X1) | (Y0^Y1)) == 0) {                                     \
            if (pixelInfo[0] == 0) {                                        \
                pixelInfo[0] = 1;                                           \
                pixelInfo[1] = X0;                                          \
                pixelInfo[2] = Y0;                                          \
                pixelInfo[3] = X0;                                          \
                pixelInfo[4] = Y0;                                          \
                hnd->dhnd->pDrawPixel(hnd->dhnd, X0, Y0);                   \
            } else if ((X0 != pixelInfo[3] || Y0 != pixelInfo[4]) &&        \
                       (X0 != pixelInfo[1] || Y0 != pixelInfo[2])) {        \
                hnd->dhnd->pDrawPixel(hnd->dhnd, X0, Y0);                   \
                pixelInfo[3] = X0;                                          \
                pixelInfo[4] = Y0;                                          \
            }                                                               \
            break;                                                          \
        }                                                                   \
                                                                            \
        if (pixelInfo[0] &&                                                 \
            ((pixelInfo[1] == X0 && pixelInfo[2] == Y0) ||                  \
            (pixelInfo[3] == X0 && pixelInfo[4] == Y0)))                    \
        {                                                                   \
            hnd->dhnd->pDrawPixel(hnd->dhnd, X0, Y0);                       \
        }                                                                   \
                                                                            \
        hnd->dhnd->pDrawLine(hnd->dhnd, X0, Y0, X1, Y1);                    \
                                                                            \
        if (pixelInfo[0] == 0) {                                            \
            pixelInfo[0] = 1;                                               \
            pixelInfo[1] = X0;                                              \
            pixelInfo[2] = Y0;                                              \
            pixelInfo[3] = X0;                                              \
            pixelInfo[4] = Y0;                                              \
        }                                                                   \
                                                                            \
        /* Switch on last pixel of the line if it was already               \
         * drawn during rendering of the previous segments                  \
         */                                                                 \
        if ((pixelInfo[1] == X1 && pixelInfo[2] == Y1) ||                   \
            (pixelInfo[3] == X1 && pixelInfo[4] == Y1))                     \
        {                                                                   \
            hnd->dhnd->pDrawPixel(hnd->dhnd, X1, Y1);                       \
        }                                                                   \
        pixelInfo[3] = X1;                                                  \
        pixelInfo[4] = Y1;                                                  \
    } while(0)

#define PROCESS_POINT(hnd, fX, fY, checkBounds, pixelInfo)                  \
    do {                                                                    \
        jint X_ = (fX)>> MDP_PREC;                                          \
        jint Y_ = (fY)>> MDP_PREC;                                          \
        if (checkBounds &&                                                  \
            (hnd->dhnd->yMin > Y_  ||                                       \
             hnd->dhnd->yMax <= Y_ ||                                       \
             hnd->dhnd->xMin > X_  ||                                       \
             hnd->dhnd->xMax <= X_)) break;                                 \
/*                                                                          \
 *       (X_,Y_) should be inside boundaries                                \
 *                                                                          \
 *       assert(hnd->dhnd->yMin <= Y_ &&                                    \
 *              hnd->dhnd->yMax >  Y_ &&                                    \
 *              hnd->dhnd->xMin <= X_ &&                                    \
 *              hnd->dhnd->xMax >  X_);                                     \
 *                                                                          \
 */                                                                         \
        if (pixelInfo[0] == 0) {                                            \
            pixelInfo[0] = 1;                                               \
            pixelInfo[1] = X_;                                              \
            pixelInfo[2] = Y_;                                              \
            pixelInfo[3] = X_;                                              \
            pixelInfo[4] = Y_;                                              \
            hnd->dhnd->pDrawPixel(hnd->dhnd, X_, Y_);                       \
        } else if ((X_ != pixelInfo[3] || Y_ != pixelInfo[4]) &&            \
                   (X_ != pixelInfo[1] || Y_ != pixelInfo[2])) {            \
            hnd->dhnd->pDrawPixel(hnd->dhnd, X_, Y_);                       \
            pixelInfo[3] = X_;                                              \
            pixelInfo[4] = Y_;                                              \
        }                                                                   \
    } while(0)


/*
 *                  Constants for the forward differencing
 *                      of the cubic and quad curves
 */

/* Maximum size of the cubic curve (calculated as the size of the bounding box
 * of the control points) which could be rendered without splitting
 */
#define MAX_CUB_SIZE    256

/* Maximum size of the quad curve (calculated as the size of the bounding box
 * of the control points) which could be rendered without splitting
 */
#define MAX_QUAD_SIZE   1024

/* Default power of 2 steps used in the forward differencing. Here DF prefix
 * stands for DeFault. Constants below are used as initial values for the
 * adaptive forward differencing algorithm.
 */
#define DF_CUB_STEPS    3
#define DF_QUAD_STEPS   2

/* Shift of the current point of the curve for preparing to the midpoint
 * rounding
 */
#define DF_CUB_SHIFT    (FWD_PREC + DF_CUB_STEPS*3 - MDP_PREC)
#define DF_QUAD_SHIFT    (FWD_PREC + DF_QUAD_STEPS*2 - MDP_PREC)

/* Default amount of steps of the forward differencing */
#define DF_CUB_COUNT    (1<<DF_CUB_STEPS)
#define DF_QUAD_COUNT    (1<<DF_QUAD_STEPS)

/* Default boundary constants used to check the necessity of the restepping */
#define DF_CUB_DEC_BND     (1<<(DF_CUB_STEPS*3 + FWD_PREC + 2))
#define DF_CUB_INC_BND     (1<<(DF_CUB_STEPS*3 + FWD_PREC - 1))
#define DF_QUAD_DEC_BND     (1<<(DF_QUAD_STEPS*2 + FWD_PREC + 2))

/* Multiplyers for the coefficients of the polynomial form of the cubic and
 * quad curves representation
 */
#define CUB_A_SHIFT   FWD_PREC
#define CUB_B_SHIFT   (DF_CUB_STEPS + FWD_PREC + 1)
#define CUB_C_SHIFT   (DF_CUB_STEPS*2 + FWD_PREC)

#define CUB_A_MDP_MULT    (1<<CUB_A_SHIFT)
#define CUB_B_MDP_MULT    (1<<CUB_B_SHIFT)
#define CUB_C_MDP_MULT    (1<<CUB_C_SHIFT)

#define QUAD_A_SHIFT   FWD_PREC
#define QUAD_B_SHIFT   (DF_QUAD_STEPS + FWD_PREC)

#define QUAD_A_MDP_MULT    (1<<QUAD_A_SHIFT)
#define QUAD_B_MDP_MULT    (1<<QUAD_B_SHIFT)

#define CALC_MAX(MAX, X) ((MAX)=((X)>(MAX))?(X):(MAX))
#define CALC_MIN(MIN, X) ((MIN)=((X)<(MIN))?(X):(MIN))
#define MAX(MAX, X) (((X)>(MAX))?(X):(MAX))
#define MIN(MIN, X) (((X)<(MIN))?(X):(MIN))
#define ABS32(X) (((X)^((X)>>31))-((X)>>31))
#define SIGN32(X) ((X) >> 31) | ((juint)(-(X)) >> 31)

/* Boundaries used for clipping large path segments (those are inside
 * [UPPER/LOWER]_BND boundaries)
 */
#define UPPER_OUT_BND (1 << (30 - MDP_PREC))
#define LOWER_OUT_BND (-UPPER_OUT_BND)

#define ADJUST(X, LBND, UBND)                                               \
    do {                                                                    \
        if ((X) < (LBND)) {                                                 \
            (X) = (LBND);                                                   \
        } else if ((X) > UBND) {                                            \
            (X) = (UBND);                                                   \
        }                                                                   \
    } while(0)

/* Following constants are used for providing open boundaries of the intervals
 */
#define EPSFX 1
#define EPSF (((jfloat)EPSFX)/MDP_MULT)

/* Calculation boundary. It is used for switching to the more slow but allowing
 * larger input values method of calculation of the initial values of the scan
 * converted line segments inside the FillPolygon.
 */
#define CALC_BND (1 << (30 - MDP_PREC))

/* Clipping macros for drawing and filling algorithms */

#define CLIP(a1, b1, a2, b2, t) \
    (b1 + ((jdouble)(t - a1)*(b2 - b1)) / (a2 - a1))

enum {
    CRES_MIN_CLIPPED,
    CRES_MAX_CLIPPED,
    CRES_NOT_CLIPPED,
    CRES_INVISIBLE
};

#define IS_CLIPPED(res) (res == CRES_MIN_CLIPPED || res == CRES_MAX_CLIPPED)

#define TESTANDCLIP(LINE_MIN, LINE_MAX, a1, b1, a2, b2, TYPE, res)  \
   do {                                                             \
        jdouble t;                                                  \
        res = CRES_NOT_CLIPPED;                                     \
        if (a1 < (LINE_MIN) || a1 > (LINE_MAX)) {                   \
            if (a1 < (LINE_MIN)) {                                  \
                if (a2 < (LINE_MIN)) {                              \
                    res = CRES_INVISIBLE;                           \
                    break;                                          \
                };                                                  \
                res = CRES_MIN_CLIPPED;                             \
                t = (LINE_MIN);                                     \
            } else {                                                \
                if (a2 > (LINE_MAX)) {                              \
                    res = CRES_INVISIBLE;                           \
                    break;                                          \
                };                                                  \
                res = CRES_MAX_CLIPPED;                             \
                t = (LINE_MAX);                                     \
            }                                                       \
            b1 = (TYPE)CLIP(a1, b1, a2, b2, t);                     \
            a1 = (TYPE)t;                                           \
        }                                                           \
   } while (0)

/* Following macro is used for clipping and clumping filled shapes.
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
 */
#define CLIPCLAMP(LINE_MIN, LINE_MAX, a1, b1, a2, b2, a3, b3, TYPE, res)  \
    do {                                                            \
        a3 = a1;                                                    \
        b3 = b1;                                                    \
        TESTANDCLIP(LINE_MIN, LINE_MAX, a1, b1, a2, b2, TYPE, res); \
        if (res == CRES_MIN_CLIPPED) {                              \
            a3 = a1;                                                \
        } else if (res == CRES_MAX_CLIPPED) {                       \
            a3 = a1;                                                \
            res = CRES_MAX_CLIPPED;                                 \
        } else if (res == CRES_INVISIBLE) {                         \
            if (a1 > LINE_MAX) {                                    \
                res =  CRES_INVISIBLE;                              \
            } else {                                                \
                a1 = (TYPE)LINE_MIN;                                \
                a2 = (TYPE)LINE_MIN;                                \
                res = CRES_NOT_CLIPPED;                             \
            }                                                       \
        }                                                           \
    } while (0)

/* Following macro is used for solving quadratic equations:
 * A*t^2 + B*t + C = 0
 * in (0,1) range. That means we put to the RES the only roots which
 * belongs to the (0,1) range. Note: 0 and 1 are not included.
 * See solveQuadratic method in
 *  src/share/classes/java/awt/geom/QuadCurve2D.java
 * for more info about calculations
 */
#define SOLVEQUADINRANGE(A,B,C,RES,RCNT)                            \
    do {                                                            \
        double param;                                               \
        if ((A) != 0) {                                             \
            /* Calculating roots of the following equation          \
             * A*t^2 + B*t + C = 0                                  \
             */                                                     \
            double d = (B)*(B) - 4*(A)*(C);                         \
            double q;                                               \
            if (d < 0) {                                            \
                break;                                              \
            }                                                       \
            d = sqrt(d);                                            \
            /* For accuracy, calculate one root using:              \
             *     (-B +/- d) / 2*A                                 \
             * and the other using:                                 \
             *     2*C / (-B +/- d)                                 \
             * Choose the sign of the +/- so that B+D gets larger   \
             * in magnitude                                         \
             */                                                     \
            if ((B) < 0) {                                          \
                d = -d;                                             \
            }                                                       \
            q = ((B) + d) / -2.0;                                   \
            param = q/(A);                                          \
            if (param < 1.0 && param > 0.0) {                       \
                (RES)[(RCNT)++] = param;                            \
            }                                                       \
            if (d == 0 || q == 0) {                                 \
                break;                                              \
            }                                                       \
            param = (C)/q;                                          \
            if (param < 1.0 && param > 0.0) {                       \
                (RES)[(RCNT)++] = param;                            \
            }                                                       \
        } else {                                                    \
            /* Calculating root of the following equation           \
             * B*t + C = 0                                          \
             */                                                     \
            if ((B) == 0) {                                         \
                break;                                              \
            }                                                       \
            param = -(C)/(B);                                       \
            if (param < 1.0 && param > 0.0) {                       \
                (RES)[(RCNT)++] = param;                            \
            }                                                       \
        }                                                           \
    } while(0)

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
void  ProcessFixedLine(ProcessHandler* hnd,jint x1,jint y1,jint x2,jint y2,
                       jint* pixelInfo,jboolean checkBounds,
                       jboolean endSubPath)
{
    /* Checking if line is inside a (X,Y),(X+MDP_MULT,Y+MDP_MULT) box */
    jint c = ((x1 ^ x2) | (y1 ^ y2));
    jint rx1, ry1, rx2, ry2;
    if ((c & MDP_W_MASK) == 0) {
        /* Checking for the segments with integer coordinates having
         * the same start and end points
         */
        if (c == 0) {
            PROCESS_POINT(hnd, x1 + MDP_HALF_MULT, y1 + MDP_HALF_MULT,
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
        jint dx = x2 - x1;
        jint dy = y2 - y1;

        /* Floor of x1, y1, x2, y2 */
        jint fx1 = x1 & MDP_W_MASK;
        jint fy1 = y1 & MDP_W_MASK;
        jint fx2 = x2 & MDP_W_MASK;
        jint fy2 = y2 & MDP_W_MASK;

        /* Processing first endpoint */
        if (fx1 == x1 || fy1 == y1) {
            /* Adding MDP_HALF_MULT to the [xy]1 if f[xy]1 == [xy]1 will not
             * affect the result
             */
            rx1 = x1 + MDP_HALF_MULT;
            ry1 = y1 + MDP_HALF_MULT;
        } else {
            /* Boundary at the direction from (x1,y1) to (x2,y2) */
            jint bx1 = (x1 < x2) ? fx1 + MDP_MULT : fx1;
            jint by1 = (y1 < y2) ? fy1 + MDP_MULT : fy1;

            /* intersection with column bx1 */
            jint cross = y1 + ((bx1 - x1)*dy)/dx;
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
            /* Adding MDP_HALF_MULT to the [xy]2 if f[xy]2 == [xy]2 will not
             * affect the result
             */
            rx2 = x2 + MDP_HALF_MULT;
            ry2 = y2 + MDP_HALF_MULT;
        } else {
            /* Boundary at the direction from (x2,y2) to (x1,y1) */
            jint bx2 = (x1 > x2) ? fx2 + MDP_MULT : fx2;
            jint by2 = (y1 > y2) ? fy2 + MDP_MULT : fy2;

            /* intersection with column bx2 */
            jint cross = y2 + ((bx2 - x2)*dy)/dx;
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

    PROCESS_LINE(hnd, rx1, ry1, rx2, ry2, checkBounds, pixelInfo);
}

/* Performing drawing of the monotonic in X and Y quadratic curves with sizes
 * less than MAX_QUAD_SIZE by using forward differencing method of calculation.
 * See comments to the DrawMonotonicCubic.
 */
static void DrawMonotonicQuad(ProcessHandler* hnd,
                              jfloat *coords,
                              jboolean checkBounds,
                              jint* pixelInfo)
{
    jint x0 = (jint)(coords[0]*MDP_MULT);
    jint y0 = (jint)(coords[1]*MDP_MULT);

    jint xe = (jint)(coords[4]*MDP_MULT);
    jint ye = (jint)(coords[5]*MDP_MULT);

    /* Extracting fractional part of coordinates of first control point */
    jint px = (x0 & (~MDP_W_MASK)) << DF_QUAD_SHIFT;
    jint py = (y0 & (~MDP_W_MASK)) << DF_QUAD_SHIFT;

    /* Setting default amount of steps */
    jint count = DF_QUAD_COUNT;

    /* Setting default shift for preparing to the midpoint rounding */
    jint shift =  DF_QUAD_SHIFT;

    jint ax = (jint)((coords[0] - 2*coords[2] +
                      coords[4])*QUAD_A_MDP_MULT);
    jint ay = (jint)((coords[1] - 2*coords[3] +
                      coords[5])*QUAD_A_MDP_MULT);

    jint bx = (jint)((-2*coords[0] + 2*coords[2])*QUAD_B_MDP_MULT);
    jint by = (jint)((-2*coords[1] + 2*coords[3])*QUAD_B_MDP_MULT);

    jint ddpx = 2*ax;
    jint ddpy = 2*ay;

    jint dpx = ax + bx;
    jint dpy = ay + by;

    jint x1, y1;

    jint x2 = x0;
    jint y2 = y0;

    jint maxDD = MAX(ABS32(ddpx),ABS32(ddpy));
    jint x0w = x0 & MDP_W_MASK;
    jint y0w = y0 & MDP_W_MASK;

    jint dx = xe - x0;
    jint dy = ye - y0;

    /* Perform decreasing step in 2 times if slope of the second forward
     * difference changes too quickly (more than a pixel per step in X or Y
     * direction). We can perform adjusting of the step size before the
     * rendering loop because the curvature of the quad curve remains the same
     * along all the curve
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
         * violating coordinate.  The check is pretty simple because the curve
         * passed to the DrawMonotonicQuad already split into the monotonic
         * in X and Y pieces
         */

        /* Bounding x2 by xe */
        if (((xe-x2)^dx) < 0) {
            x2 = xe;
        }

        /* Bounding y2 by ye */
        if (((ye-y2)^dy) < 0) {
            y2 = ye;
        }

        hnd->pProcessFixedLine(hnd, x1, y1, x2, y2, pixelInfo, checkBounds,
                               JNI_FALSE);
    }

    /* We are performing one step less than necessary and use actual (xe,ye)
     * curve's endpoint instead of calculated. This prevent us from accumulated
     * errors at the last point.
     */

    hnd->pProcessFixedLine(hnd, x2, y2, xe, ye, pixelInfo, checkBounds,
                           JNI_FALSE);
}

/*
 * Checking size of the quad curves and split them if necessary.
 * Calling DrawMonotonicQuad for the curves of the appropriate size.
 * Note: coords array could be changed
 */
static void ProcessMonotonicQuad(ProcessHandler* hnd,
                                 jfloat *coords,
                                 jint* pixelInfo) {

    jfloat coords1[6];
    jfloat xMin, xMax;
    jfloat yMin, yMax;

    xMin = xMax = coords[0];
    yMin = yMax = coords[1];

    CALC_MIN(xMin, coords[2]);
    CALC_MAX(xMax, coords[2]);
    CALC_MIN(yMin, coords[3]);
    CALC_MAX(yMax, coords[3]);
    CALC_MIN(xMin, coords[4]);
    CALC_MAX(xMax, coords[4]);
    CALC_MIN(yMin, coords[5]);
    CALC_MAX(yMax, coords[5]);


    if (hnd->clipMode == PH_MODE_DRAW_CLIP) {

        /* In case of drawing we could just skip curves which are completely
         * out of bounds
         */
        if (hnd->dhnd->xMaxf < xMin || hnd->dhnd->xMinf > xMax ||
            hnd->dhnd->yMaxf < yMin || hnd->dhnd->yMinf > yMax) {
            return;
        }
    } else {

        /* In case of filling we could skip curves which are above,
         * below and behind the right boundary of the visible area
         */

         if (hnd->dhnd->yMaxf < yMin || hnd->dhnd->yMinf > yMax ||
             hnd->dhnd->xMaxf < xMin)
         {
             return;
         }

        /* We could clamp x coordinates to the corresponding boundary
         * if the curve is completely behind the left one
         */

        if (hnd->dhnd->xMinf > xMax) {
            coords[0] = coords[2] = coords[4] = hnd->dhnd->xMinf;
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
                          * curve is visible, so the check is pretty simple
                          */
                         hnd->dhnd->xMinf >= xMin || hnd->dhnd->xMaxf <= xMax ||
                         hnd->dhnd->yMinf >= yMin || hnd->dhnd->yMaxf <= yMax,
                         pixelInfo);
    }
}

/*
 * Bite the piece of the quadratic curve from start point till the point
 * corresponding to the specified parameter then call ProcessQuad for the
 * bitten part.
 * Note: coords array will be changed
 */
static void ProcessFirstMonotonicPartOfQuad(ProcessHandler* hnd, jfloat* coords,
                                            jint* pixelInfo, jfloat t)
{
    jfloat coords1[6];

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

/*
 * Split quadratic curve into monotonic in X and Y parts. Calling
 * ProcessMonotonicQuad for each monotonic piece of the curve.
 * Note: coords array could be changed
 */
static void ProcessQuad(ProcessHandler* hnd, jfloat* coords, jint* pixelInfo) {

    /* Temporary array for holding parameters corresponding to the extreme in X
     * and Y points. The values are inside the (0,1) range (0 and 1 excluded)
     * and in ascending order.
     */
    double params[2];

    jint cnt = 0;
    double param;

    /* Simple check for monotonicity in X before searching for the extreme
     * points of the X(t) function. We first check if the curve is monotonic
     * in X by seeing if all of the X coordinates are strongly ordered.
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
     * points of the Y(t) function. We first check if the curve is monotonic
     * in Y by seeing if all of the Y coordinates are strongly ordered.
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
                                            (jfloat)params[0]);
            break;
        case 2:
            ProcessFirstMonotonicPartOfQuad(hnd, coords, pixelInfo,
                                            (jfloat)params[0]);
            param = params[1] - params[0];
            if (param > 0) {
                ProcessFirstMonotonicPartOfQuad(hnd, coords, pixelInfo,
                    /* Scale parameter to match with rest of the curve */
                    (jfloat)(param/(1.0 - params[0])));
            }
            break;
    }

    ProcessMonotonicQuad(hnd,coords,pixelInfo);
}

/*
 * Performing drawing of the monotonic in X and Y cubic curves with sizes less
 * than MAX_CUB_SIZE by using forward differencing method of calculation.
 *
 * Here is some math used in the code below.
 *
 * If we express the parametric equation for the coordinates as
 * simple polynomial:
 *
 *  V(t) = a * t^3 + b * t^2 + c * t + d
 *
 * The equations for how we derive these polynomial coefficients
 * from the Bezier control points can be found in the method comments
 * for the CubicCurve.fillEqn Java method.
 *
 * From this polynomial, we can derive the forward differences to
 * allow us to calculate V(t+K) from V(t) as follows:
 *
 * 1) V1(0)
 *        = V(K)-V(0)
 *        = aK^3 + bK^2 + cK + d - d
 *        = aK^3 + bK^2 + cK
 *
 * 2) V1(K)
 *        = V(2K)-V(K)
 *        = 8aK^3 + 4bK^2 + 2cK + d - aK^3 - bK^2 - cK - d
 *        = 7aK^3 + 3bK^2 + cK
 *
 * 3) V1(2K)
 *        = V(3K)-V(2K)
 *        = 27aK^3 + 9bK^2 + 3cK + d - 8aK^3 - 4bK^2 - 2cK - d
 *        = 19aK^3 + 5bK^2 + cK
 *
 * 4) V2(0)
 *        = V1(K) - V1(0)
 *        = 7aK^3 + 3bK^2 + cK - aK^3 - bK^2 - cK
 *        = 6aK^3 + 2bK^2
 *
 * 5) V2(K)
 *        = V1(2K) - V1(K)
 *        = 19aK^3 + 5bK^2 + cK - 7aK^3 - 3bK^2 - cK
 *        = 12aK^3 + 2bK^2
 *
 * 6) V3(0)
 *        = V2(K) - V2(0)
 *        = 12aK^3 + 2bK^2 - 6aK^3 - 2bK^2
 *        = 6aK^3
 *
 * Note that if we continue on to calculate V1(3K), V2(2K) and
 * V3(K) we will see that V3(K) == V3(0) so we need at most
 * 3 cascading forward differences to step through the cubic
 * curve.
 *
 * Note, b coefficient calculating in the DrawCubic is actually twice the b
 * coefficient seen above.  It's been done for the better accuracy.
 *
 * In our case, initialy K is chosen as 1/(2^DF_CUB_STEPS) this value is taken
 * with FWD_PREC bits precision. This means that we should do 2^DF_CUB_STEPS
 * steps to pass through all the curve.
 *
 * On each step we examine how far we are stepping by examining our first(V1)
 * and second (V2) order derivatives and verifying that they are met following
 * conditions:
 *
 * abs(V2) <= DF_CUB_DEC_BND
 * abs(V1) > DF_CUB_INC_BND
 *
 * So, ensures that we step through the curve more slowly when its curvature is
 * high and faster when its curvature is lower.  If the step size needs
 * adjustment we adjust it so that we step either twice as fast, or twice as
 * slow until our step size is within range.  This modifies our stepping
 * variables as follows:
 *
 * Decreasing step size
 * (See Graphics Gems/by A.Glassner,(Tutorial on forward differencing),601-602)
 *
 * V3 = oV3/8
 * V2 = oV2/4 - V3
 * V1 = (oV1 - V2)/2
 *
 * Here V1-V3 stands for new values of the forward differencies and oV1 - oV3
 * for the old ones
 *
 * Using the equations above it's easy to calculating stepping variables for
 * the increasing step size:
 *
 * V1 = 2*oV1 + oV2
 * V2 = 4*oV2 + 4*oV3
 * V3 = 8*oV3
 *
 * And then for not to running out of 32 bit precision we are performing 3 bit
 * shift of the forward differencing precision (keeping in shift variable) in
 * left or right direction depending on what is  happening (decreasing or
 * increasing). So, all oV1 - oV3 variables should be thought as appropriately
 * shifted in regard to the V1 - V3.
 *
 * Taking all of the above into account we will have following:
 *
 * Decreasing step size:
 *
 * shift = shift + 3
 * V3 keeps the same
 * V2 = 2*oV2 - V3
 * V1 = 4*oV1 - V2/2
 *
 * Increasing step size:
 *
 * shift = shift - 3
 * V1 = oV1/4 + oV2/8
 * V2 = oV2/2 + oV3/2
 * V3 keeps the same
 *
 */

static void DrawMonotonicCubic(ProcessHandler* hnd,
                               jfloat *coords,
                               jboolean checkBounds,
                               jint* pixelInfo)
{
    jint x0 = (jint)(coords[0]*MDP_MULT);
    jint y0 = (jint)(coords[1]*MDP_MULT);

    jint xe = (jint)(coords[6]*MDP_MULT);
    jint ye = (jint)(coords[7]*MDP_MULT);

    /* Extracting fractional part of coordinates of first control point */
    jint px = (x0 & (~MDP_W_MASK)) << DF_CUB_SHIFT;
    jint py = (y0 & (~MDP_W_MASK)) << DF_CUB_SHIFT;

    /* Setting default boundary values for checking first and second forward
     * difference for the necessity of the restepping. See comments to the
     * boundary values in ProcessQuad for more info.
     */
    jint incStepBnd1 = DF_CUB_INC_BND;
    jint incStepBnd2 = DF_CUB_INC_BND << 1;
    jint decStepBnd1 = DF_CUB_DEC_BND;
    jint decStepBnd2 = DF_CUB_DEC_BND << 1;

    /* Setting default amount of steps */
    jint count = DF_CUB_COUNT;

    /* Setting default shift for preparing to the midpoint rounding */
    jint shift =  DF_CUB_SHIFT;

    jint ax = (jint)((-coords[0] + 3*coords[2] - 3*coords[4] +
                coords[6])*CUB_A_MDP_MULT);
    jint ay = (jint)((-coords[1] + 3*coords[3] - 3*coords[5] +
                coords[7])*CUB_A_MDP_MULT);

    jint bx = (jint)((3*coords[0] - 6*coords[2] +
              3*coords[4])*CUB_B_MDP_MULT);
    jint by = (jint)((3*coords[1] - 6*coords[3] +
              3*coords[5])*CUB_B_MDP_MULT);

    jint cx = (jint)((-3*coords[0] + 3*coords[2])*(CUB_C_MDP_MULT));
    jint cy = (jint)((-3*coords[1] + 3*coords[3])*(CUB_C_MDP_MULT));

    jint dddpx = 6*ax;
    jint dddpy = 6*ay;

    jint ddpx = dddpx + bx;
    jint ddpy = dddpy + by;

    jint dpx = ax + (bx>>1) + cx;
    jint dpy = ay + (by>>1) + cy;

    jint x1, y1;

    jint x2 = x0;
    jint y2 = y0;

    /* Calculating whole part of the first point of the curve */
    jint x0w = x0 & MDP_W_MASK;
    jint y0w = y0 & MDP_W_MASK;

    jint dx = xe - x0;
    jint dy = ye - y0;

    while (count > 0) {
        /* Perform decreasing step in 2 times if necessary */
        while (
               /* The code below is an optimized version of the checks:
                *   abs(ddpx) > decStepBnd1 ||
                *   abs(ddpy) > decStepBnd1
                */
               (juint)(ddpx + decStepBnd1) > (juint)decStepBnd2 ||
               (juint)(ddpy + decStepBnd1) > (juint)decStepBnd2)
        {
            ddpx = (ddpx<<1) - dddpx;
            ddpy = (ddpy<<1) - dddpy;
            dpx = (dpx<<2) - (ddpx>>1);
            dpy = (dpy<<2) - (ddpy>>1);
            count <<=1;
            decStepBnd1 <<=3;
            decStepBnd2 <<=3;
            incStepBnd1 <<=3;
            incStepBnd2 <<=3;
            px <<=3;
            py <<=3;
            shift += 3;
        }

        /* Perform increasing step in 2 times if necessary.
         * Note: we could do it only in even steps
         */

        while (((count & 1) ^ 1) && shift > DF_CUB_SHIFT  &&
               /* The code below is an optimized version of the check:
                *   abs(dpx) <= incStepBnd1 &&
                *   abs(dpy) <= incStepBnd1
                */
               (juint)(dpx + incStepBnd1) <= (juint)incStepBnd2 &&
               (juint)(dpy + incStepBnd1) <= (juint)incStepBnd2)
        {
            dpx = (dpx>>2) + (ddpx>>3);
            dpy = (dpy>>2) + (ddpy>>3);
            ddpx = (ddpx + dddpx)>>1;
            ddpy = (ddpy + dddpy)>>1;
            count >>=1;
            decStepBnd1 >>=3;
            decStepBnd2 >>=3;
            incStepBnd1 >>=3;
            incStepBnd2 >>=3;
            px >>=3;
            py >>=3;
            shift -= 3;
        }

        count--;

        /* We are performing one step less than necessary and use actual
         * (xe,ye) endpoint of the curve instead of calculated. This prevent
         * us from accumulated errors at the last point.
         */
        if (count) {

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
             * because the curve passed to the DrawMonotonicCubic already
             * split into the monotonic in X and Y pieces
             */

            /* Bounding x2 by xe */
            if (((xe-x2)^dx) < 0) {
                x2 = xe;
            }

            /* Bounding y2 by ye */
            if (((ye-y2)^dy) < 0) {
                y2 = ye;
            }

            hnd->pProcessFixedLine(hnd, x1, y1, x2, y2, pixelInfo, checkBounds,
                                   JNI_FALSE);
        } else {
            hnd->pProcessFixedLine(hnd, x2, y2, xe, ye, pixelInfo, checkBounds,
                                   JNI_FALSE);
        }
    }
}

/*
 * Checking size of the cubic curves and split them if necessary.
 * Calling DrawMonotonicCubic for the curves of the appropriate size.
 * Note: coords array could be changed
 */
static void ProcessMonotonicCubic(ProcessHandler* hnd,
                                  jfloat *coords,
                                  jint* pixelInfo) {

    jfloat coords1[8];
    jfloat tx, ty;
    jfloat xMin, xMax;
    jfloat yMin, yMax;

    xMin = xMax = coords[0];
    yMin = yMax = coords[1];

    CALC_MIN(xMin, coords[2]);
    CALC_MAX(xMax, coords[2]);
    CALC_MIN(yMin, coords[3]);
    CALC_MAX(yMax, coords[3]);
    CALC_MIN(xMin, coords[4]);
    CALC_MAX(xMax, coords[4]);
    CALC_MIN(yMin, coords[5]);
    CALC_MAX(yMax, coords[5]);
    CALC_MIN(xMin, coords[6]);
    CALC_MAX(xMax, coords[6]);
    CALC_MIN(yMin, coords[7]);
    CALC_MAX(yMax, coords[7]);

    if (hnd->clipMode == PH_MODE_DRAW_CLIP) {

       /* In case of drawing we could just skip curves which are completely
        * out of bounds
        */
        if (hnd->dhnd->xMaxf < xMin || hnd->dhnd->xMinf > xMax ||
            hnd->dhnd->yMaxf < yMin || hnd->dhnd->yMinf > yMax) {
            return;
        }
    } else {

       /* In case of filling we could skip curves which are above,
        * below and behind the right boundary of the visible area
        */

        if (hnd->dhnd->yMaxf < yMin || hnd->dhnd->yMinf > yMax ||
            hnd->dhnd->xMaxf < xMin)
        {
            return;
        }

       /* We could clamp x coordinates to the corresponding boundary
        * if the curve is completely behind the left one
        */

        if (hnd->dhnd->xMinf > xMax) {
            coords[0] = coords[2] = coords[4] = coords[6] =
                hnd->dhnd->xMinf;
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
                            * boundary of the visible area. We know that the
                            * curve is visible, so the check is pretty simple
                            */
                           hnd->dhnd->xMinf > xMin || hnd->dhnd->xMaxf < xMax ||
                           hnd->dhnd->yMinf > yMin || hnd->dhnd->yMaxf < yMax,
                           pixelInfo);
    }
}

/*
 * Bite the piece of the cubic curve from start point till the point
 * corresponding to the specified parameter then call ProcessMonotonicCubic for
 * the bitten part.
 * Note: coords array will be changed
 */
static void ProcessFirstMonotonicPartOfCubic(ProcessHandler* hnd,
                                             jfloat* coords, jint* pixelInfo,
                                             jfloat t)
{
    jfloat coords1[8];
    jfloat tx, ty;

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

/*
 * Split cubic curve into monotonic in X and Y parts. Calling ProcessCubic for
 * each monotonic piece of the curve.
 *
 * Note: coords array could be changed
 */
static void ProcessCubic(ProcessHandler* hnd, jfloat* coords, jint* pixelInfo)
{
    /* Temporary array for holding parameters corresponding to the extreme in X
     * and Y points. The values are inside the (0,1) range (0 and 1 excluded)
     * and in ascending order.
     */
    double params[4];
    jint cnt = 0, i;

    /* Simple check for monotonicity in X before searching for the extreme
     * points of the X(t) function. We first check if the curve is monotonic in
     * X by seeing if all of the X coordinates are strongly ordered.
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
        double ax = -coords[0] + 3*coords[2] - 3*coords[4] + coords[6];
        double bx = 2*(coords[0] - 2*coords[2] + coords[4]);
        double cx = -coords[0] + coords[2];

        SOLVEQUADINRANGE(ax,bx,cx,params,cnt);
    }

    /* Simple check for monotonicity in Y before searching for the extreme
     * points of the Y(t) function. We first check if the curve is monotonic in
     * Y by seeing if all of the Y coordinates are strongly ordered.
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
        double ay = -coords[1] + 3*coords[3] - 3*coords[5] + coords[7];
        double by = 2*(coords[1] - 2*coords[3] + coords[5]);
        double cy = -coords[1] + coords[3];

        SOLVEQUADINRANGE(ay,by,cy,params,cnt);
    }

    if (cnt > 0) {
        /* Sorting parameter values corresponding to the extremum points of
         * the curve. We are using insertion sort because of tiny size of the
         * array.
         */
        jint j;

        for(i = 1; i < cnt; i++) {
            double value = params[i];
            for (j = i - 1; j >= 0 && params[j] > value; j--) {
                params[j + 1] = params[j];
            }
            params[j + 1] = value;
        }

        /* Processing obtained monotonic parts */
        ProcessFirstMonotonicPartOfCubic(hnd, coords, pixelInfo,
                                         (jfloat)params[0]);
        for (i = 1; i < cnt; i++) {
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

static void ProcessLine(ProcessHandler* hnd,
                        jfloat *coord1, jfloat *coord2, jint* pixelInfo) {

    jfloat xMin, yMin, xMax, yMax;
    jint X1, Y1, X2, Y2, X3, Y3, res;
    jboolean clipped = JNI_FALSE;
    jfloat x1 = coord1[0];
    jfloat y1 = coord1[1];
    jfloat x2 = coord2[0];
    jfloat y2 = coord2[1];
    jfloat x3,y3;

    jboolean lastClipped;

    xMin = hnd->dhnd->xMinf;
    yMin = hnd->dhnd->yMinf;
    xMax = hnd->dhnd->xMaxf;
    yMax = hnd->dhnd->yMaxf;

    TESTANDCLIP(yMin, yMax, y1, x1, y2, x2, jfloat, res);
    if (res == CRES_INVISIBLE) return;
    clipped = IS_CLIPPED(res);
    TESTANDCLIP(yMin, yMax, y2, x2, y1, x1, jfloat, res);
    if (res == CRES_INVISIBLE) return;
    lastClipped = IS_CLIPPED(res);
    clipped = clipped || lastClipped;

    if (hnd->clipMode == PH_MODE_DRAW_CLIP) {
        TESTANDCLIP(xMin, xMax,
                    x1, y1, x2, y2, jfloat, res);
        if (res == CRES_INVISIBLE) return;
        clipped = clipped || IS_CLIPPED(res);
        TESTANDCLIP(xMin, xMax,
                    x2, y2, x1, y1, jfloat, res);
        if (res == CRES_INVISIBLE) return;
        lastClipped = lastClipped || IS_CLIPPED(res);
        clipped = clipped || lastClipped;
        X1 = (jint)(x1*MDP_MULT);
        Y1 = (jint)(y1*MDP_MULT);
        X2 = (jint)(x2*MDP_MULT);
        Y2 = (jint)(y2*MDP_MULT);

        hnd->pProcessFixedLine(hnd, X1, Y1, X2, Y2, pixelInfo,
                               clipped, /* enable boundary checking in case
                                           of clipping to avoid entering
                                           out of bounds which could
                                           happens during rounding
                                         */
                               lastClipped /* Notify pProcessFixedLine that
                                              this is the end of the
                                              subpath (because of exiting
                                              out of boundaries)
                                            */
                               );
    } else {
        /* Clamping starting from first vertex of the processed segment
         */
        CLIPCLAMP(xMin, xMax, x1, y1, x2, y2, x3, y3, jfloat, res);
        X1 = (jint)(x1*MDP_MULT);
        Y1 = (jint)(y1*MDP_MULT);

        /* Clamping only by left boundary */
        if (res == CRES_MIN_CLIPPED) {
            X3 = (jint)(x3*MDP_MULT);
            Y3 = (jint)(y3*MDP_MULT);
            hnd->pProcessFixedLine(hnd, X3, Y3, X1, Y1, pixelInfo,
                                   JNI_FALSE, lastClipped);

        } else if (res == CRES_INVISIBLE) {
            return;
        }

        /* Clamping starting from last vertex of the processed segment
         */
        CLIPCLAMP(xMin, xMax, x2, y2, x1, y1, x3, y3, jfloat, res);

        /* Checking if there was a clip by right boundary */
        lastClipped = lastClipped || (res == CRES_MAX_CLIPPED);

        X2 = (jint)(x2*MDP_MULT);
        Y2 = (jint)(y2*MDP_MULT);
        hnd->pProcessFixedLine(hnd, X1, Y1, X2, Y2, pixelInfo,
                               JNI_FALSE, lastClipped);

        /* Clamping only by left boundary */
        if (res == CRES_MIN_CLIPPED) {
            X3 = (jint)(x3*MDP_MULT);
            Y3 = (jint)(y3*MDP_MULT);
            hnd->pProcessFixedLine(hnd, X2, Y2, X3, Y3, pixelInfo,
                                   JNI_FALSE, lastClipped);
        }
    }
}

jboolean ProcessPath(ProcessHandler* hnd,
                     jfloat transXf, jfloat transYf,
                     jfloat* coords, jint maxCoords,
                     jbyte* types, jint numTypes)
{
    jfloat tCoords[8];
    jfloat closeCoord[2];
    jint pixelInfo[5];
    jboolean skip = JNI_FALSE;
    jboolean subpathStarted = JNI_FALSE;
    jfloat lastX, lastY;
    int i, index = 0;

    pixelInfo[0] = 0;

    /* Adding support of the KEY_STROKE_CONTROL rendering hint.
     * Now we are supporting two modes: "pixels at centers" and
     * "pixels at corners".
     * First one is disabled by default but could be enabled by setting
     * VALUE_STROKE_PURE to the rendering hint. It means that pixel at the
     * screen (x,y) has (x + 0.5, y + 0.5) float coordinates.
     *
     * Second one is enabled by default and means straightforward mapping
     * (x,y) --> (x,y)
     *
     */
    if (hnd->stroke == PH_STROKE_PURE) {
        closeCoord[0] = -0.5f;
        closeCoord[1] = -0.5f;
        transXf -= 0.5;
        transYf -= 0.5;
    } else {
        closeCoord[0] = 0.0f;
        closeCoord[1] = 0.0f;
    }

    /* Adjusting boundaries to the capabilities of the ProcessPath code */
    ADJUST(hnd->dhnd->xMin, LOWER_OUT_BND, UPPER_OUT_BND);
    ADJUST(hnd->dhnd->yMin, LOWER_OUT_BND, UPPER_OUT_BND);
    ADJUST(hnd->dhnd->xMax, LOWER_OUT_BND, UPPER_OUT_BND);
    ADJUST(hnd->dhnd->yMax, LOWER_OUT_BND, UPPER_OUT_BND);


    /*                Setting up fractional clipping box
     *
     * We are using following float -> int mapping:
     *
     *      xi = floor(xf + 0.5)
     *
     * So, fractional values that hit the [xmin, xmax) integer interval will be
     * situated inside the [xmin-0.5, xmax - 0.5) fractional interval. We are
     * using EPSF constant to provide that upper boundary is not included.
     */
    hnd->dhnd->xMinf = hnd->dhnd->xMin - 0.5f;
    hnd->dhnd->yMinf = hnd->dhnd->yMin - 0.5f;
    hnd->dhnd->xMaxf = hnd->dhnd->xMax - 0.5f - EPSF;
    hnd->dhnd->yMaxf = hnd->dhnd->yMax - 0.5f - EPSF;


    for (i = 0; i < numTypes; i++) {
        switch (types[i]) {
            case java_awt_geom_PathIterator_SEG_MOVETO:
                if (index + 2 <= maxCoords) {
                    /* Performing closing of the unclosed segments */
                    if (subpathStarted & !skip) {
                        if (hnd->clipMode == PH_MODE_FILL_CLIP) {
                            if (tCoords[0] != closeCoord[0] ||
                                tCoords[1] != closeCoord[1])
                            {
                                ProcessLine(hnd, tCoords, closeCoord,
                                            pixelInfo);
                            }
                        }
                        hnd->pProcessEndSubPath(hnd);
                    }

                    tCoords[0] = coords[index++] + transXf;
                    tCoords[1] = coords[index++] + transYf;

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
                        subpathStarted = JNI_TRUE;
                        skip = JNI_FALSE;
                        closeCoord[0] = tCoords[0];
                        closeCoord[1] = tCoords[1];
                    } else {
                        skip = JNI_TRUE;
                    }
                } else {
                    return JNI_FALSE;
                }
                break;
            case java_awt_geom_PathIterator_SEG_LINETO:
                if (index + 2 <= maxCoords) {
                    lastX = tCoords[2] = coords[index++] + transXf;
                    lastY = tCoords[3] = coords[index++] + transYf;

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
                            subpathStarted = JNI_TRUE;
                            skip = JNI_FALSE;
                        } else {
                            ProcessLine(hnd, tCoords, tCoords + 2,
                                        pixelInfo);
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                } else {
                    return JNI_FALSE;
                }
                break;
            case java_awt_geom_PathIterator_SEG_QUADTO:
                if (index + 4 <= maxCoords) {
                    tCoords[2] = coords[index++] + transXf;
                    tCoords[3] = coords[index++] + transYf;
                    lastX = tCoords[4] = coords[index++] + transXf;
                    lastY = tCoords[5] = coords[index++] + transYf;

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
                            subpathStarted = JNI_TRUE;
                            skip = JNI_FALSE;
                        } else {
                            if (tCoords[2] < UPPER_BND &&
                                tCoords[2] > LOWER_BND &&
                                tCoords[3] < UPPER_BND &&
                                tCoords[3] > LOWER_BND)
                            {
                                ProcessQuad(hnd, tCoords, pixelInfo);
                            } else {
                                ProcessLine(hnd, tCoords,
                                            tCoords + 4, pixelInfo);
                            }
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                } else {
                    return JNI_FALSE;
                }
                break;
            case java_awt_geom_PathIterator_SEG_CUBICTO:
                    if (index + 6 <= maxCoords) {
                    tCoords[2] = coords[index++] + transXf;
                    tCoords[3] = coords[index++] + transYf;
                    tCoords[4] = coords[index++] + transXf;
                    tCoords[5] = coords[index++] + transYf;
                    lastX = tCoords[6] = coords[index++] + transXf;
                    lastY = tCoords[7] = coords[index++] + transYf;

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
                            subpathStarted = JNI_TRUE;
                            skip = JNI_FALSE;
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
                                ProcessLine(hnd, tCoords, tCoords + 6,
                                            pixelInfo);
                            }
                            tCoords[0] = lastX;
                            tCoords[1] = lastY;
                        }
                    }
                } else {
                    return JNI_FALSE;
                }
                break;
            case java_awt_geom_PathIterator_SEG_CLOSE:
                if (subpathStarted && !skip) {
                    skip = JNI_FALSE;
                    if (tCoords[0] != closeCoord[0] ||
                        tCoords[1] != closeCoord[1])
                    {
                        ProcessLine(hnd, tCoords, closeCoord, pixelInfo);
                        /* Storing last path's point for using in
                         * following segments without initial moveTo
                         */
                        tCoords[0] = closeCoord[0];
                        tCoords[1] = closeCoord[1];
                    }

                    hnd->pProcessEndSubPath(hnd);
                }

                break;
        }
    }

    /* Performing closing of the unclosed segments */
    if (subpathStarted & !skip) {
        if (hnd->clipMode == PH_MODE_FILL_CLIP) {
            if (tCoords[0] != closeCoord[0] ||
                tCoords[1] != closeCoord[1])
            {
                ProcessLine(hnd, tCoords, closeCoord,
                            pixelInfo);
            }
        }
        hnd->pProcessEndSubPath(hnd);
    }

    return JNI_TRUE;
}

/* TODO Add checking of the result of the malloc/realloc functions to handle
 * out of memory error and don't leak earlier allocated data
 */


#define ALLOC(ptr, type, n) \
    ptr = (type *)malloc((n)*sizeof(type))
#define REALLOC(ptr, type, n) \
    ptr = (type *)realloc(ptr, (n)*sizeof(type))


struct _Edge;

typedef struct _Point {
    jint x;
    jint y;
    jboolean lastPoint;
    struct _Point* prev;
    struct _Point* next;
    struct _Point* nextByY;
    jboolean endSL;
    struct _Edge* edge;
} Point;


typedef struct _Edge {
    jint          x;
    jint          dx;
    Point*        p;
    jint          dir;
    struct _Edge* prev;
    struct _Edge* next;
} Edge;

/* Size of the default buffer in the FillData structure. This buffer is
 * replaced with heap allocated in case of large paths.
 */
#define DF_MAX_POINT 256

/* Following structure accumulates points of the non-continuous flattened
 * path during iteration through the origin path's segments . The end
 * of the each subpath is marked as lastPoint flag set at the last point
 */

typedef struct {
    Point   *plgPnts;
    Point   dfPlgPnts[DF_MAX_POINT];
    jint    plgSize;
    jint    plgMax;
    jint    plgYMin;
    jint    plgYMax;
} FillData;

#define FD_INIT(PTR)                                                        \
    do {                                                                    \
        (PTR)->plgPnts = (PTR)->dfPlgPnts;                                  \
        (PTR)->plgSize = 0;                                                 \
        (PTR)->plgMax = DF_MAX_POINT;                                       \
    } while(0)

#define FD_ADD_POINT(PTR, X, Y, LASTPT)                                     \
    do {                                                                    \
        Point* _pnts = (PTR)->plgPnts;                                      \
        jint _size = (PTR)->plgSize;                                        \
        if (_size >= (PTR)->plgMax) {                                       \
            jint newMax = (PTR)->plgMax*2;                                  \
            if ((PTR)->plgPnts == (PTR)->dfPlgPnts) {                       \
                (PTR)->plgPnts = (Point*)malloc(newMax*sizeof(Point));      \
                memcpy((PTR)->plgPnts, _pnts, _size*sizeof(Point));         \
            } else {                                                        \
                (PTR)->plgPnts = (Point*)realloc(                           \
                    _pnts, newMax*sizeof(Point));                           \
            }                                                               \
            _pnts = (PTR)->plgPnts;                                         \
            (PTR)->plgMax = newMax;                                         \
        }                                                                   \
        _pnts += _size;                                                     \
        _pnts->x = X;                                                       \
        _pnts->y = Y;                                                       \
        _pnts->lastPoint = LASTPT;                                          \
        if (_size) {                                                        \
            if ((PTR)->plgYMin > Y) (PTR)->plgYMin = Y;                     \
            if ((PTR)->plgYMax < Y) (PTR)->plgYMax = Y;                     \
        } else {                                                            \
            (PTR)->plgYMin = Y;                                             \
            (PTR)->plgYMax = Y;                                             \
        }                                                                   \
        (PTR)->plgSize = _size + 1;                                         \
    } while(0)


#define FD_FREE_POINTS(PTR)                                                 \
    do {                                                                    \
        if ((PTR)->plgPnts != (PTR)->dfPlgPnts) {                           \
            free((PTR)->plgPnts);                                           \
        }                                                                   \
    } while(0)

#define FD_IS_EMPTY(PTR) (!((PTR)->plgSize))

#define FD_IS_ENDED(PTR) ((PTR)->plgPnts[(PTR)->plgSize - 1].lastPoint)

#define FD_SET_ENDED(PTR)                                                   \
    do {                                                                    \
        (PTR)->plgPnts[(PTR)->plgSize - 1].lastPoint = JNI_TRUE;            \
    } while(0)

#define PFD(HND) ((FillData*)(HND)->pData)

/* Bubble sorting in the ascending order of the linked list. This
 * implementation stops processing the list if there were no changes during the
 * previous pass.
 *
 * LIST - ptr to the ptr to the first element of the list
 * ETYPE - type of the element in the list
 * NEXT - accessor to the next field in the list element
 * GET_LKEY - accessor to the key of the list element
 */
#define LBUBBLE_SORT(LIST, ETYPE, NEXT, GET_LKEY)                           \
    do {                                                                    \
        ETYPE *p, *q, *r, *s = NULL, *temp ;                                \
        jint wasSwap = 1;                                                   \
        /* r precedes p and s points to the node up to which comparisons    \
         * are to be made */                                                \
        while ( s != NEXT(*LIST) && wasSwap) {                              \
            r = p = *LIST;                                                  \
            q = NEXT(p);                                                    \
            wasSwap = 0;                                                    \
            while ( p != s ) {                                              \
                if (GET_LKEY(p) >= GET_LKEY(q)) {                           \
                    wasSwap = 1;                                            \
                    if ( p == *LIST ) {                                     \
                        temp = NEXT(q);                                     \
                        NEXT(q) = p ;                                       \
                        NEXT(p) = temp ;                                    \
                        *LIST = q ;                                         \
                        r = q ;                                             \
                    } else {                                                \
                        temp = NEXT(q);                                     \
                        NEXT(q) = p ;                                       \
                        NEXT(p) = temp ;                                    \
                        NEXT(r) = q ;                                       \
                        r = q ;                                             \
                    }                                                       \
                } else {                                                    \
                    r = p ;                                                 \
                    p = NEXT(p);                                            \
                }                                                           \
                q = NEXT(p);                                                \
                if ( q == s ) s = p ;                                       \
            }                                                               \
        }                                                                   \
    } while(0);

/* Accessors for the Edge structure to work with LBUBBLE_SORT */
#define GET_ACTIVE_KEY(a) (a->x)
#define GET_ACTIVE_NEXT(a) ((a)->next)

/* TODO: Implement stack/heap allocation technique for active edges
 */
#define DELETE_ACTIVE(head,pnt)                                     \
do {                                                                \
    Edge *prevp = pnt->prev;                                        \
    Edge *nextp = pnt->next;                                        \
    if (prevp) {                                                    \
        prevp->next = nextp;                                        \
    } else {                                                        \
        head = nextp;                                               \
    }                                                               \
    if (nextp) {                                                    \
        nextp->prev = prevp;                                        \
    }                                                               \
} while(0);

#define INSERT_ACTIVE(head,pnt,cy)                                  \
do {                                                                \
    Point *np = pnt->next;                                          \
    Edge *ne = active + nact;                                       \
    if (pnt->y == np->y) {                                          \
        /* Skipping horizontal segments */                          \
        break;                                                      \
    } else {                                                        \
        jint dX = np->x - pnt->x;                                   \
        jint dY = np->y - pnt->y;                                   \
        jint dy;                                                    \
        if (pnt->y < np->y) {                                       \
            ne->dir = -1;                                           \
            ne->p = pnt;                                            \
            ne->x = pnt->x;                                         \
            dy = cy - pnt->y;                                       \
        } else { /* pnt->y > np->y */                               \
            ne->dir = 1;                                            \
            ne->p = np;                                             \
            ne->x = np->x;                                          \
            dy = cy - np->y;                                        \
        }                                                           \
                                                                    \
        /* We need to worry only about dX because dY is in        */\
        /* denominator and abs(dy) < MDP_MULT (cy is a first      */\
        /* scanline of the scan converted segment and we subtract */\
        /* y coordinate of the nearest segment's end from it to   */\
        /* obtain dy)                                             */\
        if (ABS32(dX) > CALC_BND) {                                 \
            ne->dx = (jint)((((jdouble)dX)*MDP_MULT)/dY);           \
            ne->x += (jint)((((jdouble)dX)*dy)/dY);                 \
        } else {                                                    \
            ne->dx = ((dX)<<MDP_PREC)/dY;                           \
            ne->x += (dX*dy)/dY;                                    \
        }                                                           \
    }                                                               \
    ne->next = head;                                                \
    ne->prev = NULL;                                                \
    if (head) {                                                     \
        head->prev = ne;                                            \
    }                                                               \
    head = active + nact;                                           \
    pnt->edge = head;                                               \
    nact++;                                                         \
} while(0);

void FillPolygon(ProcessHandler* hnd,
                 jint fillRule) {
    jint k, y, xl, xr;
    jint drawing;
    Edge* activeList, *active;
    Edge* curEdge, *prevEdge;
    jint nact;
    jint n;
    Point* pt, *curpt, *ept;
    Point** yHash;
    Point** curHash;
    jint rightBnd = hnd->dhnd->xMax - 1;
    FillData* pfd = (FillData*)(hnd->pData);
    jint yMin = pfd->plgYMin;
    jint yMax = pfd->plgYMax;
    jint hashSize = ((yMax - yMin)>>MDP_PREC) + 4;

    /* Because of support of the KEY_STROKE_CONTROL hint we are performing
     * shift of the coordinates at the higher level
     */
    jint hashOffset = ((yMin - 1) & MDP_W_MASK);

// TODO creating lists using fake first element to avoid special casing of
// the first element in the list (which otherwise should be performed in each
// list operation)

    /* Winding counter */
    jint counter;

    /* Calculating mask to be applied to the winding counter */
    jint counterMask =
        (fillRule == java_awt_geom_PathIterator_WIND_NON_ZERO)? -1:1;
    pt = pfd->plgPnts;
    n = pfd->plgSize;

    if (n <=1) return;

    ALLOC(yHash, Point*, hashSize);
    for (k = 0; k < hashSize; k++) {
        yHash[k] = NULL;
    }

    ALLOC(active, Edge, n);

    /* Creating double linked list (prev, next links) describing path order and
     * hash table with points which fall between scanlines. nextByY link is
     * used for the points which are between same scanlines. Scanlines are
     * passed through the centers of the pixels.
     */
    curpt = pt;
    curpt->prev = NULL;
    ept = pt + n - 1;
    for (curpt = pt; curpt != ept; curpt++) {
        Point* nextpt = curpt + 1;
        curHash =  yHash + ((curpt->y - hashOffset - 1) >> MDP_PREC);
        curpt->nextByY = *curHash;
        *curHash = curpt;
        curpt->next = nextpt;
        nextpt->prev = curpt;
        curpt->edge = NULL;
    }

    curHash =  yHash + ((ept->y - hashOffset - 1) >> MDP_PREC);
    ept->nextByY = *curHash;
    *curHash = ept;
    ept->next = NULL;
    ept->edge = NULL;
    nact = 0;

    activeList = NULL;
    for (y=hashOffset + MDP_MULT,k = 0;
         y<=yMax && k < hashSize; y += MDP_MULT, k++)
    {
        for(pt = yHash[k];pt; pt=pt->nextByY) {
            /* pt->y should be inside hashed interval
             * assert(y-MDP_MULT <= pt->y && pt->y < y);
             */
            if (pt->prev && !pt->prev->lastPoint) {
                if (pt->prev->edge && pt->prev->y <= y) {
                    DELETE_ACTIVE(activeList, pt->prev->edge);
                    pt->prev->edge = NULL;
                } else  if (pt->prev->y > y) {
                    INSERT_ACTIVE(activeList, pt->prev, y);
                }
            }

            if (!pt->lastPoint && pt->next) {
                if (pt->edge && pt->next->y <= y) {
                    DELETE_ACTIVE(activeList, pt->edge);
                    pt->edge = NULL;
                } else if (pt->next->y > y) {
                    INSERT_ACTIVE(activeList, pt, y);
                }
            }
        }

        if (!activeList) continue;

        /* We could not use O(N) Radix sort here because in most cases list of
         * edges almost sorted. So, bubble sort (O(N^2))is working much
         * better. Note, in case of array of edges Shell sort is more
         * efficient.
         */
        LBUBBLE_SORT((&activeList), Edge, GET_ACTIVE_NEXT, GET_ACTIVE_KEY);

        /* Correction of the back links in the double linked edge list */
        curEdge=activeList;
        prevEdge = NULL;
        while (curEdge) {
            curEdge->prev = prevEdge;
            prevEdge = curEdge;
            curEdge = curEdge->next;
        }

        xl = xr = hnd->dhnd->xMin;
        curEdge = activeList;
        counter = 0;
        drawing = 0;
        for(;curEdge; curEdge = curEdge->next) {
            counter += curEdge->dir;
            if ((counter & counterMask) && !drawing) {
                xl = (curEdge->x + MDP_MULT - 1)>>MDP_PREC;
                drawing = 1;
            }

            if (!(counter & counterMask) && drawing) {
                xr = (curEdge->x - 1)>>MDP_PREC;
                if (xl <= xr) {
                    hnd->dhnd->pDrawScanline(hnd->dhnd, xl, xr, y >> MDP_PREC);
                }
                drawing = 0;
            }

            curEdge->x += curEdge->dx;
        }

        /* Performing drawing till the right boundary (for correct rendering
         * shapes clipped at the right side)
         */
        if (drawing && xl <= rightBnd) {
            hnd->dhnd->pDrawScanline(hnd->dhnd, xl, rightBnd, y >> MDP_PREC);
        }
    }
    free(active);
    free(yHash);
}



void  StoreFixedLine(ProcessHandler* hnd,jint x1,jint y1,jint x2,jint y2,
                     jint* pixelInfo,jboolean checkBounds,
                     jboolean endSubPath)  {
    FillData* pfd;
    jint outXMin, outXMax, outYMin, outYMax;
    jint x3, y3, res;

    /* There is no need to round line coordinates to the forward differencing
     * precision anymore. Such a rounding was used for preventing the curve go
     * out the endpoint (this sometimes does not help). The problem was fixed
     * in the forward differencing loops.
     */

    if (checkBounds) {
        jboolean lastClipped = JNI_FALSE;

        /* This function is used only for filling shapes, so there is no
         * check for the type of clipping
         */
        outXMin = (jint)(hnd->dhnd->xMinf * MDP_MULT);
        outXMax = (jint)(hnd->dhnd->xMaxf * MDP_MULT);
        outYMin = (jint)(hnd->dhnd->yMinf * MDP_MULT);
        outYMax = (jint)(hnd->dhnd->yMaxf * MDP_MULT);

        TESTANDCLIP(outYMin, outYMax, y1, x1, y2, x2, jint, res);
        if (res == CRES_INVISIBLE) return;
        TESTANDCLIP(outYMin, outYMax, y2, x2, y1, x1, jint, res);
        if (res == CRES_INVISIBLE) return;
        lastClipped = IS_CLIPPED(res);

        /* Clamping starting from first vertex of the processed segment */
        CLIPCLAMP(outXMin, outXMax, x1, y1, x2, y2, x3, y3, jint, res);

        /* Clamping only by left boundary */
        if (res == CRES_MIN_CLIPPED) {
            StoreFixedLine(hnd, x3, y3, x1, y1, pixelInfo,
                           JNI_FALSE, lastClipped);

        } else if (res == CRES_INVISIBLE) {
            return;
        }

        /* Clamping starting from last vertex of the processed segment */
        CLIPCLAMP(outXMin, outXMax, x2, y2, x1, y1, x3, y3, jint, res);

        /* Checking if there was a clip by right boundary */
        lastClipped = lastClipped || (res == CRES_MAX_CLIPPED);

        StoreFixedLine(hnd, x1, y1, x2, y2, pixelInfo,
                         JNI_FALSE, lastClipped);

        /* Clamping only by left boundary */
        if (res == CRES_MIN_CLIPPED) {
            StoreFixedLine(hnd, x2, y2, x3, y3, pixelInfo,
                           JNI_FALSE, lastClipped);
        }

        return;
    }
    pfd = (FillData*)(hnd->pData);

    /* Adding first point of the line only in case of empty or just finished
     * path
     */
    if (FD_IS_EMPTY(pfd) || FD_IS_ENDED(pfd)) {
        FD_ADD_POINT(pfd, x1, y1, JNI_FALSE);
    }

    FD_ADD_POINT(pfd, x2, y2, JNI_FALSE);

    if (endSubPath) {
        FD_SET_ENDED(pfd);
    }
}


static void endSubPath(ProcessHandler* hnd) {
    FillData* pfd = (FillData*)(hnd->pData);
    if (!FD_IS_EMPTY(pfd)) {
        FD_SET_ENDED(pfd);
    }
}

static void stubEndSubPath(ProcessHandler* hnd) {
}

JNIEXPORT jboolean JNICALL
doFillPath(DrawHandler* dhnd,
                    jint transX, jint transY,
                    jfloat* coords, jint maxCoords,
                    jbyte* types, jint numTypes,
                    PHStroke stroke, jint fillRule)
{
    jint res;

    FillData fillData;

    ProcessHandler hnd =
    {
        &StoreFixedLine,
        &endSubPath,
        NULL,
        PH_STROKE_DEFAULT,
        PH_MODE_FILL_CLIP,
        NULL
    };

    /* Initialization of the following fields in the declaration of the hnd
     * above causes warnings on sun studio compiler with  -xc99=%none option
     * applied (this option means compliance with C90 standard instead of C99)
     */
    hnd.dhnd = dhnd;
    hnd.pData = &fillData;
    hnd.stroke = stroke;

    FD_INIT(&fillData);
    res = ProcessPath(&hnd, (jfloat)transX, (jfloat)transY,
                      coords, maxCoords, types, numTypes);
    if (!res) {
        FD_FREE_POINTS(&fillData);
        return JNI_FALSE;
    }
    FillPolygon(&hnd, fillRule);
    FD_FREE_POINTS(&fillData);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
doDrawPath(DrawHandler* dhnd,
                    void (*pProcessEndSubPath)(ProcessHandler*),
                    jint transX, jint transY,
                    jfloat* coords, jint maxCoords,
                    jbyte* types, jint numTypes, PHStroke stroke)
{
    ProcessHandler hnd =
    {
        &ProcessFixedLine,
        NULL,
        NULL,
        PH_STROKE_DEFAULT,
        PH_MODE_DRAW_CLIP,
        NULL
    };

    /* Initialization of the following fields in the declaration of the hnd
     * above causes warnings on sun studio compiler with  -xc99=%none option
     * applied (this option means compliance with C90 standard instead of C99)
     */
    hnd.dhnd = dhnd;
    hnd.stroke = stroke;

    hnd.pProcessEndSubPath = (pProcessEndSubPath == NULL)?
        stubEndSubPath : pProcessEndSubPath;
    return ProcessPath(&hnd, (jfloat)transX, (jfloat)transY, coords, maxCoords,
                       types, numTypes);
}
