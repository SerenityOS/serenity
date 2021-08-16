/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This include file contains information on how to use a SurfaceData
 * object from native code.
 */

#ifndef _Included_SurfaceData
#define _Included_SurfaceData

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * This structure is used to represent a rectangular bounding box
 * throughout various functions in the native SurfaceData API.
 *
 * All coordinates (x1 <= x < x2, y1 <= y < y2) are considered to
 * be inside these bounds.
 */
typedef struct {
    jint x1;
    jint y1;
    jint x2;
    jint y2;
} SurfaceDataBounds;

#define SD_RASINFO_PRIVATE_SIZE         64

/*
 * The SurfaceDataRasInfo structure is used to pass in and return various
 * pieces of information about the destination drawable.  In particular:
 *
 *      SurfaceDataBounds bounds;
 * [Needed for SD_LOCK_READ or SD_LOCK_WRITE]
 * The 2 dimensional bounds of the raster array that is needed.  Valid
 * memory locations are required at:
 *      *(pixeltype *) (((char *)rasBase) + y * scanStride + x * pixelStride)
 * for each x, y pair such that (bounds.x1 <= x < bounds.x2) and
 * (bounds.y1 <= y < bounds.y2).
 *
 *      void *rasBase;
 * [Requires SD_LOCK_READ or SD_LOCK_WRITE]
 * A pointer to the device space origin (0, 0) of the indicated raster
 * data.  This pointer may point to a location that is outside of the
 * allocated memory for the requested bounds and it may even point
 * outside of accessible memory.  Only the locations that fall within
 * the coordinates indicated by the requested bounds are guaranteed
 * to be accessible.
 *
 *      jint pixelBitOffset;
 * [Requires SD_LOCK_READ or SD_LOCK_WRITE]
 * The number of bits offset from the beginning of the first byte
 * of a scanline to the first bit of the first pixel on that scanline.
 * The bit offset must be less than 8 and it must be the same for each
 * scanline.  This field is only needed by image types which pack
 * multiple pixels into a byte, such as ByteBinary1Bit et al.  For
 * image types which use whole bytes (or shorts or ints) to store
 * their pixels, this field will always be 0.
 *
 *      jint pixelStride;
 * [Requires SD_LOCK_READ or SD_LOCK_WRITE]
 * The pixel stride is the distance in bytes from the data for one pixel
 * to the data for the pixel at the next x coordinate (x, y) => (x+1, y).
 * For data types that pack multiple pixels into a byte, such as
 * ByteBinary1Bit et al, this field will be 0 and the loops which
 * render to and from such data need to calculate their own offset
 * from the beginning of the scanline using the absolute x coordinate
 * combined with the pixelBitOffset field.
 * Bugfix 6220829 - this field used to be unsigned int, but some
 * primitives used negative pixel offsets and the corresponding
 * unsigned stride values caused the resulting pixel offset to
 * to always be a positive 32-bit quantity - causing problems on
 * 64-bit architectures.
 *
 *      jint scanStride;
 * [Requires SD_LOCK_READ or SD_LOCK_WRITE]
 * The scan stride is the distance in bytes from the data for one pixel
 * to the data for the pixel at the next y coordinate (x, y) => (x, y+1).
 * Bugfix 6220829 - this field used to be unsigned int, but some
 * primitives used negative pixel offsets and the corresponding
 * unsigned stride values caused the resulting pixel offset to
 * to always be a positive 32-bit quantity - causing problems on
 * 64-bit architectures.
 *
 *      unsigned int lutSize;
 * [Requires SD_LOCK_LUT]
 * The number of entries in the color lookup table.  The data beyond the
 * end of the map will be undefined.
 *
 *      jint *lutBase;
 * [Requires SD_LOCK_LUT]
 * A pointer to the beginning of the color lookup table for the colormap.
 * The color lookup table is formatted as an array of jint values each
 * representing the 32-bit ARGB color for the pixel representing by the
 * corresponding index.  The table is guaranteed to contain at least 256
 * valid memory locations even if the size of the map is smaller than 256.
 *
 *      unsigned char *invColorTable;
 * [Requires SD_LOCK_INVCOLOR]
 * A pointer to the beginning of the inverse color lookup table for the
 * colormap.  The inverse color lookup table is formatted as a 32x32x32
 * array of bytes indexed by RxGxB where each component is reduced to 5
 * bits of precision before indexing.
 *
 *      char *redErrTable;
 *      char *grnErrTable;
 *      char *bluErrTable;
 * [Requires SD_LOCK_INVCOLOR]
 * Pointers to the beginning of the ordered dither color error tables
 * for the colormap.  The error tables are formatted as an 8x8 array
 * of bytes indexed by coordinates using the formula [y & 7][x & 7].
 *
 *      int *invGrayTable;
 * [Requires SD_LOCK_INVGRAY]
 * A pointer to the beginning of the inverse gray lookup table for the
 * colormap.  The inverse color lookup table is formatted as an array
 * of 256 integers indexed by a byte gray level and storing an index
 * into the colormap of the closest matching gray pixel.
 *
 *      union priv {};
 * A buffer of private data for the SurfaceData implementation.
 * This field is a union of a data block of the desired default
 * size (SD_RASINFO_PRIVATE_SIZE) and a (void *) pointer that
 * ensures proper "strictest" alignment on all platforms.
 */
typedef struct {
    SurfaceDataBounds   bounds;                 /* bounds of raster array */
    void                *rasBase;               /* Pointer to (0, 0) pixel */
    jint                pixelBitOffset;         /* bit offset to (0, *) pixel */
    jint                pixelStride;            /* bytes to next X pixel */
    jint                scanStride;             /* bytes to next Y pixel */
    unsigned int        lutSize;                /* # colors in colormap */
    jint                *lutBase;               /* Pointer to colormap[0] */
    unsigned char       *invColorTable;         /* Inverse color table */
    char                *redErrTable;           /* Red ordered dither table */
    char                *grnErrTable;           /* Green ordered dither table */
    char                *bluErrTable;           /* Blue ordered dither table */
    int                 *invGrayTable;          /* Inverse gray table */
    int                 representsPrimaries;    /* whether cmap represents primary colors */
    union {
        void            *align;                 /* ensures strict alignment */
        char            data[SD_RASINFO_PRIVATE_SIZE];
    } priv;
} SurfaceDataRasInfo;

typedef struct _SurfaceDataOps SurfaceDataOps;

/*
 * This function is used to lock a particular region of a particular
 * destination.  Once this method is called, no changes of any of the
 * data returned by any of the other SurfaceData vectored functions
 * may change until a corresponding call to Release is made.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The rasInfo parameter should be a pointer to a SurfaceDataRasInfo
 * structure in which the bounds have been initialized to the maximum
 * bounds of the raster data that will need to be accessed later.
 *
 * The lockflags parameter should indicate which information will be
 * needed by the caller.  The various flags which may be OR'd together
 * may consist of any of the following:
 *      SD_LOCK_READ            The caller needs to read pixels from the dest
 *      SD_LOCK_WRITE           The caller needs to write pixels to the dest
 *      SD_LOCK_RD_WR           A combination of (SD_LOCK_READ | SD_LOCK_WRITE)
 *      SD_LOCK_LUT             The caller needs the colormap (Lut)
 *      SD_LOCK_INVCOLOR        The caller needs the inverse color table
 *      SD_LOCK_INVGRAY         The caller needs the inverse gray table
 *      SD_LOCK_FASTEST         The caller only wants direct pixel access
 * Note that the SD_LOCK_LUT, SD_LOCK_INVCOLOR, and SD_LOCK_INVGRAY flags
 * are only valid for destinations with IndexColorModels.
 * Also note that SD_LOCK_FASTEST will only succeed if the access to the
 * pixels will occur just as fast regardless of the size of the bounds.
 * This flag is used by the Text rendering routines to determine if it
 * matters whether or not they have calculated a tight bounding box for
 * the pixels they will be touching.
 *
 * Return value:
 *
 * If this function succeeds, it will return SD_SUCCESS (0).
 *
 * If this function is unable to honor the SD_LOCK_FASTEST flag,
 * it will return SD_SLOWLOCK.  The bounds parameter of the
 * SurfaceDataRasInfo object should be intersected with a tighter
 * bounding rectangle before calling the GetRasInfo function so
 * as to minimize the amount pixel copying or conversion.  Note
 * that the Lock function may have already intersected the
 * bounds with a tighter rectangle as it tried to honor the
 * SD_SLOWLOCK flag and so the caller should only use intersection
 * operations to further restrict the bounds.
 *
 * If this function fails for any reason that is not recoverable,
 * it will throw an appropriate Java exception and return SD_FAILED.
 *
 * Operation:
 *
 * This function will intersect the bounds specified in the rasInfo
 * parameter with the available raster data in the destination drawable
 * and modify the contents of the bounds field to represent the maximum
 * available raster data.
 *
 * If the available raster data in the destination drawable consists of
 * a non-rectangular region of pixels, this method may throw an InvalidPipe
 * exception (optionally the object may decide to provide a copy of the
 * destination pixel data with undefined data in the inaccessible portions).
 *
 * Further processing by the caller may discover that a smaller region of
 * data is actually needed and the call to GetRasData can be made with a
 * still smaller bounds.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 * Note to implementers:
 *      The caller may also continue to use JNI methods after this method
 *      is called so it is important that implementations of SurfaceData
 *      not return from this function with any outstanding JNI Critical
 *      locks that have not been released.
 */
typedef jint LockFunc(JNIEnv *env,
                      SurfaceDataOps *ops,
                      SurfaceDataRasInfo *rasInfo,
                      jint lockflags);

/*
 * This function returns information about the raster data for the drawable.
 * The function will fill in or modify the contents of the SurfaceDataRasInfo
 * structure that is passed in with various pieces of information depending
 * on what was requested in the lockflags parameter that was handed into
 * the LockFunc.  For more information on which pieces of information are
 * returned based upon the lock flags see the documentation for the
 * RasInfo structure above.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The pRasInfo parameter should be a pointer to the same structure of type
 * SurfaceDataRasInfo.  The bounds member of that structure should be
 * initialized to the bounding box of the raster data that is actually
 * needed for reading or writing before calling this function.  These
 * bounds must be a subset of the raster bounds that were given to the
 * LockFunc or the results will be undefined.
 *
 * If the surface was locked with the flag SD_LOCK_FASTEST then this
 * function may reevaluate the bounds in the RasInfo structure and
 * return a subset of what was requested.  Callers that use that flag
 * should be prepared to reevaluate their clipping after GetRasInfo
 * returns.  If the SD_LOCK_FASTEST flag was not specified, then this
 * function will return a buffer containing all of the pixels in the
 * requested bounds without reevaluating them.
 *
 * Any information that was requested in the lockflags of the LockFunc
 * will be returned and NULL pointers will be returned for all other
 * information.
 *
 * Note to callers:
 *      This function may use JNI Critical methods so it is important
 *      that the caller not call any other JNI methods after this function
 *      returns until the Release function is called.
 */
typedef void GetRasInfoFunc(JNIEnv *env,
                            SurfaceDataOps *ops,
                            SurfaceDataRasInfo *pRasInfo);

/*
 * This function releases all of the Critical data for the specified
 * drawable.
 *
 * This function vector is allowed to be NULL if a given SurfaceData
 * implementation does not require the use of JNI Critical array locks.
 * Callers should use the "SurfaceData_InvokeRelease(env, ops)" macro
 * to handle the conditional invocation of this function.
 *
 * In particular, this function will release any outstanding JNI Critical
 * locks that the SurfaceData implementation may have used so that it
 * will be safe for the caller to start using arbitrary JNI calls or
 * return from its calling JNI function.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The pRasInfo parameter should be a pointer to the same structure of
 * type SurfaceDataRasInfo that was passed to the GetRasInfo function.
 * The bounds should be unchanged since that call.
 *
 * Note to callers:
 *      This function will release any outstanding JNI Critical locks so
 *      it will once again be safe to use arbitrary JNI calls or return
 *      to the enclosing JNI native context.
 *
 * Note to implementers:
 *      This function may not use any JNI methods other than to release
 *      outstanding JNI Critical array locks since there may be other
 *      nested SurfacData objects holding locks with their own outstanding
 *      JNI Critical locks.  This restriction includes the use of the
 *      JNI monitor calls so that all MonitorExit invocations must be
 *      done in the Unlock function.
 */
typedef void ReleaseFunc(JNIEnv *env,
                         SurfaceDataOps *ops,
                         SurfaceDataRasInfo *pRasInfo);

/*
 * This function unlocks the specified drawable.
 *
 * This function vector is allowed to be NULL if a given SurfaceData
 * implementation does not require any unlocking of the destination.
 * Callers should use the "SurfaceData_InvokeUnlock(env, ops)" macro
 * to handle the conditional invocation of this function.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The pRasInfo parameter should be a pointer to the same structure of
 * type SurfaceDataRasInfo that was passed to the GetRasInfo function.
 * The bounds should be unchanged since that call.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 * Note to implementers:
 *      This function may be used to release any JNI monitors used to
 *      prevent the destination from being modified.  It may also be
 *      used to perform operations which may require blocking (such as
 *      executing X11 operations which may need to flush data).
 */
typedef void UnlockFunc(JNIEnv *env,
                        SurfaceDataOps *ops,
                        SurfaceDataRasInfo *pRasInfo);

/*
 * This function sets up the specified drawable.  Some surfaces may
 * need to perform certain operations during Setup that cannot be
 * done after later operations such as Lock.  For example, on
 * win9x systems, when any surface is locked we cannot make a call to
 * the message-handling thread.
 *
 * This function vector is allowed to be NULL if a given SurfaceData
 * implementation does not require any setup.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 */
typedef void SetupFunc(JNIEnv *env,
                       SurfaceDataOps *ops);

/*
 * This function disposes the specified SurfaceDataOps structure
 * and associated native resources.
 * The implementation is SurfaceData-type specific.
 */
typedef void DisposeFunc(JNIEnv *env,
                         SurfaceDataOps *ops);

/*
 * Constants used for return values.  Constants less than 0 are
 * unrecoverable failures and indicate that a Java exception has
 * already been thrown.  Constants greater than 0 are conditional
 * successes which warn the caller that various optional features
 * were not available so that workarounds can be used.
 */
#define SD_FAILURE              -1
#define SD_SUCCESS              0
#define SD_SLOWLOCK             1

/*
 * Constants for the flags used in the Lock function.
 */
#define SD_LOCK_READ            (1 << 0)
#define SD_LOCK_WRITE           (1 << 1)
#define SD_LOCK_RD_WR           (SD_LOCK_READ | SD_LOCK_WRITE)
#define SD_LOCK_LUT             (1 << 2)
#define SD_LOCK_INVCOLOR        (1 << 3)
#define SD_LOCK_INVGRAY         (1 << 4)
#define SD_LOCK_FASTEST         (1 << 5)
#define SD_LOCK_PARTIAL         (1 << 6)
#define SD_LOCK_PARTIAL_WRITE   (SD_LOCK_WRITE | SD_LOCK_PARTIAL)
#define SD_LOCK_NEED_PIXELS     (SD_LOCK_READ | SD_LOCK_PARTIAL)

/*
 * This structure provides the function vectors for manipulating
 * and retrieving information about the destination drawable.
 * There are also variables for the surface data object used by
 * native code to track the state of the surface.
 * The sdObject is a pointer to the Java SurfaceData object;
 * this is set in SurfaceData_InitOps() and used by any object
 * using the ops structure to refer to elements in the Java object
 * (such as fields that we need to set from native code).
 */
struct _SurfaceDataOps {
    LockFunc            *Lock;
    GetRasInfoFunc      *GetRasInfo;
    ReleaseFunc         *Release;
    UnlockFunc          *Unlock;
    SetupFunc           *Setup;
    DisposeFunc         *Dispose;
    jobject             sdObject;
};

#define _ClrReduce(c)   (((unsigned char) c) >> 3)

/*
 * This macro performs a lookup in an inverse color table given 3 8-bit
 * RGB primaries.  It automates the process of reducing the primaries
 * to 5-bits of precision and using them to index into the specified
 * inverse color lookup table.
 */
#define SurfaceData_InvColorMap(invcolortbl, r, g, b) \
    (invcolortbl)[(_ClrReduce(r)<<10) + (_ClrReduce(g)<<5) + _ClrReduce(b)]

/*
 * This macro invokes the SurfaceData Release function only if the
 * function vector is not NULL.
 */
#define SurfaceData_InvokeRelease(env, ops, pRI)        \
    do {                                                \
        if ((ops)->Release != NULL) {                   \
            (ops)->Release(env, ops, pRI);              \
        }                                               \
    } while(0)

/*
 * This macro invokes the SurfaceData Unlock function only if the
 * function vector is not NULL.
 */
#define SurfaceData_InvokeUnlock(env, ops, pRI)         \
    do {                                                \
        if ((ops)->Unlock != NULL) {                    \
            (ops)->Unlock(env, ops, pRI);               \
        }                                               \
    } while(0)

/*
 * This macro invokes both the SurfaceData Release and Unlock functions
 * only if the function vectors are not NULL.  It can be used in cases
 * where only one surface has been accessed and where no other JNI
 * Critical locks (which would need to be released after Release and
 * before Unlock) are held by the calling function.
 */
#define SurfaceData_InvokeReleaseUnlock(env, ops, pRI)  \
    do {                                                \
        if ((ops)->Release != NULL) {                   \
            (ops)->Release(env, ops, pRI);              \
        }                                               \
        if ((ops)->Unlock != NULL) {                    \
            (ops)->Unlock(env, ops, pRI);               \
        }                                               \
    } while(0)

/*
 * This macro invokes both the SurfaceData Release and Unlock functions
 * on two nested drawables only if the function vectors are not NULL.
 * It can be used in cases where two surfaces have been accessed and
 * where no other JNI Critical locks (which would need to be released
 * after Release and before Unlock) are held by the calling function.  The
 * two ops vectors should be specified in the same order that they were
 * locked.  Both surfaces will be released and then both unlocked.
 */
#define SurfaceData_InvokeReleaseUnlock2(env, ops1, pRI1, ops2, pRI2)   \
    do {                                                        \
        if ((ops2)->Release != NULL) {                          \
            (ops2)->Release(env, ops2, pRI2);                   \
        }                                                       \
        if ((ops1)->Release != NULL) {                          \
            (ops1)->Release(env, ops1, pRI1);                   \
        }                                                       \
        if ((ops2)->Unlock != NULL) {                           \
            (ops2)->Unlock(env, ops2, pRI2);                    \
        }                                                       \
        if ((ops1)->Unlock != NULL) {                           \
            (ops1)->Unlock(env, ops1, pRI1);                    \
        }                                                       \
    } while(0)

#define SurfaceData_InvokeDispose(env, ops)                     \
    do {                                                        \
        if ((ops)->Dispose != NULL) {                           \
            (ops)->Dispose(env, ops);                           \
        }                                                       \
    } while(0)

#define SurfaceData_InvokeSetup(env, ops)                       \
    do {                                                        \
        if ((ops)->Setup != NULL) {                             \
            (ops)->Setup(env, ops);                             \
        }                                                       \
    } while(0)

/*
 * This function returns a pointer to a native SurfaceDataOps
 * structure for accessing the indicated SurfaceData Java object.
 *
 * Note to callers:
 *      This function uses JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 *      The caller may continue to use JNI methods after this method
 *      is called since this function will not leave any outstanding
 *      JNI Critical locks unreleased.
 */
JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_GetOps(JNIEnv *env, jobject sData);

/*
 * Does the same as the above, but doesn't call Setup function
 * even if it's set.
 */
JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_GetOpsNoSetup(JNIEnv *env, jobject sData);

/*
 * This function stores a pointer to a native SurfaceDataOps
 * structure into the indicated Java SurfaceData object.
 *
 * Note to callers:
 *      This function uses JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 *      The caller may continue to use JNI methods after this method
 *      is called since this function will not leave any outstanding
 *      JNI Critical locks unreleased.
 */
JNIEXPORT void JNICALL
SurfaceData_SetOps(JNIEnv *env, jobject sData, SurfaceDataOps *ops);

/*
 * This function throws an InvalidPipeException which will cause the
 * calling SunGraphics2D object to revalidate its pipelines and call
 * again.  This utility method should be called from the SurfaceData
 * native Lock routine when some attribute of the surface has changed
 * that requires pipeline revalidation, including:
 *
 *      The bit depth or pixel format of the surface.
 *      The surface (window) has been disposed.
 *      The device clip of the surface has been changed (resize, visibility, etc.)
 *
 * Note to callers:
 *      This function uses JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 *      The caller may continue to use JNI methods after this method
 *      is called since this function will not leave any outstanding
 *      JNI Critical locks unreleased.
 */
JNIEXPORT void JNICALL
SurfaceData_ThrowInvalidPipeException(JNIEnv *env, const char *msg);

/*
 * This function intersects two bounds objects which exist in the same
 * coordinate space.  The contents of the first parameter (dst) are
 * modified to contain the intersection of the two bounds while the
 * contents of the second parameter (src) are untouched.
 */
JNIEXPORT void JNICALL
SurfaceData_IntersectBounds(SurfaceDataBounds *dst, SurfaceDataBounds *src);

/*
 * This function intersects a bounds object with a rectangle specified
 * in lox, loy, hix, hiy format in the same coordinate space.  The
 * contents of the first parameter (bounds) are modified to contain
 * the intersection of the two rectangular regions.
 */
JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYXY(SurfaceDataBounds *bounds,
                                jint lox, jint loy, jint hix, jint hiy);

/*
 * This function intersects a bounds object with a rectangle specified
 * in XYWH format in the same coordinate space.  The contents of the
 * first parameter (bounds) are modified to contain the intersection
 * of the two rectangular regions.
 */
JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYWH(SurfaceDataBounds *bounds,
                                jint x, jint y, jint w, jint h);

/*
 * This function intersects two bounds objects which exist in different
 * coordinate spaces.  The coordinate spaces of the two objects are
 * related such that a given coordinate in the space of the A bounds
 * is related to the analogous coordinate in the space of the B bounds
 * by the formula: (AX + BXminusAX, AY + BYminusAY) == (BX, BY).
 * The contents of both bounds objects are modified to represent their
 * mutual intersection.
 */
JNIEXPORT void JNICALL
SurfaceData_IntersectBlitBounds(SurfaceDataBounds *Abounds,
                                SurfaceDataBounds *Bbounds,
                                jint BXminusAX, jint BYminusAY);


/*
 * This function creates and initializes the ops structure.  The function
 * is called by "subclasses" of SurfaceData (e.g., BufImgSurfaceData)
 * which pass in the size of the structure to allocate (subclasses generally
 * need additional fields in the ops structure particular to their usage
 * of the structure).  The structure is allocated and initialized
 * and is stored in the SurfaceData java object for later retrieval.
 * Subclasses of SurfaceData should call this function instead of allocating
 * the memory directly.
 */
JNIEXPORT SurfaceDataOps * JNICALL
SurfaceData_InitOps(JNIEnv *env, jobject sData, int opsSize);

/*
 * This function invokes the ops-specific disposal function.
 * It is a part of the finalizers-free disposal mechanism.
 * (see Disposer and DefaultDisposerRecord classes for more information)
 * It also destroys the ops structure created in SurfaceData_InitOps.
 */
void SurfaceData_DisposeOps(JNIEnv *env, jlong ops);

#ifdef __cplusplus
};
#endif

#endif
