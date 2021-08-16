/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include "jni_util.h"
#define IMGEXTERN
#include "imageInitIDs.h"

JNIEXPORT void JNICALL
Java_java_awt_image_BufferedImage_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_BImgRasterID = (*env)->GetFieldID(env, cls, "raster",
                                        "Ljava/awt/image/WritableRaster;"));
    CHECK_NULL(g_BImgTypeID = (*env)->GetFieldID(env, cls, "imageType", "I"));
    CHECK_NULL(g_BImgCMID = (*env)->GetFieldID(env, cls, "colorModel",
                                        "Ljava/awt/image/ColorModel;"));
    CHECK_NULL(g_BImgGetRGBMID = (*env)->GetMethodID(env, cls, "getRGB",
                                        "(IIII[III)[I"));
    CHECK_NULL(g_BImgSetRGBMID = (*env)->GetMethodID(env, cls, "setRGB",
                                        "(IIII[III)V"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_Raster_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_RasterWidthID    = (*env)->GetFieldID(env, cls, "width", "I"));
    CHECK_NULL(g_RasterHeightID   = (*env)->GetFieldID(env, cls, "height", "I"));
    CHECK_NULL(g_RasterNumBandsID = (*env)->GetFieldID(env, cls, "numBands", "I"));
    CHECK_NULL(g_RasterMinXID  = (*env)->GetFieldID(env, cls, "minX", "I"));
    CHECK_NULL(g_RasterMinYID  = (*env)->GetFieldID(env, cls, "minY", "I"));
    CHECK_NULL(g_RasterBaseOriginXID  = (*env)->GetFieldID(env, cls,
                                        "sampleModelTranslateX", "I"));
    CHECK_NULL(g_RasterBaseOriginYID  = (*env)->GetFieldID(env, cls,
                                        "sampleModelTranslateY", "I"));
    CHECK_NULL(g_RasterSampleModelID = (*env)->GetFieldID(env, cls,
                                        "sampleModel","Ljava/awt/image/SampleModel;"));
    CHECK_NULL(g_RasterNumDataElementsID = (*env)->GetFieldID(env, cls,
                                        "numDataElements", "I"));
    CHECK_NULL(g_RasterNumBandsID = (*env)->GetFieldID(env, cls, "numBands", "I"));
    CHECK_NULL(g_RasterDataBufferID = (*env)->GetFieldID(env, cls, "dataBuffer",
                                        "Ljava/awt/image/DataBuffer;"));
}

JNIEXPORT void JNICALL
Java_sun_awt_image_ByteComponentRaster_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_BCRdataID = (*env)->GetFieldID(env, cls, "data", "[B"));
    CHECK_NULL(g_BCRscanstrID = (*env)->GetFieldID(env, cls, "scanlineStride", "I"));
    CHECK_NULL(g_BCRpixstrID = (*env)->GetFieldID(env, cls, "pixelStride", "I"));
    CHECK_NULL(g_BCRdataOffsetsID = (*env)->GetFieldID(env, cls, "dataOffsets", "[I"));
    CHECK_NULL(g_BCRtypeID = (*env)->GetFieldID(env, cls, "type", "I"));
}

JNIEXPORT void JNICALL
Java_sun_awt_image_BytePackedRaster_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_BPRdataID = (*env)->GetFieldID(env, cls, "data", "[B"));
    CHECK_NULL(g_BPRscanstrID = (*env)->GetFieldID(env, cls, "scanlineStride", "I"));
    CHECK_NULL(g_BPRpixstrID = (*env)->GetFieldID(env, cls, "pixelBitStride", "I"));
    CHECK_NULL(g_BPRtypeID = (*env)->GetFieldID(env, cls, "type", "I"));
    CHECK_NULL(g_BPRdataBitOffsetID = (*env)->GetFieldID(env, cls, "dataBitOffset", "I"));
}

JNIEXPORT void JNICALL
Java_sun_awt_image_ShortComponentRaster_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_SCRdataID = (*env)->GetFieldID(env, cls, "data", "[S"));
    CHECK_NULL(g_SCRscanstrID = (*env)->GetFieldID(env, cls, "scanlineStride", "I"));
    CHECK_NULL(g_SCRpixstrID = (*env)->GetFieldID(env, cls, "pixelStride", "I"));
    CHECK_NULL(g_SCRdataOffsetsID = (*env)->GetFieldID(env, cls, "dataOffsets", "[I"));
    CHECK_NULL(g_SCRtypeID = (*env)->GetFieldID(env, cls, "type", "I"));
}
JNIEXPORT void JNICALL
Java_sun_awt_image_IntegerComponentRaster_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_ICRdataID = (*env)->GetFieldID(env, cls, "data", "[I"));
    CHECK_NULL(g_ICRscanstrID = (*env)->GetFieldID(env, cls, "scanlineStride", "I"));
    CHECK_NULL(g_ICRpixstrID = (*env)->GetFieldID(env, cls, "pixelStride", "I"));
    CHECK_NULL(g_ICRdataOffsetsID = (*env)->GetFieldID(env, cls, "dataOffsets", "[I"));
    CHECK_NULL(g_ICRtypeID = (*env)->GetFieldID(env, cls, "type", "I"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_SinglePixelPackedSampleModel_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_SPPSMmaskArrID = (*env)->GetFieldID(env, cls, "bitMasks", "[I"));
    CHECK_NULL(g_SPPSMmaskOffID = (*env)->GetFieldID(env, cls, "bitOffsets", "[I"));
    CHECK_NULL(g_SPPSMnBitsID   = (*env)->GetFieldID(env, cls, "bitSizes", "[I"));
    CHECK_NULL(g_SPPSMmaxBitID  = (*env)->GetFieldID(env, cls, "maxBitSize", "I"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_ColorModel_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_CMpDataID = (*env)->GetFieldID (env, cls, "pData", "J"));
    CHECK_NULL(g_CMnBitsID  = (*env)->GetFieldID(env, cls, "nBits", "[I"));
    CHECK_NULL(g_CMcspaceID = (*env)->GetFieldID(env, cls, "colorSpace",
                                    "Ljava/awt/color/ColorSpace;"));
    CHECK_NULL(g_CMnumComponentsID = (*env)->GetFieldID(env, cls, "numComponents", "I"));
    CHECK_NULL(g_CMsuppAlphaID  = (*env)->GetFieldID(env, cls, "supportsAlpha", "Z"));
    CHECK_NULL(g_CMisAlphaPreID = (*env)->GetFieldID(env, cls, "isAlphaPremultiplied",
                                          "Z"));
    CHECK_NULL(g_CMtransparencyID = (*env)->GetFieldID(env, cls, "transparency", "I"));
    CHECK_NULL(g_CMcsTypeID       = (*env)->GetFieldID(env, cls, "colorSpaceType", "I"));
    CHECK_NULL(g_CMis_sRGBID      = (*env)->GetFieldID(env, cls, "is_sRGB", "Z"));
    CHECK_NULL(g_CMgetRGBdefaultMID   = (*env)->GetStaticMethodID(env, cls,
                                                       "getRGBdefault",
                                             "()Ljava/awt/image/ColorModel;"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_IndexColorModel_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_ICMtransIdxID = (*env)->GetFieldID(env, cls, "transparent_index", "I"));
    CHECK_NULL(g_ICMmapSizeID  = (*env)->GetFieldID(env, cls, "map_size", "I"));
    CHECK_NULL(g_ICMrgbID      = (*env)->GetFieldID(env, cls, "rgb", "[I"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_SampleModel_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_SMWidthID = (*env)->GetFieldID(env, cls, "width","I"));
    CHECK_NULL(g_SMHeightID = (*env)->GetFieldID(env, cls, "height","I"));
    CHECK_NULL(g_SMGetPixelsMID = (*env)->GetMethodID(env, cls, "getPixels",
                                      "(IIII[ILjava/awt/image/DataBuffer;)[I"));
    CHECK_NULL(g_SMSetPixelsMID = (*env)->GetMethodID(env, cls, "setPixels",
                                      "(IIII[ILjava/awt/image/DataBuffer;)V"));
}

JNIEXPORT void JNICALL
Java_java_awt_image_Kernel_initIDs(JNIEnv *env, jclass cls) {
    CHECK_NULL(g_KernelWidthID   = (*env)->GetFieldID(env, cls, "width", "I"));
    CHECK_NULL(g_KernelHeightID  = (*env)->GetFieldID(env, cls, "height", "I"));
    CHECK_NULL(g_KernelDataID    = (*env)->GetFieldID(env, cls, "data", "[F"));
}
