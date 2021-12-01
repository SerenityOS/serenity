/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
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

void glVertex2d(GLdouble x, GLdouble y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2dv(const GLdouble* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2f(GLfloat x, GLfloat y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2fv(const GLfloat* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2i(GLint x, GLint y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2iv(const GLint* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2s(GLshort x, GLshort y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2sv(const GLshort* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex3d(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3dv(const GLdouble* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3f(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3fv(const GLfloat* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3i(GLint x, GLint y, GLint z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3iv(const GLint* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3s(GLshort x, GLshort y, GLshort z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3sv(const GLshort* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex4d(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4dv(const GLdouble* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4f(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4fv(const GLfloat* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4i(GLint x, GLint y, GLint z, GLint w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4iv(const GLint* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4s(GLshort x, GLshort y, GLshort z, GLshort w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4sv(const GLshort* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glTexCoord2f(GLfloat s, GLfloat t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 0.0f);
}

void glTexCoord2fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], 0.0f, 0.0f);
}

void glTexCoord4fv(const GLfloat* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], v[2], v[3]);
}

void glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz)
{
    g_gl_context->gl_normal(nx, ny, nz);
}

void glNormal3fv(GLfloat const* v)
{
    g_gl_context->gl_normal(v[0], v[1], v[2]);
}
