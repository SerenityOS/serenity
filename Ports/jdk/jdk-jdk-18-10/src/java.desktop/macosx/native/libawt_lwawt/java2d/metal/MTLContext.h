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

#ifndef MTLContext_h_Included
#define MTLContext_h_Included

#include "sun_java2d_pipe_BufferedContext.h"
#include "sun_java2d_metal_MTLContext_MTLContextCaps.h"

#import <Metal/Metal.h>

#include "MTLTexturePool.h"
#include "MTLPipelineStatesStorage.h"
#include "MTLTransform.h"
#include "MTLComposite.h"
#include "MTLPaints.h"
#include "MTLClip.h"
#include "EncoderManager.h"
#include "MTLSamplerManager.h"

@class MTLStencilManager;

// Constant from
// https://developer.apple.com/metal/Metal-Feature-Set-Tables.pdf
#define MTL_GPU_FAMILY_MAC_TXT_SIZE 16384

/**
 * The MTLCommandBufferWrapper class contains command buffer and
 * associated resources that will be released in completion handler
 * */
@interface MTLCommandBufferWrapper : NSObject
- (id<MTLCommandBuffer>) getCommandBuffer;
- (void) onComplete; // invoked from completion handler in some pooled thread
- (void) registerPooledTexture:(MTLPooledTextureHandle *)handle;
@end

/**
 * The MTLContext class contains cached state relevant to the native
 * MTL context stored within the native ctxInfo field.  Each Java-level
 * MTLContext object is associated with a native-level MTLContext class.
 * */
@interface MTLContext : NSObject
@property (readonly) MTLComposite * composite;
@property (readwrite, retain) MTLPaint * paint;
@property (readonly) MTLTransform * transform;
@property (readonly) MTLClip * clip;

@property jint          textureFunction;
@property jboolean      vertexCacheEnabled;
@property jboolean      aaEnabled;

@property (readonly, strong)   id<MTLDevice>   device;
@property (strong) id<MTLCommandQueue>         commandQueue;
@property (strong) id<MTLCommandQueue>         blitCommandQueue;
@property (strong) id<MTLBuffer>               vertexBuffer;

@property (readonly) EncoderManager * encoderManager;
@property (readonly) MTLSamplerManager * samplerManager;
@property (readonly) MTLStencilManager * stencilManager;

@property (strong)MTLPipelineStatesStorage*   pipelineStateStorage;
@property (strong)MTLTexturePool*             texturePool;

- (MTLCommandBufferWrapper *) getCommandBufferWrapper; // creates command buffer wrapper (when doesn't exist)
- (MTLCommandBufferWrapper *) pullCommandBufferWrapper; // returns current buffer wrapper with loosing object ownership

/**
 * Fetches the MTLContext associated with the given destination surface,
 * makes the context current for those surfaces, updates the destination
 * viewport, and then returns a pointer to the MTLContext.
 */
+ (MTLContext*) setSurfacesEnv:(JNIEnv*)env src:(jlong)pSrc dst:(jlong)pDst;

- (id)initWithDevice:(id<MTLDevice>)d shadersLib:(NSString*)shadersLib;
- (void)dealloc;

/**
 * Resets the current clip state (disables both scissor and depth tests).
 */
- (void)resetClip;

/**
 * Sets the Metal scissor bounds to the provided rectangular clip bounds.
 */
- (void)setClipRectX1:(jint)x1 Y1:(jint)y1 X2:(jint)x2 Y2:(jint)y2;

- (const MTLScissorRect *)clipRect;

/**
 * Sets up a complex (shape) clip using the Metal stencil buffer.  This
 * method prepares the stencil buffer so that the clip Region spans can
 * be "rendered" into it.  The stencil buffer is first cleared, then the
 * stencil func is setup so that when we render the clip spans,
 * nothing is rendered into the color buffer, but for each pixel that would
 * be rendered, a 0xFF value is placed into that location in the stencil
 * buffer.  With stencil test enabled, pixels will only be rendered into the
 * color buffer if the corresponding value at that (x,y) location in the
 * stencil buffer is equal to 0xFF.
 */
- (void)beginShapeClip:(BMTLSDOps *)dstOps;

/**
 * Finishes setting up the shape clip by resetting the stencil func
 * so that future rendering operations will once again be encoded for the
 * color buffer (while respecting the clip set up in the stencil buffer).
 */
- (void)endShapeClip:(BMTLSDOps *)dstOps;

/**
 * Resets all Metal compositing state (disables blending and logic
 * operations).
 */
- (void)resetComposite;

/**
 * Initializes the Metal blending state.  XOR mode is disabled and the
 * appropriate blend functions are setup based on the AlphaComposite rule
 * constant.
 */
- (void)setAlphaCompositeRule:(jint)rule extraAlpha:(jfloat)extraAlpha
                        flags:(jint)flags;

/**
 * Returns autorelease string with composite description (for debugging only)
 */
- (NSString*)getCompositeDescription;

/**
 * Returns autorelease string with paint description (for debugging only)
 */
- (NSString*)getPaintDescription;

/**
 * Initializes the Metal logic op state to XOR mode.  Blending is disabled
 * before enabling logic op mode.  The XOR pixel value will be applied
 * later in the MTLContext_SetColor() method.
 */
- (void)setXorComposite:(jint)xorPixel;
- (jboolean)useXORComposite;

/**
 * Resets the Metal transform state back to the identity matrix.
 */
- (void)resetTransform;

/**
 * Initializes the Metal transform state by setting the modelview transform
 * using the given matrix parameters.
 *
 * REMIND: it may be worthwhile to add serial id to AffineTransform, so we
 *         could do a quick check to see if the xform has changed since
 *         last time... a simple object compare won't suffice...
 */
- (void)setTransformM00:(jdouble) m00 M10:(jdouble) m10
                    M01:(jdouble) m01 M11:(jdouble) m11
                    M02:(jdouble) m02 M12:(jdouble) m12;

- (void)reset;
- (void)resetPaint;
- (void)setColorPaint:(int)pixel;
- (void)setGradientPaintUseMask:(jboolean)useMask
                         cyclic:(jboolean)cyclic
                             p0:(jdouble)p0
                             p1:(jdouble)p1
                             p3:(jdouble)p3
                         pixel1:(jint)pixel1
                         pixel2:(jint) pixel2;
- (void)setLinearGradientPaint:(jboolean)useMask
                   linear:(jboolean)linear
              cycleMethod:(jint)cycleMethod
                 numStops:(jint)numStops
                       p0:(jfloat)p0
                       p1:(jfloat)p1
                       p3:(jfloat)p3
                fractions:(jfloat *)fractions
                   pixels:(jint *)pixels;
- (void)setRadialGradientPaint:(jboolean)useMask
                   linear:(jboolean)linear
              cycleMethod:(jboolean)cycleMethod
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
- (void)setTexturePaint:(jboolean)useMask
           pSrcOps:(jlong)pSrcOps
            filter:(jboolean)filter
               xp0:(jdouble)xp0
               xp1:(jdouble)xp1
               xp3:(jdouble)xp3
               yp0:(jdouble)yp0
               yp1:(jdouble)yp1
               yp3:(jdouble)yp3;

// Sets current image conversion operation (instance of MTLConvolveOp, MTLRescaleOp, MTLLookupOp).
// Used only in MTLIsoBlit (to blit image with some conversion). Pattern of usage: enableOp -> IsoBlit -> disableOp.
// TODO: Need to remove it from MTLContext and pass it as an argument for IsoBlit (because it's more
// simple and clear)
-(void)setBufImgOp:(NSObject*)bufImgOp;

-(NSObject*)getBufImgOp;

- (id<MTLCommandBuffer>)createCommandBuffer;
- (id<MTLCommandBuffer>)createBlitCommandBuffer;
@end

/**
 * See BufferedContext.java for more on these flags...
 */
#define MTLC_NO_CONTEXT_FLAGS \
    sun_java2d_pipe_BufferedContext_NO_CONTEXT_FLAGS
#define MTLC_SRC_IS_OPAQUE    \
    sun_java2d_pipe_BufferedContext_SRC_IS_OPAQUE
#define MTLC_USE_MASK         \
    sun_java2d_pipe_BufferedContext_USE_MASK

#endif /* MTLContext_h_Included */
