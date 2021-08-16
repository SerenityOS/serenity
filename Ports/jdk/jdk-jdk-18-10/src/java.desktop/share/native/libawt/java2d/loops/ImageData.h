/*
 * Copyright (c) 1997, 2000, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @author Charlton Innovations, Inc.
 */

#ifndef _Included_ImageData
#define _Included_ImageData

#ifdef __cplusplus
extern "C" {
#endif

#include "colordata.h"


typedef struct ImageDataID {
    jfieldID dataID;
    jfieldID lutDataID;
    jfieldID typeID;
    jfieldID lutDataLengthID;
    jfieldID pixelStrideID;
    jfieldID scanlineStrideID;
    jfieldID numChannelsID;
    jfieldID bytePerChannelID;
    jfieldID pixelsPerDataUnitID;

    jfieldID xViewAreaID;
    jfieldID yViewAreaID;
    jfieldID dxViewAreaID;
    jfieldID dyViewAreaID;
    jfieldID xDeviceAreaID;
    jfieldID yDeviceAreaID;
    jfieldID dxDeviceAreaID;
    jfieldID dyDeviceAreaID;
    jfieldID xOutputAreaID;
    jfieldID yOutputAreaID;
    jfieldID dxOutputAreaID;
    jfieldID dyOutputAreaID;

    jfieldID intDataID;
    jfieldID shortDataID;
    jfieldID byteDataID;

    jfieldID lutArrayID;

    jfieldID originXID;
    jfieldID originYID;

    jfieldID theResRatioID;
    jfieldID theScaleFactorXID;
    jfieldID theScaleFactorYID;

    jfieldID lockMethodID;
    jfieldID lockFunctionID;
    jfieldID platformInfoID;
    jfieldID deviceInfoID;
    jfieldID colorModelID;

    jfieldID grayInverseLutDataID;
} ImageDataID;

extern ImageDataID gImageData;

int minImageWidths(JNIEnv *env, int width1, jobject img1, jobject img2);
int minImageRows(JNIEnv *env, int rows1, jobject img1, jobject img2);

typedef int (*deferredLockFunc) (JNIEnv *env, jobject idData);


typedef struct ImageDataIntLockInfo {
    unsigned int *lockedBuffer;     /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jintArray arrayToLock;      /* filled if buffer not previously locked   */
    unsigned int *arrayLockedBuffer;    /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */
} ImageDataIntLockInfo;

typedef struct ImageDataShortLockInfo {
    unsigned short *lockedBuffer;   /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jshortArray arrayToLock;    /* filled if buffer not previously locked   */
    unsigned short *arrayLockedBuffer;  /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */
} ImageDataShortLockInfo;

typedef struct ImageDataByteLockInfo {
    unsigned char *lockedBuffer;    /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jbyteArray arrayToLock;     /* filled if buffer not previously locked   */
    unsigned char *arrayLockedBuffer;   /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */
} ImageDataByteLockInfo;

typedef struct ImageDataShortIndexedLockInfo {
    unsigned short *lockedBuffer;   /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jshortArray arrayToLock;    /* filled if buffer not previously locked   */
    unsigned short *arrayLockedBuffer;  /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */

    unsigned int *lockedLut;
    jintArray  arrayToLockLut;
    unsigned int *arrayLockedLut;
    unsigned int arrayLutSize;
} ImageDataShortIndexedLockInfo;

typedef struct ImageDataByteIndexedLockInfo {
    unsigned char *lockedBuffer;    /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jbyteArray arrayToLock;     /* filled if buffer not previously locked   */
    unsigned char *arrayLockedBuffer;   /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */

    unsigned int *lockedLut;
    jintArray  arrayToLockLut;
    unsigned int *arrayLockedLut;
    unsigned int arrayLutSize;
    unsigned int minLut[256];   /* provide min size LUT - speed inner loops */
    ColorData *colorData;
    unsigned int lockedForWrite;
    const char* inv_cmap;       /* The inverse cmap to use */
} ImageDataByteIndexedLockInfo;

typedef struct ImageDataIndex8GrayLockInfo {
    unsigned char *lockedBuffer;    /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;

    jbyteArray arrayToLock;     /* filled if buffer not previously locked   */
    unsigned char *arrayLockedBuffer;   /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */

    unsigned int *lockedLut;
    jintArray  arrayToLockLut;
    unsigned int *arrayLockedLut;
    unsigned int arrayLutSize;
    unsigned int minLut[256];
    ColorData *colorData;
    unsigned int lockedForWrite;
    const char* inv_cmap;       /* The inverse cmap to use */

    unsigned int *lockedInverseGrayLut;

} ImageDataIndex8GrayLockInfo;

typedef struct ImageDataIndex12GrayLockInfo {
    unsigned short *lockedBuffer;    /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;

    jshortArray arrayToLock;     /* filled if buffer not previously locked   */
    unsigned short *arrayLockedBuffer;   /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */

    unsigned int *lockedLut;
    jintArray  arrayToLockLut;
    unsigned int *arrayLockedLut;
    unsigned int arrayLutSize;
    unsigned int *minLut;   /* Not used right now, and therefore just having a
                                pointer instead of an array */
    ColorData *colorData;
    unsigned int lockedForWrite;
    const char* inv_cmap;   /* The inverse cmap to use */

    unsigned int *lockedInverseGrayLut;

} ImageDataIndex12GrayLockInfo;

typedef struct ImageDataBitLockInfo {
    unsigned char *lockedBuffer;    /* filled if buffer previously locked   */
    deferredLockFunc lockFunction;  /* ptr to lock function (optional)      */
    unsigned int xOutput,yOutput;   /* top-left of clipped output area      */
    unsigned int scanStride;
    unsigned int bytePerChannel;
    unsigned int pixelStride;
    unsigned int pixelsPerData;

    jbyteArray arrayToLock;     /* filled if buffer not previously locked   */
    unsigned char *arrayLockedBuffer;   /* state needed for unlock of array */
    int arrayLockedOffset;      /* offset from start of array to copy image */
} ImageDataBitLockInfo;

int offsetOfAlphaData(JNIEnv *env, jobject img, int scanStride);
#define offsetOfSrcData(env, img, srcStride, srcBump, offsetVar) \
      do { \
          int x1, y1; \
          int x2, y2; \
          x1 = (*env)->GetIntField(env, img, gImageData.xDeviceAreaID); \
          y1 = (*env)->GetIntField(env, img, gImageData.yDeviceAreaID); \
          x2 = (*env)->GetIntField(env, img, gImageData.xOutputAreaID); \
          y2 = (*env)->GetIntField(env, img, gImageData.yOutputAreaID); \
          offsetVar = srcBump * (x2 - x1) +  srcStride * (y2 - y1); \
      } while (0);

long getPlatformInfoFromImageData(JNIEnv *env, jobject img);

JNIEXPORT void JNICALL
getViewOriginFromImageData(JNIEnv *env, jobject img, int *x, int *y);

JNIEXPORT void JNICALL
getDeviceOriginFromImageData(JNIEnv *env, jobject img, int *x, int *y);

JNIEXPORT void JNICALL
getOutputOriginFromImageData(JNIEnv *env, jobject img, int *x, int *y);

JNIEXPORT void JNICALL
getTypeFromImageData(JNIEnv *env, jobject img, int *type);

JNIEXPORT void JNICALL
getOriginFromImageData(JNIEnv *env, jobject img, int *x, int *y);

JNIEXPORT double JNICALL
getResRatioFromImageData(JNIEnv *env, jobject img);

JNIEXPORT void JNICALL
getScaleFactorFromImageData(JNIEnv *env, jobject img, double *sx, double *sy);

JNIEXPORT int JNICALL
getDeviceInfoFromImageData(JNIEnv *env, jobject img);

/*
 *  Integer component raster handlers
 */

JNIEXPORT void JNICALL getIntImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataIntLockInfo *lockInfo);
JNIEXPORT unsigned int * JNICALL lockIntImageData(
    JNIEnv *env, ImageDataIntLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockIntImageData(
    JNIEnv *env, ImageDataIntLockInfo *lockInfo);

/*
 *  Short component raster handlers
 */

JNIEXPORT void JNICALL getShortImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataShortLockInfo *lockInfo);
JNIEXPORT unsigned short * JNICALL lockShortImageData(
    JNIEnv *env, ImageDataShortLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockShortImageData(
    JNIEnv *env, ImageDataShortLockInfo *lockInfo);

/*
 *  Byte component raster handlers
 */

JNIEXPORT void JNICALL getByteImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataByteLockInfo *lockInfo);
JNIEXPORT unsigned char * JNICALL lockByteImageData(
    JNIEnv *env, ImageDataByteLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockByteImageData(
    JNIEnv *env, ImageDataByteLockInfo *lockInfo);

/*
 *  Short Indexed component raster handlers
 */

JNIEXPORT void JNICALL getShortIndexedImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataShortIndexedLockInfo *lockInfo);
JNIEXPORT unsigned short * JNICALL lockShortIndexedImageData(
    JNIEnv *env, ImageDataShortIndexedLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockShortIndexedImageData(
    JNIEnv *env, ImageDataShortIndexedLockInfo *lockInfo);

/*
 *  Byte Indexed component raster handlers
 */

JNIEXPORT void JNICALL getByteIndexedImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataByteIndexedLockInfo *lockInfo);
JNIEXPORT unsigned char * JNICALL lockByteIndexedImageData(
    JNIEnv *env, ImageDataByteIndexedLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockByteIndexedImageData(
    JNIEnv *env, ImageDataByteIndexedLockInfo *lockInfo);
/*
 *  Index 8 Gray component raster handlers
 */

JNIEXPORT void JNICALL getIndex8GrayImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataIndex8GrayLockInfo *lockInfo);
JNIEXPORT unsigned char * JNICALL lockIndex8GrayImageData(
    JNIEnv *env, ImageDataIndex8GrayLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockIndex8GrayImageData(
    JNIEnv *env, ImageDataIndex8GrayLockInfo *lockInfo);
/*
 *  Index 12 Gray component raster handlers
 */

JNIEXPORT void JNICALL getIndex12GrayImageLockInfo(
    JNIEnv *env, jobject img,
    ImageDataIndex12GrayLockInfo *lockInfo);
JNIEXPORT unsigned short * JNICALL lockIndex12GrayImageData(
    JNIEnv *env, ImageDataIndex12GrayLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockIndex12GrayImageData(
    JNIEnv *env, ImageDataIndex12GrayLockInfo *lockInfo);

/*
 *  Bit component raster handlers
 */

JNIEXPORT void JNICALL getBitImageLockInfo(
    JNIEnv *env, jobject img, ImageDataBitLockInfo *lockInfo);
JNIEXPORT unsigned char *JNICALL lockBitImageData(
    JNIEnv *env, ImageDataBitLockInfo *lockInfo);
JNIEXPORT void JNICALL unlockBitImageData(
    JNIEnv *env, ImageDataBitLockInfo *lockInfo);

#ifdef __cplusplus
};
#endif

#endif
