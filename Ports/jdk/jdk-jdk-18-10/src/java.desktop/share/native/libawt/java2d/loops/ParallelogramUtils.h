/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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

#ifndef ParallelogramUtils_h_Included
#define ParallelogramUtils_h_Included

#ifdef __cplusplus
extern "C" {
#endif

#define PGRAM_MIN_MAX(bmin, bmax, v0, dv1, dv2, AA) \
    do { \
        double vmin, vmax; \
        if (dv1 < 0) { \
            vmin = v0+dv1; \
            vmax = v0; \
        } else { \
            vmin = v0; \
            vmax = v0+dv1; \
        } \
        if (dv2 < 0) { \
            vmin += dv2; \
        } else { \
            vmax += dv2; \
        } \
        if (AA) { \
            bmin = (jint) floor(vmin); \
            bmax = (jint) ceil(vmax); \
        } else { \
            bmin = (jint) floor(vmin + 0.5); \
            bmax = (jint) floor(vmax + 0.5); \
        } \
    } while(0)

#define PGRAM_INIT_X(starty, x, y, slope) \
    (DblToLong((x) + (slope) * ((starty)+0.5 - (y))) + LongOneHalf - 1)

/*
 * Sort parallelogram by y values, ensure that each delta vector
 * has a non-negative y delta.
 */
#define SORT_PGRAM(x0, y0, dx1, dy1, dx2, dy2, OTHER_SWAP_CODE) \
    do { \
        if (dy1 < 0) { \
            x0 += dx1;  y0 += dy1; \
            dx1 = -dx1; dy1 = -dy1; \
        } \
        if (dy2 < 0) { \
            x0 += dx2;  y0 += dy2; \
            dx2 = -dx2; dy2 = -dy2; \
        } \
        /* Sort delta vectors so dxy1 is left of dxy2. */ \
        if (dx1 * dy2 > dx2 * dy1) { \
            double v; \
            v = dx1; dx1 = dx2; dx2 = v; \
            v = dy1; dy1 = dy2; dy2 = v; \
            OTHER_SWAP_CODE \
        } \
    } while(0)

#endif /* ParallelogramUtils_h_Included */
