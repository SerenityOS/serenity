/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_PARSE_IMAGE_H
#define AWT_PARSE_IMAGE_H

#include <jni.h>
#include <jni_util.h>

/***************************************************************************
 *                               Definitions                               *
 ***************************************************************************/

#ifndef TRUE
#define TRUE (1)
#endif

#ifndef FALSE
#define FALSE (0)
#endif

typedef enum {
    IMG_SUCCESS=0,
    IMG_FAILURE=-1
} ImgStatus_t;

#define UNKNOWN_DATA_TYPE  0
#define BYTE_DATA_TYPE     1
#define SHORT_DATA_TYPE    2
#define INT_DATA_TYPE      3

#define UNKNOWN_RASTER_TYPE   0
#define COMPONENT_RASTER_TYPE 1
#define BANDED_RASTER_TYPE    2
#define PACKED_RASTER_TYPE    3

#define UNKNOWN_CM_TYPE   0
#define COMPONENT_CM_TYPE 1
#define DIRECT_CM_TYPE    2
#define INDEX_CM_TYPE     3
#define PACKED_CM_TYPE    4

/* Packing types */
#define UNKNOWN_PACKING         0
#define BYTE_COMPONENTS         0x1
#define SHORT_COMPONENTS        0x2
#define PACKED_INT              0x3
#define PACKED_SHORT            0x4
#define PACKED_BYTE             0x5

/* Interleaving */
#define INTERLEAVED     0x10
#define BANDED          0x20
#define SINGLE_BAND     0x30
#define PACKED_BAND     0x40

#define BYTE_INTERLEAVED   (BYTE_COMPONENTS  | INTERLEAVED)
#define SHORT_INTERLEAVED  (SHORT_COMPONENTS | INTERLEAVED)
#define BYTE_SINGLE_BAND   (BYTE_COMPONENTS  | SINGLE_BAND)
#define BYTE_PACKED_BAND   (BYTE_COMPONENTS  | PACKED_BAND)
#define SHORT_SINGLE_BAND  (SHORT_COMPONENTS | SINGLE_BAND)
#define BYTE_BANDED        (BYTE_COMPONENTS  | BANDED)
#define SHORT_BANDED       (SHORT_COMPONENTS | BANDED)
#define PACKED_BYTE_INTER  (PACKED_BYTE      | INTERLEAVED)
#define PACKED_SHORT_INTER (PACKED_SHORT     | INTERLEAVED)
#define PACKED_INT_INTER   (PACKED_INT       | INTERLEAVED)

#define MAX_NUMBANDS 32

/* Struct that holds information about a SinglePixelPackedModel object */
typedef struct {
    jint maskArray[MAX_NUMBANDS];
    jint offsets[MAX_NUMBANDS];
    jint nBits[MAX_NUMBANDS];
    jint  maxBitSize;
    jint isUsed; // flag to indicate whether the raster sample model is SPPSM
} SPPSampleModelS_t;

/* Struct that holds information for the Raster object */
typedef struct {
    jobject jraster;       /* The raster object */
    jobject jdata;         /* Data storage object */
    jobject jsampleModel;   /* The sample model */
    SPPSampleModelS_t sppsm; /* SinglePixelPackedSampleModel mask/offsets */

    jint *chanOffsets;      /* Array of channel offsets (or bit offsets) */

    int width;             /* Width of the raster */
    int height;            /* Height of the raster */
    int minX;              /* origin of this raster x */
    int minY;              /* origin of this raster x */

    int baseOriginX;       /* origin of base raster */
    int baseOriginY;       /* origin of base raster x */
    int baseRasterWidth;   /* size of baseRaster */
    int baseRasterHeight;  /* size of baseRaster */
    int numDataElements;   /* Number of data bands in raster */
    int numBands;          /* Number of bands in the raster  */
    int scanlineStride;    /* Scanline Stride */
    int pixelStride;       /* Pixel stride (or pixel bit stride) */
    int dataIsShared;      /* If TRUE, data is shared */
    int rasterType;        /* Type of raster */
    int dataType;          /* Data type of the raster data */
    int dataSize;          /* Number of bytes per data element */
    int type;               /* Raster type */
} RasterS_t;


/* Struct that holds information about the ColorModel object */
typedef struct {
    jobject jrgb;          /* For ICM, rgb lut object */
    jobject jcmodel;
    jobject jcspace;
    jint *nBits;            /* Number of bits per component */

    int cmType;            /* Type of color model */
    int isDefaultCM;       /* If TRUE, it is the default color model */
    int isDefaultCompatCM; /* If TRUE, it is compatible with the default CM */
                           /* Might be 4 byte and band order different */
    int is_sRGB;           /* If TRUE, the color space is sRGB */
    int numComponents;     /* Total number of components */
    int supportsAlpha;     /* If it supports alpha */
    int isAlphaPre;        /* If TRUE, alpha is premultiplied */
    int csType;            /* Type of ColorSpace */
    int transparency;
    int maxNbits;
    int transIdx;          /* For ICM, transparent pixel */
    int mapSize;           /* For ICM, size of the lut */
} ColorModelS_t;

typedef struct {
    int *colorOrder;

    int channelOffset;
    int dataOffset;        /* # bytes into the data array */
    int sStride;
    int pStride;
    int packing;
    int numChans;
    int alphaIndex;        /* -1 if no alpha */
    int needToExpand;      /* If true, the pixels are packed */
    int expandToNbits;     /* If needToExpand, how many bits to allocate */
} HintS_t;

/* Struct that holds information for the BufferedImage object */
typedef struct {
    jobject jimage;        /* The BufferedImage object */
    RasterS_t raster;      /* The raster structure */
    ColorModelS_t cmodel;  /* The color model structure */
    HintS_t hints;         /* Hint structure */
    int     imageType;     /* Type of image */
} BufImageS_t;

/***************************************************************************
 *                      Function Prototypes                                *
 ***************************************************************************/
int awt_parseImage(JNIEnv *env, jobject jimage, BufImageS_t **imagePP,
                   int handleCustom);

int awt_parseRaster(JNIEnv *env, jobject jraster, RasterS_t *rasterP);

int awt_parseColorModel (JNIEnv *env, jobject jcmodel, int imageType,
                         ColorModelS_t *cmP);

void awt_freeParsedRaster(RasterS_t *rasterP, int freeRasterP);

void awt_freeParsedImage(BufImageS_t *imageP, int freeImageP);

int awt_getPixels(JNIEnv *env, RasterS_t *rasterP, void *bufferP);

int awt_setPixels(JNIEnv *env, RasterS_t *rasterP, void *bufferP);

#endif /* AWT_PARSE_IMAGE_H */
