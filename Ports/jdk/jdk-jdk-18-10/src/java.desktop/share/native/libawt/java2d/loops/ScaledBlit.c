/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <math.h>

#include "jni_util.h"
#include "GraphicsPrimitiveMgr.h"
#include "Region.h"

#include "sun_java2d_loops_ScaledBlit.h"

/*
 * The scaling loops used inside the helper functions are based on the
 * following pseudocode for stepping through the source image:
 *
 * shift - number of bits of sub-pixel precision in scaled values
 * srcxorig, srcyorig - scaled location of first pixel
 * srcxinc, srcyinc - scaled x and y increments
 * dstwidth, dstheight - number of pixels to process across and down
 *
 * 1. srcy = srcyorig;
 * 2. for (dstheight) {
 * 3.     srcx = srcxorig;
 * 4.     for (dstwidth) {
 * 5.         fetch and process pixel for (srcx >> shift, srcy >> shift)
 * 6.         srcx += srcxinc;
 * 7.     }
 * 8.     srcy += srcyinc;
 * 9. }
 *
 * Note that each execution of line 6 or 8 accumulates error of
 * +/- 1 into the scaled coordinate variables.  These lines are
 * each executed once per pixel across or once per pixel down
 * the region being iterated over, thus the error can accumulate
 * up to a magnitude of dstwidth in the horizontal direction and
 * dstheight in the vertical direction.
 *
 * If the error ever reaches a magnitude of (1 << shift) then we
 * will be off by at least 1 source pixel in our mapping.
 *
 * Note that we increment the source coordinates by the srcxinc
 * and srcyinc variables in each step.  Thus, if our error ever
 * accumulates to a magnitude equal to srcxinc or srcyinc then
 * we will be ahead or behind of "where we should be" by at least
 * one iteration.  Since each iteration is a destination pixel,
 * this means that our actual location will be off by at least
 * one destination pixel.
 *
 * This means that all of the values:
 *
 *     - (1 << shift)
 *     - srcxinc
 *     - srcyinc
 *
 * all represent a maximum bound on how much error we can accumulate
 * before we are off by a source or a destination pixel.  Thus,
 * we should make sure that we never process more than that many
 * pixels if we want to maintain single pixel accuracy.  Even
 * better would be to process many fewer pixels than those bounds
 * to ensure that our accumulated error is much smaller than a
 * pixel.
 */

/*
 * Find and return the largest tile size that is a power of 2 and
 * which is small enough to yield some reassuring degree of subpixel
 * accuracy.  The degree of subpixel accuracy that will be preserved
 * by the tile size it chooses will vary and the details on how
 * it makes this decision are detailed in the comments below.
 */
static jint
findpow2tilesize(jint shift, jint sxinc, jint syinc)
{
    /*
     * The initial value of shift is our first estimate for
     * the power of 2 for our tilesize since it ensures
     * less than 1 source pixel of error.
     *
     * Reducing it until (1 << shift) is not larger than the
     * smallest of our increments ensures we will have no more
     * than 1 destination pixel of error as well.
     */
    if (sxinc > syinc) {
        sxinc = syinc;
    }
    if (sxinc == 0) {
        /* Degenerate case will cause infinite loop in next loop... */
        return 1;
    }
    while ((1 << shift) > sxinc) {
        shift--;
    }
    /*
     * shift is now the largest it can be for less than 1 pixel
     * of error in either source or destination spaces.
     *
     * Now we will try for at least 8 bits of subpixel accuracy
     * with a tile size of at least 256x256 and reduce our subpixel
     * accuracy on a sliding scale down to a tilesize of 1x1 when
     * we have no bits of sub-pixel accuracy.
     */
    if (shift >= 16) {
        /* Subtracting 8 asks for 8 bits of subpixel accuracy. */
        shift -= 8;
    } else {
        /* Ask for half of the remaining bits to be subpixel accuracy. */
        /* Rounding is in favor of subpixel accuracy over tile size. */
        /* Worst case, shift == 0 and tilesize == (1 << 0) == 1 */
        shift /= 2;
    }
    return (1 << shift);
}

/*
 * For a given integer destination pixel coordinate "id", calculate the
 * integer destination coordinate of the start of the "ts" sized tile
 * in which it resides.
 * Tiles all start at even multiples of the tile size from the integer
 * destination origin "io".
 *
 * id == integer destination coordinate
 * io == integer destination operation origin
 * ts == tilesize (must be power of 2)
 */
#define TILESTART(id, io, ts)   ((io) + (((id)-(io)) & (~((ts)-1))))

/*
 * For a given integer destination pixel coordinate "id", calculate the
 * sub-pixel accurate source coordinate from which its sample comes.
 * The returned source coordinate is expressed in a shifted fractional
 * arithmetic number system.
 *
 * id == integer destination coordinate
 * fo == floating point destination operation origin,
 * sf == source coordinate scale factor per destination pixel
 *       (multiplied by fractional arithmetic "shift")
 *
 * The caller is required to cast this value to the appropriate
 * integer type for the needed precision.  The rendering code which
 * deals only with valid coordinates within the bounds of the source
 * rectangle uses jint.  The setup code, which occasionally deals
 * with coordinates that run out of bounds, uses jlong.
 *
 * Note that the rounding in this calculation is at a fraction of a
 * source pixel of (1.0 / (1<<shift)) since the scale factor includes
 * the fractional shift.  As a result, the type of rounding used is
 * not very significant (floor, floor(x+.5), or ceil(x-.5)), but the
 * ceil(x-.5) version is used for consistency with the way that pixel
 * coordinates are rounded to assign the ".5" value to the lower
 * integer.
 */
#define SRCLOC(id, fo, sf)   (ceil((((id) + 0.5) - (fo)) * (sf) - 0.5))

/*
 * Reverse map a srctarget coordinate into device space and refine the
 * answer.  More specifically, what we are looking for is the smallest
 * destination coordinate that maps to a source coordinate that is
 * greater than or equal to the given target source coordinate.
 *
 * Note that since the inner loops use math that maps a destination
 * coordinate into source space and that, even though the equation
 * we use below is the theoretical inverse of the dst->src mapping,
 * we cannot rely on floating point math to guarantee that applying
 * both of these equations in sequence will give us an exact mapping
 * of src->dst->src.  Thus, we must search back and forth to see if
 * we really map back to the given source coordinate and that we are
 * the smallest destination coordinate that does so.
 *
 * Note that, in practice, the answer from the initial guess tends to
 * be the right answer most of the time and the loop ends up finding
 * one iteration to be ">= srctarget" and the next to be "< srctarget"
 * and thus finds the answer in 2 iterations.  A small number of
 * times, the initial guess is 1 too low and so we do one iteration
 * at "< srctarget" and the next at ">= srctarget" and again find the
 * answer in 2 iterations.  All cases encountered during testing ended
 * up falling into one of those 2 categories and so the loop was always
 * executed exactly twice.
 *
 * Note also that the calculation of srcloc below may attempt to calculate
 * the src location of the destination pixel which is "1 beyond" the
 * end of the source image.  Since our shift calculation code in the
 * main function only guaranteed that "srcw << shift" did not overflow
 * a 32-bit signed integer, we cannot guarantee that "(srcw+1) << shift"
 * or, more generally, "(srcw << shift)+srcinc" does not overflow.
 * As a result, we perform our calculations here with jlong values
 * so that we aren't affected by this overflow.  Since srcw (shifted)
 * and srcinc are both 32-bit values, their sum cannot possibly overflow
 * a jlong.  In fact, we can step up to a couple of billion steps of
 * size "srcinc" past the end of the image before we have to worry
 * about overflow - in practice, though, the search never steps more
 * than 1 past the end of the image so this buffer is more than enough.
 */
static jint
refine(jint intorigin, jdouble dblorigin, jint tilesize,
       jdouble scale, jint srctarget, jint srcinc)
{
    /* Make a first estimate of dest coordinate from srctarget */
    jint dstloc = (jint) ceil(dblorigin + srctarget / scale - 0.5);
    /* Loop until we get at least one value < and one >= the target */
    jboolean wasneg = JNI_FALSE;
    jboolean waspos = JNI_FALSE;
    jlong lsrcinc = srcinc;
    jlong lsrctarget = srctarget;

    while (JNI_TRUE) {
        /*
         * Find src coordinate from dest coordinate using the same
         * math we will use below when iterating over tiles.
         */
        jint tilestart = TILESTART(dstloc, intorigin, tilesize);
        jlong lsrcloc = (jlong) SRCLOC(tilestart, dblorigin, scale);
        if (dstloc > tilestart) {
            lsrcloc += lsrcinc * ((jlong) dstloc - tilestart);
        }
        if (lsrcloc >= lsrctarget) {
            /*
             * If we were previously less than target, then the current
             * dstloc is the smallest dst which maps >= the target.
             */
            if (wasneg) break;
            dstloc--;
            waspos = JNI_TRUE;
        } else {
            /*
             * If we were previously greater than target, then this must
             * be the first dstloc which maps to < the target.  Since we
             * want the smallest which maps >= the target, increment it
             * first before returning.
             */
            dstloc++;
            if (waspos) break;
            wasneg = JNI_TRUE;
        }
    }
    return dstloc;
}

/*
 * Class:     sun_java2d_loops_ScaledBlit
 * Method:    Scale
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/SurfaceData;Ljava/awt/Composite;Lsun/java2d/pipe/Region;IIIIDDDD)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_ScaledBlit_Scale
    (JNIEnv *env, jobject self,
     jobject srcData, jobject dstData,
     jobject comp, jobject clip,
     jint sx1, jint sy1, jint sx2, jint sy2,
     jdouble ddx1, jdouble ddy1, jdouble ddx2, jdouble ddy2)
{
    SurfaceDataOps *srcOps;
    SurfaceDataOps *dstOps;
    SurfaceDataRasInfo srcInfo;
    SurfaceDataRasInfo dstInfo;
    NativePrimitive *pPrim;
    CompositeInfo compInfo;
    jint sxinc, syinc, shift;
    jint tilesize;
    jint idx1, idy1;
    jdouble scalex, scaley;
    RegionData clipInfo;
    jint dstFlags;
    jboolean xunderflow, yunderflow;

    pPrim = GetNativePrim(env, self);
    if (pPrim == NULL) {
        return;
    }
    if (pPrim->pCompType->getCompInfo != NULL) {
        (*pPrim->pCompType->getCompInfo)(env, &compInfo, comp);
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
     * Determine the precision to use for the fixed point math
     * for the coordinate scaling.
     * - OR together srcw and srch to get the MSB between the two
     * - Next shift it up until it goes negative
     * - Count the shifts and that will be the most accurate
     *   precision available for the fixed point math
     * - a source coordinate of 1.0 will be (1 << shift)
     * - srcw & srch will be (srcw << shift) and (srch << shift)
     *   and will not overflow
     * Note that if srcw or srch are so large that they are
     * negative numbers before shifting, then:
     * - shift will be 0
     * - tilesize will end up being 1x1 tiles
     * - we will brute force calculate the source location
     *   of every destination pixel using the TILESTART and
     *   SRCLOC macros in this function and then call the
     *   scale helper function to copy one pixel at a time.
     * - TILESTART involves mostly jdouble calculations so
     *   it should not have integer overflow problems.
     */
    sxinc = (sx2 - sx1) | (sy2 - sy1);
    shift = 0;
    if (sxinc > 0) {
        while ((sxinc <<= 1) > 0) {
            shift++;
        }
    }
    /*
     * Now determine the scaled integer increments used to traverse
     * the source image for each destination pixel.  Our shift value
     * has been calculated above so that any location within the
     * destination image can be represented as a scaled integer
     * without incurring integer overflow.
     *
     * But we also need to worry about overflow of the sxinc and syinc
     * parameters.  We already know that "srcw<<shift" and "srch<<shift"
     * cannot overflow a jint, and the only time that sxinc and syinc
     * can be larger than those two values is if ddy2-ddy1 or ddx2-ddx1
     * are smaller than 1.  Since this situation implies that the
     * output area is no more than one pixel wide or tall, then we are
     * stepping by distances that are at least the size of the image
     * and only one destination pixel will ever be rendered - thus the
     * amount by which we step is largely irrelevant since after
     * drawing the first "in bounds" pixel, we will step completely
     * out of the source image and render nothing more.  As a result,
     * we assign the appropriate "size of image" stepping parameter
     * for any scale to smaller than one device pixel.
     */
    yunderflow = (ddy2 - ddy1) < 1.0;
    scaley = (((jdouble) (sy2 - sy1)) / (ddy2 - ddy1)) * (1 << shift);
    syinc = (yunderflow ? ((sy2 - sy1) << shift) : (jint) scaley);
    xunderflow = (ddx2 - ddx1) < 1.0;
    scalex = (((jdouble) (sx2 - sx1)) / (ddx2 - ddx1)) * (1 << shift);
    sxinc = (xunderflow ? ((sx2 - sx1) << shift) : (jint) scalex);
    tilesize = findpow2tilesize(shift, sxinc, syinc);


    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;
    if (srcOps->Lock(env, srcOps, &srcInfo, pPrim->srcflags) != SD_SUCCESS) {
        return;
    }
    if (srcInfo.bounds.x2 <= srcInfo.bounds.x1 ||
        srcInfo.bounds.y2 <= srcInfo.bounds.y1)
    {
        SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
        return;
    }

    /*
     * Only refine lower bounds if lower source coordinate was clipped
     * because the math will work out to be exactly idx1, idy1 if not.
     * Always refine upper bounds since we want to make sure not to
     * overstep the source bounds based on the tiled iteration math.
     *
     * For underflow cases, simply check if the SRCLOC for the single
     * destination pixel maps inside the source bounds.  If it does,
     * we render that pixel row or column (and only that pixel row
     * or column).  If it does not, we render nothing.
     */
    idx1 = (jint) ceil(ddx1 - 0.5);
    idy1 = (jint) ceil(ddy1 - 0.5);
    if (xunderflow) {
        jdouble x = sx1 + (SRCLOC(idx1, ddx1, scalex) / (1 << shift));
        dstInfo.bounds.x1 = dstInfo.bounds.x2 = idx1;
        if (x >= srcInfo.bounds.x1 && x < srcInfo.bounds.x2) {
            dstInfo.bounds.x2++;
        }
    } else {
        dstInfo.bounds.x1 = ((srcInfo.bounds.x1 <= sx1)
                             ? idx1
                             : refine(idx1, ddx1, tilesize, scalex,
                                      (srcInfo.bounds.x1-sx1) << shift, sxinc));
        dstInfo.bounds.x2 = refine(idx1, ddx1, tilesize, scalex,
                                   (srcInfo.bounds.x2-sx1) << shift, sxinc);
    }
    if (yunderflow) {
        jdouble y = sy1 + (SRCLOC(idy1, ddy1, scaley) / (1 << shift));
        dstInfo.bounds.y1 = dstInfo.bounds.y2 = idy1;
        if (y >= srcInfo.bounds.y1 && y < srcInfo.bounds.y2) {
            dstInfo.bounds.y2++;
        }
    } else {
        dstInfo.bounds.y1 = ((srcInfo.bounds.y1 <= sy1)
                             ? idy1
                             : refine(idy1, ddy1, tilesize, scaley,
                                      (srcInfo.bounds.y1-sy1) << shift, syinc));
        dstInfo.bounds.y2 = refine(idy1, ddy1, tilesize, scaley,
                                   (srcInfo.bounds.y2-sy1) << shift, syinc);
    }

    SurfaceData_IntersectBounds(&dstInfo.bounds, &clipInfo.bounds);
    dstFlags = pPrim->dstflags;
    if (!Region_IsRectangular(&clipInfo)) {
        dstFlags |= SD_LOCK_PARTIAL_WRITE;
    }
    if (dstOps->Lock(env, dstOps, &dstInfo, dstFlags) != SD_SUCCESS) {
        SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
        return;
    }

    if (dstInfo.bounds.x2 > dstInfo.bounds.x1 &&
        dstInfo.bounds.y2 > dstInfo.bounds.y1)
    {
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        dstOps->GetRasInfo(env, dstOps, &dstInfo);
        if (srcInfo.rasBase && dstInfo.rasBase) {
            SurfaceDataBounds span;
            void *pSrc = PtrCoord(srcInfo.rasBase,
                                  sx1, srcInfo.pixelStride,
                                  sy1, srcInfo.scanStride);

            Region_IntersectBounds(&clipInfo, &dstInfo.bounds);
            Region_StartIteration(env, &clipInfo);
            if (tilesize >= (ddx2 - ddx1) &&
                tilesize >= (ddy2 - ddy1))
            {
                /* Do everything in one tile */
                jint sxloc = (jint) SRCLOC(idx1, ddx1, scalex);
                jint syloc = (jint) SRCLOC(idy1, ddy1, scaley);
                while (Region_NextIteration(&clipInfo, &span)) {
                    jint tsxloc = sxloc;
                    jint tsyloc = syloc;
                    void *pDst;

                    if (span.y1 > idy1) {
                        tsyloc += syinc * (span.y1 - idy1);
                    }
                    if (span.x1 > idx1) {
                        tsxloc += sxinc * (span.x1 - idx1);
                    }

                    pDst = PtrCoord(dstInfo.rasBase,
                                    span.x1, dstInfo.pixelStride,
                                    span.y1, dstInfo.scanStride);
                    (*pPrim->funcs.scaledblit)(pSrc, pDst,
                                               span.x2-span.x1, span.y2-span.y1,
                                               tsxloc, tsyloc,
                                               sxinc, syinc, shift,
                                               &srcInfo, &dstInfo,
                                               pPrim, &compInfo);
                }
            } else {
                /* Break each clip span into tiles for better accuracy. */
                while (Region_NextIteration(&clipInfo, &span)) {
                    jint tilex, tiley;
                    jint sxloc, syloc;
                    jint x1, y1, x2, y2;
                    void *pDst;

                    for (tiley = TILESTART(span.y1, idy1, tilesize);
                         tiley < span.y2;
                         tiley += tilesize)
                    {
                        /* Clip span to Y range of current tile */
                        y1 = tiley;
                        y2 = tiley + tilesize;
                        if (y1 < span.y1) y1 = span.y1;
                        if (y2 > span.y2) y2 = span.y2;

                        /* Find scaled source coordinate of first pixel */
                        syloc = (jint) SRCLOC(tiley, ddy1, scaley);
                        if (y1 > tiley) {
                            syloc += syinc * (y1 - tiley);
                        }

                        for (tilex = TILESTART(span.x1, idx1, tilesize);
                             tilex < span.x2;
                             tilex += tilesize)
                        {
                            /* Clip span to X range of current tile */
                            x1 = tilex;
                            x2 = tilex + tilesize;
                            if (x1 < span.x1) x1 = span.x1;
                            if (x2 > span.x2) x2 = span.x2;

                            /* Find scaled source coordinate of first pixel */
                            sxloc = (jint) SRCLOC(tilex, ddx1, scalex);
                            if (x1 > tilex) {
                                sxloc += sxinc * (x1 - tilex);
                            }

                            pDst = PtrCoord(dstInfo.rasBase,
                                            x1, dstInfo.pixelStride,
                                            y1, dstInfo.scanStride);
                            (*pPrim->funcs.scaledblit)(pSrc, pDst, x2-x1, y2-y1,
                                                       sxloc, syloc,
                                                       sxinc, syinc, shift,
                                                       &srcInfo, &dstInfo,
                                                       pPrim, &compInfo);
                        }
                    }
                }
            }
            Region_EndIteration(env, &clipInfo);
        }
        SurfaceData_InvokeRelease(env, dstOps, &dstInfo);
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
}
