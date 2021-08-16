/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AnyByteBinary_h_Included
#define AnyByteBinary_h_Included

#include <string.h>

#include "AlphaMacros.h"
#include "GraphicsPrimitiveMgr.h"
#include "LoopMacros.h"
#include "LineUtils.h"

/*
 * This file contains macros that are similar to those found in LoopMacros.h
 * and AlphaMacros.h, yet have been specialized to manipulate any one of the
 * surfaces in the "ByteBinary" family.  It also contains generalized versions
 * of some macros that are used by the more specific ByteBinary surfaces.
 */

/* REMIND: the ByteBinary store macros should probably do ordered dithering */
#define DeclareByteBinaryLoadVars(PREFIX) \
    jint *PREFIX ## Lut;

#define DeclareByteBinaryStoreVars(PREFIX) \
    unsigned char *PREFIX ## InvLut;

#define SetByteBinaryStoreVarsYPos(PREFIX, pRasInfo, LOC)
#define SetByteBinaryStoreVarsXPos(PREFIX, pRasInfo, LOC)

#define InitByteBinaryLoadVars(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

#define InitByteBinaryStoreVarsY(PREFIX, pRasInfo) \
    PREFIX ## InvLut = (pRasInfo)->invColorTable

#define InitByteBinaryStoreVarsX(PREFIX, pRasInfo)
#define NextByteBinaryStoreVarsX(PREFIX)
#define NextByteBinaryStoreVarsY(PREFIX)


#define DeclareByteBinaryInitialLoadVars(TYPE, INFO, pRas, PREFIX, x) \
    int PREFIX ## adjx = (x) + (INFO)->pixelBitOffset / TYPE ## BitsPerPixel; \
    int PREFIX ## index = (PREFIX ## adjx) / TYPE ## PixelsPerByte; \
    int PREFIX ## bits = TYPE ## MaxBitOffset - \
                             (((PREFIX ## adjx) % TYPE ## PixelsPerByte) * \
                              TYPE ## BitsPerPixel); \
    int PREFIX ## bbpix = (pRas)[PREFIX ## index];

#define InitialLoadByteBinary(TYPE, pRas, PREFIX) \
    do { \
        if (PREFIX ## bits < 0) { \
            (pRas)[PREFIX ## index] = (jubyte) PREFIX ## bbpix; \
            PREFIX ## bbpix = (pRas)[++(PREFIX ## index)]; \
            PREFIX ## bits = TYPE ## MaxBitOffset; \
        } \
    } while (0)

#define ShiftBitsByteBinary(TYPE, PREFIX) \
    PREFIX ## bits -= TYPE ## BitsPerPixel

#define FinalStoreByteBinary(TYPE, pRas, PREFIX) \
    (pRas)[PREFIX ## index] = (jubyte) PREFIX ## bbpix

#define CurrentPixelByteBinary(TYPE, PREFIX) \
    ((PREFIX ## bbpix >> PREFIX ## bits) & TYPE ## PixelMask)


#define StoreByteBinaryPixel(TYPE, pRas, x, pixel)

#define StoreByteBinaryPixelData(TYPE, pPix, x, pixel, PREFIX) \
    do { \
        PREFIX ## bbpix &= ~(TYPE ## PixelMask << PREFIX ## bits); \
        PREFIX ## bbpix |= (pixel << PREFIX ## bits); \
    } while (0)

#define ByteBinaryPixelFromArgb(TYPE, pixel, rgb, pRasInfo) \
    do { \
        jint r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        (pixel) = SurfaceData_InvColorMap((pRasInfo)->invColorTable, \
                                          r, g, b); \
    } while (0)

#define XorByteBinaryPixelData(TYPE, pDst, x, PREFIX, \
                               srcpixel, xorpixel, mask) \
    PREFIX ## bbpix ^= ((((srcpixel) ^ (xorpixel)) & TYPE ## PixelMask) \
                           << PREFIX ## bits)


#define LoadByteBinaryTo1IntRgb(TYPE, pRas, PREFIX, x, rgb) \
    (rgb) = PREFIX ## Lut[CurrentPixelByteBinary(TYPE, PREFIX)]

#define LoadByteBinaryTo1IntArgb(TYPE, pRas, PREFIX, x, argb) \
    (argb) = PREFIX ## Lut[CurrentPixelByteBinary(TYPE, PREFIX)]

#define LoadByteBinaryTo3ByteRgb(TYPE, pRas, PREFIX, x, r, g, b) \
    do { \
        jint rgb = PREFIX ## Lut[CurrentPixelByteBinary(TYPE, PREFIX)]; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
    } while (0)

#define LoadByteBinaryTo4ByteArgb(TYPE, pRas, PREFIX, x, a, r, g, b) \
    do { \
        jint argb = PREFIX ## Lut[CurrentPixelByteBinary(TYPE, PREFIX)]; \
        ExtractIntDcmComponents1234(argb, a, r, g, b); \
    } while (0)

#define StoreByteBinaryFrom1IntRgb(TYPE, pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreByteBinaryFrom3ByteRgb(TYPE, pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreByteBinaryFrom1IntArgb(TYPE, pRas, PREFIX, x, argb) \
    StoreByteBinaryFrom1IntRgb(TYPE, pRas, PREFIX, x, argb)

#define StoreByteBinaryFrom3ByteRgb(TYPE, pRas, PREFIX, x, r, g, b) \
    StoreByteBinaryPixelData(TYPE, pRas, x, \
                             SurfaceData_InvColorMap(PREFIX ## InvLut, \
                                                     r, g, b), \
                             PREFIX)

#define StoreByteBinaryFrom4ByteArgb(TYPE, pRas, PREFIX, x, a, r, g, b) \
    StoreByteBinaryFrom3ByteRgb(TYPE, pRas, PREFIX, x, r, g, b)


#define DeclareByteBinaryAlphaLoadData(TYPE, PREFIX) \
    jint *PREFIX ## Lut; \
    jint PREFIX ## rgb;

#define InitByteBinaryAlphaLoadData(TYPE, PREFIX, pRasInfo) \
    do { \
        PREFIX ## Lut = (pRasInfo)->lutBase; \
        PREFIX ## rgb = 0; \
    } while (0)

#define LoadAlphaFromByteBinaryFor4ByteArgb(TYPE, pRas, PREFIX, COMP_PREFIX) \
    do { \
        PREFIX ## rgb = PREFIX ## Lut[CurrentPixelByteBinary(TYPE, PREFIX)]; \
        COMP_PREFIX ## A = ((juint) PREFIX ## rgb) >> 24; \
    } while (0)

#define Postload4ByteArgbFromByteBinary(TYPE, pRas, PREFIX, COMP_PREFIX) \
    do { \
        COMP_PREFIX ## R = (PREFIX ## rgb >> 16) & 0xff; \
        COMP_PREFIX ## G = (PREFIX ## rgb >>  8) & 0xff; \
        COMP_PREFIX ## B = (PREFIX ## rgb >>  0) & 0xff; \
    } while (0)


#define ByteBinaryIsPremultiplied       0

#define StoreByteBinaryFrom4ByteArgbComps(TYPE, pRas, PREFIX, x, COMP_PREFIX)\
    StoreByteBinaryFrom4ByteArgb(TYPE, pRas, PREFIX, x, \
                                 COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                 COMP_PREFIX ## G, COMP_PREFIX ## B)




#define BBBlitLoopWidthHeight(SRCTYPE, SRCPTR, SRCBASE, SRCINFO, SRCPREFIX, \
                              DSTTYPE, DSTPTR, DSTBASE, DSTINFO, DSTPREFIX, \
                              WIDTH, HEIGHT, BODY) \
    do { \
        SRCTYPE ## DataType *SRCPTR = (SRCTYPE ## DataType *) (SRCBASE); \
        DSTTYPE ## DataType *DSTPTR = (DSTTYPE ## DataType *) (DSTBASE); \
        jint srcScan = (SRCINFO)->scanStride; \
        jint dstScan = (DSTINFO)->scanStride; \
        jint srcx1 = (SRCINFO)->bounds.x1; \
        jint dstx1 = (DSTINFO)->bounds.x1; \
        Init ## DSTTYPE ## StoreVarsY(DSTPREFIX, DSTINFO); \
        srcScan -= (WIDTH) * SRCTYPE ## PixelStride; \
        dstScan -= (WIDTH) * DSTTYPE ## PixelStride; \
        do { \
            Declare ## SRCTYPE ## InitialLoadVars(SRCINFO, SRCPTR, SRCPREFIX, \
                                                  srcx1) \
            Declare ## DSTTYPE ## InitialLoadVars(DSTINFO, DSTPTR, DSTPREFIX, \
                                                  dstx1) \
            juint w = WIDTH; \
            Init ## DSTTYPE ## StoreVarsX(DSTPREFIX, DSTINFO); \
            do { \
                InitialLoad ## SRCTYPE(SRCPTR, SRCPREFIX); \
                InitialLoad ## DSTTYPE(DSTPTR, DSTPREFIX); \
                BODY; \
                ShiftBits ## SRCTYPE(SRCPREFIX); \
                ShiftBits ## DSTTYPE(DSTPREFIX); \
                SRCPTR = PtrAddBytes(SRCPTR, SRCTYPE ## PixelStride); \
                DSTPTR = PtrAddBytes(DSTPTR, DSTTYPE ## PixelStride); \
                Next ## DSTTYPE ## StoreVarsX(DSTPREFIX); \
            } while (--w > 0); \
            FinalStore ## DSTTYPE(DSTPTR, DSTPREFIX); \
            SRCPTR = PtrAddBytes(SRCPTR, srcScan); \
            DSTPTR = PtrAddBytes(DSTPTR, dstScan); \
            Next ## DSTTYPE ## StoreVarsY(DSTPREFIX); \
        } while (--HEIGHT > 0); \
    } while (0)

#define BBXorVia1IntArgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                         DSTPTR, DSTTYPE, DSTPREFIX, \
                         XVAR, XORPIXEL, MASK, DSTINFOPTR) \
    do { \
        jint srcpixel; \
        Load ## SRCTYPE ## To1IntArgb(SRCPTR, SRCPREFIX, XVAR, srcpixel); \
 \
        if (IsArgbTransparent(srcpixel)) { \
            break; \
        } \
 \
        DSTTYPE ## PixelFromArgb(srcpixel, srcpixel, DSTINFOPTR); \
 \
        Xor ## DSTTYPE ## PixelData(DSTPTR, XVAR, DSTPREFIX, srcpixel, \
                                    XORPIXEL, MASK); \
    } while (0)

#define DEFINE_BYTE_BINARY_CONVERT_BLIT(SRC, DST, STRATEGY) \
void NAME_CONVERT_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                                 juint width, juint height, \
                                 SurfaceDataRasInfo *pSrcInfo, \
                                 SurfaceDataRasInfo *pDstInfo, \
                                 NativePrimitive *pPrim, \
                                 CompositeInfo *pCompInfo) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    BBBlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, SrcRead, \
                          DST, pDst, dstBase, pDstInfo, DstWrite, \
                          width, height, \
                          ConvertVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                                 pDst, DST, DstWrite, \
                                                 0, 0)); \
}

#define DEFINE_BYTE_BINARY_XOR_BLIT(SRC, DST) \
void NAME_XOR_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                             juint width, juint height, \
                             SurfaceDataRasInfo *pSrcInfo, \
                             SurfaceDataRasInfo *pDstInfo, \
                             NativePrimitive *pPrim, \
                             CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    BBBlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, SrcRead, \
                          DST, pDst, dstBase, pDstInfo, DstWrite, \
                          width, height, \
                          BBXorVia1IntArgb(pSrc, SRC, SrcRead, \
                                           pDst, DST, DstWrite, \
                                           0, xorpixel, \
                                           alphamask, pDstInfo)); \
}

#define DEFINE_BYTE_BINARY_SOLID_FILLRECT(DST) \
void NAME_SOLID_FILLRECT(DST)(SurfaceDataRasInfo *pRasInfo, \
                              jint lox, jint loy, \
                              jint hix, jint hiy, \
                              jint pixel, \
                              NativePrimitive *pPrim, \
                              CompositeInfo *pCompInfo) \
{ \
    DST ## DataType *pPix; \
    jint scan = pRasInfo->scanStride; \
    juint height = hiy - loy; \
    juint width = hix - lox; \
 \
    pPix = PtrCoord(pRasInfo->rasBase, lox, DST ## PixelStride, loy, scan); \
    do { \
        Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, lox) \
        jint w = width; \
        do { \
            InitialLoad ## DST(pPix, DstPix); \
            Store ## DST ## PixelData(pPix, 0, pixel, DstPix); \
            ShiftBits ## DST(DstPix); \
        } while (--w > 0); \
        FinalStore ## DST(pPix, DstPix); \
        pPix = PtrAddBytes(pPix, scan); \
    } while (--height > 0); \
}

#define DEFINE_BYTE_BINARY_SOLID_FILLSPANS(DST) \
void NAME_SOLID_FILLSPANS(DST)(SurfaceDataRasInfo *pRasInfo, \
                               SpanIteratorFuncs *pSpanFuncs, void *siData, \
                               jint pixel, NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    void *pBase = pRasInfo->rasBase; \
    jint scan = pRasInfo->scanStride; \
    jint bbox[4]; \
 \
    while ((*pSpanFuncs->nextSpan)(siData, bbox)) { \
        jint x = bbox[0]; \
        jint y = bbox[1]; \
        juint w = bbox[2] - x; \
        juint h = bbox[3] - y; \
        DST ## DataType *pPix = PtrCoord(pBase, \
                                         x, DST ## PixelStride, \
                                         y, scan); \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x) \
            jint relx = w; \
            do { \
                InitialLoad ## DST(pPix, DstPix); \
                Store ## DST ## PixelData(pPix, 0, pixel, DstPix); \
                ShiftBits ## DST(DstPix); \
            } while (--relx > 0); \
            FinalStore ## DST(pPix, DstPix); \
            pPix = PtrAddBytes(pPix, scan); \
        } while (--h > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_SOLID_DRAWLINE(DST) \
void NAME_SOLID_DRAWLINE(DST)(SurfaceDataRasInfo *pRasInfo, \
                              jint x1, jint y1, jint pixel, \
                              jint steps, jint error, \
                              jint bumpmajormask, jint errmajor, \
                              jint bumpminormask, jint errminor, \
                              NativePrimitive *pPrim, \
                              CompositeInfo *pCompInfo) \
{ \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix = PtrCoord(pRasInfo->rasBase, \
                                     x1, DST ## PixelStride, \
                                     y1, scan); \
    DeclareBumps(bumpmajor, bumpminor) \
 \
    scan *= DST ## PixelsPerByte; \
    InitBumps(bumpmajor, bumpminor, bumpmajormask, bumpminormask, 1, scan); \
    if (errmajor == 0) { \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x1) \
            Store ## DST ## PixelData(pPix, 0, pixel, DstPix); \
            FinalStore ## DST(pPix, DstPix); \
            x1 += bumpmajor; \
        } while (--steps > 0); \
    } else { \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x1) \
            Store ## DST ## PixelData(pPix, 0, pixel, DstPix); \
            FinalStore ## DST(pPix, DstPix); \
            if (error < 0) { \
                x1 += bumpmajor; \
                error += errmajor; \
            } else { \
                x1 += bumpminor; \
                error -= errminor; \
            } \
        } while (--steps > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_XOR_FILLRECT(DST) \
void NAME_XOR_FILLRECT(DST)(SurfaceDataRasInfo *pRasInfo, \
                            jint lox, jint loy, \
                            jint hix, jint hiy, \
                            jint pixel, \
                            NativePrimitive *pPrim, \
                            CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    DST ## DataType *pPix; \
    jint scan = pRasInfo->scanStride; \
    juint height = hiy - loy; \
    juint width = hix - lox; \
 \
    pPix = PtrCoord(pRasInfo->rasBase, lox, DST ## PixelStride, loy, scan); \
    do { \
        Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, lox) \
        jint w = width; \
        do { \
            InitialLoad ## DST(pPix, DstPix); \
            Xor ## DST ## PixelData(pPix, 0, DstPix, \
                                    pixel, xorpixel, alphamask); \
            ShiftBits ## DST(DstPix); \
        } while (--w > 0); \
        FinalStore ## DST(pPix, DstPix); \
        pPix = PtrAddBytes(pPix, scan); \
    } while (--height > 0); \
}

#define DEFINE_BYTE_BINARY_XOR_FILLSPANS(DST) \
void NAME_XOR_FILLSPANS(DST)(SurfaceDataRasInfo *pRasInfo, \
                             SpanIteratorFuncs *pSpanFuncs, \
                             void *siData, jint pixel, \
                             NativePrimitive *pPrim, \
                             CompositeInfo *pCompInfo) \
{ \
    void *pBase = pRasInfo->rasBase; \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    jint scan = pRasInfo->scanStride; \
    jint bbox[4]; \
 \
    while ((*pSpanFuncs->nextSpan)(siData, bbox)) { \
        jint x = bbox[0]; \
        jint y = bbox[1]; \
        juint w = bbox[2] - x; \
        juint h = bbox[3] - y; \
        DST ## DataType *pPix = PtrCoord(pBase, \
                                         x, DST ## PixelStride, \
                                         y, scan); \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x) \
            jint relx = w; \
            do { \
                InitialLoad ## DST(pPix, DstPix); \
                Xor ## DST ## PixelData(pPix, 0, DstPix, \
                                        pixel, xorpixel, alphamask); \
                ShiftBits ## DST(DstPix); \
            } while (--relx > 0); \
            FinalStore ## DST(pPix, DstPix); \
            pPix = PtrAddBytes(pPix, scan); \
        } while (--h > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_XOR_DRAWLINE(DST) \
void NAME_XOR_DRAWLINE(DST)(SurfaceDataRasInfo *pRasInfo, \
                            jint x1, jint y1, jint pixel, \
                            jint steps, jint error, \
                            jint bumpmajormask, jint errmajor, \
                            jint bumpminormask, jint errminor, \
                            NativePrimitive *pPrim, \
                            CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix = PtrCoord(pRasInfo->rasBase, \
                                     x1, DST ## PixelStride, \
                                     y1, scan); \
    DeclareBumps(bumpmajor, bumpminor) \
 \
    scan *= DST ## PixelsPerByte; \
    InitBumps(bumpmajor, bumpminor, bumpmajormask, bumpminormask, 1, scan); \
 \
    if (errmajor == 0) { \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x1) \
            Xor ## DST ## PixelData(pPix, 0, DstPix, \
                                    pixel, xorpixel, alphamask); \
            FinalStore ## DST(pPix, DstPix); \
            x1 += bumpmajor; \
        } while (--steps > 0); \
    } else { \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, x1) \
            Xor ## DST ## PixelData(pPix, 0, DstPix, \
                                    pixel, xorpixel, alphamask); \
            FinalStore ## DST(pPix, DstPix); \
            if (error < 0) { \
                x1 += bumpmajor; \
                error += errmajor; \
            } else { \
                x1 += bumpminor; \
                error -= errminor; \
            } \
        } while (--steps > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_SOLID_DRAWGLYPHLIST(DST) \
void NAME_SOLID_DRAWGLYPHLIST(DST)(SurfaceDataRasInfo *pRasInfo, \
                                   ImageRef *glyphs, \
                                   jint totalGlyphs, jint fgpixel, \
                                   jint argbcolor, \
                                   jint clipLeft, jint clipTop, \
                                   jint clipRight, jint clipBottom, \
                                   NativePrimitive *pPrim, \
                                   CompositeInfo *pCompInfo) \
{ \
    jint glyphCounter; \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix; \
\
    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) { \
        DeclareDrawGlyphListClipVars(pixels, rowBytes, width, height, \
                                     left, top, right, bottom) \
        ClipDrawGlyphList(DST, pixels, 1, rowBytes, width, height, \
                          left, top, right, bottom, \
                          clipLeft, clipTop, clipRight, clipBottom, \
                          glyphs, glyphCounter, continue) \
        pPix = PtrCoord(pRasInfo->rasBase,left,DST ## PixelStride,top,scan); \
\
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, left) \
            jint x = 0; \
            do { \
                InitialLoad ## DST(pPix, DstPix); \
                if (pixels[x]) { \
                    Store ## DST ## PixelData(pPix, 0, fgpixel, DstPix); \
                } \
                ShiftBits ## DST(DstPix); \
            } while (++x < width); \
            FinalStore ## DST(pPix, DstPix); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
        } while (--height > 0); \
    } \
}

/*
 * REMIND: we shouldn't be attempting to do antialiased text for the
 *         ByteBinary surfaces in the first place
 */
#define DEFINE_BYTE_BINARY_SOLID_DRAWGLYPHLISTAA(DST, STRATEGY) \
void NAME_SOLID_DRAWGLYPHLISTAA(DST)(SurfaceDataRasInfo *pRasInfo, \
                                     ImageRef *glyphs, \
                                     jint totalGlyphs, jint fgpixel, \
                                     jint argbcolor, \
                                     jint clipLeft, jint clipTop, \
                                     jint clipRight, jint clipBottom, \
                                     NativePrimitive *pPrim, \
                                     CompositeInfo *pCompInfo) \
{ \
    jint glyphCounter; \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix; \
    DeclareAlphaVarFor ## STRATEGY(srcA) \
    DeclareCompVarsFor ## STRATEGY(src) \
\
    Declare ## DST ## LoadVars(pix) \
    Declare ## DST ## StoreVars(pix) \
\
    Init ## DST ## LoadVars(pix, pRasInfo); \
    Init ## DST ## StoreVarsY(pix, pRasInfo); \
    Init ## DST ## StoreVarsX(pix, pRasInfo); \
    Extract ## STRATEGY ## CompsAndAlphaFromArgb(argbcolor, src); \
\
    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) { \
        DeclareDrawGlyphListClipVars(pixels, rowBytes, width, height, \
                                     left, top, right, bottom) \
        ClipDrawGlyphList(DST, pixels, 1, rowBytes, width, height, \
                          left, top, right, bottom, \
                          clipLeft, clipTop, clipRight, clipBottom, \
                          glyphs, glyphCounter, continue) \
        pPix = PtrCoord(pRasInfo->rasBase,left,DST ## PixelStride,top,scan); \
\
        Set ## DST ## StoreVarsYPos(pix, pRasInfo, top); \
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, pix, left) \
            int x = 0; \
            Set ## DST ## StoreVarsXPos(pix, pRasInfo, left); \
            do { \
                InitialLoad ## DST(pPix, pix); \
                GlyphListAABlend ## STRATEGY(DST, pixels, x, pPix, \
                                             fgpixel, pix, src); \
                ShiftBits ## DST(pix); \
                Next ## DST ## StoreVarsX(pix); \
            } while (++x < width); \
            FinalStore ## DST(pPix, pix); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
            Next ## DST ## StoreVarsY(pix); \
        } while (--height > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_XOR_DRAWGLYPHLIST(DST) \
void NAME_XOR_DRAWGLYPHLIST(DST)(SurfaceDataRasInfo *pRasInfo, \
                                 ImageRef *glyphs, \
                                 jint totalGlyphs, jint fgpixel, \
                                 jint argbcolor, \
                                 jint clipLeft, jint clipTop, \
                                 jint clipRight, jint clipBottom, \
                                 NativePrimitive *pPrim, \
                                 CompositeInfo *pCompInfo) \
{ \
    jint glyphCounter; \
    jint scan = pRasInfo->scanStride; \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    DST ## DataType *pPix; \
 \
    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) { \
        DeclareDrawGlyphListClipVars(pixels, rowBytes, width, height, \
                                     left, top, right, bottom) \
        ClipDrawGlyphList(DST, pixels, 1, rowBytes, width, height, \
                          left, top, right, bottom, \
                          clipLeft, clipTop, clipRight, clipBottom, \
                          glyphs, glyphCounter, continue) \
        pPix = PtrCoord(pRasInfo->rasBase,left,DST ## PixelStride,top,scan); \
\
        do { \
            Declare ## DST ## InitialLoadVars(pRasInfo, pPix, DstPix, left) \
            jint x = 0; \
            do { \
                InitialLoad ## DST(pPix, DstPix); \
                if (pixels[x]) { \
                    Xor ## DST ## PixelData(pPix, 0, DstPix, \
                                            fgpixel, xorpixel, alphamask); \
                } \
                ShiftBits ## DST(DstPix); \
            } while (++x < width); \
            FinalStore ## DST(pPix, DstPix); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
        } while (--height > 0); \
    } \
}

#define DEFINE_BYTE_BINARY_ALPHA_MASKBLIT(SRC, DST, STRATEGY) \
void NAME_ALPHA_MASKBLIT(SRC, DST) \
    (void *dstBase, void *srcBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     SurfaceDataRasInfo *pDstInfo, \
     SurfaceDataRasInfo *pSrcInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAndSetOpaqueAlphaVarFor ## STRATEGY(pathA) \
    DeclareAndClearAlphaVarFor ## STRATEGY(srcA) \
    DeclareAndClearAlphaVarFor ## STRATEGY(dstA) \
    DeclareAndInitExtraAlphaFor ## STRATEGY(extraA) \
    jint srcScan = pSrcInfo->scanStride; \
    jint dstScan = pDstInfo->scanStride; \
    jboolean loadsrc, loaddst; \
    jint srcx1 = pSrcInfo->bounds.x1; \
    jint dstx1 = pDstInfo->bounds.x1; \
    SRC ## DataType *pSrc = (SRC ## DataType *) (srcBase); \
    DST ## DataType *pDst = (DST ## DataType *) (dstBase); \
    Declare ## SRC ## AlphaLoadData(SrcRead) \
    Declare ## DST ## AlphaLoadData(DstWrite) \
    Declare ## DST ## StoreVars(DstWrite) \
    DeclareAlphaOperands(SrcOp) \
    DeclareAlphaOperands(DstOp) \
 \
    ExtractAlphaOperandsFor ## STRATEGY(AlphaRules[pCompInfo->rule].srcOps, \
                                        SrcOp); \
    ExtractAlphaOperandsFor ## STRATEGY(AlphaRules[pCompInfo->rule].dstOps, \
                                        DstOp); \
    loadsrc = !FuncIsZero(SrcOp) || FuncNeedsAlpha(DstOp); \
    loaddst = pMask || !FuncIsZero(DstOp) || FuncNeedsAlpha(SrcOp); \
 \
    Init ## SRC ## AlphaLoadData(SrcRead, pSrcInfo); \
    Init ## DST ## AlphaLoadData(DstWrite, pDstInfo); \
    srcScan -= width * SRC ## PixelStride; \
    dstScan -= width * DST ## PixelStride; \
    maskScan -= width; \
    if (pMask) { \
        pMask += maskOff; \
    } \
 \
    Init ## DST ## StoreVarsY(DstWrite, pDstInfo); \
    do { \
        Declare ## SRC ## InitialLoadVars(pSrcInfo, pSrc, SrcRead, srcx1) \
        Declare ## DST ## InitialLoadVars(pDstInfo, pDst, DstWrite, dstx1) \
        jint w = width; \
        Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
        do { \
            DeclareAlphaVarFor ## STRATEGY(resA) \
            DeclareCompVarsFor ## STRATEGY(res) \
            DeclareAlphaVarFor ## STRATEGY(srcF) \
            DeclareAlphaVarFor ## STRATEGY(dstF) \
 \
            InitialLoad ## SRC(pSrc, SrcRead); \
            InitialLoad ## DST(pDst, DstWrite); \
            if (pMask) { \
                pathA = *pMask++; \
                if (!pathA) { \
                    ShiftBits ## SRC(SrcRead); \
                    ShiftBits ## DST(DstWrite); \
                    pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                    pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                    Next ## DST ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                PromoteByteAlphaFor ## STRATEGY(pathA); \
            } \
            if (loadsrc) { \
                LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc,SrcRead,src); \
                srcA = MultiplyAlphaFor ## STRATEGY(extraA, srcA); \
            } \
            if (loaddst) { \
                LoadAlphaFrom ## DST ## For ## STRATEGY(pDst,DstWrite,dst); \
            } \
            srcF = ApplyAlphaOperands(SrcOp, dstA); \
            dstF = ApplyAlphaOperands(DstOp, srcA); \
            if (pathA != MaxValFor ## STRATEGY) { \
                srcF = MultiplyAlphaFor ## STRATEGY(pathA, srcF); \
                dstF = MaxValFor ## STRATEGY - pathA + \
                           MultiplyAlphaFor ## STRATEGY(pathA, dstF); \
            } \
            if (srcF) { \
                resA = MultiplyAlphaFor ## STRATEGY(srcF, srcA); \
                if (!(SRC ## IsPremultiplied)) { \
                    srcF = resA; \
                } else { \
                    srcF = MultiplyAlphaFor ## STRATEGY(srcF, extraA); \
                } \
                if (srcF) { \
                    /* assert(loadsrc); */ \
                    Postload ## STRATEGY ## From ## SRC(pSrc, SrcRead, res); \
                    if (srcF != MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                              srcF, res); \
                    } \
                } else { \
                    Set ## STRATEGY ## CompsToZero(res); \
                } \
            } else { \
                if (dstF == MaxValFor ## STRATEGY) { \
                    ShiftBits ## SRC(SrcRead); \
                    ShiftBits ## DST(DstWrite); \
                    pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                    pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                    Next ## DST ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                resA = 0; \
                Set ## STRATEGY ## CompsToZero(res); \
            } \
            if (dstF) { \
                dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                if (!(DST ## IsPremultiplied)) { \
                    dstF = dstA; \
                } \
                resA += dstA; \
                if (dstF) { \
                    DeclareCompVarsFor ## STRATEGY(tmp) \
                    /* assert(loaddst); */ \
                    Postload ## STRATEGY ## From ## DST(pDst,DstWrite,tmp); \
                    if (dstF != MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(tmp, \
                                                              dstF, tmp); \
                    } \
                    Store ## STRATEGY ## CompsUsingOp(res, +=, tmp); \
                } \
            } \
            if (!(DST ## IsPremultiplied) && resA && \
                resA < MaxValFor ## STRATEGY) \
            { \
                DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
            } \
            Store ## DST ## From ## STRATEGY ## Comps(pDst, DstWrite, \
                                                      0, res); \
            ShiftBits ## SRC(SrcRead); \
            ShiftBits ## DST(DstWrite); \
            pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
            pDst = PtrAddBytes(pDst, DST ## PixelStride); \
            Next ## DST ## StoreVarsX(DstWrite); \
        } while (--w > 0); \
        FinalStore ## DST(pDst, DstWrite); \
        pSrc = PtrAddBytes(pSrc, srcScan); \
        pDst = PtrAddBytes(pDst, dstScan); \
        Next ## DST ## StoreVarsY(DstWrite); \
        if (pMask) { \
            pMask = PtrAddBytes(pMask, maskScan); \
        } \
    } while (--height > 0); \
}

#define DEFINE_BYTE_BINARY_ALPHA_MASKFILL(TYPE, STRATEGY) \
void NAME_ALPHA_MASKFILL(TYPE) \
    (void *rasBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     jint fgColor, \
     SurfaceDataRasInfo *pRasInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAndSetOpaqueAlphaVarFor ## STRATEGY(pathA) \
    DeclareAlphaVarFor ## STRATEGY(srcA) \
    DeclareCompVarsFor ## STRATEGY(src) \
    DeclareAndClearAlphaVarFor ## STRATEGY(dstA) \
    DeclareAlphaVarFor ## STRATEGY(dstF) \
    DeclareAlphaVarFor ## STRATEGY(dstFbase) \
    jint rasScan = pRasInfo->scanStride; \
    jboolean loaddst; \
    jint x1 = pRasInfo->bounds.x1; \
    TYPE ## DataType *pRas = (TYPE ## DataType *) (rasBase); \
    Declare ## TYPE ## AlphaLoadData(DstWrite) \
    Declare ## TYPE ## StoreVars(DstWrite) \
    DeclareAlphaOperands(SrcOp) \
    DeclareAlphaOperands(DstOp) \
 \
    Extract ## STRATEGY ## CompsAndAlphaFromArgb(fgColor, src); \
    if (srcA != MaxValFor ## STRATEGY) { \
        MultiplyAndStore ## STRATEGY ## Comps(src, srcA, src); \
    } \
 \
    ExtractAlphaOperandsFor ## STRATEGY(AlphaRules[pCompInfo->rule].srcOps, \
                                        SrcOp); \
    ExtractAlphaOperandsFor ## STRATEGY(AlphaRules[pCompInfo->rule].dstOps, \
                                        DstOp); \
    loaddst = pMask || !FuncIsZero(DstOp) || FuncNeedsAlpha(SrcOp); \
 \
    dstFbase = dstF = ApplyAlphaOperands(DstOp, srcA); \
 \
    Init ## TYPE ## AlphaLoadData(DstWrite, pRasInfo); \
    maskScan -= width; \
    if (pMask) { \
        pMask += maskOff; \
    } \
 \
    Init ## TYPE ## StoreVarsY(DstWrite, pRasInfo); \
    do { \
        Declare ## TYPE ## InitialLoadVars(pRasInfo, pRas, DstWrite, x1) \
        jint w = width; \
        Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
        do { \
            DeclareAlphaVarFor ## STRATEGY(resA) \
            DeclareCompVarsFor ## STRATEGY(res) \
            DeclareAlphaVarFor ## STRATEGY(srcF) \
 \
            InitialLoad ## TYPE(pRas, DstWrite); \
            if (pMask) { \
                pathA = *pMask++; \
                if (!pathA) { \
                    ShiftBits ## TYPE(DstWrite); \
                    Next ## TYPE ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                PromoteByteAlphaFor ## STRATEGY(pathA); \
                dstF = dstFbase; \
            } \
            if (loaddst) { \
                LoadAlphaFrom ## TYPE ## For ## STRATEGY(pRas,DstWrite,dst);\
            } \
            srcF = ApplyAlphaOperands(SrcOp, dstA); \
            if (pathA != MaxValFor ## STRATEGY) { \
                srcF = MultiplyAlphaFor ## STRATEGY(pathA, srcF); \
                dstF = MaxValFor ## STRATEGY - pathA + \
                           MultiplyAlphaFor ## STRATEGY(pathA, dstF); \
            } \
            if (srcF) { \
                if (srcF == MaxValFor ## STRATEGY) { \
                    resA = srcA; \
                    Store ## STRATEGY ## CompsUsingOp(res, =, src); \
                } else { \
                    resA = MultiplyAlphaFor ## STRATEGY(srcF, srcA); \
                    MultiplyAndStore ## STRATEGY ## Comps(res, srcF, src); \
                } \
            } else { \
                if (dstF == MaxValFor ## STRATEGY) { \
                    ShiftBits ## TYPE(DstWrite); \
                    Next ## TYPE ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                resA = 0; \
                Set ## STRATEGY ## CompsToZero(res); \
            } \
            if (dstF) { \
                dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                if (!(TYPE ## IsPremultiplied)) { \
                    dstF = dstA; \
                } \
                resA += dstA; \
                if (dstF) { \
                    DeclareCompVarsFor ## STRATEGY(tmp) \
                    /* assert(loaddst); */ \
                    Postload ## STRATEGY ## From ## TYPE(pRas,DstWrite,tmp); \
                    if (dstF != MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(tmp, \
                                                              dstF, tmp); \
                    } \
                    Store ## STRATEGY ## CompsUsingOp(res, +=, tmp); \
                } \
            } \
            if (!(TYPE ## IsPremultiplied) && resA && \
                resA < MaxValFor ## STRATEGY) \
            { \
                DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
            } \
            Store ## TYPE ## From ## STRATEGY ## Comps(pRas, DstWrite, \
                                                       0, res); \
            ShiftBits ## TYPE(DstWrite); \
            Next ## TYPE ## StoreVarsX(DstWrite); \
        } while (--w > 0); \
        FinalStore ## TYPE(pRas, DstWrite); \
        pRas = PtrAddBytes(pRas, rasScan); \
        Next ## TYPE ## StoreVarsY(DstWrite); \
        if (pMask) { \
            pMask = PtrAddBytes(pMask, maskScan); \
        } \
    } while (--height > 0); \
}


/*
 * The macros defined above use the following macro definitions supplied
 * for the various ByteBinary-specific surface types to manipulate pixel data.
 *
 * In the macro names in the following definitions, the string <stype>
 * is used as a place holder for the SurfaceType name (eg. ByteBinary2Bit).
 * The macros above access these type specific macros using the ANSI
 * CPP token concatenation operator "##".
 *
 * Declare<stype>InitialLoadVars     Declare and initialize the variables used
 *                                   for managing byte/bit offsets
 * InitialLoad<stype>                Store the current byte, fetch the next
 *                                   byte, and reset the bit offset
 * ShiftBits<stype>                  Advance to the next pixel by adjusting
 *                                   the bit offset (1, 2, or 4 bits)
 * FinalStore<stype>                 Store the current byte
 * CurrentPixel<stype>               Represents the current pixel by shifting
 *                                   the value with the current bit offset and
 *                                   then masking the value to either 1, 2, or
 *                                   4 bits
 */

#endif /* AnyByteBinary_h_Included */
