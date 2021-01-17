/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <math.h>
#include <stdlib.h>

namespace Gfx {

template<typename T>
class Line {
public:
    Line() { }

    Line(Point<T> a, Point<T> b)
        : m_a(a)
        , m_b(b)
    {
    }

    template<typename U>
    Line(U a, U b)
        : m_a(a)
        , m_b(b)
    {
    }

    template<typename U>
    explicit Line(const Line<U>& other)
        : m_a(other.a())
        , m_b(other.b())
    {
    }

    bool intersects(const Line& other) const
    {
        return intersected(other).has_value();
    }

    Optional<Point<T>> intersected(const Line& other) const
    {
        auto cross_product = [](const Point<T>& p1, const Point<T>& p2) {
            return p1.x() * p2.y() - p1.y() * p2.x();
        };
        auto r = m_b - m_a;
        auto s = other.m_b - other.m_a;
        auto delta_a = other.m_a - m_a;
        auto num = cross_product(delta_a, r);
        auto denom = cross_product(r, s);
        if (denom == 0) {
            if (num == 0) {
                // Lines are collinear, check if line ends are touching
                if (m_a == other.m_a || m_a == other.m_b)
                    return m_a;
                if (m_b == other.m_a || m_b == other.m_b)
                    return m_b;
                // Check if they're overlapping
                if (!(m_b.x() - m_a.x() < 0 && m_b.x() - other.m_a.x() < 0 && other.m_b.x() - m_a.x() && other.m_b.x() - other.m_a.x())) {
                    // Overlapping
                    // TODO find center point?
                }
                if (!(m_b.y() - m_a.y() < 0 && m_b.y() - other.m_a.y() < 0 && other.m_b.y() - m_a.y() && other.m_b.y() - other.m_a.y())) {
                    // Overlapping
                    // TODO find center point?
                }
                return {};
            } else {
                // Lines are parallel and not intersecting
                return {};
            }
        }
        auto u = static_cast<float>(num) / static_cast<float>(denom);
        if (u < 0.0 || u > 1.0) {
            // Lines are not parallel and don't intersect
            return {};
        }
        auto t = static_cast<float>(cross_product(delta_a, s)) / static_cast<float>(denom);
        if (t < 0.0 || t > 1.0) {
            // Lines are not parallel and don't intersect
            return {};
        }
        // TODO: round if we're dealing with int
        return Point<T>{ m_a.x() + static_cast<T>(t * r.x()), m_a.y() + static_cast<T>(t * r.y()) };
    }

    float length() const
    {
        return m_a.distance_from(m_b);
    }

    Point<T> closest_to(const Point<T>& point) const
    {
        if (m_a == m_b)
            return m_a;
        auto delta_a = point.x() - m_a.x();
        auto delta_b = point.y() - m_a.y();
        auto delta_c = m_b.x() - m_a.x();
        auto delta_d = m_b.y() - m_a.y();
        auto len_sq = delta_c * delta_c + delta_d * delta_d;
        float param = -1.0;
        if (len_sq != 0)
            param = static_cast<float>(delta_a * delta_c + delta_b * delta_d) / static_cast<float>(len_sq);
        if (param < 0)
            return m_a;
        if (param > 1)
            return m_b;
        // TODO: round if we're dealing with int
        return { static_cast<T>(m_a.x() + param * delta_c), static_cast<T>(m_a.y() + param * delta_d) };
    }

    Line<T> shortest_line_to(const Point<T>& point) const
    {
        return { closest_to(point), point };
    }

    float distance_to(const Point<T>& point) const
    {
        return shortest_line_to(point).length();
    }

    const Point<T>& a() const { return m_a; }
    const Point<T>& b() const { return m_b; }

    void set_a(const Point<T>& a) { m_a = a; }
    void set_b(const Point<T>& b) { m_b = b; }

    String to_string() const;

private:
    Point<T> m_a;
    Point<T> m_b;
};

template<>
inline String IntLine::to_string() const
{
    return String::format("[%d,%d -> %dx%d]", m_a.x(), m_a.y(), m_b.x(), m_b.y());
}

template<>
inline String FloatLine::to_string() const
{
    return String::format("[%f,%f %fx%f]", m_a.x(), m_a.y(), m_b.x(), m_b.y());
}

}
