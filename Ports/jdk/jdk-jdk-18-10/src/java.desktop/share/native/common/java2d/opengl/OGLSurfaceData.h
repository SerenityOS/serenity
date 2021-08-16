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

#ifndef OGLSurfaceData_h_Included
#define OGLSurfaceData_h_Included

#include "java_awt_image_AffineTransformOp.h"
#include "sun_java2d_opengl_OGLSurfaceData.h"
#include "sun_java2d_pipe_hw_AccelSurface.h"

#include "J2D_GL/gl.h"
#include "SurfaceData.h"
#include "Trace.h"
#include "OGLFuncs.h"

typedef struct _OGLSDOps OGLSDOps;

/**
 * The OGLPixelFormat structure contains all the information OpenGL needs to
 * know when copying from or into a particular system memory image buffer (via
 * glDrawPixels(), glReadPixels, glTexSubImage2D(), etc).
 *
 *     GLenum format;
 * The pixel format parameter used in glDrawPixels() and other similar calls.
 * Indicates the component ordering for each pixel (e.g. GL_BGRA).
 *
 *     GLenum type;
 * The pixel data type parameter used in glDrawPixels() and other similar
 * calls.  Indicates the data type for an entire pixel or for each component
 * in a pixel (e.g. GL_UNSIGNED_BYTE with GL_BGR means a pixel consists of
 * 3 unsigned byte components, blue first, then green, then red;
 * GL_UNSIGNED_INT_8_8_8_8_REV with GL_BGRA means a pixel consists of 1
 * unsigned integer comprised of four byte components, alpha first, then red,
 * then green, then blue).
 *
 *     jint alignment;
 * The byte alignment parameter used in glPixelStorei(GL_UNPACK_ALIGNMENT).  A
 * value of 4 indicates that each pixel starts on a 4-byte aligned region in
 * memory, and so on.  This alignment parameter helps OpenGL speed up pixel
 * transfer operations by transferring memory in aligned blocks.
 *
 *     jboolean hasAlpha;
 * If true, indicates that this pixel format contains an alpha component.
 *
 *     jboolean isPremult;
 * If true, indicates that this pixel format contains color components that
 * have been pre-multiplied by their corresponding alpha component.
 */
typedef struct {
    GLenum   format;
    GLenum   type;
    jint     alignment;
    jboolean hasAlpha;
    jboolean isPremult;
} OGLPixelFormat;

/**
 * The OGLSDOps structure describes a native OpenGL surface and contains all
 * information pertaining to the native surface.  Some information about
 * the more important/different fields:
 *
 *     void *privOps;
 * Pointer to native-specific (GLX, WGL, etc.) SurfaceData info, such as the
 * native Drawable handle and GraphicsConfig data.
 *
 *     jobject graphicsConfig;;
 * Strong reference to the OGLGraphicsConfig used by this OGLSurfaceData.
 *
 *     jint drawableType;
 * The surface type; can be any one of the surface type constants defined
 * below (OGLSD_WINDOW, OGLSD_TEXTURE, etc).
 *
 *     GLenum activeBuffer;
 * Can be either GL_FRONT if this is the front buffer surface of an onscreen
 * window or a pbuffer surface, or GL_BACK if this is the backbuffer surface
 * of an onscreen window.
 *
 *     jboolean isOpaque;
 * If true, the surface should be treated as being fully opaque.  If
 * the underlying surface (e.g. pbuffer) has an alpha channel and isOpaque
 * is true, then we should take appropriate action (i.e. call glColorMask()
 * to disable writes into the alpha channel) to ensure that the surface
 * remains fully opaque.
 *
 *     jboolean needsInit;
 * If true, the surface requires some one-time initialization, which should
 * be performed after a context has been made current to the surface for
 * the first time.
 *
 *     jint x/yOffset
 * The offset in pixels of the OpenGL viewport origin from the lower-left
 * corner of the heavyweight drawable.  For example, a top-level frame on
 * Windows XP has lower-left insets of (4,4).  The OpenGL viewport origin
 * would typically begin at the lower-left corner of the client region (inside
 * the frame decorations), but AWT/Swing will take the insets into account
 * when rendering into that window.  So in order to account for this, we
 * need to adjust the OpenGL viewport origin by an x/yOffset of (-4,-4).  On
 * X11, top-level frames typically don't have this insets issue, so their
 * x/yOffset would be (0,0) (the same applies to pbuffers).
 *
 *     jint width/height;
 * The cached surface bounds.  For offscreen surface types (OGLSD_FBOBJECT,
 * OGLSD_TEXTURE, etc.) these values must remain constant.  Onscreen window
 * surfaces (OGLSD_WINDOW, OGLSD_FLIP_BACKBUFFER, etc.) may have their
 * bounds changed in response to a programmatic or user-initiated event, so
 * these values represent the last known dimensions.  To determine the true
 * current bounds of this surface, query the native Drawable through the
 * privOps field.
 *
 *     GLuint textureID;
 * The texture object handle, as generated by glGenTextures().  If this value
 * is zero, the texture has not yet been initialized.
 *
 *     jint textureWidth/Height;
 * The actual bounds of the texture object for this surface.  If the
 * GL_ARB_texture_non_power_of_two extension is not present, the dimensions
 * of an OpenGL texture object must be a power-of-two (e.g. 64x32 or 128x512).
 * The texture image that we care about has dimensions specified by the width
 * and height fields in this OGLSDOps structure.  For example, if the image
 * to be stored in the texture has dimensions 115x47, the actual OpenGL
 * texture we allocate will have dimensions 128x64 to meet the pow2
 * restriction.  The image bounds within the texture can be accessed using
 * floating point texture coordinates in the range [0.0,1.0].
 *
 *     GLenum textureTarget;
 * The texture target of the texture object for this surface.  If this
 * surface is not backed by a texture, this value is set to zero.  Otherwise,
 * this value is GL_TEXTURE_RECTANGLE_ARB when the GL_ARB_texture_rectangle
 * extension is in use; if not, it is set to GL_TEXTURE_2D.
 *
 *     GLint textureFilter;
 * The current filter state for this texture object (can be either GL_NEAREST
 * or GL_LINEAR).  We cache this value here and check it before updating
 * the filter state to avoid redundant calls to glTexParameteri() when the
 * filter state remains constant (see the OGLSD_UPDATE_TEXTURE_FILTER()
 * macro below).
 *
 *     GLuint fbobjectID, depthID;
 * The object handles for the framebuffer object and depth renderbuffer
 * associated with this surface.  These fields are only used when
 * drawableType is OGLSD_FBOBJECT, otherwise they are zero.
 */
struct _OGLSDOps {
    SurfaceDataOps               sdOps;
    void                         *privOps;
    jobject                      graphicsConfig;
    jint                         drawableType;
    GLenum                       activeBuffer;
    jboolean                     isOpaque;
    jboolean                     needsInit;
    jint                         xOffset;
    jint                         yOffset;
    jint                         width;
    jint                         height;
    GLuint                       textureID;
    jint                         textureWidth;
    jint                         textureHeight;
    GLenum                       textureTarget;
    GLint                        textureFilter;
    GLuint                       fbobjectID;
    GLuint                       depthID;
};

/**
 * The following convenience macros are used when rendering rectangles (either
 * a single rectangle, or a whole series of them).  To render a single
 * rectangle, simply invoke the GLRECT() macro.  To render a whole series of
 * rectangles, such as spans in a complex shape, first invoke GLRECT_BEGIN(),
 * then invoke the appropriate inner loop macro (either XYXY or XYWH) for
 * each rectangle, and finally invoke GLRECT_END() to notify OpenGL that the
 * vertex list is complete.  Care should be taken to avoid calling OpenGL
 * commands (besides GLRECT_BODY_*()) inside the BEGIN/END pair.
 */

#define GLRECT_BEGIN j2d_glBegin(GL_QUADS)

#define GLRECT_BODY_XYXY(x1, y1, x2, y2) \
    do { \
        j2d_glVertex2i(x1, y1); \
        j2d_glVertex2i(x2, y1); \
        j2d_glVertex2i(x2, y2); \
        j2d_glVertex2i(x1, y2); \
    } while (0)

#define GLRECT_BODY_XYWH(x, y, w, h) \
    GLRECT_BODY_XYXY(x, y, (x) + (w), (y) + (h))

#define GLRECT_END j2d_glEnd()

#define GLRECT(x, y, w, h) \
    do { \
        GLRECT_BEGIN; \
        GLRECT_BODY_XYWH(x, y, w, h); \
        GLRECT_END; \
    } while (0)

/**
 * These are shorthand names for the surface type constants defined in
 * OGLSurfaceData.java.
 */
#define OGLSD_UNDEFINED       sun_java2d_pipe_hw_AccelSurface_UNDEFINED
#define OGLSD_WINDOW          sun_java2d_pipe_hw_AccelSurface_WINDOW
#define OGLSD_TEXTURE         sun_java2d_pipe_hw_AccelSurface_TEXTURE
#define OGLSD_FLIP_BACKBUFFER sun_java2d_pipe_hw_AccelSurface_FLIP_BACKBUFFER
#define OGLSD_FBOBJECT        sun_java2d_pipe_hw_AccelSurface_RT_TEXTURE

/**
 * These are shorthand names for the filtering method constants used by
 * image transform methods.
 */
#define OGLSD_XFORM_DEFAULT 0
#define OGLSD_XFORM_NEAREST_NEIGHBOR \
    java_awt_image_AffineTransformOp_TYPE_NEAREST_NEIGHBOR
#define OGLSD_XFORM_BILINEAR \
    java_awt_image_AffineTransformOp_TYPE_BILINEAR

/**
 * Helper macros that update the current texture filter state only when
 * it needs to be changed, which helps reduce overhead for small texturing
 * operations.  The filter state is set on a per-texture (not per-context)
 * basis; for example, it is possible for one texture to be using GL_NEAREST
 * while another texture uses GL_LINEAR under the same context.
 */
#define OGLSD_INIT_TEXTURE_FILTER(oglSDOps, filter)                          \
    do {                                                                     \
        j2d_glTexParameteri((oglSDOps)->textureTarget,                       \
                            GL_TEXTURE_MAG_FILTER, (filter));                \
        j2d_glTexParameteri((oglSDOps)->textureTarget,                       \
                            GL_TEXTURE_MIN_FILTER, (filter));                \
        (oglSDOps)->textureFilter = (filter);                                \
    } while (0)

#define OGLSD_UPDATE_TEXTURE_FILTER(oglSDOps, filter)    \
    do {                                                 \
        if ((oglSDOps)->textureFilter != (filter)) {     \
            OGLSD_INIT_TEXTURE_FILTER(oglSDOps, filter); \
        }                                                \
    } while (0)

/**
 * Convenience macros for setting the texture wrap mode for a given target.
 * The texture wrap mode should be reset to our default value of
 * GL_CLAMP_TO_EDGE by calling OGLSD_RESET_TEXTURE_WRAP() when a texture
 * is first created.  If another mode is needed (e.g. GL_REPEAT in the case
 * of TexturePaint acceleration), one can call the OGLSD_UPDATE_TEXTURE_WRAP()
 * macro to easily set up the new wrap mode.  However, it is important to
 * restore the wrap mode back to its default value (by calling the
 * OGLSD_RESET_TEXTURE_WRAP() macro) when the operation is finished.
 */
#define OGLSD_UPDATE_TEXTURE_WRAP(target, wrap)                   \
    do {                                                          \
        j2d_glTexParameteri((target), GL_TEXTURE_WRAP_S, (wrap)); \
        j2d_glTexParameteri((target), GL_TEXTURE_WRAP_T, (wrap)); \
    } while (0)

#define OGLSD_RESET_TEXTURE_WRAP(target) \
    OGLSD_UPDATE_TEXTURE_WRAP(target, GL_CLAMP_TO_EDGE)

/**
 * Exported methods.
 */
jint OGLSD_Lock(JNIEnv *env,
                SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo,
                jint lockflags);
void OGLSD_GetRasInfo(JNIEnv *env,
                      SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo);
void OGLSD_Unlock(JNIEnv *env,
                  SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo);
void OGLSD_Dispose(JNIEnv *env, SurfaceDataOps *ops);
void OGLSD_Delete(JNIEnv *env, OGLSDOps *oglsdo);
jint OGLSD_NextPowerOfTwo(jint val, jint max);
jboolean OGLSD_InitFBObject(GLuint *fbobjectID, GLuint *depthID,
                            GLuint textureID, GLenum textureTarget,
                            jint textureWidth, jint textureHeight);

#endif /* OGLSurfaceData_h_Included */
