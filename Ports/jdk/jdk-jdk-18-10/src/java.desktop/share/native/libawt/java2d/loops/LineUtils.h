/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

#ifndef LineUtils_h_Included
#define LineUtils_h_Included

#define SIGNED(d, v)    (((d) < 0) ? (-((jint) (v))) : ((jint) (v)))
#define SWAP(a, b, t)   do { jint t = a; a = b; b = t; } while (0)
#define SETORDERED(a,b,min,max, shorten) \
    do { \
        if (a < b) { \
            min = a; \
            max = b - shorten; \
        } else { \
            min = b + shorten; \
            max = a; \
        } \
    } while (0)

#define BUMP_NOOP         0x0
#define BUMP_POS_PIXEL    0x1
#define BUMP_NEG_PIXEL    0x2
#define BUMP_POS_SCAN     0x4
#define BUMP_NEG_SCAN     0x8

extern jboolean LineUtils_SetupBresenham(jint x1, jint y1, jint x2, jint y2,
                                         jint shorten,
                                         SurfaceDataBounds *pBounds,
                                         jint *pStartX, jint *pStartY,
                                         jint *pSteps, jint *pError,
                                         jint *pErrMajor, jint *pBumpMajorMask,
                                         jint *pErrMinor, jint *pBumpMinorMask);

#define LineUtils_ProcessLine(pRasInfo, pixel, pLine, pPrim, pCompInfo, \
                              X1, Y1, X2, Y2, shorten) \
    do { \
        jint tx1, ty1, tx2, ty2; \
        if (Y1 == Y2) { \
            if (Y1 >= (pRasInfo)->bounds.y1 && Y1 < (pRasInfo)->bounds.y2) { \
                SETORDERED(X1, X2, tx1, tx2, shorten); \
                if (++tx2 < tx1) --tx2; /* integer overflow */ \
                if (tx1 < (pRasInfo)->bounds.x1) tx1 = (pRasInfo)->bounds.x1; \
                if (tx2 > (pRasInfo)->bounds.x2) tx2 = (pRasInfo)->bounds.x2; \
                if (tx1 < tx2) { \
                    (*pLine)((pRasInfo), tx1, Y1, pixel, tx2 - tx1, 0, \
                             BUMP_POS_PIXEL, 0, \
                             BUMP_NOOP, 0, pPrim, pCompInfo); \
                } \
            } \
        } else if (X1 == X2) { \
            if (X1 >= (pRasInfo)->bounds.x1 && X1 < (pRasInfo)->bounds.x2) { \
                SETORDERED(Y1, Y2, ty1, ty2, shorten); \
                if (++ty2 < ty1) --ty2; /* integer overflow */ \
                if (ty1 < (pRasInfo)->bounds.y1) ty1 = (pRasInfo)->bounds.y1; \
                if (ty2 > (pRasInfo)->bounds.y2) ty2 = (pRasInfo)->bounds.y2; \
                if (ty1 < ty2) { \
                    (*pLine)((pRasInfo), X1, ty1, pixel, ty2 - ty1, 0, \
                             BUMP_POS_SCAN, 0, \
                             BUMP_NOOP, 0, pPrim, pCompInfo); \
                } \
            } \
        } else { \
            jint steps; \
            jint error; \
            jint errmajor, errminor; \
            jint bumpmajormask, bumpminormask; \
            if (LineUtils_SetupBresenham(X1, Y1, X2, Y2, shorten, \
                                         &(pRasInfo)->bounds, \
                                         &tx1, &ty1, \
                                         &steps, &error, \
                                         &errmajor, &bumpmajormask, \
                                         &errminor, &bumpminormask)) \
            { \
                (*pLine)((pRasInfo), tx1, ty1, pixel, steps, error, \
                         bumpmajormask, errmajor, bumpminormask, errminor, \
                         pPrim, pCompInfo); \
            } \
        } \
    } while (0)

#endif /* LineUtils_h_Included */
