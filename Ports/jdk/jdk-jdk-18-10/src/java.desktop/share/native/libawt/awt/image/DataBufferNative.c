/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>

#include "SurfaceData.h"
#include "sun_awt_image_DataBufferNative.h"

#include "jni_util.h"
#include "debug_trace.h"
#include <stdio.h>

unsigned char *DBN_GetPixelPointer(JNIEnv *env, jint x, int y,
                                   SurfaceDataRasInfo *lockInfo,
                                   SurfaceDataOps *ops, int lockFlag)
{
    if (ops == NULL) {
        return NULL;
    }

    lockInfo->bounds.x1 = x;
    lockInfo->bounds.y1 = y;
    lockInfo->bounds.x2 = x + 1;
    lockInfo->bounds.y2 = y + 1;
    if (ops->Lock(env, ops, lockInfo, lockFlag) != SD_SUCCESS) {
        return NULL;
    }
    ops->GetRasInfo(env, ops, lockInfo);
    if (lockInfo->rasBase) {
        unsigned char *pixelPtr = (
            (unsigned char*)lockInfo->rasBase +
            (x * lockInfo->pixelStride + y * lockInfo->scanStride));
        return pixelPtr;
    }
    SurfaceData_InvokeRelease(env, ops, lockInfo);
    SurfaceData_InvokeUnlock(env, ops, lockInfo);
    return NULL;
}

/*
 * Class:     sun_awt_image_DataBufferNative
 * Method:    getElem
 * Signature:
 */
JNIEXPORT jint JNICALL
Java_sun_awt_image_DataBufferNative_getElem(JNIEnv *env, jobject dbn,
                                            jint x, jint y, jobject sd)
{
    jint returnVal = -1;
    unsigned char *pixelPtr;
    SurfaceDataRasInfo lockInfo;
    SurfaceDataOps *ops;
    lockInfo.rasBase = NULL;

    ops = SurfaceData_GetOps(env, sd);
    JNU_CHECK_EXCEPTION_RETURN(env, -1);

    if (!(pixelPtr = DBN_GetPixelPointer(env, x, y, &lockInfo,
                                         ops, SD_LOCK_READ)))
    {
        return returnVal;
    }
    switch (lockInfo.pixelStride) {
    case 4:
        returnVal = *(int *)pixelPtr;
        break;
    /* REMIND: do we need a 3-byte case (for 24-bit) here? */
    case 2:
        returnVal = *(unsigned short *)pixelPtr;
        break;
    case 1:
        returnVal = *pixelPtr;
        break;
    default:
        break;
    }
    SurfaceData_InvokeRelease(env, ops, &lockInfo);
    SurfaceData_InvokeUnlock(env, ops, &lockInfo);
    return returnVal;
}


/*
 * Class:     sun_awt_image_DataBufferNative
 * Method:    setElem
 * Signature:
 */
JNIEXPORT void JNICALL
Java_sun_awt_image_DataBufferNative_setElem(JNIEnv *env, jobject dbn,
                                            jint x, jint y, jint val, jobject sd)
{
    SurfaceDataRasInfo lockInfo;
    SurfaceDataOps *ops;
    unsigned char *pixelPtr;
    lockInfo.rasBase = NULL;

    ops = SurfaceData_GetOps(env, sd);
    JNU_CHECK_EXCEPTION(env);

    if (!(pixelPtr = DBN_GetPixelPointer(env, x, y, &lockInfo,
                                         ops, SD_LOCK_WRITE)))
    {
        return;
    }

    switch (lockInfo.pixelStride) {
    case 4:
        *(int *)pixelPtr = val;
        break;
    /* REMIND: do we need a 3-byte case (for 24-bit) here? */
    case 2:
        *(unsigned short *)pixelPtr = (unsigned short)val;
        break;
    case 1:
        *pixelPtr = (unsigned char)val;
        break;
    default:
        break;
    }
    SurfaceData_InvokeRelease(env, ops, &lockInfo);
    SurfaceData_InvokeUnlock(env, ops, &lockInfo);
}
