/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <jlong.h>
#include "X11SurfaceData.h"
#include "Region.h"

JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11PMBlitLoops_nativeBlit
    (JNIEnv *env, jobject joSelf,
     jlong srcData, jlong dstData,
     jlong gc, jobject clip,
     jint srcx, jint srcy,
     jint dstx, jint dsty,
     jint width, jint height)
{
#ifndef HEADLESS
    X11SDOps *srcXsdo, *dstXsdo;
    SurfaceDataBounds span, srcBounds;
    RegionData clipInfo;
    GC xgc;

    if (width <= 0 || height <= 0) {
        return;
    }

    srcXsdo = (X11SDOps *)jlong_to_ptr(srcData);
    if (srcXsdo == NULL) {
        return;
    }
    dstXsdo = (X11SDOps *)jlong_to_ptr(dstData);
    if (dstXsdo == NULL) {
        return;
    }
    if (Region_GetInfo(env, clip, &clipInfo)) {
        return;
    }

    xgc = (GC)gc;
    if (xgc == NULL) {
        return;
    }

#ifdef MITSHM
    if (srcXsdo->isPixmap) {
        X11SD_UnPuntPixmap(srcXsdo);
    }
#endif /* MITSHM */

    /* clip the source rect to the source pixmap's dimensions */
    srcBounds.x1 = srcx;
    srcBounds.y1 = srcy;
    srcBounds.x2 = srcx + width;
    srcBounds.y2 = srcy + height;
    SurfaceData_IntersectBoundsXYXY(&srcBounds,
                                    0, 0, srcXsdo->pmWidth, srcXsdo->pmHeight);
    span.x1 = dstx;
    span.y1 = dsty;
    span.x2 = dstx + width;
    span.y2 = dsty + height;

    /* intersect the source and dest rects */
    SurfaceData_IntersectBlitBounds(&srcBounds, &span,
                                    dstx - srcx, dsty - srcy);
    srcx = srcBounds.x1;
    srcy = srcBounds.y1;
    dstx = span.x1;
    dsty = span.y1;

    if (srcXsdo->bitmask != 0) {
        XSetClipOrigin(awt_display, xgc, dstx - srcx, dsty - srcy);
        XSetClipMask(awt_display, xgc, srcXsdo->bitmask);
    }

    Region_IntersectBounds(&clipInfo, &span);
    if (!Region_IsEmpty(&clipInfo)) {
        Region_StartIteration(env, &clipInfo);
        srcx -= dstx;
        srcy -= dsty;
        while (Region_NextIteration(&clipInfo, &span)) {
            XCopyArea(awt_display, srcXsdo->drawable, dstXsdo->drawable, xgc,
                      srcx + span.x1, srcy + span.y1,
                      span.x2 - span.x1, span.y2 - span.y1,
                      span.x1, span.y1);
        }
        Region_EndIteration(env, &clipInfo);
    }

    if (srcXsdo->bitmask != 0) {
        XSetClipMask(awt_display, xgc, None);
    }

#ifdef MITSHM
    if (srcXsdo->shmPMData.usingShmPixmap) {
        srcXsdo->shmPMData.xRequestSent = JNI_TRUE;
    }
#endif /* MITSHM */
    X11SD_DirectRenderNotify(env, dstXsdo);
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11PMBlitBgLoops_nativeBlitBg
    (JNIEnv *env, jobject joSelf,
     jlong srcData, jlong dstData,
     jlong xgc, jint pixel,
     jint srcx, jint srcy,
     jint dstx, jint dsty,
     jint width, jint height)
{
#ifndef HEADLESS
    X11SDOps *srcXsdo, *dstXsdo;
    GC dstGC;
    SurfaceDataBounds dstBounds, srcBounds;
    Drawable srcDrawable;

    if (width <= 0 || height <= 0) {
        return;
    }

    srcXsdo = (X11SDOps *)jlong_to_ptr(srcData);
    if (srcXsdo == NULL) {
        return;
    }
    dstXsdo = (X11SDOps *)jlong_to_ptr(dstData);
    if (dstXsdo == NULL) {
        return;
    }

    dstGC = (GC)xgc;
    if (dstGC == NULL) {
        return;
    }

#ifdef MITSHM
    if (srcXsdo->isPixmap) {
        X11SD_UnPuntPixmap(srcXsdo);
    }
#endif /* MITSHM */

    srcDrawable = srcXsdo->GetPixmapWithBg(env, srcXsdo, pixel);
    if (srcDrawable == 0) {
        return;
    }

    /* clip the source rect to the source pixmap's dimensions */
    srcBounds.x1 = srcx;
    srcBounds.y1 = srcy;
    srcBounds.x2 = srcx + width;
    srcBounds.y2 = srcy + height;
    SurfaceData_IntersectBoundsXYXY(&srcBounds,
                                    0, 0, srcXsdo->pmWidth, srcXsdo->pmHeight);
    dstBounds.x1 = dstx;
    dstBounds.y1 = dsty;
    dstBounds.x2 = dstx + width;
    dstBounds.y2 = dsty + height;

    /* intersect the source and dest rects */
    SurfaceData_IntersectBlitBounds(&srcBounds, &dstBounds,
                                    dstx - srcx, dsty - srcy);
    srcx = srcBounds.x1;
    srcy = srcBounds.y1;
    dstx = dstBounds.x1;
    dsty = dstBounds.y1;
    width = srcBounds.x2 - srcBounds.x1;
    height = srcBounds.y2 - srcBounds.y1;

    /* do an unmasked copy as we've already filled transparent
       pixels of the source image with the desired color */
    XCopyArea(awt_display, srcDrawable, dstXsdo->drawable, dstGC,
              srcx, srcy, width, height, dstx, dsty);

    srcXsdo->ReleasePixmapWithBg(env, srcXsdo);
    X11SD_DirectRenderNotify(env, dstXsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11PMBlitLoops
 * Method:    updateBitmask
 * Signature: (Lsun/java2d/SurfaceData;Lsun/java2d/SurfaceData;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11PMBlitLoops_updateBitmask
    (JNIEnv *env, jclass xpmbl, jobject srcsd, jobject dstsd, jboolean isICM)
{
#ifndef HEADLESS
    SurfaceDataOps *srcOps = SurfaceData_GetOps(env, srcsd);
    X11SDOps *xsdo = (X11SDOps *) SurfaceData_GetOps(env, dstsd);
    SurfaceDataRasInfo srcInfo;

    int flags;
    int screen;
    int width;
    int height;
    jint srcScan, dstScan;
    int rowCount;
    unsigned char *pDst;
    XImage *image;
    GC xgc;

    if (srcOps == NULL || xsdo == NULL) {
        JNU_ThrowNullPointerException(env, "Null BISD in updateMaskRegion");
        return;
    }

    AWT_LOCK();

    screen = xsdo->configData->awt_visInfo.screen;
    width = xsdo->pmWidth;
    height = xsdo->pmHeight;

    if (xsdo->bitmask == 0) {
        /* create the bitmask if it is not yet created */
        xsdo->bitmask = XCreatePixmap(awt_display,
                                      RootWindow(awt_display, screen),
                                      width, height, 1);
        if (xsdo->bitmask == 0) {
            AWT_UNLOCK();
            if (!(*env)->ExceptionCheck(env))
            {
                JNU_ThrowOutOfMemoryError(env,
                                          "Cannot create bitmask for "
                                          "offscreen surface");
            }
            return;
        }
    }

    /* Create a bitmask image and then blit it to the pixmap. */
    image = XCreateImage(awt_display, DefaultVisual(awt_display, screen),
                         1, XYBitmap, 0, NULL, width, height, 32, 0);
    if (image == NULL) {
        AWT_UNLOCK();
        if (!(*env)->ExceptionCheck(env))
        {
             JNU_ThrowOutOfMemoryError(env, "Cannot allocate bitmask for mask");
        }
        return;
    }
    dstScan = image->bytes_per_line;
    image->data = malloc((size_t) dstScan * height);
    if (image->data == NULL) {
        XFree(image);
        AWT_UNLOCK();
        if (!(*env)->ExceptionCheck(env))
        {
            JNU_ThrowOutOfMemoryError(env, "Cannot allocate bitmask for mask");
        }
        return;
    }
    pDst = (unsigned char *)image->data;

    srcInfo.bounds.x1 = 0;
    srcInfo.bounds.y1 = 0;
    srcInfo.bounds.x2 = width;
    srcInfo.bounds.y2 = height;

    flags = (isICM ? (SD_LOCK_LUT | SD_LOCK_READ) : SD_LOCK_READ);
    if (srcOps->Lock(env, srcOps, &srcInfo, flags) != SD_SUCCESS) {
        XDestroyImage(image);
        AWT_UNLOCK();
        return;
    }
    srcOps->GetRasInfo(env, srcOps, &srcInfo);

    rowCount = height;
    if (isICM) {
        unsigned char *pSrc;
        jint *srcLut;

        srcScan = srcInfo.scanStride;
        srcLut = srcInfo.lutBase;
        pSrc = (unsigned char *)srcInfo.rasBase;

        if (image->bitmap_bit_order == MSBFirst) {
            do {
                int x = 0, bx = 0;
                unsigned int pix = 0;
                unsigned int bit = 0x80;
                unsigned char *srcPixel = pSrc;
                do {
                    if (bit == 0) {
                        pDst[bx++] = (unsigned char)pix;
                        pix = 0;
                        bit = 0x80;
                    }
                    pix |= bit & (srcLut[*srcPixel++] >> 31);
                    bit >>= 1;
                } while (++x < width);
                pDst[bx] = (unsigned char)pix;
                pDst += dstScan;
                pSrc = (unsigned char *) (((intptr_t)pSrc) + srcScan);
            } while (--rowCount > 0);
        } else {
            do {
                int x = 0, bx = 0;
                unsigned int pix = 0;
                unsigned int bit = 1;
                unsigned char *srcPixel = pSrc;
                do {
                    if ((bit >> 8) != 0) {
                        pDst[bx++] = (unsigned char) pix;
                        pix = 0;
                        bit = 1;
                    }
                    pix |= bit & (srcLut[*srcPixel++] >> 31);
                    bit <<= 1;
                } while (++x < width);
                pDst[bx] = (unsigned char) pix;
                pDst += dstScan;
                pSrc = (unsigned char *) (((intptr_t)pSrc) + srcScan);
            } while (--rowCount > 0);
        }
    } else /*DCM with ARGB*/ {
        unsigned int *pSrc;

        /* this is a number of pixels in a row, not number of bytes */
        srcScan = srcInfo.scanStride;
        pSrc = (unsigned int *)srcInfo.rasBase;

        if (image->bitmap_bit_order == MSBFirst) {
            do {
                int x = 0, bx = 0;
                unsigned int pix = 0;
                unsigned int bit = 0x80;
                int *srcPixel = (int *) pSrc;
                do {
                    if (bit == 0) {
                        /* next word */
                        pDst[bx++] = (unsigned char)pix;
                        pix = 0;
                        bit = 0x80;
                    }
                    if (*srcPixel++ & 0xff000000) {
                        /* if src pixel is opaque, set the bit in the bitmap */
                        pix |= bit;
                    }
                    bit >>= 1;
                } while (++x < width);
                /* last pixels in a row */
                pDst[bx] = (unsigned char)pix;

                pDst += dstScan;
                pSrc = (unsigned int *) (((intptr_t)pSrc) + srcScan);
            } while (--rowCount > 0);
        } else {
            do {
                int x = 0, bx = 0;
                unsigned int pix = 0;
                unsigned int bit = 1;
                int *srcPixel = (int *) pSrc;
                do {
                    if ((bit >> 8) != 0) {
                        pDst[bx++] = (unsigned char)pix;
                        pix = 0;
                        bit = 1;
                    }
                    if (*srcPixel++ & 0xff000000) {
                        /* if src pixel is opaque, set the bit in the bitmap */
                        pix |= bit;
                    }
                    bit <<= 1;
                } while (++x < width);
                pDst[bx] = (unsigned char)pix;
                pDst += dstScan;
                pSrc = (unsigned int *) (((intptr_t)pSrc) + srcScan);
            } while (--rowCount > 0);
        }
    }
    SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);

    xgc = XCreateGC(awt_display, xsdo->bitmask, 0L, NULL);
    XSetForeground(awt_display, xgc, 1);
    XSetBackground(awt_display, xgc, 0);
    XPutImage(awt_display, xsdo->bitmask, xgc,
              image, 0, 0, 0, 0, width, height);

    XFreeGC(awt_display, xgc);
    XDestroyImage(image);

    AWT_UNLOCK();
#endif /* !HEADLESS */
}
