/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AlphaMacros_h_Included
#define AlphaMacros_h_Included

#include "GraphicsPrimitiveMgr.h"
#include "AlphaMath.h"
#include "IntArgb.h"                 /* for "Extract...FromArgb" macros */

#define DeclareAlphaOperands(PREFIX) \
    jint PREFIX ## And, PREFIX ## Xor, PREFIX ## Add;

#define ExtractAlphaOperandsFor4ByteArgb(f, PREFIX) \
    do { \
        PREFIX ## And = (f).andval; \
        PREFIX ## Xor = (f).xorval; \
        PREFIX ## Add = (jint) (f).addval - PREFIX ## Xor; \
    } while (0)

#define ExtractAlphaOperandsFor1ByteGray(f, PREFIX) \
    ExtractAlphaOperandsFor4ByteArgb(f, PREFIX)

#define ExtractAlphaOperandsFor1ShortGray(f, PREFIX) \
    do { \
        PREFIX ## And = ((f).andval << 8) + (f).andval; \
        PREFIX ## Xor = (f).xorval; \
        PREFIX ## Add = (jint) (((f).addval << 8) + (f).addval) - \
                                                            PREFIX ## Xor; \
    } while (0)

#define ApplyAlphaOperands(PREFIX, a) \
    ((((a) & PREFIX ## And) ^ PREFIX ## Xor) + PREFIX ## Add)

#define FuncNeedsAlpha(PREFIX)  (PREFIX ## And != 0)
#define FuncIsZero(PREFIX)      ((PREFIX ## And | PREFIX ## Add) == 0)

typedef struct {
    jubyte      addval;
    jubyte      andval;
    jshort      xorval;
} AlphaOperands;

typedef struct {
    AlphaOperands       srcOps;
    AlphaOperands       dstOps;
} AlphaFunc;

extern AlphaFunc AlphaRules[];

#define DEFINE_ALPHA_MASKBLIT(SRC, DST, STRATEGY) \
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
    SRC ## DataType *pSrc = (SRC ## DataType *) (srcBase); \
    DST ## DataType *pDst = (DST ## DataType *) (dstBase); \
    Declare ## SRC ## AlphaLoadData(SrcPix) \
    Declare ## DST ## AlphaLoadData(DstPix) \
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
    Init ## SRC ## AlphaLoadData(SrcPix, pSrcInfo); \
    Init ## DST ## AlphaLoadData(DstPix, pDstInfo); \
    srcScan -= width * SRC ## PixelStride; \
    dstScan -= width * DST ## PixelStride; \
    maskScan -= width; \
    if (pMask) { \
        pMask += maskOff; \
    } \
 \
    Init ## DST ## StoreVarsY(DstWrite, pDstInfo); \
    do { \
        jint w = width; \
        Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
        do { \
            DeclareAlphaVarFor ## STRATEGY(resA) \
            DeclareCompVarsFor ## STRATEGY(res) \
            DeclareAlphaVarFor ## STRATEGY(srcF) \
            DeclareAlphaVarFor ## STRATEGY(dstF) \
 \
            if (pMask) { \
                pathA = *pMask++; \
                if (!pathA) { \
                    pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                    pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                    Next ## DST ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                PromoteByteAlphaFor ## STRATEGY(pathA); \
            } \
            if (loadsrc) { \
                LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc, SrcPix, src); \
                srcA = MultiplyAlphaFor ## STRATEGY(extraA, srcA); \
            } \
            if (loaddst) { \
                LoadAlphaFrom ## DST ## For ## STRATEGY(pDst, DstPix, dst); \
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
                    Postload ## STRATEGY ## From ## SRC(pSrc, SrcPix, res); \
                    if (srcF != MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                              srcF, res); \
                    } \
                } else { \
                    if (dstF == MaxValFor ## STRATEGY) { \
                        pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                        pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                        Next ## DST ## StoreVarsX(DstWrite); \
                        continue; \
                    } \
                    Set ## STRATEGY ## CompsToZero(res); \
                } \
            } else { \
                if (dstF == MaxValFor ## STRATEGY) { \
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
                    Postload ## STRATEGY ## From ## DST(pDst, DstPix, tmp); \
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
            pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
            pDst = PtrAddBytes(pDst, DST ## PixelStride); \
            Next ## DST ## StoreVarsX(DstWrite); \
        } while (--w > 0); \
        pSrc = PtrAddBytes(pSrc, srcScan); \
        pDst = PtrAddBytes(pDst, dstScan); \
        Next ## DST ## StoreVarsY(DstWrite); \
        if (pMask) { \
            pMask = PtrAddBytes(pMask, maskScan); \
        } \
    } while (--height > 0); \
}

/* REMIND: This macro is as yet, untested */
#define DEFINE_SRC_MASKBLIT(SRC, DST, STRATEGY) \
void NAME_SRC_MASKBLIT(SRC, DST) \
    (void *dstBase, void *srcBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     SurfaceDataRasInfo *pDstInfo, \
     SurfaceDataRasInfo *pSrcInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAndInitExtraAlphaFor ## STRATEGY(extraA) \
    jint srcScan = pSrcInfo->scanStride; \
    jint dstScan = pDstInfo->scanStride; \
    SRC ## DataType *pSrc = (SRC ## DataType *) (srcBase); \
    DST ## DataType *pDst = (DST ## DataType *) (dstBase); \
    Declare ## SRC ## AlphaLoadData(SrcPix) \
    Declare ## DST ## AlphaLoadData(DstPix) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## AlphaLoadData(SrcPix, pSrcInfo); \
    Init ## DST ## AlphaLoadData(DstPix, pDstInfo); \
    srcScan -= width * SRC ## PixelStride; \
    dstScan -= width * DST ## PixelStride; \
 \
    Init ## DST ## StoreVarsY(DstWrite, pDstInfo); \
    if (pMask) { \
        maskScan -= width; \
        pMask += maskOff; \
        do { \
            jint w = width; \
            Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAlphaVarFor ## STRATEGY(srcF) \
                DeclareAlphaVarFor ## STRATEGY(dstF) \
                DeclareAndInitPathAlphaFor ## STRATEGY(pathA) \
 \
                if (pathA) { \
                    LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc, \
                                                            SrcPix, res); \
                    resA = MultiplyAlphaFor ## STRATEGY(extraA, resA); \
                    if (SRC ## IsPremultiplied) { \
                        srcF = extraA; \
                    } else { \
                        srcF = resA; \
                    } \
                    Postload ## STRATEGY ## From ## SRC(pSrc, SrcPix, res); \
                    if (pathA < 0xff) { \
                        DeclareAlphaVarFor ## STRATEGY(dstA) \
                        DeclareCompVarsFor ## STRATEGY(dst) \
                        PromoteByteAlphaFor ## STRATEGY(pathA); \
                        srcF = MultiplyAlphaFor ## STRATEGY(pathA, srcF); \
                        dstF = MaxValFor ## STRATEGY - pathA; \
                        LoadAlphaFrom ## DST ## For ## STRATEGY(pDst, \
                                                                DstPix, \
                                                                dst); \
                        dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA) \
                        if (!(DST ## IsPremultiplied)) { \
                            dstF = dstA; \
                        } \
                        Postload ## STRATEGY ## From ## DST(pDst, DstPix, \
                                                            dst); \
                        resA = dstA + \
                                 MultiplyAlphaFor ## STRATEGY(pathA, resA); \
                        MultMultAddAndStore ## STRATEGY ## Comps(res, \
                                                                 dstF, dst, \
                                                                 srcF, res); \
                    } else if (srcF < MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                              srcF, src); \
                    } \
                    if (!(DST ## IsPremultiplied) && resA && \
                        resA < MaxValFor ## STRATEGY) \
                    { \
                        DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
                    } \
                    Store ## DST ## From ## STRATEGY ## Comps(pDst, DstWrite,\
                                                              0, res);\
                } \
                pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                Next ## DST ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pSrc = PtrAddBytes(pSrc, srcScan); \
            pDst = PtrAddBytes(pDst, dstScan); \
            Next ## DST ## StoreVarsY(DstWrite); \
            pMask = PtrAddBytes(pMask, maskScan); \
        } while (--height > 0); \
    } else /* pMask == 0 */ { \
        do { \
            jint w = width; \
            Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAlphaVarFor ## STRATEGY(srcF) \
 \
                LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc, SrcPix, res); \
                resA = MultiplyAlphaFor ## STRATEGY(extraA, resA); \
                if (SRC ## IsPremultiplied) { \
                    srcF = extraA; \
                } else { \
                    srcF = resA; \
                } \
                Postload ## STRATEGY ## From ## SRC(pSrc, SrcPix, res); \
                if (srcF < MaxValFor ## STRATEGY) { \
                    MultiplyAndStore ## STRATEGY ## Comps(res, srcF, src); \
                } \
                if (!(DST ## IsPremultiplied) && resA && \
                    resA < MaxValFor ## STRATEGY) \
                { \
                    DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
                } \
                Store ## DST ## From ## STRATEGY ## Comps(pDst, DstWrite, \
                                                          0, res); \
                pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                Next ## DST ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pSrc = PtrAddBytes(pSrc, srcScan); \
            pDst = PtrAddBytes(pDst, dstScan); \
            Next ## DST ## StoreVarsY(DstWrite); \
        } while (--height > 0); \
    } \
}

#define DEFINE_SRCOVER_MASKBLIT(SRC, DST, STRATEGY) \
void NAME_SRCOVER_MASKBLIT(SRC, DST) \
    (void *dstBase, void *srcBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     SurfaceDataRasInfo *pDstInfo, \
     SurfaceDataRasInfo *pSrcInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAndInitExtraAlphaFor ## STRATEGY(extraA) \
    jint srcScan = pSrcInfo->scanStride; \
    jint dstScan = pDstInfo->scanStride; \
    SRC ## DataType *pSrc = (SRC ## DataType *) (srcBase); \
    DST ## DataType *pDst = (DST ## DataType *) (dstBase); \
    Declare ## SRC ## AlphaLoadData(SrcPix) \
    Declare ## DST ## AlphaLoadData(DstPix) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## AlphaLoadData(SrcPix, pSrcInfo); \
    Init ## DST ## AlphaLoadData(DstPix, pDstInfo); \
    srcScan -= width * SRC ## PixelStride; \
    dstScan -= width * DST ## PixelStride; \
 \
    Init ## DST ## StoreVarsY(DstWrite, pDstInfo); \
    if (pMask) { \
        pMask += maskOff; \
        maskScan -= width; \
        do { \
            jint w = width; \
            Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
            do { \
                DeclareAndInitPathAlphaFor ## STRATEGY(pathA) \
 \
                if (pathA) { \
                    DeclareAlphaVarFor ## STRATEGY(resA) \
                    DeclareCompVarsFor ## STRATEGY(res) \
                    DeclareAlphaVarFor ## STRATEGY(srcF) \
                    PromoteByteAlphaFor ## STRATEGY(pathA); \
                    pathA = MultiplyAlphaFor ## STRATEGY(pathA, extraA); \
                    LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc, \
                                                            SrcPix, res); \
                    resA = MultiplyAlphaFor ## STRATEGY(pathA, resA); \
                    if (resA) { \
                        if (SRC ## IsPremultiplied) { \
                            srcF = pathA; \
                        } else { \
                            srcF = resA; \
                        } \
                        Postload ## STRATEGY ## From ## SRC(pSrc, SrcPix, \
                                                            res); \
                        if (resA < MaxValFor ## STRATEGY) { \
                            DeclareAlphaVarFor ## STRATEGY(dstA) \
                            DeclareCompVarsFor ## STRATEGY(dst) \
                            DeclareAndInvertAlphaVarFor ## STRATEGY(dstF, \
                                                                    resA) \
                            LoadAlphaFrom ## DST ## For ## STRATEGY(pDst, \
                                                                    DstPix, \
                                                                    dst); \
                            dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                            if (!(DST ## IsPremultiplied)) { \
                                dstF = dstA; \
                            } \
                            Postload ## STRATEGY ## From ## DST(pDst, DstPix,\
                                                                dst); \
                            resA += dstA; \
                            MultMultAddAndStore ## STRATEGY ## Comps(res, \
                                                                  dstF, dst, \
                                                                  srcF, res);\
                        } else if (srcF < MaxValFor ## STRATEGY) { \
                            MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                                  srcF, res);\
                        } \
                        if (!(DST ## IsOpaque) && \
                            !(DST ## IsPremultiplied) && resA && \
                            resA < MaxValFor ## STRATEGY) \
                        { \
                            DivideAndStore ## STRATEGY ## Comps(res, \
                                                                res, resA); \
                        } \
                        Store ## DST ## From ## STRATEGY ## Comps(pDst, \
                                                                  DstWrite, \
                                                                  0, res); \
                    } \
                } \
                pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                Next ## DST ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pSrc = PtrAddBytes(pSrc, srcScan); \
            pDst = PtrAddBytes(pDst, dstScan); \
            Next ## DST ## StoreVarsY(DstWrite); \
            pMask = PtrAddBytes(pMask, maskScan); \
        } while (--height > 0); \
    } else /* pMask == 0 */ { \
        do { \
            jint w = width; \
            Init ## DST ## StoreVarsX(DstWrite, pDstInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAlphaVarFor ## STRATEGY(srcF) \
 \
                LoadAlphaFrom ## SRC ## For ## STRATEGY(pSrc, SrcPix, res); \
                resA = MultiplyAlphaFor ## STRATEGY(extraA, resA); \
                if (resA) { \
                    if (SRC ## IsPremultiplied) { \
                        srcF = extraA; \
                    } else { \
                        srcF = resA; \
                    } \
                    Postload ## STRATEGY ## From ## SRC(pSrc, SrcPix, res); \
                    if (resA < MaxValFor ## STRATEGY) { \
                        DeclareAlphaVarFor ## STRATEGY(dstA) \
                        DeclareCompVarsFor ## STRATEGY(dst) \
                        DeclareAndInvertAlphaVarFor ## STRATEGY(dstF, resA) \
                        LoadAlphaFrom ## DST ## For ## STRATEGY(pDst, \
                                                                DstPix, \
                                                                dst); \
                        dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                        if (!(DST ## IsPremultiplied)) { \
                            dstF = dstA; \
                        } \
                        Postload ## STRATEGY ## From ## DST(pDst, DstPix, \
                                                            dst); \
                        resA += dstA; \
                        MultMultAddAndStore ## STRATEGY ## Comps(res, \
                                                                 dstF, dst, \
                                                                 srcF, res); \
                    } else if (srcF < MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                              srcF, res); \
                    } \
                    if (!(DST ## IsOpaque) && \
                        !(DST ## IsPremultiplied) && resA && \
                        resA < MaxValFor ## STRATEGY) \
                    { \
                        DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
                    } \
                    Store ## DST ## From ## STRATEGY ## Comps(pDst, DstWrite,\
                                                              0, res); \
                } \
                pSrc = PtrAddBytes(pSrc, SRC ## PixelStride); \
                pDst = PtrAddBytes(pDst, DST ## PixelStride); \
                Next ## DST ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pSrc = PtrAddBytes(pSrc, srcScan); \
            pDst = PtrAddBytes(pDst, dstScan); \
            Next ## DST ## StoreVarsY(DstWrite); \
        } while (--height > 0); \
    } \
}

#define DEFINE_ALPHA_MASKFILL(TYPE, STRATEGY) \
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
    TYPE ## DataType *pRas = (TYPE ## DataType *) (rasBase); \
    Declare ## TYPE ## AlphaLoadData(DstPix) \
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
    Init ## TYPE ## AlphaLoadData(DstPix, pRasInfo); \
    rasScan -= width * TYPE ## PixelStride; \
    maskScan -= width; \
    if (pMask) { \
        pMask += maskOff; \
    } \
 \
    Init ## TYPE ## StoreVarsY(DstWrite, pRasInfo); \
    do { \
        jint w = width; \
        Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
        do { \
            DeclareAlphaVarFor ## STRATEGY(resA) \
            DeclareCompVarsFor ## STRATEGY(res) \
            DeclareAlphaVarFor ## STRATEGY(srcF) \
 \
            if (pMask) { \
                pathA = *pMask++; \
                if (!pathA) { \
                    pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                    Next ## TYPE ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                PromoteByteAlphaFor ## STRATEGY(pathA); \
                dstF = dstFbase; \
            } \
            if (loaddst) { \
                LoadAlphaFrom ## TYPE ## For ## STRATEGY(pRas, DstPix, dst);\
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
                    pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                    Next ## TYPE ## StoreVarsX(DstWrite); \
                    continue; \
                } \
                resA = 0; \
                Set ## STRATEGY ## CompsToZero(res); \
            } \
            if (dstF) { \
                dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                resA += dstA; \
                if (TYPE ## IsPremultiplied) { \
                    dstA = dstF; \
                } \
                if (dstA) { \
                    DeclareCompVarsFor ## STRATEGY(tmp) \
                    /* assert(loaddst); */ \
                    Postload ## STRATEGY ## From ## TYPE(pRas, DstPix, tmp); \
                    if (dstA != MaxValFor ## STRATEGY) { \
                        MultiplyAndStore ## STRATEGY ## Comps(tmp, \
                                                              dstA, tmp); \
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
            pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
            Next ## TYPE ## StoreVarsX(DstWrite); \
        } while (--w > 0); \
        pRas = PtrAddBytes(pRas, rasScan); \
        Next ## TYPE ## StoreVarsY(DstWrite); \
        if (pMask) { \
            pMask = PtrAddBytes(pMask, maskScan); \
        } \
    } while (--height > 0); \
}

#define DEFINE_SRC_MASKFILL(TYPE, STRATEGY) \
void NAME_SRC_MASKFILL(TYPE) \
    (void *rasBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     jint fgColor, \
     SurfaceDataRasInfo *pRasInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAlphaVarFor ## STRATEGY(srcA) \
    DeclareCompVarsFor ## STRATEGY(src) \
    jint rasScan = pRasInfo->scanStride; \
    TYPE ## DataType *pRas = (TYPE ## DataType *) (rasBase); \
    Declare ## TYPE ## AlphaLoadData(DstPix) \
    Declare ## TYPE ## StoreVars(DstWrite) \
    Declare ## TYPE ## BlendFillVars(DstFill) \
 \
    Extract ## STRATEGY ## CompsAndAlphaFromArgb(fgColor, src); \
    if (srcA == 0) { \
        Set ## STRATEGY ## CompsToZero(src); \
        Clear ## TYPE ## BlendFillVars(DstFill, fgColor); \
    } else { \
        if (!(TYPE ## IsPremultiplied)) { \
            Init ## TYPE ## BlendFillVarsNonPre(DstFill, fgColor, src); \
        } \
        if (srcA != MaxValFor ## STRATEGY) { \
            MultiplyAndStore ## STRATEGY ## Comps(src, srcA, src); \
        } \
        if (TYPE ## IsPremultiplied) { \
            Init ## TYPE ## BlendFillVarsPre(DstFill, fgColor, src); \
        } \
    } \
 \
    Init ## TYPE ## AlphaLoadData(DstPix, pRasInfo); \
    Init ## TYPE ## StoreVarsY(DstWrite, pRasInfo); \
 \
    rasScan -= width * TYPE ## PixelStride; \
    if (pMask) { \
        pMask += maskOff; \
        maskScan -= width; \
        do { \
            jint w = width; \
            Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAlphaVarFor ## STRATEGY(dstF) \
                DeclareAndInitPathAlphaFor ## STRATEGY(pathA) \
 \
                if (pathA > 0) { \
                    if (pathA == 0xff) { \
                        /* pathA ignored here, not promoted */ \
                        Store ## TYPE ## BlendFill(pRas, DstFill, 0, \
                                                   fgColor, src); \
                    } else { \
                        PromoteByteAlphaFor ## STRATEGY(pathA); \
                        dstF = MaxValFor ## STRATEGY - pathA; \
                        LoadAlphaFrom ## TYPE ## For ## STRATEGY(pRas, \
                                                                 DstPix, \
                                                                 res); \
                        resA = MultiplyAlphaFor ## STRATEGY(dstF, resA); \
                        if (!(TYPE ## IsPremultiplied)) { \
                            dstF = resA; \
                        } \
                        resA += MultiplyAlphaFor ## STRATEGY(pathA, srcA); \
                        Postload ## STRATEGY ## From ## TYPE(pRas, DstPix, \
                                                             res); \
                        MultMultAddAndStore ## STRATEGY ## Comps(res, \
                                                                 dstF, res, \
                                                                 pathA, src);\
                        if (!(TYPE ## IsPremultiplied) && resA && \
                            resA < MaxValFor ## STRATEGY) \
                        { \
                            DivideAndStore ## STRATEGY ## Comps(res, \
                                                                res, resA); \
                        } \
                        Store ## TYPE ## From ## STRATEGY ## Comps(pRas, \
                                                                   DstWrite, \
                                                                   0, res); \
                    } \
                } \
                pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                Next ## TYPE ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pRas = PtrAddBytes(pRas, rasScan); \
            Next ## TYPE ## StoreVarsY(DstWrite); \
            pMask = PtrAddBytes(pMask, maskScan); \
        } while (--height > 0); \
    } else /* pMask == 0 */ { \
        do { \
            jint w = width; \
            Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
            do { \
                Store ## TYPE ## BlendFill(pRas, DstFill, 0, fgColor, src); \
                pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                Next ## TYPE ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pRas = PtrAddBytes(pRas, rasScan); \
            Next ## TYPE ## StoreVarsY(DstWrite); \
        } while (--height > 0); \
    } \
}

#define DEFINE_SRCOVER_MASKFILL(TYPE, STRATEGY) \
void NAME_SRCOVER_MASKFILL(TYPE) \
    (void *rasBase, \
     jubyte *pMask, jint maskOff, jint maskScan, \
     jint width, jint height, \
     jint fgColor, \
     SurfaceDataRasInfo *pRasInfo, \
     NativePrimitive *pPrim, \
     CompositeInfo *pCompInfo) \
{ \
    DeclareAlphaVarFor ## STRATEGY(srcA) \
    DeclareCompVarsFor ## STRATEGY(src) \
    jint rasScan = pRasInfo->scanStride; \
    TYPE ## DataType *pRas = (TYPE ## DataType *) (rasBase); \
    Declare ## TYPE ## AlphaLoadData(DstPix) \
    Declare ## TYPE ## StoreVars(DstWrite) \
 \
    Extract ## STRATEGY ## CompsAndAlphaFromArgb(fgColor, src); \
    if (srcA != MaxValFor ## STRATEGY) { \
        if (srcA == 0) { \
            return; \
        } \
        MultiplyAndStore ## STRATEGY ## Comps(src, srcA, src); \
    } \
 \
    Init ## TYPE ## AlphaLoadData(DstPix, pRasInfo); \
    Init ## TYPE ## StoreVarsY(DstWrite, pRasInfo); \
 \
    rasScan -= width * TYPE ## PixelStride; \
    if (pMask) { \
        pMask += maskOff; \
        maskScan -= width; \
        do { \
            jint w = width; \
            Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAndInitPathAlphaFor ## STRATEGY(pathA) \
 \
                if (pathA > 0) { \
                    if (pathA != 0xff) { \
                        PromoteByteAlphaFor ## STRATEGY(pathA); \
                        resA = MultiplyAlphaFor ## STRATEGY(pathA, srcA); \
                        MultiplyAndStore ## STRATEGY ## Comps(res, \
                                                              pathA, src); \
                    } else { \
                        /* pathA ignored here, not promoted */ \
                        resA = srcA; \
                        Store ## STRATEGY ## CompsUsingOp(res, =, src); \
                    } \
                    if (resA != MaxValFor ## STRATEGY) { \
                        DeclareAndInvertAlphaVarFor ## STRATEGY(dstF, resA) \
                        DeclareAndClearAlphaVarFor ## STRATEGY(dstA) \
                        LoadAlphaFrom ## TYPE ## For ## STRATEGY(pRas, \
                                                                 DstPix, \
                                                                 dst); \
                        dstA = MultiplyAlphaFor ## STRATEGY(dstF, dstA); \
                        if (!(TYPE ## IsPremultiplied)) { \
                            dstF = dstA; \
                        } \
                        resA += dstA; \
                        if (dstF) { \
                            DeclareCompVarsFor ## STRATEGY(tmp) \
                            Postload ## STRATEGY ## From ## TYPE(pRas, \
                                                                 DstPix, \
                                                                 tmp); \
                            if (dstF != MaxValFor ## STRATEGY) { \
                                MultiplyAndStore ## STRATEGY ## Comps(tmp, \
                                                                      dstF, \
                                                                      tmp); \
                            } \
                            Store ## STRATEGY ## CompsUsingOp(res, +=, tmp); \
                        } \
                    } \
                    if (!(TYPE ## IsOpaque) && \
                        !(TYPE ## IsPremultiplied) && resA && \
                        resA < MaxValFor ## STRATEGY) \
                    { \
                        DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
                    } \
                    Store ## TYPE ## From ## STRATEGY ## Comps(pRas, \
                                                               DstWrite, 0, \
                                                               res); \
                } \
                pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                Next ## TYPE ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pRas = PtrAddBytes(pRas, rasScan); \
            Next ## TYPE ## StoreVarsY(DstWrite); \
            pMask = PtrAddBytes(pMask, maskScan); \
        } while (--height > 0); \
    } else /* pMask == 0 */ { \
        do { \
            jint w = width; \
            Init ## TYPE ## StoreVarsX(DstWrite, pRasInfo); \
            do { \
                DeclareAlphaVarFor ## STRATEGY(resA) \
                DeclareCompVarsFor ## STRATEGY(res) \
                DeclareAndInvertAlphaVarFor ## STRATEGY(dstF, srcA) \
\
                LoadAlphaFrom ## TYPE ## For ## STRATEGY(pRas, DstPix, res);\
                resA = MultiplyAlphaFor ## STRATEGY(dstF, resA); \
                if (!(TYPE ## IsPremultiplied)) { \
                    dstF = resA; \
                } \
                resA += srcA; \
                Postload ## STRATEGY ## From ## TYPE(pRas, DstPix, res); \
                MultiplyAddAndStore ## STRATEGY ## Comps(res, \
                                                         dstF, res, src); \
                if (!(TYPE ## IsOpaque) && \
                    !(TYPE ## IsPremultiplied) && resA && \
                    resA < MaxValFor ## STRATEGY) \
                { \
                    DivideAndStore ## STRATEGY ## Comps(res, res, resA); \
                } \
                Store ## TYPE ## From ## STRATEGY ## Comps(pRas, DstWrite, \
                                                           0, res); \
                pRas = PtrAddBytes(pRas, TYPE ## PixelStride); \
                Next ## TYPE ## StoreVarsX(DstWrite); \
            } while (--w > 0); \
            pRas = PtrAddBytes(pRas, rasScan); \
            Next ## TYPE ## StoreVarsY(DstWrite); \
        } while (--height > 0); \
    } \
}


/*
 * The macros defined above use the following macro definitions supplied
 * for the various surface types to manipulate pixels and pixel data.
 * The surface-specific macros are typically supplied by header files
 * named after the SurfaceType name (eg. IntArgb.h, ByteGray.h, etc.).
 *
 * In the macro names in the following definitions, the string <stype>
 * is used as a place holder for the SurfaceType name (eg. IntArgb).  The
 * string <strategy> is a place holder for the strategy name (eg. 4ByteArgb).
 * The macros above access these type specific macros using the ANSI
 * CPP token concatenation operator "##".
 *
 * Declare<stype>AlphaLoadData       Declare the variables used when an alpha
 *                                   value is pre-fetched to see whether or
 *                                   not blending needs to occur
 * Init<stype>AlphaLoadData          Initialize the aforementioned variables
 * LoadAlphaFrom<stype>For<strategy> Load the alpha value for the given pixel
 *                                   into a variable used later (the strategy
 *                                   type determines the bit depth of the
 *                                   alpha value)
 * Postload<strategy>From<stype>     Load the pixel components from the given
 *                                   surface type into the form required by
 *                                   the given strategy.  Typically there will
 *                                   be a couple macros of this variety, one
 *                                   for 4ByteArgb, one for 1ByteGray, one
 *                                   for 1ShortGray, etc.  Its code is only
 *                                   executed when blending needs to occur.
 *
 * <stype>IsPremultiplied            Constant specifying whether the pixel
 *                                   components have been premultiplied with
 *                                   the alpha value
 * Declare<stype>BlendFillVars       Declare the variables used when alpha
 *                                   blending need not occur (mask and source
 *                                   pixel are opaque)
 * Clear<stype>BlendFillVars         Clear the variables used in a no-blend
 *                                   situation (may modify argb argument)
 * Init<stype>BlendFillVarsNonPre    Initialize the variables used for a
 *                                   no-blending situation (this macro is for
 *                                   surfaces that do not have premultiplied
 *                                   components) (may modify argb argument)
 * Init<stype>BlendFillVarsPre       Initialize the variables used for a
 *                                   no-blending situation (this macro is for
 *                                   surfaces that have premultiplied
 *                                   components) (may modify argb argument)
 * Store<stype>BlendFill             Simply store the pixel for the given
 *                                   surface (used when blending is
 *                                   unnecessary)
 * Store<stype>From<strategy>Comps   Store the pixel for the given surface
 *                                   type after converting it from a pixel of
 *                                   the given strategy
 */

#endif /* AlphaMacros_h_Included */
