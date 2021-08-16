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

#include "MTLPaints.h"
#include "MTLClip.h"
#include "common.h"

#include "sun_java2d_SunGraphics2D.h"
#include "sun_java2d_pipe_BufferedPaints.h"
#import "MTLComposite.h"
#import "MTLBufImgOps.h"
#include "MTLRenderQueue.h"

#define RGBA_TO_V4(c)              \
{                                  \
    (((c) >> 16) & (0xFF))/255.0f, \
    (((c) >> 8) & 0xFF)/255.0f,    \
    ((c) & 0xFF)/255.0f,           \
    (((c) >> 24) & 0xFF)/255.0f    \
}

#define FLOAT_ARR_TO_V4(p) \
{                      \
    p[0], \
    p[1], \
    p[2], \
    p[3]  \
}

static MTLRenderPipelineDescriptor * templateRenderPipelineDesc = nil;
static MTLRenderPipelineDescriptor * templateTexturePipelineDesc = nil;
static MTLRenderPipelineDescriptor * templateAATexturePipelineDesc = nil;
static MTLRenderPipelineDescriptor * templateLCDPipelineDesc = nil;
static MTLRenderPipelineDescriptor * templateAAPipelineDesc = nil;
static void
setTxtUniforms(MTLContext *mtlc, int color, id <MTLRenderCommandEncoder> encoder, int interpolation, bool repeat,
               jfloat extraAlpha, const SurfaceRasterFlags *srcFlags, const SurfaceRasterFlags *dstFlags, int mode);

static void initTemplatePipelineDescriptors() {
    if (templateRenderPipelineDesc != nil && templateTexturePipelineDesc != nil &&
        templateAATexturePipelineDesc != nil && templateLCDPipelineDesc != nil &&
        templateAAPipelineDesc != nil)
        return;

    MTLVertexDescriptor *vertDesc = [[MTLVertexDescriptor new] autorelease];
    vertDesc.attributes[VertexAttributePosition].format = MTLVertexFormatFloat2;
    vertDesc.attributes[VertexAttributePosition].offset = 0;
    vertDesc.attributes[VertexAttributePosition].bufferIndex = MeshVertexBuffer;
    vertDesc.layouts[MeshVertexBuffer].stride = sizeof(struct Vertex);
    vertDesc.layouts[MeshVertexBuffer].stepRate = 1;
    vertDesc.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;

    templateRenderPipelineDesc = [MTLRenderPipelineDescriptor new];
    templateRenderPipelineDesc.sampleCount = 1;
    templateRenderPipelineDesc.vertexDescriptor = vertDesc;
    templateRenderPipelineDesc.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;
    templateRenderPipelineDesc.colorAttachments[0].rgbBlendOperation =   MTLBlendOperationAdd;
    templateRenderPipelineDesc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
    templateRenderPipelineDesc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorOne;
    templateRenderPipelineDesc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
    templateRenderPipelineDesc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    templateRenderPipelineDesc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    templateRenderPipelineDesc.label = @"template_render";

    templateTexturePipelineDesc = [templateRenderPipelineDesc copy];
    templateTexturePipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].format = MTLVertexFormatFloat2;
    templateTexturePipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].offset = 2*sizeof(float);
    templateTexturePipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].bufferIndex = MeshVertexBuffer;
    templateTexturePipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stride = sizeof(struct TxtVertex);
    templateTexturePipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepRate = 1;
    templateTexturePipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;
    templateTexturePipelineDesc.label = @"template_texture";

    templateAATexturePipelineDesc = [templateTexturePipelineDesc copy];
    templateAATexturePipelineDesc.label = @"template_aa_texture";

    templateLCDPipelineDesc = [MTLRenderPipelineDescriptor new];
    templateLCDPipelineDesc.sampleCount = 1;
    templateLCDPipelineDesc.vertexDescriptor = vertDesc;
    templateLCDPipelineDesc.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;
    templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].format = MTLVertexFormatFloat2;
    templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].offset = 2*sizeof(float);
    templateLCDPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].bufferIndex = MeshVertexBuffer;
    templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stride = sizeof(struct TxtVertex);
    templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepRate = 1;
    templateLCDPipelineDesc.vertexDescriptor.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;
    templateLCDPipelineDesc.label = @"template_lcd";

    vertDesc = [[MTLVertexDescriptor new] autorelease];
    vertDesc.attributes[VertexAttributePosition].format = MTLVertexFormatFloat2;
    vertDesc.attributes[VertexAttributePosition].offset = 0;
    vertDesc.attributes[VertexAttributePosition].bufferIndex = MeshVertexBuffer;
    vertDesc.layouts[MeshVertexBuffer].stride = sizeof(struct AAVertex);
    vertDesc.layouts[MeshVertexBuffer].stepRate = 1;
    vertDesc.layouts[MeshVertexBuffer].stepFunction = MTLVertexStepFunctionPerVertex;

    templateAAPipelineDesc = [MTLRenderPipelineDescriptor new];
    templateAAPipelineDesc.sampleCount = 1;
    templateAAPipelineDesc.vertexDescriptor = vertDesc;
    templateAAPipelineDesc.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;
    templateAAPipelineDesc.colorAttachments[0].rgbBlendOperation =   MTLBlendOperationAdd;
    templateAAPipelineDesc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
    templateAAPipelineDesc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorOne;
    templateAAPipelineDesc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
    templateAAPipelineDesc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    templateAAPipelineDesc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
    templateAAPipelineDesc.colorAttachments[0].blendingEnabled = YES;
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].format = MTLVertexFormatFloat2;
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].offset = 2*sizeof(float);
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeTexPos].bufferIndex = MeshVertexBuffer;
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeITexPos].format = MTLVertexFormatFloat2;
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeITexPos].offset = 4*sizeof(float);
    templateAAPipelineDesc.vertexDescriptor.attributes[VertexAttributeITexPos].bufferIndex = MeshVertexBuffer;
    templateAAPipelineDesc.label = @"template_aa";
}


@implementation MTLColorPaint {
// color-mode
jint _color;
}
- (id)initWithColor:(jint)color {
    self = [super initWithState:sun_java2d_SunGraphics2D_PAINT_ALPHACOLOR];

    if (self) {
        _color = color;
    }
    return self;
}

- (jint)color {
    return _color;
}

- (BOOL)isEqual:(MTLColorPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    return _color == other->_color;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + _color;
    return h;
}

- (NSString *)description {
    return [NSString stringWithFormat:
            @"[r=%d g=%d b=%d a=%d]",
            (_color >> 16) & (0xFF),
            (_color >> 8) & 0xFF,
            (_color) & 0xFF,
            (_color >> 24) & 0xFF];
}

- (void)setPipelineState:(id<MTLRenderCommandEncoder>)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();

    MTLRenderPipelineDescriptor *rpDesc = nil;

    NSString *vertShader = @"vert_col";
    NSString *fragShader = @"frag_col";

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt";
        fragShader = @"frag_txt";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];
        if (renderOptions->isAA) {
            fragShader = @"aa_frag_txt";
            rpDesc = [[templateAATexturePipelineDesc copy] autorelease];
        }
        if (renderOptions->isText) {
            fragShader = @"frag_text";
        }
        if (renderOptions->isLCD) {
            vertShader = @"vert_txt_lcd";
            fragShader = @"lcd_color";
            rpDesc = [[templateLCDPipelineDesc copy] autorelease];
        }
        setTxtUniforms(mtlc, _color, encoder,
                       renderOptions->interpolation, NO, [mtlc.composite getExtraAlpha], &renderOptions->srcFlags,
                       &renderOptions->dstFlags, 1);
    } else if (renderOptions->isAAShader) {
        vertShader = @"vert_col_aa";
        fragShader = @"frag_col_aa";
        rpDesc = [[templateAAPipelineDesc copy] autorelease];
    } else {
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    }

    struct FrameUniforms uf = {RGBA_TO_V4(_color)};
    [encoder setVertexBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

- (void)setXorModePipelineState:(id<MTLRenderCommandEncoder>)encoder
                        context:(MTLContext *)mtlc
                  renderOptions:(const RenderOptions *)renderOptions
           pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    NSString * vertShader = @"vert_col_xorMode";
    NSString * fragShader = @"frag_col_xorMode";
    MTLRenderPipelineDescriptor * rpDesc = nil;
    jint xorColor = (jint) [mtlc.composite getXorColor];
    // Calculate _color ^ xorColor for RGB components
    // This color gets XORed with destination framebuffer pixel color
    const int col = _color ^ xorColor;
    BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt_xorMode";
        fragShader = @"frag_txt_xorMode";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];

        setTxtUniforms(mtlc, col, encoder,
                       renderOptions->interpolation, NO, [mtlc.composite getExtraAlpha],
                       &renderOptions->srcFlags, &renderOptions->dstFlags, 1);
        [encoder setFragmentBytes:&xorColor length:sizeof(xorColor) atIndex:0];

        [encoder setFragmentTexture:dstOps->pTexture atIndex:1];
    } else {
        struct FrameUniforms uf = {RGBA_TO_V4(col)};
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];

        [encoder setVertexBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
        [encoder setFragmentTexture:dstOps->pTexture atIndex:0];
    }

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}
@end

@implementation MTLBaseGradPaint {
    jboolean      _useMask;
@protected
    jint          _cyclic;
}

- (id)initWithState:(jint)state mask:(jboolean)useMask cyclic:(jboolean)cyclic {
    self = [super initWithState:state];

    if (self) {
        _useMask = useMask;
        _cyclic = cyclic;
    }
    return self;
}

- (BOOL)isEqual:(MTLBaseGradPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    return [super isEqual:self] && _cyclic == other->_cyclic && _useMask == other->_useMask;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + _cyclic;
    h = h*31 + _useMask;
    return h;
}

@end

@implementation MTLGradPaint {
    jdouble _p0;
    jdouble _p1;
    jdouble _p3;
    jint _pixel1;
    jint _pixel2;
}
- (id)initWithUseMask:(jboolean)useMask
               cyclic:(jboolean)cyclic
                   p0:(jdouble)p0
                   p1:(jdouble)p1
                   p3:(jdouble)p3
               pixel1:(jint)pixel1
               pixel2:(jint)pixel2
{
    self = [super initWithState:sun_java2d_SunGraphics2D_PAINT_GRADIENT
                           mask:useMask
                         cyclic:cyclic];

    if (self) {
        _p0 = p0;
        _p1 = p1;
        _p3 = p3;
        _pixel1 = pixel1;
        _pixel2 = pixel2;
    }
    return self;
}

- (BOOL)isEqual:(MTLGradPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    return [super isEqual:self] && _p0 == other->_p0 &&
           _p1 == other->_p1 && _p3 == other->_p3 &&
           _pixel1 == other->_pixel1 && _pixel2 == other->_pixel2;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + [@(_p0) hash];
    h = h*31 + [@(_p1) hash];;
    h = h*31 + [@(_p3) hash];;
    h = h*31 + _pixel1;
    h = h*31 + _pixel2;
    return h;
}
- (NSString *)description {
    return [NSString stringWithFormat:@"gradient"];
}

- (void)setPipelineState:(id)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    MTLRenderPipelineDescriptor *rpDesc = nil;

    NSString *vertShader = @"vert_grad";
    NSString *fragShader = @"frag_grad";

    struct GradFrameUniforms uf = {
            {_p0, _p1, _p3},
            RGBA_TO_V4(_pixel1),
            RGBA_TO_V4(_pixel2),
            _cyclic,
            [mtlc.composite getExtraAlpha]
    };
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:0];

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt_grad";
        fragShader = @"frag_txt_grad";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];
    } else {
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    }

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

- (void)setXorModePipelineState:(id)encoder
                        context:(MTLContext *)mtlc
                  renderOptions:(const RenderOptions *)renderOptions
           pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    // This block is not reached in current implementation.
    // Gradient paint XOR mode rendering uses a tile based rendering using a SW pipe (similar to OGL)
    initTemplatePipelineDescriptors();
    NSString* vertShader = @"vert_grad_xorMode";
    NSString* fragShader = @"frag_grad_xorMode";
    MTLRenderPipelineDescriptor *rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    jint xorColor = (jint) [mtlc.composite getXorColor];

    struct GradFrameUniforms uf = {
            {_p0, _p1, _p3},
            RGBA_TO_V4(_pixel1 ^ xorColor),
            RGBA_TO_V4(_pixel2 ^ xorColor),
            _cyclic,
            [mtlc.composite getExtraAlpha]
    };

    [encoder setFragmentBytes: &uf length:sizeof(uf) atIndex:0];
    BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();
    [encoder setFragmentTexture:dstOps->pTexture atIndex:0];

    J2dTraceLn(J2D_TRACE_INFO, "MTLPaints - setXorModePipelineState -- PAINT_GRADIENT");

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

@end

@implementation MTLBaseMultiGradPaint {
    @protected
    jboolean _linear;
    jint _numFracts;
    jfloat _fract[GRAD_MAX_FRACTIONS];
    jint _pixel[GRAD_MAX_FRACTIONS];
}

- (id)initWithState:(jint)state
               mask:(jboolean)useMask
             linear:(jboolean)linear
        cycleMethod:(jboolean)cycleMethod
           numStops:(jint)numStops
          fractions:(jfloat *)fractions
             pixels:(jint *)pixels
{
    self = [super initWithState:state
                           mask:useMask
                         cyclic:cycleMethod];

    if (self) {
        _linear = linear;
        memcpy(_fract, fractions,numStops*sizeof(jfloat));
        memcpy(_pixel, pixels, numStops*sizeof(jint));
        _numFracts = numStops;
    }
    return self;
}

- (BOOL)isEqual:(MTLBaseMultiGradPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    if (_numFracts != other->_numFracts || ![super isEqual:self])
        return NO;
    for (int i = 0; i < _numFracts; i++) {
        if (_fract[i] != other->_fract[i]) return NO;
        if (_pixel[i] != other->_pixel[i]) return NO;
    }
    return YES;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + _numFracts;
    for (int i = 0; i < _numFracts; i++) {
        h = h*31 + [@(_fract[i]) hash];
        h = h*31 + _pixel[i];
    }
    return h;
}

@end

@implementation MTLLinearGradPaint {
    jdouble _p0;
    jdouble _p1;
    jdouble _p3;
}
- (void)setPipelineState:(id)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage

{
    initTemplatePipelineDescriptors();
    MTLRenderPipelineDescriptor *rpDesc = nil;

    NSString *vertShader = @"vert_grad";
    NSString *fragShader = @"frag_lin_grad";

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt_grad";
        fragShader = @"frag_txt_lin_grad";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];
    } else {
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    }

    struct LinGradFrameUniforms uf = {
            {_p0, _p1, _p3},
            {},
            {},
            _numFracts,
            _linear,
            _cyclic,
            [mtlc.composite getExtraAlpha]
    };

    memcpy(uf.fract, _fract, _numFracts*sizeof(jfloat));
    for (int i = 0; i < _numFracts; i++) {
        vector_float4 v = RGBA_TO_V4(_pixel[i]);
        uf.color[i] = v;
    }
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:0];

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

- (id)initWithUseMask:(jboolean)useMask
               linear:(jboolean)linear
          cycleMethod:(jboolean)cycleMethod
             numStops:(jint)numStops
                   p0:(jfloat)p0
                   p1:(jfloat)p1
                   p3:(jfloat)p3
            fractions:(jfloat *)fractions
               pixels:(jint *)pixels
{
    self = [super initWithState:sun_java2d_SunGraphics2D_PAINT_LIN_GRADIENT
                           mask:useMask
                         linear:linear
                    cycleMethod:cycleMethod
                       numStops:numStops
                      fractions:fractions
                         pixels:pixels];

    if (self) {
        _p0 = p0;
        _p1 = p1;
        _p3 = p3;
    }
    return self;
}

- (BOOL)isEqual:(MTLLinearGradPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]] || ![super isEqual:other])
        return NO;

    return _p0 == other->_p0 && _p1 == other->_p1 && _p3 == other->_p3;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + [@(_p0) hash];
    h = h*31 + [@(_p1) hash];
    h = h*31 + [@(_p3) hash];
    return h;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"linear_gradient"];
}
@end

@implementation MTLRadialGradPaint {
    jfloat _m00;
    jfloat _m01;
    jfloat _m02;
    jfloat _m10;
    jfloat _m11;
    jfloat _m12;
    jfloat _focusX;
}

- (id)initWithUseMask:(jboolean)useMask
               linear:(jboolean)linear
          cycleMethod:(jint)cycleMethod
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
    self = [super initWithState:sun_java2d_SunGraphics2D_PAINT_RAD_GRADIENT
                           mask:useMask
                         linear:linear
                    cycleMethod:cycleMethod
                       numStops:numStops
                      fractions:fractions
                         pixels:pixels];

    if (self) {
        _m00 = m00;
        _m01 = m01;
        _m02 = m02;
        _m10 = m10;
        _m11 = m11;
        _m12 = m12;
        _focusX = focusX;
    }
    return self;
}

- (BOOL)isEqual:(MTLRadialGradPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]]
            || ![super isEqual:self])
        return NO;

    return _m00 == other->_m00 && _m01 == other->_m01 && _m02 == other->_m02 &&
           _m10 == other->_m10 && _m11 == other->_m11 && _m12 == other->_m12 &&
           _focusX == other->_focusX;
}

- (NSUInteger)hash {
    NSUInteger h = [super hash];
    h = h*31 + [@(_m00) hash];
    h = h*31 + [@(_m01) hash];
    h = h*31 + [@(_m02) hash];
    h = h*31 + [@(_m10) hash];
    h = h*31 + [@(_m11) hash];
    h = h*31 + [@(_m12) hash];
    return h;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"radial_gradient"];
}

- (void)setPipelineState:(id)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    MTLRenderPipelineDescriptor *rpDesc = nil;

    NSString *vertShader = @"vert_grad";
    NSString *fragShader = @"frag_rad_grad";

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt_grad";
        fragShader = @"frag_txt_rad_grad";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];
    } else {
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    }

    struct RadGradFrameUniforms uf = {
            {},
            {},
            _numFracts,
            _linear,
            _cyclic,
            {_m00, _m01, _m02},
            {_m10, _m11, _m12},
            {},
            [mtlc.composite getExtraAlpha]
    };

    uf.precalc[0] = _focusX;
    uf.precalc[1] = 1.0 - (_focusX * _focusX);
    uf.precalc[2] = 1.0 / uf.precalc[1];

    memcpy(uf.fract, _fract, _numFracts*sizeof(jfloat));
    for (int i = 0; i < _numFracts; i++) {
        vector_float4 v = RGBA_TO_V4(_pixel[i]);
        uf.color[i] = v;
    }
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:0];
    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}


@end

@implementation MTLTexturePaint {
    struct AnchorData _anchor;
    id <MTLTexture> _paintTexture;
    jboolean _isOpaque;
}

- (id)initWithUseMask:(jboolean)useMask
              textureID:(id)textureId
               isOpaque:(jboolean)isOpaque
                 filter:(jboolean)filter
                    xp0:(jdouble)xp0
                    xp1:(jdouble)xp1
                    xp3:(jdouble)xp3
                    yp0:(jdouble)yp0
                    yp1:(jdouble)yp1
                    yp3:(jdouble)yp3
{
    self = [super initWithState:sun_java2d_SunGraphics2D_PAINT_TEXTURE];

    if (self) {
        _paintTexture = textureId;
        _anchor.xParams[0] = xp0;
        _anchor.xParams[1] = xp1;
        _anchor.xParams[2] = xp3;

        _anchor.yParams[0] = yp0;
        _anchor.yParams[1] = yp1;
        _anchor.yParams[2] = yp3;
        _isOpaque = isOpaque;
    }
    return self;

}

- (BOOL)isEqual:(MTLTexturePaint *)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    return [_paintTexture isEqual:other->_paintTexture]
            && _anchor.xParams[0] == other->_anchor.xParams[0]
            && _anchor.xParams[1] == other->_anchor.xParams[1]
            && _anchor.xParams[2] == other->_anchor.xParams[2]
            && _anchor.yParams[0] == other->_anchor.yParams[0]
            && _anchor.yParams[1] == other->_anchor.yParams[1]
            && _anchor.yParams[2] == other->_anchor.yParams[2];
}

- (NSString *)description {
    return [NSString stringWithFormat:@"texture_paint"];
}

- (void)setPipelineState:(id)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    MTLRenderPipelineDescriptor *rpDesc = nil;

    NSString* vertShader = @"vert_tp";
    NSString* fragShader = @"frag_tp";

    [encoder setVertexBytes:&_anchor length:sizeof(_anchor) atIndex:FrameUniformBuffer];

    if (renderOptions->isTexture) {
        vertShader = @"vert_txt_tp";
        fragShader = @"frag_txt_tp";
        rpDesc = [[templateTexturePipelineDesc copy] autorelease];
        [encoder setFragmentTexture:_paintTexture atIndex:1];
    } else {
        rpDesc = [[templateRenderPipelineDesc copy] autorelease];
        [encoder setFragmentTexture:_paintTexture atIndex:0];
    }
    const SurfaceRasterFlags srcFlags = {_isOpaque, renderOptions->srcFlags.isPremultiplied};
    setTxtUniforms(mtlc, 0, encoder,
                   renderOptions->interpolation, YES, [mtlc.composite getExtraAlpha],
                   &srcFlags, &renderOptions->dstFlags, 0);

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

- (void)setXorModePipelineState:(id)encoder
                        context:(MTLContext *)mtlc
                  renderOptions:(const RenderOptions *)renderOptions
           pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    // This block is not reached in current implementation.
    // Texture paint XOR mode rendering uses a tile based rendering using a SW pipe (similar to OGL)
    NSString* vertShader = @"vert_tp_xorMode";
    NSString* fragShader = @"frag_tp_xorMode";
    MTLRenderPipelineDescriptor *rpDesc = [[templateRenderPipelineDesc copy] autorelease];
    jint xorColor = (jint) [mtlc.composite getXorColor];

    [encoder setVertexBytes:&_anchor length:sizeof(_anchor) atIndex:FrameUniformBuffer];
    [encoder setFragmentTexture:_paintTexture atIndex: 0];
    BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();
    [encoder setFragmentTexture:dstOps->pTexture atIndex:1];
    [encoder setFragmentBytes:&xorColor length:sizeof(xorColor) atIndex: 0];

    J2dTraceLn(J2D_TRACE_INFO, "MTLPaints - setXorModePipelineState -- PAINT_TEXTURE");

    id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                        vertexShaderId:vertShader
                                                                      fragmentShaderId:fragShader
                                                                             composite:mtlc.composite
                                                                         renderOptions:renderOptions
                                                                         stencilNeeded:[mtlc.clip isShape]];
    [encoder setRenderPipelineState:pipelineState];
}

@end

@implementation MTLPaint {
    jint _paintState;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _paintState = sun_java2d_SunGraphics2D_PAINT_UNDEFINED;
    }

    return self;
}

- (instancetype)initWithState:(jint)state {
    self = [super init];
    if (self) {
        _paintState = state;
    }
    return self;
}

- (BOOL)isEqual:(MTLPaint *)other {
    if (other == self)
        return YES;
    if (!other || ![other isKindOfClass:[self class]])
        return NO;
    return _paintState == other->_paintState;
}

- (NSUInteger)hash {
    return _paintState;
}

- (NSString *)description {
    return @"unknown-paint";
}

static void
setTxtUniforms(MTLContext *mtlc, int color, id <MTLRenderCommandEncoder> encoder, int interpolation, bool repeat,
               jfloat extraAlpha, const SurfaceRasterFlags *srcFlags, const SurfaceRasterFlags *dstFlags, int mode) {
    struct TxtFrameUniforms uf = {RGBA_TO_V4(color), mode, srcFlags->isOpaque, dstFlags->isOpaque, extraAlpha};
    [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
    [mtlc.samplerManager setSamplerWithEncoder:encoder interpolation:interpolation repeat:repeat];
}

// For the current paint mode:
// 1. Selects vertex+fragment shaders (and corresponding pipelineDesc) and set pipelineState
// 2. Set vertex and fragment buffers
// Base implementation is used in drawTex2Tex
- (void)setPipelineState:(id <MTLRenderCommandEncoder>)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    // Called from drawTex2Tex used in flushBuffer and for buffered image ops
    if (renderOptions->isTexture) {
        NSString * vertShader = @"vert_txt";
        NSString * fragShader = @"frag_txt";
        MTLRenderPipelineDescriptor* rpDesc = [[templateTexturePipelineDesc copy] autorelease];

        NSObject *bufImgOp = [mtlc getBufImgOp];
        if (bufImgOp != nil) {
            if ([bufImgOp isKindOfClass:[MTLRescaleOp class]]) {
                MTLRescaleOp *rescaleOp = bufImgOp;
                fragShader = @"frag_txt_op_rescale";

                struct TxtFrameOpRescaleUniforms uf = {
                        RGBA_TO_V4(0), [mtlc.composite getExtraAlpha], renderOptions->srcFlags.isOpaque,
                        rescaleOp.isNonPremult,
                        FLOAT_ARR_TO_V4([rescaleOp getScaleFactors]), FLOAT_ARR_TO_V4([rescaleOp getOffsets])
                };
                [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
                [mtlc.samplerManager setSamplerWithEncoder:encoder interpolation:renderOptions->interpolation repeat:NO];
            } else if ([bufImgOp isKindOfClass:[MTLConvolveOp class]]) {
                MTLConvolveOp *convolveOp = bufImgOp;
                fragShader = @"frag_txt_op_convolve";

                struct TxtFrameOpConvolveUniforms uf = {
                        [mtlc.composite getExtraAlpha], renderOptions->srcFlags.isOpaque,
                        FLOAT_ARR_TO_V4([convolveOp getImgEdge]),
                        convolveOp.kernelSize, convolveOp.isEdgeZeroFill,
                };
                [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
                [mtlc.samplerManager setSamplerWithEncoder:encoder interpolation:renderOptions->interpolation repeat:NO];

                [encoder setFragmentBuffer:[convolveOp getBuffer] offset:0 atIndex:2];
            } else if ([bufImgOp isKindOfClass:[MTLLookupOp class]]) {
                MTLLookupOp *lookupOp = bufImgOp;
                fragShader = @"frag_txt_op_lookup";

                struct TxtFrameOpLookupUniforms uf = {
                        [mtlc.composite getExtraAlpha], renderOptions->srcFlags.isOpaque,
                        FLOAT_ARR_TO_V4([lookupOp getOffset]), lookupOp.isUseSrcAlpha, lookupOp.isNonPremult,
                };
                [encoder setFragmentBytes:&uf length:sizeof(uf) atIndex:FrameUniformBuffer];
                [mtlc.samplerManager setSamplerWithEncoder:encoder interpolation:renderOptions->interpolation repeat:NO];
                [encoder setFragmentTexture:[lookupOp getLookupTexture] atIndex:1];
            }
        } else {
            setTxtUniforms(mtlc, 0, encoder,
                           renderOptions->interpolation, NO, [mtlc.composite getExtraAlpha],
                           &renderOptions->srcFlags,
                           &renderOptions->dstFlags, 0);

        }
        id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                            vertexShaderId:vertShader
                                                                          fragmentShaderId:fragShader
                                                                                 composite:mtlc.composite
                                                                             renderOptions:renderOptions
                                                                             stencilNeeded:[mtlc.clip isShape]];
        [encoder setRenderPipelineState:pipelineState];
    }
}

// For the current paint mode:
// 1. Selects vertex+fragment shaders (and corresponding pipelineDesc) and set pipelineState
// 2. Set vertex and fragment buffers
- (void)setXorModePipelineState:(id <MTLRenderCommandEncoder>)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage
{
    initTemplatePipelineDescriptors();
    if (renderOptions->isTexture) {
        jint xorColor = (jint) [mtlc.composite getXorColor];
        NSString * vertShader = @"vert_txt_xorMode";
        NSString * fragShader = @"frag_txt_xorMode";
        MTLRenderPipelineDescriptor * rpDesc = [[templateTexturePipelineDesc copy] autorelease];

        const int col = 0 ^ xorColor;
        setTxtUniforms(mtlc, col, encoder,
                       renderOptions->interpolation, NO, [mtlc.composite getExtraAlpha],
                       &renderOptions->srcFlags, &renderOptions->dstFlags, 0);
        [encoder setFragmentBytes:&xorColor length:sizeof(xorColor) atIndex: 0];

        BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();
        [encoder setFragmentTexture:dstOps->pTexture atIndex:1];

        setTxtUniforms(mtlc, 0, encoder,
                       renderOptions->interpolation, NO, [mtlc.composite getExtraAlpha],
                       &renderOptions->srcFlags,
                       &renderOptions->dstFlags, 0);

        id <MTLRenderPipelineState> pipelineState = [pipelineStateStorage getPipelineState:rpDesc
                                                                            vertexShaderId:vertShader
                                                                          fragmentShaderId:fragShader
                                                                                 composite:mtlc.composite
                                                                             renderOptions:renderOptions
                                                                             stencilNeeded:[mtlc.clip isShape]];
        [encoder setRenderPipelineState:pipelineState];
    }
}

@end
