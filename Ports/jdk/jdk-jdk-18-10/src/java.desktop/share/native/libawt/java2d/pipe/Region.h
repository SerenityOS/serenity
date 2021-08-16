/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _Included_Region
#define _Included_Region

#ifdef __cplusplus
extern "C" {
#endif

#include <SurfaceData.h>
#include "utility/rect.h"


/*
 * This file provides a number of structures, macros, and C functions
 * for native code to use to iterate through the list of rectangles
 * included in a Java Region object.  The intended usage pattern should
 * comply with the following code sample:
 *
 *      RegionData rgnInfo;
 *      Region_GetInfo(env, javaregion, &rgnInfo);
 *      // Calculate the area of interest for the graphics operation.
 *      Region_IntersectBounds(&rgnInfo, lox, loy, hix, hiy);
 *      if (!Region_IsEmpty(&rgnInfo)) {
 *              If (Region_IsRectangular(&rgnInfo)) {
 *                      // Optional code optimized for a single rectangle
 *              } else {
 *                      SurfaceDataBounds span;
 *                      Region_StartIteration(env, &rgnInfo);
 *                      // this next line is optional if the info is needed
 *                      int numrects = Region_CountIterationRects(&rgnInfo);
 *                      while (Region_NextIteration(&rgnInfo, &span)) {
 *                              // Process span.x1, span.y1, span.x2, span.y2
 *                      }
 *                      Region_EndIteration(env, &rgnInfo);
 *              }
 *      }
 */

/*
 * This structure is not meant to be accessed by code outside of
 * Region.h or Region.c.  It is exposed here so that callers can
 * stack-allocate one of these structures for performance.
 */
typedef struct {
    SurfaceDataBounds   bounds;
    jint                endIndex;
    jobject             bands;
    jint                index;
    jint                numrects;
    jint                *pBands;
} RegionData;

/*
 * Initialize a native RegionData structure from a Java object
 * of type sun.java2d.pipe.Region.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 */
JNIEXPORT jint JNICALL
Region_GetInfo(JNIEnv *env, jobject region, RegionData *pRgnInfo);

/*
 * This function retrieves the bounds from a Java Region object and
 * returns them in the specified SurfaceDataBounds structure.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 */
JNIEXPORT void JNICALL
Region_GetBounds(JNIEnv *env, jobject region, SurfaceDataBounds *b);

/*
 * Intersect the specified SurfaceDataBounds with the bounds of
 * the indicated RegionData structure.  The Region iteration will
 * subsequently honor those bounds.
 */
#define Region_IntersectBounds(pRgnInfo, pDstBounds) \
    SurfaceData_IntersectBounds(&(pRgnInfo)->bounds, pDstBounds)

/*
 * Intersect the specified bounding coordinates with the bounds of
 * the indicated RegionData structure.  The Region iteration will
 * subsequently honor those bounds.
 */
#define Region_IntersectBoundsXYXY(pRgnInfo, x1, y1, x2, y2) \
    SurfaceData_IntersectBoundsXYXY(&(pRgnInfo)->bounds, x1, y1, x2, y2)

/*
 * Test whether the bounds of the specified RegionData structure
 * are now trivially empty.
 *
 * Note that this test only checks the overall bounds of the Region
 * and does not check to see if there are any individual subrectangles
 * which make up the region that intersect the current bounds.
 * Typically a Java Region object will have tight bounds that reflects
 * a non-empty set of subrectangles in the list, but after a given
 * graphics operation has intersected the RegionData with the area
 * of interest for that operation using one of the above calls to
 * IntersectBounds, the new bounds may fail to intersect any of
 * the subrectangles.
 */
#define Region_IsEmpty(pRgnInfo) \
    ((pRgnInfo)->bounds.x1 >= (pRgnInfo)->bounds.x2 || \
     (pRgnInfo)->bounds.y1 >= (pRgnInfo)->bounds.y2)

/*
 * Test whether the RegionData structure represents a single rectangle.
 *
 * Note that this test only checks to see if the original Java Region
 * object is a simple rectangle and does not take into account the
 * subsetting of the list of rectangles that might occur if a given
 * graphics operation intersects the bounds with an area of interest.
 */
#define Region_IsRectangular(pRgnInfo) \
    ((pRgnInfo)->endIndex == 0)

/*
 * Initialize a given RegionData structure for iteration of the
 * list of subrectangles.  This operation can be performed on
 * empty regions, simple rectangular regions and complex regions
 * without loss of generality.
 *
 * Note to callers:
 *      This function may use JNI Critical methods so it is important
 *      that the caller not call any other JNI methods after this function
 *      returns until the RegionEndIteration function is called.
 */
JNIEXPORT void JNICALL
Region_StartIteration(JNIEnv *env, RegionData *pRgnInfo);

/*
 * Count the number of subrectangles in the indicated RegionData.
 * The subrectangles will be compared against the bounds of the
 * Region so only those subrectangles that intersect the area of
 * interest will be included in the returned count.
 *
 * Note to callers:
 *      This function may only be called after Region_StartIteration
 *      and before Region_EndIteration are called on a given RegionData
 *      structure.
 */
JNIEXPORT jint JNICALL
Region_CountIterationRects(RegionData *pRgnInfo);

/*
 * Process the list of subrectangles in the RegionData structure and
 * assign the bounds of that subrectangle to the pSpan structure and
 * return a non-zero return value if one exists.  If there are no
 * more subrectangles in the given area of interest specified by
 * the bounds of the RegionData structure, then return 0.
 *
 * Note to callers:
 *      This function may only be called after Region_StartIteration
 *      and before Region_EndIteration are called on a given RegionData
 *      structure.
 */
JNIEXPORT jint JNICALL
Region_NextIteration(RegionData *pRgnInfo, SurfaceDataBounds *pSpan);

/*
 * Uninitialize a RegionData structure and discard any information
 * that was needed to iterate the list of subrectangles.
 *
 * Note to callers:
 *      This function will release any outstanding JNI Critical locks so
 *      it will once again be safe to use arbitrary JNI calls or return
 *      to the enclosing JNI native context.
 */
JNIEXPORT void JNICALL
Region_EndIteration(JNIEnv *env, RegionData *pRgnInfo);


/*
 * Converts a sun.java2d.pipe.Region object to a list of
 * rectangles using platform specific native data representation
 * (see the src/$PLATFORM/native/sun/awt/utility/rect.h header
 * files.)
 */
JNIEXPORT int JNICALL
RegionToYXBandedRectangles(JNIEnv *env,
        jint x1, jint y1, jint x2, jint y2, jobject region,
        RECT_T ** pRect, unsigned int initialBufferSize);


#ifdef __cplusplus
};
#endif

#endif
