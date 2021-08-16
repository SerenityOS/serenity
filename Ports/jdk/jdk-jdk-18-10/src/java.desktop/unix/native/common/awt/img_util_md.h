/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "color.h"

#if !defined(HEADLESS) && !defined(MACOSX)
typedef struct {
    ImgConvertData cvdata;      /* The data needed by ImgConvertFcn's */
    struct Hsun_awt_image_ImageRepresentation *hJavaObject;     /* backptr */
    XID pixmap;                 /* The X11 pixmap containing the image */
    XID mask;                   /* The X11 pixmap with the transparency mask */
    int bgcolor;                /* The current bg color installed in pixmap */

    int depth;                  /* The depth of the destination image */
    int dstW;                   /* The width of the destination pixmap */
    int dstH;                   /* The height of the destination pixmap */

    XImage *xim;                /* The Ximage structure for the temp buffer */
    XImage *maskim;             /* The Ximage structure for the mask */

    int hints;                  /* The delivery hints from the producer */

    Region curpixels;           /* The region of randomly converted pixels */
    struct {
        int num;                /* The last fully delivered scanline */
        char *seen;             /* The lines which have been delivered */
    } curlines;                 /* For hints=COMPLETESCANLINES */
} IRData;

typedef unsigned int MaskBits;

extern int image_Done(IRData *ird, int x1, int y1, int x2, int y2);

extern void *image_InitMask(IRData *ird, int x1, int y1, int x2, int y2);

#define BufComplete(cvdata, dstX1, dstY1, dstX2, dstY2)         \
    image_Done((IRData *) cvdata, dstX1, dstY1, dstX2, dstY2)

#define SendRow(ird, dstY, dstX1, dstX2)

#define ImgInitMask(cvdata, x1, y1, x2, y2)                     \
    image_InitMask((IRData *)cvdata, x1, y1, x2, y2)

#define ScanBytes(cvdata)       (((IRData *)cvdata)->xim->bytes_per_line)

#define MaskScan(cvdata)                                        \
        ((((IRData *)cvdata)->maskim->bytes_per_line) >> 2)

#endif /* !HEADLESS && !MACOSX */

#define MaskOffset(x)           ((x) >> 5)

#define MaskInit(x)             (1U << (31 - ((x) & 31)))

#define SetOpaqueBit(mask, bit)         ((mask) |= (bit))
#define SetTransparentBit(mask, bit)    ((mask) &= ~(bit))

#define UCHAR_ARG(uc)    ((unsigned char)(uc))
#define ColorCubeFSMap(r, g, b) \
    cData->img_clr_tbl [    ((UCHAR_ARG(r)>>3)<<10) |                   \
                    ((UCHAR_ARG(g)>>3)<<5) | (UCHAR_ARG(b)>>3)]

#define ColorCubeOrdMapSgn(r, g, b) \
    ((dstLockInfo.inv_cmap)[    ((UCHAR_ARG(r)>>3)<<10) |                   \
                    ((UCHAR_ARG(g)>>3)<<5) | (UCHAR_ARG(b)>>3)])

#define GetPixelRGB(pixel, red, green, blue)                    \
    do {                                                        \
        ColorEntry *cp = &awt_Colors[pixel];                    \
        red = cp->r;                                            \
        green = cp->g;                                          \
        blue = cp->b;                                           \
    } while (0)

#define CUBEMAP(r,g,b) ColorCubeOrdMapSgn(r, g, b)
#define cubemapArray 1

extern uns_ordered_dither_array img_oda_alpha;

extern void freeICMColorData(ColorData *pData);

JNIEXPORT void JNICALL
initInverseGrayLut(int* prgb, int rgbsize, ColorData* cData);

extern unsigned char* initCubemap(int* cmap, int cmap_len, int cube_dim);
extern void initDitherTables(ColorData* cData);

#define SET_CUBEMAPARRAY \
    lockInfo->inv_cmap = (const char*)lockInfo->colorData->img_clr_tbl
