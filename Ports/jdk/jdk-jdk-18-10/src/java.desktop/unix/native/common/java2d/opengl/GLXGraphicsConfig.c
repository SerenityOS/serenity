/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_opengl_GLXGraphicsConfig.h"

#include "jni.h"
#include "jlong.h"
#include "GLXGraphicsConfig.h"
#include "GLXSurfaceData.h"
#include "awt_GraphicsEnv.h"
#include "awt_util.h"

#ifndef HEADLESS

extern Bool usingXinerama;

/**
 * This is a globally shared context used when creating textures.  When any
 * new contexts are created, they specify this context as the "share list"
 * context, which means any texture objects created when this shared context
 * is current will be available to any other context.
 */
static GLXContext sharedContext = 0;

/**
 * Attempts to initialize GLX and the core OpenGL library.  For this method
 * to return JNI_TRUE, the following must be true:
 *     - libGL must be loaded successfully (via dlopen)
 *     - all function symbols from libGL must be available and loaded properly
 *     - the GLX extension must be available through X11
 *     - client GLX version must be >= 1.3
 * If any of these requirements are not met, this method will return
 * JNI_FALSE, indicating there is no hope of using GLX/OpenGL for any
 * GraphicsConfig in the environment.
 */
static jboolean
GLXGC_InitGLX()
{
    int errorbase, eventbase;
    const char *version;

    J2dRlsTraceLn(J2D_TRACE_INFO, "GLXGC_InitGLX");

    if (!OGLFuncs_OpenLibrary()) {
        return JNI_FALSE;
    }

    if (!OGLFuncs_InitPlatformFuncs() ||
        !OGLFuncs_InitBaseFuncs() ||
        !OGLFuncs_InitExtFuncs())
    {
        OGLFuncs_CloseLibrary();
        return JNI_FALSE;
    }

    if (!j2d_glXQueryExtension(awt_display, &errorbase, &eventbase)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXGC_InitGLX: GLX extension is not present");
        OGLFuncs_CloseLibrary();
        return JNI_FALSE;
    }

    version = j2d_glXGetClientString(awt_display, GLX_VERSION);
    if (version == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXGC_InitGLX: could not query GLX version");
        OGLFuncs_CloseLibrary();
        return JNI_FALSE;
    }

    // we now only verify that the client GLX version is >= 1.3 (if the
    // server does not support GLX 1.3, then we will find that out later
    // when we attempt to create a GLXFBConfig)
    J2dRlsTraceLn1(J2D_TRACE_INFO,
                   "GLXGC_InitGLX: client GLX version=%s", version);
    if (!((version[0] == '1' && version[2] >= '3') || (version[0] > '1'))) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXGC_InitGLX: invalid GLX version; 1.3 is required");
        OGLFuncs_CloseLibrary();
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/**
 * Returns JNI_TRUE if GLX is available for the current display.  Note that
 * this method will attempt to initialize GLX (and all the necessary function
 * symbols) if it has not been already.  The AWT_LOCK must be acquired before
 * calling this method.
 */
jboolean
GLXGC_IsGLXAvailable()
{
    static jboolean glxAvailable = JNI_FALSE;
    static jboolean firstTime = JNI_TRUE;

    J2dTraceLn(J2D_TRACE_INFO, "GLXGC_IsGLXAvailable");

    if (firstTime) {
        glxAvailable = GLXGC_InitGLX();
        firstTime = JNI_FALSE;
    }

    return glxAvailable;
}

/**
 * Disposes all memory and resources allocated for the given OGLContext.
 */
static void
GLXGC_DestroyOGLContext(OGLContext *oglc)
{
    GLXCtxInfo *ctxinfo;

    J2dTraceLn(J2D_TRACE_INFO, "GLXGC_DestroyOGLContext");

    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "GLXGC_DestroyOGLContext: context is null");
        return;
    }

    // at this point, this context will be current to its scratch surface
    // so the following GL/GLX operations should be safe...

    OGLContext_DestroyContextResources(oglc);

    ctxinfo = (GLXCtxInfo *)oglc->ctxInfo;
    if (ctxinfo != NULL) {
        // release the current context before we continue
        j2d_glXMakeContextCurrent(awt_display, None, None, NULL);

        if (ctxinfo->context != 0) {
            j2d_glXDestroyContext(awt_display, ctxinfo->context);
        }
        if (ctxinfo->scratchSurface != 0) {
            j2d_glXDestroyPbuffer(awt_display, ctxinfo->scratchSurface);
        }

        free(ctxinfo);
    }

    free(oglc);
}

/**
 * Disposes all memory and resources associated with the given
 * GLXGraphicsConfigInfo (including its native OGLContext data).
 */
void
OGLGC_DestroyOGLGraphicsConfig(jlong pConfigInfo)
{
    GLXGraphicsConfigInfo *glxinfo =
        (GLXGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);

    J2dTraceLn(J2D_TRACE_INFO, "OGLGC_DestroyOGLGraphicsConfig");

    if (glxinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLGC_DestroyOGLGraphicsConfig: info is null");
        return;
    }

    if (glxinfo->context != NULL) {
        GLXGC_DestroyOGLContext(glxinfo->context);
    }

    free(glxinfo);
}

/**
 * Attempts to create a new GLXFBConfig for the requested screen and visual.
 * If visualid is 0, this method will iterate through all GLXFBConfigs (if
 * any) that match the requested attributes and will attempt to find an
 * fbconfig with a minimal combined depth+stencil buffer.  Note that we
 * currently only need depth capabilities (for shape clipping purposes), but
 * glXChooseFBConfig() will often return a list of fbconfigs with the largest
 * depth buffer (and stencil) sizes at the top of the list.  Therefore, we
 * scan through the whole list to find the most VRAM-efficient fbconfig.
 * If visualid is non-zero, the GLXFBConfig associated with the given visual
 * is chosen (assuming it meets the requested attributes).  If there are no
 * valid GLXFBConfigs available, this method returns 0.
 */
static GLXFBConfig
GLXGC_InitFBConfig(JNIEnv *env, jint screennum, VisualID visualid)
{
    GLXFBConfig *fbconfigs;
    GLXFBConfig chosenConfig = 0;
    int nconfs, i;
    int attrlist[] = {GLX_DRAWABLE_TYPE, GLX_WINDOW_BIT | GLX_PBUFFER_BIT,
                      GLX_RENDER_TYPE, GLX_RGBA_BIT,
                      GLX_CONFIG_CAVEAT, GLX_NONE, // avoid "slow" configs
                      GLX_DEPTH_SIZE, 16, // anything >= 16 will work for us
                      0};

    // this is the initial minimum value for the combined depth+stencil size
    // (we initialize it to some absurdly high value; realistic values will
    // be much less than this number)
    int minDepthPlusStencil = 512;

    J2dRlsTraceLn2(J2D_TRACE_INFO, "GLXGC_InitFBConfig: scn=%d vis=0x%x",
                   screennum, visualid);

    // find all fbconfigs for this screen with the provided attributes
    fbconfigs = j2d_glXChooseFBConfig(awt_display, screennum,
                                      attrlist, &nconfs);

    if ((fbconfigs == NULL) || (nconfs <= 0)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_InitFBConfig: could not find any valid fbconfigs");
        return 0;
    }

    J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  candidate fbconfigs:");

    // iterate through the list of fbconfigs, looking for the one that matches
    // the requested VisualID and supports RGBA rendering as well as the
    // creation of windows and pbuffers
    for (i = 0; i < nconfs; i++) {
        XVisualInfo *xvi;
        VisualID fbvisualid;
        GLXFBConfig fbc = fbconfigs[i];

        // get VisualID from GLXFBConfig
        xvi = j2d_glXGetVisualFromFBConfig(awt_display, fbc);
        if (xvi == NULL) {
            continue;
        }
        fbvisualid = xvi->visualid;
        XFree(xvi);

        if (visualid == 0 || visualid == fbvisualid) {
            int dtype, rtype, depth, stencil, db, alpha, gamma;

            // get GLX-specific attributes from GLXFBConfig
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_DRAWABLE_TYPE, &dtype);
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_RENDER_TYPE, &rtype);
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_DEPTH_SIZE, &depth);
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_STENCIL_SIZE, &stencil);

            // these attributes don't affect our decision, but they are
            // interesting for trace logs, so we will query them anyway
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_DOUBLEBUFFER, &db);
            j2d_glXGetFBConfigAttrib(awt_display, fbc,
                                     GLX_ALPHA_SIZE, &alpha);

            J2dRlsTrace5(J2D_TRACE_VERBOSE,
                "[V]     id=0x%x db=%d alpha=%d depth=%d stencil=%d valid=",
                         fbvisualid, db, alpha, depth, stencil);

            if ((dtype & GLX_WINDOW_BIT) &&
                (dtype & GLX_PBUFFER_BIT) &&
                (rtype & GLX_RGBA_BIT) &&
                (depth >= 16))
            {
                if (visualid == 0) {
                    // when visualid == 0, we loop through all configs
                    // looking for an fbconfig that has the smallest combined
                    // depth+stencil size (this keeps VRAM usage to a minimum)
                    if ((depth + stencil) < minDepthPlusStencil) {
                        J2dRlsTrace(J2D_TRACE_VERBOSE, "true\n");
                        minDepthPlusStencil = depth + stencil;
                        chosenConfig = fbc;
                    } else {
                        J2dRlsTrace(J2D_TRACE_VERBOSE,
                                    "false (large depth)\n");
                    }
                    continue;
                } else {
                    // in this case, visualid == fbvisualid, which means
                    // we've found a valid fbconfig corresponding to the
                    // requested VisualID, so break out of the loop
                    J2dRlsTrace(J2D_TRACE_VERBOSE, "true\n");
                    chosenConfig = fbc;
                    break;
                }
            } else {
                J2dRlsTrace(J2D_TRACE_VERBOSE, "false (bad match)\n");
            }
        }
    }

    // free the list of fbconfigs
    XFree(fbconfigs);

    if (chosenConfig == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_InitFBConfig: could not find an appropriate fbconfig");
        return 0;
    }

    return chosenConfig;
}

/**
 * Returns the X11 VisualID that corresponds to the best GLXFBConfig for the
 * given screen.  If no valid visual could be found, this method returns zero.
 * Note that this method will attempt to initialize GLX (and all the
 * necessary function symbols) if it has not been already.  The AWT_LOCK
 * must be acquired before calling this method.
 */
VisualID
GLXGC_FindBestVisual(JNIEnv *env, jint screen)
{
    GLXFBConfig fbc;
    XVisualInfo *xvi;
    VisualID visualid;

    J2dRlsTraceLn1(J2D_TRACE_INFO, "GLXGC_FindBestVisual: scn=%d", screen);

    if (!GLXGC_IsGLXAvailable()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_FindBestVisual: could not initialize GLX");
        return 0;
    }

    fbc = GLXGC_InitFBConfig(env, screen, 0);
    if (fbc == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_FindBestVisual: could not find best visual");
        return 0;
    }

    xvi = j2d_glXGetVisualFromFBConfig(awt_display, fbc);
    if (xvi == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_FindBestVisual: could not get visual for fbconfig");
        return 0;
    }

    visualid = xvi->visualid;
    XFree(xvi);

    J2dRlsTraceLn2(J2D_TRACE_INFO,
        "GLXGC_FindBestVisual: chose 0x%x as the best visual for screen %d",
                   visualid, screen);

    return visualid;
}

/**
 * Creates a scratch pbuffer, which can be used to make a context current
 * for extension queries, etc.
 */
static GLXPbuffer
GLXGC_InitScratchPbuffer(GLXFBConfig fbconfig)
{
    int pbattrlist[] = {GLX_PBUFFER_WIDTH, 4,
                        GLX_PBUFFER_HEIGHT, 4,
                        GLX_PRESERVED_CONTENTS, GL_FALSE,
                        0};

    J2dTraceLn(J2D_TRACE_INFO, "GLXGC_InitScratchPbuffer");

    return j2d_glXCreatePbuffer(awt_display, fbconfig, pbattrlist);
}

/**
 * Initializes a new OGLContext, which includes the native GLXContext handle
 * and some other important information such as the associated GLXFBConfig.
 */
static OGLContext *
GLXGC_InitOGLContext(GLXFBConfig fbconfig, GLXContext context,
                     GLXPbuffer scratch, jint caps)
{
    OGLContext *oglc;
    GLXCtxInfo *ctxinfo;

    J2dTraceLn(J2D_TRACE_INFO, "GLXGC_InitOGLContext");

    oglc = (OGLContext *)malloc(sizeof(OGLContext));
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_InitOGLContext: could not allocate memory for oglc");
        return NULL;
    }

    memset(oglc, 0, sizeof(OGLContext));

    ctxinfo = (GLXCtxInfo *)malloc(sizeof(GLXCtxInfo));
    if (ctxinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGC_InitOGLContext: could not allocate memory for ctxinfo");
        free(oglc);
        return NULL;
    }

    ctxinfo->fbconfig = fbconfig;
    ctxinfo->context = context;
    ctxinfo->scratchSurface = scratch;
    oglc->ctxInfo = ctxinfo;
    oglc->caps = caps;

    return oglc;
}

#endif /* !HEADLESS */

/**
 * Determines whether the GLX pipeline can be used for a given GraphicsConfig
 * provided its screen number and visual ID.  If the minimum requirements are
 * met, the native GLXGraphicsConfigInfo structure is initialized for this
 * GraphicsConfig with the necessary information (GLXFBConfig, etc.)
 * and a pointer to this structure is returned as a jlong.  If
 * initialization fails at any point, zero is returned, indicating that GLX
 * cannot be used for this GraphicsConfig (we should fallback on the existing
 * X11 pipeline).
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_opengl_GLXGraphicsConfig_getGLXConfigInfo(JNIEnv *env,
                                                          jclass glxgc,
                                                          jint screennum,
                                                          jint visnum)
{
#ifndef HEADLESS
    OGLContext *oglc;
    GLXFBConfig fbconfig;
    GLXContext context;
    GLXPbuffer scratch;
    GLXGraphicsConfigInfo *glxinfo;
    jint caps = CAPS_EMPTY;
    int db;
    const unsigned char *versionstr;

    J2dRlsTraceLn(J2D_TRACE_INFO, "GLXGraphicsConfig_getGLXConfigInfo");

    if (usingXinerama) {
        // when Xinerama is enabled, the screen ID needs to be 0
        screennum = 0;
    }

    fbconfig = GLXGC_InitFBConfig(env, screennum, (VisualID)visnum);
    if (fbconfig == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: could not create fbconfig");
        return 0L;
    }

    if (sharedContext == 0) {
        // create the one shared context
        sharedContext = j2d_glXCreateNewContext(awt_display, fbconfig,
                                                GLX_RGBA_TYPE, 0, GL_TRUE);
        if (sharedContext == 0) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "GLXGraphicsConfig_getGLXConfigInfo: could not create shared context");
            return 0L;
        }
    }

    // create the GLXContext for this GLXGraphicsConfig
    context = j2d_glXCreateNewContext(awt_display, fbconfig,
                                      GLX_RGBA_TYPE, sharedContext,
                                      GL_TRUE);
    if (context == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: could not create GLX context");
        return 0L;
    }

    // this is pretty sketchy, but it seems to be the easiest way to create
    // some form of GLXDrawable using only the display and a GLXFBConfig
    // (in order to make the context current for checking the version,
    // extensions, etc)...
    scratch = GLXGC_InitScratchPbuffer(fbconfig);
    if (scratch == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: could not create scratch pbuffer");
        j2d_glXDestroyContext(awt_display, context);
        return 0L;
    }

    // the context must be made current before we can query the
    // version and extension strings
    j2d_glXMakeContextCurrent(awt_display, scratch, scratch, context);

    versionstr = j2d_glGetString(GL_VERSION);
    OGLContext_GetExtensionInfo(env, &caps);

    // destroy the temporary resources
    j2d_glXMakeContextCurrent(awt_display, None, None, NULL);

    J2dRlsTraceLn1(J2D_TRACE_INFO,
        "GLXGraphicsConfig_getGLXConfigInfo: OpenGL version=%s",
                   (versionstr == NULL) ? "null" : (char *)versionstr);

    if (!OGLContext_IsVersionSupported(versionstr)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: OpenGL 1.2 is required");
        j2d_glXDestroyPbuffer(awt_display, scratch);
        j2d_glXDestroyContext(awt_display, context);
        return 0L;
    }

    // get config-specific capabilities
    j2d_glXGetFBConfigAttrib(awt_display, fbconfig, GLX_DOUBLEBUFFER, &db);
    if (db) {
        caps |= CAPS_DOUBLEBUFFERED;
    }

    // initialize the OGLContext, which wraps the GLXFBConfig and GLXContext
    oglc = GLXGC_InitOGLContext(fbconfig, context, scratch, caps);
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: could not create oglc");
        j2d_glXDestroyPbuffer(awt_display, scratch);
        j2d_glXDestroyContext(awt_display, context);
        return 0L;
    }

    J2dTraceLn(J2D_TRACE_VERBOSE,
        "GLXGraphicsConfig_getGLXConfigInfo: finished checking dependencies");

    // create the GLXGraphicsConfigInfo record for this config
    glxinfo = (GLXGraphicsConfigInfo *)malloc(sizeof(GLXGraphicsConfigInfo));
    if (glxinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "GLXGraphicsConfig_getGLXConfigInfo: could not allocate memory for glxinfo");
        GLXGC_DestroyOGLContext(oglc);
        return 0L;
    }

    glxinfo->screen = screennum;
    glxinfo->visual = visnum;
    glxinfo->context = oglc;
    glxinfo->fbconfig = fbconfig;

    return ptr_to_jlong(glxinfo);
#else
    return 0L;
#endif /* !HEADLESS */
}

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_GLXGraphicsConfig_initConfig(JNIEnv *env,
                                                    jobject glxgc,
                                                    jlong aData,
                                                    jlong configInfo)
{
#ifndef HEADLESS
    GLXGraphicsConfigInfo *glxinfo;
    AwtGraphicsConfigDataPtr configData =
        (AwtGraphicsConfigDataPtr)jlong_to_ptr(aData);

    J2dTraceLn(J2D_TRACE_INFO, "GLXGraphicsConfig_initConfig");

    if (configData == NULL) {
        JNU_ThrowNullPointerException(env, "Native GraphicsConfig missing");
        return;
    }

    glxinfo = (GLXGraphicsConfigInfo *)jlong_to_ptr(configInfo);
    if (glxinfo == NULL) {
        JNU_ThrowNullPointerException(env,
                                      "GLXGraphicsConfigInfo data missing");
        return;
    }

    configData->glxInfo = glxinfo;
#endif /* !HEADLESS */
}

JNIEXPORT jint JNICALL
Java_sun_java2d_opengl_GLXGraphicsConfig_getOGLCapabilities(JNIEnv *env,
                                                            jclass glxgc,
                                                            jlong configInfo)
{
#ifndef HEADLESS
    GLXGraphicsConfigInfo *glxinfo =
        (GLXGraphicsConfigInfo *)jlong_to_ptr(configInfo);

    J2dTraceLn(J2D_TRACE_INFO, "GLXGraphicsConfig_getOGLCapabilities");

    if (glxinfo == NULL || glxinfo->context == NULL) {
        return CAPS_EMPTY;
    }

    return glxinfo->context->caps;
#else
    return CAPS_EMPTY;
#endif /* !HEADLESS */
}
