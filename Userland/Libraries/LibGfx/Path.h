/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Line.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class Segment : public RefCounted<Segment> {
public:
    enum class Type {
        Invalid,
        MoveTo,
        LineTo,
        QuadraticBezierCurveTo,
        CubicBezierCurveTo,
    };

    Segment(FloatPoint point)
        : m_point(point)
    {
    }

    virtual ~Segment() = default;

    FloatPoint point() const { return m_point; }
    virtual Type type() const = 0;

protected:
    FloatPoint m_point;
};

class MoveSegment final : public Segment {
public:
    MoveSegment(FloatPoint point)
        : Segment(point)
    {
    }

private:
    virtual Type type() const override { return Segment::Type::MoveTo; }
};

class LineSegment final : public Segment {
public:
    LineSegment(FloatPoint point)
        : Segment(point)
    {
    }

    virtual ~LineSegment() override = default;

private:
    virtual Type type() const override { return Segment::Type::LineTo; }
};

class QuadraticBezierCurveSegment final : public Segment {
public:
    QuadraticBezierCurveSegment(FloatPoint point, FloatPoint through)
        : Segment(point)
        , m_through(through)
    {
    }

    virtual ~QuadraticBezierCurveSegment() override = default;

    FloatPoint through() const { return m_through; }

private:
    virtual Type type() const override { return Segment::Type::QuadraticBezierCurveTo; }

    FloatPoint m_through;
};

class CubicBezierCurveSegment final : public Segment {
public:
    CubicBezierCurveSegment(FloatPoint point, FloatPoint through_0, FloatPoint through_1)
        : Segment(point)
        , m_through_0(through_0)
        , m_through_1(through_1)
    {
    }

    virtual ~CubicBezierCurveSegment() override = default;

    FloatPoint through_0() const { return m_through_0; }
    FloatPoint through_1() const { return m_through_1; }

private:
    virtual Type type() const override { return Segment::Type::CubicBezierCurveTo; }

    FloatPoint m_through_0;
    FloatPoint m_through_1;
};

class Path {
public:
    Path() = default;

    void move_to(FloatPoint point)
    {
        append_segment<MoveSegment>(point);
    }

    void line_to(FloatPoint point)
    {
        append_segment<LineSegment>(point);
        invalidate_split_lines();
    }

    void horizontal_line_to(float x)
    {
        float previous_y = 0;
        if (!m_segments.is_empty())
            previous_y = m_segments.last()->point().y();
        line_to({ x, previous_y });
    }

    void vertical_line_to(float y)
    {
        float previous_x = 0;
        if (!m_segments.is_empty())
            previous_x = m_segments.last()->point().x();
        line_to({ previous_x, y });
    }

    void quadratic_bezier_curve_to(FloatPoint through, FloatPoint point)
    {
        append_segment<QuadraticBezierCurveSegment>(point, through);
        invalidate_split_lines();
    }

    void cubic_bezier_curve_to(FloatPoint c1, FloatPoint c2, FloatPoint p2)
    {
        append_segment<CubicBezierCurveSegment>(p2, c1, c2);
        invalidate_split_lines();
    }

    void elliptical_arc_to(FloatPoint point, FloatSize radii, float x_axis_rotation, bool large_arc, bool sweep);
    void arc_to(FloatPoint point, float radius, bool large_arc, bool sweep)
    {
        elliptical_arc_to(point, { radius, radius }, 0, large_arc, sweep);
    }

    FloatPoint last_point();

    void close();
    void close_all_subpaths();

    Vector<NonnullRefPtr<Segment const>> const& segments() const { return m_segments; }

    auto& split_lines() const
    {
        if (!m_split_lines.has_value()) {
            const_cast<Path*>(this)->segmentize_path();
            VERIFY(m_split_lines.has_value());
        }
        return m_split_lines.value();
    }

    void clear()
    {
        m_segments.clear();
        m_split_lines.clear();
    }

    Gfx::FloatRect const& bounding_box() const
    {
        if (!m_bounding_box.has_value()) {
            const_cast<Path*>(this)->segmentize_path();
            VERIFY(m_bounding_box.has_value());
        }
        return m_bounding_box.value();
    }

    void append_path(Path const& path)
    {
        m_segments.ensure_capacity(m_segments.size() + path.m_segments.size());
        for (auto const& segment : path.m_segments)
            m_segments.unchecked_append(segment);
        invalidate_split_lines();
    }

    Path copy_transformed(AffineTransform const&) const;
    void add_path(Path const&);
    void ensure_subpath(FloatPoint point);
    DeprecatedString to_deprecated_string() const;

    Path stroke_to_fill(float thickness) const;

private:
    void approximate_elliptical_arc_with_cubic_beziers(FloatPoint center, FloatSize radii, float x_axis_rotation, float theta, float theta_delta);

    void invalidate_split_lines()
    {
        m_bounding_box.clear();
        m_split_lines.clear();
    }
    void segmentize_path();

    template<typename T, typename... Args>
    void append_segment(Args&&... args)
    {
        m_segments.append(adopt_ref(*new T(forward<Args>(args)...)));
    }

    Vector<NonnullRefPtr<Segment const>> m_segments {};

    Optional<Vector<FloatLine>> m_split_lines {};
    Optional<Gfx::FloatRect> m_bounding_box;
    bool m_need_new_subpath = { true };
};

}
