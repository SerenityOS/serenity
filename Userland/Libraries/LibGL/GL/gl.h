/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
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

// Culled face side
#define GL_FRONT 0x0404
#define GL_BACK 0x0405
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

// Lighting related defines
#define GL_FLAT 0x1D00
#define GL_SMOOTH 0x1D01

// More blend factors
#define GL_CONSTANT_COLOR 0x8001
#define GL_ONE_MINUS_CONSTANT_COLOR 0x8002
#define GL_CONSTANT_ALPHA 0x8003
#define GL_ONE_MINUS_CONSTANT_ALPHA 0x8004

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
typedef unsigned long GLsizei;
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
GLAPI void glEnd();
GLAPI void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
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

#ifdef __cplusplus
}
#endif
