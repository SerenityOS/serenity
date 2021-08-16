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

#include <jni.h>
#include "jlong.h"

#include "D3DPipeline.h"

#include "SurfaceData.h"
#include "D3DBlitLoops.h"
#include "D3DRenderQueue.h"
#include "D3DSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"

#include "IntArgb.h"
#include "IntArgbPre.h"
#include "IntRgb.h"
#include "IntBgr.h"
#include "Ushort555Rgb.h"
#include "Ushort565Rgb.h"
#include "ByteIndexed.h"


extern "C" BlitFunc IntArgbToIntArgbPreConvert;
extern "C" BlitFunc IntArgbPreToIntArgbConvert;
extern "C" BlitFunc IntArgbBmToIntArgbConvert;
extern "C" BlitFunc IntRgbToIntArgbConvert;
extern "C" BlitFunc ThreeByteBgrToIntArgbConvert;
extern "C" BlitFunc Ushort565RgbToIntArgbConvert;
extern "C" BlitFunc Ushort555RgbToIntArgbConvert;
extern "C" BlitFunc IntBgrToIntArgbConvert;
extern "C" BlitFunc AnyIntIsomorphicCopy;
extern "C" BlitFunc ByteIndexedToIntArgbConvert;
extern "C" BlitFunc ByteIndexedToIntArgbPreConvert;

#define GETMIN(v1, v2)    (((v1) > (t=(v2))) && ((v1) = t))
#define GETMAX(v1, v2)    (((v1) < (t=(v2))) && ((v1) = t))

#ifdef D3D_PPL_DLL

JNIEXPORT void JNICALL
SurfaceData_IntersectBounds(SurfaceDataBounds *dst, SurfaceDataBounds *src)
{
    int t;
    GETMAX(dst->x1, src->x1);
    GETMAX(dst->y1, src->y1);
    GETMIN(dst->x2, src->x2);
    GETMIN(dst->y2, src->y2);
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYXY(SurfaceDataBounds *bounds,
                                jint x1, jint y1, jint x2, jint y2)
{
    int t;
    GETMAX(bounds->x1, x1);
    GETMAX(bounds->y1, y1);
    GETMIN(bounds->x2, x2);
    GETMIN(bounds->y2, y2);
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBoundsXYWH(SurfaceDataBounds *bounds,
                                jint x, jint y, jint w, jint h)
{
    w = (w <= 0) ? x : x+w;
    if (w < x) {
        w = 0x7fffffff;
    }
    if (bounds->x1 < x) {
        bounds->x1 = x;
    }
    if (bounds->x2 > w) {
        bounds->x2 = w;
    }
    h = (h <= 0) ? y : y+h;
    if (h < y) {
        h = 0x7fffffff;
    }
    if (bounds->y1 < y) {
        bounds->y1 = y;
    }
    if (bounds->y2 > h) {
        bounds->y2 = h;
    }
}

JNIEXPORT void JNICALL
SurfaceData_IntersectBlitBounds(SurfaceDataBounds *src,
                                SurfaceDataBounds *dst,
                                jint dx, jint dy)
{
    int t;
    GETMAX(dst->x1, src->x1 + dx);
    GETMAX(dst->y1, src->y1 + dy);
    GETMIN(dst->x2, src->x2 + dx);
    GETMIN(dst->y2, src->y2 + dy);
    GETMAX(src->x1, dst->x1 - dx);
    GETMAX(src->y1, dst->y1 - dy);
    GETMIN(src->x2, dst->x2 - dx);
    GETMIN(src->y2, dst->y2 - dy);
}

#endif /* D3D_PPL_DLL */

D3DPIPELINE_API HRESULT
D3DBL_CopySurfaceToIntArgbImage(IDirect3DSurface9 *pSurface,
                                SurfaceDataRasInfo *pDstInfo,
                                jint srcx, jint srcy,
                                jint srcWidth, jint srcHeight,
                                jint dstx, jint dsty)
{
    HRESULT res = S_OK;
    D3DLOCKED_RECT lockedRect;
    RECT r = { srcx, srcy, srcx+srcWidth, srcy+srcHeight };
    D3DSURFACE_DESC desc;
    SurfaceDataRasInfo srcInfo;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBL_CopySurfaceToIntArgbImage");
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                " rect={%-4d, %-4d, %-4d, %-4d}",
                r.left, r.top, r.right, r.bottom);

    res = pSurface->LockRect(&lockedRect, &r, D3DLOCK_NOSYSLOCK);
    RETURN_STATUS_IF_FAILED(res);
    pSurface->GetDesc(&desc);

    ZeroMemory(&srcInfo, sizeof(SurfaceDataRasInfo));
    // srcInfo.bounds.x1 = 0;
    // srcInfo.bounds.y1 = 0;
    srcInfo.bounds.x2 = srcWidth;
    srcInfo.bounds.y2 = srcHeight;
    srcInfo.scanStride = lockedRect.Pitch;

    void *pSrcBase = lockedRect.pBits;
    void *pDstBase = PtrCoord(pDstInfo->rasBase,
                              dstx, pDstInfo->pixelStride,
                              dsty, pDstInfo->scanStride);

    switch (desc.Format) {
        case D3DFMT_A8R8G8B8:
            srcInfo.pixelStride = 4;
            IntArgbPreToIntArgbConvert(pSrcBase, pDstBase,
                                       srcWidth, srcHeight,
                                       &srcInfo, pDstInfo, NULL, NULL);
            break;
        case D3DFMT_X8R8G8B8:
            srcInfo.pixelStride = 4;
            IntRgbToIntArgbConvert(pSrcBase, pDstBase,
                                   srcWidth, srcHeight,
                                   &srcInfo, pDstInfo, NULL, NULL);
            break;
        case D3DFMT_X8B8G8R8:
            srcInfo.pixelStride = 4;
            IntBgrToIntArgbConvert(pSrcBase, pDstBase,
                                   srcWidth, srcHeight,
                                   &srcInfo, pDstInfo, NULL, NULL);
            break;
        case D3DFMT_X1R5G5B5:
            srcInfo.pixelStride = 2;
            Ushort555RgbToIntArgbConvert(pSrcBase, pDstBase,
                                         srcWidth, srcHeight,
                                         &srcInfo, pDstInfo, NULL, NULL);
            break;
        case D3DFMT_R5G6B5:
            srcInfo.pixelStride = 2;
            Ushort565RgbToIntArgbConvert(pSrcBase, pDstBase,
                                         srcWidth, srcHeight,
                                         &srcInfo, pDstInfo, NULL, NULL);
            break;
        default:
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                "D3DBL_CopySurfaceToIntArgbImage: unknown format %d",
                desc.Format);
    }

    return pSurface->UnlockRect();
}

D3DPIPELINE_API HRESULT
D3DBL_CopyImageToIntXrgbSurface(SurfaceDataRasInfo *pSrcInfo,
                                int srctype,
                                D3DResource *pDstSurfaceRes,
                                jint srcx, jint srcy,
                                jint srcWidth, jint srcHeight,
                                jint dstx, jint dsty)
{
    HRESULT res = S_OK;
    D3DLOCKED_RECT lockedRect;
    RECT r = { dstx, dsty, dstx+srcWidth, dsty+srcHeight };
    RECT *pR = &r;
    SurfaceDataRasInfo dstInfo;
    IDirect3DSurface9 *pDstSurface = pDstSurfaceRes->GetSurface();
    D3DSURFACE_DESC *pDesc = pDstSurfaceRes->GetDesc();
    DWORD dwLockFlags = D3DLOCK_NOSYSLOCK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBL_CopyImageToIntXrgbSurface");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                " srctype=%d rect={%-4d, %-4d, %-4d, %-4d}",
                srctype, r.left, r.top, r.right, r.bottom);

    if (pDesc->Usage == D3DUSAGE_DYNAMIC) {
        // it is safe to lock with discard because we don't care about the
        // contents of dynamic textures, and some drivers are happier if
        // dynamic textures are always locked with DISCARD
        dwLockFlags |= D3DLOCK_DISCARD;
        pR = NULL;
    } else {
        // in non-DYNAMIC case we lock the exact rect so there's no need to
        // offset the destination pointer
        dstx = 0;
        dsty = 0;
    }

    res = pDstSurface->LockRect(&lockedRect, pR, dwLockFlags);
    RETURN_STATUS_IF_FAILED(res);

    ZeroMemory(&dstInfo, sizeof(SurfaceDataRasInfo));
    // dstInfo.bounds.x1 = 0;
    // dstInfo.bounds.y1 = 0;
    dstInfo.bounds.x2 = srcWidth;
    dstInfo.bounds.y2 = srcHeight;
    dstInfo.scanStride = lockedRect.Pitch;
    dstInfo.pixelStride = 4;

    void *pSrcBase = PtrCoord(pSrcInfo->rasBase,
                              srcx, pSrcInfo->pixelStride,
                              srcy, pSrcInfo->scanStride);
    void *pDstBase = PtrCoord(lockedRect.pBits,
                              dstx, dstInfo.pixelStride,
                              dsty, dstInfo.scanStride);

    switch (srctype) {
        case ST_INT_ARGB:
            IntArgbToIntArgbPreConvert(pSrcBase, pDstBase,
                                       srcWidth, srcHeight,
                                       pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_INT_ARGB_PRE:
            AnyIntIsomorphicCopy(pSrcBase, pDstBase,
                                 srcWidth, srcHeight,
                                 pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_INT_RGB:
            IntRgbToIntArgbConvert(pSrcBase, pDstBase,
                                   srcWidth, srcHeight,
                                   pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_INT_ARGB_BM:
            // REMIND: we don't have such sw loop
            // so this path is disabled for now on java level
//            IntArgbBmToIntArgbPreConvert(pSrcBase, pDstBase,
//                                         srcWidth, srcHeight,
//                                         pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_INT_BGR:
            IntBgrToIntArgbConvert(pSrcBase, pDstBase,
                                   srcWidth, srcHeight,
                                   pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_3BYTE_BGR:
            ThreeByteBgrToIntArgbConvert(pSrcBase, pDstBase,
                                         srcWidth, srcHeight,
                                         pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_USHORT_555_RGB:
            Ushort555RgbToIntArgbConvert(pSrcBase, pDstBase,
                                         srcWidth, srcHeight,
                                         pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_USHORT_565_RGB:
            Ushort565RgbToIntArgbConvert(pSrcBase, pDstBase,
                                         srcWidth, srcHeight,
                                         pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_BYTE_INDEXED:
            ByteIndexedToIntArgbPreConvert(pSrcBase, pDstBase,
                                           srcWidth, srcHeight,
                                           pSrcInfo, &dstInfo, NULL, NULL);
            break;
        case ST_BYTE_INDEXED_BM:
            // REMIND: we don't have such sw loop
            // so this path is disabled for now on java level
//            ByteIndexedBmToIntArgbPreConvert(pSrcBase, pDstBase,
//                                             srcWidth, srcHeight,
//                                             pSrcInfo, &dstInfo, NULL, NULL);
            break;
        default:
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                           "D3DBL_CopyImageToIntXrgbSurface: unknown type %d",
                           srctype);
    }

    return pDstSurface->UnlockRect();
}

/**
 * Inner loop used for copying a source "render-to" D3D "Surface" to a
 * destination D3D "Surface".  Note that the same surface can
 * not be used as both the source and destination, as is the case in a copyArea()
 * operation.  This method is invoked from D3DBlitLoops_IsoBlit().
 *
 * The standard StretchRect() mechanism is used to copy the source region
 * into the destination region.  If the regions have different dimensions,
 * the source will be scaled into the destination as appropriate (only
 * nearest neighbor filtering will be applied for simple scale operations).
 */
HRESULT
D3DBlitSurfaceToSurface(D3DContext *d3dc, D3DSDOps *srcOps, D3DSDOps *dstOps,
                        D3DTEXTUREFILTERTYPE hint,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        jint dx1, jint dy1, jint dx2, jint dy2)
{
    IDirect3DSurface9 *pSrc, *pDst;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitSurfaceToSurface");

    RETURN_STATUS_IF_NULL(srcOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(pSrc = srcOps->pResource->GetSurface(), E_FAIL);
    RETURN_STATUS_IF_NULL(pDst = dstOps->pResource->GetSurface(), E_FAIL);

    d3dc->UpdateState(STATE_OTHEROP);
    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();

    // need to clip the destination bounds,
    // otherwise StretchRect could fail
    jint sw    = sx2 - sx1;
    jint sh    = sy2 - sy1;
    jdouble dw = dx2 - dx1;
    jdouble dh = dy2 - dy1;

    SurfaceDataBounds dstBounds;
    dstBounds.x1 = dx1;
    dstBounds.y1 = dy1;
    dstBounds.x2 = dx2;
    dstBounds.y2 = dy2;
    SurfaceData_IntersectBoundsXYXY(&dstBounds, 0, 0,
                                    dstOps->width, dstOps->height);
    if (d3dc->GetClipType() == CLIP_RECT) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  rect clip, clip dest manually");
        RECT clipRect;
        pd3dDevice->GetScissorRect(&clipRect);
        SurfaceData_IntersectBoundsXYXY(&dstBounds,
                                        clipRect.left, clipRect.top,
                                        clipRect.right, clipRect.bottom);
    }

    if (dstBounds.x1 != dx1) {
        sx1 += (int)((dstBounds.x1 - dx1) * (sw / dw));
    }
    if (dstBounds.y1 != dy1) {
        sy1 += (int)((dstBounds.y1 - dy1) * (sh / dh));
    }
    if (dstBounds.x2 != dx2) {
        sx2 += (int)((dstBounds.x2 - dx2) * (sw / dw));
    }
    if (dstBounds.y2 != dy2) {
        sy2 += (int)((dstBounds.y2 - dy2) * (sh / dh));
    }

    // check if the rects are empty (StretchRect will fail if so)
    if (dstBounds.x1 >= dstBounds.x2 || dstBounds.y1 >= dstBounds.y2 ||
        sx1 >= sx2 || sy1 >= sy2)
    {
        return S_OK;
    }

    RECT srcRect = { sx1, sy1, sx2, sy2 };
    RECT dstRect = { dstBounds.x1, dstBounds.y1, dstBounds.x2, dstBounds.y2 };

    return pd3dDevice->StretchRect(pSrc, &srcRect, pDst, &dstRect, hint);
}

/**
 * A convenience method for issuing DrawTexture calls depending on the
 * hint. See detailed explanation below.
 */
static inline HRESULT
D3DDrawTextureWithHint(D3DContext *d3dc, D3DTEXTUREFILTERTYPE hint,
                       jint srcWidth, jint srcHeight,
                       float tw, float th,
                       jint sx1, jint sy1, jint sx2, jint sy2,
                       float dx1, float dy1, float dx2, float dy2,
                       float tx1, float ty1, float tx2, float ty2)
{
    HRESULT res;

    if (hint == D3DTEXF_LINEAR &&
        (srcWidth != tw  || srcHeight != th ||
         srcWidth != sx2 || srcHeight != sy2 ))
    {
        /*
         * When the image bounds are smaller than the bounds of the
         * texture that the image resides in, D3DTEXF_LINEAR will use pixels
         * from outside the valid image bounds, which could result in garbage
         * pixels showing up at the edges of the transformed result.  We set
         * the texture wrap mode to D3DTADDRESS_CLAMP, which solves the problem
         * for the top and left edges.  But when the source bounds do not
         * match the texture bounds, we need to perform this as a four-part
         * operation in order to prevent the filter used by D3D from using
         * invalid pixels at the bottom and right edges.
         *
         * Note that we only need to apply this technique when the source
         * bounds are equal to the actual image bounds.  If the source bounds
         * fall within the image bounds there is no need to apply this hack
         * because the filter used by D3D will access valid pixels.
         * Likewise, if the image bounds are equal to the texture bounds,
         * then the edge conditions are handled properly by D3DTADDRESS_CLAMP.
         */

        // These values represent the bottom-right corner of source texture
        // region pulled in by 1/2 of a source texel.
        float tx2adj = tx2 - (1.0f / (2.0f * tw));
        float ty2adj = ty2 - (1.0f / (2.0f * th));

        // These values represent the above coordinates pulled in by a
        // tiny fraction.  As an example, if we sample the tiny area from
        // tx2adj2 to tx2adj, the result should be the solid color at the
        // texel center corresponding to tx2adj.
        float tx2adj2 = tx2adj - 0.0001f;
        float ty2adj2 = ty2adj - 0.0001f;

        // These values represent the bottom-right corner of the destination
        // region pulled in by 1/2 of a destination pixel.
        float dx2adj = dx2 - 0.5f;
        float dy2adj = dy2 - 0.5f;

        // First, render a majority of the source texture, from the top-left
        // corner to the bottom-right, but not including the right or bottom
        // edges.
        d3dc->pVCacher->DrawTexture(dx1, dy1, dx2adj, dy2adj,
                                    tx1, ty1, tx2adj, ty2adj);

        // Second, render the remaining sliver on the right edge.
        d3dc->pVCacher->DrawTexture(dx2adj, dy1, dx2, dy2adj,
                                    tx2adj2, ty1, tx2adj, ty2adj);

        // Third, render the remaining sliver on the bottom edge.
        d3dc->pVCacher->DrawTexture(dx1, dy2adj, dx2adj, dy2,
                                    tx1, ty2adj2, tx2adj, ty2adj);

        // Finally, render the remaining speck at the bottom-right corner.
        res = d3dc->pVCacher->DrawTexture(dx2adj, dy2adj, dx2, dy2,
                                          tx2adj2, ty2adj2, tx2adj, ty2adj);
    } else {
        /*
         * As mentioned above, we can issue a simple textured quad if:
         *   - the hint is D3DTEXF_POINT or
         *   - the source bounds are sufficiently inside the texture bounds or
         *   - the image bounds are equal to the texture bounds (as is the
         *     case when the image has power-of-two dimensions, or when the
         *     device supports non-pow2 textures)
         */
        res =  d3dc->pVCacher->DrawTexture(dx1, dy1, dx2, dy2,
                                           tx1, ty1, tx2, ty2);
    }
    return res;
}

/**
 * Inner loop used for copying a source D3D "Texture" to a destination
 * D3D "Surface".  This method is invoked from D3DBlitLoops_IsoBlit().
 *
 * This method will copy, scale, or transform the source texture into the
 * destination depending on the transform state, as established in
 * and D3DContext::SetTransform().  If the source texture is
 * transformed in any way when rendered into the destination, the filtering
 * method applied is determined by the hint parameter.
 */
static HRESULT
D3DBlitTextureToSurface(D3DContext *d3dc,
                        D3DSDOps *srcOps, D3DSDOps *dstOps,
                        jboolean rtt, D3DTEXTUREFILTERTYPE hint,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        float dx1, float dy1, float dx2, float dy2)
{
    HRESULT res;
    IDirect3DTexture9 *pSrc;
    IDirect3DDevice9 *pd3dDevice;
    float tx1, ty1, tx2, ty2;
    float tw, th;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitTextureToSurface");

    RETURN_STATUS_IF_NULL(srcOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);

    pSrc = srcOps->pResource->GetTexture();
    RETURN_STATUS_IF_NULL(pSrc, E_FAIL);

    if (FAILED(res = d3dc->BeginScene(STATE_TEXTUREOP)   ||
        FAILED(res = d3dc->SetTexture(pSrc))))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "D3DBlitTextureToSurface: BeginScene or SetTexture failed");
        return res;
    }

    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, hint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, hint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);

    tw = (float)srcOps->pResource->GetDesc()->Width;
    th = (float)srcOps->pResource->GetDesc()->Height;

    // convert the source bounds into the range [0,1]
    tx1 = ((float)sx1) / tw;
    ty1 = ((float)sy1) / th;
    tx2 = ((float)sx2) / tw;
    ty2 = ((float)sy2) / th;

    return D3DDrawTextureWithHint(d3dc, hint,
                                  srcOps->width, srcOps->height,
                                  tw, th,
                                  sx1, sy1, sx2, sy2,
                                  dx1, dy1, dx2, dy2,
                                  tx1, ty1, tx2, ty2);
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface or
 * D3D "Surface" to a destination D3D "Surface", using an D3D texture
 * tile as an intermediate surface.  This method is invoked from
 * D3DBlitLoops_Blit() for "Sw" surfaces and D3DBlitLoops_IsoBlit() for
 * "Surface" surfaces.
 *
 * This method is used to transform the source surface into the destination.
 * Pixel rectangles cannot be arbitrarily transformed.  However, texture
 * mapped quads do respect the modelview transform matrix, so we use
 * textures here to perform the transform operation.  This method uses a
 * tile-based approach in which a small subregion of the source surface is
 * copied into a cached texture tile.  The texture tile is then mapped
 * into the appropriate location in the destination surface.
 *
 */
D3DPIPELINE_API HRESULT
D3DBlitToSurfaceViaTexture(D3DContext *d3dc, SurfaceDataRasInfo *srcInfo,
                           int srctype, D3DSDOps *srcOps,
                           jboolean swsurface, jint hint,
                           jint sx1, jint sy1, jint sx2, jint sy2,
                           jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    double tx1, ty1, tx2, ty2;
    double dx, dy, dw, dh, cdw, cdh;
    jint tw, th;
    jint sx, sy, sw, sh;
    HRESULT res = S_OK;
    D3DResource *pBlitTextureRes = NULL;
    IDirect3DTexture9 *pBlitTexture = NULL;
    IDirect3DSurface9 *pBlitSurface = NULL, *pSrc = NULL;
    D3DTEXTUREFILTERTYPE fhint =
            (hint == D3DSD_XFORM_BILINEAR) ? D3DTEXF_LINEAR : D3DTEXF_POINT;
    fhint = d3dc->IsTextureFilteringSupported(fhint) ? fhint : D3DTEXF_NONE;

    if (swsurface) {
        res = d3dc->GetResourceManager()->GetBlitTexture(&pBlitTextureRes);
    } else {
        RETURN_STATUS_IF_NULL(srcOps->pResource, E_FAIL);
        RETURN_STATUS_IF_NULL(pSrc = srcOps->pResource->GetSurface(), E_FAIL);

        res = d3dc->GetResourceManager()->
                GetBlitRTTexture(D3DC_BLIT_TILE_SIZE, D3DC_BLIT_TILE_SIZE,
                                 srcOps->pResource->GetDesc()->Format,
                                 &pBlitTextureRes);
    }
    if (FAILED(res)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DBlitToSurfaceViaTexture: could not init blit tile");
        return res;
    }
    pBlitSurface = pBlitTextureRes->GetSurface();
    pBlitTexture = pBlitTextureRes->GetTexture();

    D3DSURFACE_DESC *pDesc = pBlitTextureRes->GetDesc();

    tx1 = 0.0f;
    ty1 = 0.0f;
    tw = pDesc->Width;
    th = pDesc->Height;
    cdw = (dx2-dx1) / (((double)(sx2-sx1)) / tw);
    cdh = (dy2-dy1) / (((double)(sy2-sy1)) / th);

    res = d3dc->BeginScene(STATE_TEXTUREOP);
    RETURN_STATUS_IF_FAILED(res);
    res = d3dc->SetTexture(pBlitTexture);
    RETURN_STATUS_IF_FAILED(res);

    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, fhint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, fhint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);

    for (sy = sy1, dy = dy1; sy < sy2; sy += th, dy += cdh) {
        sh = ((sy + th) > sy2) ? (sy2 - sy) : th;
        dh = ((dy + cdh) > dy2) ? (dy2 - dy) : cdh;

        for (sx = sx1, dx = dx1; sx < sx2; sx += tw, dx += cdw) {
            sw = ((sx + tw) > sx2) ? (sx2 - sx) : tw;
            dw = ((dx + cdw) > dx2) ? (dx2 - dx) : cdw;

            tx2 = ((double)sw) / tw;
            ty2 = ((double)sh) / th;

            if (swsurface) {
                D3DBL_CopyImageToIntXrgbSurface(srcInfo,
                        srctype, pBlitTextureRes,
                        sx, sy, sw, sh,
                        0, 0);
            } else {
                RECT srcRect = { (LONG)sx, (LONG)sy,
                                 (LONG)(sx+dw), (LONG)(sy+dh) };
                RECT dstRect = { 0l, 0l, (LONG)dw, (LONG)dh };
                pd3dDevice->StretchRect(pSrc,
                                        &srcRect, pBlitSurface, &dstRect,
                                        D3DTEXF_NONE);
            }
            D3DDrawTextureWithHint(d3dc, fhint,
                   tw, th,
                   (float)tw, (float)th,
                   sx, sy, sw, sh,
                   (float)dx, (float)dy, (float)(dx+dw), (float)(dy+dh),
                   (float)tx1, (float)ty1, (float)tx2, (float)ty2);
            res = d3dc->pVCacher->Render();
        }
    }
    return res;
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface to a
 * destination D3D "Texture".  This method is invoked from
 * D3DBlitLoops_Blit().
 *
 * The source surface is effectively loaded into the D3D texture object,
 * which must have already been initialized by D3DSD_initTexture().  Note
 * that this method is only capable of copying the source surface into the
 * destination surface (i.e. no scaling or general transform is allowed).
 * This restriction should not be an issue as this method is only used
 * currently to cache a static system memory image into an D3D texture in
 * a hidden-acceleration situation.
 */
static HRESULT
D3DBlitSwToTexture(D3DContext *d3dc,
                   SurfaceDataRasInfo *srcInfo, int srctype,
                   D3DSDOps *dstOps,
                   jint sx1, jint sy1, jint sx2, jint sy2)
{
    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps->pResource->GetSurface(), E_FAIL);

    return D3DBL_CopyImageToIntXrgbSurface(srcInfo, srctype,
                                           dstOps->pResource,
                                           sx1, sy1, sx2-sx1, sy2-sy1,
                                           0, 0);
}

/**
 * General blit method for copying a native D3D surface (of type "Surface"
 * or "Texture") to another D3D "Surface".  If texture is JNI_TRUE, this
 * method will invoke the Texture->Surface inner loop; otherwise, one of the
 * Surface->Surface inner loops will be invoked, depending on the transform
 * state.
 */
D3DPIPELINE_API HRESULT
D3DBlitLoops_IsoBlit(JNIEnv *env,
                     D3DContext *d3dc, jlong pSrcOps, jlong pDstOps,
                     jboolean xform, jint hint,
                     jboolean texture, jboolean rtt,
                     jint sx1, jint sy1, jint sx2, jint sy2,
                     jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    D3DSDOps *srcOps = (D3DSDOps *)jlong_to_ptr(pSrcOps);
    D3DSDOps *dstOps = (D3DSDOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo;
    jint sw    = sx2 - sx1;
    jint sh    = sy2 - sy1;
    jdouble dw = dx2 - dx1;
    jdouble dh = dy2 - dy1;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitLoops_IsoBlit");

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "D3DBlitLoops_IsoBlit: invalid dimensions");
        return E_FAIL;
    }

    RETURN_STATUS_IF_NULL(srcOps, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc->Get3DDevice(), E_FAIL);

    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;

    SurfaceData_IntersectBoundsXYXY(&srcInfo.bounds,
                                    0, 0, srcOps->width, srcOps->height);


    HRESULT res = S_OK;
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

        D3DTEXTUREFILTERTYPE fhint =
            (hint == D3DSD_XFORM_BILINEAR) ? D3DTEXF_LINEAR : D3DTEXF_POINT;
        if (texture) {
            fhint = d3dc->IsTextureFilteringSupported(fhint) ?
                fhint : D3DTEXF_NONE;
            res = D3DBlitTextureToSurface(d3dc, srcOps, dstOps, rtt, fhint,
                                          sx1, sy1, sx2, sy2,
                                          (float)dx1, (float)dy1,
                                          (float)dx2, (float)dy2);
        } else {
            // StretchRect does not do compositing or clipping
            IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
            DWORD abEnabled = 0;

            pd3dDevice->GetRenderState(D3DRS_ALPHABLENDENABLE, &abEnabled);
            J2dTraceLn3(J2D_TRACE_VERBOSE, "  xform=%d clip=%d abEnabled=%d",
                        xform, d3dc->GetClipType(), abEnabled);
            if (!xform && d3dc->GetClipType() != CLIP_SHAPE && !abEnabled) {
                fhint = d3dc->IsStretchRectFilteringSupported(fhint) ?
                    fhint : D3DTEXF_NONE;

                res = D3DBlitSurfaceToSurface(d3dc, srcOps, dstOps, fhint,
                                              sx1, sy1, sx2, sy2,
                                              (int)dx1, (int)dy1,
                                               (int)dx2, (int)dy2);
            } else {
                res = D3DBlitToSurfaceViaTexture(d3dc, &srcInfo,
                                                 // surface type is unused here
                                                 ST_INT_ARGB_PRE,
                                                 srcOps,
                                                 JNI_FALSE, hint,
                                                 sx1, sy1, sx2, sy2,
                                                 dx1, dy1, dx2, dy2);
            }
        }
    }
    return res;
}

/**
 * General blit method for copying a system memory ("Sw") surface to a native
 * D3D surface (of type "Surface" or "Texture").  If texture is JNI_TRUE,
 * this method will invoke the Sw->Texture inner loop; otherwise, one of the
 * Sw->Surface inner loops will be invoked, depending on the transform state.
 */
HRESULT
D3DBlitLoops_Blit(JNIEnv *env,
                  D3DContext *d3dc, jlong pSrcOps, jlong pDstOps,
                  jboolean xform, jint hint,
                  jint srctype, jboolean texture,
                  jint sx1, jint sy1, jint sx2, jint sy2,
                  jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    SurfaceDataOps *srcOps = (SurfaceDataOps *)jlong_to_ptr(pSrcOps);
    D3DSDOps *dstOps = (D3DSDOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo;
    HRESULT res = S_OK;
    jint sw    = sx2 - sx1;
    jint sh    = sy2 - sy1;
    jdouble dw = dx2 - dx1;
    jdouble dh = dy2 - dy1;
    jint lockFlags = SD_LOCK_READ;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitLoops_Blit");

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0 || srctype < 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "D3DBlitLoops_Blit: invalid dimensions or srctype");
        return E_FAIL;
    }

    RETURN_STATUS_IF_NULL(srcOps, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc->Get3DDevice(), E_FAIL);

    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;

    if (srctype == ST_BYTE_INDEXED || srctype == ST_BYTE_INDEXED_BM) {
        lockFlags |= SD_LOCK_LUT;
    }
    if (srcOps->Lock(env, srcOps, &srcInfo, lockFlags) != SD_SUCCESS) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "D3DBlitLoops_Blit: could not acquire lock");
        return E_FAIL;
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

            if (texture) {
                // These coordinates will always be integers since we
                // only ever do a straight copy from sw to texture.
                // Thus these casts are "safe" - no loss of precision.
                res = D3DBlitSwToTexture(d3dc, &srcInfo, srctype, dstOps,
                                        (jint)dx1, (jint)dy1,
                                        (jint)dx2, (jint)dy2);
            } else {
                res = D3DBlitToSurfaceViaTexture(d3dc, &srcInfo, srctype, NULL,
                                                 JNI_TRUE, hint,
                                                 sx1, sy1, sx2, sy2,
                                                 dx1, dy1, dx2, dy2);
            }
        }
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
    return res;
}

/**
 * Specialized blit method for copying a native D3D "Surface" (pbuffer,
 * window, etc.) to a system memory ("Sw") surface.
 */
HRESULT
D3DBlitLoops_SurfaceToSwBlit(JNIEnv *env, D3DContext *d3dc,
                             jlong pSrcOps, jlong pDstOps, jint dsttype,
                             jint srcx, jint srcy, jint dstx, jint dsty,
                             jint width, jint height)
{
    D3DSDOps *srcOps = (D3DSDOps *)jlong_to_ptr(pSrcOps);
    SurfaceDataOps *dstOps = (SurfaceDataOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo, dstInfo;
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitLoops_SurfaceToSwBlit");

    if (width <= 0 || height <= 0) {
        J2dTraceLn(J2D_TRACE_WARNING,
            "D3DBlitLoops_SurfaceToSwBlit: dimensions are non-positive");
        return S_OK;
    }

    RETURN_STATUS_IF_NULL(srcOps, E_FAIL);
    RETURN_STATUS_IF_NULL(srcOps->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dc->Get3DDevice(), E_FAIL);

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
            "D3DBlitLoops_SurfaceToSwBlit: could not acquire dst lock");
        return S_OK;
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
            IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
            IDirect3DSurface9 *pSrc = srcOps->pResource->GetSurface();
            D3DFORMAT srcFmt = srcOps->pResource->GetDesc()->Format;
            UINT srcw = srcOps->pResource->GetDesc()->Width;
            UINT srch = srcOps->pResource->GetDesc()->Height;
            D3DResource *pLockableRes;

            srcx = srcInfo.bounds.x1;
            srcy = srcInfo.bounds.y1;
            dstx = dstInfo.bounds.x1;
            dsty = dstInfo.bounds.y1;
            width = srcInfo.bounds.x2 - srcInfo.bounds.x1;
            height = srcInfo.bounds.y2 - srcInfo.bounds.y1;

            J2dTraceLn4(J2D_TRACE_VERBOSE, "  sx=%d sy=%d w=%d h=%d",
                        srcx, srcy, width, height);
            J2dTraceLn2(J2D_TRACE_VERBOSE, "  dx=%d dy=%d",
                        dstx, dsty);

            d3dc->UpdateState(STATE_OTHEROP);

            // if we read more than 50% of the image it is faster
            // to get the whole thing (50% is pulled out of a hat)
            BOOL fullRead = ((width * height) >= (srcw * srch * 0.5f));
            UINT lockSrcX = 0, lockSrcY = 0;

            if (fullRead) {
                // read whole surface into a sysmem surface
                lockSrcX = srcx;
                lockSrcY = srcy;
                // the dest surface must have the same dimensions and format as
                // the source, GetBlitOSPSurface ensures that
                res = d3dc->GetResourceManager()->
                    GetBlitOSPSurface(srcw, srch, srcFmt, &pLockableRes);
            } else {
                // we first copy the source region to a temp
                // render target surface of the same format as the
                // source, then copy the pixels to the
                // target buffered image surface
                res = d3dc->GetResourceManager()->
                    GetLockableRTSurface(width, height, srcFmt, &pLockableRes);
            }
            if (SUCCEEDED(res)) {
                IDirect3DSurface9 *pTmpSurface = pLockableRes->GetSurface();

                if (fullRead) {
                    res = pd3dDevice->GetRenderTargetData(pSrc, pTmpSurface);
                } else {
                    RECT srcRect = { srcx, srcy, srcx+width, srcy+height};
                    RECT dstRect = { 0l, 0l, width, height };

                    res = pd3dDevice->StretchRect(pSrc,
                                                  &srcRect, pTmpSurface,
                                                  &dstRect, D3DTEXF_NONE);
                }

                if (SUCCEEDED(res)) {
                    res = D3DBL_CopySurfaceToIntArgbImage(
                            pTmpSurface,                       /* src surface */
                            &dstInfo,                          /* dst info    */
                            lockSrcX, lockSrcY, width, height, /* src rect    */
                            dstx, dsty);                       /* dst coords  */
                }
            }
        }
        SurfaceData_InvokeRelease(env, dstOps, &dstInfo);
    }
    SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
    return res;
}

HRESULT
D3DBlitLoops_CopyArea(JNIEnv *env,
                      D3DContext *d3dc, D3DSDOps *dstOps,
                      jint x, jint y, jint width, jint height,
                      jint dx, jint dy)
{
    SurfaceDataBounds srcBounds, dstBounds;
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBlitLoops_CopyArea");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps->pResource, E_FAIL);

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  x=%d y=%d w=%d h=%d",
                x, y, width, height);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  dx=%d dy=%d",
                dx, dy);

    IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    ClipType clipType = d3dc->GetClipType();

    srcBounds.x1 = x;
    srcBounds.y1 = y;
    srcBounds.x2 = srcBounds.x1 + width;
    srcBounds.y2 = srcBounds.y1 + height;
    dstBounds.x1 = x + dx;
    dstBounds.y1 = y + dy;
    dstBounds.x2 = dstBounds.x1 + width;
    dstBounds.y2 = dstBounds.y1 + height;

    SurfaceData_IntersectBoundsXYXY(&srcBounds,
                                    0, 0, dstOps->width, dstOps->height);
    if (clipType == CLIP_RECT) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  rect clip, clip dest manually");
        RECT clipRect;
        pd3dDevice->GetScissorRect(&clipRect);
        SurfaceData_IntersectBoundsXYXY(&dstBounds,
                                        clipRect.left, clipRect.top,
                                        clipRect.right, clipRect.bottom);
    }
    SurfaceData_IntersectBoundsXYXY(&dstBounds,
                                    0, 0, dstOps->width, dstOps->height);
    SurfaceData_IntersectBlitBounds(&dstBounds, &srcBounds, -dx, -dy);

    if (dstBounds.x1 < dstBounds.x2 && dstBounds.y1 < dstBounds.y2) {
        jint sx1 = srcBounds.x1, sy1 = srcBounds.y1,
             sx2 = srcBounds.x2, sy2 = srcBounds.y2;
        jint dx1 = dstBounds.x1, dy1 = dstBounds.y1,
             dx2 = dstBounds.x2, dy2 = dstBounds.y2;
        jint dw = dx2 - dx1, dh = dy2 - dy1;

        IDirect3DTexture9 *pBlitTexture = NULL;
        IDirect3DSurface9 *pBlitSurface = NULL;
        D3DResource *pBlitTextureRes;

        res = d3dc->GetResourceManager()->
            GetBlitRTTexture(dw, dh,
                             dstOps->pResource->GetDesc()->Format,
                             &pBlitTextureRes);
        if (SUCCEEDED(res)) {
            pBlitSurface = pBlitTextureRes->GetSurface();
            pBlitTexture = pBlitTextureRes->GetTexture();
        }
        if (!pBlitTexture || !pBlitSurface) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "D3DBlitLoops_CopyArea: could not init blit tile");
            return E_FAIL;
        }

        // flush the rendering first
        d3dc->UpdateState(STATE_OTHEROP);

        // REMIND: see if we could always use texture mapping;
        // the assumption here is that StretchRect is faster,
        // if it's not, then we should always use texture mapping

        // from src surface to the temp texture
        RECT srcRect =    { sx1, sy1, sx2, sy2 };
        RECT tmpDstRect = { 0l, 0l,  0+dw,  0+dh };
        res = pd3dDevice->StretchRect(dstOps->pResource->GetSurface(), &srcRect,
                                      pBlitSurface, &tmpDstRect,
                                      D3DTEXF_NONE);
        if (clipType != CLIP_SHAPE) {
            J2dTraceLn(J2D_TRACE_VERBOSE, "  rect or no clip, use StretchRect");
            // just do stretch rect to the destination
            RECT dstRect = { dx1, dy1, dx2, dy2 };
            // from temp surface to the destination
            res = pd3dDevice->StretchRect(pBlitSurface, &tmpDstRect,
                                          dstOps->pResource->GetSurface(),
                                          &dstRect,
                                          D3DTEXF_NONE);
        } else {
            J2dTraceLn(J2D_TRACE_VERBOSE, "  shape clip, use texture mapping");
            // shape clip - have to use texture mapping
            D3DTEXTUREFILTERTYPE fhint =
                d3dc->IsTextureFilteringSupported(D3DTEXF_NONE) ?
                    D3DTEXF_NONE: D3DTEXF_POINT;
            pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, fhint);
            pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, fhint);
            res = d3dc->BeginScene(STATE_TEXTUREOP);
            RETURN_STATUS_IF_FAILED(res);
            res = d3dc->SetTexture(pBlitTexture);

            float tx2 = (float)dw/(float)pBlitTextureRes->GetDesc()->Width;
            float ty2 = (float)dh/(float)pBlitTextureRes->GetDesc()->Height;
            res = d3dc->pVCacher->DrawTexture(
                                (float)dx1, (float)dy1, (float)dx2, (float)dy2,
                                0.0f, 0.0f, tx2, ty2);
        }
    }
    return res;
}
