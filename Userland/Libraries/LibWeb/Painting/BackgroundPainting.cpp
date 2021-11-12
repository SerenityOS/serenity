/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_background(PaintContext& context, Layout::NodeWithStyleAndBoxModelMetrics const& layout_node, Gfx::IntRect const& border_rect, Color background_color, Vector<CSS::BackgroundLayerData> const* background_layers, BorderRadiusData const& border_radius)
{
    auto& painter = context.painter();

    auto get_box = [&](CSS::BackgroundBox box) {
        auto box_rect = border_rect;
        switch (box) {
        case CSS::BackgroundBox::ContentBox: {
            auto& padding = layout_node.box_model().padding;
            box_rect.shrink(padding.top, padding.right, padding.bottom, padding.left);
            [[fallthrough]];
        }
        case CSS::BackgroundBox::PaddingBox: {
            auto& border = layout_node.box_model().border;
            box_rect.shrink(border.top, border.right, border.bottom, border.left);
            [[fallthrough]];
        }
        case CSS::BackgroundBox::BorderBox:
        default:
            return box_rect;
        }
    };

    auto color_rect = border_rect;
    if (background_layers && !background_layers->is_empty())
        color_rect = get_box(background_layers->last().clip);
    // FIXME: Support elliptical corners
    painter.fill_rect_with_rounded_corners(color_rect, background_color, border_radius.top_left, border_radius.top_right, border_radius.bottom_right, border_radius.bottom_left);

    if (!background_layers)
        return;

    // Note: Background layers are ordered front-to-back, so we paint them in reverse
    for (int layer_index = background_layers->size() - 1; layer_index >= 0; layer_index--) {
        auto& layer = background_layers->at(layer_index);
        if (!layer.image || !layer.image->bitmap())
            continue;
        auto& image = *layer.image->bitmap();

        // Clip
        auto clip_rect = get_box(layer.clip);
        painter.save();
        painter.add_clip_rect(clip_rect);

        auto painting_rect = border_rect;

        // FIXME: Attachment
        // FIXME: Size
        int scaled_width = image.width();
        int scaled_height = image.height();

        // FIXME: Origin
        // FIXME: Position

        // Repetition
        switch (layer.repeat_x) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            // The background rect is already sized to align with 'repeat'.
            break;
        case CSS::Repeat::NoRepeat:
            painting_rect.set_width(min(painting_rect.width(), scaled_width));
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
            painting_rect.set_height(min(painting_rect.height(), scaled_height));
            break;
        }

        // FIXME: Handle rounded corners
        painter.blit_tiled(painting_rect, image, image.rect());
        painter.restore();
    }
}

}
