/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibGL/GLContext.h>

namespace GL {

static constexpr size_t matrix_stack_limit(GLenum matrix_mode)
{
    switch (matrix_mode) {
    case GL_MODELVIEW:
        return MODELVIEW_MATRIX_STACK_LIMIT;
    case GL_PROJECTION:
        return PROJECTION_MATRIX_STACK_LIMIT;
    case GL_TEXTURE:
        return TEXTURE_MATRIX_STACK_LIMIT;
    }
    VERIFY_NOT_REACHED();
}

void GLContext::gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_frustum, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(near_val < 0 || far_val < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(left == right || bottom == top || near_val == far_val, GL_INVALID_VALUE);

    // Let's do some math!
    auto a = static_cast<float>((right + left) / (right - left));
    auto b = static_cast<float>((top + bottom) / (top - bottom));
    auto c = static_cast<float>(-((far_val + near_val) / (far_val - near_val)));
    auto d = static_cast<float>(-((2 * far_val * near_val) / (far_val - near_val)));

    FloatMatrix4x4 frustum {
        static_cast<float>(2 * near_val / (right - left)), 0, a, 0,
        0, static_cast<float>(2 * near_val / (top - bottom)), b, 0,
        0, 0, c, d,
        0, 0, -1, 0
    };
    update_current_matrix(*m_current_matrix * frustum);
}

void GLContext::gl_load_identity()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_load_identity);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    update_current_matrix(FloatMatrix4x4::identity());
}

void GLContext::gl_load_matrix(FloatMatrix4x4 const& matrix)
{
    APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(gl_load_matrix, matrix);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    update_current_matrix(matrix);
}

void GLContext::gl_matrix_mode(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_matrix_mode, mode);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode < GL_MODELVIEW || mode > GL_TEXTURE, GL_INVALID_ENUM);

    m_current_matrix_mode = mode;
    switch (mode) {
    case GL_MODELVIEW:
        m_current_matrix_stack = &m_model_view_matrix_stack;
        break;
    case GL_PROJECTION:
        m_current_matrix_stack = &m_projection_matrix_stack;
        break;
    case GL_TEXTURE:
        m_current_matrix_stack = &m_active_texture_unit->texture_matrix_stack();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    m_current_matrix = &m_current_matrix_stack->last();
}

void GLContext::gl_mult_matrix(FloatMatrix4x4 const& matrix)
{
    APPEND_TO_CALL_LIST_WITH_ARG_AND_RETURN_IF_NEEDED(gl_mult_matrix, matrix);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    update_current_matrix(*m_current_matrix * matrix);
}

void GLContext::gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_ortho, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(left == right || bottom == top || near_val == far_val, GL_INVALID_VALUE);

    auto rl = right - left;
    auto tb = top - bottom;
    auto fn = far_val - near_val;
    auto tx = -(right + left) / rl;
    auto ty = -(top + bottom) / tb;
    auto tz = -(far_val + near_val) / fn;

    FloatMatrix4x4 projection {
        static_cast<float>(2 / rl), 0, 0, static_cast<float>(tx),
        0, static_cast<float>(2 / tb), 0, static_cast<float>(ty),
        0, 0, static_cast<float>(-2 / fn), static_cast<float>(tz),
        0, 0, 0, 1
    };
    update_current_matrix(*m_current_matrix * projection);
}

void GLContext::gl_pop_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_pop_matrix);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(m_current_matrix_stack->size() <= 1, GL_STACK_UNDERFLOW);

    m_current_matrix_stack->take_last();
    m_current_matrix = &m_current_matrix_stack->last();
    m_matrices_dirty = true;
}

void GLContext::gl_push_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_push_matrix);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(m_current_matrix_stack->size() >= matrix_stack_limit(m_current_matrix_mode), GL_STACK_OVERFLOW);

    m_current_matrix_stack->append(*m_current_matrix);
    m_current_matrix = &m_current_matrix_stack->last();
    m_matrices_dirty = true;
}

void GLContext::gl_rotate(GLfloat angle, GLfloat x, GLfloat y, GLfloat z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_rotate, angle, x, y, z);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    FloatVector3 axis = { x, y, z };
    if (axis.length() > 0.f)
        axis.normalize();
    auto rotation_mat = Gfx::rotation_matrix(axis, AK::to_radians(angle));
    update_current_matrix(*m_current_matrix * rotation_mat);
}

void GLContext::gl_scale(GLfloat x, GLfloat y, GLfloat z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_scale, x, y, z);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto scale_matrix = Gfx::scale_matrix(FloatVector3 { x, y, z });
    update_current_matrix(*m_current_matrix * scale_matrix);
}

void GLContext::gl_translate(GLfloat x, GLfloat y, GLfloat z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_translate, x, y, z);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto translation_matrix = Gfx::translation_matrix(FloatVector3 { x, y, z });
    update_current_matrix(*m_current_matrix * translation_matrix);
}

void GLContext::sync_matrices()
{
    if (!m_matrices_dirty)
        return;

    m_rasterizer->set_model_view_transform(model_view_matrix());
    m_rasterizer->set_projection_transform(projection_matrix());

    m_matrices_dirty = false;
}

}
