/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void paint_box_shadow(PaintContext& context, Gfx::IntRect const& content_rect, Vector<BoxShadowData> const& box_shadow_layers)
{
    if (box_shadow_layers.is_empty())
        return;

    auto& painter = context.painter();

    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (int layer_index = box_shadow_layers.size() - 1; layer_index >= 0; layer_index--) {
        auto& box_shadow_data = box_shadow_layers[layer_index];
        // FIXME: Paint inset shadows.
        if (box_shadow_data.placement != BoxShadowPlacement::Outer)
            continue;

        // FIXME: Account for rounded corners.

        auto fill_rect_masked = [](auto& painter, auto fill_rect, auto mask_rect, auto color) {
            Gfx::DisjointRectSet rect_set;
            rect_set.add(fill_rect);
            auto shattered = rect_set.shatter(mask_rect);
            for (auto& rect : shattered.rects())
                painter.fill_rect(rect, color);
        };

        // If there's no blurring, we can save a lot of effort.
        if (box_shadow_data.blur_radius == 0) {
            fill_rect_masked(painter, content_rect.inflated(box_shadow_data.spread_distance, box_shadow_data.spread_distance, box_shadow_data.spread_distance, box_shadow_data.spread_distance).translated(box_shadow_data.offset_x, box_shadow_data.offset_y), content_rect, box_shadow_data.color);
            continue;
        }

        auto expansion = box_shadow_data.spread_distance - (box_shadow_data.blur_radius * 2);
        Gfx::IntRect solid_rect = {
            content_rect.x() + box_shadow_data.offset_x - expansion,
            content_rect.y() + box_shadow_data.offset_y - expansion,
            content_rect.width() + 2 * expansion,
            content_rect.height() + 2 * expansion
        };
        fill_rect_masked(painter, solid_rect, content_rect, box_shadow_data.color);

        // Calculating and blurring the box-shadow full size is expensive, and wasteful - aside from the corners,
        // all vertical strips of the shadow are identical, and the same goes for horizontal ones.
        // So instead, we generate a shadow bitmap that is just large enough to include the corners and 1px of
        // non-corner, and then we repeatedly blit sections of it. This is similar to a NinePatch on Android.

        auto corner_size = box_shadow_data.blur_radius * 4;
        Gfx::IntRect corner_rect { 0, 0, corner_size, corner_size };
        Gfx::IntRect all_corners_rect { 0, 0, corner_size * 2 + 1, corner_size * 2 + 1 };
        Gfx::IntRect left_edge_rect { 0, corner_size, corner_size, 1 };
        Gfx::IntRect right_edge_rect { all_corners_rect.width() - corner_size, corner_size, corner_size, 1 };
        Gfx::IntRect top_edge_rect { corner_size, 0, 1, corner_size };
        Gfx::IntRect bottom_edge_rect { corner_size, all_corners_rect.height() - corner_size, 1, corner_size };

        auto shadows_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, all_corners_rect.size());
        if (shadows_bitmap.is_error()) {
            dbgln("Unable to allocate temporary bitmap for box-shadow rendering: {}", shadows_bitmap.error());
            return;
        }
        auto shadow_bitmap = shadows_bitmap.release_value();
        Gfx::Painter corner_painter { *shadow_bitmap };
        auto double_radius = box_shadow_data.blur_radius * 2;
        corner_painter.fill_rect(all_corners_rect.shrunken(double_radius, double_radius, double_radius, double_radius), box_shadow_data.color);
        Gfx::FastBoxBlurFilter filter(*shadow_bitmap);
        filter.apply_three_passes(box_shadow_data.blur_radius);

        auto left_start = solid_rect.left() - corner_size;
        auto right_start = solid_rect.left() + solid_rect.width();
        auto top_start = solid_rect.top() - corner_size;
        auto bottom_start = solid_rect.top() + solid_rect.height();

        // FIXME: Painter only lets us define a clip-rect which discards drawing outside of it, whereas here we want
        //        a rect which discards drawing inside it. So, we run the draw operations 4 times with clip-rects
        //        covering each side of the content_rect exactly once.
        auto paint_shadow = [&](Gfx::IntRect clip_rect) {
            painter.save();
            painter.add_clip_rect(clip_rect);

            // Paint corners
            painter.blit({ left_start, top_start }, shadow_bitmap, corner_rect);
            painter.blit({ right_start, top_start }, shadow_bitmap, corner_rect.translated(corner_rect.width() + 1, 0));
            painter.blit({ left_start, bottom_start }, shadow_bitmap, corner_rect.translated(0, corner_rect.height() + 1));
            painter.blit({ right_start, bottom_start }, shadow_bitmap, corner_rect.translated(corner_rect.width() + 1, corner_rect.height() + 1));

            // Horizontal edges
            for (auto y = solid_rect.top(); y <= solid_rect.bottom(); ++y) {
                painter.blit({ left_start, y }, shadow_bitmap, left_edge_rect);
                painter.blit({ right_start, y }, shadow_bitmap, right_edge_rect);
            }

            // Vertical edges
            for (auto x = solid_rect.left(); x <= solid_rect.right(); ++x) {
                painter.blit({ x, top_start }, shadow_bitmap, top_edge_rect);
                painter.blit({ x, bottom_start }, shadow_bitmap, bottom_edge_rect);
            }

            painter.restore();
        };

        // Everything above content_rect, including sides
        paint_shadow({ 0, 0, painter.target()->width(), content_rect.top() });

        // Everything below content_rect, including sides
        paint_shadow({ 0, content_rect.bottom() + 1, painter.target()->width(), painter.target()->height() });

        // Everything directly to the left of content_rect
        paint_shadow({ 0, content_rect.top(), content_rect.left(), content_rect.height() });

        // Everything directly to the right of content_rect
        paint_shadow({ content_rect.right() + 1, content_rect.top(), painter.target()->width(), content_rect.height() });
    }
}

}
