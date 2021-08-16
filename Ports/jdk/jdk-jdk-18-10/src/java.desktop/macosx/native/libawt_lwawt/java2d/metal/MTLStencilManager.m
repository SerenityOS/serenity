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

#include "MTLStencilManager.h"
//#include "MTLContext.h"
//#include "sun_java2d_SunGraphics2D.h"
//#import "common.h"

@implementation MTLStencilManager {
    id<MTLDepthStencilState> _stencilState;
    id<MTLDepthStencilState> _genStencilState;
}

@synthesize stencilState = _stencilState;
@synthesize genStencilState = _genStencilState;

- (id _Nonnull)initWithDevice:(id<MTLDevice>) device {
    self = [super init];
    if (self) {
        MTLDepthStencilDescriptor* stencilDescriptor;
        stencilDescriptor = [[MTLDepthStencilDescriptor new] autorelease];
        stencilDescriptor.frontFaceStencil.stencilCompareFunction = MTLCompareFunctionEqual;
        stencilDescriptor.frontFaceStencil.stencilFailureOperation = MTLStencilOperationKeep;

        // TODO : backFaceStencil can be set to nil if all primitives are drawn as front-facing primitives
        // currently, fill parallelogram uses back-facing primitive drawing - that needs to be changed.
        // Once that part is changed, set backFaceStencil to nil
        //stencilDescriptor.backFaceStencil = nil;

        stencilDescriptor.backFaceStencil.stencilCompareFunction = MTLCompareFunctionEqual;
        stencilDescriptor.backFaceStencil.stencilFailureOperation = MTLStencilOperationKeep;
        _stencilState = [device newDepthStencilStateWithDescriptor:stencilDescriptor];

        MTLDepthStencilDescriptor* genStencilDescriptor;
        genStencilDescriptor = [[MTLDepthStencilDescriptor new] autorelease];
        genStencilDescriptor.backFaceStencil.stencilCompareFunction = MTLCompareFunctionAlways;
        genStencilDescriptor.backFaceStencil.depthStencilPassOperation = MTLStencilOperationReplace;
        genStencilDescriptor.frontFaceStencil.stencilCompareFunction = MTLCompareFunctionAlways;
        genStencilDescriptor.frontFaceStencil.depthStencilPassOperation = MTLStencilOperationReplace;
        _genStencilState = [device newDepthStencilStateWithDescriptor:genStencilDescriptor];
    }
    return self;
}

- (void)dealloc {
    [_stencilState release];
    [super dealloc];
}

@end
