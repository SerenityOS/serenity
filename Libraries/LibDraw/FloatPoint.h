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

namespace Gfx {

class FloatRect;

class FloatPoint {
public:
    FloatPoint() {}
    FloatPoint(float x, float y)
        : m_x(x)
        , m_y(y)
    {
    }
    float x() const { return m_x; }
    float y() const { return m_y; }

    void set_x(float x) { m_x = x; }
    void set_y(float y) { m_y = y; }

    void move_by(float dx, float dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const FloatPoint& delta)
    {
        move_by(delta.x(), delta.y());
    }

    FloatPoint translated(const FloatPoint& delta) const
    {
        FloatPoint point = *this;
        point.move_by(delta);
        return point;
    }

    FloatPoint translated(float dx, float dy) const
    {
        FloatPoint point = *this;
        point.move_by(dx, dy);
        return point;
    }

    void constrain(const FloatRect&);

    bool operator==(const FloatPoint& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const FloatPoint& other) const
    {
        return !(*this == other);
    }

    FloatPoint operator-() const { return { -m_x, -m_y }; }

    FloatPoint operator-(const FloatPoint& other) const { return { m_x - other.m_x, m_y - other.m_y }; }
    FloatPoint& operator-=(const FloatPoint& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    FloatPoint& operator+=(const FloatPoint& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }
    FloatPoint operator+(const FloatPoint& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    String to_string() const { return String::format("[%g,%g]", x(), y()); }

    bool is_null() const { return !m_x && !m_y; }

    float primary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? y() : x();
    }

    void set_primary_offset_for_orientation(Orientation orientation, float value)
    {
        if (orientation == Orientation::Vertical)
            set_y(value);
        else
            set_x(value);
    }

    float secondary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? x() : y();
    }

    void set_secondary_offset_for_orientation(Orientation orientation, float value)
    {
        if (orientation == Orientation::Vertical)
            set_x(value);
        else
            set_y(value);
    }

private:
    float m_x { 0 };
    float m_y { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const FloatPoint& value)
{
    return stream << value.to_string();
}

}

using Gfx::FloatPoint;
