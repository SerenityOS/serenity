/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>

#include "sun_java2d_opengl_WGLGraphicsConfig.h"

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "WGLGraphicsConfig.h"
#include "WGLSurfaceData.h"

/**
 * This is a globally shared context used when creating textures.  When any
 * new contexts are created, they specify this context as the "share list"
 * context, which means any texture objects created when this shared context
 * is current will be available to any other context in any other thread.
 */
HGLRC sharedContext = 0;

/**
 * Attempts to initialize WGL and the core OpenGL library.  For this method
 * to return JNI_TRUE, the following must be true:
 *     - opengl32.dll must be loaded successfully (via LoadLibrary)
 *     - all core WGL/OGL function symbols from opengl32.dll must be
 *       available and loaded properly
 * If any of these requirements are not met, this method will return
 * JNI_FALSE, indicating there is no hope of using WGL/OpenGL for any
 * GraphicsConfig in the environment.
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_opengl_WGLGraphicsConfig_initWGL(JNIEnv *env, jclass wglgc)
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "WGLGraphicsConfig_initWGL");

    if (!OGLFuncs_OpenLibrary()) {
        return JNI_FALSE;
    }

    if (!OGLFuncs_InitPlatformFuncs() ||
        !OGLFuncs_InitBaseFuncs())
    {
        OGLFuncs_CloseLibrary();
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/**
 * Disposes all memory and resources allocated for the given OGLContext.
 */
static void
WGLGC_DestroyOGLContext(OGLContext *oglc)
{
    WGLCtxInfo *ctxinfo;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_DestroyOGLContext");

    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "WGLGC_DestroyOGLContext: context is null");
        return;
    }

    // at this point, this context will be current to its scratch surface,
    // so the following operations should be safe...

    OGLContext_DestroyContextResources(oglc);

    ctxinfo = (WGLCtxInfo *)oglc->ctxInfo;
    if (ctxinfo != NULL) {
        // release the current context before we continue
        j2d_wglMakeCurrent(NULL, NULL);

        if (ctxinfo->context != 0) {
            j2d_wglDeleteContext(ctxinfo->context);
        }
        if (ctxinfo->scratchSurface != 0) {
            if (ctxinfo->scratchSurfaceDC != 0) {
                j2d_wglReleasePbufferDCARB(ctxinfo->scratchSurface,
                                           ctxinfo->scratchSurfaceDC);
            }
            j2d_wglDestroyPbufferARB(ctxinfo->scratchSurface);
        }

        free(ctxinfo);
    }

    free(oglc);
}

/**
 * Disposes all memory and resources associated with the given
 * WGLGraphicsConfigInfo (including its native OGLContext data).
 */
void
OGLGC_DestroyOGLGraphicsConfig(jlong pConfigInfo)
{
    WGLGraphicsConfigInfo *wglinfo =
        (WGLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);

    J2dTraceLn(J2D_TRACE_INFO, "OGLGC_DestroyOGLGraphicsConfig");

    if (wglinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLGC_DestroyOGLGraphicsConfig: info is null");
        return;
    }

    if (wglinfo->context != NULL) {
        WGLGC_DestroyOGLContext(wglinfo->context);
    }

    free(wglinfo);
}

/**
 * Creates a temporary (non-visible) window that can be used for querying
 * the OpenGL capabilities of a given device.
 *
 * REMIND: should be able to create a window on a specific device...
 */
HWND
WGLGC_CreateScratchWindow(jint screennum)
{
    static jboolean firsttime = JNI_TRUE;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_CreateScratchWindow");

    if (firsttime) {
        WNDCLASS wc;

        // setup window class information
        ZeroMemory(&wc, sizeof(WNDCLASS));
        wc.hInstance = GetModuleHandle(NULL);
        wc.lpfnWndProc = DefWindowProc;
        wc.lpszClassName = L"Tmp";
        if (RegisterClass(&wc) == 0) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "WGLGC_CreateScratchWindow: error registering window class");
            return 0;
        }

        firsttime = JNI_FALSE;
    }

    // create scratch window
    return CreateWindow(L"Tmp", L"Tmp", 0,
                        CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
                        CW_USEDEFAULT, NULL, NULL,
                        GetModuleHandle(NULL), NULL);
}

/**
 * Returns a pixel format identifier that is suitable for Java 2D's needs
 * (must have a depth buffer, support for pbuffers, etc).  This method will
 * iterate through all pixel formats (if any) that match the requested
 * attributes and will attempt to find a pixel format with a minimal combined
 * depth+stencil buffer.  Note that we currently only need depth capabilities
 * (for shape clipping purposes), but wglChoosePixelFormatARB() will often
 * return a list of pixel formats with the largest depth buffer (and stencil)
 * sizes at the top of the list.  Therefore, we scan through the whole list
 * to find the most VRAM-efficient pixel format.  If no appropriate pixel
 * format can be found, this method returns 0.
 */
static int
WGLGC_GetPixelFormatForDC(HDC hdc)
{
    int attrs[] = {
        WGL_PIXEL_TYPE_ARB, WGL_TYPE_RGBA_ARB,
        WGL_DRAW_TO_WINDOW_ARB, GL_TRUE,
        WGL_DRAW_TO_PBUFFER_ARB, GL_TRUE,
        WGL_DOUBLE_BUFFER_ARB, GL_TRUE,
        WGL_DEPTH_BITS_ARB, 16, // anything >= 16 will work for us
        0
    };
    int pixfmts[32];
    int chosenPixFmt = 0;
    int nfmts, i;

    // this is the initial minimum value for the combined depth+stencil size
    // (we initialize it to some absurdly high value; realistic values will
    // be much less than this number)
    int minDepthPlusStencil = 512;

    J2dRlsTraceLn(J2D_TRACE_INFO, "WGLGC_GetPixelFormatForDC");

    // find all pixel formats (maximum of 32) with the provided attributes
    if (!j2d_wglChoosePixelFormatARB(hdc, attrs, NULL, 32, pixfmts, &nfmts)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_GetPixelFormatForDC: error choosing pixel format");
        return 0;
    }

    if (nfmts <= 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_GetPixelFormatForDC: no pixel formats found");
        return 0;
    }

    J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  candidate pixel formats:");

    // iterate through the list of pixel formats, looking for the one that
    // meets our requirements while keeping the combined depth+stencil sizes
    // to a minimum
    for (i = 0; i < nfmts; i++) {
        int attrKeys[] = {
            WGL_DEPTH_BITS_ARB, WGL_STENCIL_BITS_ARB,
            WGL_DOUBLE_BUFFER_ARB, WGL_ALPHA_BITS_ARB
        };
        int attrVals[4];
        int pixfmt = pixfmts[i];
        int depth, stencil, db, alpha;

        j2d_wglGetPixelFormatAttribivARB(hdc, pixfmt, 0, 4,
                                         attrKeys, attrVals);

        depth   = attrVals[0];
        stencil = attrVals[1];
        db      = attrVals[2];
        alpha   = attrVals[3];

        J2dRlsTrace5(J2D_TRACE_VERBOSE,
            "[V]     pixfmt=%d db=%d alpha=%d depth=%d stencil=%d valid=",
                     pixfmt, db, alpha, depth, stencil);

        if ((depth + stencil) < minDepthPlusStencil) {
            J2dRlsTrace(J2D_TRACE_VERBOSE, "true\n");
            minDepthPlusStencil = depth + stencil;
            chosenPixFmt = pixfmt;
        } else {
            J2dRlsTrace(J2D_TRACE_VERBOSE, "false (large depth)\n");
        }
    }

    if (chosenPixFmt == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_GetPixelFormatForDC: could not find appropriate pixfmt");
        return 0;
    }

    J2dRlsTraceLn1(J2D_TRACE_INFO,
        "WGLGC_GetPixelFormatForDC: chose %d as the best pixel format",
                   chosenPixFmt);

    return chosenPixFmt;
}

/**
 * Sets a "basic" pixel format for the given HDC.  This method is used only
 * for initializing a scratch window far enough such that we can load
 * GL/WGL extension function pointers using wglGetProcAddress.  (This method
 * differs from the one above in that it does not use wglChoosePixelFormatARB,
 * which is a WGL extension function, since we can't use that method without
 * first loading the extension functions under a "basic" pixel format.)
 */
static jboolean
WGLGC_SetBasicPixelFormatForDC(HDC hdc)
{
    PIXELFORMATDESCRIPTOR pfd;
    int pixfmt;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_SetBasicPixelFormatForDC");

    // find pixel format
    ZeroMemory(&pfd, sizeof(PIXELFORMATDESCRIPTOR));
    pfd.nSize = sizeof(PIXELFORMATDESCRIPTOR);
    pfd.nVersion = 1;
    pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pixfmt = ChoosePixelFormat(hdc, &pfd);

    if (!SetPixelFormat(hdc, pixfmt, &pfd)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_SetBasicPixelFormatForDC: error setting pixel format");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/**
 * Creates a context that is compatible with the given pixel format
 * identifier.  Returns 0 if the context could not be created properly.
 */
static HGLRC
WGLGC_CreateContext(jint screennum, jint pixfmt)
{
    PIXELFORMATDESCRIPTOR pfd;
    HWND hwnd;
    HDC hdc;
    HGLRC hglrc;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_CreateContext");

    hwnd = WGLGC_CreateScratchWindow(screennum);
    if (hwnd == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_CreateContext: could not create scratch window");
        return 0;
    }

    // get the HDC for the scratch window
    hdc = GetDC(hwnd);
    if (hdc == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_CreateContext: could not get dc for scratch window");
        DestroyWindow(hwnd);
        return 0;
    }

    // set the pixel format for the scratch window
    if (!SetPixelFormat(hdc, pixfmt, &pfd)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_CreateContext: error setting pixel format");
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0;
    }

    // create a context based on the scratch window
    hglrc = j2d_wglCreateContext(hdc);

    // release the temporary resources
    ReleaseDC(hwnd, hdc);
    DestroyWindow(hwnd);

    return hglrc;
}

/**
 * Initializes the extension function pointers for the given device.  Note
 * that under WGL, extension functions have different entrypoints depending
 * on the device, so we must first make a context current for the given
 * device before attempting to load the function pointers via
 * wglGetProcAddress.
 *
 * REMIND: ideally the extension function pointers would not be global, but
 *         rather would be stored in a structure associated with the
 *         WGLGraphicsConfig, so that we use the correct function entrypoint
 *         depending on the destination device...
 */
static jboolean
WGLGC_InitExtFuncs(jint screennum)
{
    HWND hwnd;
    HDC hdc;
    HGLRC context;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_InitExtFuncs");

    // create a scratch window
    hwnd = WGLGC_CreateScratchWindow(screennum);
    if (hwnd == 0) {
        return JNI_FALSE;
    }

    // get the HDC for the scratch window
    hdc = GetDC(hwnd);
    if (hdc == 0) {
        DestroyWindow(hwnd);
        return JNI_FALSE;
    }

    // find and set a basic pixel format for the scratch window
    if (!WGLGC_SetBasicPixelFormatForDC(hdc)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitExtFuncs: could not find appropriate pixfmt");
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return JNI_FALSE;
    }

    // create a temporary context
    context = j2d_wglCreateContext(hdc);
    if (context == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitExtFuncs: could not create temp WGL context");
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return JNI_FALSE;
    }

    // make the context current so that we can load the function pointers
    // using wglGetProcAddress
    if (!j2d_wglMakeCurrent(hdc, context)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitExtFuncs: could not make temp context current");
        j2d_wglDeleteContext(context);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return JNI_FALSE;
    }

    if (!OGLFuncs_InitExtFuncs()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitExtFuncs: could not initialize extension funcs");
        j2d_wglMakeCurrent(NULL, NULL);
        j2d_wglDeleteContext(context);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return JNI_FALSE;
    }

    // destroy the temporary resources
    j2d_wglMakeCurrent(NULL, NULL);
    j2d_wglDeleteContext(context);
    ReleaseDC(hwnd, hdc);
    DestroyWindow(hwnd);

    return JNI_TRUE;
}

/**
 * Initializes a new OGLContext, which includes the native WGL context handle
 * and some other important information such as the associated pixel format.
 */
static OGLContext *
WGLGC_InitOGLContext(jint pixfmt, HGLRC context,
                     HPBUFFERARB scratch, HDC scratchDC, jint caps)
{
    OGLContext *oglc;
    WGLCtxInfo *ctxinfo;

    J2dTraceLn(J2D_TRACE_INFO, "WGLGC_InitOGLContext");

    oglc = (OGLContext *)malloc(sizeof(OGLContext));
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitOGLContext: could not allocate memory for oglc");
        return NULL;
    }

    memset(oglc, 0, sizeof(OGLContext));

    ctxinfo = (WGLCtxInfo *)malloc(sizeof(WGLCtxInfo));
    if (ctxinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGC_InitOGLContext: could not allocate memory for ctxinfo");
        free(oglc);
        return NULL;
    }

    ctxinfo->context = context;
    ctxinfo->scratchSurface = scratch;
    ctxinfo->scratchSurfaceDC = scratchDC;
    oglc->ctxInfo = ctxinfo;
    oglc->caps = caps;

    return oglc;
}

/**
 * Determines whether the WGL pipeline can be used for a given GraphicsConfig
 * provided its screen number and visual ID.  If the minimum requirements are
 * met, the native WGLGraphicsConfigInfo structure is initialized for this
 * GraphicsConfig with the necessary information (pixel format, etc.)
 * and a pointer to this structure is returned as a jlong.  If
 * initialization fails at any point, zero is returned, indicating that WGL
 * cannot be used for this GraphicsConfig (we should fallback on the existing
 * DX pipeline).
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_opengl_WGLGraphicsConfig_getWGLConfigInfo(JNIEnv *env,
                                                          jclass wglgc,
                                                          jint screennum,
                                                          jint pixfmt)
{
    OGLContext *oglc;
    PIXELFORMATDESCRIPTOR pfd;
    HWND hwnd;
    HDC hdc;
    HGLRC context;
    HPBUFFERARB scratch;
    HDC scratchDC;
    WGLGraphicsConfigInfo *wglinfo;
    const unsigned char *versionstr;
    const char *extstr;
    jint caps = CAPS_EMPTY;
    int attrKeys[] = { WGL_DOUBLE_BUFFER_ARB};
    int attrVals[1];

    J2dRlsTraceLn(J2D_TRACE_INFO, "WGLGraphicsConfig_getWGLConfigInfo");

    // initialize GL/WGL extension functions
    if (!WGLGC_InitExtFuncs(screennum)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not init ext funcs");
        return 0L;
    }

    // create a scratch window
    hwnd = WGLGC_CreateScratchWindow(screennum);
    if (hwnd == 0) {
        return 0L;
    }

    // get the HDC for the scratch window
    hdc = GetDC(hwnd);
    if (hdc == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not get dc for scratch window");
        DestroyWindow(hwnd);
        return 0L;
    }

    if (pixfmt == 0) {
        // find an appropriate pixel format
        pixfmt = WGLGC_GetPixelFormatForDC(hdc);
        if (pixfmt == 0) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "WGLGraphicsConfig_getWGLConfigInfo: could not find appropriate pixfmt");
            ReleaseDC(hwnd, hdc);
            DestroyWindow(hwnd);
            return 0L;
        }
    }

    if (sharedContext == 0) {
        // create the one shared context
        sharedContext = WGLGC_CreateContext(screennum, pixfmt);
        if (sharedContext == 0) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "WGLGraphicsConfig_getWGLConfigInfo: could not create shared context");
            ReleaseDC(hwnd, hdc);
            DestroyWindow(hwnd);
            return 0L;
        }
    }

    // set the pixel format for the scratch window
    if (!SetPixelFormat(hdc, pixfmt, &pfd)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsconfig_getWGLConfigInfo: error setting pixel format");
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0L;
    }

    // create the HGLRC (context) for this WGLGraphicsConfig
    context = j2d_wglCreateContext(hdc);
    if (context == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not create WGL context");
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0L;
    }

    // REMIND: when using wglShareLists, the two contexts must use an
    //         identical pixel format...
    if (!j2d_wglShareLists(sharedContext, context)) {
        J2dRlsTraceLn(J2D_TRACE_WARNING,
            "WGLGraphicsConfig_getWGLConfigInfo: unable to share lists");
    }

    // make the context current so that we can query the OpenGL version
    // and extension strings
    if (!j2d_wglMakeCurrent(hdc, context)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not make temp context current");
        j2d_wglDeleteContext(context);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0L;
    }

    // get version and extension strings
    versionstr = j2d_glGetString(GL_VERSION);
    extstr = j2d_wglGetExtensionsStringARB(hdc);
    OGLContext_GetExtensionInfo(env, &caps);

    J2dRlsTraceLn1(J2D_TRACE_INFO,
        "WGLGraphicsConfig_getWGLConfigInfo: OpenGL version=%s",
                   (versionstr == NULL) ? "null" : (char *)versionstr);

    if (!OGLContext_IsVersionSupported(versionstr)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: OpenGL 1.2 is required");
        j2d_wglMakeCurrent(NULL, NULL);
        j2d_wglDeleteContext(context);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0L;
    }

    // check for required WGL extensions
    if (!OGLContext_IsExtensionAvailable(extstr, "WGL_ARB_pbuffer") ||
        !OGLContext_IsExtensionAvailable(extstr, "WGL_ARB_make_current_read")||
        !OGLContext_IsExtensionAvailable(extstr, "WGL_ARB_pixel_format"))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: required ext(s) unavailable");
        j2d_wglMakeCurrent(NULL, NULL);
        j2d_wglDeleteContext(context);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
        return 0L;
    }

    // get config-specific capabilities
    j2d_wglGetPixelFormatAttribivARB(hdc, pixfmt, 0, 1, attrKeys, attrVals);
    if (attrVals[0]) {
        caps |= CAPS_DOUBLEBUFFERED;
    }

    // create the scratch pbuffer
    scratch = j2d_wglCreatePbufferARB(hdc, pixfmt, 1, 1, NULL);

    // destroy the temporary resources
    j2d_wglMakeCurrent(NULL, NULL);
    ReleaseDC(hwnd, hdc);
    DestroyWindow(hwnd);

    if (scratch == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not create scratch surface");
        j2d_wglDeleteContext(context);
        return 0L;
    }

    // get the HDC for the scratch pbuffer
    scratchDC = j2d_wglGetPbufferDCARB(scratch);
    if (scratchDC == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not get hdc for scratch surface");
        j2d_wglDeleteContext(context);
        j2d_wglDestroyPbufferARB(scratch);
        return 0L;
    }

    // initialize the OGLContext, which wraps the pixfmt and HGLRC (context)
    oglc = WGLGC_InitOGLContext(pixfmt, context, scratch, scratchDC, caps);
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not create oglc");
        j2d_wglDeleteContext(context);
        j2d_wglReleasePbufferDCARB(scratch, scratchDC);
        j2d_wglDestroyPbufferARB(scratch);
        return 0L;
    }

    J2dTraceLn(J2D_TRACE_VERBOSE,
        "WGLGraphicsConfig_getWGLConfigInfo: finished checking dependencies");

    // create the WGLGraphicsConfigInfo record for this config
    wglinfo = (WGLGraphicsConfigInfo *)malloc(sizeof(WGLGraphicsConfigInfo));
    if (wglinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "WGLGraphicsConfig_getWGLConfigInfo: could not allocate memory for wglinfo");
        WGLGC_DestroyOGLContext(oglc);
        return 0L;
    }

    wglinfo->screen = screennum;
    wglinfo->pixfmt = pixfmt;
    wglinfo->context = oglc;

    return ptr_to_jlong(wglinfo);
}

JNIEXPORT jint JNICALL
Java_sun_java2d_opengl_WGLGraphicsConfig_getDefaultPixFmt(JNIEnv *env,
                                                          jclass wglgc,
                                                          jint screennum)
{
    J2dTraceLn(J2D_TRACE_INFO, "WGLGraphicsConfig_getDefaultPixFmt");

    // REMIND: eventually we should implement this method so that it finds
    //         the most appropriate default pixel format for the given
    //         device; for now, we'll just return 0, and then we'll find
    //         an appropriate pixel format in WGLGC_GetWGLConfigInfo()...
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_java2d_opengl_WGLGraphicsConfig_getOGLCapabilities(JNIEnv *env,
                                                            jclass wglgc,
                                                            jlong configInfo)
{
    WGLGraphicsConfigInfo *wglinfo =
        (WGLGraphicsConfigInfo *)jlong_to_ptr(configInfo);

    J2dTraceLn(J2D_TRACE_INFO, "WGLGraphicsConfig_getOGLCapabilities");

    if (wglinfo == NULL || wglinfo->context == NULL) {
        return CAPS_EMPTY;
    }

    return wglinfo->context->caps;
}
