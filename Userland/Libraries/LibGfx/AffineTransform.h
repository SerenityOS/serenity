/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class AffineTransform {
public:
    AffineTransform()
        : m_values { 1, 0, 0, 1, 0, 0 }
    {
    }

    AffineTransform(float a, float b, float c, float d, float e, float f)
        : m_values { a, b, c, d, e, f }
    {
    }

    bool is_identity() const;

    void map(float unmapped_x, float unmapped_y, float& mapped_x, float& mapped_y) const;

    template<typename T>
    Point<T> map(const Point<T>&) const;

    template<typename T>
    Size<T> map(const Size<T>&) const;

    template<typename T>
    Rect<T> map(const Rect<T>&) const;

    float a() const { return m_values[0]; }
    float b() const { return m_values[1]; }
    float c() const { return m_values[2]; }
    float d() const { return m_values[3]; }
    float e() const { return m_values[4]; }
    float f() const { return m_values[5]; }

    float x_scale() const;
    float y_scale() const;

    AffineTransform& scale(float sx, float sy);
    AffineTransform& translate(float tx, float ty);
    AffineTransform& rotate_radians(float);
    AffineTransform& multiply(const AffineTransform&);

private:
    float m_values[6] { 0 };
};

}
