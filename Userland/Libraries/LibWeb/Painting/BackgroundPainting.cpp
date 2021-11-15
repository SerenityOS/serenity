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

        // FIXME: Attachment
        // FIXME: Size
        Gfx::IntRect image_rect { border_rect.x(), border_rect.y(), image.width(), image.height() };

        // FIXME: Origin
        // FIXME: Position

        // Repetition
        bool repeat_x = false;
        bool repeat_y = false;
        float x_step = 0;
        float y_step = 0;

        switch (layer.repeat_x) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            x_step = image_rect.width();
            repeat_x = true;
            break;
        case CSS::Repeat::NoRepeat:
            repeat_x = false;
            break;
        }
        // Move image_rect to the left-most tile position that is still visible
        if (repeat_x && image_rect.x() > clip_rect.x()) {
            auto x_delta = floorf(x_step * ceilf((image_rect.x() - clip_rect.x()) / x_step));
            image_rect.set_x(image_rect.x() - x_delta);
        }

        switch (layer.repeat_y) {
        case CSS::Repeat::Round:
        case CSS::Repeat::Space:
            // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
        case CSS::Repeat::Repeat:
            y_step = image_rect.height();
            repeat_y = true;
            break;
        case CSS::Repeat::NoRepeat:
            repeat_y = false;
            break;
        }
        // Move image_rect to the top-most tile position that is still visible
        if (repeat_y && image_rect.y() > clip_rect.y()) {
            auto y_delta = floorf(y_step * ceilf((image_rect.y() - clip_rect.y()) / y_step));
            image_rect.set_y(image_rect.y() - y_delta);
        }

        // FIXME: Handle rounded corners
        float initial_image_x = image_rect.x();
        float image_y = image_rect.y();
        while (image_y < clip_rect.bottom()) {
            image_rect.set_y(roundf(image_y));

            float image_x = initial_image_x;
            while (image_x < clip_rect.right()) {
                image_rect.set_x(roundf(image_x));
                painter.draw_scaled_bitmap(image_rect, image, image.rect(), 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
                if (!repeat_x)
                    break;
                image_x += x_step;
            }

            if (!repeat_y)
                break;
            image_y += y_step;
        }

        painter.restore();
    }
}

}
