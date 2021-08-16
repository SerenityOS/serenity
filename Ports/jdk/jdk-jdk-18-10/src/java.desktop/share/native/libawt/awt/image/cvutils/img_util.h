/*
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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
 * This file defines some of the standard utility macros and definitions
 * used throughout the image conversion package header files.
 */

#include "img_globals.h"

#define ALPHASHIFT      24
#define REDSHIFT        16
#define GREENSHIFT      8
#define BLUESHIFT       0

/*
 * The following mapping is used between coordinates when scaling an
 * image:
 *
 *      srcXY = floor(((dstXY + .5) * srcWH) / dstWH)
 *            = floor((dstXY * srcWH + .5 * srcWH) / dstWH)
 *            = floor((2 * dstXY * srcWH + srcWH) / (2 * dstWH))
 *
 * Since the numerator can always be assumed to be non-negative for
 * all values of dstXY >= 0 and srcWH,dstWH >= 1, then the floor
 * function can be calculated using the standard C integer division
 * operator.
 *
 * To calculate back from a source range of pixels to the destination
 * range of pixels that they will affect, we need to find a srcXY
 * that satisfies the following inequality based upon the above mapping
 * function:
 *
 *      srcXY <= (2 * dstXY * srcWH + srcWH) / (2 * dstWH) < (srcXY+1)
 *      2 * srcXY * dstWH <= 2 * dstXY * srcWH + srcWH < 2 * (srcXY+1) * dstWH
 *
 * To calculate the lowest dstXY that satisfies these constraints, we use
 * the first half of the inequality:
 *
 *      2 * dstXY * srcWH + srcWH >= 2 * srcXY * dstWH
 *      2 * dstXY * srcWH >= 2 * srcXY * dstWH - srcWH
 *      dstXY >= (2 * srcXY * dstWH - srcWH) / (2 * srcWH)
 *      dstXY = ceil((2 * srcXY * dstWH - srcWH) / (2 * srcWH))
 *      dstXY = floor((2 * srcXY * dstWH - srcWH + 2*srcWH - 1) / (2 * srcWH))
 *      dstXY = floor((2 * srcXY * dstWH + srcWH - 1) / (2 * srcWH))
 *
 * Since the numerator can be shown to be non-negative, we can calculate
 * this with the standard C integer division operator.
 *
 * To calculate the highest dstXY that satisfies these constraints, we use
 * the second half of the inequality:
 *
 *      2 * dstXY * srcWH + srcWH < 2 * (srcXY+1) * dstWH
 *      2 * dstXY * srcWH < 2 * (srcXY+1) * dstWH - srcWH
 *      dstXY < (2 * (srcXY+1) * dstWH - srcWH) / (2 * srcWH)
 *      dstXY = ceil((2 * (srcXY+1) * dstWH - srcWH) / (2 * srcWH)) - 1
 *      dstXY = floor((2 * (srcXY+1) * dstWH - srcWH + 2 * srcWH - 1)
 *                    / (2 * srcWH)) - 1
 *      dstXY = floor((2 * (srcXY+1) * dstWH + srcWH - 1) / (2 * srcWH)) - 1
 *
 * Again, the numerator is always non-negative so we can use integer division.
 */

#define SRC_XY(dstXY, srcWH, dstWH) \
    (((2 * (dstXY) * (srcWH)) + (srcWH)) / (2 * (dstWH)))

#define DEST_XY_RANGE_START(srcXY, srcWH, dstWH) \
    (((2 * (srcXY) * (dstWH)) + (srcWH) - 1) / (2 * (srcWH)))

#define DEST_XY_RANGE_END(srcXY, srcWH, dstWH) \
    (((2 * ((srcXY) + 1) * (dstWH)) + (srcWH) - 1) / (2 * (srcWH)) - 1)

/*
 * This union is a utility structure for manipulating pixel pointers
 * of variable depths.
 */
typedef union {
    void *vp;
    unsigned char *bp;
    unsigned short *sp;
    unsigned int *ip;
} pixptr;

#define RGBTOGRAY(r, g, b) ((int) (.299 * r + .587 * g + .114 * b))

#define ComponentBound(c)                                       \
    (((c) < 0) ? 0 : (((c) > 255) ? 255 : (c)))

#define paddedwidth(number, boundary)                           \
    (((number) + ((boundary) - 1)) & (~((boundary) - 1)))
