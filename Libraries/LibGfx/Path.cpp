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

        segments.append({ Point(p0.x(), p0.y()),
            Point(p1.x(), p1.y()),
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
                add_line(Point(p0.x(), p0.y()), Point(p1.x(), p1.y()));
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

Vector<Path::LineSegment> Path::split_lines(Path::ShapeKind kind)
{
    if (m_split_lines.has_value()) {
        const auto& lines = m_split_lines.value();
        if (kind == Complex)
            return lines;

        Vector<LineSegment> segments;
        for (auto& line : lines) {
            if (is_part_of_closed_polygon(line.from, line.to))
                segments.append(line);
        }

        return move(segments);
    }

    segmentize_path();
    ASSERT(m_split_lines.has_value());
    return split_lines(kind);
}

void Path::generate_path_graph()
{
    // Generate a (possibly) disconnected cyclic directed graph
    // of the line segments in the path.
    // This graph will be used to determine whether a line should
    // be considered as part of an edge for the shape

    // FIXME: This will not chop lines up, so we might still have some
    //        filling artifacts after this, as a line might pass over an edge
    //        but be itself a part of _another_ polygon.
    HashMap<u32, OwnPtr<PathGraphNode>> graph;
    m_graph_node_map = move(graph);

    const auto& lines = split_lines();

    if (!lines.size())
        return;

    // now use scanline to find intersecting lines
    auto scanline = lines.first().maximum_y;
    auto last_line = lines.last().minimum_y;

    Vector<LineSegment> active_list;

    for (auto& line : lines) {
        if (line.maximum_y < scanline)
            break;

        active_list.append(line);
    }

    while (scanline >= last_line) {
        if (active_list.size() > 1) {
            quick_sort(active_list, [](const auto& line0, const auto& line1) {
                return line1.x < line0.x;
            });

            // for every two lines next to each other in the active list
            // figure out if they intersect, if they do, store
            // the right line as the child of the left line
            // in the path graph
            for (size_t i = 1; i < active_list.size(); ++i) {
                auto& left_line = active_list[i - 1];
                auto& right_line = active_list[i];

                auto left_hash = hash_line(left_line.from, left_line.to);
                auto right_hash = hash_line(right_line.from, right_line.to);

                auto maybe_left_entry = m_graph_node_map.value().get(left_hash);
                auto maybe_right_entry = m_graph_node_map.value().get(right_hash);

                if (!maybe_left_entry.has_value()) {
                    auto left_entry = make<PathGraphNode>(left_hash, left_line);
                    m_graph_node_map.value().set(left_hash, move(left_entry));
                    maybe_left_entry = m_graph_node_map.value().get(left_hash);
                }

                if (!maybe_right_entry.has_value()) {
                    auto right_entry = make<PathGraphNode>(right_hash, right_line);
                    m_graph_node_map.value().set(right_hash, move(right_entry));
                    maybe_right_entry = m_graph_node_map.value().get(right_hash);
                }

                // check all four sides for possible intersection
                if (((int)fabs(left_line.x - right_line.x)) <= 1
                    || ((int)fabs(left_line.x - right_line.x + left_line.inverse_slope)) <= 1
                    || ((int)fabs(left_line.x - right_line.x + right_line.inverse_slope)) <= 1
                    || ((int)fabs(left_line.x - right_line.x + +right_line.inverse_slope + left_line.inverse_slope)) <= 1) {

                    const_cast<PathGraphNode*>(maybe_left_entry.value())->children.append(maybe_right_entry.value());
                }

                left_line.x -= left_line.inverse_slope;
            }

            active_list.last().x -= active_list.last().inverse_slope;
        }

        --scanline;

        // remove any edge that goes out of bound from the active list
        for (size_t i = 0, count = active_list.size(); i < count; ++i) {
            if (scanline <= active_list[i].minimum_y) {
                active_list.remove(i);
                --count;
                --i;
            }
        }
    }
}

bool Path::is_part_of_closed_polygon(const Point& p0, const Point& p1)
{
    if (!m_graph_node_map.has_value())
        generate_path_graph();

    ASSERT(m_graph_node_map.has_value());

    auto hash = hash_line(p0, p1);
    auto maybe_entry = m_graph_node_map.value().get(hash);

    if (!maybe_entry.has_value())
        return true;

    const auto& entry = maybe_entry.value();

    // check if the entry is part of a loop
    auto is_part_of_loop = false;
    HashTable<u32> visited;
    Vector<const PathGraphNode*> queue;

    queue.append(entry);

    for (; queue.size();) {
        const auto* node = queue.take_first();
        if (visited.contains(node->hash))
            continue;

        visited.set(node->hash);

        if (node == entry) {
            is_part_of_loop = true;
            break;
        }
    }

    return is_part_of_loop;
}

// FIXME: We need a better hash, and a wider type
unsigned Path::hash_line(const Point& from, const Point& to)
{
    u32 p0 = pair_int_hash(from.x(), from.y());
    u32 p1 = pair_int_hash(to.x(), to.y());
    return pair_int_hash(p0, p1);
}

}
