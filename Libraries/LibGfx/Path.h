/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGfx/FloatPoint.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class Path {
public:
    struct Segment {
        enum class Type {
            Invalid,
            MoveTo,
            LineTo,
            QuadraticBezierCurveTo,
        };

        Type type { Type::Invalid };
        FloatPoint point;
        Optional<FloatPoint> through {};
    };

    Path() { }

    void move_to(const FloatPoint& point)
    {
        m_segments.append({ Segment::Type::MoveTo, point });
    }

    void line_to(const FloatPoint& point)
    {
        m_segments.append({ Segment::Type::LineTo, point });
        invalidate_split_lines();
    }

    void quadratic_bezier_curve_to(const FloatPoint& through, const FloatPoint& point)
    {
        m_segments.append({ Segment::Type::QuadraticBezierCurveTo, point, through });
        invalidate_split_lines();
    }

    void close();
    void close_all_subpaths();

    struct LineSegment {
        FloatPoint from, to;
        float inverse_slope;
        float x_of_minimum_y;
        float maximum_y;
        float minimum_y;
        float x;
    };

    const Vector<Segment>& segments() const { return m_segments; }
    const auto& split_lines()
    {
        if (m_split_lines.has_value())
            return m_split_lines.value();
        segmentize_path();
        ASSERT(m_split_lines.has_value());
        return m_split_lines.value();
    }

    String to_string() const;

private:
    void invalidate_split_lines()
    {
        m_split_lines.clear();
    }
    void segmentize_path();

    Vector<Segment> m_segments;

    Optional<Vector<LineSegment>> m_split_lines {};
};

inline const LogStream& operator<<(const LogStream& stream, const Path& path)
{
    return stream << path.to_string();
}

}
