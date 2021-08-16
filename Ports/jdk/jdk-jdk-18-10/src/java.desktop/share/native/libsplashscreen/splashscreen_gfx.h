/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SPLASHSCREEN_GFX_H
#define SPLASHSCREEN_GFX_H

/*  splashscreen_gfx is a general purpose code for converting pixmaps between various visuals
    it is not very effective, but is universal and concise */

#include "splashscreen_config.h"

enum
{
    BYTE_ORDER_LSBFIRST = 0,    // least significant byte first
    BYTE_ORDER_MSBFIRST = 1,    // most significant byte first
    BYTE_ORDER_NATIVE = 2       // exactly the same as the arch we're running this on
        // will behave identical to _LSBFIRST or _MSBFIRST,
        // but more effective
};

enum
{
    DITHER_SIZE = 16,
    DITHER_MASK = 15
};

typedef struct DitherSettings
{
    int numColors;
    rgbquad_t colorTable[512];
    unsigned matrix[DITHER_SIZE][DITHER_SIZE];
} DitherSettings;

/* this structure is similar to Xlib's Visual */

typedef struct ImageFormat
{
    rgbquad_t mask[4];
    int shift[4];
    int depthBytes;             // 1,2,3 or 4. 3 is not supported for XCVT_BYTE_ORDER_NATIVE.
    int byteOrder;              // see BYTE_ORDER_LSBFIRST, BYTE_ORDER_MSBFIRST or BYTE_ORDER_NATIVE
    int fixedBits;              // this value is or'ed with the color value on get or put, non-indexed only
                                // for indexed color, may be used when pre-decoding the colormap
    rgbquad_t *colorMap;        // colormap should be pre-decoded (i.e. an array of rgbquads)
                                // when colormap is non-NULL, the source color is an index to a colormap, and
                                // masks/shifts are unused.
    unsigned transparentColor;  // only for indexed colors. this is transparent color _INDEX_.
                                // use a more-than-max value when you don't need transparency.
    int premultiplied;
    DitherSettings *dithers;
    int numColors;              // in the colormap, only for indexed color
    rgbquad_t *colorIndex;      // color remapping index for dithering mode
} ImageFormat;

/* this structure defines a rectangular portion of an image buffer. height and/or width may be inverted. */

typedef struct ImageRect
{
    int numLines;               // number of scanlines in the rectangle
    int numSamples;             // number of samples in the line
    int stride;                 // distance between first samples of n'th and n+1'th scanlines, in bytes
    int depthBytes;             // distance between n'th and n+1'th sample in a scanline, in bytes
    void *pBits;                // points to sample 0, scanline 0
    ImageFormat *format;        // format of the samples
    int row, col, jump;         // dithering indexes
} ImageRect;

enum
{
    CVT_COPY,
    CVT_ALPHATEST,
    CVT_BLEND
};

#define  MAX_COLOR_VALUE    255
#define  QUAD_ALPHA_MASK    0xFF000000
#define  QUAD_RED_MASK      0x00FF0000
#define  QUAD_GREEN_MASK    0x0000FF00
#define  QUAD_BLUE_MASK     0x000000FF

#define  QUAD_ALPHA_SHIFT   24
#define  QUAD_RED_SHIFT     16
#define  QUAD_GREEN_SHIFT   8
#define  QUAD_BLUE_SHIFT    0

#define QUAD_ALPHA(value) (((value)&QUAD_ALPHA_MASK)>>QUAD_ALPHA_SHIFT)
#define QUAD_RED(value) (((value)&QUAD_RED_MASK)>>QUAD_RED_SHIFT)
#define QUAD_GREEN(value) (((value)&QUAD_GREEN_MASK)>>QUAD_GREEN_SHIFT)
#define QUAD_BLUE(value) (((value)&QUAD_BLUE_MASK)>>QUAD_BLUE_SHIFT)

#define MAKE_QUAD(r,g,b,a) \
    (((a)<<QUAD_ALPHA_SHIFT)&QUAD_ALPHA_MASK)| \
    (((r)<<QUAD_RED_SHIFT)&QUAD_RED_MASK)| \
    (((g)<<QUAD_GREEN_SHIFT)&QUAD_GREEN_MASK)| \
    (((b)<<QUAD_BLUE_SHIFT)&QUAD_BLUE_MASK) \


/* alpha testing threshold. what's >= the threshold is considered non-transparent when doing
   conversion operation with CVT_ALPHATEST and when generating shapes/regions with
   BitmapToYXBandedRectangles */

#define ALPHA_THRESHOLD     0x80000000

void initRect(ImageRect * pRect, int x, int y, int width, int height, int jump,
        int stride, void *pBits, ImageFormat * format);
int convertRect2(ImageRect * pSrcRect, ImageRect * pDstRect, int mode,
        ImageRect * pSrcRect2);
int convertRect(ImageRect * pSrcRect, ImageRect * pDstRect, int mode);
void convertLine(void *pSrc, int incSrc, void *pDst, int incDst, int n,
        ImageFormat * srcFormat, ImageFormat * dstFormat, int mode,
        void *pSrc2, int incSrc2, ImageFormat * srcFormat2, int row, int col);
void initFormat(ImageFormat * format, int redMask, int greenMask,
        int blueMask, int alphaMask);
int fillRect(rgbquad_t color, ImageRect * pDstRect);
void dumpFormat(ImageFormat * format);

void optimizeFormat(ImageFormat * format);

void initDither(DitherSettings * pDither, int numColors, int scale);

int quantizeColors(int maxNumColors, int *numColors);

void initColorCube(int *numColors, rgbquad_t * pColorMap,
        DitherSettings * pDithers, rgbquad_t * colorIndex);
int platformByteOrder();

#endif
