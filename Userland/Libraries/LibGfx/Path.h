/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Line.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class Path;

class PathSegment {
public:
    enum Command : u8 {
        MoveTo,
        LineTo,
        QuadraticBezierCurveTo,
        CubicBezierCurveTo,
    };

    ALWAYS_INLINE Command command() const { return m_command; }
    ALWAYS_INLINE FloatPoint point() const { return m_points.last(); }
    ALWAYS_INLINE FloatPoint through() const
    {
        VERIFY(m_command == Command::QuadraticBezierCurveTo);
        return m_points[0];
    }
    ALWAYS_INLINE FloatPoint through_0() const
    {
        VERIFY(m_command == Command::CubicBezierCurveTo);
        return m_points[0];
    }
    ALWAYS_INLINE FloatPoint through_1() const
    {
        VERIFY(m_command == Command::CubicBezierCurveTo);
        return m_points[1];
    }

    static constexpr int points_per_command(Command command)
    {
        switch (command) {
        case Command::MoveTo:
        case Command::LineTo:
            return 1; // Single point.
        case Command::QuadraticBezierCurveTo:
            return 2; // Control point + point.
        case Command::CubicBezierCurveTo:
            return 3; // Two control points + point.
        }
        VERIFY_NOT_REACHED();
    }

    PathSegment(Command command, ReadonlySpan<FloatPoint> points)
        : m_command(command)
        , m_points(points) {};

private:
    Command m_command;
    ReadonlySpan<FloatPoint> m_points;
};

class PathSegmentIterator {
public:
    int operator<=>(PathSegmentIterator other) const
    {
        if (m_command_index > other.m_command_index)
            return 1;
        if (m_command_index < other.m_command_index)
            return -1;
        return 0;
    }
    bool operator==(PathSegmentIterator other) const { return m_command_index == other.m_command_index; }
    bool operator!=(PathSegmentIterator other) const { return m_command_index != other.m_command_index; }

    PathSegmentIterator operator++()
    {
        if (m_command_index < m_commands.size())
            m_point_index += PathSegment::points_per_command(m_commands[m_command_index++]);
        return *this;
    }
    PathSegmentIterator operator++(int)
    {
        PathSegmentIterator old(*this);
        ++*this;
        return old;
    }

    PathSegmentIterator operator--()
    {
        if (m_command_index > 0)
            m_point_index -= PathSegment::points_per_command(m_commands[--m_command_index]);
        return *this;
    }
    PathSegmentIterator operator--(int)
    {
        PathSegmentIterator old(*this);
        --*this;
        return old;
    }

    PathSegment operator*() const
    {
        auto command = m_commands[m_command_index];
        return PathSegment { command, m_points.span().slice(m_point_index, PathSegment::points_per_command(command)) };
    }

    PathSegmentIterator& operator=(PathSegmentIterator const& other)
    {
        m_point_index = other.m_point_index;
        m_command_index = other.m_command_index;
        return *this;
    }
    PathSegmentIterator(PathSegmentIterator const&) = default;

    friend Path;

private:
    PathSegmentIterator(Vector<FloatPoint> const& points, Vector<PathSegment::Command> const& commands, size_t point_index = 0, size_t command_index = 0)
        : m_points(points)
        , m_commands(commands)
        , m_point_index(point_index)
        , m_command_index(command_index)
    {
    }

    // Note: Store reference to vectors from Gfx::Path so appending segments does not invalidate iterators.
    Vector<FloatPoint> const& m_points;
    Vector<PathSegment::Command> const& m_commands;
    size_t m_point_index { 0 };
    size_t m_command_index { 0 };
};

class Path {
public:
    Path() = default;

    void move_to(FloatPoint point)
    {
        append_segment<PathSegment::MoveTo>(point);
    }

    void line_to(FloatPoint point)
    {
        append_segment<PathSegment::LineTo>(point);
        invalidate_split_lines();
    }

    void horizontal_line_to(float x)
    {
        line_to({ x, last_point().y() });
    }

    void vertical_line_to(float y)
    {
        line_to({ last_point().x(), y });
    }

    void quadratic_bezier_curve_to(FloatPoint through, FloatPoint point)
    {
        append_segment<PathSegment::QuadraticBezierCurveTo>(through, point);
        invalidate_split_lines();
    }

    void cubic_bezier_curve_to(FloatPoint c1, FloatPoint c2, FloatPoint p2)
    {
        append_segment<PathSegment::CubicBezierCurveTo>(c1, c2, p2);
        invalidate_split_lines();
    }

    void elliptical_arc_to(FloatPoint point, FloatSize radii, float x_axis_rotation, bool large_arc, bool sweep);
    void arc_to(FloatPoint point, float radius, bool large_arc, bool sweep)
    {
        elliptical_arc_to(point, { radius, radius }, 0, large_arc, sweep);
    }

    void text(Utf8View, Font const&);

    FloatPoint last_point()
    {
        if (!m_points.is_empty())
            return m_points.last();
        return {};
    }

    void close();
    void close_all_subpaths();

    Path stroke_to_fill(float thickness) const;

    Path place_text_along(Utf8View text, Font const&) const;

    Path copy_transformed(AffineTransform const&) const;

    ReadonlySpan<FloatLine> split_lines() const
    {
        if (!m_split_lines.has_value()) {
            const_cast<Path*>(this)->segmentize_path();
            VERIFY(m_split_lines.has_value());
        }
        return m_split_lines->lines;
    }

    Gfx::FloatRect const& bounding_box() const
    {
        (void)split_lines();
        return m_split_lines->bounding_box;
    }

    void append_path(Path const& path)
    {
        m_commands.extend(path.m_commands);
        m_points.extend(path.m_points);
        invalidate_split_lines();
    }

    ByteString to_byte_string() const;

    PathSegmentIterator begin() const
    {
        return PathSegmentIterator(m_points, m_commands);
    }

    PathSegmentIterator end() const
    {
        return PathSegmentIterator(m_points, m_commands, m_points.size(), m_commands.size());
    }

    bool is_empty() const
    {
        return m_commands.is_empty();
    }

    void clear()
    {
        *this = Path {};
    }

private:
    void approximate_elliptical_arc_with_cubic_beziers(FloatPoint center, FloatSize radii, float x_axis_rotation, float theta, float theta_delta);

    void invalidate_split_lines()
    {
        m_split_lines.clear();
    }
    void segmentize_path();

    template<PathSegment::Command command, typename... Args>
    void append_segment(Args&&... args)
    {
        constexpr auto point_count = sizeof...(Args);
        static_assert(point_count == PathSegment::points_per_command(command));
        FloatPoint points[] { args... };
        // Note: This should maintain the invariant that `m_points.last()` is always the last point in the path.
        m_points.append(points, point_count);
        m_commands.append(command);
    }

    Vector<FloatPoint> m_points {};
    Vector<PathSegment::Command> m_commands {};

    struct SplitLines {
        Vector<FloatLine> lines;
        Gfx::FloatRect bounding_box;
    };

    Optional<SplitLines> m_split_lines {};
};

}
