/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
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
        EllipticalArcTo,
    };

    Segment(const FloatPoint& point)
        : m_point(point)
    {
    }

    virtual ~Segment() = default;

    const FloatPoint& point() const { return m_point; }
    virtual Type type() const = 0;

protected:
    FloatPoint m_point;
};

class MoveSegment final : public Segment {
public:
    MoveSegment(const FloatPoint& point)
        : Segment(point)
    {
    }

private:
    virtual Type type() const override { return Segment::Type::MoveTo; }
};

class LineSegment final : public Segment {
public:
    LineSegment(const FloatPoint& point)
        : Segment(point)
    {
    }

    virtual ~LineSegment() override = default;

private:
    virtual Type type() const override { return Segment::Type::LineTo; }
};

class QuadraticBezierCurveSegment final : public Segment {
public:
    QuadraticBezierCurveSegment(const FloatPoint& point, const FloatPoint& through)
        : Segment(point)
        , m_through(through)
    {
    }

    virtual ~QuadraticBezierCurveSegment() override = default;

    const FloatPoint& through() const { return m_through; }

private:
    virtual Type type() const override { return Segment::Type::QuadraticBezierCurveTo; }

    FloatPoint m_through;
};

class CubicBezierCurveSegment final : public Segment {
public:
    CubicBezierCurveSegment(const FloatPoint& point, const FloatPoint& through_0, const FloatPoint& through_1)
        : Segment(point)
        , m_through_0(through_0)
        , m_through_1(through_1)
    {
    }

    virtual ~CubicBezierCurveSegment() override = default;

    const FloatPoint& through_0() const { return m_through_0; }
    const FloatPoint& through_1() const { return m_through_1; }

private:
    virtual Type type() const override { return Segment::Type::CubicBezierCurveTo; }

    FloatPoint m_through_0;
    FloatPoint m_through_1;
};

class EllipticalArcSegment final : public Segment {
public:
    EllipticalArcSegment(const FloatPoint& point, const FloatPoint& center, const FloatPoint radii, float x_axis_rotation, float theta_1, float theta_delta)
        : Segment(point)
        , m_center(center)
        , m_radii(radii)
        , m_x_axis_rotation(x_axis_rotation)
        , m_theta_1(theta_1)
        , m_theta_delta(theta_delta)
    {
    }

    virtual ~EllipticalArcSegment() override = default;

    const FloatPoint& center() const { return m_center; }
    const FloatPoint& radii() const { return m_radii; }
    float x_axis_rotation() const { return m_x_axis_rotation; }
    float theta_1() const { return m_theta_1; }
    float theta_delta() const { return m_theta_delta; }

private:
    virtual Type type() const override { return Segment::Type::EllipticalArcTo; }

    FloatPoint m_center;
    FloatPoint m_radii;
    float m_x_axis_rotation;
    float m_theta_1;
    float m_theta_delta;
};

class Path {
public:
    Path() { }

    void move_to(const FloatPoint& point)
    {
        append_segment<MoveSegment>(point);
    }

    void line_to(const FloatPoint& point)
    {
        append_segment<LineSegment>(point);
        invalidate_split_lines();
    }

    void horizontal_line_to(float x)
    {
        float previous_y = 0;
        if (!m_segments.is_empty())
            previous_y = m_segments.last().point().y();
        line_to({ x, previous_y });
    }

    void vertical_line_to(float y)
    {
        float previous_x = 0;
        if (!m_segments.is_empty())
            previous_x = m_segments.last().point().x();
        line_to({ previous_x, y });
    }

    void quadratic_bezier_curve_to(const FloatPoint& through, const FloatPoint& point)
    {
        append_segment<QuadraticBezierCurveSegment>(point, through);
        invalidate_split_lines();
    }

    void cubic_bezier_curve_to(FloatPoint const& c1, FloatPoint const& c2, FloatPoint const& p2)
    {
        append_segment<CubicBezierCurveSegment>(p2, c1, c2);
        invalidate_split_lines();
    }

    void elliptical_arc_to(const FloatPoint& point, const FloatPoint& radii, double x_axis_rotation, bool large_arc, bool sweep);
    void arc_to(const FloatPoint& point, float radius, bool large_arc, bool sweep)
    {
        elliptical_arc_to(point, { radius, radius }, 0, large_arc, sweep);
    }

    // Note: This does not do any sanity checks!
    void elliptical_arc_to(const FloatPoint& endpoint, const FloatPoint& center, const FloatPoint& radii, double x_axis_rotation, double theta, double theta_delta)
    {
        append_segment<EllipticalArcSegment>(
            endpoint,
            center,
            radii,
            x_axis_rotation,
            theta,
            theta_delta);

        invalidate_split_lines();
    }

    void close();
    void close_all_subpaths();

    struct SplitLineSegment {
        FloatPoint from, to;
        float inverse_slope;
        float x_of_minimum_y;
        float maximum_y;
        float minimum_y;
        float x;
    };

    const NonnullRefPtrVector<Segment>& segments() const { return m_segments; }
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

    String to_string() const;

private:
    void invalidate_split_lines()
    {
        m_split_lines.clear();
    }
    void segmentize_path();

    template<typename T, typename... Args>
    void append_segment(Args&&... args)
    {
        m_segments.append(adopt_ref(*new T(forward<Args>(args)...)));
    }

    NonnullRefPtrVector<Segment> m_segments {};

    Optional<Vector<SplitLineSegment>> m_split_lines {};
    Optional<Gfx::FloatRect> m_bounding_box;
};

}
