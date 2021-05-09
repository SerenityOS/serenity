/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

GLuint glGenLists(GLsizei range)
{
    return g_gl_context->gl_gen_lists(range);
}

void glCallList(GLuint list)
{
    return g_gl_context->gl_call_list(list);
}

void glDeleteLists(GLuint list, GLsizei range)
{
    return g_gl_context->gl_delete_lists(list, range);
}

void glEndList(void)
{
    return g_gl_context->gl_end_list();
}

void glNewList(GLuint list, GLenum mode)
{
    return g_gl_context->gl_new_list(list, mode);
}
