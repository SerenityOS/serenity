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

#ifndef MTLLayer_h_Included
#define MTLLayer_h_Included
#import <Metal/Metal.h>
#import <QuartzCore/CAMetalLayer.h>
#include <CoreVideo/CVDisplayLink.h>
#import "common.h"

@interface MTLLayer : CAMetalLayer
{
@private
    jobject javaLayer;

    // intermediate buffer, used the RQ lock to synchronize
    MTLContext* ctx;
    float bufferWidth;
    float bufferHeight;
    id<MTLTexture> buffer;
    int nextDrawableCount;
    int topInset;
    int leftInset;
    CVDisplayLinkRef displayLink;
}

@property (nonatomic) jobject javaLayer;
@property (readwrite, assign) MTLContext* ctx;
@property (readwrite, assign) float bufferWidth;
@property (readwrite, assign) float bufferHeight;
@property (readwrite, assign) id<MTLTexture> buffer;
@property (readwrite, assign) int nextDrawableCount;
@property (readwrite, assign) int topInset;
@property (readwrite, assign) int leftInset;
@property (readwrite, assign) CVDisplayLinkRef displayLink;

- (id) initWithJavaLayer:(jobject)layer;

- (void) blitTexture;
- (void) fillParallelogramCtxX:(jfloat)x
                             Y:(jfloat)y
                           DX1:(jfloat)dx1
                           DY1:(jfloat)dy1
                           DX2:(jfloat)dx2
                           DY2:(jfloat)dy2;
- (void) blitCallback;
- (void) display;
- (void) redraw;
- (void) startDisplayLink;
- (void) stopDisplayLink;
@end

#endif /* MTLLayer_h_Included */
