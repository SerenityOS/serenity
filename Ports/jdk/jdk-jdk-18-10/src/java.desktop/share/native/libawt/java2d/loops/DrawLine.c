/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

#include "GraphicsPrimitiveMgr.h"

#include "LineUtils.h"

#include "sun_java2d_loops_DrawLine.h"

#define OUTCODE_TOP     1
#define OUTCODE_BOTTOM  2
#define OUTCODE_LEFT    4
#define OUTCODE_RIGHT   8

static void
RefineBounds(SurfaceDataBounds *bounds, jint x1, jint y1, jint x2, jint y2)
{
    jint min, max;
    if (x1 < x2) {
        min = x1;
        max = x2;
    } else {
        min = x2;
        max = x1;
    }
    max++;
    if (max <= min) {
        /* integer overflow */
        max--;
    }
    if (bounds->x1 < min) bounds->x1 = min;
    if (bounds->x2 > max) bounds->x2 = max;
    if (y1 < y2) {
        min = y1;
        max = y2;
    } else {
        min = y2;
        max = y1;
    }
    max++;
    if (max <= min) {
        /* integer overflow */
        max--;
    }
    if (bounds->y1 < min) bounds->y1 = min;
    if (bounds->y2 > max) bounds->y2 = max;
}

#define _out(v, vmin, vmax, cmin, cmax) \
    ((v < vmin) ? cmin : ((v > vmax) ? cmax : 0))

#define outcode(x, y, xmin, ymin, xmax, ymax) \
    (_out(y, ymin, ymax, OUTCODE_TOP, OUTCODE_BOTTOM) | \
     _out(x, xmin, xmax, OUTCODE_LEFT, OUTCODE_RIGHT))

/*
 * "Small" math here will be done if the coordinates are less
 * than 15 bits in range (-16384 => 16383).  This could be
 * expanded to 16 bits if we rearrange some of the math in
 * the normal version of SetupBresenham.
 * "Big" math here will be done with coordinates with 30 bits
 * of total range - 2 bits less than a jint holds.
 * Intermediate calculations for "Big" coordinates will be
 * done using jlong variables.
 */
#define OverflowsSmall(v)       ((v) != (((v) << 17) >> 17))
#define OverflowsBig(v)         ((v) != (((v) << 2) >> 2))
#define BIG_MAX                 ((1 << 29) - 1)
#define BIG_MIN                 (-(1 << 29))

#define SETUP_BRESENHAM(CALC_TYPE, ORIGX1, ORIGY1, ORIGX2, ORIGY2, SHORTEN) \
do { \
    jint X1 = ORIGX1, Y1 = ORIGY1, X2 = ORIGX2, Y2 = ORIGY2; \
    jint dx, dy, ax, ay; \
    jint cxmin, cymin, cxmax, cymax; \
    jint outcode1, outcode2; \
    jboolean xmajor; \
    jint errminor, errmajor; \
    jint error; \
    jint steps; \
 \
    dx = X2 - X1; \
    dy = Y2 - Y1; \
    ax = (dx < 0) ? -dx : dx; \
    ay = (dy < 0) ? -dy : dy; \
 \
    cxmin = pBounds->x1; \
    cymin = pBounds->y1; \
    cxmax = pBounds->x2 - 1; \
    cymax = pBounds->y2 - 1; \
    xmajor = (ax >= ay); \
 \
    outcode1 = outcode(X1, Y1, cxmin, cymin, cxmax, cymax); \
    outcode2 = outcode(X2, Y2, cxmin, cymin, cxmax, cymax); \
    while ((outcode1 | outcode2) != 0) { \
        CALC_TYPE xsteps, ysteps; \
        if ((outcode1 & outcode2) != 0) { \
            return JNI_FALSE; \
        } \
        if (outcode1 != 0) { \
            if (outcode1 & (OUTCODE_TOP | OUTCODE_BOTTOM)) { \
                if (outcode1 & OUTCODE_TOP) { \
                    Y1 = cymin; \
                } else { \
                    Y1 = cymax; \
                } \
                ysteps = Y1 - ORIGY1; \
                if (ysteps < 0) { \
                    ysteps = -ysteps; \
                } \
                xsteps = 2 * ysteps * ax + ay; \
                if (xmajor) { \
                    xsteps += ay - ax - 1; \
                } \
                xsteps = xsteps / (2 * ay); \
                if (dx < 0) { \
                    xsteps = -xsteps; \
                } \
                X1 = ORIGX1 + (jint) xsteps; \
            } else if (outcode1 & (OUTCODE_LEFT | OUTCODE_RIGHT)) { \
                if (outcode1 & OUTCODE_LEFT) { \
                    X1 = cxmin; \
                } else { \
                    X1 = cxmax; \
                } \
                xsteps = X1 - ORIGX1; \
                if (xsteps < 0) { \
                    xsteps = -xsteps; \
                } \
                ysteps = 2 * xsteps * ay + ax; \
                if (!xmajor) { \
                    ysteps += ax - ay - 1; \
                } \
                ysteps = ysteps / (2 * ax); \
                if (dy < 0) { \
                    ysteps = -ysteps; \
                } \
                Y1 = ORIGY1 + (jint) ysteps; \
            } \
            outcode1 = outcode(X1, Y1, cxmin, cymin, cxmax, cymax); \
        } else { \
            if (outcode2 & (OUTCODE_TOP | OUTCODE_BOTTOM)) { \
                if (outcode2 & OUTCODE_TOP) { \
                    Y2 = cymin; \
                } else { \
                    Y2 = cymax; \
                } \
                ysteps = Y2 - ORIGY2; \
                if (ysteps < 0) { \
                    ysteps = -ysteps; \
                } \
                xsteps = 2 * ysteps * ax + ay; \
                if (xmajor) { \
                    xsteps += ay - ax; \
                } else { \
                    xsteps -= 1; \
                } \
                xsteps = xsteps / (2 * ay); \
                if (dx > 0) { \
                    xsteps = -xsteps; \
                } \
                X2 = ORIGX2 + (jint) xsteps; \
            } else if (outcode2 & (OUTCODE_LEFT | OUTCODE_RIGHT)) { \
                if (outcode2 & OUTCODE_LEFT) { \
                    X2 = cxmin; \
                } else { \
                    X2 = cxmax; \
                } \
                xsteps = X2 - ORIGX2; \
                if (xsteps < 0) { \
                    xsteps = -xsteps; \
                } \
                ysteps = 2 * xsteps * ay + ax; \
                if (xmajor) { \
                    ysteps -= 1; \
                } else { \
                    ysteps += ax - ay; \
                } \
                ysteps = ysteps / (2 * ax); \
                if (dy > 0) { \
                    ysteps = -ysteps; \
                } \
                Y2 = ORIGY2 + (jint) ysteps; \
            } \
            outcode2 = outcode(X2, Y2, cxmin, cymin, cxmax, cymax); \
        } \
    } \
    *pStartX = X1; \
    *pStartY = Y1; \
 \
    if (xmajor) { \
        errmajor = ay * 2; \
        errminor = ax * 2; \
        *pBumpMajorMask = (dx < 0) ? BUMP_NEG_PIXEL : BUMP_POS_PIXEL; \
        *pBumpMinorMask = (dy < 0) ? BUMP_NEG_SCAN : BUMP_POS_SCAN; \
        ax = -ax; /* For clipping adjustment below */ \
        steps = X2 - X1; \
        if (X2 != ORIGX2) { \
            SHORTEN = 0; \
        } \
    } else { \
        errmajor = ax * 2; \
        errminor = ay * 2; \
        *pBumpMajorMask = (dy < 0) ? BUMP_NEG_SCAN : BUMP_POS_SCAN; \
        *pBumpMinorMask = (dx < 0) ? BUMP_NEG_PIXEL : BUMP_POS_PIXEL; \
        ay = -ay; /* For clipping adjustment below */ \
        steps = Y2 - Y1; \
        if (Y2 != ORIGY2) { \
            SHORTEN = 0; \
        } \
    } \
    if ((steps = ((steps >= 0) ? steps : -steps) + 1 - SHORTEN) == 0) { \
        return JNI_FALSE; \
    } \
    error = - (errminor / 2); \
    if (Y1 != ORIGY1) { \
        jint ysteps = Y1 - ORIGY1; \
        if (ysteps < 0) { \
            ysteps = -ysteps; \
        } \
        error += ysteps * ax * 2; \
    } \
    if (X1 != ORIGX1) { \
        jint xsteps = X1 - ORIGX1; \
        if (xsteps < 0) { \
            xsteps = -xsteps; \
        } \
        error += xsteps * ay * 2; \
    } \
    error += errmajor; \
    errminor -= errmajor; \
 \
    *pSteps = steps; \
    *pError = error; \
    *pErrMajor = errmajor; \
    *pErrMinor = errminor; \
} while (0)

static jboolean
LineUtils_SetupBresenhamBig(jint _x1, jint _y1, jint _x2, jint _y2,
                            jint shorten,
                            SurfaceDataBounds *pBounds,
                            jint *pStartX, jint *pStartY,
                            jint *pSteps, jint *pError,
                            jint *pErrMajor, jint *pBumpMajorMask,
                            jint *pErrMinor, jint *pBumpMinorMask)
{
    /*
     * Part of calculating the Bresenham parameters for line stepping
     * involves being able to store numbers that are twice the magnitude
     * of the biggest absolute difference in coordinates.  Since we
     * want the stepping parameters to be stored in jints, we then need
     * to avoid any absolute differences more than 30 bits.  Thus, we
     * need to preprocess the coordinates to reduce their range to 30
     * bits regardless of clipping.  We need to cut their range back
     * before we do the clipping because the Bresenham stepping values
     * need to be calculated based on the "unclipped" coordinates.
     *
     * Thus, first we perform a "pre-clipping" stage to bring the
     * coordinates within the 30-bit range and then we proceed to the
     * regular clipping procedure, pretending that these were the
     * original coordinates all along.  Since this operation occurs
     * based on a constant "pre-clip" rectangle of +/- 30 bits without
     * any consideration for the final clip, the rounding errors that
     * occur here will depend only on the line coordinates and be
     * invariant with respect to the particular device/user clip
     * rectangles in effect at the time.  Thus, rendering a given
     * large-range line will be consistent under a variety of
     * clipping conditions.
     */
    if (OverflowsBig(_x1) || OverflowsBig(_y1) ||
        OverflowsBig(_x2) || OverflowsBig(_y2))
    {
        /*
         * Use doubles to get us into range for "Big" arithmetic.
         *
         * The math of adjusting an endpoint for clipping can involve
         * an intermediate result with twice the number of bits as the
         * original coordinate range.  Since we want to maintain as
         * much as 30 bits of precision in the resulting coordinates,
         * we will get roundoff here even using IEEE double-precision
         * arithmetic which cannot carry 60 bits of mantissa.  Since
         * the rounding errors will be consistent for a given set
         * of input coordinates the potential roundoff error should
         * not affect the consistency of our rendering.
         */
        double X1d = _x1;
        double Y1d = _y1;
        double X2d = _x2;
        double Y2d = _y2;
        double DXd = X2d - X1d;
        double DYd = Y2d - Y1d;
        if (_x1 < BIG_MIN) {
            Y1d = _y1 + (BIG_MIN - _x1) * DYd / DXd;
            X1d = BIG_MIN;
        } else if (_x1 > BIG_MAX) {
            Y1d = _y1 - (_x1 - BIG_MAX) * DYd / DXd;
            X1d = BIG_MAX;
        }
        /* Use Y1d instead of _y1 for testing now as we may have modified it */
        if (Y1d < BIG_MIN) {
            X1d = _x1 + (BIG_MIN - _y1) * DXd / DYd;
            Y1d = BIG_MIN;
        } else if (Y1d > BIG_MAX) {
            X1d = _x1 - (_y1 - BIG_MAX) * DXd / DYd;
            Y1d = BIG_MAX;
        }
        if (_x2 < BIG_MIN) {
            Y2d = _y2 + (BIG_MIN - _x2) * DYd / DXd;
            X2d = BIG_MIN;
        } else if (_x2 > BIG_MAX) {
            Y2d = _y2 - (_x2 - BIG_MAX) * DYd / DXd;
            X2d = BIG_MAX;
        }
        /* Use Y2d instead of _y2 for testing now as we may have modified it */
        if (Y2d < BIG_MIN) {
            X2d = _x2 + (BIG_MIN - _y2) * DXd / DYd;
            Y2d = BIG_MIN;
        } else if (Y2d > BIG_MAX) {
            X2d = _x2 - (_y2 - BIG_MAX) * DXd / DYd;
            Y2d = BIG_MAX;
        }
        _x1 = (int) X1d;
        _y1 = (int) Y1d;
        _x2 = (int) X2d;
        _y2 = (int) Y2d;
    }

    SETUP_BRESENHAM(jlong, _x1, _y1, _x2, _y2, shorten);

    return JNI_TRUE;
}

jboolean
LineUtils_SetupBresenham(jint _x1, jint _y1, jint _x2, jint _y2,
                         jint shorten,
                         SurfaceDataBounds *pBounds,
                         jint *pStartX, jint *pStartY,
                         jint *pSteps, jint *pError,
                         jint *pErrMajor, jint *pBumpMajorMask,
                         jint *pErrMinor, jint *pBumpMinorMask)
{
    if (OverflowsSmall(_x1) || OverflowsSmall(_y1) ||
        OverflowsSmall(_x2) || OverflowsSmall(_y2))
    {
        return LineUtils_SetupBresenhamBig(_x1, _y1, _x2, _y2, shorten,
                                           pBounds,
                                           pStartX, pStartY,
                                           pSteps, pError,
                                           pErrMajor, pBumpMajorMask,
                                           pErrMinor, pBumpMinorMask);
    }

    SETUP_BRESENHAM(jint, _x1, _y1, _x2, _y2, shorten);

    return JNI_TRUE;
}

/*
 * Class:     sun_java2d_loops_DrawLine
 * Method:    DrawLine
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawLine_DrawLine
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData,
     jint x1, jint y1, jint x2, jint y2)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint pixel = GrPrim_Sg2dGetPixel(env, sg2d);

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    if (pPrim->pCompType->getCompInfo != NULL) {
        GrPrim_Sg2dGetCompInfo(env, sg2d, pPrim, &compInfo);
    }

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
        return;
    }

    GrPrim_Sg2dGetClip(env, sg2d, &rasInfo.bounds);

    RefineBounds(&rasInfo.bounds, x1, y1, x2, y2);

    if (sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags) != SD_SUCCESS) {
        return;
    }

    if (rasInfo.bounds.x2 > rasInfo.bounds.x1 &&
        rasInfo.bounds.y2 > rasInfo.bounds.y1)
    {
        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase) {
            LineUtils_ProcessLine(&rasInfo, pixel,
                                  pPrim->funcs.drawline, pPrim, &compInfo,
                                  x1, y1, x2, y2, 0);
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
