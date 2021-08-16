/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_opengl_GLXSurfaceData.h"

#include "OGLRenderQueue.h"
#include "GLXGraphicsConfig.h"
#include "GLXSurfaceData.h"
#include "awt_Component.h"
#include "awt_GraphicsEnv.h"

/**
 * The methods in this file implement the native windowing system specific
 * layer (GLX) for the OpenGL-based Java 2D pipeline.
 */

#ifndef HEADLESS

extern LockFunc       OGLSD_Lock;
extern GetRasInfoFunc OGLSD_GetRasInfo;
extern UnlockFunc     OGLSD_Unlock;
extern DisposeFunc    OGLSD_Dispose;

extern void
    OGLSD_SetNativeDimensions(JNIEnv *env, OGLSDOps *oglsdo, jint w, jint h);

jboolean surfaceCreationFailed = JNI_FALSE;

#endif /* !HEADLESS */

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_GLXSurfaceData_initOps(JNIEnv *env, jobject glxsd,
                                              jobject gc,
                                              jobject peer, jlong aData)
{
#ifndef HEADLESS
    gc = (*env)->NewGlobalRef(env, gc);
    if (gc == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }

    OGLSDOps *oglsdo = (OGLSDOps *)SurfaceData_InitOps(env, glxsd,
                                                       sizeof(OGLSDOps));
    if (oglsdo == NULL) {
        (*env)->DeleteGlobalRef(env, gc);
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }
    // later the graphicsConfig will be used for deallocation of oglsdo
    oglsdo->graphicsConfig = gc;

    GLXSDOps *glxsdo = (GLXSDOps *)malloc(sizeof(GLXSDOps));

    if (glxsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "creating native GLX ops");
        return;
    }

    J2dTraceLn(J2D_TRACE_INFO, "GLXSurfaceData_initOps");

    oglsdo->privOps = glxsdo;

    oglsdo->sdOps.Lock       = OGLSD_Lock;
    oglsdo->sdOps.GetRasInfo = OGLSD_GetRasInfo;
    oglsdo->sdOps.Unlock     = OGLSD_Unlock;
    oglsdo->sdOps.Dispose    = OGLSD_Dispose;

    oglsdo->drawableType = OGLSD_UNDEFINED;
    oglsdo->activeBuffer = GL_FRONT;
    oglsdo->needsInit = JNI_TRUE;

    if (peer != NULL) {
        glxsdo->window = JNU_CallMethodByName(env, NULL, peer,
                                              "getContentWindow", "()J").j;
    } else {
        glxsdo->window = 0;
    }
    glxsdo->configData = (AwtGraphicsConfigDataPtr)jlong_to_ptr(aData);
    if (glxsdo->configData == NULL) {
        free(glxsdo);
        JNU_ThrowNullPointerException(env,
                                 "Native GraphicsConfig data block missing");
        return;
    }

    if (glxsdo->configData->glxInfo == NULL) {
        free(glxsdo);
        JNU_ThrowNullPointerException(env, "GLXGraphicsConfigInfo missing");
        return;
    }
#endif /* HEADLESS */
}

#ifndef HEADLESS

/**
 * This function disposes of any native windowing system resources associated
 * with this surface.
 */
void
OGLSD_DestroyOGLSurface(JNIEnv *env, OGLSDOps *oglsdo)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_DestroyOGLSurface");
    // X Window is free'd later by AWT code...
}

/**
 * Makes the given context current to its associated "scratch" surface.  If
 * the operation is successful, this method will return JNI_TRUE; otherwise,
 * returns JNI_FALSE.
 */
static jboolean
GLXSD_MakeCurrentToScratch(JNIEnv *env, OGLContext *oglc)
{
    GLXCtxInfo *ctxInfo;

    J2dTraceLn(J2D_TRACE_INFO, "GLXSD_MakeCurrentToScratch");

    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXSD_MakeCurrentToScratch: context is null");
        return JNI_FALSE;
    }

    ctxInfo = (GLXCtxInfo *)oglc->ctxInfo;
    if (!j2d_glXMakeContextCurrent(awt_display,
                                   ctxInfo->scratchSurface,
                                   ctxInfo->scratchSurface,
                                   ctxInfo->context))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXSD_MakeCurrentToScratch: could not make current");
        return JNI_FALSE;
    }

    return JNI_TRUE;
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
    GLXGraphicsConfigInfo *glxInfo =
        (GLXGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);
    OGLContext *oglc;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SetScratchContext");

    if (glxInfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SetScratchContext: glx config info is null");
        return NULL;
    }

    oglc = glxInfo->context;
    if (!GLXSD_MakeCurrentToScratch(env, oglc)) {
        return NULL;
    }

    if (OGLC_IS_CAP_PRESENT(oglc, CAPS_EXT_FBOBJECT)) {
        // the GL_EXT_framebuffer_object extension is present, so this call
        // will ensure that we are bound to the scratch pbuffer (and not
        // some other framebuffer object)
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }

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
    GLXSDOps *dstGLXOps = (GLXSDOps *)dstOps->privOps;
    OGLContext *oglc;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_MakeOGLContextCurrent");

    oglc = dstGLXOps->configData->glxInfo->context;
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_MakeOGLContextCurrent: context is null");
        return NULL;
    }

    if (dstOps->drawableType == OGLSD_FBOBJECT) {
        OGLContext *currentContext = OGLRenderQueue_GetCurrentContext();

        // first make sure we have a current context (if the context isn't
        // already current to some drawable, we will make it current to
        // its scratch surface)
        if (oglc != currentContext) {
            if (!GLXSD_MakeCurrentToScratch(env, oglc)) {
                return NULL;
            }
        }

        // now bind to the fbobject associated with the destination surface;
        // this means that all rendering will go into the fbobject destination
        // (note that we unbind the currently bound texture first; this is
        // recommended procedure when binding an fbobject)
        j2d_glBindTexture(dstOps->textureTarget, 0);
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dstOps->fbobjectID);
    } else {
        GLXSDOps *srcGLXOps = (GLXSDOps *)srcOps->privOps;
        GLXCtxInfo *ctxinfo = (GLXCtxInfo *)oglc->ctxInfo;

        // make the context current
        if (!j2d_glXMakeContextCurrent(awt_display,
                                       dstGLXOps->drawable,
                                       srcGLXOps->drawable,
                                       ctxinfo->context))
        {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "OGLSD_MakeOGLContextCurrent: could not make current");
            return NULL;
        }

        if (OGLC_IS_CAP_PRESENT(oglc, CAPS_EXT_FBOBJECT)) {
            // the GL_EXT_framebuffer_object extension is present, so we
            // must bind to the default (windowing system provided)
            // framebuffer
            j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        }
    }

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
    GLXSDOps *glxsdo;
    Window window;
    XWindowAttributes attr;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_InitOGLWindow");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: ops are null");
        return JNI_FALSE;
    }

    glxsdo = (GLXSDOps *)oglsdo->privOps;
    if (glxsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: glx ops are null");
        return JNI_FALSE;
    }

    window = glxsdo->window;
    if (window == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: window is invalid");
        return JNI_FALSE;
    }

    XGetWindowAttributes(awt_display, window, &attr);
    oglsdo->width = attr.width;
    oglsdo->height = attr.height;

    oglsdo->drawableType = OGLSD_WINDOW;
    oglsdo->isOpaque = JNI_TRUE;
    oglsdo->xOffset = 0;
    oglsdo->yOffset = 0;
    glxsdo->drawable = window;
    glxsdo->xdrawable = window;

    J2dTraceLn2(J2D_TRACE_VERBOSE, "  created window: w=%d h=%d",
                oglsdo->width, oglsdo->height);

    return JNI_TRUE;
}

static int
GLXSD_BadAllocXErrHandler(Display *display, XErrorEvent *xerr)
{
    if (xerr->error_code == BadAlloc) {
        surfaceCreationFailed = JNI_TRUE;
    }
    return 0;
}

void
OGLSD_SwapBuffers(JNIEnv *env, jlong window)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SwapBuffers");

    if (window == 0L) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SwapBuffers: window is null");
        return;
    }

    j2d_glXSwapBuffers(awt_display, (Window)window);
}

// needed by Mac OS X port, no-op on other platforms
void
OGLSD_Flush(JNIEnv *env)
{
}

#endif /* !HEADLESS */
