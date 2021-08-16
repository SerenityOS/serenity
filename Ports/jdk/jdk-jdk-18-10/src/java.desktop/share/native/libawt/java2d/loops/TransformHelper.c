/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include "jni_util.h"
#include "math.h"

#include "GraphicsPrimitiveMgr.h"
#include "Region.h"

#include "sun_java2d_loops_TransformHelper.h"
#include "java_awt_image_AffineTransformOp.h"

/*
 * The stub functions replace the bilinear and bicubic interpolation
 * functions with NOP versions so that the performance of the helper
 * functions that fetch the data can be more directly tested.  They
 * are not compiled or enabled by default.  Change the following
 * #undef to a #define to build the stub functions.
 *
 * When compiled, they are enabled by the environment variable TXSTUB.
 * When compiled, there is also code to disable the VIS versions and
 * use the C versions in this file in their place by defining the TXNOVIS
 * environment variable.
 */
#undef MAKE_STUBS

/* The number of IntArgbPre samples to store in the temporary buffer. */
#define LINE_SIZE       2048

/* The size of a stack allocated buffer to hold edge coordinates (see below). */
#define MAXEDGES 1024

/* Declare the software interpolation functions. */
static TransformInterpFunc BilinearInterp;
static TransformInterpFunc BicubicInterp;

#ifdef MAKE_STUBS
/* Optionally Declare the stub interpolation functions. */
static TransformInterpFunc BilinearInterpStub;
static TransformInterpFunc BicubicInterpStub;
#endif /* MAKE_STUBS */

/*
 * Initially choose the software interpolation functions.
 * These choices can be overridden by platform code that runs during the
 * primitive registration phase of initialization by storing pointers to
 * better functions in these pointers.
 * Compiling the stubs also turns on code below that can re-install the
 * software functions or stub functions on the first call to this primitive.
 */
TransformInterpFunc *pBilinearFunc = BilinearInterp;
TransformInterpFunc *pBicubicFunc = BicubicInterp;

/*
 * The dxydxy parameters of the inverse transform determine how
 * quickly we step through the source image.  For tiny scale
 * factors (on the order of 1E-16 or so) the stepping distances
 * are huge.  The image has been scaled so small that stepping
 * a single pixel in device space moves the sampling point by
 * billions (or more) pixels in the source image space.  These
 * huge stepping values can overflow the whole part of the longs
 * we use for the fixed point stepping equations and so we need
 * a more robust solution.  We could simply iterate over every
 * device pixel, use the inverse transform to transform it back
 * into the source image coordinate system and then test it for
 * being in range and sample pixel-by-pixel, but that is quite
 * a bit more expensive.  Fortunately, if the scale factors are
 * so tiny that we overflow our long values then the number of
 * pixels we are planning to visit should be very tiny.  The only
 * exception to that rule is if the scale factor along one
 * dimension is tiny (creating the huge stepping values), and
 * the scale factor along the other dimension is fairly regular
 * or an up-scale.  In that case we have a lot of pixels along
 * the direction of the larger axis to sample, but few along the
 * smaller axis.  Though, pessimally, with an added shear factor
 * such a linearly tiny image could have bounds that cover a large
 * number of pixels.  Such odd transformations should be very
 * rare and the absolute limit on calculations would involve a
 * single reverse transform of every pixel in the output image
 * which is not fast, but it should not cause an undue stall
 * of the rendering software.
 *
 * The specific test we will use is to calculate the inverse
 * transformed values of every corner of the destination bounds
 * (in order to be user-clip independent) and if we can
 * perform a fixed-point-long inverse transform of all of
 * those points without overflowing we will use the fast
 * fixed point algorithm.  Otherwise we will use the safe
 * per-pixel transform algorithm.
 * The 4 corners are 0,0, 0,dsth, dstw,0, dstw,dsth
 * Transformed they are:
 *     tx,               ty
 *     tx       +dxdy*H, ty       +dydy*H
 *     tx+dxdx*W,        ty+dydx*W
 *     tx+dxdx*W+dxdy*H, ty+dydx*W+dydy*H
 */
/* We reject coordinates not less than 1<<30 so that the distance between */
/* any 2 of them is less than 1<<31 which would overflow into the sign */
/* bit of a signed long value used to represent fixed point coordinates. */
#define TX_FIXED_UNSAFE(v)  (fabs(v) >= (1<<30))
static jboolean
checkOverflow(jint dxoff, jint dyoff,
              SurfaceDataBounds *pBounds,
              TransformInfo *pItxInfo,
              jdouble *retx, jdouble *rety)
{
    jdouble x, y;

    x = dxoff+pBounds->x1+0.5; /* Center of pixel x1 */
    y = dyoff+pBounds->y1+0.5; /* Center of pixel y1 */
    Transform_transform(pItxInfo, &x, &y);
    *retx = x;
    *rety = y;
    if (TX_FIXED_UNSAFE(x) || TX_FIXED_UNSAFE(y)) {
        return JNI_TRUE;
    }

    x = dxoff+pBounds->x2-0.5; /* Center of pixel x2-1 */
    y = dyoff+pBounds->y1+0.5; /* Center of pixel y1 */
    Transform_transform(pItxInfo, &x, &y);
    if (TX_FIXED_UNSAFE(x) || TX_FIXED_UNSAFE(y)) {
        return JNI_TRUE;
    }

    x = dxoff+pBounds->x1+0.5; /* Center of pixel x1 */
    y = dyoff+pBounds->y2-0.5; /* Center of pixel y2-1 */
    Transform_transform(pItxInfo, &x, &y);
    if (TX_FIXED_UNSAFE(x) || TX_FIXED_UNSAFE(y)) {
        return JNI_TRUE;
    }

    x = dxoff+pBounds->x2-0.5; /* Center of pixel x2-1 */
    y = dyoff+pBounds->y2-0.5; /* Center of pixel y2-1 */
    Transform_transform(pItxInfo, &x, &y);
    if (TX_FIXED_UNSAFE(x) || TX_FIXED_UNSAFE(y)) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

/*
 * Fill the edge buffer with pairs of coordinates representing the maximum
 * left and right pixels of the destination surface that should be processed
 * on each scanline, clipped to the bounds parameter.
 * The number of scanlines to calculate is implied by the bounds parameter.
 * Only pixels that map back through the specified (inverse) transform to a
 * source coordinate that falls within the (0, 0, sw, sh) bounds of the
 * source image should be processed.
 * pEdges points to an array of jints that holds 2 + numedges*2 values where
 * numedges should match (pBounds->y2 - pBounds->y1).
 * The first two jints in pEdges should be set to y1 and y2 and every pair
 * of jints after that represent the xmin,xmax of all pixels in range of
 * the transformed blit for the corresponding scanline.
 */
static void
calculateEdges(jint *pEdges,
               SurfaceDataBounds *pBounds,
               TransformInfo *pItxInfo,
               jlong xbase, jlong ybase,
               juint sw, juint sh)
{
    jlong dxdxlong, dydxlong;
    jlong dxdylong, dydylong;
    jlong drowxlong, drowylong;
    jint dx1, dy1, dx2, dy2;

    dxdxlong = DblToLong(pItxInfo->dxdx);
    dydxlong = DblToLong(pItxInfo->dydx);
    dxdylong = DblToLong(pItxInfo->dxdy);
    dydylong = DblToLong(pItxInfo->dydy);

    dx1 = pBounds->x1;
    dy1 = pBounds->y1;
    dx2 = pBounds->x2;
    dy2 = pBounds->y2;
    *pEdges++ = dy1;
    *pEdges++ = dy2;

    drowxlong = (dx2-dx1-1) * dxdxlong;
    drowylong = (dx2-dx1-1) * dydxlong;

    while (dy1 < dy2) {
        jlong xlong, ylong;

        dx1 = pBounds->x1;
        dx2 = pBounds->x2;

        xlong = xbase;
        ylong = ybase;
        while (dx1 < dx2 &&
               (((juint) WholeOfLong(ylong)) >= sh ||
                ((juint) WholeOfLong(xlong)) >= sw))
        {
            dx1++;
            xlong += dxdxlong;
            ylong += dydxlong;
        }

        xlong = xbase + drowxlong;
        ylong = ybase + drowylong;
        while (dx2 > dx1 &&
               (((juint) WholeOfLong(ylong)) >= sh ||
                ((juint) WholeOfLong(xlong)) >= sw))
        {
            dx2--;
            xlong -= dxdxlong;
            ylong -= dydxlong;
        }

        *pEdges++ = dx1;
        *pEdges++ = dx2;

        /* Increment to next scanline */
        xbase += dxdylong;
        ybase += dydylong;
        dy1++;
    }
}

static void
Transform_SafeHelper(JNIEnv *env,
                     SurfaceDataOps *srcOps,
                     SurfaceDataOps *dstOps,
                     SurfaceDataRasInfo *pSrcInfo,
                     SurfaceDataRasInfo *pDstInfo,
                     NativePrimitive *pMaskBlitPrim,
                     CompositeInfo *pCompInfo,
                     TransformHelperFunc *pHelperFunc,
                     TransformInterpFunc *pInterpFunc,
                     RegionData *pClipInfo, TransformInfo *pItxInfo,
                     jint *pData, jint *pEdges,
                     jint dxoff, jint dyoff, jint sw, jint sh);

/*
 * Class:     sun_java2d_loops_TransformHelper
 * Method:    Transform
 * Signature: (Lsun/java2d/loops/MaskBlit;Lsun/java2d/SurfaceData;Lsun/java2d/SurfaceData;Ljava/awt/Composite;Lsun/java2d/pipe/Region;Ljava/awt/geom/AffineTransform;IIIIIIIII[I)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_TransformHelper_Transform
    (JNIEnv *env, jobject self,
     jobject maskblit,
     jobject srcData, jobject dstData,
     jobject comp, jobject clip,
     jobject itxform, jint txtype,
     jint sx1, jint sy1, jint sx2, jint sy2,
     jint dx1, jint dy1, jint dx2, jint dy2,
     jintArray edgeArray, jint dxoff, jint dyoff)
{
    SurfaceDataOps *srcOps;
    SurfaceDataOps *dstOps;
    SurfaceDataRasInfo srcInfo;
    SurfaceDataRasInfo dstInfo;
    NativePrimitive *pHelperPrim;
    NativePrimitive *pMaskBlitPrim;
    CompositeInfo compInfo;
    RegionData clipInfo;
    TransformInfo itxInfo;
    jint maxlinepix;
    TransformHelperFunc *pHelperFunc;
    TransformInterpFunc *pInterpFunc;
    jdouble xorig, yorig;
    jlong numedges;
    jint *pEdges;
    jint edgebuf[2 + MAXEDGES * 2];
    union {
        jlong align;
        jint data[LINE_SIZE];
    } rgb;

#ifdef MAKE_STUBS
    static int th_initialized;

    /* For debugging only - used to swap in alternate funcs for perf testing */
    if (!th_initialized) {
        if (getenv("TXSTUB") != 0) {
            pBilinearFunc = BilinearInterpStub;
            pBicubicFunc = BicubicInterpStub;
        } else if (getenv("TXNOVIS") != 0) {
            pBilinearFunc = BilinearInterp;
            pBicubicFunc = BicubicInterp;
        }
        th_initialized = 1;
    }
#endif /* MAKE_STUBS */

    pHelperPrim = GetNativePrim(env, self);
    if (pHelperPrim == NULL) {
        /* Should never happen... */
        return;
    }
    pMaskBlitPrim = GetNativePrim(env, maskblit);
    if (pMaskBlitPrim == NULL) {
        /* Exception was thrown by GetNativePrim */
        return;
    }
    if (pMaskBlitPrim->pCompType->getCompInfo != NULL) {
        (*pMaskBlitPrim->pCompType->getCompInfo)(env, &compInfo, comp);
    }
    if (Region_GetInfo(env, clip, &clipInfo)) {
        return;
    }

    srcOps = SurfaceData_GetOps(env, srcData);
    if (srcOps == 0) {
        return;
    }
    dstOps = SurfaceData_GetOps(env, dstData);
    if (dstOps == 0) {
        return;
    }

    /*
     * Grab the appropriate pointer to the helper and interpolation
     * routines and calculate the maximum number of destination pixels
     * that can be processed in one intermediate buffer based on the
     * size of the buffer and the number of samples needed per pixel.
     */
    switch (txtype) {
    case java_awt_image_AffineTransformOp_TYPE_NEAREST_NEIGHBOR:
        pHelperFunc = pHelperPrim->funcs.transformhelpers->nnHelper;
        pInterpFunc = NULL;
        maxlinepix = LINE_SIZE;
        break;
    case java_awt_image_AffineTransformOp_TYPE_BILINEAR:
        pHelperFunc = pHelperPrim->funcs.transformhelpers->blHelper;
        pInterpFunc = pBilinearFunc;
        maxlinepix = LINE_SIZE / 4;
        break;
    case java_awt_image_AffineTransformOp_TYPE_BICUBIC:
        pHelperFunc = pHelperPrim->funcs.transformhelpers->bcHelper;
        pInterpFunc = pBicubicFunc;
        maxlinepix = LINE_SIZE / 16;
        break;
    default:
        // Should not happen, but just in case.
        return;
    }

    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;
    dstInfo.bounds.x1 = dx1;
    dstInfo.bounds.y1 = dy1;
    dstInfo.bounds.x2 = dx2;
    dstInfo.bounds.y2 = dy2;
    SurfaceData_IntersectBounds(&dstInfo.bounds, &clipInfo.bounds);
    if (srcOps->Lock(env, srcOps, &srcInfo, pHelperPrim->srcflags)
        != SD_SUCCESS)
    {
        /* edgeArray should already contain zeros for min/maxy */
        return;
    }
    if (dstOps->Lock(env, dstOps, &dstInfo, pMaskBlitPrim->dstflags)
        != SD_SUCCESS)
    {
        SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
        /* edgeArray should already contain zeros for min/maxy */
        return;
    }
    Region_IntersectBounds(&clipInfo, &dstInfo.bounds);
    Transform_GetInfo(env, itxform, &itxInfo);

    numedges = (((jlong) dstInfo.bounds.y2) - ((jlong) dstInfo.bounds.y1));
    if (numedges <= 0) {
        pEdges = NULL;
    } else if (!JNU_IsNull(env, edgeArray)) {
        /*
         * Ideally Java should allocate an array large enough, but if
         * we ever have a miscommunication about the number of edge
         * lines, or if the Java array calculation should overflow to
         * a positive number and succeed in allocating an array that
         * is too small, we need to verify that it can still hold the
         * number of integers that we plan to store to be safe.
         */
        jsize edgesize = (*env)->GetArrayLength(env, edgeArray);
        /* (edgesize/2 - 1) should avoid any overflow or underflow. */
        pEdges = (((edgesize / 2) - 1) >= numedges)
            ? (*env)->GetPrimitiveArrayCritical(env, edgeArray, NULL)
            : NULL;
    } else if (numedges > MAXEDGES) {
        /* numedges variable (jlong) can be at most ((1<<32)-1) */
        /* memsize can overflow a jint, but not a jlong */
        jlong memsize = ((numedges * 2) + 2) * sizeof(*pEdges);
        pEdges = (memsize == ((size_t) memsize))
            ? malloc((size_t) memsize)
            : NULL;
    } else {
        pEdges = edgebuf;
    }

    if (pEdges == NULL) {
        if (!(*env)->ExceptionCheck(env) && numedges > 0) {
            JNU_ThrowInternalError(env, "Unable to allocate edge list");
        }
        SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
        SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
        /* edgeArray should already contain zeros for min/maxy */
        return;
    }


    if (!Region_IsEmpty(&clipInfo)) {
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        dstOps->GetRasInfo(env, dstOps, &dstInfo);
        if (srcInfo.rasBase == NULL || dstInfo.rasBase == NULL) {
            pEdges[0] = pEdges[1] = 0;
        } else if (checkOverflow(dxoff, dyoff, &dstInfo.bounds,
                                 &itxInfo, &xorig, &yorig))
        {
            Transform_SafeHelper(env, srcOps, dstOps,
                                 &srcInfo, &dstInfo,
                                 pMaskBlitPrim, &compInfo,
                                 pHelperFunc, pInterpFunc,
                                 &clipInfo, &itxInfo, rgb.data, pEdges,
                                 dxoff, dyoff, sx2-sx1, sy2-sy1);
        } else {
            SurfaceDataBounds span;
            jlong dxdxlong, dydxlong;
            jlong dxdylong, dydylong;
            jlong xbase, ybase;

            dxdxlong = DblToLong(itxInfo.dxdx);
            dydxlong = DblToLong(itxInfo.dydx);
            dxdylong = DblToLong(itxInfo.dxdy);
            dydylong = DblToLong(itxInfo.dydy);
            xbase = DblToLong(xorig);
            ybase = DblToLong(yorig);

            calculateEdges(pEdges, &dstInfo.bounds, &itxInfo,
                           xbase, ybase, sx2-sx1, sy2-sy1);

            Region_StartIteration(env, &clipInfo);
            while (Region_NextIteration(&clipInfo, &span)) {
                jlong rowxlong, rowylong;
                void *pDst;

                dy1 = span.y1;
                dy2 = span.y2;
                rowxlong = xbase + (dy1 - dstInfo.bounds.y1) * dxdylong;
                rowylong = ybase + (dy1 - dstInfo.bounds.y1) * dydylong;

                while (dy1 < dy2) {
                    jlong xlong, ylong;

                    /* Note - process at most one scanline at a time. */

                    dx1 = pEdges[(dy1 - dstInfo.bounds.y1) * 2 + 2];
                    dx2 = pEdges[(dy1 - dstInfo.bounds.y1) * 2 + 3];
                    if (dx1 < span.x1) dx1 = span.x1;
                    if (dx2 > span.x2) dx2 = span.x2;

                    /* All pixels from dx1 to dx2 have centers in bounds */
                    while (dx1 < dx2) {
                        /* Can process at most one buffer full at a time */
                        jint numpix = dx2 - dx1;
                        if (numpix > maxlinepix) {
                            numpix = maxlinepix;
                        }

                        xlong =
                            rowxlong + ((dx1 - dstInfo.bounds.x1) * dxdxlong);
                        ylong =
                            rowylong + ((dx1 - dstInfo.bounds.x1) * dydxlong);

                        /* Get IntArgbPre pixel data from source */
                        (*pHelperFunc)(&srcInfo,
                                       rgb.data, numpix,
                                       xlong, dxdxlong,
                                       ylong, dydxlong);

                        /* Interpolate result pixels if needed */
                        if (pInterpFunc) {
                            (*pInterpFunc)(rgb.data, numpix,
                                           FractOfLong(xlong-LongOneHalf),
                                           FractOfLong(dxdxlong),
                                           FractOfLong(ylong-LongOneHalf),
                                           FractOfLong(dydxlong));
                        }

                        /* Store/Composite interpolated pixels into dest */
                        pDst = PtrCoord(dstInfo.rasBase,
                                        dx1, dstInfo.pixelStride,
                                        dy1, dstInfo.scanStride);
                        (*pMaskBlitPrim->funcs.maskblit)(pDst, rgb.data,
                                                         0, 0, 0,
                                                         numpix, 1,
                                                         &dstInfo, &srcInfo,
                                                         pMaskBlitPrim,
                                                         &compInfo);

                        /* Increment to next buffer worth of input pixels */
                        dx1 += maxlinepix;
                    }

                    /* Increment to next scanline */
                    rowxlong += dxdylong;
                    rowylong += dydylong;
                    dy1++;
                }
            }
            Region_EndIteration(env, &clipInfo);
        }
        SurfaceData_InvokeRelease(env, dstOps, &dstInfo);
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    } else {
        pEdges[0] = pEdges[1] = 0;
    }

    if (!JNU_IsNull(env, edgeArray)) {
        (*env)->ReleasePrimitiveArrayCritical(env, edgeArray, pEdges, 0);
    } else if (pEdges != edgebuf) {
        free(pEdges);
    }
    SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
}

static void
Transform_SafeHelper(JNIEnv *env,
                     SurfaceDataOps *srcOps,
                     SurfaceDataOps *dstOps,
                     SurfaceDataRasInfo *pSrcInfo,
                     SurfaceDataRasInfo *pDstInfo,
                     NativePrimitive *pMaskBlitPrim,
                     CompositeInfo *pCompInfo,
                     TransformHelperFunc *pHelperFunc,
                     TransformInterpFunc *pInterpFunc,
                     RegionData *pClipInfo, TransformInfo *pItxInfo,
                     jint *pData, jint *pEdges,
                     jint dxoff, jint dyoff, jint sw, jint sh)
{
    SurfaceDataBounds span;
    jint dx1, dx2;
    jint dy1, dy2;
    jint i, iy;

    dy1 = pDstInfo->bounds.y1;
    dy2 = pDstInfo->bounds.y2;
    dx1 = pDstInfo->bounds.x1;
    dx2 = pDstInfo->bounds.x2;
    pEdges[0] = dy1;
    pEdges[1] = dy2;
    for (iy = dy1; iy < dy2; iy++) {
        jint i = (iy - dy1) * 2;
        /* row spans are set to max,min until we find a pixel in range below */
        pEdges[i + 2] = dx2;
        pEdges[i + 3] = dx1;
    }

    Region_StartIteration(env, pClipInfo);
    while (Region_NextIteration(pClipInfo, &span)) {
        dy1 = span.y1;
        dy2 = span.y2;
        while (dy1 < dy2) {
            dx1 = span.x1;
            dx2 = span.x2;
            i = (dy1 - pDstInfo->bounds.y1) * 2;
            while (dx1 < dx2) {
                jdouble x, y;
                jlong xlong, ylong;

                x = dxoff + dx1 + 0.5;
                y = dyoff + dy1 + 0.5;
                Transform_transform(pItxInfo, &x, &y);
                xlong = DblToLong(x);
                ylong = DblToLong(y);

                /* Process only pixels with centers in bounds
                 * Test double values to avoid overflow in conversion
                 * to long values and then also test the long values
                 * in case they rounded up and out of bounds during
                 * the conversion.
                 */
                if (x >= 0 && y >= 0 && x < sw && y < sh &&
                    WholeOfLong(xlong) < sw &&
                    WholeOfLong(ylong) < sh)
                {
                    void *pDst;

                    if (pEdges[i + 2] > dx1) {
                        pEdges[i + 2] = dx1;
                    }
                    if (pEdges[i + 3] <= dx1) {
                        pEdges[i + 3] = dx1 + 1;
                    }

                    /* Get IntArgbPre pixel data from source */
                    (*pHelperFunc)(pSrcInfo,
                                   pData, 1,
                                   xlong, 0,
                                   ylong, 0);

                    /* Interpolate result pixels if needed */
                    if (pInterpFunc) {
                        (*pInterpFunc)(pData, 1,
                                       FractOfLong(xlong-LongOneHalf), 0,
                                       FractOfLong(ylong-LongOneHalf), 0);
                    }

                    /* Store/Composite interpolated pixels into dest */
                    pDst = PtrCoord(pDstInfo->rasBase,
                                    dx1, pDstInfo->pixelStride,
                                    dy1, pDstInfo->scanStride);
                    (*pMaskBlitPrim->funcs.maskblit)(pDst, pData,
                                                     0, 0, 0,
                                                     1, 1,
                                                     pDstInfo, pSrcInfo,
                                                     pMaskBlitPrim,
                                                     pCompInfo);
                }

                /* Increment to next input pixel */
                dx1++;
            }

            /* Increment to next scanline */
            dy1++;
        }
    }
    Region_EndIteration(env, pClipInfo);
}

#define BL_INTERP_V1_to_V2_by_F(v1, v2, f) \
    (((v1)<<8) + ((v2)-(v1))*(f))

#define BL_ACCUM(comp) \
    do { \
        jint c1 = ((jubyte *) pRGB)[comp]; \
        jint c2 = ((jubyte *) pRGB)[comp+4]; \
        jint cR = BL_INTERP_V1_to_V2_by_F(c1, c2, xfactor); \
        c1 = ((jubyte *) pRGB)[comp+8]; \
        c2 = ((jubyte *) pRGB)[comp+12]; \
        c2 = BL_INTERP_V1_to_V2_by_F(c1, c2, xfactor); \
        cR = BL_INTERP_V1_to_V2_by_F(cR, c2, yfactor); \
        ((jubyte *)pRes)[comp] = (jubyte) ((cR + (1<<15)) >> 16); \
    } while (0)

static void
BilinearInterp(jint *pRGB, jint numpix,
               jint xfract, jint dxfract,
               jint yfract, jint dyfract)
{
    jint j;
    jint *pRes = pRGB;

    for (j = 0; j < numpix; j++) {
        jint xfactor;
        jint yfactor;
        xfactor = URShift(xfract, 32-8);
        yfactor = URShift(yfract, 32-8);
        BL_ACCUM(0);
        BL_ACCUM(1);
        BL_ACCUM(2);
        BL_ACCUM(3);
        pRes++;
        pRGB += 4;
        xfract += dxfract;
        yfract += dyfract;
    }
}

#define SAT(val, max) \
    do { \
        val &= ~(val >> 31);  /* negatives become 0 */ \
        val -= max;           /* only overflows are now positive */ \
        val &= (val >> 31);   /* positives become 0 */ \
        val += max;           /* range is now [0 -> max] */ \
    } while (0)

/* For x86, integer multiplies are faster than floating point */
/* Note that on x86 Linux the choice of best algorithm varies
 * depending on the compiler optimization and the processor type.
 * Currently, the sun/awt x86 Linux builds are not optimized so
 * all the variations produce mediocre performance.
 * For now we will use the choice that works best for the Windows
 * build until the (lack of) optimization issues on Linux are resolved.
 */
#define BICUBIC_USE_INT_MATH

#ifdef BICUBIC_USE_DBL_CAST

#define BC_DblToCoeff(v)        (v)
#define BC_COEFF_ONE            1.0
#define BC_TYPE                 jdouble
#define BC_V_HALF               0.5
#define BC_CompToV(v)           ((jdouble) (v))
#define BC_STORE_COMPS(pRes) \
    do { \
        jint a = (jint) accumA; \
        jint r = (jint) accumR; \
        jint g = (jint) accumG; \
        jint b = (jint) accumB; \
        SAT(a, 255); \
        SAT(r, a); \
        SAT(g, a); \
        SAT(b, a); \
        *pRes = ((a << 24) | (r << 16) | (g <<  8) | (b)); \
    } while (0)

#endif /* BICUBIC_USE_DBL_CAST */

#ifdef BICUBIC_USE_DBL_LUT

#define ItoD1(v)    ((jdouble) (v))
#define ItoD4(v)    ItoD1(v),  ItoD1(v+1),   ItoD1(v+2),   ItoD1(v+3)
#define ItoD16(v)   ItoD4(v),  ItoD4(v+4),   ItoD4(v+8),   ItoD4(v+12)
#define ItoD64(v)   ItoD16(v), ItoD16(v+16), ItoD16(v+32), ItoD16(v+48)

static jdouble ItoD_table[] = {
    ItoD64(0), ItoD64(64), ItoD64(128), ItoD64(192)
};

#define BC_DblToCoeff(v)        (v)
#define BC_COEFF_ONE            1.0
#define BC_TYPE                 jdouble
#define BC_V_HALF               0.5
#define BC_CompToV(v)           ItoD_table[v]
#define BC_STORE_COMPS(pRes) \
    do { \
        jint a = (jint) accumA; \
        jint r = (jint) accumR; \
        jint g = (jint) accumG; \
        jint b = (jint) accumB; \
        SAT(a, 255); \
        SAT(r, a); \
        SAT(g, a); \
        SAT(b, a); \
        *pRes = ((a << 24) | (r << 16) | (g <<  8) | (b)); \
    } while (0)

#endif /* BICUBIC_USE_DBL_LUT */

#ifdef BICUBIC_USE_INT_MATH

#define BC_DblToCoeff(v)        ((jint) ((v) * 256))
#define BC_COEFF_ONE            256
#define BC_TYPE                 jint
#define BC_V_HALF               (1 << 15)
#define BC_CompToV(v)           ((jint) v)
#define BC_STORE_COMPS(pRes) \
    do { \
        accumA >>= 16; \
        accumR >>= 16; \
        accumG >>= 16; \
        accumB >>= 16; \
        SAT(accumA, 255); \
        SAT(accumR, accumA); \
        SAT(accumG, accumA); \
        SAT(accumB, accumA); \
        *pRes = ((accumA << 24) | (accumR << 16) | (accumG << 8) | (accumB)); \
    } while (0)

#endif /* BICUBIC_USE_INT_MATH */

#define BC_ACCUM(index, ycindex, xcindex) \
    do { \
        BC_TYPE factor = bicubic_coeff[xcindex] * bicubic_coeff[ycindex]; \
        int rgb; \
        rgb = pRGB[index]; \
        accumB += BC_CompToV((rgb >>  0) & 0xff) * factor; \
        accumG += BC_CompToV((rgb >>  8) & 0xff) * factor; \
        accumR += BC_CompToV((rgb >> 16) & 0xff) * factor; \
        accumA += BC_CompToV((rgb >> 24) & 0xff) * factor; \
    } while (0)

static BC_TYPE bicubic_coeff[513];
static jboolean bicubictableinited;

static void
init_bicubic_table(jdouble A)
{
    /*
     * The following formulas are designed to give smooth
     * results when 'A' is -0.5 or -1.0.
     */
    int i;
    for (i = 0; i < 256; i++) {
        /* r(x) = (A + 2)|x|^3 - (A + 3)|x|^2 + 1 , 0 <= |x| < 1 */
        jdouble x = i / 256.0;
        x = ((A+2)*x - (A+3))*x*x + 1;
        bicubic_coeff[i] = BC_DblToCoeff(x);
    }

    for (; i < 384; i++) {
        /* r(x) = A|x|^3 - 5A|x|^2 + 8A|x| - 4A , 1 <= |x| < 2 */
        jdouble x = i / 256.0;
        x = ((A*x - 5*A)*x + 8*A)*x - 4*A;
        bicubic_coeff[i] = BC_DblToCoeff(x);
    }

    bicubic_coeff[384] = (BC_COEFF_ONE - bicubic_coeff[128]*2) / 2;

    for (i++; i <= 512; i++) {
        bicubic_coeff[i] = BC_COEFF_ONE - (bicubic_coeff[512-i] +
                                           bicubic_coeff[i-256] +
                                           bicubic_coeff[768-i]);
    }

    bicubictableinited = JNI_TRUE;
}

static void
BicubicInterp(jint *pRGB, jint numpix,
              jint xfract, jint dxfract,
              jint yfract, jint dyfract)
{
    jint i;
    jint *pRes = pRGB;

    if (!bicubictableinited) {
        init_bicubic_table(-0.5);
    }

    for (i = 0; i < numpix; i++) {
        BC_TYPE accumA, accumR, accumG, accumB;
        jint xfactor, yfactor;

        xfactor = URShift(xfract, 32-8);
        yfactor = URShift(yfract, 32-8);
        accumA = accumR = accumG = accumB = BC_V_HALF;
        BC_ACCUM(0, yfactor+256, xfactor+256);
        BC_ACCUM(1, yfactor+256, xfactor+  0);
        BC_ACCUM(2, yfactor+256, 256-xfactor);
        BC_ACCUM(3, yfactor+256, 512-xfactor);
        BC_ACCUM(4, yfactor+  0, xfactor+256);
        BC_ACCUM(5, yfactor+  0, xfactor+  0);
        BC_ACCUM(6, yfactor+  0, 256-xfactor);
        BC_ACCUM(7, yfactor+  0, 512-xfactor);
        BC_ACCUM(8, 256-yfactor, xfactor+256);
        BC_ACCUM(9, 256-yfactor, xfactor+  0);
        BC_ACCUM(10, 256-yfactor, 256-xfactor);
        BC_ACCUM(11, 256-yfactor, 512-xfactor);
        BC_ACCUM(12, 512-yfactor, xfactor+256);
        BC_ACCUM(13, 512-yfactor, xfactor+  0);
        BC_ACCUM(14, 512-yfactor, 256-xfactor);
        BC_ACCUM(15, 512-yfactor, 512-xfactor);
        BC_STORE_COMPS(pRes);
        pRes++;
        pRGB += 16;
        xfract += dxfract;
        yfract += dyfract;
    }
}

#ifdef MAKE_STUBS

static void
BilinearInterpStub(jint *pRGBbase, jint numpix,
                   jint xfract, jint dxfract,
                   jint yfract, jint dyfract)
{
    jint *pRGB = pRGBbase;
    while (--numpix >= 0) {
        *pRGBbase = *pRGB;
        pRGBbase += 1;
        pRGB += 4;
    }
}

static void
BicubicInterpStub(jint *pRGBbase, jint numpix,
                  jint xfract, jint dxfract,
                  jint yfract, jint dyfract)
{
    jint *pRGB = pRGBbase+5;
    while (--numpix >= 0) {
        *pRGBbase = *pRGB;
        pRGBbase += 1;
        pRGB += 16;
    }
}

#endif /* MAKE_STUBS */
