/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <math.h>

#include "jni.h"
#include "jni_util.h"

#include "sun_java2d_pipe_SpanClipRenderer.h"

jfieldID pBandsArrayID;
jfieldID pEndIndexID;
jfieldID pRegionID;
jfieldID pCurIndexID;
jfieldID pNumXbandsID;

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_SpanClipRenderer_initIDs
    (JNIEnv *env, jclass src, jclass rc, jclass ric)
{
    /* Region fields */
    pBandsArrayID = (*env)->GetFieldID(env, rc, "bands", "[I");
    if (pBandsArrayID == NULL) {
        return;
    }
    pEndIndexID = (*env)->GetFieldID(env, rc, "endIndex", "I");
    if (pEndIndexID == NULL) {
        return;
    }

    /* RegionIterator fields */
    pRegionID = (*env)->GetFieldID(env, ric, "region",
                                   "Lsun/java2d/pipe/Region;");
    if (pRegionID == NULL) {
        return;
    }
    pCurIndexID = (*env)->GetFieldID(env, ric, "curIndex", "I");
    if (pCurIndexID == NULL) {
        return;
    }
    pNumXbandsID = (*env)->GetFieldID(env, ric, "numXbands", "I");
    if (pNumXbandsID == NULL) {
        return;
    }
}

static void
fill(jbyte *alpha, jint offset, jint tsize,
     jint x, jint y, jint w, jint h, jbyte value)
{
    alpha += offset + y * tsize + x;
    tsize -= w;
    while (--h >= 0) {
        for (x = 0; x < w; x++) {
            *alpha++ = value;
        }
        alpha += tsize;
    }
}

static jboolean
nextYRange(jint *box, jint *bands, jint endIndex,
           jint *pCurIndex, jint *pNumXbands)
{
    jint curIndex = *pCurIndex;
    jint numXbands = *pNumXbands;
    jboolean ret;

    curIndex += numXbands * 2;
    ret = (curIndex + 3 < endIndex);
    if (ret) {
        box[1] = bands[curIndex++];
        box[3] = bands[curIndex++];
        numXbands = bands[curIndex++];
    } else {
        numXbands = 0;
    }
    *pCurIndex = curIndex;
    *pNumXbands = numXbands;
    return ret;
}

static jboolean
nextXBand(jint *box, jint *bands, jint endIndex,
          jint *pCurIndex, jint *pNumXbands)
{
    jint curIndex = *pCurIndex;
    jint numXbands = *pNumXbands;

    if (numXbands <= 0 || curIndex + 2 > endIndex) {
        return JNI_FALSE;
    }
    numXbands--;
    box[0] = bands[curIndex++];
    box[2] = bands[curIndex++];

    *pCurIndex = curIndex;
    *pNumXbands = numXbands;
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_SpanClipRenderer_fillTile
    (JNIEnv *env, jobject sr, jobject ri,
     jbyteArray alphaTile, jint offset, jint tsize, jintArray boxArray)
{
    jbyte *alpha;
    jint *box;
    jint w, h;
    jsize alphalen;

    if ((*env)->GetArrayLength(env, boxArray) < 4) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "band array");
        return;
    }
    alphalen = (*env)->GetArrayLength(env, alphaTile);

    box = (*env)->GetPrimitiveArrayCritical(env, boxArray, 0);
    if (box == NULL) {
        return;
    }

    w = box[2] - box[0];
    h = box[3] - box[1];

    if (alphalen < offset || (alphalen - offset) / tsize < h) {
        (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);
        JNU_ThrowArrayIndexOutOfBoundsException(env, "alpha tile array");
        return;
    }

    alpha = (*env)->GetPrimitiveArrayCritical(env, alphaTile, 0);
    if (alpha == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);
        return;
    }

    fill(alpha, offset, tsize, 0, 0, w, h, (jbyte) 0xff);

    (*env)->ReleasePrimitiveArrayCritical(env, alphaTile, alpha, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);

    Java_sun_java2d_pipe_SpanClipRenderer_eraseTile(env, sr, ri,
                                                    alphaTile, offset, tsize,
                                                    boxArray);
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_SpanClipRenderer_eraseTile
    (JNIEnv *env, jobject sr, jobject ri,
     jbyteArray alphaTile, jint offset, jint tsize, jintArray boxArray)
{
    jobject region;
    jintArray bandsArray;
    jint *bands;
    jbyte *alpha;
    jint *box;
    jint endIndex;
    jint curIndex;
    jint saveCurIndex;
    jint numXbands;
    jint saveNumXbands;
    jint lox;
    jint loy;
    jint hix;
    jint hiy;
    jint firstx;
    jint firsty;
    jint lastx;
    jint lasty;
    jint curx;
    jsize alphalen;

    if ((*env)->GetArrayLength(env, boxArray) < 4) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "band array");
        return;
    }
    alphalen = (*env)->GetArrayLength(env, alphaTile);

    saveCurIndex = (*env)->GetIntField(env, ri, pCurIndexID);
    saveNumXbands = (*env)->GetIntField(env, ri, pNumXbandsID);
    region = (*env)->GetObjectField(env, ri, pRegionID);
    bandsArray = (*env)->GetObjectField(env, region, pBandsArrayID);
    endIndex = (*env)->GetIntField(env, region, pEndIndexID);

    if (endIndex > (*env)->GetArrayLength(env, bandsArray)) {
        endIndex = (*env)->GetArrayLength(env, bandsArray);
    }

    box = (*env)->GetPrimitiveArrayCritical(env, boxArray, 0);
    if (box == NULL) {
        return;
    }

    lox = box[0];
    loy = box[1];
    hix = box[2];
    hiy = box[3];

    if (alphalen < offset ||
        alphalen < offset + (hix-lox) ||
        (alphalen - offset - (hix-lox)) / tsize < (hiy - loy - 1)) {
        (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);
        JNU_ThrowArrayIndexOutOfBoundsException(env, "alpha tile array");
        return;
    }

    bands = (*env)->GetPrimitiveArrayCritical(env, bandsArray, 0);
    if (bands == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);
        return;
    }
    alpha = (*env)->GetPrimitiveArrayCritical(env, alphaTile, 0);
    if (alpha == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, bandsArray, bands, 0);
        (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);
        return;
    }

    curIndex = saveCurIndex;
    numXbands = saveNumXbands;
    firsty = hiy;
    lasty = hiy;
    firstx = hix;
    lastx = lox;

    while (nextYRange(box, bands, endIndex, &curIndex, &numXbands)) {
        if (box[3] <= loy) {
            saveNumXbands = numXbands;
            saveCurIndex = curIndex;
            continue;
        }
        if (box[1] >= hiy) {
            break;
        }
        if (box[1] < loy) {
            box[1] = loy;
        }
        if (box[3] > hiy) {
            box[3] = hiy;
        }
        curx = lox;
        while (nextXBand(box, bands, endIndex, &curIndex, &numXbands)) {
            if (box[2] <= lox) {
                continue;
            }
            if (box[0] >= hix) {
                break;
            }
            if (box[0] < lox) {
                box[0] = lox;
            }
            if (lasty < box[1]) {
                fill(alpha, offset, tsize,
                     0, lasty - loy,
                     hix - lox, box[1] - lasty, 0);
            }
            lasty = box[3];
            if (firstx > box[0]) {
                firstx = box[0];
            }
            if (curx < box[0]) {
                fill(alpha, offset, tsize,
                     curx - lox, box[1] - loy,
                     box[0] - curx, box[3] - box[1], 0);
            }
            curx = box[2];
            if (curx >= hix) {
                curx = hix;
                break;
            }
        }
        if (curx > lox) {
            if (curx < hix) {
                fill(alpha, offset, tsize,
                     curx - lox, box[1] - loy,
                     hix - curx, box[3] - box[1], 0);
            }
            if (firsty > box[1]) {
                firsty = box[1];
            }
        }
        if (lastx < curx) {
            lastx = curx;
        }
    }

    box[0] = firstx;
    box[1] = firsty;
    box[2] = lastx;
    box[3] = lasty;

    (*env)->ReleasePrimitiveArrayCritical(env, alphaTile, alpha, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, bandsArray, bands, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, boxArray, box, 0);

    (*env)->SetIntField(env, ri, pCurIndexID, saveCurIndex);
    (*env)->SetIntField(env, ri, pNumXbandsID, saveNumXbands);
}
