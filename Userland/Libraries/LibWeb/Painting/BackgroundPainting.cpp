/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/GradientPainting.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

// https://www.w3.org/TR/css-backgrounds-3/#backgrounds
void paint_background(PaintContext& context, Layout::NodeWithStyleAndBoxModelMetrics const& layout_node, CSSPixelRect const& border_rect, Color background_color, CSS::ImageRendering image_rendering, Vector<CSS::BackgroundLayerData> const* background_layers, BorderRadiiData const& border_radii)
{
    auto& painter = context.painter();

    struct BackgroundBox {
        CSSPixelRect rect;
        BorderRadiiData radii;

        inline void shrink(CSSPixels top, CSSPixels right, CSSPixels bottom, CSSPixels left)
        {
            rect.shrink(top, right, bottom, left);
            radii.shrink(top, right, bottom, left);
        }
    };

    BackgroundBox border_box {
        border_rect,
        border_radii
    };

    auto get_box = [&](CSS::BackgroundBox box_clip) {
        auto box = border_box;
        switch (box_clip) {
        case CSS::BackgroundBox::ContentBox: {
            auto& padding = layout_node.box_model().padding;
            box.shrink(padding.top, padding.right, padding.bottom, padding.left);
            [[fallthrough]];
        }
        case CSS::BackgroundBox::PaddingBox: {
            auto& border = layout_node.box_model().border;
            box.shrink(border.top, border.right, border.bottom, border.left);
            [[fallthrough]];
        }
        case CSS::BackgroundBox::BorderBox:
        default:
            return box;
        }
    };

    auto color_box = border_box;
    if (background_layers && !background_layers->is_empty())
        color_box = get_box(background_layers->last().clip);

    auto layer_is_paintable = [&](auto& layer) {
        return layer.background_image && layer.background_image->is_paintable();
    };

    bool has_paintable_layers = false;
    if (background_layers) {
        for (auto& layer : *background_layers) {
            if (layer_is_paintable(layer)) {
                has_paintable_layers = true;
                break;
            }
        }
    }

    Gfx::AntiAliasingPainter aa_painter { painter };
    aa_painter.fill_rect_with_rounded_corners(context.rounded_device_rect(color_box.rect).to_type<int>(),
        background_color, color_box.radii.top_left.as_corner(context), color_box.radii.top_right.as_corner(context), color_box.radii.bottom_right.as_corner(context), color_box.radii.bottom_left.as_corner(context));

    if (!has_paintable_layers)
        return;

    struct {
        DevicePixels top { 0 };
        DevicePixels bottom { 0 };
        DevicePixels left { 0 };
        DevicePixels right { 0 };
    } clip_shrink;

    auto border_top = layout_node.computed_values().border_top();
    auto border_bottom = layout_node.computed_values().border_bottom();
    auto border_left = layout_node.computed_values().border_left();
    auto border_right = layout_node.computed_values().border_right();

    if (border_top.color.alpha() == 255 && border_bottom.color.alpha() == 255
        && border_left.color.alpha() == 255 && border_right.color.alpha() == 255) {
        clip_shrink.top = context.rounded_device_pixels(border_top.width);
        clip_shrink.bottom = context.rounded_device_pixels(border_bottom.width);
        clip_shrink.left = context.rounded_device_pixels(border_left.width);
        clip_shrink.right = context.rounded_device_pixels(border_right.width);
    }

    // Note: Background layers are ordered front-to-back, so we paint them in reverse
    for (auto& layer : background_layers->in_reverse()) {
        if (!layer_is_paintable(layer))
            continue;
        Gfx::PainterStateSaver state { painter };

        // Clip
        auto clip_box = get_box(layer.clip);

        CSSPixelRect const& css_clip_rect = clip_box.rect;
        auto clip_rect = context.rounded_device_rect(css_clip_rect);
        painter.add_clip_rect(clip_rect.to_type<int>());
        ScopedCornerRadiusClip corner_clip { context, painter, clip_rect, clip_box.radii };

        if (layer.clip == CSS::BackgroundBox::BorderBox) {
            // Shrink the effective clip rect if to account for the bits the borders will definitely paint over
            // (if they all have alpha == 255).
            clip_rect.shrink(clip_shrink.top, clip_shrink.right, clip_shrink.bottom, clip_shrink.left);
        }

        auto& image = *layer.background_image;
        CSSPixelRect background_positioning_area;

        // Attachment and Origin
        switch (layer.attachment) {
        case CSS::BackgroundAttachment::Fixed:
            background_positioning_area = layout_node.root().browsing_context().viewport_rect();
            break;
        case CSS::BackgroundAttachment::Local:
            background_positioning_area = get_box(layer.origin).rect;
            if (is<Layout::Box>(layout_node)) {
                auto* paintable_box = static_cast<Layout::Box const&>(layout_node).paintable_box();
                if (paintable_box) {
                    auto scroll_offset = paintable_box->scroll_offset();
                    background_positioning_area.translate_by(-scroll_offset.x(), -scroll_offset.y());
                }
            }
            break;
        case CSS::BackgroundAttachment::Scroll:
            background_positioning_area = get_box(layer.origin).rect;
            break;
        }

        // FIXME: Implement proper default sizing algorithm: https://drafts.csswg.org/css-images/#default-sizing
        CSSPixels natural_image_width = image.natural_width().value_or(background_positioning_area.width());
        CSSPixels natural_image_height = image.natural_height().value_or(background_positioning_area.height());

        // If any of these are zero, the NaNs will pop up in the painting code.
        if (background_positioning_area.is_empty() || natural_image_height <= 0 || natural_image_width <= 0)
            continue;

        // Size
        CSSPixelRect image_rect;
        switch (layer.size_type) {
        case CSS::BackgroundSize::Contain: {
            double max_width_ratio = (background_positioning_area.width() / natural_image_width).to_double();
            double max_height_ratio = (background_positioning_area.height() / natural_image_height).to_double();
            double ratio = min(max_width_ratio, max_height_ratio);
            image_rect.set_size(natural_image_width.scaled(ratio), natural_image_height.scaled(ratio));
            break;
        }
        case CSS::BackgroundSize::Cover: {
            double max_width_ratio = (background_positioning_area.width() / natural_image_width).to_double();
            double max_height_ratio = (background_positioning_area.height() / natural_image_height).to_double();
            double ratio = max(max_width_ratio, max_height_ratio);
            image_rect.set_size(natural_image_width.scaled(ratio), natural_image_height.scaled(ratio));
            break;
        }
        case CSS::BackgroundSize::LengthPercentage: {
            CSSPixels width;
            CSSPixels height;
            bool x_is_auto = layer.size_x.is_auto();
            bool y_is_auto = layer.size_y.is_auto();
            if (x_is_auto && y_is_auto) {
                width = natural_image_width;
                height = natural_image_height;
            } else if (x_is_auto) {
                height = layer.size_y.to_px(layout_node, background_positioning_area.height());
                width = natural_image_width * (height / natural_image_height);
            } else if (y_is_auto) {
                width = layer.size_x.to_px(layout_node, background_positioning_area.width());
                height = natural_image_height * (width / natural_image_width);
            } else {
                width = layer.size_x.to_px(layout_node, background_positioning_area.width());
                height = layer.size_y.to_px(layout_node, background_positioning_area.height());
            }

            image_rect.set_size(width, height);
            break;
        }
        }

        // If after sizing we have a 0px image, we're done. Attempting to paint this would be an infinite loop.
        if (image_rect.is_empty())
            continue;

        // If background-repeat is round for one (or both) dimensions, there is a second step.
        // The UA must scale the image in that dimension (or both dimensions) so that it fits a
        // whole number of times in the background positioning area.
        if (layer.repeat_x == CSS::Repeat::Round || layer.repeat_y == CSS::Repeat::Round) {
            // If X ≠ 0 is the width of the image after step one and W is the width of the
            // background positioning area, then the rounded width X' = W / round(W / X)
            // where round() is a function that returns the nearest natural number
            // (integer greater than zero).
            if (layer.repeat_x == CSS::Repeat::Round) {
                image_rect.set_width(background_positioning_area.width() / round(background_positioning_area.width() / image_rect.width()));
            }
            if (layer.repeat_y == CSS::Repeat::Round) {
                image_rect.set_height(background_positioning_area.height() / round(background_positioning_area.height() / image_rect.height()));
            }

            // If background-repeat is round for one dimension only and if background-size is auto
            // for the other dimension, then there is a third step: that other dimension is scaled
            // so that the original aspect ratio is restored.
            if (layer.repeat_x != layer.repeat_y) {
                if (layer.size_x.is_auto()) {
                    image_rect.set_width(natural_image_width * (image_rect.height() / natural_image_height));
                }
                if (layer.size_y.is_auto()) {
                    image_rect.set_height(natural_image_height * (image_rect.width() / natural_image_width));
                }
            }
        }

        CSSPixels space_x = background_positioning_area.width() - image_rect.width();
        CSSPixels space_y = background_positioning_area.height() - image_rect.height();

        // Position
        CSSPixels offset_x = layer.position_offset_x.to_px(layout_node, space_x);
        if (layer.position_edge_x == CSS::PositionEdge::Right) {
            image_rect.set_right_without_resize(background_positioning_area.right() - offset_x);
        } else {
            image_rect.set_left(background_positioning_area.left() + offset_x);
        }

        CSSPixels offset_y = layer.position_offset_y.to_px(layout_node, space_y);
        if (layer.position_edge_y == CSS::PositionEdge::Bottom) {
            image_rect.set_bottom_without_resize(background_positioning_area.bottom() - offset_y);
        } else {
            image_rect.set_top(background_positioning_area.top() + offset_y);
        }

        // Repetition
        bool repeat_x = false;
        bool repeat_y = false;
        CSSPixels x_step = 0;
        CSSPixels y_step = 0;

        switch (layer.repeat_x) {
        case CSS::Repeat::Round:
            x_step = image_rect.width();
            repeat_x = true;
            break;
        case CSS::Repeat::Space: {
            int whole_images = (background_positioning_area.width() / image_rect.width()).to_int();
            if (whole_images <= 1) {
                x_step = image_rect.width();
                repeat_x = false;
            } else {
                auto space = fmod(background_positioning_area.width().to_double(), image_rect.width().to_double());
                x_step = image_rect.width() + CSSPixels(space / static_cast<double>(whole_images - 1));
                repeat_x = true;
            }
            break;
        }
        case CSS::Repeat::Repeat:
            x_step = image_rect.width();
            repeat_x = true;
            break;
        case CSS::Repeat::NoRepeat:
            repeat_x = false;
            break;
        }
        // Move image_rect to the left-most tile position that is still visible
        if (repeat_x && image_rect.x() > css_clip_rect.x()) {
            auto x_delta = floor(x_step * ceil((image_rect.x() - css_clip_rect.x()) / x_step));
            image_rect.set_x(image_rect.x() - x_delta);
        }

        switch (layer.repeat_y) {
        case CSS::Repeat::Round:
            y_step = image_rect.height();
            repeat_y = true;
            break;
        case CSS::Repeat::Space: {
            int whole_images = (background_positioning_area.height() / image_rect.height()).to_int();
            if (whole_images <= 1) {
                y_step = image_rect.height();
                repeat_y = false;
            } else {
                auto space = fmod(background_positioning_area.height().to_float(), image_rect.height().to_float());
                y_step = image_rect.height() + CSSPixels(static_cast<double>(space) / static_cast<double>(whole_images - 1));
                repeat_y = true;
            }
            break;
        }
        case CSS::Repeat::Repeat:
            y_step = image_rect.height();
            repeat_y = true;
            break;
        case CSS::Repeat::NoRepeat:
            repeat_y = false;
            break;
        }
        // Move image_rect to the top-most tile position that is still visible
        if (repeat_y && image_rect.y() > css_clip_rect.y()) {
            auto y_delta = floor(y_step * ceil((image_rect.y() - css_clip_rect.y()) / y_step));
            image_rect.set_y(image_rect.y() - y_delta);
        }

        CSSPixels initial_image_x = image_rect.x();
        CSSPixels image_y = image_rect.y();
        Optional<DevicePixelRect> last_image_device_rect;

        image.resolve_for_size(layout_node, image_rect.size());

        while (image_y < css_clip_rect.bottom()) {
            image_rect.set_y(image_y);

            auto image_x = initial_image_x;
            while (image_x < css_clip_rect.right()) {
                image_rect.set_x(image_x);
                auto image_device_rect = context.rounded_device_rect(image_rect);
                if (image_device_rect != last_image_device_rect && !context.would_be_fully_clipped_by_painter(image_device_rect))
                    image.paint(context, image_device_rect, image_rendering);
                last_image_device_rect = image_device_rect;
                if (!repeat_x)
                    break;
                image_x += x_step;
            }

            if (!repeat_y)
                break;
            image_y += y_step;
        }
    }
}

}
