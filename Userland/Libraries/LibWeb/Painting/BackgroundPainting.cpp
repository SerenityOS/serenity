/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/GradientPainting.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/RecordingPainter.h>

namespace Web::Painting {

// https://drafts.csswg.org/css-images/#default-sizing
static CSSPixelSize run_default_sizing_algorithm(
    Optional<CSSPixels> specified_width, Optional<CSSPixels> specified_height,
    Optional<CSSPixels> natural_width, Optional<CSSPixels> natural_height,
    Optional<CSSPixelFraction> natural_aspect_ratio,
    CSSPixelSize default_size)
{
    // If the specified size is a definite width and height, the concrete object size is given that width and height.
    if (specified_width.has_value() && specified_height.has_value())
        return CSSPixelSize { specified_width.value(), specified_height.value() };
    // If the specified size is only a width or height (but not both) then the concrete object size is given that specified width or height.
    // The other dimension is calculated as follows:
    if (specified_width.has_value() || specified_height.has_value()) {
        // 1. If the object has a natural aspect ratio,
        // the missing dimension of the concrete object size is calculated using that aspect ratio and the present dimension.
        if (natural_aspect_ratio.has_value() && !natural_aspect_ratio->might_be_saturated()) {
            if (specified_width.has_value())
                return CSSPixelSize { specified_width.value(), (CSSPixels(1) / natural_aspect_ratio.value()) * specified_width.value() };
            if (specified_height.has_value())
                return CSSPixelSize { specified_height.value() * natural_aspect_ratio.value(), specified_height.value() };
        }
        // 2. Otherwise, if the missing dimension is present in the object’s natural dimensions,
        // the missing dimension is taken from the object’s natural dimensions.
        if (specified_height.has_value() && natural_width.has_value())
            return CSSPixelSize { natural_width.value(), specified_height.value() };
        if (specified_width.has_value() && natural_height.has_value())
            return CSSPixelSize { specified_width.value(), natural_height.value() };
        // 3. Otherwise, the missing dimension of the concrete object size is taken from the default object size.
        if (specified_height.has_value())
            return CSSPixelSize { default_size.width(), specified_height.value() };
        if (specified_width.has_value())
            return CSSPixelSize { specified_width.value(), default_size.height() };
        VERIFY_NOT_REACHED();
    }
    // If the specified size has no constraints:
    // 1. If the object has a natural height or width, its size is resolved as if its natural dimensions were given as the specified size.
    if (natural_width.has_value() || natural_height.has_value())
        return run_default_sizing_algorithm(natural_width, natural_height, natural_width, natural_height, natural_aspect_ratio, default_size);
    // FIXME: 2. Otherwise, its size is resolved as a contain constraint against the default object size.
    return default_size;
}

// https://www.w3.org/TR/css-backgrounds-3/#backgrounds
void paint_background(PaintContext& context, Layout::NodeWithStyleAndBoxModelMetrics const& layout_node, CSSPixelRect const& border_rect, Color background_color, CSS::ImageRendering image_rendering, Vector<CSS::BackgroundLayerData> const* background_layers, BorderRadiiData const& border_radii, Vector<Gfx::Path> const& clip_paths)
{
    auto& painter = context.recording_painter();

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

    painter.fill_rect_with_rounded_corners(
        context.rounded_device_rect(color_box.rect).to_type<int>(),
        background_color,
        color_box.radii.top_left.as_corner(context),
        color_box.radii.top_right.as_corner(context),
        color_box.radii.bottom_right.as_corner(context),
        color_box.radii.bottom_left.as_corner(context),
        clip_paths);

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
        RecordingPainterStateSaver state { painter };

        // Clip
        auto clip_box = get_box(layer.clip);

        CSSPixelRect const& css_clip_rect = clip_box.rect;
        auto clip_rect = context.rounded_device_rect(css_clip_rect);
        painter.add_clip_rect(clip_rect.to_type<int>());
        ScopedCornerRadiusClip corner_clip { context, clip_rect, clip_box.radii };

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
            background_positioning_area = layout_node.root().navigable()->viewport_rect();
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

        Optional<CSSPixels> specified_width {};
        Optional<CSSPixels> specified_height {};
        if (layer.size_type == CSS::BackgroundSize::LengthPercentage) {
            if (!layer.size_x.is_auto())
                specified_width = layer.size_x.to_px(layout_node, background_positioning_area.width());
            if (!layer.size_y.is_auto())
                specified_height = layer.size_y.to_px(layout_node, background_positioning_area.height());
        }
        auto concrete_image_size = run_default_sizing_algorithm(
            specified_width, specified_height,
            image.natural_width(), image.natural_height(), image.natural_aspect_ratio(),
            background_positioning_area.size());

        // If any of these are zero, the NaNs will pop up in the painting code.
        if (background_positioning_area.is_empty() || concrete_image_size.is_empty())
            continue;

        // Size
        CSSPixelRect image_rect;
        switch (layer.size_type) {
        case CSS::BackgroundSize::Contain: {
            double max_width_ratio = (background_positioning_area.width() / concrete_image_size.width()).to_double();
            double max_height_ratio = (background_positioning_area.height() / concrete_image_size.height()).to_double();
            double ratio = min(max_width_ratio, max_height_ratio);
            image_rect.set_size(concrete_image_size.width().scaled(ratio), concrete_image_size.height().scaled(ratio));
            break;
        }
        case CSS::BackgroundSize::Cover: {
            double max_width_ratio = (background_positioning_area.width() / concrete_image_size.width()).to_double();
            double max_height_ratio = (background_positioning_area.height() / concrete_image_size.height()).to_double();
            double ratio = max(max_width_ratio, max_height_ratio);
            image_rect.set_size(concrete_image_size.width().scaled(ratio), concrete_image_size.height().scaled(ratio));
            break;
        }
        case CSS::BackgroundSize::LengthPercentage:
            image_rect.set_size(concrete_image_size);
            break;
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
                    image_rect.set_width(image_rect.height() * (concrete_image_size.width() / concrete_image_size.height()));
                }
                if (layer.size_y.is_auto()) {
                    image_rect.set_height(image_rect.width() * (concrete_image_size.height() / concrete_image_size.width()));
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
                x_step = image_rect.width() + CSSPixels::nearest_value_for(space / static_cast<double>(whole_images - 1));
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
                y_step = image_rect.height() + CSSPixels::nearest_value_for(static_cast<double>(space) / static_cast<double>(whole_images - 1));
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

        auto for_each_image_device_rect = [&](auto callback) {
            while (image_y < css_clip_rect.bottom()) {
                image_rect.set_y(image_y);

                auto image_x = initial_image_x;
                while (image_x < css_clip_rect.right()) {
                    image_rect.set_x(image_x);
                    auto image_device_rect = context.rounded_device_rect(image_rect);
                    callback(image_device_rect);
                    if (!repeat_x)
                        break;
                    image_x += x_step;
                }

                if (!repeat_y)
                    break;
                image_y += y_step;
            }
        };

        if (auto color = image.color_if_single_pixel_bitmap(); color.has_value()) {
            // OPTIMIZATION: If the image is a single pixel, we can just fill the whole area with it.
            //               However, we must first figure out the real coverage area, taking repeat etc into account.

            // FIXME: This could be written in a far more efficient way.
            auto fill_rect = Optional<DevicePixelRect> {};
            for_each_image_device_rect([&](auto const& image_device_rect) {
                if (!fill_rect.has_value()) {
                    fill_rect = image_device_rect;
                } else {
                    fill_rect = fill_rect->united(image_device_rect);
                }
            });
            painter.fill_rect(fill_rect->to_type<int>(), color.value(), clip_paths);
        } else {
            for_each_image_device_rect([&](auto const& image_device_rect) {
                image.paint(context, image_device_rect, image_rendering, clip_paths);
            });
        }
    }
}

}
