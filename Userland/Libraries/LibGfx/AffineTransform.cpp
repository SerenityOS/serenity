/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Quad.h>
#include <LibGfx/Rect.h>

namespace Gfx {

bool AffineTransform::is_identity() const
{
    return m_values[0] == 1 && m_values[1] == 0 && m_values[2] == 0 && m_values[3] == 1 && m_values[4] == 0 && m_values[5] == 0;
}

bool AffineTransform::is_identity_or_translation() const
{
    return a() == 1 && b() == 0 && c() == 0 && d() == 1;
}

float AffineTransform::x_scale() const
{
    return AK::hypot(m_values[0], m_values[1]);
}

float AffineTransform::y_scale() const
{
    return AK::hypot(m_values[2], m_values[3]);
}

FloatPoint AffineTransform::scale() const
{
    return { x_scale(), y_scale() };
}

float AffineTransform::x_translation() const
{
    return e();
}

float AffineTransform::y_translation() const
{
    return f();
}

FloatPoint AffineTransform::translation() const
{
    return { x_translation(), y_translation() };
}

AffineTransform& AffineTransform::scale(float sx, float sy)
{
    m_values[0] *= sx;
    m_values[1] *= sx;
    m_values[2] *= sy;
    m_values[3] *= sy;
    return *this;
}

AffineTransform& AffineTransform::scale(FloatPoint const& s)
{
    return scale(s.x(), s.y());
}

AffineTransform& AffineTransform::set_scale(float sx, float sy)
{
    m_values[0] = sx;
    m_values[1] = 0;
    m_values[2] = 0;
    m_values[3] = sy;
    return *this;
}

AffineTransform& AffineTransform::set_scale(FloatPoint const& s)
{
    return set_scale(s.x(), s.y());
}

AffineTransform& AffineTransform::translate(float tx, float ty)
{
    m_values[4] += tx * m_values[0] + ty * m_values[2];
    m_values[5] += tx * m_values[1] + ty * m_values[3];
    return *this;
}

AffineTransform& AffineTransform::translate(FloatPoint const& t)
{
    return translate(t.x(), t.y());
}

AffineTransform& AffineTransform::set_translation(float tx, float ty)
{
    m_values[4] = tx;
    m_values[5] = ty;
    return *this;
}

AffineTransform& AffineTransform::set_translation(FloatPoint const& t)
{
    return set_translation(t.x(), t.y());
}

AffineTransform& AffineTransform::multiply(AffineTransform const& other)
{
    AffineTransform result;
    result.m_values[0] = other.a() * a() + other.b() * c();
    result.m_values[1] = other.a() * b() + other.b() * d();
    result.m_values[2] = other.c() * a() + other.d() * c();
    result.m_values[3] = other.c() * b() + other.d() * d();
    result.m_values[4] = other.e() * a() + other.f() * c() + e();
    result.m_values[5] = other.e() * b() + other.f() * d() + f();
    *this = result;
    return *this;
}

AffineTransform& AffineTransform::rotate_radians(float radians)
{
    float sin_angle;
    float cos_angle;
    AK::sincos(radians, sin_angle, cos_angle);
    AffineTransform rotation(cos_angle, sin_angle, -sin_angle, cos_angle, 0, 0);
    multiply(rotation);
    return *this;
}

Optional<AffineTransform> AffineTransform::inverse() const
{
    auto determinant = a() * d() - b() * c();
    if (determinant == 0)
        return {};
    return AffineTransform {
        d() / determinant,
        -b() / determinant,
        -c() / determinant,
        a() / determinant,
        (c() * f() - d() * e()) / determinant,
        (b() * e() - a() * f()) / determinant,
    };
}

void AffineTransform::map(float unmapped_x, float unmapped_y, float& mapped_x, float& mapped_y) const
{
    mapped_x = a() * unmapped_x + c() * unmapped_y + e();
    mapped_y = b() * unmapped_x + d() * unmapped_y + f();
}

template<>
IntPoint AffineTransform::map(IntPoint const& point) const
{
    float mapped_x;
    float mapped_y;
    map(static_cast<float>(point.x()), static_cast<float>(point.y()), mapped_x, mapped_y);
    return { round_to<int>(mapped_x), round_to<int>(mapped_y) };
}

template<>
FloatPoint AffineTransform::map(FloatPoint const& point) const
{
    float mapped_x;
    float mapped_y;
    map(point.x(), point.y(), mapped_x, mapped_y);
    return { mapped_x, mapped_y };
}

template<>
IntSize AffineTransform::map(IntSize const& size) const
{
    return {
        round_to<int>(static_cast<float>(size.width()) * x_scale()),
        round_to<int>(static_cast<float>(size.height()) * y_scale()),
    };
}

template<>
FloatSize AffineTransform::map(FloatSize const& size) const
{
    return { size.width() * x_scale(), size.height() * y_scale() };
}

template<typename T>
static T smallest_of(T p1, T p2, T p3, T p4)
{
    return min(min(p1, p2), min(p3, p4));
}

template<typename T>
static T largest_of(T p1, T p2, T p3, T p4)
{
    return max(max(p1, p2), max(p3, p4));
}

template<>
FloatRect AffineTransform::map(FloatRect const& rect) const
{
    FloatPoint p1 = map(rect.top_left());
    FloatPoint p2 = map(rect.top_right().translated(1, 0));
    FloatPoint p3 = map(rect.bottom_right().translated(1, 1));
    FloatPoint p4 = map(rect.bottom_left().translated(0, 1));
    float left = smallest_of(p1.x(), p2.x(), p3.x(), p4.x());
    float top = smallest_of(p1.y(), p2.y(), p3.y(), p4.y());
    float right = largest_of(p1.x(), p2.x(), p3.x(), p4.x());
    float bottom = largest_of(p1.y(), p2.y(), p3.y(), p4.y());
    return { left, top, right - left, bottom - top };
}

template<>
IntRect AffineTransform::map(IntRect const& rect) const
{
    return enclosing_int_rect(map(FloatRect(rect)));
}

Quad<float> AffineTransform::map_to_quad(Rect<float> const& rect) const
{
    return {
        map(rect.top_left()),
        map(rect.top_right()),
        map(rect.bottom_right()),
        map(rect.bottom_left()),
    };
}

}
