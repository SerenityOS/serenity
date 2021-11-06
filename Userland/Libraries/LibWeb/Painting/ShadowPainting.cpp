/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void paint_box_shadow(PaintContext& context, Gfx::IntRect const& content_rect, BoxShadowData const& box_shadow_data)
{
    Gfx::IntRect bitmap_rect = {
        0,
        0,
        content_rect.width() + 4 * box_shadow_data.blur_radius,
        content_rect.height() + 4 * box_shadow_data.blur_radius
    };

    Gfx::IntPoint blur_rect_position = {
        content_rect.x() - 2 * box_shadow_data.blur_radius + box_shadow_data.offset_x,
        content_rect.y() - 2 * box_shadow_data.blur_radius + box_shadow_data.offset_y
    };

    if (bitmap_rect.is_empty())
        return;

    auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, bitmap_rect.size());
    if (bitmap_or_error.is_error()) {
        dbgln("Unable to allocate temporary bitmap for box-shadow rendering: {}", bitmap_or_error.error());
        return;
    }
    auto new_bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();

    Gfx::Painter painter(*new_bitmap);
    painter.fill_rect({ { 2 * box_shadow_data.blur_radius, 2 * box_shadow_data.blur_radius }, content_rect.size() }, box_shadow_data.color);

    Gfx::FastBoxBlurFilter filter(*new_bitmap);
    filter.apply_three_passes(box_shadow_data.blur_radius);

    Gfx::DisjointRectSet rect_set;
    rect_set.add(bitmap_rect);
    auto shattered = rect_set.shatter({ content_rect.location() - blur_rect_position, content_rect.size() });

    for (auto& rect : shattered.rects())
        context.painter().blit(rect.location() + blur_rect_position, *new_bitmap, rect);
}

}
