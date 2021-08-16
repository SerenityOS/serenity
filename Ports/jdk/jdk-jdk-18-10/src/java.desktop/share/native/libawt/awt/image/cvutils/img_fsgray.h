/*
 * Copyright (c) 1996, Oracle and/or its affiliates. All rights reserved.
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
 * This file contains macro definitions for the Encoding category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation uses a Floyd-Steinberg error diffusion technique
 * to produce a very high quality version of an image with only an 8-bit
 * (or less) gray ramp.  The error diffusion technique requires that the
 * input color information be delivered in a special order from the top
 * row to the bottom row and then left to right within each row, thus
 * it is only valid in cases where the ImageProducer has specified the
 * TopDownLeftRight delivery hint.  If the data is not read in that order,
 * no mathematical or memory access errors should occur, but the dithering
 * error will be spread through the pixels of the output image in an
 * unpleasant manner.
 */

#include "img_fsutil.h"

/*
 * These definitions vector the standard macro names to the "Gray"
 * versions of those macros only if the "DitherDeclared" keyword has
 * not yet been defined elsewhere.  The "DitherDeclared" keyword is
 * also defined here to claim ownership of the primary implementation
 * even though this file does not rely on the definitions in any other
 * files.
 */
#ifndef DitherDeclared
#define DitherDeclared
#define DeclareDitherVars       DeclareGrayDitherVars
#define InitDither              InitGrayDither
#define StartDitherLine         StartGrayDitherLine
#define DitherPixel             GrayDitherPixel
#define DitherBufComplete       GrayDitherBufComplete
#endif

typedef struct {
    int gray;
} GrayDitherError;

#define DeclareGrayDitherVars                                   \
    extern unsigned char img_grays[256];                        \
    extern unsigned char img_bwgamma[256];                      \
    int egray;                                                  \
    GrayDitherError *gep;

#define InitGrayDither(cvdata, clrdata, dstTW)                          \
    do {                                                                \
        if (cvdata->fserrors == 0) {                                    \
            int size = (dstTW + 2) * sizeof(GrayDitherError);           \
            gep = (GrayDitherError *) sysMalloc(size);                  \
            if (gep == 0) {                                             \
                SignalError(0, JAVAPKG "OutOfMemoryError", 0);          \
                return SCALEFAILURE;                                    \
            }                                                           \
            memset(gep, 0, size);                                       \
            cvdata->fserrors = (void *) gep;                            \
        }                                                               \
    } while (0)


#define StartGrayDitherLine(cvdata, dstX1, dstY)                        \
    do {                                                                \
        gep = cvdata->fserrors;                                         \
        if (dstX1) {                                                    \
            egray = gep[0].gray;                                        \
            gep += dstX1;                                               \
        } else {                                                        \
            egray = 0;                                                  \
        }                                                               \
    } while (0)

#define GrayDitherPixel(dstX, dstY, pixel, red, green, blue)            \
    do {                                                                \
        int e1, e2, e3;                                                 \
                                                                        \
        /* convert to gray value */                                     \
        e2 = RGBTOGRAY(red, green, blue);                               \
                                                                        \
        /* add previous errors */                                       \
        e2 += gep[1].gray;                                              \
                                                                        \
        /* bounds checking */                                           \
        e2 = ComponentBound(e2);                                        \
                                                                        \
        /* Store the closest color in the destination pixel */          \
        e2 = img_bwgamma[e2];                                           \
        pixel = img_grays[e2];                                          \
        GetPixelRGB(pixel, red, green, blue);                           \
                                                                        \
        /* Set the error from the previous lap */                       \
        gep[1].gray = egray;                                            \
                                                                        \
        /* compute the errors */                                        \
        egray = e2 - red;                                               \
                                                                        \
        /* distribute the errors */                                     \
        DitherDist(gep, e1, e2, e3, egray, gray);                       \
        gep++;                                                          \
    } while (0)

#define GrayDitherBufComplete(cvdata, dstX1)                            \
    do {                                                                \
        if (dstX1) {                                                    \
            gep = cvdata->fserrors;                                     \
            gep[0].gray = egray;                                        \
        }                                                               \
    } while (0)
