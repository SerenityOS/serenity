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

#import "MTLPipelineStatesStorage.h"

#include "GraphicsPrimitiveMgr.h"
#import "MTLComposite.h"

#include "sun_java2d_SunGraphics2D.h"

extern const SurfaceRasterFlags defaultRasterFlags;

static void setBlendingFactors(
        MTLRenderPipelineColorAttachmentDescriptor * cad,
        MTLComposite* composite,
        const RenderOptions * renderOptions);

@implementation MTLPipelineStatesStorage

@synthesize device;
@synthesize library;
@synthesize shaders;
@synthesize states;

- (id) initWithDevice:(id<MTLDevice>)dev shaderLibPath:(NSString *)shadersLib {
    self = [super init];
    if (self == nil) return self;

    self.device = dev;

    NSError *error = nil;
    self.library = [dev newLibraryWithFile:shadersLib error:&error];
    if (!self.library) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLPipelineStatesStorage.initWithDevice() - Failed to load Metal shader library.");
        return nil;
    }
    self.shaders = [NSMutableDictionary dictionaryWithCapacity:10];
    self.states = [NSMutableDictionary dictionaryWithCapacity:10];
    computeStates = [[NSMutableDictionary dictionaryWithCapacity:10] retain] ;
    return self;
}

- (NSPointerArray * ) getSubStates:(NSString *)vertexShaderId fragmentShader:(NSString *)fragmentShaderId {
    NSMutableDictionary * vSubStates = states[vertexShaderId];
    if (vSubStates == nil) {
        @autoreleasepool {
            vSubStates = [NSMutableDictionary dictionary];
            [states setObject:vSubStates forKey:vertexShaderId];
        }
    }
    NSPointerArray * sSubStates = vSubStates[fragmentShaderId];
    if (sSubStates == nil) {
        @autoreleasepool {
            sSubStates = [NSPointerArray strongObjectsPointerArray];
            [vSubStates setObject:sSubStates forKey:fragmentShaderId];
        }
    }
    return sSubStates;
}

- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId
{
    RenderOptions defaultOptions = {JNI_FALSE, JNI_FALSE, 0/*unused*/, {JNI_FALSE, JNI_TRUE}, {JNI_FALSE, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_FALSE};
    return [self getPipelineState:pipelineDescriptor
                   vertexShaderId:vertexShaderId
                 fragmentShaderId:fragmentShaderId
                        composite:nil
                    renderOptions:&defaultOptions
                    stencilNeeded:NO];
}

- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId
                               stencilNeeded:(bool)stencilNeeded
{
    RenderOptions defaultOptions = {JNI_FALSE, JNI_FALSE, 0/*unused*/, {JNI_FALSE, JNI_TRUE}, {JNI_FALSE, JNI_TRUE}, JNI_FALSE, JNI_FALSE, JNI_FALSE};
    return [self getPipelineState:pipelineDescriptor
                   vertexShaderId:vertexShaderId
                 fragmentShaderId:fragmentShaderId
                        composite:nil
                    renderOptions:&defaultOptions
                    stencilNeeded:stencilNeeded];
}

// Base method to obtain MTLRenderPipelineState.
// NOTE: parameters compositeRule, srcFlags, dstFlags are used to set MTLRenderPipelineColorAttachmentDescriptor multipliers
- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId
                                      composite:(MTLComposite*) composite
                                  renderOptions:(const RenderOptions *)renderOptions
                                  stencilNeeded:(bool)stencilNeeded;
{
    jint compositeRule = composite != nil ? [composite getRule] : RULE_Src;
    const jboolean useXorComposite = composite != nil && [composite getCompositeState] == sun_java2d_SunGraphics2D_COMP_XOR;
    const jboolean useComposite = composite != nil && compositeRule >= 0
        && compositeRule < java_awt_AlphaComposite_MAX_RULE;

    // Calculate index by flags and compositeRule
    // TODO: reimplement, use map with convenient key (calculated by all arguments)
    int subIndex = 0;
    if (useXorComposite) {
        // compositeRule value is already XOR_COMPOSITE_RULE
    }
    else {
        if (useComposite) {
            if (!renderOptions->srcFlags.isPremultiplied)
                subIndex |= 1;
            if (renderOptions->srcFlags.isOpaque)
                subIndex |= 1 << 1;
            if (!renderOptions->dstFlags.isPremultiplied)
                subIndex |= 1 << 2;
            if (renderOptions->dstFlags.isOpaque)
                subIndex |= 1 << 3;
        } else
            compositeRule = RULE_Src;
    }

    if (stencilNeeded) {
        subIndex |= 1 << 4;
    }

    if (renderOptions->isAA) {
        subIndex |= 1 << 5;
    }

    if ((composite != nil && FLT_LT([composite getExtraAlpha], 1.0f))) {
        subIndex |= 1 << 6;
    }

    int index = compositeRule*128 + subIndex;

    NSPointerArray * subStates = [self getSubStates:vertexShaderId fragmentShader:fragmentShaderId];

    if (index >= subStates.count) {
        subStates.count = (NSUInteger) (index + 1);
    }

    id<MTLRenderPipelineState> result = [subStates pointerAtIndex:index];
    if (result == nil) {
        @autoreleasepool {
            id <MTLFunction> vertexShader = [self getShader:vertexShaderId];
            id <MTLFunction> fragmentShader = [self getShader:fragmentShaderId];
            MTLRenderPipelineDescriptor *pipelineDesc = [[pipelineDescriptor copy] autorelease];
            pipelineDesc.vertexFunction = vertexShader;
            pipelineDesc.fragmentFunction = fragmentShader;

            if (useXorComposite) {
                /* The below configuration is the best performant implementation of XOR mode rendering.
                   It was found that it works ONLY for basic Colors and not for all RGB combinations.
                   Hence, a slow performant XOR mode rendering has been implemented by
                   disabling blending & committing after each draw call.
                   In XOR mode rendering, subsequent draw calls are rendered
                   by shader using already rendered framebuffer pixel value XORed
                   with current draw color and XOR color.
                pipelineDesc.colorAttachments[0].blendingEnabled = YES;
                pipelineDesc.colorAttachments[0].rgbBlendOperation = MTLBlendOperationAdd;
                pipelineDesc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorOneMinusDestinationColor;
                pipelineDesc.colorAttachments[0].destinationRGBBlendFactor =  MTLBlendFactorOneMinusSourceColor;
                */

                pipelineDesc.colorAttachments[0].blendingEnabled = NO;
            } else if (useComposite ||
                       (composite != nil  &&
                        FLT_LT([composite getExtraAlpha], 1.0f)))
            {
                setBlendingFactors(
                        pipelineDesc.colorAttachments[0],
                        composite,
                        renderOptions
                );
            }
            if (stencilNeeded) {
                pipelineDesc.stencilAttachmentPixelFormat = MTLPixelFormatStencil8;
            } else {
                // We continue to use same encoder when we move from shape clip
                // to other opcodes. So we need to maintain apprppriate state
                // for stencilAttachmentPixelFormat until we end the encoder
                pipelineDesc.stencilAttachmentPixelFormat = MTLPixelFormatInvalid;
            }

            if (renderOptions->isAA) {
                pipelineDesc.sampleCount = MTLAASampleCount;
                pipelineDesc.colorAttachments[0].rgbBlendOperation =   MTLBlendOperationAdd;
                pipelineDesc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
                pipelineDesc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorOne;
                pipelineDesc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
                pipelineDesc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
                pipelineDesc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
                pipelineDesc.colorAttachments[0].blendingEnabled = YES;
            }

            NSError *error = nil;
            result = [[self.device newRenderPipelineStateWithDescriptor:pipelineDesc error:&error] autorelease];
            if (result == nil) {
                NSLog(@"Failed to create pipeline state, error %@", error);
                exit(0);
            }

            [subStates insertPointer:result atIndex:index];
        }
    }

    return result;
}

- (id<MTLComputePipelineState>) getComputePipelineState:(NSString *)computeShaderId {
    id<MTLComputePipelineState> result = computeStates[computeShaderId];
    if (result == nil) {
        id <MTLFunction> computeShader = [self getShader:computeShaderId];
        @autoreleasepool {
            NSError *error = nil;
            result = (id <MTLComputePipelineState>) [[self.device newComputePipelineStateWithFunction:computeShader error:&error] autorelease];
            if (result == nil) {
                NSLog(@"Failed to create pipeline state, error %@", error);
                exit(0);
            }
            computeStates[computeShaderId] = result;
        }
    }
    return result;
}

- (id<MTLFunction>) getShader:(NSString *)name {
    id<MTLFunction> result = [self.shaders valueForKey:name];
    if (result == nil) {
        result = [[self.library newFunctionWithName:name] autorelease];
        [self.shaders setValue:result forKey:name];
    }
    return result;
}

- (void) dealloc {
    [super dealloc];
    [computeStates release];
}
@end

/**
 * The MTLBlendRule structure encapsulates the two enumerated values that
 * comprise a given Porter-Duff blending (compositing) rule.  For example,
 * the "SrcOver" rule can be represented by:
 *     rule.src = MTLBlendFactorZero;
 *     rule.dst = MTLBlendFactorOneMinusSourceAlpha;
 *
 *     MTLBlendFactor src;
 * The constant representing the source factor in this Porter-Duff rule.
 *
 *     MTLBlendFactor dst;
 * The constant representing the destination factor in this Porter-Duff rule.
 */
struct MTLBlendRule {
    MTLBlendFactor src;
    MTLBlendFactor dst;
};

/**
 * This table contains the standard blending rules (or Porter-Duff compositing
 * factors) used in setBlendingFactors(), indexed by the rule constants from the
 * AlphaComposite class.
 */
static struct MTLBlendRule StdBlendRules[] = {
        { MTLBlendFactorZero,                     MTLBlendFactorZero                }, /* 0 - Nothing      */
        { MTLBlendFactorZero,                     MTLBlendFactorZero                }, /* 1 - RULE_Clear   */
        { MTLBlendFactorOne,                      MTLBlendFactorZero                }, /* 2 - RULE_Src     */
        { MTLBlendFactorOne,                      MTLBlendFactorOneMinusSourceAlpha }, /* 3 - RULE_SrcOver */
        { MTLBlendFactorOneMinusDestinationAlpha, MTLBlendFactorOne                 }, /* 4 - RULE_DstOver */
        { MTLBlendFactorDestinationAlpha,         MTLBlendFactorZero                }, /* 5 - RULE_SrcIn   */
        { MTLBlendFactorZero,                     MTLBlendFactorSourceAlpha         }, /* 6 - RULE_DstIn   */
        { MTLBlendFactorOneMinusDestinationAlpha, MTLBlendFactorZero                }, /* 7 - RULE_SrcOut  */
        { MTLBlendFactorZero,                     MTLBlendFactorOneMinusSourceAlpha }, /* 8 - RULE_DstOut  */
        { MTLBlendFactorZero,                     MTLBlendFactorOne                 }, /* 9 - RULE_Dst     */
        { MTLBlendFactorDestinationAlpha,         MTLBlendFactorOneMinusSourceAlpha }, /*10 - RULE_SrcAtop */
        { MTLBlendFactorOneMinusDestinationAlpha, MTLBlendFactorSourceAlpha         }, /*11 - RULE_DstAtop */
        { MTLBlendFactorOneMinusDestinationAlpha, MTLBlendFactorOneMinusSourceAlpha }, /*12 - RULE_AlphaXor*/
};

static void setBlendingFactors(
        MTLRenderPipelineColorAttachmentDescriptor * cad,
        MTLComposite* composite,
        const RenderOptions * renderOptions
) {
    const long compositeRule = composite != nil ? [composite getRule] : RULE_Src;

    if ((compositeRule == RULE_Src || compositeRule == RULE_SrcOver) &&
        (composite == nil || FLT_GE([composite getExtraAlpha], 1.0f)) &&
        (renderOptions->srcFlags.isOpaque))
    {
        cad.blendingEnabled = NO;
        return;
    }

    cad.blendingEnabled = YES;
    cad.rgbBlendOperation = MTLBlendOperationAdd;
    cad.alphaBlendOperation = MTLBlendOperationAdd;

    cad.sourceAlphaBlendFactor = StdBlendRules[compositeRule].src;
    cad.sourceRGBBlendFactor = StdBlendRules[compositeRule].src;
    cad.destinationAlphaBlendFactor = StdBlendRules[compositeRule].dst;
    cad.destinationRGBBlendFactor = StdBlendRules[compositeRule].dst;
}
