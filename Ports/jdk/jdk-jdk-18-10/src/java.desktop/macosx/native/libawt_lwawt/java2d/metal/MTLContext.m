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

#include "sun_java2d_SunGraphics2D.h"

#include "jlong.h"
#import "MTLContext.h"
#include "MTLRenderQueue.h"
#import "MTLSamplerManager.h"
#import "MTLStencilManager.h"


extern jboolean MTLSD_InitMTLWindow(JNIEnv *env, MTLSDOps *mtlsdo);

static struct TxtVertex verts[PGRAM_VERTEX_COUNT] = {
        {{-1.0, 1.0}, {0.0, 0.0}},
        {{1.0, 1.0}, {1.0, 0.0}},
        {{1.0, -1.0}, {1.0, 1.0}},
        {{1.0, -1.0}, {1.0, 1.0}},
        {{-1.0, -1.0}, {0.0, 1.0}},
        {{-1.0, 1.0}, {0.0, 0.0}}
};

MTLTransform* tempTransform = nil;

@implementation MTLCommandBufferWrapper {
    id<MTLCommandBuffer> _commandBuffer;
    NSMutableArray * _pooledTextures;
    NSLock* _lock;
}

- (id) initWithCommandBuffer:(id<MTLCommandBuffer>)cmdBuf {
    self = [super init];
    if (self) {
        _commandBuffer = [cmdBuf retain];
        _pooledTextures = [[NSMutableArray alloc] init];
        _lock = [[NSLock alloc] init];
    }
    return self;
}

- (id<MTLCommandBuffer>) getCommandBuffer {
    return _commandBuffer;
}

- (void) onComplete { // invoked from completion handler in some pooled thread
    [_lock lock];
    @try {
        for (int c = 0; c < [_pooledTextures count]; ++c)
            [[_pooledTextures objectAtIndex:c] releaseTexture];
        [_pooledTextures removeAllObjects];
    } @finally {
        [_lock unlock];
    }

}

- (void) registerPooledTexture:(MTLPooledTextureHandle *)handle {
    [_lock lock];
    @try {
        [_pooledTextures addObject:handle];
    } @finally {
        [_lock unlock];
    }
}

- (void) dealloc {
    [self onComplete];

    [_pooledTextures release];
    _pooledTextures = nil;

    [_commandBuffer release];
    _commandBuffer = nil;

    [_lock release];
    _lock = nil;
    [super dealloc];
}

@end

@implementation MTLContext {
    MTLCommandBufferWrapper * _commandBufferWrapper;

    MTLComposite *     _composite;
    MTLPaint *         _paint;
    MTLTransform *     _transform;
    MTLTransform *     _tempTransform;
    MTLClip *          _clip;
    NSObject*          _bufImgOp; // TODO: pass as parameter of IsoBlit

    EncoderManager * _encoderManager;
    MTLSamplerManager * _samplerManager;
    MTLStencilManager * _stencilManager;
}

@synthesize textureFunction,
            vertexCacheEnabled, aaEnabled, device, pipelineStateStorage,
            commandQueue, blitCommandQueue, vertexBuffer,
            texturePool, paint=_paint, encoderManager=_encoderManager,
            samplerManager=_samplerManager, stencilManager=_stencilManager;

extern void initSamplers(id<MTLDevice> device);

- (id)initWithDevice:(id<MTLDevice>)d shadersLib:(NSString*)shadersLib {
    self = [super init];
    if (self) {
        // Initialization code here.
        device = d;

        pipelineStateStorage = [[MTLPipelineStatesStorage alloc] initWithDevice:device shaderLibPath:shadersLib];
        if (pipelineStateStorage == nil) {
            J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLContext.initWithDevice(): Failed to initialize MTLPipelineStatesStorage.");
            return nil;
        }

        texturePool = [[MTLTexturePool alloc] initWithDevice:device];

        vertexBuffer = [device newBufferWithBytes:verts
                                           length:sizeof(verts)
                                          options:MTLResourceCPUCacheModeDefaultCache];

        _encoderManager = [[EncoderManager alloc] init];
        [_encoderManager setContext:self];
        _samplerManager = [[MTLSamplerManager alloc] initWithDevice:device];
        _stencilManager = [[MTLStencilManager alloc] initWithDevice:device];
        _composite = [[MTLComposite alloc] init];
        _paint = [[MTLPaint alloc] init];
        _transform = [[MTLTransform alloc] init];
        _clip = [[MTLClip alloc] init];
        _bufImgOp = nil;

        _commandBufferWrapper = nil;

        // Create command queue
        commandQueue = [device newCommandQueue];
        blitCommandQueue = [device newCommandQueue];

        _tempTransform = [[MTLTransform alloc] init];
    }
    return self;
}

- (void)dealloc {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.dealloc");

    // TODO : Check that texturePool is completely released.
    // texturePool content is released in MTLCommandBufferWrapper.onComplete()
    //self.texturePool = nil;
    self.vertexBuffer = nil;
    self.commandQueue = nil;
    self.blitCommandQueue = nil;
    self.pipelineStateStorage = nil;

    if (_encoderManager != nil) {
        [_encoderManager release];
        _encoderManager = nil;
    }

    if (_samplerManager != nil) {
        [_samplerManager release];
        _samplerManager = nil;
    }

    if (_stencilManager != nil) {
        [_stencilManager release];
        _stencilManager = nil;
    }

    if (_composite != nil) {
        [_composite release];
        _composite = nil;
    }

    if (_paint != nil) {
        [_paint release];
        _paint = nil;
    }

    if (_transform != nil) {
        [_transform release];
        _transform = nil;
    }

    if (_tempTransform != nil) {
        [_tempTransform release];
        _tempTransform = nil;
    }

    if (_clip != nil) {
        [_clip release];
        _clip = nil;
    }

    [super dealloc];
}

- (void) reset {
    J2dTraceLn(J2D_TRACE_VERBOSE, "MTLContext : reset");

    // Add code for context state reset here
}

 - (MTLCommandBufferWrapper *) getCommandBufferWrapper {
    if (_commandBufferWrapper == nil) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "MTLContext : commandBuffer is NULL");
        // NOTE: Command queues are thread-safe and allow multiple outstanding command buffers to be encoded simultaneously.
        _commandBufferWrapper = [[MTLCommandBufferWrapper alloc] initWithCommandBuffer:[self.commandQueue commandBuffer]];// released in [layer blitTexture]
    }
    return _commandBufferWrapper;
}

- (MTLCommandBufferWrapper *) pullCommandBufferWrapper {
    MTLCommandBufferWrapper * result = _commandBufferWrapper;
    _commandBufferWrapper = nil;
    return result;
}

+ (MTLContext*) setSurfacesEnv:(JNIEnv*)env src:(jlong)pSrc dst:(jlong)pDst {
    BMTLSDOps *srcOps = (BMTLSDOps *)jlong_to_ptr(pSrc);
    BMTLSDOps *dstOps = (BMTLSDOps *)jlong_to_ptr(pDst);
    MTLContext *mtlc = NULL;

    if (srcOps == NULL || dstOps == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLContext_SetSurfaces: ops are null");
        return NULL;
    }

    J2dTraceLn6(J2D_TRACE_VERBOSE, "MTLContext_SetSurfaces: bsrc=%p (tex=%p type=%d), bdst=%p (tex=%p type=%d)", srcOps, srcOps->pTexture, srcOps->drawableType, dstOps, dstOps->pTexture, dstOps->drawableType);

    if (dstOps->drawableType == MTLSD_TEXTURE) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "MTLContext_SetSurfaces: texture cannot be used as destination");
        return NULL;
    }

    if (dstOps->drawableType == MTLSD_UNDEFINED) {
        // initialize the surface as an MTLSD_WINDOW
        if (!MTLSD_InitMTLWindow(env, dstOps)) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "MTLContext_SetSurfaces: could not init MTL window");
            return NULL;
        }
    }

    // make the context current
    MTLSDOps *dstMTLOps = (MTLSDOps *)dstOps->privOps;
    mtlc = dstMTLOps->configInfo->context;

    if (mtlc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "MTLContext_SetSurfaces: could not make context current");
        return NULL;
    }

    return mtlc;
}

- (void)resetClip {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.resetClip");
    [_clip reset];
}

- (void)setClipRectX1:(jint)x1 Y1:(jint)y1 X2:(jint)x2 Y2:(jint)y2 {
    J2dTraceLn4(J2D_TRACE_INFO, "MTLContext.setClipRect: %d,%d - %d,%d", x1, y1, x2, y2);
    [_clip setClipRectX1:x1 Y1:y1 X2:x2 Y2:y2];
}

- (void)beginShapeClip:(BMTLSDOps *)dstOps {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.beginShapeClip");
    [_clip beginShapeClip:dstOps context:self];

    // Store the current transform as we need to use identity transform
    // for clip spans rendering
    [_tempTransform copyFrom:_transform];
    [self resetTransform];
}

- (void)endShapeClip:(BMTLSDOps *)dstOps {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.endShapeClip");
    [_clip endShapeClip:dstOps context:self];

    // Reset transform for further rendering
    [_transform copyFrom:_tempTransform];
}

- (void)resetComposite {
    J2dTraceLn(J2D_TRACE_VERBOSE, "MTLContext_ResetComposite");
    [_composite reset];
}

- (void)setAlphaCompositeRule:(jint)rule extraAlpha:(jfloat)extraAlpha
                        flags:(jint)flags {
    J2dTraceLn3(J2D_TRACE_INFO, "MTLContext_SetAlphaComposite: rule=%d, extraAlpha=%1.2f, flags=%d", rule, extraAlpha, flags);

    [_composite setRule:rule extraAlpha:extraAlpha];
}

- (NSString*)getCompositeDescription {
    return [_composite getDescription];
}

- (NSString*)getPaintDescription {
    return [_paint getDescription];
}

- (void)setXorComposite:(jint)xp {
    J2dTraceLn1(J2D_TRACE_INFO, "MTLContext.setXorComposite: xorPixel=%08x", xp);

    [_composite setXORComposite:xp];
}

- (jboolean) useXORComposite {
    return ([_composite getCompositeState] == sun_java2d_SunGraphics2D_COMP_XOR);
}

- (void)resetTransform {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext_ResetTransform");
    [_transform resetTransform];
}

- (void)setTransformM00:(jdouble) m00 M10:(jdouble) m10
                    M01:(jdouble) m01 M11:(jdouble) m11
                    M02:(jdouble) m02 M12:(jdouble) m12 {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext_SetTransform");
    [_transform setTransformM00:m00 M10:m10 M01:m01 M11:m11 M02:m02 M12:m12];
}

- (void)resetPaint {
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.resetPaint");
    self.paint = [[[MTLPaint alloc] init] autorelease];
}

- (void)setColorPaint:(int)pixel {
    J2dTraceLn5(J2D_TRACE_INFO, "MTLContext.setColorPaint: pixel=%08x [r=%d g=%d b=%d a=%d]", pixel, (pixel >> 16) & (0xFF), (pixel >> 8) & 0xFF, (pixel) & 0xFF, (pixel >> 24) & 0xFF);
    self.paint = [[[MTLColorPaint alloc] initWithColor:pixel] autorelease];
}

- (void)setGradientPaintUseMask:(jboolean)useMask
                         cyclic:(jboolean)cyclic
                             p0:(jdouble)p0
                             p1:(jdouble)p1
                             p3:(jdouble)p3
                         pixel1:(jint)pixel1
                         pixel2:(jint) pixel2
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.setGradientPaintUseMask");
    self.paint = [[[MTLGradPaint alloc] initWithUseMask:useMask
                                                cyclic:cyclic
                                                    p0:p0
                                                    p1:p1
                                                    p3:p3
                                                pixel1:pixel1
                                                pixel2:pixel2] autorelease];
}

- (void)setLinearGradientPaint:(jboolean)useMask
                        linear:(jboolean)linear
                   cycleMethod:(jint)cycleMethod
                                // 0 - NO_CYCLE
                                // 1 - REFLECT
                                // 2 - REPEAT

                      numStops:(jint)numStops
                            p0:(jfloat)p0
                            p1:(jfloat)p1
                            p3:(jfloat)p3
                     fractions:(jfloat*)fractions
                        pixels:(jint*)pixels
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.setLinearGradientPaint");
    self.paint = [[[MTLLinearGradPaint alloc] initWithUseMask:useMask
                       linear:linear
                  cycleMethod:cycleMethod
                     numStops:numStops
                           p0:p0
                           p1:p1
                           p3:p3
                    fractions:fractions
                       pixels:pixels] autorelease];
}

- (void)setRadialGradientPaint:(jboolean)useMask
                        linear:(jboolean)linear
                   cycleMethod:(jboolean)cycleMethod
                      numStops:(jint)numStops
                           m00:(jfloat)m00
                           m01:(jfloat)m01
                           m02:(jfloat)m02
                           m10:(jfloat)m10
                           m11:(jfloat)m11
                           m12:(jfloat)m12
                        focusX:(jfloat)focusX
                     fractions:(void *)fractions
                        pixels:(void *)pixels
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLContext.setRadialGradientPaint");
    self.paint = [[[MTLRadialGradPaint alloc] initWithUseMask:useMask
                                                      linear:linear
                                                 cycleMethod:cycleMethod
                                                    numStops:numStops
                                                         m00:m00
                                                         m01:m01
                                                         m02:m02
                                                         m10:m10
                                                         m11:m11
                                                         m12:m12
                                                      focusX:focusX
                                                   fractions:fractions
                                                      pixels:pixels] autorelease];
}

- (void)setTexturePaint:(jboolean)useMask
                pSrcOps:(jlong)pSrcOps
                 filter:(jboolean)filter
                    xp0:(jdouble)xp0
                    xp1:(jdouble)xp1
                    xp3:(jdouble)xp3
                    yp0:(jdouble)yp0
                    yp1:(jdouble)yp1
                    yp3:(jdouble)yp3
{
    BMTLSDOps *srcOps = (BMTLSDOps *)jlong_to_ptr(pSrcOps);

    if (srcOps == NULL || srcOps->pTexture == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLContext_setTexturePaint: texture paint - texture is null");
        return;
    }

    J2dTraceLn1(J2D_TRACE_INFO, "MTLContext.setTexturePaint [tex=%p]", srcOps->pTexture);

    self.paint = [[[MTLTexturePaint alloc] initWithUseMask:useMask
                                                textureID:srcOps->pTexture
                                                  isOpaque:srcOps->isOpaque
                                                   filter:filter
                                                      xp0:xp0
                                                      xp1:xp1
                                                      xp3:xp3
                                                      yp0:yp0
                                                      yp1:yp1
                                                      yp3:yp3] autorelease];
}

- (id<MTLCommandBuffer>)createCommandBuffer {
    return [self.commandQueue commandBuffer];
}

/*
 * This should be exclusively used only for final blit
 * and present of CAMetalDrawable in MTLLayer
 */
- (id<MTLCommandBuffer>)createBlitCommandBuffer {
    return [self.blitCommandQueue commandBuffer];
}

-(void)setBufImgOp:(NSObject*)bufImgOp {
    if (_bufImgOp != nil) {
        [_bufImgOp release]; // context owns bufImgOp object
    }
    _bufImgOp = bufImgOp;
}

-(NSObject*)getBufImgOp {
    return _bufImgOp;
}

@end
