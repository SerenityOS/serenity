/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
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

void glBindTexture(GLenum target, GLuint texture)
{
    g_gl_context->gl_bind_texture(target, texture);
}

// Note: This is an _extremely_ misleading API name. This sets the active
// texture unit, NOT the active texture itself...
void glActiveTexture(GLenum texture)
{
    g_gl_context->gl_active_texture(texture);
}
