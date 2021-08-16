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

#include "jni_util.h"
#include "jlong.h"

#include "sun_java2d_loops_GraphicsPrimitiveMgr.h"

#include "Region.h"
#include "GraphicsPrimitiveMgr.h"
#include "AlphaMacros.h"

static char *InitName = "<init>";
static char *InitSig =  ("(JLsun/java2d/loops/SurfaceType;"
                         "Lsun/java2d/loops/CompositeType;"
                         "Lsun/java2d/loops/SurfaceType;)V");

static char *RegisterName =     "register";
static char *RegisterSig =      "([Lsun/java2d/loops/GraphicsPrimitive;)V";

static jclass GraphicsPrimitiveMgr;
static jclass GraphicsPrimitive;

static jmethodID RegisterID;
static jfieldID pNativePrimID;
static jfieldID pixelID;
static jfieldID eargbID;
static jfieldID clipRegionID;
static jfieldID compositeID;
static jfieldID lcdTextContrastID;
static jfieldID xorPixelID;
static jfieldID xorColorID;
static jfieldID alphaMaskID;
static jfieldID ruleID;
static jfieldID extraAlphaID;

static jfieldID m00ID;
static jfieldID m01ID;
static jfieldID m02ID;
static jfieldID m10ID;
static jfieldID m11ID;
static jfieldID m12ID;

static jmethodID getRgbID;

static jboolean InitPrimTypes(JNIEnv *env);
static jboolean InitSurfaceTypes(JNIEnv *env, jclass SurfaceType);
static jboolean InitCompositeTypes(JNIEnv *env, jclass CompositeType);

JNIEXPORT jfieldID path2DTypesID;
JNIEXPORT jfieldID path2DNumTypesID;
JNIEXPORT jfieldID path2DWindingRuleID;
JNIEXPORT jfieldID path2DFloatCoordsID;
JNIEXPORT jfieldID sg2dStrokeHintID;
JNIEXPORT jint sunHints_INTVAL_STROKE_PURE;

/*
 * Class:     sun_java2d_loops_GraphicsPrimitiveMgr
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_GraphicsPrimitiveMgr_initIDs
    (JNIEnv *env, jclass GPMgr,
     jclass GP, jclass ST, jclass CT,
     jclass SG2D, jclass Color, jclass AT,
     jclass XORComp, jclass AlphaComp,
     jclass Path2D, jclass Path2DFloat,
     jclass SHints)
{
    jfieldID fid;
    initAlphaTables();
    GraphicsPrimitiveMgr = (*env)->NewGlobalRef(env, GPMgr);
    GraphicsPrimitive = (*env)->NewGlobalRef(env, GP);
    if (GraphicsPrimitiveMgr == NULL || GraphicsPrimitive == NULL) {
        JNU_ThrowOutOfMemoryError(env, "creating global refs");
        return;
    }
    if (!InitPrimTypes(env) ||
        !InitSurfaceTypes(env, ST) ||
        !InitCompositeTypes(env, CT))
    {
        return;
    }
    CHECK_NULL(RegisterID =
        (*env)->GetStaticMethodID(env, GPMgr, RegisterName, RegisterSig));
    CHECK_NULL(pNativePrimID = (*env)->GetFieldID(env, GP, "pNativePrim", "J"));
    CHECK_NULL(pixelID = (*env)->GetFieldID(env, SG2D, "pixel", "I"));
    CHECK_NULL(eargbID = (*env)->GetFieldID(env, SG2D, "eargb", "I"));
    CHECK_NULL(clipRegionID =
        (*env)->GetFieldID(env, SG2D, "clipRegion", "Lsun/java2d/pipe/Region;"));
    CHECK_NULL(compositeID =
        (*env)->GetFieldID(env, SG2D, "composite", "Ljava/awt/Composite;"));
    CHECK_NULL(lcdTextContrastID =
        (*env)->GetFieldID(env, SG2D, "lcdTextContrast", "I"));
    CHECK_NULL(getRgbID = (*env)->GetMethodID(env, Color, "getRGB", "()I"));
    CHECK_NULL(xorPixelID = (*env)->GetFieldID(env, XORComp, "xorPixel", "I"));
    CHECK_NULL(xorColorID =
        (*env)->GetFieldID(env, XORComp, "xorColor", "Ljava/awt/Color;"));
    CHECK_NULL(alphaMaskID =
        (*env)->GetFieldID(env, XORComp, "alphaMask", "I"));
    CHECK_NULL(ruleID = (*env)->GetFieldID(env, AlphaComp, "rule", "I"));
    CHECK_NULL(extraAlphaID =
        (*env)->GetFieldID(env, AlphaComp, "extraAlpha", "F"));


    CHECK_NULL(m00ID = (*env)->GetFieldID(env, AT, "m00", "D"));
    CHECK_NULL(m01ID = (*env)->GetFieldID(env, AT, "m01", "D"));
    CHECK_NULL(m02ID = (*env)->GetFieldID(env, AT, "m02", "D"));
    CHECK_NULL(m10ID = (*env)->GetFieldID(env, AT, "m10", "D"));
    CHECK_NULL(m11ID = (*env)->GetFieldID(env, AT, "m11", "D"));
    CHECK_NULL(m12ID = (*env)->GetFieldID(env, AT, "m12", "D"));

    CHECK_NULL(path2DTypesID =
        (*env)->GetFieldID(env, Path2D, "pointTypes", "[B"));
    CHECK_NULL(path2DNumTypesID =
        (*env)->GetFieldID(env, Path2D, "numTypes", "I"));
    CHECK_NULL(path2DWindingRuleID =
        (*env)->GetFieldID(env, Path2D, "windingRule", "I"));
    CHECK_NULL(path2DFloatCoordsID =
        (*env)->GetFieldID(env, Path2DFloat, "floatCoords", "[F"));
    CHECK_NULL(sg2dStrokeHintID =
        (*env)->GetFieldID(env, SG2D, "strokeHint", "I"));
    CHECK_NULL(fid =
        (*env)->GetStaticFieldID(env, SHints, "INTVAL_STROKE_PURE", "I"));
    sunHints_INTVAL_STROKE_PURE = (*env)->GetStaticIntField(env, SHints, fid);
}

void GrPrim_RefineBounds(SurfaceDataBounds *bounds, jint transX, jint transY,
                         jfloat *coords,  jint maxCoords)
{
    jint xmin, ymin, xmax, ymax;
    if (maxCoords > 1) {
        xmin = xmax = transX + (jint)(*coords++ + 0.5);
        ymin = ymax = transY + (jint)(*coords++ + 0.5);
        for (;maxCoords > 1; maxCoords -= 2) {
            jint x = transX + (jint)(*coords++ + 0.5);
            jint y = transY + (jint)(*coords++ + 0.5);
            if (xmin > x) xmin = x;
            if (ymin > y) ymin = y;
            if (xmax < x) xmax = x;
            if (ymax < y) ymax = y;
        }
        if (++xmax < xmin) xmax--;
        if (++ymax < ymin) ymax--;
        if (bounds->x1 < xmin) bounds->x1 = xmin;
        if (bounds->y1 < ymin) bounds->y1 = ymin;
        if (bounds->x2 > xmax) bounds->x2 = xmax;
        if (bounds->y2 > ymax) bounds->y2 = ymax;
    } else {
        bounds->x2 = bounds->x1;
        bounds->y2 = bounds->y1;
    }
}

/*
 * Class:     sun_java2d_loops_GraphicsPrimitiveMgr
 * Method:    registerNativeLoops
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_GraphicsPrimitiveMgr_registerNativeLoops
    (JNIEnv *env, jclass GPMgr)
{
    RegisterFunc RegisterAnyByte;
    RegisterFunc RegisterByteBinary1Bit;
    RegisterFunc RegisterByteBinary2Bit;
    RegisterFunc RegisterByteBinary4Bit;
    RegisterFunc RegisterByteIndexed;
    RegisterFunc RegisterByteGray;
    RegisterFunc RegisterIndex8Gray;
    RegisterFunc RegisterIndex12Gray;
    RegisterFunc RegisterAnyShort;
    RegisterFunc RegisterUshort555Rgb;
    RegisterFunc RegisterUshort565Rgb;
    RegisterFunc RegisterUshort4444Argb;
    RegisterFunc RegisterUshort555Rgbx;
    RegisterFunc RegisterUshortGray;
    RegisterFunc RegisterUshortIndexed;
    RegisterFunc RegisterAny3Byte;
    RegisterFunc RegisterThreeByteBgr;
    RegisterFunc RegisterAnyInt;
    RegisterFunc RegisterIntArgb;
    RegisterFunc RegisterIntArgbPre;
    RegisterFunc RegisterIntArgbBm;
    RegisterFunc RegisterIntRgb;
    RegisterFunc RegisterIntBgr;
    RegisterFunc RegisterIntRgbx;
    RegisterFunc RegisterAny4Byte;
    RegisterFunc RegisterFourByteAbgr;
    RegisterFunc RegisterFourByteAbgrPre;

    if (!RegisterAnyByte(env) ||
        !RegisterByteBinary1Bit(env) ||
        !RegisterByteBinary2Bit(env) ||
        !RegisterByteBinary4Bit(env) ||
        !RegisterByteIndexed(env) ||
        !RegisterByteGray(env) ||
        !RegisterIndex8Gray(env) ||
        !RegisterIndex12Gray(env) ||
        !RegisterAnyShort(env) ||
        !RegisterUshort555Rgb(env) ||
        !RegisterUshort565Rgb(env) ||
        !RegisterUshort4444Argb(env) ||
        !RegisterUshort555Rgbx(env) ||
        !RegisterUshortGray(env) ||
        !RegisterUshortIndexed(env) ||
        !RegisterAny3Byte(env) ||
        !RegisterThreeByteBgr(env) ||
        !RegisterAnyInt(env) ||
        !RegisterIntArgb(env) ||
        !RegisterIntArgbPre(env) ||
        !RegisterIntArgbBm(env) ||
        !RegisterIntRgb(env) ||
        !RegisterIntBgr(env) ||
        !RegisterIntRgbx(env) ||
        !RegisterAny4Byte(env) ||
        !RegisterFourByteAbgr(env) ||
        !RegisterFourByteAbgrPre(env))
    {
        return;
    }
}

#define _StartOf(T)     ((T *) (&T##s))
#define _NumberOf(T)    (sizeof(T##s) / sizeof(T))
#define _EndOf(T)       (_StartOf(T) + _NumberOf(T))

#define PrimTypeStart   _StartOf(PrimitiveType)
#define PrimTypeEnd     _EndOf(PrimitiveType)

#define SurfTypeStart   _StartOf(SurfaceType)
#define SurfTypeEnd     _EndOf(SurfaceType)

#define CompTypeStart   _StartOf(CompositeType)
#define CompTypeEnd     _EndOf(CompositeType)

/*
 * This function initializes the global collection of PrimitiveType
 * structures by retrieving the necessary Java Class object and the
 * associated methodID of the necessary constructor.
 *
 * See PrimitiveTypes.* below.
 */
static jboolean InitPrimTypes(JNIEnv *env)
{
    jboolean ok = JNI_TRUE;
    PrimitiveType *pPrimType;
    jclass cl;

    for (pPrimType = PrimTypeStart; pPrimType < PrimTypeEnd; pPrimType++) {
        cl = (*env)->FindClass(env, pPrimType->ClassName);
        if (cl == NULL) {
            ok = JNI_FALSE;
            break;
        }
        pPrimType->ClassObject = (*env)->NewGlobalRef(env, cl);
        pPrimType->Constructor =
            (*env)->GetMethodID(env, cl, InitName, InitSig);

        (*env)->DeleteLocalRef(env, cl);
        if (pPrimType->ClassObject == NULL ||
            pPrimType->Constructor == NULL)
        {
            ok = JNI_FALSE;
            break;
        }
    }

    if (!ok) {
        for (pPrimType = PrimTypeStart; pPrimType < PrimTypeEnd; pPrimType++) {
            if (pPrimType->ClassObject != NULL) {
                (*env)->DeleteGlobalRef(env, pPrimType->ClassObject);
                pPrimType->ClassObject = NULL;
            }
            pPrimType->Constructor = NULL;
        }
    }

    return ok;
}

/*
 * This function initializes the global collection of SurfaceType
 * or CompositeType structures by retrieving the corresponding Java
 * object stored as a static field on the Java Class.
 *
 * See SurfaceTypes.* below.
 * See CompositeeTypes.* below.
 */
static jboolean InitSimpleTypes
    (JNIEnv *env, jclass SimpleClass, char *SimpleSig,
     SurfCompHdr *pStart, SurfCompHdr *pEnd, jsize size)
{
    jboolean ok = JNI_TRUE;
    SurfCompHdr *pHdr;
    jfieldID field;
    jobject obj;

    for (pHdr = pStart; pHdr < pEnd; pHdr = PtrAddBytes(pHdr, size)) {
        field = (*env)->GetStaticFieldID(env,
                                         SimpleClass,
                                         pHdr->Name,
                                         SimpleSig);
        if (field == NULL) {
            ok = JNI_FALSE;
            break;
        }
        obj = (*env)->GetStaticObjectField(env, SimpleClass, field);
        if (obj == NULL) {
            ok = JNI_FALSE;
            break;
        }
        pHdr->Object = (*env)->NewGlobalRef(env, obj);
        (*env)->DeleteLocalRef(env, obj);
        if (pHdr->Object == NULL) {
            ok = JNI_FALSE;
            break;
        }
    }

    if (!ok) {
        for (pHdr = pStart; pHdr < pEnd; pHdr = PtrAddBytes(pHdr, size)) {
            if (pHdr->Object != NULL) {
                (*env)->DeleteGlobalRef(env, pHdr->Object);
                pHdr->Object = NULL;
            }
        }
    }

    return ok;
}

static jboolean InitSurfaceTypes(JNIEnv *env, jclass ST)
{
    return InitSimpleTypes(env, ST, "Lsun/java2d/loops/SurfaceType;",
                           (SurfCompHdr *) SurfTypeStart,
                           (SurfCompHdr *) SurfTypeEnd,
                           sizeof(SurfaceType));
}

static jboolean InitCompositeTypes(JNIEnv *env, jclass CT)
{
    return InitSimpleTypes(env, CT, "Lsun/java2d/loops/CompositeType;",
                           (SurfCompHdr *) CompTypeStart,
                           (SurfCompHdr *) CompTypeEnd,
                           sizeof(CompositeType));
}

/*
 * This function registers a set of Java GraphicsPrimitive objects
 * based on information stored in an array of NativePrimitive structures.
 */
jboolean RegisterPrimitives(JNIEnv *env,
                            NativePrimitive *pPrim,
                            jint NumPrimitives)
{
    jarray primitives;
    int i;

    primitives = (*env)->NewObjectArray(env, NumPrimitives,
                                        GraphicsPrimitive, NULL);
    if (primitives == NULL) {
        return JNI_FALSE;
    }

    for (i = 0; i < NumPrimitives; i++, pPrim++) {
        jint srcflags, dstflags;
        jobject prim;
        PrimitiveType *pType = pPrim->pPrimType;
        SurfaceType *pSrc = pPrim->pSrcType;
        CompositeType *pComp = pPrim->pCompType;
        SurfaceType *pDst = pPrim->pDstType;

        pPrim->funcs.initializer = pPrim->funcs_c.initializer;

        /*
         * Calculate the necessary SurfaceData lock flags for the
         * source and destination surfaces based on the information
         * stored in the PrimitiveType, SurfaceType, and CompositeType
         * structures.  The starting point is the values that are
         * already stored in the NativePrimitive structure.  These
         * flags are usually left as 0, but can be filled in by
         * native primitive loops that have special needs that are
         * not deducible from their declared attributes.
         */
        srcflags = pPrim->srcflags;
        dstflags = pPrim->dstflags;
        srcflags |= pType->srcflags;
        dstflags |= pType->dstflags;
        dstflags |= pComp->dstflags;
        if (srcflags & SD_LOCK_READ) srcflags |= pSrc->readflags;
        /* if (srcflags & SD_LOCK_WRITE) srcflags |= pSrc->writeflags; */
        if (dstflags & SD_LOCK_READ) dstflags |= pDst->readflags;
        if (dstflags & SD_LOCK_WRITE) dstflags |= pDst->writeflags;
        pPrim->srcflags = srcflags;
        pPrim->dstflags = dstflags;

        prim = (*env)->NewObject(env,
                                 pType->ClassObject,
                                 pType->Constructor,
                                 ptr_to_jlong(pPrim),
                                 pSrc->hdr.Object,
                                 pComp->hdr.Object,
                                 pDst->hdr.Object);
        if (prim == NULL) {
            break;
        }
        (*env)->SetObjectArrayElement(env, primitives, i, prim);
        (*env)->DeleteLocalRef(env, prim);
        if ((*env)->ExceptionCheck(env)) {
            break;
        }
    }

    if (i >= NumPrimitives) {
        /* No error - upcall to GraphicsPrimitiveMgr to register the
         * new primitives... */
        (*env)->CallStaticVoidMethod(env, GraphicsPrimitiveMgr, RegisterID,
                                     primitives);
    }
    (*env)->DeleteLocalRef(env, primitives);

    return !((*env)->ExceptionCheck(env));
}

JNIEXPORT NativePrimitive * JNICALL
GetNativePrim(JNIEnv *env, jobject gp)
{
    NativePrimitive *pPrim;

    pPrim = (NativePrimitive *) JNU_GetLongFieldAsPtr(env, gp, pNativePrimID);
    if (pPrim == NULL) {
        JNU_ThrowInternalError(env, "Non-native Primitive invoked natively");
    }

    return pPrim;
}

JNIEXPORT void JNICALL
GrPrim_Sg2dGetCompInfo(JNIEnv *env, jobject sg2d,
                       NativePrimitive *pPrim, CompositeInfo *pCompInfo)
{
    jobject comp;

    comp = (*env)->GetObjectField(env, sg2d, compositeID);
    (*pPrim->pCompType->getCompInfo)(env, pCompInfo, comp);
    (*env)->DeleteLocalRef(env, comp);
}

JNIEXPORT jint JNICALL
GrPrim_CompGetXorColor(JNIEnv *env, jobject comp)
{
    jobject color;
    jint rgb;

    color = (*env)->GetObjectField(env, comp, xorColorID);
    rgb = (*env)->CallIntMethod(env, color, getRgbID);
    (*env)->DeleteLocalRef(env, color);

    return rgb;
}

JNIEXPORT void JNICALL
GrPrim_Sg2dGetClip(JNIEnv *env, jobject sg2d, SurfaceDataBounds *bounds)
{
    jobject clip = (*env)->GetObjectField(env, sg2d, clipRegionID);
    Region_GetBounds(env, clip, bounds);
}

JNIEXPORT jint JNICALL
GrPrim_Sg2dGetPixel(JNIEnv *env, jobject sg2d)
{
    return (*env)->GetIntField(env, sg2d, pixelID);
}

JNIEXPORT jint JNICALL
GrPrim_Sg2dGetEaRGB(JNIEnv *env, jobject sg2d)
{
    return (*env)->GetIntField(env, sg2d, eargbID);
}

JNIEXPORT jint JNICALL
GrPrim_Sg2dGetLCDTextContrast(JNIEnv *env, jobject sg2d)
{
    return (*env)->GetIntField(env, sg2d, lcdTextContrastID);
}

/*
 * Helper function for CompositeTypes.Xor
 */
JNIEXPORT void JNICALL
GrPrim_CompGetXorInfo(JNIEnv *env, CompositeInfo *pCompInfo, jobject comp)
{
    pCompInfo->rule = RULE_Xor;
    pCompInfo->details.xorPixel = (*env)->GetIntField(env, comp, xorPixelID);
    pCompInfo->alphaMask = (*env)->GetIntField(env, comp, alphaMaskID);
}

/*
 * Helper function for CompositeTypes.AnyAlpha
 */
JNIEXPORT void JNICALL
GrPrim_CompGetAlphaInfo(JNIEnv *env, CompositeInfo *pCompInfo, jobject comp)
{
    pCompInfo->rule =
        (*env)->GetIntField(env, comp, ruleID);
    pCompInfo->details.extraAlpha =
        (*env)->GetFloatField(env, comp, extraAlphaID);
}

JNIEXPORT void JNICALL
Transform_GetInfo(JNIEnv *env, jobject txform, TransformInfo *pTxInfo)
{
    pTxInfo->dxdx = (*env)->GetDoubleField(env, txform, m00ID);
    pTxInfo->dxdy = (*env)->GetDoubleField(env, txform, m01ID);
    pTxInfo->tx   = (*env)->GetDoubleField(env, txform, m02ID);
    pTxInfo->dydx = (*env)->GetDoubleField(env, txform, m10ID);
    pTxInfo->dydy = (*env)->GetDoubleField(env, txform, m11ID);
    pTxInfo->ty   = (*env)->GetDoubleField(env, txform, m12ID);
}

JNIEXPORT void JNICALL
Transform_transform(TransformInfo *pTxInfo, jdouble *pX, jdouble *pY)
{
    jdouble x = *pX;
    jdouble y = *pY;

    *pX = pTxInfo->dxdx * x + pTxInfo->dxdy * y + pTxInfo->tx;
    *pY = pTxInfo->dydx * x + pTxInfo->dydy * y + pTxInfo->ty;
}

/*
 * External declarations for the pixelFor helper methods for the various
 * named surface types.  These functions are defined in the various
 * files that contain the loop functions for their type.
 */
extern PixelForFunc PixelForByteBinary;
extern PixelForFunc PixelForByteIndexed;
extern PixelForFunc PixelForByteGray;
extern PixelForFunc PixelForIndex8Gray;
extern PixelForFunc PixelForIndex12Gray;
extern PixelForFunc PixelForUshort555Rgb;
extern PixelForFunc PixelForUshort555Rgbx;
extern PixelForFunc PixelForUshort565Rgb;
extern PixelForFunc PixelForUshort4444Argb;
extern PixelForFunc PixelForUshortGray;
extern PixelForFunc PixelForUshortIndexed;
extern PixelForFunc PixelForIntArgbPre;
extern PixelForFunc PixelForIntArgbBm;
extern PixelForFunc PixelForIntBgr;
extern PixelForFunc PixelForIntRgbx;
extern PixelForFunc PixelForFourByteAbgr;
extern PixelForFunc PixelForFourByteAbgrPre;

/*
 * Definition and initialization of the globally accessible PrimitiveTypes.
 */
struct _PrimitiveTypes PrimitiveTypes = {
    { "sun/java2d/loops/Blit", SD_LOCK_READ, SD_LOCK_WRITE, NULL, NULL},
    { "sun/java2d/loops/BlitBg", SD_LOCK_READ, SD_LOCK_WRITE, NULL, NULL},
    { "sun/java2d/loops/ScaledBlit", SD_LOCK_READ, SD_LOCK_WRITE, NULL, NULL},
    { "sun/java2d/loops/FillRect", 0, SD_LOCK_WRITE, NULL, NULL},
    { "sun/java2d/loops/FillSpans", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/FillParallelogram", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/DrawParallelogram", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/DrawLine", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/DrawRect", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/DrawPolygons", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/DrawPath", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/FillPath", 0, SD_LOCK_PARTIAL_WRITE, NULL, NULL},
    { "sun/java2d/loops/MaskBlit", SD_LOCK_READ, SD_LOCK_RD_WR, NULL, NULL},
    { "sun/java2d/loops/MaskFill", 0, SD_LOCK_RD_WR, NULL, NULL},
    { "sun/java2d/loops/DrawGlyphList", 0, SD_LOCK_PARTIAL_WRITE |
                                           SD_LOCK_FASTEST, NULL, NULL},
    { "sun/java2d/loops/DrawGlyphListAA", 0, SD_LOCK_RD_WR | SD_LOCK_FASTEST, NULL, NULL},
    { "sun/java2d/loops/DrawGlyphListLCD", 0, SD_LOCK_RD_WR | SD_LOCK_FASTEST, NULL, NULL},
    { "sun/java2d/loops/TransformHelper", SD_LOCK_READ, 0, NULL, NULL}
};

/*
 * Definition and initialization of the globally accessible SurfaceTypes.
 */
struct _SurfaceTypes SurfaceTypes = {
    { { "OpaqueColor", NULL}, NULL, 0, 0 },
    { { "AnyColor", NULL}, NULL, 0, 0 },
    { { "AnyByte", NULL}, NULL, 0, 0 },
    { { "ByteBinary1Bit", NULL},
      PixelForByteBinary, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "ByteBinary2Bit", NULL},
      PixelForByteBinary, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "ByteBinary4Bit", NULL},
      PixelForByteBinary, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "ByteIndexed", NULL},
      PixelForByteIndexed, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "ByteIndexedBm", NULL},
      PixelForByteIndexed, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "ByteGray", NULL}, PixelForByteGray, 0, 0},
    { { "Index8Gray", NULL},
      PixelForIndex8Gray, SD_LOCK_LUT, SD_LOCK_INVGRAY },
    { { "Index12Gray", NULL},
      PixelForIndex12Gray, SD_LOCK_LUT, SD_LOCK_INVGRAY },
    { { "AnyShort", NULL}, NULL, 0, 0 },
    { { "Ushort555Rgb", NULL}, PixelForUshort555Rgb, 0, 0},
    { { "Ushort555Rgbx", NULL}, PixelForUshort555Rgbx, 0, 0},
    { { "Ushort565Rgb", NULL}, PixelForUshort565Rgb, 0, 0 },
    { { "Ushort4444Argb", NULL}, PixelForUshort4444Argb, 0, 0 },
    { { "UshortGray", NULL}, PixelForUshortGray, 0, 0},
    { { "UshortIndexed", NULL},
      PixelForUshortIndexed, SD_LOCK_LUT, SD_LOCK_INVCOLOR },
    { { "Any3Byte", NULL},  NULL, 0, 0 },
    { { "ThreeByteBgr", NULL},  NULL, 0, 0 },
    { { "AnyInt", NULL}, NULL, 0, 0 },
    { { "IntArgb", NULL},  NULL, 0, 0 },
    { { "IntArgbPre", NULL}, PixelForIntArgbPre, 0, 0},
    { { "IntArgbBm", NULL}, PixelForIntArgbBm, 0, 0},
    { { "IntRgb", NULL},  NULL, 0, 0 },
    { { "IntBgr", NULL}, PixelForIntBgr, 0, 0},
    { { "IntRgbx", NULL}, PixelForIntRgbx, 0, 0},
    { { "Any4Byte", NULL},  NULL, 0, 0 },
    { { "FourByteAbgr", NULL}, PixelForFourByteAbgr, 0, 0},
    { { "FourByteAbgrPre", NULL}, PixelForFourByteAbgrPre, 0, 0},
};

/*
 * Definition and initialization of the globally accessible CompositeTypes.
 */
struct _CompositeTypes CompositeTypes = {
    { { "SrcNoEa", NULL}, NULL, 0},
    { { "SrcOverNoEa", NULL}, NULL, SD_LOCK_RD_WR },
    { { "SrcOverNoEa", NULL}, NULL, SD_LOCK_PARTIAL_WRITE }, /* SrcOverBmNoEa */
    { { "Src", NULL}, GrPrim_CompGetAlphaInfo, 0},
    { { "SrcOver", NULL}, GrPrim_CompGetAlphaInfo, SD_LOCK_RD_WR },
    { { "Xor", NULL}, GrPrim_CompGetXorInfo, SD_LOCK_RD_WR },
    { { "AnyAlpha", NULL}, GrPrim_CompGetAlphaInfo, SD_LOCK_RD_WR },
};
