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

#import <stdlib.h>

#import "sun_java2d_opengl_CGLSurfaceData.h"

#import "JNIUtilities.h"
#import "OGLRenderQueue.h"
#import "CGLGraphicsConfig.h"
#import "CGLSurfaceData.h"
#import "ThreadUtilities.h"

/* JDK's glext.h is already included and will prevent the Apple glext.h
 * being included, so define the externs directly
 */
extern void glBindFramebufferEXT(GLenum target, GLuint framebuffer);
extern CGLError CGLTexImageIOSurface2D(
        CGLContextObj ctx, GLenum target, GLenum internal_format,
        GLsizei width, GLsizei height, GLenum format, GLenum type,
        IOSurfaceRef ioSurface, GLuint plane);

/**
 * The methods in this file implement the native windowing system specific
 * layer (CGL) for the OpenGL-based Java 2D pipeline.
 */

#pragma mark -
#pragma mark "--- Mac OS X specific methods for GL pipeline ---"

// TODO: hack that's called from OGLRenderQueue to test out unlockFocus behavior
#if 0
void
OGLSD_UnlockFocus(OGLContext *oglc, OGLSDOps *dstOps)
{
    CGLCtxInfo *ctxinfo = (CGLCtxInfo *)oglc->ctxInfo;
    CGLSDOps *cglsdo = (CGLSDOps *)dstOps->privOps;
    fprintf(stderr, "about to unlock focus: %p %p\n",
            cglsdo->peerData, ctxinfo->context);

    NSOpenGLView *nsView = cglsdo->peerData;
    if (nsView != NULL) {
JNI_COCOA_ENTER(env);
        [nsView unlockFocus];
JNI_COCOA_EXIT(env);
    }
}
#endif

/**
 * Makes the given context current to its associated "scratch" surface.  If
 * the operation is successful, this method will return JNI_TRUE; otherwise,
 * returns JNI_FALSE.
 */
static jboolean
CGLSD_MakeCurrentToScratch(JNIEnv *env, OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "CGLSD_MakeCurrentToScratch");

    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "CGLSD_MakeCurrentToScratch: context is null");
        return JNI_FALSE;
    }

JNI_COCOA_ENTER(env);

    CGLCtxInfo *ctxinfo = (CGLCtxInfo *)oglc->ctxInfo;
#if USE_NSVIEW_FOR_SCRATCH
    [ctxinfo->context makeCurrentContext];
#else
    [ctxinfo->context clearDrawable];
    [ctxinfo->context makeCurrentContext];
    [ctxinfo->context setPixelBuffer: ctxinfo->scratchSurface
            cubeMapFace: 0
            mipMapLevel: 0
            currentVirtualScreen: [ctxinfo->context currentVirtualScreen]];
#endif

JNI_COCOA_EXIT(env);

    return JNI_TRUE;
}

/**
 * This function disposes of any native windowing system resources associated
 * with this surface.
 */
void
OGLSD_DestroyOGLSurface(JNIEnv *env, OGLSDOps *oglsdo)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_DestroyOGLSurface");

JNI_COCOA_ENTER(env);

    CGLSDOps *cglsdo = (CGLSDOps *)oglsdo->privOps;
    if (oglsdo->drawableType == OGLSD_WINDOW) {
        // detach the NSView from the NSOpenGLContext
        CGLGraphicsConfigInfo *cglInfo = cglsdo->configInfo;
        OGLContext *oglc = cglInfo->context;
        CGLCtxInfo *ctxinfo = (CGLCtxInfo *)oglc->ctxInfo;
        [ctxinfo->context clearDrawable];
    }

    oglsdo->drawableType = OGLSD_UNDEFINED;

JNI_COCOA_EXIT(env);
}

/**
 * Makes the given GraphicsConfig's context current to its associated
 * "scratch" surface.  If there is a problem making the context current,
 * this method will return NULL; otherwise, returns a pointer to the
 * OGLContext that is associated with the given GraphicsConfig.
 */
OGLContext *
OGLSD_SetScratchSurface(JNIEnv *env, jlong pConfigInfo)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SetScratchContext");

    CGLGraphicsConfigInfo *cglInfo = (CGLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);
    if (cglInfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_SetScratchContext: cgl config info is null");
        return NULL;
    }

    OGLContext *oglc = cglInfo->context;
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_SetScratchContext: ogl context is null");
        return NULL;
    }

    CGLCtxInfo *ctxinfo = (CGLCtxInfo *)oglc->ctxInfo;

JNI_COCOA_ENTER(env);

    // avoid changing the context's target view whenever possible, since
    // calling setView causes flickering; as long as our context is current
    // to some view, it's not necessary to switch to the scratch surface
    if ([ctxinfo->context view] == nil) {
        // it seems to be necessary to explicitly flush between context changes
        OGLContext *currentContext = OGLRenderQueue_GetCurrentContext();
        if (currentContext != NULL) {
            j2d_glFlush();
        }

        if (!CGLSD_MakeCurrentToScratch(env, oglc)) {
            return NULL;
        }
    // make sure our context is current
    } else if ([NSOpenGLContext currentContext] != ctxinfo->context) {
        [ctxinfo->context makeCurrentContext];
    }

    if (OGLC_IS_CAP_PRESENT(oglc, CAPS_EXT_FBOBJECT)) {
        // the GL_EXT_framebuffer_object extension is present, so this call
        // will ensure that we are bound to the scratch surface (and not
        // some other framebuffer object)
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }

JNI_COCOA_EXIT(env);

    return oglc;
}

/**
 * Makes a context current to the given source and destination
 * surfaces.  If there is a problem making the context current, this method
 * will return NULL; otherwise, returns a pointer to the OGLContext that is
 * associated with the destination surface.
 */
OGLContext *
OGLSD_MakeOGLContextCurrent(JNIEnv *env, OGLSDOps *srcOps, OGLSDOps *dstOps)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_MakeOGLContextCurrent");

    CGLSDOps *dstCGLOps = (CGLSDOps *)dstOps->privOps;

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  src: %d %p dst: %d %p", srcOps->drawableType, srcOps, dstOps->drawableType, dstOps);

    OGLContext *oglc = dstCGLOps->configInfo->context;
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_MakeOGLContextCurrent: context is null");
        return NULL;
    }

    CGLCtxInfo *ctxinfo = (CGLCtxInfo *)oglc->ctxInfo;

    // it seems to be necessary to explicitly flush between context changes
    OGLContext *currentContext = OGLRenderQueue_GetCurrentContext();
    if (currentContext != NULL) {
        j2d_glFlush();
    }

    if (dstOps->drawableType == OGLSD_FBOBJECT) {
        // first make sure we have a current context (if the context isn't
        // already current to some drawable, we will make it current to
        // its scratch surface)
        if (oglc != currentContext) {
            if (!CGLSD_MakeCurrentToScratch(env, oglc)) {
                return NULL;
            }
        }

        // now bind to the fbobject associated with the destination surface;
        // this means that all rendering will go into the fbobject destination
        // (note that we unbind the currently bound texture first; this is
        // recommended procedure when binding an fbobject)
        j2d_glBindTexture(GL_TEXTURE_2D, 0);
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dstOps->fbobjectID);

        return oglc;
    }

JNI_COCOA_ENTER(env);

    CGLSDOps *cglsdo = (CGLSDOps *)dstOps->privOps;
    NSView *nsView = (NSView *)cglsdo->peerData;

    if ([ctxinfo->context view] != nsView) {
        [ctxinfo->context makeCurrentContext];
        [ctxinfo->context setView: nsView];
    }

    if (OGLC_IS_CAP_PRESENT(oglc, CAPS_EXT_FBOBJECT)) {
        // the GL_EXT_framebuffer_object extension is present, so we
        // must bind to the default (windowing system provided)
        // framebuffer
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }

JNI_COCOA_EXIT(env);

    return oglc;
}

/**
 * This function initializes a native window surface and caches the window
 * bounds in the given OGLSDOps.  Returns JNI_TRUE if the operation was
 * successful; JNI_FALSE otherwise.
 */
jboolean
OGLSD_InitOGLWindow(JNIEnv *env, OGLSDOps *oglsdo)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_InitOGLWindow");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_InitOGLWindow: ops are null");
        return JNI_FALSE;
    }

    CGLSDOps *cglsdo = (CGLSDOps *)oglsdo->privOps;
    if (cglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_InitOGLWindow: cgl ops are null");
        return JNI_FALSE;
    }

    AWTView *v = cglsdo->peerData;
    if (v == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "OGLSD_InitOGLWindow: view is invalid");
        return JNI_FALSE;
    }

JNI_COCOA_ENTER(env);
    NSRect surfaceBounds = [v bounds];
    oglsdo->drawableType = OGLSD_WINDOW;
    oglsdo->isOpaque = JNI_TRUE;
    oglsdo->width = surfaceBounds.size.width;
    oglsdo->height = surfaceBounds.size.height;
JNI_COCOA_EXIT(env);

    J2dTraceLn2(J2D_TRACE_VERBOSE, "  created window: w=%d h=%d", oglsdo->width, oglsdo->height);

    return JNI_TRUE;
}

void
OGLSD_SwapBuffers(JNIEnv *env, jlong pPeerData)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SwapBuffers");

JNI_COCOA_ENTER(env);
    [[NSOpenGLContext currentContext] flushBuffer];
JNI_COCOA_EXIT(env);
}

void
OGLSD_Flush(JNIEnv *env)
{
    OGLSDOps *dstOps = OGLRenderQueue_GetCurrentDestination();
    if (dstOps != NULL) {
        CGLSDOps *dstCGLOps = (CGLSDOps *)dstOps->privOps;
        CGLLayer *layer = (CGLLayer*)dstCGLOps->layer;
        if (layer != NULL) {
            [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
                AWT_ASSERT_APPKIT_THREAD;
                [layer setNeedsDisplay];
            }];
        }
    }
}

#pragma mark -
#pragma mark "--- CGLSurfaceData methods ---"

extern LockFunc        OGLSD_Lock;
extern GetRasInfoFunc  OGLSD_GetRasInfo;
extern UnlockFunc      OGLSD_Unlock;
extern DisposeFunc     OGLSD_Dispose;

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_CGLSurfaceData_initOps
    (JNIEnv *env, jobject cglsd, jobject gc,
     jlong pConfigInfo, jlong pPeerData, jlong layerPtr,
     jint xoff, jint yoff, jboolean isOpaque)
{
    J2dTraceLn(J2D_TRACE_INFO, "CGLSurfaceData_initOps");
    J2dTraceLn1(J2D_TRACE_INFO, "  pPeerData=%p", jlong_to_ptr(pPeerData));
    J2dTraceLn2(J2D_TRACE_INFO, "  xoff=%d, yoff=%d", (int)xoff, (int)yoff);

    gc = (*env)->NewGlobalRef(env, gc);
    if (gc == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }

    OGLSDOps *oglsdo = (OGLSDOps *)
        SurfaceData_InitOps(env, cglsd, sizeof(OGLSDOps));
    if (oglsdo == NULL) {
        (*env)->DeleteGlobalRef(env, gc);
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }
    // later the graphicsConfig will be used for deallocation of oglsdo
    oglsdo->graphicsConfig = gc;

    CGLSDOps *cglsdo = (CGLSDOps *)malloc(sizeof(CGLSDOps));
    if (cglsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "creating native cgl ops");
        return;
    }

    oglsdo->privOps = cglsdo;

    oglsdo->sdOps.Lock               = OGLSD_Lock;
    oglsdo->sdOps.GetRasInfo         = OGLSD_GetRasInfo;
    oglsdo->sdOps.Unlock             = OGLSD_Unlock;
    oglsdo->sdOps.Dispose            = OGLSD_Dispose;

    oglsdo->drawableType = OGLSD_UNDEFINED;
    oglsdo->activeBuffer = GL_FRONT;
    oglsdo->needsInit = JNI_TRUE;
    oglsdo->xOffset = xoff;
    oglsdo->yOffset = yoff;
    oglsdo->isOpaque = isOpaque;

    cglsdo->peerData = (AWTView *)jlong_to_ptr(pPeerData);
    cglsdo->layer = (CGLLayer *)jlong_to_ptr(layerPtr);
    cglsdo->configInfo = (CGLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);

    if (cglsdo->configInfo == NULL) {
        free(cglsdo);
        JNU_ThrowNullPointerException(env, "Config info is null in initOps");
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_CGLSurfaceData_clearWindow
(JNIEnv *env, jobject cglsd)
{
    J2dTraceLn(J2D_TRACE_INFO, "CGLSurfaceData_clearWindow");

    OGLSDOps *oglsdo = (OGLSDOps*) SurfaceData_GetOps(env, cglsd);
    CGLSDOps *cglsdo = (CGLSDOps*) oglsdo->privOps;

    cglsdo->peerData = NULL;
    cglsdo->layer = NULL;
}
