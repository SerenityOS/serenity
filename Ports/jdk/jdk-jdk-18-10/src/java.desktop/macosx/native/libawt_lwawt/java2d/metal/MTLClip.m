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

#include "MTLClip.h"

#include "MTLContext.h"
#include "MTLStencilManager.h"
#include "common.h"

static MTLRenderPipelineDescriptor * templateStencilPipelineDesc = nil;

static void initTemplatePipelineDescriptors() {
    if (templateStencilPipelineDesc != nil)
        return;

    MTLVertexDescriptor *vertDesc = [[MTLVertexDescriptor new] autorelease];
    vertDesc.attributes[VertexAttributePosition].format = MTLVertexFormatFloat2;
    vertDesc.attributes[VertexAttributePosition].offset = 0;
    vertDesc.attributes[VertexAttributePosition].bufferIndex = MeshVertexBuffer;
    vertDesc.layouts[MeshVertexBuffer].stride = sizeof(struct Vertex);
    vertDesc.layouts[MeshVertexBuffer].stepRate = 1;
    vertDesc.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;

    templateStencilPipelineDesc = [MTLRenderPipelineDescriptor new];
    templateStencilPipelineDesc.sampleCount = 1;
    templateStencilPipelineDesc.vertexDescriptor = vertDesc;
    templateStencilPipelineDesc.colorAttachments[0].pixelFormat = MTLPixelFormatR8Uint; // A byte buffer format
    templateStencilPipelineDesc.stencilAttachmentPixelFormat = MTLPixelFormatStencil8;
    templateStencilPipelineDesc.label = @"template_stencil";
}

@implementation MTLClip {
    jint _clipType;
    MTLScissorRect  _clipRect;
    MTLContext* _mtlc;
    BMTLSDOps*  _dstOps;
    BOOL _stencilMaskGenerationInProgress;
    BOOL _stencilMaskGenerationStarted;
    BOOL _clipReady;
    MTLOrigin _clipShapeOrigin;
    MTLSize _clipShapeSize;
}

@synthesize dstOps = _dstOps;

- (id)init {
    self = [super init];
    if (self) {
        _clipType = NO_CLIP;
        _mtlc = nil;
        _dstOps = NULL;
        _stencilMaskGenerationInProgress = NO;
        _stencilMaskGenerationStarted = NO;
        _clipReady = NO;
    }
    return self;
}

- (BOOL)isEqual:(MTLClip *)other {
    if (self == other)
        return YES;
    if (_stencilMaskGenerationInProgress == JNI_TRUE)
        return other->_stencilMaskGenerationInProgress == JNI_TRUE;
    if (_clipType != other->_clipType)
        return NO;
    if (_clipType == NO_CLIP)
        return YES;
    if (_clipType == RECT_CLIP) {
        return _clipRect.x == other->_clipRect.x && _clipRect.y == other->_clipRect.y
               && _clipRect.width == other->_clipRect.width && _clipRect.height == other->_clipRect.height;
    }

    // NOTE: can compare stencil-data pointers here
    return YES;
}

- (BOOL)isShape {
    return _clipType == SHAPE_CLIP;
}

- (BOOL)isRect __unused {
    return _clipType == RECT_CLIP;
}

- (const MTLScissorRect * _Nullable) getRect {
    return _clipType == RECT_CLIP ? &_clipRect : NULL;
}

- (void)copyFrom:(MTLClip *)other {
    _clipType = other->_clipType;
    _stencilMaskGenerationInProgress = other->_stencilMaskGenerationInProgress;
    _dstOps = other->_dstOps;
    _mtlc = other->_mtlc;
    if (other->_clipType == RECT_CLIP) {
        _clipRect = other->_clipRect;
    }
}

- (void)reset {
    _clipType = NO_CLIP;
    _stencilMaskGenerationInProgress = JNI_FALSE;
}


- (void)setClipRectX1:(jint)x1 Y1:(jint)y1 X2:(jint)x2 Y2:(jint)y2 {
    if (_clipType == SHAPE_CLIP) {
        _dstOps = NULL;
    }

    if (x1 >= x2 || y1 >= y2) {
        J2dTraceLn4(J2D_TRACE_ERROR, "MTLClip.setClipRect: invalid rect: x1=%d y1=%d x2=%d y2=%d", x1, y1, x2, y2);
        _clipType = NO_CLIP;
    }

    const jint width = x2 - x1;
    const jint height = y2 - y1;

    J2dTraceLn4(J2D_TRACE_INFO, "MTLClip.setClipRect: x=%d y=%d w=%d h=%d", x1, y1, width, height);

    _clipRect.x = (NSUInteger)((x1 >= 0) ? x1 : 0);
    _clipRect.y = (NSUInteger)((y1 >= 0) ? y1 : 0);
    _clipRect.width = (NSUInteger)((width >= 0) ? width : 0);
    _clipRect.height = (NSUInteger)((height >= 0) ? height : 0);
    _clipType = RECT_CLIP;
}

- (void)beginShapeClip:(BMTLSDOps *)dstOps context:(MTLContext *)mtlc {
    _stencilMaskGenerationInProgress = YES;
    _mtlc = mtlc;
    if ((dstOps == NULL) || (dstOps->pStencilData == NULL) || (dstOps->pStencilTexture == NULL)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLContext_beginShapeClip: stencil render target or stencil texture is NULL");
        return;
    }

    // Clear the stencil render buffer & stencil texture
    @autoreleasepool {
        if (dstOps->width <= 0 || dstOps->height <= 0) {
          return;
        }

        _clipShapeSize = MTLSizeMake(0, 0, 1);
        // Use out of bounds origin to correctly calculate shape boundaries
        _clipShapeOrigin = MTLOriginMake((NSUInteger) dstOps->width, (NSUInteger) dstOps->height, 0);
        _dstOps = dstOps;
    }
}

- (void)endShapeClip:(BMTLSDOps *)dstOps context:(MTLContext *)mtlc {

    if ((dstOps == NULL) || (dstOps->pStencilData == NULL) || (dstOps->pStencilTexture == NULL)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLContext_endShapeClip: stencil render target or stencil texture is NULL");
        return;
    }

    // Complete the rendering to the stencil buffer ------------
    [mtlc.encoderManager endEncoder];

    MTLCommandBufferWrapper* cbWrapper = [mtlc pullCommandBufferWrapper];

    id<MTLCommandBuffer> commandBuffer = [cbWrapper getCommandBuffer];
    [commandBuffer addCompletedHandler:^(id <MTLCommandBuffer> c) {
        [cbWrapper release];
    }];

    [commandBuffer commit];
    _stencilMaskGenerationInProgress = NO;
    _stencilMaskGenerationStarted = NO;
    _dstOps = dstOps;
    _clipType = SHAPE_CLIP;
    _clipReady = NO;
}

- (void)setMaskGenerationPipelineState:(id<MTLRenderCommandEncoder>)encoder
                  destWidth:(NSUInteger)dw
                 destHeight:(NSUInteger)dh
       pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();

    // A  PipelineState for rendering to a byte-buffered texture that will be used as a stencil
    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:templateStencilPipelineDesc
                                                                         vertexShaderId:@"vert_stencil"
                                                                       fragmentShaderId:@"frag_stencil"
                                                                          stencilNeeded:YES];
    [encoder setRenderPipelineState:pipelineState];

    struct FrameUniforms uf; // color is ignored while writing to stencil buffer
    memset(&uf, 0, sizeof(uf));
    [encoder setVertexBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];

    _clipRect.x = 0;
    _clipRect.y = 0;
    _clipRect.width = dw;
    _clipRect.height = dh;

    [encoder setDepthStencilState: _mtlc.stencilManager.genStencilState];
    [encoder setStencilReferenceValue:255];
    [encoder setScissorRect:_clipRect]; // just for insurance (to reset possible clip from previous drawing)
}

- (void)setScissorOrStencil:(id<MTLRenderCommandEncoder>)encoder
                  destWidth:(NSUInteger)dw
                 destHeight:(NSUInteger)dh
                     device:(id<MTLDevice>)device
{
    if (_clipType == NO_CLIP || _clipType == SHAPE_CLIP) {
        _clipRect.x = 0;
        _clipRect.y = 0;
        _clipRect.width = dw;
        _clipRect.height = dh;
    }

    // Clamping clip rect to the destination area
    MTLScissorRect rect = _clipRect;

    if (rect.x > dw) {
        rect.x = dw;
    }

    if (rect.y > dh) {
        rect.y = dh;
    }

    if (rect.x + rect.width > dw) {
        rect.width = dw - rect.x;
    }

    if (rect.y + rect.height > dh) {
        rect.height = dh - rect.y;
    }

    [encoder setScissorRect:rect];
    if (_clipType == NO_CLIP || _clipType == RECT_CLIP) {
        // NOTE: It seems that we can use the same encoder (with disabled stencil test) when mode changes from SHAPE to RECT.
        // But [encoder setDepthStencilState:nil] causes crash, so we have to recreate encoder in such case.
        // So we can omit [encoder setDepthStencilState:nil] here.
        return;
    }

    if (_clipType == SHAPE_CLIP) {
        // Enable stencil test
        [encoder setDepthStencilState:_mtlc.stencilManager.stencilState];
        [encoder setStencilReferenceValue:0xFF];
    }
}

- (NSString *)getDescription __unused {
    if (_clipType == NO_CLIP) {
        return @"NO_CLIP";
    }
    if (_clipType == RECT_CLIP) {
        return [NSString stringWithFormat:@"RECT_CLIP [%lu,%lu - %lux%lu]", _clipRect.x, _clipRect.y, _clipRect.width, _clipRect.height];
    }
    return [NSString stringWithFormat:@"SHAPE_CLIP"];
}

- (id<MTLTexture>) stencilTextureRef {
    if (_dstOps == NULL) return nil;

    return _dstOps->pStencilTexture;;
}

- (NSUInteger)shapeX {
    return _clipShapeOrigin.x;
}

- (void)setShapeX:(NSUInteger)shapeX {
    _clipShapeOrigin.x = shapeX;
}

- (NSUInteger)shapeY {
    return _clipShapeOrigin.y;
}

- (void)setShapeY:(NSUInteger)shapeY {
    _clipShapeOrigin.y = shapeY;
}

- (NSUInteger)shapeWidth {
    return _clipShapeSize.width;
}

- (void)setShapeWidth:(NSUInteger)shapeWidth {
    _clipShapeSize.width = shapeWidth;
}

- (NSUInteger)shapeHeight {
    return _clipShapeSize.height;
}

- (void)setShapeHeight:(NSUInteger)shapeHeight {
    _clipShapeSize.height = shapeHeight;
}


@end
