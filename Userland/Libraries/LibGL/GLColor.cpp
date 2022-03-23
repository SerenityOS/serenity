/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glColor3d(GLdouble r, GLdouble g, GLdouble b)
{
    g_gl_context->gl_color(r, g, b, 1.0);
}

void glColor3dv(GLdouble const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], 1.0);
}

void glColor3f(GLfloat r, GLfloat g, GLfloat b)
{
    g_gl_context->gl_color(r, g, b, 1.0);
}

void glColor3fv(const GLfloat* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], 1.0);
}

void glColor3ub(GLubyte r, GLubyte g, GLubyte b)
{
    g_gl_context->gl_color(r / 255.0, g / 255.0, b / 255.0, 1.0);
}

void glColor3ubv(GLubyte const* v)
{
    g_gl_context->gl_color(v[0] / 255.0, v[1] / 255.0, v[2] / 255.0, 1.0);
}

void glColor4b(GLbyte r, GLbyte g, GLbyte b, GLbyte a)
{
    g_gl_context->gl_color(
        (static_cast<double>(r) + 128) / 127.5 - 1,
        (static_cast<double>(g) + 128) / 127.5 - 1,
        (static_cast<double>(b) + 128) / 127.5 - 1,
        (static_cast<double>(a) + 128) / 127.5 - 1);
}

void glColor4dv(GLdouble const* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], v[3]);
}

void glColor4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a)
{
    g_gl_context->gl_color(r, g, b, a);
}

void glColor4fv(const GLfloat* v)
{
    g_gl_context->gl_color(v[0], v[1], v[2], v[3]);
}

void glColor4ub(GLubyte r, GLubyte g, GLubyte b, GLubyte a)
{
    g_gl_context->gl_color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
}

void glColor4ubv(const GLubyte* v)
{
    g_gl_context->gl_color(v[0] / 255.0, v[1] / 255.0, v[2] / 255.0, v[3] / 255.0);
}
