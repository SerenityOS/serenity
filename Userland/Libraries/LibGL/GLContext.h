/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GL/gl.h"
#include <AK/OwnPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>

namespace GL {

class GLContext {
public:
    virtual ~GLContext();

    virtual void gl_begin(GLenum mode) = 0;
    virtual void gl_clear(GLbitfield mask) = 0;
    virtual void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) = 0;
    virtual void gl_clear_depth(GLdouble depth) = 0;
    virtual void gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a) = 0;
    virtual void gl_delete_textures(GLsizei n, const GLuint* textures) = 0;
    virtual void gl_end() = 0;
    virtual void gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) = 0;
    virtual void gl_gen_textures(GLsizei n, GLuint* textures) = 0;
    virtual GLenum gl_get_error() = 0;
    virtual GLubyte* gl_get_string(GLenum name) = 0;
    virtual void gl_load_identity() = 0;
    virtual void gl_load_matrix(const FloatMatrix4x4& matrix) = 0;
    virtual void gl_matrix_mode(GLenum mode) = 0;
    virtual void gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) = 0;
    virtual void gl_push_matrix() = 0;
    virtual void gl_pop_matrix() = 0;
    virtual void gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_scale(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_translate(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w) = 0;
    virtual void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height) = 0;
    virtual void gl_enable(GLenum) = 0;
    virtual void gl_disable(GLenum) = 0;
    virtual void gl_front_face(GLenum) = 0;
    virtual void gl_cull_face(GLenum) = 0;
    virtual GLuint gl_gen_lists(GLsizei range) = 0;
    virtual void gl_call_list(GLuint list) = 0;
    virtual void gl_delete_lists(GLuint list, GLsizei range) = 0;
    virtual void gl_end_list(void) = 0;
    virtual void gl_new_list(GLuint list, GLenum mode) = 0;
    virtual void gl_flush() = 0;
    virtual void gl_finish() = 0;
    virtual void gl_blend_func(GLenum src_factor, GLenum dst_factor) = 0;
    virtual void gl_shade_model(GLenum mode) = 0;
    virtual void gl_alpha_func(GLenum func, GLclampf ref) = 0;
    virtual void gl_hint(GLenum target, GLenum mode) = 0;
    virtual void gl_read_buffer(GLenum mode) = 0;
    virtual void gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels) = 0;
    virtual void gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data) = 0;
    virtual void gl_tex_coord(GLfloat s, GLfloat t, GLfloat r, GLfloat q) = 0;
    virtual void gl_bind_texture(GLenum target, GLuint texture) = 0;
    virtual void gl_active_texture(GLenum texture) = 0;

    virtual void present() = 0;
};

OwnPtr<GLContext> create_context(Gfx::Bitmap&);
void make_context_current(GLContext*);

}
