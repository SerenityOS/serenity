/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef LoopMacros_h_Included
#define LoopMacros_h_Included

#include "j2d_md.h"

#include "LineUtils.h"

/*
 * This file contains macros to aid in defining native graphics
 * primitive functions.
 *
 * A number of useful building block macros are defined, but the
 * vast majority of primitives are defined completely by a single
 * macro expansion which uses macro names in the argument list to
 * choose not only from a small number of strategies but also to
 * choose macro packages specific to the source and destination
 * pixel formats - greatly simplifying all aspects of creating
 * a new loop.
 *
 * See the following macros which define entire functions with
 * just one or two surface names and sometimes a strategy name:
 *     DEFINE_ISOCOPY_BLIT(ANYTYPE)
 *     DEFINE_ISOXOR_BLIT(ANYTYPE)
 *     DEFINE_CONVERT_BLIT(SRC, DST, CONV_METHOD)
 *     DEFINE_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY)
 *     DEFINE_XPAR_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY)
 *     DEFINE_XPAR_BLITBG_LUT(SRC, DST, LUT_STRATEGY)
 *     DEFINE_SOLID_FILLRECT(DST)
 *     DEFINE_SOLID_FILLSPANS(DST)
 *     DEFINE_SOLID_DRAWLINE(DST)
 *
 * Many of these loop macros take the name of a SurfaceType as
 * an argument and use the ANSI CPP token concatenation operator
 * "##" to reference macro and type definitions that are specific
 * to that type of surface.
 *
 * A description of the various surface specific macro utilities
 * that are used by these loop macros appears at the end of the
 * file.  The definitions of these surface-specific macros will
 * usually appear in a header file named after the SurfaceType
 * name (i.e. IntArgb.h, ByteGray.h, etc.).
 */

/*
 * This loop is the standard "while (--height > 0)" loop used by
 * some of the blits below.
 */
#define BlitLoopHeight(SRCTYPE, SRCPTR, SRCBASE, SRCINFO, \
                       DSTTYPE, DSTPTR, DSTBASE, DSTINFO, DSTPREFIX, \
                       HEIGHT, BODY) \
    do { \
        SRCTYPE ## DataType *SRCPTR = (SRCTYPE ## DataType *) (SRCBASE); \
        DSTTYPE ## DataType *DSTPTR = (DSTTYPE ## DataType *) (DSTBASE); \
        jint srcScan = (SRCINFO)->scanStride; \
        jint dstScan = (DSTINFO)->scanStride; \
        Init ## DSTTYPE ## StoreVarsY(DSTPREFIX, DSTINFO); \
        do { \
            BODY; \
            SRCPTR = PtrAddBytes(SRCPTR, srcScan); \
            DSTPTR = PtrAddBytes(DSTPTR, dstScan); \
            Next ## DSTTYPE ## StoreVarsY(DSTPREFIX); \
        } while (--HEIGHT > 0); \
    } while (0)

/*
 * This loop is the standard nested "while (--width/height > 0)" loop
 * used by most of the basic blits below.
 */
#define BlitLoopWidthHeight(SRCTYPE, SRCPTR, SRCBASE, SRCINFO, \
                            DSTTYPE, DSTPTR, DSTBASE, DSTINFO, DSTPREFIX, \
                            WIDTH, HEIGHT, BODY) \
    do { \
        SRCTYPE ## DataType *SRCPTR = (SRCTYPE ## DataType *) (SRCBASE); \
        DSTTYPE ## DataType *DSTPTR = (DSTTYPE ## DataType *) (DSTBASE); \
        jint srcScan = (SRCINFO)->scanStride; \
        jint dstScan = (DSTINFO)->scanStride; \
        Init ## DSTTYPE ## StoreVarsY(DSTPREFIX, DSTINFO); \
        srcScan -= (WIDTH) * SRCTYPE ## PixelStride; \
        dstScan -= (WIDTH) * DSTTYPE ## PixelStride; \
        do { \
            juint w = WIDTH; \
            Init ## DSTTYPE ## StoreVarsX(DSTPREFIX, DSTINFO); \
            do { \
                BODY; \
                SRCPTR = PtrAddBytes(SRCPTR, SRCTYPE ## PixelStride); \
                DSTPTR = PtrAddBytes(DSTPTR, DSTTYPE ## PixelStride); \
                Next ## DSTTYPE ## StoreVarsX(DSTPREFIX); \
            } while (--w > 0); \
            SRCPTR = PtrAddBytes(SRCPTR, srcScan); \
            DSTPTR = PtrAddBytes(DSTPTR, dstScan); \
            Next ## DSTTYPE ## StoreVarsY(DSTPREFIX); \
        } while (--HEIGHT > 0); \
    } while (0)

/*
 * This loop is the standard nested "while (--width/height > 0)" loop
 * used by most of the scaled blits below.  It calculates the proper
 * X source variable
 */
#define BlitLoopScaleWidthHeight(SRCTYPE, SRCPTR, SRCBASE, SRCINFO, \
                                 DSTTYPE, DSTPTR, DSTBASE, DSTINFO, DSTPREFIX, \
                                 XVAR, WIDTH, HEIGHT, \
                                 SXLOC, SYLOC, SXINC, SYINC, SHIFT, \
                                 BODY) \
    do { \
        SRCTYPE ## DataType *SRCPTR; \
        DSTTYPE ## DataType *DSTPTR = (DSTTYPE ## DataType *) (DSTBASE); \
        jint srcScan = (SRCINFO)->scanStride; \
        jint dstScan = (DSTINFO)->scanStride; \
        Init ## DSTTYPE ## StoreVarsY(DSTPREFIX, DSTINFO); \
        dstScan -= (WIDTH) * DSTTYPE ## PixelStride; \
        do { \
            juint w = WIDTH; \
            jint tmpsxloc = SXLOC; \
            SRCPTR = PtrPixelsRow(SRCBASE, (SYLOC >> SHIFT), srcScan); \
            Init ## DSTTYPE ## StoreVarsX(DSTPREFIX, DSTINFO); \
            do { \
                jint XVAR = (tmpsxloc >> SHIFT); \
                BODY; \
                DSTPTR = PtrAddBytes(DSTPTR, DSTTYPE ## PixelStride); \
                Next ## DSTTYPE ## StoreVarsX(DSTPREFIX); \
                tmpsxloc += SXINC; \
            } while (--w > 0); \
            DSTPTR = PtrAddBytes(DSTPTR, dstScan); \
            Next ## DSTTYPE ## StoreVarsY(DSTPREFIX); \
            SYLOC += SYINC; \
        } while (--HEIGHT > 0); \
    } while (0)

/*
 * This loop is a standard horizontal loop iterating with a "relative"
 * X coordinate (0 <= X < WIDTH) used primarily by the LUT conversion
 * preprocessing loops below.
 */
#define BlitLoopXRel(DSTTYPE, DSTINFO, DSTPREFIX, \
                     XVAR, WIDTH, BODY) \
    do { \
        juint XVAR = 0; \
        Init ## DSTTYPE ## StoreVarsX(DSTPREFIX, DSTINFO); \
        do { \
            BODY; \
            Next ## DSTTYPE ## StoreVarsX(DSTPREFIX); \
        } while (++XVAR < WIDTH); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via an intermediate "jint rgb" format.
 */
#define ConvertVia1IntRgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                          DSTPTR, DSTTYPE, DSTPREFIX, \
                          SXVAR, DXVAR) \
    do { \
        int rgb; \
        Load ## SRCTYPE ## To1IntRgb(SRCPTR, SRCPREFIX, SXVAR, rgb); \
        Store ## DSTTYPE ## From1IntRgb(DSTPTR, DSTPREFIX, DXVAR, rgb); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via an intermediate "jint argb" format.
 */
#define ConvertVia1IntArgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                           DSTPTR, DSTTYPE, DSTPREFIX, \
                           SXVAR, DXVAR) \
    do { \
        int argb; \
        Load ## SRCTYPE ## To1IntArgb(SRCPTR, SRCPREFIX, SXVAR, argb); \
        Store ## DSTTYPE ## From1IntArgb(DSTPTR, DSTPREFIX, DXVAR, argb); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via an intermediate set of 3 component variables "jint r, g, b".
 */
#define ConvertVia3ByteRgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                           DSTPTR, DSTTYPE, DSTPREFIX, \
                           SXVAR, DXVAR) \
    do { \
        jint r, g, b; \
        Load ## SRCTYPE ## To3ByteRgb(SRCPTR, SRCPREFIX, SXVAR, r, g, b); \
        Store ## DSTTYPE ## From3ByteRgb(DSTPTR, DSTPREFIX, DXVAR, r, g, b); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via an intermediate set of 4 component variables "jint a, r, g, b".
 */
#define ConvertVia4ByteArgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                            DSTPTR, DSTTYPE, DSTPREFIX, \
                            SXVAR, DXVAR) \
    do { \
        jint a, r, g, b; \
        Load ## SRCTYPE ## To4ByteArgb(SRCPTR, SRCPREFIX, SXVAR, a, r, g, b); \
        Store ## DSTTYPE ## From4ByteArgb(DSTPTR, DSTPREFIX, DXVAR, \
                                          a, r, g, b); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via an intermediate "jint gray" format.
 */
#define ConvertVia1ByteGray(SRCPTR, SRCTYPE, SRCPREFIX, \
                            DSTPTR, DSTTYPE, DSTPREFIX, \
                            SXVAR, DXVAR) \
    do { \
        jint gray; \
        Load ## SRCTYPE ## To1ByteGray(SRCPTR, SRCPREFIX, SXVAR, gray); \
        Store ## DSTTYPE ## From1ByteGray(DSTPTR, DSTPREFIX, DXVAR, gray); \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_XPAR_CONVERT_BLIT
 * macros.  It converts from the source pixel format to the destination
 * via the specified intermediate format while testing for transparent pixels.
 */
#define ConvertXparVia1IntRgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                              DSTPTR, DSTTYPE, DSTPREFIX, \
                              SXVAR, DXVAR) \
    do { \
        Declare ## SRCTYPE ## Data(XparLoad); \
        Load ## SRCTYPE ## Data(SRCPTR, SRCPREFIX, SXVAR, XparLoad); \
        if (! (Is ## SRCTYPE ## DataTransparent(XparLoad))) { \
            int rgb; \
            Convert ## SRCTYPE ## DataTo1IntRgb(XparLoad, rgb); \
            Store ## DSTTYPE ## From1IntRgb(DSTPTR, DSTPREFIX, DXVAR, rgb); \
        } \
    } while (0)

/*
 * This is a "conversion strategy" for use with the DEFINE_XPAR_BLITBG
 * macros.  It converts from the source pixel format to the destination
 * via the specified intermediate format while substituting the specified
 * bgcolor for transparent pixels.
 */
#define BgCopyXparVia1IntRgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                             DSTPTR, DSTTYPE, DSTPREFIX, \
                             SXVAR, DXVAR, BGPIXEL, BGPREFIX) \
    do { \
        Declare ## SRCTYPE ## Data(XparLoad); \
        Load ## SRCTYPE ## Data(SRCPTR, SRCPREFIX, SXVAR, XparLoad); \
        if (Is ## SRCTYPE ## DataTransparent(XparLoad)) { \
            Store ## DSTTYPE ## PixelData(DSTPTR, DXVAR, BGPIXEL, BGPREFIX); \
        } else { \
            int rgb; \
            Convert ## SRCTYPE ## DataTo1IntRgb(XparLoad, rgb); \
            Store ## DSTTYPE ## From1IntRgb(DSTPTR, DSTPREFIX, DXVAR, rgb); \
        } \
    } while (0)

/*
 * This macro determines whether or not the given pixel is considered
 * "transparent" for XOR purposes.  The ARGB pixel is considered
 * "transparent" if the alpha value is < 0.5.
 */
#define IsArgbTransparent(pixel) \
    (((jint) pixel) >= 0)

/*
 * This is a "conversion strategy" for use with the DEFINE_XOR_BLIT macro.  It
 * converts the source pixel to an intermediate ARGB value and then converts
 * the ARGB value to the pixel representation for the destination surface.  It
 * then XORs the srcpixel, xorpixel, and destination pixel together and stores
 * the result in the destination surface.
 */
#define XorVia1IntArgb(SRCPTR, SRCTYPE, SRCPREFIX, \
                       DSTPTR, DSTTYPE, DSTANYTYPE, \
                       XVAR, XORPIXEL, XORPREFIX, \
                       MASK, MASKPREFIX, DSTINFOPTR) \
    do { \
        jint srcpixel; \
        Declare ## DSTANYTYPE ## PixelData(pix) \
        Load ## SRCTYPE ## To1IntArgb(SRCPTR, SRCPREFIX, XVAR, srcpixel); \
 \
        if (IsArgbTransparent(srcpixel)) { \
            break; \
        } \
 \
        DSTTYPE ## PixelFromArgb(srcpixel, srcpixel, DSTINFOPTR); \
 \
        Extract ## DSTANYTYPE ## PixelData(srcpixel, pix); \
        Xor ## DSTANYTYPE ## PixelData(srcpixel, pix, DSTPTR, XVAR, \
                                       XORPIXEL, XORPREFIX, \
                                       MASK, MASKPREFIX); \
    } while (0)

/*
 * "LUT_STRATEGY" macro sets.
 *
 * There are 2 major strategies for dealing with luts and 3
 * implementations of those strategies.
 *
 * The 2 strategies are "PreProcessLut" and "ConvertOnTheFly".
 *
 * For the "PreProcessLut" strategy, the raw ARGB lut supplied
 * by the SD_LOCK_LUT flag is converted at the beginning into a
 * form that is more suited for storing into the destination
 * pixel format.  The inner loop consists of a series of table
 * lookups with very little conversion from that intermediate
 * pixel format.
 *
 * For the "ConvertOnTheFly" strategy, the raw ARGB values are
 * converted on a pixel by pixel basis in the inner loop itself.
 * This strategy is most useful for formats which tend to use
 * the ARGB color format as their pixel format also.
 *
 * Each of these strategies has 3 implementations which are needed
 * for the special cases:
 * - straight conversion (invoked from DEFINE_CONVERT_BLIT_LUT)
 * - straight conversion with transparency handling (invoked from
 *   DEFINE_XPAR_CONVERT_BLIT_LUT)
 * - straight conversion with a bgcolor for the transparent pixels
 *   (invoked from DEFINE_XPAR_BLITBG_LUT)
 */

/***
 * Start of PreProcessLut strategy macros, CONVERT_BLIT implementation.
 */
#define LutSize(TYPE) \
    (1 << TYPE ## BitsPerPixel)

#define DeclarePreProcessLutLut(SRC, DST, PIXLUT) \
    DST ## PixelType PIXLUT[LutSize(SRC)];

#define SetupPreProcessLutLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO) \
    do { \
        jint *srcLut = (SRCINFO)->lutBase; \
        juint lutSize = (SRCINFO)->lutSize; \
        Declare ## DST ## StoreVars(PreLut) \
        Init ## DST ## StoreVarsY(PreLut, DSTINFO); \
        if (lutSize >= LutSize(SRC)) { \
            lutSize = LutSize(SRC); \
        } else { \
            DST ## PixelType *pPIXLUT = &PIXLUT[lutSize]; \
            do { \
                Store ## DST ## From1IntArgb(pPIXLUT, PreLut, 0, 0); \
            } while (++pPIXLUT < &PIXLUT[LutSize(SRC)]); \
        } \
        BlitLoopXRel(DST, DSTINFO, PreLut, x, lutSize, \
                     do { \
                         jint argb = srcLut[x]; \
                         Store ## DST ## From1IntArgb(PIXLUT, PreLut, x, argb); \
                     } while (0)); \
    } while (0)

#define BodyPreProcessLutLut(SRCPTR, SRCTYPE, PIXLUT, \
                             DSTPTR, DSTTYPE, DSTPREFIX, \
                             SXVAR, DXVAR) \
    DSTPTR[DXVAR] = PIXLUT[SRCPTR[SXVAR]]

/*
 * End of PreProcessLut/CONVERT_BLIT macros.
 ***/

/***
 * Start of ConvertOnTheFly strategy macros, CONVERT_BLIT implementation.
 */
#define DeclareConvertOnTheFlyLut(SRC, DST, PIXLUT) \
    Declare ## SRC ## LoadVars(PIXLUT)

#define SetupConvertOnTheFlyLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO) \
    Init ## SRC ## LoadVars(PIXLUT, SRCINFO)

#define BodyConvertOnTheFlyLut(SRCPTR, SRCTYPE, PIXLUT, \
                               DSTPTR, DSTTYPE, DSTPREFIX, \
                               SXVAR, DXVAR) \
    ConvertVia1IntArgb(SRCPTR, SRCTYPE, PIXLUT, \
                       DSTPTR, DSTTYPE, DSTPREFIX, \
                       SXVAR, DXVAR)

/*
 * End of ConvertOnTheFly/CONVERT_BLIT macros.
 ***/

/***
 * Start of PreProcessLut strategy macros, XPAR_CONVERT_BLIT implementation.
 */
#define DeclarePreProcessLutXparLut(SRC, DST, PIXLUT) \
    jint PIXLUT[LutSize(SRC)];

#define SetupPreProcessLutXparLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO) \
    do { \
        jint *srcLut = (SRCINFO)->lutBase; \
        juint lutSize = (SRCINFO)->lutSize; \
        Declare ## DST ## StoreVars(PreLut) \
        Init ## DST ## StoreVarsY(PreLut, DSTINFO); \
        if (lutSize >= LutSize(SRC)) { \
            lutSize = LutSize(SRC); \
        } else { \
            jint *pPIXLUT = &PIXLUT[lutSize]; \
            do { \
                pPIXLUT[0] = DST ## XparLutEntry; \
            } while (++pPIXLUT < &PIXLUT[LutSize(SRC)]); \
        } \
        BlitLoopXRel(DST, DSTINFO, PreLut, x, lutSize, \
                     do { \
                         jint argb = srcLut[x]; \
                         if (argb < 0) { \
                             Store ## DST ## NonXparFromArgb \
                                 (PIXLUT, PreLut, x, argb); \
                         } else { \
                             PIXLUT[x] = DST ## XparLutEntry; \
                         } \
                     } while (0)); \
    } while (0)

#define BodyPreProcessLutXparLut(SRCPTR, SRCTYPE, PIXLUT, \
                                 DSTPTR, DSTTYPE, DSTPREFIX, \
                                 SXVAR, DXVAR) \
    do { \
        jint pix = PIXLUT[SRCPTR[SXVAR]]; \
        if (! DSTTYPE ## IsXparLutEntry(pix)) { \
            DSTPTR[DXVAR] = (DSTTYPE ## PixelType) pix; \
        } \
    } while (0)

/*
 * End of PreProcessLut/XPAR_CONVERT_BLIT macros.
 ***/

/***
 * Start of ConvertOnTheFly strategy macros, CONVERT_BLIT implementation.
 */
#define DeclareConvertOnTheFlyXparLut(SRC, DST, PIXLUT) \
    Declare ## SRC ## LoadVars(PIXLUT)

#define SetupConvertOnTheFlyXparLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO) \
    Init ## SRC ## LoadVars(PIXLUT, SRCINFO)

#define BodyConvertOnTheFlyXparLut(SRCPTR, SRCTYPE, PIXLUT, \
                                   DSTPTR, DSTTYPE, DSTPREFIX, \
                                   SXVAR, DXVAR) \
    do { \
        jint argb; \
        Load ## SRCTYPE ## To1IntArgb(SRCPTR, PIXLUT, SXVAR, argb); \
        if (argb < 0) { \
            Store ## DSTTYPE ## From1IntArgb(DSTPTR, DSTPREFIX, DXVAR, argb); \
        } \
    } while (0)

/*
 * End of ConvertOnTheFly/CONVERT_BLIT macros.
 ***/

/***
 * Start of PreProcessLut strategy macros, BLITBG implementation.
 */
#define DeclarePreProcessLutBgLut(SRC, DST, PIXLUT) \
    jint PIXLUT[LutSize(SRC)];

#define SetupPreProcessLutBgLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO, BGPIXEL) \
    do { \
        jint *srcLut = (SRCINFO)->lutBase; \
        juint lutSize = (SRCINFO)->lutSize; \
        Declare ## DST ## StoreVars(PreLut) \
        Init ## DST ## StoreVarsY(PreLut, DSTINFO); \
        if (lutSize >= LutSize(SRC)) { \
            lutSize = LutSize(SRC); \
        } else { \
            jint *pPIXLUT = &PIXLUT[lutSize]; \
            do { \
                pPIXLUT[0] = BGPIXEL; \
            } while (++pPIXLUT < &PIXLUT[LutSize(SRC)]); \
        } \
        BlitLoopXRel(DST, DSTINFO, PreLut, x, lutSize, \
                     do { \
                         jint argb = srcLut[x]; \
                         if (argb < 0) { \
                             Store ## DST ## From1IntArgb(PIXLUT, PreLut, \
                                                          x, argb); \
                         } else { \
                             PIXLUT[x] = BGPIXEL; \
                         } \
                     } while (0)); \
    } while (0)

#define BodyPreProcessLutBgLut(SRCPTR, SRCTYPE, PIXLUT, \
                               DSTPTR, DSTTYPE, DSTPREFIX, \
                               SXVAR, DXVAR, BGPIXEL) \
    do { \
        jint pix = PIXLUT[SRCPTR[SXVAR]]; \
        Store ## DSTTYPE ## Pixel(DSTPTR, DXVAR, pix); \
    } while (0)

/*
 * End of PreProcessLut/BLITBG implementation.
 ***/

/***
 * Start of ConvertOnTheFly strategy macros, BLITBG implementation.
 */
#define DeclareConvertOnTheFlyBgLut(SRC, DST, PIXLUT) \
    Declare ## SRC ## LoadVars(PIXLUT) \
    Declare ## DST ## PixelData(bgpix);

#define SetupConvertOnTheFlyBgLut(SRC, DST, PIXLUT, SRCINFO, DSTINFO, BGPIXEL) \
    do { \
        Init ## SRC ## LoadVars(PIXLUT, SRCINFO); \
        Extract ## DST ## PixelData(BGPIXEL, bgpix); \
    } while (0)

#define BodyConvertOnTheFlyBgLut(SRCPTR, SRCTYPE, PIXLUT, \
                                 DSTPTR, DSTTYPE, DSTPREFIX, \
                                 SXVAR, DXVAR, BGPIXEL) \
    do { \
        jint argb; \
        Load ## SRCTYPE ## To1IntArgb(SRCPTR, PIXLUT, SXVAR, argb); \
        if (argb < 0) { \
            Store ## DSTTYPE ## From1IntArgb(DSTPTR, DSTPREFIX, DXVAR, argb); \
        } else { \
            Store ## DSTTYPE ## PixelData(DSTPTR, DXVAR, BGPIXEL, bgpix); \
        } \
    } while (0)

/*
 * End of ConvertOnTheFly/BLITBG macros.
 ***/

/*
 * These macros provide consistent naming conventions for the
 * various types of native primitive inner loop functions.
 * The names are mechanically constructed from the SurfaceType names.
 */
#define NAME_CONVERT_BLIT(SRC, DST)      SRC ## To ## DST ## Convert

#define NAME_SCALE_BLIT(SRC, DST)        SRC ## To ## DST ## ScaleConvert

#define NAME_XPAR_CONVERT_BLIT(SRC, DST) SRC ## To ## DST ## XparOver

#define NAME_XPAR_SCALE_BLIT(SRC, DST)   SRC ## To ## DST ## ScaleXparOver

#define NAME_XPAR_BLITBG(SRC, DST)       SRC ## To ## DST ## XparBgCopy

#define NAME_XOR_BLIT(SRC, DST)          SRC ## To ## DST ## XorBlit

#define NAME_ISOCOPY_BLIT(ANYTYPE)       ANYTYPE ## IsomorphicCopy

#define NAME_ISOSCALE_BLIT(ANYTYPE)      ANYTYPE ## IsomorphicScaleCopy

#define NAME_ISOXOR_BLIT(ANYTYPE)        ANYTYPE ## IsomorphicXorCopy

#define NAME_SOLID_FILLRECT(TYPE)        TYPE ## SetRect

#define NAME_SOLID_FILLSPANS(TYPE)       TYPE ## SetSpans

#define NAME_SOLID_DRAWLINE(TYPE)        TYPE ## SetLine

#define NAME_XOR_FILLRECT(TYPE)          TYPE ## XorRect

#define NAME_XOR_FILLSPANS(TYPE)         TYPE ## XorSpans

#define NAME_XOR_DRAWLINE(TYPE)          TYPE ## XorLine

#define NAME_SRC_MASKFILL(TYPE)          TYPE ## SrcMaskFill

#define NAME_SRCOVER_MASKFILL(TYPE)      TYPE ## SrcOverMaskFill

#define NAME_ALPHA_MASKFILL(TYPE)        TYPE ## AlphaMaskFill

#define NAME_SRCOVER_MASKBLIT(SRC, DST)  SRC ## To ## DST ## SrcOverMaskBlit

#define NAME_ALPHA_MASKBLIT(SRC, DST)    SRC ## To ## DST ## AlphaMaskBlit

#define NAME_SOLID_DRAWGLYPHLIST(TYPE)   TYPE ## DrawGlyphList

#define NAME_SOLID_DRAWGLYPHLISTAA(TYPE) TYPE ## DrawGlyphListAA

#define NAME_SOLID_DRAWGLYPHLISTLCD(TYPE) TYPE ## DrawGlyphListLCD

#define NAME_XOR_DRAWGLYPHLIST(TYPE)     TYPE ## DrawGlyphListXor

#define NAME_TRANSFORMHELPER(TYPE, MODE) TYPE ## MODE ## TransformHelper

#define NAME_TRANSFORMHELPER_NN(TYPE)    NAME_TRANSFORMHELPER(TYPE, NrstNbr)
#define NAME_TRANSFORMHELPER_BL(TYPE)    NAME_TRANSFORMHELPER(TYPE, Bilinear)
#define NAME_TRANSFORMHELPER_BC(TYPE)    NAME_TRANSFORMHELPER(TYPE, Bicubic)

#define NAME_TRANSFORMHELPER_FUNCS(TYPE) TYPE ## TransformHelperFuncs

#define NAME_SOLID_FILLPGRAM(TYPE)       TYPE ## SetParallelogram
#define NAME_SOLID_PGRAM_FUNCS(TYPE)     TYPE ## SetParallelogramFuncs

#define NAME_XOR_FILLPGRAM(TYPE)         TYPE ## XorParallelogram
#define NAME_XOR_PGRAM_FUNCS(TYPE)       TYPE ## XorParallelogramFuncs

/*
 * These macros conveniently name and declare the indicated native
 * primitive loop function for forward referencing.
 */
#define DECLARE_CONVERT_BLIT(SRC, DST) \
    BlitFunc NAME_CONVERT_BLIT(SRC, DST)

#define DECLARE_SCALE_BLIT(SRC, DST) \
    ScaleBlitFunc NAME_SCALE_BLIT(SRC, DST)

#define DECLARE_XPAR_CONVERT_BLIT(SRC, DST) \
    BlitFunc NAME_XPAR_CONVERT_BLIT(SRC, DST)

#define DECLARE_XPAR_SCALE_BLIT(SRC, DST) \
    ScaleBlitFunc NAME_XPAR_SCALE_BLIT(SRC, DST)

#define DECLARE_XPAR_BLITBG(SRC, DST) \
    BlitBgFunc NAME_XPAR_BLITBG(SRC, DST)

#define DECLARE_XOR_BLIT(SRC, DST) \
    BlitFunc NAME_XOR_BLIT(SRC, DST)

#define DECLARE_ISOCOPY_BLIT(ANYTYPE) \
    BlitFunc NAME_ISOCOPY_BLIT(ANYTYPE)

#define DECLARE_ISOSCALE_BLIT(ANYTYPE) \
    ScaleBlitFunc NAME_ISOSCALE_BLIT(ANYTYPE)

#define DECLARE_ISOXOR_BLIT(ANYTYPE) \
    BlitFunc NAME_ISOXOR_BLIT(ANYTYPE)

#define DECLARE_SOLID_FILLRECT(TYPE) \
    FillRectFunc NAME_SOLID_FILLRECT(TYPE)

#define DECLARE_SOLID_FILLSPANS(TYPE) \
    FillSpansFunc NAME_SOLID_FILLSPANS(TYPE)

#define DECLARE_SOLID_DRAWLINE(TYPE) \
    DrawLineFunc NAME_SOLID_DRAWLINE(TYPE)

#define DECLARE_XOR_FILLRECT(TYPE) \
    FillRectFunc NAME_XOR_FILLRECT(TYPE)

#define DECLARE_XOR_FILLSPANS(TYPE) \
    FillSpansFunc NAME_XOR_FILLSPANS(TYPE)

#define DECLARE_XOR_DRAWLINE(TYPE) \
    DrawLineFunc NAME_XOR_DRAWLINE(TYPE)

#define DECLARE_ALPHA_MASKFILL(TYPE) \
    MaskFillFunc NAME_ALPHA_MASKFILL(TYPE)

#define DECLARE_SRC_MASKFILL(TYPE) \
    MaskFillFunc NAME_SRC_MASKFILL(TYPE)

#define DECLARE_SRCOVER_MASKFILL(TYPE) \
    MaskFillFunc NAME_SRCOVER_MASKFILL(TYPE)

#define DECLARE_SRCOVER_MASKBLIT(SRC, DST) \
    MaskBlitFunc NAME_SRCOVER_MASKBLIT(SRC, DST)

#define DECLARE_ALPHA_MASKBLIT(SRC, DST) \
    MaskBlitFunc NAME_ALPHA_MASKBLIT(SRC, DST)

#define DECLARE_SOLID_DRAWGLYPHLIST(TYPE) \
    DrawGlyphListFunc NAME_SOLID_DRAWGLYPHLIST(TYPE)

#define DECLARE_SOLID_DRAWGLYPHLISTAA(TYPE) \
    DrawGlyphListAAFunc NAME_SOLID_DRAWGLYPHLISTAA(TYPE)

#define DECLARE_SOLID_DRAWGLYPHLISTLCD(TYPE) \
    DrawGlyphListLCDFunc NAME_SOLID_DRAWGLYPHLISTLCD(TYPE)

#define DECLARE_XOR_DRAWGLYPHLIST(TYPE) \
    DrawGlyphListFunc NAME_XOR_DRAWGLYPHLIST(TYPE)

#define DECLARE_TRANSFORMHELPER_FUNCS(TYPE) \
    TransformHelperFunc NAME_TRANSFORMHELPER_NN(TYPE); \
    TransformHelperFunc NAME_TRANSFORMHELPER_BL(TYPE); \
    TransformHelperFunc NAME_TRANSFORMHELPER_BC(TYPE); \
    TransformHelperFuncs NAME_TRANSFORMHELPER_FUNCS(TYPE)

#define DECLARE_SOLID_PARALLELOGRAM(TYPE) \
    FillParallelogramFunc NAME_SOLID_FILLPGRAM(TYPE); \
    DECLARE_SOLID_DRAWLINE(TYPE); \
    DrawParallelogramFuncs NAME_SOLID_PGRAM_FUNCS(TYPE)

#define DECLARE_XOR_PARALLELOGRAM(TYPE) \
    FillParallelogramFunc NAME_XOR_FILLPGRAM(TYPE); \
    DECLARE_XOR_DRAWLINE(TYPE); \
    DrawParallelogramFuncs NAME_XOR_PGRAM_FUNCS(TYPE)

/*
 * These macros construct the necessary NativePrimitive structure
 * for the indicated native primitive loop function which will be
 * declared somewhere prior and defined elsewhere (usually after).
 */
#define REGISTER_CONVERT_BLIT(SRC, DST) \
    REGISTER_BLIT(SRC, SrcNoEa, DST, NAME_CONVERT_BLIT(SRC, DST))

#define REGISTER_CONVERT_BLIT_FLAGS(SRC, DST, SFLAGS, DFLAGS) \
    REGISTER_BLIT_FLAGS(SRC, SrcNoEa, DST, NAME_CONVERT_BLIT(SRC, DST), \
                        SFLAGS, DFLAGS)

#define REGISTER_CONVERT_BLIT_EQUIV(SRC, DST, FUNC) \
    REGISTER_BLIT(SRC, SrcNoEa, DST, FUNC)

#define REGISTER_SCALE_BLIT(SRC, DST) \
    REGISTER_SCALEBLIT(SRC, SrcNoEa, DST, NAME_SCALE_BLIT(SRC, DST))

#define REGISTER_SCALE_BLIT_FLAGS(SRC, DST, SFLAGS, DFLAGS) \
    REGISTER_SCALEBLIT_FLAGS(SRC, SrcNoEa, DST, NAME_SCALE_BLIT(SRC, DST), \
                             SFLAGS, DFLAGS)

#define REGISTER_SCALE_BLIT_EQUIV(SRC, DST, FUNC) \
    REGISTER_SCALEBLIT(SRC, SrcNoEa, DST, FUNC)

#define REGISTER_XPAR_CONVERT_BLIT(SRC, DST) \
    REGISTER_BLIT(SRC, SrcOverBmNoEa, DST, NAME_XPAR_CONVERT_BLIT(SRC, DST))

#define REGISTER_XPAR_CONVERT_BLIT_EQUIV(SRC, DST, FUNC) \
    REGISTER_BLIT(SRC, SrcOverBmNoEa, DST, FUNC)

#define REGISTER_XPAR_SCALE_BLIT(SRC, DST) \
    REGISTER_SCALEBLIT(SRC, SrcOverBmNoEa, DST, NAME_XPAR_SCALE_BLIT(SRC, DST))

#define REGISTER_XPAR_SCALE_BLIT_EQUIV(SRC, DST, FUNC) \
    REGISTER_SCALEBLIT(SRC, SrcOverBmNoEa, DST, FUNC)

#define REGISTER_XPAR_BLITBG(SRC, DST) \
    REGISTER_BLITBG(SRC, SrcNoEa, DST, NAME_XPAR_BLITBG(SRC, DST))

#define REGISTER_XPAR_BLITBG_EQUIV(SRC, DST, FUNC) \
    REGISTER_BLITBG(SRC, SrcNoEa, DST, FUNC)

#define REGISTER_XOR_BLIT(SRC, DST) \
    REGISTER_BLIT(SRC, Xor, DST, NAME_XOR_BLIT(SRC, DST))

#define REGISTER_ISOCOPY_BLIT(THISTYPE, ANYTYPE) \
    REGISTER_BLIT(THISTYPE, SrcNoEa, THISTYPE, NAME_ISOCOPY_BLIT(ANYTYPE))

#define REGISTER_ISOSCALE_BLIT(THISTYPE, ANYTYPE) \
    REGISTER_SCALEBLIT(THISTYPE, SrcNoEa, THISTYPE, NAME_ISOSCALE_BLIT(ANYTYPE))

#define REGISTER_ISOXOR_BLIT(THISTYPE, ANYTYPE) \
    REGISTER_BLIT(THISTYPE, Xor, THISTYPE, NAME_ISOXOR_BLIT(ANYTYPE))

#define REGISTER_SOLID_FILLRECT(TYPE) \
    REGISTER_FILLRECT(AnyColor, SrcNoEa, TYPE, NAME_SOLID_FILLRECT(TYPE))

#define REGISTER_SOLID_FILLSPANS(TYPE) \
    REGISTER_FILLSPANS(AnyColor, SrcNoEa, TYPE, NAME_SOLID_FILLSPANS(TYPE))

#define REGISTER_SOLID_LINE_PRIMITIVES(TYPE) \
    REGISTER_LINE_PRIMITIVES(AnyColor, SrcNoEa, TYPE, \
                             NAME_SOLID_DRAWLINE(TYPE))

#define REGISTER_XOR_FILLRECT(TYPE) \
    REGISTER_FILLRECT(AnyColor, Xor, TYPE, NAME_XOR_FILLRECT(TYPE))

#define REGISTER_XOR_FILLSPANS(TYPE) \
    REGISTER_FILLSPANS(AnyColor, Xor, TYPE, NAME_XOR_FILLSPANS(TYPE))

#define REGISTER_XOR_LINE_PRIMITIVES(TYPE) \
    REGISTER_LINE_PRIMITIVES(AnyColor, Xor, TYPE, NAME_XOR_DRAWLINE(TYPE))

#define REGISTER_ALPHA_MASKFILL(TYPE) \
    REGISTER_MASKFILL(AnyColor, AnyAlpha, TYPE, NAME_ALPHA_MASKFILL(TYPE))

#define REGISTER_SRC_MASKFILL(TYPE) \
    REGISTER_MASKFILL(AnyColor, Src, TYPE, NAME_SRC_MASKFILL(TYPE))

#define REGISTER_SRCOVER_MASKFILL(TYPE) \
    REGISTER_MASKFILL(AnyColor, SrcOver, TYPE, NAME_SRCOVER_MASKFILL(TYPE))

#define REGISTER_SRCOVER_MASKBLIT(SRC, DST) \
    REGISTER_MASKBLIT(SRC, SrcOver, DST, NAME_SRCOVER_MASKBLIT(SRC, DST))

#define REGISTER_ALPHA_MASKBLIT(SRC, DST) \
    REGISTER_MASKBLIT(SRC, AnyAlpha, DST, NAME_ALPHA_MASKBLIT(SRC, DST))

#define REGISTER_SOLID_DRAWGLYPHLIST(TYPE) \
    REGISTER_DRAWGLYPHLIST(AnyColor, SrcNoEa, TYPE, \
                           NAME_SOLID_DRAWGLYPHLIST(TYPE))

#define REGISTER_SOLID_DRAWGLYPHLISTAA(TYPE) \
    REGISTER_DRAWGLYPHLISTAA(AnyColor, SrcNoEa, TYPE, \
                             NAME_SOLID_DRAWGLYPHLISTAA(TYPE))

#define REGISTER_SOLID_DRAWGLYPHLISTLCD(TYPE) \
    REGISTER_DRAWGLYPHLISTLCD(AnyColor, SrcNoEa, TYPE, \
                             NAME_SOLID_DRAWGLYPHLISTLCD(TYPE))

#define REGISTER_XOR_DRAWGLYPHLIST(TYPE) \
    REGISTER_DRAWGLYPHLIST(AnyColor, Xor, TYPE, \
                           NAME_XOR_DRAWGLYPHLIST(TYPE)), \
    REGISTER_DRAWGLYPHLISTAA(AnyColor, Xor, TYPE, \
                             NAME_XOR_DRAWGLYPHLIST(TYPE))

#define REGISTER_TRANSFORMHELPER_FUNCS(TYPE) \
    REGISTER_PRIMITIVE(TransformHelper, TYPE, SrcNoEa, IntArgbPre, \
                       (AnyFunc *) &NAME_TRANSFORMHELPER_FUNCS(TYPE))

#define REGISTER_SOLID_PARALLELOGRAM(TYPE) \
    REGISTER_PRIMITIVE(FillParallelogram, AnyColor, SrcNoEa, TYPE, \
                       NAME_SOLID_FILLPGRAM(TYPE)), \
    REGISTER_PRIMITIVE(DrawParallelogram, AnyColor, SrcNoEa, TYPE, \
                       (AnyFunc *) &NAME_SOLID_PGRAM_FUNCS(TYPE))

#define REGISTER_XOR_PARALLELOGRAM(TYPE) \
    REGISTER_PRIMITIVE(FillParallelogram, AnyColor, Xor, TYPE, \
                       NAME_XOR_FILLPGRAM(TYPE)), \
    REGISTER_PRIMITIVE(DrawParallelogram, AnyColor, Xor, TYPE, \
                       (AnyFunc *) &NAME_XOR_PGRAM_FUNCS(TYPE))

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for copying pixels of a common type from one buffer to another.
 */
#define DEFINE_ISOCOPY_BLIT(ANYTYPE) \
void NAME_ISOCOPY_BLIT(ANYTYPE)(void *srcBase, void *dstBase, \
                                juint width, juint height, \
                                SurfaceDataRasInfo *pSrcInfo, \
                                SurfaceDataRasInfo *pDstInfo, \
                                NativePrimitive *pPrim, \
                                CompositeInfo *pCompInfo) \
{ \
    Declare ## ANYTYPE ## StoreVars(DstWrite) \
    BlitLoopHeight(ANYTYPE, pSrc, srcBase, pSrcInfo, \
                   ANYTYPE, pDst, dstBase, pDstInfo, DstWrite, \
                   height, \
                   memcpy(pDst, pSrc, width * ANYTYPE ## PixelStride)); \
}

/*
 * This macro defines an entire function to implement a ScaleBlit inner loop
 * for scaling pixels of a common type from one buffer to another.
 */
#define DEFINE_ISOSCALE_BLIT(ANYTYPE) \
void NAME_ISOSCALE_BLIT(ANYTYPE)(void *srcBase, void *dstBase, \
                                 juint width, juint height, \
                                 jint sxloc, jint syloc, \
                                 jint sxinc, jint syinc, jint shift, \
                                 SurfaceDataRasInfo *pSrcInfo, \
                                 SurfaceDataRasInfo *pDstInfo, \
                                 NativePrimitive *pPrim, \
                                 CompositeInfo *pCompInfo) \
{ \
    Declare ## ANYTYPE ## StoreVars(DstWrite) \
    BlitLoopScaleWidthHeight(ANYTYPE, pSrc, srcBase, pSrcInfo, \
                             ANYTYPE, pDst, dstBase, pDstInfo, DstWrite, \
                             x, width, height, \
                             sxloc, syloc, sxinc, syinc, shift, \
                             Copy ## ANYTYPE ## PixelData(pSrc, x, pDst, 0)); \
}

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for XORing pixels of a common type from one buffer into another.
 */
#define DEFINE_ISOXOR_BLIT(ANYTYPE) \
void NAME_ISOXOR_BLIT(ANYTYPE)(void *srcBase, void *dstBase, \
                               juint width, juint height, \
                               SurfaceDataRasInfo *pSrcInfo, \
                               SurfaceDataRasInfo *pDstInfo, \
                               NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    Declare ## ANYTYPE ## PixelData(xor) \
    Declare ## ANYTYPE ## StoreVars(DstWrite) \
 \
    Extract ## ANYTYPE ## PixelData(xorpixel, xor); \
 \
    BlitLoopWidthHeight(ANYTYPE, pSrc, srcBase, pSrcInfo, \
                        ANYTYPE, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        XorCopy ## ANYTYPE ## PixelData(pSrc, pDst, 0, \
                                                        xorpixel, xor)); \
}

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for converting pixels from a buffer of one type into a buffer of
 * another type.  No blending is done of the pixels.
 */
#define DEFINE_CONVERT_BLIT(SRC, DST, STRATEGY) \
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
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        ConvertVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                               pDst, DST, DstWrite, \
                                               0, 0)); \
}

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for converting pixels from a buffer of byte pixels with a lookup
 * table into a buffer of another type.  No blending is done of the pixels.
 */
#define DEFINE_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY) \
void NAME_CONVERT_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                                 juint width, juint height, \
                                 SurfaceDataRasInfo *pSrcInfo, \
                                 SurfaceDataRasInfo *pDstInfo, \
                                 NativePrimitive *pPrim, \
                                 CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## LUT_STRATEGY ## Lut(SRC, DST, pixLut) \
 \
    Setup ## LUT_STRATEGY ## Lut(SRC, DST, pixLut,\
                                 pSrcInfo, pDstInfo); \
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        Body ## LUT_STRATEGY ## Lut(pSrc, SRC, \
                                                    pixLut, \
                                                    pDst, DST, \
                                                    DstWrite, 0, 0));\
}
#define DEFINE_CONVERT_BLIT_LUT8(SRC, DST, LUT_STRATEGY) \
    DEFINE_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY)

/*
 * This macro defines an entire function to implement a ScaleBlit inner
 * loop for scaling and converting pixels from a buffer of one type into
 * a buffer of another type.  No blending is done of the pixels.
 */
#define DEFINE_SCALE_BLIT(SRC, DST, STRATEGY) \
void NAME_SCALE_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                               juint width, juint height, \
                               jint sxloc, jint syloc, \
                               jint sxinc, jint syinc, jint shift, \
                               SurfaceDataRasInfo *pSrcInfo, \
                               SurfaceDataRasInfo *pDstInfo, \
                               NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    BlitLoopScaleWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                             DST, pDst, dstBase, pDstInfo, DstWrite, \
                             x, width, height, \
                             sxloc, syloc, sxinc, syinc, shift, \
                             ConvertVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                                    pDst, DST, DstWrite, \
                                                    x, 0)); \
}

/*
 * This macro defines an entire function to implement a ScaleBlit inner
 * loop for scaling and converting pixels from a buffer of byte pixels
 * with a lookup table into a buffer of another type.  No blending is
 * done of the pixels.
 */
#define DEFINE_SCALE_BLIT_LUT(SRC, DST, LUT_STRATEGY) \
void NAME_SCALE_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                               juint width, juint height, \
                               jint sxloc, jint syloc, \
                               jint sxinc, jint syinc, jint shift, \
                               SurfaceDataRasInfo *pSrcInfo, \
                               SurfaceDataRasInfo *pDstInfo, \
                               NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## LUT_STRATEGY ## Lut(SRC, DST, pixLut) \
 \
    Setup ## LUT_STRATEGY ## Lut(SRC, DST, pixLut, pSrcInfo, pDstInfo); \
    BlitLoopScaleWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                             DST, pDst, dstBase, pDstInfo, DstWrite, \
                             x, width, height, \
                             sxloc, syloc, sxinc, syinc, shift, \
                             Body ## LUT_STRATEGY ## Lut(pSrc, SRC, pixLut, \
                                                         pDst, DST, \
                                                         DstWrite, x, 0));\
}
#define DEFINE_SCALE_BLIT_LUT8(SRC, DST, LUT_STRATEGY) \
    DEFINE_SCALE_BLIT_LUT(SRC, DST, LUT_STRATEGY)

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for drawing opaque pixels from a buffer of one type onto a buffer of
 * another type, ignoring the transparent pixels in the source buffer.
 * No blending is done of the pixels - the converted pixel value is
 * either copied or the destination is left untouched.
 */
#define DEFINE_XPAR_CONVERT_BLIT(SRC, DST, STRATEGY) \
void NAME_XPAR_CONVERT_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
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
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        ConvertXparVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                                   pDst, DST, DstWrite, \
                                                   0, 0)); \
}

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for converting pixels from a buffer of byte pixels with a lookup
 * table containing transparent pixels into a buffer of another type.
 * No blending is done of the pixels - the converted pixel value is
 * either copied or the destination is left untouched.
 */
#define DEFINE_XPAR_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY) \
void NAME_XPAR_CONVERT_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                                      juint width, juint height, \
                                      SurfaceDataRasInfo *pSrcInfo, \
                                      SurfaceDataRasInfo *pDstInfo, \
                                      NativePrimitive *pPrim, \
                                      CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## LUT_STRATEGY ## XparLut(SRC, DST, pixLut) \
 \
    Setup ## LUT_STRATEGY ## XparLut(SRC, DST, pixLut, pSrcInfo, pDstInfo); \
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        Body ## LUT_STRATEGY ## XparLut(pSrc, SRC, pixLut, \
                                                        pDst, DST, \
                                                        DstWrite, 0, 0)); \
}
#define DEFINE_XPAR_CONVERT_BLIT_LUT8(SRC, DST, LUT_STRATEGY) \
    DEFINE_XPAR_CONVERT_BLIT_LUT(SRC, DST, LUT_STRATEGY)

/*
 * This macro defines an entire function to implement a ScaleBlit inner
 * loop for scaling and converting pixels from a buffer of byte pixels
 * with a lookup table containing transparent pixels into a buffer of
 * another type.
 * No blending is done of the pixels - the converted pixel value is
 * either copied or the destination is left untouched.
 */
#define DEFINE_XPAR_SCALE_BLIT_LUT(SRC, DST, LUT_STRATEGY) \
void NAME_XPAR_SCALE_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                                    juint width, juint height, \
                                    jint sxloc, jint syloc, \
                                    jint sxinc, jint syinc, jint shift, \
                                    SurfaceDataRasInfo *pSrcInfo, \
                                    SurfaceDataRasInfo *pDstInfo, \
                                    NativePrimitive *pPrim, \
                                    CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## LUT_STRATEGY ## XparLut(SRC, DST, pixLut) \
 \
    Setup ## LUT_STRATEGY ## XparLut(SRC, DST, pixLut, pSrcInfo, pDstInfo); \
    BlitLoopScaleWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                             DST, pDst, dstBase, pDstInfo, DstWrite, \
                             x, width, height, \
                             sxloc, syloc, sxinc, syinc, shift, \
                             Body ## LUT_STRATEGY ## XparLut(pSrc, SRC, pixLut, \
                                                             pDst, DST, \
                                                             DstWrite, \
                                                             x, 0)); \
}
#define DEFINE_XPAR_SCALE_BLIT_LUT8(SRC, DST, LUT_STRATEGY) \
    DEFINE_XPAR_SCALE_BLIT_LUT(SRC, DST, LUT_STRATEGY)

/*
 * This macro defines an entire function to implement a ScaleBlit inner
 * loop for scaling and converting pixels from a buffer of one type
 * containing transparent pixels into a buffer of another type.
 *
 * No blending is done of the pixels - the converted pixel value is
 * either copied or the destination is left untouched.
 */
#define DEFINE_XPAR_SCALE_BLIT(SRC, DST, STRATEGY) \
void NAME_XPAR_SCALE_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                               juint width, juint height, \
                               jint sxloc, jint syloc, \
                               jint sxinc, jint syinc, jint shift, \
                               SurfaceDataRasInfo *pSrcInfo, \
                               SurfaceDataRasInfo *pDstInfo, \
                               NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    BlitLoopScaleWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                             DST, pDst, dstBase, pDstInfo, DstWrite, \
                             x, width, height, \
                             sxloc, syloc, sxinc, syinc, shift, \
                             ConvertXparVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                                        pDst, DST, DstWrite, \
                                                        x, 0)); \
}

/*
 * This macro defines an entire function to implement a BlitBg inner loop
 * for converting pixels from a buffer of one type containing transparent
 * pixels into a buffer of another type with a specified bgcolor for the
 * transparent pixels.
 * No blending is done of the pixels other than to substitute the
 * bgcolor for any transparent pixels.
 */
#define DEFINE_XPAR_BLITBG(SRC, DST, STRATEGY) \
void NAME_XPAR_BLITBG(SRC, DST)(void *srcBase, void *dstBase, \
                                juint width, juint height, \
                                jint bgpixel, \
                                SurfaceDataRasInfo *pSrcInfo, \
                                SurfaceDataRasInfo *pDstInfo, \
                                NativePrimitive *pPrim, \
                                CompositeInfo *pCompInfo) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## DST ## PixelData(bgdata) \
 \
    Extract ## DST ## PixelData(bgpixel, bgdata); \
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        BgCopyXparVia ## STRATEGY(pSrc, SRC, SrcRead, \
                                                  pDst, DST, DstWrite, \
                                                  0, 0, bgpixel, bgdata)); \
}

/*
 * This macro defines an entire function to implement a BlitBg inner loop
 * for converting pixels from a buffer of byte pixels with a lookup
 * table containing transparent pixels into a buffer of another type
 * with a specified bgcolor for the transparent pixels.
 * No blending is done of the pixels other than to substitute the
 * bgcolor for any transparent pixels.
 */
#define DEFINE_XPAR_BLITBG_LUT(SRC, DST, LUT_STRATEGY) \
void NAME_XPAR_BLITBG(SRC, DST)(void *srcBase, void *dstBase, \
                                juint width, juint height, \
                                jint bgpixel, \
                                SurfaceDataRasInfo *pSrcInfo, \
                                SurfaceDataRasInfo *pDstInfo, \
                                NativePrimitive *pPrim, \
                                CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## StoreVars(DstWrite) \
    Declare ## LUT_STRATEGY ## BgLut(SRC, DST, pixLut) \
 \
    Setup ## LUT_STRATEGY ## BgLut(SRC, DST, pixLut, pSrcInfo, pDstInfo, \
                                   bgpixel); \
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        Body ## LUT_STRATEGY ## BgLut(pSrc, SRC, pixLut, \
                                                      pDst, DST, \
                                                      DstWrite, 0, 0, \
                                                      bgpixel)); \
}
#define DEFINE_XPAR_BLITBG_LUT8(SRC, DST, LUT_STRATEGY) \
    DEFINE_XPAR_BLITBG_LUT(SRC, DST, LUT_STRATEGY)

/*
 * This macro defines an entire function to implement a Blit inner loop
 * for converting pixels from a buffer of one type into a buffer of
 * another type.  Each source pixel is XORed with the current XOR color value.
 * That result is then XORed with the destination pixel and the final
 * result is stored in the destination surface.
 */
#define DEFINE_XOR_BLIT(SRC, DST, DSTANYTYPE) \
void NAME_XOR_BLIT(SRC, DST)(void *srcBase, void *dstBase, \
                             juint width, juint height, \
                             SurfaceDataRasInfo *pSrcInfo, \
                             SurfaceDataRasInfo *pDstInfo, \
                             NativePrimitive *pPrim, \
                             CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    Declare ## DSTANYTYPE ## PixelData(xor) \
    Declare ## DSTANYTYPE ## PixelData(mask) \
    Declare ## SRC ## LoadVars(SrcRead) \
    Declare ## DST ## StoreVars(DstWrite) \
 \
    Extract ## DSTANYTYPE ## PixelData(xorpixel, xor); \
    Extract ## DSTANYTYPE ## PixelData(alphamask, mask); \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    BlitLoopWidthHeight(SRC, pSrc, srcBase, pSrcInfo, \
                        DST, pDst, dstBase, pDstInfo, DstWrite, \
                        width, height, \
                        XorVia1IntArgb(pSrc, SRC, SrcRead, \
                                       pDst, DST, DSTANYTYPE, \
                                       0, xorpixel, xor, \
                                       alphamask, mask, pDstInfo)); \
}

/*
 * This macro defines an entire function to implement a FillRect inner loop
 * for setting a rectangular region of pixels to a specific pixel value.
 * No blending of the fill color is done with the pixels.
 */
#define DEFINE_SOLID_FILLRECT(DST) \
void NAME_SOLID_FILLRECT(DST)(SurfaceDataRasInfo *pRasInfo, \
                              jint lox, jint loy, \
                              jint hix, jint hiy, \
                              jint pixel, \
                              NativePrimitive *pPrim, \
                              CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## PixelData(pix) \
    DST ## DataType *pPix; \
    jint scan = pRasInfo->scanStride; \
    juint height = hiy - loy; \
    juint width = hix - lox; \
 \
    pPix = PtrCoord(pRasInfo->rasBase, lox, DST ## PixelStride, loy, scan); \
    Extract ## DST ## PixelData(pixel, pix); \
    do { \
        juint x = 0; \
        do { \
            Store ## DST ## PixelData(pPix, x, pixel, pix); \
        } while (++x < width); \
        pPix = PtrAddBytes(pPix, scan); \
    } while (--height > 0); \
}

/*
 * This macro defines an entire function to implement a FillSpans inner loop
 * for iterating through a list of spans and setting those regions of pixels
 * to a specific pixel value.  No blending of the fill color is done with
 * the pixels.
 */
#define DEFINE_SOLID_FILLSPANS(DST) \
void NAME_SOLID_FILLSPANS(DST)(SurfaceDataRasInfo *pRasInfo, \
                               SpanIteratorFuncs *pSpanFuncs, void *siData, \
                               jint pixel, NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    void *pBase = pRasInfo->rasBase; \
    Declare ## DST ## PixelData(pix) \
    jint scan = pRasInfo->scanStride; \
    jint bbox[4]; \
 \
    Extract ## DST ## PixelData(pixel, pix); \
    while ((*pSpanFuncs->nextSpan)(siData, bbox)) { \
        jint x = bbox[0]; \
        jint y = bbox[1]; \
        juint w = bbox[2] - x; \
        juint h = bbox[3] - y; \
        DST ## DataType *pPix = PtrCoord(pBase, \
                                         x, DST ## PixelStride, \
                                         y, scan); \
        do { \
            juint relx; \
            for (relx = 0; relx < w; relx++) { \
                Store ## DST ## PixelData(pPix, relx, pixel, pix); \
            } \
            pPix = PtrAddBytes(pPix, scan); \
        } while (--h > 0); \
    } \
}

/*
 * This macro defines an entire function to implement a FillParallelogram
 * inner loop for tracing 2 diagonal edges (left and right) and setting
 * those regions of pixels between them to a specific pixel value.
 * No blending of the fill color is done with the pixels.
 */
#define DEFINE_SOLID_FILLPGRAM(DST) \
void NAME_SOLID_FILLPGRAM(DST)(SurfaceDataRasInfo *pRasInfo, \
                               jint lox, jint loy, jint hix, jint hiy, \
                               jlong leftx, jlong dleftx, \
                               jlong rightx, jlong drightx, \
                               jint pixel, struct _NativePrimitive *pPrim, \
                               CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## PixelData(pix) \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix = PtrCoord(pRasInfo->rasBase, 0, 0, loy, scan); \
 \
    Extract ## DST ## PixelData(pixel, pix); \
    while (loy < hiy) { \
        jint lx = WholeOfLong(leftx); \
        jint rx = WholeOfLong(rightx); \
        if (lx < lox) lx = lox; \
        if (rx > hix) rx = hix; \
        while (lx < rx) { \
            Store ## DST ## PixelData(pPix, lx, pixel, pix); \
            lx++; \
        } \
        pPix = PtrAddBytes(pPix, scan); \
        leftx += dleftx; \
        rightx += drightx; \
        loy++; \
    } \
}

#define DEFINE_SOLID_DRAWPARALLELOGRAM_FUNCS(DST) \
    DrawParallelogramFuncs NAME_SOLID_PGRAM_FUNCS(DST) = { \
        NAME_SOLID_FILLPGRAM(DST), \
        NAME_SOLID_DRAWLINE(DST), \
    };

#define DEFINE_SOLID_PARALLELOGRAM(DST) \
    DEFINE_SOLID_FILLPGRAM(DST) \
    DEFINE_SOLID_DRAWPARALLELOGRAM_FUNCS(DST)

/*
 * This macro declares the bumpmajor and bumpminor variables used for the
 * DrawLine functions.
 */
#define DeclareBumps(BUMPMAJOR, BUMPMINOR) \
    jint BUMPMAJOR, BUMPMINOR;

/*
 * This macro extracts "instructions" from the bumpmajor and bumpminor masks
 * that determine the initial bumpmajor and bumpminor values.  The bumpmajor
 * and bumpminor masks are laid out in the following format:
 *
 * bumpmajormask:                      bumpminormask:
 * bit0: bumpmajor = pixelStride       bit0: bumpminor = pixelStride
 * bit1: bumpmajor = -pixelStride      bit1: bumpminor = -pixelStride
 * bit2: bumpmajor = scanStride        bit2: bumpminor = scanStride
 * bit3: bumpmajor = -scanStride       bit3: bumpminor = -scanStride
 */
#define InitBumps(BUMPMAJOR, BUMPMINOR, \
                  BUMPMAJORMASK, BUMPMINORMASK, \
                  PIXELSTRIDE, SCANSTRIDE) \
    BUMPMAJOR = (BUMPMAJORMASK & BUMP_POS_PIXEL) ? PIXELSTRIDE : \
                    (BUMPMAJORMASK & BUMP_NEG_PIXEL) ? -PIXELSTRIDE : \
                        (BUMPMAJORMASK & BUMP_POS_SCAN) ? SCANSTRIDE : \
                                                          -SCANSTRIDE; \
    BUMPMINOR = (BUMPMINORMASK & BUMP_POS_PIXEL) ? PIXELSTRIDE : \
                    (BUMPMINORMASK & BUMP_NEG_PIXEL) ? -PIXELSTRIDE : \
                        (BUMPMINORMASK & BUMP_POS_SCAN) ? SCANSTRIDE : \
                            (BUMPMINORMASK & BUMP_NEG_SCAN) ? -SCANSTRIDE : \
                                                              0; \
    BUMPMINOR += BUMPMAJOR;

/*
 * This macro defines an entire function to implement a DrawLine inner loop
 * for iterating along a horizontal or vertical line and setting the pixels
 * on that line to a specific pixel value.  No blending of the fill color
 * is done with the pixels.
 */
#define DEFINE_SOLID_DRAWLINE(DST) \
void NAME_SOLID_DRAWLINE(DST)(SurfaceDataRasInfo *pRasInfo, \
                              jint x1, jint y1, jint pixel, \
                              jint steps, jint error, \
                              jint bumpmajormask, jint errmajor, \
                              jint bumpminormask, jint errminor, \
                              NativePrimitive *pPrim, \
                              CompositeInfo *pCompInfo) \
{ \
    Declare ## DST ## PixelData(pix) \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix = PtrCoord(pRasInfo->rasBase, \
                                     x1, DST ## PixelStride, \
                                     y1, scan); \
    DeclareBumps(bumpmajor, bumpminor) \
 \
    InitBumps(bumpmajor, bumpminor, bumpmajormask, bumpminormask, \
              DST ## PixelStride, scan); \
    Extract ## DST ## PixelData(pixel, pix); \
    if (errmajor == 0) { \
        do { \
            Store ## DST ## PixelData(pPix, 0, pixel, pix); \
            pPix = PtrAddBytes(pPix, bumpmajor); \
        } while (--steps > 0); \
    } else { \
        do { \
            Store ## DST ## PixelData(pPix, 0, pixel, pix); \
            if (error < 0) { \
                pPix = PtrAddBytes(pPix, bumpmajor); \
                error += errmajor; \
            } else { \
                pPix = PtrAddBytes(pPix, bumpminor); \
                error -= errminor; \
            } \
        } while (--steps > 0); \
    } \
}

/*
 * This macro defines an entire function to implement a FillRect inner loop
 * for setting a rectangular region of pixels to a specific pixel value.
 * Each destination pixel is XORed with the current XOR mode color as well as
 * the current fill color.
 */
#define DEFINE_XOR_FILLRECT(DST) \
void NAME_XOR_FILLRECT(DST)(SurfaceDataRasInfo *pRasInfo, \
                            jint lox, jint loy, \
                            jint hix, jint hiy, \
                            jint pixel, \
                            NativePrimitive *pPrim, \
                            CompositeInfo *pCompInfo) \
{ \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    Declare ## DST ## PixelData(xor) \
    Declare ## DST ## PixelData(pix) \
    Declare ## DST ## PixelData(mask) \
    DST ## DataType *pPix; \
    jint scan = pRasInfo->scanStride; \
    juint height = hiy - loy; \
    juint width = hix - lox; \
 \
    pPix = PtrCoord(pRasInfo->rasBase, lox, DST ## PixelStride, loy, scan); \
    Extract ## DST ## PixelData(xorpixel, xor); \
    Extract ## DST ## PixelData(pixel, pix); \
    Extract ## DST ## PixelData(alphamask, mask); \
 \
    do { \
        juint x = 0; \
        do { \
            Xor ## DST ## PixelData(pixel, pix, pPix, x, \
                                    xorpixel, xor, alphamask, mask); \
        } while (++x < width); \
        pPix = PtrAddBytes(pPix, scan); \
    } while (--height > 0); \
}

/*
 * This macro defines an entire function to implement a FillSpans inner loop
 * for iterating through a list of spans and setting those regions of pixels
 * to a specific pixel value.  Each destination pixel is XORed with the
 * current XOR mode color as well as the current fill color.
 */
#define DEFINE_XOR_FILLSPANS(DST) \
void NAME_XOR_FILLSPANS(DST)(SurfaceDataRasInfo *pRasInfo, \
                             SpanIteratorFuncs *pSpanFuncs, \
                             void *siData, jint pixel, \
                             NativePrimitive *pPrim, \
                             CompositeInfo *pCompInfo) \
{ \
    void *pBase = pRasInfo->rasBase; \
    jint xorpixel = pCompInfo->details.xorPixel; \
    juint alphamask = pCompInfo->alphaMask; \
    Declare ## DST ## PixelData(xor) \
    Declare ## DST ## PixelData(pix) \
    Declare ## DST ## PixelData(mask) \
    jint scan = pRasInfo->scanStride; \
    jint bbox[4]; \
 \
    Extract ## DST ## PixelData(xorpixel, xor); \
    Extract ## DST ## PixelData(pixel, pix); \
    Extract ## DST ## PixelData(alphamask, mask); \
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
            juint relx; \
            for (relx = 0; relx < w; relx++) { \
                Xor ## DST ## PixelData(pixel, pix, pPix, relx, \
                                        xorpixel, xor, alphamask, mask); \
            } \
            pPix = PtrAddBytes(pPix, scan); \
        } while (--h > 0); \
    } \
}

/*
 * This macro defines an entire function to implement a DrawLine inner loop
 * for iterating along a horizontal or vertical line and setting the pixels
 * on that line to a specific pixel value.  Each destination pixel is XORed
 * with the current XOR mode color as well as the current draw color.
 */
#define DEFINE_XOR_DRAWLINE(DST) \
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
    Declare ## DST ## PixelData(xor) \
    Declare ## DST ## PixelData(pix) \
    Declare ## DST ## PixelData(mask) \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix = PtrCoord(pRasInfo->rasBase, \
                                     x1, DST ## PixelStride, \
                                     y1, scan); \
    DeclareBumps(bumpmajor, bumpminor) \
 \
    InitBumps(bumpmajor, bumpminor, bumpmajormask, bumpminormask, \
              DST ## PixelStride, scan); \
    Extract ## DST ## PixelData(xorpixel, xor); \
    Extract ## DST ## PixelData(pixel, pix); \
    Extract ## DST ## PixelData(alphamask, mask); \
 \
    if (errmajor == 0) { \
        do { \
            Xor ## DST ## PixelData(pixel, pix, pPix, 0, \
                                    xorpixel, xor, alphamask, mask); \
            pPix = PtrAddBytes(pPix, bumpmajor); \
        } while (--steps > 0); \
    } else { \
        do { \
            Xor ## DST ## PixelData(pixel, pix, pPix, 0, \
                                    xorpixel, xor, alphamask, mask); \
            if (error < 0) { \
                pPix = PtrAddBytes(pPix, bumpmajor); \
                error += errmajor; \
            } else { \
                pPix = PtrAddBytes(pPix, bumpminor); \
                error -= errminor; \
            } \
        } while (--steps > 0); \
    } \
}

/*
 * This macro is used to declare the variables needed by the glyph clipping
 * macro.
 */
#define DeclareDrawGlyphListClipVars(PIXELS, ROWBYTES, WIDTH, HEIGHT, \
                                     LEFT, TOP, RIGHT, BOTTOM) \
    const jubyte * PIXELS; \
    int ROWBYTES; \
    int LEFT, TOP; \
    int WIDTH, HEIGHT; \
    int RIGHT, BOTTOM;

/*
 * This macro represents the glyph clipping code used in the various
 * DRAWGLYPHLIST macros.  This macro is typically used within a loop.  Note
 * that the body of this macro is NOT wrapped in a do..while block due to
 * the use of continue statements within the block (those continue statements
 * are intended skip the outer loop, not the do..while loop).  To combat this
 * problem, pass in the code (typically a continue statement) that should be
 * executed when a null glyph is encountered.
 */
#define ClipDrawGlyphList(DST, PIXELS, BYTESPERPIXEL, ROWBYTES, WIDTH, HEIGHT,\
                          LEFT, TOP, RIGHT, BOTTOM, \
                          CLIPLEFT, CLIPTOP, CLIPRIGHT, CLIPBOTTOM, \
                          GLYPHS, GLYPHCOUNTER, NULLGLYPHCODE) \
    PIXELS = (const jubyte *)GLYPHS[GLYPHCOUNTER].pixels; \
    if (!PIXELS) { \
        NULLGLYPHCODE; \
    } \
    ROWBYTES = GLYPHS[GLYPHCOUNTER].rowBytes; \
    LEFT     = GLYPHS[GLYPHCOUNTER].x; \
    TOP      = GLYPHS[GLYPHCOUNTER].y; \
    WIDTH    = GLYPHS[GLYPHCOUNTER].width; \
    HEIGHT   = GLYPHS[GLYPHCOUNTER].height; \
\
    /* if any clipping required, modify parameters now */ \
    RIGHT  = LEFT + WIDTH; \
    BOTTOM = TOP + HEIGHT; \
    if (LEFT < CLIPLEFT) { \
    /* Multiply needed for LCD text as PIXELS is really BYTES */ \
        PIXELS += (CLIPLEFT - LEFT) * BYTESPERPIXEL ; \
        LEFT = CLIPLEFT; \
    } \
    if (TOP < CLIPTOP) { \
        PIXELS += (CLIPTOP - TOP) * ROWBYTES; \
        TOP = CLIPTOP; \
    } \
    if (RIGHT > CLIPRIGHT) { \
        RIGHT = CLIPRIGHT; \
    } \
    if (BOTTOM > CLIPBOTTOM) { \
        BOTTOM = CLIPBOTTOM; \
    } \
    if (RIGHT <= LEFT || BOTTOM <= TOP) { \
        NULLGLYPHCODE; \
    } \
    WIDTH = RIGHT - LEFT; \
    HEIGHT = BOTTOM - TOP;

#define DEFINE_SOLID_DRAWGLYPHLIST(DST) \
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
    Declare ## DST ## PixelData(pix) \
    DST ## DataType *pPix; \
\
    Extract ## DST ## PixelData(fgpixel, pix); \
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
            int x = 0; \
            do { \
                if (pixels[x]) { \
                    Store ## DST ## PixelData(pPix, x, fgpixel, pix); \
                } \
            } while (++x < width); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
        } while (--height > 0); \
    } \
}

#define GlyphListAABlend3ByteRgb(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                 FG_PIXEL, PREFIX, SRC_PREFIX) \
   do { \
        DeclareCompVarsFor3ByteRgb(dst) \
        jint mixValSrc = GLYPH_PIXELS[PIXEL_INDEX]; \
        if (mixValSrc) { \
            if (mixValSrc < 255) { \
                jint mixValDst = 255 - mixValSrc; \
                Load ## DST ## To3ByteRgb(DST_PTR, pix, PIXEL_INDEX, \
                                          dstR, dstG, dstB); \
                MultMultAddAndStore3ByteRgbComps(dst, mixValDst, dst, \
                                                 mixValSrc, SRC_PREFIX); \
                Store ## DST ## From3ByteRgb(DST_PTR, pix, PIXEL_INDEX, \
                                             dstR, dstG, dstB); \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
            } \
        } \
    } while (0);

/*
 * Antialiased glyph drawing results in artifacts around the character edges
 * when text is drawn ontop of translucent background color. The standard
 * blending equation for two colors:
 * destColor = srcColor * glyphAlpha + destColor * (1 - glyphAlpha)
 * works only when srcColor and destColor are opaque. For translucent srcColor
 * and destColor, the respective alpha components in each color will influence
 * the visibility of the color and the visibility of the color below it. Hence
 * the equation for blending is given as:
 * resA = srcAlpha + dstAlpha * (1 - srcAlpha)
 * resCol = (srcColor * srcAlpha + destColor * destAlpha * (1- srcAlpha))/resA
 * In addition, srcAlpha is multiplied with the glyphAlpha- that indicates the
 * grayscale mask value of the glyph being drawn. The combined result provides
 * smooth antialiased text on the buffer without any artifacts. Since the
 * logic is executed for every pixel in a glyph, the implementation is further
 * optimized to reduce computation and improve execution time.
 */
#define GlyphListAABlend4ByteArgb(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                  FG_PIXEL, PREFIX, SRC_PREFIX) \
    do { \
        DeclareAlphaVarFor4ByteArgb(resA) \
        DeclareCompVarsFor4ByteArgb(res) \
        jint mixValSrc = GLYPH_PIXELS[PIXEL_INDEX]; \
        if (mixValSrc) { \
            if (mixValSrc != 0xff) { \
                PromoteByteAlphaFor4ByteArgb(mixValSrc); \
                resA = MultiplyAlphaFor4ByteArgb(mixValSrc, SRC_PREFIX ## A); \
            } else { \
                resA = SRC_PREFIX ## A; \
            } \
            if (resA != MaxValFor4ByteArgb) { \
                DeclareAndInvertAlphaVarFor4ByteArgb(dstF, resA) \
                DeclareAndClearAlphaVarFor4ByteArgb(dstA) \
                DeclareCompVarsFor4ByteArgb(dst) \
                DeclareCompVarsFor4ByteArgb(tmp) \
                MultiplyAndStore4ByteArgbComps(res, resA, SRC_PREFIX); \
                if (!(DST ## IsPremultiplied)) { \
                    Load ## DST ## To4ByteArgb(DST_PTR, pix, PIXEL_INDEX, \
                                               dstA, dstR, dstG, dstB); \
                    Store4ByteArgbCompsUsingOp(tmp, =, dst); \
                } else { \
                    Declare ## DST ## AlphaLoadData(DstPix) \
                    jint pixelOffset = PIXEL_INDEX * (DST ## PixelStride); \
                    DST ## DataType *pixelAddress = PtrAddBytes(DST_PTR, \
                                                                pixelOffset); \
                    LoadAlphaFrom ## DST ## For4ByteArgb(pixelAddress, \
                                                         DstPix, \
                                                         dst); \
                    Postload4ByteArgbFrom ## DST(pixelAddress, \
                                                 DstPix, \
                                                 tmp); \
                } \
                if (dstA) { \
                    DeclareAlphaVarFor4ByteArgb(blendF) \
                    dstA = MultiplyAlphaFor4ByteArgb(dstF, dstA); \
                    resA += dstA; \
                    blendF = SrcOver ## DST ## BlendFactor(dstF, dstA); \
                    if (blendF != MaxValFor4ByteArgb) { \
                        MultiplyAndStore4ByteArgbComps(tmp, \
                                                       blendF, \
                                                       tmp); \
                    } \
                    Store4ByteArgbCompsUsingOp(res, +=, tmp); \
                } \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
                break; \
            } \
            if (!(DST ## IsOpaque) && \
                !(DST ## IsPremultiplied) && resA && \
                resA < MaxValFor4ByteArgb) \
            { \
                DivideAndStore4ByteArgbComps(res, res, resA); \
            } \
            Store ## DST ## From4ByteArgbComps(DST_PTR, pix, \
                                               PIXEL_INDEX, res); \
        } \
    } while (0);

#define GlyphListAABlend1ByteGray(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                  FG_PIXEL, PREFIX, SRC_PREFIX) \
   do { \
        DeclareCompVarsFor1ByteGray(dst) \
        jint mixValSrc = GLYPH_PIXELS[PIXEL_INDEX]; \
        if (mixValSrc) { \
            if (mixValSrc < 255) { \
                jint mixValDst = 255 - mixValSrc; \
                Load ## DST ## To1ByteGray(DST_PTR, pix, PIXEL_INDEX, \
                                           dstG); \
                MultMultAddAndStore1ByteGrayComps(dst, mixValDst, dst, \
                                                  mixValSrc, SRC_PREFIX); \
                Store ## DST ## From1ByteGray(DST_PTR, pix, PIXEL_INDEX, \
                                              dstG); \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
            } \
        } \
    } while (0);

#define GlyphListAABlend1ShortGray(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                   FG_PIXEL, PREFIX, SRC_PREFIX) \
   do { \
        DeclareCompVarsFor1ShortGray(dst) \
        juint mixValSrc = GLYPH_PIXELS[PIXEL_INDEX]; \
        if (mixValSrc) { \
            if (mixValSrc < 255) { \
                juint mixValDst; \
                PromoteByteAlphaFor1ShortGray(mixValSrc); \
                mixValDst = 0xffff - mixValSrc; \
                Load ## DST ## To1ShortGray(DST_PTR, pix, PIXEL_INDEX, \
                                            dstG); \
                MultMultAddAndStore1ShortGrayComps(dst, mixValDst, dst, \
                                                   mixValSrc, SRC_PREFIX); \
                Store ## DST ## From1ShortGray(DST_PTR, pix, PIXEL_INDEX, \
                                               dstG); \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
            } \
        } \
    } while (0);

#define DEFINE_SOLID_DRAWGLYPHLISTAA(DST, STRATEGY) \
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
    Declare ## DST ## PixelData(solidpix) \
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
    Extract ## DST ## PixelData(fgpixel, solidpix); \
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
            int x = 0; \
            Set ## DST ## StoreVarsXPos(pix, pRasInfo, left); \
            do { \
                GlyphListAABlend ## STRATEGY(DST, pixels, x, pPix, \
                                             fgpixel, solidpix, src); \
                Next ## DST ## StoreVarsX(pix); \
            } while (++x < width); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
            Next ## DST ## StoreVarsY(pix); \
        } while (--height > 0); \
    } \
}


#define GlyphListLCDBlend3ByteRgb(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                  FG_PIXEL, PREFIX, SRC_PREFIX) \
   do { \
        DeclareCompVarsFor3ByteRgb(dst) \
        jint mixValSrcG = GLYPH_PIXELS[PIXEL_INDEX*3+1]; \
        jint mixValSrcR, mixValSrcB; \
        if (rgbOrder) { \
            mixValSrcR = GLYPH_PIXELS[PIXEL_INDEX*3]; \
            mixValSrcB = GLYPH_PIXELS[PIXEL_INDEX*3+2]; \
        } else { \
            mixValSrcR = GLYPH_PIXELS[PIXEL_INDEX*3+2]; \
            mixValSrcB = GLYPH_PIXELS[PIXEL_INDEX*3]; \
        } \
        if ((mixValSrcR | mixValSrcG | mixValSrcB) != 0) { \
            if ((mixValSrcR & mixValSrcG & mixValSrcB) < 255) { \
                jint mixValDstR = 255 - mixValSrcR; \
                jint mixValDstG = 255 - mixValSrcG; \
                jint mixValDstB = 255 - mixValSrcB; \
                Load ## DST ## To3ByteRgb(DST_PTR, pix, PIXEL_INDEX, \
                                          dstR, dstG, dstB); \
                dstR = invGammaLut[dstR]; \
                dstG = invGammaLut[dstG]; \
                dstB = invGammaLut[dstB]; \
                MultMultAddAndStoreLCD3ByteRgbComps(dst, mixValDst, dst, \
                                                    mixValSrc, SRC_PREFIX); \
                dstR = gammaLut[dstR]; \
                dstG = gammaLut[dstG]; \
                dstB = gammaLut[dstB]; \
                Store ## DST ## From3ByteRgb(DST_PTR, pix, PIXEL_INDEX, \
                                             dstR, dstG, dstB); \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
            } \
        } \
    } while (0)


/* There is no alpha channel in the glyph data with which to interpolate
 * between the src and dst alphas, but a reasonable approximation is to
 * sum the coverage alphas of the colour channels and divide by 3.
 * We can approximate division by 3 using mult and shift. See
 * sun/font/scalerMethods.c for a detailed explanation of why "21931"
 */
#define GlyphListLCDBlend4ByteArgb(DST, GLYPH_PIXELS, PIXEL_INDEX, DST_PTR, \
                                  FG_PIXEL, PREFIX, SRC_PREFIX) \
   do { \
        DeclareAlphaVarFor4ByteArgb(dstA) \
        DeclareCompVarsFor4ByteArgb(dst) \
        jint mixValSrcG = GLYPH_PIXELS[PIXEL_INDEX*3+1]; \
        jint mixValSrcR, mixValSrcB; \
        if (rgbOrder) { \
            mixValSrcR = GLYPH_PIXELS[PIXEL_INDEX*3]; \
            mixValSrcB = GLYPH_PIXELS[PIXEL_INDEX*3+2]; \
        } else { \
            mixValSrcR = GLYPH_PIXELS[PIXEL_INDEX*3+2]; \
            mixValSrcB = GLYPH_PIXELS[PIXEL_INDEX*3]; \
        } \
        if ((mixValSrcR | mixValSrcG | mixValSrcB) != 0) { \
            if ((mixValSrcR & mixValSrcG & mixValSrcB) < 255) { \
                jint mixValDstR = 255 - mixValSrcR; \
                jint mixValDstG = 255 - mixValSrcG; \
                jint mixValDstB = 255 - mixValSrcB; \
                jint mixValSrcA = ((mixValSrcR + mixValSrcG + mixValSrcB) \
                                    * 21931) >> 16;\
                jint mixValDstA = 255 - mixValSrcA; \
                Load ## DST ## To4ByteArgb(DST_PTR, pix, PIXEL_INDEX, \
                                           dstA, dstR, dstG, dstB); \
                dstR = invGammaLut[dstR]; \
                dstG = invGammaLut[dstG]; \
                dstB = invGammaLut[dstB]; \
                dstA = MUL8(dstA, mixValDstA) + \
                       MUL8(SRC_PREFIX ## A, mixValSrcA); \
                MultMultAddAndStoreLCD4ByteArgbComps(dst, mixValDst, dst, \
                                                  mixValSrc, SRC_PREFIX); \
                dstR = gammaLut[dstR]; \
                dstG = gammaLut[dstG]; \
                dstB = gammaLut[dstB]; \
                if (!(DST ## IsOpaque) && \
                    !(DST ## IsPremultiplied) && dstA && dstA < 255) { \
                    DivideAndStore4ByteArgbComps(dst, dst, dstA); \
                } \
                Store ## DST ## From4ByteArgbComps(DST_PTR, pix, \
                                                   PIXEL_INDEX, dst); \
            } else { \
                Store ## DST ## PixelData(DST_PTR, PIXEL_INDEX, \
                                          FG_PIXEL, PREFIX); \
            } \
        } \
    } while (0);

#define DEFINE_SOLID_DRAWGLYPHLISTLCD(DST, STRATEGY) \
void NAME_SOLID_DRAWGLYPHLISTLCD(DST)(SurfaceDataRasInfo *pRasInfo, \
                                     ImageRef *glyphs, \
                                     jint totalGlyphs, jint fgpixel, \
                                     jint argbcolor, \
                                     jint clipLeft, jint clipTop, \
                                     jint clipRight, jint clipBottom, \
                                     jint rgbOrder, \
                                     unsigned char *gammaLut, \
                                     unsigned char * invGammaLut, \
                                     NativePrimitive *pPrim, \
                                     CompositeInfo *pCompInfo) \
{ \
    jint glyphCounter, bpp; \
    jint scan = pRasInfo->scanStride; \
    DST ## DataType *pPix; \
    Declare ## DST ## PixelData(solidpix) \
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
    Extract ## DST ## PixelData(fgpixel, solidpix); \
    srcR = invGammaLut[srcR]; \
    srcG = invGammaLut[srcG]; \
    srcB = invGammaLut[srcB]; \
\
    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) { \
        DeclareDrawGlyphListClipVars(pixels, rowBytes, width, height, \
                                     left, top, right, bottom) \
        bpp = \
        (glyphs[glyphCounter].rowBytes == glyphs[glyphCounter].width) ? 1 : 3;\
        ClipDrawGlyphList(DST, pixels, bpp, rowBytes, width, height, \
                          left, top, right, bottom, \
                          clipLeft, clipTop, clipRight, clipBottom, \
                          glyphs, glyphCounter, continue) \
        pPix = PtrCoord(pRasInfo->rasBase,left,DST ## PixelStride,top,scan); \
\
        Set ## DST ## StoreVarsYPos(pix, pRasInfo, top); \
        if (bpp!=1) { \
           /* subpixel positioning adjustment */ \
            pixels += glyphs[glyphCounter].rowBytesOffset; \
        } \
        do { \
            int x = 0; \
            Set ## DST ## StoreVarsXPos(pix, pRasInfo, left); \
            if (bpp==1) { \
                do { \
                    if (pixels[x]) { \
                        Store ## DST ## PixelData(pPix, x, fgpixel, solidpix);\
                    } \
                } while (++x < width); \
            } else { \
                do { \
                    GlyphListLCDBlend ## STRATEGY(DST, pixels, x, pPix, \
                                                   fgpixel, solidpix, src); \
                    Next ## DST ## StoreVarsX(pix); \
                } while (++x < width); \
            } \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
            Next ## DST ## StoreVarsY(pix); \
        } while (--height > 0); \
    } \
}

#define DEFINE_XOR_DRAWGLYPHLIST(DST) \
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
    Declare ## DST ## PixelData(xor) \
    Declare ## DST ## PixelData(pix) \
    Declare ## DST ## PixelData(mask) \
    DST ## DataType *pPix; \
 \
    Extract ## DST ## PixelData(xorpixel, xor); \
    Extract ## DST ## PixelData(fgpixel, pix); \
    Extract ## DST ## PixelData(alphamask, mask); \
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
            int x = 0; \
            do { \
                if (pixels[x]) { \
                    Xor ## DST ## PixelData(fgpixel, pix, pPix, x, \
                                            xorpixel, xor, alphamask, mask); \
                } \
            } while (++x < width); \
            pPix = PtrAddBytes(pPix, scan); \
            pixels += rowBytes; \
        } while (--height > 0); \
    } \
}

#define DEFINE_TRANSFORMHELPER_NN(SRC) \
void NAME_TRANSFORMHELPER_NN(SRC)(SurfaceDataRasInfo *pSrcInfo, \
                                  jint *pRGB, jint numpix, \
                                  jlong xlong, jlong dxlong, \
                                  jlong ylong, jlong dylong) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    SRC ## DataType *pBase = pSrcInfo->rasBase; \
    jint scan = pSrcInfo->scanStride; \
    jint *pEnd = pRGB + numpix; \
 \
    xlong += IntToLong(pSrcInfo->bounds.x1); \
    ylong += IntToLong(pSrcInfo->bounds.y1); \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    while (pRGB < pEnd) { \
        SRC ## DataType *pRow = PtrPixelsRow(pBase, WholeOfLong(ylong), scan); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 0, \
                                    SrcRead, pRow, WholeOfLong(xlong)); \
        pRGB++; \
        xlong += dxlong; \
        ylong += dylong; \
    } \
}

#define DEFINE_TRANSFORMHELPER_BL(SRC) \
void NAME_TRANSFORMHELPER_BL(SRC)(SurfaceDataRasInfo *pSrcInfo, \
                                  jint *pRGB, jint numpix, \
                                  jlong xlong, jlong dxlong, \
                                  jlong ylong, jlong dylong) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    jint scan = pSrcInfo->scanStride; \
    jint cx, cy, cw, ch; \
    jint *pEnd = pRGB + numpix*4; \
 \
    cx = pSrcInfo->bounds.x1; \
    cw = pSrcInfo->bounds.x2-cx; \
 \
    cy = pSrcInfo->bounds.y1; \
    ch = pSrcInfo->bounds.y2-cy; \
 \
    xlong -= LongOneHalf; \
    ylong -= LongOneHalf; \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    while (pRGB < pEnd) { \
        jint xwhole = WholeOfLong(xlong); \
        jint ywhole = WholeOfLong(ylong); \
        jint xdelta, ydelta, isneg; \
        SRC ## DataType *pRow; \
 \
        xdelta = ((juint) (xwhole + 1 - cw)) >> 31; \
        isneg = xwhole >> 31; \
        xwhole -= isneg; \
        xdelta += isneg; \
 \
        ydelta = ((ywhole + 1 - ch) >> 31); \
        isneg = ywhole >> 31; \
        ywhole -= isneg; \
        ydelta -= isneg; \
        ydelta &= scan; \
 \
        xwhole += cx; \
        pRow = PtrPixelsRow(pSrcInfo->rasBase, ywhole + cy, scan); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 0, SrcRead, pRow, xwhole); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 1, SrcRead, pRow, xwhole+xdelta); \
        pRow = PtrAddBytes(pRow, ydelta); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 2, SrcRead, pRow, xwhole); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 3, SrcRead, pRow, xwhole+xdelta); \
 \
        pRGB += 4; \
        xlong += dxlong; \
        ylong += dylong; \
    } \
}

#define DEFINE_TRANSFORMHELPER_BC(SRC) \
void NAME_TRANSFORMHELPER_BC(SRC)(SurfaceDataRasInfo *pSrcInfo, \
                                  jint *pRGB, jint numpix, \
                                  jlong xlong, jlong dxlong, \
                                  jlong ylong, jlong dylong) \
{ \
    Declare ## SRC ## LoadVars(SrcRead) \
    jint scan = pSrcInfo->scanStride; \
    jint cx, cy, cw, ch; \
    jint *pEnd = pRGB + numpix*16; \
 \
    cx = pSrcInfo->bounds.x1; \
    cw = pSrcInfo->bounds.x2-cx; \
 \
    cy = pSrcInfo->bounds.y1; \
    ch = pSrcInfo->bounds.y2-cy; \
 \
    xlong -= LongOneHalf; \
    ylong -= LongOneHalf; \
 \
    Init ## SRC ## LoadVars(SrcRead, pSrcInfo); \
    while (pRGB < pEnd) { \
        jint xwhole = WholeOfLong(xlong); \
        jint ywhole = WholeOfLong(ylong); \
        jint xdelta0, xdelta1, xdelta2; \
        jint ydelta0, ydelta1, ydelta2; \
        jint isneg; \
        SRC ## DataType *pRow; \
 \
        xdelta0 = (-xwhole) >> 31; \
        xdelta1 = ((juint) (xwhole + 1 - cw)) >> 31; \
        xdelta2 = ((juint) (xwhole + 2 - cw)) >> 31; \
        isneg = xwhole >> 31; \
        xwhole -= isneg; \
        xdelta1 += isneg; \
        xdelta2 += xdelta1; \
 \
        ydelta0 = ((-ywhole) >> 31) & (-scan); \
        ydelta1 = ((ywhole + 1 - ch) >> 31) & scan; \
        ydelta2 = ((ywhole + 2 - ch) >> 31) & scan; \
        isneg = ywhole >> 31; \
        ywhole -= isneg; \
        ydelta1 += (isneg & -scan); \
 \
        xwhole += cx; \
        pRow = PtrPixelsRow(pSrcInfo->rasBase, ywhole + cy, scan); \
        pRow = PtrAddBytes(pRow, ydelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  0, SrcRead, pRow, xwhole+xdelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  1, SrcRead, pRow, xwhole        ); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  2, SrcRead, pRow, xwhole+xdelta1); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  3, SrcRead, pRow, xwhole+xdelta2); \
        pRow = PtrAddBytes(pRow, -ydelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  4, SrcRead, pRow, xwhole+xdelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  5, SrcRead, pRow, xwhole        ); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  6, SrcRead, pRow, xwhole+xdelta1); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  7, SrcRead, pRow, xwhole+xdelta2); \
        pRow = PtrAddBytes(pRow, ydelta1); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  8, SrcRead, pRow, xwhole+xdelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB,  9, SrcRead, pRow, xwhole        ); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 10, SrcRead, pRow, xwhole+xdelta1); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 11, SrcRead, pRow, xwhole+xdelta2); \
        pRow = PtrAddBytes(pRow, ydelta2); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 12, SrcRead, pRow, xwhole+xdelta0); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 13, SrcRead, pRow, xwhole        ); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 14, SrcRead, pRow, xwhole+xdelta1); \
        Copy ## SRC ## ToIntArgbPre(pRGB, 15, SrcRead, pRow, xwhole+xdelta2); \
 \
        pRGB += 16; \
        xlong += dxlong; \
        ylong += dylong; \
    } \
}

#define DEFINE_TRANSFORMHELPER_FUNCS(SRC) \
    TransformHelperFuncs NAME_TRANSFORMHELPER_FUNCS(SRC) = { \
        NAME_TRANSFORMHELPER_NN(SRC), \
        NAME_TRANSFORMHELPER_BL(SRC), \
        NAME_TRANSFORMHELPER_BC(SRC), \
    };

#define DEFINE_TRANSFORMHELPERS(SRC) \
    DEFINE_TRANSFORMHELPER_NN(SRC) \
    DEFINE_TRANSFORMHELPER_BL(SRC) \
    DEFINE_TRANSFORMHELPER_BC(SRC) \
    DEFINE_TRANSFORMHELPER_FUNCS(SRC)

/*
 * The macros defined above use the following macro definitions supplied
 * for the various surface types to manipulate pixels and pixel data.
 * The surface-specific macros are typically supplied by header files
 * named after the SurfaceType name (i.e. IntArgb.h, ByteGray.h, etc.).
 *
 * In the macro names in the following definitions, the string <stype>
 * is used as a place holder for the SurfaceType name (i.e. IntArgb).
 * The macros above access these type specific macros using the ANSI
 * CPP token concatenation operator "##".
 *
 * <stype>DataType               A typedef for the type of the pointer
 *                               that is used to access the raster data
 *                               for the given surface type.
 * <stype>PixelStride            Pixel stride for the surface type.
 *
 * Declare<stype>LoadVars        Declare the variables needed to control
 *                               loading color information from an stype
 *                               raster (i.e. lookup tables).
 * Init<stype>LoadVars           Init the lookup table variables.
 * Declare<stype>StoreVars       Declare the storage variables needed to
 *                               control storing pixel data based on the
 *                               pixel coordinate (i.e. dithering variables).
 * Init<stype>StoreVarsY         Init the dither variables for starting Y.
 * Next<stype>StoreVarsY         Increment the dither variables for next Y.
 * Init<stype>StoreVarsX         Init the dither variables for starting X.
 * Next<stype>StoreVarsX         Increment the dither variables for next X.
 *
 * Load<stype>To1IntRgb          Load a pixel and form an INT_RGB integer.
 * Store<stype>From1IntRgb       Store a pixel from an INT_RGB integer.
 * Load<stype>To1IntArgb         Load a pixel and form an INT_ARGB integer.
 * Store<stype>From1IntArgb      Store a pixel from an INT_ARGB integer.
 * Load<stype>To3ByteRgb         Load a pixel into R, G, and B components.
 * Store<stype>From3ByteRgb      Store a pixel from R, G, and B components.
 * Load<stype>To4ByteArgb        Load a pixel into A, R, G, and B components.
 * Store<stype>From4ByteArgb     Store a pixel from A, R, G, and B components.
 * Load<stype>To1ByteGray        Load a pixel and form a BYTE_GRAY byte.
 * Store<stype>From1ByteGray     Store a pixel from a BYTE_GRAY byte.
 *
 * <stype>PixelType              Typedef for a "single quantity pixel" (SQP)
 *                               that can hold the data for one stype pixel.
 * <stype>XparLutEntry           An SQP that can be used to represent a
 *                               transparent pixel for stype.
 * Store<stype>NonXparFromArgb   Store an SQP from an INT_ARGB integer in
 *                               such a way that it would not be confused
 *                               with the XparLutEntry value for stype.
 * <stype>IsXparLutEntry         Test an SQP for the XparLutEntry value.
 * Store<stype>Pixel             Store the pixel data from an SQP.
 * <stype>PixelFromArgb          Converts an INT_ARGB value into the specific
 *                               pixel representation for the surface type.
 *
 * Declare<stype>PixelData       Declare the pixel data variables (PDV) needed
 *                               to hold the elements of pixel data ready to
 *                               store into an stype raster (may be empty for
 *                               stypes whose SQP format is their data format).
 * Extract<stype>PixelData       Extract an SQP value into the PDVs.
 * Store<stype>PixelData         Store the PDVs into an stype raster.
 * XorCopy<stype>PixelData       Xor the PDVs into an stype raster.
 */
#endif /* LoopMacros_h_Included */
