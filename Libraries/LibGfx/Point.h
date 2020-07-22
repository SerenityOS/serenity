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
#include <LibIPC/Forward.h>
#include <stdlib.h>

namespace Gfx {

class IntRect;
class FloatPoint;

class IntPoint {
public:
    IntPoint() { }
    IntPoint(int x, int y)
        : m_x(x)
        , m_y(y)
    {
    }

    IntPoint(const FloatPoint&);

    int x() const { return m_x; }
    int y() const { return m_y; }

    void set_x(int x) { m_x = x; }
    void set_y(int y) { m_y = y; }

    void move_by(int dx, int dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const IntPoint& delta)
    {
        move_by(delta.x(), delta.y());
    }

    IntPoint translated(const IntPoint& delta) const
    {
        IntPoint point = *this;
        point.move_by(delta);
        return point;
    }

    IntPoint translated(int dx, int dy) const
    {
        IntPoint point = *this;
        point.move_by(dx, dy);
        return point;
    }

    void constrain(const IntRect&);

    bool operator==(const IntPoint& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const IntPoint& other) const
    {
        return !(*this == other);
    }

    IntPoint operator-() const { return { -m_x, -m_y }; }

    IntPoint operator-(const IntPoint& other) const { return { m_x - other.m_x, m_y - other.m_y }; }
    IntPoint& operator-=(const IntPoint& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    IntPoint& operator+=(const IntPoint& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }
    IntPoint operator+(const IntPoint& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    IntPoint& operator*=(int factor)
    {
        m_x *= factor;
        m_y *= factor;
        return *this;
    }
    IntPoint operator*(int factor) const { return { m_x * factor, m_y * factor }; }

    IntPoint& operator/=(int factor)
    {
        m_x /= factor;
        m_y /= factor;
        return *this;
    }
    IntPoint operator/(int factor) const { return { m_x / factor, m_y / factor }; }

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

    int dx_relative_to(const IntPoint& other) const
    {
        return x() - other.x();
    }

    int dy_relative_to(const IntPoint& other) const
    {
        return y() - other.y();
    }

    // Returns pixels moved from other in either direction
    int pixels_moved(const IntPoint& other) const
    {
        return max(abs(dx_relative_to(other)), abs(dy_relative_to(other)));
    }

private:
    int m_x { 0 };
    int m_y { 0 };
};

const LogStream& operator<<(const LogStream&, const IntPoint&);

}

namespace IPC {
bool encode(Encoder&, const Gfx::IntPoint&);
bool decode(Decoder&, Gfx::IntPoint&);
}
