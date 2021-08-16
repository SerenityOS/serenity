/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <limits.h>
#include <math.h>
#include <jlong.h>

#include "sun_java2d_metal_MTLTextRenderer.h"

#include "SurfaceData.h"
#include "MTLContext.h"
#include "MTLRenderQueue.h"
#include "MTLTextRenderer.h"
#include "MTLVertexCache.h"
#include "MTLGlyphCache.h"
#include "MTLBlitLoops.h"

/**
 * The following constants define the inner and outer bounds of the
 * accelerated glyph cache.
 */
#define MTLTR_CACHE_WIDTH       512
#define MTLTR_CACHE_HEIGHT      512
#define MTLTR_CACHE_CELL_WIDTH  32
#define MTLTR_CACHE_CELL_HEIGHT 32

/**
 * The current "glyph mode" state.  This variable is used to track the
 * codepath used to render a particular glyph.  This variable is reset to
 * MODE_NOT_INITED at the beginning of every call to MTLTR_DrawGlyphList().
 * As each glyph is rendered, the glyphMode variable is updated to reflect
 * the current mode, so if the current mode is the same as the mode used
 * to render the previous glyph, we can avoid doing costly setup operations
 * each time.
 */
typedef enum {
    MODE_NOT_INITED,
    MODE_USE_CACHE_GRAY,
    MODE_USE_CACHE_LCD,
    MODE_NO_CACHE_GRAY,
    MODE_NO_CACHE_LCD,
    MODE_NO_CACHE_COLOR
} GlyphMode;
static GlyphMode glyphMode = MODE_NOT_INITED;

/**
 * There are two separate glyph caches: for AA and for LCD.
 * Once one of them is initialized as either GRAY or LCD, it
 * stays in that mode for the duration of the application.  It should
 * be safe to use this one glyph cache for all screens in a multimon
 * environment, since the glyph cache texture is shared between all contexts,
 * and (in theory) Metal drivers should be smart enough to manage that
 * texture across all screens.
 */

static MTLGlyphCacheInfo *glyphCacheLCD = NULL;
static MTLGlyphCacheInfo *glyphCacheAA = NULL;

/**
 * This value tracks the previous LCD rgbOrder setting, so if the rgbOrder
 * value has changed since the last time, it indicates that we need to
 * invalidate the cache, which may already store glyph images in the reverse
 * order.  Note that in most real world applications this value will not
 * change over the course of the application, but tests like Font2DTest
 * allow for changing the ordering at runtime, so we need to handle that case.
 */
static jboolean lastRGBOrder = JNI_TRUE;

/**
 * This constant defines the size of the tile to use in the
 * MTLTR_DrawLCDGlyphNoCache() method.  See below for more on why we
 * restrict this value to a particular size.
 */
#define MTLTR_NOCACHE_TILE_SIZE 32

static struct TxtVertex txtVertices[6];
static jint vertexCacheIndex = 0;
static id<MTLRenderCommandEncoder> lcdCacheEncoder = nil;

#define LCD_ADD_VERTEX(TX, TY, DX, DY, DZ) \
    do { \
        struct TxtVertex *v = &txtVertices[vertexCacheIndex++]; \
        v->txtpos[0] = TX; \
        v->txtpos[1] = TY; \
        v->position[0]= DX; \
        v->position[1] = DY; \
    } while (0)

#define LCD_ADD_TRIANGLES(TX1, TY1, TX2, TY2, DX1, DY1, DX2, DY2) \
    do { \
        LCD_ADD_VERTEX(TX1, TY1, DX1, DY1, 0); \
        LCD_ADD_VERTEX(TX2, TY1, DX2, DY1, 0); \
        LCD_ADD_VERTEX(TX2, TY2, DX2, DY2, 0); \
        LCD_ADD_VERTEX(TX2, TY2, DX2, DY2, 0); \
        LCD_ADD_VERTEX(TX1, TY2, DX1, DY2, 0); \
        LCD_ADD_VERTEX(TX1, TY1, DX1, DY1, 0); \
    } while (0)

/**
 * Initializes the one glyph cache (texture and data structure).
 * If lcdCache is JNI_TRUE, the texture will contain RGB data,
 * otherwise we will simply store the grayscale/monochrome glyph images
 * as intensity values.
 */
static jboolean
MTLTR_InitGlyphCache(MTLContext *mtlc, jboolean lcdCache)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_InitGlyphCache");
    // TODO : Need to verify RGB order in case of LCD
    MTLPixelFormat pixelFormat =
        lcdCache ? MTLPixelFormatBGRA8Unorm : MTLPixelFormatA8Unorm;

    MTLGlyphCacheInfo *gcinfo;
    // init glyph cache data structure
    gcinfo = MTLGlyphCache_Init(MTLTR_CACHE_WIDTH,
                                MTLTR_CACHE_HEIGHT,
                                MTLTR_CACHE_CELL_WIDTH,
                                MTLTR_CACHE_CELL_HEIGHT,
                                MTLVertexCache_FlushGlyphVertexCache);

    if (gcinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "MTLTR_InitGlyphCache: could not init MTL glyph cache");
        return JNI_FALSE;
    }

    MTLTextureDescriptor *textureDescriptor =
        [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:pixelFormat
                                                            width:MTLTR_CACHE_WIDTH
                                                            height:MTLTR_CACHE_HEIGHT
                                                            mipmapped:NO];

    gcinfo->texture = [mtlc.device newTextureWithDescriptor:textureDescriptor];

    if (lcdCache) {
        glyphCacheLCD = gcinfo;
    } else {
        glyphCacheAA = gcinfo;
    }

    return JNI_TRUE;
}

id<MTLTexture>
MTLTR_GetGlyphCacheTexture()
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_GetGlyphCacheTexture");
    if (glyphCacheAA != NULL) {
        return glyphCacheAA->texture;
    }
    return NULL;
}

/**
 * Adds the given glyph to the glyph cache (texture and data structure)
 * associated with the given MTLContext.
 */
static void
MTLTR_AddToGlyphCache(GlyphInfo *glyph, MTLContext *mtlc,
                      jboolean lcdCache)
{
    MTLCacheCellInfo *ccinfo;
    MTLGlyphCacheInfo *gcinfo;
    jint w = glyph->width;
    jint h = glyph->height;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_AddToGlyphCache");
    if (!lcdCache) {
        gcinfo = glyphCacheAA;
    } else {
        gcinfo = glyphCacheLCD;
    }

    if ((gcinfo == NULL) || (glyph->image == NULL)) {
        return;
    }

    bool isCacheFull = MTLGlyphCache_IsCacheFull(gcinfo, glyph);
    if (isCacheFull) {
        MTLGlyphCache_Free(gcinfo);
        if (!lcdCache) {
            MTLTR_InitGlyphCache(mtlc, JNI_FALSE);
            gcinfo = glyphCacheAA;
        } else {
            MTLTR_InitGlyphCache(mtlc, JNI_TRUE);
            gcinfo = glyphCacheLCD;
        }
    }
    MTLGlyphCache_AddGlyph(gcinfo, glyph);
    ccinfo = (MTLCacheCellInfo *) glyph->cellInfo;

    if (ccinfo != NULL) {
        // store glyph image in texture cell
        MTLRegion region = {
                {ccinfo->x,  ccinfo->y,   0},
                {w, h, 1}
        };
        if (!lcdCache) {
            NSUInteger bytesPerRow = 1 * w;
            [gcinfo->texture replaceRegion:region
                             mipmapLevel:0
                             withBytes:glyph->image
                             bytesPerRow:bytesPerRow];
        } else {
            unsigned int imageBytes = w * h * 4;
            unsigned char imageData[imageBytes];
            memset(&imageData, 0, sizeof(imageData));

            int srcIndex = 0;
            int dstIndex = 0;
            for (int i = 0; i < (w * h); i++) {
                imageData[dstIndex++] = glyph->image[srcIndex++];
                imageData[dstIndex++] = glyph->image[srcIndex++];
                imageData[dstIndex++] = glyph->image[srcIndex++];
                imageData[dstIndex++] = 0xFF;
            }

            NSUInteger bytesPerRow = 4 * w;
            [gcinfo->texture replaceRegion:region
                             mipmapLevel:0
                             withBytes:imageData
                             bytesPerRow:bytesPerRow];
        }
    }
}

static jboolean
MTLTR_SetLCDContrast(MTLContext *mtlc,
                     jint contrast,
                     id<MTLRenderCommandEncoder> encoder)
{
    if (![mtlc.paint isKindOfClass:[MTLColorPaint class]]) {
        return JNI_FALSE;
    }
    MTLColorPaint* cPaint = (MTLColorPaint *) mtlc.paint;
    // update the current color settings
    double gamma = ((double)contrast) / 100.0;
    double invgamma = 1.0/gamma;
    jfloat radj, gadj, badj;
    jfloat clr[4];
    jint col = cPaint.color;

    J2dTraceLn2(J2D_TRACE_INFO, "primary color %x, contrast %d", col, contrast);
    J2dTraceLn2(J2D_TRACE_INFO, "gamma %f, invgamma %f", gamma, invgamma);

    clr[0] = ((col >> 16) & 0xFF)/255.0f;
    clr[1] = ((col >> 8) & 0xFF)/255.0f;
    clr[2] = ((col) & 0xFF)/255.0f;

    // gamma adjust the primary color
    radj = (float)pow(clr[0], gamma);
    gadj = (float)pow(clr[1], gamma);
    badj = (float)pow(clr[2], gamma);

    struct LCDFrameUniforms uf = {
            {radj, gadj, badj},
            {gamma, gamma, gamma},
            {invgamma, invgamma, invgamma}};
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
    return JNI_TRUE;
}

void
MTLTR_EnableGlyphVertexCache(MTLContext *mtlc, BMTLSDOps *dstOps)
{
J2dTraceLn(J2D_TRACE_INFO, "MTLTR_EnableGlyphVertexCache");

    if (!MTLVertexCache_InitVertexCache()) {
        return;
    }

    if (glyphCacheAA == NULL) {
        if (!MTLTR_InitGlyphCache(mtlc, JNI_FALSE)) {
            return;
        }
    }
    MTLVertexCache_CreateSamplingEncoder(mtlc, dstOps);
}

void
MTLTR_DisableGlyphVertexCache(MTLContext *mtlc)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DisableGlyphVertexCache");
    MTLVertexCache_FlushGlyphVertexCache();
    MTLVertexCache_FreeVertexCache();
}

void MTLTR_FreeGlyphCaches() {
    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_FreeGlyphCaches : freeing glyph caches.");

    if (glyphCacheAA != NULL) {
        [glyphCacheAA->texture release];
        MTLGlyphCache_Free(glyphCacheAA);
        glyphCacheAA = NULL;
    }

    if (glyphCacheLCD != NULL) {
        [glyphCacheLCD->texture release];
        MTLGlyphCache_Free(glyphCacheLCD);
        glyphCacheLCD = NULL;
    }
}

static MTLPaint* storedPaint = nil;

static void EnableColorGlyphPainting(MTLContext *mtlc) {
    storedPaint = mtlc.paint;
    mtlc.paint = [[MTLPaint alloc] init];
}

static void DisableColorGlyphPainting(MTLContext *mtlc) {
    [mtlc.paint release];
    mtlc.paint = storedPaint;
    storedPaint = nil;
}

static jboolean
MTLTR_DrawGrayscaleGlyphViaCache(MTLContext *mtlc,
                                 GlyphInfo *ginfo, jint x, jint y, BMTLSDOps *dstOps)
{
    MTLCacheCellInfo *cell;
    jfloat x1, y1, x2, y2;

    if (glyphMode != MODE_USE_CACHE_GRAY) {
        if (glyphMode == MODE_NO_CACHE_GRAY) {
            MTLVertexCache_DisableMaskCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_LCD) {
            [mtlc.encoderManager endEncoder];
            lcdCacheEncoder = nil;
        } else if (glyphMode == MODE_NO_CACHE_COLOR) {
            DisableColorGlyphPainting(mtlc);
        }
        MTLTR_EnableGlyphVertexCache(mtlc, dstOps);
        glyphMode = MODE_USE_CACHE_GRAY;
    }

    if (ginfo->cellInfo == NULL) {
        // attempt to add glyph to accelerated glyph cache
        MTLTR_AddToGlyphCache(ginfo, mtlc, JNI_FALSE);

        if (ginfo->cellInfo == NULL) {
            // we'll just no-op in the rare case that the cell is NULL
            return JNI_TRUE;
        }
    }

    cell = (MTLCacheCellInfo *) (ginfo->cellInfo);
    cell->timesRendered++;

    x1 = (jfloat)x;
    y1 = (jfloat)y;
    x2 = x1 + ginfo->width;
    y2 = y1 + ginfo->height;

    MTLVertexCache_AddGlyphQuad(mtlc,
                                cell->tx1, cell->ty1,
                                cell->tx2, cell->ty2,
                                x1, y1, x2, y2);

    return JNI_TRUE;
}

static jboolean
MTLTR_DrawLCDGlyphViaCache(MTLContext *mtlc, BMTLSDOps *dstOps,
                           GlyphInfo *ginfo, jint x, jint y,
                           jboolean rgbOrder, jint contrast)
{
    CacheCellInfo *cell;
    jfloat tx1, ty1, tx2, ty2;
    jint w = ginfo->width;
    jint h = ginfo->height;

    if (glyphMode != MODE_USE_CACHE_LCD) {
        if (glyphMode == MODE_NO_CACHE_GRAY) {
            MTLVertexCache_DisableMaskCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        } else if (glyphMode == MODE_NO_CACHE_COLOR) {
            DisableColorGlyphPainting(mtlc);
        }

        if (glyphCacheLCD == NULL) {
            if (!MTLTR_InitGlyphCache(mtlc, JNI_TRUE)) {
                return JNI_FALSE;
            }
        }
        if (lcdCacheEncoder == nil) {
            lcdCacheEncoder = [mtlc.encoderManager getLCDEncoder:dstOps->pTexture isSrcOpaque:YES isDstOpaque:YES];
        }
        if (rgbOrder != lastRGBOrder) {
            // need to invalidate the cache in this case; see comments
            // for lastRGBOrder above
            MTLGlyphCache_Invalidate(glyphCacheLCD);
            lastRGBOrder = rgbOrder;
        }

        glyphMode = MODE_USE_CACHE_LCD;
    }

    if (ginfo->cellInfo == NULL) {
        // attempt to add glyph to accelerated glyph cache
        // TODO : Handle RGB order
        MTLTR_AddToGlyphCache(ginfo, mtlc, JNI_TRUE);

        if (ginfo->cellInfo == NULL) {
            // we'll just no-op in the rare case that the cell is NULL
            return JNI_TRUE;
        }
    }
    cell = (CacheCellInfo *) (ginfo->cellInfo);
    cell->timesRendered++;

    MTLTR_SetLCDContrast(mtlc, contrast, lcdCacheEncoder);
    tx1 = cell->tx1;
    ty1 = cell->ty1;
    tx2 = cell->tx2;
    ty2 = cell->ty2;

    J2dTraceLn4(J2D_TRACE_INFO, "tx1 = %f, ty1 = %f, tx2 = %f, ty2 = %f", tx1, ty1, tx2, ty2);
    J2dTraceLn2(J2D_TRACE_INFO, "width = %d height = %d", dstOps->width, dstOps->height);

    LCD_ADD_TRIANGLES(tx1, ty1, tx2, ty2, x, y, x+w, y+h);

    [lcdCacheEncoder setVertexBytes:txtVertices length:sizeof(txtVertices) atIndex:MeshVertexBuffer];
    [lcdCacheEncoder setFragmentTexture:glyphCacheLCD->texture atIndex:0];
    [lcdCacheEncoder setFragmentTexture:dstOps->pTexture atIndex:1];

    [lcdCacheEncoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:6];

    vertexCacheIndex = 0;

    return JNI_TRUE;
}

static jboolean
MTLTR_DrawGrayscaleGlyphNoCache(MTLContext *mtlc,
                                GlyphInfo *ginfo, jint x, jint y, BMTLSDOps *dstOps)
{
    jint tw, th;
    jint sx, sy, sw, sh;
    jint x0;
    jint w = ginfo->width;
    jint h = ginfo->height;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGrayscaleGlyphNoCache");
    if (glyphMode != MODE_NO_CACHE_GRAY) {
        if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_LCD) {
            [mtlc.encoderManager endEncoder];
            lcdCacheEncoder = nil;
        } else if (glyphMode == MODE_NO_CACHE_COLOR) {
            DisableColorGlyphPainting(mtlc);
        }
        MTLVertexCache_EnableMaskCache(mtlc, dstOps);
        glyphMode = MODE_NO_CACHE_GRAY;
    }

    x0 = x;
    tw = MTLVC_MASK_CACHE_TILE_WIDTH;
    th = MTLVC_MASK_CACHE_TILE_HEIGHT;

    for (sy = 0; sy < h; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > h) ? (h - sy) : th;

        for (sx = 0; sx < w; sx += tw, x += tw) {
            sw = ((sx + tw) > w) ? (w - sx) : tw;

            J2dTraceLn7(J2D_TRACE_INFO, "sx = %d sy = %d x = %d y = %d sw = %d sh = %d w = %d", sx, sy, x, y, sw, sh, w);
            MTLVertexCache_AddMaskQuad(mtlc,
                                       sx, sy, x, y, sw, sh,
                                       w, ginfo->image,
                                       dstOps);
        }
    }

    return JNI_TRUE;
}


static jboolean
MTLTR_DrawLCDGlyphNoCache(MTLContext *mtlc, BMTLSDOps *dstOps,
                          GlyphInfo *ginfo, jint x, jint y,
                          jint rowBytesOffset,
                          jboolean rgbOrder, jint contrast)
{
    jfloat tx1, ty1, tx2, ty2;
    jint tw, th;
    jint w = ginfo->width;
    jint h = ginfo->height;
    id<MTLTexture> blitTexture = nil;

    J2dTraceLn2(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache x %d, y%d", x, y);
    J2dTraceLn3(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache rowBytesOffset=%d, rgbOrder=%d, contrast=%d", rowBytesOffset, rgbOrder, contrast);


    id<MTLRenderCommandEncoder> encoder = nil;

    MTLTextureDescriptor *textureDescriptor =
        [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                            width:w
                                                            height:h
                                                            mipmapped:NO];

    blitTexture = [mtlc.device newTextureWithDescriptor:textureDescriptor];

    if (glyphMode != MODE_NO_CACHE_LCD) {
        if (glyphMode == MODE_NO_CACHE_GRAY) {
            MTLVertexCache_DisableMaskCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_LCD) {
            [mtlc.encoderManager endEncoder];
            lcdCacheEncoder = nil;
        } else if (glyphMode == MODE_NO_CACHE_COLOR) {
            DisableColorGlyphPainting(mtlc);
        }

        if (blitTexture == nil) {
            J2dTraceLn(J2D_TRACE_ERROR, "can't obtain temporary texture object from pool");
            return JNI_FALSE;
        }


        glyphMode = MODE_NO_CACHE_LCD;
    }
    encoder = [mtlc.encoderManager getLCDEncoder:dstOps->pTexture isSrcOpaque:YES isDstOpaque:YES];
    MTLTR_SetLCDContrast(mtlc, contrast, encoder);

    unsigned int imageBytes = w * h * 4;
    unsigned char imageData[imageBytes];
    memset(&imageData, 0, sizeof(imageData));

    int srcIndex = 0;
    int dstIndex = 0;
    for (int i = 0; i < (w * h); i++) {
        imageData[dstIndex++] = ginfo->image[srcIndex++ + rowBytesOffset];
        imageData[dstIndex++] = ginfo->image[srcIndex++ + rowBytesOffset];
        imageData[dstIndex++] = ginfo->image[srcIndex++ + rowBytesOffset];
        imageData[dstIndex++] = 0xFF;
    }

    // copy LCD mask into glyph texture tile
    MTLRegion region = MTLRegionMake2D(0, 0, w, h);

    NSUInteger bytesPerRow = 4 * ginfo->width;
    [blitTexture replaceRegion:region
                 mipmapLevel:0
                 withBytes:imageData
                 bytesPerRow:bytesPerRow];

    tx1 = 0.0f;
    ty1 = 0.0f;
    tx2 = 1.0f;
    ty2 = 1.0f;

    J2dTraceLn2(J2D_TRACE_INFO, "MTLTR_DrawLCDGlyphNoCache : dstOps->width = %d, dstOps->height = %d", dstOps->width, dstOps->height);

    LCD_ADD_TRIANGLES(tx1, ty1, tx2, ty2, x, y, x+w, y+h);

    [encoder setVertexBytes:txtVertices length:sizeof(txtVertices) atIndex:MeshVertexBuffer];
    [encoder setFragmentTexture:blitTexture atIndex:0];
    [encoder setFragmentTexture:dstOps->pTexture atIndex:1];

    [encoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:6];

    vertexCacheIndex = 0;
    [mtlc.encoderManager endEncoder];
    [blitTexture release];

    MTLCommandBufferWrapper* cbwrapper = [mtlc pullCommandBufferWrapper];

    id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
    [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
        [cbwrapper release];
    }];

    [commandbuf commit];
    [commandbuf waitUntilCompleted];

    return JNI_TRUE;
}

static jboolean
MTLTR_DrawColorGlyphNoCache(MTLContext *mtlc,
                            GlyphInfo *ginfo, jint x, jint y, BMTLSDOps *dstOps)
{
    id<MTLTexture> dest = dstOps->pTexture;
    const void *src = ginfo->image;
    jint w = ginfo->width;
    jint h = ginfo->height;
    jint rowBytes = ginfo->rowBytes;
    unsigned int imageSize = rowBytes * h;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawColorGlyphNoCache");

    if (glyphMode != MODE_NO_CACHE_COLOR) {
        if (glyphMode == MODE_NO_CACHE_GRAY) {
            MTLVertexCache_DisableMaskCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_GRAY) {
            MTLTR_DisableGlyphVertexCache(mtlc);
        } else if (glyphMode == MODE_USE_CACHE_LCD) {
            [mtlc.encoderManager endEncoder];
            lcdCacheEncoder = nil;
        }
        glyphMode = MODE_NO_CACHE_COLOR;
        EnableColorGlyphPainting(mtlc);
    }

    MTLPooledTextureHandle* texHandle = [mtlc.texturePool getTexture:w height:h format:MTLPixelFormatBGRA8Unorm];
    if (texHandle == nil) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLTR_DrawColorGlyphNoCache: can't obtain temporary texture object from pool");
        return JNI_FALSE;
    }

    [[mtlc getCommandBufferWrapper] registerPooledTexture:texHandle];

    [texHandle.texture replaceRegion:MTLRegionMake2D(0, 0, w, h)
                         mipmapLevel:0
                           withBytes:src
                         bytesPerRow:rowBytes];

    drawTex2Tex(mtlc, texHandle.texture, dest, JNI_FALSE, dstOps->isOpaque, INTERPOLATION_NEAREST_NEIGHBOR,
                0, 0, w, h, x, y, x + w, y + h);

    return JNI_TRUE;
}

// see DrawGlyphList.c for more on this macro...
#define FLOOR_ASSIGN(l, r) \
    if ((r)<0) (l) = ((int)floor(r)); else (l) = ((int)(r))

void
MTLTR_DrawGlyphList(JNIEnv *env, MTLContext *mtlc, BMTLSDOps *dstOps,
                    jint totalGlyphs, jboolean usePositions,
                    jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
                    jfloat glyphListOrigX, jfloat glyphListOrigY,
                    unsigned char *images, unsigned char *positions)
{
    int glyphCounter;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList");

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(images);
    if (usePositions) {
        RETURN_IF_NULL(positions);
    }

    glyphMode = MODE_NOT_INITED;
    J2dTraceLn1(J2D_TRACE_INFO, "totalGlyphs = %d", totalGlyphs);
    jboolean flushBeforeLCD = JNI_FALSE;

    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) {
        J2dTraceLn(J2D_TRACE_INFO, "Entered for loop for glyph list");
        jint x, y;
        jfloat glyphx, glyphy;
        jboolean ok;
        GlyphInfo *ginfo = (GlyphInfo *)jlong_to_ptr(NEXT_LONG(images));

        if (ginfo == NULL) {
            // this shouldn't happen, but if it does we'll just break out...
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "MTLTR_DrawGlyphList: glyph info is null");
            break;
        }

        if (usePositions) {
            jfloat posx = NEXT_FLOAT(positions);
            jfloat posy = NEXT_FLOAT(positions);
            glyphx = glyphListOrigX + posx + ginfo->topLeftX;
            glyphy = glyphListOrigY + posy + ginfo->topLeftY;
            FLOOR_ASSIGN(x, glyphx);
            FLOOR_ASSIGN(y, glyphy);
        } else {
            glyphx = glyphListOrigX + ginfo->topLeftX;
            glyphy = glyphListOrigY + ginfo->topLeftY;
            FLOOR_ASSIGN(x, glyphx);
            FLOOR_ASSIGN(y, glyphy);
            glyphListOrigX += ginfo->advanceX;
            glyphListOrigY += ginfo->advanceY;
        }

        if (ginfo->image == NULL) {
            J2dTraceLn(J2D_TRACE_INFO, "Glyph image is null");
            continue;
        }

        J2dTraceLn2(J2D_TRACE_INFO, "Glyph width = %d height = %d", ginfo->width, ginfo->height);
        J2dTraceLn1(J2D_TRACE_INFO, "rowBytes = %d", ginfo->rowBytes);
        if (ginfo->rowBytes == ginfo->width) {
            // grayscale or monochrome glyph data
            if (ginfo->width <= MTLTR_CACHE_CELL_WIDTH &&
                ginfo->height <= MTLTR_CACHE_CELL_HEIGHT)
            {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList Grayscale cache");
                ok = MTLTR_DrawGrayscaleGlyphViaCache(mtlc, ginfo, x, y, dstOps);
            } else {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList Grayscale no cache");
                ok = MTLTR_DrawGrayscaleGlyphNoCache(mtlc, ginfo, x, y, dstOps);
            }
        } else if (ginfo->rowBytes == ginfo->width * 4) {
            J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList color glyph no cache");
            ok = MTLTR_DrawColorGlyphNoCache(mtlc, ginfo, x, y, dstOps);
            flushBeforeLCD = JNI_FALSE;
        } else {
            if (!flushBeforeLCD) {
                [mtlc.encoderManager endEncoder];
                MTLCommandBufferWrapper* cbwrapper = [mtlc pullCommandBufferWrapper];

                id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
                [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
                    [cbwrapper release];
                }];

                [commandbuf commit];
                flushBeforeLCD = JNI_TRUE;
            }

            // LCD-optimized glyph data
            jint rowBytesOffset = 0;

            if (subPixPos) {
                jint frac = (jint)((glyphx - x) * 3);
                if (frac != 0) {
                    rowBytesOffset = 3 - frac;
                    x += 1;
                }
            }

            if (rowBytesOffset == 0 &&
                ginfo->width <= MTLTR_CACHE_CELL_WIDTH &&
                ginfo->height <= MTLTR_CACHE_CELL_HEIGHT)
            {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList LCD cache");
                ok = MTLTR_DrawLCDGlyphViaCache(mtlc, dstOps,
                                                ginfo, x, y,
                                                rgbOrder, lcdContrast);
            } else {
                J2dTraceLn(J2D_TRACE_INFO, "MTLTR_DrawGlyphList LCD no cache");
                ok = MTLTR_DrawLCDGlyphNoCache(mtlc, dstOps,
                                               ginfo, x, y,
                                               rowBytesOffset,
                                               rgbOrder, lcdContrast);
            }
        }

        if (!ok) {
            break;
        }
    }
    /*
     * Only in case of grayscale text drawing we need to flush
     * cache. Still in case of LCD we are not using any intermediate
     * cache.
     */
    if (glyphMode == MODE_NO_CACHE_GRAY) {
        MTLVertexCache_DisableMaskCache(mtlc);
    } else if (glyphMode == MODE_USE_CACHE_GRAY) {
        MTLTR_DisableGlyphVertexCache(mtlc);
    } else if (glyphMode == MODE_USE_CACHE_LCD) {
        [mtlc.encoderManager endEncoder];
        lcdCacheEncoder = nil;
    } else if (glyphMode == MODE_NO_CACHE_COLOR) {
        DisableColorGlyphPainting(mtlc);
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_metal_MTLTextRenderer_drawGlyphList
    (JNIEnv *env, jobject self,
     jint numGlyphs, jboolean usePositions,
     jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
     jfloat glyphListOrigX, jfloat glyphListOrigY,
     jlongArray imgArray, jfloatArray posArray)
{
    unsigned char *images;

    J2dTraceLn(J2D_TRACE_INFO, "MTLTextRenderer_drawGlyphList");

    images = (unsigned char *)
        (*env)->GetPrimitiveArrayCritical(env, imgArray, NULL);
    if (images != NULL) {
        MTLContext *mtlc = MTLRenderQueue_GetCurrentContext();
        BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();

        if (usePositions) {
            unsigned char *positions = (unsigned char *)
                (*env)->GetPrimitiveArrayCritical(env, posArray, NULL);
            if (positions != NULL) {
                MTLTR_DrawGlyphList(env, mtlc, dstOps,
                                    numGlyphs, usePositions,
                                    subPixPos, rgbOrder, lcdContrast,
                                    glyphListOrigX, glyphListOrigY,
                                    images, positions);
                (*env)->ReleasePrimitiveArrayCritical(env, posArray,
                                                      positions, JNI_ABORT);
            }
        } else {
            MTLTR_DrawGlyphList(env, mtlc, dstOps,
                                numGlyphs, usePositions,
                                subPixPos, rgbOrder, lcdContrast,
                                glyphListOrigX, glyphListOrigY,
                                images, NULL);
        }
        if (mtlc != NULL) {
            RESET_PREVIOUS_OP();
            [mtlc.encoderManager endEncoder];
            MTLCommandBufferWrapper * cbwrapper = [mtlc pullCommandBufferWrapper];
            id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
            [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
                [cbwrapper release];
            }];
            [commandbuf commit];
        }

        (*env)->ReleasePrimitiveArrayCritical(env, imgArray,
                                              images, JNI_ABORT);
    }
}
