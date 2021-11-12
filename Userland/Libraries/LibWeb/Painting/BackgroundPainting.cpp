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

void paint_background(PaintContext& context, Gfx::IntRect const& background_rect, Color background_color, Vector<CSS::BackgroundLayerData> const* background_layers, BorderRadiusData const& border_radius)
{
    // FIXME: Support elliptical corners
    context.painter().fill_rect_with_rounded_corners(background_rect, background_color, border_radius.top_left, border_radius.top_right, border_radius.bottom_right, border_radius.bottom_left);

    if (!background_layers)
        return;

    // Note: Background layers are ordered front-to-back, so we paint them in reverse
    for (int layer_index = background_layers->size() - 1; layer_index >= 0; layer_index--) {
        auto& layer = background_layers->at(layer_index);
        if (!layer.image || !layer.image->bitmap())
            continue;
        auto& image = *layer.image->bitmap();

        auto image_rect = background_rect;
        switch (layer.repeat_x) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            // The background rect is already sized to align with 'repeat'.
            break;
        case CSS::Repeat::NoRepeat:
            image_rect.set_width(min(image_rect.width(), image.width()));
            break;
        }

        switch (layer.repeat_y) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            // The background rect is already sized to align with 'repeat'.
            break;
        case CSS::Repeat::NoRepeat:
            image_rect.set_height(min(image_rect.height(), image.height()));
            break;
        }

        // FIXME: Handle rounded corners
        context.painter().blit_tiled(image_rect, image, image.rect());
    }
}

}
