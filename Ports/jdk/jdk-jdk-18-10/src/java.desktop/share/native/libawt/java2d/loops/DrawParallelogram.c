/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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

#include "math.h"
#include "GraphicsPrimitiveMgr.h"
#include "LineUtils.h"
#include "Trace.h"
#include "ParallelogramUtils.h"

#include "sun_java2d_loops_DrawParallelogram.h"

#define HANDLE_PGRAM_EDGE(X1, Y1, X2, Y2, \
                          pRasInfo, pixel, pPrim, pFunc, pCompInfo) \
    do { \
         jint ix1 = (jint) floor(X1); \
         jint ix2 = (jint) floor(X2); \
         jint iy1 = (jint) floor(Y1); \
         jint iy2 = (jint) floor(Y2); \
         LineUtils_ProcessLine(pRasInfo, pixel, \
                               pFunc, pPrim, pCompInfo, \
                               ix1, iy1, ix2, iy2, JNI_TRUE); \
    } while (0)

typedef struct {
    jdouble x0;
    jdouble y0;
    jdouble y1;
    jdouble slope;
    jlong dx;
    jint ystart;
    jint yend;
} EdgeInfo;

#define STORE_EDGE(pEDGE, X0, Y0, Y1, SLOPE, DELTAX) \
    do { \
        (pEDGE)->x0 = (X0); \
        (pEDGE)->y0 = (Y0); \
        (pEDGE)->y1 = (Y1); \
        (pEDGE)->slope = (SLOPE); \
        (pEDGE)->dx = (DELTAX); \
        (pEDGE)->ystart = (jint) floor((Y0) + 0.5); \
        (pEDGE)->yend   = (jint) floor((Y1) + 0.5); \
    } while (0)

#define STORE_PGRAM(pLTEDGE, pRTEDGE, \
                    X0, Y0, dX1, dY1, dX2, dY2, \
                    SLOPE1, SLOPE2, DELTAX1, DELTAX2) \
    do { \
        STORE_EDGE((pLTEDGE)+0, \
                   (X0), (Y0), (Y0) + (dY1), \
                   (SLOPE1), (DELTAX1)); \
        STORE_EDGE((pRTEDGE)+0, \
                   (X0), (Y0), (Y0) + (dY2), \
                   (SLOPE2), (DELTAX2)); \
        STORE_EDGE((pLTEDGE)+1, \
                   (X0) + (dX1), (Y0) + (dY1), (Y0) + (dY1) + (dY2), \
                   (SLOPE2), (DELTAX2)); \
        STORE_EDGE((pRTEDGE)+1, \
                   (X0) + (dX2), (Y0) + (dY2), (Y0) + (dY1) + (dY2), \
                   (SLOPE1), (DELTAX1)); \
    } while (0)

/*
 * Class:     sun_java2d_loops_DrawParallelogram
 * Method:    DrawParallelogram
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;DDDDDDDD)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawParallelogram_DrawParallelogram
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData,
     jdouble x0, jdouble y0,
     jdouble dx1, jdouble dy1,
     jdouble dx2, jdouble dy2,
     jdouble lw1, jdouble lw2)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint pixel;
    EdgeInfo edges[8];
    EdgeInfo *active[4];
    jint ix1, iy1, ix2, iy2;
    jdouble ldx1, ldy1, ldx2, ldy2;
    jdouble ox0, oy0;

    /*
     * Sort parallelogram by y values, ensure that each delta vector
     * has a non-negative y delta.
     */
    SORT_PGRAM(x0, y0, dx1, dy1, dx2, dy2,
               v = lw1; lw1 = lw2; lw2 = v;);

    // dx,dy for line width in the "1" and "2" directions.
    ldx1 = dx1 * lw1;
    ldy1 = dy1 * lw1;
    ldx2 = dx2 * lw2;
    ldy2 = dy2 * lw2;

    // calculate origin of the outer parallelogram
    ox0 = x0 - (ldx1 + ldx2) / 2.0;
    oy0 = y0 - (ldy1 + ldy2) / 2.0;

    PGRAM_MIN_MAX(ix1, ix2, ox0, dx1+ldx1, dx2+ldx2, JNI_FALSE);
    iy1 = (jint) floor(oy0 + 0.5);
    iy2 = (jint) floor(oy0 + dy1 + ldy1 + dy2 + ldy2 + 0.5);

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    pixel = GrPrim_Sg2dGetPixel(env, sg2d);
    if (pPrim->pCompType->getCompInfo != NULL) {
        GrPrim_Sg2dGetCompInfo(env, sg2d, pPrim, &compInfo);
    }

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == NULL) {
        return;
    }

    GrPrim_Sg2dGetClip(env, sg2d, &rasInfo.bounds);
    SurfaceData_IntersectBoundsXYXY(&rasInfo.bounds, ix1, iy1, ix2, iy2);
    if (rasInfo.bounds.y2 <= rasInfo.bounds.y1 ||
        rasInfo.bounds.x2 <= rasInfo.bounds.x1)
    {
        return;
    }

    if (sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags) != SD_SUCCESS) {
        return;
    }

    ix1 = rasInfo.bounds.x1;
    iy1 = rasInfo.bounds.y1;
    ix2 = rasInfo.bounds.x2;
    iy2 = rasInfo.bounds.y2;
    if (ix2 > ix1 && iy2 > iy1) {
        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase) {
            jdouble lslope, rslope;
            jlong ldx, rdx;
            jint loy, hiy, numedges;
            FillParallelogramFunc *pFill =
                pPrim->funcs.drawparallelogram->fillpgram;

            lslope = (dy1 == 0) ? 0 : dx1 / dy1;
            rslope = (dy2 == 0) ? 0 : dx2 / dy2;
            ldx = DblToLong(lslope);
            rdx = DblToLong(rslope);

            // Only need to generate 4 quads if the interior still
            // has a hole in it (i.e. if the line width ratios were
            // both less than 1.0)
            if (lw1 < 1.0 && lw2 < 1.0) {
                // If the line widths are both less than a pixel wide
                // then we can use a drawline function instead for even
                // more performance.
                lw1 = sqrt(ldx1*ldx1 + ldy1*ldy1);
                lw2 = sqrt(ldx2*ldx2 + ldy2*ldy2);
                if (lw1 <= 1.0001 && lw2 <= 1.0001) {
                    jdouble x3, y3;
                    DrawLineFunc *pLine =
                        pPrim->funcs.drawparallelogram->drawline;

                    x3 = (dx1 += x0);
                    y3 = (dy1 += y0);
                    x3 += dx2;
                    y3 += dy2;
                    dx2 += x0;
                    dy2 += y0;

                    HANDLE_PGRAM_EDGE( x0,  y0, dx1, dy1,
                                      &rasInfo, pixel, pPrim, pLine, &compInfo);
                    HANDLE_PGRAM_EDGE(dx1, dy1,  x3,  y3,
                                      &rasInfo, pixel, pPrim, pLine, &compInfo);
                    HANDLE_PGRAM_EDGE( x3,  y3, dx2, dy2,
                                      &rasInfo, pixel, pPrim, pLine, &compInfo);
                    HANDLE_PGRAM_EDGE(dx2, dy2,  x0,  y0,
                                      &rasInfo, pixel, pPrim, pLine, &compInfo);
                    SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
                    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
                    return;
                }

                // To simplify the edge management below we presort the
                // inner and outer edges so that they are globally sorted
                // from left to right.  If you scan across the array of
                // edges for a given Y range then the edges you encounter
                // will be sorted in X as well.
                // If AB are left top and bottom edges of outer parallelogram,
                // and CD are the right pair of edges, and abcd are the
                // corresponding inner parallelogram edges then we want them
                // sorted as ABabcdCD to ensure this horizontal ordering.
                // Conceptually it is like 2 pairs of nested parentheses.
                STORE_PGRAM(edges + 2, edges + 4,
                            ox0 + ldx1 + ldx2, oy0 + ldy1 + ldy2,
                            dx1 - ldx1, dy1 - ldy1,
                            dx2 - ldx2, dy2 - ldy2,
                            lslope, rslope, ldx, rdx);
                numedges = 8;
            } else {
                // The line width ratios were large enough to consume
                // the entire hole in the middle of the parallelogram
                // so we can just issue one large quad for the outer
                // parallelogram.
                numedges = 4;
            }

            // The outer parallelogram always goes in the first two
            // and last two entries in the array so we either have
            // ABabcdCD ordering for 8 edges or ABCD ordering for 4
            // edges.  See comment above where we store the inner
            // parallelogram for a more complete description.
            STORE_PGRAM(edges + 0, edges + numedges-2,
                        ox0, oy0,
                        dx1 + ldx1, dy1 + ldy1,
                        dx2 + ldx2, dy2 + ldy2,
                        lslope, rslope, ldx, rdx);

            loy = edges[0].ystart;
            if (loy < iy1) loy = iy1;
            while (loy < iy2) {
                jint numactive = 0;
                jint cur;

                hiy = iy2;
                // Maintaining a sorted edge list is probably overkill for
                // 4 or 8 edges.  The indices chosen above for storing the
                // inner and outer left and right edges already guarantee
                // left to right ordering so we just need to scan for edges
                // that overlap the current Y range (and also determine the
                // maximum Y value for which the range is valid).
                for (cur = 0; cur < numedges; cur++) {
                    EdgeInfo *pEdge = &edges[cur];
                    jint yend = pEdge->yend;
                    if (loy < yend) {
                        // This edge is still in play, have we reached it yet?
                        jint ystart = pEdge->ystart;
                        if (loy < ystart) {
                            // This edge is not active (yet)
                            // Stop before we get to the top of it
                            if (hiy > ystart) hiy = ystart;
                        } else {
                            // This edge is active, store it
                            active[numactive++] = pEdge;
                            // And stop when we get to the bottom of it
                            if (hiy > yend) hiy = yend;
                        }
                    }
                }
#ifdef DEBUG
                if ((numactive & 1) != 0) {
                    J2dTraceLn1(J2D_TRACE_ERROR,
                                "DrawParallelogram: "
                                "ODD NUMBER OF PGRAM EDGES (%d)!!",
                                numactive);
                }
#endif
                for (cur = 0; cur < numactive; cur += 2) {
                    EdgeInfo *pLeft  = active[cur+0];
                    EdgeInfo *pRight = active[cur+1];
                    jlong lx = PGRAM_INIT_X(loy,
                                            pLeft->x0, pLeft->y0,
                                            pLeft->slope);
                    jlong rx = PGRAM_INIT_X(loy,
                                            pRight->x0, pRight->y0,
                                            pRight->slope);
                    (*pFill)(&rasInfo,
                             ix1, loy, ix2, hiy,
                             lx, pLeft->dx,
                             rx, pRight->dx,
                             pixel, pPrim, &compInfo);
                }
                loy = hiy;
            }
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
