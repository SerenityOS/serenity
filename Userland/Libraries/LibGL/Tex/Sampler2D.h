/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>

namespace GL {

class Sampler2D final {
public:
    GLint min_filter() const { return m_min_filter; }
    GLint mag_filter() const { return m_mag_filter; }
    GLint wrap_s_mode() const { return m_wrap_s_mode; }
    GLint wrap_t_mode() const { return m_wrap_t_mode; }

    void set_min_filter(GLint value) { m_min_filter = value; }
    void set_mag_filter(GLint value) { m_mag_filter = value; }
    void set_wrap_s_mode(GLint value) { m_wrap_s_mode = value; }
    void set_wrap_t_mode(GLint value) { m_wrap_t_mode = value; }

private:
    GLint m_min_filter { GL_NEAREST_MIPMAP_LINEAR };
    GLint m_mag_filter { GL_LINEAR };
    GLint m_wrap_s_mode { GL_REPEAT };
    GLint m_wrap_t_mode { GL_REPEAT };
};
}
