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
#include <LibDraw/FloatPoint.h>
#include <LibDraw/FloatSize.h>
#include <LibDraw/Orientation.h>
#include <LibDraw/Rect.h>
#include <LibDraw/TextAlignment.h>
#include <math.h>

class FloatRect {
public:
    FloatRect() {}
    FloatRect(float x, float y, float width, float height)
        : m_location(x, y)
        , m_size(width, height)
    {
    }
    FloatRect(const FloatPoint& location, const FloatSize& size)
        : m_location(location)
        , m_size(size)
    {
    }
    FloatRect(const FloatRect& other)
        : m_location(other.m_location)
        , m_size(other.m_size)
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

    void move_by(float dx, float dy)
    {
        m_location.move_by(dx, dy);
    }

    void move_by(const FloatPoint& delta)
    {
        m_location.move_by(delta);
    }

    FloatPoint center() const
    {
        return { x() + width() / 2, y() + height() / 2 };
    }

    void set_location(const FloatPoint& location)
    {
        m_location = location;
    }

    void set_size(const FloatSize& size)
    {
        m_size = size;
    }

    void set_size(float width, float height)
    {
        m_size.set_width(width);
        m_size.set_height(height);
    }

    void inflate(float w, float h)
    {
        set_x(x() - w / 2);
        set_width(width() + w);
        set_y(y() - h / 2);
        set_height(height() + h);
    }

    void shrink(float w, float h)
    {
        set_x(x() + w / 2);
        set_width(width() - w);
        set_y(y() + h / 2);
        set_height(height() - h);
    }

    FloatRect shrunken(float w, float h) const
    {
        FloatRect rect = *this;
        rect.shrink(w, h);
        return rect;
    }

    FloatRect inflated(float w, float h) const
    {
        FloatRect rect = *this;
        rect.inflate(w, h);
        return rect;
    }

    FloatRect translated(float dx, float dy) const
    {
        FloatRect rect = *this;
        rect.move_by(dx, dy);
        return rect;
    }

    FloatRect translated(const FloatPoint& delta) const
    {
        FloatRect rect = *this;
        rect.move_by(delta);
        return rect;
    }

    bool contains_vertically(float y) const
    {
        return y >= top() && y <= bottom();
    }

    bool contains_horizontally(float x) const
    {
        return x >= left() && x <= right();
    }

    bool contains(float x, float y) const
    {
        return x >= m_location.x() && x <= right() && y >= m_location.y() && y <= bottom();
    }

    bool contains(const FloatPoint& point) const
    {
        return contains(point.x(), point.y());
    }

    bool contains(const FloatRect& other) const
    {
        return left() <= other.left()
            && right() >= other.right()
            && top() <= other.top()
            && bottom() >= other.bottom();
    }

    float primary_offset_for_orientation(Orientation orientation) const { return m_location.primary_offset_for_orientation(orientation); }
    void set_primary_offset_for_orientation(Orientation orientation, float value) { m_location.set_primary_offset_for_orientation(orientation, value); }
    float secondary_offset_for_orientation(Orientation orientation) const { return m_location.secondary_offset_for_orientation(orientation); }
    void set_secondary_offset_for_orientation(Orientation orientation, float value) { m_location.set_secondary_offset_for_orientation(orientation, value); }

    float primary_size_for_orientation(Orientation orientation) const { return m_size.primary_size_for_orientation(orientation); }
    float secondary_size_for_orientation(Orientation orientation) const { return m_size.secondary_size_for_orientation(orientation); }
    void set_primary_size_for_orientation(Orientation orientation, float value) { m_size.set_primary_size_for_orientation(orientation, value); }
    void set_secondary_size_for_orientation(Orientation orientation, float value) { m_size.set_secondary_size_for_orientation(orientation, value); }

    float first_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return top();
        return left();
    }

    float last_edge_for_orientation(Orientation orientation) const
    {
        if (orientation == Orientation::Vertical)
            return bottom();
        return right();
    }

    float left() const { return x(); }
    float right() const { return x() + width() - 1; }
    float top() const { return y(); }
    float bottom() const { return y() + height() - 1; }

    void set_left(float left)
    {
        set_x(left);
    }

    void set_top(float top)
    {
        set_y(top);
    }

    void set_right(float right)
    {
        set_width(right - x() + 1);
    }

    void set_bottom(float bottom)
    {
        set_height(bottom - y() + 1);
    }

    void set_right_without_resize(float new_right)
    {
        float delta = new_right - right();
        move_by(delta, 0);
    }

    void set_bottom_without_resize(float new_bottom)
    {
        float delta = new_bottom - bottom();
        move_by(0, delta);
    }

    bool intersects(const FloatRect& other) const
    {
        return left() <= other.right()
            && other.left() <= right()
            && top() <= other.bottom()
            && other.top() <= bottom();
    }

    float x() const { return location().x(); }
    float y() const { return location().y(); }
    float width() const { return m_size.width(); }
    float height() const { return m_size.height(); }

    void set_x(float x) { m_location.set_x(x); }
    void set_y(float y) { m_location.set_y(y); }
    void set_width(float width) { m_size.set_width(width); }
    void set_height(float height) { m_size.set_height(height); }

    FloatPoint location() const { return m_location; }
    FloatSize size() const { return m_size; }

    Vector<FloatRect, 4> shatter(const FloatRect& hammer) const;

    bool operator==(const FloatRect& other) const
    {
        return m_location == other.m_location
            && m_size == other.m_size;
    }

    void intersect(const FloatRect&);

    static FloatRect intersection(const FloatRect& a, const FloatRect& b)
    {
        FloatRect r(a);
        r.intersect(b);
        return r;
    }

    FloatRect intersected(const FloatRect& other) const
    {
        return intersection(*this, other);
    }

    FloatRect united(const FloatRect&) const;

    FloatPoint top_left() const { return { left(), top() }; }
    FloatPoint top_right() const { return { right(), top() }; }
    FloatPoint bottom_left() const { return { left(), bottom() }; }
    FloatPoint bottom_right() const { return { right(), bottom() }; }

    void align_within(const FloatRect&, TextAlignment);

    void center_within(const FloatRect& other)
    {
        center_horizontally_within(other);
        center_vertically_within(other);
    }

    void center_horizontally_within(const FloatRect& other)
    {
        set_x(other.center().x() - width() / 2);
    }

    void center_vertically_within(const FloatRect& other)
    {
        set_y(other.center().y() - height() / 2);
    }

    String to_string() const { return String::format("[%g,%g %gx%g]", x(), y(), width(), height()); }

private:
    FloatPoint m_location;
    FloatSize m_size;
};

inline void FloatPoint::constrain(const FloatRect& rect)
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

inline const LogStream& operator<<(const LogStream& stream, const FloatRect& value)
{
    return stream << value.to_string();
}

inline Rect enclosing_int_rect(const FloatRect& float_rect)
{
    return { (int)float_rect.x(), (int)float_rect.y(), (int)ceilf(float_rect.width()), (int)ceilf(float_rect.height()) };
}
