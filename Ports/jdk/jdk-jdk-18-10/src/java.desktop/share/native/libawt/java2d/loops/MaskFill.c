/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <string.h>
#include "GraphicsPrimitiveMgr.h"
#include "ParallelogramUtils.h"

#include "sun_java2d_loops_MaskFill.h"

/*
 * Class:     sun_java2d_loops_MaskFill
 * Method:    MaskFill
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Ljava/awt/Composite;IIII[BII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_MaskFill_MaskFill
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject comp,
     jint x, jint y, jint w, jint h,
     jbyteArray maskArray, jint maskoff, jint maskscan)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    if (pPrim->pCompType->getCompInfo != NULL) {
        (*pPrim->pCompType->getCompInfo)(env, &compInfo, comp);
    }

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
        return;
    }

    rasInfo.bounds.x1 = x;
    rasInfo.bounds.y1 = y;
    rasInfo.bounds.x2 = x + w;
    rasInfo.bounds.y2 = y + h;
    if (sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags) != SD_SUCCESS) {
        return;
    }

    if (rasInfo.bounds.x2 > rasInfo.bounds.x1 &&
        rasInfo.bounds.y2 > rasInfo.bounds.y1)
    {
        jint color = GrPrim_Sg2dGetEaRGB(env, sg2d);
        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase) {
            jint width = rasInfo.bounds.x2 - rasInfo.bounds.x1;
            jint height = rasInfo.bounds.y2 - rasInfo.bounds.y1;
            void *pDst = PtrCoord(rasInfo.rasBase,
                                  rasInfo.bounds.x1, rasInfo.pixelStride,
                                  rasInfo.bounds.y1, rasInfo.scanStride);
            unsigned char *pMask =
                (maskArray
                 ? (*env)->GetPrimitiveArrayCritical(env, maskArray, 0)
                 : 0);
            if (maskArray != NULL && pMask == NULL) {
                SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
                SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
                return;
            }
            maskoff += ((rasInfo.bounds.y1 - y) * maskscan +
                        (rasInfo.bounds.x1 - x));
            (*pPrim->funcs.maskfill)(pDst,
                                     pMask, maskoff, maskscan,
                                     width, height,
                                     color, &rasInfo,
                                     pPrim, &compInfo);
            if (pMask) {
                (*env)->ReleasePrimitiveArrayCritical(env, maskArray,
                                                      pMask, JNI_ABORT);
            }
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
   }
   SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}

#define MASK_BUF_LEN 1024

#define DblToMask(v) ((unsigned char) ((v)*255.9999))

/* Fills an aligned rectangle with potentially translucent edges. */
static void
fillAARect(NativePrimitive *pPrim, SurfaceDataRasInfo *pRasInfo,
           CompositeInfo *pCompInfo, jint color, unsigned char *pMask,
           void *pDst,
           jdouble x1, jdouble y1, jdouble x2, jdouble y2)
{
    jint cx1 = pRasInfo->bounds.x1;
    jint cy1 = pRasInfo->bounds.y1;
    jint cx2 = pRasInfo->bounds.x2;
    jint cy2 = pRasInfo->bounds.y2;
    jint rx1 = (jint) ceil(x1);
    jint ry1 = (jint) ceil(y1);
    jint rx2 = (jint) floor(x2);
    jint ry2 = (jint) floor(y2);
    jint width = cx2 - cx1;
    jint scan = pRasInfo->scanStride;
    /* Convert xy12 into the edge coverage fractions for those edges. */
    x1 = rx1-x1;
    y1 = ry1-y1;
    x2 = x2-rx2;
    y2 = y2-ry2;
    if (ry2 < ry1) {
        /* Accumulate bottom coverage into top coverage. */
        y1 = y1 + y2 - 1.0;
        /* prevent processing of "bottom fractional row" */
        ry2 = cy2;
    }
    if (rx2 < rx1) {
        /* Accumulate right coverage into left coverage. */
        x1 = x1 + x2 - 1.0;
        /* prevent processing of "right fractional column" */
        rx2 = cx2;
    }
    /* Check for a visible "top fractional row" and process it */
    if (cy1 < ry1) {
        unsigned char midcov = DblToMask(y1);
        jint x;
        for (x = 0; x < width; x++) {
            pMask[x] = midcov;
        }
        if (cx1 < rx1) {
            pMask[0] = DblToMask(y1 * x1);
        }
        if (cx2 > rx2) {
            pMask[width-1] = DblToMask(y1 * x2);
        }
        (*pPrim->funcs.maskfill)(pDst,
                                 pMask, 0, 0,
                                 width, 1,
                                 color, pRasInfo,
                                 pPrim, pCompInfo);
        pDst = PtrAddBytes(pDst, scan);
        cy1++;
    }
    /* Check for a visible "left fract, solid middle, right fract" section. */
    if (cy1 < ry2 && cy1 < cy2) {
        jint midh = ((ry2 < cy2) ? ry2 : cy2) - cy1;
        jint midx = cx1;
        void *pMid = pDst;
        /* First process the left "fractional column" if it is visible. */
        if (midx < rx1) {
            pMask[0] = DblToMask(x1);
            /* Note: maskscan == 0 means we reuse this value for every row. */
            (*pPrim->funcs.maskfill)(pMid,
                                     pMask, 0, 0,
                                     1, midh,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
            pMid = PtrAddBytes(pMid, pRasInfo->pixelStride);
            midx++;
        }
        /* Process the central solid section if it is visible. */
        if (midx < rx2 && midx < cx2) {
            jint midw = ((rx2 < cx2) ? rx2 : cx2) - midx;
            /* A NULL mask buffer means "all coverages are 0xff" */
            (*pPrim->funcs.maskfill)(pMid,
                                     NULL, 0, 0,
                                     midw, midh,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
            pMid = PtrCoord(pMid, midw, pRasInfo->pixelStride, 0, 0);
            midx += midw;
        }
        /* Finally process the right "fractional column" if it is visible. */
        if (midx < cx2) {
            pMask[0] = DblToMask(x2);
            /* Note: maskscan == 0 means we reuse this value for every row. */
            (*pPrim->funcs.maskfill)(pMid,
                                     pMask, 0, 0,
                                     1, midh,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
        }
        cy1 += midh;
        pDst = PtrCoord(pDst, 0, 0, midh, scan);
    }
    /* Check for a visible "bottom fractional row" and process it */
    if (cy1 < cy2) {
        unsigned char midcov = DblToMask(y2);
        jint x;
        for (x = 0; x < width; x++) {
            pMask[x] = midcov;
        }
        if (cx1 < rx1) {
            pMask[0] = DblToMask(y2 * x1);
        }
        if (cx2 > rx2) {
            pMask[width-1] = DblToMask(y2 * x2);
        }
        (*pPrim->funcs.maskfill)(pDst,
                                 pMask, 0, 0,
                                 width, 1,
                                 color, pRasInfo,
                                 pPrim, pCompInfo);
    }
}

/*
 * Support code for arbitrary tracing and MaskFill filling of
 * non-rectilinear (diagonal) parallelograms.
 *
 * This code is based upon the following model of AA coverage.
 *
 * Each edge of a parallelogram (for fillPgram) or a double
 * parallelogram (inner and outer parallelograms for drawPgram)
 * can be rasterized independently because the geometry is well
 * defined in such a way that none of the sides will ever cross
 * each other and they have a fixed ordering that is fairly
 * well predetermined.
 *
 * So, for each edge we will look at the diagonal line that
 * the edge makes as it passes through a row of pixels.  Some
 * such diagonal lines may pass entirely through the row of
 * pixels in a single pixel column.  Some may cut across the
 * row and pass through several pixel columns before they pass
 * on to the next row.
 *
 * As the edge passes through the row of pixels it will affect
 * the coverage of the pixels it passes through as well as all
 * of the pixels to the right of the edge.  The coverage will
 * either be increased (by a left edge of a parallelogram) or
 * decreased (by a right edge) for all pixels to the right, until
 * another edge passing the opposite direction is encountered.
 *
 * The coverage added or subtracted by an edge as it crosses a
 * given pixel is calculated using a trapezoid formula in the
 * following manner:
 *
 *                /
 *     +-----+---/-+-----+
 *     |     |  /  |     |
 *     |     | /   |     |
 *     +-----+/----+-----+
 *           /
 *
 * The area to the right of that edge for the pixel where it
 * crosses is given as:
 *
 *     trapheight * (topedge + bottomedge)/2
 *
 * Another thing to note is that the above formula gives the
 * contribution of that edge to the given pixel where it crossed,
 * but in so crossing the pixel row, it also created 100% coverage
 * for all of the pixels to the right.
 *
 * This example was simplified in that the edge depicted crossed
 * the complete pixel row and it did so entirely within the bounds
 * of a single pixel column.  In practice, many edges may start or
 * end in a given row and thus provide only partial row coverage
 * (i.e. the total "trapheight" in the formula never reaches 1.0).
 * And in other cases, edges may travel sideways through several
 * pixel columns on a given pixel row from where they enter it to
 * where the leave it (which also mans that the trapheight for a
 * given pixel will be less than 1.0, but by the time the edge
 * completes its journey through the pixel row the "coverage shadow"
 * that it casts on all pixels to the right eventually reaches 100%).
 *
 * In order to simplify the calculations so that we don't have to
 * keep propagating coverages we calculate for one edge "until we
 * reach another edge" we will process one edge at a time and
 * simply record in a buffer the amount that an edge added to
 * or subtracted from the coverage for a given pixel and its
 * following right-side neighbors.  Thus, the true total coverage
 * of a given pixel is only determined by summing the deltas for
 * that pixel and all of the pixels to its left.  Since we already
 * have to scan the buffer to change floating point coverages into
 * mask values for a MaskFill loop, it is simple enough to sum the
 * values as we perform that scan from left to right.
 *
 * In the above example, note that 2 deltas need to be recorded even
 * though the edge only intersected a single pixel.  The delta recorded
 * for the pixel where the edge crossed will be approximately 55%
 * (guesstimating by examining the poor ascii art) which is fine for
 * determining how to render that pixel, but the rest of the pixels
 * to its right should have their coverage modified by a full 100%
 * and the 55% delta value we recorded for the pixel that the edge
 * crossed will not get them there.  We adjust for this by adding
 * the "remainder" of the coverage implied by the shadow to the
 * pixel immediately to the right of where we record a trapezoidal
 * contribution.  In this case a delta of 45% will be recorded in
 * the pixel immediately to the right to raise the total to 100%.
 *
 * As we sum these delta values as we process the line from left
 * to right, these delta values will typically drive the sum from
 * 0% up to 100% and back down to 0% over the course of a single
 * pixel row.  In the case of a drawn (double) parallelogram the
 * sum will go to 100% and back to 0% twice on most scanlines.
 *
 * The fillAAPgram and drawAAPgram functions drive the main flow
 * of the algorithm with help from the following structures,
 * macros, and functions.  It is probably best to start with
 * those 2 functions to gain an understanding of the algorithm.
 */
typedef struct {
    jdouble x;
    jdouble y;
    jdouble xbot;
    jdouble ybot;
    jdouble xnexty;
    jdouble ynextx;
    jdouble xnextx;
    jdouble linedx;
    jdouble celldx;
    jdouble celldy;
    jboolean isTrailing;
} EdgeInfo;

#define MIN_DELTA  (1.0/256.0)

/*
 * Calculates slopes and deltas for an edge and stores results in an EdgeInfo.
 * Returns true if the edge was valid (i.e. not ignored for some reason).
 */
static jboolean
storeEdge(EdgeInfo *pEdge,
          jdouble x, jdouble y, jdouble dx, jdouble dy,
          jint cx1, jint cy1, jint cx2, jint cy2,
          jboolean isTrailing)
{
    jdouble xbot = x + dx;
    jdouble ybot = y + dy;
    jboolean ret;

    pEdge->x = x;
    pEdge->y = y;
    pEdge->xbot = xbot;
    pEdge->ybot = ybot;

    /* Note that parallelograms are sorted so dy is always non-negative */
    if (dy > MIN_DELTA &&        /* NaN and horizontal protection */
        ybot > cy1 &&            /* NaN and "OUT_ABOVE" protection */
        y < cy2 &&               /* NaN and "OUT_BELOW" protection */
        xbot == xbot &&          /* NaN protection */
        (x < cx2 || xbot < cx2)) /* "OUT_RIGHT" protection */
        /* Note: "OUT_LEFT" segments may still contribute coverage... */
    {
        /* no NaNs, dy is not horizontal, and segment contributes to clip */
        if (dx < -MIN_DELTA || dx > MIN_DELTA) {
            /* dx is not vertical */
            jdouble linedx;
            jdouble celldy;
            jdouble nextx;

            linedx = dx / dy;
            celldy = dy / dx;
            if (y < cy1) {
                pEdge->x = x = x + (cy1 - y) * linedx;
                pEdge->y = y = cy1;
            }
            pEdge->linedx = linedx;
            if (dx < 0) {
                pEdge->celldx = -1.0;
                pEdge->celldy = -celldy;
                pEdge->xnextx = nextx = ceil(x) - 1.0;
            } else {
                pEdge->celldx = +1.0;
                pEdge->celldy = celldy;
                pEdge->xnextx = nextx = floor(x) + 1.0;
            }
            pEdge->ynextx = y + (nextx - x) * celldy;
            pEdge->xnexty = x + ((floor(y) + 1) - y) * linedx;
        } else {
            /* dx is essentially vertical */
            if (y < cy1) {
                pEdge->y = y = cy1;
            }
            pEdge->xbot = x;
            pEdge->linedx = 0.0;
            pEdge->celldx = 0.0;
            pEdge->celldy = 1.0;
            pEdge->xnextx = x;
            pEdge->xnexty = x;
            pEdge->ynextx = ybot;
        }
        ret = JNI_TRUE;
    } else {
        /* There is some reason to ignore this segment, "celldy=0" omits it */
        pEdge->ybot = y;
        pEdge->linedx = dx;
        pEdge->celldx = dx;
        pEdge->celldy = 0.0;
        pEdge->xnextx = xbot;
        pEdge->xnexty = xbot;
        pEdge->ynextx = y;
        ret = JNI_FALSE;
    }
    pEdge->isTrailing = isTrailing;
    return ret;
}

/*
 * Calculates and stores slopes and deltas for all edges of a parallelogram.
 * Returns true if at least 1 edge was valid (i.e. not ignored for some reason).
 *
 * The inverted flag is true for an outer parallelogram (left and right
 * edges are leading and trailing) and false for an inner parallelogram
 * (where the left edge is trailing and the right edge is leading).
 */
static jboolean
storePgram(EdgeInfo *pLeftEdge, EdgeInfo *pRightEdge,
           jdouble x, jdouble y,
           jdouble dx1, jdouble dy1,
           jdouble dx2, jdouble dy2,
           jint cx1, jint cy1, jint cx2, jint cy2,
           jboolean inverted)
{
    jboolean ret = JNI_FALSE;
    ret = (storeEdge(pLeftEdge  + 0,
                     x    , y    , dx1, dy1,
                     cx1, cy1, cx2, cy2, inverted) || ret);
    ret = (storeEdge(pLeftEdge  + 1,
                     x+dx1, y+dy1, dx2, dy2,
                     cx1, cy1, cx2, cy2, inverted) || ret);
    ret = (storeEdge(pRightEdge + 0,
                     x    , y    , dx2, dy2,
                     cx1, cy1, cx2, cy2, !inverted) || ret);
    ret = (storeEdge(pRightEdge + 1,
                     x+dx2, y+dy2, dx1, dy1,
                     cx1, cy1, cx2, cy2, !inverted) || ret);
    return ret;
}

/*
 * The X0,Y0,X1,Y1 values represent a trapezoidal fragment whose
 * coverage must be accounted for in the accum buffer.
 *
 * All four values are assumed to fall within (or on the edge of)
 * a single pixel.
 *
 * The trapezoid area is accumulated into the proper element of
 * the accum buffer and the remainder of the "slice height" is
 * accumulated into the element to its right.
 */
#define INSERT_ACCUM(pACCUM, IMIN, IMAX, X0, Y0, X1, Y1, CX1, CX2, MULT) \
    do { \
        jdouble xmid = ((X0) + (X1)) * 0.5; \
        if (xmid <= (CX2)) { \
            jdouble sliceh = ((Y1) - (Y0)); \
            jdouble slicearea; \
            jint i; \
            if (xmid < (CX1)) { \
                /* Accumulate the entire slice height into accum[0]. */ \
                i = 0; \
                slicearea = sliceh; \
            } else { \
                jdouble xpos = floor(xmid); \
                i = ((jint) xpos) - (CX1); \
                slicearea = (xpos+1-xmid) * sliceh; \
            } \
            if (IMIN > i) { \
                IMIN = i; \
            } \
            (pACCUM)[i++] += (jfloat) ((MULT) * slicearea); \
            (pACCUM)[i++] += (jfloat) ((MULT) * (sliceh - slicearea)); \
            if (IMAX < i) { \
                IMAX = i; \
            } \
        } \
    } while (0)

/*
 * Accumulate the contributions for a given edge crossing a given
 * scan line into the corresponding entries of the accum buffer.
 * CY1 is the Y coordinate of the top edge of the scanline and CY2
 * is equal to (CY1 + 1) and is the Y coordinate of the bottom edge
 * of the scanline.  CX1 and CX2 are the left and right edges of the
 * clip (or area of interest) being rendered.
 *
 * The edge is processed from the top edge to the bottom edge and
 * a single pixel column at a time.
 */
#define ACCUM_EDGE(pEDGE, pACCUM, IMIN, IMAX, CX1, CY1, CX2, CY2) \
    do { \
        jdouble x, y, xnext, ynext, xlast, ylast, dx, dy, mult; \
        y = (pEDGE)->y; \
        dy = (pEDGE)->celldy; \
        ylast = (pEDGE)->ybot; \
        if (ylast <= (CY1) || y >= (CY2) || dy == 0.0) { \
            break; \
        } \
        x = (pEDGE)->x; \
        dx = (pEDGE)->celldx; \
        if (ylast > (CY2)) { \
            ylast = (CY2); \
            xlast = (pEDGE)->xnexty; \
        } else { \
            xlast = (pEDGE)->xbot; \
        } \
        xnext = (pEDGE)->xnextx; \
        ynext = (pEDGE)->ynextx; \
        mult = ((pEDGE)->isTrailing) ? -1.0 : 1.0; \
        while (ynext <= ylast) { \
            INSERT_ACCUM(pACCUM, IMIN, IMAX, \
                         x, y, xnext, ynext, \
                         CX1, CX2, mult); \
            x = xnext; \
            y = ynext; \
            xnext += dx; \
            ynext += dy; \
        } \
        (pEDGE)->ynextx = ynext; \
        (pEDGE)->xnextx = xnext; \
        INSERT_ACCUM(pACCUM, IMIN, IMAX, \
                     x, y, xlast, ylast, \
                     CX1, CX2, mult); \
        (pEDGE)->x = xlast; \
        (pEDGE)->y = ylast; \
        (pEDGE)->xnexty = xlast + (pEDGE)->linedx; \
    } while(0)

/* Main function to fill a single Parallelogram */
static void
fillAAPgram(NativePrimitive *pPrim, SurfaceDataRasInfo *pRasInfo,
            CompositeInfo *pCompInfo, jint color, unsigned char *pMask,
            void *pDst,
            jdouble x1, jdouble y1,
            jdouble dx1, jdouble dy1,
            jdouble dx2, jdouble dy2)
{
    jint cx1 = pRasInfo->bounds.x1;
    jint cy1 = pRasInfo->bounds.y1;
    jint cx2 = pRasInfo->bounds.x2;
    jint cy2 = pRasInfo->bounds.y2;
    jint width = cx2 - cx1;
    EdgeInfo edges[4];
    jfloat localaccum[MASK_BUF_LEN + 1];
    jfloat *pAccum;

    if (!storePgram(edges + 0, edges + 2,
                    x1, y1, dx1, dy1, dx2, dy2,
                    cx1, cy1, cx2, cy2,
                    JNI_FALSE))
    {
        return;
    }

    pAccum = ((width > MASK_BUF_LEN)
              ? malloc((width + 1) * sizeof(jfloat))
              : localaccum);
    if (pAccum == NULL) {
        return;
    }
    memset(pAccum, 0, (width+1) * sizeof(jfloat));

    while (cy1 < cy2) {
        jint lmin, lmax, rmin, rmax;
        jint moff, x;
        jdouble accum;
        unsigned char lastcov;

        lmin = rmin = width + 2;
        lmax = rmax = 0;
        ACCUM_EDGE(&edges[0], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[1], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[2], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[3], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        if (lmax > width) {
            lmax = width; /* Extra col has data we do not need. */
        }
        if (rmax > width) {
            rmax = width; /* Extra col has data we do not need. */
        }
        /* If ranges overlap, handle both in the first pass. */
        if (rmin <= lmax) {
            lmax = rmax;
        }

        x = lmin;
        accum = 0.0;
        moff = 0;
        lastcov = 0;
        while (x < lmax) {
            accum += pAccum[x];
            pAccum[x] = 0.0f;
            pMask[moff++] = lastcov = DblToMask(accum);
            x++;
        }
        /* Check for a solid center section. */
        if (lastcov == 0xFF) {
            jint endx;
            void *pRow;

            /* First process the existing partial coverage data. */
            if (moff > 0) {
                pRow = PtrCoord(pDst, x-moff, pRasInfo->pixelStride, 0, 0);
                (*pPrim->funcs.maskfill)(pRow,
                                         pMask, 0, 0,
                                         moff, 1,
                                         color, pRasInfo,
                                         pPrim, pCompInfo);
                moff = 0;
            }

            /* Where does the center section end? */
            /* If there is no right AA edge in the accum buffer, then */
            /* the right edge was beyond the clip, so fill out to width */
            endx = (rmin < rmax) ? rmin : width;
            if (x < endx) {
                pRow = PtrCoord(pDst, x, pRasInfo->pixelStride, 0, 0);
                (*pPrim->funcs.maskfill)(pRow,
                                         NULL, 0, 0,
                                         endx - x, 1,
                                         color, pRasInfo,
                                         pPrim, pCompInfo);
                x = endx;
            }
        } else if (lastcov > 0 && rmin >= rmax) {
            /* We are not at 0 coverage, but there is no right edge, */
            /* force a right edge so we process pixels out to width. */
            rmax = width;
        }
        /* The following loop will process the right AA edge and/or any */
        /* partial coverage center section not processed above. */
        while (x < rmax) {
            accum += pAccum[x];
            pAccum[x] = 0.0f;
            pMask[moff++] = DblToMask(accum);
            x++;
        }
        if (moff > 0) {
            void *pRow = PtrCoord(pDst, x-moff, pRasInfo->pixelStride, 0, 0);
            (*pPrim->funcs.maskfill)(pRow,
                                     pMask, 0, 0,
                                     moff, 1,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
        }
        pDst = PtrAddBytes(pDst, pRasInfo->scanStride);
        cy1++;
    }
    if (pAccum != localaccum) {
        free(pAccum);
    }
}

/*
 * Class:     sun_java2d_loops_MaskFill
 * Method:    FillAAPgram
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Ljava/awt/Composite;DDDDDD)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_MaskFill_FillAAPgram
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject comp,
     jdouble x0, jdouble y0,
     jdouble dx1, jdouble dy1,
     jdouble dx2, jdouble dy2)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint ix1, iy1, ix2, iy2;

    if ((dy1 == 0 && dx1 == 0) || (dy2 == 0 && dx2 == 0)) {
        return;
    }

    /*
     * Sort parallelogram by y values, ensure that each delta vector
     * has a non-negative y delta.
     */
    SORT_PGRAM(x0, y0, dx1, dy1, dx2, dy2, );

    PGRAM_MIN_MAX(ix1, ix2, x0, dx1, dx2, JNI_TRUE);
    iy1 = (jint) floor(y0);
    iy2 = (jint) ceil(y0 + dy1 + dy2);

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    if (pPrim->pCompType->getCompInfo != NULL) {
        (*pPrim->pCompType->getCompInfo)(env, &compInfo, comp);
    }

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
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
        jint width = ix2 - ix1;
        jint color = GrPrim_Sg2dGetEaRGB(env, sg2d);
        unsigned char localmask[MASK_BUF_LEN];
        unsigned char *pMask = ((width > MASK_BUF_LEN)
                                ? malloc(width)
                                : localmask);

        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase != NULL && pMask != NULL) {
            void *pDst = PtrCoord(rasInfo.rasBase,
                                  ix1, rasInfo.pixelStride,
                                  iy1, rasInfo.scanStride);
            if (dy1 == 0 && dx2 == 0) {
                if (dx1 < 0) {
                    // We sorted by Y above, but not by X
                    x0 += dx1;
                    dx1 = -dx1;
                }
                fillAARect(pPrim, &rasInfo, &compInfo,
                           color, pMask, pDst,
                           x0, y0, x0+dx1, y0+dy2);
            } else if (dx1 == 0 && dy2 == 0) {
                if (dx2 < 0) {
                    // We sorted by Y above, but not by X
                    x0 += dx2;
                    dx2 = -dx2;
                }
                fillAARect(pPrim, &rasInfo, &compInfo,
                           color, pMask, pDst,
                           x0, y0, x0+dx2, y0+dy1);
            } else {
                fillAAPgram(pPrim, &rasInfo, &compInfo,
                            color, pMask, pDst,
                            x0, y0, dx1, dy1, dx2, dy2);
            }
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
        if (pMask != NULL && pMask != localmask) {
            free(pMask);
        }
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}

/* Main function to fill a double pair of (inner and outer) parallelograms */
static void
drawAAPgram(NativePrimitive *pPrim, SurfaceDataRasInfo *pRasInfo,
            CompositeInfo *pCompInfo, jint color, unsigned char *pMask,
            void *pDst,
            jdouble ox0, jdouble oy0,
            jdouble dx1, jdouble dy1,
            jdouble dx2, jdouble dy2,
            jdouble ldx1, jdouble ldy1,
            jdouble ldx2, jdouble ldy2)
{
    jint cx1 = pRasInfo->bounds.x1;
    jint cy1 = pRasInfo->bounds.y1;
    jint cx2 = pRasInfo->bounds.x2;
    jint cy2 = pRasInfo->bounds.y2;
    jint width = cx2 - cx1;
    EdgeInfo edges[8];
    jfloat localaccum[MASK_BUF_LEN + 1];
    jfloat *pAccum;

    if (!storePgram(edges + 0, edges + 6,
                    ox0, oy0,
                    dx1 + ldx1, dy1 + ldy1,
                    dx2 + ldx2, dy2 + ldy2,
                    cx1, cy1, cx2, cy2,
                    JNI_FALSE))
    {
        /* If outer pgram does not contribute, then inner cannot either. */
        return;
    }
    storePgram(edges + 2, edges + 4,
               ox0 + ldx1 + ldx2, oy0 + ldy1 + ldy2,
               dx1 - ldx1, dy1 - ldy1,
               dx2 - ldx2, dy2 - ldy2,
               cx1, cy1, cx2, cy2,
               JNI_TRUE);

    pAccum = ((width > MASK_BUF_LEN)
              ? malloc((width + 1) * sizeof(jfloat))
              : localaccum);
    if (pAccum == NULL) {
        return;
    }
    memset(pAccum, 0, (width+1) * sizeof(jfloat));

    while (cy1 < cy2) {
        jint lmin, lmax, rmin, rmax;
        jint moff, x;
        jdouble accum;
        unsigned char lastcov;

        lmin = rmin = width + 2;
        lmax = rmax = 0;
        ACCUM_EDGE(&edges[0], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[1], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[2], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[3], pAccum, lmin, lmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[4], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[5], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[6], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        ACCUM_EDGE(&edges[7], pAccum, rmin, rmax,
                   cx1, cy1, cx2, cy1+1);
        if (lmax > width) {
            lmax = width; /* Extra col has data we do not need. */
        }
        if (rmax > width) {
            rmax = width; /* Extra col has data we do not need. */
        }
        /* If ranges overlap, handle both in the first pass. */
        if (rmin <= lmax) {
            lmax = rmax;
        }

        x = lmin;
        accum = 0.0;
        moff = 0;
        lastcov = 0;
        while (x < lmax) {
            accum += pAccum[x];
            pAccum[x] = 0.0f;
            pMask[moff++] = lastcov = DblToMask(accum);
            x++;
        }
        /* Check for an empty or solidcenter section. */
        if (lastcov == 0 || lastcov == 0xFF) {
            jint endx;
            void *pRow;

            /* First process the existing partial coverage data. */
            if (moff > 0) {
                pRow = PtrCoord(pDst, x-moff, pRasInfo->pixelStride, 0, 0);
                (*pPrim->funcs.maskfill)(pRow,
                                         pMask, 0, 0,
                                         moff, 1,
                                         color, pRasInfo,
                                         pPrim, pCompInfo);
                moff = 0;
            }

            /* Where does the center section end? */
            /* If there is no right AA edge in the accum buffer, then */
            /* the right edge was beyond the clip, so fill out to width */
            endx = (rmin < rmax) ? rmin : width;
            if (x < endx) {
                if (lastcov == 0xFF) {
                    pRow = PtrCoord(pDst, x, pRasInfo->pixelStride, 0, 0);
                    (*pPrim->funcs.maskfill)(pRow,
                                             NULL, 0, 0,
                                             endx - x, 1,
                                             color, pRasInfo,
                                             pPrim, pCompInfo);
                }
                x = endx;
            }
        } else if (rmin >= rmax) {
            /* We are not at 0 coverage, but there is no right edge, */
            /* force a right edge so we process pixels out to width. */
            rmax = width;
        }
        /* The following loop will process the right AA edge and/or any */
        /* partial coverage center section not processed above. */
        while (x < rmax) {
            accum += pAccum[x];
            pAccum[x] = 0.0f;
            pMask[moff++] = lastcov = DblToMask(accum);
            x++;
        }
        if (moff > 0) {
            void *pRow = PtrCoord(pDst, x-moff, pRasInfo->pixelStride, 0, 0);
            (*pPrim->funcs.maskfill)(pRow,
                                     pMask, 0, 0,
                                     moff, 1,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
        }
        if (lastcov == 0xFF && x < width) {
            void *pRow = PtrCoord(pDst, x, pRasInfo->pixelStride, 0, 0);
            (*pPrim->funcs.maskfill)(pRow,
                                     NULL, 0, 0,
                                     width - x, 1,
                                     color, pRasInfo,
                                     pPrim, pCompInfo);
        }
        pDst = PtrAddBytes(pDst, pRasInfo->scanStride);
        cy1++;
    }
    if (pAccum != localaccum) {
        free(pAccum);
    }
}

/*
 * Class:     sun_java2d_loops_MaskFill
 * Method:    DrawAAPgram
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Ljava/awt/Composite;DDDDDDDD)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_MaskFill_DrawAAPgram
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject comp,
     jdouble x0, jdouble y0,
     jdouble dx1, jdouble dy1,
     jdouble dx2, jdouble dy2,
     jdouble lw1, jdouble lw2)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint ix1, iy1, ix2, iy2;
    jdouble ldx1, ldy1, ldx2, ldy2;
    jdouble ox0, oy0;

    if ((dy1 == 0 && dx1 == 0) || (dy2 == 0 && dx2 == 0)) {
        return;
    }

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

    if (lw1 >= 1.0 || lw2 >= 1.0) {
        /* Only need to fill an outer pgram if the interior no longer
         * has a hole in it (i.e. if either of the line width ratios
         * were greater than or equal to 1.0).
         */
        Java_sun_java2d_loops_MaskFill_FillAAPgram(env, self,
                                                   sg2d, sData, comp,
                                                   ox0, oy0,
                                                   dx1 + ldx1, dy1 + ldy1,
                                                   dx2 + ldx2, dy2 + ldy2);
        return;
    }

    PGRAM_MIN_MAX(ix1, ix2, ox0, dx1+ldx1, dx2+ldx2, JNI_TRUE);
    iy1 = (jint) floor(oy0);
    iy2 = (jint) ceil(oy0 + dy1 + ldy1 + dy2 + ldy2);

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    if (pPrim->pCompType->getCompInfo != NULL) {
        (*pPrim->pCompType->getCompInfo)(env, &compInfo, comp);
    }

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
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
        jint width = ix2 - ix1;
        jint color = GrPrim_Sg2dGetEaRGB(env, sg2d);
        unsigned char localmask[MASK_BUF_LEN];
        unsigned char *pMask = ((width > MASK_BUF_LEN)
                                ? malloc(width)
                                : localmask);

        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase != NULL && pMask != NULL) {
            void *pDst = PtrCoord(rasInfo.rasBase,
                                  ix1, rasInfo.pixelStride,
                                  iy1, rasInfo.scanStride);
            /*
             * NOTE: aligned rects could probably be drawn
             * even faster with a little work here.
             * if (dy1 == 0 && dx2 == 0) {
             *     drawAARect(pPrim, &rasInfo, &compInfo,
             *                color, pMask, pDst,
             *                ox0, oy0, ox0+dx1+ldx1, oy0+dy2+ldy2, ldx1, ldy2);
             * } else if (dx1 == 0 && dy2 == 0) {
             *     drawAARect(pPrim, &rasInfo, &compInfo,
             *                color, pMask, pDst,
             *                ox0, oy0, ox0+dx2+ldx2, oy0+dy1+ldy1, ldx2, ldy1);
             * } else {
             */
            drawAAPgram(pPrim, &rasInfo, &compInfo,
                        color, pMask, pDst,
                        ox0, oy0,
                        dx1, dy1, dx2, dy2,
                        ldx1, ldy1, ldx2, ldy2);
            /*
             * }
             */
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
        if (pMask != NULL && pMask != localmask) {
            free(pMask);
        }
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
