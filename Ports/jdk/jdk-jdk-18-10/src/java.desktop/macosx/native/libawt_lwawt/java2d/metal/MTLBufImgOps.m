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

#include <jlong.h>

#include "MTLBufImgOps.h"
#include "MTLContext.h"
#include "MTLRenderQueue.h"
#include "MTLSurfaceDataBase.h"
#include "GraphicsPrimitiveMgr.h"

@implementation MTLRescaleOp {
    jboolean _isNonPremult;
    jfloat _normScaleFactors[4];
    jfloat _normOffsets[4];
}

-(jfloat *) getScaleFactors {
    return _normScaleFactors;
}
-(jfloat *) getOffsets {
    return _normOffsets;
}

- (id)init:(jboolean)isNonPremult factors:(unsigned char *)factors offsets:(unsigned char *)offsets {
    self = [super init];
    if (self) {
        J2dTraceLn1(J2D_TRACE_INFO,"Created MTLRescaleOp: isNonPremult=%d", isNonPremult);

        _isNonPremult = isNonPremult;
        _normScaleFactors[0] = NEXT_FLOAT(factors);
        _normScaleFactors[1] = NEXT_FLOAT(factors);
        _normScaleFactors[2] = NEXT_FLOAT(factors);
        _normScaleFactors[3] = NEXT_FLOAT(factors);
        _normOffsets[0] = NEXT_FLOAT(offsets);
        _normOffsets[1] = NEXT_FLOAT(offsets);
        _normOffsets[2] = NEXT_FLOAT(offsets);
        _normOffsets[3] = NEXT_FLOAT(offsets);
    }
    return self;
}

- (NSString *)getDescription {
    return [NSString stringWithFormat:@"rescale: nonPremult=%d", _isNonPremult];
}
@end

@implementation MTLConvolveOp {
    id<MTLBuffer> _buffer;
    float _imgEdge[4];
    int _kernelSize;
    jboolean _isEdgeZeroFill;
}

- (id)init:(jboolean)edgeZeroFill kernelWidth:(jint)kernelWidth
                                 kernelHeight:(jint)kernelHeight
                                     srcWidth:(jint)srcWidth
                                    srcHeight:(jint)srcHeight
                                       kernel:(unsigned char *)kernel
                                       device:(id<MTLDevice>)device {
    self = [super init];
    if (self) {
        J2dTraceLn2(J2D_TRACE_INFO,"Created MTLConvolveOp: kernelW=%d kernelH=%d", kernelWidth, kernelHeight);
        _isEdgeZeroFill = edgeZeroFill;

        _kernelSize = kernelWidth * kernelHeight;
        _buffer = [device newBufferWithLength:_kernelSize*sizeof(vector_float3) options:MTLResourceStorageModeShared];

        float * kernelVals = [_buffer contents];
        int kIndex = 0;
        for (int i = -kernelHeight/2; i < kernelHeight/2+1; i++) {
            for (int j = -kernelWidth/2; j < kernelWidth/2+1; j++) {
                kernelVals[kIndex+0] = j/(float)srcWidth;
                kernelVals[kIndex+1] = i/(float)srcHeight;
                kernelVals[kIndex+2] = NEXT_FLOAT(kernel);
                kIndex += 3;
            }
        }

        _imgEdge[0] = (kernelWidth/2)/(float)srcWidth;
        _imgEdge[1] = (kernelHeight/2)/(float)srcHeight;
        _imgEdge[2] = 1 - _imgEdge[0];
        _imgEdge[3] = 1 - _imgEdge[1];
    }
    return self;
}

- (void) dealloc {
    [_buffer release];
    [super dealloc];
}

- (id<MTLBuffer>) getBuffer {
    return _buffer;
}

- (const float *) getImgEdge {
    return _imgEdge;
}

- (NSString *)getDescription {
    return [NSString stringWithFormat:@"convolve: isEdgeZeroFill=%d", _isEdgeZeroFill];
}
@end


@implementation MTLLookupOp {
    float _offset[4];
    jboolean _isUseSrcAlpha;
    jboolean _isNonPremult;

    id<MTLTexture> _lookupTex;
}

- (id)init:(jboolean)nonPremult shortData:(jboolean)shortData
                                 numBands:(jint)numBands
                               bandLength:(jint)bandLength
                                   offset:(jint)offset
                              tableValues:(void *)tableValues
                                   device:(id<MTLDevice>)device {
    self = [super init];
    if (self) {
        J2dTraceLn4(J2D_TRACE_INFO,"Created MTLLookupOp: short=%d num=%d len=%d off=%d",
                    shortData, numBands, bandLength, offset);

        _isUseSrcAlpha = numBands != 4;
        _isNonPremult = nonPremult;

        _offset[0] = offset / 255.0f;
        _offset[1] = _offset[0];
        _offset[2] = _offset[0];
        _offset[3] = _offset[0];

        MTLTextureDescriptor *textureDescriptor =
                [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatA8Unorm
                                                                   width:(NSUInteger)256
                                                                  height:(NSUInteger)4
                                                               mipmapped:NO];

        _lookupTex = [device newTextureWithDescriptor:textureDescriptor];

        void *bands[4];
        for (int i = 0; i < 4; i++) {
            bands[i] = NULL;
        }
        int bytesPerElem = (shortData ? 2 : 1);
        if (numBands == 1) {
            // replicate the single band for R/G/B; alpha band is unused
            for (int i = 0; i < 3; i++) {
                bands[i] = tableValues;
            }
            bands[3] = NULL;
        } else if (numBands == 3) {
            // user supplied band for each of R/G/B; alpha band is unused
            for (int i = 0; i < 3; i++) {
                bands[i] = PtrPixelsBand(tableValues, i, bandLength, bytesPerElem);
            }
            bands[3] = NULL;
        } else if (numBands == 4) {
            // user supplied band for each of R/G/B/A
            for (int i = 0; i < 4; i++) {
                bands[i] = PtrPixelsBand(tableValues, i, bandLength, bytesPerElem);
            }
        }

        for (int i = 0; i < 4; i++) {
            if (bands[i] == NULL)
                continue;

            MTLRegion region = {
                    {0, i, 0},
                    {bandLength, 1,1}
            };

            [_lookupTex replaceRegion:region
                                    mipmapLevel:0
                                      withBytes:bands[i]
                                    bytesPerRow:bandLength*bytesPerElem];
        }
    }
    return self;
}

- (void) dealloc {
    [_lookupTex release];
    [super dealloc];
}

- (jfloat *) getOffset {
    return _offset;
}

- (id<MTLTexture>) getLookupTexture {
    return _lookupTex;
}

- (NSString *)getDescription {
    return [NSString stringWithFormat:@"lookup: offset=%f", _offset[0]];
}

@end
