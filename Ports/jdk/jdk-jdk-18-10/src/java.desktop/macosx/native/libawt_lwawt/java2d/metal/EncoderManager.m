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

#include "EncoderManager.h"
#include "MTLContext.h"
#include "sun_java2d_SunGraphics2D.h"
#import "common.h"

// NOTE: uncomment to disable comparing cached encoder states with requested (for debugging)
// #define ALWAYS_UPDATE_ENCODER_STATES

const SurfaceRasterFlags defaultRasterFlags = { JNI_FALSE, JNI_TRUE };

// Internal utility class that represents the set of 'mutable' encoder properties
@interface EncoderStates : NSObject
@property (readonly) MTLClip * clip;

- (id)init;
- (void)dealloc;

- (void)reset:(id<MTLTexture>)destination
           isDstOpaque:(jboolean)isDstOpaque
    isDstPremultiplied:(jboolean)isDstPremultiplied
                  isAA:(jboolean)isAA
                  isText:(jboolean)isText
                  isLCD:(jboolean)isLCD;

- (void)updateEncoder:(id<MTLRenderCommandEncoder>)encoder
              context:(MTLContext *)mtlc
        renderOptions:(const RenderOptions *)renderOptions
          forceUpdate:(jboolean)forceUpdate;
@property (assign) jboolean aa;
@property (assign) jboolean text;
@property (assign) jboolean lcd;
@property (assign) jboolean aaShader;
@property (retain) MTLPaint* paint;
@end

@implementation EncoderStates {
    MTLPipelineStatesStorage * _pipelineStateStorage;
    id<MTLDevice> _device;

    // Persistent encoder properties
    id<MTLTexture> _destination;
    SurfaceRasterFlags _dstFlags;

    jboolean _isAA;
    jboolean _isText;
    jboolean _isLCD;
    jboolean _isAAShader;

    //
    // Cached 'mutable' states of encoder
    //

    // Composite rule and source raster flags (it affects the CAD-multipliers (of pipelineState))
    MTLComposite * _composite;
    SurfaceRasterFlags _srcFlags;

    // Paint mode (it affects shaders (of pipelineState) and corresponding buffers)
    MTLPaint * _paint;

    // If true, indicates that encoder is used for texture drawing (user must do [encoder setFragmentTexture:] before drawing)
    jboolean _isTexture;
    int _interpolationMode;

    // Clip rect or stencil
    MTLClip * _clip;

    // Transform (affects transformation inside vertex shader)
    MTLTransform * _transform;
}
@synthesize aa = _isAA;
@synthesize text = _isText;
@synthesize lcd = _isLCD;
@synthesize aaShader = _isAAShader;
@synthesize paint = _paint;

- (id)init {
    self = [super init];
    if (self) {
        _destination = nil;
        _composite = [[MTLComposite alloc] init];
        _paint = [[MTLPaint alloc] init];
        _transform = [[MTLTransform alloc] init];
        _clip = [[MTLClip alloc] init];
    }
    return self;
}

- (void)dealloc {
    [_composite release];
    [_paint release];
    [_transform release];
    [super dealloc];
}

- (void)setContext:(MTLContext * _Nonnull)mtlc {
    self->_pipelineStateStorage = mtlc.pipelineStateStorage;
    self->_device = mtlc.device;
}

- (void)reset:(id<MTLTexture>)destination
           isDstOpaque:(jboolean)isDstOpaque
    isDstPremultiplied:(jboolean)isDstPremultiplied
                  isAA:(jboolean)isAA
                  isText:(jboolean)isText
                  isLCD:(jboolean)isLCD {
    _destination = destination;
    _dstFlags.isOpaque = isDstOpaque;
    _dstFlags.isPremultiplied = isDstPremultiplied;
    _isAA = isAA;
    _isText = isText;
    _isLCD = isLCD;
    // NOTE: probably it's better to invalidate/reset all cached states now
}

- (void)updateEncoder:(id<MTLRenderCommandEncoder>)encoder
              context:(MTLContext *)mtlc
        renderOptions:(const RenderOptions *)renderOptions
          forceUpdate:(jboolean)forceUpdate
{
    // 1. Process special case for stencil mask generation
    if (mtlc.clip.stencilMaskGenerationInProgress == JNI_TRUE) {
        // use separate pipeline state for stencil generation
        if (forceUpdate || (_clip.stencilMaskGenerationInProgress != JNI_TRUE)) {
            [_clip copyFrom:mtlc.clip];
            [_clip setMaskGenerationPipelineState:encoder
                                        destWidth:_destination.width
                                       destHeight:_destination.height
                             pipelineStateStorage:_pipelineStateStorage];
        }

        [self updateTransform:encoder transform:mtlc.transform forceUpdate:forceUpdate];
        return;
    }

    // 2. Otherwise update all 'mutable' properties of encoder
    [self updatePipelineState:encoder
                      context:mtlc
                renderOptions:renderOptions
                  forceUpdate:forceUpdate];
    [self updateTransform:encoder transform:mtlc.transform forceUpdate:forceUpdate];
    [self updateClip:encoder clip:mtlc.clip forceUpdate:forceUpdate];
}

//
// Internal methods that update states when necessary (compare with cached states)
//

// Updates pipelineState (and corresponding buffers) with use of paint+composite+flags
- (void)updatePipelineState:(id<MTLRenderCommandEncoder>)encoder
                    context:(MTLContext *)mtlc
              renderOptions:(const RenderOptions *)renderOptions
                forceUpdate:(jboolean)forceUpdate
{
    if (!forceUpdate
        && [_paint isEqual:mtlc.paint]
        && [_composite isEqual:mtlc.composite]
        && (_isTexture == renderOptions->isTexture && (!renderOptions->isTexture || _interpolationMode == renderOptions->interpolation)) // interpolation is used only in texture mode
        && _isAA == renderOptions->isAA
        && _isAAShader == renderOptions->isAAShader
        && _isText == renderOptions->isText
        && _isLCD == renderOptions->isLCD
        && _srcFlags.isOpaque == renderOptions->srcFlags.isOpaque && _srcFlags.isPremultiplied == renderOptions->srcFlags.isPremultiplied)
        return;

    self.paint = mtlc.paint;
    [_composite copyFrom:mtlc.composite];
    _isTexture = renderOptions->isTexture;
    _interpolationMode = renderOptions->interpolation;
    _isAA = renderOptions->isAA;
    _isAAShader = renderOptions->isAAShader;
    _isText = renderOptions->isText;
    _isLCD = renderOptions->isLCD;
    _srcFlags = renderOptions->srcFlags;

    if ((jint)[mtlc.composite getCompositeState] == sun_java2d_SunGraphics2D_COMP_XOR) {

        [mtlc.paint setXorModePipelineState:encoder
                               context:mtlc
                         renderOptions:renderOptions
                  pipelineStateStorage:_pipelineStateStorage];
    } else {
        [mtlc.paint  setPipelineState:encoder
                              context:mtlc
                        renderOptions:renderOptions
                 pipelineStateStorage:_pipelineStateStorage];
    }
}

- (void) updateClip:(id<MTLRenderCommandEncoder>)encoder clip:(MTLClip *)clip forceUpdate:(jboolean)forceUpdate
{
    if (clip.stencilMaskGenerationInProgress == JNI_TRUE) {
        // don't set setScissorOrStencil when generation in progress
        return;
    }

    if (!forceUpdate && [_clip isEqual:clip])
        return;

    [_clip copyFrom:clip];
    [_clip setScissorOrStencil:encoder
                     destWidth:_destination.width
                    destHeight:_destination.height
                        device:_device];
}

- (void)updateTransform:(id <MTLRenderCommandEncoder>)encoder
              transform:(MTLTransform *)transform
            forceUpdate:(jboolean)forceUpdate
{
    if (!forceUpdate
        && [_transform isEqual:transform])
        return;

    [_transform copyFrom:transform];
    [_transform setVertexMatrix:encoder
                        destWidth:_destination.width
                       destHeight:_destination.height];
}

@end

@implementation EncoderManager {
    MTLContext * _mtlc; // used to obtain CommandBufferWrapper and Composite/Paint/Transform

    id<MTLRenderCommandEncoder> _encoder;

    // 'Persistent' properties of encoder
    id<MTLTexture> _destination;
    id<MTLTexture> _aaDestination;
    BOOL _useStencil;

    // 'Mutable' states of encoder
    EncoderStates * _encoderStates;
}

- (id _Nonnull)init {
    self = [super init];
    if (self) {
        _encoder = nil;
        _destination = nil;
        _aaDestination = nil;
        _useStencil = NO;
        _encoderStates = [[EncoderStates alloc] init];

    }
    return self;
}

- (void)dealloc {
    [_encoderStates release];
    [super dealloc];
}

- (void)setContext:(MTLContex * _Nonnull)mtlc {
    self->_mtlc = mtlc;
    [self->_encoderStates setContext:mtlc];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getRenderEncoder:(const BMTLSDOps * _Nonnull)dstOps
{
    return [self getRenderEncoder:dstOps->pTexture isDstOpaque:dstOps->isOpaque];
}

- (id<MTLRenderCommandEncoder> _Nonnull)getAARenderEncoder:(const BMTLSDOps * _Nonnull)dstOps {
  id<MTLTexture> dstTxt = dstOps->pTexture;
  RenderOptions roptions = {JNI_FALSE, JNI_TRUE, INTERPOLATION_NEAREST_NEIGHBOR, defaultRasterFlags, {dstOps->isOpaque, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_FALSE};
  return [self getEncoder:dstTxt renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull)getAAShaderRenderEncoder:(const BMTLSDOps * _Nonnull)dstOps
{
    RenderOptions roptions = {JNI_FALSE, JNI_FALSE, INTERPOLATION_NEAREST_NEIGHBOR, defaultRasterFlags, {dstOps->isOpaque, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_TRUE};
    return [self getEncoder:dstOps->pTexture renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull)getRenderEncoder:(id<MTLTexture> _Nonnull)dest
                                             isDstOpaque:(bool)isOpaque
{
    RenderOptions roptions = {JNI_FALSE, JNI_FALSE, INTERPOLATION_NEAREST_NEIGHBOR, defaultRasterFlags, {isOpaque, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_FALSE};
    return [self getEncoder:dest renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getTextureEncoder:(const BMTLSDOps * _Nonnull)dstOps
                                      isSrcOpaque:(bool)isSrcOpaque
{
    return [self getTextureEncoder:dstOps->pTexture
                       isSrcOpaque:isSrcOpaque
                       isDstOpaque:dstOps->isOpaque
                     interpolation:INTERPOLATION_NEAREST_NEIGHBOR];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                               isSrcOpaque:(bool)isSrcOpaque
                                               isDstOpaque:(bool)isDstOpaque
{
    return [self getTextureEncoder:dest
                       isSrcOpaque:isSrcOpaque
                       isDstOpaque:isDstOpaque
                     interpolation:INTERPOLATION_NEAREST_NEIGHBOR
                              isAA:JNI_FALSE];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getLCDEncoder:(id<MTLTexture> _Nonnull)dest
                                               isSrcOpaque:(bool)isSrcOpaque
                                               isDstOpaque:(bool)isDstOpaque
{
    RenderOptions roptions = {JNI_TRUE, JNI_FALSE, INTERPOLATION_NEAREST_NEIGHBOR, {isSrcOpaque, JNI_TRUE }, {isDstOpaque, JNI_TRUE}, JNI_FALSE, JNI_TRUE, JNI_FALSE};
    return [self getEncoder:dest renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                      isSrcOpaque:(bool)isSrcOpaque
                                      isDstOpaque:(bool)isDstOpaque
                                    interpolation:(int)interpolation
                                             isAA:(jboolean)isAA
{
    RenderOptions roptions = {JNI_TRUE, isAA, interpolation, { isSrcOpaque, JNI_TRUE }, {isDstOpaque, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_FALSE};
    return [self getEncoder:dest renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                               isSrcOpaque:(bool)isSrcOpaque
                                               isDstOpaque:(bool)isDstOpaque
                                             interpolation:(int)interpolation
{
    return [self getTextureEncoder:dest isSrcOpaque:isSrcOpaque isDstOpaque:isDstOpaque interpolation:interpolation isAA:JNI_FALSE];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getTextEncoder:(const BMTLSDOps * _Nonnull)dstOps
                                      isSrcOpaque:(bool)isSrcOpaque
{
    RenderOptions roptions = {JNI_TRUE, JNI_FALSE, INTERPOLATION_NEAREST_NEIGHBOR, { isSrcOpaque, JNI_TRUE }, {dstOps->isOpaque, JNI_TRUE}, JNI_TRUE, JNI_FALSE, JNI_FALSE};
    return [self getEncoder:dstOps->pTexture renderOptions:&roptions];
}

- (id<MTLRenderCommandEncoder> _Nonnull) getEncoder:(id <MTLTexture> _Nonnull)dest
                                      renderOptions:(const RenderOptions * _Nonnull)renderOptions
{
  //
  // 1. check whether it's necessary to call endEncoder
  //
  jboolean needEnd = JNI_FALSE;
  if (_encoder != nil) {
    if (_destination != dest || renderOptions->isAA != _encoderStates.aa) {
      J2dTraceLn2(J2D_TRACE_VERBOSE,
                  "end common encoder because of dest change: %p -> %p",
                  _destination, dest);
      needEnd = JNI_TRUE;
    } else if ((_useStencil == NO) != ([_mtlc.clip isShape] == NO)) {
      // 1. When mode changes RECT -> SHAPE we must recreate encoder with
      // stencilAttachment (todo: consider the case when current encoder already
      // has stencil)
      //
      // 2. When mode changes SHAPE -> RECT it seems that we can use the same
      // encoder with disabled stencil test, but [encoder
      // setDepthStencilState:nil] causes crash, so we have to recreate encoder
      // in such case
      J2dTraceLn2(J2D_TRACE_VERBOSE,
                  "end common encoder because toggle stencil: %d -> %d",
                  (int)_useStencil, (int)[_mtlc.clip isShape]);
      needEnd = JNI_TRUE;
    }
  }
  if (needEnd)
    [self endEncoder];

  //
  // 2. recreate encoder if necessary
  //
  jboolean forceUpdate = JNI_FALSE;
#ifdef ALWAYS_UPDATE_ENCODER_STATES
  forceUpdate = JNI_TRUE;
#endif // ALWAYS_UPDATE_ENCODER_STATES

  if (_encoder == nil) {
    _destination = dest;
    _useStencil = [_mtlc.clip isShape] && !_mtlc.clip.stencilMaskGenerationInProgress;
    forceUpdate = JNI_TRUE;

    MTLCommandBufferWrapper *cbw = [_mtlc getCommandBufferWrapper];
    MTLRenderPassDescriptor *rpd =
        [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor *ca = rpd.colorAttachments[0];
    ca.texture = dest;

    // TODO: Find out why we cannot use
    // if (_mtlc.clip.stencilMaskGenerationInProgress == YES) {
    //     ca.loadAction = MTLLoadActionClear;
    //     ca.clearColor = MTLClearColorMake(0.0f, 0.0f,0.0f, 0.0f);
    // }
    // here to avoid creation of clearEncoder in beginShapeClip

    ca.loadAction = MTLLoadActionLoad;
    ca.storeAction = MTLStoreActionStore;

    if (_useStencil && !renderOptions->isAA) {
        // If you enable stencil testing or stencil writing, the
        // MTLRenderPassDescriptor must include a stencil attachment.
        rpd.stencilAttachment.loadAction = MTLLoadActionLoad;
        rpd.stencilAttachment.storeAction = MTLStoreActionStore;
        rpd.stencilAttachment.texture = _mtlc.clip.stencilTextureRef;
    } else if (_mtlc.clip.stencilMaskGenerationInProgress == YES) {
        rpd.stencilAttachment.texture = _mtlc.clip.dstOps->pStencilTexture;
        rpd.stencilAttachment.clearStencil = 0;
        rpd.stencilAttachment.loadAction = _mtlc.clip.stencilMaskGenerationStarted? MTLLoadActionLoad : MTLLoadActionClear;
        _mtlc.clip.stencilMaskGenerationStarted = YES;
        rpd.stencilAttachment.storeAction = MTLStoreActionStore;
    }

    // J2dTraceLn1(J2D_TRACE_VERBOSE, "created render encoder to draw on
    // tex=%p", dest);
    _encoder = [[cbw getCommandBuffer] renderCommandEncoderWithDescriptor:rpd];

    [_encoderStates reset:dest
               isDstOpaque:renderOptions->dstFlags.isOpaque
        isDstPremultiplied:YES
                      isAA:renderOptions->isAA
                      isText:renderOptions->isText
                      isLCD:renderOptions->isLCD];
  }

  //
  // 3. update encoder states
  //
  [_encoderStates updateEncoder:_encoder
                        context:_mtlc
                  renderOptions:renderOptions
                    forceUpdate:forceUpdate];

  return _encoder;
}

- (id<MTLBlitCommandEncoder> _Nonnull) createBlitEncoder {
    [self endEncoder];
    return [[[_mtlc getCommandBufferWrapper] getCommandBuffer] blitCommandEncoder];
}

- (void) endEncoder {
    if (_encoder != nil) {
      [_encoder endEncoding];
      _encoder = nil;
      _destination = nil;
    }
}

@end
