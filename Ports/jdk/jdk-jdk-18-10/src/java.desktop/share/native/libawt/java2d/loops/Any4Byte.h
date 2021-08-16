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
 * LoopMacros.h to manipulate a surface of type "Any4Byte".
 */

typedef jubyte  Any4ByteDataType;

#define Any4BytePixelStride     4

#define DeclareAny4ByteLoadVars(PREFIX)
#define DeclareAny4ByteStoreVars(PREFIX)
#define InitAny4ByteLoadVars(PREFIX, pRasInfo)
#define InitAny4ByteStoreVarsY(PREFIX, pRasInfo)
#define InitAny4ByteStoreVarsX(PREFIX, pRasInfo)
#define NextAny4ByteStoreVarsX(PREFIX)
#define NextAny4ByteStoreVarsY(PREFIX)

#define DeclareAny4BytePixelData(PREFIX) \
    jubyte PREFIX ## 0, PREFIX ## 1, PREFIX ## 2, PREFIX ## 3;

#define ExtractAny4BytePixelData(PIXEL, PREFIX) \
    do { \
        PREFIX ## 0 = (jubyte) (PIXEL); \
        PREFIX ## 1 = (jubyte) (PIXEL >> 8); \
        PREFIX ## 2 = (jubyte) (PIXEL >> 16); \
        PREFIX ## 3 = (jubyte) (PIXEL >> 24); \
    } while (0)

#define StoreAny4BytePixelData(pPix, x, pixel, PREFIX) \
    do { \
        (pPix)[4*x+0] = PREFIX ## 0; \
        (pPix)[4*x+1] = PREFIX ## 1; \
        (pPix)[4*x+2] = PREFIX ## 2; \
        (pPix)[4*x+3] = PREFIX ## 3; \
    } while (0)

#define CopyAny4BytePixelData(pSrc, sx, pDst, dx) \
    do { \
        (pDst)[4*dx+0] = (pSrc)[4*sx+0]; \
        (pDst)[4*dx+1] = (pSrc)[4*sx+1]; \
        (pDst)[4*dx+2] = (pSrc)[4*sx+2]; \
        (pDst)[4*dx+3] = (pSrc)[4*sx+3]; \
    } while (0)

#define XorCopyAny4BytePixelData(pSrc, pDst, x, xorpixel, XORPREFIX) \
    do { \
        (pDst)[4*x+0] ^= (pSrc)[4*x+0] ^ XORPREFIX ## 0; \
        (pDst)[4*x+1] ^= (pSrc)[4*x+1] ^ XORPREFIX ## 1; \
        (pDst)[4*x+2] ^= (pSrc)[4*x+2] ^ XORPREFIX ## 2; \
        (pDst)[4*x+3] ^= (pSrc)[4*x+3] ^ XORPREFIX ## 3; \
    } while (0)

#define XorAny4BytePixelData(srcpixel, SRCPREFIX, pDst, x, \
                             xorpixel, XORPREFIX, mask, MASKPREFIX) \
    do { \
        (pDst)[4*x+0] ^= ((SRCPREFIX ## 0 ^ XORPREFIX ## 0) & \
                          ~MASKPREFIX ## 0); \
        (pDst)[4*x+1] ^= ((SRCPREFIX ## 1 ^ XORPREFIX ## 1) & \
                          ~MASKPREFIX ## 1); \
        (pDst)[4*x+2] ^= ((SRCPREFIX ## 2 ^ XORPREFIX ## 2) & \
                          ~MASKPREFIX ## 2); \
        (pDst)[4*x+3] ^= ((SRCPREFIX ## 3 ^ XORPREFIX ## 3) & \
                          ~MASKPREFIX ## 3); \
    } while (0)

DECLARE_ISOCOPY_BLIT(Any4Byte);
DECLARE_ISOSCALE_BLIT(Any4Byte);
DECLARE_ISOXOR_BLIT(Any4Byte);

#define REGISTER_ANY4BYTE_ISOCOPY_BLIT(FOURBYTETYPE) \
    REGISTER_ISOCOPY_BLIT(FOURBYTETYPE, Any4Byte)

#define REGISTER_ANY4BYTE_ISOSCALE_BLIT(FOURBYTETYPE) \
    REGISTER_ISOSCALE_BLIT(FOURBYTETYPE, Any4Byte)

#define REGISTER_ANY4BYTE_ISOXOR_BLIT(FOURBYTETYPE) \
    REGISTER_ISOXOR_BLIT(FOURBYTETYPE, Any4Byte)
