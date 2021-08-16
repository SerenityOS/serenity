/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_util.h"

#include "Region.h"
#include "sizecalc.h"

static jfieldID endIndexID;
static jfieldID bandsID;
static jfieldID loxID;
static jfieldID loyID;
static jfieldID hixID;
static jfieldID hiyID;

#define InitField(var, env, jcl, name, type) \
do { \
    var = (*env)->GetFieldID(env, jcl, name, type); \
    if (var == NULL) { \
        return; \
    } \
} while (0)

/*
 * Class:     sun_java2d_pipe_Region
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_pipe_Region_initIDs(JNIEnv *env, jclass reg)
{
    InitField(endIndexID, env, reg, "endIndex", "I");
    InitField(bandsID, env, reg, "bands", "[I");

    InitField(loxID, env, reg, "lox", "I");
    InitField(loyID, env, reg, "loy", "I");
    InitField(hixID, env, reg, "hix", "I");
    InitField(hiyID, env, reg, "hiy", "I");
}

JNIEXPORT jint JNICALL
Region_GetInfo(JNIEnv *env, jobject region, RegionData *pRgnInfo)
{
    if (JNU_IsNull(env, region)) {
        pRgnInfo->bounds.x1 = pRgnInfo->bounds.y1 = 0x80000000;
        pRgnInfo->bounds.x2 = pRgnInfo->bounds.y2 = 0x7fffffff;
        pRgnInfo->endIndex = 0;
    } else {
        pRgnInfo->bounds.x1 = (*env)->GetIntField(env, region, loxID);
        pRgnInfo->bounds.y1 = (*env)->GetIntField(env, region, loyID);
        pRgnInfo->bounds.x2 = (*env)->GetIntField(env, region, hixID);
        pRgnInfo->bounds.y2 = (*env)->GetIntField(env, region, hiyID);
        pRgnInfo->endIndex = (*env)->GetIntField(env, region, endIndexID);
    }
    pRgnInfo->bands = (Region_IsRectangular(pRgnInfo)
                       ? NULL
                       : (*env)->GetObjectField(env, region, bandsID));
    return 0;
}

JNIEXPORT void JNICALL
Region_GetBounds(JNIEnv *env, jobject region, SurfaceDataBounds *b)
{
    if (JNU_IsNull(env, region)) {
        b->x1 = b->y1 = 0x80000000;
        b->x2 = b->y2 = 0x7fffffff;
    } else {
        b->x1 = (*env)->GetIntField(env, region, loxID);
        b->y1 = (*env)->GetIntField(env, region, loyID);
        b->x2 = (*env)->GetIntField(env, region, hixID);
        b->y2 = (*env)->GetIntField(env, region, hiyID);
    }
}

JNIEXPORT void JNICALL
Region_StartIteration(JNIEnv *env, RegionData *pRgnInfo)
{
    pRgnInfo->pBands =
        (Region_IsRectangular(pRgnInfo)
         ? NULL
         : (*env)->GetPrimitiveArrayCritical(env, pRgnInfo->bands, 0));
    pRgnInfo->index = 0;
    pRgnInfo->numrects = 0;
}

JNIEXPORT jint JNICALL
Region_CountIterationRects(RegionData *pRgnInfo)
{
    jint totalrects;
    if (Region_IsEmpty(pRgnInfo)) {
        totalrects = 0;
    } else if (Region_IsRectangular(pRgnInfo)) {
        totalrects = 1;
    } else {
        jint *pBands = pRgnInfo->pBands;
        int index = 0;
        totalrects = 0;
        while (index < pRgnInfo->endIndex) {
            jint xy1 = pBands[index++];
            jint xy2 = pBands[index++];
            jint numrects = pBands[index++];
            if (xy1 >= pRgnInfo->bounds.y2) {
                break;
            }
            if (xy2 > pRgnInfo->bounds.y1) {
                while (numrects > 0) {
                    xy1 = pBands[index++];
                    xy2 = pBands[index++];
                    numrects--;
                    if (xy1 >= pRgnInfo->bounds.x2) {
                        break;
                    }
                    if (xy2 > pRgnInfo->bounds.x1) {
                        totalrects++;
                    }
                }
            }
            index += numrects * 2;
        }
    }
    return totalrects;
}

JNIEXPORT jint JNICALL
Region_NextIteration(RegionData *pRgnInfo, SurfaceDataBounds *pSpan)
{
    jint index = pRgnInfo->index;
    if (Region_IsRectangular(pRgnInfo)) {
        if (index > 0 || Region_IsEmpty(pRgnInfo)) {
            return 0;
        }
        pSpan->x1 = pRgnInfo->bounds.x1;
        pSpan->x2 = pRgnInfo->bounds.x2;
        pSpan->y1 = pRgnInfo->bounds.y1;
        pSpan->y2 = pRgnInfo->bounds.y2;
        index = 1;
    } else {
        jint *pBands = pRgnInfo->pBands;
        jint xy1, xy2;
        jint numrects = pRgnInfo->numrects;
        while (JNI_TRUE) {
            if (numrects <= 0) {
                if (index >= pRgnInfo->endIndex) {
                    return 0;
                }
                xy1 = pBands[index++];
                if (xy1 >= pRgnInfo->bounds.y2) {
                    return 0;
                }
                if (xy1 < pRgnInfo->bounds.y1) {
                    xy1 = pRgnInfo->bounds.y1;
                }
                xy2 = pBands[index++];
                numrects = pBands[index++];
                if (xy2 > pRgnInfo->bounds.y2) {
                    xy2 = pRgnInfo->bounds.y2;
                }
                if (xy2 <= xy1) {
                    index += numrects * 2;
                    numrects = 0;
                    continue;
                }
                pSpan->y1 = xy1;
                pSpan->y2 = xy2;
            }
            xy1 = pBands[index++];
            xy2 = pBands[index++];
            numrects--;
            if (xy1 >= pRgnInfo->bounds.x2) {
                index += numrects * 2;
                numrects = 0;
                continue;
            }
            if (xy1 < pRgnInfo->bounds.x1) {
                xy1 = pRgnInfo->bounds.x1;
            }
            if (xy2 > pRgnInfo->bounds.x2) {
                xy2 = pRgnInfo->bounds.x2;
            }
            if (xy2 > xy1) {
                pSpan->x1 = xy1;
                pSpan->x2 = xy2;
                break;
            }
        }
        pRgnInfo->numrects = numrects;
    }
    pRgnInfo->index = index;
    return 1;
}

JNIEXPORT void JNICALL
Region_EndIteration(JNIEnv *env, RegionData *pRgnInfo)
{
    if (pRgnInfo->endIndex != 0) {
        (*env)->ReleasePrimitiveArrayCritical(env, pRgnInfo->bands,
                                              pRgnInfo->pBands, JNI_ABORT);
    }
}

/*
 * The code was extracted from
 * src/solaris/native/sun/java2d/x11/X11SurfaceData.c
 * XSetClip() method.
 *
 * If the region is null, the shape is considered to be
 * a rectangle (x1, y1, x2-x1, y2-y1).
 *
 * The *pRect must point to a buffer of initialBufferSize
 * rectangles. If there're more than initialBufferSize
 * rectangles in the region, the buffer is reallocated
 * and its pointer is being stored at the *pRect. Using
 * this practice we may use a small local (on the stack)
 * buffer and avoid allocating/freeing a memory if we
 * operate simple regions.
 */
JNIEXPORT int JNICALL
RegionToYXBandedRectangles(JNIEnv *env,
        jint x1, jint y1, jint x2, jint y2, jobject region,
        RECT_T ** pRect, unsigned int initialBufferSize)
{
    RegionData clipInfo;
    SurfaceDataBounds span;
    int i, numrects;

    if (region == NULL) {
        if (x2 <= x1 || y2 <= y1) {
            /* empty clip, disable rendering */
            numrects = 0;
        } else {
            RECT_SET(**pRect, x1, y1, x2 - x1, y2 - y1);
            numrects = 1;
        }
    } else {
        if (Region_GetInfo(env, region, &clipInfo)) {
            /* return; REMIND: What to do here? */
        }
        Region_StartIteration(env, &clipInfo);
        if ((*env)->ExceptionCheck(env)) {
            return 0;
        }

        numrects = Region_CountIterationRects(&clipInfo);
        if ((unsigned long)numrects > initialBufferSize) {
            *pRect = (RECT_T *) SAFE_SIZE_ARRAY_ALLOC(malloc, numrects, sizeof(RECT_T));
            if (*pRect == NULL) {
                Region_EndIteration(env, &clipInfo);
                JNU_ThrowOutOfMemoryError(env,
                                          "Can't allocate shape region memory");
                return 0;
            }
        }
        for (i = 0; Region_NextIteration(&clipInfo, &span); i++) {
            RECT_SET((*pRect)[i], span.x1, span.y1, span.x2 - span.x1, span.y2 - span.y1);
        }
        Region_EndIteration(env, &clipInfo);
    }

    return numrects;
}
