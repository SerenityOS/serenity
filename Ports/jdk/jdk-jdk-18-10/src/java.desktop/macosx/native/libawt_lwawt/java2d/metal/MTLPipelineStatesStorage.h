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

#ifndef MTLPipelineStatesStorage_h_Included
#define MTLPipelineStatesStorage_h_Included

#import "MTLUtils.h"
#include "RenderOptions.h"

@class MTLComposite;

/**
 * The MTLPipelineStatesStorage class used to obtain MTLRenderPipelineState
 * */


@interface MTLPipelineStatesStorage : NSObject {
@private

id<MTLDevice>       device;
id<MTLLibrary>      library;
NSMutableDictionary<NSString*, id<MTLFunction>> * shaders;
NSMutableDictionary<NSString*, id<MTLComputePipelineState>> * computeStates;
}

@property (readwrite, assign) id<MTLDevice> device;
@property (readwrite, retain) id<MTLLibrary> library;
@property (readwrite, retain) NSMutableDictionary<NSString*, id<MTLFunction>> * shaders;
@property (readwrite, retain) NSMutableDictionary<NSString*, NSMutableDictionary *> * states;

- (id) initWithDevice:(id<MTLDevice>)device shaderLibPath:(NSString *)shadersLib;

- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId;

- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId
                                  stencilNeeded:(bool)stencilNeeded;

- (id<MTLRenderPipelineState>) getPipelineState:(MTLRenderPipelineDescriptor *) pipelineDescriptor
                                 vertexShaderId:(NSString *)vertexShaderId
                               fragmentShaderId:(NSString *)fragmentShaderId
                                      composite:(MTLComposite*)composite
                                  renderOptions:(const RenderOptions *)renderOptions
                                  stencilNeeded:(bool)stencilNeeded;

- (id<MTLComputePipelineState>) getComputePipelineState:(NSString *)computeShaderId;

- (id<MTLFunction>) getShader:(NSString *)name;
@end


#endif // MTLPipelineStatesStorage_h_Included
