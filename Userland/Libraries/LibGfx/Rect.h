/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibGfx/AffineTransform.h>
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
    Rect() = default;

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

    [[nodiscard]] ALWAYS_INLINE T x() const { return location().x(); }
    [[nodiscard]] ALWAYS_INLINE T y() const { return location().y(); }
    [[nodiscard]] ALWAYS_INLINE T width() const { return m_size.width(); }
    [[nodiscard]] ALWAYS_INLINE T height() const { return m_size.height(); }

    ALWAYS_INLINE void set_x(T x) { m_location.set_x(x); }
    ALWAYS_INLINE void set_y(T y) { m_location.set_y(y); }
    ALWAYS_INLINE void set_width(T width) { m_size.set_width(width); }
    ALWAYS_INLINE void set_height(T height) { m_size.set_height(height); }

    [[nodiscard]] ALWAYS_INLINE const Point<T>& location() const { return m_location; }
    [[nodiscard]] ALWAYS_INLINE const Size<T>& size() const { return m_size; }

    [[nodiscard]] ALWAYS_INLINE bool is_null() const { return width() == 0 && height() == 0; }
    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return width() <= 0 || height() <= 0; }

    ALWAYS_INLINE void translate_by(T dx, T dy) { m_location.translate_by(dx, dy); }
    ALWAYS_INLINE void translate_by(T dboth) { m_location.translate_by(dboth); }
    ALWAYS_INLINE void translate_by(const Point<T>& delta) { m_location.translate_by(delta); }

    ALWAYS_INLINE void scale_by(T dx, T dy)
    {
        m_location.scale_by(dx, dy);
        m_size.scale_by(dx, dy);
    }
    ALWAYS_INLINE void scale_by(T dboth) { scale_by(dboth, dboth); }
    ALWAYS_INLINE void scale_by(const Point<T>& delta) { scale_by(delta.x(), delta.y()); }

    void transform_by(const AffineTransform& transform) { *this = transform.map(*this); }

    Point<T> center() const
    {
        return { x() + width() / 2, y() + height() / 2 };
    }

    ALWAYS_INLINE void set_location(const Point<T>& location)
    {
        m_location = location;
    }

    ALWAYS_INLINE void set_size(const Size<T>& size)
    {
        m_size = size;
    }

    void set_size_around(const Size<T>&, const Point<T>& fixed_point);

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

    void inflate(const Size<T>& size)
    {
        set_x(x() - size.width() / 2);
        set_width(width() + size.width());
        set_y(y() - size.height() / 2);
        set_height(height() + size.height());
    }

    void shrink(T w, T h)
    {
        set_x(x() + w / 2);
        set_width(width() - w);
        set_y(y() + h / 2);
        set_height(height() - h);
    }

    void shrink(const Size<T>& size)
    {
        set_x(x() + size.width() / 2);
        set_width(width() - size.width());
        set_y(y() + size.height() / 2);
        set_height(height() - size.height());
    }

    Rect<T> translated(T dx, T dy) const
    {
        Rect<T> rect = *this;
        rect.translate_by(dx, dy);
        return rect;
    }

    Rect<T> translated(const Point<T>& delta) const
    {
        Rect<T> rect = *this;
        rect.translate_by(delta);
        return rect;
    }

    Rect<T> scaled(T sx, T sy) const
    {
        Rect<T> rect = *this;
        rect.scale_by(sx, sy);
        return rect;
    }

    Rect<T> scaled(const Point<T>& s) const
    {
        Rect<T> rect = *this;
        rect.scale_by(s);
        return rect;
    }

    Rect<T> transformed(const AffineTransform& transform) const
    {
        Rect<T> rect = *this;
        rect.transform_by(transform);
        return rect;
    }

    Rect<T> shrunken(T w, T h) const
    {
        Rect<T> rect = *this;
        rect.shrink(w, h);
        return rect;
    }

    Rect<T> shrunken(const Size<T>& size) const
    {
        Rect<T> rect = *this;
        rect.shrink(size);
        return rect;
    }

    Rect<T> inflated(T w, T h) const
    {
        Rect<T> rect = *this;
        rect.inflate(w, h);
        return rect;
    }

    Rect<T> inflated(const Size<T>& size) const
    {
        Rect<T> rect = *this;
        rect.inflate(size);
        return rect;
    }

    Rect<T> take_from_right(T w)
    {
        if (w > width())
            w = width();
        Rect<T> rect = *this;
        set_width(width() - w);
        rect.set_x(x() + width());
        rect.set_width(w);
        return rect;
    }

    Rect<T> take_from_left(T w)
    {
        if (w > width())
            w = width();
        Rect<T> rect = *this;
        set_x(x() + w);
        set_width(width() - w);
        rect.set_width(w);
        return rect;
    }

    Rect<T> take_from_top(T h)
    {
        if (h > height())
            h = height();
        Rect<T> rect = *this;
        set_y(y() + h);
        set_height(height() - h);
        rect.set_height(h);
        return rect;
    }

    Rect<T> take_from_bottom(T h)
    {
        if (h > height())
            h = height();
        Rect<T> rect = *this;
        set_height(height() - h);
        rect.set_y(y() + height());
        rect.set_height(h);
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

    ALWAYS_INLINE bool contains(const Point<T>& point) const
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

    ALWAYS_INLINE int primary_offset_for_orientation(Orientation orientation) const { return m_location.primary_offset_for_orientation(orientation); }
    ALWAYS_INLINE void set_primary_offset_for_orientation(Orientation orientation, int value) { m_location.set_primary_offset_for_orientation(orientation, value); }
    ALWAYS_INLINE int secondary_offset_for_orientation(Orientation orientation) const { return m_location.secondary_offset_for_orientation(orientation); }
    ALWAYS_INLINE void set_secondary_offset_for_orientation(Orientation orientation, int value) { m_location.set_secondary_offset_for_orientation(orientation, value); }

    ALWAYS_INLINE int primary_size_for_orientation(Orientation orientation) const { return m_size.primary_size_for_orientation(orientation); }
    ALWAYS_INLINE int secondary_size_for_orientation(Orientation orientation) const { return m_size.secondary_size_for_orientation(orientation); }
    ALWAYS_INLINE void set_primary_size_for_orientation(Orientation orientation, int value) { m_size.set_primary_size_for_orientation(orientation, value); }
    ALWAYS_INLINE void set_secondary_size_for_orientation(Orientation orientation, int value) { m_size.set_secondary_size_for_orientation(orientation, value); }

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

    [[nodiscard]] ALWAYS_INLINE T left() const { return x(); }
    [[nodiscard]] ALWAYS_INLINE T right() const { return x() + width() - 1; }
    [[nodiscard]] ALWAYS_INLINE T top() const { return y(); }
    [[nodiscard]] ALWAYS_INLINE T bottom() const { return y() + height() - 1; }

    ALWAYS_INLINE void set_left(T left)
    {
        set_x(left);
    }

    ALWAYS_INLINE void set_top(T top)
    {
        set_y(top);
    }

    ALWAYS_INLINE void set_right(T right)
    {
        set_width(right - x() + 1);
    }

    ALWAYS_INLINE void set_bottom(T bottom)
    {
        set_height(bottom - y() + 1);
    }

    void set_right_without_resize(T new_right)
    {
        int delta = new_right - right();
        translate_by(delta, 0);
    }

    void set_bottom_without_resize(T new_bottom)
    {
        int delta = new_bottom - bottom();
        translate_by(0, delta);
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

    Vector<Rect<T>, 4> shatter(const Rect<T>& hammer) const;

    template<class U>
    bool operator==(const Rect<U>& other) const
    {
        return location() == other.location() && size() == other.size();
    }

    template<class U>
    bool operator!=(const Rect<U>& other) const
    {
        return !(*this == other);
    }

    Rect<T> operator*(T factor) const { return { m_location * factor, m_size * factor }; }

    Rect<T>& operator*=(T factor)
    {
        m_location *= factor;
        m_size *= factor;
        return *this;
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

    ALWAYS_INLINE Rect<T> intersected(const Rect<T>& other) const
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
    ALWAYS_INLINE Rect<U> to_type() const
    {
        return Rect<U>(*this);
    }

    String to_string() const;

private:
    Point<T> m_location;
    Size<T> m_size;
};

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
    void format(FormatBuilder& builder, const Gfx::Rect<T>& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

}

namespace IPC {

bool decode(Decoder&, Gfx::IntRect&);
bool encode(Encoder&, const Gfx::IntRect&);

}
