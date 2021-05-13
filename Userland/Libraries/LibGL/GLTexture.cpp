/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GLContext.h"
#include <LibGL/GL/gl.h>

extern GL::GLContext* g_gl_context;

void glGenTextures(GLsizei n, GLuint* textures)
{
    g_gl_context->gl_gen_textures(n, textures);
}

void glDeleteTextures(GLsizei n, const GLuint* textures)
{
    g_gl_context->gl_delete_textures(n, textures);
}

void glTexImage2D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data)
{
    g_gl_context->gl_tex_image_2d(target, level, internalFormat, width, height, border, format, type, data);
}
