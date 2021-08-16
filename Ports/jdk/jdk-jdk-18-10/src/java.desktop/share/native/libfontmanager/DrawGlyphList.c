/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include "jlong.h"
#include "math.h"
#include "string.h"
#include "stdlib.h"
#include "sunfontids.h"
#include "fontscalerdefs.h"
#include "glyphblitting.h"
#include "GraphicsPrimitiveMgr.h"
#include "sun_java2d_loops_DrawGlyphList.h"
#include "sun_java2d_loops_DrawGlyphListAA.h"


/*
 * Need to account for the rare case when (eg) repainting damaged
 * areas results in the drawing location being negative, in which
 * case (int) rounding always goes towards zero. We need to always
 * round down instead, so that we paint at the correct position.
 * We only call "floor" when value is < 0 (ie rarely).
 * Storing the result of (eg) (x+ginfo->topLeftX) benchmarks is more
 * expensive than repeating the calculation as we do here.
 * "floor" shows up as a significant cost in app-level microbenchmarks.
 * This macro avoids calling it on positive values, instead using an
 * (int) cast.
 */
#define FLOOR_ASSIGN(l, r)\
 if ((r)<0) (l) = ((int)floor(r)); else (l) = ((int)(r))

GlyphBlitVector* setupBlitVector(JNIEnv *env, jobject glyphlist,
                                 jint fromGlyph, jint toGlyph) {

    int g;
    size_t bytesNeeded;
    jlong *imagePtrs;
    jfloat* positions = NULL;
    GlyphInfo *ginfo;
    GlyphBlitVector *gbv;

    jfloat x = (*env)->GetFloatField(env, glyphlist, sunFontIDs.glyphListX);
    jfloat y = (*env)->GetFloatField(env, glyphlist, sunFontIDs.glyphListY);
    jint len =  toGlyph - fromGlyph;
    jlongArray glyphImages = (jlongArray)
        (*env)->GetObjectField(env, glyphlist, sunFontIDs.glyphImages);
    jfloatArray glyphPositions =
      (*env)->GetBooleanField(env, glyphlist, sunFontIDs.glyphListUsePos)
        ? (jfloatArray)
      (*env)->GetObjectField(env, glyphlist, sunFontIDs.glyphListPos)
        : NULL;

    bytesNeeded = sizeof(GlyphBlitVector)+sizeof(ImageRef)*len;
    gbv = (GlyphBlitVector*)malloc(bytesNeeded);
    if (gbv == NULL) {
        return NULL;
    }
    gbv->numGlyphs = len;
    gbv->glyphs = (ImageRef*)((unsigned char*)gbv+sizeof(GlyphBlitVector));

    imagePtrs = (*env)->GetPrimitiveArrayCritical(env, glyphImages, NULL);
    if (imagePtrs == NULL) {
        free(gbv);
        return (GlyphBlitVector*)NULL;
    }

    if (glyphPositions) {
        int n = fromGlyph * 2 - 1;

        positions =
          (*env)->GetPrimitiveArrayCritical(env, glyphPositions, NULL);
        if (positions == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, glyphImages,
                                                  imagePtrs, JNI_ABORT);
            free(gbv);
            return (GlyphBlitVector*)NULL;
        }

        for (g=0; g<len; g++) {
            jfloat px = x + positions[++n];
            jfloat py = y + positions[++n];

            ginfo = (GlyphInfo*)((uintptr_t)imagePtrs[g + fromGlyph]);
            gbv->glyphs[g].glyphInfo = ginfo;
            gbv->glyphs[g].pixels = ginfo->image;
            gbv->glyphs[g].width = ginfo->width;
            gbv->glyphs[g].rowBytes = ginfo->rowBytes;
            gbv->glyphs[g].height = ginfo->height;
            FLOOR_ASSIGN(gbv->glyphs[g].x, px + ginfo->topLeftX);
            FLOOR_ASSIGN(gbv->glyphs[g].y, py + ginfo->topLeftY);
        }
        (*env)->ReleasePrimitiveArrayCritical(env,glyphPositions,
                                              positions, JNI_ABORT);
    } else {
        for (g=0; g<len; g++) {
            ginfo = (GlyphInfo*)((uintptr_t)imagePtrs[g + fromGlyph]);
            gbv->glyphs[g].glyphInfo = ginfo;
            gbv->glyphs[g].pixels = ginfo->image;
            gbv->glyphs[g].width = ginfo->width;
            gbv->glyphs[g].rowBytes = ginfo->rowBytes;
            gbv->glyphs[g].height = ginfo->height;
            FLOOR_ASSIGN(gbv->glyphs[g].x, x + ginfo->topLeftX);
            FLOOR_ASSIGN(gbv->glyphs[g].y, y + ginfo->topLeftY);

            /* copy image data into this array at x/y locations */
            x += ginfo->advanceX;
            y += ginfo->advanceY;
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, glyphImages, imagePtrs,
                                          JNI_ABORT);

    if (!glyphPositions) {
        (*env)->SetFloatField(env, glyphlist, sunFontIDs.glyphListX, x);
        (*env)->SetFloatField(env, glyphlist, sunFontIDs.glyphListY, y);
    }

    return gbv;
}

jint RefineBounds(GlyphBlitVector *gbv, SurfaceDataBounds *bounds) {
    int index;
    jint dx1, dy1, dx2, dy2;
    ImageRef glyphImage;
    int num = gbv->numGlyphs;
    SurfaceDataBounds glyphs;

    glyphs.x1 = glyphs.y1 = 0x7fffffff;
    glyphs.x2 = glyphs.y2 = 0x80000000;
    for (index = 0; index < num; index++) {
        glyphImage = gbv->glyphs[index];
        dx1 = (jint) glyphImage.x;
        dy1 = (jint) glyphImage.y;
        dx2 = dx1 + glyphImage.width;
        dy2 = dy1 + glyphImage.height;
        if (glyphs.x1 > dx1) glyphs.x1 = dx1;
        if (glyphs.y1 > dy1) glyphs.y1 = dy1;
        if (glyphs.x2 < dx2) glyphs.x2 = dx2;
        if (glyphs.y2 < dy2) glyphs.y2 = dy2;
    }

    SurfaceData_IntersectBounds(bounds, &glyphs);
    return (bounds->x1 < bounds->x2 && bounds->y1 < bounds->y2);
}




/* since the AA and non-AA loop functions share a common method
 * signature, can call both through this common function since
 * there's no difference except for the inner loop.
 * This could be a macro but there's enough of those already.
 */
static void drawGlyphList(JNIEnv *env, jobject self,
                          jobject sg2d, jobject sData,
                          GlyphBlitVector *gbv, jint pixel, jint color,
                          NativePrimitive *pPrim, DrawGlyphListFunc *func) {

    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    CompositeInfo compInfo;
    int clipLeft, clipRight, clipTop, clipBottom;
    int ret;

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
        return;
    }

    if (pPrim->pCompType->getCompInfo != NULL) {
        GrPrim_Sg2dGetCompInfo(env, sg2d, pPrim, &compInfo);
    }

    GrPrim_Sg2dGetClip(env, sg2d, &rasInfo.bounds);
    if (rasInfo.bounds.y2 <= rasInfo.bounds.y1 ||
        rasInfo.bounds.x2 <= rasInfo.bounds.x1)
    {
        return;
    }

    ret = sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags);
    if (ret != SD_SUCCESS) {
        if (ret == SD_SLOWLOCK) {
            if (!RefineBounds(gbv, &rasInfo.bounds)) {
                SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
                return;
            }
        } else {
            return;
        }
    }

    sdOps->GetRasInfo(env, sdOps, &rasInfo);
    if (!rasInfo.rasBase) {
        SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
        return;
    }
    clipLeft    = rasInfo.bounds.x1;
    clipRight   = rasInfo.bounds.x2;
    clipTop     = rasInfo.bounds.y1;
    clipBottom  = rasInfo.bounds.y2;
    if (clipRight > clipLeft && clipBottom > clipTop) {

        (*func)(&rasInfo,
                gbv->glyphs, gbv->numGlyphs,
                pixel, color,
                clipLeft, clipTop,
                clipRight, clipBottom,
                pPrim, &compInfo);
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);

    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}

static unsigned char* getLCDGammaLUT(int gamma);
static unsigned char* getInvLCDGammaLUT(int gamma);

static void drawGlyphListLCD(JNIEnv *env, jobject self,
                          jobject sg2d, jobject sData,
                          GlyphBlitVector *gbv, jint pixel, jint color,
                          jboolean rgbOrder, int contrast,
                          NativePrimitive *pPrim,
                          DrawGlyphListLCDFunc *func) {

    SurfaceDataOps *sdOps;
    SurfaceDataRasInfo rasInfo;
    CompositeInfo compInfo;
    int clipLeft, clipRight, clipTop, clipBottom;
    int ret;

    sdOps = SurfaceData_GetOps(env, sData);
    if (sdOps == 0) {
        return;
    }

    if (pPrim->pCompType->getCompInfo != NULL) {
        GrPrim_Sg2dGetCompInfo(env, sg2d, pPrim, &compInfo);
    }

    GrPrim_Sg2dGetClip(env, sg2d, &rasInfo.bounds);
    if (rasInfo.bounds.y2 <= rasInfo.bounds.y1 ||
        rasInfo.bounds.x2 <= rasInfo.bounds.x1)
    {
        return;
    }

    ret = sdOps->Lock(env, sdOps, &rasInfo, pPrim->dstflags);
    if (ret != SD_SUCCESS) {
        if (ret == SD_SLOWLOCK) {
            if (!RefineBounds(gbv, &rasInfo.bounds)) {
                SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
                return;
            }
        } else {
            return;
        }
    }

    sdOps->GetRasInfo(env, sdOps, &rasInfo);
    if (!rasInfo.rasBase) {
        SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
        return;
    }
    clipLeft    = rasInfo.bounds.x1;
    clipRight   = rasInfo.bounds.x2;
    clipTop     = rasInfo.bounds.y1;
    clipBottom  = rasInfo.bounds.y2;

    if (clipRight > clipLeft && clipBottom > clipTop) {

        (*func)(&rasInfo,
                gbv->glyphs, gbv->numGlyphs,
                pixel, color,
                clipLeft, clipTop,
                clipRight, clipBottom, (jint)rgbOrder,
                getLCDGammaLUT(contrast), getInvLCDGammaLUT(contrast),
                pPrim, &compInfo);
        SurfaceData_InvokeRelease(env, sdOps, &rasInfo);

    }
    SurfaceData_InvokeUnlock(env, sdOps, &rasInfo);
}

/*
 * Class:     sun_java2d_loops_DrawGlyphList
 * Method:    DrawGlyphList
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Lsun/java2d/font/GlyphList;II)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawGlyphList_DrawGlyphList
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject glyphlist,
     jint fromGlyph, jint toGlyph) {

    jint pixel, color;
    GlyphBlitVector* gbv;
    NativePrimitive *pPrim;

    if ((pPrim = GetNativePrim(env, self)) == NULL) {
        return;
    }

    if ((gbv = setupBlitVector(env, glyphlist, fromGlyph, toGlyph)) == NULL) {
        return;
    }

    pixel = GrPrim_Sg2dGetPixel(env, sg2d);
    color = GrPrim_Sg2dGetEaRGB(env, sg2d);
    drawGlyphList(env, self, sg2d, sData, gbv, pixel, color,
                  pPrim, pPrim->funcs.drawglyphlist);
    free(gbv);

}

/*
 * Class:     sun_java2d_loops_DrawGlyphListAA
 * Method:    DrawGlyphListAA
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Lsun/java2d/font/GlyphList;II)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawGlyphListAA_DrawGlyphListAA
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject glyphlist,
     jint fromGlyph, jint toGlyph) {

    jint pixel, color;
    GlyphBlitVector* gbv;
    NativePrimitive *pPrim;

    if ((pPrim = GetNativePrim(env, self)) == NULL) {
        return;
    }

    if ((gbv = setupBlitVector(env, glyphlist, fromGlyph, toGlyph)) == NULL) {
        return;
    }
    pixel = GrPrim_Sg2dGetPixel(env, sg2d);
    color = GrPrim_Sg2dGetEaRGB(env, sg2d);
    drawGlyphList(env, self, sg2d, sData, gbv, pixel, color,
                  pPrim, pPrim->funcs.drawglyphlistaa);
    free(gbv);
}

/*
 * Class:     sun_java2d_loops_DrawGlyphListLCD
 * Method:    DrawGlyphListLCD
 * Signature: (Lsun/java2d/SunGraphics2D;Lsun/java2d/SurfaceData;Lsun/java2d/font/GlyphList;II)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_loops_DrawGlyphListLCD_DrawGlyphListLCD
    (JNIEnv *env, jobject self,
     jobject sg2d, jobject sData, jobject glyphlist,
     jint fromGlyph, jint toGlyph) {

    jint pixel, color, contrast;
    jboolean rgbOrder;
    GlyphBlitVector* gbv;
    NativePrimitive *pPrim;

    if ((pPrim = GetNativePrim(env, self)) == NULL) {
        return;
    }

    if ((gbv = setupLCDBlitVector(env, glyphlist, fromGlyph, toGlyph))
            == NULL) {
        return;
    }
    pixel = GrPrim_Sg2dGetPixel(env, sg2d);
    color = GrPrim_Sg2dGetEaRGB(env, sg2d);
    contrast = GrPrim_Sg2dGetLCDTextContrast(env, sg2d);
    rgbOrder = (*env)->GetBooleanField(env,glyphlist, sunFontIDs.lcdRGBOrder);
    drawGlyphListLCD(env, self, sg2d, sData, gbv, pixel, color,
                     rgbOrder, contrast,
                     pPrim, pPrim->funcs.drawglyphlistlcd);
    free(gbv);
}

/*
 *  LCD text utilises a filter which spreads energy to adjacent subpixels.
 *  So we add 3 bytes (one whole pixel) of padding at the start of every row
 *  to hold energy from the very leftmost sub-pixel.
 *  This is to the left of the intended glyph image position so LCD text also
 *  adjusts the top-left X position of the padded image one pixel to the left
 *  so a glyph image is drawn in the same place it would be if the padding
 *  were not present.
 *
 *  So in the glyph cache for LCD text the first two bytes of every row are
 *  zero.
 *  We make use of this to be able to adjust the rendering position of the
 *  text when the client specifies a fractional metrics sub-pixel positioning
 *  rendering hint.
 *
 *  So the first 6 bytes in a cache row looks like :
 *  00 00 Ex G0 G1 G2
 *
 *  where
 *  00 are the always zero bytes
 *  Ex is extra energy spread from the glyph into the left padding pixel.
 *  Gn are the RGB component bytes of the first pixel of the glyph image
 *  For an RGB display G0 is the red component, etc.
 *
 *  If a glyph is drawn at X=12 then the G0 G1 G2 pixel is placed at that
 *  position : ie G0 is drawn in the first sub-pixel at X=12
 *
 *  Draw at X=12,0
 *  PIXEL POS 11 11 11 12 12 12 13 13 13
 *  SUBPX POS  0  1  2  0  1  2  0  1  2
 *            00 00 Ex G0 G1 G2
 *
 *  If a sub-pixel rounded glyph position is calculated as being X=12.33 -
 *  ie 12 and one-third pixels, we want the result to look like this :
 *  Draw at X=12,1
 *  PIXEL POS 11 11 11 12 12 12 13 13 13
 *  SUBPX POS  0  1  2  0  1  2  0  1  2
 *               00 00 Ex G0 G1 G2
 *
 *  ie the G0 byte is moved one sub-pixel to the right.
 *  To do this we need to make two adjustments :
 *  - set X=X+1
 *  - set start of scan row to start+2, ie index past the two zero bytes
 *  ie we don't need the 00 00 bytes at all any more. Rendering start X
 *  can skip over those.
 *
 *  Lets look at the final case :
 *  If a sub-pixel rounded glyph position is calculated as being X=12.67 -
 *  ie 12 and two-third pixels, we want the result to look like this :
 *  Draw at X=12,2
 *  PIXEL POS 11 11 11 12 12 12 13 13 13
 *  SUBPX POS  0  1  2  0  1  2  0  1  2
 *                  00 00 Ex G0 G1 G2
 *
 *  ie the G0 byte is moved two sub-pixels to the right, so that the image
 *  starts at 12.67
 *  To do this we need to make these two adjustments :
 *  - set X=X+1
 *  - set start of scan row to start+1, ie index past the first zero byte
 *  In this case the second of the 00 bytes is used as a no-op on the first
 *   red sub-pixel position.
 *
 *  The final adjustment needed to make all this work is note that if
 *  we moved the start of row one or two bytes in we will go one or two bytes
 *  past the end of the row. So the glyph cache needs to have 2 bytes of
 *  zero padding at the end of each row. This is the extra memory cost to
 *  accommodate this algorithm.
 *
 *  The resulting text is perhaps fractionally better in overall perception
 *  than rounding to the whole pixel grid, as a few issues arise.
 *
 *  * the improvement in inter-glyph spacing as well as being limited
 *  to 1/3 pixel resolution, is also limited because the glyphs were hinted
 *  so they fit to the whole pixel grid. It may be worthwhile to pursue
 *  disabling x-axis gridfitting.
 *
 *  * an LCD display may have gaps between the pixels that are greater
 *  than the subpixels. Thus for thin stemmed fonts, if the shift causes
 *  the "heart" of a stem to span whole pixels it may appear more diffuse -
 *  less sharp. Eliminating hinting would probably not make this worse - in
 *  effect we have already doing that here. But it would improve the spacing.
 *
 *  * perhaps contradicting the above point in some ways, more diffuse glyphs
 *  are better at reducing colour fringing, but what appears to be more
 *  colour fringing in this FM case is more likely attributable to a greater
 *  likelihood for glyphs to abutt. In integer metrics or even whole pixel
 *  rendered fractional metrics, there's typically more space between the
 *  glyphs. Perhaps disabling X-axis grid-fitting will help with that.
 */
GlyphBlitVector* setupLCDBlitVector(JNIEnv *env, jobject glyphlist,
                                    jint fromGlyph, jint toGlyph) {

    int g;
    size_t bytesNeeded;
    jlong *imagePtrs;
    jfloat* positions = NULL;
    GlyphInfo *ginfo;
    GlyphBlitVector *gbv;

    jfloat x = (*env)->GetFloatField(env, glyphlist, sunFontIDs.glyphListX);
    jfloat y = (*env)->GetFloatField(env, glyphlist, sunFontIDs.glyphListY);
    jint len =  toGlyph - fromGlyph;
    jlongArray glyphImages = (jlongArray)
        (*env)->GetObjectField(env, glyphlist, sunFontIDs.glyphImages);
    jfloatArray glyphPositions =
      (*env)->GetBooleanField(env, glyphlist, sunFontIDs.glyphListUsePos)
        ? (jfloatArray)
      (*env)->GetObjectField(env, glyphlist, sunFontIDs.glyphListPos)
        : NULL;
    jboolean subPixPos =
      (*env)->GetBooleanField(env,glyphlist, sunFontIDs.lcdSubPixPos);

    bytesNeeded = sizeof(GlyphBlitVector)+sizeof(ImageRef)*len;
    gbv = (GlyphBlitVector*)malloc(bytesNeeded);
    if (gbv == NULL) {
        return NULL;
    }
    gbv->numGlyphs = len;
    gbv->glyphs = (ImageRef*)((unsigned char*)gbv+sizeof(GlyphBlitVector));

    imagePtrs = (*env)->GetPrimitiveArrayCritical(env, glyphImages, NULL);
    if (imagePtrs == NULL) {
        free(gbv);
        return (GlyphBlitVector*)NULL;
    }

    /* The position of the start of the text is adjusted up so
     * that we can round it to an integral pixel position for a
     * bitmap glyph or non-subpixel positioning, and round it to an
     * integral subpixel position for that case, hence 0.5/3 = 0.166667
     * Presently subPixPos means FM, and FM disables embedded bitmaps
     * Therefore if subPixPos is true we should never get embedded bitmaps
     * and the glyphlist will be homogenous. This test and the position
     * adjustments will need to be per glyph once this case becomes
     * heterogenous.
     * Also set subPixPos=false if detect a B&W bitmap as we only
     * need to test that on a per glyph basis once the list becomes
     * heterogenous
     */
    if (subPixPos && len > 0) {
        ginfo = (GlyphInfo*)((uintptr_t)imagePtrs[fromGlyph]);
        if (ginfo == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, glyphImages,
                                                  imagePtrs, JNI_ABORT);
            free(gbv);
            return (GlyphBlitVector*)NULL;
        }
        /* rowBytes==width tests if its a B&W or LCD glyph */
        if (ginfo->width == ginfo->rowBytes) {
            subPixPos = JNI_FALSE;
        }
    }

     if (glyphPositions) {
        int n = fromGlyph * 2 - 1;

        positions =
          (*env)->GetPrimitiveArrayCritical(env, glyphPositions, NULL);
        if (positions == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, glyphImages,
                                                  imagePtrs, JNI_ABORT);
            free(gbv);
            return (GlyphBlitVector*)NULL;
        }

        for (g=0; g<len; g++) {
            jfloat px, py;

            ginfo = (GlyphInfo*)((uintptr_t)imagePtrs[g + fromGlyph]);
            if (ginfo == NULL) {
                (*env)->ReleasePrimitiveArrayCritical(env, glyphImages,
                                                  imagePtrs, JNI_ABORT);
                free(gbv);
                return (GlyphBlitVector*)NULL;
            }
            gbv->glyphs[g].glyphInfo = ginfo;
            gbv->glyphs[g].pixels = ginfo->image;
            gbv->glyphs[g].width = ginfo->width;
            gbv->glyphs[g].rowBytes = ginfo->rowBytes;
            gbv->glyphs[g].height = ginfo->height;

            px = x + positions[++n];
            py = y + positions[++n];

            /*
             * Subpixel positioning may be requested for LCD text.
             *
             * Subpixel positioning can take place only in the direction in
             * which the subpixels increase the resolution.
             * So this is useful for the typical case of vertical stripes
             * increasing the resolution in the direction of the glyph
             * advances - ie typical horizontally laid out text.
             * If the subpixel stripes are horizontal, subpixel positioning
             * can take place only in the vertical direction, which isn't
             * as useful - you would have to be drawing rotated text on
             * a display which actually had that organisation. A pretty
             * unlikely combination.
             * So this is supported only for vertical stripes which
             * increase the horizontal resolution.
             * If in this case the client also rotates the text then there
             * will still be some benefit for small rotations. For 90 degree
             * rotation there's no horizontal advance and less benefit
             * from the subpixel rendering too.
             * The test for width==rowBytes detects the case where the glyph
             * is a B&W image obtained from an embedded bitmap. In that
             * case we cannot apply sub-pixel positioning so ignore it.
             * This is handled on a per glyph basis.
             */
            if (subPixPos) {
                int frac;
                float pos;

                px += 0.1666667f - 0.5f;
                py += 0.1666667f - 0.5f;

                pos = px + ginfo->topLeftX;
                FLOOR_ASSIGN(gbv->glyphs[g].x, pos);
                /* Calculate the fractional pixel position - ie the subpixel
                 * position within the RGB/BGR triple. We are rounding to
                 * the nearest, even though we just do (int) since at the
                 * start of the loop the position was already adjusted by
                 * 0.5 (sub)pixels to get rounding.
                 * Thus the "fractional" position will be 0, 1 or 2.
                 * eg 0->0.32 is 0, 0.33->0.66 is 1, > 0.66->0.99 is 2.
                 * We can use an (int) cast here since the floor operation
                 * above guarantees us that the value is positive.
                 */
                frac = (int)((pos - gbv->glyphs[g].x)*3);
                if (frac == 0) {
                    /* frac rounded down to zero, so this is equivalent
                     * to no sub-pixel positioning.
                     */
                    gbv->glyphs[g].rowBytesOffset = 0;
                } else {
                    /* In this case we need to adjust both the position at
                     * which the glyph will be positioned by one pixel to the
                     * left and adjust the position in the glyph image row
                     * from which to extract the data
                     * Every glyph image row has 2 bytes padding
                     * on the right to account for this.
                     */
                    gbv->glyphs[g].rowBytesOffset = 3-frac;
                    gbv->glyphs[g].x += 1;
                }
            } else {
                FLOOR_ASSIGN(gbv->glyphs[g].x, px + ginfo->topLeftX);
                gbv->glyphs[g].rowBytesOffset = 0;
            }
            FLOOR_ASSIGN(gbv->glyphs[g].y, py + ginfo->topLeftY);
        }
        (*env)->ReleasePrimitiveArrayCritical(env,glyphPositions,
                                              positions, JNI_ABORT);
    } else {
        for (g=0; g<len; g++) {
            jfloat px = x;
            jfloat py = y;
            ginfo = (GlyphInfo*)((uintptr_t)imagePtrs[g + fromGlyph]);
            if (ginfo == NULL) {
                (*env)->ReleasePrimitiveArrayCritical(env, glyphImages,
                                                  imagePtrs, JNI_ABORT);
                free(gbv);
                return (GlyphBlitVector*)NULL;
            }
            gbv->glyphs[g].glyphInfo = ginfo;
            gbv->glyphs[g].pixels = ginfo->image;
            gbv->glyphs[g].width = ginfo->width;
            gbv->glyphs[g].rowBytes = ginfo->rowBytes;
            gbv->glyphs[g].height = ginfo->height;

            if (subPixPos) {
                int frac;
                float pos;

                px += 0.1666667f - 0.5f;
                py += 0.1666667f - 0.5f;

                pos = px + ginfo->topLeftX;
                FLOOR_ASSIGN(gbv->glyphs[g].x, pos);
                frac = (int)((pos - gbv->glyphs[g].x)*3);
                if (frac == 0) {
                    gbv->glyphs[g].rowBytesOffset = 0;
                } else {
                    gbv->glyphs[g].rowBytesOffset = 3-frac;
                    gbv->glyphs[g].x += 1;
                }
            } else {
                FLOOR_ASSIGN(gbv->glyphs[g].x, px + ginfo->topLeftX);
                gbv->glyphs[g].rowBytesOffset = 0;
            }
            FLOOR_ASSIGN(gbv->glyphs[g].y, py + ginfo->topLeftY);

            /* copy image data into this array at x/y locations */
            x += ginfo->advanceX;
            y += ginfo->advanceY;
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, glyphImages, imagePtrs,
                                          JNI_ABORT);
    if (!glyphPositions) {
        (*env)->SetFloatField(env, glyphlist, sunFontIDs.glyphListX, x);
        (*env)->SetFloatField(env, glyphlist, sunFontIDs.glyphListY, y);
    }

    return gbv;
}

/* LCD text needs to go through a gamma (contrast) adjustment.
 * Gamma is constrained to the range 1.0->2.2 with a quantization of
 * 0.01 (more than good enough). Representing as an integer with that
 * precision yields a range 100->250 thus we need to store up to 151 LUTs
 * and inverse LUTs.
 * We allocate the actual LUTs on an as needed basis. Typically zero or
 * one is what will be needed.
 * Colour component values are in the range 0.0->1.0 represented as an integer
 * in the range 0->255 (ie in a byte). It is assumed that even if we have 5
 * bit colour components these are presented mapped on to 8 bit components.
 * lcdGammaLUT references LUTs which convert linear colour components
 * to a gamma adjusted space, and
 * lcdInvGammaLUT references LUTs which convert gamma adjusted colour
 * components to a linear space.
 */
#define MIN_GAMMA 100
#define MAX_GAMMA 250
#define LCDLUTCOUNT (MAX_GAMMA-MIN_GAMMA+1)
 UInt8 *lcdGammaLUT[LCDLUTCOUNT];
 UInt8 *lcdInvGammaLUT[LCDLUTCOUNT];

void initLUT(int gamma) {
  int i,index;
  double ig,g;

  index = gamma-MIN_GAMMA;

  lcdGammaLUT[index] = (UInt8*)malloc(256);
  lcdInvGammaLUT[index] = (UInt8*)malloc(256);
  if (gamma==100) {
    for (i=0;i<256;i++) {
      lcdGammaLUT[index][i] = (UInt8)i;
      lcdInvGammaLUT[index][i] = (UInt8)i;
    }
    return;
  }

  ig = ((double)gamma)/100.0;
  g = 1.0/ig;
  lcdGammaLUT[index][0] = (UInt8)0;
  lcdInvGammaLUT[index][0] = (UInt8)0;
  lcdGammaLUT[index][255] = (UInt8)255;
  lcdInvGammaLUT[index][255] = (UInt8)255;
  for (i=1;i<255;i++) {
    double val = ((double)i)/255.0;
    double gval = pow(val, g);
    double igval = pow(val, ig);
    lcdGammaLUT[index][i] = (UInt8)(255*gval);
    lcdInvGammaLUT[index][i] = (UInt8)(255*igval);
  }
}

static unsigned char* getLCDGammaLUT(int gamma) {
  int index;

  if (gamma<MIN_GAMMA) {
     gamma = MIN_GAMMA;
  } else if (gamma>MAX_GAMMA) {
     gamma = MAX_GAMMA;
  }
  index = gamma-MIN_GAMMA;
  if (!lcdGammaLUT[index]) {
    initLUT(gamma);
  }
  return (unsigned char*)lcdGammaLUT[index];
}

static unsigned char* getInvLCDGammaLUT(int gamma) {
  int index;

   if (gamma<MIN_GAMMA) {
     gamma = MIN_GAMMA;
  } else if (gamma>MAX_GAMMA) {
     gamma = MAX_GAMMA;
  }
  index = gamma-MIN_GAMMA;
  if (!lcdInvGammaLUT[index]) {
    initLUT(gamma);
  }
  return (unsigned char*)lcdInvGammaLUT[index];
}

#if 0
void printDefaultTables(int gamma) {
  int i;
  UInt8 *g, *ig;
  lcdGammaLUT[gamma-MIN_GAMMA] = NULL;
  lcdInvGammaLUT[gamma-MIN_GAMMA] = NULL;
  g = getLCDGammaLUT(gamma);
  ig = getInvLCDGammaLUT(gamma);
  printf("UInt8 defaultGammaLUT[256] = {\n");
  for (i=0;i<256;i++) {
    if (i % 8 == 0) {
      printf("    /* %3d */  ", i);
    }
    printf("%4d, ",(int)(g[i]&0xff));
    if ((i+1) % 8 == 0) {
      printf("\n");
    }
  }
  printf("};\n");

  printf("UInt8 defaultInvGammaLUT[256] = {\n");
  for (i=0;i<256;i++) {
    if (i % 8 == 0) {
      printf("    /* %3d */  ", i);
    }
    printf("%4d, ",(int)(ig[i]&0xff));
    if ((i+1) % 8 == 0) {
      printf("\n");
    }
  }
  printf("};\n");
}
#endif

/* These tables are generated for a Gamma adjustment of 1.4 */
UInt8 defaultGammaLUT[256] = {
    /*   0 */     0,    4,    7,   10,   13,   15,   17,   19,
    /*   8 */    21,   23,   25,   27,   28,   30,   32,   33,
    /*  16 */    35,   36,   38,   39,   41,   42,   44,   45,
    /*  24 */    47,   48,   49,   51,   52,   53,   55,   56,
    /*  32 */    57,   59,   60,   61,   62,   64,   65,   66,
    /*  40 */    67,   69,   70,   71,   72,   73,   75,   76,
    /*  48 */    77,   78,   79,   80,   81,   83,   84,   85,
    /*  56 */    86,   87,   88,   89,   90,   91,   92,   93,
    /*  64 */    94,   96,   97,   98,   99,  100,  101,  102,
    /*  72 */   103,  104,  105,  106,  107,  108,  109,  110,
    /*  80 */   111,  112,  113,  114,  115,  116,  117,  118,
    /*  88 */   119,  120,  121,  122,  123,  124,  125,  125,
    /*  96 */   126,  127,  128,  129,  130,  131,  132,  133,
    /* 104 */   134,  135,  136,  137,  138,  138,  139,  140,
    /* 112 */   141,  142,  143,  144,  145,  146,  147,  147,
    /* 120 */   148,  149,  150,  151,  152,  153,  154,  154,
    /* 128 */   155,  156,  157,  158,  159,  160,  161,  161,
    /* 136 */   162,  163,  164,  165,  166,  167,  167,  168,
    /* 144 */   169,  170,  171,  172,  172,  173,  174,  175,
    /* 152 */   176,  177,  177,  178,  179,  180,  181,  181,
    /* 160 */   182,  183,  184,  185,  186,  186,  187,  188,
    /* 168 */   189,  190,  190,  191,  192,  193,  194,  194,
    /* 176 */   195,  196,  197,  198,  198,  199,  200,  201,
    /* 184 */   201,  202,  203,  204,  205,  205,  206,  207,
    /* 192 */   208,  208,  209,  210,  211,  212,  212,  213,
    /* 200 */   214,  215,  215,  216,  217,  218,  218,  219,
    /* 208 */   220,  221,  221,  222,  223,  224,  224,  225,
    /* 216 */   226,  227,  227,  228,  229,  230,  230,  231,
    /* 224 */   232,  233,  233,  234,  235,  236,  236,  237,
    /* 232 */   238,  239,  239,  240,  241,  242,  242,  243,
    /* 240 */   244,  244,  245,  246,  247,  247,  248,  249,
    /* 248 */   249,  250,  251,  252,  252,  253,  254,  255,
};

UInt8 defaultInvGammaLUT[256] = {
    /*   0 */     0,    0,    0,    0,    0,    1,    1,    1,
    /*   8 */     2,    2,    2,    3,    3,    3,    4,    4,
    /*  16 */     5,    5,    6,    6,    7,    7,    8,    8,
    /*  24 */     9,    9,   10,   10,   11,   12,   12,   13,
    /*  32 */    13,   14,   15,   15,   16,   17,   17,   18,
    /*  40 */    19,   19,   20,   21,   21,   22,   23,   23,
    /*  48 */    24,   25,   26,   26,   27,   28,   29,   29,
    /*  56 */    30,   31,   32,   32,   33,   34,   35,   36,
    /*  64 */    36,   37,   38,   39,   40,   40,   41,   42,
    /*  72 */    43,   44,   45,   45,   46,   47,   48,   49,
    /*  80 */    50,   51,   52,   52,   53,   54,   55,   56,
    /*  88 */    57,   58,   59,   60,   61,   62,   63,   64,
    /*  96 */    64,   65,   66,   67,   68,   69,   70,   71,
    /* 104 */    72,   73,   74,   75,   76,   77,   78,   79,
    /* 112 */    80,   81,   82,   83,   84,   85,   86,   87,
    /* 120 */    88,   89,   90,   91,   92,   93,   95,   96,
    /* 128 */    97,   98,   99,  100,  101,  102,  103,  104,
    /* 136 */   105,  106,  107,  109,  110,  111,  112,  113,
    /* 144 */   114,  115,  116,  117,  119,  120,  121,  122,
    /* 152 */   123,  124,  125,  127,  128,  129,  130,  131,
    /* 160 */   132,  133,  135,  136,  137,  138,  139,  140,
    /* 168 */   142,  143,  144,  145,  146,  148,  149,  150,
    /* 176 */   151,  152,  154,  155,  156,  157,  159,  160,
    /* 184 */   161,  162,  163,  165,  166,  167,  168,  170,
    /* 192 */   171,  172,  173,  175,  176,  177,  178,  180,
    /* 200 */   181,  182,  184,  185,  186,  187,  189,  190,
    /* 208 */   191,  193,  194,  195,  196,  198,  199,  200,
    /* 216 */   202,  203,  204,  206,  207,  208,  210,  211,
    /* 224 */   212,  214,  215,  216,  218,  219,  220,  222,
    /* 232 */   223,  224,  226,  227,  228,  230,  231,  232,
    /* 240 */   234,  235,  236,  238,  239,  241,  242,  243,
    /* 248 */   245,  246,  248,  249,  250,  252,  253,  255,
};


/* Since our default is 140, here we can populate that from pre-calculated
 * data, it needs only 512 bytes - plus a few more of overhead - and saves
 * about that many intrinsic function calls plus other FP calculations.
 */
void initLCDGammaTables() {
   memset(lcdGammaLUT, 0,  LCDLUTCOUNT * sizeof(UInt8*));
   memset(lcdInvGammaLUT, 0, LCDLUTCOUNT * sizeof(UInt8*));
/*    printDefaultTables(140); */
   lcdGammaLUT[40] = defaultGammaLUT;
   lcdInvGammaLUT[40] = defaultInvGammaLUT;
}
