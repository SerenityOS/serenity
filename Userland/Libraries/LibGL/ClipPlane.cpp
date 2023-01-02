/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, Ryan Bethke <ryanbethke11@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_clip_plane(GLenum plane, GLdouble const* equation)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clip_plane, plane, equation);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF((plane < GL_CLIP_PLANE0) || (plane > GL_CLIP_PLANE5), GL_INVALID_ENUM);

    auto plane_idx = static_cast<size_t>(plane) - GL_CLIP_PLANE0;

    auto eqn = FloatVector4(equation[0], equation[1], equation[2], equation[3]);
    m_clip_plane_attributes.eye_clip_plane[plane_idx] = model_view_matrix() * eqn;
    m_clip_planes_dirty = true;
}

void GLContext::gl_get_clip_plane(GLenum plane, GLdouble* equation)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF((plane < GL_CLIP_PLANE0) || (plane > GL_CLIP_PLANE5), GL_INVALID_ENUM);

    auto plane_idx = static_cast<size_t>(plane) - GL_CLIP_PLANE0;
    equation[0] = static_cast<GLdouble>(m_clip_plane_attributes.eye_clip_plane[plane_idx][0]);
    equation[1] = static_cast<GLdouble>(m_clip_plane_attributes.eye_clip_plane[plane_idx][1]);
    equation[2] = static_cast<GLdouble>(m_clip_plane_attributes.eye_clip_plane[plane_idx][2]);
    equation[3] = static_cast<GLdouble>(m_clip_plane_attributes.eye_clip_plane[plane_idx][3]);
}

void GLContext::sync_clip_planes()
{
    if (!m_clip_planes_dirty)
        return;
    m_clip_planes_dirty = false;

    // TODO: Replace magic number 6 with device-dependent constant
    Vector<FloatVector4, 6> user_clip_planes;
    for (size_t plane_idx = 0; plane_idx < 6; ++plane_idx) {
        if ((m_clip_plane_attributes.enabled & (1 << plane_idx)) != 0u) {
            user_clip_planes.append(m_clip_plane_attributes.eye_clip_plane[plane_idx]);
        }
    }
    m_rasterizer->set_clip_planes(user_clip_planes);
}

}
