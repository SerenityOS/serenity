/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glFogfv(GLenum pname, GLfloat* params)
{
    g_gl_context->gl_fogfv(pname, params);
}

void glFogf(GLenum pname, GLfloat param)
{
    g_gl_context->gl_fogf(pname, param);
}

void glFogi(GLenum pname, GLint param)
{
    g_gl_context->gl_fogi(pname, param);
}
