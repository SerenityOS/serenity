/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#ifndef GLAPI
#    define GLAPI extern
#endif

// OpenGL related `defines`
#define GL_TRUE 1
#define GL_FALSE 0

// Matrix Modes
#define GL_MODELVIEW 0x0050
#define GL_PROJECTION 0x0051

// glBegin/glEnd primitive types
#define GL_TRIANGLES 0x0100
#define GL_QUADS 0x0101
#define GL_TRIANGLE_FAN 0x0102
#define GL_TRIANGLE_STRIP 0x0103
#define GL_POLYGON 0x0104

// Depth buffer and alpha test compare functions
#define GL_NEVER 0x0200
#define GL_LESS 0x0201
#define GL_EQUAL 0x0202
#define GL_LEQUAL 0x0203
#define GL_GREATER 0x0204
#define GL_NOTEQUAL 0x0205
#define GL_GEQUAL 0x0206
#define GL_ALWAYS 0x0207

// Buffer bits
#define GL_COLOR_BUFFER_BIT 0x0200
#define GL_DEPTH_BUFFER_BIT 0x0400

// Enable capabilities
#define GL_CULL_FACE 0x0B44
#define GL_DEPTH_TEST 0x0B71

// Alpha testing
#define GL_ALPHA_TEST 0x0BC0
#define GL_ALPHA_TEST_REF 0x0BC2
#define GL_ALPHA_TEST_FUNC 0x0BC1

// Alpha blending
#define GL_BLEND 0x0BE2

// Utility
#define GL_VENDOR 0x1F00
#define GL_RENDERER 0x1F01
#define GL_VERSION 0x1F02

// Blend factors
#define GL_ZERO 0
#define GL_ONE 1
#define GL_SRC_COLOR 0x0300
#define GL_ONE_MINUS_SRC_COLOR 0x0301
#define GL_SRC_ALPHA 0x0302
#define GL_ONE_MINUS_SRC_ALPHA 0x0303
#define GL_DST_ALPHA 0x0304
#define GL_ONE_MINUS_DST_ALPHA 0x0305
#define GL_DST_COLOR 0x0306
#define GL_ONE_MINUS_DST_COLOR 0x0307
#define GL_SRC_ALPHA_SATURATE 0x0308

// Sides
#define GL_FRONT_LEFT 0x0400
#define GL_FRONT_RIGHT 0x0401
#define GL_BACK_LEFT 0x0402
#define GL_BACK_RIGHT 0x0403
#define GL_FRONT 0x0404
#define GL_BACK 0x0405
#define GL_LEFT 0x0406
#define GL_RIGHT 0x0407
#define GL_FRONT_AND_BACK 0x0408

// Error codes
#define GL_NO_ERROR 0
#define GL_INVALID_ENUM 0x500
#define GL_INVALID_VALUE 0x501
#define GL_INVALID_OPERATION 0x502
#define GL_STACK_OVERFLOW 0x0503
#define GL_STACK_UNDERFLOW 0x0504
#define GL_OUT_OF_MEMORY 0x505
#define GL_INVALID_FRAMEBUFFER_OPERATION 0x506

// Triangle winding order
#define GL_CW 0x0900
#define GL_CCW 0x0901

// Hint enums
#define GL_DONT_CARE 0x1100
#define GL_FASTEST 0x1101
#define GL_NICEST 0x1102

#define GL_PERSPECTIVE_CORRECTION_HINT 0x0C50
#define GL_POINT_SMOOTH_HINT 0x0C51
#define GL_LINE_SMOOTH_HINT 0x0C52
#define GL_POLYGON_SMOOTH_HINT 0x0C53
#define GL_FOG_HINT 0x0C54
#define GL_GENERATE_MIPMAP_HINT 0x8192
#define GL_TEXTURE_COMPRESSION_HINT 0x84EF

// Listing enums
#define GL_COMPILE 0x1300
#define GL_COMPILE_AND_EXECUTE 0x1301

// Type enums
#define GL_BYTE 0x1400
#define GL_UNSIGNED_BYTE 0x1401
#define GL_SHORT 0x1402
#define GL_UNSIGNED_SHORT 0x1403
#define GL_INT 0x1404
#define GL_UNSIGNED_INT 0x1405
#define GL_FLOAT 0x1406

// Format enums
#define GL_COLOR_INDEX 0x1900
#define GL_LUMINANCE 0x1909
#define GL_LUMINANCE_ALPHA 0x190A
#define GL_BITMAP 0x1A00
#define GL_STENCIL_INDEX 0x1901
#define GL_DEPTH_COMPONENT 0x1902
#define GL_RED 0x1903
#define GL_GREEN 0x1904
#define GL_BLUE 0x1905
#define GL_ALPHA 0x1906
#define GL_RGB 0x1907
#define GL_RGBA 0x1908

// Lighting related defines
#define GL_FLAT 0x1D00
#define GL_SMOOTH 0x1D01

// More blend factors
#define GL_CONSTANT_COLOR 0x8001
#define GL_ONE_MINUS_CONSTANT_COLOR 0x8002
#define GL_CONSTANT_ALPHA 0x8003
#define GL_ONE_MINUS_CONSTANT_ALPHA 0x8004

// Pixel formats
#define GL_RGB 0x1907
#define GL_RGBA 0x1908
#define GL_BGR 0x190B
#define GL_BGRA 0x190C

// Source pixel data format
#define GL_UNSIGNED_BYTE 0x1401

// Texture targets
#define GL_TEXTURE_2D 0x0DE1

// Texture Unit indices
#define GL_TEXTURE0 0x84C0
#define GL_TEXTURE1 0x84C1
#define GL_TEXTURE2 0x84C2
#define GL_TEXTURE3 0x84C3
#define GL_TEXTURE4 0x84C4
#define GL_TEXTURE5 0x84C5
#define GL_TEXTURE6 0x84C6
#define GL_TEXTURE7 0x84C7
#define GL_TEXTURE8 0x84C8
#define GL_TEXTURE9 0x84C9
#define GL_TEXTURE10 0x84CA
#define GL_TEXTURE11 0x84CB
#define GL_TEXTURE12 0x84CC
#define GL_TEXTURE13 0x84CD
#define GL_TEXTURE14 0x84CE
#define GL_TEXTURE15 0x84CF
#define GL_TEXTURE16 0x84D0
#define GL_TEXTURE17 0x84D1
#define GL_TEXTURE18 0x84D2
#define GL_TEXTURE19 0x84D3
#define GL_TEXTURE20 0x84D4
#define GL_TEXTURE21 0x84D5
#define GL_TEXTURE22 0x84D6
#define GL_TEXTURE23 0x84D7
#define GL_TEXTURE24 0x84D8
#define GL_TEXTURE25 0x84D9
#define GL_TEXTURE26 0x84DA
#define GL_TEXTURE27 0x84DB
#define GL_TEXTURE28 0x84DC
#define GL_TEXTURE29 0x84DD
#define GL_TEXTURE30 0x84DE
#define GL_TEXTURE31 0x84DF

// Texture Environment and Parameters
#define GL_NEAREST 0x2600
#define GL_LINEAR 0x2601
#define GL_NEAREST_MIPMAP_LINEAR 0x2602
#define GL_REPEAT 0x2603

//
// OpenGL typedefs
//
// Defines types used by all OpenGL applications
// https://www.khronos.org/opengl/wiki/OpenGL_Type
typedef char GLchar;
typedef unsigned char GLuchar;
typedef unsigned char GLubyte;
typedef short GLshort;
typedef unsigned short GLushort;
typedef int GLint;
typedef unsigned int GLuint;
typedef int GLfixed;
typedef long long GLint64;
typedef unsigned long long GLuint64;
typedef int GLsizei;
typedef void GLvoid;
typedef float GLfloat;
typedef float GLclampf;
typedef double GLdouble;
typedef unsigned int GLenum;
typedef unsigned int GLbitfield;

GLAPI void glBegin(GLenum mode);
GLAPI void glClear(GLbitfield mask);
GLAPI void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
GLAPI void glClearDepth(GLdouble depth);
GLAPI void glColor3f(GLfloat r, GLfloat g, GLfloat b);
GLAPI void glColor4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a);
GLAPI void glColor4fv(const GLfloat* v);
GLAPI void glColor4ub(GLubyte r, GLubyte g, GLubyte b, GLubyte a);
GLAPI void glColor4ubv(const GLubyte* v);
GLAPI void glDeleteTextures(GLsizei n, const GLuint* textures);
GLAPI void glEnd();
GLAPI void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
GLAPI void glGenTextures(GLsizei n, GLuint* textures);
GLAPI GLenum glGetError();
GLAPI GLubyte* glGetString(GLenum name);
GLAPI void glLoadIdentity();
GLAPI void glLoadMatrixf(const GLfloat* matrix);
GLAPI void glMatrixMode(GLenum mode);
GLAPI void glOrtho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
GLAPI void glPushMatrix();
GLAPI void glPopMatrix();
GLAPI void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
GLAPI void glScalef(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glTranslatef(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glVertex2d(GLdouble x, GLdouble y);
GLAPI void glVertex2dv(const GLdouble* v);
GLAPI void glVertex2f(GLfloat x, GLfloat y);
GLAPI void glVertex2fv(const GLfloat* v);
GLAPI void glVertex2i(GLint x, GLint y);
GLAPI void glVertex2iv(const GLint* v);
GLAPI void glVertex2s(GLshort x, GLshort y);
GLAPI void glVertex2sv(const GLshort* v);
GLAPI void glVertex3d(GLdouble x, GLdouble y, GLdouble z);
GLAPI void glVertex3dv(const GLdouble* v);
GLAPI void glVertex3f(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glVertex3fv(const GLfloat* v);
GLAPI void glVertex3i(GLint x, GLint y, GLint z);
GLAPI void glVertex3iv(const GLint* v);
GLAPI void glVertex3s(GLshort x, GLshort y, GLshort z);
GLAPI void glVertex3sv(const GLshort* v);
GLAPI void glVertex4d(GLdouble x, GLdouble y, GLdouble z, GLdouble w);
GLAPI void glVertex4dv(const GLdouble* v);
GLAPI void glVertex4f(GLfloat x, GLfloat y, GLfloat z, GLfloat w);
GLAPI void glVertex4fv(const GLfloat* v);
GLAPI void glVertex4i(GLint x, GLint y, GLint z, GLint w);
GLAPI void glVertex4iv(const GLint* v);
GLAPI void glVertex4s(GLshort x, GLshort y, GLshort z, GLshort w);
GLAPI void glVertex4sv(const GLshort* v);
GLAPI void glViewport(GLint x, GLint y, GLsizei width, GLsizei height);
GLAPI void glEnable(GLenum cap);
GLAPI void glDisable(GLenum cap);
GLAPI void glCullFace(GLenum mode);
GLAPI void glFrontFace(GLenum mode);
GLAPI GLuint glGenLists(GLsizei range);
GLAPI void glCallList(GLuint list);
GLAPI void glDeleteLists(GLuint list, GLsizei range);
GLAPI void glEndList(void);
GLAPI void glNewList(GLuint list, GLenum mode);
GLAPI void glFlush();
GLAPI void glFinish();
GLAPI void glBlendFunc(GLenum sfactor, GLenum dfactor);
GLAPI void glShadeModel(GLenum mode);
GLAPI void glAlphaFunc(GLenum func, GLclampf ref);
GLAPI void glHint(GLenum target, GLenum mode);
GLAPI void glReadBuffer(GLenum mode);
GLAPI void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels);
GLAPI void glTexImage2D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data);
GLAPI void glTexCoord2f(GLfloat s, GLfloat t);
GLAPI void glBindTexture(GLenum target, GLuint texture);
GLAPI void glActiveTexture(GLenum texture);

#ifdef __cplusplus
}
#endif
