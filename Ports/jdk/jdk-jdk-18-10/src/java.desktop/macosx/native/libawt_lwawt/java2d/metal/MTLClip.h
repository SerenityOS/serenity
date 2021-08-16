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

#import <limits.h>
#ifndef MTLClip_h_Included
#define MTLClip_h_Included

#import <Metal/Metal.h>

#include <jni.h>

#include "MTLSurfaceDataBase.h"

enum Clip {
    NO_CLIP,
    RECT_CLIP,
    SHAPE_CLIP
};

@class MTLContext;
@class MTLPipelineStatesStorage;

/**
 * The MTLClip class represents clip mode (rect or stencil)
 * */

@interface MTLClip : NSObject
@property (readonly) id<MTLTexture> stencilTextureRef;
@property (readonly) BOOL stencilMaskGenerationInProgress;
@property (readwrite ) BOOL stencilMaskGenerationStarted;
@property NSUInteger shapeX;
@property NSUInteger shapeY;
@property NSUInteger shapeWidth;
@property NSUInteger shapeHeight;
@property (readonly) BMTLSDOps* dstOps;

- (id)init;
- (BOOL)isEqual:(MTLClip *)other; // used to compare requested with cached
- (void)copyFrom:(MTLClip *)other; // used to save cached

- (BOOL)isShape;
- (BOOL)isRect;

// returns null when clipType != RECT_CLIP
- (const MTLScissorRect *) getRect;

- (void)reset;
- (void)resetStencilState;
- (void)setClipRectX1:(jint)x1 Y1:(jint)y1 X2:(jint)x2 Y2:(jint)y2;
- (void)beginShapeClip:(BMTLSDOps *)dstOps context:(MTLContext *)mtlc;
- (void)endShapeClip:(BMTLSDOps *)dstOps context:(MTLContext *)mtlc;

- (void)setScissorOrStencil:(id<MTLRenderCommandEncoder>)encoder
                  destWidth:(NSUInteger)dw
                 destHeight:(NSUInteger)dh
                     device:(id<MTLDevice>)device;

- (void)setMaskGenerationPipelineState:(id<MTLRenderCommandEncoder>)encoder
                             destWidth:(NSUInteger)dw
                            destHeight:(NSUInteger)dh
                  pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage;

- (NSString *)getDescription __unused; // creates autorelease string
@end

#endif // MTLClip_h_Included
