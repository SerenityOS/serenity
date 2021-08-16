/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include "SurfaceData.h"

#include "awt_p.h"
#include "awt_GraphicsEnv.h"

#ifdef HEADLESS
#include "GLXGraphicsConfig.h"
#endif

#include <X11/extensions/Xrender.h>

/**
 * This include file contains support declarations for loops using the
 * X11 extended SurfaceData interface to talk to an X11 drawable from
 * native code.
 */

#ifdef HEADLESS
#define X11SDOps void
#else /* HEADLESS */
typedef struct _X11SDOps X11SDOps;

/*
 * This function returns an X11 Drawable which transparent pixels
 * (if there are any) were set to the specified color.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The xsdo parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The pixel parameter should be a color to which the transparent
 * pixels of the image should be se set to.
 */
typedef Drawable GetPixmapBgFunc(JNIEnv *env,
                                 X11SDOps *xsdo,
                                 jint pixel);

/*
 * This function releases the lock set by GetPixmapBg
 * function of the indicated X11SDOps structure.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 */
typedef void ReleasePixmapBgFunc(JNIEnv *env,
                                 X11SDOps *xsdo);


#ifdef MITSHM
typedef struct {
    XShmSegmentInfo     *shmSegInfo;    /* Shared Memory Segment Info */
    jint                bytesPerLine;   /* needed for ShMem lock */
    jboolean            xRequestSent;   /* true if x request is sent w/o XSync */
    jlong               pmSize;

    jboolean            usingShmPixmap;
    Drawable            pixmap;
    Drawable            shmPixmap;
    jint                numBltsSinceRead;
    jint                pixelsReadSinceBlt;
    jint                pixelsReadThreshold;
    jint                numBltsThreshold;
} ShmPixmapData;
#endif /* MITSHM */

struct _X11SDOps {
    SurfaceDataOps      sdOps;
    GetPixmapBgFunc     *GetPixmapWithBg;
    ReleasePixmapBgFunc *ReleasePixmapWithBg;
    jboolean            invalid;
    jboolean            isPixmap;
    jobject             peer;
    Drawable            drawable;
    GC                  javaGC;        /* used for Java-level GC validation */
    GC                  cachedGC;      /* cached for use in X11SD_Unlock() */
    jint                depth;
    jint                pixelmask;
    AwtGraphicsConfigData *configData;
    ColorData           *cData;
    Pixmap              bitmask;
    jint                bgPixel;       /* bg pixel for the pixmap */
    jboolean            isBgInitialized; /* whether the bg pixel is valid */
    jint                pmWidth;       /* width, height of the */
    jint                pmHeight;      /* pixmap */
    Picture             xrPic;
#ifdef MITSHM
    ShmPixmapData       shmPMData;     /* data for switching between shm/nonshm pixmaps*/
#endif /* MITSHM */
};

#define X11SD_LOCK_UNLOCKED     0       /* surface is not locked */
#define X11SD_LOCK_BY_NULL      1       /* surface locked for NOP */
#define X11SD_LOCK_BY_XIMAGE    2       /* surface locked by Get/PutImage */
#define X11SD_LOCK_BY_SHMEM     4       /* surface locked by ShMemExt */

#ifdef MITSHM
XImage * X11SD_GetSharedImage       (X11SDOps *xsdo,
                                     jint width, jint height,
                                     jint maxWidth, jint maxHeight,
                                     jboolean readBits);
XImage * X11SD_CreateSharedImage    (X11SDOps *xsdo, jint width, jint height);
Drawable X11SD_CreateSharedPixmap   (X11SDOps *xsdo);
void     X11SD_DropSharedSegment    (XShmSegmentInfo *shminfo);
void     X11SD_PuntPixmap           (X11SDOps *xsdo, jint width, jint height);
void     X11SD_UnPuntPixmap         (X11SDOps *xsdo);
jboolean X11SD_CachedXImageFits     (jint width, jint height,
                                     jint maxWidth, jint maxHeight,
                                     jint depth, jboolean readBits);
XImage * X11SD_GetCachedXImage      (jint width, jint height, jboolean readBits);
#endif /* MITSHM */
jint     X11SD_InitWindow(JNIEnv *env, X11SDOps *xsdo);
void     X11SD_DisposeOrCacheXImage (XImage * image);
void     X11SD_DisposeXImage(XImage * image);
void     X11SD_DirectRenderNotify(JNIEnv *env, X11SDOps *xsdo);
#endif /* !HEADLESS */

jboolean XShared_initIDs(JNIEnv *env, jboolean allowShmPixmaps);
jboolean XShared_initSurface(JNIEnv *env, X11SDOps *xsdo, jint depth, jint width, jint height, jlong drawable);

/*
 * This function returns a pointer to a native X11SDOps structure
 * for accessing the indicated X11 SurfaceData Java object.  It
 * verifies that the indicated SurfaceData object is an instance
 * of X11SurfaceData before returning and will return NULL if the
 * wrong SurfaceData object is being accessed.  This function will
 * throw the appropriate Java exception if it returns NULL so that
 * the caller can simply return.
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
JNIEXPORT X11SDOps * JNICALL
X11SurfaceData_GetOps(JNIEnv *env, jobject sData);
