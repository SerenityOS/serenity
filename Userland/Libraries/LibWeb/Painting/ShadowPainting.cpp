/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Filters/StackBlurFilter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/LineBoxFragment.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

static void paint_inner_box_shadow(PaintContext& context, CSSPixelRect const& content_rect,
    BordersData const& borders_data, BorderRadiiData const& border_radii, ShadowData const& box_shadow_data)
{
    auto& painter = context.painter();
    auto device_content_rect = context.rounded_device_rect(content_rect);

    auto border_radii_shrunken = border_radii;
    border_radii_shrunken.shrink(borders_data.top.width, borders_data.right.width, borders_data.bottom.width, borders_data.left.width);
    ScopedCornerRadiusClip corner_clipper { context, painter, device_content_rect, border_radii_shrunken, CornerClip::Outside };
    DevicePixels offset_x = context.rounded_device_pixels(box_shadow_data.offset_x);
    DevicePixels offset_y = context.rounded_device_pixels(box_shadow_data.offset_y);
    DevicePixels blur_radius = context.rounded_device_pixels(box_shadow_data.blur_radius);
    DevicePixels spread_distance = context.rounded_device_pixels(box_shadow_data.spread_distance);
    auto shadows_bitmap_rect = device_content_rect.inflated(
        blur_radius.value() + offset_y.value(),
        blur_radius.value() + abs(offset_x.value()),
        blur_radius.value() + abs(offset_y.value()),
        blur_radius.value() + offset_x.value());
    auto shadows_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, shadows_bitmap_rect.size().to_type<int>());
    if (shadows_bitmap.is_error()) {
        dbgln("Unable to allocate temporary bitmap {} for box-shadow rendering: {}", device_content_rect, shadows_bitmap.error());
        return;
    }
    auto shadow_bitmap = shadows_bitmap.release_value();
    Gfx::Painter shadow_painter { *shadow_bitmap };
    Gfx::AntiAliasingPainter shadow_aa_painter { shadow_painter };
    auto device_content_rect_int = device_content_rect.to_type<int>();
    auto origin_device_content_rect = device_content_rect_int.translated(-device_content_rect_int.x(), -device_content_rect_int.y());
    auto outer_shadow_rect = origin_device_content_rect.translated({ offset_x + blur_radius.value(), offset_y + blur_radius.value() });
    auto spread_distance_value = spread_distance.value();
    auto inner_shadow_rect = outer_shadow_rect.inflated(-spread_distance_value, -spread_distance_value, -spread_distance_value, -spread_distance_value);
    outer_shadow_rect.inflate(
        blur_radius.value() + offset_y.value(),
        blur_radius.value() + abs(offset_x.value()),
        blur_radius.value() + abs(offset_y.value()),
        blur_radius.value() + offset_x.value());
    auto top_left_corner = border_radii_shrunken.top_left.as_corner(context);
    auto top_right_corner = border_radii_shrunken.top_right.as_corner(context);
    auto bottom_right_corner = border_radii_shrunken.bottom_right.as_corner(context);
    auto bottom_left_corner = border_radii_shrunken.bottom_left.as_corner(context);
    shadow_painter.fill_rect(outer_shadow_rect, box_shadow_data.color.with_alpha(0xff));
    if (border_radii_shrunken.has_any_radius()) {
        shadow_aa_painter.fill_rect_with_rounded_corners(inner_shadow_rect, box_shadow_data.color.with_alpha(0xff),
            top_left_corner, top_right_corner, bottom_right_corner, bottom_left_corner,
            Gfx::AntiAliasingPainter::BlendMode::AlphaSubtract);
    } else {
        shadow_painter.clear_rect(inner_shadow_rect, Color::Transparent);
    }
    Gfx::StackBlurFilter filter(*shadow_bitmap);
    filter.process_rgba(blur_radius.value(), box_shadow_data.color);
    Gfx::PainterStateSaver save { painter };
    painter.add_clip_rect(device_content_rect_int);
    painter.blit({ device_content_rect_int.left() - blur_radius.value(), device_content_rect_int.top() - blur_radius.value() },
        *shadow_bitmap, shadow_bitmap->rect(), box_shadow_data.color.alpha() / 255.);
}

static void paint_outer_box_shadow(PaintContext& context, CSSPixelRect const& content_rect,
    BorderRadiiData const& border_radii, ShadowData const& box_shadow_data)
{
    auto& painter = context.painter();
    auto device_content_rect = context.rounded_device_rect(content_rect);

    auto top_left_corner = border_radii.top_left.as_corner(context);
    auto top_right_corner = border_radii.top_right.as_corner(context);
    auto bottom_right_corner = border_radii.bottom_right.as_corner(context);
    auto bottom_left_corner = border_radii.bottom_left.as_corner(context);

    ScopedCornerRadiusClip corner_clipper { context, painter, device_content_rect, border_radii, CornerClip::Inside };

    DevicePixels offset_x = context.rounded_device_pixels(box_shadow_data.offset_x);
    DevicePixels offset_y = context.rounded_device_pixels(box_shadow_data.offset_y);
    DevicePixels blur_radius = context.rounded_device_pixels(box_shadow_data.blur_radius);
    DevicePixels spread_distance = context.rounded_device_pixels(box_shadow_data.spread_distance);

    auto fill_rect_masked = [](auto& painter, auto fill_rect, auto mask_rect, auto color) {
        Gfx::DisjointRectSet<DevicePixels> rect_set;
        rect_set.add(fill_rect);
        auto shattered = rect_set.shatter(mask_rect);
        for (auto& rect : shattered.rects())
            painter.fill_rect(rect.template to_type<int>(), color);
    };

    // Our blur cannot handle radii over 255 so there's no point trying (255 is silly big anyway)
    blur_radius = clamp(blur_radius, 0, 255);

    // If there's no blurring, nor rounded corners, we can save a lot of effort.
    auto non_blurred_shadow_rect = device_content_rect.inflated(spread_distance, spread_distance, spread_distance, spread_distance);
    if (blur_radius == 0 && !border_radii.has_any_radius()) {
        fill_rect_masked(painter, non_blurred_shadow_rect.translated(offset_x, offset_y), device_content_rect, box_shadow_data.color);
        return;
    }

    auto top_left_shadow_corner = top_left_corner;
    auto top_right_shadow_corner = top_right_corner;
    auto bottom_right_shadow_corner = bottom_right_corner;
    auto bottom_left_shadow_corner = bottom_left_corner;

    auto spread_corner = [&](auto& corner) {
        if (corner) {
            corner.horizontal_radius += spread_distance.value();
            corner.vertical_radius += spread_distance.value();
        }
    };

    spread_corner(top_left_shadow_corner);
    spread_corner(top_right_shadow_corner);
    spread_corner(bottom_right_shadow_corner);
    spread_corner(bottom_left_shadow_corner);

    auto expansion = spread_distance - (blur_radius * 2);
    DevicePixelRect inner_bounding_rect = {
        device_content_rect.x() + offset_x - expansion,
        device_content_rect.y() + offset_y - expansion,
        device_content_rect.width() + 2 * expansion,
        device_content_rect.height() + 2 * expansion
    };

    // Calculating and blurring the box-shadow full size is expensive, and wasteful - aside from the corners,
    // all vertical strips of the shadow are identical, and the same goes for horizontal ones.
    // So instead, we generate a shadow bitmap that is just large enough to include the corners and 1px of
    // non-corner, and then we repeatedly blit sections of it. This is similar to a NinePatch on Android.
    auto double_radius = blur_radius * 2;
    auto blurred_edge_thickness = blur_radius * 4;

    auto default_corner_size = Gfx::IntSize { double_radius, double_radius };
    auto top_left_corner_size = (top_left_shadow_corner ? top_left_shadow_corner.as_rect().size() : default_corner_size).to_type<DevicePixels>();
    auto top_right_corner_size = (top_right_shadow_corner ? top_right_shadow_corner.as_rect().size() : default_corner_size).to_type<DevicePixels>();
    auto bottom_left_corner_size = (bottom_left_shadow_corner ? bottom_left_shadow_corner.as_rect().size() : default_corner_size).to_type<DevicePixels>();
    auto bottom_right_corner_size = (bottom_right_shadow_corner ? bottom_right_shadow_corner.as_rect().size() : default_corner_size).to_type<DevicePixels>();

    auto max_edge_width = non_blurred_shadow_rect.width() / 2;
    auto max_edge_height = non_blurred_shadow_rect.height() / 2;
    auto extra_edge_width = non_blurred_shadow_rect.width() % 2;
    auto extra_edge_height = non_blurred_shadow_rect.height() % 2;

    auto clip_corner_size = [&](auto& size, auto const& corner, DevicePixels x_bonus = 0, DevicePixels y_bonus = 0) {
        auto max_x = (max_edge_width + x_bonus).value();
        auto max_y = (max_edge_height + y_bonus).value();
        auto min_x = max(corner.horizontal_radius, min(double_radius, max_x).value());
        auto min_y = max(corner.vertical_radius, min(double_radius, max_y).value());
        if (min_x <= max_x)
            size.set_width(clamp(size.width(), min_x, max_x));
        if (min_y <= max_y)
            size.set_height(clamp(size.height(), min_y, max_y));
    };

    clip_corner_size(top_left_corner_size, top_left_corner, extra_edge_width, extra_edge_height);
    clip_corner_size(top_right_corner_size, top_right_corner, 0, extra_edge_height);
    clip_corner_size(bottom_left_corner_size, bottom_left_corner, extra_edge_width);
    clip_corner_size(bottom_right_corner_size, bottom_right_corner);

    auto shadow_bitmap_rect = DevicePixelRect(
        0, 0,
        max(max(
                top_left_corner_size.width() + top_right_corner_size.width(),
                bottom_left_corner_size.width() + bottom_right_corner_size.width()),
            max(top_left_corner_size.width() + bottom_right_corner_size.width(),
                bottom_left_corner_size.width() + top_right_corner_size.width()))
            + 1 + blurred_edge_thickness,
        max(max(
                top_left_corner_size.height() + bottom_left_corner_size.height(),
                top_right_corner_size.height() + bottom_right_corner_size.height()),
            max(top_left_corner_size.height() + bottom_right_corner_size.height(),
                bottom_left_corner_size.height() + top_right_corner_size.height()))
            + 1 + blurred_edge_thickness);

    auto top_left_corner_rect = DevicePixelRect {
        0, 0,
        top_left_corner_size.width() + double_radius,
        top_left_corner_size.height() + double_radius
    };
    auto top_right_corner_rect = DevicePixelRect {
        shadow_bitmap_rect.width() - (top_right_corner_size.width() + double_radius), 0,
        top_right_corner_size.width() + double_radius,
        top_right_corner_size.height() + double_radius
    };
    auto bottom_right_corner_rect = DevicePixelRect {
        shadow_bitmap_rect.width() - (bottom_right_corner_size.width() + double_radius),
        shadow_bitmap_rect.height() - (bottom_right_corner_size.height() + double_radius),
        bottom_right_corner_size.width() + double_radius,
        bottom_right_corner_size.height() + double_radius
    };
    auto bottom_left_corner_rect = DevicePixelRect {
        0, shadow_bitmap_rect.height() - (bottom_left_corner_size.height() + double_radius),
        bottom_left_corner_size.width() + double_radius,
        bottom_left_corner_size.height() + double_radius
    };

    auto horizontal_edge_width = min(max_edge_height, double_radius) + double_radius;
    auto vertical_edge_width = min(max_edge_width, double_radius) + double_radius;
    auto horizontal_top_edge_width = min(max_edge_height + extra_edge_height, double_radius) + double_radius;
    auto vertical_left_edge_width = min(max_edge_width + extra_edge_width, double_radius) + double_radius;

    DevicePixelRect left_edge_rect { 0, top_left_corner_rect.height(), vertical_left_edge_width, 1 };
    DevicePixelRect right_edge_rect { shadow_bitmap_rect.width() - vertical_edge_width, top_right_corner_rect.height(), vertical_edge_width, 1 };
    DevicePixelRect top_edge_rect { top_left_corner_rect.width(), 0, 1, horizontal_top_edge_width };
    DevicePixelRect bottom_edge_rect { bottom_left_corner_rect.width(), shadow_bitmap_rect.height() - horizontal_edge_width, 1, horizontal_edge_width };

    auto shadows_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, shadow_bitmap_rect.size().to_type<int>());
    if (shadows_bitmap.is_error()) {
        dbgln("Unable to allocate temporary bitmap {} for box-shadow rendering: {}", shadow_bitmap_rect, shadows_bitmap.error());
        return;
    }
    auto shadow_bitmap = shadows_bitmap.release_value();
    Gfx::Painter corner_painter { *shadow_bitmap };
    Gfx::AntiAliasingPainter aa_corner_painter { corner_painter };

    aa_corner_painter.fill_rect_with_rounded_corners(
        shadow_bitmap_rect.shrunken(double_radius, double_radius, double_radius, double_radius).to_type<int>(),
        box_shadow_data.color, top_left_shadow_corner, top_right_shadow_corner, bottom_right_shadow_corner, bottom_left_shadow_corner);
    Gfx::StackBlurFilter filter(*shadow_bitmap);
    filter.process_rgba(blur_radius.value(), box_shadow_data.color);

    auto paint_shadow_infill = [&] {
        if (!border_radii.has_any_radius())
            return painter.fill_rect(inner_bounding_rect.to_type<int>(), box_shadow_data.color);

        auto top_left_inner_width = top_left_corner_rect.width() - blurred_edge_thickness;
        auto top_left_inner_height = top_left_corner_rect.height() - blurred_edge_thickness;
        auto top_right_inner_width = top_right_corner_rect.width() - blurred_edge_thickness;
        auto top_right_inner_height = top_right_corner_rect.height() - blurred_edge_thickness;
        auto bottom_right_inner_width = bottom_right_corner_rect.width() - blurred_edge_thickness;
        auto bottom_right_inner_height = bottom_right_corner_rect.height() - blurred_edge_thickness;
        auto bottom_left_inner_width = bottom_left_corner_rect.width() - blurred_edge_thickness;
        auto bottom_left_inner_height = bottom_left_corner_rect.height() - blurred_edge_thickness;

        DevicePixelRect top_rect {
            inner_bounding_rect.x() + top_left_inner_width,
            inner_bounding_rect.y(),
            inner_bounding_rect.width() - top_left_inner_width - top_right_inner_width,
            top_left_inner_height
        };
        DevicePixelRect right_rect {
            inner_bounding_rect.x() + inner_bounding_rect.width() - top_right_inner_width,
            inner_bounding_rect.y() + top_right_inner_height,
            top_right_inner_width,
            inner_bounding_rect.height() - top_right_inner_height - bottom_right_inner_height
        };
        DevicePixelRect bottom_rect {
            inner_bounding_rect.x() + bottom_left_inner_width,
            inner_bounding_rect.y() + inner_bounding_rect.height() - bottom_right_inner_height,
            inner_bounding_rect.width() - bottom_left_inner_width - bottom_right_inner_width,
            bottom_right_inner_height
        };
        DevicePixelRect left_rect {
            inner_bounding_rect.x(),
            inner_bounding_rect.y() + top_left_inner_height,
            bottom_left_inner_width,
            inner_bounding_rect.height() - top_left_inner_height - bottom_left_inner_height
        };
        DevicePixelRect inner = {
            left_rect.x() + left_rect.width(),
            left_rect.y(),
            inner_bounding_rect.width() - left_rect.width() - right_rect.width(),
            inner_bounding_rect.height() - top_rect.height() - bottom_rect.height()
        };

        painter.fill_rect(top_rect.to_type<int>(), box_shadow_data.color);
        painter.fill_rect(right_rect.to_type<int>(), box_shadow_data.color);
        painter.fill_rect(bottom_rect.to_type<int>(), box_shadow_data.color);
        painter.fill_rect(left_rect.to_type<int>(), box_shadow_data.color);
        painter.fill_rect(inner.to_type<int>(), box_shadow_data.color);
    };

    auto left_start = inner_bounding_rect.left() - blurred_edge_thickness;
    auto right_start = inner_bounding_rect.left() + inner_bounding_rect.width() + (blurred_edge_thickness - vertical_edge_width);
    auto top_start = inner_bounding_rect.top() - blurred_edge_thickness;
    auto bottom_start = inner_bounding_rect.top() + inner_bounding_rect.height() + (blurred_edge_thickness - horizontal_edge_width);

    auto top_left_corner_blit_pos = inner_bounding_rect.top_left().translated(-blurred_edge_thickness, -blurred_edge_thickness);
    auto top_right_corner_blit_pos = inner_bounding_rect.top_right().translated(-top_right_corner_size.width() + double_radius, -blurred_edge_thickness);
    auto bottom_left_corner_blit_pos = inner_bounding_rect.bottom_left().translated(-blurred_edge_thickness, -bottom_left_corner_size.height() + double_radius);
    auto bottom_right_corner_blit_pos = inner_bounding_rect.bottom_right().translated(-bottom_right_corner_size.width() + double_radius, -bottom_right_corner_size.height() + double_radius);

    auto paint_shadow = [&](DevicePixelRect clip_rect) {
        Gfx::PainterStateSaver save { painter };
        painter.add_clip_rect(clip_rect.to_type<int>());

        paint_shadow_infill();

        // Corners
        painter.blit(top_left_corner_blit_pos.to_type<int>(), shadow_bitmap, top_left_corner_rect.to_type<int>());
        painter.blit(top_right_corner_blit_pos.to_type<int>(), shadow_bitmap, top_right_corner_rect.to_type<int>());
        painter.blit(bottom_left_corner_blit_pos.to_type<int>(), shadow_bitmap, bottom_left_corner_rect.to_type<int>());
        painter.blit(bottom_right_corner_blit_pos.to_type<int>(), shadow_bitmap, bottom_right_corner_rect.to_type<int>());

        // Horizontal edges
        for (auto x = inner_bounding_rect.left() + (bottom_left_corner_size.width() - double_radius); x < inner_bounding_rect.right() - (bottom_right_corner_size.width() - double_radius); ++x)
            painter.blit({ x, bottom_start }, shadow_bitmap, bottom_edge_rect.to_type<int>());
        for (auto x = inner_bounding_rect.left() + (top_left_corner_size.width() - double_radius); x < inner_bounding_rect.right() - (top_right_corner_size.width() - double_radius); ++x)
            painter.blit({ x, top_start }, shadow_bitmap, top_edge_rect.to_type<int>());

        // Vertical edges
        for (auto y = inner_bounding_rect.top() + (top_right_corner_size.height() - double_radius); y < inner_bounding_rect.bottom() - (bottom_right_corner_size.height() - double_radius); ++y)
            painter.blit({ right_start, y }, shadow_bitmap, right_edge_rect.to_type<int>());
        for (auto y = inner_bounding_rect.top() + (top_left_corner_size.height() - double_radius); y < inner_bounding_rect.bottom() - (bottom_left_corner_size.height() - double_radius); ++y)
            painter.blit({ left_start, y }, shadow_bitmap, left_edge_rect.to_type<int>());
    };

    // FIXME: Painter only lets us define a clip-rect which discards drawing outside of it, whereas here we want
    //        a rect which discards drawing inside it. So, we run the draw operations 4 to 8 times with clip-rects
    //        covering each side of the content_rect exactly once.

    // If we were painting a shadow without a border radius we'd want to clip everything inside the box below.
    // If painting a shadow with rounded corners (but still rectangular) we want to clip everything inside
    // the box except the corners. This gives us an upper bound of 8 shadow paints now :^(.
    // (However, this does not seem to be the costly part in profiling).
    //
    // ┌───┬────────┬───┐
    // │   │xxxxxxxx│   │
    // ├───┼────────┼───┤
    // │xxx│xxxxxxxx│xxx│
    // │xxx│xxxxxxxx│xxx│
    // │xxx│xxxxxxxx│xxx│
    // │xxx│xxxxxxxx│xxx│
    // │xxx│xxxxxxxx│xxx│
    // ├───┼────────┼───┤
    // │   │ xxxxxx │   │
    // └───┴────────┴───┘

    // How many times would you like to paint the shadow sir?
    // Yes.

    // FIXME: Could reduce the shadow paints from 8 to 4 for shadows with all corner radii 50%.

    // FIXME: We use this since we want the clip rect to include everything after a certain x or y.
    // Note: Using painter.target()->width() or height() does not work, when the painter is a small
    // translated bitmap rather than full screen, as the clip rect may not intersect.
    constexpr auto really_large_number = NumericLimits<int>::max() / 2;

    // Everything above content_rect, including sides
    paint_shadow({ 0, 0, really_large_number, device_content_rect.top() });

    // Everything below content_rect, including sides
    paint_shadow({ 0, device_content_rect.bottom(), really_large_number, really_large_number });

    // Everything directly to the left of content_rect
    paint_shadow({ 0, device_content_rect.top(), device_content_rect.left(), device_content_rect.height() });

    // Everything directly to the right of content_rect
    paint_shadow({ device_content_rect.right(), device_content_rect.top(), really_large_number, device_content_rect.height() });

    if (top_left_corner) {
        // Inside the top left corner (the part outside the border radius)
        auto top_left = top_left_corner.as_rect().to_type<DevicePixels>().translated(device_content_rect.top_left());
        paint_shadow(top_left);
    }

    if (top_right_corner) {
        // Inside the top right corner (the part outside the border radius)
        auto top_right = top_right_corner.as_rect().to_type<DevicePixels>().translated(device_content_rect.top_right().translated(-top_right_corner.horizontal_radius, 0));
        paint_shadow(top_right);
    }

    if (bottom_right_corner) {
        // Inside the bottom right corner (the part outside the border radius)
        auto bottom_right = bottom_right_corner.as_rect().to_type<DevicePixels>().translated(device_content_rect.bottom_right().translated(-bottom_right_corner.horizontal_radius, -bottom_right_corner.vertical_radius));
        paint_shadow(bottom_right);
    }

    if (bottom_left_corner) {
        // Inside the bottom left corner (the part outside the border radius)
        auto bottom_left = bottom_left_corner.as_rect().to_type<DevicePixels>().translated(device_content_rect.bottom_left().translated(0, -bottom_left_corner.vertical_radius));
        paint_shadow(bottom_left);
    }
}

void paint_box_shadow(PaintContext& context,
    CSSPixelRect const& bordered_content_rect,
    CSSPixelRect const& borderless_content_rect,
    BordersData const& borders_data,
    BorderRadiiData const& border_radii,
    Vector<ShadowData> const& box_shadow_layers)
{
    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (auto& box_shadow_data : box_shadow_layers.in_reverse()) {
        if (box_shadow_data.placement == ShadowPlacement::Inner) {
            paint_inner_box_shadow(context, borderless_content_rect, borders_data, border_radii, box_shadow_data);
        } else {
            paint_outer_box_shadow(context, bordered_content_rect, border_radii, box_shadow_data);
        }
    }
}

void paint_text_shadow(PaintContext& context, Layout::LineBoxFragment const& fragment, Vector<ShadowData> const& shadow_layers)
{
    if (shadow_layers.is_empty() || fragment.text().is_empty())
        return;

    auto& painter = context.painter();

    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (auto& layer : shadow_layers.in_reverse()) {
        DevicePixels offset_x = context.rounded_device_pixels(layer.offset_x);
        DevicePixels offset_y = context.rounded_device_pixels(layer.offset_y);
        DevicePixels blur_radius = context.rounded_device_pixels(layer.blur_radius);
        DevicePixels fragment_width = context.enclosing_device_pixels(fragment.width());
        DevicePixels fragment_height = context.enclosing_device_pixels(fragment.height());

        // Space around the painted text to allow it to blur.
        // FIXME: Include spread in this once we use that.
        DevicePixels margin = blur_radius * 2;
        DevicePixelRect text_rect {
            margin, margin,
            fragment_width, fragment_height
        };
        DevicePixelRect bounding_rect {
            0, 0,
            text_rect.width() + margin + margin,
            text_rect.height() + margin + margin
        };
        // FIXME: Figure out the maximum bitmap size for all shadows and then allocate it once and reuse it?
        auto maybe_shadow_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, bounding_rect.size().to_type<int>());
        if (maybe_shadow_bitmap.is_error()) {
            dbgln("Unable to allocate temporary bitmap {} for text-shadow rendering: {}", bounding_rect.size(), maybe_shadow_bitmap.error());
            return;
        }
        auto shadow_bitmap = maybe_shadow_bitmap.release_value();

        Gfx::Painter shadow_painter { *shadow_bitmap };
        // FIXME: "Spread" the shadow somehow.
        DevicePixelPoint baseline_start(text_rect.x(), text_rect.y() + context.rounded_device_pixels(fragment.baseline()));
        shadow_painter.draw_text_run(baseline_start.to_type<int>(), Utf8View(fragment.text()), fragment.layout_node().scaled_font(context), layer.color);

        // Blur
        Gfx::StackBlurFilter filter(*shadow_bitmap);
        filter.process_rgba(blur_radius.value(), layer.color);

        auto draw_rect = context.enclosing_device_rect(fragment.absolute_rect());
        DevicePixelPoint draw_location {
            draw_rect.x() + offset_x - margin,
            draw_rect.y() + offset_y - margin
        };
        painter.blit(draw_location.to_type<int>(), *shadow_bitmap, bounding_rect.to_type<int>());
    }
}

}
