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

#include "MTLSamplerManager.h"
#include "MTLContext.h"
#include "sun_java2d_SunGraphics2D.h"
#import "common.h"

@implementation MTLSamplerManager {
    id<MTLSamplerState> _samplerNearestClamp;
    id<MTLSamplerState> _samplerLinearClamp;
    id<MTLSamplerState> _samplerNearestRepeat;
    id<MTLSamplerState> _samplerLinearRepeat;
}

- (id _Nonnull)initWithDevice:(id<MTLDevice>) device {
    self = [super init];
    if (self) {
        MTLSamplerDescriptor *samplerDescriptor = [[MTLSamplerDescriptor new] autorelease];

        samplerDescriptor.rAddressMode = MTLSamplerAddressModeClampToEdge;
        samplerDescriptor.sAddressMode = MTLSamplerAddressModeClampToEdge;
        samplerDescriptor.tAddressMode = MTLSamplerAddressModeClampToEdge;

        samplerDescriptor.minFilter = MTLSamplerMinMagFilterNearest;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterNearest;
        _samplerNearestClamp = [device newSamplerStateWithDescriptor:samplerDescriptor];

        samplerDescriptor.minFilter = MTLSamplerMinMagFilterLinear;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterLinear;
        _samplerLinearClamp = [device newSamplerStateWithDescriptor:samplerDescriptor];

        samplerDescriptor.rAddressMode = MTLSamplerAddressModeRepeat;
        samplerDescriptor.sAddressMode = MTLSamplerAddressModeRepeat;
        samplerDescriptor.tAddressMode = MTLSamplerAddressModeRepeat;

        samplerDescriptor.minFilter = MTLSamplerMinMagFilterNearest;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterNearest;
        _samplerNearestRepeat = [device newSamplerStateWithDescriptor:samplerDescriptor];

        samplerDescriptor.minFilter = MTLSamplerMinMagFilterLinear;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterLinear;
        _samplerLinearRepeat = [device newSamplerStateWithDescriptor:samplerDescriptor];
    }
    return self;
}

- (void) setSamplerWithEncoder:(id<MTLRenderCommandEncoder>) encoder
                 interpolation:(int) interpolation
                        repeat:(bool) repeat {
    id<MTLSamplerState> sampler;
    if (repeat) {
        sampler = interpolation == INTERPOLATION_BILINEAR ? _samplerLinearRepeat : _samplerNearestRepeat;
    } else {
        sampler = interpolation == INTERPOLATION_BILINEAR ? _samplerLinearClamp : _samplerNearestClamp;
    }
    [encoder setFragmentSamplerState:sampler atIndex:0];
}

- (void)dealloc {
    [_samplerNearestClamp release];
    [_samplerLinearClamp release];
    [_samplerNearestRepeat release];
    [_samplerLinearRepeat release];
    [super dealloc];
}

@end
