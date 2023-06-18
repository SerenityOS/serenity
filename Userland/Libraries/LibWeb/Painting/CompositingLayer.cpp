/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/CompositingLayer.h>

namespace Web::Painting {

Tile::Tile(int x, int y)
    : m_x(x)
    , m_y(y)
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { Tile::Size, Tile::Size }).release_value_but_fixme_should_propagate_errors();
}

Tile& CompositingLayer::tile(int x, int y)
{
    auto it = m_tiles.find_if([&](Tile const& tile) {
        return tile.x() == x && tile.y() == y;
    });

    if (it == m_tiles.end()) {
        Tile tile(x, y);
        m_tiles.append(move(tile));
        return m_tiles.last();
    }

    return *it;
}

void CompositingLayer::invalidate(DevicePixelRect rect)
{
    for (auto& tile : m_tiles) {
        if (tile.rect().intersects(rect))
            tile.set_needs_repaint(true);
    }
}

void CompositingLayer::paint(PaintContext& context, DevicePixelRect viewport_rect)
{
    int start_tile_x, end_tile_x, start_tile_y, end_tile_y;
    if (is_fixed_position()) {
        start_tile_x = 0;
        end_tile_x = static_cast<int>(viewport_rect.width() / Tile::Size);
        start_tile_y = 0;
        end_tile_y = static_cast<int>(viewport_rect.height() / Tile::Size);
    } else {
        start_tile_x = static_cast<int>(viewport_rect.x() / Tile::Size);
        end_tile_x = static_cast<int>((viewport_rect.x() + viewport_rect.width()) / Tile::Size);
        start_tile_y = static_cast<int>(viewport_rect.y() / Tile::Size);
        end_tile_y = static_cast<int>((viewport_rect.y() + viewport_rect.height()) / Tile::Size);
    }

    for (auto tile_x = start_tile_x; tile_x <= end_tile_x; tile_x++) {
        for (auto tile_y = start_tile_y; tile_y <= end_tile_y; tile_y++) {
            auto& tile = this->tile(tile_x, tile_y);
            for (auto& stacking_context : m_stacking_contexts) {
                if (!tile.needs_repaint())
                    continue;
                tile.set_needs_repaint(false);

                Gfx::Painter tile_painter(tile.bitmap());
                tile_painter.translate(-tile.rect().location().to_type<int>());
                auto tile_paint_context = context.clone(tile_painter);
                stacking_context->paint(tile_paint_context);
            }

            DevicePixelPoint position = is_fixed_position() ? tile.rect().location() : (tile.rect().location() - viewport_rect.location());
            context.painter().blit(position.to_type<int>(), tile.bitmap(), { 0, 0, static_cast<int>(Tile::Size), static_cast<int>(Tile::Size) });
        }
    }
}

}
