/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <malloc.h>
#include <math.h>
#include <jlong.h>

#include "sun_java2d_d3d_D3DTextRenderer.h"
#include "sun_java2d_pipe_BufferedTextPipe.h"

#include "SurfaceData.h"
#include "D3DContext.h"
#include "D3DSurfaceData.h"
#include "D3DRenderQueue.h"
#include "D3DTextRenderer.h"
#include "D3DGlyphCache.h"
#include "AccelGlyphCache.h"
#include "fontscalerdefs.h"

/**
 * The current "glyph mode" state.  This variable is used to track the
 * codepath used to render a particular glyph.  This variable is reset to
 * MODE_NOT_INITED at the beginning of every call to D3DTR_DrawGlyphList().
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
    MODE_NO_CACHE_LCD
} GlyphMode;
static GlyphMode glyphMode = MODE_NOT_INITED;

/**
 * The current bounds of the "cached destination" texture, in destination
 * coordinate space.  The width/height of these bounds will not exceed the
 * D3DTR_CACHED_DEST_WIDTH/HEIGHT values defined above.  These bounds are
 * only considered valid when the isCachedDestValid flag is JNI_TRUE.
 */
static SurfaceDataBounds cachedDestBounds;

/**
 * This flag indicates whether the "cached destination" texture contains
 * valid data.  This flag is reset to JNI_FALSE at the beginning of every
 * call to D3DTR_DrawGlyphList().  Once we copy valid destination data
 * into the cached texture, this flag is set to JNI_TRUE.  This way, we
 * can limit the number of times we need to copy destination data, which
 * is a very costly operation.
 */
static jboolean isCachedDestValid = JNI_FALSE;

/**
 * The bounds of the previously rendered LCD glyph, in destination
 * coordinate space.  We use these bounds to determine whether the glyph
 * currently being rendered overlaps the previously rendered glyph (i.e.
 * its bounding box intersects that of the previously rendered glyph).
 * If so, we need to re-read the destination area associated with that
 * previous glyph so that we can correctly blend with the actual
 * destination data.
 */
static SurfaceDataBounds previousGlyphBounds;

/**
 * Updates the gamma and inverse gamma values for the LCD text shader.
 */
static HRESULT
D3DTR_UpdateLCDTextContrast(D3DContext *d3dc, jint contrast)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();

    jfloat fcon = ((jfloat)contrast) / 100.0f;
    jfloat invgamma = fcon;
    jfloat gamma = 1.0f / invgamma;
    jfloat vals[4];

    // update the "invgamma" parameter of the shader program
    vals[0] = invgamma;
    vals[1] = invgamma;
    vals[2] = invgamma;
    vals[3] = 0.0f; // unused
    pd3dDevice->SetPixelShaderConstantF(1, vals, 1);

    // update the "gamma" parameter of the shader program
    vals[0] = gamma;
    vals[1] = gamma;
    vals[2] = gamma;
    vals[3] = 0.0f; // unused
    res = pd3dDevice->SetPixelShaderConstantF(2, vals, 1);

    return res;
}

/**
 * Updates the current gamma-adjusted source color ("src_adj") of the LCD
 * text shader program.  Note that we could calculate this value in the
 * shader (e.g. just as we do for "dst_adj"), but would be unnecessary work
 * (and a measurable performance hit, maybe around 5%) since this value is
 * constant over the entire glyph list.  So instead we just calculate the
 * gamma-adjusted value once and update the uniform parameter of the LCD
 * shader as needed.
 */
static HRESULT
D3DTR_UpdateLCDTextColor(D3DContext *d3dc, jint contrast)
{
    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    jfloat gamma = ((jfloat)contrast) / 100.0f;
    jfloat clr[4];

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DTR_UpdateLCDTextColor: contrast=%d", contrast);

    /*
     * Note: Ideally we would update the "srcAdj" uniform parameter only
     * when there is a change in the source color.  Fortunately, the cost
     * of querying the current D3D color state and updating the uniform
     * value is quite small, and in the common case we only need to do this
     * once per GlyphList, so we gain little from trying to optimize too
     * eagerly here.
     */

    // get the current D3D primary color state
    jint color = d3dc->pVCacher->GetColor();
    clr[0] = (jfloat)((color >> 16) & 0xff) / 255.0f;
    clr[1] = (jfloat)((color >>  8) & 0xff) / 255.0f;
    clr[2] = (jfloat)((color >>  0) & 0xff) / 255.0f;
    clr[3] = 0.0f; // unused

    // gamma adjust the primary color
    clr[0] = (jfloat)pow(clr[0], gamma);
    clr[1] = (jfloat)pow(clr[1], gamma);
    clr[2] = (jfloat)pow(clr[2], gamma);

    // update the "srcAdj" parameter of the shader program with this value
    return pd3dDevice->SetPixelShaderConstantF(0, clr, 1);
}

/**
 * Enables the LCD text shader and updates any related state, such as the
 * gamma values.
 */
static HRESULT
D3DTR_EnableLCDGlyphModeState(D3DContext *d3dc, D3DSDOps *dstOps,
                              jboolean useCache, jint contrast)
{
    D3DResource *pGlyphTexRes, *pCachedDestTexRes;
    IDirect3DTexture9 *pGlyphTex, *pCachedDestTex;

    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);

    HRESULT res = S_OK;
    if (useCache) {
        // glyph cache had been already initialized
        pGlyphTexRes = d3dc->GetLCDGlyphCache()->GetGlyphCacheTexture();
    } else {
        res = d3dc->GetResourceManager()->GetBlitTexture(&pGlyphTexRes);
    }
    RETURN_STATUS_IF_FAILED(res);

    pGlyphTex = pGlyphTexRes->GetTexture();

    res = d3dc->GetResourceManager()->
        GetCachedDestTexture(dstOps->pResource->GetDesc()->Format,
                             &pCachedDestTexRes);
    RETURN_STATUS_IF_FAILED(res);
    pCachedDestTex = pCachedDestTexRes->GetTexture();

    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    D3DTEXTUREFILTERTYPE fhint =
        d3dc->IsTextureFilteringSupported(D3DTEXF_NONE) ?
        D3DTEXF_NONE : D3DTEXF_POINT;
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, fhint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, fhint);
    pd3dDevice->SetSamplerState(1, D3DSAMP_MAGFILTER, fhint);
    pd3dDevice->SetSamplerState(1, D3DSAMP_MINFILTER, fhint);
    d3dc->UpdateTextureColorState(D3DTA_TEXTURE, 1);

    // bind the texture containing glyph data to texture unit 0
    d3dc->SetTexture(pGlyphTex, 0);

    // bind the texture tile containing destination data to texture unit 1
    d3dc->SetTexture(pCachedDestTex, 1);

    // create/enable the LCD text shader
    res = d3dc->EnableLCDTextProgram();
    RETURN_STATUS_IF_FAILED(res);

    // update the current contrast settings (note: these change very rarely,
    // but it seems that D3D pixel shader registers aren't maintained as
    // part of the pixel shader instance, so we need to update these
    // everytime around in case another shader blew away the contents
    // of those registers)
    D3DTR_UpdateLCDTextContrast(d3dc, contrast);

    // update the current color settings
    return D3DTR_UpdateLCDTextColor(d3dc, contrast);
}

HRESULT
D3DTR_EnableGlyphVertexCache(D3DContext *d3dc)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DTR_EnableGlyphVertexCache");

    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    D3DTEXTUREFILTERTYPE fhint =
        d3dc->IsTextureFilteringSupported(D3DTEXF_NONE) ?
        D3DTEXF_NONE : D3DTEXF_POINT;
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, fhint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, fhint);

    // glyph cache had been successfully initialized if we got here
    D3DResource *pGlyphCacheTexRes =
        d3dc->GetGrayscaleGlyphCache()->GetGlyphCacheTexture();
    return d3dc->SetTexture(pGlyphCacheTexRes->GetTexture(), 0);
}

HRESULT
D3DTR_DisableGlyphVertexCache(D3DContext *d3dc)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DTR_DisableGlyphVertexCache");

    return d3dc->SetTexture(NULL, 0);
}

/**
 * Disables any pending state associated with the current "glyph mode".
 */
static HRESULT
D3DTR_DisableGlyphModeState(D3DContext *d3dc)
{
    HRESULT res = S_OK;
    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();

    switch (glyphMode) {
    case MODE_NO_CACHE_LCD:
    case MODE_USE_CACHE_LCD:
        d3dc->FlushVertexQueue();
        pd3dDevice->SetPixelShader(NULL);
        res = d3dc->SetTexture(NULL, 1);
        break;

    case MODE_NO_CACHE_GRAY:
    case MODE_USE_CACHE_GRAY:
    case MODE_NOT_INITED:
    default:
        break;
    }
    return res;
}

static HRESULT
D3DTR_DrawGrayscaleGlyphViaCache(D3DContext *d3dc,
                                 GlyphInfo *ginfo, jint x, jint y)
{
    HRESULT res = S_OK;
    D3DGlyphCache *pGrayscaleGCache;
    CacheCellInfo *cell;
    GlyphCacheInfo *gcache;
    jfloat x1, y1, x2, y2;

    J2dTraceLn(J2D_TRACE_VERBOSE, "D3DTR_DrawGrayscaleGlyphViaCache");

    if (glyphMode != MODE_USE_CACHE_GRAY) {
        D3DTR_DisableGlyphModeState(d3dc);

        res = d3dc->BeginScene(STATE_GLYPHOP);
        RETURN_STATUS_IF_FAILED(res);

        glyphMode = MODE_USE_CACHE_GRAY;
    }

    pGrayscaleGCache = d3dc->GetGrayscaleGlyphCache();
    gcache = pGrayscaleGCache->GetGlyphCache();
    cell = AccelGlyphCache_GetCellInfoForCache(ginfo, gcache);
    if (cell == NULL) {
        // attempt to add glyph to accelerated glyph cache
        res = pGrayscaleGCache->AddGlyph(ginfo);
        RETURN_STATUS_IF_FAILED(res);

        cell = AccelGlyphCache_GetCellInfoForCache(ginfo, gcache);
        RETURN_STATUS_IF_NULL(cell, E_FAIL);
    }

    cell->timesRendered++;

    x1 = (jfloat)x;
    y1 = (jfloat)y;
    x2 = x1 + ginfo->width;
    y2 = y1 + ginfo->height;

    return d3dc->pVCacher->DrawTexture(x1, y1, x2, y2,
                                       cell->tx1, cell->ty1,
                                       cell->tx2, cell->ty2);
}

/**
 * Evaluates to true if the rectangle defined by gx1/gy1/gx2/gy2 is
 * inside outerBounds.
 */
#define INSIDE(gx1, gy1, gx2, gy2, outerBounds) \
    (((gx1) >= outerBounds.x1) && ((gy1) >= outerBounds.y1) && \
     ((gx2) <= outerBounds.x2) && ((gy2) <= outerBounds.y2))

/**
 * Evaluates to true if the rectangle defined by gx1/gy1/gx2/gy2 intersects
 * the rectangle defined by bounds.
 */
#define INTERSECTS(gx1, gy1, gx2, gy2, bounds) \
    ((bounds.x2   > (gx1)) && (bounds.y2 > (gy1)) && \
     (bounds.x1   < (gx2)) && (bounds.y1 < (gy2)))

/**
 * This method checks to see if the given LCD glyph bounds fall within the
 * cached destination texture bounds.  If so, this method can return
 * immediately.  If not, this method will copy a chunk of framebuffer data
 * into the cached destination texture and then update the current cached
 * destination bounds before returning.
 *
 * The agx1, agx2 are "adjusted" glyph bounds, which are only used when checking
 * against the previous glyph bounds.
 */
static HRESULT
D3DTR_UpdateCachedDestination(D3DContext *d3dc, D3DSDOps *dstOps,
                              GlyphInfo *ginfo,
                              jint gx1, jint gy1, jint gx2, jint gy2,
                              jint agx1, jint agx2,
                              jint glyphIndex, jint totalGlyphs)
{
    jint dx1, dy1, dx2, dy2;
    D3DResource *pCachedDestTexRes;
    IDirect3DSurface9 *pCachedDestSurface, *pDst;
    HRESULT res = S_OK;

    if (isCachedDestValid && INSIDE(gx1, gy1, gx2, gy2, cachedDestBounds)) {
        // glyph is already within the cached destination bounds; no need
        // to read back the entire destination region again, but we do
        // need to see if the current glyph overlaps the previous glyph...

        // only use the "adjusted" glyph bounds when checking against
        // previous glyph's bounds
        gx1 = agx1;
        gx2 = agx2;

        if (INTERSECTS(gx1, gy1, gx2, gy2, previousGlyphBounds)) {
            // the current glyph overlaps the destination region touched
            // by the previous glyph, so now we need to read back the part
            // of the destination corresponding to the previous glyph
            dx1 = previousGlyphBounds.x1;
            dy1 = previousGlyphBounds.y1;
            dx2 = previousGlyphBounds.x2;
            dy2 = previousGlyphBounds.y2;

            // REMIND: make sure we flush any pending primitives that are
            // dependent on the current contents of the cached dest
            d3dc->FlushVertexQueue();

            RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);
            RETURN_STATUS_IF_NULL(pDst = dstOps->pResource->GetSurface(),
                                  E_FAIL);
            res = d3dc->GetResourceManager()->
                GetCachedDestTexture(dstOps->pResource->GetDesc()->Format,
                                     &pCachedDestTexRes);
            RETURN_STATUS_IF_FAILED(res);
            pCachedDestSurface = pCachedDestTexRes->GetSurface();

            // now dxy12 represent the "desired" destination bounds, but the
            // StretchRect() call may fail if these fall outside the actual
            // surface bounds; therefore, we use cxy12 to represent the
            // clamped bounds, and dxy12 are saved for later
            jint cx1 = (dx1 < 0) ? 0 : dx1;
            jint cy1 = (dy1 < 0) ? 0 : dy1;
            jint cx2 = (dx2 > dstOps->width)  ? dstOps->width  : dx2;
            jint cy2 = (dy2 > dstOps->height) ? dstOps->height : dy2;

            if (cx2 > cx1 && cy2 > cy1) {
                // copy destination into subregion of cached texture tile
                //   cx1-cachedDestBounds.x1 == +xoffset from left of texture
                //   cy1-cachedDestBounds.y1 == +yoffset from top of texture
                //   cx2-cachedDestBounds.x1 == +xoffset from left of texture
                //   cy2-cachedDestBounds.y1 == +yoffset from top of texture
                jint cdx1 = cx1-cachedDestBounds.x1;
                jint cdy1 = cy1-cachedDestBounds.y1;
                jint cdx2 = cx2-cachedDestBounds.x1;
                jint cdy2 = cy2-cachedDestBounds.y1;
                RECT srcRect = {  cx1,  cy1,  cx2,  cy2 };
                RECT dstRect = { cdx1, cdy1, cdx2, cdy2 };

                IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
                res = pd3dDevice->StretchRect(pDst, &srcRect,
                                              pCachedDestSurface, &dstRect,
                                              D3DTEXF_NONE);
            }
        }
    } else {
        // destination region is not valid, so we need to read back a
        // chunk of the destination into our cached texture

        // position the upper-left corner of the destination region on the
        // "top" line of glyph list
        // REMIND: this isn't ideal; it would be better if we had some idea
        //         of the bounding box of the whole glyph list (this is
        //         do-able, but would require iterating through the whole
        //         list up front, which may present its own problems)
        dx1 = gx1;
        dy1 = gy1;

        jint remainingWidth;
        if (ginfo->advanceX > 0) {
            // estimate the width based on our current position in the glyph
            // list and using the x advance of the current glyph (this is just
            // a quick and dirty heuristic; if this is a "thin" glyph image,
            // then we're likely to underestimate, and if it's "thick" then we
            // may end up reading back more than we need to)
            remainingWidth =
                (jint)(ginfo->advanceX * (totalGlyphs - glyphIndex));
            if (remainingWidth > D3DTR_CACHED_DEST_WIDTH) {
                remainingWidth = D3DTR_CACHED_DEST_WIDTH;
            } else if (remainingWidth < ginfo->width) {
                // in some cases, the x-advance may be slightly smaller
                // than the actual width of the glyph; if so, adjust our
                // estimate so that we can accommodate the entire glyph
                remainingWidth = ginfo->width;
            }
        } else {
            // a negative advance is possible when rendering rotated text,
            // in which case it is difficult to estimate an appropriate
            // region for readback, so we will pick a region that
            // encompasses just the current glyph
            remainingWidth = ginfo->width;
        }
        dx2 = dx1 + remainingWidth;

        // estimate the height (this is another sloppy heuristic; we'll
        // make the cached destination region tall enough to encompass most
        // glyphs that are small enough to fit in the glyph cache, and then
        // we add a little something extra to account for descenders
        dy2 = dy1 + D3DTR_CACHE_CELL_HEIGHT + 2;

        // REMIND: make sure we flush any pending primitives that are
        // dependent on the current contents of the cached dest
        d3dc->FlushVertexQueue();

        RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);
        RETURN_STATUS_IF_NULL(pDst = dstOps->pResource->GetSurface(), E_FAIL);
        res = d3dc->GetResourceManager()->
            GetCachedDestTexture(dstOps->pResource->GetDesc()->Format,
                                 &pCachedDestTexRes);
        RETURN_STATUS_IF_FAILED(res);
        pCachedDestSurface = pCachedDestTexRes->GetSurface();

        // now dxy12 represent the "desired" destination bounds, but the
        // StretchRect() call may fail if these fall outside the actual
        // surface bounds; therefore, we use cxy12 to represent the
        // clamped bounds, and dxy12 are saved for later
        jint cx1 = (dx1 < 0) ? 0 : dx1;
        jint cy1 = (dy1 < 0) ? 0 : dy1;
        jint cx2 = (dx2 > dstOps->width)  ? dstOps->width  : dx2;
        jint cy2 = (dy2 > dstOps->height) ? dstOps->height : dy2;

        if (cx2 > cx1 && cy2 > cy1) {
            // copy destination into cached texture tile (the upper-left
            // corner of the destination region will be positioned at the
            // upper-left corner (0,0) of the texture)
            RECT srcRect = { cx1, cy1, cx2, cy2 };
            RECT dstRect = { cx1-dx1, cy1-dy1, cx2-dx1, cy2-dy1 };

            IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
            res = pd3dDevice->StretchRect(pDst, &srcRect,
                                          pCachedDestSurface, &dstRect,
                                          D3DTEXF_NONE);
        }

        // update the cached bounds and mark it valid
        cachedDestBounds.x1 = dx1;
        cachedDestBounds.y1 = dy1;
        cachedDestBounds.x2 = dx2;
        cachedDestBounds.y2 = dy2;
        isCachedDestValid = JNI_TRUE;
    }

    // always update the previous glyph bounds
    previousGlyphBounds.x1 = gx1;
    previousGlyphBounds.y1 = gy1;
    previousGlyphBounds.x2 = gx2;
    previousGlyphBounds.y2 = gy2;

    return res;
}

static HRESULT
D3DTR_DrawLCDGlyphViaCache(D3DContext *d3dc, D3DSDOps *dstOps,
                           GlyphInfo *ginfo, jint x, jint y,
                           jint glyphIndex, jint totalGlyphs,
                           jboolean rgbOrder, jint contrast)
{
    HRESULT res;
    D3DGlyphCache *pLCDGCache;
    CacheCellInfo *cell;
    GlyphCacheInfo *gcache;
    jint dx1, dy1, dx2, dy2;
    jfloat dtx1, dty1, dtx2, dty2;

    J2dTraceLn(J2D_TRACE_VERBOSE, "D3DTR_DrawLCDGlyphViaCache");

    // the glyph cache is initialized before this method is called
    pLCDGCache = d3dc->GetLCDGlyphCache();

    if (glyphMode != MODE_USE_CACHE_LCD) {
        D3DTR_DisableGlyphModeState(d3dc);

        res = d3dc->BeginScene(STATE_TEXTUREOP);
        RETURN_STATUS_IF_FAILED(res);

        pLCDGCache->CheckGlyphCacheByteOrder(rgbOrder);

        res = D3DTR_EnableLCDGlyphModeState(d3dc, dstOps, JNI_TRUE, contrast);
        RETURN_STATUS_IF_FAILED(res);

        glyphMode = MODE_USE_CACHE_LCD;
    }

    gcache = pLCDGCache->GetGlyphCache();
    cell = AccelGlyphCache_GetCellInfoForCache(ginfo, gcache);
    if (cell == NULL) {
        // attempt to add glyph to accelerated glyph cache
        res = pLCDGCache->AddGlyph(ginfo);
        RETURN_STATUS_IF_FAILED(res);

        // we'll just no-op in the rare case that the cell is NULL
        cell = AccelGlyphCache_GetCellInfoForCache(ginfo, gcache);
        RETURN_STATUS_IF_NULL(cell, E_FAIL);
    }

    cell->timesRendered++;

    // location of the glyph in the destination's coordinate space
    dx1 = x;
    dy1 = y;
    dx2 = dx1 + ginfo->width;
    dy2 = dy1 + ginfo->height;

    // copy destination into second cached texture, if necessary
    D3DTR_UpdateCachedDestination(d3dc,
                                  dstOps, ginfo,
                                  dx1, dy1,
                                  dx2, dy2,
                                  dx1 + cell->leftOff,  // adjusted dx1
                                  dx2 + cell->rightOff, // adjusted dx2
                                  glyphIndex, totalGlyphs);

    // texture coordinates of the destination tile
    dtx1 = ((jfloat)(dx1 - cachedDestBounds.x1)) / D3DTR_CACHED_DEST_WIDTH;
    dty1 = ((jfloat)(dy1 - cachedDestBounds.y1)) / D3DTR_CACHED_DEST_HEIGHT;
    dtx2 = ((jfloat)(dx2 - cachedDestBounds.x1)) / D3DTR_CACHED_DEST_WIDTH;
    dty2 = ((jfloat)(dy2 - cachedDestBounds.y1)) / D3DTR_CACHED_DEST_HEIGHT;

    // render composed texture to the destination surface
    return d3dc->pVCacher->DrawTexture((jfloat)dx1, (jfloat)dy1,
                                       (jfloat)dx2, (jfloat)dy2,
                                        cell->tx1, cell->ty1,
                                        cell->tx2, cell->ty2,
                                        dtx1, dty1, dtx2, dty2);
}

static HRESULT
D3DTR_DrawGrayscaleGlyphNoCache(D3DContext *d3dc,
                                GlyphInfo *ginfo, jint x, jint y)
{
    jint tw, th;
    jint sx, sy, sw, sh;
    jint x0;
    jint w = ginfo->width;
    jint h = ginfo->height;
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_VERBOSE, "D3DTR_DrawGrayscaleGlyphNoCache");

    if (glyphMode != MODE_NO_CACHE_GRAY) {
        D3DTR_DisableGlyphModeState(d3dc);

        res = d3dc->BeginScene(STATE_MASKOP);
        RETURN_STATUS_IF_FAILED(res);

        glyphMode = MODE_NO_CACHE_GRAY;
    }

    x0 = x;
    tw = D3D_MASK_CACHE_TILE_WIDTH;
    th = D3D_MASK_CACHE_TILE_HEIGHT;

    for (sy = 0; sy < h; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > h) ? (h - sy) : th;

        for (sx = 0; sx < w; sx += tw, x += tw) {
            sw = ((sx + tw) > w) ? (w - sx) : tw;

            res = d3dc->GetMaskCache()->AddMaskQuad(sx, sy, x, y, sw, sh,
                                                    w, ginfo->image);
        }
    }

    return res;
}

static HRESULT
D3DTR_DrawLCDGlyphNoCache(D3DContext *d3dc, D3DSDOps *dstOps,
                          GlyphInfo *ginfo, jint x, jint y,
                          jint rowBytesOffset,
                          jboolean rgbOrder, jint contrast)
{
    jfloat tx1, ty1, tx2, ty2;
    jfloat dx1, dy1, dx2, dy2;
    jfloat dtx1, dty1, dtx2, dty2;
    jint tw, th;
    jint sx, sy, sw, sh;
    jint cx1, cy1, cx2, cy2;
    jint x0;
    jint w = ginfo->width;
    jint h = ginfo->height;
    TileFormat tileFormat = rgbOrder ? TILEFMT_3BYTE_RGB : TILEFMT_3BYTE_BGR;

    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    D3DResource *pBlitTextureRes, *pCachedDestTextureRes;
    IDirect3DTexture9 *pBlitTexture;
    IDirect3DSurface9 *pCachedDestSurface, *pDst;
    HRESULT res;

    J2dTraceLn(J2D_TRACE_VERBOSE, "D3DTR_DrawLCDGlyphNoCache");

    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(pDst = dstOps->pResource->GetSurface(), E_FAIL);

    res = d3dc->GetResourceManager()->GetBlitTexture(&pBlitTextureRes);
    RETURN_STATUS_IF_FAILED(res);

    res = d3dc->GetResourceManager()->
        GetCachedDestTexture(dstOps->pResource->GetDesc()->Format,
                             &pCachedDestTextureRes);
    RETURN_STATUS_IF_FAILED(res);

    pBlitTexture = pBlitTextureRes->GetTexture();
    pCachedDestSurface = pCachedDestTextureRes->GetSurface();

    if (glyphMode != MODE_NO_CACHE_LCD) {
        D3DTR_DisableGlyphModeState(d3dc);

        res = d3dc->BeginScene(STATE_TEXTUREOP);
        RETURN_STATUS_IF_FAILED(res);
        res = D3DTR_EnableLCDGlyphModeState(d3dc,dstOps, JNI_FALSE, contrast);
        RETURN_STATUS_IF_FAILED(res);

        glyphMode = MODE_NO_CACHE_LCD;
    }

    x0 = x;
    tx1 = 0.0f;
    ty1 = 0.0f;
    dtx1 = 0.0f;
    dty1 = 0.0f;
    tw = D3DTR_NOCACHE_TILE_SIZE;
    th = D3DTR_NOCACHE_TILE_SIZE;

    for (sy = 0; sy < h; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > h) ? (h - sy) : th;

        for (sx = 0; sx < w; sx += tw, x += tw) {
            sw = ((sx + tw) > w) ? (w - sx) : tw;

            // calculate the bounds of the tile to be copied from the
            // destination into the cached tile
            cx1 = x;
            cy1 = y;
            cx2 = cx1 + sw;
            cy2 = cy1 + sh;

            // need to clamp to the destination bounds, otherwise the
            // StretchRect() call may fail
            if (cx1 < 0)              cx1 = 0;
            if (cy1 < 0)              cy1 = 0;
            if (cx2 > dstOps->width)  cx2 = dstOps->width;
            if (cy2 > dstOps->height) cy2 = dstOps->height;

            if (cx2 > cx1 && cy2 > cy1) {
                // copy LCD mask into glyph texture tile
                d3dc->UploadTileToTexture(pBlitTextureRes,
                                          ginfo->image+rowBytesOffset,
                                          0, 0, sx, sy, sw, sh,
                                          ginfo->rowBytes, tileFormat);

                // update the lower-right glyph texture coordinates
                tx2 = ((jfloat)sw) / D3DC_BLIT_TILE_SIZE;
                ty2 = ((jfloat)sh) / D3DC_BLIT_TILE_SIZE;

                // calculate the actual destination vertices
                dx1 = (jfloat)x;
                dy1 = (jfloat)y;
                dx2 = dx1 + sw;
                dy2 = dy1 + sh;

                // copy destination into cached texture tile (the upper-left
                // corner of the destination region will be positioned at the
                // upper-left corner (0,0) of the texture)
                RECT srcRect = { cx1, cy1, cx2, cy2 };
                RECT dstRect = { cx1-x, cy1-y, cx2-x, cy2-y };
                pd3dDevice->StretchRect(pDst, &srcRect,
                                        pCachedDestSurface,
                                        &dstRect,
                                        D3DTEXF_NONE);

                // update the remaining destination texture coordinates
                dtx2 = ((jfloat)sw) / D3DTR_CACHED_DEST_WIDTH;
                dty2 = ((jfloat)sh) / D3DTR_CACHED_DEST_HEIGHT;

                // render composed texture to the destination surface
                res = d3dc->pVCacher->DrawTexture( dx1,  dy1,  dx2,  dy2,
                                                   tx1,  ty1,  tx2,  ty2,
                                                   dtx1, dty1, dtx2, dty2);

                // unfortunately we need to flush after each tile
                d3dc->FlushVertexQueue();
            }
        }
    }

    return res;
}

// see DrawGlyphList.c for more on this macro...
#define FLOOR_ASSIGN(l, r) \
    if ((r)<0) (l) = ((int)floor(r)); else (l) = ((int)(r))

HRESULT
D3DTR_DrawGlyphList(D3DContext *d3dc, D3DSDOps *dstOps,
                    jint totalGlyphs, jboolean usePositions,
                    jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
                    jfloat glyphListOrigX, jfloat glyphListOrigY,
                    unsigned char *images, unsigned char *positions)
{
    int glyphCounter;
    HRESULT res = S_OK;
    J2dTraceLn(J2D_TRACE_INFO, "D3DTR_DrawGlyphList");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc->Get3DDevice(), E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    RETURN_STATUS_IF_NULL(images, E_FAIL);
    if (usePositions) {
        RETURN_STATUS_IF_NULL(positions, E_FAIL);
    }

    glyphMode = MODE_NOT_INITED;
    isCachedDestValid = JNI_FALSE;

    for (glyphCounter = 0; glyphCounter < totalGlyphs; glyphCounter++) {
        jint x, y;
        jfloat glyphx, glyphy;
        jboolean grayscale;
        GlyphInfo *ginfo = (GlyphInfo *)jlong_to_ptr(NEXT_LONG(images));

        if (ginfo == NULL) {
            // this shouldn't happen, but if it does we'll just break out...
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "D3DTR_DrawGlyphList: glyph info is null");
            break;
        }

        grayscale = (ginfo->rowBytes == ginfo->width);

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
            continue;
        }

        if (grayscale) {
            // grayscale or monochrome glyph data
            if (ginfo->width <= D3DTR_CACHE_CELL_WIDTH &&
                ginfo->height <= D3DTR_CACHE_CELL_HEIGHT &&
                SUCCEEDED(d3dc->InitGrayscaleGlyphCache()))
            {
                res = D3DTR_DrawGrayscaleGlyphViaCache(d3dc, ginfo, x, y);
            } else {
                res = D3DTR_DrawGrayscaleGlyphNoCache(d3dc, ginfo, x, y);
            }
        } else {
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
                ginfo->width <= D3DTR_CACHE_CELL_WIDTH &&
                ginfo->height <= D3DTR_CACHE_CELL_HEIGHT &&
                SUCCEEDED(d3dc->InitLCDGlyphCache()))
            {
                res = D3DTR_DrawLCDGlyphViaCache(d3dc, dstOps,
                                                 ginfo, x, y,
                                                 glyphCounter, totalGlyphs,
                                                 rgbOrder, lcdContrast);
            } else {
                res = D3DTR_DrawLCDGlyphNoCache(d3dc, dstOps,
                                                ginfo, x, y,
                                                rowBytesOffset,
                                                rgbOrder, lcdContrast);
            }
        }

        if (FAILED(res)) {
            break;
        }
    }

    D3DTR_DisableGlyphModeState(d3dc);
    return res;
}

JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DTextRenderer_drawGlyphList
    (JNIEnv *env, jobject self,
     jint numGlyphs, jboolean usePositions,
     jboolean subPixPos, jboolean rgbOrder, jint lcdContrast,
     jfloat glyphListOrigX, jfloat glyphListOrigY,
     jlongArray imgArray, jfloatArray posArray)
{
    unsigned char *images;

    J2dTraceLn(J2D_TRACE_INFO, "D3DTextRenderer_drawGlyphList");

    images = (unsigned char *)
        env->GetPrimitiveArrayCritical(imgArray, NULL);
    if (images != NULL) {
        D3DContext *d3dc = D3DRQ_GetCurrentContext();
        D3DSDOps *dstOps = D3DRQ_GetCurrentDestination();

        if (usePositions) {
            unsigned char *positions = (unsigned char *)
                env->GetPrimitiveArrayCritical(posArray, NULL);
            if (positions != NULL) {
                D3DTR_DrawGlyphList(d3dc, dstOps,
                                    numGlyphs, usePositions,
                                    subPixPos, rgbOrder, lcdContrast,
                                    glyphListOrigX, glyphListOrigY,
                                    images, positions);
                env->ReleasePrimitiveArrayCritical(posArray,
                                                   positions, JNI_ABORT);
            }
        } else {
            D3DTR_DrawGlyphList(d3dc, dstOps,
                                numGlyphs, usePositions,
                                subPixPos, rgbOrder, lcdContrast,
                                glyphListOrigX, glyphListOrigY,
                                images, NULL);
        }

        // reset current state, and ensure rendering is flushed to dest
        if (d3dc != NULL) {
            d3dc->FlushVertexQueue();
        }

        env->ReleasePrimitiveArrayCritical(imgArray,
                                           images, JNI_ABORT);
    }
}
