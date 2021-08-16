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
#include "ParallelogramUtils.h"

#include "sun_java2d_loops_FillParallelogram.h"

/*
 * Class:     sun_java2d_loops_FillParallelogram
 * Method:    FillParallelogram
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;DDDDDD)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_FillParallelogram_FillParallelogram
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData,
     jdouble x0, jdouble y0,
     jdouble dx1, jdouble dy1,
     jdouble dx2, jdouble dy2)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint pixel;
    jint ix1, iy1, ix2, iy2;

    if ((dy1 == 0 && dx1 == 0) || (dy2 == 0 && dx2 == 0)) {
        return;
    }

    /*
     * Sort parallelogram by y values, ensure that each delta vector
     * has a non-negative y delta.
     */
    SORT_PGRAM(x0, y0, dx1, dy1, dx2, dy2, );

    PGRAM_MIN_MAX(ix1, ix2, x0, dx1, dx2, JNI_FALSE);
    iy1 = (jint) floor(y0 + 0.5);
    iy2 = (jint) floor(y0 + dy1 + dy2 + 0.5);

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
            jdouble lslope = (dy1 == 0) ? 0 : dx1 / dy1;
            jdouble rslope = (dy2 == 0) ? 0 : dx2 / dy2;
            jlong ldx = DblToLong(lslope);
            jlong rdx = DblToLong(rslope);
            jint cy1, cy2, loy, hiy;
            dx1 += x0;
            dy1 += y0;
            dx2 += x0;
            dy2 += y0;
            cy1 = (jint) floor(dy1 + 0.5);
            cy2 = (jint) floor(dy2 + 0.5);

            /* Top triangular portion. */
            loy = iy1;
            hiy = (cy1 < cy2) ? cy1 : cy2;
            if (hiy > iy2) hiy = iy2;
            if (loy < hiy) {
                jlong lx = PGRAM_INIT_X(loy, x0, y0, lslope);
                jlong rx = PGRAM_INIT_X(loy, x0, y0, rslope);
                (*pPrim->funcs.fillparallelogram)(&rasInfo,
                                                  ix1, loy, ix2, hiy,
                                                  lx, ldx, rx, rdx,
                                                  pixel, pPrim, &compInfo);
            }

            /* Middle parallelogram portion, which way does it slant? */
            if (cy1 < cy2) {
                /* Middle parallelogram portion, slanted to right. */
                /* left leg turned a corner at y0+dy1 */
                /* right leg continuing on its initial trajectory from y0 */
                loy = cy1;
                hiy = cy2;
                if (loy < iy1) loy = iy1;
                if (hiy > iy2) hiy = iy2;
                if (loy < hiy) {
                    jlong lx = PGRAM_INIT_X(loy, dx1, dy1, rslope);
                    jlong rx = PGRAM_INIT_X(loy,  x0,  y0, rslope);
                    (*pPrim->funcs.fillparallelogram)(&rasInfo,
                                                      ix1, loy, ix2, hiy,
                                                      lx, rdx, rx, rdx,
                                                      pixel, pPrim, &compInfo);
                }
            } else if (cy2 < cy1) {
                /* Middle parallelogram portion, slanted to left. */
                /* left leg continuing on its initial trajectory from y0 */
                /* right leg turned a corner at y0+dy2 */
                loy = cy2;
                hiy = cy1;
                if (loy < iy1) loy = iy1;
                if (hiy > iy2) hiy = iy2;
                if (loy < hiy) {
                    jlong lx = PGRAM_INIT_X(loy,  x0,  y0, lslope);
                    jlong rx = PGRAM_INIT_X(loy, dx2, dy2, lslope);
                    (*pPrim->funcs.fillparallelogram)(&rasInfo,
                                                      ix1, loy, ix2, hiy,
                                                      lx, ldx, rx, ldx,
                                                      pixel, pPrim, &compInfo);
                }
            }

            /* Bottom triangular portion. */
            loy = (cy1 > cy2) ? cy1 : cy2;
            if (loy < iy1) loy = iy1;
            hiy = iy2;
            if (loy < hiy) {
                /* left leg turned its corner at y0+dy1, now moving right */
                /* right leg turned its corner at y0+dy2, now moving left */
                jlong lx = PGRAM_INIT_X(loy, dx1, dy1, rslope);
                jlong rx = PGRAM_INIT_X(loy, dx2, dy2, lslope);
                (*pPrim->funcs.fillparallelogram)(&rasInfo,
                                                  ix1, loy, ix2, hiy,
                                                  lx, rdx, rx, ldx,
                                                  pixel, pPrim, &compInfo);
            }
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
