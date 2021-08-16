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

#include "jni_util.h"

#include "GraphicsPrimitiveMgr.h"
#include "LineUtils.h"

#include "sun_java2d_loops_DrawPolygons.h"

static void
RefineBounds(SurfaceDataBounds *bounds, jint transX, jint transY,
             jint *xPointsPtr, jint *yPointsPtr, jint pointsNeeded)
{
    jint xmin, ymin, xmax, ymax;
    if (pointsNeeded > 0) {
        xmin = xmax = transX + *xPointsPtr++;
        ymin = ymax = transY + *yPointsPtr++;
        while (--pointsNeeded > 0) {
            jint x = transX + *xPointsPtr++;
            jint y = transY + *yPointsPtr++;
            if (xmin > x) xmin = x;
            if (ymin > y) ymin = y;
            if (xmax < x) xmax = x;
            if (ymax < y) ymax = y;
        }
        if (++xmax < xmin) xmax--;
        if (++ymax < ymin) ymax--;
        if (bounds->x1 < xmin) bounds->x1 = xmin;
        if (bounds->y1 < ymin) bounds->y1 = ymin;
        if (bounds->x2 > xmax) bounds->x2 = xmax;
        if (bounds->y2 > ymax) bounds->y2 = ymax;
    } else {
        bounds->x2 = bounds->x1;
        bounds->y2 = bounds->y1;
    }
}

static void
ProcessPoly(SurfaceDataRasInfo *pRasInfo,
            DrawLineFunc *pLine,
            NativePrimitive *pPrim,
            CompositeInfo *pCompInfo,
            jint pixel, jint transX, jint transY,
            jint *xPointsPtr, jint *yPointsPtr,
            jint *nPointsPtr, jint numPolys,
            jboolean close)
{
    int i;
    for (i = 0; i < numPolys; i++) {
        jint numPts = nPointsPtr[i];
        if (numPts > 1) {
            jint x0, y0, x1, y1;
            jboolean empty = JNI_TRUE;
            x0 = x1 = transX + *xPointsPtr++;
            y0 = y1 = transY + *yPointsPtr++;
            while (--numPts > 0) {
                jint x2 = transX + *xPointsPtr++;
                jint y2 = transY + *yPointsPtr++;
                empty = (empty && x1 == x2 && y1 == y2);
                LineUtils_ProcessLine(pRasInfo, pixel, pLine,
                                      pPrim, pCompInfo,
                                      x1, y1, x2, y2,
                                      (numPts > 1 || close));
                x1 = x2;
                y1 = y2;
            }
            if (close && (empty || x1 != x0 || y1 != y0)) {
                LineUtils_ProcessLine(pRasInfo, pixel, pLine,
                                      pPrim, pCompInfo,
                                      x1, y1, x0, y0, !empty);
            }
        } else if (numPts == 1) {
            xPointsPtr++;
            yPointsPtr++;
        }
    }
}

/*
 * Class:     sun_java2d_loops_DrawPolygons
 * Method:    DrawPolygons
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;[I[I[IIIIZ)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawPolygons_DrawPolygons
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData,
     jintArray xPointsArray, jintArray yPointsArray,
     jintArray nPointsArray, jint numPolys,
     jint transX, jint transY, jboolean close)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jsize nPointsLen, xPointsLen, yPointsLen;
    jint *nPointsPtr = NULL;
    jint *xPointsPtr = NULL;
    jint *yPointsPtr = NULL;
    jint pointsNeeded;
    jint i, ret;
    jboolean ok = JNI_TRUE;
    jint pixel = GrPrim_Sg2dGetPixel(env, sg2d);

    if (JNU_IsNull(env, xPointsArray) || JNU_IsNull(env, yPointsArray)) {
        JNU_ThrowNullPointerException(env, "coordinate array");
        return;
    }
    if (JNU_IsNull(env, nPointsArray)) {
        JNU_ThrowNullPointerException(env, "polygon length array");
        return;
    }

    nPointsLen = (*env)->GetArrayLength(env, nPointsArray);
    xPointsLen = (*env)->GetArrayLength(env, xPointsArray);
    yPointsLen = (*env)->GetArrayLength(env, yPointsArray);
    if (nPointsLen < numPolys) {
        JNU_ThrowArrayIndexOutOfBoundsException(env,
                                                "polygon length array size");
        return;
    }

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

    ret = sdOps->Lock(env, sdOps, &rasInfo, SD_LOCK_FASTEST | pPrim->dstflags);
    if (ret == SD_FAILURE) {
        return;
    }

    nPointsPtr = (*env)->GetPrimitiveArrayCritical(env, nPointsArray, NULL);
    if (!nPointsPtr) {
        ok = JNI_FALSE;
    }

    if (ok) {
        pointsNeeded = 0;
        for (i = 0; i < numPolys; i++) {
            if (nPointsPtr[i] > 0) {
                pointsNeeded += nPointsPtr[i];
            }
        }

        if (yPointsLen < pointsNeeded || xPointsLen < pointsNeeded) {
            (*env)->ReleasePrimitiveArrayCritical(env, nPointsArray,
                                                  nPointsPtr, JNI_ABORT);
            SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
            JNU_ThrowArrayIndexOutOfBoundsException(env,
                                                    "coordinate array length");
            return;
        }

        xPointsPtr = (*env)->GetPrimitiveArrayCritical(env, xPointsArray, NULL);
        if (!xPointsPtr) {
            ok = JNI_FALSE;
        }
        if (ok) {
            yPointsPtr = (*env)->GetPrimitiveArrayCritical(env, yPointsArray, NULL);
            if (!yPointsPtr) {
                ok = JNI_FALSE;
            }
        }
    }

    if (ok) {
        if (ret == SD_SLOWLOCK) {
            RefineBounds(&rasInfo.bounds, transX, transY,
                         xPointsPtr, yPointsPtr, pointsNeeded);
            ok = (rasInfo.bounds.x2 > rasInfo.bounds.x1 &&
                  rasInfo.bounds.y2 > rasInfo.bounds.y1);
        }
    }

    if (ok) {
        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase &&
            rasInfo.bounds.x2 > rasInfo.bounds.x1 &&
            rasInfo.bounds.y2 > rasInfo.bounds.y1)
        {
            ProcessPoly(&rasInfo, pPrim->funcs.drawline, pPrim, &compInfo,
                        pixel, transX, transY,
                        xPointsPtr, yPointsPtr,
                        nPointsPtr, numPolys,
                        close);
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
    }

    if (nPointsPtr) {
        (*env)->ReleasePrimitiveArrayCritical(env, nPointsArray,
                                              nPointsPtr, JNI_ABORT);
    }
    if (xPointsPtr) {
        (*env)->ReleasePrimitiveArrayCritical(env, xPointsArray,
                                              xPointsPtr, JNI_ABORT);
    }
    if (yPointsPtr) {
        (*env)->ReleasePrimitiveArrayCritical(env, yPointsArray,
                                              yPointsPtr, JNI_ABORT);
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
