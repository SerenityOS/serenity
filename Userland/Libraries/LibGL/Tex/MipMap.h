/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGL/GL/gl.h>
#include <LibGfx/Vector4.h>

namespace GL {

class MipMap {
public:
    MipMap() = default;
    ~MipMap() = default;

    void set_width(GLsizei width) { m_width = width; }
    void set_height(GLsizei height) { m_height = height; }
    GLsizei width() const { return m_width; }
    GLsizei height() const { return m_height; }

    Vector<u32>& pixel_data() { return m_pixel_data; }
    const Vector<u32>& pixel_data() const { return m_pixel_data; }

    FloatVector4 texel(unsigned x, unsigned y) const
    {
        if (x >= (unsigned)m_width || y >= (unsigned)m_height)
            return { 0, 0, 0, 0 };

        u32 texel = m_pixel_data.at(y * m_width + x);

        return {
            ((texel >> 16) & 0xff) / 255.f,
            ((texel >> 8) & 0xff) / 255.f,
            (texel & 0xff) / 255.f,
            ((texel >> 24) & 0xff) / 255.f
        };
    }

private:
    GLsizei m_width { 0 };
    GLsizei m_height { 0 };
    Vector<u32> m_pixel_data;
};
}
