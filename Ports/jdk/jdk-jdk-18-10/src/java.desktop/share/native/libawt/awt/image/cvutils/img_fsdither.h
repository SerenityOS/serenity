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
 * (or less) RGB colormap or gray ramp.  The error diffusion technique
 * requires that the input color information be delivered in a special
 * order from the top row to the bottom row and then left to right within
 * each row, thus it is only valid in cases where the ImageProducer has
 * specified the TopDownLeftRight delivery hint.  If the data is not read
 * in that order, no mathematical or memory access errors should occur,
 * but the dithering error will be spread through the pixels of the output
 * image in an unpleasant manner.
 */

/*
 * These definitions vector the standard macro names to the "Any"
 * versions of those macros.  The "DitherDeclared" keyword is also
 * defined to indicate to the other include files that they are not
 * defining the primary implementation.  All other include files
 * will check for the existance of the "DitherDeclared" keyword
 * and define their implementations of the Encoding macros using
 * more specific names without overriding the standard names.  This
 * is done so that the other files can be included here to reuse
 * their implementations for the specific cases.
 */
#define DitherDeclared
#define DeclareDitherVars       DeclareAnyDitherVars
#define InitDither              InitAnyDither
#define StartDitherLine         StartAnyDitherLine
#define DitherPixel             AnyDitherPixel
#define DitherBufComplete       AnyDitherBufComplete

/* Include the specific implementations for color and grayscale displays */
#include "img_fscolor.h"
#include "img_fsgray.h"

#define DeclareAnyDitherVars                                    \
    DeclareColorDitherVars                                      \
    DeclareGrayDitherVars                                       \
    int grayscale;

#define InitAnyDither(cvdata, clrdata, dstTW)                           \
    do {                                                                \
        if (grayscale = clrdata->grayscale) {                           \
            InitGrayDither(cvdata, clrdata, dstTW);                     \
        } else {                                                        \
            InitColorDither(cvdata, clrdata, dstTW);                    \
        }                                                               \
    } while (0)

#define StartAnyDitherLine(cvdata, dstX1, dstY)                         \
    do {                                                                \
        if (grayscale) {                                                \
            StartGrayDitherLine(cvdata, dstX1, dstY);                   \
        } else {                                                        \
            StartColorDitherLine(cvdata, dstX1, dstY);                  \
        }                                                               \
    } while (0)

#define AnyDitherPixel(dstX, dstY, pixel, red, green, blue)             \
    do {                                                                \
        if (grayscale) {                                                \
            GrayDitherPixel(dstX, dstY, pixel, red, green, blue);       \
        } else {                                                        \
            ColorDitherPixel(dstX, dstY, pixel, red, green, blue);      \
        }                                                               \
    } while (0)

#define AnyDitherBufComplete(cvdata, dstX1)                             \
    do {                                                                \
        if (grayscale) {                                                \
            GrayDitherBufComplete(cvdata, dstX1);                       \
        } else {                                                        \
            ColorDitherBufComplete(cvdata, dstX1);                      \
        }                                                               \
    } while (0)
