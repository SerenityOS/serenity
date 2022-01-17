/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glClearStencil(GLint s)
{
    g_gl_context->gl_clear_stencil(s);
}

void glStencilFunc(GLenum func, GLint ref, GLuint mask)
{
    g_gl_context->gl_stencil_func_separate(GL_FRONT_AND_BACK, func, ref, mask);
}

void glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask)
{
    g_gl_context->gl_stencil_func_separate(face, func, ref, mask);
}

void glStencilOp(GLenum sfail, GLenum dpfail, GLenum dppass)
{
    g_gl_context->gl_stencil_op_separate(GL_FRONT_AND_BACK, sfail, dpfail, dppass);
}

void glStencilOpSeparate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass)
{
    g_gl_context->gl_stencil_op_separate(face, sfail, dpfail, dppass);
}

void glStencilMask(GLuint mask)
{
    g_gl_context->gl_stencil_mask_separate(GL_FRONT_AND_BACK, mask);
}

void glStencilMaskSeparate(GLenum face, GLuint mask)
{
    g_gl_context->gl_stencil_mask_separate(face, mask);
}
