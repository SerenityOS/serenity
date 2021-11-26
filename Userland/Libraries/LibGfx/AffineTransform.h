/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
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

    [[nodiscard]] ALWAYS_INLINE float a() const { return m_values[0]; }
    [[nodiscard]] ALWAYS_INLINE float b() const { return m_values[1]; }
    [[nodiscard]] ALWAYS_INLINE float c() const { return m_values[2]; }
    [[nodiscard]] ALWAYS_INLINE float d() const { return m_values[3]; }
    [[nodiscard]] ALWAYS_INLINE float e() const { return m_values[4]; }
    [[nodiscard]] ALWAYS_INLINE float f() const { return m_values[5]; }

    [[nodiscard]] float x_scale() const;
    [[nodiscard]] float y_scale() const;
    [[nodiscard]] FloatPoint scale() const;
    [[nodiscard]] float x_translation() const;
    [[nodiscard]] float y_translation() const;
    [[nodiscard]] FloatPoint translation() const;

    AffineTransform& scale(float sx, float sy);
    AffineTransform& scale(const FloatPoint& s);
    AffineTransform& set_scale(float sx, float sy);
    AffineTransform& set_scale(const FloatPoint& s);
    AffineTransform& translate(float tx, float ty);
    AffineTransform& translate(const FloatPoint& t);
    AffineTransform& set_translation(float tx, float ty);
    AffineTransform& set_translation(const FloatPoint& t);
    AffineTransform& rotate_radians(float);
    AffineTransform& multiply(const AffineTransform&);

private:
    float m_values[6] { 0 };
};

}

template<>
struct AK::Formatter<Gfx::AffineTransform> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::AffineTransform const& value)
    {
        return Formatter<FormatString>::format(builder, "[{} {} {} {} {} {}]", value.a(), value.b(), value.c(), value.d(), value.e(), value.f());
    }
};
