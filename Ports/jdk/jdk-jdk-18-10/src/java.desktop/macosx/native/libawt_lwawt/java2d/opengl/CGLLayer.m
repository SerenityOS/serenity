/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#import "CGLGraphicsConfig.h"
#import "CGLLayer.h"
#import "ThreadUtilities.h"
#import "LWCToolkit.h"
#import "CGLSurfaceData.h"
#import "JNIUtilities.h"


extern NSOpenGLPixelFormat *sharedPixelFormat;
extern NSOpenGLContext *sharedContext;

@implementation CGLLayer

@synthesize javaLayer;
@synthesize textureID;
@synthesize target;
@synthesize textureWidth;
@synthesize textureHeight;

- (id) initWithJavaLayer:(jobject)layer;
{
AWT_ASSERT_APPKIT_THREAD;
    // Initialize ourselves
    self = [super init];
    if (self == nil) return self;

    self.javaLayer = layer;

    // NOTE: async=YES means that the layer is re-cached periodically
    self.asynchronous = FALSE;
    self.contentsGravity = kCAGravityTopLeft;
    //Layer backed view
    //self.needsDisplayOnBoundsChange = YES;
    //self.autoresizingMask = kCALayerWidthSizable | kCALayerHeightSizable;

    //Disable CALayer's default animation
    NSMutableDictionary * actions = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                    [NSNull null], @"anchorPoint",
                                    [NSNull null], @"bounds",
                                    [NSNull null], @"contents",
                                    [NSNull null], @"contentsScale",
                                    [NSNull null], @"onOrderIn",
                                    [NSNull null], @"onOrderOut",
                                    [NSNull null], @"position",
                                    [NSNull null], @"sublayers",
                                    nil];
    self.actions = actions;
    [actions release];

    textureID = 0; // texture will be created by rendering pipe
    target = 0;

    return self;
}

- (void) dealloc {
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
    (*env)->DeleteWeakGlobalRef(env, self.javaLayer);
    self.javaLayer = nil;
    [super dealloc];
}

- (CGLPixelFormatObj)copyCGLPixelFormatForDisplayMask:(uint32_t)mask {
    return CGLRetainPixelFormat(sharedPixelFormat.CGLPixelFormatObj);
}

- (CGLContextObj)copyCGLContextForPixelFormat:(CGLPixelFormatObj)pixelFormat {
    CGLContextObj contextObj = NULL;
    CGLCreateContext(pixelFormat, sharedContext.CGLContextObj, &contextObj);
    return contextObj;
}

// use texture (intermediate buffer) as src and blit it to the layer
- (void) blitTexture
{
    if (textureID == 0) {
        return;
    }

    glEnable(target);
    glBindTexture(target, textureID);

    glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE); // srccopy

    float swid = 1.0f, shgt = 1.0f;
    if (target == GL_TEXTURE_RECTANGLE_ARB) {
        swid = textureWidth;
        shgt = textureHeight;
    }
    glBegin(GL_QUADS);
    glTexCoord2f(0.0f, 0.0f); glVertex2f(-1.0f, -1.0f);
    glTexCoord2f(swid, 0.0f); glVertex2f( 1.0f, -1.0f);
    glTexCoord2f(swid, shgt); glVertex2f( 1.0f,  1.0f);
    glTexCoord2f(0.0f, shgt); glVertex2f(-1.0f,  1.0f);
    glEnd();

    glBindTexture(target, 0);
    glDisable(target);
}

-(BOOL)canDrawInCGLContext:(CGLContextObj)glContext pixelFormat:(CGLPixelFormatObj)pixelFormat forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp{
    return textureID == 0 ? NO : YES;
}

-(void)drawInCGLContext:(CGLContextObj)glContext pixelFormat:(CGLPixelFormatObj)pixelFormat forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    AWT_ASSERT_APPKIT_THREAD;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    DECLARE_CLASS(jc_JavaLayer, "sun/java2d/opengl/CGLLayer");
    DECLARE_METHOD(jm_drawInCGLContext, jc_JavaLayer, "drawInCGLContext", "()V");

    jobject javaLayerLocalRef = (*env)->NewLocalRef(env, self.javaLayer);
    if ((*env)->IsSameObject(env, javaLayerLocalRef, NULL)) {
        return;
    }

    // Set the current context to the one given to us.
    CGLSetCurrentContext(glContext);

    // Should clear the whole CALayer, because it can be larger than our texture.
    glClearColor(0.0, 0.0, 0.0, 0.0);
    glClear(GL_COLOR_BUFFER_BIT);

    glViewport(0, 0, textureWidth, textureHeight);

    (*env)->CallVoidMethod(env, javaLayerLocalRef, jm_drawInCGLContext);
    CHECK_EXCEPTION();
    (*env)->DeleteLocalRef(env, javaLayerLocalRef);

    // Call super to finalize the drawing. By default all it does is call glFlush().
    [super drawInCGLContext:glContext pixelFormat:pixelFormat forLayerTime:timeInterval displayTime:timeStamp];

    CGLSetCurrentContext(NULL);
}

@end

/*
 * Class:     sun_java2d_opengl_CGLLayer
 * Method:    nativeCreateLayer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_opengl_CGLLayer_nativeCreateLayer
(JNIEnv *env, jobject obj)
{
    __block CGLLayer *layer = nil;

JNI_COCOA_ENTER(env);

    jobject javaLayer = (*env)->NewWeakGlobalRef(env, obj);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
            AWT_ASSERT_APPKIT_THREAD;

            layer = [[CGLLayer alloc] initWithJavaLayer: javaLayer];
    }];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(layer);
}

// Must be called under the RQ lock.
JNIEXPORT void JNICALL
Java_sun_java2d_opengl_CGLLayer_validate
(JNIEnv *env, jclass cls, jlong layerPtr, jobject surfaceData)
{
    CGLLayer *layer = OBJC(layerPtr);

    if (surfaceData != NULL) {
        OGLSDOps *oglsdo = (OGLSDOps*) SurfaceData_GetOps(env, surfaceData);
        layer.textureID = oglsdo->textureID;
        layer.target = GL_TEXTURE_2D;
        layer.textureWidth = oglsdo->width;
        layer.textureHeight = oglsdo->height;
    } else {
        layer.textureID = 0;
    }
}

// Must be called on the AppKit thread and under the RQ lock.
JNIEXPORT void JNICALL
Java_sun_java2d_opengl_CGLLayer_blitTexture
(JNIEnv *env, jclass cls, jlong layerPtr)
{
    CGLLayer *layer = jlong_to_ptr(layerPtr);

    [layer blitTexture];
}

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_CGLLayer_nativeSetScale
(JNIEnv *env, jclass cls, jlong layerPtr, jdouble scale)
{
    JNI_COCOA_ENTER(env);
    CGLLayer *layer = jlong_to_ptr(layerPtr);
    // We always call all setXX methods asynchronously, exception is only in
    // this method where we need to change native texture size and layer's scale
    // in one call on appkit, otherwise we'll get window's contents blinking,
    // during screen-2-screen moving.
    [ThreadUtilities performOnMainThreadWaiting:[NSThread isMainThread] block:^(){
        layer.contentsScale = scale;
    }];
    JNI_COCOA_EXIT(env);
}
