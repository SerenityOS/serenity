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
 * This implementation can encode the color information into 32-bit
 * output pixels directly by using shift amounts to specify which
 * bits of the 32-bit output pixel should contain the red, green,
 * and blue components.
 */

#define DeclareDitherVars                                               \
    int red_dither_shift, green_dither_shift, blue_dither_shift;

#define InitDither(cvdata, clrdata, dstTW)                      \
    do {                                                        \
        red_dither_shift = clrdata->rOff;                       \
        green_dither_shift = clrdata->gOff;                     \
        blue_dither_shift = clrdata->bOff;                      \
    } while (0)

#define StartDitherLine(cvdata, dstX1, dstY)                    \
    do {} while (0)

#define DitherPixel(dstX, dstY, pixel, red, green, blue)        \
    do {                                                        \
        pixel = ((red << red_dither_shift) |                    \
                 (green << green_dither_shift) |                \
                 (blue << blue_dither_shift));                  \
    } while (0)

#define DitherBufComplete(cvdata, dstX1)                        \
    do {} while (0)
