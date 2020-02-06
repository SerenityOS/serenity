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

#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibDraw/Orientation.h>
#include <LibDraw/Point.h>
#include <LibDraw/Size.h>
#include <LibDraw/TextAlignment.h>

namespace Gfx {

class Rect {
public:
    Rect() {}
    Rect(int x, int y, int width, int height)
        : m_location(x, y)
        , m_size(width, height)
    {
    }
    Rect(const Point& location, const Size& size)
        : m_location(location)
        , m_size(size)
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

    void move_by(int dx, int dy)
    {
        m_location.move_by(dx, dy);
    }

    void move_by(const Point& delta)
    {
        m_location.move_by(delta);
    }

    Point center() const
    {
        return { x() + width() / 2, y() + height() / 2 };
    }

    void set_location(const Point& location)
    {
        m_location = location;
    }

    void set_size(const Size& size)
    {
        m_size = size;
    }

    void set_size(int width, int height)
    {
        m_size.set_width(width);
        m_size.set_height(height);
    }

    void inflate(int w, int h)
    {
        set_x(x() - w / 2);
        set_width(width() + w);
        set_y(y() - h / 2);
        set_height(height() + h);
    }

    void shrink(int w, int h)
    {
        set_x(x() + w / 2);
        set_width(width() - w);
        set_y(y() + h / 2);
        set_height(height() - h);
    }

    Rect shrunken(int w, int h) const
    {
        Rect rect = *this;
        rect.shrink(w, h);
        return rect;
    }

    Rect inflated(int w, int h) const
    {
        Rect rect = *this;
        rect.inflate(w, h);
        return rect;
    }

    Rect translated(int dx, int dy) const
    {
        Rect rect = *this;
        rect.move_by(dx, dy);
        return rect;
    }

    Rect translated(const Point& delta) const
    {
        Rect rect = *this;
        rect.move_by(delta);
        return rect;
    }

    bool contains_vertically(int y) const
    {
        return y >= top() && y <= bottom();
    }

    bool contains_horizontally(int x) const
    {
        return x >= left() && x <= right();
    }

    bool contains(int x, int y) const
    {
        return x >= m_location.x() && x <= right() && y >= m_location.y() && y <= bottom();
    }

    bool contains(const Point& point) const
    {
        return contains(point.x(), point.y());
    }

    bool contains(const Rect& other) const
    {
        return left() <= other.left()
            && right() >= other.right()
            && top() <= other.top()
            && bottom() >= other.bottom();
    }

    int primary_offset_for_orientation(Orientation orientation) const { return m_location.primary_offset_for_orientation(orientation); }
    void set_primary_offset_for_orientation(Orientation orientation, int value) { m_location.set_primary_offset_for_orientation(orientation, value); }
    int secondary_offset_for_orientation(Orientation orientation) const { return m_location.secondary_offset_for_orientation(orientation); }
    void set_secondary_offset_for_orientation(Orientation orientation, int value) { m_location.set_secondary_offset_for_orientation(orientation, value); }

    int primary_size_for_orientation(Orientation orientation) const { return m_size.primary_size_for_orientation(orientation); }
    int secondary_size_for_orientation(Orientation orientation) const { return m_size.secondary_size_for_orientation(orientation); }
    void set_primary_size_for_orientation(Orientation orientation, int value) { m_size.set_primary_size_for_orientation(orientation, value); }
    void set_secondary_size_for_orientation(Orientation orientation, int value) { m_size.set_secondary_size_for_orientation(orientation, value); }

    int first_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return top();
        return left();
    }

    int last_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return bottom();
        return right();
    }

    int left() const { return x(); }
    int right() const { return x() + width() - 1; }
    int top() const { return y(); }
    int bottom() const { return y() + height() - 1; }

    void set_left(int left)
    {
        set_x(left);
    }

    void set_top(int top)
    {
        set_y(top);
    }

    void set_right(int right)
    {
        set_width(right - x() + 1);
    }

    void set_bottom(int bottom)
    {
        set_height(bottom - y() + 1);
    }

    void set_right_without_resize(int new_right)
    {
        int delta = new_right - right();
        move_by(delta, 0);
    }

    void set_bottom_without_resize(int new_bottom)
    {
        int delta = new_bottom - bottom();
        move_by(0, delta);
    }

    bool intersects_vertically(const Rect& other) const
    {
        return top() <= other.bottom()
            && other.top() <= bottom();
    }

    bool intersects_horizontally(const Rect& other) const
    {
        return left() <= other.right()
            && other.left() <= right();
    }

    bool intersects(const Rect& other) const
    {
        return left() <= other.right()
            && other.left() <= right()
            && top() <= other.bottom()
            && other.top() <= bottom();
    }

    int x() const { return location().x(); }
    int y() const { return location().y(); }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

    void set_x(int x) { m_location.set_x(x); }
    void set_y(int y) { m_location.set_y(y); }
    void set_width(int width) { m_size.set_width(width); }
    void set_height(int height) { m_size.set_height(height); }

    Point location() const { return m_location; }
    Size size() const { return m_size; }

    Vector<Rect, 4> shatter(const Rect& hammer) const;

    bool operator==(const Rect& other) const
    {
        return m_location == other.m_location
            && m_size == other.m_size;
    }

    void intersect(const Rect&);

    static Rect from_two_points(const Point& a, const Point& b)
    {
        return { min(a.x(), b.x()), min(a.y(), b.y()), abs(a.x() - b.x()), abs(a.y() - b.y()) };
    }

    static Rect intersection(const Rect& a, const Rect& b)
    {
        Rect r(a);
        r.intersect(b);
        return r;
    }

    Rect intersected(const Rect& other) const
    {
        return intersection(*this, other);
    }

    Rect united(const Rect&) const;

    Point top_left() const { return { left(), top() }; }
    Point top_right() const { return { right(), top() }; }
    Point bottom_left() const { return { left(), bottom() }; }
    Point bottom_right() const { return { right(), bottom() }; }

    void align_within(const Rect&, TextAlignment);

    void center_within(const Rect& other)
    {
        center_horizontally_within(other);
        center_vertically_within(other);
    }

    void center_horizontally_within(const Rect& other)
    {
        set_x(other.center().x() - width() / 2);
    }

    void center_vertically_within(const Rect& other)
    {
        set_y(other.center().y() - height() / 2);
    }

    String to_string() const { return String::format("[%d,%d %dx%d]", x(), y(), width(), height()); }

private:
    Point m_location;
    Size m_size;
};

inline void Point::constrain(const Rect& rect)
{
    if (x() < rect.left())
        set_x(rect.left());
    else if (x() > rect.right())
        set_x(rect.right());
    if (y() < rect.top())
        set_y(rect.top());
    else if (y() > rect.bottom())
        set_y(rect.bottom());
}

inline const LogStream& operator<<(const LogStream& stream, const Rect& value)
{
    return stream << value.to_string();
}

}

using Gfx::Rect;
