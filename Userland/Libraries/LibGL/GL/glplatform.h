/*
 * Copyright (c) 2021-2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef GLAPI
#    define GLAPI extern
#endif
#define GLAPIENTRY
#define APIENTRY GLAPIENTRY
#ifndef APIENTRYP
#    define APIENTRYP APIENTRY*
#endif

//
// OpenGL typedefs
//
// Defines types used by all OpenGL applications
// https://www.khronos.org/opengl/wiki/OpenGL_Type
typedef char GLchar;
typedef signed char GLbyte;
typedef unsigned char GLuchar;
typedef unsigned char GLubyte;
typedef unsigned char GLboolean;
typedef short GLshort;
typedef unsigned short GLushort;
typedef int GLint;
typedef long GLint64;
typedef long GLintptr;
typedef unsigned int GLuint;
typedef unsigned long GLuint64;
typedef int GLfixed;
typedef int GLsizei;
typedef long GLsizeiptr;
typedef void GLvoid;
typedef float GLfloat;
typedef double GLclampd;
typedef float GLclampf;
typedef double GLdouble;
typedef unsigned int GLenum;
typedef unsigned int GLbitfield;
