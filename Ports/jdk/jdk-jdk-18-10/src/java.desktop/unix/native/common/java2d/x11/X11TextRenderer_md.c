/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

#include "GlyphImageRef.h"

#ifdef HEADLESS
#include "SurfaceData.h"
#else
#include "X11SurfaceData.h"
#include "GraphicsPrimitiveMgr.h"
#endif /* !HEADLESS */
#include <jlong.h>

#define TEXT_BM_WIDTH   1024
#define TEXT_BM_HEIGHT  32

#ifndef HEADLESS

static jboolean checkPixmap(JNIEnv *env, AwtGraphicsConfigDataPtr cData)
{
    XImage *img;
    int image_size;
    Window root;

    if (cData->monoImage == NULL) {
        img = XCreateImage(awt_display, NULL, 1, XYBitmap, 0, 0,
                                   TEXT_BM_WIDTH, TEXT_BM_HEIGHT, 32, 0);
        if (img != NULL) {
            image_size = img->bytes_per_line * TEXT_BM_HEIGHT;
            // assert(BM_W and BM_H are not large enough to overflow);
            img->data = (char *) malloc(image_size);
            if (img->data == NULL) {
                XFree(img);
            } else {
                // Force same bit/byte ordering
                img->bitmap_bit_order = img->byte_order;
                cData->monoImage = img;
            }
        }
        if (cData->monoImage == NULL) {
            JNU_ThrowOutOfMemoryError(env, "Cannot allocate bitmap for text");
            return JNI_FALSE;
        }
    }
    if (cData->monoPixmap == 0 ||
        cData->monoPixmapGC == NULL ||
        cData->monoPixmapWidth != TEXT_BM_WIDTH ||
        cData->monoPixmapHeight != TEXT_BM_HEIGHT)
    {
        if (cData->monoPixmap != 0) {
            XFreePixmap(awt_display, cData->monoPixmap);
            cData->monoPixmap = 0;
        }
        if (cData->monoPixmapGC != NULL) {
            XFreeGC(awt_display, cData->monoPixmapGC);
            cData->monoPixmapGC = 0;
        }
        root = RootWindow(awt_display, cData->awt_visInfo.screen);
        cData->monoPixmap = XCreatePixmap(awt_display, root,
                                          TEXT_BM_WIDTH, TEXT_BM_HEIGHT, 1);
        if (cData->monoPixmap == 0) {
            JNU_ThrowOutOfMemoryError(env, "Cannot allocate pixmap for text");
            return JNI_FALSE;
        }
        cData->monoPixmapGC = XCreateGC(awt_display, cData->monoPixmap,
                                        0, NULL);
        if (cData->monoPixmapGC == NULL) {
            XFreePixmap(awt_display, cData->monoPixmap);
            cData->monoPixmap = 0;
            JNU_ThrowOutOfMemoryError(env, "Cannot allocate pixmap for text");
            return JNI_FALSE;
        }
        XSetForeground(awt_display, cData->monoPixmapGC, 1);
        XSetBackground(awt_display, cData->monoPixmapGC, 0);
        cData->monoPixmapWidth = TEXT_BM_WIDTH;
        cData->monoPixmapHeight = TEXT_BM_HEIGHT;
    }
    return JNI_TRUE;
}

static void FillBitmap(XImage *theImage,
                       ImageRef *glyphs, jint totalGlyphs,
                       jint clipLeft, jint clipTop,
                       jint clipRight, jint clipBottom)
{
    int glyphCounter;
    int scan = theImage->bytes_per_line;
    int y, left, top, right, bottom, width, height;
    jubyte *pPix;
    const jubyte *pixels;
    unsigned int rowBytes;

    pPix = (jubyte *) theImage->data;
    glyphCounter = ((clipRight - clipLeft) + 7) >> 3;
    for (y = clipTop; y < clipBottom; y++) {
        memset(pPix, 0, glyphCounter);
        pPix += scan;
    }

    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) {
        pixels = (const jubyte *)glyphs[glyphCounter].pixels;
        if (!pixels) {
            continue;
        }
        rowBytes = glyphs[glyphCounter].width;
        left     = glyphs[glyphCounter].x;
        top      = glyphs[glyphCounter].y;
        width    = glyphs[glyphCounter].width;
        height   = glyphs[glyphCounter].height;

        /* if any clipping required, modify parameters now */
        right  = left + width;
        bottom = top + height;
        if (left < clipLeft) {
            pixels += clipLeft - left;
            left = clipLeft;
        }
        if (top < clipTop) {
            pixels += (clipTop - top) * rowBytes;
            top = clipTop;
        }
        if (right > clipRight) {
            right = clipRight;
        }
        if (bottom > clipBottom) {
            bottom = clipBottom;
        }
        if (right <= left || bottom <= top) {
            continue;
        }
        width = right - left;
        height = bottom - top;
        top -= clipTop;
        left -= clipLeft;
        pPix = ((jubyte *) theImage->data) + (left >> 3) + (intptr_t) top * scan;
        left &= 0x07;
        if (theImage->bitmap_bit_order == MSBFirst) {
            left = 0x80 >> left;
            do {
                int x = 0, bx = 0;
                int pix = pPix[0];
                int bit = left;
                do {
                    if (bit == 0) {
                        pPix[bx] = (jubyte) pix;
                        pix = pPix[++bx];
                        bit = 0x80;
                    }
                    if (pixels[x]) {
                        pix |= bit;
                    }
                    bit >>= 1;
                } while (++x < width);
                pPix[bx] = (jubyte) pix;
                pPix += scan;
                pixels += rowBytes;
            } while (--height > 0);
        } else {
            left = 1 << left;
            do {
                int x = 0, bx = 0;
                int pix = pPix[0];
                int bit = left;
                do {
                    if ((bit >> 8) != 0) {
                        pPix[bx] = (jubyte) pix;
                        pix = pPix[++bx];
                        bit = 1;
                    }
                    if (pixels[x]) {
                        pix |= bit;
                    }
                    bit <<= 1;
                } while (++x < width);
                pPix[bx] = (jubyte) pix;
                pPix += scan;
                pixels += rowBytes;
            } while (--height > 0);
        }
    }
}
#endif /* !HEADLESS */

JNIEXPORT void JNICALL
AWTDrawGlyphList(JNIEnv *env, jobject xtr,
                 jlong dstData, jlong gc,
                 SurfaceDataBounds *bounds, ImageRef *glyphs, jint totalGlyphs)
{
#ifndef HEADLESS
    GC xgc, theGC;
    XImage *theImage;
    Pixmap thePixmap;
    XGCValues xgcv;
    int scan, screen;
    AwtGraphicsConfigDataPtr cData;
    X11SDOps *xsdo = (X11SDOps *)jlong_to_ptr(dstData);
    jint cx1, cy1, cx2, cy2;

    if (xsdo == NULL) {
        return;
    }

    xgc = (GC)gc;
    if (xgc == NULL) {
        return;
    }

    screen = xsdo->configData->awt_visInfo.screen;
    cData = getDefaultConfig(screen);
    if (!checkPixmap(env, cData)) {
        return;
    }
    theImage = cData->monoImage;
    thePixmap = cData->monoPixmap;
    theGC = cData->monoPixmapGC;

    scan = theImage->bytes_per_line;

    xgcv.fill_style = FillStippled;
    xgcv.stipple = thePixmap;
    xgcv.ts_x_origin = bounds->x1;
    xgcv.ts_y_origin = bounds->y1;
    XChangeGC(awt_display, xgc,
              GCFillStyle | GCStipple | GCTileStipXOrigin | GCTileStipYOrigin,
              &xgcv);

    cy1 = bounds->y1;
    while (cy1 < bounds->y2) {
        cy2 = cy1 + TEXT_BM_HEIGHT;
        if (cy2 > bounds->y2) cy2 = bounds->y2;

        cx1 = bounds->x1;
        while (cx1 < bounds->x2) {
            cx2 = cx1 + TEXT_BM_WIDTH;
            if (cx2 > bounds->x2) cx2 = bounds->x2;

            FillBitmap(theImage,
                       glyphs,
                       totalGlyphs,
                       cx1, cy1, cx2, cy2);

            // NOTE: Since we are tiling around by BM_W, BM_H offsets
            // and thePixmap is BM_W x BM_H, we do not have to move
            // the TSOrigin at each step since the stipple repeats
            // every BM_W, BM_H units
            XPutImage(awt_display, thePixmap, theGC, theImage,
                      0, 0, 0, 0, cx2 - cx1, cy2 - cy1);
            /* MGA on Linux doesn't pick up the new stipple image data,
             * probably because it caches the image as a hardware pixmap
             * and doesn't update it when the pixmap image data is changed.
             * So if the loop is executed more than once, update the GC
             * which triggers the required behaviour. This extra XChangeGC
             * call only happens on large or rotated text so isn't a
             * significant new overhead..
             * This code needs to execute on a Solaris client too, in case
             * we are remote displaying to a MGA.
             */
            if (cy1 != bounds->y1 || cx1 != bounds->x1) {
                XChangeGC(awt_display, xgc, GCStipple, &xgcv);
            }

            XFillRectangle(awt_display, xsdo->drawable, xgc,
                           cx1, cy1, cx2 - cx1, cy2 - cy1);

            cx1 = cx2;
        }

        cy1 = cy2;
    }
    XSetFillStyle(awt_display, xgc, FillSolid);

    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}
