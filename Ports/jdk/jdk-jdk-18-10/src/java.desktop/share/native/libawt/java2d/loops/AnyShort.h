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

#include "GraphicsPrimitiveMgr.h"
#include "LoopMacros.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "AnyShort".
 */

typedef jshort  AnyShortDataType;

#define AnyShortPixelStride     2

#define DeclareAnyShortLoadVars(PREFIX)
#define DeclareAnyShortStoreVars(PREFIX)
#define InitAnyShortLoadVars(PREFIX, pRasInfo)
#define InitAnyShortStoreVarsY(PREFIX, pRasInfo)
#define InitAnyShortStoreVarsX(PREFIX, pRasInfo)
#define NextAnyShortStoreVarsX(PREFIX)
#define NextAnyShortStoreVarsY(PREFIX)

#define DeclareAnyShortPixelData(PREFIX)

#define ExtractAnyShortPixelData(PIXEL, PREFIX)

#define StoreAnyShortPixelData(pPix, x, pixel, PREFIX) \
    (pPix)[x] = (jshort) (pixel)

#define CopyAnyShortPixelData(pSrc, sx, pDst, dx) \
    (pDst)[dx] = (pSrc)[sx]

#define XorCopyAnyShortPixelData(pSrc, pDst, x, xorpixel, XORPREFIX) \
    (pDst)[x] ^= (pSrc)[x] ^ (xorpixel)

#define XorAnyShortPixelData(srcpixel, SRCPREFIX, pDst, x, \
                             xorpixel, XORPREFIX, mask, MASKPREFIX) \
    (pDst)[x] ^= (((srcpixel) ^ (xorpixel)) & ~(mask))

DECLARE_ISOCOPY_BLIT(AnyShort);
DECLARE_ISOSCALE_BLIT(AnyShort);
DECLARE_ISOXOR_BLIT(AnyShort);

#define REGISTER_ANYSHORT_ISOCOPY_BLIT(SHORTTYPE) \
    REGISTER_ISOCOPY_BLIT(SHORTTYPE, AnyShort)

#define REGISTER_ANYSHORT_ISOSCALE_BLIT(SHORTTYPE) \
    REGISTER_ISOSCALE_BLIT(SHORTTYPE, AnyShort)

#define REGISTER_ANYSHORT_ISOXOR_BLIT(SHORTTYPE) \
    REGISTER_ISOXOR_BLIT(SHORTTYPE, AnyShort)
