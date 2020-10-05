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

    void quadratic_bezier_curve_to(const FloatPoint& through, const FloatPoint& point)
    {
        append_segment<QuadraticBezierCurveSegment>(point, through);
        invalidate_split_lines();
    }

    void elliptical_arc_to(const FloatPoint& point, const FloatPoint& center, const FloatPoint& radii, float x_axis_rotation, float theta_1, float theta_delta)
    {
        append_segment<EllipticalArcSegment>(point, center, radii, x_axis_rotation, theta_1, theta_delta);
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
    const auto& split_lines()
    {
        if (!m_split_lines.has_value()) {
            segmentize_path();
            ASSERT(m_split_lines.has_value());
        }
        return m_split_lines.value();
    }

    const Gfx::FloatRect& bounding_box()
    {
        if (!m_bounding_box.has_value()) {
            segmentize_path();
            ASSERT(m_bounding_box.has_value());
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
        m_segments.append(adopt(*new T(forward<Args>(args)...)));
    }

    NonnullRefPtrVector<Segment> m_segments {};

    Optional<Vector<SplitLineSegment>> m_split_lines {};
    Optional<Gfx::FloatRect> m_bounding_box;
};

inline const LogStream& operator<<(const LogStream& stream, const Path& path)
{
    return stream << path.to_string();
}

}
