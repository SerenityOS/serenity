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

#ifndef EncoderManager_h_Included
#define EncoderManager_h_Included

#import <Metal/Metal.h>

#include "RenderOptions.h"

@class MTLContex;

/**
 * The EncoderManager class used to obtain MTLRenderCommandEncoder (or MTLBlitCommandEncoder) corresponding
 * to the current state of MTLContext.
 *
 * Due to performance issues (creation of MTLRenderCommandEncoder isn't cheap), each getXXXEncoder invocation
 * updates properties of common (cached) encoder and returns this encoder.
 *
 * Base method getEncoder does the following:
 *  1. Checks whether common encoder must be closed and recreated (some of encoder properties is 'persistent',
 *  for example destination, stencil, or any other property of MTLRenderPassDescriptor)
 *  2. Updates 'mutable' properties encoder: pipelineState (with corresponding buffers), clip, transform, e.t.c. To avoid
 *  unnecessary calls of [encoder setXXX] this manager compares requested state with cached one.
 */
@interface EncoderManager : NSObject
- (id _Nonnull)init;
- (void)dealloc;

- (void)setContext:(MTLContex * _Nonnull)mtlc;

// returns encoder that renders/fills geometry with current paint and composite
- (id<MTLRenderCommandEncoder> _Nonnull)getRenderEncoder:(const BMTLSDOps * _Nonnull)dstOps;

- (id<MTLRenderCommandEncoder> _Nonnull)getAARenderEncoder:(const BMTLSDOps * _Nonnull)dstOps;

- (id<MTLRenderCommandEncoder> _Nonnull)getRenderEncoder:(id<MTLTexture> _Nonnull)dest
                                             isDstOpaque:(bool)isOpaque;

- (id<MTLRenderCommandEncoder> _Nonnull)getAAShaderRenderEncoder:(const BMTLSDOps * _Nonnull)dstOps;

// returns encoder that renders/fills geometry with current composite and with given texture
// (user must call [encoder setFragmentTexture] before any rendering)
- (id<MTLRenderCommandEncoder> _Nonnull)getTextureEncoder:(const BMTLSDOps * _Nonnull)dstOps
                                      isSrcOpaque:(bool)isSrcOpaque;

- (id<MTLRenderCommandEncoder> _Nonnull) getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                               isSrcOpaque:(bool)isSrcOpaque
                                               isDstOpaque:(bool)isDstOpaque;

- (id<MTLRenderCommandEncoder> _Nonnull) getLCDEncoder:(id<MTLTexture> _Nonnull)dest
                                               isSrcOpaque:(bool)isSrcOpaque
                                               isDstOpaque:(bool)isDstOpaque;

- (id<MTLRenderCommandEncoder> _Nonnull)getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                      isSrcOpaque:(bool)isSrcOpaque
                                      isDstOpaque:(bool)isDstOpaque
                                    interpolation:(int)interpolation;

- (id<MTLRenderCommandEncoder> _Nonnull)getTextureEncoder:(id<MTLTexture> _Nonnull)dest
                                              isSrcOpaque:(bool)isSrcOpaque
                                              isDstOpaque:(bool)isDstOpaque
                                            interpolation:(int)interpolation
                                                     isAA:(jboolean)isAA;

- (id<MTLRenderCommandEncoder> _Nonnull)getTextEncoder:(const BMTLSDOps * _Nonnull)dstOps
                                      isSrcOpaque:(bool)isSrcOpaque;

// Base method to obtain any MTLRenderCommandEncoder
- (id<MTLRenderCommandEncoder> _Nonnull) getEncoder:(id<MTLTexture> _Nonnull)dest
                                       isDestOpaque:(jboolean)isDestOpaque
                                      renderOptions:(const RenderOptions * _Nonnull)renderOptions;

- (id<MTLBlitCommandEncoder> _Nonnull)createBlitEncoder;

- (void)endEncoder;
@end

#endif // EncoderManager_h_Included
