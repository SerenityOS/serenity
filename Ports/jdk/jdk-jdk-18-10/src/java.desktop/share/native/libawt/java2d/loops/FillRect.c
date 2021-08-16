/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_loops_FillRect.h"

/*
 * Class:     sun_java2d_loops_FillRect
 * Method:    FillRect
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_FillRect_FillRect
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData,
     jint x, jint y, jint w, jint h)
{
    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint pixel = GrPrim_Sg2dGetPixel(env, sg2d);

    if (w <= 0 || h <= 0) {
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
    SurfaceData_IntersectBoundsXYWH(&rasInfo.bounds, x, y, w, h);
    if (rasInfo.bounds.y2 <= rasInfo.bounds.y1 ||
        rasInfo.bounds.x2 <= rasInfo.bounds.x1)
    {
        return;
    }

    if (sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags) != SD_SUCCESS) {
        return;
    }

    if (rasInfo.bounds.x2 > rasInfo.bounds.x1 &&
        rasInfo.bounds.y2 > rasInfo.bounds.y1)
    {
        sdOps->GetRasInfo(env, sdOps, &rasInfo);
        if (rasInfo.rasBase) {
            (*pPrim->funcs.fillrect)(&rasInfo,
                                     rasInfo.bounds.x1, rasInfo.bounds.y1,
                                     rasInfo.bounds.x2, rasInfo.bounds.y2,
                                     pixel, pPrim, &compInfo);
        }
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);
    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}
