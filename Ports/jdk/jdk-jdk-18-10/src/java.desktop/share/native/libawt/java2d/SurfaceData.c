/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "SurfaceData.h"

#include "jni_util.h"
#include "Disposer.h"

#include "stdlib.h"
#include "string.h"

/**
 * This include file contains information on how to use a SurfaceData
 * object from native code.
 */

static jclass pInvalidPipeClass;
static jclass pNullSurfaceDataClass;
static jfieldID pDataID;
static jfieldID allGrayID;

jfieldID validID;
GeneralDisposeFunc SurfaceData_DisposeOps;

#define InitClass(var, env, name) \
do { \
    var = (*env)->FindClass(env, name); \
    if (var == NULL) { \
        return; \
    } \
} while (0)

#define InitField(var, env, jcl, name, type) \
do { \
    var = (*env)->GetFieldID(env, jcl, name, type); \
    if (var == NULL) { \
        return; \
    } \
} while (0)

#define InitGlobalClassRef(var, env, name) \
do { \
    jobject jtmp; \
    InitClass(jtmp, env, name); \
    var = (*env)->NewGlobalRef(env, jtmp); \
    if (var == NULL) { \
        return; \
    } \
} while (0)

/*
 * Class:     sun_java2d_SurfaceData
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_SurfaceData_initIDs(JNIEnv *env, jclass sd)
{
    jclass pICMClass;

    InitGlobalClassRef(pInvalidPipeClass, env,
                       "sun/java2d/InvalidPipeException");

    InitGlobalClassRef(pNullSurfaceDataClass, env,
                       "sun/java2d/NullSurfaceData");

    InitField(pDataID, env, sd, "pData", "J");
    InitField(validID, env, sd, "valid", "Z");

    InitClass(pICMClass, env, "java/awt/image/IndexColorModel");
    InitField(allGrayID, env, pICMClass, "allgrayopaque", "Z");
}

/*
 * Class:     sun_java2d_SurfaceData
 * Method:    isOpaqueGray
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_SurfaceData_isOpaqueGray(JNIEnv *env, jclass sdClass,
                                         jobject icm)
{
    if (icm == NULL) {
        return JNI_FALSE;
    }
    return (*env)->GetBooleanField(env, icm, allGrayID);
}

static SurfaceDataOps *
GetSDOps(JNIEnv *env, jobject sData, jboolean callSetup)
{
    SurfaceDataOps *ops;
    if (JNU_IsNull(env, sData)) {
        JNU_ThrowNullPointerException(env, "surfaceData");
        return NULL;
    }
    ops = (SurfaceDataOps *)JNU_GetLongFieldAsPtr(env, sData, pDataID);
    if (ops == NULL) {
        if (!(*env)->ExceptionOccurred(env) &&
            !(*env)->IsInstanceOf(env, sData, pNullSurfaceDataClass))
        {
            if (!(*env)->GetBooleanField(env, sData, validID)) {
                SurfaceData_ThrowInvalidPipeException(env, "invalid data");
            } else {
                JNU_ThrowNullPointerException(env, "native ops missing");
            }
        }
    } else if (callSetup) {
        SurfaceData_InvokeSetup(env, ops);
    }
    return ops;
}

JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_GetOps(JNIEnv *env, jobject sData)
{
    return GetSDOps(env, sData, JNI_TRUE);
}

JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_GetOpsNoSetup(JNIEnv *env, jobject sData)
{
    return GetSDOps(env, sData, JNI_FALSE);
}

JNIEXPORT void JNICALL
SurfaceData_SetOps(JNIEnv *env, jobject sData, SurfaceDataOps *ops)
{
    if (JNU_GetLongFieldAsPtr(env, sData, pDataID) == NULL) {
        JNU_SetLongFieldFromPtr(env, sData, pDataID, ops);
        /* Register the data for disposal */
        Disposer_AddRecord(env, sData,
                           SurfaceData_DisposeOps,
                           ptr_to_jlong(ops));
    } else {
        JNU_ThrowInternalError(env, "Attempting to set SurfaceData ops twice");
    }
}

JNIEXPORT void JNICALL
SurfaceData_ThrowInvalidPipeException(JNIEnv *env, const char *msg)
{
    (*env)->ThrowNew(env, pInvalidPipeClass, msg);
}

#define GETMIN(v1, v2)          (((v1) > (t=(v2))) && ((v1) = t))
#define GETMAX(v1, v2)          (((v1) < (t=(v2))) && ((v1) = t))

JNIEXPORT void JNICALL
SurfaceData_IntersectBounds(SurfaceDataBounds *dst, SurfaceDataBounds *src)
{
    int t;
    GETMAX(dst->x1, src->x1);
    GETMAX(dst->y1, src->y1);
    GETMIN(dst->x2, src->x2);
    GETMIN(dst->y2, src->y2);
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYXY(SurfaceDataBounds *bounds,
                                jint x1, jint y1, jint x2, jint y2)
{
    int t;
    GETMAX(bounds->x1, x1);
    GETMAX(bounds->y1, y1);
    GETMIN(bounds->x2, x2);
    GETMIN(bounds->y2, y2);
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYWH(SurfaceDataBounds *bounds,
                                jint x, jint y, jint w, jint h)
{
    w = (w <= 0) ? x : x+w;
    if (w < x) {
        w = 0x7fffffff;
    }
    if (bounds->x1 < x) {
        bounds->x1 = x;
    }
    if (bounds->x2 > w) {
        bounds->x2 = w;
    }
    h = (h <= 0) ? y : y+h;
    if (h < y) {
        h = 0x7fffffff;
    }
    if (bounds->y1 < y) {
        bounds->y1 = y;
    }
    if (bounds->y2 > h) {
        bounds->y2 = h;
    }
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBlitBounds(SurfaceDataBounds *src,
                                SurfaceDataBounds *dst,
                                jint dx, jint dy)
{
    int t;
    GETMAX(dst->x1, src->x1 + dx);
    GETMAX(dst->y1, src->y1 + dy);
    GETMIN(dst->x2, src->x2 + dx);
    GETMIN(dst->y2, src->y2 + dy);
    GETMAX(src->x1, dst->x1 - dx);
    GETMAX(src->y1, dst->y1 - dy);
    GETMIN(src->x2, dst->x2 - dx);
    GETMIN(src->y2, dst->y2 - dy);
}

JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_InitOps(JNIEnv *env, jobject sData, int opsSize)
{
    SurfaceDataOps *ops = malloc(opsSize);
    SurfaceData_SetOps(env, sData, ops);
    if (ops != NULL) {
        memset(ops, 0, opsSize);
        if (!(*env)->ExceptionCheck(env)) {
            ops->sdObject = (*env)->NewWeakGlobalRef(env, sData);
        }
    }
    return ops;
}

void SurfaceData_DisposeOps(JNIEnv *env, jlong ops)
{
    if (ops != 0) {
        SurfaceDataOps *sdops = (SurfaceDataOps*)jlong_to_ptr(ops);
        /* Invoke the ops-specific disposal function */
        SurfaceData_InvokeDispose(env, sdops);
        (*env)->DeleteWeakGlobalRef(env, sdops->sdObject);
        free(sdops);
    }
}
