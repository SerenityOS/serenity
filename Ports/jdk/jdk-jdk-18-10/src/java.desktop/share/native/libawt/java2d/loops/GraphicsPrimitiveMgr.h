/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GraphicsPrimitiveMgr_h_Included
#define GraphicsPrimitiveMgr_h_Included

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>
#include "jni.h"

#include "java_awt_AlphaComposite.h"

#include "SurfaceData.h"
#include "SpanIterator.h"

#include "j2d_md.h"

#include "AlphaMath.h"
#include "GlyphImageRef.h"

/*
 * This structure contains all of the information about a particular
 * type of GraphicsPrimitive, such as a FillRect, a MaskFill, or a Blit.
 *
 * A global collection of these structures is declared and initialized
 * to contain the necessary Java (JNI) information so that appropriate
 * Java GraphicsPrimitive objects can be quickly constructed for a set
 * of native loops simply by referencing the necessary entry from that
 * collection for the type of primitive being registered.
 *
 * See PrimitiveTypes.{Blit,BlitBg,FillRect,...} below.
 */
typedef struct _PrimitiveType {
    char                *ClassName;
    jint                srcflags;
    jint                dstflags;
    jclass              ClassObject;
    jmethodID           Constructor;
} PrimitiveType;

/* The integer constants to identify the compositing rule being defined. */
#define RULE_Xor        (java_awt_AlphaComposite_MIN_RULE - 1)
#define RULE_Clear      java_awt_AlphaComposite_CLEAR
#define RULE_Src        java_awt_AlphaComposite_SRC
#define RULE_SrcOver    java_awt_AlphaComposite_SRC_OVER
#define RULE_DstOver    java_awt_AlphaComposite_DST_OVER
#define RULE_SrcIn      java_awt_AlphaComposite_SRC_IN
#define RULE_DstIn      java_awt_AlphaComposite_DST_IN
#define RULE_SrcOut     java_awt_AlphaComposite_SRC_OUT
#define RULE_DstOut     java_awt_AlphaComposite_DST_OUT

/*
 * This structure holds the information retrieved from a Java
 * Composite object for easy transfer to various C functions
 * that implement the inner loop for a native primitive.
 *
 * Currently only AlphaComposite and XORComposite are supported.
 */
typedef struct _CompositeInfo {
    jint        rule;           /* See RULE_* constants above */
    union {
        jfloat  extraAlpha;     /* from AlphaComposite */
        jint    xorPixel;       /* from XORComposite */
    } details;
    juint       alphaMask;      /* from XORComposite */
} CompositeInfo;

/*
 * This structure is the common header for the two native structures
 * that hold information about a particular SurfaceType or CompositeType.
 *
 * A global collection of these structures is declared and initialized
 * to contain the necessary Java (JNI) information so that appropriate
 * Java GraphicsPrimitive objects can be quickly constructed for a set
 * of native loops simply by referencing the necessary entry from that
 * collection for the type of composite or surface being implemented.
 *
 * See SurfaceTypes.{OpaqueColor,IntArgb,ByteGray,...} below.
 * See CompositeTypes.{Xor,AnyAlpha,...} below.
 */
typedef struct _SurfCompHdr {
    char                *Name;
    jobject             Object;
} SurfCompHdr;

/*
 * The definitions for the SurfaceType structure described above.
 */

/*
 * The signature for a function that returns the specific integer
 * format pixel for a given ARGB color value for a particular
 * SurfaceType implementation.
 * This function is valid only after GetRasInfo call for the
 * associated surface.
 */
typedef jint (PixelForFunc)(SurfaceDataRasInfo *pRasInfo, jint rgb);

/*
 * The additional information needed to manipulate a surface:
 * - The pixelFor function for translating ARGB values.
 *   Valid only after GetRasInfo call for this surface.
 * - The additional flags needed when reading from this surface.
 * - The additional flags needed when writing to this surface.
 */
typedef struct _SurfaceType {
    SurfCompHdr         hdr;
    PixelForFunc        *pixelFor;
    jint                readflags;
    jint                writeflags;
} SurfaceType;

/*
 * The definitions for the CompositeType structure described above.
 */

/*
 * The signature for a function that fills in a CompositeInfo
 * structure from the information present in a given Java Composite
 * object.
 */
typedef void (JNICALL CompInfoFunc)(JNIEnv *env,
                                    CompositeInfo *pCompInfo,
                                    jobject Composite);

/*
 * The additional information needed to implement a primitive that
 * performs a particular composite operation:
 * - The getCompInfo function for filling in a CompositeInfo structure.
 * - The additional flags needed for locking the destination surface.
 */
typedef struct _CompositeType {
    SurfCompHdr         hdr;
    CompInfoFunc        *getCompInfo;
    jint                dstflags;
} CompositeType;

/*
 * The signature of the native functions that register a set of
 * related native GraphicsPrimitive functions.
 */
typedef jboolean (RegisterFunc)(JNIEnv *env);

struct _NativePrimitive;        /* forward reference for function typedefs */

/*
 * This empty function signature represents an "old pre-ANSI style"
 * function declaration which makes no claims about the argument list
 * other than that the types of the arguments will undergo argument
 * promotion in the calling conventions.
 * (See section A7.3.2 in K&R 2nd edition.)
 *
 * When trying to statically initialize the function pointer field of
 * a NativePrimitive structure, which is a union of all possible
 * inner loop function signatures, the initializer constant must be
 * compatible with the first field in the union.  This generic function
 * type allows us to assign any function pointer to that union as long
 * as it meets the requirements specified above (i.e. all arguments
 * are compatible with their promoted values according to the old
 * style argument promotion calling semantics).
 *
 * Note: This means that you cannot define an argument to any of
 * these native functions which is a byte or a short as that value
 * would not be passed in the same way for an ANSI-style full prototype
 * calling convention and an old-style argument promotion calling
 * convention.
 */
typedef void (AnyFunc)();

/*
 * The signature of the inner loop function for a "Blit".
 */
typedef void (BlitFunc)(void *pSrc, void *pDst,
                        juint width, juint height,
                        SurfaceDataRasInfo *pSrcInfo,
                        SurfaceDataRasInfo *pDstInfo,
                        struct _NativePrimitive *pPrim,
                        CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "BlitBg".
 */
typedef void (BlitBgFunc)(void *pSrc, void *pDst,
                          juint width, juint height, jint bgpixel,
                          SurfaceDataRasInfo *pSrcInfo,
                          SurfaceDataRasInfo *pDstInfo,
                          struct _NativePrimitive *pPrim,
                          CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "ScaleBlit".
 */
typedef void (ScaleBlitFunc)(void *pSrc, void *pDst,
                             juint dstwidth, juint dstheight,
                             jint sxloc, jint syloc,
                             jint sxinc, jint syinc, jint scale,
                             SurfaceDataRasInfo *pSrcInfo,
                             SurfaceDataRasInfo *pDstInfo,
                             struct _NativePrimitive *pPrim,
                             CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "FillRect".
 */
typedef void (FillRectFunc)(SurfaceDataRasInfo *pRasInfo,
                            jint lox, jint loy,
                            jint hix, jint hiy,
                            jint pixel, struct _NativePrimitive *pPrim,
                            CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "FillSpans".
 */
typedef void (FillSpansFunc)(SurfaceDataRasInfo *pRasInfo,
                             SpanIteratorFuncs *pSpanFuncs, void *siData,
                             jint pixel, struct _NativePrimitive *pPrim,
                             CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "DrawLine".
 * Note that this same inner loop is used for native DrawRect
 * and DrawPolygons primitives.
 */
typedef void (DrawLineFunc)(SurfaceDataRasInfo *pRasInfo,
                            jint x1, jint y1, jint pixel,
                            jint steps, jint error,
                            jint bumpmajormask, jint errmajor,
                            jint bumpminormask, jint errminor,
                            struct _NativePrimitive *pPrim,
                            CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "MaskFill".
 */
typedef void (MaskFillFunc)(void *pRas,
                            unsigned char *pMask, jint maskOff, jint maskScan,
                            jint width, jint height,
                            jint fgColor,
                            SurfaceDataRasInfo *pRasInfo,
                            struct _NativePrimitive *pPrim,
                            CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "MaskBlit".
 */
typedef void (MaskBlitFunc)(void *pDst, void *pSrc,
                            unsigned char *pMask, jint maskOff, jint maskScan,
                            jint width, jint height,
                            SurfaceDataRasInfo *pDstInfo,
                            SurfaceDataRasInfo *pSrcInfo,
                            struct _NativePrimitive *pPrim,
                            CompositeInfo *pCompInfo);
/*
 * The signature of the inner loop function for a "DrawGlyphList".
 */
typedef void (DrawGlyphListFunc)(SurfaceDataRasInfo *pRasInfo,
                                 ImageRef *glyphs,
                                 jint totalGlyphs,
                                 jint fgpixel, jint fgcolor,
                                 jint cx1, jint cy1,
                                 jint cx2, jint cy2,
                                 struct _NativePrimitive *pPrim,
                                 CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "DrawGlyphListAA".
 */
typedef void (DrawGlyphListAAFunc)(SurfaceDataRasInfo *pRasInfo,
                                   ImageRef *glyphs,
                                   jint totalGlyphs,
                                   jint fgpixel, jint fgcolor,
                                   jint cx1, jint cy1,
                                   jint cx2, jint cy2,
                                   struct _NativePrimitive *pPrim,
                                   CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop function for a "DrawGlyphListLCD".
 * rgbOrder is a jint rather than a jboolean so that this typedef matches
 * AnyFunc which is the first element in a union in NativePrimitive's
 * initialiser. See the comments alongside declaration of the AnyFunc type for
 * a full explanation.
 */
typedef void (DrawGlyphListLCDFunc)(SurfaceDataRasInfo *pRasInfo,
                                    ImageRef *glyphs,
                                    jint totalGlyphs,
                                    jint fgpixel, jint fgcolor,
                                    jint cx1, jint cy1,
                                    jint cx2, jint cy2,
                                    jint rgbOrder,
                                    unsigned char *gammaLut,
                                    unsigned char *invGammaLut,
                                    struct _NativePrimitive *pPrim,
                                    CompositeInfo *pCompInfo);

/*
 * The signature of the inner loop functions for a "TransformHelper".
 */
typedef void (TransformHelperFunc)(SurfaceDataRasInfo *pSrcInfo,
                                   jint *pRGB, jint numpix,
                                   jlong xlong, jlong dxlong,
                                   jlong ylong, jlong dylong);

typedef struct {
    TransformHelperFunc         *nnHelper;
    TransformHelperFunc         *blHelper;
    TransformHelperFunc         *bcHelper;
} TransformHelperFuncs;

typedef void (TransformInterpFunc)(jint *pRGBbase, jint numpix,
                                   jint xfract, jint dxfract,
                                   jint yfract, jint dyfract);

/*
 * The signature of the inner loop function for a "FillParallelogram"
 * Note that this same inner loop is used for native DrawParallelogram
 * primitives.
 * Note that these functions are paired with equivalent DrawLine
 * inner loop functions to facilitate nicer looking and faster thin
 * transformed drawrect calls.
 */
typedef void (FillParallelogramFunc)(SurfaceDataRasInfo *pRasInfo,
                                     jint lox, jint loy, jint hix, jint hiy,
                                     jlong leftx, jlong dleftx,
                                     jlong rightx, jlong drightx,
                                     jint pixel, struct _NativePrimitive *pPrim,
                                     CompositeInfo *pCompInfo);

typedef struct {
    FillParallelogramFunc       *fillpgram;
    DrawLineFunc                *drawline;
} DrawParallelogramFuncs;

/*
 * This structure contains all information for defining a single
 * native GraphicsPrimitive, including:
 * - The information about the type of the GraphicsPrimitive subclass.
 * - The information about the type of the source surface.
 * - The information about the type of the compositing operation.
 * - The information about the type of the destination surface.
 * - A pointer to the function that performs the actual inner loop work.
 * - Extra flags needed for locking the source and destination surfaces
 *   above and beyond the flags specified in the Primitive, Composite
 *   and SurfaceType structures.  (For most native primitives these
 *   flags can be calculated automatically from information stored in
 *   the PrimitiveType, SurfaceType, and CompositeType structures.)
 */
typedef struct _NativePrimitive {
    PrimitiveType       *pPrimType;
    SurfaceType         *pSrcType;
    CompositeType       *pCompType;
    SurfaceType         *pDstType;
    /* See declaration of AnyFunc type above for comments explaining why
     * only AnyFunc is used by the initializers for these union fields
     * and consequent type restrictions.
     */
    union {
        AnyFunc                 *initializer;
        BlitFunc                *blit;
        BlitBgFunc              *blitbg;
        ScaleBlitFunc           *scaledblit;
        FillRectFunc            *fillrect;
        FillSpansFunc           *fillspans;
        FillParallelogramFunc   *fillparallelogram;
        DrawParallelogramFuncs  *drawparallelogram;
        DrawLineFunc            *drawline;
        MaskFillFunc            *maskfill;
        MaskBlitFunc            *maskblit;
        DrawGlyphListFunc       *drawglyphlist;
        DrawGlyphListFunc       *drawglyphlistaa;
        DrawGlyphListLCDFunc    *drawglyphlistlcd;
        TransformHelperFuncs    *transformhelpers;
    } funcs, funcs_c;
    jint                srcflags;
    jint                dstflags;
} NativePrimitive;

/*
 * The global collection of all primitive types.  Specific NativePrimitive
 * structures can be statically initialized by pointing to these structures.
 */
extern struct _PrimitiveTypes {
    PrimitiveType       Blit;
    PrimitiveType       BlitBg;
    PrimitiveType       ScaledBlit;
    PrimitiveType       FillRect;
    PrimitiveType       FillSpans;
    PrimitiveType       FillParallelogram;
    PrimitiveType       DrawParallelogram;
    PrimitiveType       DrawLine;
    PrimitiveType       DrawRect;
    PrimitiveType       DrawPolygons;
    PrimitiveType       DrawPath;
    PrimitiveType       FillPath;
    PrimitiveType       MaskBlit;
    PrimitiveType       MaskFill;
    PrimitiveType       DrawGlyphList;
    PrimitiveType       DrawGlyphListAA;
    PrimitiveType       DrawGlyphListLCD;
    PrimitiveType       TransformHelper;
} PrimitiveTypes;

/*
 * The global collection of all surface types.  Specific NativePrimitive
 * structures can be statically initialized by pointing to these structures.
 */
extern struct _SurfaceTypes {
    SurfaceType         OpaqueColor;
    SurfaceType         AnyColor;
    SurfaceType         AnyByte;
    SurfaceType         ByteBinary1Bit;
    SurfaceType         ByteBinary2Bit;
    SurfaceType         ByteBinary4Bit;
    SurfaceType         ByteIndexed;
    SurfaceType         ByteIndexedBm;
    SurfaceType         ByteGray;
    SurfaceType         Index8Gray;
    SurfaceType         Index12Gray;
    SurfaceType         AnyShort;
    SurfaceType         Ushort555Rgb;
    SurfaceType         Ushort555Rgbx;
    SurfaceType         Ushort565Rgb;
    SurfaceType         Ushort4444Argb;
    SurfaceType         UshortGray;
    SurfaceType         UshortIndexed;
    SurfaceType         Any3Byte;
    SurfaceType         ThreeByteBgr;
    SurfaceType         AnyInt;
    SurfaceType         IntArgb;
    SurfaceType         IntArgbPre;
    SurfaceType         IntArgbBm;
    SurfaceType         IntRgb;
    SurfaceType         IntBgr;
    SurfaceType         IntRgbx;
    SurfaceType         Any4Byte;
    SurfaceType         FourByteAbgr;
    SurfaceType         FourByteAbgrPre;
} SurfaceTypes;

/*
 * The global collection of all composite types.  Specific NativePrimitive
 * structures can be statically initialized by pointing to these structures.
 */
extern struct _CompositeTypes {
    CompositeType       SrcNoEa;
    CompositeType       SrcOverNoEa;
    CompositeType       SrcOverBmNoEa;
    CompositeType       Src;
    CompositeType       SrcOver;
    CompositeType       Xor;
    CompositeType       AnyAlpha;
} CompositeTypes;

#define ArraySize(A)    (sizeof(A) / sizeof(A[0]))

#define PtrAddBytes(p, b)               ((void *) (((intptr_t) (p)) + (b)))
#define PtrCoord(p, x, xinc, y, yinc)   PtrAddBytes(p, \
                                                    ((ptrdiff_t)(y))*(yinc) + \
                                                    ((ptrdiff_t)(x))*(xinc))
#define PtrPixelsRow(p, y, scanStride)    PtrAddBytes(p, \
    ((intptr_t) (y)) * (scanStride))

#define PtrPixelsBand(p, y, length, elemSize)    PtrAddBytes(p, \
    ((intptr_t) (y)) * (length) * (elemSize))

/*
 * The function to call with an array of NativePrimitive structures
 * to register them with the Java GraphicsPrimitiveMgr.
 */
extern jboolean RegisterPrimitives(JNIEnv *env,
                                   NativePrimitive *pPrim,
                                   jint NumPrimitives);

/*
 * The utility function to retrieve the NativePrimitive structure
 * from a given Java GraphicsPrimitive object.
 */
extern JNIEXPORT NativePrimitive * JNICALL
GetNativePrim(JNIEnv *env, jobject gp);

/*
 * Utility functions to get values from a Java SunGraphics2D or Color object.
 */
extern JNIEXPORT void JNICALL
GrPrim_Sg2dGetCompInfo(JNIEnv *env, jobject sg2d,
                       NativePrimitive *pPrim,
                       CompositeInfo *pCompInfo);
extern JNIEXPORT jint JNICALL
GrPrim_CompGetXorColor(JNIEnv *env, jobject comp);
extern JNIEXPORT void JNICALL
GrPrim_CompGetXorInfo(JNIEnv *env, CompositeInfo *pCompInfo, jobject comp);
extern JNIEXPORT void JNICALL
GrPrim_CompGetAlphaInfo(JNIEnv *env, CompositeInfo *pCompInfo, jobject comp);

extern JNIEXPORT void JNICALL
GrPrim_Sg2dGetClip(JNIEnv *env, jobject sg2d,
                   SurfaceDataBounds *bounds);

extern JNIEXPORT jint JNICALL
GrPrim_Sg2dGetPixel(JNIEnv *env, jobject sg2d);
extern JNIEXPORT jint JNICALL
GrPrim_Sg2dGetEaRGB(JNIEnv *env, jobject sg2d);
extern JNIEXPORT jint JNICALL
GrPrim_Sg2dGetLCDTextContrast(JNIEnv *env, jobject sg2d);

/*
 * Data structure and functions to retrieve and use
 * AffineTransform objects from the native level.
 */
typedef struct {
    jdouble dxdx;       /* dx in dest space for each dx in src space */
    jdouble dxdy;       /* dx in dest space for each dy in src space */
    jdouble tx;
    jdouble dydx;       /* dy in dest space for each dx in src space */
    jdouble dydy;       /* dy in dest space for each dy in src space */
    jdouble ty;
} TransformInfo;

extern JNIEXPORT void JNICALL
Transform_GetInfo(JNIEnv *env, jobject txform, TransformInfo *pTxInfo);
extern JNIEXPORT void JNICALL
Transform_transform(TransformInfo *pTxInfo, jdouble *pX, jdouble *pY);

void GrPrim_RefineBounds(SurfaceDataBounds *bounds, jint transX, jint transY,
                         jfloat *coords,  jint maxCoords);

JNIEXPORT extern jfieldID path2DTypesID;
JNIEXPORT extern jfieldID path2DNumTypesID;
JNIEXPORT extern jfieldID path2DWindingRuleID;
JNIEXPORT extern jfieldID path2DFloatCoordsID;
JNIEXPORT extern jfieldID sg2dStrokeHintID;
JNIEXPORT extern jint sunHints_INTVAL_STROKE_PURE;

/*
 * Macros for using jlong variables as 32bits.32bits fractional values
 */
#define LongOneHalf     (((jlong) 1) << 31)
#define IntToLong(i)    (((jlong) (i)) << 32)
#define DblToLong(d)    ((jlong) ((d) * IntToLong(1)))
#define LongToDbl(l)    (((jdouble) l) / IntToLong(1))
#define WholeOfLong(l)  ((jint) ((l) >> 32))
#define FractOfLong(l)  ((jint) (l))
#define URShift(i, n)   (((juint) (i)) >> (n))

/*
 * Macros to help in defining arrays of NativePrimitive structures.
 *
 * These macros are the very base macros.  More specific macros are
 * defined in LoopMacros.h.
 *
 * Note that the DrawLine, DrawRect, and DrawPolygons primitives are
 * all registered together from a single shared native function pointer.
 */

#define REGISTER_PRIMITIVE(TYPE, SRC, COMP, DST, FUNC) \
    { \
        & PrimitiveTypes.TYPE, \
        & SurfaceTypes.SRC, \
        & CompositeTypes.COMP, \
        & SurfaceTypes.DST, \
        {FUNC}, \
        {FUNC}, \
        0,   \
        0   \
    }

#define REGISTER_PRIMITIVE_FLAGS(TYPE, SRC, COMP, DST, FUNC, SFLAGS, DFLAGS) \
    { \
        & PrimitiveTypes.TYPE, \
        & SurfaceTypes.SRC, \
        & CompositeTypes.COMP, \
        & SurfaceTypes.DST, \
        {FUNC}, \
        {FUNC}, \
        SFLAGS, \
        DFLAGS, \
    }

#define REGISTER_BLIT(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(Blit, SRC, COMP, DST, FUNC)

#define REGISTER_BLIT_FLAGS(SRC, COMP, DST, FUNC, SFLAGS, DFLAGS) \
    REGISTER_PRIMITIVE_FLAGS(Blit, SRC, COMP, DST, FUNC, SFLAGS, DFLAGS)

#define REGISTER_SCALEBLIT(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(ScaledBlit, SRC, COMP, DST, FUNC)

#define REGISTER_SCALEBLIT_FLAGS(SRC, COMP, DST, FUNC, SFLAGS, DFLAGS) \
    REGISTER_PRIMITIVE_FLAGS(ScaledBlit, SRC, COMP, DST, FUNC, SFLAGS, DFLAGS)

#define REGISTER_BLITBG(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(BlitBg, SRC, COMP, DST, FUNC)

#define REGISTER_FILLRECT(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(FillRect, SRC, COMP, DST, FUNC)

#define REGISTER_FILLSPANS(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(FillSpans, SRC, COMP, DST, FUNC)

#define REGISTER_FILLPGRAM(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(FillParallelogram, SRC, COMP, DST, FUNC), \
    REGISTER_PRIMITIVE(DrawParallelogram, SRC, COMP, DST, FUNC)

#define REGISTER_LINE_PRIMITIVES(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(DrawLine, SRC, COMP, DST, FUNC), \
    REGISTER_PRIMITIVE(DrawRect, SRC, COMP, DST, FUNC), \
    REGISTER_PRIMITIVE(DrawPolygons, SRC, COMP, DST, FUNC), \
    REGISTER_PRIMITIVE(DrawPath, SRC, COMP, DST, FUNC), \
    REGISTER_PRIMITIVE(FillPath, SRC, COMP, DST, FUNC)

#define REGISTER_MASKBLIT(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(MaskBlit, SRC, COMP, DST, FUNC)

#define REGISTER_MASKFILL(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(MaskFill, SRC, COMP, DST, FUNC)

#define REGISTER_DRAWGLYPHLIST(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(DrawGlyphList, SRC, COMP, DST, FUNC)

#define REGISTER_DRAWGLYPHLISTAA(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(DrawGlyphListAA, SRC, COMP, DST, FUNC)

#define REGISTER_DRAWGLYPHLISTLCD(SRC, COMP, DST, FUNC) \
    REGISTER_PRIMITIVE(DrawGlyphListLCD, SRC, COMP, DST, FUNC)

#ifdef __cplusplus
};
#endif

#endif /* GraphicsPrimitiveMgr_h_Included */
