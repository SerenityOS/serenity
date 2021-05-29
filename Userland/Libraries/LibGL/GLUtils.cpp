/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glEnable(GLenum cap)
{
    g_gl_context->gl_enable(cap);
}

void glDisable(GLenum cap)
{
    g_gl_context->gl_disable(cap);
}

void glFrontFace(GLenum mode)
{
    g_gl_context->gl_front_face(mode);
}

void glCullFace(GLenum mode)
{
    g_gl_context->gl_cull_face(mode);
}

void glClear(GLbitfield mask)
{
    g_gl_context->gl_clear(mask);
}

void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    g_gl_context->gl_clear_color(red, green, blue, alpha);
}

void glClearDepth(GLdouble depth)
{
    g_gl_context->gl_clear_depth(depth);
}

GLubyte* glGetString(GLenum name)
{
    return g_gl_context->gl_get_string(name);
}

void glViewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    g_gl_context->gl_viewport(x, y, width, height);
}

GLenum glGetError()
{
    return g_gl_context->gl_get_error();
}

void glFlush()
{
    g_gl_context->gl_flush();
}

void glFinish()
{
    g_gl_context->gl_finish();
}

void glHint(GLenum target, GLenum mode)
{
    g_gl_context->gl_hint(target, mode);
}

void glReadBuffer(GLenum mode)
{
    g_gl_context->gl_read_buffer(mode);
}

void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    g_gl_context->gl_read_pixels(x, y, width, height, format, type, pixels);
}
