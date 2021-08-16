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
 * an 8-bit (or less) RGB colormap.  The ordered dithering technique
 * does not rely on the order in which the pixels are processed so
 * this file can be used in cases where the ImageProducer has not
 * specified the TopDownLeftRight delivery hint.  The ordered dither
 * technique is also much faster than the Floyd-Steinberg error diffusion
 * algorithm so this implementation would also be appropriate for
 * cases where performance is critical such as the processing of a
 * video stream.
 *
 * This file can be used to provide the default implementation of the
 * Encoding macros for RGB colormapped displays.
 */

/*
 * These definitions vector the standard macro names to the "Color"
 * versions of those macros only if the "DitherDeclared" keyword has
 * not yet been defined elsewhere.  The "DitherDeclared" keyword is
 * also defined here to claim ownership of the primary implementation
 * even though this file does not rely on the definitions in any other
 * files.
 */
#ifndef DitherDeclared
#define DitherDeclared
#define DeclareDitherVars       DeclareAllColorDitherVars
#define InitDither              InitColorDither
#define StartDitherLine         StartColorDitherLine
#define DitherPixel             ColorDitherPixel
#define DitherBufComplete       ColorDitherBufComplete
#endif

#define DeclareAllColorDitherVars                       \
    DeclareColorDitherVars                              \
    int relx, rely;

#define DeclareColorDitherVars                          \
    extern uns_ordered_dither_array img_oda_red;        \
    extern uns_ordered_dither_array img_oda_green;      \
    extern uns_ordered_dither_array img_oda_blue;

#define InitColorDither(cvdata, clrdata, dstTW)                 \
    do {} while (0)

#define StartColorDitherLine(cvdata, dstX1, dstY)               \
    do {                                                        \
        relx = dstX1 & 7;                                       \
        rely = dstY & 7;                                        \
    } while (0)

/*
 * The adjustments below are gross, but they are required due to
 * the way color lookups are done.
 * The second set of adjustments simply clips the values generated
 * by the ordered dithering values to a limit of 256 which represents
 * full intensity.
 * The first set of adjustments prepares for the fact that when
 * the final lookup is done, maximum intensity is represented by
 * the value 256, but the input values go from 0 to 255.  As a
 * result, the maximum input intensity needs to be mapped from
 * 255 to 256.  The Floyd-Steinberg lookups use a rounding
 * calculation to handle mapping the values near 255 to the maximum
 * intensity, but ordered dithering uses a truncating calculation
 * so the value 255 will be rounded down to the second highest
 * intensity thereby causing an occasionaly dark pixel when rendering
 * the maximum input intensity.  Other intensities (less than 255)
 * are left alone since modifying them would slightly disturb their
 * error distribution.  In particular, for red, the value 0xe0 has
 * a maximum error of 0x1f added to it which must not be mapped to
 * the maximum intensity since intensity 0xe0 can be represented
 * exactly.  So, a calculated 0xff (0xe0 + 0x1f) needs to be left
 * less than 256, but a natural 255, or a calculated (>=) 256
 * should be mapped to maximum intensity.
 */
#define ColorDitherPixel(dstX, dstY, pixel, red, green, blue)   \
    do {                                                        \
        if (red == 255) {                                       \
            red = 256;                                          \
        } else {                                                \
            red += img_oda_red[relx][rely];                     \
            if (red > 255) red = 256;                           \
        }                                                       \
        if (green == 255) {                                     \
            green = 256;                                        \
        } else {                                                \
            green += img_oda_green[relx][rely];                 \
            if (green > 255) green = 256;                       \
        }                                                       \
        if (blue == 255) {                                      \
            blue = 256;                                         \
        } else {                                                \
            blue += img_oda_blue[relx][rely];                   \
            if (blue > 255) blue = 256;                         \
        }                                                       \
        pixel = ColorCubeOrdMapUns(red, green, blue);           \
        relx = (relx + 1) & 7;                                  \
    } while (0)

#define ColorDitherBufComplete(cvdata, dstX1)                   \
    do {} while (0)
