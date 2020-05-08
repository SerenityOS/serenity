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

#include <AK/Function.h>
#include <AK/HashFunctions.h>
#include <AK/HashTable.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <math.h>

namespace Gfx {

void Path::close()
{
    if (m_segments.size() <= 1)
        return;

    invalidate_split_lines();

    auto& last_point = m_segments.last().point;

    for (ssize_t i = m_segments.size() - 1; i >= 0; --i) {
        auto& segment = m_segments[i];
        if (segment.type == Segment::Type::MoveTo) {
            if (last_point == segment.point)
                return;
            m_segments.append({ Segment::Type::LineTo, segment.point });
            return;
        }
    }
}

String Path::to_string() const
{
    StringBuilder builder;
    builder.append("Path { ");
    for (auto& segment : m_segments) {
        switch (segment.type) {
        case Segment::Type::MoveTo:
            builder.append("MoveTo");
            break;
        case Segment::Type::LineTo:
            builder.append("LineTo");
            break;
        case Segment::Type::QuadraticBezierCurveTo:
            builder.append("QuadraticBezierCurveTo");
            break;
        case Segment::Type::Invalid:
            builder.append("Invalid");
            break;
        }
        builder.append('(');
        builder.append(segment.point.to_string());
        if (segment.through.has_value()) {
            builder.append(", ");
            builder.append(segment.through.value().to_string());
        }
        builder.append(')');

        builder.append(' ');
    }
    builder.append("}");
    return builder.to_string();
}

void Path::segmentize_path()
{
    Vector<LineSegment> segments;

    auto add_line = [&](const auto& p0, const auto& p1) {
        float ymax = p0.y(), ymin = p1.y(), x_of_ymin = p1.x(), x_of_ymax = p0.x();
        auto slope = p0.x() == p1.x() ? 0 : ((float)(p0.y() - p1.y())) / ((float)(p0.x() - p1.x()));
        if (p0.y() < p1.y()) {
            ymin = ymax;
            ymax = p1.y();
            x_of_ymax = x_of_ymin;
            x_of_ymin = p0.x();
        }

        segments.append({ FloatPoint(p0.x(), p0.y()),
            FloatPoint(p1.x(), p1.y()),
            slope == 0 ? 0 : 1 / slope,
            x_of_ymin,
            ymax, ymin, x_of_ymax });
    };

    FloatPoint cursor { 0, 0 };
    for (auto& segment : m_segments) {
        switch (segment.type) {
        case Segment::Type::MoveTo:
            cursor = segment.point;
            break;
        case Segment::Type::LineTo: {
            add_line(cursor, segment.point);
            cursor = segment.point;
            break;
        }
        case Segment::Type::QuadraticBezierCurveTo: {
            auto& control = segment.through.value();
            Painter::for_each_line_segment_on_bezier_curve(control, cursor, segment.point, [&](const FloatPoint& p0, const FloatPoint& p1) {
                add_line(p0, p1);
            });
            cursor = segment.point;
            break;
        }
        case Segment::Type::Invalid:
            ASSERT_NOT_REACHED();
            break;
        }
    }

    // sort segments by ymax
    quick_sort(segments, [](const auto& line0, const auto& line1) {
        return line1.maximum_y < line0.maximum_y;
    });

    m_split_lines = move(segments);
}

}
