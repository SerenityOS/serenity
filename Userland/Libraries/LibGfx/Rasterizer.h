/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class Rasterizer {
public:
    struct Paint {
        AK::Variant<Color, LinearGradient> coloring;
    };
    struct Edge {
        Point<float> from, to;
        float top() const { return AK::min(from.y(), to.y()); }
        float bottom() const { return AK::max(from.y(), to.y()); }
    };
    enum class FillRule {
        nonzero,
        evenodd
    };
    Rasterizer(Bitmap& image);
    void set_transform(AffineTransform const& transform);
    void set_clip_rect(const IntRect clip_rect);
    void rasterize_edges(FillRule fill_rule, Paint const& paint);

    void add_edge(Edge edge)
    {
        edge = {
            edge.from.transformed(m_transform),
            edge.to.transformed(m_transform),
        };
        if (edge.from.y() != edge.to.y())
            m_edges.append(edge);
    }

private:
    struct ActiveEdge {
        float x, dx, end;
        int winding;
        ActiveEdge(Edge const& edge, float y);
    };
    void rasterize_scanline(FillRule fill_rule);
    void update_coverage(float x0, float x1);
    void fill_scanline_solid(size_t i, Color in_color);
    void fill_scanline_linear_gradient(size_t i, LinearGradient const& gradient);
    static constexpr uint8_t m_oversampling = 5;
    size_t m_min_col, m_max_col;
    Bitmap& m_image;
    IntRect m_clip_rect;
    IntRect m_effective_clip_rect;
    AffineTransform m_transform;
    AK::Vector<uint8_t> m_coverage;
    AK::Vector<Edge> m_edges;
    AK::Vector<ActiveEdge> m_active_edges;
};

}
