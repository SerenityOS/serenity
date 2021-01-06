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

// Buffer bits
#define GL_COLOR_BUFFER_BIT 0x0200

// Utility
#define GL_VENDOR 0x1F00
#define GL_RENDERER 0x1F01
#define GL_VERSION 0x1F02

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
GLAPI void glColor3f(GLfloat r, GLfloat g, GLfloat b);
GLAPI void glEnd();
GLAPI void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
GLAPI GLubyte* glGetString(GLenum name);
GLAPI void glLoadIdentity();
GLAPI void glMatrixMode(GLenum mode);
GLAPI void glPushMatrix();
GLAPI void glPopMatrix();
GLAPI void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
GLAPI void glTranslatef(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glVertex3f(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glViewport(GLint x, GLint y, GLsizei width, GLsizei height);

#ifdef __cplusplus
}
#endif
