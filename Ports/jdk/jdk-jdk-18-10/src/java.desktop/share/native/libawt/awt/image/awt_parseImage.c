/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include "awt_parseImage.h"
#include "imageInitIDs.h"
#include "java_awt_Transparency.h"
#include "java_awt_image_BufferedImage.h"
#include "sun_awt_image_IntegerComponentRaster.h"
#include "sun_awt_image_ImagingLib.h"
#include "java_awt_color_ColorSpace.h"
#include "awt_Mlib.h"
#include "safe_alloc.h"
#include "safe_math.h"

static int setHints(JNIEnv *env, BufImageS_t *imageP);



/* Parse the buffered image.  All of the raster information is returned in the
 * imagePP structure.
 *
 * The handleCustom parameter specifies whether or not the caller
 * can use custom channels.  If it is false and a custom channel
 * is encountered, the returned value will be 0 and all structures
 * will be deallocated.
 *
 * Return value:
 *    -1:     Exception
 *     0:     Can't do it.
 *     1:     Success
 */
int awt_parseImage(JNIEnv *env, jobject jimage, BufImageS_t **imagePP,
                   int handleCustom) {
    BufImageS_t *imageP;
    int status;
    jobject jraster;
    jobject jcmodel;

    /* Make sure the image exists */
    if (JNU_IsNull(env, jimage)) {
        JNU_ThrowNullPointerException(env, "null BufferedImage object");
        return -1;
    }

    if ((imageP = (BufImageS_t *) calloc(1, sizeof(BufImageS_t))) == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Out of memory");
        return -1;
    }
    imageP->jimage = jimage;

    /* Retrieve the raster */
    if ((jraster = (*env)->GetObjectField(env, jimage,
                                          g_BImgRasterID)) == NULL) {
        free((void *) imageP);
        JNU_ThrowNullPointerException(env, "null Raster object");
        return 0;
    }

    /* Retrieve the image type */
    imageP->imageType = (*env)->GetIntField(env, jimage, g_BImgTypeID);

    /* Parse the raster */
    if ((status = awt_parseRaster(env, jraster, &imageP->raster)) <= 0) {
        free((void *)imageP);
        return status;
    }

    /* Retrieve the color model */
    if ((jcmodel = (*env)->GetObjectField(env, jimage, g_BImgCMID)) == NULL) {
        free((void *) imageP);
        JNU_ThrowNullPointerException(env, "null Raster object");
        return 0;
    }

    /* Parse the color model */
    if ((status = awt_parseColorModel(env, jcmodel, imageP->imageType,
                                      &imageP->cmodel)) <= 0) {
        awt_freeParsedRaster(&imageP->raster, FALSE);
        free((void *)imageP);
        return 0;
    }

    /* Set hints  */
    if ((status = setHints(env, imageP)) <= 0) {
        awt_freeParsedImage(imageP, TRUE);
        return 0;
    }

    *imagePP = imageP;

    return status;
}

/* Verifies whether the channel offsets are sane and correspond to the type of
 * the raster.
 *
 * Return value:
 *     0: Failure: channel offsets are invalid
 *     1: Success
 */
static int checkChannelOffsets(RasterS_t *rasterP, int dataArrayLength) {
    int i, lastPixelOffset, lastScanOffset;
    switch (rasterP->rasterType) {
    case COMPONENT_RASTER_TYPE:
        if (!SAFE_TO_MULT(rasterP->height, rasterP->scanlineStride)) {
            return 0;
        }
        if (!SAFE_TO_MULT(rasterP->width, rasterP->pixelStride)) {
            return 0;
        }

        lastScanOffset = (rasterP->height - 1) * rasterP->scanlineStride;
        lastPixelOffset = (rasterP->width - 1) * rasterP->pixelStride;


        if (!SAFE_TO_ADD(lastPixelOffset, lastScanOffset)) {
            return 0;
        }

        lastPixelOffset += lastScanOffset;

        for (i = 0; i < rasterP->numDataElements; i++) {
            int off = rasterP->chanOffsets[i];
            int size = lastPixelOffset + off;

            if (off < 0 || !SAFE_TO_ADD(lastPixelOffset, off)) {
                return 0;
            }

            if (size < lastPixelOffset || size >= dataArrayLength) {
                // an overflow, or insufficient buffer capacity
                return 0;
            }
        }
        return 1;
    case BANDED_RASTER_TYPE:
        // NB:caller does not support the banded rasters yet,
        // so this branch of the code must be re-defined in
        // order to provide valid criteria for the data offsets
        // verification, when/if banded rasters will be supported.
        // At the moment, we prohibit banded rasters as well.
        return 0;
    default:
        // PACKED_RASTER_TYPE: does not support channel offsets
        // UNKNOWN_RASTER_TYPE: should not be used, likely indicates an error
        return 0;
    }
}

/* Parse the raster.  All of the raster information is returned in the
 * rasterP structure.
 *
 * Return value:
 *    -1:     Exception
 *     0:     Can't do it (Custom channel)
 *     1:     Success
 */
int awt_parseRaster(JNIEnv *env, jobject jraster, RasterS_t *rasterP) {
    jobject joffs = NULL;
    /* int status;*/
    jclass singlePixelPackedSampleModelClass = NULL;
    jclass integerComponentRasterClass = NULL;
    jclass byteComponentRasterClass = NULL;
    jclass shortComponentRasterClass = NULL;
    jclass bytePackedRasterClass = NULL;

    if (JNU_IsNull(env, jraster)) {
        JNU_ThrowNullPointerException(env, "null Raster object");
        return -1;
    }

    rasterP->jraster = jraster;
    rasterP->width   = (*env)->GetIntField(env, jraster, g_RasterWidthID);
    rasterP->height  = (*env)->GetIntField(env, jraster, g_RasterHeightID);
    rasterP->numDataElements = (*env)->GetIntField(env, jraster,
                                                   g_RasterNumDataElementsID);
    rasterP->numBands = (*env)->GetIntField(env, jraster,
                                            g_RasterNumBandsID);

    rasterP->baseOriginX = (*env)->GetIntField(env, jraster,
                                               g_RasterBaseOriginXID);
    rasterP->baseOriginY = (*env)->GetIntField(env, jraster,
                                               g_RasterBaseOriginYID);
    rasterP->minX = (*env)->GetIntField(env, jraster, g_RasterMinXID);
    rasterP->minY = (*env)->GetIntField(env, jraster, g_RasterMinYID);

    rasterP->jsampleModel = (*env)->GetObjectField(env, jraster,
                                                   g_RasterSampleModelID);

    if (JNU_IsNull(env, rasterP->jsampleModel)) {
        JNU_ThrowNullPointerException(env, "null Raster object");
        return -1;
    }

    // make sure that the raster type is initialized
    rasterP->rasterType = UNKNOWN_RASTER_TYPE;

    if (rasterP->numBands <= 0 ||
        rasterP->numBands > MAX_NUMBANDS)
    {
        /*
         * we can't handle such kind of rasters due to limitations
         * of SPPSampleModelS_t structure and expand/set methods.
         */
        return 0;
    }

    rasterP->sppsm.isUsed = 0;

    singlePixelPackedSampleModelClass = (*env)->FindClass(env,
                            "java/awt/image/SinglePixelPackedSampleModel");
    CHECK_NULL_RETURN(singlePixelPackedSampleModelClass, -1);
    if ((*env)->IsInstanceOf(env, rasterP->jsampleModel,
                             singlePixelPackedSampleModelClass)) {
        jobject jmask, joffs, jnbits;

        rasterP->sppsm.isUsed = 1;

        rasterP->sppsm.maxBitSize = (*env)->GetIntField(env,
                                                        rasterP->jsampleModel,
                                                        g_SPPSMmaxBitID);
        jmask = (*env)->GetObjectField(env, rasterP->jsampleModel,
                                       g_SPPSMmaskArrID);
        joffs = (*env)->GetObjectField(env, rasterP->jsampleModel,
                                       g_SPPSMmaskOffID);
        jnbits = (*env)->GetObjectField(env, rasterP->jsampleModel,
                                        g_SPPSMnBitsID);
        if (jmask == NULL || joffs == NULL || jnbits == NULL ||
            rasterP->sppsm.maxBitSize < 0)
        {
            JNU_ThrowInternalError(env, "Can't grab SPPSM fields");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, jmask, 0,
                                  rasterP->numBands, rasterP->sppsm.maskArray);
        (*env)->GetIntArrayRegion(env, joffs, 0,
                                  rasterP->numBands, rasterP->sppsm.offsets);
        (*env)->GetIntArrayRegion(env, jnbits, 0,
                                  rasterP->numBands, rasterP->sppsm.nBits);

    }
    rasterP->baseRasterWidth = (*env)->GetIntField(env, rasterP->jsampleModel,
                                                   g_SMWidthID);
    rasterP->baseRasterHeight = (*env)->GetIntField(env,
                                                    rasterP->jsampleModel,
                                                    g_SMHeightID);

    integerComponentRasterClass = (*env)->FindClass(env, "sun/awt/image/IntegerComponentRaster");
    CHECK_NULL_RETURN(integerComponentRasterClass, -1);
    byteComponentRasterClass = (*env)->FindClass(env, "sun/awt/image/ByteComponentRaster");
    CHECK_NULL_RETURN(byteComponentRasterClass, -1);
    shortComponentRasterClass = (*env)->FindClass(env,"sun/awt/image/ShortComponentRaster");
    CHECK_NULL_RETURN(shortComponentRasterClass, -1);
    bytePackedRasterClass = (*env)->FindClass(env, "sun/awt/image/BytePackedRaster");
    CHECK_NULL_RETURN(bytePackedRasterClass, -1);
    if ((*env)->IsInstanceOf(env, jraster, integerComponentRasterClass)){
        rasterP->jdata = (*env)->GetObjectField(env, jraster, g_ICRdataID);
        rasterP->dataType = INT_DATA_TYPE;
        rasterP->dataSize = 4;
        rasterP->dataIsShared = TRUE;
        rasterP->rasterType = COMPONENT_RASTER_TYPE;
        rasterP->type = (*env)->GetIntField(env, jraster, g_ICRtypeID);
        rasterP->scanlineStride = (*env)->GetIntField(env, jraster, g_ICRscanstrID);
        rasterP->pixelStride = (*env)->GetIntField(env, jraster, g_ICRpixstrID);
        joffs = (*env)->GetObjectField(env, jraster, g_ICRdataOffsetsID);
    }
    else if ((*env)->IsInstanceOf(env, jraster, byteComponentRasterClass)){
        rasterP->jdata = (*env)->GetObjectField(env, jraster, g_BCRdataID);
        rasterP->dataType = BYTE_DATA_TYPE;
        rasterP->dataSize = 1;
        rasterP->dataIsShared = TRUE;
        rasterP->rasterType = COMPONENT_RASTER_TYPE;
        rasterP->type = (*env)->GetIntField(env, jraster, g_BCRtypeID);
        rasterP->scanlineStride = (*env)->GetIntField(env, jraster, g_BCRscanstrID);
        rasterP->pixelStride = (*env)->GetIntField(env, jraster, g_BCRpixstrID);
        joffs = (*env)->GetObjectField(env, jraster, g_BCRdataOffsetsID);
    }
    else if ((*env)->IsInstanceOf(env, jraster, shortComponentRasterClass)){
        rasterP->jdata = (*env)->GetObjectField(env, jraster, g_SCRdataID);
        rasterP->dataType = SHORT_DATA_TYPE;
        rasterP->dataSize = 2;
        rasterP->dataIsShared = TRUE;
        rasterP->rasterType = COMPONENT_RASTER_TYPE;
        rasterP->type = (*env)->GetIntField(env, jraster, g_SCRtypeID);
        rasterP->scanlineStride = (*env)->GetIntField(env, jraster, g_SCRscanstrID);
        rasterP->pixelStride = (*env)->GetIntField(env, jraster, g_SCRpixstrID);
        joffs = (*env)->GetObjectField(env, jraster, g_SCRdataOffsetsID);
    }
    else if ((*env)->IsInstanceOf(env, jraster, bytePackedRasterClass)){
        rasterP->rasterType = PACKED_RASTER_TYPE;
        rasterP->dataType = BYTE_DATA_TYPE;
        rasterP->dataSize = 1;
        rasterP->scanlineStride = (*env)->GetIntField(env, jraster, g_BPRscanstrID);
        rasterP->pixelStride = (*env)->GetIntField(env, jraster, g_BPRpixstrID);
        rasterP->jdata = (*env)->GetObjectField(env, jraster, g_BPRdataID);
        rasterP->type = (*env)->GetIntField(env, jraster, g_BPRtypeID);
        rasterP->chanOffsets = NULL;
        if (SAFE_TO_ALLOC_2(rasterP->numDataElements, sizeof(jint))) {
            rasterP->chanOffsets =
                (jint *)malloc(rasterP->numDataElements * sizeof(jint));
        }
        if (rasterP->chanOffsets == NULL) {
            /* Out of memory */
            JNU_ThrowOutOfMemoryError(env, "Out of memory");
            return -1;
        }
        rasterP->chanOffsets[0] = (*env)->GetIntField(env, jraster, g_BPRdataBitOffsetID);
        rasterP->dataType = BYTE_DATA_TYPE;
    }
    else {
        rasterP->type = sun_awt_image_IntegerComponentRaster_TYPE_CUSTOM;
        rasterP->dataType = UNKNOWN_DATA_TYPE;
        rasterP->rasterType = UNKNOWN_RASTER_TYPE;
        rasterP->chanOffsets = NULL;
        /* Custom raster */
        return 0;
    }

    // do basic validation of the raster structure
    if (rasterP->width <= 0 || rasterP->height <= 0 ||
        rasterP->pixelStride <= 0 || rasterP->scanlineStride <= 0)
    {
        // invalid raster
        return -1;
    }

    // channel (data) offsets
    switch (rasterP->rasterType) {
    case COMPONENT_RASTER_TYPE:
    case BANDED_RASTER_TYPE: // note that this routine does not support banded rasters at the moment
        // get channel (data) offsets
        rasterP->chanOffsets = NULL;
        if (SAFE_TO_ALLOC_2(rasterP->numDataElements, sizeof(jint))) {
            rasterP->chanOffsets =
                (jint *)malloc(rasterP->numDataElements * sizeof(jint));
        }
        if (rasterP->chanOffsets == NULL) {
            /* Out of memory */
            JNU_ThrowOutOfMemoryError(env, "Out of memory");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, joffs, 0, rasterP->numDataElements,
                                  rasterP->chanOffsets);
        if (rasterP->jdata == NULL) {
            // unable to verify the raster
            return -1;
        }
        // verify whether channel offsets look sane
        if (!checkChannelOffsets(rasterP, (*env)->GetArrayLength(env, rasterP->jdata))) {
            return -1;
        }
        break;
    default:
        ; // PACKED_RASTER_TYPE does not use the channel offsets.
    }

    /* additional check for sppsm fields validity: make sure that
     * size of raster samples doesn't exceed the data type capacity.
     */
    if (rasterP->dataType > UNKNOWN_DATA_TYPE && /* data type has been recognized */
        rasterP->sppsm.maxBitSize > 0 && /* raster has SPP sample model */
        rasterP->sppsm.maxBitSize > (rasterP->dataSize * 8))
    {
        JNU_ThrowInternalError(env, "Raster samples are too big");
        return -1;
    }

#if 0
    fprintf(stderr,"---------------------\n");
    fprintf(stderr,"Width  : %d\n",rasterP->width);
    fprintf(stderr,"Height : %d\n",rasterP->height);
    fprintf(stderr,"X      : %d\n",rasterP->x);
    fprintf(stderr,"Y      : %d\n",rasterP->y);
    fprintf(stderr,"numC   : %d\n",rasterP->numDataElements);
    fprintf(stderr,"SS     : %d\n",rasterP->scanlineStride);
    fprintf(stderr,"PS     : %d\n",rasterP->pixelStride);
    fprintf(stderr,"CO     : %d\n",rasterP->chanOffsets);
    fprintf(stderr,"shared?: %d\n",rasterP->dataIsShared);
    fprintf(stderr,"RasterT: %d\n",rasterP->rasterType);
    fprintf(stderr,"DataT  : %d\n",rasterP->dataType);
    fprintf(stderr,"---------------------\n");
#endif

    return 1;
}

static int getColorModelType(JNIEnv *env, jobject jcmodel) {
    jclass colorModelClass;

    colorModelClass = (*env)->FindClass(env,
                                        "java/awt/image/IndexColorModel");
    CHECK_NULL_RETURN(colorModelClass, UNKNOWN_CM_TYPE);

    if ((*env)->IsInstanceOf(env, jcmodel, colorModelClass))
    {
        return INDEX_CM_TYPE;
    }

    colorModelClass = (*env)->FindClass(env,
                                        "java/awt/image/PackedColorModel");
    CHECK_NULL_RETURN(colorModelClass, UNKNOWN_CM_TYPE);
    if ((*env)->IsInstanceOf(env, jcmodel, colorModelClass))
    {
        colorModelClass = (*env)->FindClass(env,
                                            "java/awt/image/DirectColorModel");
        CHECK_NULL_RETURN(colorModelClass, UNKNOWN_CM_TYPE);
        if  ((*env)->IsInstanceOf(env, jcmodel, colorModelClass)) {
            return DIRECT_CM_TYPE;
        }
        else {
            return PACKED_CM_TYPE;
        }
    }
    colorModelClass = (*env)->FindClass(env,
                                        "java/awt/image/ComponentColorModel");
    CHECK_NULL_RETURN(colorModelClass, UNKNOWN_CM_TYPE);
    if ((*env)->IsInstanceOf(env, jcmodel, colorModelClass))
    {
        return COMPONENT_CM_TYPE;
    }

    return UNKNOWN_CM_TYPE;
}

int awt_parseColorModel (JNIEnv *env, jobject jcmodel, int imageType,
                         ColorModelS_t *cmP) {
    /*jmethodID jID;   */
    jobject jnBits;
    jsize   nBitsLength;

    int i;
    static jobject s_jdefCM = NULL;

    if (JNU_IsNull(env, jcmodel)) {
        JNU_ThrowNullPointerException(env, "null ColorModel object");
        return -1;
    }

    cmP->jcmodel = jcmodel;

    cmP->jcspace = (*env)->GetObjectField(env, jcmodel, g_CMcspaceID);

    cmP->numComponents = (*env)->GetIntField(env, jcmodel,
                                             g_CMnumComponentsID);
    cmP->supportsAlpha = (*env)->GetBooleanField(env, jcmodel,
                                                 g_CMsuppAlphaID);
    cmP->isAlphaPre = (*env)->GetBooleanField(env,jcmodel,
                                              g_CMisAlphaPreID);
    cmP->transparency = (*env)->GetIntField(env, jcmodel,
                                            g_CMtransparencyID);

    jnBits = (*env)->GetObjectField(env, jcmodel, g_CMnBitsID);
    if (jnBits == NULL) {
        JNU_ThrowNullPointerException(env, "null nBits structure in CModel");
        return -1;
    }

    nBitsLength = (*env)->GetArrayLength(env, jnBits);
    if (nBitsLength != cmP->numComponents) {
        // invalid number of components?
        return -1;
    }

    cmP->nBits = NULL;
    if (SAFE_TO_ALLOC_2(cmP->numComponents, sizeof(jint))) {
        cmP->nBits = (jint *)malloc(cmP->numComponents * sizeof(jint));
    }

    if (cmP->nBits == NULL){
        JNU_ThrowOutOfMemoryError(env, "Out of memory");
        return -1;
    }
    (*env)->GetIntArrayRegion(env, jnBits, 0, cmP->numComponents,
                              cmP->nBits);
    cmP->maxNbits = 0;
    for (i=0; i < cmP->numComponents; i++) {
        if (cmP->maxNbits < cmP->nBits[i]) {
            cmP->maxNbits = cmP->nBits[i];
        }
    }

    cmP->is_sRGB = (*env)->GetBooleanField(env, cmP->jcmodel, g_CMis_sRGBID);

    cmP->csType = (*env)->GetIntField(env, cmP->jcmodel, g_CMcsTypeID);

    cmP->cmType = getColorModelType(env, jcmodel);
    JNU_CHECK_EXCEPTION_RETURN(env, -1);

    cmP->isDefaultCM = FALSE;
    cmP->isDefaultCompatCM = FALSE;

    /* look for standard cases */
    if (imageType == java_awt_image_BufferedImage_TYPE_INT_ARGB) {
        cmP->isDefaultCM = TRUE;
        cmP->isDefaultCompatCM = TRUE;
    } else if (imageType == java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE ||
               imageType == java_awt_image_BufferedImage_TYPE_INT_RGB ||
               imageType == java_awt_image_BufferedImage_TYPE_INT_BGR ||
               imageType == java_awt_image_BufferedImage_TYPE_4BYTE_ABGR ||
               imageType == java_awt_image_BufferedImage_TYPE_4BYTE_ABGR_PRE)
    {
        cmP->isDefaultCompatCM = TRUE;
    }
    else {
        /* Figure out if this is the default CM */
        if (s_jdefCM == NULL) {
            jobject defCM;
            jclass jcm = (*env)->FindClass(env, "java/awt/image/ColorModel");
            CHECK_NULL_RETURN(jcm, -1);
            defCM = (*env)->CallStaticObjectMethod(env, jcm,
                                                   g_CMgetRGBdefaultMID, NULL);
            s_jdefCM = (*env)->NewGlobalRef(env, defCM);
            if (defCM == NULL || s_jdefCM == NULL) {
                (*env)->ExceptionClear(env);
                JNU_ThrowNullPointerException(env, "Unable to find default CM");
                return -1;
            }
        }
        cmP->isDefaultCM = ((*env)->IsSameObject(env, s_jdefCM, jcmodel));
        cmP->isDefaultCompatCM = cmP->isDefaultCM;
    }

    /* check whether image attributes correspond to default cm */
    if (cmP->isDefaultCompatCM) {
        if (cmP->csType != java_awt_color_ColorSpace_TYPE_RGB ||
            !cmP->is_sRGB)
        {
            return -1;
        }

        for (i = 0; i < cmP->numComponents; i++) {
            if (cmP->nBits[i] != 8) {
                return -1;
            }
        }
    }

    /* Get index color model attributes */
    if (imageType == java_awt_image_BufferedImage_TYPE_BYTE_INDEXED ||
        cmP->cmType == INDEX_CM_TYPE)
    {
        cmP->transIdx = (*env)->GetIntField(env, jcmodel, g_ICMtransIdxID);
        cmP->mapSize = (*env)->GetIntField(env, jcmodel, g_ICMmapSizeID);
        cmP->jrgb    = (*env)->GetObjectField(env, jcmodel, g_ICMrgbID);
        if (cmP->transIdx == -1) {
            /* Need to find the transparent index */
            int *rgb = (int *) (*env)->GetPrimitiveArrayCritical(env,
                                                                 cmP->jrgb,
                                                                 NULL);
            if (rgb == NULL) {
                return -1;
            }
            for (i=0; i < cmP->mapSize; i++) {
                if ((rgb[i]&0xff000000) == 0) {
                    cmP->transIdx = i;
                    break;
                }
            }
            (*env)->ReleasePrimitiveArrayCritical(env, cmP->jrgb, rgb,
                                                  JNI_ABORT);
            if (cmP->transIdx == -1) {
                /* Now what? No transparent pixel... */
                cmP->transIdx = 0;
            }
        }
    }

    return 1;
}

void awt_freeParsedRaster(RasterS_t *rasterP, int freeRasterP) {
    if (rasterP->chanOffsets) {
        free((void *) rasterP->chanOffsets);
    }

    if (freeRasterP) {
        free((void *) rasterP);
    }
}

void awt_freeParsedImage(BufImageS_t *imageP, int freeImageP) {
    if (imageP->hints.colorOrder) {
        free ((void *) imageP->hints.colorOrder);
    }

    if (imageP->cmodel.nBits) {
        free ((void *) imageP->cmodel.nBits);
    }

    /* Free the raster */
    awt_freeParsedRaster(&imageP->raster, FALSE);

    if (freeImageP) {
        free((void *) imageP);
    }
}

static void
awt_getBIColorOrder(int type, int *colorOrder) {
    switch(type) {
        case java_awt_image_BufferedImage_TYPE_INT_ARGB:
        case java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE:
#ifdef _LITTLE_ENDIAN
            colorOrder[0] = 2;
            colorOrder[1] = 1;
            colorOrder[2] = 0;
            colorOrder[3] = 3;
#else
            colorOrder[0] = 1;
            colorOrder[1] = 2;
            colorOrder[2] = 3;
            colorOrder[3] = 0;
#endif
            break;
        case java_awt_image_BufferedImage_TYPE_INT_BGR:
#ifdef _LITTLE_ENDIAN
            colorOrder[0] = 0;
            colorOrder[1] = 1;
            colorOrder[2] = 2;
#else
            colorOrder[0] = 3;
            colorOrder[1] = 2;
            colorOrder[2] = 1;
#endif
            break;
        case java_awt_image_BufferedImage_TYPE_INT_RGB:
#ifdef _LITTLE_ENDIAN
            colorOrder[0] = 2;
            colorOrder[1] = 1;
            colorOrder[2] = 0;
#else
            colorOrder[0] = 1;
            colorOrder[1] = 2;
            colorOrder[2] = 3;
#endif
            break;
        case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR:
        case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR_PRE:
            colorOrder[0] = 3;
            colorOrder[1] = 2;
            colorOrder[2] = 1;
            colorOrder[3] = 0;
            break;
        case java_awt_image_BufferedImage_TYPE_3BYTE_BGR:
            colorOrder[0] = 2;
            colorOrder[1] = 1;
            colorOrder[2] = 0;
            break;
        case java_awt_image_BufferedImage_TYPE_USHORT_565_RGB:
        case java_awt_image_BufferedImage_TYPE_USHORT_555_RGB:
            colorOrder[0] = 0;
            colorOrder[1] = 1;
            colorOrder[2] = 2;
            break;
        case java_awt_image_BufferedImage_TYPE_BYTE_GRAY:
        case java_awt_image_BufferedImage_TYPE_USHORT_GRAY:
        case java_awt_image_BufferedImage_TYPE_BYTE_BINARY:
        case java_awt_image_BufferedImage_TYPE_BYTE_INDEXED:
            colorOrder[0] = 0;
            break;
    }
}

static int
setHints(JNIEnv *env, BufImageS_t *imageP) {
    HintS_t *hintP = &imageP->hints;
    RasterS_t *rasterP = &imageP->raster;
    ColorModelS_t *cmodelP = &imageP->cmodel;
    int imageType = imageP->imageType;

    // check whether raster and color model are compatible
    if (cmodelP->numComponents != rasterP->numBands) {
        if (cmodelP->cmType != INDEX_CM_TYPE) {
            return -1;
        }
    }

    hintP->numChans = imageP->cmodel.numComponents;
    hintP->colorOrder = NULL;
    if (SAFE_TO_ALLOC_2(hintP->numChans, sizeof(int))) {
        hintP->colorOrder = (int *)malloc(hintP->numChans * sizeof(int));
    }
    if (hintP->colorOrder == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Out of memory");
        return -1;
    }
    if (imageType != java_awt_image_BufferedImage_TYPE_CUSTOM) {
        awt_getBIColorOrder(imageType, hintP->colorOrder);
    }
    if (imageType == java_awt_image_BufferedImage_TYPE_INT_ARGB ||
        imageType == java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE ||
        imageType == java_awt_image_BufferedImage_TYPE_INT_RGB)
    {
        hintP->channelOffset = rasterP->chanOffsets[0];
        /* These hints are #bytes  */
        hintP->dataOffset    = hintP->channelOffset*rasterP->dataSize;
        hintP->sStride = rasterP->scanlineStride*rasterP->dataSize;
        hintP->pStride = rasterP->pixelStride*rasterP->dataSize;
        hintP->packing = BYTE_INTERLEAVED;
    } else if (imageType ==java_awt_image_BufferedImage_TYPE_4BYTE_ABGR ||
               imageType==java_awt_image_BufferedImage_TYPE_4BYTE_ABGR_PRE||
               imageType == java_awt_image_BufferedImage_TYPE_3BYTE_BGR ||
               imageType == java_awt_image_BufferedImage_TYPE_INT_BGR)
    {
        if (imageType == java_awt_image_BufferedImage_TYPE_INT_BGR) {
            hintP->channelOffset = rasterP->chanOffsets[0];
        }
        else {
            hintP->channelOffset = rasterP->chanOffsets[hintP->numChans-1];
        }
        hintP->dataOffset    = hintP->channelOffset*rasterP->dataSize;
        hintP->sStride = rasterP->scanlineStride*rasterP->dataSize;
        hintP->pStride = rasterP->pixelStride*rasterP->dataSize;
        hintP->packing = BYTE_INTERLEAVED;
    } else if (imageType==java_awt_image_BufferedImage_TYPE_USHORT_565_RGB ||
               imageType==java_awt_image_BufferedImage_TYPE_USHORT_555_RGB) {
        hintP->needToExpand  = TRUE;
        hintP->expandToNbits = 8;
        hintP->packing = PACKED_SHORT_INTER;
    } else if (cmodelP->cmType == INDEX_CM_TYPE) {
        int i;
        hintP->numChans = 1;
        hintP->channelOffset = rasterP->chanOffsets[0];
        hintP->dataOffset    = hintP->channelOffset*rasterP->dataSize;
        hintP->sStride = rasterP->scanlineStride*rasterP->dataSize;
        hintP->pStride = rasterP->pixelStride*rasterP->dataSize;
        switch(rasterP->dataType ) {
        case BYTE_DATA_TYPE:
            if (rasterP->rasterType == PACKED_RASTER_TYPE) {
                hintP->needToExpand = TRUE;
                hintP->expandToNbits = 8;
                hintP->packing = BYTE_PACKED_BAND;
            }
            else {
                hintP->packing = BYTE_SINGLE_BAND;
            }
            break;
        case SHORT_DATA_TYPE:
            hintP->packing = SHORT_SINGLE_BAND;
            break;
        case INT_DATA_TYPE:
        default:
            hintP->packing = UNKNOWN_PACKING;
            break;
        }
        for (i=0; i < hintP->numChans; i++) {
            hintP->colorOrder[i] = i;
        }
    }
    else if (cmodelP->cmType == COMPONENT_CM_TYPE) {
        /* Figure out if it is interleaved */
        int bits=1;
        int i;
        int low = rasterP->chanOffsets[0];
        int diff;
        int banded = 0;
        for (i=1; i < hintP->numChans; i++) {
            if (rasterP->chanOffsets[i] < low) {
                low = rasterP->chanOffsets[i];
            }
        }
        for (i=1; i < hintP->numChans; i++) {
            diff = rasterP->chanOffsets[i]-low;
            if (diff < hintP->numChans) {
                if (bits & (1<<diff)) {
                    /* Overlapping samples */
                    /* Could just copy */
                    return -1;
                }
                bits |= (1<<diff);
            }
            else if (diff >= rasterP->width) {
                banded = 1;
            }
            /* Ignore the case if bands are overlapping */
        }
        hintP->channelOffset = low;
        hintP->dataOffset    = low*rasterP->dataSize;
        hintP->sStride       = rasterP->scanlineStride*rasterP->dataSize;
        hintP->pStride       = rasterP->pixelStride*rasterP->dataSize;
        switch(rasterP->dataType) {
        case BYTE_DATA_TYPE:
            hintP->packing = BYTE_COMPONENTS;
            break;
        case SHORT_DATA_TYPE:
            hintP->packing = SHORT_COMPONENTS;
            break;
        default:
            /* Don't handle any other case */
            return -1;
        }
        if (bits == ((1<<hintP->numChans)-1)) {
            hintP->packing |= INTERLEAVED;
            for (i=0; i < hintP->numChans; i++) {
                hintP->colorOrder[rasterP->chanOffsets[i]-low] = i;
            }
        }
        else if (banded == 1) {
            int bandSize = rasterP->width*rasterP->height;
            hintP->packing |= BANDED;
            for (i=0; i < hintP->numChans; i++) {
                /* REMIND: Not necessarily correct */
                hintP->colorOrder[(rasterP->chanOffsets[i]-low)%bandSize] = i;
            }
        }
        else {
            return -1;
        }
    }
    else if (cmodelP->cmType == DIRECT_CM_TYPE || cmodelP->cmType == PACKED_CM_TYPE) {
        int i;

        /* do some sanity check first: make sure that
         * - sample model is SinglePixelPackedSampleModel
         * - number of bands in the raster corresponds to the number
         *   of color components in the color model
         */
        if (!rasterP->sppsm.isUsed ||
            rasterP->numBands != cmodelP->numComponents)
        {
            /* given raster is not compatible with the color model,
             * so the operation has to be aborted.
             */
            return -1;
        }

        if (cmodelP->maxNbits > 8) {
            hintP->needToExpand = TRUE;
            hintP->expandToNbits = cmodelP->maxNbits;
        }
        else {
            for (i=0; i < rasterP->numBands; i++) {
                if (!(rasterP->sppsm.offsets[i] % 8)) {
                    hintP->needToExpand  = TRUE;
                    hintP->expandToNbits = 8;
                    break;
                }
                else {
                    hintP->colorOrder[i] = rasterP->sppsm.offsets[i]>>3;
                }
            }
        }

        hintP->channelOffset = rasterP->chanOffsets[0];
        hintP->dataOffset    = hintP->channelOffset*rasterP->dataSize;
        hintP->sStride = rasterP->scanlineStride*rasterP->dataSize;
        hintP->pStride = rasterP->pixelStride*rasterP->dataSize;
        if (hintP->needToExpand) {
            switch(rasterP->dataType) {
            case BYTE_DATA_TYPE:
                hintP->packing = PACKED_BYTE_INTER;
                break;
            case SHORT_DATA_TYPE:
                hintP->packing = PACKED_SHORT_INTER;
                break;
            case INT_DATA_TYPE:
                hintP->packing = PACKED_INT_INTER;
                break;
            default:
                /* Don't know what it is */
                return -1;
            }
        }
        else {
            hintP->packing = BYTE_INTERLEAVED;

        }
    }
    else {
        /* REMIND: Need to handle more cases */
        return -1;
    }

    return 1;
}

#define MAX_TO_GRAB (10240)

typedef union {
    void *pv;
    unsigned char *pb;
    unsigned short *ps;
} PixelData_t;


int awt_getPixels(JNIEnv *env, RasterS_t *rasterP, void *bufferP) {
    const int w = rasterP->width;
    const int h = rasterP->height;
    const int numBands = rasterP->numBands;
    int y;
    int i;
    int maxLines;
    jobject jsm;
    int off = 0;
    jarray jdata = NULL;
    jobject jdatabuffer;
    int *dataP;
    int maxSamples;
    PixelData_t p;

    if (bufferP == NULL) {
        return -1;
    }

    if (rasterP->dataType != BYTE_DATA_TYPE &&
        rasterP->dataType != SHORT_DATA_TYPE)
    {
        return -1;
    }

    p.pv = bufferP;

    if (!SAFE_TO_MULT(w, numBands)) {
        return -1;
    }
    maxSamples = w * numBands;

    maxLines = maxSamples > MAX_TO_GRAB ? 1 : (MAX_TO_GRAB / maxSamples);
    if (maxLines > h) {
        maxLines = h;
    }

    if (!SAFE_TO_MULT(maxSamples, maxLines)) {
        return -1;
    }

    maxSamples *= maxLines;

    jsm = (*env)->GetObjectField(env, rasterP->jraster, g_RasterSampleModelID);
    jdatabuffer = (*env)->GetObjectField(env, rasterP->jraster,
                                         g_RasterDataBufferID);

    jdata = (*env)->NewIntArray(env, maxSamples);
    if (JNU_IsNull(env, jdata)) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Out of Memory");
        return -1;
    }

    for (y = 0; y < h; y += maxLines) {
        if (y + maxLines > h) {
            maxLines = h - y;
            maxSamples = w * numBands * maxLines;
        }

        (*env)->CallObjectMethod(env, jsm, g_SMGetPixelsMID,
                                 0, y, w,
                                 maxLines, jdata, jdatabuffer);

        if ((*env)->ExceptionOccurred(env)) {
            (*env)->DeleteLocalRef(env, jdata);
            return -1;
        }

        dataP = (int *) (*env)->GetPrimitiveArrayCritical(env, jdata,
                                                          NULL);
        if (dataP == NULL) {
            (*env)->DeleteLocalRef(env, jdata);
            return -1;
        }

        switch (rasterP->dataType) {
        case BYTE_DATA_TYPE:
            for (i = 0; i < maxSamples; i ++) {
                p.pb[off++] = (unsigned char) dataP[i];
            }
            break;
        case SHORT_DATA_TYPE:
            for (i = 0; i < maxSamples; i ++) {
                p.ps[off++] = (unsigned short) dataP[i];
            }
            break;
        }

        (*env)->ReleasePrimitiveArrayCritical(env, jdata, dataP,
                                              JNI_ABORT);
    }
    (*env)->DeleteLocalRef(env, jdata);

    return 1;
}

int awt_setPixels(JNIEnv *env, RasterS_t *rasterP, void *bufferP) {
    const int w = rasterP->width;
    const int h = rasterP->height;
    const int numBands = rasterP->numBands;

    int y;
    int i;
    int maxLines;
    jobject jsm;
    int off = 0;
    jarray jdata = NULL;
    jobject jdatabuffer;
    int *dataP;
    int maxSamples;
    PixelData_t p;

    if (bufferP == NULL) {
        return -1;
    }

    if (rasterP->dataType != BYTE_DATA_TYPE &&
        rasterP->dataType != SHORT_DATA_TYPE)
    {
        return -1;
    }

    p.pv = bufferP;

    if (!SAFE_TO_MULT(w, numBands)) {
        return -1;
    }
    maxSamples = w * numBands;

    maxLines = maxSamples > MAX_TO_GRAB ? 1 : (MAX_TO_GRAB / maxSamples);
    if (maxLines > h) {
        maxLines = h;
    }

    if (!SAFE_TO_MULT(maxSamples, maxLines)) {
        return -1;
    }

    maxSamples *= maxLines;

    jsm = (*env)->GetObjectField(env, rasterP->jraster, g_RasterSampleModelID);
    jdatabuffer = (*env)->GetObjectField(env, rasterP->jraster,
                                         g_RasterDataBufferID);

    jdata = (*env)->NewIntArray(env, maxSamples);
    if (JNU_IsNull(env, jdata)) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Out of Memory");
        return -1;
    }

    for (y = 0; y < h; y += maxLines) {
        if (y + maxLines > h) {
            maxLines = h - y;
            maxSamples = w * numBands * maxLines;
        }
        dataP = (int *) (*env)->GetPrimitiveArrayCritical(env, jdata,
                                                          NULL);
        if (dataP == NULL) {
            (*env)->DeleteLocalRef(env, jdata);
            return -1;
        }

        switch (rasterP->dataType) {
        case BYTE_DATA_TYPE:
            for (i = 0; i < maxSamples; i ++) {
                dataP[i] = p.pb[off++];
            }
            break;
        case SHORT_DATA_TYPE:
            for (i = 0; i < maxSamples; i ++) {
                dataP[i] = p.ps[off++];
            }
            break;
        }

        (*env)->ReleasePrimitiveArrayCritical(env, jdata, dataP,
                                              JNI_ABORT);

        (*env)->CallVoidMethod(env, jsm, g_SMSetPixelsMID,
                               0, y, w,
                               maxLines, jdata, jdatabuffer);

        if ((*env)->ExceptionOccurred(env)) {
            (*env)->DeleteLocalRef(env, jdata);
            return -1;
        }
    }

    (*env)->DeleteLocalRef(env, jdata);

    return 1;
}
