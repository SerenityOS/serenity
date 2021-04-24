/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GLContext.h"
#include "GLStruct.h"
#include <AK/Vector.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Vector3.h>

namespace GL {

class SoftwareGLContext : public GLContext {
public:
    virtual void gl_begin(GLenum mode) override;
    virtual void gl_clear(GLbitfield mask) override;
    virtual void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) override;
    virtual void gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a) override;
    virtual void gl_end() override;
    virtual void gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) override;
    virtual GLenum gl_get_error() override;
    virtual GLubyte* gl_get_string(GLenum name) override;
    virtual void gl_load_identity() override;
    virtual void gl_load_matrix(const FloatMatrix4x4& matrix) override;
    virtual void gl_matrix_mode(GLenum mode) override;
    virtual void gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) override;
    virtual void gl_push_matrix() override;
    virtual void gl_pop_matrix() override;
    virtual void gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_scale(GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_translate(GLdouble x, GLdouble y, GLdouble z) override;
    virtual void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w) override;
    virtual void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height) override;
    virtual void gl_enable(GLenum) override;
    virtual void gl_disable(GLenum) override;
    virtual void gl_front_face(GLenum) override;
    virtual void gl_cull_face(GLenum) override;

private:
    GLenum m_current_draw_mode;
    GLenum m_current_matrix_mode;
    FloatMatrix4x4 m_projection_matrix;
    FloatMatrix4x4 m_model_view_matrix;

    FloatMatrix4x4 m_current_matrix;

    Vector<FloatMatrix4x4> m_projection_matrix_stack;
    Vector<FloatMatrix4x4> m_model_view_matrix_stack;

    FloatVector4 m_clear_color = { 0.0f, 0.0f, 0.0f, 0.0f };
    FloatVector4 m_current_vertex_color = { 1.0f, 1.0f, 1.0f, 1.0f };

    Vector<GLVertex> vertex_list;
    Vector<GLTriangle> triangle_list;
    Vector<GLTriangle> processed_triangles;

    GLenum m_error = GL_NO_ERROR;
    bool m_in_draw_state = false;

    bool m_cull_faces = false;
    GLenum m_front_face = GL_CCW;
    GLenum m_culled_sides = GL_BACK;
};

}
