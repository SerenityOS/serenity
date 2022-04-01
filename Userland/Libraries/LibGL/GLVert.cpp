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

void glVertex2dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2f(GLfloat x, GLfloat y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2i(GLint x, GLint y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex2s(GLshort x, GLshort y)
{
    g_gl_context->gl_vertex(x, y, 0.0, 1.0);
}

void glVertex2sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], 0.0, 1.0);
}

void glVertex3d(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3f(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3i(GLint x, GLint y, GLint z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex3s(GLshort x, GLshort y, GLshort z)
{
    g_gl_context->gl_vertex(x, y, z, 1.0);
}

void glVertex3sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], 1.0);
}

void glVertex4d(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4dv(GLdouble const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4f(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4fv(GLfloat const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4i(GLint x, GLint y, GLint z, GLint w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4iv(GLint const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glVertex4s(GLshort x, GLshort y, GLshort z, GLshort w)
{
    g_gl_context->gl_vertex(x, y, z, w);
}

void glVertex4sv(GLshort const* v)
{
    g_gl_context->gl_vertex(v[0], v[1], v[2], v[3]);
}

void glTexCoord1f(GLfloat s)
{
    g_gl_context->gl_tex_coord(s, 0.0f, 0.0f, 1.0f);
}

void glTexCoord1fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], 0.0f, 0.0f, 1.0f);
}

void glTexCoord2d(GLdouble s, GLdouble t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord2dv(GLdouble const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], 0.0f, 1.0f);
}

void glTexCoord2f(GLfloat s, GLfloat t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord2fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], 0.0f, 1.0f);
}

void glTexCoord2i(GLint s, GLint t)
{
    g_gl_context->gl_tex_coord(s, t, 0.0f, 1.0f);
}

void glTexCoord3f(GLfloat s, GLfloat t, GLfloat r)
{
    g_gl_context->gl_tex_coord(s, t, r, 1.0f);
}

void glTexCoord3fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], v[2], 1.0f);
}

void glTexCoord4f(GLfloat s, GLfloat t, GLfloat r, GLfloat q)
{
    g_gl_context->gl_tex_coord(s, t, r, q);
}

void glTexCoord4fv(GLfloat const* v)
{
    g_gl_context->gl_tex_coord(v[0], v[1], v[2], v[3]);
}

void glMultiTexCoord2fARB(GLenum target, GLfloat s, GLfloat t)
{
    glMultiTexCoord2f(target, s, t);
}

void glMultiTexCoord2f(GLenum target, GLfloat s, GLfloat t)
{
    g_gl_context->gl_multi_tex_coord(target, s, t, 0.0f, 1.0f);
}

void glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz)
{
    g_gl_context->gl_normal(nx, ny, nz);
}

void glNormal3fv(GLfloat const* v)
{
    g_gl_context->gl_normal(v[0], v[1], v[2]);
}

void glNormalPointer(GLenum type, GLsizei stride, void const* pointer)
{
    g_gl_context->gl_normal_pointer(type, stride, pointer);
}

void glRectf(GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2)
{
    g_gl_context->gl_rect(x1, y1, x2, y2);
}

void glRecti(GLint x1, GLint y1, GLint x2, GLint y2)
{
    g_gl_context->gl_rect(x1, y1, x2, y2);
}
