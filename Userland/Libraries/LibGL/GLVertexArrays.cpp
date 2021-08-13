/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glVertexPointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    g_gl_context->gl_vertex_pointer(size, type, stride, pointer);
}

void glColorPointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    g_gl_context->gl_color_pointer(size, type, stride, pointer);
}

void glTexCoordPointer(GLint size, GLenum type, GLsizei stride, const void* pointer)
{
    g_gl_context->gl_tex_coord_pointer(size, type, stride, pointer);
}
