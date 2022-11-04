/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rasterizer.h>

namespace Gfx {

class StrokePainter {
public:
    enum class LineEnd {
        Start,
        End
    };
    enum class CapType {
        Butt,
        Square,
        Round
    };
    enum class JoinType {
        Bevel,
        Miter,
        Round
    };
    enum class EndType {
        CloseWithCorner,
        CloseWihoutCorner,
        Open
    };
    StrokePainter(Bitmap& image)
        : m_rasterizer { image }
    {
    }

    void begin(Point<float> p, bool closed, float thickness)
    {
        m_thickness = thickness;
        m_closed = closed;
        m_first_point = m_current_point = p;
        m_first = true;
        m_first_join = true;
    }

    void stroke_to(Point<float> p)
    {
        if (m_current_point.distance_from(p) == 0)
            return;
        if (m_first) {
            m_second_point = p;
            if (!m_closed)
                add_cap(p);
            m_first = false;
        } else {
            add_join(p);
        }
        m_last_point = m_current_point;
        m_current_point = p;
    }

    void end()
    {
        if (m_first)
            return;
        else if (m_closed)
            close();
        else
            add_cap(m_last_point);
    }

    void end_path(Rasterizer::Paint const& paint)
    {
        m_rasterizer.rasterize_edges(Rasterizer::FillRule::nonzero, paint);
    }

    void set_transform(AffineTransform const& transform)
    {
        m_rasterizer.set_transform(transform);
    }

    void set_clip_rect(const IntRect clip_rect)
    {
        m_rasterizer.set_clip_rect(clip_rect);
    }

private:
    static Point<float> intersect(
        Point<float> p1,
        Point<float> d1,
        Point<float> p2,
        Point<float> d2)
    {
        return intersect(p1, d1, p2, d2, d1.cross(d2));
    }

    static Point<float> intersect(
        Point<float> p1,
        Point<float> d1,
        Point<float> p2,
        Point<float> d2,
        float c)
    {
        return -(
                   d2 * p1.cross(p1 + d1) - d1 * p2.cross(p2 + d2))
            / c;
    }

    static float norm(Point<float> p)
    {
        return AK::sqrt(p.dot(p));
    }

    static Point<float> normalized(Point<float> p)
    {
        return p / norm(p);
    }

    static Point<float> left(Point<float> p)
    {
        return Point<float> { -p.y(), p.x() };
    }

    Point<float> offset(Point<float> d) const
    {
        return left(d) * (m_thickness / 2);
    }

    void add_edge(Rasterizer::Edge edge)
    {
        m_rasterizer.add_edge(edge);
    }

    void add_join(Point<float> p)
    {
        auto d1 = direction();
        auto o1 = offset(d1);
        auto l1 = m_current_point - o1;
        auto r1 = m_current_point + o1;

        auto d2 = direction(p);
        auto o2 = offset(d2);
        auto l2 = p - o2;
        auto r2 = p + o2;

        auto c = d1.cross(d2);

        auto join_start = [this](auto left, auto right) {
            if (m_first_join) {
                m_close_left = left;
                m_close_right = right;
                m_first_join = false;
            } else {
                add_edge({ m_left, left });
                add_edge({ right, m_right });
            }
        };

        if (AK::abs(c) < 0.0001f) {
            join_start(l2, r2);
            m_left = l2;
            m_right = r2;
            return;
        }

        switch (m_join_type) {
        case JoinType::Bevel: {
            if (c > 0) {
                auto right = intersect(r1, d1, r2, d2, c);
                join_start(l1, right);
                add_edge({ l1, l2 });
                m_left = l2;
                m_right = right;
            } else {
                auto left = intersect(l1, d1, l2, d2, c);
                join_start(left, r1);
                add_edge({ r2, r1 });
                m_left = left;
                m_right = r2;
            }
            break;
        }
        case JoinType::Miter: {
            auto left = intersect(l1, d1, l2, d2, c);
            auto right = intersect(r1, d1, r2, d2, c);
            join_start(left, right);
            m_left = left;
            m_right = right;
            break;
        }
        case JoinType::Round: {
            if (c > 0) {
                auto right = intersect(r1, d1, r2, d2, c);
                join_start(l1, right);
                add_circle_segment(m_current_point, l1, l2);
                m_left = l2;
                m_right = right;
            } else {
                auto left = intersect(l1, d1, l2, d2, c);
                join_start(left, r1);
                add_circle_segment(m_current_point, r2, r1);
                m_left = left;
                m_right = r2;
            }
        }
        }
    }

    void add_cap(Point<float> towards)
    {
        auto d = direction(towards);
        auto o = offset(d);
        auto l = m_current_point - o;
        auto r = m_current_point + o;
        switch (m_cap_type) {
        case CapType::Butt: {
            add_edge({ r, l });
            break;
        }
        case CapType::Square: {
            auto backoff = normalized(d) * (m_thickness / 2);
            l -= backoff;
            r -= backoff;
            add_edge({ r, l });
            break;
        }
        case CapType::Round: {
            add_circle_segment(m_current_point, r, l);
            break;
        }
        }
    }

    void close()
    {
        stroke_to(m_first_point);
        add_join(m_second_point);
        add_edge({ m_left, m_close_left });
        add_edge({ m_close_right, m_right });
    }

    void add_circle_segment(
        Point<float> center,
        Point<float> from,
        Point<float> to)
    {
        auto r = from.distance_from(center);
        auto r1 = from - center;
        auto r2 = to - center;
        auto a1 = AK::atan2(r1.x(), r1.y());
        auto a2 = AK::atan2(r2.x(), r2.y());
        if (r1.cross(r2) < 0) {
            if (a1 > a2)
                a1 -= 2 * float(M_PI);
        } else {
            if (a1 < a2)
                a2 -= 2 * float(M_PI);
        }
        size_t n_steps = AK::max(1, to.distance_from(from));
        Point<float> last = from;
        for (size_t i = 0; i < n_steps; ++i) {
            auto a = a1 + float(i + 1) / float(n_steps + 1) * (a2 - a1);
            float s, c;
            AK::sincos(a, s, c);
            auto p = m_current_point + Point<float> { s, c } * r;
            add_edge({ last, p });
            last = p;
        }
        add_edge({ last, to });
    }

    Point<float> direction() const
    {
        return normalized(m_current_point - m_last_point);
    }

    Point<float> direction(Point<float> next) const
    {
        return normalized(next - m_current_point);
    }

    Rasterizer m_rasterizer;
    float m_thickness = 1;
    bool m_closed;
    bool m_first = true;
    bool m_first_join = true;
    CapType m_cap_type = CapType::Butt;
    JoinType m_join_type = JoinType::Miter;
    Point<float> m_left;
    Point<float> m_right;
    Point<float> m_close_left;
    Point<float> m_close_right;
    Point<float> m_first_point;
    Point<float> m_second_point;
    Point<float> m_last_point;
    Point<float> m_current_point;
};

}
