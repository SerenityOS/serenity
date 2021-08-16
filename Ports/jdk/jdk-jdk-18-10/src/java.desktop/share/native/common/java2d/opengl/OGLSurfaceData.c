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

#ifndef HEADLESS

#include <stdlib.h>

#include "sun_java2d_opengl_OGLSurfaceData.h"

#include "jlong.h"
#include "jni_util.h"
#include "OGLSurfaceData.h"

/**
 * The following methods are implemented in the windowing system (i.e. GLX
 * and WGL) source files.
 */
extern jboolean OGLSD_InitOGLWindow(JNIEnv *env, OGLSDOps *oglsdo);
extern void OGLSD_DestroyOGLSurface(JNIEnv *env, OGLSDOps *oglsdo);

void OGLSD_SetNativeDimensions(JNIEnv *env, OGLSDOps *oglsdo, jint w, jint h);

/**
 * This table contains the "pixel formats" for all system memory surfaces
 * that OpenGL is capable of handling, indexed by the "PF_" constants defined
 * in OGLSurfaceData.java.  These pixel formats contain information that is
 * passed to OpenGL when copying from a system memory ("Sw") surface to
 * an OpenGL "Surface" (via glDrawPixels()) or "Texture" (via glTexImage2D()).
 */
OGLPixelFormat PixelFormats[] = {
    { GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
      4, 1, 0,                                     }, /* 0 - IntArgb      */
    { GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
      4, 1, 1,                                     }, /* 1 - IntArgbPre   */
    { GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
      4, 0, 1,                                     }, /* 2 - IntRgb       */
    { GL_RGBA, GL_UNSIGNED_INT_8_8_8_8,
      4, 0, 1,                                     }, /* 3 - IntRgbx      */
    { GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV,
      4, 0, 1,                                     }, /* 4 - IntBgr       */
    { GL_BGRA, GL_UNSIGNED_INT_8_8_8_8,
      4, 0, 1,                                     }, /* 5 - IntBgrx      */
    { GL_RGB,  GL_UNSIGNED_SHORT_5_6_5,
      2, 0, 1,                                     }, /* 6 - Ushort565Rgb */
    { GL_BGRA, GL_UNSIGNED_SHORT_1_5_5_5_REV,
      2, 0, 1,                                     }, /* 7 - Ushort555Rgb */
    { GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1,
      2, 0, 1,                                     }, /* 8 - Ushort555Rgbx*/
    { GL_LUMINANCE, GL_UNSIGNED_BYTE,
      1, 0, 1,                                     }, /* 9 - ByteGray     */
    { GL_LUMINANCE, GL_UNSIGNED_SHORT,
      2, 0, 1,                                     }, /*10 - UshortGray   */
    { GL_BGR,  GL_UNSIGNED_BYTE,
      1, 0, 1,                                     }, /*11 - ThreeByteBgr */};

/**
 * Given a starting value and a maximum limit, returns the first power-of-two
 * greater than the starting value.  If the resulting value is greater than
 * the maximum limit, zero is returned.
 */
jint
OGLSD_NextPowerOfTwo(jint val, jint max)
{
    jint i;

    if (val > max) {
        return 0;
    }

    for (i = 1; i < val; i *= 2);

    return i;
}

/**
 * Returns true if both given dimensions are a power of two.
 */
static jboolean
OGLSD_IsPowerOfTwo(jint width, jint height)
{
    return (((width & (width-1)) | (height & (height-1))) == 0);
}

/**
 * Initializes an OpenGL texture object.
 *
 * If the isOpaque parameter is JNI_FALSE, then the texture will have a
 * full alpha channel; otherwise, the texture will be opaque (this can
 * help save VRAM when translucency is not needed).
 *
 * If the GL_ARB_texture_non_power_of_two extension is present (texNonPow2
 * is JNI_TRUE), the actual texture is allowed to have non-power-of-two
 * dimensions, and therefore width==textureWidth and height==textureHeight.
 *
 * Failing that, if the GL_ARB_texture_rectangle extension is present
 * (texRect is JNI_TRUE), the actual texture is allowed to have
 * non-power-of-two dimensions, except that instead of using the usual
 * GL_TEXTURE_2D target, we need to use the GL_TEXTURE_RECTANGLE_ARB target.
 * Note that the GL_REPEAT wrapping mode is not allowed with this target,
 * so if that mode is needed (e.g. as is the case in the TexturePaint code)
 * one should pass JNI_FALSE to avoid using this extension.  Also note that
 * when the texture target is GL_TEXTURE_RECTANGLE_ARB, texture coordinates
 * must be specified in the range [0,width] and [0,height] rather than
 * [0,1] as is the case with the usual GL_TEXTURE_2D target (so take care)!
 *
 * Otherwise, the actual texture must have power-of-two dimensions, and
 * therefore the textureWidth and textureHeight will be the next
 * power-of-two greater than (or equal to) the requested width and height.
 */
static jboolean
OGLSD_InitTextureObject(OGLSDOps *oglsdo,
                        jboolean isOpaque,
                        jboolean texNonPow2, jboolean texRect,
                        jint width, jint height)
{
    GLenum texTarget, texProxyTarget;
    GLint format = GL_RGBA;
    GLint size = GL_UNSIGNED_INT_8_8_8_8;
    GLuint texID;
    GLsizei texWidth, texHeight, realWidth, realHeight;
    GLint texMax;

    J2dTraceLn4(J2D_TRACE_INFO,
                "OGLSD_InitTextureObject: w=%d h=%d opq=%d nonpow2=%d",
                width, height, isOpaque, texNonPow2);

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLSD_InitTextureObject: ops are null");
        return JNI_FALSE;
    }

    if (texNonPow2) {
        // use non-pow2 dimensions with GL_TEXTURE_2D target
        j2d_glGetIntegerv(GL_MAX_TEXTURE_SIZE, &texMax);
        texWidth = (width <= texMax) ? width : 0;
        texHeight = (height <= texMax) ? height : 0;
        texTarget = GL_TEXTURE_2D;
        texProxyTarget = GL_PROXY_TEXTURE_2D;
    } else if (texRect) {
        // use non-pow2 dimensions with GL_TEXTURE_RECTANGLE_ARB target
        j2d_glGetIntegerv(GL_MAX_RECTANGLE_TEXTURE_SIZE_ARB, &texMax);
        texWidth = (width <= texMax) ? width : 0;
        texHeight = (height <= texMax) ? height : 0;
        texTarget = GL_TEXTURE_RECTANGLE_ARB;
        texProxyTarget = GL_PROXY_TEXTURE_RECTANGLE_ARB;
    } else {
        // find the appropriate power-of-two dimensions
        j2d_glGetIntegerv(GL_MAX_TEXTURE_SIZE, &texMax);
        texWidth = OGLSD_NextPowerOfTwo(width, texMax);
        texHeight = OGLSD_NextPowerOfTwo(height, texMax);
        texTarget = GL_TEXTURE_2D;
        texProxyTarget = GL_PROXY_TEXTURE_2D;
    }

    J2dTraceLn3(J2D_TRACE_VERBOSE,
                "  desired texture dimensions: w=%d h=%d max=%d",
                texWidth, texHeight, texMax);

    // if either dimension is 0, we cannot allocate a texture with the
    // requested dimensions
    if ((texWidth == 0) || (texHeight == 0)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSD_InitTextureObject: texture dimensions too large");
        return JNI_FALSE;
    }

    // now use a proxy to determine whether we can create a texture with
    // the calculated power-of-two dimensions and the given internal format
    j2d_glTexImage2D(texProxyTarget, 0, format,
                     texWidth, texHeight, 0,
                     format, size, NULL);
    j2d_glGetTexLevelParameteriv(texProxyTarget, 0,
                                 GL_TEXTURE_WIDTH, &realWidth);
    j2d_glGetTexLevelParameteriv(texProxyTarget, 0,
                                 GL_TEXTURE_HEIGHT, &realHeight);

    // if the requested dimensions and proxy dimensions don't match,
    // we shouldn't attempt to create the texture
    if ((realWidth != texWidth) || (realHeight != texHeight)) {
        J2dRlsTraceLn2(J2D_TRACE_ERROR,
            "OGLSD_InitTextureObject: actual (w=%d h=%d) != requested",
                       realWidth, realHeight);
        return JNI_FALSE;
    }

    // initialize the texture with some dummy data (this allows us to create
    // a texture object once with 2^n dimensions, and then use
    // glTexSubImage2D() to provide further updates)
    j2d_glGenTextures(1, &texID);
    j2d_glBindTexture(texTarget, texID);
    j2d_glTexImage2D(texTarget, 0, format,
                     texWidth, texHeight, 0,
                     format, size, NULL);

    oglsdo->isOpaque = isOpaque;
    oglsdo->xOffset = 0;
    oglsdo->yOffset = 0;
    oglsdo->width = width;
    oglsdo->height = height;
    oglsdo->textureID = texID;
    oglsdo->textureWidth = texWidth;
    oglsdo->textureHeight = texHeight;
    oglsdo->textureTarget = texTarget;
    OGLSD_INIT_TEXTURE_FILTER(oglsdo, GL_NEAREST);
    OGLSD_RESET_TEXTURE_WRAP(texTarget);

    J2dTraceLn3(J2D_TRACE_VERBOSE, "  created texture: w=%d h=%d id=%d",
                width, height, texID);

    return JNI_TRUE;
}

/**
 * Initializes an OpenGL texture, using the given width and height as
 * a guide.  See OGLSD_InitTextureObject() for more information.
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_opengl_OGLSurfaceData_initTexture
    (JNIEnv *env, jobject oglsd,
     jlong pData, jboolean isOpaque,
     jboolean texNonPow2, jboolean texRect,
     jint width, jint height)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);

    J2dTraceLn2(J2D_TRACE_INFO, "OGLSurfaceData_initTexture: w=%d h=%d",
                width, height);

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initTexture: ops are null");
        return JNI_FALSE;
    }

    /*
     * We only use the GL_ARB_texture_rectangle extension if it is available
     * and the requested bounds are not pow2 (it is probably faster to use
     * GL_TEXTURE_2D for pow2 textures, and besides, our TexturePaint
     * code relies on GL_REPEAT, which is not allowed for
     * GL_TEXTURE_RECTANGLE_ARB targets).
     */
    texRect = texRect && !OGLSD_IsPowerOfTwo(width, height);

    if (!OGLSD_InitTextureObject(oglsdo, isOpaque, texNonPow2, texRect,
                                 width, height))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initTexture: could not init texture object");
        return JNI_FALSE;
    }

    OGLSD_SetNativeDimensions(env, oglsdo,
                              oglsdo->textureWidth, oglsdo->textureHeight);

    oglsdo->drawableType = OGLSD_TEXTURE;
    // other fields (e.g. width, height) are set in OGLSD_InitTextureObject()

    return JNI_TRUE;
}

/**
 * Initializes a framebuffer object based on the given textureID and its
 * width/height.  This method will iterate through all possible depth formats
 * to find one that is supported by the drivers/hardware.  (Since our use of
 * the depth buffer is fairly simplistic, we hope to find a depth format that
 * uses as little VRAM as possible.)  If an appropriate depth buffer is found
 * and all attachments are successful (i.e. the framebuffer object is
 * "complete"), then this method will return JNI_TRUE and will initialize
 * the values of fbobjectID and depthID using the IDs created by this method.
 * Otherwise, this method returns JNI_FALSE.  Note that the caller is only
 * responsible for deleting the allocated fbobject and depth renderbuffer
 * resources if this method returned JNI_TRUE.
 */
jboolean
OGLSD_InitFBObject(GLuint *fbobjectID, GLuint *depthID,
                   GLuint textureID, GLenum textureTarget,
                   jint textureWidth, jint textureHeight)
{
    GLenum depthFormats[] = {
        GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32
    };
    GLuint fboTmpID, depthTmpID;
    jboolean foundDepth = JNI_FALSE;
    int i;

    J2dTraceLn3(J2D_TRACE_INFO, "OGLSD_InitFBObject: w=%d h=%d texid=%d",
                textureWidth, textureHeight, textureID);

    // initialize framebuffer object
    j2d_glGenFramebuffersEXT(1, &fboTmpID);
    j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fboTmpID);

    // attach color texture to framebuffer object
    j2d_glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,
                                  GL_COLOR_ATTACHMENT0_EXT,
                                  textureTarget, textureID, 0);

    // attempt to create a depth renderbuffer of a particular format; we
    // will start with the smallest size and then work our way up
    for (i = 0; i < 3; i++) {
        GLenum error, status;
        GLenum depthFormat = depthFormats[i];
        int depthSize = 16 + (i * 8);

        // initialize depth renderbuffer
        j2d_glGenRenderbuffersEXT(1, &depthTmpID);
        j2d_glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, depthTmpID);
        j2d_glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, depthFormat,
                                     textureWidth, textureHeight);

        // creation of depth buffer could potentially fail, so check for error
        error = j2d_glGetError();
        if (error != GL_NO_ERROR) {
            J2dTraceLn2(J2D_TRACE_VERBOSE,
                "OGLSD_InitFBObject: could not create depth buffer: depth=%d error=%x",
                           depthSize, error);
            j2d_glDeleteRenderbuffersEXT(1, &depthTmpID);
            continue;
        }

        // attach depth renderbuffer to framebuffer object
        j2d_glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT,
                                         GL_DEPTH_ATTACHMENT_EXT,
                                         GL_RENDERBUFFER_EXT, depthTmpID);

        // now check for framebuffer "completeness"
        status = j2d_glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);

        if (status == GL_FRAMEBUFFER_COMPLETE_EXT) {
            // we found a valid format, so break out of the loop
            J2dTraceLn1(J2D_TRACE_VERBOSE,
                        "  framebuffer is complete: depth=%d", depthSize);
            foundDepth = JNI_TRUE;
            break;
        } else {
            // this depth format didn't work, so delete and try another format
            J2dTraceLn2(J2D_TRACE_VERBOSE,
                        "  framebuffer is incomplete: depth=%d status=%x",
                        depthSize, status);
            j2d_glDeleteRenderbuffersEXT(1, &depthTmpID);
        }
    }

    // unbind the texture and framebuffer objects (they will be bound again
    // later as needed)
    j2d_glBindTexture(textureTarget, 0);
    j2d_glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, 0);
    j2d_glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

    if (!foundDepth) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSD_InitFBObject: could not find valid depth format");
        j2d_glDeleteFramebuffersEXT(1, &fboTmpID);
        return JNI_FALSE;
    }

    *fbobjectID = fboTmpID;
    *depthID = depthTmpID;

    return JNI_TRUE;
}

/**
 * Initializes a framebuffer object, using the given width and height as
 * a guide.  See OGLSD_InitTextureObject() and OGLSD_InitFBObject()
 * for more information.
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_opengl_OGLSurfaceData_initFBObject
    (JNIEnv *env, jobject oglsd,
     jlong pData, jboolean isOpaque,
     jboolean texNonPow2, jboolean texRect,
     jint width, jint height)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);
    GLuint fbobjectID, depthID;

    J2dTraceLn2(J2D_TRACE_INFO,
                "OGLSurfaceData_initFBObject: w=%d h=%d",
                width, height);

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initFBObject: ops are null");
        return JNI_FALSE;
    }

    // initialize color texture object
    if (!OGLSD_InitTextureObject(oglsdo, isOpaque, texNonPow2, texRect,
                                 width, height))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initFBObject: could not init texture object");
        return JNI_FALSE;
    }

    // initialize framebuffer object using color texture created above
    if (!OGLSD_InitFBObject(&fbobjectID, &depthID,
                            oglsdo->textureID, oglsdo->textureTarget,
                            oglsdo->textureWidth, oglsdo->textureHeight))
    {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initFBObject: could not init fbobject");
        j2d_glDeleteTextures(1, &oglsdo->textureID);
        return JNI_FALSE;
    }

    oglsdo->drawableType = OGLSD_FBOBJECT;
    // other fields (e.g. width, height) are set in OGLSD_InitTextureObject()
    oglsdo->fbobjectID = fbobjectID;
    oglsdo->depthID = depthID;

    OGLSD_SetNativeDimensions(env, oglsdo,
                              oglsdo->textureWidth, oglsdo->textureHeight);

    // framebuffer objects differ from other OpenGL surfaces in that the
    // value passed to glRead/DrawBuffer() must be GL_COLOR_ATTACHMENTn_EXT,
    // rather than GL_FRONT (or GL_BACK)
    oglsdo->activeBuffer = GL_COLOR_ATTACHMENT0_EXT;

    return JNI_TRUE;
}

/**
 * Initializes a surface in the backbuffer of a given double-buffered
 * onscreen window for use in a BufferStrategy.Flip situation.  The bounds of
 * the backbuffer surface should always be kept in sync with the bounds of
 * the underlying native window.
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_opengl_OGLSurfaceData_initFlipBackbuffer
    (JNIEnv *env, jobject oglsd,
     jlong pData)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);

    J2dTraceLn(J2D_TRACE_INFO, "OGLSurfaceData_initFlipBackbuffer");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initFlipBackbuffer: ops are null");
        return JNI_FALSE;
    }

    if (oglsdo->drawableType == OGLSD_UNDEFINED) {
        if (!OGLSD_InitOGLWindow(env, oglsdo)) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "OGLSurfaceData_initFlipBackbuffer: could not init window");
            return JNI_FALSE;
        }
    }

    if (oglsdo->drawableType != OGLSD_WINDOW) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_initFlipBackbuffer: drawable is not a window");
        return JNI_FALSE;
    }

    oglsdo->drawableType = OGLSD_FLIP_BACKBUFFER;
    // x/yOffset have already been set in OGLSD_InitOGLWindow()...
    // REMIND: for some reason, flipping won't work properly on IFB unless we
    //         explicitly use BACK_LEFT rather than BACK...
    oglsdo->activeBuffer = GL_BACK_LEFT;

    OGLSD_SetNativeDimensions(env, oglsdo, oglsdo->width, oglsdo->height);

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_sun_java2d_opengl_OGLSurfaceData_getTextureTarget
    (JNIEnv *env, jobject oglsd,
     jlong pData)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);

    J2dTraceLn(J2D_TRACE_INFO, "OGLSurfaceData_getTextureTarget");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_getTextureTarget: ops are null");
        return 0;
    }

    return (jint)oglsdo->textureTarget;
}

JNIEXPORT jint JNICALL
Java_sun_java2d_opengl_OGLSurfaceData_getTextureID
    (JNIEnv *env, jobject oglsd,
     jlong pData)
{
    OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);

    J2dTraceLn(J2D_TRACE_INFO, "OGLSurfaceData_getTextureID");

    if (oglsdo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLSurfaceData_getTextureID: ops are null");
        return 0L;
    }

    return (jint)oglsdo->textureID;
}

/**
 * Initializes nativeWidth/Height fields of the surfaceData object with
 * passed arguments.
 */
void
OGLSD_SetNativeDimensions(JNIEnv *env, OGLSDOps *oglsdo,
                          jint width, jint height)
{
    jobject sdObject;

    sdObject = (*env)->NewLocalRef(env, oglsdo->sdOps.sdObject);
    if (sdObject == NULL) {
        return;
    }

    JNU_SetFieldByName(env, NULL, sdObject, "nativeWidth", "I", width);
    if (!((*env)->ExceptionOccurred(env))) {
        JNU_SetFieldByName(env, NULL, sdObject, "nativeHeight", "I", height);
    }

    (*env)->DeleteLocalRef(env, sdObject);
}

/**
 * Deletes native OpenGL resources associated with this surface.
 */
void
OGLSD_Delete(JNIEnv *env, OGLSDOps *oglsdo)
{
    J2dTraceLn1(J2D_TRACE_INFO, "OGLSD_Delete: type=%d",
                oglsdo->drawableType);

    if (oglsdo->drawableType == OGLSD_TEXTURE) {
        if (oglsdo->textureID != 0) {
            j2d_glDeleteTextures(1, &oglsdo->textureID);
            oglsdo->textureID = 0;
        }
    } else if (oglsdo->drawableType == OGLSD_FBOBJECT) {
        if (oglsdo->textureID != 0) {
            j2d_glDeleteTextures(1, &oglsdo->textureID);
            oglsdo->textureID = 0;
        }
        if (oglsdo->depthID != 0) {
            j2d_glDeleteRenderbuffersEXT(1, &oglsdo->depthID);
            oglsdo->depthID = 0;
        }
        if (oglsdo->fbobjectID != 0) {
            j2d_glDeleteFramebuffersEXT(1, &oglsdo->fbobjectID);
            oglsdo->fbobjectID = 0;
        }
    } else {
        // dispose windowing system resources (pbuffer, pixmap, etc)
        OGLSD_DestroyOGLSurface(env, oglsdo);
    }
}

/**
 * This is the implementation of the general DisposeFunc defined in
 * SurfaceData.h and used by the Disposer mechanism.  It first flushes all
 * native OpenGL resources and then frees any memory allocated within the
 * native OGLSDOps structure.
 */
void
OGLSD_Dispose(JNIEnv *env, SurfaceDataOps *ops)
{
    OGLSDOps *oglsdo = (OGLSDOps *)ops;
    jobject graphicsConfig = oglsdo->graphicsConfig;

    JNU_CallStaticMethodByName(env, NULL, "sun/java2d/opengl/OGLSurfaceData",
                               "dispose",
                               "(JLsun/java2d/opengl/OGLGraphicsConfig;)V",
                               ptr_to_jlong(ops), graphicsConfig);
    (*env)->DeleteGlobalRef(env, graphicsConfig);
    oglsdo->graphicsConfig = NULL;
}

/**
 * This is the implementation of the general surface LockFunc defined in
 * SurfaceData.h.
 */
jint
OGLSD_Lock(JNIEnv *env,
           SurfaceDataOps *ops,
           SurfaceDataRasInfo *pRasInfo,
           jint lockflags)
{
    JNU_ThrowInternalError(env, "OGLSD_Lock not implemented!");
    return SD_FAILURE;
}

/**
 * This is the implementation of the general GetRasInfoFunc defined in
 * SurfaceData.h.
 */
void
OGLSD_GetRasInfo(JNIEnv *env,
                 SurfaceDataOps *ops,
                 SurfaceDataRasInfo *pRasInfo)
{
    JNU_ThrowInternalError(env, "OGLSD_GetRasInfo not implemented!");
}

/**
 * This is the implementation of the general surface UnlockFunc defined in
 * SurfaceData.h.
 */
void
OGLSD_Unlock(JNIEnv *env,
             SurfaceDataOps *ops,
             SurfaceDataRasInfo *pRasInfo)
{
    JNU_ThrowInternalError(env, "OGLSD_Unlock not implemented!");
}

#endif /* !HEADLESS */
