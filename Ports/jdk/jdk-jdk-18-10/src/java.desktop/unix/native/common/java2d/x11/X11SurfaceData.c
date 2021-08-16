/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "X11SurfaceData.h"
#include "GraphicsPrimitiveMgr.h"
#include "Region.h"
#include "Trace.h"

/* Needed to define intptr_t */
#include "gdefs.h"

#include "jni_util.h"
#include "jvm_md.h"
#include "awt_Component.h"
#include "awt_GraphicsEnv.h"

#include <dlfcn.h>

#ifndef HEADLESS

/**
 * This file contains support code for loops using the SurfaceData
 * interface to talk to an X11 drawable from native code.
 */

typedef struct _X11RIPrivate {
    jint                lockType;
    jint                lockFlags;
    XImage              *img;
    int                 x, y;
} X11RIPrivate;

#define XSD_MAX(a,b) ((a) > (b) ? (a) : (b))
#define XSD_MIN(a,b) ((a) < (b) ? (a) : (b))

static LockFunc X11SD_Lock;
static GetRasInfoFunc X11SD_GetRasInfo;
static UnlockFunc X11SD_Unlock;
static DisposeFunc X11SD_Dispose;
static GetPixmapBgFunc X11SD_GetPixmapWithBg;
static ReleasePixmapBgFunc X11SD_ReleasePixmapWithBg;
extern int XShmAttachXErrHandler(Display *display, XErrorEvent *xerr);
extern AwtGraphicsConfigDataPtr
    getGraphicsConfigFromComponentPeer(JNIEnv *env, jobject this);
extern struct X11GraphicsConfigIDs x11GraphicsConfigIDs;

static int X11SD_FindClip(SurfaceDataBounds *b, SurfaceDataBounds *bounds,
                          X11SDOps *xsdo);
static int X11SD_ClipToRoot(SurfaceDataBounds *b, SurfaceDataBounds *bounds,
                            X11SDOps *xsdo);
static void X11SD_SwapBytes(X11SDOps *xsdo, XImage *img, int depth, int bpp);
static XImage * X11SD_GetImage(JNIEnv *env, X11SDOps *xsdo,
                               SurfaceDataBounds *bounds,
                               jint lockFlags);
static int X11SD_GetBitmapPad(int pixelStride);

extern jfieldID validID;

static int nativeByteOrder;
static jclass xorCompClass;

jint useMitShmExt = CANT_USE_MITSHM;
jint useMitShmPixmaps = CANT_USE_MITSHM;
jint forceSharedPixmaps = JNI_FALSE;

#ifdef MITSHM
int mitShmPermissionMask = MITSHM_PERM_OWNER;
#endif

/* Cached shared image, one for all surface datas. */
static XImage * cachedXImage;

#endif /* !HEADLESS */

jboolean XShared_initIDs(JNIEnv *env, jboolean allowShmPixmaps)
{
#ifndef HEADLESS
   union {
        char c[4];
        int i;
    } endian;

    endian.i = 0xff000000;
    nativeByteOrder = (endian.c[0]) ? MSBFirst : LSBFirst;

    cachedXImage = NULL;

    if (sizeof(X11RIPrivate) > SD_RASINFO_PRIVATE_SIZE) {
        JNU_ThrowInternalError(env, "Private RasInfo structure too large!");
        return JNI_FALSE;
    }

#ifdef MITSHM
    if (getenv("NO_AWT_MITSHM") == NULL &&
        getenv("NO_J2D_MITSHM") == NULL) {
        char * force;
        char * permission = getenv("J2D_MITSHM_PERMISSION");
        if (permission != NULL) {
            if (strcmp(permission, "common") == 0) {
                mitShmPermissionMask = MITSHM_PERM_COMMON;
            }
        }

        TryInitMITShm(env, &useMitShmExt, &useMitShmPixmaps);

        if(allowShmPixmaps) {
          useMitShmPixmaps = (useMitShmPixmaps == CAN_USE_MITSHM);
          force = getenv("J2D_PIXMAPS");
          if (force != NULL) {
              if (useMitShmPixmaps && (strcmp(force, "shared") == 0)) {
                  forceSharedPixmaps = JNI_TRUE;
              } else if (strcmp(force, "server") == 0) {
                  useMitShmPixmaps = JNI_FALSE;
              }
          }
        }else {
          useMitShmPixmaps = JNI_FALSE;
        }
    }
#endif /* MITSHM */

#endif /* !HEADLESS */

    return JNI_TRUE;
}


/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;Z)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11SurfaceData_initIDs(JNIEnv *env, jclass xsd,
                                           jclass XORComp)
{
#ifndef HEADLESS
  if(XShared_initIDs(env, JNI_TRUE))
  {
    xorCompClass = (*env)->NewGlobalRef(env, XORComp);
  }
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    isDrawableValid
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_x11_XSurfaceData_isDrawableValid(JNIEnv *env, jobject this)
{
    jboolean ret = JNI_FALSE;

#ifndef HEADLESS
    X11SDOps *xsdo = X11SurfaceData_GetOps(env, this);

    AWT_LOCK();
    if (xsdo->drawable != 0 || X11SD_InitWindow(env, xsdo) == SD_SUCCESS) {
        ret = JNI_TRUE;
    }
    AWT_UNLOCK();
#endif /* !HEADLESS */

    return ret;
}

/*
 * Class: sun_java2d_x11_X11SurfaceData
 * Method: isShmPMAvailable
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_x11_X11SurfaceData_isShmPMAvailable(JNIEnv *env, jobject this)
{
#if defined(HEADLESS) || !defined(MITSHM)
    return JNI_FALSE;
#else
    return (jboolean)useMitShmPixmaps;
#endif /* HEADLESS, MITSHM */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    initOps
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_initOps(JNIEnv *env, jobject xsd,
                                           jobject peer,
                                           jobject graphicsConfig, jint depth)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps*)SurfaceData_InitOps(env, xsd, sizeof(X11SDOps));
    jboolean hasException;
    if (xsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }
    xsdo->sdOps.Lock = X11SD_Lock;
    xsdo->sdOps.GetRasInfo = X11SD_GetRasInfo;
    xsdo->sdOps.Unlock = X11SD_Unlock;
    xsdo->sdOps.Dispose = X11SD_Dispose;
    xsdo->GetPixmapWithBg = X11SD_GetPixmapWithBg;
    xsdo->ReleasePixmapWithBg = X11SD_ReleasePixmapWithBg;
    if (peer != NULL) {
        xsdo->drawable = JNU_CallMethodByName(env, &hasException, peer, "getWindow", "()J").j;
        if (hasException) {
            return;
        }
    } else {
        xsdo->drawable = 0;
    }
    xsdo->depth = depth;
    xsdo->isPixmap = JNI_FALSE;
    xsdo->bitmask = 0;
    xsdo->bgPixel = 0;
    xsdo->isBgInitialized = JNI_FALSE;
#ifdef MITSHM
    xsdo->shmPMData.shmSegInfo = NULL;
    xsdo->shmPMData.xRequestSent = JNI_FALSE;
    xsdo->shmPMData.pmSize = 0;
    xsdo->shmPMData.usingShmPixmap = JNI_FALSE;
    xsdo->shmPMData.pixmap = 0;
    xsdo->shmPMData.shmPixmap = 0;
    xsdo->shmPMData.numBltsSinceRead = 0;
    xsdo->shmPMData.pixelsReadSinceBlt = 0;
    xsdo->shmPMData.numBltsThreshold = 2;
#endif /* MITSHM */

    xsdo->configData = (AwtGraphicsConfigDataPtr)
        JNU_GetLongFieldAsPtr(env,
                              graphicsConfig,
                              x11GraphicsConfigIDs.aData);
    if (xsdo->configData == NULL) {
        JNU_ThrowNullPointerException(env,
                                      "Native GraphicsConfig data block missing");
        return;
    }
    if (depth > 12) {
        xsdo->pixelmask = (xsdo->configData->awt_visInfo.red_mask |
                           xsdo->configData->awt_visInfo.green_mask |
                           xsdo->configData->awt_visInfo.blue_mask);
    } else if (depth == 12) {
        xsdo->pixelmask = 0xfff;
    } else {
        xsdo->pixelmask = 0xff;
    }

    xsdo->xrPic = None;
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    flushNativeSurface
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_flushNativeSurface(JNIEnv *env, jobject xsd)
{
#ifndef HEADLESS
    SurfaceDataOps *ops = SurfaceData_GetOps(env, xsd);

    if (ops != NULL) {
        X11SD_Dispose(env, ops);
    }
#endif /* !HEADLESS */
}


JNIEXPORT X11SDOps * JNICALL
X11SurfaceData_GetOps(JNIEnv *env, jobject sData)
{
#ifdef HEADLESS
    return NULL;
#else
    SurfaceDataOps *ops = SurfaceData_GetOps(env, sData);
    if (ops != NULL && ops->Lock != X11SD_Lock) {
        SurfaceData_ThrowInvalidPipeException(env, "not an X11 SurfaceData");
        ops = NULL;
    }
    return (X11SDOps *) ops;
#endif /* !HEADLESS */
}

/*
 * Method for disposing X11SD-specific data
 */
static void
X11SD_Dispose(JNIEnv *env, SurfaceDataOps *ops)
{
#ifndef HEADLESS
    /* ops is assumed non-null as it is checked in SurfaceData_DisposeOps */
    X11SDOps * xsdo = (X11SDOps*)ops;

    AWT_LOCK();

    xsdo->invalid = JNI_TRUE;

    if (xsdo->xrPic != None) {
        XRenderFreePicture(awt_display, xsdo->xrPic);
        xsdo->xrPic = None;
     }

    if (xsdo->isPixmap == JNI_TRUE && xsdo->drawable != 0) {
#ifdef MITSHM
        if (xsdo->shmPMData.shmSegInfo != NULL) {
            X11SD_DropSharedSegment(xsdo->shmPMData.shmSegInfo);
            xsdo->shmPMData.shmSegInfo = NULL;
        }
        if (xsdo->shmPMData.pixmap) {
            XFreePixmap(awt_display, xsdo->shmPMData.pixmap);
            xsdo->shmPMData.pixmap = 0;
        }
        if (xsdo->shmPMData.shmPixmap) {
            XFreePixmap(awt_display, xsdo->shmPMData.shmPixmap);
            xsdo->shmPMData.shmPixmap = 0;
        }
#else
        XFreePixmap(awt_display, xsdo->drawable);
#endif /* MITSHM */
        xsdo->drawable = 0;
    }
    if (xsdo->bitmask != 0) {
        XFreePixmap(awt_display, xsdo->bitmask);
        xsdo->bitmask = 0;
    }
    if (xsdo->javaGC != NULL) {
        XFreeGC(awt_display, xsdo->javaGC);
        xsdo->javaGC = NULL;
    }
    if (xsdo->cachedGC != NULL) {
        XFreeGC(awt_display, xsdo->cachedGC);
        xsdo->cachedGC = NULL;
    }

    if(xsdo->xrPic != None) {
      XRenderFreePicture(awt_display, xsdo->xrPic);
    }

    AWT_UNLOCK();
#endif /* !HEADLESS */
}
/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    setInvalid
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_setInvalid(JNIEnv *env, jobject xsd)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) SurfaceData_GetOps(env, xsd);

    if (xsdo != NULL) {
        xsdo->invalid = JNI_TRUE;
    }
#endif /* !HEADLESS */
}


jboolean XShared_initSurface(JNIEnv *env, X11SDOps *xsdo, jint depth, jint width, jint height, jlong drawable)
{
#ifndef HEADLESS

    if (drawable != (jlong)0) {
        /* Double-buffering */
        xsdo->drawable = drawable;
        xsdo->isPixmap = JNI_FALSE;
    } else {
        jboolean sizeIsInvalid = JNI_FALSE;
        jlong scan = 0;

        /*
         * width , height must be nonzero otherwise XCreatePixmap
         * generates BadValue in error_handler
         */
        if (width <= 0 || height <= 0 || width > 32767 || height > 32767) {
            sizeIsInvalid = JNI_TRUE;
        } else {
            XImage* tmpImg = NULL;

            AWT_LOCK();
            tmpImg = XCreateImage(awt_display,
                xsdo->configData->awt_visInfo.visual,
                depth, ZPixmap, 0, NULL, width, height,
                X11SD_GetBitmapPad(xsdo->configData->pixelStride), 0);
            if (tmpImg) {
                scan = (jlong) tmpImg->bytes_per_line;
                XDestroyImage(tmpImg);
                tmpImg = NULL;
            }
            AWT_UNLOCK();
            JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
        }

        if (sizeIsInvalid || (scan * height > 0x7FFFFFFFL)) {
            JNU_ThrowOutOfMemoryError(env,
                                  "Can't create offscreen surface");
            return JNI_FALSE;
        }
        xsdo->isPixmap = JNI_TRUE;

        xsdo->pmWidth = width;
        xsdo->pmHeight = height;

#ifdef MITSHM
        xsdo->shmPMData.pmSize = (jlong) width * height * depth;
        xsdo->shmPMData.pixelsReadThreshold = width * height / 8;
        if (forceSharedPixmaps) {
            AWT_LOCK();
            xsdo->drawable = X11SD_CreateSharedPixmap(xsdo);
            AWT_UNLOCK();
            JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
            if (xsdo->drawable) {
                xsdo->shmPMData.usingShmPixmap = JNI_TRUE;
                xsdo->shmPMData.shmPixmap = xsdo->drawable;
                return JNI_TRUE;
            }
        }
#endif /* MITSHM */

        AWT_LOCK();
        xsdo->drawable =
            XCreatePixmap(awt_display,
                          RootWindow(awt_display,
                                     xsdo->configData->awt_visInfo.screen),
                          width, height, depth);
        AWT_UNLOCK();
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
#ifdef MITSHM
        xsdo->shmPMData.usingShmPixmap = JNI_FALSE;
        xsdo->shmPMData.pixmap = xsdo->drawable;
#endif /* MITSHM */
    }
    if (xsdo->drawable == 0) {
        JNU_ThrowOutOfMemoryError(env,
                                  "Can't create offscreen surface");
        return JNI_FALSE;
    }

#endif /* !HEADLESS */
    return JNI_TRUE;
}


/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    initSurface
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11SurfaceData_initSurface(JNIEnv *env, jclass xsd,
                                               jint depth,
                                               jint width, jint height,
                                               jlong drawable)
{
#ifndef HEADLESS
    X11SDOps *xsdo = X11SurfaceData_GetOps(env, xsd);
    if (xsdo == NULL) {
        return;
    }

    if (xsdo->configData->awt_cmap == (Colormap)NULL) {
        awtJNI_CreateColorData(env, xsdo->configData, 1);
        JNU_CHECK_EXCEPTION(env);
    }
    /* color_data will be initialized in awtJNI_CreateColorData for
       8-bit visuals */
    xsdo->cData = xsdo->configData->color_data;

    XShared_initSurface(env, xsdo, depth, width, height, drawable);
    xsdo->xrPic = None;
#endif /* !HEADLESS */
}

#ifndef HEADLESS

#ifdef MITSHM

void X11SD_DropSharedSegment(XShmSegmentInfo *shminfo)
{
    if (shminfo != NULL) {
        XShmDetach(awt_display, shminfo);
        shmdt(shminfo->shmaddr);
/*      REMIND: we don't need shmctl(shminfo->shmid, IPC_RMID, 0); here. */
/*      Check X11SD_CreateSharedImage() for the explanation */
    }
}

XImage* X11SD_CreateSharedImage(X11SDOps *xsdo,
                                   jint width, jint height)
{
    XImage *img = NULL;
    XShmSegmentInfo *shminfo;

    shminfo = malloc(sizeof(XShmSegmentInfo));
    if (shminfo == NULL) {
        return NULL;
    }
    memset(shminfo, 0, sizeof(XShmSegmentInfo));

    img = XShmCreateImage(awt_display, xsdo->configData->awt_visInfo.visual,
                          xsdo->depth, ZPixmap, NULL, shminfo,
                          width, height);
    if (img == NULL) {
        free((void *)shminfo);
        return NULL;
    }
    shminfo->shmid =
        shmget(IPC_PRIVATE, (size_t) height * img->bytes_per_line,
               IPC_CREAT|mitShmPermissionMask);
    if (shminfo->shmid < 0) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "X11SD_SetupSharedSegment shmget has failed: %s",
                       strerror(errno));
        free((void *)shminfo);
        XDestroyImage(img);
        return NULL;
    }

    shminfo->shmaddr = (char *) shmat(shminfo->shmid, 0, 0);
    if (shminfo->shmaddr == ((char *) -1)) {
        shmctl(shminfo->shmid, IPC_RMID, 0);
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "X11SD_SetupSharedSegment shmat has failed: %s",
                       strerror(errno));
        free((void *)shminfo);
        XDestroyImage(img);
        return NULL;
    }

    shminfo->readOnly = False;

    resetXShmAttachFailed();
    EXEC_WITH_XERROR_HANDLER(XShmAttachXErrHandler,
                             XShmAttach(awt_display, shminfo));

    /*
     * Once the XSync round trip has finished then we
     * can get rid of the id so that this segment does not stick
     * around after we go away, holding system resources.
     */
    shmctl(shminfo->shmid, IPC_RMID, 0);

    if (isXShmAttachFailed() == JNI_TRUE) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "X11SD_SetupSharedSegment XShmAttach has failed: %s",
                       strerror(errno));
        shmdt(shminfo->shmaddr);
        free((void *)shminfo);
        XDestroyImage(img);
        return NULL;
    }

    img->data = shminfo->shmaddr;
    img->obdata = (char *)shminfo;

    return img;
}

XImage* X11SD_GetSharedImage(X11SDOps *xsdo, jint width, jint height,
                             jint maxWidth, jint maxHeight, jboolean readBits)
{
    XImage * retImage = NULL;
    if (cachedXImage != NULL &&
        X11SD_CachedXImageFits(width, height, maxWidth, maxHeight,
                               xsdo->depth, readBits)) {
        /* sync so previous data gets flushed */
        XSync(awt_display, False);
        retImage = cachedXImage;
        cachedXImage = (XImage *)NULL;
    } else if ((jlong) width * height * xsdo->depth > 0x10000) {
        retImage = X11SD_CreateSharedImage(xsdo, width, height);
    }
    return retImage;
}

Drawable X11SD_CreateSharedPixmap(X11SDOps *xsdo)
{
    XShmSegmentInfo *shminfo;
    XImage *img = NULL;
    Drawable pixmap;
    int scan;
    int width = xsdo->pmWidth;
    int height = xsdo->pmHeight;

    if (xsdo->shmPMData.pmSize < 0x10000) {
        /* only use shared mem pixmaps for relatively big images */
        return 0;
    }

    /* need to create shared(!) image to get bytes_per_line */
    img = X11SD_CreateSharedImage(xsdo, width, height);
    if (img == NULL) {
        return 0;
    }
    scan = img->bytes_per_line;
    shminfo = (XShmSegmentInfo*)img->obdata;
    XFree(img);

    pixmap =
        XShmCreatePixmap(awt_display,
                         RootWindow(awt_display,
                                    xsdo->configData->awt_visInfo.screen),
                         shminfo->shmaddr, shminfo,
                         width, height, xsdo->depth);
    if (pixmap == 0) {
        X11SD_DropSharedSegment(shminfo);
        return 0;
    }

    xsdo->shmPMData.shmSegInfo = shminfo;
    xsdo->shmPMData.bytesPerLine = scan;
    return pixmap;
}

void X11SD_PuntPixmap(X11SDOps *xsdo, jint width, jint height)
{

    if (useMitShmPixmaps != CAN_USE_MITSHM || forceSharedPixmaps) {
        return;
    }

    /* we wouldn't be here if it's a shared pixmap, so no check
     * for !usingShmPixmap.
     */

    xsdo->shmPMData.numBltsSinceRead = 0;

    xsdo->shmPMData.pixelsReadSinceBlt += width * height;
    if (xsdo->shmPMData.pixelsReadSinceBlt >
        xsdo->shmPMData.pixelsReadThreshold) {
        if (!xsdo->shmPMData.shmPixmap) {
            xsdo->shmPMData.shmPixmap =
                X11SD_CreateSharedPixmap(xsdo);
        }
        if (xsdo->shmPMData.shmPixmap) {
            GC xgc = XCreateGC(awt_display, xsdo->shmPMData.shmPixmap, 0L, NULL);
            if (xgc != NULL) {
                xsdo->shmPMData.usingShmPixmap = JNI_TRUE;
                xsdo->drawable = xsdo->shmPMData.shmPixmap;
                XCopyArea(awt_display,
                          xsdo->shmPMData.pixmap, xsdo->drawable, xgc,
                          0, 0, xsdo->pmWidth, xsdo->pmHeight, 0, 0);
                XSync(awt_display, False);
                xsdo->shmPMData.xRequestSent = JNI_FALSE;
                XFreeGC(awt_display, xgc);
            }
        }
    }
}

void X11SD_UnPuntPixmap(X11SDOps *xsdo)
{
    if (useMitShmPixmaps != CAN_USE_MITSHM || forceSharedPixmaps) {
        return;
    }
    xsdo->shmPMData.pixelsReadSinceBlt = 0;
    if (xsdo->shmPMData.numBltsSinceRead >=
        xsdo->shmPMData.numBltsThreshold)
    {
        if (xsdo->shmPMData.usingShmPixmap) {
            if (!xsdo->shmPMData.pixmap) {
                xsdo->shmPMData.pixmap =
                    XCreatePixmap(awt_display,
                                  RootWindow(awt_display,
                                             xsdo->configData->awt_visInfo.screen),
                                  xsdo->pmWidth, xsdo->pmHeight, xsdo->depth);
            }
            if (xsdo->shmPMData.pixmap) {
                GC xgc = XCreateGC(awt_display, xsdo->shmPMData.pixmap, 0L, NULL);
                if (xgc != NULL) {
                    xsdo->drawable = xsdo->shmPMData.pixmap;
                    XCopyArea(awt_display,
                              xsdo->shmPMData.shmPixmap, xsdo->drawable, xgc,
                              0, 0, xsdo->pmWidth, xsdo->pmHeight, 0, 0);
                    XSync(awt_display, False);
                    XFreeGC(awt_display, xgc);
                    xsdo->shmPMData.xRequestSent = JNI_FALSE;
                    xsdo->shmPMData.usingShmPixmap = JNI_FALSE;
                    xsdo->shmPMData.numBltsThreshold *= 2;
                }
            }
        }
    } else {
        xsdo->shmPMData.numBltsSinceRead++;
    }
}

/**
 * Determines if the cached image can be used for current operation.
 * If the image is to be used to be read into by XShmGetImage,
 * it must be close enough to avoid excessive reading from the screen;
 * otherwise it should just be at least the size requested.
 */
jboolean X11SD_CachedXImageFits(jint width, jint height, jint maxWidth,
                                jint maxHeight, jint depth, jboolean readBits)
{
    /* we assume here that the cached image exists */
    jint imgWidth = cachedXImage->width;
    jint imgHeight = cachedXImage->height;

    if (imgWidth < width || imgHeight < height || depth != cachedXImage->depth)  {
        /* doesn't fit if any of the cached image dimensions is smaller
           or the depths are different */
        return JNI_FALSE;
    }

    if (!readBits) {
        /* Not reading from this image, so any image at least of the
           size requested will do */
        return JNI_TRUE;
    }

    if ((imgWidth < width + 64) && (imgHeight < height + 64)
         && imgWidth <= maxWidth && imgHeight <= maxHeight)
    {
        /* Cached image's width/height shouldn't be more than 64 pixels
         * larger than requested, because the region in XShmGetImage
         * can't be specified and we don't want to read too much.
         * Furthermore it has to be smaller than maxWidth/Height
         * so drawables are not read out of bounds.
         */
        return JNI_TRUE;
    }

    return JNI_FALSE;
}
#endif /* MITSHM */

jint X11SD_InitWindow(JNIEnv *env, X11SDOps *xsdo)
{
    if (xsdo->isPixmap == JNI_TRUE) {
        return SD_FAILURE;
    }
    xsdo->cData = xsdo->configData->color_data;

    return SD_SUCCESS;
}

static jint X11SD_Lock(JNIEnv *env,
                       SurfaceDataOps *ops,
                       SurfaceDataRasInfo *pRasInfo,
                       jint lockflags)
{
    X11SDOps *xsdo = (X11SDOps *) ops;
    X11RIPrivate *xpriv = (X11RIPrivate *) &(pRasInfo->priv);
    int ret = SD_SUCCESS;

    AWT_LOCK();

    if (xsdo->invalid) {
        AWT_UNLOCK();
        SurfaceData_ThrowInvalidPipeException(env, "bounds changed");
        return SD_FAILURE;
    }
    xsdo->cData = xsdo->configData->color_data;
    if (xsdo->drawable == 0 && X11SD_InitWindow(env, xsdo) == SD_FAILURE) {
        AWT_UNLOCK();
        return SD_FAILURE;
    }
    if ((lockflags & SD_LOCK_LUT) != 0 &&
        (xsdo->cData == NULL ||
         xsdo->cData->awt_icmLUT == NULL))
    {
        AWT_UNLOCK();
        if (!(*env)->ExceptionCheck(env))
        {
             JNU_ThrowNullPointerException(env, "colormap lookup table");
        }
        return SD_FAILURE;
    }
    if ((lockflags & SD_LOCK_INVCOLOR) != 0 &&
        (xsdo->cData == NULL ||
         xsdo->cData->img_clr_tbl == NULL ||
         xsdo->cData->img_oda_red == NULL ||
         xsdo->cData->img_oda_green == NULL ||
         xsdo->cData->img_oda_blue == NULL))
    {
        AWT_UNLOCK();
        if (!(*env)->ExceptionCheck(env))
        {
             JNU_ThrowNullPointerException(env, "inverse colormap lookup table");
        }
        return SD_FAILURE;
    }
    if ((lockflags & SD_LOCK_INVGRAY) != 0 &&
        (xsdo->cData == NULL ||
         xsdo->cData->pGrayInverseLutData == NULL))
    {
        AWT_UNLOCK();
        if (!(*env)->ExceptionCheck(env))
        {
            JNU_ThrowNullPointerException(env, "inverse gray lookup table");
        }
        return SD_FAILURE;
    }
    if (lockflags & SD_LOCK_RD_WR) {
        if (lockflags & SD_LOCK_FASTEST) {
            ret = SD_SLOWLOCK;
        }
        xpriv->lockType = X11SD_LOCK_BY_XIMAGE;
        if (xsdo->isPixmap) {
#ifdef MITSHM
            if (xsdo->shmPMData.usingShmPixmap) {
                xpriv->lockType = X11SD_LOCK_BY_SHMEM;
            }
#endif /* MITSHM */
            if (pRasInfo->bounds.x1 < 0) {
                pRasInfo->bounds.x1 = 0;
            }
            if (pRasInfo->bounds.y1 < 0) {
                pRasInfo->bounds.y1 = 0;
            }
            if (pRasInfo->bounds.x2 > xsdo->pmWidth) {
                pRasInfo->bounds.x2 = xsdo->pmWidth;
            }
            if (pRasInfo->bounds.y2 > xsdo->pmHeight) {
                pRasInfo->bounds.y2 = xsdo->pmHeight;
            }
        }
    } else {
        /* They didn't lock for anything - we won't give them anything */
        xpriv->lockType = X11SD_LOCK_BY_NULL;
    }
    xpriv->lockFlags = lockflags;
    xpriv->img = NULL;

    return ret;
    /* AWT_UNLOCK() called in Unlock */
}

static void X11SD_GetRasInfo(JNIEnv *env,
                             SurfaceDataOps *ops,
                             SurfaceDataRasInfo *pRasInfo)
{
    X11SDOps *xsdo = (X11SDOps *) ops;
    X11RIPrivate *xpriv = (X11RIPrivate *) &(pRasInfo->priv);
    jint lockFlags = xpriv->lockFlags;
    jint depth = xsdo->depth;
    int mult = xsdo->configData->pixelStride;


#ifdef MITSHM
    if (xpriv->lockType == X11SD_LOCK_BY_SHMEM) {
        if (xsdo->shmPMData.xRequestSent == JNI_TRUE) {
            /* need to sync before using shared mem pixmap
             if any x calls were issued for this pixmap */
            XSync(awt_display, False);
            xsdo->shmPMData.xRequestSent = JNI_FALSE;
        }
        xpriv->x = pRasInfo->bounds.x1;
        xpriv->y = pRasInfo->bounds.y1;
        pRasInfo->rasBase = xsdo->shmPMData.shmSegInfo->shmaddr;
        pRasInfo->pixelStride = mult;
        pRasInfo->pixelBitOffset = 0;
        pRasInfo->scanStride = xsdo->shmPMData.bytesPerLine;
    } else
#endif /* MITSHM */
    if (xpriv->lockType == X11SD_LOCK_BY_XIMAGE) {
        int x, y, w, h;
        x = pRasInfo->bounds.x1;
        y = pRasInfo->bounds.y1;
        w = pRasInfo->bounds.x2 - x;
        h = pRasInfo->bounds.y2 - y;

        xpriv->img = X11SD_GetImage(env, xsdo, &pRasInfo->bounds, lockFlags);
        if (xpriv->img) {
            int scan = xpriv->img->bytes_per_line;
            xpriv->x = x;
            xpriv->y = y;
            pRasInfo->rasBase = xpriv->img->data - x * mult - (intptr_t) y * scan;
            pRasInfo->pixelStride = mult;
            pRasInfo->pixelBitOffset = 0;
            pRasInfo->scanStride = scan;
        } else {
            pRasInfo->rasBase = NULL;
            pRasInfo->pixelStride = 0;
            pRasInfo->pixelBitOffset = 0;
            pRasInfo->scanStride = 0;
        }
    } else {
        /* They didn't lock for anything - we won't give them anything */
        pRasInfo->rasBase = NULL;
        pRasInfo->pixelStride = 0;
        pRasInfo->pixelBitOffset = 0;
        pRasInfo->scanStride = 0;
    }
    if (lockFlags & SD_LOCK_LUT) {
        pRasInfo->lutBase = (jint *) xsdo->cData->awt_icmLUT;
        pRasInfo->lutSize = xsdo->cData->awt_numICMcolors;
    } else {
        pRasInfo->lutBase = NULL;
        pRasInfo->lutSize = 0;
    }
    if (lockFlags & SD_LOCK_INVCOLOR) {
        pRasInfo->invColorTable = xsdo->cData->img_clr_tbl;
        pRasInfo->redErrTable = xsdo->cData->img_oda_red;
        pRasInfo->grnErrTable = xsdo->cData->img_oda_green;
        pRasInfo->bluErrTable = xsdo->cData->img_oda_blue;
    } else {
        pRasInfo->invColorTable = NULL;
        pRasInfo->redErrTable = NULL;
        pRasInfo->grnErrTable = NULL;
        pRasInfo->bluErrTable = NULL;
    }
    if (lockFlags & SD_LOCK_INVGRAY) {
        pRasInfo->invGrayTable = xsdo->cData->pGrayInverseLutData;
    } else {
        pRasInfo->invGrayTable = NULL;
    }
}

static void X11SD_Unlock(JNIEnv *env,
                         SurfaceDataOps *ops,
                         SurfaceDataRasInfo *pRasInfo)
{
    X11SDOps *xsdo = (X11SDOps *) ops;
    X11RIPrivate *xpriv = (X11RIPrivate *) &(pRasInfo->priv);

    if (xpriv->lockType == X11SD_LOCK_BY_XIMAGE &&
               xpriv->img != NULL)
    {
        if (xpriv->lockFlags & SD_LOCK_WRITE) {
            int x = xpriv->x;
            int y = xpriv->y;
            int w = pRasInfo->bounds.x2 - x;
            int h = pRasInfo->bounds.y2 - y;
            Drawable drawable = xsdo->drawable;
            GC xgc = xsdo->cachedGC;
            if (xgc == NULL) {
                xsdo->cachedGC = xgc =
                    XCreateGC(awt_display, drawable, 0L, NULL);
            }

            if (xpriv->img->byte_order != nativeByteOrder) {
                /* switching bytes back in 24 and 32 bpp cases. */
                /* For 16 bit XLib will switch for us.          */
                if (xsdo->depth > 16) {
                    X11SD_SwapBytes(xsdo, xpriv->img, xsdo->depth,
                        xsdo->configData->awtImage->wsImageFormat.bits_per_pixel);
                }
            }

#ifdef MITSHM
            if (xpriv->img->obdata != NULL) {
                XShmPutImage(awt_display, drawable, xgc,
                             xpriv->img, 0, 0, x, y, w, h, False);
                XFlush(awt_display);
            } else {
                XPutImage(awt_display, drawable, xgc,
                          xpriv->img, 0, 0, x, y, w, h);
            }
            if (xsdo->shmPMData.usingShmPixmap) {
                xsdo->shmPMData.xRequestSent = JNI_TRUE;
            }
#else
            XPutImage(awt_display, drawable, xgc,
                      xpriv->img, 0, 0, x, y, w, h);
#endif /* MITSHM */

        }
        X11SD_DisposeOrCacheXImage(xpriv->img);
        xpriv->img = (XImage *)NULL;
    }
    /* the background pixel is not valid anymore */
    if (xpriv->lockFlags & SD_LOCK_WRITE) {
        xsdo->isBgInitialized = JNI_FALSE;
    }
    xpriv->lockType = X11SD_LOCK_UNLOCKED;
    AWT_UNLOCK();
}

static int
X11SD_ClipToRoot(SurfaceDataBounds *b, SurfaceDataBounds *bounds,
                 X11SDOps *xsdo)
{
    short x1=0, y1=0, x2=0, y2=0;
    int tmpx, tmpy;
    Window tmpchild;

    Window window = (Window)(xsdo->drawable); /* is always a Window */
    XWindowAttributes winAttr;

    Status status = XGetWindowAttributes(awt_display, window, &winAttr);
    if (status == 0) {
        /* Failure, X window no longer valid. */
        return FALSE;
    }
    if (!XTranslateCoordinates(awt_display, window,
                               RootWindowOfScreen(winAttr.screen),
                               0, 0, &tmpx, &tmpy, &tmpchild)) {
        return FALSE;
    }

    x1 = -(x1 + tmpx);
    y1 = -(y1 + tmpy);

    x2 = x1 + DisplayWidth(awt_display, xsdo->configData->awt_visInfo.screen);
    y2 = y1 + DisplayHeight(awt_display, xsdo->configData->awt_visInfo.screen);

    x1 = XSD_MAX(bounds->x1, x1);
    y1 = XSD_MAX(bounds->y1, y1);
    x2 = XSD_MIN(bounds->x2, x2);
    y2 = XSD_MIN(bounds->y2, y2);
    if ((x1 >= x2) || (y1 >= y2)) {
        return FALSE;
    }
    b->x1 = x1;
    b->y1 = y1;
    b->x2 = x2;
    b->y2 = y2;

    return TRUE;
}

/*
 * x1, y1, x2, y2 - our rectangle in the coord system of
 * the widget
 * px1, xy1, px2, py2 - current parent rect coords in the
 * same system
 */
static int
X11SD_FindClip(SurfaceDataBounds *b, SurfaceDataBounds *bounds, X11SDOps *xsdo)
{
    return TRUE;
}

static void
X11SD_SwapBytes(X11SDOps *xsdo, XImage * img, int depth, int bpp) {
    jlong lengthInBytes = (jlong) img->height * img->bytes_per_line;
    jlong i;

    switch (depth) {
    case 12:
    case 15:
    case 16:
        {
            /* AB -> BA */
            unsigned short *d = (unsigned short *)img->data;
            unsigned short t;
            for (i = 0; i < lengthInBytes/2; i++) {
                t = *d;
                *d++ = (t >> 8) | (t << 8);
            }
            img->byte_order = nativeByteOrder;
            img->bitmap_bit_order = nativeByteOrder;
            break;
        }
    case 24:
        {
            /* ABC -> CBA */
            if (bpp == 24) {
                // 4517321: Only swap if we have a "real" ThreeByteBgr
                // visual (denoted by a red_mask of 0xff).  Due to ambiguity
                // in the X11 spec, it appears that the swap is not required
                // on Linux configurations that use 24 bits per pixel (denoted
                // by a red_mask of 0xff0000).
                if (xsdo->configData->awt_visInfo.red_mask == 0xff) {
                    int scan = img->bytes_per_line;
                    unsigned char *d = (unsigned char *) img->data;
                    unsigned char *d1;
                    unsigned int t;
                    int j;

                    for (i = 0; i < img->height; i++, d += scan) {
                        d1 = d;
                        for (j = 0; j < img->width; j++, d1 += 3) {
                            /* not obvious opt from XLib src */
                            t = d1[0]; d1[0] = d1[2]; d1[2] = t;
                        }
                    }
                }
                break;
            }
        }
        /* FALL THROUGH for 32-bit case */
    case 32:
        {
            /* ABCD -> DCBA */
            unsigned int *d = (unsigned int *) img->data;
            unsigned int t;
            for (i = 0; i < lengthInBytes/4; i++) {
                t = *d;
                *d++ = ((t >> 24) |
                        ((t >> 8) & 0xff00) |
                        ((t & 0xff00) << 8) |
                        (t << 24));
            }
            break;
        }
    }
}

static XImage * X11SD_GetImage(JNIEnv *env, X11SDOps *xsdo,
                               SurfaceDataBounds *bounds,
                               jint lockFlags)
{
    int x, y, w, h, maxWidth, maxHeight;
    int scan;
    XImage * img = NULL;
    Drawable drawable;
    int depth = xsdo->depth;
    int mult = xsdo->configData->pixelStride;
    int pad = X11SD_GetBitmapPad(mult);
    jboolean readBits = lockFlags & SD_LOCK_NEED_PIXELS;

    x = bounds->x1;
    y = bounds->y1;
    w = bounds->x2 - x;
    h = bounds->y2 - y;

#ifdef MITSHM
    if (useMitShmExt == CAN_USE_MITSHM) {
        if (xsdo->isPixmap) {
            if (readBits) {
                X11SD_PuntPixmap(xsdo, w, h);
            }
            maxWidth = xsdo->pmWidth;
            maxHeight = xsdo->pmHeight;
        } else {
            XWindowAttributes winAttr;
            if (XGetWindowAttributes(awt_display,
                                     (Window) xsdo->drawable, &winAttr) != 0) {
                maxWidth = winAttr.width;
                maxHeight = winAttr.height;
           } else {
                /* XGWA failed which isn't a good thing. Defaulting to using
                 * x,y means that after the subtraction of these we will use
                 * w=0, h=0 which is a reasonable default on such a failure.
                 */
                maxWidth = x;
                maxHeight = y;
           }
        }
        maxWidth -= x;
        maxHeight -= y;

        img = X11SD_GetSharedImage(xsdo, w, h, maxWidth, maxHeight, readBits);
    }
#endif /* MITSHM */
    drawable = xsdo->drawable;

    if (readBits) {
#ifdef MITSHM
        if (img != NULL) {
            if (!XShmGetImage(awt_display, drawable, img, x, y, -1)) {
                X11SD_DisposeOrCacheXImage(img);
                img = NULL;
            }
        }
        if (img == NULL) {
            img = XGetImage(awt_display, drawable, x, y, w, h, -1, ZPixmap);
            if (img != NULL) {
                img->obdata = NULL;
            }
        }
#else
        img = XGetImage(awt_display, drawable, x, y, w, h, -1, ZPixmap);
#endif /* MITSHM */
        if (img == NULL) {
            SurfaceDataBounds temp;
            img = XCreateImage(awt_display,
                               xsdo->configData->awt_visInfo.visual,
                               depth, ZPixmap, 0, NULL, w, h, pad, 0);
            if (img == NULL) {
                return NULL;
            }

            scan = img->bytes_per_line;
            img->data = malloc((size_t) h * scan);
            if (img->data == NULL) {
                XFree(img);
                return NULL;
            }

            if (xsdo->isPixmap == JNI_FALSE &&
                X11SD_ClipToRoot(&temp, bounds, xsdo)) {

                XImage * temp_image;
                temp_image = XGetImage(awt_display, drawable,
                                       temp.x1, temp.y1,
                                       temp.x2 - temp.x1,
                                       temp.y2 - temp.y1,
                                       -1, ZPixmap);
                if (temp_image == NULL) {
                    XGrabServer(awt_display);
                    if (X11SD_FindClip(&temp, bounds, xsdo)) {
                        temp_image =
                            XGetImage(awt_display, drawable,
                                      temp.x1, temp.y1,
                                      temp.x2 - temp.x1,
                                      temp.y2 - temp.y1,
                                      -1, ZPixmap);
                    }
                    XUngrabServer(awt_display);
                    /* Workaround for bug 5039226 */
                    XSync(awt_display, False);
                }
                if (temp_image != NULL) {
                    int temp_scan, bytes_to_copy;
                    char * img_addr, * temp_addr;
                    int i;

                    img_addr = img->data +
                        (intptr_t) (temp.y1 - y) * scan + (temp.x1 - x) * mult;
                    temp_scan = temp_image->bytes_per_line;
                    temp_addr = temp_image->data;
                    bytes_to_copy = (temp.x2 - temp.x1) * mult;
                    for (i = temp.y1; i < temp.y2; i++) {
                        memcpy(img_addr, temp_addr, bytes_to_copy);
                        img_addr += scan;
                        temp_addr += temp_scan;
                    }
                    XDestroyImage(temp_image);
                }
            }
            img->obdata = NULL;
        }
        if (depth > 8 && img->byte_order != nativeByteOrder) {
            X11SD_SwapBytes(xsdo, img, depth,
                xsdo->configData->awtImage->wsImageFormat.bits_per_pixel);
        }
    } else {
        /*
         * REMIND: This might be better to move to the Lock function
         * to avoid lengthy I/O pauses inside what may be a critical
         * section.  This will be more critical when SD_LOCK_READ is
         * implemented.  Another solution is to cache the pixels
         * to avoid reading for every operation.
         */
        if (img == NULL) {
            img = XCreateImage(awt_display,
                               xsdo->configData->awt_visInfo.visual,
                               depth, ZPixmap, 0, NULL, w, h, pad, 0);
            if (img == NULL) {
                return NULL;
            }

            img->data = malloc((size_t) h * img->bytes_per_line);
            if (img->data == NULL) {
                XFree(img);
                return NULL;
            }

            img->obdata = NULL;

            if (img->byte_order != nativeByteOrder &&
                (depth == 15 || depth == 16 || depth == 12)) {
                /* bytes will be swapped by XLib. */
                img->byte_order = nativeByteOrder;
                img->bitmap_bit_order = nativeByteOrder;
            }
        }
    }
    return img;
}

void X11SD_DisposeOrCacheXImage(XImage * image) {
    /* REMIND: might want to check if the new image worth caching. */
    /* Cache only shared images. Passed image is assumed to be non-null. */
    if (image->obdata != NULL) {
        if (cachedXImage != NULL) {
            X11SD_DisposeXImage(cachedXImage);
        }
        cachedXImage = image;
    } else {
        X11SD_DisposeXImage(image);
    }
}

void X11SD_DisposeXImage(XImage * image) {
    if (image != NULL) {
#ifdef MITSHM
        if (image->obdata != NULL) {
            X11SD_DropSharedSegment((XShmSegmentInfo*)image->obdata);
            image->obdata = NULL;
        }
#endif /* MITSHM */
        XDestroyImage(image);
    }
}

void
X11SD_DirectRenderNotify(JNIEnv *env, X11SDOps *xsdo)
{
#ifdef MITSHM
    if (xsdo->shmPMData.usingShmPixmap) {
        xsdo->shmPMData.xRequestSent = JNI_TRUE;
    }
#endif /* MITSHM */
    awt_output_flush();
}

/*
 * Sets transparent pixels in the pixmap to
 * the specified solid background color and returns it.
 * Doesn't update source pixmap unless the color of the
 * transparent pixels is different from the specified color.
 *
 * Note: The AWT lock must be held by the current thread
 * while calling into this method.
 */
static Drawable
X11SD_GetPixmapWithBg(JNIEnv *env, X11SDOps *xsdo, jint pixel)
{
    /* assert AWT_CHECK_HAVE_LOCK(); */

    if (xsdo->invalid) {
        AWT_UNLOCK();
        SurfaceData_ThrowInvalidPipeException(env, "bounds changed");
        return 0;
    }

    /* the image doesn't have transparency, just return it */
    if (xsdo->bitmask == 0) {
        /* don't need to unlock here, the caller will unlock through
           the release call */
        return xsdo->drawable;
    }

    /* Check if current color of the transparent pixels is different
       from the specified one */
    if (xsdo->isBgInitialized == JNI_FALSE || xsdo->bgPixel != pixel) {
        GC srcGC;
        GC bmGC;

        if (xsdo->drawable == 0) {
            AWT_UNLOCK();
            return 0;
        }

        bmGC = XCreateGC(awt_display, xsdo->bitmask, 0, NULL);
        if (bmGC == NULL) {
            AWT_UNLOCK();
            return 0;
        }

        /* invert the bitmask */
        XSetFunction(awt_display, bmGC, GXxor);
        XSetForeground(awt_display, bmGC, 1);
        XFillRectangle(awt_display, xsdo->bitmask, bmGC,
                       0, 0, xsdo->pmWidth, xsdo->pmHeight);

        srcGC = XCreateGC(awt_display, xsdo->drawable, 0L, NULL);
        if (srcGC == NULL) {
            XFreeGC(awt_display, bmGC);
            AWT_UNLOCK();
            return 0;
        }

        /* set transparent pixels in the source pm to the bg color */
        XSetClipMask(awt_display, srcGC, xsdo->bitmask);
        XSetForeground(awt_display, srcGC, pixel);
        XFillRectangle(awt_display, xsdo->drawable, srcGC,
                       0, 0, xsdo->pmWidth, xsdo->pmHeight);

        /* invert the mask back */
        XFillRectangle(awt_display, xsdo->bitmask, bmGC,
                       0, 0, xsdo->pmWidth, xsdo->pmHeight);

        XFreeGC(awt_display, bmGC);
        XFreeGC(awt_display, srcGC);
        xsdo->bgPixel = pixel;
        xsdo->isBgInitialized = JNI_TRUE;
    }

    return xsdo->drawable;
}

static void
X11SD_ReleasePixmapWithBg(JNIEnv *env, X11SDOps *xsdo)
{
#ifdef MITSHM
    if (xsdo->shmPMData.usingShmPixmap) {
        xsdo->shmPMData.xRequestSent = JNI_TRUE;
    }
#endif /* MITSHM */
}

static int X11SD_GetBitmapPad(int pixelStride) {
    // pad must be 8, 16, or 32
    return (pixelStride == 3) ? 32 : pixelStride * 8;
}

#endif /* !HEADLESS */

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XCreateGC
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_x11_XSurfaceData_XCreateGC
    (JNIEnv *env, jclass xsd, jlong pXSData)
{
    jlong ret;

#ifndef HEADLESS
    X11SDOps *xsdo;

    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XCreateGC");

    xsdo = (X11SDOps *) pXSData;
    if (xsdo == NULL) {
        return 0L;
    }

    xsdo->javaGC = XCreateGC(awt_display, xsdo->drawable, 0, NULL);
    ret = (jlong) xsdo->javaGC;
#else /* !HEADLESS */
    ret = 0L;
#endif /* !HEADLESS */

    return ret;
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XResetClip
 * Signature: (JIIIILsun/java2d/pipe/Region;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_XResetClip
    (JNIEnv *env, jclass xsd, jlong xgc)
{
#ifndef HEADLESS
    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XResetClip");
    XSetClipMask(awt_display, (GC) xgc, None);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XSetClip
 * Signature: (JIIIILsun/java2d/pipe/Region;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_XSetClip
    (JNIEnv *env, jclass xsd, jlong xgc,
     jint x1, jint y1, jint x2, jint y2,
     jobject complexclip)
{
#ifndef HEADLESS
    int numrects;
    XRectangle rects[256];
    XRectangle *pRect = rects;

    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XSetClip");

    numrects = RegionToYXBandedRectangles(env,
            x1, y1, x2, y2, complexclip,
            &pRect, 256);

    XSetClipRectangles(awt_display, (GC) xgc, 0, 0, pRect, numrects, YXBanded);

    if (pRect != rects) {
        free(pRect);
    }
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XSetCopyMode
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11SurfaceData_XSetCopyMode
    (JNIEnv *env, jclass xsd, jlong xgc)
{
#ifndef HEADLESS
    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XSetCopyMode");
    XSetFunction(awt_display, (GC) xgc, GXcopy);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XSetXorMode
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11SurfaceData_XSetXorMode
    (JNIEnv *env, jclass xr, jlong xgc)
{
#ifndef HEADLESS
    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XSetXorMode");
    XSetFunction(awt_display, (GC) xgc, GXxor);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XSetForeground
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11SurfaceData_XSetForeground
    (JNIEnv *env, jclass xsd, jlong xgc, jint pixel)
{
#ifndef HEADLESS
    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XSetForeground");
    XSetForeground(awt_display, (GC) xgc, pixel);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11SurfaceData
 * Method:    XSetGraphicsExposures
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_XSurfaceData_XSetGraphicsExposures
    (JNIEnv *env, jclass xsd, jlong xgc, jboolean needExposures)
{
#ifndef HEADLESS
    J2dTraceLn(J2D_TRACE_INFO, "in X11SurfaceData_XSetGraphicsExposures");
    XSetGraphicsExposures(awt_display, (GC) xgc, needExposures ? True : False);
#endif /* !HEADLESS */
}
