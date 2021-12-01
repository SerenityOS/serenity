/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glRasterPos2i(GLint x, GLint y)
{
    g_gl_context->gl_raster_pos(static_cast<float>(x), static_cast<float>(y), 0.0f, 1.0f);
}
