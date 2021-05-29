/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glBlendFunc(GLenum sfactor, GLenum dfactor)
{
    return g_gl_context->gl_blend_func(sfactor, dfactor);
}

void glAlphaFunc(GLenum func, GLclampf ref)
{
    return g_gl_context->gl_alpha_func(func, ref);
}
