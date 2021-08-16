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

#include <jni.h>
#include <jlong.h>

#include "SurfaceData.h"
#include "MTLBlitLoops.h"
#include "MTLRenderQueue.h"
#include "MTLSurfaceData.h"
#include "MTLUtils.h"
#include "GraphicsPrimitiveMgr.h"

#include <string.h> // memcpy
#include "IntArgbPre.h"

#import <Accelerate/Accelerate.h>

#ifdef DEBUG
#define TRACE_ISOBLIT
#define TRACE_BLIT
#endif //DEBUG
//#define DEBUG_ISOBLIT
//#define DEBUG_BLIT

typedef struct {
    // Consider deleting this field, since it's always MTLPixelFormatBGRA8Unorm
    jboolean hasAlpha;
    jboolean isPremult;
    const uint8_t* swizzleMap;
} MTLRasterFormatInfo;


const uint8_t rgb_to_rgba[4] =  {0, 1, 2, 3};
const uint8_t xrgb_to_rgba[4] = {1, 2, 3, 0};
const uint8_t bgr_to_rgba[4] =  {2, 1, 0, 3};
const uint8_t xbgr_to_rgba[4] = {3, 2, 1, 0};

/**
 * This table contains the "pixel formats" for all system memory surfaces
 * that Metal is capable of handling, indexed by the "PF_" constants defined
 * in MTLLSurfaceData.java.  These pixel formats contain information that is
 * passed to Metal when copying from a system memory ("Sw") surface to
 * an Metal surface
 */
MTLRasterFormatInfo RasterFormatInfos[] = {
        { 1, 0, nil }, /* 0 - IntArgb      */ // Argb (in java notation)
        { 1, 1, nil }, /* 1 - IntArgbPre   */
        { 0, 1, rgb_to_rgba }, /* 2 - IntRgb       */
        { 0, 1, xrgb_to_rgba }, /* 3 - IntRgbx      */
        { 0, 1, bgr_to_rgba  }, /* 4 - IntBgr       */
        { 0, 1, xbgr_to_rgba }, /* 5 - IntBgrx      */

//        TODO: support 2-byte formats
//        { GL_BGRA, GL_UNSIGNED_SHORT_1_5_5_5_REV,
//                2, 0, 1,                                     }, /* 7 - Ushort555Rgb */
//        { GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1,
//                2, 0, 1,                                     }, /* 8 - Ushort555Rgbx*/
//        { GL_LUMINANCE, GL_UNSIGNED_BYTE,
//                1, 0, 1,                                     }, /* 9 - ByteGray     */
//        { GL_LUMINANCE, GL_UNSIGNED_SHORT,
//                2, 0, 1,                                     }, /*10 - UshortGray   */
//        { GL_BGR,  GL_UNSIGNED_BYTE,
//                1, 0, 1,                                     }, /*11 - ThreeByteBgr */
};

extern void J2dTraceImpl(int level, jboolean cr, const char *string, ...);

void fillTxQuad(
        struct TxtVertex * txQuadVerts,
        jint sx1, jint sy1, jint sx2, jint sy2, jint sw, jint sh,
        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2, jdouble dw, jdouble dh
) {
    const float nsx1 = sx1/(float)sw;
    const float nsy1 = sy1/(float)sh;
    const float nsx2 = sx2/(float)sw;
    const float nsy2 = sy2/(float)sh;

    txQuadVerts[0].position[0] = dx1;
    txQuadVerts[0].position[1] = dy1;
    txQuadVerts[0].txtpos[0]   = nsx1;
    txQuadVerts[0].txtpos[1]   = nsy1;

    txQuadVerts[1].position[0] = dx2;
    txQuadVerts[1].position[1] = dy1;
    txQuadVerts[1].txtpos[0]   = nsx2;
    txQuadVerts[1].txtpos[1]   = nsy1;

    txQuadVerts[2].position[0] = dx2;
    txQuadVerts[2].position[1] = dy2;
    txQuadVerts[2].txtpos[0]   = nsx2;
    txQuadVerts[2].txtpos[1]   = nsy2;

    txQuadVerts[3].position[0] = dx2;
    txQuadVerts[3].position[1] = dy2;
    txQuadVerts[3].txtpos[0]   = nsx2;
    txQuadVerts[3].txtpos[1]   = nsy2;

    txQuadVerts[4].position[0] = dx1;
    txQuadVerts[4].position[1] = dy2;
    txQuadVerts[4].txtpos[0]   = nsx1;
    txQuadVerts[4].txtpos[1]   = nsy2;

    txQuadVerts[5].position[0] = dx1;
    txQuadVerts[5].position[1] = dy1;
    txQuadVerts[5].txtpos[0]   = nsx1;
    txQuadVerts[5].txtpos[1]   = nsy1;
}

//#define TRACE_drawTex2Tex

void drawTex2Tex(MTLContext *mtlc,
                        id<MTLTexture> src, id<MTLTexture> dst,
                        jboolean isSrcOpaque, jboolean isDstOpaque, jint hint,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
#ifdef TRACE_drawTex2Tex
    J2dRlsTraceLn2(J2D_TRACE_VERBOSE, "drawTex2Tex: src tex=%p, dst tex=%p", src, dst);
    J2dRlsTraceLn4(J2D_TRACE_VERBOSE, "  sw=%d sh=%d dw=%d dh=%d", src.width, src.height, dst.width, dst.height);
    J2dRlsTraceLn4(J2D_TRACE_VERBOSE, "  sx1=%d sy1=%d sx2=%d sy2=%d", sx1, sy1, sx2, sy2);
    J2dRlsTraceLn4(J2D_TRACE_VERBOSE, "  dx1=%f dy1=%f dx2=%f dy2=%f", dx1, dy1, dx2, dy2);
#endif //TRACE_drawTex2Tex

    id<MTLRenderCommandEncoder> encoder = [mtlc.encoderManager getTextureEncoder:dst
                                                                     isSrcOpaque:isSrcOpaque
                                                                     isDstOpaque:isDstOpaque
                                                                   interpolation:hint
    ];

    struct TxtVertex quadTxVerticesBuffer[6];
    fillTxQuad(quadTxVerticesBuffer, sx1, sy1, sx2, sy2, src.width, src.height, dx1, dy1, dx2, dy2, dst.width, dst.height);

    [encoder setVertexBytes:quadTxVerticesBuffer length:sizeof(quadTxVerticesBuffer) atIndex:MeshVertexBuffer];
    [encoder setFragmentTexture:src atIndex: 0];
    [encoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:6];
}

static void fillSwizzleUniforms(struct SwizzleUniforms *uniforms, const MTLRasterFormatInfo *rfi) {
    const size_t SWIZZLE_MAP_SIZE = 4;
    memcpy(&uniforms->swizzle, rfi->swizzleMap, SWIZZLE_MAP_SIZE);
    uniforms->hasAlpha = rfi->hasAlpha;
}

static void
replaceTextureRegion(MTLContext *mtlc, id<MTLTexture> dest, const SurfaceDataRasInfo *srcInfo,
                     const MTLRasterFormatInfo *rfi,
                     int dx1, int dy1, int dx2, int dy2) {
    const int sw = MIN(srcInfo->bounds.x2 - srcInfo->bounds.x1, MTL_GPU_FAMILY_MAC_TXT_SIZE);
    const int sh = MIN(srcInfo->bounds.y2 - srcInfo->bounds.y1, MTL_GPU_FAMILY_MAC_TXT_SIZE);
    const int dw = MIN(dx2 - dx1, MTL_GPU_FAMILY_MAC_TXT_SIZE);
    const int dh = MIN(dy2 - dy1, MTL_GPU_FAMILY_MAC_TXT_SIZE);

    if (dw < sw || dh < sh) {
        J2dTraceLn4(J2D_TRACE_ERROR, "replaceTextureRegion: dest size: (%d, %d) less than source size: (%d, %d)", dw, dh, sw, sh);
        return;
    }

    const void *raster = srcInfo->rasBase;
    raster += (NSUInteger)srcInfo->bounds.y1 * (NSUInteger)srcInfo->scanStride + (NSUInteger)srcInfo->bounds.x1 * (NSUInteger)srcInfo->pixelStride;

    @autoreleasepool {
        J2dTraceLn4(J2D_TRACE_VERBOSE, "replaceTextureRegion src (dw, dh) : [%d, %d] dest (dx1, dy1) =[%d, %d]",
                    dw, dh, dx1, dy1);
        id<MTLBuffer> buff = [[mtlc.device newBufferWithLength:(sw * sh * srcInfo->pixelStride) options:MTLResourceStorageModeManaged] autorelease];

        // copy src pixels inside src bounds to buff
        for (int row = 0; row < sh; row++) {
            memcpy(buff.contents + (row * sw * srcInfo->pixelStride), raster, sw * srcInfo->pixelStride);
            raster += (NSUInteger)srcInfo->scanStride;
        }
        [buff didModifyRange:NSMakeRange(0, buff.length)];

        if (rfi->swizzleMap != nil) {
            id <MTLBuffer> swizzled = [[mtlc.device newBufferWithLength:(sw * sh * srcInfo->pixelStride) options:MTLResourceStorageModeManaged] autorelease];

            // this should be cheap, since data is already on GPU
            id<MTLCommandBuffer> cb = [mtlc createCommandBuffer];
            id<MTLComputeCommandEncoder> computeEncoder = [cb computeCommandEncoder];
            id<MTLComputePipelineState> computePipelineState = [mtlc.pipelineStateStorage
                                                                getComputePipelineState:@"swizzle_to_rgba"];
            [computeEncoder setComputePipelineState:computePipelineState];

            [computeEncoder setBuffer:buff offset:0 atIndex:0];
            [computeEncoder setBuffer:swizzled offset:0 atIndex:1];

            struct SwizzleUniforms uniforms;
            fillSwizzleUniforms(&uniforms, rfi);
            [computeEncoder setBytes:&uniforms length:sizeof(struct SwizzleUniforms) atIndex:2];

            NSUInteger pixelCount = buff.length / srcInfo->pixelStride;
            [computeEncoder setBytes:&pixelCount length:sizeof(NSUInteger) atIndex:3];

            NSUInteger threadGroupSize = computePipelineState.maxTotalThreadsPerThreadgroup;
            if (threadGroupSize == 0) {
               threadGroupSize = 1;
            }
            MTLSize threadsPerGroup = MTLSizeMake(threadGroupSize, 1, 1);
            MTLSize threadGroups = MTLSizeMake((pixelCount + threadGroupSize - 1) / threadGroupSize,
                                               1, 1);
            [computeEncoder dispatchThreadgroups:threadGroups
                           threadsPerThreadgroup:threadsPerGroup];
            [computeEncoder endEncoding];
            [cb commit];

            buff = swizzled;
        }

        id<MTLBlitCommandEncoder> blitEncoder = [mtlc.encoderManager createBlitEncoder];
        [blitEncoder copyFromBuffer:buff
                       sourceOffset:0 sourceBytesPerRow:(sw * srcInfo->pixelStride)
                sourceBytesPerImage:(sw * sh * srcInfo->pixelStride) sourceSize:MTLSizeMake(sw, sh, 1)
                          toTexture:dest
                   destinationSlice:0 destinationLevel:0 destinationOrigin:MTLOriginMake(dx1, dy1, 0)];
        [blitEncoder endEncoding];
        [mtlc.encoderManager endEncoder];

        MTLCommandBufferWrapper * cbwrapper = [mtlc pullCommandBufferWrapper];
        id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
        [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
            [cbwrapper release];
        }];
        [commandbuf commit];
    }
}

/**
 * Inner loop used for copying a source system memory ("Sw") surface to a
 * destination MTL "Surface".  This method is invoked from
 * MTLBlitLoops_Blit().
 */

static void
MTLBlitSwToTextureViaPooledTexture(
        MTLContext *mtlc, SurfaceDataRasInfo *srcInfo, BMTLSDOps * bmtlsdOps,
        MTLRasterFormatInfo *rfi, jint hint,
        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    int sw = srcInfo->bounds.x2 - srcInfo->bounds.x1;
    int sh = srcInfo->bounds.y2 - srcInfo->bounds.y1;

    sw = MIN(sw, MTL_GPU_FAMILY_MAC_TXT_SIZE);
    sh = MIN(sh, MTL_GPU_FAMILY_MAC_TXT_SIZE);

    id<MTLTexture> dest = bmtlsdOps->pTexture;

    MTLPooledTextureHandle * texHandle = [mtlc.texturePool getTexture:sw height:sh format:MTLPixelFormatBGRA8Unorm];
    if (texHandle == nil) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLBlitSwToTextureViaPooledTexture: can't obtain temporary texture object from pool");
        return;
    }
    [[mtlc getCommandBufferWrapper] registerPooledTexture:texHandle];

    id<MTLTexture> texBuff = texHandle.texture;
    replaceTextureRegion(mtlc, texBuff, srcInfo, rfi, 0, 0, sw, sh);

    drawTex2Tex(mtlc, texBuff, dest, !rfi->hasAlpha, bmtlsdOps->isOpaque, hint,
                0, 0, sw, sh, dx1, dy1, dx2, dy2);
}

static
jboolean isIntegerAndUnscaled(
        jint sx1, jint sy1, jint sx2, jint sy2,
        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2
) {
    const jdouble epsilon = 0.0001f;

    // check that dx1,dy1 is integer
    if (fabs(dx1 - (int)dx1) > epsilon || fabs(dy1 - (int)dy1) > epsilon) {
        return JNI_FALSE;
    }
    // check that destSize equals srcSize
    if (fabs(dx2 - dx1 - sx2 + sx1) > epsilon || fabs(dy2 - dy1 - sy2 + sy1) > epsilon) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static
jboolean clipDestCoords(
        jdouble *dx1, jdouble *dy1, jdouble *dx2, jdouble *dy2,
        jint *sx1, jint *sy1, jint *sx2, jint *sy2,
        jint destW, jint destH, const MTLScissorRect * clipRect
) {
    // Trim destination rect by clip-rect (or dest.bounds)
    const jint sw    = *sx2 - *sx1;
    const jint sh    = *sy2 - *sy1;
    const jdouble dw = *dx2 - *dx1;
    const jdouble dh = *dy2 - *dy1;

    jdouble dcx1 = 0;
    jdouble dcx2 = destW;
    jdouble dcy1 = 0;
    jdouble dcy2 = destH;
    if (clipRect != NULL) {
        if (clipRect->x > dcx1)
            dcx1 = clipRect->x;
        const int maxX = clipRect->x + clipRect->width;
        if (dcx2 > maxX)
            dcx2 = maxX;
        if (clipRect->y > dcy1)
            dcy1 = clipRect->y;
        const int maxY = clipRect->y + clipRect->height;
        if (dcy2 > maxY)
            dcy2 = maxY;

        if (dcx1 >= dcx2) {
            J2dTraceLn2(J2D_TRACE_ERROR, "\tclipDestCoords: dcx1=%1.2f, dcx2=%1.2f", dcx1, dcx2);
            dcx1 = dcx2;
        }
        if (dcy1 >= dcy2) {
            J2dTraceLn2(J2D_TRACE_ERROR, "\tclipDestCoords: dcy1=%1.2f, dcy2=%1.2f", dcy1, dcy2);
            dcy1 = dcy2;
        }
    }
    if (*dx2 <= dcx1 || *dx1 >= dcx2 || *dy2 <= dcy1 || *dy1 >= dcy2) {
        J2dTraceLn(J2D_TRACE_INFO, "\tclipDestCoords: dest rect doesn't intersect clip area");
        J2dTraceLn4(J2D_TRACE_INFO, "\tdx2=%1.4f <= dcx1=%1.4f || *dx1=%1.4f >= dcx2=%1.4f", *dx2, dcx1, *dx1, dcx2);
        J2dTraceLn4(J2D_TRACE_INFO, "\t*dy2=%1.4f <= dcy1=%1.4f || *dy1=%1.4f >= dcy2=%1.4f", *dy2, dcy1, *dy1, dcy2);
        return JNI_FALSE;
    }
    if (*dx1 < dcx1) {
        J2dTraceLn3(J2D_TRACE_VERBOSE, "\t\tdx1=%1.2f, will be clipped to %1.2f | sx1+=%d", *dx1, dcx1, (jint)((dcx1 - *dx1) * (sw/dw)));
        *sx1 += (jint)((dcx1 - *dx1) * (sw/dw));
        *dx1 = dcx1;
    }
    if (*dx2 > dcx2) {
        J2dTraceLn3(J2D_TRACE_VERBOSE, "\t\tdx2=%1.2f, will be clipped to %1.2f | sx2-=%d", *dx2, dcx2, (jint)((*dx2 - dcx2) * (sw/dw)));
        *sx2 -= (jint)((*dx2 - dcx2) * (sw/dw));
        *dx2 = dcx2;
    }
    if (*dy1 < dcy1) {
        J2dTraceLn3(J2D_TRACE_VERBOSE, "\t\tdy1=%1.2f, will be clipped to %1.2f | sy1+=%d", *dy1, dcy1, (jint)((dcy1 - *dy1) * (sh/dh)));
        *sy1 += (jint)((dcy1 - *dy1) * (sh/dh));
        *dy1 = dcy1;
    }
    if (*dy2 > dcy2) {
        J2dTraceLn3(J2D_TRACE_VERBOSE, "\t\tdy2=%1.2f, will be clipped to %1.2f | sy2-=%d", *dy2, dcy2, (jint)((*dy2 - dcy2) * (sh/dh)));
        *sy2 -= (jint)((*dy2 - dcy2) * (sh/dh));
        *dy2 = dcy2;
    }
    return JNI_TRUE;
}

/**
 * General blit method for copying a native MTL surface to another MTL "Surface".
 * Parameter texture == true forces to use 'texture' codepath (dest coordinates will always be integers).
 * Parameter xform == true only when AffineTransform is used (invoked only from TransformBlit, dest coordinates will always be integers).
 */
void
MTLBlitLoops_IsoBlit(JNIEnv *env,
                     MTLContext *mtlc, jlong pSrcOps, jlong pDstOps,
                     jboolean xform, jint hint, jboolean texture,
                     jint sx1, jint sy1, jint sx2, jint sy2,
                     jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    BMTLSDOps *srcOps = (BMTLSDOps *)jlong_to_ptr(pSrcOps);
    BMTLSDOps *dstOps = (BMTLSDOps *)jlong_to_ptr(pDstOps);

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    // Verify if we use a valid MTLContext
    MTLSDOps *dstMTLOps = (MTLSDOps *)dstOps->privOps;
    RETURN_IF_TRUE(dstMTLOps->configInfo != NULL && mtlc != dstMTLOps->configInfo->context);

    MTLSDOps *srcMTLOps = (MTLSDOps *)srcOps->privOps;
    RETURN_IF_TRUE(srcMTLOps->configInfo != NULL && mtlc != srcMTLOps->configInfo->context);

    id<MTLTexture> srcTex = srcOps->pTexture;
    id<MTLTexture> dstTex = dstOps->pTexture;
    if (srcTex == nil || srcTex == nil) {
        J2dTraceLn2(J2D_TRACE_ERROR, "MTLBlitLoops_IsoBlit: surface is null (stex=%p, dtex=%p)", srcTex, dstTex);
        return;
    }

    const jint sw    = sx2 - sx1;
    const jint sh    = sy2 - sy1;
    const jdouble dw = dx2 - dx1;
    const jdouble dh = dy2 - dy1;

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
        J2dTraceLn4(J2D_TRACE_WARNING, "MTLBlitLoops_IsoBlit: invalid dimensions: sw=%d, sh%d, dw=%d, dh=%d", sw, sh, dw, dh);
        return;
    }

#ifdef DEBUG_ISOBLIT
    if ((xform == JNI_TRUE) != (mtlc.useTransform == JNI_TRUE)) {
        J2dTraceImpl(J2D_TRACE_ERROR, JNI_TRUE,
                "MTLBlitLoops_IsoBlit state error: xform=%d, mtlc.useTransform=%d, texture=%d",
                xform, mtlc.useTransform, texture);
    }
#endif // DEBUG_ISOBLIT

    if (!xform) {
        clipDestCoords(
                &dx1, &dy1, &dx2, &dy2,
                &sx1, &sy1, &sx2, &sy2,
                dstTex.width, dstTex.height, texture ? NULL : [mtlc.clip getRect]
        );
    }

    SurfaceDataBounds bounds;
    bounds.x1 = sx1;
    bounds.y1 = sy1;
    bounds.x2 = sx2;
    bounds.y2 = sy2;
    SurfaceData_IntersectBoundsXYXY(&bounds, 0, 0, srcOps->width, srcOps->height);

    if (bounds.x2 <= bounds.x1 || bounds.y2 <= bounds.y1) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "MTLBlitLoops_IsoBlit: source rectangle doesn't intersect with source surface bounds");
        J2dTraceLn6(J2D_TRACE_VERBOSE, "  sx1=%d sy1=%d sx2=%d sy2=%d sw=%d sh=%d", sx1, sy1, sx2, sy2, srcOps->width, srcOps->height);
        J2dTraceLn4(J2D_TRACE_VERBOSE, "  dx1=%f dy1=%f dx2=%f dy2=%f", dx1, dy1, dx2, dy2);
        return;
    }

    if (bounds.x1 != sx1) {
        dx1 += (bounds.x1 - sx1) * (dw / sw);
        sx1 = bounds.x1;
    }
    if (bounds.y1 != sy1) {
        dy1 += (bounds.y1 - sy1) * (dh / sh);
        sy1 = bounds.y1;
    }
    if (bounds.x2 != sx2) {
        dx2 += (bounds.x2 - sx2) * (dw / sw);
        sx2 = bounds.x2;
    }
    if (bounds.y2 != sy2) {
        dy2 += (bounds.y2 - sy2) * (dh / sh);
        sy2 = bounds.y2;
    }

#ifdef TRACE_ISOBLIT
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE,
         "MTLBlitLoops_IsoBlit [tx=%d, xf=%d, AC=%s]: src=%s, dst=%s | (%d, %d, %d, %d)->(%1.2f, %1.2f, %1.2f, %1.2f)",
         texture, xform, [mtlc getCompositeDescription].cString,
         getSurfaceDescription(srcOps).cString, getSurfaceDescription(dstOps).cString,
         sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
#endif //TRACE_ISOBLIT

    if (!texture && !xform
        && srcOps->isOpaque
        && isIntegerAndUnscaled(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2)
        && (dstOps->isOpaque || !srcOps->isOpaque)
    ) {
#ifdef TRACE_ISOBLIT
        J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_TRUE," [via blitEncoder]");
#endif //TRACE_ISOBLIT

        id <MTLBlitCommandEncoder> blitEncoder = [mtlc.encoderManager createBlitEncoder];
        [blitEncoder copyFromTexture:srcTex
                         sourceSlice:0
                         sourceLevel:0
                        sourceOrigin:MTLOriginMake(sx1, sy1, 0)
                          sourceSize:MTLSizeMake(sx2 - sx1, sy2 - sy1, 1)
                           toTexture:dstTex
                    destinationSlice:0
                    destinationLevel:0
                   destinationOrigin:MTLOriginMake(dx1, dy1, 0)];
        [blitEncoder endEncoding];
        return;
    }

#ifdef TRACE_ISOBLIT
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_TRUE," [via sampling]");
#endif //TRACE_ISOBLIT
    drawTex2Tex(mtlc, srcTex, dstTex,
            srcOps->isOpaque, dstOps->isOpaque,
            hint, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
}

/**
 * General blit method for copying a system memory ("Sw") surface to a native MTL surface.
 * Parameter texture == true only in SwToTextureBlit (straight copy from sw to texture), dest coordinates will always be integers.
 * Parameter xform == true only when AffineTransform is used (invoked only from TransformBlit, dest coordinates will always be integers).
 */
void
MTLBlitLoops_Blit(JNIEnv *env,
                  MTLContext *mtlc, jlong pSrcOps, jlong pDstOps,
                  jboolean xform, jint hint,
                  jint srctype, jboolean texture,
                  jint sx1, jint sy1, jint sx2, jint sy2,
                  jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2)
{
    SurfaceDataOps *srcOps = (SurfaceDataOps *)jlong_to_ptr(pSrcOps);
    BMTLSDOps *dstOps = (BMTLSDOps *)jlong_to_ptr(pDstOps);

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    // Verify if we use a valid MTLContext
    MTLSDOps *dstMTLOps = (MTLSDOps *)dstOps->privOps;
    RETURN_IF_TRUE(dstMTLOps->configInfo != NULL && mtlc != dstMTLOps->configInfo->context);

    id<MTLTexture> dest = dstOps->pTexture;
    if (dest == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLBlitLoops_Blit: dest is null");
        return;
    }
    if (srctype < 0 || srctype >= sizeof(RasterFormatInfos)/ sizeof(MTLRasterFormatInfo)) {
        J2dTraceLn1(J2D_TRACE_ERROR, "MTLBlitLoops_Blit: source pixel format %d isn't supported", srctype);
        return;
    }
    const jint sw    = sx2 - sx1;
    const jint sh    = sy2 - sy1;
    const jdouble dw = dx2 - dx1;
    const jdouble dh = dy2 - dy1;

    if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLBlitLoops_Blit: invalid dimensions");
        return;
    }

#ifdef DEBUG_BLIT
    if (
        (xform == JNI_TRUE) != (mtlc.useTransform == JNI_TRUE)
        || (xform && texture)
    ) {
        J2dTraceImpl(J2D_TRACE_ERROR, JNI_TRUE,
                "MTLBlitLoops_Blit state error: xform=%d, mtlc.useTransform=%d, texture=%d",
                xform, mtlc.useTransform, texture);
    }
    if (texture) {
        if (!isIntegerAndUnscaled(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2)) {
            J2dTraceImpl(J2D_TRACE_ERROR, JNI_TRUE,
                    "MTLBlitLoops_Blit state error: texture=true, but src and dst dimensions aren't equal or dest coords aren't integers");
        }
        if (!dstOps->isOpaque && !RasterFormatInfos[srctype].hasAlpha) {
            J2dTraceImpl(J2D_TRACE_ERROR, JNI_TRUE,
                    "MTLBlitLoops_Blit state error: texture=true, but dest has alpha and source hasn't alpha, can't use texture-codepath");
        }
    }
#endif // DEBUG_BLIT
    if (!xform) {
        clipDestCoords(
                &dx1, &dy1, &dx2, &dy2,
                &sx1, &sy1, &sx2, &sy2,
                dest.width, dest.height, texture ? NULL : [mtlc.clip getRect]
        );
    }

    SurfaceDataRasInfo srcInfo;
    srcInfo.bounds.x1 = sx1;
    srcInfo.bounds.y1 = sy1;
    srcInfo.bounds.x2 = sx2;
    srcInfo.bounds.y2 = sy2;

    // NOTE: This function will modify the contents of the bounds field to represent the maximum available raster data.
    if (srcOps->Lock(env, srcOps, &srcInfo, SD_LOCK_READ) != SD_SUCCESS) {
        J2dTraceLn(J2D_TRACE_WARNING, "MTLBlitLoops_Blit: could not acquire lock");
        return;
    }

    if (srcInfo.bounds.x2 > srcInfo.bounds.x1 && srcInfo.bounds.y2 > srcInfo.bounds.y1) {
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        if (srcInfo.rasBase) {
            if (srcInfo.bounds.x1 != sx1) {
                const int dx = srcInfo.bounds.x1 - sx1;
                dx1 += dx * (dw / sw);
            }
            if (srcInfo.bounds.y1 != sy1) {
                const int dy = srcInfo.bounds.y1 - sy1;
                dy1 += dy * (dh / sh);
            }
            if (srcInfo.bounds.x2 != sx2) {
                const int dx = srcInfo.bounds.x2 - sx2;
                dx2 += dx * (dw / sw);
            }
            if (srcInfo.bounds.y2 != sy2) {
                const int dy = srcInfo.bounds.y2 - sy2;
                dy2 += dy * (dh / sh);
            }

#ifdef TRACE_BLIT
            J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE,
                    "MTLBlitLoops_Blit [tx=%d, xf=%d, AC=%s]: bdst=%s, src=%p (%dx%d) O=%d premul=%d | (%d, %d, %d, %d)->(%1.2f, %1.2f, %1.2f, %1.2f)",
                    texture, xform, [mtlc getCompositeDescription].cString,
                    getSurfaceDescription(dstOps).cString, srcOps,
                    sx2 - sx1, sy2 - sy1,
                    RasterFormatInfos[srctype].hasAlpha ? 0 : 1, RasterFormatInfos[srctype].isPremult ? 1 : 0,
                    sx1, sy1, sx2, sy2,
                    dx1, dy1, dx2, dy2);
#endif //TRACE_BLIT

            MTLRasterFormatInfo rfi = RasterFormatInfos[srctype];

            if (texture) {
                replaceTextureRegion(mtlc, dest, &srcInfo, &rfi, (int) dx1, (int) dy1, (int) dx2, (int) dy2);
            } else {
                MTLBlitSwToTextureViaPooledTexture(mtlc, &srcInfo, dstOps, &rfi, hint, dx1, dy1, dx2, dy2);
            }
        }
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
}

void copyFromMTLBuffer(void *pDst, id<MTLBuffer> srcBuf, NSUInteger offset, NSUInteger len, BOOL convertFromArgbPre) {
    char *pSrc = (char*)srcBuf.contents + offset;
    if (convertFromArgbPre) {
        NSUInteger pixelLen = len >> 2;
        for (NSUInteger i = 0; i < pixelLen; i++) {
            LoadIntArgbPreTo1IntArgb((jint*)pSrc, 0, i, ((jint*)pDst)[i]);
        }
    } else {
        memcpy(pDst, pSrc, len);
    }
}

/**
 * Specialized blit method for copying a native MTL "Surface" (pbuffer,
 * window, etc.) to a system memory ("Sw") surface.
 */
void
MTLBlitLoops_SurfaceToSwBlit(JNIEnv *env, MTLContext *mtlc,
                             jlong pSrcOps, jlong pDstOps, jint dsttype,
                             jint srcx, jint srcy, jint dstx, jint dsty,
                             jint width, jint height)
{
    J2dTraceLn6(J2D_TRACE_VERBOSE, "MTLBlitLoops_SurfaceToSwBlit: sx=%d sy=%d w=%d h=%d dx=%d dy=%d", srcx, srcy, width, height, dstx, dsty);

    BMTLSDOps *srcOps = (BMTLSDOps *)jlong_to_ptr(pSrcOps);
    SurfaceDataOps *dstOps = (SurfaceDataOps *)jlong_to_ptr(pDstOps);
    SurfaceDataRasInfo srcInfo, dstInfo;

    if (dsttype < 0 || dsttype >= sizeof(RasterFormatInfos)/ sizeof(MTLRasterFormatInfo)) {
        J2dTraceLn1(J2D_TRACE_ERROR, "MTLBlitLoops_SurfaceToSwBlit: destination pixel format %d isn't supported", dsttype);
        return;
    }

    if (width <= 0 || height <= 0) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLBlitLoops_SurfaceToSwBlit: dimensions are non-positive");
        return;
    }

    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(mtlc);
    RETURN_IF_TRUE(width < 0);
    RETURN_IF_TRUE(height < 0);
    NSUInteger w = (NSUInteger)width;
    NSUInteger h = (NSUInteger)height;

    srcInfo.bounds.x1 = srcx;
    srcInfo.bounds.y1 = srcy;
    srcInfo.bounds.x2 = srcx + width;
    srcInfo.bounds.y2 = srcy + height;
    dstInfo.bounds.x1 = dstx;
    dstInfo.bounds.y1 = dsty;
    dstInfo.bounds.x2 = dstx + width;
    dstInfo.bounds.y2 = dsty + height;

    if (dstOps->Lock(env, dstOps, &dstInfo, SD_LOCK_WRITE) != SD_SUCCESS) {
        J2dTraceLn(J2D_TRACE_WARNING,"MTLBlitLoops_SurfaceToSwBlit: could not acquire dst lock");
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

            pDst = PtrPixelsRow(pDst, dstx, dstInfo.pixelStride);
            pDst = PtrPixelsRow(pDst, dsty, dstInfo.scanStride);

            NSUInteger byteLength = w * h * 4; // NOTE: assume that src format is MTLPixelFormatBGRA8Unorm

            // Create MTLBuffer (or use static)
            id<MTLBuffer> mtlbuf;
#ifdef USE_STATIC_BUFFER
            // NOTE: theoretically we can use newBufferWithBytesNoCopy, but pDst must be allocated with special API
            // mtlbuf = [mtlc.device
            //          newBufferWithBytesNoCopy:pDst
            //                            length:(NSUInteger) srcLength
            //                           options:MTLResourceCPUCacheModeDefaultCache
            //                       deallocator:nil];
            //
            // see https://developer.apple.com/documentation/metal/mtldevice/1433382-newbufferwithbytesnocopy?language=objc
            //
            // The storage allocation of the returned new MTLBuffer object is the same as the pointer input value.
            // The existing memory allocation must be covered by a single VM region, typically allocated with vm_allocate or mmap.
            // Memory allocated by malloc is specifically disallowed.

            static id<MTLBuffer> mtlIntermediateBuffer = nil; // need to reimplement with MTLBufferManager
            if (mtlIntermediateBuffer == nil || mtlIntermediateBuffer.length < srcLength) {
                if (mtlIntermediateBuffer != nil) {
                    [mtlIntermediateBuffer release];
                }
                mtlIntermediateBuffer = [mtlc.device newBufferWithLength:srcLength options:MTLResourceCPUCacheModeDefaultCache];
            }
            mtlbuf = mtlIntermediateBuffer;
#else // USE_STATIC_BUFFER
            mtlbuf = [mtlc.device newBufferWithLength:byteLength options:MTLResourceStorageModeShared];
#endif // USE_STATIC_BUFFER

            // Read from surface into MTLBuffer
            // NOTE: using of separate blitCommandBuffer can produce errors (draw into surface (with general cmd-buf)
            // can be unfinished when reading raster from blit cmd-buf).
            // Consider to use [mtlc.encoderManager createBlitEncoder] and [mtlc commitCommandBuffer:JNI_TRUE];
            J2dTraceLn1(J2D_TRACE_VERBOSE, "MTLBlitLoops_SurfaceToSwBlit: source texture %p", srcOps->pTexture);

            id<MTLCommandBuffer> cb = [mtlc createCommandBuffer];
            id<MTLBlitCommandEncoder> blitEncoder = [cb blitCommandEncoder];
            [blitEncoder copyFromTexture:srcOps->pTexture
                            sourceSlice:0
                            sourceLevel:0
                           sourceOrigin:MTLOriginMake(srcx, srcy, 0)
                             sourceSize:MTLSizeMake(w, h, 1)
                               toBuffer:mtlbuf
                      destinationOffset:0 /*offset already taken in: pDst = PtrPixelsRow(pDst, dstx,  dstInfo.pixelStride)*/
                 destinationBytesPerRow:w*4
               destinationBytesPerImage:byteLength];
            [blitEncoder endEncoding];

            // Commit and wait for reading complete
            [cb commit];
            [cb waitUntilCompleted];

            // Perform conversion if necessary
            BOOL convertFromPre = !RasterFormatInfos[dsttype].isPremult && !srcOps->isOpaque;

            if ((dstInfo.scanStride == w * dstInfo.pixelStride) &&
                (height == (dstInfo.bounds.y2 - dstInfo.bounds.y1))) {
                // mtlbuf.contents have same dimensions as of pDst
                copyFromMTLBuffer(pDst, mtlbuf, 0, byteLength, convertFromPre);
            } else {
                // mtlbuf.contents have smaller dimensions than pDst
                // copy each row from mtlbuf.contents at appropriate position in pDst
                // Note : pDst is already addjusted for offsets using PtrAddBytes above

                NSUInteger rowSize = w * dstInfo.pixelStride;
                for (int y = 0; y < height; y++) {
                    copyFromMTLBuffer(pDst, mtlbuf, y * rowSize, rowSize, convertFromPre);
                    pDst = PtrAddBytes(pDst, dstInfo.scanStride);
                }
            }

#ifndef USE_STATIC_BUFFER
            [mtlbuf release];
#endif // USE_STATIC_BUFFER
        }
        SurfaceData_InvokeRelease(env, dstOps, &dstInfo);
    }
    SurfaceData_InvokeUnlock(env, dstOps, &dstInfo);
}

void
MTLBlitLoops_CopyArea(JNIEnv *env,
                      MTLContext *mtlc, BMTLSDOps *dstOps,
                      jint x, jint y, jint width, jint height,
                      jint dx, jint dy)
{
#ifdef DEBUG
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_TRUE, "MTLBlitLoops_CopyArea: bdst=%p [tex=%p] %dx%d | src (%d, %d), %dx%d -> dst (%d, %d)",
            dstOps, dstOps->pTexture, ((id<MTLTexture>)dstOps->pTexture).width, ((id<MTLTexture>)dstOps->pTexture).height, x, y, width, height, dx, dy);
#endif //DEBUG
    jint texWidth = ((id<MTLTexture>)dstOps->pTexture).width;
    jint texHeight = ((id<MTLTexture>)dstOps->pTexture).height;

    SurfaceDataBounds srcBounds, dstBounds;
    srcBounds.x1 = x;
    srcBounds.y1 = y;
    srcBounds.x2 = srcBounds.x1 + width;
    srcBounds.y2 = srcBounds.y1 + height;
    dstBounds.x1 = x + dx;
    dstBounds.y1 = y + dy;
    dstBounds.x2 = dstBounds.x1 + width;
    dstBounds.y2 = dstBounds.y1 + height;

    SurfaceData_IntersectBoundsXYXY(&srcBounds, 0, 0, texWidth, texHeight);
    SurfaceData_IntersectBoundsXYXY(&dstBounds, 0, 0, texWidth, texHeight);
    SurfaceData_IntersectBlitBounds(&dstBounds, &srcBounds, -dx, -dy);

    int srcWidth = (srcBounds.x2 - srcBounds.x1);
    int srcHeight = (srcBounds.y2 - srcBounds.y1);

   if ((srcBounds.x1 < srcBounds.x2 && srcBounds.y1 < srcBounds.y2) &&
       (dstBounds.x1 < dstBounds.x2 && dstBounds.y1 < dstBounds.y2))
   {
        @autoreleasepool {
            struct TxtVertex quadTxVerticesBuffer[6];
            MTLPooledTextureHandle * interHandle =
                [mtlc.texturePool getTexture:texWidth
                                      height:texHeight
                                      format:MTLPixelFormatBGRA8Unorm];
            if (interHandle == nil) {
                J2dTraceLn(J2D_TRACE_ERROR,
                    "MTLBlitLoops_CopyArea: texture handle is null");
                return;
            }
            [[mtlc getCommandBufferWrapper] registerPooledTexture:interHandle];

            id<MTLTexture> interTexture = interHandle.texture;

            /*
             * We need to consider common states like clipping while
             * performing copyArea, thats why we use drawTex2Tex and
             * get encoder with appropriate state from EncoderManager
             * and not directly use MTLBlitCommandEncoder for texture copy.
             */

            // copy content to intermediate texture
            drawTex2Tex(mtlc, dstOps->pTexture, interTexture, dstOps->isOpaque,
                        JNI_FALSE, INTERPOLATION_NEAREST_NEIGHBOR,
                        0, 0, texWidth, texHeight, 0, 0, texWidth, texHeight);

            // copy content with appropriate bounds to destination texture
            drawTex2Tex(mtlc, interTexture, dstOps->pTexture, JNI_FALSE,
                        dstOps->isOpaque, INTERPOLATION_NEAREST_NEIGHBOR,
                        srcBounds.x1, srcBounds.y1, srcBounds.x2, srcBounds.y2,
                        dstBounds.x1, dstBounds.y1, dstBounds.x2, dstBounds.y2);
            [mtlc.encoderManager endEncoder];
            MTLCommandBufferWrapper * cbwrapper =
                [mtlc pullCommandBufferWrapper];
            id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
            [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
                [cbwrapper release];
            }];
            [commandbuf commit];
        }
   }
}
