/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef IMAGEINITIDS_H
#define IMAGEINITIDS_H

#include "jni.h"

#ifndef IMGEXTERN
# define IMGEXTERN extern
#endif

/* BufferedImage ids */
IMGEXTERN jfieldID g_BImgRasterID;
IMGEXTERN jfieldID g_BImgTypeID;
IMGEXTERN jfieldID g_BImgCMID;
IMGEXTERN jmethodID g_BImgGetRGBMID;
IMGEXTERN jmethodID g_BImgSetRGBMID;

/* Raster ids */
IMGEXTERN jfieldID g_RasterWidthID;
IMGEXTERN jfieldID g_RasterHeightID;
IMGEXTERN jfieldID g_RasterBaseRasterID;
IMGEXTERN jfieldID g_RasterMinXID;
IMGEXTERN jfieldID g_RasterMinYID;
IMGEXTERN jfieldID g_RasterBaseOriginXID;
IMGEXTERN jfieldID g_RasterBaseOriginYID;
IMGEXTERN jfieldID g_RasterSampleModelID;
IMGEXTERN jfieldID g_RasterDataBufferID;
IMGEXTERN jfieldID g_RasterNumDataElementsID;
IMGEXTERN jfieldID g_RasterNumBandsID;

IMGEXTERN jfieldID g_BCRdataID;
IMGEXTERN jfieldID g_BCRscanstrID;
IMGEXTERN jfieldID g_BCRpixstrID;
IMGEXTERN jfieldID g_BCRdataOffsetsID;
IMGEXTERN jfieldID g_BCRtypeID;
IMGEXTERN jfieldID g_BPRdataID;
IMGEXTERN jfieldID g_BPRscanstrID;
IMGEXTERN jfieldID g_BPRpixstrID;
IMGEXTERN jfieldID g_BPRtypeID;
IMGEXTERN jfieldID g_BPRdataBitOffsetID;
IMGEXTERN jfieldID g_SCRdataID;
IMGEXTERN jfieldID g_SCRscanstrID;
IMGEXTERN jfieldID g_SCRpixstrID;
IMGEXTERN jfieldID g_SCRdataOffsetsID;
IMGEXTERN jfieldID g_SCRtypeID;
IMGEXTERN jfieldID g_ICRdataID;
IMGEXTERN jfieldID g_ICRscanstrID;
IMGEXTERN jfieldID g_ICRpixstrID;
IMGEXTERN jfieldID g_ICRdataOffsetsID;
IMGEXTERN jfieldID g_ICRtypeID;

/* Color Model ids */
JNIEXPORT
IMGEXTERN jfieldID g_CMpDataID;
IMGEXTERN jfieldID g_CMnBitsID;
IMGEXTERN jfieldID g_CMcspaceID;
IMGEXTERN jfieldID g_CMnumComponentsID;
IMGEXTERN jfieldID g_CMsuppAlphaID;
IMGEXTERN jfieldID g_CMisAlphaPreID;
IMGEXTERN jfieldID g_CMtransparencyID;
IMGEXTERN jfieldID g_CMcsTypeID;
IMGEXTERN jfieldID g_CMis_sRGBID;
IMGEXTERN jmethodID g_CMgetRGBdefaultMID;

IMGEXTERN jfieldID g_ICMtransIdxID;
IMGEXTERN jfieldID g_ICMmapSizeID;
IMGEXTERN jfieldID g_ICMrgbID;

/* Sample Model ids */
IMGEXTERN jfieldID g_SMWidthID;
IMGEXTERN jfieldID g_SMHeightID;
IMGEXTERN jmethodID g_SMGetPixelsMID;
IMGEXTERN jmethodID g_SMSetPixelsMID;

/* Single Pixel Packed Sample Model ids */
IMGEXTERN jfieldID g_SPPSMmaskArrID;
IMGEXTERN jfieldID g_SPPSMmaskOffID;
IMGEXTERN jfieldID g_SPPSMnBitsID;
IMGEXTERN jfieldID g_SPPSMmaxBitID;

/* Kernel ids */
IMGEXTERN jfieldID g_KernelWidthID;
IMGEXTERN jfieldID g_KernelHeightID;
IMGEXTERN jfieldID g_KernelDataID;

#endif /* IMAGEINITIDS_H */
