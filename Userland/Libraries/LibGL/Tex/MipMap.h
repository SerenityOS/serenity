/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>

namespace GL {

class MipMap {
public:
    void set_width(GLsizei width) { m_width = width; }
    void set_height(GLsizei height) { m_height = height; }
    GLsizei width() const { return m_width; }
    GLsizei height() const { return m_height; }

private:
    GLsizei m_width { 0 };
    GLsizei m_height { 0 };
};
}
