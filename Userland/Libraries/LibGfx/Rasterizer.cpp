/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rasterizer.h>

namespace Gfx {

Rasterizer::Rasterizer(Bitmap& image)
    : m_image { image }
    , m_clip_rect { m_image.rect() }
{
    m_coverage.resize(m_image.width());
}

void Rasterizer::set_transform(AffineTransform const& transform)
{
    m_transform = transform;
}

void Rasterizer::set_clip_rect(const IntRect clip_rect)
{
    m_clip_rect = m_image.rect().intersected(clip_rect);
}

void Rasterizer::rasterize_edges(FillRule fill_rule, Paint const& paint)
{
    AK::quick_sort(m_edges, [](auto a, auto b) { return a.top() < b.top(); });
    auto next_edge = m_edges.begin();
    for (int i = m_clip_rect.top(); i <= m_clip_rect.bottom(); ++i) {
        m_min_col = m_clip_rect.right();
        m_max_col = m_clip_rect.left();
        memset(m_coverage.data(), 0, m_coverage.size());
        if (m_active_edges.is_empty()) {
            if (m_edges.is_empty())
                break;
            i = AK::max<int>(i, m_edges.first().top());
        }
        for (size_t s = 0; s < m_oversampling; ++s) {
            auto scany = i + float(s) / m_oversampling;
            m_active_edges.remove_all_matching([scany](auto& e) { return e.end <= scany; });
            for (auto& edge : m_active_edges)
                edge.x += edge.dx / m_oversampling;
            for (; next_edge != m_edges.end() && next_edge->top() <= scany; ++next_edge) {
                if (next_edge->bottom() > scany)
                    m_active_edges.append(ActiveEdge { *next_edge, scany });
            }
            AK::quick_sort(m_active_edges, [](auto a, auto b) { return a.x < b.x; });
            rasterize_scanline(fill_rule);
        }
        if (m_min_col <= m_max_col) {
            paint.coloring.visit(
                [&](Color color) {
                    fill_scanline_solid(i, color);
                },
                [&](LinearGradient const& gradient) {
                    fill_scanline_linear_gradient(i, gradient);
                });
        }
    }
    m_edges.clear();
    m_active_edges.clear();
}

Rasterizer::ActiveEdge::ActiveEdge(Rasterizer::Edge const& edge, float y)
{
    auto from = edge.from;
    auto to = edge.to;
    if (from.y() > to.y()) {
        swap(from, to);
        winding = -1;
    } else {
        winding = 1;
    }
    auto d = to - from;
    dx = d.x() / d.y();
    x = from.x() + dx * (y - from.y());
    end = to.y();
}

void Rasterizer::rasterize_scanline(FillRule fill_rule)
{
    float x = 0;
    int winding = 0;
    switch (fill_rule) {
    case FillRule::nonzero:
        for (auto& edge : m_active_edges) {
            if (winding == 0) {
                x = edge.x;
                winding = edge.winding;
            } else {
                winding += edge.winding;
                if (winding == 0)
                    update_coverage(x, edge.x);
            }
        }
        break;
    case FillRule::evenodd:
        for (auto& edge : m_active_edges) {
            if (winding == 0) {
                x = edge.x;
                winding = 1;
            } else {
                winding = 0;
                update_coverage(x, edge.x);
            }
        }
        break;
    }
}

void Rasterizer::update_coverage(float x0, float x1)
{
    size_t first_index = AK::clamp<size_t>(x0, m_clip_rect.left(), m_clip_rect.right());
    size_t last_index = AK::clamp<size_t>(x1, m_clip_rect.left(), m_clip_rect.right());
    m_min_col = AK::min(m_min_col, first_index);
    m_max_col = AK::max(m_max_col, last_index);
    auto start_coverage = AK::clamp(first_index + 1 - x0, 0, 1);
    auto end_coverage = AK::clamp(x1 - last_index, 0, 1);
    if (first_index == last_index) {
        m_coverage[first_index] += (AK::clamp(start_coverage * end_coverage, 0, 1) * 255) / m_oversampling;
    } else {
        m_coverage[first_index] += (AK::clamp(start_coverage, 0, 1) * 255) / m_oversampling;
        for (size_t i = first_index + 1; i < last_index; ++i) {
            m_coverage[i] += 255 / m_oversampling;
        }
        m_coverage[last_index] += (AK::clamp(end_coverage, 0, 1) * 255) / m_oversampling;
    }
}

void Rasterizer::fill_scanline_solid(size_t i, Color in_color)
{
    auto row = m_image.scanline(i);
    for (size_t j = m_min_col; j <= m_max_col; ++j) {
        auto color = in_color.with_alpha((uint16_t(in_color.alpha()) * m_coverage[j]) >> 8);
        row[j] = Color::from_argb(row[j]).blend(color).value();
    }
}

void Rasterizer::fill_scanline_linear_gradient(size_t i, LinearGradient const& gradient)
{
    auto row = m_image.scanline(i);
    for (size_t j = m_min_col; j <= m_max_col; ++j) {
        auto in_color = gradient({ j, i });
        auto color = in_color.with_alpha((uint16_t(in_color.alpha()) * m_coverage[j]) >> 8);
        row[j] = Color::from_argb(row[j]).blend(color).value();
    }
}

}
