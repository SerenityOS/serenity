/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>

#include "sun_java2d_opengl_WGLSurfaceData.h"

#include "jni.h"
#include "jlong.h"
#include "jni_util.h"
#include "sizecalc.h"
#include "OGLRenderQueue.h"
#include "WGLGraphicsConfig.h"
#include "WGLSurfaceData.h"

/**
 * The methods in this file implement the native windowing system specific
 * layer (WGL) for the OpenGL-based Java 2D pipeline.
 */

extern LockFunc                     OGLSD_Lock;
extern GetRasInfoFunc               OGLSD_GetRasInfo;
extern UnlockFunc                   OGLSD_Unlock;
extern DisposeFunc                  OGLSD_Dispose;

extern OGLPixelFormat PixelFormats[];
extern void AwtWindow_UpdateWindow(JNIEnv *env, jobject peer,
                                   jint w, jint h, HBITMAP hBitmap);
extern HBITMAP BitmapUtil_CreateBitmapFromARGBPre(int width, int height,
                                                  int srcStride,
                                                  int* imageData);
extern void AwtComponent_GetInsets(JNIEnv *env, jobject peer, RECT *insets);

extern void
    OGLSD_SetNativeDimensions(JNIEnv *env, OGLSDOps *oglsdo, jint w, jint h);

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_WGLSurfaceData_initOps(JNIEnv *env, jobject wglsd,
                                              jobject gc, jlong pConfigInfo,
                                              jobject peer, jlong hwnd)
{
    gc = (*env)->NewGlobalRef(env, gc);
    if (gc == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }

    OGLSDOps *oglsdo = (OGLSDOps *)SurfaceData_InitOps(env, wglsd,
                                                       sizeof(OGLSDOps));
    if (oglsdo == NULL) {
        (*env)->DeleteGlobalRef(env, gc);
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }
    // later the graphicsConfig will be used for deallocation of oglsdo
    oglsdo->graphicsConfig = gc;

    WGLSDOps *wglsdo = (WGLSDOps *)malloc(sizeof(WGLSDOps));

    J2dTraceLn(J2D_TRACE_INFO, "WGLSurfaceData_initOps");

    if (wglsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "creating native wgl ops");
        return;
    }
    if (oglsdo == NULL) {
        free(wglsdo);
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }

    oglsdo->privOps = wglsdo;

    oglsdo->sdOps.Lock               = OGLSD_Lock;
    oglsdo->sdOps.GetRasInfo         = OGLSD_GetRasInfo;
    oglsdo->sdOps.Unlock             = OGLSD_Unlock;
    oglsdo->sdOps.Dispose            = OGLSD_Dispose;

    oglsdo->drawableType = OGLSD_UNDEFINED;
    oglsdo->activeBuffer = GL_FRONT;
    oglsdo->needsInit = JNI_TRUE;
    if (peer != NULL) {
        RECT insets;
        AwtComponent_GetInsets(env, peer, &insets);
        oglsdo->xOffset = -insets.left;
        oglsdo->yOffset = -insets.bottom;
    } else {
        oglsdo->xOffset = 0;
        oglsdo->yOffset = 0;
    }

    wglsdo->window = (HWND)jlong_to_ptr(hwnd);
    wglsdo->configInfo = (WGLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);
    if (wglsdo->configInfo == NULL) {
        free(wglsdo);
        JNU_ThrowNullPointerException(env, "Config info is null in initOps");
    }
}

/**
 * This function disposes of any native windowing system resources associated
 * with this surface.
 */
void
OGLSD_DestroyOGLSurface(JNIEnv *env, OGLSDOps *oglsdo)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_DestroyOGLSurface");
    // Window is free'd later by AWT code...
}

/**
 * Makes the given context current to its associated "scratch" surface.  If
 * the operation is successful, this method will return JNI_TRUE; otherwise,
 * returns JNI_FALSE.
 */
static jboolean
WGLSD_MakeCurrentToScratch(JNIEnv *env, OGLContext *oglc)
{
    WGLCtxInfo *ctxInfo;

    J2dTraceLn(J2D_TRACE_INFO, "WGLSD_MakeCurrentToScratch");

    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "WGLSD_MakeCurrentToScratch: context is null");
        return JNI_FALSE;
    }

    ctxInfo = (WGLCtxInfo *)oglc->ctxInfo;
    if (!j2d_wglMakeCurrent(ctxInfo->scratchSurfaceDC, ctxInfo->context)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "WGLSD_MakeCurrentToScratch: could not make current");
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
    WGLGraphicsConfigInfo *wglInfo =
        (WGLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);
    OGLContext *oglc;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SetScratchContext");

    if (wglInfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SetScratchContext: wgl config info is null");
        return NULL;
    }

    oglc = wglInfo->context;
    if (!WGLSD_MakeCurrentToScratch(env, oglc)) {
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
    WGLSDOps *srcWGLOps = (WGLSDOps *)srcOps->privOps;
    WGLSDOps *dstWGLOps = (WGLSDOps *)dstOps->privOps;
    OGLContext *oglc;
    WGLCtxInfo *ctxinfo;
    HDC srcHDC, dstHDC;
    BOOL success;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_MakeOGLContextCurrent");

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  src: %d %p dst: %d %p",
                srcOps->drawableType, srcOps,
                dstOps->drawableType, dstOps);

    oglc = dstWGLOps->configInfo->context;
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
            if (!WGLSD_MakeCurrentToScratch(env, oglc)) {
                return NULL;
            }
        }

        // now bind to the fbobject associated with the destination surface;
        // this means that all rendering will go into the fbobject destination
        // (note that we unbind the currently bound texture first; this is
        // recommended procedure when binding an fbobject)
        j2d_glBindTexture(dstOps->textureTarget, 0);
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dstOps->fbobjectID);

        return oglc;
    }

    ctxinfo = (WGLCtxInfo *)oglc->ctxInfo;

    // get the hdc for the destination surface
    dstHDC = GetDC(dstWGLOps->window);

    // get the hdc for the source surface
    // the source will always be equal to the destination in this case
    srcHDC = dstHDC;

    // REMIND: in theory we should be able to use wglMakeContextCurrentARB()
    // even when the src/dst surfaces are the same, but this causes problems
    // on ATI's drivers (see 6525997); for now we will only use it when the
    // surfaces are different, otherwise we will use the old
    // wglMakeCurrent() approach...
    if (srcHDC != dstHDC) {
        // use WGL_ARB_make_current_read extension to make context current
        success =
            j2d_wglMakeContextCurrentARB(dstHDC, srcHDC, ctxinfo->context);
    } else {
        // use the old approach for making current to the destination
        success = j2d_wglMakeCurrent(dstHDC, ctxinfo->context);
    }
    if (!success) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_MakeOGLContextCurrent: could not make current");
        ReleaseDC(dstWGLOps->window, dstHDC);
        return NULL;
    }

    if (OGLC_IS_CAP_PRESENT(oglc, CAPS_EXT_FBOBJECT)) {
        // the GL_EXT_framebuffer_object extension is present, so we
        // must bind to the default (windowing system provided)
        // framebuffer
        j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    }

    ReleaseDC(dstWGLOps->window, dstHDC);

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
    PIXELFORMATDESCRIPTOR pfd;
    WGLSDOps *wglsdo;
    WGLGraphicsConfigInfo *wglInfo;
    HWND window;
    RECT wbounds;
    HDC hdc;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_InitOGLWindow");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: ops are null");
        return JNI_FALSE;
    }

    wglsdo = (WGLSDOps *)oglsdo->privOps;
    if (wglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: wgl ops are null");
        return JNI_FALSE;
    }

    wglInfo = wglsdo->configInfo;
    if (wglInfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: graphics config info is null");
        return JNI_FALSE;
    }

    window = wglsdo->window;
    if (!IsWindow(window)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: disposed component");
        return JNI_FALSE;
    }

    GetWindowRect(window, &wbounds);

    hdc = GetDC(window);
    if (hdc == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: invalid hdc");
        return JNI_FALSE;
    }

    if (!SetPixelFormat(hdc, wglInfo->pixfmt, &pfd)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitOGLWindow: error setting pixel format");
        ReleaseDC(window, hdc);
        return JNI_FALSE;
    }

    ReleaseDC(window, hdc);

    oglsdo->drawableType = OGLSD_WINDOW;
    oglsdo->isOpaque = JNI_TRUE;
    oglsdo->width = wbounds.right - wbounds.left;
    oglsdo->height = wbounds.bottom - wbounds.top;
    wglsdo->pbufferDC = 0;

    J2dTraceLn2(J2D_TRACE_VERBOSE, "  created window: w=%d h=%d",
                oglsdo->width, oglsdo->height);

    return JNI_TRUE;
}

void
OGLSD_SwapBuffers(JNIEnv *env, jlong pPeerData)
{
    HWND window;
    HDC hdc;

    J2dTraceLn(J2D_TRACE_INFO, "OGLSD_SwapBuffers");

    window = AwtComponent_GetHWnd(env, pPeerData);
    if (!IsWindow(window)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SwapBuffers: disposed component");
        return;
    }

    hdc = GetDC(window);
    if (hdc == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SwapBuffers: invalid hdc");
        return;
    }

    if (!SwapBuffers(hdc)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SwapBuffers: error in SwapBuffers");
    }

    if (!ReleaseDC(window, hdc)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_SwapBuffers: error while releasing dc");
    }
}

// needed by Mac OS X port, no-op on other platforms
void
OGLSD_Flush(JNIEnv *env)
{
}

/*
 * Class:     sun_java2d_opengl_WGLSurfaceData
 * Method:    updateWindowAccelImpl
 * Signature: (JJII)Z
 */
JNIEXPORT jboolean JNICALL
    Java_sun_java2d_opengl_WGLSurfaceData_updateWindowAccelImpl
  (JNIEnv *env, jclass clazz, jlong pData, jobject peer, jint w, jint h)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);
    OGLPixelFormat pf = PixelFormats[0/*PF_INT_ARGB_PRE*/];
    HBITMAP hBitmap = NULL;
    void *pDst;
    jint srcx, srcy, dstx, dsty, width, height;
    jint pixelStride = 4;
    jint scanStride = pixelStride * w;

    J2dTraceLn(J2D_TRACE_INFO, "WGLSurfaceData_updateWindowAccelImpl");

    if (w <= 0 || h <= 0) {
        return JNI_TRUE;
    }
    if (oglsdo == NULL) {
        return JNI_FALSE;
    }
    RESET_PREVIOUS_OP();

    width = w;
    height = h;
    srcx = srcy = dstx = dsty = 0;

    pDst = SAFE_SIZE_ARRAY_ALLOC(malloc, height, scanStride);
    if (pDst == NULL) {
        return JNI_FALSE;
    }
    ZeroMemory(pDst, height * scanStride);

    // the code below is mostly copied from OGLBlitLoops_SurfaceToSwBlit

    j2d_glPixelStorei(GL_PACK_SKIP_PIXELS, dstx);
    j2d_glPixelStorei(GL_PACK_ROW_LENGTH, scanStride / pixelStride);
    j2d_glPixelStorei(GL_PACK_ALIGNMENT, pf.alignment);

    // this accounts for lower-left origin of the source region
    srcx = oglsdo->xOffset + srcx;
    srcy = oglsdo->yOffset + oglsdo->height - (srcy + 1);
    // we must read one scanline at a time because there is no way
    // to read starting at the top-left corner of the source region
    while (height > 0) {
        j2d_glPixelStorei(GL_PACK_SKIP_ROWS, dsty);
        j2d_glReadPixels(srcx, srcy, width, 1,
                         pf.format, pf.type, pDst);
        srcy--;
        dsty++;
        height--;
    }

    j2d_glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
    j2d_glPixelStorei(GL_PACK_SKIP_ROWS, 0);
    j2d_glPixelStorei(GL_PACK_ROW_LENGTH, 0);
    j2d_glPixelStorei(GL_PACK_ALIGNMENT, 4);

    // the pixels read from the surface are already premultiplied
    hBitmap = BitmapUtil_CreateBitmapFromARGBPre(w, h, scanStride,
                                                 (int*)pDst);
    free(pDst);

    if (hBitmap == NULL) {
        return JNI_FALSE;
    }

    AwtWindow_UpdateWindow(env, peer, w, h, hBitmap);

    // hBitmap is released in UpdateWindow

    return JNI_TRUE;
}
