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

#ifndef MTLPaints_h_Included
#define MTLPaints_h_Included

#import <Metal/Metal.h>
#include "RenderOptions.h"

#define sun_java2d_SunGraphics2D_PAINT_UNDEFINED -1

@class MTLContext;
@class MTLComposite;
@class MTLClip;
@class MTLPipelineStatesStorage;

/**
 * The MTLPaint class represents paint mode (color, gradient etc.)
 */

@interface MTLPaint : NSObject

- (id)initWithState:(jint)state;
- (BOOL)isEqual:(MTLPaint *)other; // used to compare requested with cached
- (NSString *)getDescription;

// For the current paint mode and passed composite (and flags):
// 1. Selects vertex+fragment shader (and corresponding pipelineDesc) and set pipelineState
// 2. Prepares corresponding buffers of vertex and fragment shaders

- (void)setPipelineState:(id <MTLRenderCommandEncoder>)encoder
                 context:(MTLContext *)mtlc
           renderOptions:(const RenderOptions *)renderOptions
    pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage;


- (void)setXorModePipelineState:(id <MTLRenderCommandEncoder>)encoder
                        context:(MTLContext *)mtlc
                  renderOptions:(const RenderOptions *)renderOptions
           pipelineStateStorage:(MTLPipelineStatesStorage *)pipelineStateStorage;
@end

@interface MTLColorPaint : MTLPaint
- (id)initWithColor:(jint)color;
@property (nonatomic, readonly) jint color;
@end

@interface MTLBaseGradPaint : MTLPaint
- (id)initWithState:(jint)state
               mask:(jboolean)useMask
             cyclic:(jboolean)cyclic;
@end

@interface MTLGradPaint : MTLBaseGradPaint

- (id)initWithUseMask:(jboolean)useMask
               cyclic:(jboolean)cyclic
                   p0:(jdouble)p0
                   p1:(jdouble)p1
                   p3:(jdouble)p3
               pixel1:(jint)pixel1
               pixel2:(jint)pixel2;
@end

@interface MTLBaseMultiGradPaint : MTLBaseGradPaint

- (id)initWithState:(jint)state
               mask:(jboolean)useMask
             linear:(jboolean)linear
        cycleMethod:(jboolean)cycleMethod
           numStops:(jint)numStops
          fractions:(jfloat *)fractions
             pixels:(jint *)pixels;
@end

@interface MTLLinearGradPaint : MTLBaseMultiGradPaint

- (id)initWithUseMask:(jboolean)useMask
               linear:(jboolean)linear
          cycleMethod:(jboolean)cycleMethod
             numStops:(jint)numStops
                   p0:(jfloat)p0
                   p1:(jfloat)p1
                   p3:(jfloat)p3
            fractions:(jfloat *)fractions
               pixels:(jint *)pixels;
@end

@interface MTLRadialGradPaint : MTLBaseMultiGradPaint

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
               pixels:(void *)pixels;
@end

@interface MTLTexturePaint : MTLPaint

- (id)initWithUseMask:(jboolean)useMask
              textureID:(id <MTLTexture>)textureID
               isOpaque:(jboolean)isOpaque
                 filter:(jboolean)filter
                    xp0:(jdouble)xp0
                    xp1:(jdouble)xp1
                    xp3:(jdouble)xp3
                    yp0:(jdouble)yp0
                    yp1:(jdouble)yp1
                    yp3:(jdouble)yp3;
@end

#endif /* MTLPaints_h_Included */
