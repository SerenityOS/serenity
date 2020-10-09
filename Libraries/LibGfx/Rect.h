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

#include <AK/Format.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Point.h>
#include <LibGfx/Size.h>
#include <LibGfx/TextAlignment.h>
#include <math.h>

namespace Gfx {

template<typename T>
T abst(T value)
{
    return value < 0 ? -value : value;
}

template<typename T>
class Rect {
public:
    Rect() { }

    Rect(T x, T y, T width, T height)
        : m_location(x, y)
        , m_size(width, height)
    {
    }

    template<typename U>
    Rect(U x, U y, U width, U height)
        : m_location(x, y)
        , m_size(width, height)
    {
    }

    Rect(const Point<T>& location, const Size<T>& size)
        : m_location(location)
        , m_size(size)
    {
    }

    template<typename U>
    Rect(const Point<U>& location, const Size<U>& size)
        : m_location(location)
        , m_size(size)
    {
    }

    template<typename U>
    explicit Rect(const Rect<U>& other)
        : m_location(other.location())
        , m_size(other.size())
    {
    }

    bool is_null() const
    {
        return width() == 0 && height() == 0;
    }

    bool is_empty() const
    {
        return width() <= 0 || height() <= 0;
    }

    void move_by(T dx, T dy)
    {
        m_location.move_by(dx, dy);
    }

    void move_by(const Point<T>& delta)
    {
        m_location.move_by(delta);
    }

    Point<T> center() const
    {
        return { x() + width() / 2, y() + height() / 2 };
    }

    void set_location(const Point<T>& location)
    {
        m_location = location;
    }

    void set_size(const Size<T>& size)
    {
        m_size = size;
    }

    void set_size(T width, T height)
    {
        m_size.set_width(width);
        m_size.set_height(height);
    }

    void inflate(T w, T h)
    {
        set_x(x() - w / 2);
        set_width(width() + w);
        set_y(y() - h / 2);
        set_height(height() + h);
    }

    void shrink(T w, T h)
    {
        set_x(x() + w / 2);
        set_width(width() - w);
        set_y(y() + h / 2);
        set_height(height() - h);
    }

    Rect<T> shrunken(T w, T h) const
    {
        Rect<T> rect = *this;
        rect.shrink(w, h);
        return rect;
    }

    Rect<T> inflated(T w, T h) const
    {
        Rect<T> rect = *this;
        rect.inflate(w, h);
        return rect;
    }

    Rect<T> translated(T dx, T dy) const
    {
        Rect<T> rect = *this;
        rect.move_by(dx, dy);
        return rect;
    }

    Rect<T> translated(const Point<T>& delta) const
    {
        Rect<T> rect = *this;
        rect.move_by(delta);
        return rect;
    }

    bool contains_vertically(T y) const
    {
        return y >= top() && y <= bottom();
    }

    bool contains_horizontally(T x) const
    {
        return x >= left() && x <= right();
    }

    bool contains(T x, T y) const
    {
        return x >= m_location.x() && x <= right() && y >= m_location.y() && y <= bottom();
    }

    bool contains(const Point<T>& point) const
    {
        return contains(point.x(), point.y());
    }

    bool contains(const Rect<T>& other) const
    {
        return left() <= other.left()
            && right() >= other.right()
            && top() <= other.top()
            && bottom() >= other.bottom();
    }

    template<typename Container>
    bool contains(const Container& others) const
    {
        bool have_any = false;
        for (const auto& other : others) {
            if (!contains(other))
                return false;
            have_any = true;
        }
        return have_any;
    }

    int primary_offset_for_orientation(Orientation orientation) const { return m_location.primary_offset_for_orientation(orientation); }
    void set_primary_offset_for_orientation(Orientation orientation, int value) { m_location.set_primary_offset_for_orientation(orientation, value); }
    int secondary_offset_for_orientation(Orientation orientation) const { return m_location.secondary_offset_for_orientation(orientation); }
    void set_secondary_offset_for_orientation(Orientation orientation, int value) { m_location.set_secondary_offset_for_orientation(orientation, value); }

    int primary_size_for_orientation(Orientation orientation) const { return m_size.primary_size_for_orientation(orientation); }
    int secondary_size_for_orientation(Orientation orientation) const { return m_size.secondary_size_for_orientation(orientation); }
    void set_primary_size_for_orientation(Orientation orientation, int value) { m_size.set_primary_size_for_orientation(orientation, value); }
    void set_secondary_size_for_orientation(Orientation orientation, int value) { m_size.set_secondary_size_for_orientation(orientation, value); }

    T first_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return top();
        return left();
    }

    T last_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return bottom();
        return right();
    }

    T left() const { return x(); }
    T right() const { return x() + width() - 1; }
    T top() const { return y(); }
    T bottom() const { return y() + height() - 1; }

    void set_left(T left)
    {
        set_x(left);
    }

    void set_top(T top)
    {
        set_y(top);
    }

    void set_right(T right)
    {
        set_width(right - x() + 1);
    }

    void set_bottom(T bottom)
    {
        set_height(bottom - y() + 1);
    }

    void set_right_without_resize(T new_right)
    {
        int delta = new_right - right();
        move_by(delta, 0);
    }

    void set_bottom_without_resize(T new_bottom)
    {
        int delta = new_bottom - bottom();
        move_by(0, delta);
    }

    bool intersects_vertically(const Rect<T>& other) const
    {
        return top() <= other.bottom() && other.top() <= bottom();
    }

    bool intersects_horizontally(const Rect<T>& other) const
    {
        return left() <= other.right() && other.left() <= right();
    }

    bool intersects(const Rect<T>& other) const
    {
        return left() <= other.right()
            && other.left() <= right()
            && top() <= other.bottom()
            && other.top() <= bottom();
    }

    template<typename Container>
    bool intersects(const Container& others) const
    {
        for (const auto& other : others) {
            if (intersects(other))
                return true;
        }
        return false;
    }

    template<typename Container, typename Function>
    IterationDecision for_each_intersected(const Container& others, Function f) const
    {
        if (is_empty())
            return IterationDecision::Continue;
        for (const auto& other : others) {
            auto intersected_rect = intersected(other);
            if (!intersected_rect.is_empty()) {
                IterationDecision decision = f(intersected_rect);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        }
        return IterationDecision::Continue;
    }

    T x() const { return location().x(); }
    T y() const { return location().y(); }
    T width() const { return m_size.width(); }
    T height() const { return m_size.height(); }

    void set_x(T x) { m_location.set_x(x); }
    void set_y(T y) { m_location.set_y(y); }
    void set_width(T width) { m_size.set_width(width); }
    void set_height(T height) { m_size.set_height(height); }

    const Point<T>& location() const { return m_location; }
    const Size<T>& size() const { return m_size; }

    Vector<Rect<T>, 4> shatter(const Rect<T>& hammer) const;

    bool operator==(const Rect<T>& other) const
    {
        return m_location == other.m_location && m_size == other.m_size;
    }

    bool operator!=(const Rect<T>& other) const
    {
        return !(*this == other);
    }

    void intersect(const Rect<T>&);

    static Rect<T> from_two_points(const Point<T>& a, const Point<T>& b)
    {
        return { min(a.x(), b.x()), min(a.y(), b.y()), abst(a.x() - b.x()), abst(a.y() - b.y()) };
    }

    static Rect<T> intersection(const Rect<T>& a, const Rect<T>& b)
    {
        Rect<T> r = a;
        r.intersect(b);
        return r;
    }

    Rect<T> intersected(const Rect<T>& other) const
    {
        return intersection(*this, other);
    }

    Rect<T> united(const Rect<T>&) const;

    Point<T> top_left() const { return { left(), top() }; }
    Point<T> top_right() const { return { right(), top() }; }
    Point<T> bottom_left() const { return { left(), bottom() }; }
    Point<T> bottom_right() const { return { right(), bottom() }; }

    void align_within(const Rect<T>&, TextAlignment);

    void center_within(const Rect<T>& other)
    {
        center_horizontally_within(other);
        center_vertically_within(other);
    }

    void center_horizontally_within(const Rect<T>& other)
    {
        set_x(other.center().x() - width() / 2);
    }

    void center_vertically_within(const Rect<T>& other)
    {
        set_y(other.center().y() - height() / 2);
    }

    template<typename U>
    Rect<U> to() const
    {
        return Rect<U>(*this);
    }

    String to_string() const;

private:
    Point<T> m_location;
    Size<T> m_size;
};

template<typename T>
const LogStream& operator<<(const LogStream& stream, const Rect<T>& rect)
{
    return stream << rect.to_string();
}

using IntRect = Rect<int>;
using FloatRect = Rect<float>;

ALWAYS_INLINE IntRect enclosing_int_rect(const FloatRect& float_rect)
{
    return {
        (int)float_rect.x(),
        (int)float_rect.y(),
        (int)ceilf(float_rect.width()),
        (int)ceilf(float_rect.height()),
    };
}

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Rect<T>> : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const Gfx::Rect<T>& value)
    {
        Formatter<StringView>::format(params, builder, value.to_string());
    }
};

}

namespace IPC {

bool decode(Decoder&, Gfx::IntRect&);
bool encode(Encoder&, const Gfx::IntRect&);

}
