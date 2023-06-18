/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/PixelUnits.h>

namespace Web::Painting {

class Tile {
public:
    static constexpr DevicePixels Size = 1024;

    Tile(int x, int y);

    int x() const { return m_x; }
    int y() const { return m_y; }

    Gfx::Bitmap& bitmap() { return *m_bitmap; }

    bool needs_repaint() const { return m_needs_repaint; }
    void set_needs_repaint(bool b) { m_needs_repaint = b; }

    DevicePixelRect rect() const
    {
        return { m_x * Size, m_y * Size, Size, Size };
    }

private:
    int m_x;
    int m_y;
    RefPtr<Gfx::Bitmap> m_bitmap;
    bool m_needs_repaint { true };
};

class CompositingLayer {
public:
    CompositingLayer(bool is_fixed_position)
        : m_is_fixed_position(is_fixed_position) {};

    Tile& tile(int x, int y);

    void invalidate(DevicePixelRect rect);

    void add_stacking_context(StackingContext* stacking_context)
    {
        m_stacking_contexts.append(stacking_context);
    }

    void paint(PaintContext& context, DevicePixelRect viewport_rect_int);

    bool is_fixed_position() const { return m_is_fixed_position; }

private:
    Vector<Tile> m_tiles;
    Vector<StackingContext*> m_stacking_contexts;
    bool m_is_fixed_position { false };
};

}
