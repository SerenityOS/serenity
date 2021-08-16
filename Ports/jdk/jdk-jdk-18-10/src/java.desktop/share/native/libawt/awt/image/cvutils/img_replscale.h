/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This file contains macro definitions for the Scaling category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation uses a simple equation which simply chooses
 * the closest input pixel to the location which is obtained from
 * mapping inversely from the output rectangle to the input rectangle.
 * The input pixels will be replicated when scaling larger than the
 * original image size since the same input pixel will be chosen for
 * more than one output pixel.  Conversely, when scaling smaller than
 * the original image size, the input pixels will be omitted as needed
 * to pare them down to the required number of samples for the output
 * image.  If there is no scaling occurring in one or both directions
 * the macros attempt to short-circuit most of the more complicated
 * calculations in an attempt to impose little cost for using this
 * implementation in the general case.  The calculations also do not
 * impose any restrictions on the order of delivery of the pixels.
 *
 * This file can be used to provide the default implementation of the
 * Scaling macros, handling both scaled and unscaled cases and any
 * order of pixel delivery.
 */

#define DeclareScaleVars                                        \
    int dstX1, dstY1, dstX, dstY, dstX2, dstY2;                 \
    int srcX1, srcXinc, srcXrem, srcXincrem, srcX1increm;       \
    int srcX, srcY, inputadjust;

#define SRCX    srcX
#define SRCY    srcY
#define DSTX    dstX
#define DSTY    dstY
#define DSTX1   dstX1
#define DSTY1   dstY1
#define DSTX2   dstX2
#define DSTY2   dstY2

#define InitScale(pixels, srcOff, srcScan,                              \
                  srcOX, srcOY, srcW, srcH,                             \
                  srcTW, srcTH, dstTW, dstTH)                           \
    do {                                                                \
        inputadjust = srcScan;                                          \
        if (srcTW == dstTW) {                                           \
            inputadjust -= srcW;                                        \
            dstX1 = srcOX;                                              \
            dstX2 = srcOX + srcW;                                       \
        } else {                                                        \
            dstX1 = DEST_XY_RANGE_START(srcOX, srcTW, dstTW);           \
            dstX2 = DEST_XY_RANGE_START(srcOX+srcW, srcTW, dstTW);      \
            if (dstX2 <= dstX1) {                                       \
                return SCALENOOP;                                       \
            }                                                           \
            srcX1 = SRC_XY(dstX1, srcTW, dstTW);                        \
            srcXinc = srcTW / dstTW;                                    \
            srcXrem = (2 * srcTW) % (2 * dstTW);                        \
            srcX1increm = (((2 * (dstX1) * (srcTW)) + (srcTW))          \
                          % (2 * (dstTW)));                             \
        }                                                               \
        if (srcTH == dstTH) {                                           \
            dstY1 = srcOY;                                              \
            dstY2 = srcOY + srcH;                                       \
            SetInputRow(pixels, srcOff, srcScan, srcOY, srcOY);         \
        } else {                                                        \
            dstY1 = DEST_XY_RANGE_START(srcOY, srcTH, dstTH);           \
            dstY2 = DEST_XY_RANGE_START(srcOY+srcH, srcTH, dstTH);      \
            if (dstY2 <= dstY1) {                                       \
                return SCALENOOP;                                       \
            }                                                           \
        }                                                               \
    } while (0)

#define RowLoop(srcOY)                                                  \
    for (dstY = dstY1; dstY < dstY2; dstY++)

#define RowSetup(srcTH, dstTH, srcTW, dstTW,                            \
                 srcOY, pixels, srcOff, srcScan)                        \
        do {                                                            \
            if (srcTH == dstTH) {                                       \
                srcY = dstY;                                            \
            } else {                                                    \
                srcY = SRC_XY(dstY, srcTH, dstTH);                      \
                SetInputRow(pixels, srcOff, srcScan, srcY, srcOY);      \
            }                                                           \
            if (srcTW != dstTW) {                                       \
                srcXincrem = srcX1increm;                               \
                srcX = srcX1;                                           \
            }                                                           \
        } while (0)

#define ColLoop(srcOX)                                                  \
        for (dstX = dstX1; dstX < dstX2; dstX++)

#define ColSetup(srcTW, dstTW, pixel)                                   \
            do {                                                        \
                if (srcTW == dstTW) {                                   \
                    srcX = dstX;                                        \
                    pixel = GetPixelInc();                              \
                } else {                                                \
                    pixel = GetPixel(srcX);                             \
                    srcX += srcXinc;                                    \
                    srcXincrem += srcXrem;                              \
                    if (srcXincrem >= (2 * dstTW)) {                    \
                        srcXincrem -= (2 * dstTW);                      \
                        srcX++;                                         \
                    }                                                   \
                }                                                       \
            } while (0)

#define RowEnd(srcTH, dstTH, srcW, srcScan)                             \
        do {                                                            \
            if (srcTH == dstTH) {                                       \
                InputPixelInc(inputadjust);                             \
            }                                                           \
        } while (0)
