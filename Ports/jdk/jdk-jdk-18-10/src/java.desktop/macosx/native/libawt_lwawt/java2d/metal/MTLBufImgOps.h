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

#ifndef MTLBufImgOps_h_Included
#define MTLBufImgOps_h_Included

#include "MTLContext.h"

@interface MTLRescaleOp : NSObject
- (id)init:(jboolean)isNonPremult factors:(unsigned char *)factors offsets:(unsigned char *)offsets;
- (jfloat *)getScaleFactors;
- (jfloat *)getOffsets;
- (NSString *)getDescription; // creates autorelease string

@property (readonly) jboolean isNonPremult;
@end

@interface MTLConvolveOp : NSObject
- (id)init:(jboolean)edgeZeroFill kernelWidth:(jint)kernelWidth
                                 kernelHeight:(jint)kernelHeight
                                     srcWidth:(jint)srcWidth
                                    srcHeight:(jint)srcHeight
                                       kernel:(unsigned char *)kernel
                                       device:(id<MTLDevice>)device;
- (void) dealloc;

- (id<MTLBuffer>) getBuffer;
- (const float *) getImgEdge;
- (NSString *)getDescription; // creates autorelease string

@property (readonly) jboolean isEdgeZeroFill;
@property (readonly) int kernelSize;
@end

@interface MTLLookupOp : NSObject
- (id)init:(jboolean)nonPremult shortData:(jboolean)shortData
                                 numBands:(jint)numBands
                               bandLength:(jint)bandLength
                                   offset:(jint)offset
                              tableValues:(void *)tableValues
                                   device:(id<MTLDevice>)device;
- (void) dealloc;

- (jfloat *)getOffset;
- (id<MTLTexture>) getLookupTexture;
- (NSString *)getDescription; // creates autorelease string

@property (readonly) jboolean isUseSrcAlpha;
@property (readonly) jboolean isNonPremult;
@end

#endif /* MTLBufImgOps_h_Included */
