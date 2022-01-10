/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"
#include <AK/Debug.h>

extern GL::GLContext* g_gl_context;

void glBitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap)
{
    g_gl_context->gl_bitmap(width, height, xorig, yorig, xmove, ymove, bitmap);
}

void glDrawPixels(GLsizei width, GLsizei height, GLenum format, GLenum type, const void* data)
{
    g_gl_context->gl_draw_pixels(width, height, format, type, data);
}

void glLineWidth(GLfloat width)
{
    g_gl_context->gl_line_width(width);
}

void glPointSize(GLfloat size)
{
    // FIXME: implement
    dbgln_if(GL_DEBUG, "glPointSize({}): unimplemented", size);
}

void glRasterPos2i(GLint x, GLint y)
{
    g_gl_context->gl_raster_pos(static_cast<float>(x), static_cast<float>(y), 0.0f, 1.0f);
}
