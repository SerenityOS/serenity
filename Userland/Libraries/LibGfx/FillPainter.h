/*
 * Copyright (c) 2022, Daniel Oberhoff <daniel@danieloberhoff.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rasterizer.h>

namespace Gfx {

class FillPainter {
public:
    FillPainter(Gfx::Bitmap& image)
        : m_rasterizer { image }
    {
    }

    void begin(Point<float> p)
    {
        m_first_point = m_current_point = p;
    }

    void edge_to(Point<float> p)
    {
        m_rasterizer.add_edge({ m_current_point, p });
        m_current_point = p;
    }

    void end()
    {
        m_rasterizer.add_edge({ m_current_point, m_first_point });
    }

    void end_path(Rasterizer::Paint const& paint)
    {
        m_rasterizer.rasterize_edges(Rasterizer::FillRule::evenodd, paint);
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
    Rasterizer m_rasterizer;
    Point<float> m_first_point;
    Point<float> m_current_point;
};

}
