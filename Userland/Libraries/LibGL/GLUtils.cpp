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

GLboolean glIsEnabled(GLenum cap)
{
    return g_gl_context->gl_is_enabled(cap);
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

void glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    g_gl_context->gl_color_mask(red, green, blue, alpha);
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
    VERIFY_CURRENT_CONTEXT_OR_VALUE(GL_NONE);
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

void glDrawBuffer(GLenum buffer)
{
    g_gl_context->gl_draw_buffer(buffer);
}

void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    g_gl_context->gl_read_pixels(x, y, width, height, format, type, pixels);
}

void glGetBooleanv(GLenum pname, GLboolean* data)
{
    g_gl_context->gl_get_booleanv(pname, data);
}

void glGetDoublev(GLenum pname, GLdouble* params)
{
    g_gl_context->gl_get_doublev(pname, params);
}

void glGetFloatv(GLenum pname, GLfloat* params)
{
    g_gl_context->gl_get_floatv(pname, params);
}

void glGetIntegerv(GLenum pname, GLint* data)
{
    VERIFY_CURRENT_CONTEXT();
    g_gl_context->gl_get_integerv(pname, data);
}

void glDepthMask(GLboolean flag)
{
    g_gl_context->gl_depth_mask(flag);
}

void glEnableClientState(GLenum cap)
{
    g_gl_context->gl_enable_client_state(cap);
}

void glDisableClientState(GLenum cap)
{
    g_gl_context->gl_disable_client_state(cap);
}

void glDepthRange(GLdouble min, GLdouble max)
{
    g_gl_context->gl_depth_range(min, max);
}

void glDepthFunc(GLenum func)
{
    g_gl_context->gl_depth_func(func);
}

void glPolygonMode(GLenum face, GLenum mode)
{
    g_gl_context->gl_polygon_mode(face, mode);
}

void glPolygonOffset(GLfloat factor, GLfloat units)
{
    g_gl_context->gl_polygon_offset(factor, units);
}

void glPixelStorei(GLenum pname, GLint param)
{
    g_gl_context->gl_pixel_storei(pname, param);
}

void glScissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
    g_gl_context->gl_scissor(x, y, width, height);
}

void glPushAttrib(GLbitfield mask)
{
    g_gl_context->gl_push_attrib(mask);
}

void glPopAttrib()
{
    g_gl_context->gl_pop_attrib();
}
