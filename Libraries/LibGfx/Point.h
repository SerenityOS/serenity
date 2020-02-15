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

#include <AK/Forward.h>
#include <AK/StdLibExtras.h>
#include <LibGfx/Orientation.h>

namespace Gfx {

class Rect;

class Point {
public:
    Point() {}
    Point(int x, int y)
        : m_x(x)
        , m_y(y)
    {
    }

    int x() const { return m_x; }
    int y() const { return m_y; }

    void set_x(int x) { m_x = x; }
    void set_y(int y) { m_y = y; }

    void move_by(int dx, int dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const Point& delta)
    {
        move_by(delta.x(), delta.y());
    }

    Point translated(const Point& delta) const
    {
        Point point = *this;
        point.move_by(delta);
        return point;
    }

    Point translated(int dx, int dy) const
    {
        Point point = *this;
        point.move_by(dx, dy);
        return point;
    }

    void constrain(const Rect&);

    bool operator==(const Point& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const Point& other) const
    {
        return !(*this == other);
    }

    Point operator-() const { return { -m_x, -m_y }; }

    Point operator-(const Point& other) const { return { m_x - other.m_x, m_y - other.m_y }; }
    Point& operator-=(const Point& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    Point& operator+=(const Point& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }
    Point operator+(const Point& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    String to_string() const;

    bool is_null() const { return !m_x && !m_y; }

    int primary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? y() : x();
    }

    void set_primary_offset_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_y(value);
        else
            set_x(value);
    }

    int secondary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? x() : y();
    }

    void set_secondary_offset_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_x(value);
        else
            set_y(value);
    }

    int dx_relative_to(const Point& other) const
    {
        return x() - other.x();
    }

    int dy_relative_to(const Point& other) const
    {
        return y() - other.y();
    }

    // Returns pixels moved from other in either direction
    int pixels_moved(const Point& other) const
    {
        return max(abs(dx_relative_to(other)), abs(dy_relative_to(other)));
    }

private:
    int m_x { 0 };
    int m_y { 0 };
};

const LogStream& operator<<(const LogStream&, const Point&);

}

namespace IPC {
bool decode(BufferStream&, Gfx::Point&);
}
