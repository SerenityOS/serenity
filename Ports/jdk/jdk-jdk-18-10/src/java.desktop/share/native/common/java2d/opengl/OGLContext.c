/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>

#include "sun_java2d_SunGraphics2D.h"

#include "jlong.h"
#include "jni_util.h"
#include "OGLContext.h"
#include "OGLRenderQueue.h"
#include "OGLSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"
#include "Region.h"

#include "jvm.h"

/**
 * The following methods are implemented in the windowing system (i.e. GLX
 * and WGL) source files.
 */
extern jboolean OGLSD_InitOGLWindow(JNIEnv *env, OGLSDOps *oglsdo);
extern OGLContext *OGLSD_MakeOGLContextCurrent(JNIEnv *env,
                                               OGLSDOps *srcOps,
                                               OGLSDOps *dstOps);

/**
 * This table contains the standard blending rules (or Porter-Duff compositing
 * factors) used in glBlendFunc(), indexed by the rule constants from the
 * AlphaComposite class.
 */
OGLBlendRule StdBlendRules[] = {
    { GL_ZERO,                GL_ZERO                }, /* 0 - Nothing      */
    { GL_ZERO,                GL_ZERO                }, /* 1 - RULE_Clear   */
    { GL_ONE,                 GL_ZERO                }, /* 2 - RULE_Src     */
    { GL_ONE,                 GL_ONE_MINUS_SRC_ALPHA }, /* 3 - RULE_SrcOver */
    { GL_ONE_MINUS_DST_ALPHA, GL_ONE                 }, /* 4 - RULE_DstOver */
    { GL_DST_ALPHA,           GL_ZERO                }, /* 5 - RULE_SrcIn   */
    { GL_ZERO,                GL_SRC_ALPHA           }, /* 6 - RULE_DstIn   */
    { GL_ONE_MINUS_DST_ALPHA, GL_ZERO                }, /* 7 - RULE_SrcOut  */
    { GL_ZERO,                GL_ONE_MINUS_SRC_ALPHA }, /* 8 - RULE_DstOut  */
    { GL_ZERO,                GL_ONE                 }, /* 9 - RULE_Dst     */
    { GL_DST_ALPHA,           GL_ONE_MINUS_SRC_ALPHA }, /*10 - RULE_SrcAtop */
    { GL_ONE_MINUS_DST_ALPHA, GL_SRC_ALPHA           }, /*11 - RULE_DstAtop */
    { GL_ONE_MINUS_DST_ALPHA, GL_ONE_MINUS_SRC_ALPHA }, /*12 - RULE_AlphaXor*/
};

/** Evaluates to "front" or "back", depending on the value of buf. */
#define OGLC_ACTIVE_BUFFER_NAME(buf) \
    (buf == GL_FRONT || buf == GL_COLOR_ATTACHMENT0_EXT) ? "front" : "back"

/**
 * Initializes the viewport and projection matrix, effectively positioning
 * the origin at the top-left corner of the surface.  This allows Java 2D
 * coordinates to be passed directly to OpenGL, which is typically based on
 * a bottom-right coordinate system.  This method also sets the appropriate
 * read and draw buffers.
 */
static void
OGLContext_SetViewport(OGLSDOps *srcOps, OGLSDOps *dstOps)
{
    jint width = dstOps->width;
    jint height = dstOps->height;

    J2dTraceLn4(J2D_TRACE_INFO,
                "OGLContext_SetViewport: w=%d h=%d read=%s draw=%s",
                width, height,
                OGLC_ACTIVE_BUFFER_NAME(srcOps->activeBuffer),
                OGLC_ACTIVE_BUFFER_NAME(dstOps->activeBuffer));

    // set the viewport and projection matrix
    j2d_glViewport(dstOps->xOffset, dstOps->yOffset,
                   (GLsizei)width, (GLsizei)height);
    j2d_glMatrixMode(GL_PROJECTION);
    j2d_glLoadIdentity();
    j2d_glOrtho(0.0, (GLdouble)width, (GLdouble)height, 0.0, -1.0, 1.0);

    // set the active read and draw buffers
    j2d_glReadBuffer(srcOps->activeBuffer);
    j2d_glDrawBuffer(dstOps->activeBuffer);

    // set the color mask to enable alpha channel only when necessary
    j2d_glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, (GLboolean)!dstOps->isOpaque);
}

/**
 * Initializes the alpha channel of the current surface so that it contains
 * fully opaque alpha values.
 */
static void
OGLContext_InitAlphaChannel()
{
    GLboolean scissorEnabled;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_InitAlphaChannel");

    // it is possible for the scissor test to be enabled at this point;
    // if it is, disable it temporarily since it can affect the glClear() op
    scissorEnabled = j2d_glIsEnabled(GL_SCISSOR_TEST);
    if (scissorEnabled) {
        j2d_glDisable(GL_SCISSOR_TEST);
    }

    // set the color mask so that we only affect the alpha channel
    j2d_glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_TRUE);

    // clear the color buffer so that the alpha channel is fully opaque
    j2d_glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    j2d_glClear(GL_COLOR_BUFFER_BIT);

    // restore the color mask (as it was set in OGLContext_SetViewport())
    j2d_glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_FALSE);

    // re-enable scissor test, only if it was enabled earlier
    if (scissorEnabled) {
        j2d_glEnable(GL_SCISSOR_TEST);
    }
}

/**
 * Fetches the OGLContext associated with the given destination surface,
 * makes the context current for those surfaces, updates the destination
 * viewport, and then returns a pointer to the OGLContext.
 */
OGLContext *
OGLContext_SetSurfaces(JNIEnv *env, jlong pSrc, jlong pDst)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrc);
    OGLSDOps *dstOps = (OGLSDOps *)jlong_to_ptr(pDst);
    OGLContext *oglc = NULL;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_SetSurfaces");

    if (srcOps == NULL || dstOps == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_SetSurfaces: ops are null");
        return NULL;
    }

    J2dTraceLn2(J2D_TRACE_VERBOSE, "  srctype=%d dsttype=%d",
                srcOps->drawableType, dstOps->drawableType);

    if (dstOps->drawableType == OGLSD_TEXTURE) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_SetSurfaces: texture cannot be used as destination");
        return NULL;
    }

    if (dstOps->drawableType == OGLSD_UNDEFINED) {
        // initialize the surface as an OGLSD_WINDOW
        if (!OGLSD_InitOGLWindow(env, dstOps)) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                "OGLContext_SetSurfaces: could not init OGL window");
            return NULL;
        }
    }

    // make the context current
    oglc = OGLSD_MakeOGLContextCurrent(env, srcOps, dstOps);
    if (oglc == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_SetSurfaces: could not make context current");
        return NULL;
    }

    // update the viewport
    OGLContext_SetViewport(srcOps, dstOps);

    // perform additional one-time initialization, if necessary
    if (dstOps->needsInit) {
        if (dstOps->isOpaque) {
            // in this case we are treating the destination as opaque, but
            // to do so, first we need to ensure that the alpha channel
            // is filled with fully opaque values (see 6319663)
            OGLContext_InitAlphaChannel();
        }
        dstOps->needsInit = JNI_FALSE;
    }

    return oglc;
}

/**
 * Resets the current clip state (disables both scissor and depth tests).
 */
void
OGLContext_ResetClip(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_ResetClip");

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    j2d_glDisable(GL_SCISSOR_TEST);
    j2d_glDisable(GL_DEPTH_TEST);
}

/**
 * Sets the OpenGL scissor bounds to the provided rectangular clip bounds.
 */
void
OGLContext_SetRectClip(OGLContext *oglc, OGLSDOps *dstOps,
                       jint x1, jint y1, jint x2, jint y2)
{
    jint width = x2 - x1;
    jint height = y2 - y1;

    J2dTraceLn4(J2D_TRACE_INFO,
                "OGLContext_SetRectClip: x=%d y=%d w=%d h=%d",
                x1, y1, width, height);

    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    if ((width < 0) || (height < 0)) {
        // use an empty scissor rectangle when the region is empty
        width = 0;
        height = 0;
    }

    j2d_glDisable(GL_DEPTH_TEST);
    j2d_glEnable(GL_SCISSOR_TEST);

    // the scissor rectangle is specified using the lower-left
    // origin of the clip region (in the framebuffer's coordinate
    // space), so we must account for the x/y offsets of the
    // destination surface
    j2d_glScissor(dstOps->xOffset + x1,
                  dstOps->yOffset + dstOps->height - (y1 + height),
                  width, height);
}

/**
 * Sets up a complex (shape) clip using the OpenGL depth buffer.  This
 * method prepares the depth buffer so that the clip Region spans can
 * be "rendered" into it.  The depth buffer is first cleared, then the
 * depth func is setup so that when we render the clip spans,
 * nothing is rendered into the color buffer, but for each pixel that would
 * be rendered, a non-zero value is placed into that location in the depth
 * buffer.  With depth test enabled, pixels will only be rendered into the
 * color buffer if the corresponding value at that (x,y) location in the
 * depth buffer differs from the incoming depth value.
 */
void
OGLContext_BeginShapeClip(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_BeginShapeClip");

    RETURN_IF_NULL(oglc);
    RESET_PREVIOUS_OP();

    j2d_glDisable(GL_SCISSOR_TEST);

    // enable depth test and clear depth buffer so that depth values are at
    // their maximum; also set the depth func to GL_ALWAYS so that the
    // depth values of the clip spans are forced into the depth buffer
    j2d_glEnable(GL_DEPTH_TEST);
    j2d_glClearDepth(1.0);
    j2d_glClear(GL_DEPTH_BUFFER_BIT);
    j2d_glDepthFunc(GL_ALWAYS);

    // disable writes into the color buffer while we set up the clip
    j2d_glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);

    // save current transform
    j2d_glMatrixMode(GL_MODELVIEW);
    j2d_glPushMatrix();

    // use identity transform plus slight translation in the z-axis when
    // setting the clip spans; this will push the clip spans (which would
    // normally be at z=0) to the z=1 plane to give them some depth
    j2d_glLoadIdentity();
    j2d_glTranslatef(0.0f, 0.0f, 1.0f);
}

/**
 * Finishes setting up the shape clip by resetting the depth func
 * so that future rendering operations will once again be written into the
 * color buffer (while respecting the clip set up in the depth buffer).
 */
void
OGLContext_EndShapeClip(OGLContext *oglc, OGLSDOps *dstOps)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_EndShapeClip");

    RETURN_IF_NULL(dstOps);
    RETURN_IF_NULL(oglc);
    RESET_PREVIOUS_OP();

    // restore transform
    j2d_glPopMatrix();

    // re-enable writes into the color buffer
    j2d_glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, (GLboolean)!dstOps->isOpaque);

    // enable the depth test so that only fragments within the clip region
    // (i.e. those fragments whose z-values are >= the values currently
    // stored in the depth buffer) are rendered
    j2d_glDepthFunc(GL_GEQUAL);
}

/**
 * Initializes the OpenGL state responsible for applying extra alpha.  This
 * step is only necessary for any operation that uses glDrawPixels() or
 * glCopyPixels() with a non-1.0f extra alpha value.  Since the source is
 * always premultiplied, we apply the extra alpha value to both alpha and
 * color components using GL_*_SCALE.
 */
void
OGLContext_SetExtraAlpha(jfloat ea)
{
    J2dTraceLn1(J2D_TRACE_INFO, "OGLContext_SetExtraAlpha: ea=%f", ea);

    j2d_glPixelTransferf(GL_ALPHA_SCALE, ea);
    j2d_glPixelTransferf(GL_RED_SCALE, ea);
    j2d_glPixelTransferf(GL_GREEN_SCALE, ea);
    j2d_glPixelTransferf(GL_BLUE_SCALE, ea);
}

/**
 * Resets all OpenGL compositing state (disables blending and logic
 * operations).
 */
void
OGLContext_ResetComposite(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_ResetComposite");

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    // disable blending and XOR mode
    if (oglc->compState == sun_java2d_SunGraphics2D_COMP_ALPHA) {
        j2d_glDisable(GL_BLEND);
    } else if (oglc->compState == sun_java2d_SunGraphics2D_COMP_XOR) {
        j2d_glDisable(GL_COLOR_LOGIC_OP);
        j2d_glDisable(GL_ALPHA_TEST);
    }

    // set state to default values
    oglc->compState = sun_java2d_SunGraphics2D_COMP_ISCOPY;
    oglc->extraAlpha = 1.0f;
}

/**
 * Initializes the OpenGL blending state.  XOR mode is disabled and the
 * appropriate blend functions are setup based on the AlphaComposite rule
 * constant.
 */
void
OGLContext_SetAlphaComposite(OGLContext *oglc,
                             jint rule, jfloat extraAlpha, jint flags)
{
    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLContext_SetAlphaComposite: flags=%d", flags);

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    // disable XOR mode
    if (oglc->compState == sun_java2d_SunGraphics2D_COMP_XOR) {
        j2d_glDisable(GL_COLOR_LOGIC_OP);
        j2d_glDisable(GL_ALPHA_TEST);
    }

    // we can safely disable blending when:
    //   - comp is SrcNoEa or SrcOverNoEa, and
    //   - the source is opaque
    // (turning off blending can have a large positive impact on
    // performance)
    if ((rule == RULE_Src || rule == RULE_SrcOver) &&
        (extraAlpha == 1.0f) &&
        (flags & OGLC_SRC_IS_OPAQUE))
    {
        J2dTraceLn1(J2D_TRACE_VERBOSE,
                    "  disabling alpha comp: rule=%d ea=1.0 src=opq", rule);
        j2d_glDisable(GL_BLEND);
    } else {
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "  enabling alpha comp: rule=%d ea=%f", rule, extraAlpha);
        j2d_glEnable(GL_BLEND);
        j2d_glBlendFunc(StdBlendRules[rule].src, StdBlendRules[rule].dst);
    }

    // update state
    oglc->compState = sun_java2d_SunGraphics2D_COMP_ALPHA;
    oglc->extraAlpha = extraAlpha;
}

/**
 * Initializes the OpenGL logic op state to XOR mode.  Blending is disabled
 * before enabling logic op mode.  The XOR pixel value will be applied
 * later in the OGLContext_SetColor() method.
 */
void
OGLContext_SetXorComposite(OGLContext *oglc, jint xorPixel)
{
    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLContext_SetXorComposite: xorPixel=%08x", xorPixel);

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    // disable blending mode
    if (oglc->compState == sun_java2d_SunGraphics2D_COMP_ALPHA) {
        j2d_glDisable(GL_BLEND);
    }

    // enable XOR mode
    j2d_glEnable(GL_COLOR_LOGIC_OP);
    j2d_glLogicOp(GL_XOR);

    // set up the alpha test so that we discard transparent fragments (this
    // is primarily useful for rendering text in XOR mode)
    j2d_glEnable(GL_ALPHA_TEST);
    j2d_glAlphaFunc(GL_NOTEQUAL, 0.0f);

    // update state
    oglc->compState = sun_java2d_SunGraphics2D_COMP_XOR;
    oglc->xorPixel = xorPixel;
    oglc->extraAlpha = 1.0f;
}

/**
 * Resets the OpenGL transform state back to the identity matrix.
 */
void
OGLContext_ResetTransform(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_ResetTransform");

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    j2d_glMatrixMode(GL_MODELVIEW);
    j2d_glLoadIdentity();
}

/**
 * Initializes the OpenGL transform state by setting the modelview transform
 * using the given matrix parameters.
 *
 * REMIND: it may be worthwhile to add serial id to AffineTransform, so we
 *         could do a quick check to see if the xform has changed since
 *         last time... a simple object compare won't suffice...
 */
void
OGLContext_SetTransform(OGLContext *oglc,
                        jdouble m00, jdouble m10,
                        jdouble m01, jdouble m11,
                        jdouble m02, jdouble m12)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_SetTransform");

    RETURN_IF_NULL(oglc);
    CHECK_PREVIOUS_OP(OGL_STATE_CHANGE);

    if (oglc->xformMatrix == NULL) {
        size_t arrsize = 16 * sizeof(GLdouble);
        oglc->xformMatrix = (GLdouble *)malloc(arrsize);
        memset(oglc->xformMatrix, 0, arrsize);
        oglc->xformMatrix[10] = 1.0;
        oglc->xformMatrix[15] = 1.0;
    }

    // copy values from AffineTransform object into native matrix array
    oglc->xformMatrix[0] = m00;
    oglc->xformMatrix[1] = m10;
    oglc->xformMatrix[4] = m01;
    oglc->xformMatrix[5] = m11;
    oglc->xformMatrix[12] = m02;
    oglc->xformMatrix[13] = m12;

    J2dTraceLn3(J2D_TRACE_VERBOSE, "  [%lf %lf %lf]",
                oglc->xformMatrix[0], oglc->xformMatrix[4],
                oglc->xformMatrix[12]);
    J2dTraceLn3(J2D_TRACE_VERBOSE, "  [%lf %lf %lf]",
                oglc->xformMatrix[1], oglc->xformMatrix[5],
                oglc->xformMatrix[13]);

    j2d_glMatrixMode(GL_MODELVIEW);
    j2d_glLoadMatrixd(oglc->xformMatrix);
}

/**
 * Creates a 2D texture of the given format and dimensions and returns the
 * texture object identifier.  This method is typically used to create a
 * temporary texture for intermediate work, such as in the
 * OGLContext_InitBlitTileTexture() method below.
 */
GLuint
OGLContext_CreateBlitTexture(GLenum internalFormat, GLenum pixelFormat,
                             GLuint width, GLuint height)
{
    GLuint texID;
    GLint sp, sr, rl, align;
    GLclampf priority = 1.0f;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_CreateBlitTexture");

    j2d_glGenTextures(1, &texID);
    j2d_glBindTexture(GL_TEXTURE_2D, texID);
    j2d_glPrioritizeTextures(1, &texID, &priority);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    OGLSD_RESET_TEXTURE_WRAP(GL_TEXTURE_2D);

    // save pixel store parameters (since this method could be invoked after
    // the caller has already set up its pixel store parameters)
    j2d_glGetIntegerv(GL_UNPACK_SKIP_PIXELS, &sp);
    j2d_glGetIntegerv(GL_UNPACK_SKIP_ROWS, &sr);
    j2d_glGetIntegerv(GL_UNPACK_ROW_LENGTH, &rl);
    j2d_glGetIntegerv(GL_UNPACK_ALIGNMENT, &align);

    // set pixel store parameters to default values
    j2d_glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
    j2d_glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
    j2d_glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
    j2d_glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    j2d_glTexImage2D(GL_TEXTURE_2D, 0, internalFormat,
                     width, height, 0,
                     pixelFormat, GL_UNSIGNED_BYTE, NULL);

    // restore pixel store parameters
    j2d_glPixelStorei(GL_UNPACK_SKIP_PIXELS, sp);
    j2d_glPixelStorei(GL_UNPACK_SKIP_ROWS, sr);
    j2d_glPixelStorei(GL_UNPACK_ROW_LENGTH, rl);
    j2d_glPixelStorei(GL_UNPACK_ALIGNMENT, align);

    return texID;
}

/**
 * Initializes a small texture tile for use with tiled blit operations (see
 * OGLBlitLoops.c and OGLMaskBlit.c for usage examples).  The texture ID for
 * the tile is stored in the given OGLContext.  The tile is initially filled
 * with garbage values, but the tile is updated as needed (via
 * glTexSubImage2D()) with real RGBA values used in tiled blit situations.
 * The internal format for the texture is GL_RGBA8, which should be sufficient
 * for storing system memory surfaces of any known format (see PixelFormats
 * for a list of compatible surface formats).
 */
jboolean
OGLContext_InitBlitTileTexture(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_InitBlitTileTexture");

    oglc->blitTextureID =
        OGLContext_CreateBlitTexture(GL_RGBA8, GL_RGBA,
                                     OGLC_BLIT_TILE_SIZE,
                                     OGLC_BLIT_TILE_SIZE);

    return JNI_TRUE;
}

/**
 * Destroys the OpenGL resources associated with the given OGLContext.
 * It is required that the native context associated with the OGLContext
 * be made current prior to calling this method.
 */
void
OGLContext_DestroyContextResources(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_DestroyContextResources");

    if (oglc->xformMatrix != NULL) {
        free(oglc->xformMatrix);
    }

    if (oglc->blitTextureID != 0) {
        j2d_glDeleteTextures(1, &oglc->blitTextureID);
    }
}

/**
 * Returns JNI_TRUE if the given extension name is available for the current
 * GraphicsConfig; JNI_FALSE otherwise.  An extension is considered available
 * if its identifier string is found amongst the space-delimited GL_EXTENSIONS
 * string.
 *
 * Adapted from the OpenGL Red Book, pg. 506.
 */
jboolean
OGLContext_IsExtensionAvailable(const char *extString, char *extName)
{
    jboolean ret = JNI_FALSE;
    char *p = (char *)extString;
    char *end;

    if (extString == NULL) {
        J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsExtensionAvailable");
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_IsExtensionAvailable: extension string is null");
        return JNI_FALSE;
    }

    end = p + strlen(p);

    while (p < end) {
        size_t n = strcspn(p, " ");

        if ((strlen(extName) == n) && (strncmp(extName, p, n) == 0)) {
            ret = JNI_TRUE;
            break;
        }

        p += (n + 1);
    }

    J2dRlsTraceLn2(J2D_TRACE_INFO,
                   "OGLContext_IsExtensionAvailable: %s=%s",
                   extName, ret ? "true" : "false");

    return ret;
}

/**
 * Returns JNI_TRUE only if all of the following conditions are met:
 *   - the GL_EXT_framebuffer_object extension is available
 *   - FBO support has been enabled via the system property
 *   - we can successfully create an FBO with depth capabilities
 */
static jboolean
OGLContext_IsFBObjectExtensionAvailable(JNIEnv *env,
                                        const char *extString)
{
    jboolean isFBObjectEnabled = JNI_FALSE;
    GLuint fbobjectID, textureID, depthID;
    jint width = 1, height = 1;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsFBObjectExtensionAvailable");

    // first see if the fbobject extension is available
    if (!OGLContext_IsExtensionAvailable(extString,
                                         "GL_EXT_framebuffer_object"))
    {
        return JNI_FALSE;
    }

    // next see if the depth texture extension is available
    if (!OGLContext_IsExtensionAvailable(extString,
                                         "GL_ARB_depth_texture"))
    {
        return JNI_FALSE;
    }

    // next see if the fbobject system property has been enabled
    isFBObjectEnabled =
        JNU_GetStaticFieldByName(env, NULL,
                                 "sun/java2d/opengl/OGLSurfaceData",
                                 "isFBObjectEnabled", "Z").z;
    if (!isFBObjectEnabled) {
        J2dRlsTraceLn(J2D_TRACE_INFO,
            "OGLContext_IsFBObjectExtensionAvailable: disabled via flag");
        return JNI_FALSE;
    }

    // finally, create a dummy fbobject with depth capabilities to see
    // if this configuration is supported by the drivers/hardware
    // (first we initialize a color texture object that will be used to
    // construct the dummy fbobject)
    j2d_glGenTextures(1, &textureID);
    j2d_glBindTexture(GL_TEXTURE_2D, textureID);
    j2d_glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                     width, height, 0,
                     GL_RGB, GL_UNSIGNED_BYTE, NULL);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    j2d_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

    // initialize framebuffer object using color texture created above
    if (!OGLSD_InitFBObject(&fbobjectID, &depthID,
                            textureID, GL_TEXTURE_2D,
                            width, height))
    {
        J2dRlsTraceLn(J2D_TRACE_INFO,
            "OGLContext_IsFBObjectExtensionAvailable: fbobject unsupported");
        j2d_glDeleteTextures(1, &textureID);
        return JNI_FALSE;
    }

    // delete the temporary resources
    j2d_glDeleteTextures(1, &textureID);
    j2d_glDeleteRenderbuffersEXT(1, &depthID);
    j2d_glDeleteFramebuffersEXT(1, &fbobjectID);

    J2dRlsTraceLn(J2D_TRACE_INFO,
        "OGLContext_IsFBObjectExtensionAvailable: fbobject supported");

    return JNI_TRUE;
}

/**
 * Returns JNI_TRUE only if all of the following conditions are met:
 *   - the GL_ARB_fragment_shader extension is available
 *   - the LCD text shader codepath has been enabled via the system property
 *   - the hardware supports the minimum number of texture units
 */
static jboolean
OGLContext_IsLCDShaderSupportAvailable(JNIEnv *env,
                                       jboolean fragShaderAvailable)
{
    jboolean isLCDShaderEnabled = JNI_FALSE;
    GLint maxTexUnits;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsLCDShaderSupportAvailable");

    // first see if the fragment shader extension is available
    if (!fragShaderAvailable) {
        return JNI_FALSE;
    }

    // next see if the lcdshader system property has been enabled
    isLCDShaderEnabled =
        JNU_GetStaticFieldByName(env, NULL,
                                 "sun/java2d/opengl/OGLSurfaceData",
                                 "isLCDShaderEnabled", "Z").z;
    if (!isLCDShaderEnabled) {
        J2dRlsTraceLn(J2D_TRACE_INFO,
            "OGLContext_IsLCDShaderSupportAvailable: disabled via flag");
        return JNI_FALSE;
    }

    // finally, check to see if the hardware supports the required number
    // of texture units
    j2d_glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS_ARB, &maxTexUnits);
    if (maxTexUnits < 2) {
        J2dRlsTraceLn1(J2D_TRACE_INFO,
          "OGLContext_IsLCDShaderSupportAvailable: not enough tex units (%d)",
          maxTexUnits);
    }

    J2dRlsTraceLn(J2D_TRACE_INFO,
        "OGLContext_IsLCDShaderSupportAvailable: LCD text shader supported");

    return JNI_TRUE;
}

/**
 * Returns JNI_TRUE only if all of the following conditions are met:
 *   - the GL_ARB_fragment_shader extension is available
 *   - the BufferedImageOp shader codepath has been enabled via the
 *     system property
 */
static jboolean
OGLContext_IsBIOpShaderSupportAvailable(JNIEnv *env,
                                        jboolean fragShaderAvailable)
{
    jboolean isBIOpShaderEnabled = JNI_FALSE;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsBIOpShaderSupportAvailable");

    // first see if the fragment shader extension is available
    if (!fragShaderAvailable) {
        return JNI_FALSE;
    }

    // next see if the biopshader system property has been enabled
    isBIOpShaderEnabled =
        JNU_GetStaticFieldByName(env, NULL,
                                 "sun/java2d/opengl/OGLSurfaceData",
                                 "isBIOpShaderEnabled", "Z").z;
    if (!isBIOpShaderEnabled) {
        J2dRlsTraceLn(J2D_TRACE_INFO,
            "OGLContext_IsBIOpShaderSupportAvailable: disabled via flag");
        return JNI_FALSE;
    }

    /*
     * Note: In theory we should probably do some other checks here, like
     * linking a sample shader to see if the hardware truly supports our
     * shader programs.  However, our current BufferedImageOp shaders were
     * designed to support first-generation shader-level hardware, so the
     * assumption is that if our shaders work on those GPUs, then they'll
     * work on newer ones as well.  Also, linking a fragment program can
     * cost valuable CPU cycles, which is another reason to avoid these
     * checks at startup.
     */

    J2dRlsTraceLn(J2D_TRACE_INFO,
        "OGLContext_IsBIOpShaderSupportAvailable: BufferedImageOp shader supported");

    return JNI_TRUE;
}

/**
 * Returns JNI_TRUE only if all of the following conditions are met:
 *   - the GL_ARB_fragment_shader extension is available
 *   - the Linear/RadialGradientPaint shader codepath has been enabled via the
 *     system property
 */
static jboolean
OGLContext_IsGradShaderSupportAvailable(JNIEnv *env,
                                        jboolean fragShaderAvailable)
{
    jboolean isGradShaderEnabled = JNI_FALSE;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsGradShaderSupportAvailable");

    // first see if the fragment shader extension is available
    if (!fragShaderAvailable) {
        return JNI_FALSE;
    }

    // next see if the gradshader system property has been enabled
    isGradShaderEnabled =
        JNU_GetStaticFieldByName(env, NULL,
                                 "sun/java2d/opengl/OGLSurfaceData",
                                 "isGradShaderEnabled", "Z").z;
    if (!isGradShaderEnabled) {
        J2dRlsTraceLn(J2D_TRACE_INFO,
            "OGLContext_IsGradShaderSupportAvailable: disabled via flag");
        return JNI_FALSE;
    }

    J2dRlsTraceLn(J2D_TRACE_INFO,
        "OGLContext_IsGradShaderSupportAvailable: Linear/RadialGradientPaint shader supported");

    return JNI_TRUE;
}

/**
 * Checks for the presence of the optional extensions used by
 * the Java 2D OpenGL pipeline.  The given caps bitfield is updated
 * to reflect the availability of these extensions.
 */
void
OGLContext_GetExtensionInfo(JNIEnv *env, jint *caps)
{
    jint vcap = OGLC_VENDOR_OTHER;
    const char *vendor = (char *)j2d_glGetString(GL_VENDOR);
    const char *e = (char *)j2d_glGetString(GL_EXTENSIONS);
    jboolean fragShaderAvail =
        OGLContext_IsExtensionAvailable(e, "GL_ARB_fragment_shader");

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_GetExtensionInfo");

    *caps |= CAPS_TEXNONSQUARE;
    if (OGLContext_IsExtensionAvailable(e, "GL_ARB_multitexture")) {
        *caps |= CAPS_MULTITEXTURE;
    }
    if (OGLContext_IsExtensionAvailable(e, "GL_ARB_texture_non_power_of_two")){
        *caps |= CAPS_TEXNONPOW2;
    }
    // 6656574: Use of the GL_ARB_texture_rectangle extension by Java 2D
    // complicates any third-party libraries that try to interact with
    // the OGL pipeline (and we've run into driver bugs in the past related
    // to this extension), so for now we will disable its use by default (unless
    // forced). We will still make use of the GL_ARB_texture_non_power_of_two
    // extension when available, which is the better choice going forward
    // anyway.
    if (OGLContext_IsExtensionAvailable(e, "GL_ARB_texture_rectangle") &&
        getenv("J2D_OGL_TEXRECT") != NULL)
    {
        *caps |= CAPS_EXT_TEXRECT;
    }
    if (OGLContext_IsFBObjectExtensionAvailable(env, e)) {
        *caps |= CAPS_EXT_FBOBJECT;
    }
    if (OGLContext_IsLCDShaderSupportAvailable(env, fragShaderAvail)) {
        *caps |= CAPS_EXT_LCD_SHADER | CAPS_PS20;
    }
    if (OGLContext_IsBIOpShaderSupportAvailable(env, fragShaderAvail)) {
        *caps |= CAPS_EXT_BIOP_SHADER | CAPS_PS20;
    }
    if (OGLContext_IsGradShaderSupportAvailable(env, fragShaderAvail)) {
        *caps |= CAPS_EXT_GRAD_SHADER | CAPS_PS20;
    }
    if (OGLContext_IsExtensionAvailable(e, "GL_NV_fragment_program")) {
        // this is an Nvidia board, at least PS 2.0, but we can't
        // use the "max instructions" heuristic since GeForce FX
        // boards report 1024 even though they're only PS 2.0,
        // so we'll check the following, which does imply PS 3.0
        if (OGLContext_IsExtensionAvailable(e, "GL_NV_fragment_program2")) {
            *caps |= CAPS_PS30;
        }
    } else {
        // for all other boards, we look at the "max instructions"
        // count reported by the GL_ARB_fragment_program extension
        // as a heuristic for detecting PS 3.0 compatible hardware
        if (OGLContext_IsExtensionAvailable(e, "GL_ARB_fragment_program")) {
            GLint instr;
            j2d_glGetProgramivARB(GL_FRAGMENT_PROGRAM_ARB,
                                  GL_MAX_PROGRAM_INSTRUCTIONS_ARB, &instr);
            if (instr > 512) {
                *caps |= CAPS_PS30;
            }
        }
    }
    if (OGLContext_IsExtensionAvailable(e, "GL_NV_texture_barrier")) {
        *caps |= CAPS_EXT_TEXBARRIER;
    }

    // stuff vendor descriptor in the upper bits of the caps
    if (vendor != NULL) {
        if (strncmp(vendor, "ATI", 3) == 0) {
            vcap = OGLC_VENDOR_ATI;
        } else if (strncmp(vendor, "NVIDIA", 6) == 0) {
            vcap = OGLC_VENDOR_NVIDIA;
        } else if (strncmp(vendor, "Intel", 5) == 0) {
            vcap = OGLC_VENDOR_INTEL;
        }
        // REMIND: new in 7 - check if needs fixing
        *caps |= ((vcap & OGLC_VCAP_MASK) << OGLC_VCAP_OFFSET);
    }

}

/**
 * Returns JNI_TRUE if the given GL_VERSION string meets the minimum
 * requirements (>= 1.2); JNI_FALSE otherwise.
 */
jboolean
OGLContext_IsVersionSupported(const unsigned char *versionstr)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_IsVersionSupported");

    if (versionstr == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_IsVersionSupported: version string is null");
        return JNI_FALSE;
    }

    // note that this check allows for OpenGL 2.x
    return ((versionstr[0] == '1' && versionstr[2] >= '2') ||
            (versionstr[0] >= '2'));
}

/**
 * Compiles and links the given fragment shader program.  If
 * successful, this function returns a handle to the newly created shader
 * program; otherwise returns 0.
 */
GLhandleARB
OGLContext_CreateFragmentProgram(const char *fragmentShaderSource)
{
    GLhandleARB fragmentShader, fragmentProgram;
    GLint success;
    int infoLogLength = 0;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_CreateFragmentProgram");

    // create the shader object and compile the shader source code
    fragmentShader = j2d_glCreateShaderObjectARB(GL_FRAGMENT_SHADER_ARB);
    j2d_glShaderSourceARB(fragmentShader, 1, &fragmentShaderSource, NULL);
    j2d_glCompileShaderARB(fragmentShader);
    j2d_glGetObjectParameterivARB(fragmentShader,
                                  GL_OBJECT_COMPILE_STATUS_ARB,
                                  &success);

    // print the compiler messages, if necessary
    j2d_glGetObjectParameterivARB(fragmentShader,
                                  GL_OBJECT_INFO_LOG_LENGTH_ARB,
                                  &infoLogLength);
    if (infoLogLength > 1) {
        char infoLog[1024];
        j2d_glGetInfoLogARB(fragmentShader, 1024, NULL, infoLog);
        J2dRlsTraceLn2(J2D_TRACE_WARNING,
            "OGLContext_CreateFragmentProgram: compiler msg (%d):\n%s",
                       infoLogLength, infoLog);
    }

    if (!success) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_CreateFragmentProgram: error compiling shader");
        j2d_glDeleteObjectARB(fragmentShader);
        return 0;
    }

    // create the program object and attach it to the shader
    fragmentProgram = j2d_glCreateProgramObjectARB();
    j2d_glAttachObjectARB(fragmentProgram, fragmentShader);

    // it is now safe to delete the shader object
    j2d_glDeleteObjectARB(fragmentShader);

    // link the program
    j2d_glLinkProgramARB(fragmentProgram);
    j2d_glGetObjectParameterivARB(fragmentProgram,
                                  GL_OBJECT_LINK_STATUS_ARB,
                                  &success);

    // print the linker messages, if necessary
    j2d_glGetObjectParameterivARB(fragmentProgram,
                                  GL_OBJECT_INFO_LOG_LENGTH_ARB,
                                  &infoLogLength);
    if (infoLogLength > 1) {
        char infoLog[1024];
        j2d_glGetInfoLogARB(fragmentProgram, 1024, NULL, infoLog);
        J2dRlsTraceLn2(J2D_TRACE_WARNING,
            "OGLContext_CreateFragmentProgram: linker msg (%d):\n%s",
                       infoLogLength, infoLog);
    }

    if (!success) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLContext_CreateFragmentProgram: error linking shader");
        j2d_glDeleteObjectARB(fragmentProgram);
        return 0;
    }

    return fragmentProgram;
}

/*
 * Class:     sun_java2d_opengl_OGLContext
 * Method:    getOGLIdString
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_java2d_opengl_OGLContext_getOGLIdString
  (JNIEnv *env, jclass oglcc)
{
    char *vendor, *renderer, *version;
    char *pAdapterId;
    jobject ret = NULL;
    int len;

    J2dTraceLn(J2D_TRACE_INFO, "OGLContext_getOGLIdString");

    vendor = (char*)j2d_glGetString(GL_VENDOR);
    if (vendor == NULL) {
        vendor = "Unknown Vendor";
    }
    renderer = (char*)j2d_glGetString(GL_RENDERER);
    if (renderer == NULL) {
        renderer = "Unknown Renderer";
    }
    version = (char*)j2d_glGetString(GL_VERSION);
    if (version == NULL) {
        version = "unknown version";
    }

    // 'vendor renderer (version)0'
    len = strlen(vendor) + 1 + strlen(renderer) + 1 + 1+strlen(version)+1 + 1;
    pAdapterId = malloc(len);
    if (pAdapterId != NULL) {

        jio_snprintf(pAdapterId, len, "%s %s (%s)", vendor, renderer, version);

        J2dTraceLn1(J2D_TRACE_VERBOSE, "  id=%s", pAdapterId);

        ret = JNU_NewStringPlatform(env, pAdapterId);

        free(pAdapterId);
    }

    return ret;
}

#endif /* !HEADLESS */
