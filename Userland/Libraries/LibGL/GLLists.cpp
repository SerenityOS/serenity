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

void glCallLists(GLsizei n, GLenum type, void const* lists)
{
    return g_gl_context->gl_call_lists(n, type, lists);
}

void glDeleteLists(GLuint list, GLsizei range)
{
    return g_gl_context->gl_delete_lists(list, range);
}

void glListBase(GLuint base)
{
    return g_gl_context->gl_list_base(base);
}

void glEndList(void)
{
    return g_gl_context->gl_end_list();
}

void glNewList(GLuint list, GLenum mode)
{
    return g_gl_context->gl_new_list(list, mode);
}

GLboolean glIsList(GLuint list)
{
    return g_gl_context->gl_is_list(list);
}
