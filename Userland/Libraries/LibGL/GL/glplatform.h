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
typedef unsigned int GLuint;
typedef int GLfixed;
typedef int GLsizei;
typedef void GLvoid;
typedef float GLfloat;
typedef double GLclampd;
typedef float GLclampf;
typedef double GLdouble;
typedef unsigned int GLenum;
typedef unsigned int GLbitfield;

#if defined(__x86_64__) || defined(__aarch64__)
typedef long GLint64;
typedef unsigned long GLuint64;
#else
typedef long long GLint64;
typedef unsigned long long GLuint64;
#endif
