/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#import "AWTSurfaceLayers.h"
#import "ThreadUtilities.h"
#import "LWCToolkit.h"
#import "JNIUtilities.h"

#import <QuartzCore/CATransaction.h>
#import <QuartzCore/CAMetalLayer.h>

@implementation AWTSurfaceLayers

@synthesize windowLayer;

- (id) initWithWindowLayer:(CALayer *)aWindowLayer {
    self = [super init];
    if (self == nil) return self;

    self.windowLayer = aWindowLayer;

    return self;
}

- (void) dealloc {
    self.windowLayer = nil;
    [super dealloc];
}

- (CALayer *) layer {
    return layer;
}

- (void) setLayer:(CALayer *)newLayer {
    if (layer != newLayer) {
        if (layer != nil || newLayer == nil) {
            [layer removeFromSuperlayer];
            [layer release];
        }

        if (newLayer != nil) {
            layer = [newLayer retain];
            // REMIND: window layer -> container layer
            [windowLayer addSublayer: layer];
        }
    }
}

// Updates back buffer size of the layer if it's an OpenGL/Metal layer
// including all OpenGL/Metal sublayers
+ (void) repaintLayersRecursively:(CALayer*)aLayer {
    if ([aLayer isKindOfClass:[CAOpenGLLayer class]] ||
        [aLayer isKindOfClass:[CAMetalLayer class]]) {
        [aLayer setNeedsDisplay];
    }
    for(CALayer *child in aLayer.sublayers) {
        [AWTSurfaceLayers repaintLayersRecursively: child];
    }
}

- (void) setBounds:(CGRect)rect {
    // translates values to the coordinate system of the "root" layer
    rect.origin.y = windowLayer.bounds.size.height - rect.origin.y - rect.size.height;
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    layer.frame = rect;
    [CATransaction commit];
    [AWTSurfaceLayers repaintLayersRecursively:layer];
}

@end

/*
 * Class:     sun_lwawt_macosx_CPlatformComponent
 * Method:    nativeCreateComponent
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CPlatformComponent_nativeCreateComponent
(JNIEnv *env, jobject obj, jlong windowLayerPtr)
{
  __block AWTSurfaceLayers *surfaceLayers = nil;

JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){

        CALayer *windowLayer = jlong_to_ptr(windowLayerPtr);
        surfaceLayers = [[AWTSurfaceLayers alloc] initWithWindowLayer: windowLayer];
    }];

JNI_COCOA_EXIT(env);

  return ptr_to_jlong(surfaceLayers);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformComponent
 * Method:    nativeSetBounds
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformComponent_nativeSetBounds
(JNIEnv *env, jclass clazz, jlong surfaceLayersPtr, jint x, jint y, jint width, jint height)
{
JNI_COCOA_ENTER(env);

  AWTSurfaceLayers *surfaceLayers = OBJC(surfaceLayersPtr);

  [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

      CGRect rect = CGRectMake(x, y, width, height);
      [surfaceLayers setBounds: rect];
  }];

JNI_COCOA_EXIT(env);
}
