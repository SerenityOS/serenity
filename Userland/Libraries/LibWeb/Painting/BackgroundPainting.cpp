/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_background(PaintContext& context, Gfx::IntRect const& background_rect, BackgroundData const& background_data, BorderRadiusData const& border_radius)
{
    // FIXME: Support elliptical corners
    context.painter().fill_rect_with_rounded_corners(background_rect, background_data.color, border_radius.top_left, border_radius.top_right, border_radius.bottom_right, border_radius.bottom_left);

    // FIXME: Support multiple background layers
    if (background_data.image) {
        auto image_rect = background_rect;
        switch (background_data.repeat_x) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            // The background rect is already sized to align with 'repeat'.
            break;
        case CSS::Repeat::NoRepeat:
            image_rect.set_width(background_data.image->width());
            break;
        }

        switch (background_data.repeat_y) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            // The background rect is already sized to align with 'repeat'.
            break;
        case CSS::Repeat::NoRepeat:
            image_rect.set_height(background_data.image->height());
            break;
        }

        // FIXME: Handle rounded corners
        context.painter().blit_tiled(image_rect, *background_data.image, background_data.image->rect());
    }
}

}
