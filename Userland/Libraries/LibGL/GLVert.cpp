/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glBegin(GLenum mode)
{
    g_gl_context->gl_begin(mode);
}

void glEnd()
{
    g_gl_context->gl_end();
}

void glVertex3f(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3fv(const GLfloat* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_rotate(angle, x, y, z);
}

void glScalef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_scale(x, y, z);
}

void glTranslatef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_translate(x, y, z);
}
