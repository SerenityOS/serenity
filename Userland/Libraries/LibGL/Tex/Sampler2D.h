/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>
#include <LibGfx/Vector4.h>

namespace GL {

class Sampler2D final {
public:
    GLint min_filter() const { return m_min_filter; }
    GLint mag_filter() const { return m_mag_filter; }
    GLint wrap_s_mode() const { return m_wrap_s_mode; }
    GLint wrap_t_mode() const { return m_wrap_t_mode; }
    FloatVector4 const& border_color() const { return m_border_color; }

    void set_min_filter(GLint value) { m_min_filter = value; }
    void set_mag_filter(GLint value) { m_mag_filter = value; }
    void set_wrap_s_mode(GLint value) { m_wrap_s_mode = value; }
    void set_wrap_t_mode(GLint value) { m_wrap_t_mode = value; }
    void set_border_color(GLfloat r, GLfloat g, GLfloat b, GLfloat a) { m_border_color = { r, g, b, a }; }

private:
    GLint m_min_filter { GL_NEAREST_MIPMAP_LINEAR };
    GLint m_mag_filter { GL_LINEAR };
    GLint m_wrap_s_mode { GL_REPEAT };
    GLint m_wrap_t_mode { GL_REPEAT };

    FloatVector4 m_border_color { 0.0f, 0.0f, 0.0f, 0.0f };
};
}
