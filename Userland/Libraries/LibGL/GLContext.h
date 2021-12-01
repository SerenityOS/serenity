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

#define VERIFY_CURRENT_CONTEXT() \
    if (!g_gl_context) {         \
        return;                  \
    }

#define VERIFY_CURRENT_CONTEXT_OR_VALUE(value) \
    if (!g_gl_context) {                       \
        return value;                          \
    }

class GLContext {
public:
    virtual ~GLContext();

    virtual void gl_begin(GLenum mode) = 0;
    virtual void gl_clear(GLbitfield mask) = 0;
    virtual void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) = 0;
    virtual void gl_clear_depth(GLdouble depth) = 0;
    virtual void gl_clear_stencil(GLint s) = 0;
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
    virtual void gl_mult_matrix(FloatMatrix4x4 const& matrix) = 0;
    virtual void gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_scale(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_translate(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w) = 0;
    virtual void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height) = 0;
    virtual void gl_enable(GLenum) = 0;
    virtual void gl_disable(GLenum) = 0;
    virtual GLboolean gl_is_enabled(GLenum) = 0;
    virtual void gl_front_face(GLenum) = 0;
    virtual void gl_cull_face(GLenum) = 0;
    virtual GLuint gl_gen_lists(GLsizei range) = 0;
    virtual void gl_call_list(GLuint list) = 0;
    virtual void gl_call_lists(GLsizei n, GLenum type, void const* lists) = 0;
    virtual void gl_delete_lists(GLuint list, GLsizei range) = 0;
    virtual void gl_list_base(GLuint base) = 0;
    virtual void gl_end_list(void) = 0;
    virtual void gl_new_list(GLuint list, GLenum mode) = 0;
    virtual GLboolean gl_is_list(GLuint list) = 0;
    virtual void gl_flush() = 0;
    virtual void gl_finish() = 0;
    virtual void gl_blend_func(GLenum src_factor, GLenum dst_factor) = 0;
    virtual void gl_shade_model(GLenum mode) = 0;
    virtual void gl_alpha_func(GLenum func, GLclampf ref) = 0;
    virtual void gl_hint(GLenum target, GLenum mode) = 0;
    virtual void gl_read_buffer(GLenum mode) = 0;
    virtual void gl_draw_buffer(GLenum buffer) = 0;
    virtual void gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels) = 0;
    virtual void gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data) = 0;
    virtual void gl_tex_sub_image_2d(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* data) = 0;
    virtual void gl_tex_parameter(GLenum target, GLenum pname, GLfloat param) = 0;
    virtual void gl_tex_coord(GLfloat s, GLfloat t, GLfloat r, GLfloat q) = 0;
    virtual void gl_tex_env(GLenum target, GLenum pname, GLfloat param) = 0;
    virtual void gl_bind_texture(GLenum target, GLuint texture) = 0;
    virtual void gl_active_texture(GLenum texture) = 0;
    virtual void gl_depth_mask(GLboolean flag) = 0;
    virtual void gl_enable_client_state(GLenum cap) = 0;
    virtual void gl_disable_client_state(GLenum cap) = 0;
    virtual void gl_vertex_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer) = 0;
    virtual void gl_color_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer) = 0;
    virtual void gl_tex_coord_pointer(GLint size, GLenum type, GLsizei stride, const void* pointer) = 0;
    virtual void gl_draw_arrays(GLenum mode, GLint first, GLsizei count) = 0;
    virtual void gl_draw_elements(GLenum mode, GLsizei count, GLenum type, const void* indices) = 0;
    virtual void gl_draw_pixels(GLsizei width, GLsizei height, GLenum format, GLenum type, const void* data) = 0;
    virtual void gl_color_mask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha) = 0;
    virtual void gl_get_booleanv(GLenum pname, GLboolean* data) = 0;
    virtual void gl_get_doublev(GLenum pname, GLdouble* params) = 0;
    virtual void gl_get_floatv(GLenum pname, GLfloat* params) = 0;
    virtual void gl_get_integerv(GLenum pname, GLint* data) = 0;
    virtual void gl_depth_range(GLdouble min, GLdouble max) = 0;
    virtual void gl_depth_func(GLenum func) = 0;
    virtual void gl_polygon_mode(GLenum face, GLenum mode) = 0;
    virtual void gl_polygon_offset(GLfloat factor, GLfloat units) = 0;
    virtual void gl_fogfv(GLenum pname, GLfloat* params) = 0;
    virtual void gl_fogf(GLenum pname, GLfloat params) = 0;
    virtual void gl_fogi(GLenum pname, GLint param) = 0;
    virtual void gl_pixel_storei(GLenum pname, GLint param) = 0;
    virtual void gl_scissor(GLint x, GLint y, GLsizei width, GLsizei height) = 0;
    virtual void gl_stencil_func_separate(GLenum face, GLenum func, GLint ref, GLuint mask) = 0;
    virtual void gl_stencil_op_separate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass) = 0;
    virtual void gl_normal(GLfloat nx, GLfloat ny, GLfloat nz) = 0;
    virtual void gl_raster_pos(GLfloat x, GLfloat y, GLfloat z, GLfloat w) = 0;
    virtual void gl_materialv(GLenum face, GLenum pname, GLfloat const* params) = 0;
    virtual void gl_line_width(GLfloat width) = 0;
    virtual void gl_push_attrib(GLbitfield mask) = 0;
    virtual void gl_pop_attrib() = 0;
    virtual void gl_light_model(GLenum pname, GLfloat x, GLfloat y, GLfloat z, GLfloat w) = 0;
    virtual void gl_bitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap) = 0;
    virtual void gl_copy_tex_image_2d(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border) = 0;

    virtual void present() = 0;
};

OwnPtr<GLContext> create_context(Gfx::Bitmap&);
void make_context_current(GLContext*);
void present_context(GLContext*);

}
