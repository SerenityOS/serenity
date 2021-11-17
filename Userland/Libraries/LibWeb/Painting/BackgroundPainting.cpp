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
        // TODO: Gradients!
        if (!layer.image || !layer.image->bitmap())
            continue;
        auto& image = *layer.image->bitmap();

        // Clip
        auto clip_rect = get_box(layer.clip);
        painter.save();
        painter.add_clip_rect(clip_rect);

        // FIXME: Attachment

        // Origin
        auto background_positioning_area = get_box(layer.origin);

        // Size
        Gfx::IntRect image_rect;
        switch (layer.size_type) {
        case CSS::BackgroundSize::Contain: {
            float max_width_ratio = (float)background_positioning_area.width() / (float)image.width();
            float max_height_ratio = (float)background_positioning_area.height() / (float)image.height();
            float ratio = min(max_width_ratio, max_height_ratio);
            image_rect.set_size(roundf(image.width() * ratio), roundf(image.height() * ratio));
            break;
        }
        case CSS::BackgroundSize::Cover: {
            float max_width_ratio = (float)background_positioning_area.width() / (float)image.width();
            float max_height_ratio = (float)background_positioning_area.height() / (float)image.height();
            float ratio = max(max_width_ratio, max_height_ratio);
            image_rect.set_size(roundf(image.width() * ratio), roundf(image.height() * ratio));
            break;
        }
        case CSS::BackgroundSize::LengthPercentage: {
            int width;
            int height;
            if (layer.size_x.is_auto() && layer.size_y.is_auto()) {
                width = image.width();
                height = image.height();
            } else if (layer.size_x.is_auto()) {
                height = layer.size_y.resolved_or_zero(layout_node, background_positioning_area.height()).to_px(layout_node);
                width = roundf(image.width() * ((float)height / (float)image.height()));
            } else if (layer.size_y.is_auto()) {
                width = layer.size_x.resolved_or_zero(layout_node, background_positioning_area.width()).to_px(layout_node);
                height = roundf(image.height() * ((float)width / (float)image.width()));
            } else {
                width = layer.size_x.resolved_or_zero(layout_node, background_positioning_area.width()).to_px(layout_node);
                height = layer.size_y.resolved_or_zero(layout_node, background_positioning_area.height()).to_px(layout_node);
            }

            image_rect.set_size(width, height);
            break;
        }
        }

        int space_x = background_positioning_area.width() - image_rect.width();
        int space_y = background_positioning_area.height() - image_rect.height();

        // Position
        int offset_x = layer.position_offset_x.resolved_or_zero(layout_node, space_x).to_px(layout_node);
        if (layer.position_edge_x == CSS::PositionEdge::Right) {
            image_rect.set_right_without_resize(background_positioning_area.right() - offset_x);
        } else {
            image_rect.set_left(background_positioning_area.left() + offset_x);
        }

        int offset_y = layer.position_offset_y.resolved_or_zero(layout_node, space_y).to_px(layout_node);
        if (layer.position_edge_y == CSS::PositionEdge::Bottom) {
            image_rect.set_bottom_without_resize(background_positioning_area.bottom() - offset_y);
        } else {
            image_rect.set_top(background_positioning_area.top() + offset_y);
        }

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
