/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HEADLESS

#include <jni.h>
#include <jlong.h>

#include "SurfaceData.h"
#include "OGLBlitLoops.h"
#include "OGLRenderQueue.h"
#include "OGLSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"

#include <stdlib.h> // malloc
#include <string.h> // memcpy
#include "IntArgbPre.h"

extern OGLPixelFormat PixelFormats[];

/**
 * Inner loop used for copying a source OpenGL "Surface" (window, pbuffer,
 * etc.) to a destination OpenGL "Surface".  Note that the same surface can
 * be used as both the source and destination, as is the case in a copyArea()
 * operation.  This method is invoked from OGLBlitLoops_IsoBlit() as well as
 * OGLBlitLoops_CopyArea().
 *
 * The standard glCopyPixels() mechanism is used to copy the source region
 * into the destination region.  If the regions have different dimensions,
 * the source will be scaled into the destination as appropriate (only
 * nearest neighbor filtering will be applied for simple scale operations).
 */
static void
OGLBlitSurfaceToSurface(OGLContext *oglc, OGLSDOps *srcOps, OGLSDOps *dstOps,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    GLfloat scalex, scaley;
    jint srcw = sx2 - sx1;
    jint srch = sy2 - sy1;

    scalex = ((GLfloat)(dx2-dx1)) / srcw;
    scaley = ((GLfloat)(dy2-dy1)) / srch;

    // the following lines account for the fact that glCopyPixels() copies a
    // region whose lower-left corner is at (x,y), but the source parameters
    // (sx1,sy1) we are given here point to the upper-left corner of the
    // source region... so here we play with the sy1 and dy1 parameters so
    // that they point to the lower-left corners of the regions...
    sx1 = srcOps->xOffset + sx1;
    sy1 = srcOps->yOffset + srcOps->height - sy2;
    dy1 = dy2;

    if (oglc->extraAlpha != 1.0f) {
        OGLContext_SetExtraAlpha(oglc->extraAlpha);
    }

    // see OGLBlitSwToSurface() for more info on the following two lines
    j2d_glRasterPos2i(0, 0);
    j2d_glBitmap(0, 0, 0, 0, (GLfloat)dx1, (GLfloat)-dy1, NULL);

    if (scalex == 1.0f && scaley == 1.0f) {
        j2d_glCopyPixels(sx1, sy1, srcw, srch, GL_COLOR);
    } else {
        j2d_glPixelZoom(scalex, scaley);
        j2d_glCopyPixels(sx1, sy1, srcw, srch, GL_COLOR);
        j2d_glPixelZoom(1.0f, 1.0f);
    }

    if (oglc->extraAlpha != 1.0f) {
        OGLContext_SetExtraAlpha(1.0f);
    }
}

/**
 * Inner loop used for copying a source OpenGL "Texture" to a destination
 * OpenGL "Surface".  This method is invoked from OGLBlitLoops_IsoBlit().
 *
 * This method will copy, scale, or transform the source texture into the
 * destination depending on the transform state, as established in
 * and OGLContext_SetTransform().  If the source texture is
 * transformed in any way when rendered into the destination, the filtering
 * method applied is determined by the hint parameter (can be GL_NEAREST or
 * GL_LINEAR).
 */
static void
OGLBlitTextureToSurface(OGLContext *oglc,
                        OGLSDOps *srcOps, OGLSDOps *dstOps,
                        jboolean rtt, jint hint,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    GLdouble tx1, ty1, tx2, ty2;

    if (rtt) {
        /*
         * The source is a render-to-texture surface.  These surfaces differ
         * from regular texture objects in that the bottom scanline (of
         * the actual image content) coincides with the top edge of the
         * texture object.  Therefore, we need to adjust the sy1/sy2
         * coordinates relative to the top scanline of the image content.
         *
         * In texture coordinates, the top-left corner of the image content
         * would be at:
         *     (0.0, (imgHeight/texHeight))
         * while the bottom-right corner corresponds to:
         *     ((imgWidth/texWidth), 0.0)
         */
        sy1 = srcOps->height - sy1;
        sy2 = srcOps->height - sy2;
    }

    if (srcOps->textureTarget == GL_TEXTURE_RECTANGLE_ARB) {
        // The GL_ARB_texture_rectangle extension requires that we specify
        // texture coordinates in the range [0,srcw] and [0,srch] instead of
        // [0,1] as we would normally do in the case of GL_TEXTURE_2D
        tx1 = (GLdouble)sx1;
        ty1 = (GLdouble)sy1;
        tx2 = (GLdouble)sx2;
        ty2 = (GLdouble)sy2;
    } else {
        // Otherwise we need to convert the source bounds into the range [0,1]
        tx1 = ((GLdouble)sx1) / srcOps->textureWidth;
        ty1 = ((GLdouble)sy1) / srcOps->textureHeight;
        tx2 = ((GLdouble)sx2) / srcOps->textureWidth;
        ty2 = ((GLdouble)sy2) / srcOps->textureHeight;
    }

    // Note that we call CHECK_PREVIOUS_OP(texTarget) in IsoBlit(), which
    // will call glEnable(texTarget) as necessary.
    j2d_glBindTexture(srcOps->textureTarget, srcOps->textureID);
    OGLC_UPDATE_TEXTURE_FUNCTION(oglc, GL_MODULATE);
    OGLSD_UPDATE_TEXTURE_FILTER(srcOps, hint);

    j2d_glBegin(GL_QUADS);
    j2d_glTexCoord2d(tx1, ty1); j2d_glVertex2d(dx1, dy1);
    j2d_glTexCoord2d(tx2, ty1); j2d_glVertex2d(dx2, dy1);
    j2d_glTexCoord2d(tx2, ty2); j2d_glVertex2d(dx2, dy2);
    j2d_glTexCoord2d(tx1, ty2); j2d_glVertex2d(dx1, dy2);
    j2d_glEnd();
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface to a
 * destination OpenGL "Surface".  This method is invoked from
 * OGLBlitLoops_Blit().
 *
 * The standard glDrawPixels() mechanism is used to copy the source region
 * into the destination region.  If the regions have different
 * dimensions, the source will be scaled into the destination
 * as appropriate (only nearest neighbor filtering will be applied for simple
 * scale operations).
 */
static void
OGLBlitSwToSurface(OGLContext *oglc, SurfaceDataRasInfo *srcInfo,
                   OGLPixelFormat *pf,
                   jint sx1, jint sy1, jint sx2, jint sy2,
                   jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    GLfloat scalex, scaley;

    scalex = ((GLfloat)(dx2-dx1)) / (sx2-sx1);
    scaley = ((GLfloat)(dy2-dy1)) / (sy2-sy1);

    if (oglc->extraAlpha != 1.0f) {
        OGLContext_SetExtraAlpha(oglc->extraAlpha);
    }
    if (!pf->hasAlpha) {
        // if the source surface does not have an alpha channel,
        // we need to ensure that the alpha values are forced to
        // the current extra alpha value (see OGLContext_SetExtraAlpha()
        // for more information)
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 0.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, oglc->extraAlpha);
    }

    // This is a rather intriguing (yet totally valid) hack... If we were to
    // specify a raster position that is outside the surface bounds, the raster
    // position would be invalid and nothing would be rendered.  However, we
    // can use a widely known trick to move the raster position outside the
    // surface bounds while maintaining its status as valid.  The following
    // call to glBitmap() renders a no-op bitmap, but offsets the current
    // raster position from (0,0) to the desired location of (dx1,-dy1)...
    j2d_glRasterPos2i(0, 0);
    j2d_glBitmap(0, 0, 0, 0, (GLfloat)dx1, (GLfloat)-dy1, NULL);

    j2d_glPixelZoom(scalex, -scaley);

    GLvoid *pSrc = PtrCoord(srcInfo->rasBase, sx1, srcInfo->pixelStride,
                                              sy1, srcInfo->scanStride);

    // in case pixel stride is not a multiple of scanline stride the copy
    // has to be done line by line (see 6207877)
    if (srcInfo->scanStride % srcInfo->pixelStride != 0) {
        jint width = sx2-sx1;
        jint height = sy2-sy1;
        while (height > 0) {
            j2d_glDrawPixels(width, 1, pf->format, pf->type, pSrc);
            j2d_glBitmap(0, 0, 0, 0, (GLfloat)0, (GLfloat)-scaley, NULL);
            pSrc = PtrAddBytes(pSrc, srcInfo->scanStride);
            height--;
        }
    } else {
        j2d_glDrawPixels(sx2-sx1, sy2-sy1, pf->format, pf->type, pSrc);
    }

    j2d_glPixelZoom(1.0, 1.0);

    if (oglc->extraAlpha != 1.0f) {
        OGLContext_SetExtraAlpha(1.0f);
    }
    if (!pf->hasAlpha) {
        // restore scale/bias to their original values
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 1.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, 0.0f);
    }
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface or
 * OpenGL "Surface" to a destination OpenGL "Surface", using an OpenGL texture
 * tile as an intermediate surface.  This method is invoked from
 * OGLBlitLoops_Blit() for "Sw" surfaces and OGLBlitLoops_IsoBlit() for
 * "Surface" surfaces.
 *
 * This method is used to transform the source surface into the destination.
 * Pixel rectangles cannot be arbitrarily transformed (without the
 * GL_EXT_pixel_transform extension, which is not supported on most modern
 * hardware).  However, texture mapped quads do respect the GL_MODELVIEW
 * transform matrix, so we use textures here to perform the transform
 * operation.  This method uses a tile-based approach in which a small
 * subregion of the source surface is copied into a cached texture tile.  The
 * texture tile is then mapped into the appropriate location in the
 * destination surface.
 *
 * REMIND: this only works well using GL_NEAREST for the filtering mode
 *         (GL_LINEAR causes visible stitching problems between tiles,
 *         but this can be fixed by making use of texture borders)
 */
static void
OGLBlitToSurfaceViaTexture(OGLContext *oglc, SurfaceDataRasInfo *srcInfo,
                           OGLPixelFormat *pf, OGLSDOps *srcOps,
                           jboolean swsurface, jint hint,
                           jint sx1, jint sy1, jint sx2, jint sy2,
                           jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    GLdouble tx1, ty1, tx2, ty2;
    GLdouble dx, dy, dw, dh, cdw, cdh;
    jint tw, th;
    jint sx, sy, sw, sh;
    GLint glhint = (hint == OGLSD_XFORM_BILINEAR) ? GL_LINEAR : GL_NEAREST;
    jboolean adjustAlpha = (pf != NULL && !pf->hasAlpha);
    jboolean slowPath;

    if (oglc->blitTextureID == 0) {
        if (!OGLContext_InitBlitTileTexture(oglc)) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "OGLBlitToSurfaceViaTexture: could not init blit tile");
            return;
        }
    }

    tx1 = 0.0f;
    ty1 = 0.0f;
    tw = OGLC_BLIT_TILE_SIZE;
    th = OGLC_BLIT_TILE_SIZE;
    cdw = (dx2-dx1) / (((GLdouble)(sx2-sx1)) / OGLC_BLIT_TILE_SIZE);
    cdh = (dy2-dy1) / (((GLdouble)(sy2-sy1)) / OGLC_BLIT_TILE_SIZE);

    j2d_glEnable(GL_TEXTURE_2D);
    j2d_glBindTexture(GL_TEXTURE_2D, oglc->blitTextureID);
    OGLC_UPDATE_TEXTURE_FUNCTION(oglc, GL_MODULATE);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, glhint);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, glhint);

    if (adjustAlpha) {
        // if the source surface does not have an alpha channel,
        // we need to ensure that the alpha values are forced to 1.0f
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 0.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, 1.0f);
    }

    // in case pixel stride is not a multiple of scanline stride the copy
    // has to be done line by line (see 6207877)
    slowPath = srcInfo->scanStride % srcInfo->pixelStride != 0;

    for (sy = sy1, dy = dy1; sy < sy2; sy += th, dy += cdh) {
        sh = ((sy + th) > sy2) ? (sy2 - sy) : th;
        dh = ((dy + cdh) > dy2) ? (dy2 - dy) : cdh;

        for (sx = sx1, dx = dx1; sx < sx2; sx += tw, dx += cdw) {
            sw = ((sx + tw) > sx2) ? (sx2 - sx) : tw;
            dw = ((dx + cdw) > dx2) ? (dx2 - dx) : cdw;

            tx2 = ((GLdouble)sw) / tw;
            ty2 = ((GLdouble)sh) / th;

            if (swsurface) {
                GLvoid *pSrc = PtrCoord(srcInfo->rasBase,
                                        sx, srcInfo->pixelStride,
                                        sy, srcInfo->scanStride);
                if (slowPath) {
                    jint tmph = sh;
                    while (tmph > 0) {
                        j2d_glTexSubImage2D(GL_TEXTURE_2D, 0,
                                            0, sh - tmph, sw, 1,
                                            pf->format, pf->type,
                                            pSrc);
                        pSrc = PtrAddBytes(pSrc, srcInfo->scanStride);
                        tmph--;
                    }
                } else {
                    j2d_glTexSubImage2D(GL_TEXTURE_2D, 0,
                                        0, 0, sw, sh,
                                        pf->format, pf->type,
                                        pSrc);
                }

                // the texture image is "right side up", so we align the
                // upper-left texture corner with the upper-left quad corner
                j2d_glBegin(GL_QUADS);
                j2d_glTexCoord2d(tx1, ty1); j2d_glVertex2d(dx, dy);
                j2d_glTexCoord2d(tx2, ty1); j2d_glVertex2d(dx + dw, dy);
                j2d_glTexCoord2d(tx2, ty2); j2d_glVertex2d(dx + dw, dy + dh);
                j2d_glTexCoord2d(tx1, ty2); j2d_glVertex2d(dx, dy + dh);
                j2d_glEnd();
            } else {
                // this accounts for lower-left origin of the source region
                jint newsx = srcOps->xOffset + sx;
                jint newsy = srcOps->yOffset + srcOps->height - (sy + sh);
                j2d_glCopyTexSubImage2D(GL_TEXTURE_2D, 0,
                                        0, 0, newsx, newsy, sw, sh);

                // the texture image is "upside down" after the last step, so
                // we align the bottom-left texture corner with the upper-left
                // quad corner (and vice versa) to effectively flip the
                // texture image
                j2d_glBegin(GL_QUADS);
                j2d_glTexCoord2d(tx1, ty2); j2d_glVertex2d(dx, dy);
                j2d_glTexCoord2d(tx2, ty2); j2d_glVertex2d(dx + dw, dy);
                j2d_glTexCoord2d(tx2, ty1); j2d_glVertex2d(dx + dw, dy + dh);
                j2d_glTexCoord2d(tx1, ty1); j2d_glVertex2d(dx, dy + dh);
                j2d_glEnd();
            }
        }
    }

    if (adjustAlpha) {
        // restore scale/bias to their original values
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 1.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, 0.0f);
    }

    j2d_glDisable(GL_TEXTURE_2D);
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface to a
 * destination OpenGL "Texture".  This method is invoked from
 * OGLBlitLoops_Blit().
 *
 * The source surface is effectively loaded into the OpenGL texture object,
 * which must have already been initialized by OGLSD_initTexture().  Note
 * that this method is only capable of copying the source surface into the
 * destination surface (i.e. no scaling or general transform is allowed).
 * This restriction should not be an issue as this method is only used
 * currently to cache a static system memory image into an OpenGL texture in
 * a hidden-acceleration situation.
 */
static void
OGLBlitSwToTexture(SurfaceDataRasInfo *srcInfo, OGLPixelFormat *pf,
                   OGLSDOps *dstOps,
                   jint dx1, jint dy1, jint dx2, jint dy2)
{
    jboolean adjustAlpha = (pf != NULL && !pf->hasAlpha);
    j2d_glBindTexture(dstOps->textureTarget, dstOps->textureID);

    if (adjustAlpha) {
        // if the source surface does not have an alpha channel,
        // we need to ensure that the alpha values are forced to 1.0f
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 0.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, 1.0f);
    }

    // in case pixel stride is not a multiple of scanline stride the copy
    // has to be done line by line (see 6207877)
    if (srcInfo->scanStride % srcInfo->pixelStride != 0) {
        jint width = dx2 - dx1;
        jint height = dy2 - dy1;
        GLvoid *pSrc = srcInfo->rasBase;

        while (height > 0) {
            j2d_glTexSubImage2D(dstOps->textureTarget, 0,
                                dx1, dy2 - height, width, 1,
                                pf->format, pf->type, pSrc);
            pSrc = PtrAddBytes(pSrc, srcInfo->scanStride);
            height--;
        }
    } else {
        j2d_glTexSubImage2D(dstOps->textureTarget, 0,
                            dx1, dy1, dx2-dx1, dy2-dy1,
                            pf->format, pf->type, srcInfo->rasBase);
    }
    if (adjustAlpha) {
        // restore scale/bias to their original values
        j2d_glPixelTransferf(GL_ALPHA_SCALE, 1.0f);
        j2d_glPixelTransferf(GL_ALPHA_BIAS, 0.0f);
    }
}

/**
 * General blit method for copying a native OpenGL surface (of type "Surface"
 * or "Texture") to another OpenGL "Surface".  If texture is JNI_TRUE, this
 * method will invoke the Texture->Surface inner loop; otherwise, one of the
 * Surface->Surface inner loops will be invoked, depending on the transform
 * state.
 *
 * REMIND: we can trick these blit methods into doing XOR simply by passing
 *         in the (pixel ^ xorpixel) as the pixel value and preceding the
 *         blit with a fillrect...
 */
void
OGLBlitLoops_IsoBlit(JNIEnv *env,
                     OGLContext *oglc, jlong pSrcOps, jlong pDstOps,
                     jboolean xform, jint hint,
                     jboolean texture, jboolean rtt,
                     jint sx1, jint sy1, jint sx2, jint sy2,
                     jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    OGLSDOps *dstOps = (OGLSDOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo;
    jint sw    = sx2 - sx1;
    jint sh    = sy2 - sy1;
    jdouble dw = dx2 - dx1;
    jdouble dh = dy2 - dy1;

    J2dTraceLn(J2D_TRACE_INFO, "OGLBlitLoops_IsoBlit");

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "OGLBlitLoops_IsoBlit: invalid dimensions");
        return;
    }

    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(oglc);

    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;

    SurfaceData_IntersectBoundsXYXY(&srcInfo.bounds,
                                    0, 0, srcOps->width, srcOps->height);

    if (srcInfo.bounds.x2 > srcInfo.bounds.x1 &&
        srcInfo.bounds.y2 > srcInfo.bounds.y1)
    {
        if (srcInfo.bounds.x1 != sx1) {
            dx1 += (srcInfo.bounds.x1 - sx1) * (dw / sw);
            sx1 = srcInfo.bounds.x1;
        }
        if (srcInfo.bounds.y1 != sy1) {
            dy1 += (srcInfo.bounds.y1 - sy1) * (dh / sh);
            sy1 = srcInfo.bounds.y1;
        }
        if (srcInfo.bounds.x2 != sx2) {
            dx2 += (srcInfo.bounds.x2 - sx2) * (dw / sw);
            sx2 = srcInfo.bounds.x2;
        }
        if (srcInfo.bounds.y2 != sy2) {
            dy2 += (srcInfo.bounds.y2 - sy2) * (dh / sh);
            sy2 = srcInfo.bounds.y2;
        }

        J2dTraceLn2(J2D_TRACE_VERBOSE, "  texture=%d hint=%d", texture, hint);
        J2dTraceLn4(J2D_TRACE_VERBOSE, "  sx1=%d sy1=%d sx2=%d sy2=%d",
                    sx1, sy1, sx2, sy2);
        J2dTraceLn4(J2D_TRACE_VERBOSE, "  dx1=%f dy1=%f dx2=%f dy2=%f",
                    dx1, dy1, dx2, dy2);

        if (texture) {
            GLint glhint = (hint == OGLSD_XFORM_BILINEAR) ? GL_LINEAR :
                                                            GL_NEAREST;
            CHECK_PREVIOUS_OP(srcOps->textureTarget);
            OGLBlitTextureToSurface(oglc, srcOps, dstOps, rtt, glhint,
                                    sx1, sy1, sx2, sy2,
                                    dx1, dy1, dx2, dy2);
        } else {
            jboolean viaTexture;
            if (xform) {
                // we must use the via-texture codepath when there is a xform
                viaTexture = JNI_TRUE;
            } else {
                // look at the vendor to see which codepath is faster
                // (this has been empirically determined; see 5020009)
                switch (OGLC_GET_VENDOR(oglc)) {
                case OGLC_VENDOR_NVIDIA:
                    // the via-texture codepath tends to be faster when
                    // there is either a simple scale OR an extra alpha
                    viaTexture =
                        (sx2-sx1) != (jint)(dx2-dx1) ||
                        (sy2-sy1) != (jint)(dy2-dy1) ||
                        oglc->extraAlpha != 1.0f;
                    break;

                case OGLC_VENDOR_ATI:
                    // the via-texture codepath tends to be faster only when
                    // there is an extra alpha involved (scaling or not)
                    viaTexture = (oglc->extraAlpha != 1.0f);
                    break;

                default:
                    // just use the glCopyPixels() codepath
                    viaTexture = JNI_FALSE;
                    break;
                }
            }

            RESET_PREVIOUS_OP();
            if (viaTexture) {
                OGLBlitToSurfaceViaTexture(oglc, &srcInfo, NULL, srcOps,
                                           JNI_FALSE, hint,
                                           sx1, sy1, sx2, sy2,
                                           dx1, dy1, dx2, dy2);
            } else {
                OGLBlitSurfaceToSurface(oglc, srcOps, dstOps,
                                        sx1, sy1, sx2, sy2,
                                        dx1, dy1, dx2, dy2);
            }
        }
    }
}

/**
 * General blit method for copying a system memory ("Sw") surface to a native
 * OpenGL surface (of type "Surface" or "Texture").  If texture is JNI_TRUE,
 * this method will invoke the Sw->Texture inner loop; otherwise, one of the
 * Sw->Surface inner loops will be invoked, depending on the transform state.
 */
void
OGLBlitLoops_Blit(JNIEnv *env,
                  OGLContext *oglc, jlong pSrcOps, jlong pDstOps,
                  jboolean xform, jint hint,
                  jint srctype, jboolean texture,
                  jint sx1, jint sy1, jint sx2, jint sy2,
                  jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    SurfaceDataOps *srcOps = (SurfaceDataOps *)jlong_to_ptr(pSrcOps);
    OGLSDOps *dstOps = (OGLSDOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo;
    OGLPixelFormat pf = PixelFormats[srctype];
    jint sw    = sx2 - sx1;
    jint sh    = sy2 - sy1;
    jdouble dw = dx2 - dx1;
    jdouble dh = dy2 - dy1;

    J2dTraceLn(J2D_TRACE_INFO, "OGLBlitLoops_Blit");

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0 || srctype < 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "OGLBlitLoops_Blit: invalid dimensions or srctype");
        return;
    }

    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(oglc);
    RESET_PREVIOUS_OP();

    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;

    if (srcOps->Lock(env, srcOps, &srcInfo, SD_LOCK_READ) != SD_SUCCESS) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "OGLBlitLoops_Blit: could not acquire lock");
        return;
    }

    if (srcInfo.bounds.x2 > srcInfo.bounds.x1 &&
        srcInfo.bounds.y2 > srcInfo.bounds.y1)
    {
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        if (srcInfo.rasBase) {
            if (srcInfo.bounds.x1 != sx1) {
                dx1 += (srcInfo.bounds.x1 - sx1) * (dw / sw);
                sx1 = srcInfo.bounds.x1;
            }
            if (srcInfo.bounds.y1 != sy1) {
                dy1 += (srcInfo.bounds.y1 - sy1) * (dh / sh);
                sy1 = srcInfo.bounds.y1;
            }
            if (srcInfo.bounds.x2 != sx2) {
                dx2 += (srcInfo.bounds.x2 - sx2) * (dw / sw);
                sx2 = srcInfo.bounds.x2;
            }
            if (srcInfo.bounds.y2 != sy2) {
                dy2 += (srcInfo.bounds.y2 - sy2) * (dh / sh);
                sy2 = srcInfo.bounds.y2;
            }

            J2dTraceLn3(J2D_TRACE_VERBOSE, "  texture=%d srctype=%d hint=%d",
                        texture, srctype, hint);
            J2dTraceLn4(J2D_TRACE_VERBOSE, "  sx1=%d sy1=%d sx2=%d sy2=%d",
                        sx1, sy1, sx2, sy2);
            J2dTraceLn4(J2D_TRACE_VERBOSE, "  dx1=%f dy1=%f dx2=%f dy2=%f",
                        dx1, dy1, dx2, dy2);

            // Note: we will calculate x/y positions in the raster manually
            j2d_glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            j2d_glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
            j2d_glPixelStorei(GL_UNPACK_ROW_LENGTH,
                              srcInfo.scanStride / srcInfo.pixelStride);
            j2d_glPixelStorei(GL_UNPACK_ALIGNMENT, pf.alignment);

            if (texture) {
                // These coordinates will always be integers since we
                // only ever do a straight copy from sw to texture.
                // Thus these casts are "safe" - no loss of precision.
                OGLBlitSwToTexture(&srcInfo, &pf, dstOps,
                                   (jint)dx1, (jint)dy1, (jint)dx2, (jint)dy2);
            } else {
                jboolean viaTexture;
                if (xform) {
                    // we must use the via-texture codepath when there
                    // is a xform
                    viaTexture = JNI_TRUE;
                } else {
                    // look at the vendor to see which codepath is faster
                    // (this has been empirically determined; see 5020009)
                    switch (OGLC_GET_VENDOR(oglc)) {
                    case OGLC_VENDOR_NVIDIA:
                        // the via-texture codepath tends to be faster when
                        // there is either a simple scale OR an extra alpha
                        viaTexture =
                            (sx2-sx1) != (jint)(dx2-dx1) ||
                            (sy2-sy1) != (jint)(dy2-dy1) ||
                            oglc->extraAlpha != 1.0f;
                        break;
#ifdef MACOSX
                    case OGLC_VENDOR_ATI:
                        // see 8024461
                        viaTexture = JNI_TRUE;
                        break;
#endif
                    case OGLC_VENDOR_INTEL:
                        viaTexture = JNI_TRUE;
                        break;
                    default:
                        // just use the glDrawPixels() codepath
                        viaTexture = JNI_FALSE;
                        break;
                    }
                }

                if (viaTexture) {
                    OGLBlitToSurfaceViaTexture(oglc, &srcInfo, &pf, NULL,
                                               JNI_TRUE, hint,
                                               sx1, sy1, sx2, sy2,
                                               dx1, dy1, dx2, dy2);
                } else {
                    OGLBlitSwToSurface(oglc, &srcInfo, &pf,
                                       sx1, sy1, sx2, sy2,
                                       dx1, dy1, dx2, dy2);
                }
            }

            j2d_glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            j2d_glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        }
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
}

/**
 * This method makes vertical flip of the provided area of Surface and convert
 * pixel's data from argbPre to argb format if requested.
 */
void flip(void *pDst, juint w, juint h, jint scanStride, jboolean convert) {
    const size_t clippedStride = 4 * w;
    void *tempRow = (h > 1 && !convert) ? malloc(clippedStride) : NULL;
    juint i = 0;
    juint step = 0;
    // vertical flip and convert argbpre to argb if necessary
    for (; i < h / 2; ++i) {
        juint *r1 = PtrPixelsRow(pDst, i, scanStride);
        juint *r2 = PtrPixelsRow(pDst, h - i - 1, scanStride);
        if (tempRow) {
            // fast path
            memcpy(tempRow, r1, clippedStride);
            memcpy(r1, r2, clippedStride);
            memcpy(r2, tempRow, clippedStride);
        } else {
            // slow path
            for (step = 0; step < w; ++step) {
                juint tmp = r1[step];
                if (convert) {
                    LoadIntArgbPreTo1IntArgb(r2, 0, step, r1[step]);
                    LoadIntArgbPreTo1IntArgb(&tmp, 0, 0, r2[step]);
                } else {
                    r1[step] = r2[step];
                    r2[step] = tmp;
                }
            }
        }
    }
    // convert the middle line if necessary
    if (convert && h % 2) {
        juint *r1 = PtrPixelsRow(pDst, i, scanStride);
        for (step = 0; step < w; ++step) {
            LoadIntArgbPreTo1IntArgb(r1, 0, step, r1[step]);
        }
    }
    if (tempRow) {
        free(tempRow);
    }
}

/**
 * Specialized blit method for copying a native OpenGL "Surface" (pbuffer,
 * window, etc.) to a system memory ("Sw") surface.
 */
void
OGLBlitLoops_SurfaceToSwBlit(JNIEnv *env, OGLContext *oglc,
                             jlong pSrcOps, jlong pDstOps, jint dsttype,
                             jint srcx, jint srcy, jint dstx, jint dsty,
                             jint width, jint height)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    SurfaceDataOps *dstOps = (SurfaceDataOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo, dstInfo;
    OGLPixelFormat pf = PixelFormats[dsttype];

    J2dTraceLn(J2D_TRACE_INFO, "OGLBlitLoops_SurfaceToSwBlit");

    if (width <= 0 || height <= 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
            "OGLBlitLoops_SurfaceToSwBlit: dimensions are non-positive");
        return;
    }

    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(oglc);
    RESET_PREVIOUS_OP();

    srcInfo.bounds.x1 = srcx;
    srcInfo.bounds.y1 = srcy;
    srcInfo.bounds.x2 = srcx + width;
    srcInfo.bounds.y2 = srcy + height;
    dstInfo.bounds.x1 = dstx;
    dstInfo.bounds.y1 = dsty;
    dstInfo.bounds.x2 = dstx + width;
    dstInfo.bounds.y2 = dsty + height;

    if (dstOps->Lock(env, dstOps, &dstInfo, SD_LOCK_WRITE) != SD_SUCCESS) {
        J2dTraceLn(J2D_TRACE_WARNING,
            "OGLBlitLoops_SurfaceToSwBlit: could not acquire dst lock");
        return;
    }

    SurfaceData_IntersectBoundsXYXY(&srcInfo.bounds,
                                    0, 0, srcOps->width, srcOps->height);
    SurfaceData_IntersectBlitBounds(&dstInfo.bounds, &srcInfo.bounds,
                                    srcx - dstx, srcy - dsty);

    if (srcInfo.bounds.x2 > srcInfo.bounds.x1 &&
        srcInfo.bounds.y2 > srcInfo.bounds.y1)
    {
        dstOps->GetRasInfo(env, dstOps, &dstInfo);
        if (dstInfo.rasBase) {
            void *pDst = dstInfo.rasBase;

            srcx = srcInfo.bounds.x1;
            srcy = srcInfo.bounds.y1;
            dstx = dstInfo.bounds.x1;
            dsty = dstInfo.bounds.y1;
            width = srcInfo.bounds.x2 - srcInfo.bounds.x1;
            height = srcInfo.bounds.y2 - srcInfo.bounds.y1;

            pDst = PtrAddBytes(pDst, dstx * dstInfo.pixelStride);
            pDst = PtrPixelsRow(pDst, dsty, dstInfo.scanStride);

            j2d_glPixelStorei(GL_PACK_ROW_LENGTH,
                              dstInfo.scanStride / dstInfo.pixelStride);
            j2d_glPixelStorei(GL_PACK_ALIGNMENT, pf.alignment);
#ifdef MACOSX
            if (srcOps->isOpaque) {
                // For some reason Apple's OpenGL implementation will
                // read back zero values from the alpha channel of an
                // opaque surface when using glReadPixels(), so here we
                // force the resulting pixels to be fully opaque.
                j2d_glPixelTransferf(GL_ALPHA_BIAS, 1.0);
            }
#endif

            J2dTraceLn4(J2D_TRACE_VERBOSE, "  sx=%d sy=%d w=%d h=%d",
                        srcx, srcy, width, height);
            J2dTraceLn2(J2D_TRACE_VERBOSE, "  dx=%d dy=%d",
                        dstx, dsty);

            // this accounts for lower-left origin of the source region
            srcx = srcOps->xOffset + srcx;
            srcy = srcOps->yOffset + srcOps->height - srcy - height;

            // Note that glReadPixels() is extremely slow!
            // So we call it only once and flip the image using memcpy.
            j2d_glReadPixels(srcx, srcy, width, height,
                             pf.format, pf.type, pDst);
            // It was checked above that width and height are positive.
            flip(pDst, (juint) width, (juint) height, dstInfo.scanStride,
                 !pf.isPremult && !srcOps->isOpaque);
#ifdef MACOSX
            if (srcOps->isOpaque) {
                j2d_glPixelTransferf(GL_ALPHA_BIAS, 0.0);
            }
#endif
            j2d_glPixelStorei(GL_PACK_ROW_LENGTH, 0);
            j2d_glPixelStorei(GL_PACK_ALIGNMENT, 4);
        }
        SurfaceData_InvokeRelease(env, dstOps, &dstInfo);
    }
    SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
}

void
OGLBlitLoops_CopyArea(JNIEnv *env,
                      OGLContext *oglc, OGLSDOps *dstOps,
                      jint x, jint y, jint width, jint height,
                      jint dx, jint dy)
{
    SurfaceDataBounds srcBounds, dstBounds;

    J2dTraceLn(J2D_TRACE_INFO, "OGLBlitLoops_CopyArea");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(dstOps);
    RESET_PREVIOUS_OP();

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  x=%d y=%d w=%d h=%d",
                x, y, width, height);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  dx=%d dy=%d",
                dx, dy);

    srcBounds.x1 = x;
    srcBounds.y1 = y;
    srcBounds.x2 = srcBounds.x1 + width;
    srcBounds.y2 = srcBounds.y1 + height;
    dstBounds.x1 = x + dx;
    dstBounds.y1 = y + dy;
    dstBounds.x2 = dstBounds.x1 + width;
    dstBounds.y2 = dstBounds.y1 + height;

    // 6430601: manually clip src/dst parameters to work around
    // some bugs in Sun's and Apple's OpenGL implementations
    // (it's a good idea to restrict the source parameters anyway, since
    // passing out of range parameters to glCopyPixels() will result in
    // an OpenGL error)
    SurfaceData_IntersectBoundsXYXY(&srcBounds,
                                    0, 0, dstOps->width, dstOps->height);
    SurfaceData_IntersectBoundsXYXY(&dstBounds,
                                    0, 0, dstOps->width, dstOps->height);
    SurfaceData_IntersectBlitBounds(&dstBounds, &srcBounds, -dx, -dy);

    if (dstBounds.x1 < dstBounds.x2 && dstBounds.y1 < dstBounds.y2) {
#ifdef MACOSX
        if (dstOps->isOpaque) {
            // For some reason Apple's OpenGL implementation will fail
            // to render glCopyPixels() when the src/dst rectangles are
            // overlapping and glColorMask() has disabled writes to the
            // alpha channel.  The workaround is to temporarily re-enable
            // the alpha channel during the glCopyPixels() operation.
            j2d_glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        }
#endif

        OGLBlitSurfaceToSurface(oglc, dstOps, dstOps,
                                srcBounds.x1, srcBounds.y1,
                                srcBounds.x2, srcBounds.y2,
                                dstBounds.x1, dstBounds.y1,
                                dstBounds.x2, dstBounds.y2);
#ifdef MACOSX
        if (dstOps->isOpaque) {
            j2d_glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_FALSE);
        }
#endif
    }
}

#endif /* !HEADLESS */
