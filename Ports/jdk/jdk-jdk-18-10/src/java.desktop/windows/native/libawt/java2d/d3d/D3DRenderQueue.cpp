/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"
#include <malloc.h>
#include "sun_java2d_pipe_BufferedOpCodes.h"

#include "jlong.h"
#include "D3DBlitLoops.h"
#include "D3DBufImgOps.h"
#include "D3DPipelineManager.h"
#include "D3DContext.h"
#include "D3DMaskBlit.h"
#include "D3DMaskFill.h"
#include "D3DPaints.h"
#include "D3DRenderQueue.h"
#include "D3DRenderer.h"
#include "D3DSurfaceData.h"
#include "D3DTextRenderer.h"
#include "Trace.h"
#include "awt_Toolkit.h"

BOOL DWMIsCompositionEnabled();

/**
 * References to the "current" context and destination surface.
 */
static D3DContext *d3dc = NULL;
static D3DSDOps *dstOps = NULL;
static BOOL bLostDevices = FALSE;

typedef struct {
    byte *buffer;
    int limit;
    jobject runnable;
} FlushBufferStruct;

HRESULT
D3DRQ_SwapBuffers(D3DPipelineManager *pMgr, D3DSDOps *d3dsdo,
                  int x1, int y1, int x2, int y2)
{
    HRESULT res;
    D3DContext *pCtx;
    IDirect3DSwapChain9 *pSwapChain;
    RECT srcRect, dstRect, *pSrcRect, *pDstRect;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRQ_SwapBuffers");
    J2dTraceLn4(J2D_TRACE_VERBOSE, "  x1=%d y1=%d x2=%d y2=%d",
                x1, y1, x2, y2);

    RETURN_STATUS_IF_NULL(d3dsdo, E_FAIL);
    RETURN_STATUS_IF_NULL(d3dsdo->pResource, E_FAIL);
    RETURN_STATUS_IF_NULL(pSwapChain=d3dsdo->pResource->GetSwapChain(), E_FAIL);

    pCtx = D3DRQ_GetCurrentContext();
    if (pCtx != NULL) {
        // flush the current vertex queue here, just in case
        res = d3dc->FlushVertexQueue();
        D3DRQ_MarkLostIfNeeded(res, dstOps);
        pCtx = NULL;
    }
    // end scene for this destination
    res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx);
    RETURN_STATUS_IF_FAILED(res);

    pCtx->EndScene();

    // This is a workaround for what apparently is a DWM bug.
    // If the dimensions of the back-buffer don't match the dimensions of
    // the window, Present() will flash the whole window with black.
    // The workaround is to detect this situation and not do a present.
    // It is ok to do so since a repaint event is coming due to the resize that
    // just happened.
    //
    // REMIND: this will need to be updated if we switch to creating
    // back-buffers of the size of the client area instead of the whole window
    // (use GetClientRect() instead of GetWindowRect()).
    if (DWMIsCompositionEnabled()) {
        RECT r;
        D3DPRESENT_PARAMETERS params;

        pSwapChain->GetPresentParameters(&params);
        GetWindowRect(params.hDeviceWindow, &r);
        int ww = r.right - r.left;
        int wh = r.bottom - r.top;
        if (ww != params.BackBufferWidth || wh != params.BackBufferHeight) {
            J2dTraceLn4(J2D_TRACE_WARNING,
                "D3DRQ_SwapBuffers: surface/window dimensions mismatch: "\
                "win: w=%d h=%d, bb: w=%d h=%d",
                ww, wh, params.BackBufferWidth, params.BackBufferHeight);

            return S_OK;
        }
    }

    if (d3dsdo->swapEffect == D3DSWAPEFFECT_COPY) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  D3DSWAPEFFECT_COPY");
        if (x1 < 0) x1 = 0;
        if (y1 < 0) y1 = 0;
        if (x2 > d3dsdo->width)  x2 = d3dsdo->width;
        if (y2 > d3dsdo->height) y2 = d3dsdo->height;
        if (x2 <= x1 || y2 <= y1) {
            // nothing to present
            return S_OK;
        }
        srcRect.left = x1;
        srcRect.top = y1;
        srcRect.right = x2;
        srcRect.bottom = y2;

        dstRect = srcRect;

        pSrcRect = &srcRect;
        pDstRect = &dstRect;
        // only offset in windowed mode
        if (pCtx!= NULL && pCtx->GetPresentationParams()->Windowed) {
            OffsetRect(pDstRect, d3dsdo->xoff, d3dsdo->yoff);
        } else {
            // some boards (Nvidia) have problems with copy strategy and
            // non-null src/dest rectangles in fs mode; unfortunately this
            // means that we'll paint over fs window decorations
            pSrcRect = NULL;
            pDstRect = NULL;
        }
    } else {
        if (d3dsdo->swapEffect == D3DSWAPEFFECT_FLIP) {
            J2dTraceLn(J2D_TRACE_VERBOSE, "  D3DSWAPEFFECT_FLIP");
        } else {
            J2dTraceLn(J2D_TRACE_VERBOSE, "  D3DSWAPEFFECT_DISCARD");
        }
        // src and dest rectangles must be NULL for FLIP/DISCARD
        pSrcRect = NULL;
        pDstRect = NULL;
    }

    res = pSwapChain->Present(pSrcRect, pDstRect, 0, NULL, 0);
    res = D3DRQ_MarkLostIfNeeded(res, d3dsdo);

    return res;
}

HRESULT
D3DRQ_MarkLostIfNeeded(HRESULT res, D3DSDOps *d3dops)
{
    if (res == D3DERR_DEVICELOST || res == D3DERR_DEVICENOTRESET) {
        D3DContext *pCtx;

        J2dTraceLn(J2D_TRACE_WARNING, "D3DRQ_MarkLostIfNeeded: device lost");
        bLostDevices = TRUE;

        // only mark surfaces belonging to the lost device
        if (d3dops != NULL &&
            SUCCEEDED(res = D3DPipelineManager::GetInstance()->
                GetD3DContext(d3dops->adapter, &pCtx)))
        {
            IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
            if (pd3dDevice) {
                HRESULT res1 = pd3dDevice->TestCooperativeLevel();
                if (res1 != D3DERR_DEVICELOST && res1 != D3DERR_DEVICENOTRESET){
                    // this surface's device is not lost, do not mark it
                    return res;
                }
            }
        }
        D3DSD_MarkLost(d3dops);
    }
    return res;
}

void D3DRQ_FlushBuffer(void *pParam)
{
    FlushBufferStruct *pFlush = (FlushBufferStruct*)pParam;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    unsigned char *b, *end;
    int limit;
    HRESULT res = S_OK;
    BOOL bSync = FALSE;

    b = pFlush->buffer;
    limit = pFlush->limit;
    J2dTraceLn1(J2D_TRACE_INFO, "D3DRQ_flushBuffer: limit=%d", limit);

    end = b + limit;

    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (pMgr == NULL) {
        J2dRlsTraceLn(J2D_TRACE_WARNING, "D3DRQ_flushBuffer: null manager");
        return;
    }

    if (bLostDevices) {
        if (SUCCEEDED(res = pMgr->HandleLostDevices())) {
            bLostDevices = FALSE;
        }
    }

    while (b < end) {
        jint opcode = NEXT_INT(b);

        J2dTraceLn1(J2D_TRACE_VERBOSE, "D3DRQ_flushBuffer: opcode=%d", opcode);

        switch (opcode) {

        // draw ops
        case sun_java2d_pipe_BufferedOpCodes_DRAW_LINE:
            {
                jint x1 = NEXT_INT(b);
                jint y1 = NEXT_INT(b);
                jint x2 = NEXT_INT(b);
                jint y2 = NEXT_INT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawLine(d3dc, x1, y1, x2, y2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_RECT:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);
                jint w = NEXT_INT(b);
                jint h = NEXT_INT(b);
                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawRect(d3dc, x, y, w, h);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_POLY:
            {
                jint nPoints      = NEXT_INT(b);
                jboolean isClosed = NEXT_BOOLEAN(b);
                jint transX       = NEXT_INT(b);
                jint transY       = NEXT_INT(b);
                jint *xPoints = (jint *)b;
                jint *yPoints = ((jint *)b) + nPoints;
                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawPoly(d3dc, nPoints, isClosed,
                                           transX, transY,
                                     xPoints, yPoints);
                SKIP_BYTES(b, nPoints * BYTES_PER_POLY_POINT);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_PIXEL:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawLine(d3dc, x, y, x, y);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_SCANLINES:
            {
                jint count = NEXT_INT(b);
                res = D3DRenderer_DrawScanlines(d3dc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SCANLINE);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_PARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                jfloat lwr21 = NEXT_FLOAT(b);
                jfloat lwr12 = NEXT_FLOAT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawParallelogram(d3dc,
                                                    x11, y11,
                                                    dx21, dy21,
                                                    dx12, dy12,
                                                    lwr21, lwr12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_AAPARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                jfloat lwr21 = NEXT_FLOAT(b);
                jfloat lwr12 = NEXT_FLOAT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_DrawAAParallelogram(d3dc,
                                                      x11, y11,
                                                      dx21, dy21,
                                                      dx12, dy12,
                                                      lwr21, lwr12);
            }
            break;

        // fill ops
        case sun_java2d_pipe_BufferedOpCodes_FILL_RECT:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);
                jint w = NEXT_INT(b);
                jint h = NEXT_INT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_FillRect(d3dc, x, y, w, h);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_PARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_FillParallelogram(d3dc,
                                                    x11, y11,
                                                    dx21, dy21,
                                                    dx12, dy12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_AAPARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);

                CONTINUE_IF_NULL(d3dc);
                res = D3DRenderer_FillAAParallelogram(d3dc,
                                                      x11, y11,
                                                      dx21, dy21,
                                                      dx12, dy12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_SPANS:
            {
                jint count = NEXT_INT(b);
                res = D3DRenderer_FillSpans(d3dc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SPAN);
            }
            break;

        // text-related ops
        case sun_java2d_pipe_BufferedOpCodes_DRAW_GLYPH_LIST:
            {
                jint numGlyphs        = NEXT_INT(b);
                jint packedParams     = NEXT_INT(b);
                jfloat glyphListOrigX = NEXT_FLOAT(b);
                jfloat glyphListOrigY = NEXT_FLOAT(b);
                jboolean usePositions = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_POSITIONS);
                jboolean subPixPos    = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_SUBPIXPOS);
                jboolean rgbOrder     = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_RGBORDER);
                jint lcdContrast      = EXTRACT_BYTE(packedParams,
                                                     OFFSET_CONTRAST);
                unsigned char *images = b;
                unsigned char *positions;
                jint bytesPerGlyph;
                if (usePositions) {
                    positions = (b + numGlyphs * BYTES_PER_GLYPH_IMAGE);
                    bytesPerGlyph = BYTES_PER_POSITIONED_GLYPH;
                } else {
                    positions = NULL;
                    bytesPerGlyph = BYTES_PER_GLYPH_IMAGE;
                }
                res = D3DTR_DrawGlyphList(d3dc, dstOps,
                                          numGlyphs, usePositions,
                                          subPixPos, rgbOrder, lcdContrast,
                                          glyphListOrigX, glyphListOrigY,
                                          images, positions);
                SKIP_BYTES(b, numGlyphs * bytesPerGlyph);
            }
            break;

        // copy-related ops
        case sun_java2d_pipe_BufferedOpCodes_COPY_AREA:
            {
                jint x  = NEXT_INT(b);
                jint y  = NEXT_INT(b);
                jint w  = NEXT_INT(b);
                jint h  = NEXT_INT(b);
                jint dx = NEXT_INT(b);
                jint dy = NEXT_INT(b);
                res = D3DBlitLoops_CopyArea(env, d3dc, dstOps,
                                            x, y, w, h, dx, dy);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_BLIT:
            {
                jint packedParams = NEXT_INT(b);
                jint sx1          = NEXT_INT(b);
                jint sy1          = NEXT_INT(b);
                jint sx2          = NEXT_INT(b);
                jint sy2          = NEXT_INT(b);
                jdouble dx1       = NEXT_DOUBLE(b);
                jdouble dy1       = NEXT_DOUBLE(b);
                jdouble dx2       = NEXT_DOUBLE(b);
                jdouble dy2       = NEXT_DOUBLE(b);
                jlong pSrc        = NEXT_LONG(b);
                jlong pDst        = NEXT_LONG(b);
                jint hint         = EXTRACT_BYTE(packedParams, OFFSET_HINT);
                jboolean texture  = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_TEXTURE);
                jboolean rtt      = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_RTT);
                jboolean xform    = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_XFORM);
                jboolean isoblit  = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_ISOBLIT);
                if (isoblit) {
                    res = D3DBlitLoops_IsoBlit(env, d3dc, pSrc, pDst,
                                               xform, hint, texture, rtt,
                                               sx1, sy1, sx2, sy2,
                                               dx1, dy1, dx2, dy2);
                    D3DRQ_MarkLostIfNeeded(res, (D3DSDOps*)pSrc);
                } else {
                    jint srctype = EXTRACT_BYTE(packedParams, OFFSET_SRCTYPE);
                    res = D3DBlitLoops_Blit(env, d3dc, pSrc, pDst,
                                            xform, hint, srctype, texture,
                                            sx1, sy1, sx2, sy2,
                                            dx1, dy1, dx2, dy2);
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SURFACE_TO_SW_BLIT:
            {
                jint sx      = NEXT_INT(b);
                jint sy      = NEXT_INT(b);
                jint dx      = NEXT_INT(b);
                jint dy      = NEXT_INT(b);
                jint w       = NEXT_INT(b);
                jint h       = NEXT_INT(b);
                jint dsttype = NEXT_INT(b);
                jlong pSrc   = NEXT_LONG(b);
                jlong pDst   = NEXT_LONG(b);
                res = D3DBlitLoops_SurfaceToSwBlit(env, d3dc,
                                                   pSrc, pDst, dsttype,
                                                   sx, sy, dx, dy, w, h);
                D3DRQ_MarkLostIfNeeded(res, (D3DSDOps*)pSrc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_MASK_FILL:
            {
                jint x        = NEXT_INT(b);
                jint y        = NEXT_INT(b);
                jint w        = NEXT_INT(b);
                jint h        = NEXT_INT(b);
                jint maskoff  = NEXT_INT(b);
                jint maskscan = NEXT_INT(b);
                jint masklen  = NEXT_INT(b);
                unsigned char *pMask = (masklen > 0) ? b : NULL;
                res = D3DMaskFill_MaskFill(d3dc, x, y, w, h,
                                           maskoff, maskscan, masklen, pMask);
                SKIP_BYTES(b, masklen);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_MASK_BLIT:
            {
                jint dstx     = NEXT_INT(b);
                jint dsty     = NEXT_INT(b);
                jint width    = NEXT_INT(b);
                jint height   = NEXT_INT(b);
                jint masklen  = width * height * sizeof(jint);
                res = D3DMaskBlit_MaskBlit(env, d3dc,
                                           dstx, dsty, width, height, b);
                SKIP_BYTES(b, masklen);
            }
            break;

        // state-related ops
        case sun_java2d_pipe_BufferedOpCodes_SET_RECT_CLIP:
            {
                jint x1 = NEXT_INT(b);
                jint y1 = NEXT_INT(b);
                jint x2 = NEXT_INT(b);
                jint y2 = NEXT_INT(b);
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->SetRectClip(x1, y1, x2, y2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_BEGIN_SHAPE_CLIP:
            {
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->BeginShapeClip();
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_SHAPE_CLIP_SPANS:
            {
                jint count = NEXT_INT(b);
                res = D3DRenderer_FillSpans(d3dc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SPAN);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_END_SHAPE_CLIP:
            {
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->EndShapeClip();
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_CLIP:
            {
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->ResetClip();
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_ALPHA_COMPOSITE:
            {
                jint rule         = NEXT_INT(b);
                jfloat extraAlpha = NEXT_FLOAT(b);
                jint flags        = NEXT_INT(b);
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->SetAlphaComposite(rule, extraAlpha, flags);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_XOR_COMPOSITE:
            {
                jint xorPixel = NEXT_INT(b);
//                res = d3dc->SetXorComposite(d3dc, xorPixel);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_COMPOSITE:
            {
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->ResetComposite();
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_TRANSFORM:
            {
                jdouble m00 = NEXT_DOUBLE(b);
                jdouble m10 = NEXT_DOUBLE(b);
                jdouble m01 = NEXT_DOUBLE(b);
                jdouble m11 = NEXT_DOUBLE(b);
                jdouble m02 = NEXT_DOUBLE(b);
                jdouble m12 = NEXT_DOUBLE(b);
                res = d3dc->SetTransform(m00, m10, m01, m11, m02, m12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_TRANSFORM:
            {
                CONTINUE_IF_NULL(d3dc);
                res = d3dc->ResetTransform();
            }
            break;

        // context-related ops
        case sun_java2d_pipe_BufferedOpCodes_SET_SURFACES:
            {
                jlong pSrc = NEXT_LONG(b);
                jlong pDst = NEXT_LONG(b);
                D3DContext *oldd3dc = NULL;
                if (d3dc != NULL) {
                    oldd3dc = d3dc;
                    d3dc = NULL;
                    oldd3dc->UpdateState(STATE_CHANGE);
                }
                dstOps = (D3DSDOps *)jlong_to_ptr(pDst);
                res = pMgr->GetD3DContext(dstOps->adapter, &d3dc);
                if (FAILED(res)) {
                    J2dRlsTraceLn(J2D_TRACE_ERROR,
                        "D3DRQ_FlushBuffer: failed to get context");
                    D3DRQ_ResetCurrentContextAndDestination();
                    break;
                }
                // REMIND: we may also want to do EndScene on each
                // render target change so that the GPU can go work on
                // whatever is already in the queue
                if (oldd3dc != d3dc && oldd3dc != NULL) {
                    res = oldd3dc->EndScene();
                }
                CONTINUE_IF_NULL(dstOps->pResource);
                res = d3dc->SetRenderTarget(dstOps->pResource->GetSurface());
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_SCRATCH_SURFACE:
            {
                jint screen = NEXT_INT(b);
                jint adapter = pMgr->GetAdapterOrdinalForScreen(screen);
                D3DContext *oldd3dc = NULL;

                if (d3dc != NULL) {
                    oldd3dc = d3dc;
                    d3dc = NULL;
                }
                res = pMgr->GetD3DContext(adapter, &d3dc);
                if (FAILED(res)) {
                    J2dRlsTraceLn(J2D_TRACE_ERROR,
                        "D3DRQ_FlushBuffer: failed to get context");
                    D3DRQ_ResetCurrentContextAndDestination();
                } else if (oldd3dc != d3dc && oldd3dc != NULL) {
                    res = oldd3dc->EndScene();
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FLUSH_SURFACE:
            {
                jlong pData = NEXT_LONG(b);
                D3DSDOps *d3dsdo = (D3DSDOps *)jlong_to_ptr(pData);
                D3DSD_Flush(d3dsdo);
                if (dstOps == d3dsdo) {
                    dstOps = NULL;
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISPOSE_SURFACE:
            {
                jlong pData = NEXT_LONG(b);
                D3DSDOps *d3dsdo = (D3DSDOps *)jlong_to_ptr(pData);
                D3DSD_Flush(d3dsdo);
                if (dstOps == d3dsdo) {
                    dstOps = NULL;
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISPOSE_CONFIG:
            {
                jlong pConfigInfo = NEXT_LONG(b);
                CONTINUE_IF_NULL(d3dc);
                // REMIND: does this need to be implemented for D3D?
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_INVALIDATE_CONTEXT:
            {
                // flush just in case there are any pending operations in
                // the hardware pipe
                if (d3dc != NULL) {
                    res = d3dc->EndScene();
                }

                // invalidate the references to the current context and
                // destination surface that are maintained at the native level
                D3DRQ_ResetCurrentContextAndDestination();
            }
            break;

        case sun_java2d_pipe_BufferedOpCodes_SYNC:
            {
                bSync = TRUE;
            }
            break;

        case sun_java2d_pipe_BufferedOpCodes_RESTORE_DEVICES:
            {
                J2dTraceLn(J2D_TRACE_INFO, "D3DRQ_FlushBuffer:  RESTORE_DEVICES");
                if (SUCCEEDED(res = pMgr->HandleLostDevices())) {
                    bLostDevices = FALSE;
                } else {
                    bLostDevices = TRUE;
                }
            }
            break;
        // multibuffering ops
        case sun_java2d_pipe_BufferedOpCodes_SWAP_BUFFERS:
            {
                jlong sdo = NEXT_LONG(b);
                jint x1 = NEXT_INT(b);
                jint y1 = NEXT_INT(b);
                jint x2 = NEXT_INT(b);
                jint y2 = NEXT_INT(b);

                res = D3DRQ_SwapBuffers(pMgr, (D3DSDOps *)jlong_to_ptr(sdo),
                                        x1, y1, x2, y2);
            }
            break;

        // special no-op (mainly used for achieving 8-byte alignment)
        case sun_java2d_pipe_BufferedOpCodes_NOOP:
            break;

        // paint-related ops
        case sun_java2d_pipe_BufferedOpCodes_RESET_PAINT:
            {
                res = D3DPaints_ResetPaint(d3dc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_COLOR:
            {
                jint pixel = NEXT_INT(b);
                res = D3DPaints_SetColor(d3dc, pixel);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_GRADIENT_PAINT:
            {
                jboolean useMask= NEXT_BOOLEAN(b);
                jboolean cyclic = NEXT_BOOLEAN(b);
                jdouble p0      = NEXT_DOUBLE(b);
                jdouble p1      = NEXT_DOUBLE(b);
                jdouble p3      = NEXT_DOUBLE(b);
                jint pixel1     = NEXT_INT(b);
                jint pixel2     = NEXT_INT(b);
                res = D3DPaints_SetGradientPaint(d3dc, useMask, cyclic,
                                                 p0, p1, p3,
                                                 pixel1, pixel2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_LINEAR_GRADIENT_PAINT:
            {
                jboolean useMask = NEXT_BOOLEAN(b);
                jboolean linear  = NEXT_BOOLEAN(b);
                jint cycleMethod = NEXT_INT(b);
                jint numStops    = NEXT_INT(b);
                jfloat p0        = NEXT_FLOAT(b);
                jfloat p1        = NEXT_FLOAT(b);
                jfloat p3        = NEXT_FLOAT(b);
                void *fractions, *pixels;
                fractions = b; SKIP_BYTES(b, numStops * sizeof(jfloat));
                pixels    = b; SKIP_BYTES(b, numStops * sizeof(jint));
                res = D3DPaints_SetLinearGradientPaint(d3dc, dstOps,
                                                        useMask, linear,
                                                        cycleMethod, numStops,
                                                        p0, p1, p3,
                                                        fractions, pixels);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_RADIAL_GRADIENT_PAINT:
            {
                jboolean useMask = NEXT_BOOLEAN(b);
                jboolean linear  = NEXT_BOOLEAN(b);
                jint numStops    = NEXT_INT(b);
                jint cycleMethod = NEXT_INT(b);
                jfloat m00       = NEXT_FLOAT(b);
                jfloat m01       = NEXT_FLOAT(b);
                jfloat m02       = NEXT_FLOAT(b);
                jfloat m10       = NEXT_FLOAT(b);
                jfloat m11       = NEXT_FLOAT(b);
                jfloat m12       = NEXT_FLOAT(b);
                jfloat focusX    = NEXT_FLOAT(b);
                void *fractions, *pixels;
                fractions = b; SKIP_BYTES(b, numStops * sizeof(jfloat));
                pixels    = b; SKIP_BYTES(b, numStops * sizeof(jint));
                res = D3DPaints_SetRadialGradientPaint(d3dc, dstOps,
                                                       useMask, linear,
                                                       cycleMethod, numStops,
                                                       m00, m01, m02,
                                                       m10, m11, m12,
                                                       focusX,
                                                       fractions, pixels);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_TEXTURE_PAINT:
            {
                jboolean useMask= NEXT_BOOLEAN(b);
                jboolean filter = NEXT_BOOLEAN(b);
                jlong pSrc      = NEXT_LONG(b);
                jdouble xp0     = NEXT_DOUBLE(b);
                jdouble xp1     = NEXT_DOUBLE(b);
                jdouble xp3     = NEXT_DOUBLE(b);
                jdouble yp0     = NEXT_DOUBLE(b);
                jdouble yp1     = NEXT_DOUBLE(b);
                jdouble yp3     = NEXT_DOUBLE(b);
                res = D3DPaints_SetTexturePaint(d3dc, useMask, pSrc, filter,
                                                xp0, xp1, xp3,
                                                yp0, yp1, yp3);
            }
            break;

        // BufferedImageOp-related ops
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_CONVOLVE_OP:
            {
                jlong pSrc        = NEXT_LONG(b);
                jboolean edgeZero = NEXT_BOOLEAN(b);
                jint kernelWidth  = NEXT_INT(b);
                jint kernelHeight = NEXT_INT(b);
                res = D3DBufImgOps_EnableConvolveOp(d3dc, pSrc, edgeZero,
                                                    kernelWidth, kernelHeight, b);
                SKIP_BYTES(b, kernelWidth * kernelHeight * sizeof(jfloat));
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_CONVOLVE_OP:
            {
                res = D3DBufImgOps_DisableConvolveOp(d3dc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_RESCALE_OP:
            {
                jlong pSrc          = NEXT_LONG(b); // unused
                jboolean nonPremult = NEXT_BOOLEAN(b);
                jint numFactors     = 4;
                unsigned char *scaleFactors = b;
                unsigned char *offsets = (b + numFactors * sizeof(jfloat));
                res = D3DBufImgOps_EnableRescaleOp(d3dc, nonPremult,
                                                   scaleFactors, offsets);
                SKIP_BYTES(b, numFactors * sizeof(jfloat) * 2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_RESCALE_OP:
            {
                D3DBufImgOps_DisableRescaleOp(d3dc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_LOOKUP_OP:
            {
                jlong pSrc          = NEXT_LONG(b); // unused
                jboolean nonPremult = NEXT_BOOLEAN(b);
                jboolean shortData  = NEXT_BOOLEAN(b);
                jint numBands       = NEXT_INT(b);
                jint bandLength     = NEXT_INT(b);
                jint offset         = NEXT_INT(b);
                jint bytesPerElem = shortData ? sizeof(jshort):sizeof(jbyte);
                void *tableValues = b;
                res = D3DBufImgOps_EnableLookupOp(d3dc, nonPremult, shortData,
                                                  numBands, bandLength, offset,
                                                  tableValues);
                SKIP_BYTES(b, numBands * bandLength * bytesPerElem);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_LOOKUP_OP:
            {
                res = D3DBufImgOps_DisableLookupOp(d3dc);
            }
            break;

        default:
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                "D3DRQ_flushBuffer: invalid opcode=%d", opcode);
            return;
        }
        // we may mark the surface lost repeatedly but that won't do much harm
        res = D3DRQ_MarkLostIfNeeded(res, dstOps);
    }

    if (d3dc != NULL) {
        res = d3dc->EndScene();
        // REMIND: EndScene is not really enough to flush the
        // whole d3d pipeline

        // REMIND: there may be an issue with BeginScene/EndScene
        // for each flushQueue, because of the blits, which flush
        // the queue
        if (bSync) {
            res = d3dc->Sync();
        }
    }

    // REMIND: we need to also handle hard errors here as well, and disable
    // particular context if needed
    D3DRQ_MarkLostIfNeeded(res, dstOps);

    if (!JNU_IsNull(env, pFlush->runnable)) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  executing runnable");
        JNU_CallMethodByName(env, NULL, pFlush->runnable, "run", "()V");
    }
}

/**
 * Returns a pointer to the "current" context, as set by the last SET_SURFACES
 * or SET_SCRATCH_SURFACE operation.
 */
D3DContext *
D3DRQ_GetCurrentContext()
{
    return d3dc;
}

/**
 * Returns a pointer to the "current" destination surface, as set by the last
 * SET_SURFACES operation.
 */
D3DSDOps *
D3DRQ_GetCurrentDestination()
{
    return dstOps;
}

/**
 * Resets current context and destination surface.
 */
void
D3DRQ_ResetCurrentContextAndDestination()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRQ_ResetCurrentContextAndDestination");

    d3dc = NULL;
    dstOps = NULL;
}

extern "C"
{

/*
 * Class:     sun_java2d_d3d_D3DRenderQueue
 * Method:    flushBuffer
 * Signature: (JILjava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DRenderQueue_flushBuffer
  (JNIEnv *env, jobject d3drq, jlong buf, jint limit, jobject runnable)
{
    FlushBufferStruct bufstr;
    // just in case we forget to init any new fields
    ZeroMemory(&bufstr, sizeof(FlushBufferStruct));

    bufstr.buffer = (unsigned char *)jlong_to_ptr(buf);
    if (bufstr.buffer == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DRenderQueue_flushBuffer: cannot get direct buffer address");
        return;
    }
    bufstr.limit = limit;

    bufstr.runnable = JNU_IsNull(env, runnable) ?
        NULL : env->NewGlobalRef(runnable);
    AwtToolkit::GetInstance().InvokeFunction(D3DRQ_FlushBuffer, &bufstr);
    if (!JNU_IsNull(env, bufstr.runnable)) {
        env->DeleteGlobalRef(bufstr.runnable);
    }
}

}
