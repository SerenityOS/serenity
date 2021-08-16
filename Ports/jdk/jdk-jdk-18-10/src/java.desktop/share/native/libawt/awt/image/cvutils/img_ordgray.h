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
 * This implementation uses an ordered dithering error matrix to
 * produce a moderately high quality version of an image with only
 * an 8-bit (or less) grayramp.  The ordered dithering technique does
 * not rely on the order in which the pixels are processed so this
 * file can be used in cases where the ImageProducer has not specified
 * the TopDownLeftRight delivery hint.  The ordered dither technique
 * is also much faster than the Floyd-Steinberg error diffusion
 * algorithm so this implementation would also be appropriate for
 * cases where performance is critical such as the processing of a
 * video stream.
 *
 * This file can be used to provide the default implementation of the
 * Encoding macros for grayscale displays.
 */

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
#define DeclareDitherVars       DeclareAllGrayDitherVars
#define InitDither              InitGrayDither
#define StartDitherLine         StartGrayDitherLine
#define DitherPixel             GrayDitherPixel
#define DitherBufComplete       GrayDitherBufComplete
#endif

#define DeclareAllGrayDitherVars                                \
    DeclareGrayDitherVars                                       \
    int relx, rely;

#define DeclareGrayDitherVars                                   \
    extern unsigned char img_grays[256];                        \
    extern unsigned char img_bwgamma[256];                      \
    extern sgn_ordered_dither_array img_oda_gray;

#define InitGrayDither(cvdata, clrdata, dstTW)                          \
    do {} while (0)

#define StartGrayDitherLine(cvdata, dstX1, dstY)                        \
    do {                                                                \
        relx = dstX1 & 7;                                               \
        rely = dstY & 7;                                                \
    } while (0)

#define GrayDitherPixel(dstX, dstY, pixel, red, green, blue)            \
    do {                                                                \
        green = RGBTOGRAY(red, green, blue);                            \
        green += img_oda_gray[relx][rely];                              \
        green = ComponentBound(green);                                  \
        pixel = img_grays[img_bwgamma[green]];                          \
        relx = (relx + 1) & 7;                                          \
    } while (0)

#define GrayDitherBufComplete(cvdata, dstX1)                            \
    do {} while (0)
