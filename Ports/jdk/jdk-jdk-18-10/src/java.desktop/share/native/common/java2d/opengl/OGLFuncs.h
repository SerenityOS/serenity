/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OGLFuncs_h_Included
#define OGLFuncs_h_Included

#ifdef MACOSX
#include <dlfcn.h>
#endif
#include "jni.h"
#include "J2D_GL/gl.h"
#include "J2D_GL/glext.h"
#include "OGLFuncMacros.h"
#include "OGLFuncs_md.h"
#include "Trace.h"

jboolean OGLFuncs_OpenLibrary();
void     OGLFuncs_CloseLibrary();
jboolean OGLFuncs_InitPlatformFuncs();
jboolean OGLFuncs_InitBaseFuncs();
jboolean OGLFuncs_InitExtFuncs();

/**
 * Core OpenGL 1.1 function typedefs
 */
typedef void (GLAPIENTRY *glAlphaFuncType)(GLenum func, GLclampf ref);
typedef GLboolean (GLAPIENTRY *glAreTexturesResidentType)(GLsizei n, const GLuint *textures, GLboolean *residences);
typedef void (GLAPIENTRY *glBeginType)(GLenum mode);
typedef void (GLAPIENTRY *glBindTextureType)(GLenum target, GLuint texture);
typedef void (GLAPIENTRY *glBitmapType)(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, const GLubyte *bitmap);
typedef void (GLAPIENTRY *glBlendFuncType)(GLenum sfactor, GLenum dfactor);
typedef void (GLAPIENTRY *glClearType)(GLbitfield mask);
typedef void (GLAPIENTRY *glClearColorType)(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
typedef void (GLAPIENTRY *glClearDepthType)(GLclampd depth);
typedef void (GLAPIENTRY *glColor3ubType)(GLubyte red, GLubyte green, GLubyte blue);
typedef void (GLAPIENTRY *glColor4fType)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);
typedef void (GLAPIENTRY *glColor4ubType)(GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha);
typedef void (GLAPIENTRY *glColorMaskType)(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
typedef void (GLAPIENTRY *glColorPointerType)(GLint size, GLenum type, GLsizei stride, const GLvoid *ptr);
typedef void (GLAPIENTRY *glCopyPixelsType)(GLint x, GLint y, GLsizei width, GLsizei height, GLenum type);
typedef void (GLAPIENTRY *glCopyTexSubImage2DType)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
typedef void (GLAPIENTRY *glDeleteTexturesType)(GLsizei n, const GLuint *textures);
typedef void (GLAPIENTRY *glDepthFuncType)(GLenum func);
typedef void (GLAPIENTRY *glDisableType)(GLenum cap);
typedef void (GLAPIENTRY *glDisableClientStateType)(GLenum array);
typedef void (GLAPIENTRY *glDrawArraysType)(GLenum mode, GLint first, GLsizei count);
typedef void (GLAPIENTRY *glDrawBufferType)(GLenum mode);
typedef void (GLAPIENTRY *glDrawPixelsType)(GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRY *glEnableType)(GLenum cap);
typedef void (GLAPIENTRY *glEnableClientStateType)(GLenum array);
typedef void (GLAPIENTRY *glEndType)(void);
typedef void (GLAPIENTRY *glFinishType)(void);
typedef void (GLAPIENTRY *glFlushType)(void);
typedef void (GLAPIENTRY *glGenTexturesType)(GLsizei n, GLuint *textures);
typedef void (GLAPIENTRY *glGetBooleanvType)(GLenum pname, GLboolean *params);
typedef void (GLAPIENTRY *glGetDoublevType)(GLenum pname, GLdouble *params);
typedef GLenum (GLAPIENTRY *glGetErrorType)(void);
typedef void (GLAPIENTRY *glGetFloatvType)(GLenum pname, GLfloat *params);
typedef void (GLAPIENTRY *glGetIntegervType)(GLenum pname, GLint *params);
typedef const GLubyte * (GLAPIENTRY *glGetStringType)(GLenum name);
typedef void (GLAPIENTRY *glGetTexLevelParameterivType)(GLenum target, GLint level, GLenum pname, GLint *params);
typedef void (GLAPIENTRY *glHintType)(GLenum target, GLenum mode);
typedef void (GLAPIENTRY *glInterleavedArraysType)(GLenum format, GLsizei stride, const GLvoid *pointer);
typedef GLboolean (GLAPIENTRY *glIsEnabledType)(GLenum cap);
typedef GLboolean (GLAPIENTRY *glIsTextureType)(GLuint texture);
typedef void (GLAPIENTRY *glLoadIdentityType)(void);
typedef void (GLAPIENTRY *glLoadMatrixdType)(const GLdouble *m);
typedef void (GLAPIENTRY *glLogicOpType)(GLenum opcode);
typedef void (GLAPIENTRY *glMatrixModeType)(GLenum mode);
typedef void (GLAPIENTRY *glOrthoType)(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar);
typedef void (GLAPIENTRY *glPixelStoreiType)(GLenum pname, GLint param);
typedef void (GLAPIENTRY *glPixelTransferfType)(GLenum pname, GLfloat param);
typedef void (GLAPIENTRY *glPixelZoomType)(GLfloat xfactor, GLfloat yfactor);
typedef void (GLAPIENTRY *glPolygonOffsetType)(GLfloat factor, GLfloat units);
typedef void (GLAPIENTRY *glPopAttribType)(void);
typedef void (GLAPIENTRY *glPopClientAttribType)(void);
typedef void (GLAPIENTRY *glPopMatrixType)(void);
typedef void (GLAPIENTRY *glPrioritizeTexturesType)(GLsizei n, const GLuint *textures, const GLclampf *priorities);
typedef void (GLAPIENTRY *glPushAttribType)(GLbitfield);
typedef void (GLAPIENTRY *glPushClientAttribType)(GLbitfield);
typedef void (GLAPIENTRY *glPushMatrixType)(void);
typedef void (GLAPIENTRY *glRasterPos2iType)(GLint x, GLint y);
typedef void (GLAPIENTRY *glReadBufferType)(GLenum mode);
typedef void (GLAPIENTRY *glReadPixelsType)(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels);
typedef void (GLAPIENTRY *glRectiType)(GLint x1, GLint y1, GLint x2, GLint y2);
typedef void (GLAPIENTRY *glScalefType)(GLfloat x, GLfloat y, GLfloat z);
typedef void (GLAPIENTRY *glScissorType)(GLint x, GLint y, GLsizei width, GLsizei height);
typedef void (GLAPIENTRY *glTexCoord2dType)(GLdouble s, GLdouble t);
typedef void (GLAPIENTRY *glTexCoord2fType)(GLfloat s, GLfloat t);
typedef void (GLAPIENTRY *glTexCoordPointerType)(GLint size, GLenum type, GLsizei stride, const GLvoid *ptr);
typedef void (GLAPIENTRY *glTexEnviType)(GLenum target, GLenum pname, GLint param);
typedef void (GLAPIENTRY *glTexGeniType)(GLenum coord, GLenum pname, GLint param);
typedef void (GLAPIENTRY *glTexGendvType)(GLenum coord, GLenum pname, const GLdouble *params);
typedef void (GLAPIENTRY *glTexImage1DType)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRY *glTexImage2DType)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRY *glTexParameteriType)(GLenum target, GLenum pname, GLint param);
typedef void (GLAPIENTRY *glTexSubImage1DType)(GLenum target, GLint level, GLint xoffset, GLsizei width, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRY *glTexSubImage2DType)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRY *glTranslatefType)(GLfloat x, GLfloat y, GLfloat z);
typedef void (GLAPIENTRY *glVertex2dType)(GLdouble x, GLdouble y);
typedef void (GLAPIENTRY *glVertex2fType)(GLfloat x, GLfloat y);
typedef void (GLAPIENTRY *glVertex2iType)(GLint x, GLint y);
typedef void (GLAPIENTRY *glVertexPointerType)(GLint size, GLenum type, GLsizei stride, const GLvoid *ptr);
typedef void (GLAPIENTRY *glViewportType)(GLint x, GLint y, GLsizei width, GLsizei height);

/**
 * OpenGL 1.2 and extension function typedefs (functions that were added in
 * the 1.2 spec and later need to be loaded on Windows as if they were
 * extensions, which is why they are called out separately here)
 */
typedef void (GLAPIENTRY *glActiveTextureARBType)(GLenum texture);
typedef void (GLAPIENTRY *glMultiTexCoord2fARBType)(GLenum texture, GLfloat s, GLfloat t);
typedef void (GLAPIENTRY *glTexImage3DType)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const GLvoid *pixels);

/**
 * GL_EXT_framebuffer_object function typedefs
 */
typedef void (GLAPIENTRY *glBindRenderbufferEXTType)(GLenum, GLuint);
typedef void (GLAPIENTRY *glDeleteRenderbuffersEXTType)(GLsizei, const GLuint *);
typedef void (GLAPIENTRY *glGenRenderbuffersEXTType)(GLsizei, GLuint *);
typedef void (GLAPIENTRY *glRenderbufferStorageEXTType)(GLenum, GLenum, GLsizei, GLsizei);
typedef void (GLAPIENTRY *glBindFramebufferEXTType)(GLenum, GLuint);
typedef void (GLAPIENTRY *glDeleteFramebuffersEXTType)(GLsizei, const GLuint *);
typedef void (GLAPIENTRY *glGenFramebuffersEXTType)(GLsizei, GLuint *);
typedef GLenum (GLAPIENTRY *glCheckFramebufferStatusEXTType)(GLenum);
typedef void (GLAPIENTRY *glFramebufferTexture2DEXTType)(GLenum, GLenum, GLenum, GLuint, GLint);
typedef void (GLAPIENTRY *glFramebufferRenderbufferEXTType)(GLenum, GLenum, GLenum, GLuint);

/**
 * GL_ARB_fragment_shader extension function typedefs
 */
typedef GLhandleARB (GLAPIENTRY *glCreateShaderObjectARBType)(GLenum);
typedef void (GLAPIENTRY *glShaderSourceARBType)(GLhandleARB, GLsizei, const GLcharARB* *, const GLint *);
typedef void (GLAPIENTRY *glCompileShaderARBType)(GLhandleARB);
typedef void (GLAPIENTRY *glUseProgramObjectARBType)(GLhandleARB);
typedef void (GLAPIENTRY *glUniform1iARBType)(GLint, GLint);
typedef void (GLAPIENTRY *glUniform1fARBType)(GLint, GLfloat);
typedef void (GLAPIENTRY *glUniform1fvARBType)(GLint, GLsizei, const GLfloat *);
typedef void (GLAPIENTRY *glUniform2fARBType)(GLint, GLfloat, GLfloat);
typedef void (GLAPIENTRY *glUniform3fARBType)(GLint, GLfloat, GLfloat, GLfloat);
typedef void (GLAPIENTRY *glUniform3fvARBType)(GLint, GLsizei, const GLfloat *);
typedef void (GLAPIENTRY *glUniform4fARBType)(GLint, GLfloat, GLfloat, GLfloat, GLfloat);
typedef void (GLAPIENTRY *glUniform4fvARBType)(GLint, GLsizei, const GLfloat *);
typedef GLint (GLAPIENTRY *glGetUniformLocationARBType)(GLhandleARB, const GLcharARB *);
typedef void (GLAPIENTRY *glGetInfoLogARBType)(GLhandleARB, GLsizei, GLsizei *, GLcharARB *);
typedef void (GLAPIENTRY *glGetProgramivARBType)(GLenum, GLenum, GLint *);
typedef void (GLAPIENTRY *glGetObjectParameterivARBType)(GLhandleARB, GLenum, GLint *);
typedef GLhandleARB (GLAPIENTRY *glCreateProgramObjectARBType)(void);
typedef void (GLAPIENTRY *glAttachObjectARBType)(GLhandleARB, GLhandleARB);
typedef void (GLAPIENTRY *glLinkProgramARBType)(GLhandleARB);
typedef void (GLAPIENTRY *glDeleteObjectARBType)(GLhandleARB);

/**
 * GL_NV_texture_barrier extension function typedef's
 */
typedef void (GLAPIENTRY *glTextureBarrierNVType) (void);

/**
 * REMIND: this caused an internal error in the MS compiler!?!?
 *
 *#define OGL_CHECK_FUNC_ERR(f) \
 *    J2dTrace1(J2D_TRACE_ERROR, "could not load function: %s", #f)
 */

#define OGL_CHECK_FUNC_ERR(f) \
    J2dRlsTraceLn(J2D_TRACE_ERROR, #f)

#define OGL_INIT_FUNC(f) \
    OGL_J2D_MANGLE(f) = (OGL_FUNC_TYPE(f)) OGL_GET_PROC_ADDRESS(f)

#define OGL_INIT_AND_CHECK_FUNC(f) \
    OGL_INIT_FUNC(f); \
    if (OGL_J2D_MANGLE(f) == NULL) { \
        OGL_CHECK_FUNC_ERR(f); \
        return JNI_FALSE; \
    }

#define OGL_INIT_EXT_FUNC(f) \
    OGL_J2D_MANGLE(f) = (OGL_FUNC_TYPE(f)) OGL_GET_EXT_PROC_ADDRESS(f)

#define OGL_INIT_AND_CHECK_EXT_FUNC(f) \
    OGL_INIT_EXT_FUNC(f); \
    if (OGL_J2D_MANGLE(f) == NULL) { \
        OGL_CHECK_FUNC_ERR(f); \
        return JNI_FALSE; \
    }

#define OGL_EXPRESS_BASE_FUNCS(action) \
    OGL_##action##_FUNC(glAlphaFunc); \
    OGL_##action##_FUNC(glAreTexturesResident); \
    OGL_##action##_FUNC(glBegin); \
    OGL_##action##_FUNC(glBindTexture); \
    OGL_##action##_FUNC(glBitmap); \
    OGL_##action##_FUNC(glBlendFunc); \
    OGL_##action##_FUNC(glClear); \
    OGL_##action##_FUNC(glClearColor); \
    OGL_##action##_FUNC(glClearDepth); \
    OGL_##action##_FUNC(glColor3ub); \
    OGL_##action##_FUNC(glColor4f); \
    OGL_##action##_FUNC(glColor4ub); \
    OGL_##action##_FUNC(glColorMask); \
    OGL_##action##_FUNC(glColorPointer); \
    OGL_##action##_FUNC(glCopyPixels); \
    OGL_##action##_FUNC(glCopyTexSubImage2D); \
    OGL_##action##_FUNC(glDeleteTextures); \
    OGL_##action##_FUNC(glDepthFunc); \
    OGL_##action##_FUNC(glDisable); \
    OGL_##action##_FUNC(glDisableClientState); \
    OGL_##action##_FUNC(glDrawArrays); \
    OGL_##action##_FUNC(glDrawBuffer); \
    OGL_##action##_FUNC(glDrawPixels); \
    OGL_##action##_FUNC(glEnable); \
    OGL_##action##_FUNC(glEnableClientState); \
    OGL_##action##_FUNC(glEnd); \
    OGL_##action##_FUNC(glFinish); \
    OGL_##action##_FUNC(glFlush); \
    OGL_##action##_FUNC(glGenTextures); \
    OGL_##action##_FUNC(glGetBooleanv); \
    OGL_##action##_FUNC(glGetDoublev); \
    OGL_##action##_FUNC(glGetError); \
    OGL_##action##_FUNC(glGetFloatv); \
    OGL_##action##_FUNC(glGetIntegerv); \
    OGL_##action##_FUNC(glGetString); \
    OGL_##action##_FUNC(glGetTexLevelParameteriv); \
    OGL_##action##_FUNC(glHint); \
    OGL_##action##_FUNC(glInterleavedArrays); \
    OGL_##action##_FUNC(glIsEnabled); \
    OGL_##action##_FUNC(glIsTexture); \
    OGL_##action##_FUNC(glLoadIdentity); \
    OGL_##action##_FUNC(glLoadMatrixd); \
    OGL_##action##_FUNC(glLogicOp); \
    OGL_##action##_FUNC(glMatrixMode); \
    OGL_##action##_FUNC(glOrtho); \
    OGL_##action##_FUNC(glPixelStorei); \
    OGL_##action##_FUNC(glPixelTransferf); \
    OGL_##action##_FUNC(glPixelZoom); \
    OGL_##action##_FUNC(glPolygonOffset); \
    OGL_##action##_FUNC(glPopAttrib); \
    OGL_##action##_FUNC(glPopClientAttrib); \
    OGL_##action##_FUNC(glPopMatrix); \
    OGL_##action##_FUNC(glPrioritizeTextures); \
    OGL_##action##_FUNC(glPushAttrib); \
    OGL_##action##_FUNC(glPushClientAttrib); \
    OGL_##action##_FUNC(glPushMatrix); \
    OGL_##action##_FUNC(glRasterPos2i); \
    OGL_##action##_FUNC(glReadBuffer); \
    OGL_##action##_FUNC(glReadPixels); \
    OGL_##action##_FUNC(glRecti); \
    OGL_##action##_FUNC(glScalef); \
    OGL_##action##_FUNC(glScissor); \
    OGL_##action##_FUNC(glTexCoord2d); \
    OGL_##action##_FUNC(glTexCoord2f); \
    OGL_##action##_FUNC(glTexCoordPointer); \
    OGL_##action##_FUNC(glTexEnvi); \
    OGL_##action##_FUNC(glTexGeni); \
    OGL_##action##_FUNC(glTexGendv); \
    OGL_##action##_FUNC(glTexImage1D); \
    OGL_##action##_FUNC(glTexImage2D); \
    OGL_##action##_FUNC(glTexParameteri); \
    OGL_##action##_FUNC(glTexSubImage1D); \
    OGL_##action##_FUNC(glTexSubImage2D); \
    OGL_##action##_FUNC(glTranslatef); \
    OGL_##action##_FUNC(glVertex2d); \
    OGL_##action##_FUNC(glVertex2f); \
    OGL_##action##_FUNC(glVertex2i); \
    OGL_##action##_FUNC(glVertexPointer); \
    OGL_##action##_FUNC(glViewport);

#define OGL_EXPRESS_EXT_FUNCS(action) \
    OGL_##action##_EXT_FUNC(glActiveTextureARB); \
    OGL_##action##_EXT_FUNC(glMultiTexCoord2fARB); \
    OGL_##action##_EXT_FUNC(glTexImage3D); \
    OGL_##action##_EXT_FUNC(glBindRenderbufferEXT); \
    OGL_##action##_EXT_FUNC(glDeleteRenderbuffersEXT); \
    OGL_##action##_EXT_FUNC(glGenRenderbuffersEXT); \
    OGL_##action##_EXT_FUNC(glRenderbufferStorageEXT); \
    OGL_##action##_EXT_FUNC(glBindFramebufferEXT); \
    OGL_##action##_EXT_FUNC(glDeleteFramebuffersEXT); \
    OGL_##action##_EXT_FUNC(glGenFramebuffersEXT); \
    OGL_##action##_EXT_FUNC(glCheckFramebufferStatusEXT); \
    OGL_##action##_EXT_FUNC(glFramebufferTexture2DEXT); \
    OGL_##action##_EXT_FUNC(glFramebufferRenderbufferEXT); \
    OGL_##action##_EXT_FUNC(glCreateProgramObjectARB); \
    OGL_##action##_EXT_FUNC(glAttachObjectARB); \
    OGL_##action##_EXT_FUNC(glLinkProgramARB); \
    OGL_##action##_EXT_FUNC(glCreateShaderObjectARB); \
    OGL_##action##_EXT_FUNC(glShaderSourceARB); \
    OGL_##action##_EXT_FUNC(glCompileShaderARB); \
    OGL_##action##_EXT_FUNC(glUseProgramObjectARB); \
    OGL_##action##_EXT_FUNC(glUniform1iARB); \
    OGL_##action##_EXT_FUNC(glUniform1fARB); \
    OGL_##action##_EXT_FUNC(glUniform1fvARB); \
    OGL_##action##_EXT_FUNC(glUniform2fARB); \
    OGL_##action##_EXT_FUNC(glUniform3fARB); \
    OGL_##action##_EXT_FUNC(glUniform3fvARB); \
    OGL_##action##_EXT_FUNC(glUniform4fARB); \
    OGL_##action##_EXT_FUNC(glUniform4fvARB); \
    OGL_##action##_EXT_FUNC(glGetUniformLocationARB); \
    OGL_##action##_EXT_FUNC(glGetProgramivARB); \
    OGL_##action##_EXT_FUNC(glGetInfoLogARB); \
    OGL_##action##_EXT_FUNC(glGetObjectParameterivARB); \
    OGL_##action##_EXT_FUNC(glDeleteObjectARB); \
    OGL_##action##_EXT_FUNC(glTextureBarrierNV);

#define OGL_EXPRESS_ALL_FUNCS(action) \
    OGL_EXPRESS_BASE_FUNCS(action) \
    OGL_EXPRESS_EXT_FUNCS(action) \
    OGL_EXPRESS_PLATFORM_FUNCS(action) \
    OGL_EXPRESS_PLATFORM_EXT_FUNCS(action)

OGL_EXPRESS_ALL_FUNCS(EXTERN)

#endif /* OGLFuncs_h_Included */
