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
#include <LibGfx/Font/Font.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/LineBoxFragment.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/PaintOuterBoxShadowParams.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void paint_inner_box_shadow(Gfx::Painter& painter, PaintOuterBoxShadowParams params)
{
    auto device_content_rect = params.device_content_rect;

    DevicePixels offset_x = params.offset_x;
    DevicePixels offset_y = params.offset_y;
    DevicePixels blur_radius = params.blur_radius;
    DevicePixels spread_distance = params.spread_distance;
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
    auto top_left_corner = params.corner_radii.top_left;
    auto top_right_corner = params.corner_radii.top_right;
    auto bottom_right_corner = params.corner_radii.bottom_right;
    auto bottom_left_corner = params.corner_radii.bottom_left;
    shadow_painter.fill_rect(outer_shadow_rect, params.box_shadow_data.color.with_alpha(0xff));
    if (params.border_radii.has_any_radius()) {
        shadow_aa_painter.fill_rect_with_rounded_corners(inner_shadow_rect, params.box_shadow_data.color.with_alpha(0xff),
            top_left_corner, top_right_corner, bottom_right_corner, bottom_left_corner,
            Gfx::AntiAliasingPainter::BlendMode::AlphaSubtract);
    } else {
        shadow_painter.clear_rect(inner_shadow_rect, Color::Transparent);
    }
    Gfx::StackBlurFilter filter(*shadow_bitmap);
    filter.process_rgba(blur_radius.value(), params.box_shadow_data.color);
    Gfx::PainterStateSaver save { painter };
    painter.add_clip_rect(device_content_rect_int);
    painter.blit({ device_content_rect_int.left() - blur_radius.value(), device_content_rect_int.top() - blur_radius.value() },
        *shadow_bitmap, shadow_bitmap->rect(), params.box_shadow_data.color.alpha() / 255.);
}

struct OuterBoxShadowMetrics {
    DevicePixelRect shadow_bitmap_rect;
    DevicePixelRect non_blurred_shadow_rect;
    DevicePixelRect inner_bounding_rect;
    DevicePixels blurred_edge_thickness;
    DevicePixels double_radius;
    DevicePixels blur_radius;

    DevicePixelRect top_left_corner_rect;
    DevicePixelRect top_right_corner_rect;
    DevicePixelRect bottom_right_corner_rect;
    DevicePixelRect bottom_left_corner_rect;

    DevicePixelPoint top_left_corner_blit_pos;
    DevicePixelPoint top_right_corner_blit_pos;
    DevicePixelPoint bottom_right_corner_blit_pos;
    DevicePixelPoint bottom_left_corner_blit_pos;

    DevicePixelSize top_left_corner_size;
    DevicePixelSize top_right_corner_size;
    DevicePixelSize bottom_right_corner_size;
    DevicePixelSize bottom_left_corner_size;

    DevicePixels left_start;
    DevicePixels top_start;
    DevicePixels right_start;
    DevicePixels bottom_start;

    DevicePixelRect left_edge_rect;
    DevicePixelRect right_edge_rect;
    DevicePixelRect top_edge_rect;
    DevicePixelRect bottom_edge_rect;

    CornerRadius top_left_shadow_corner;
    CornerRadius top_right_shadow_corner;
    CornerRadius bottom_right_shadow_corner;
    CornerRadius bottom_left_shadow_corner;
};

static OuterBoxShadowMetrics get_outer_box_shadow_configuration(PaintOuterBoxShadowParams params)
{
    auto device_content_rect = params.device_content_rect;

    auto top_left_corner = params.corner_radii.top_left;
    auto top_right_corner = params.corner_radii.top_right;
    auto bottom_right_corner = params.corner_radii.bottom_right;
    auto bottom_left_corner = params.corner_radii.bottom_left;

    DevicePixels offset_x = params.offset_x;
    DevicePixels offset_y = params.offset_y;
    DevicePixels blur_radius = params.blur_radius;
    DevicePixels spread_distance = params.spread_distance;

    // Our blur cannot handle radii over 255 so there's no point trying (255 is silly big anyway)
    blur_radius = clamp(blur_radius, 0, 255);

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

    auto non_blurred_shadow_rect = device_content_rect.inflated(spread_distance, spread_distance, spread_distance, spread_distance);

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

    auto left_start = inner_bounding_rect.left() - blurred_edge_thickness;
    auto right_start = inner_bounding_rect.left() + inner_bounding_rect.width() + (blurred_edge_thickness - vertical_edge_width);
    auto top_start = inner_bounding_rect.top() - blurred_edge_thickness;
    auto bottom_start = inner_bounding_rect.top() + inner_bounding_rect.height() + (blurred_edge_thickness - horizontal_edge_width);

    auto top_left_corner_blit_pos = inner_bounding_rect.top_left().translated(-blurred_edge_thickness, -blurred_edge_thickness);
    auto top_right_corner_blit_pos = inner_bounding_rect.top_right().translated(-top_right_corner_size.width() + double_radius, -blurred_edge_thickness);
    auto bottom_left_corner_blit_pos = inner_bounding_rect.bottom_left().translated(-blurred_edge_thickness, -bottom_left_corner_size.height() + double_radius);
    auto bottom_right_corner_blit_pos = inner_bounding_rect.bottom_right().translated(-bottom_right_corner_size.width() + double_radius, -bottom_right_corner_size.height() + double_radius);

    return OuterBoxShadowMetrics {
        .shadow_bitmap_rect = shadow_bitmap_rect,
        .non_blurred_shadow_rect = non_blurred_shadow_rect,
        .inner_bounding_rect = inner_bounding_rect,
        .blurred_edge_thickness = blurred_edge_thickness,
        .double_radius = double_radius,
        .blur_radius = blur_radius,

        .top_left_corner_rect = top_left_corner_rect,
        .top_right_corner_rect = top_right_corner_rect,
        .bottom_right_corner_rect = bottom_right_corner_rect,
        .bottom_left_corner_rect = bottom_left_corner_rect,

        .top_left_corner_blit_pos = top_left_corner_blit_pos,
        .top_right_corner_blit_pos = top_right_corner_blit_pos,
        .bottom_right_corner_blit_pos = bottom_right_corner_blit_pos,
        .bottom_left_corner_blit_pos = bottom_left_corner_blit_pos,

        .top_left_corner_size = top_left_corner_size,
        .top_right_corner_size = top_right_corner_size,
        .bottom_right_corner_size = bottom_right_corner_size,
        .bottom_left_corner_size = bottom_left_corner_size,

        .left_start = left_start,
        .top_start = top_start,
        .right_start = right_start,
        .bottom_start = bottom_start,

        .left_edge_rect = left_edge_rect,
        .right_edge_rect = right_edge_rect,
        .top_edge_rect = top_edge_rect,
        .bottom_edge_rect = bottom_edge_rect,

        .top_left_shadow_corner = top_left_shadow_corner,
        .top_right_shadow_corner = top_right_shadow_corner,
        .bottom_right_shadow_corner = bottom_right_shadow_corner,
        .bottom_left_shadow_corner = bottom_left_shadow_corner,
    };
}

Gfx::IntRect get_outer_box_shadow_bounding_rect(PaintOuterBoxShadowParams params)
{
    auto shadow_config = get_outer_box_shadow_configuration(params);

    auto const& top_left_corner_blit_pos = shadow_config.top_left_corner_blit_pos;
    auto const& top_right_corner_blit_pos = shadow_config.top_right_corner_blit_pos;
    auto const& bottom_left_corner_blit_pos = shadow_config.bottom_left_corner_blit_pos;
    auto const& top_right_corner_rect = shadow_config.top_right_corner_rect;
    auto const& bottom_left_corner_rect = shadow_config.bottom_left_corner_rect;

    return Gfx::IntRect {
        top_left_corner_blit_pos,
        { top_right_corner_blit_pos.x() - top_left_corner_blit_pos.x() + top_right_corner_rect.width(),
            bottom_left_corner_blit_pos.y() - top_left_corner_blit_pos.y() + bottom_left_corner_rect.height() }
    };
}

void paint_outer_box_shadow(Gfx::Painter& painter, PaintOuterBoxShadowParams params)
{
    auto const& box_shadow_data = params.box_shadow_data;
    auto const& device_content_rect = params.device_content_rect;
    auto const& border_radii = params.border_radii;

    auto const& top_left_corner = params.corner_radii.top_left;
    auto const& top_right_corner = params.corner_radii.top_right;
    auto const& bottom_right_corner = params.corner_radii.bottom_right;
    auto const& bottom_left_corner = params.corner_radii.bottom_left;

    DevicePixels offset_x = params.offset_x;
    DevicePixels offset_y = params.offset_y;

    auto shadow_config = get_outer_box_shadow_configuration(params);

    auto const& shadow_bitmap_rect = shadow_config.shadow_bitmap_rect;
    auto const& non_blurred_shadow_rect = shadow_config.non_blurred_shadow_rect;
    auto const& inner_bounding_rect = shadow_config.inner_bounding_rect;
    auto const& blurred_edge_thickness = shadow_config.blurred_edge_thickness;
    auto const& double_radius = shadow_config.double_radius;
    auto const& blur_radius = shadow_config.blur_radius;

    auto const& top_left_corner_rect = shadow_config.top_left_corner_rect;
    auto const& top_right_corner_rect = shadow_config.top_right_corner_rect;
    auto const& bottom_right_corner_rect = shadow_config.bottom_right_corner_rect;
    auto const& bottom_left_corner_rect = shadow_config.bottom_left_corner_rect;

    auto const& top_left_corner_blit_pos = shadow_config.top_left_corner_blit_pos;
    auto const& top_right_corner_blit_pos = shadow_config.top_right_corner_blit_pos;
    auto const& bottom_right_corner_blit_pos = shadow_config.bottom_right_corner_blit_pos;
    auto const& bottom_left_corner_blit_pos = shadow_config.bottom_left_corner_blit_pos;

    auto const& top_left_corner_size = shadow_config.top_left_corner_size;
    auto const& top_right_corner_size = shadow_config.top_right_corner_size;
    auto const& bottom_right_corner_size = shadow_config.bottom_right_corner_size;
    auto const& bottom_left_corner_size = shadow_config.bottom_left_corner_size;

    auto const& left_start = shadow_config.left_start;
    auto const& top_start = shadow_config.top_start;
    auto const& right_start = shadow_config.right_start;
    auto const& bottom_start = shadow_config.bottom_start;

    auto const& left_edge_rect = shadow_config.left_edge_rect;
    auto const& right_edge_rect = shadow_config.right_edge_rect;
    auto const& top_edge_rect = shadow_config.top_edge_rect;
    auto const& bottom_edge_rect = shadow_config.bottom_edge_rect;

    auto const& top_left_shadow_corner = shadow_config.top_left_shadow_corner;
    auto const& top_right_shadow_corner = shadow_config.top_right_shadow_corner;
    auto const& bottom_right_shadow_corner = shadow_config.bottom_right_shadow_corner;
    auto const& bottom_left_shadow_corner = shadow_config.bottom_left_shadow_corner;

    auto fill_rect_masked = [](auto& painter, auto fill_rect, auto mask_rect, auto color) {
        Gfx::DisjointRectSet<DevicePixels> rect_set;
        rect_set.add(fill_rect);
        auto shattered = rect_set.shatter(mask_rect);
        for (auto& rect : shattered.rects())
            painter.fill_rect(rect.template to_type<int>(), color);
    };

    // If there's no blurring, nor rounded corners, we can save a lot of effort.
    if (blur_radius == 0 && !border_radii.has_any_radius()) {
        fill_rect_masked(painter, non_blurred_shadow_rect.translated(offset_x, offset_y), device_content_rect, box_shadow_data.color);
        return;
    }

    auto paint_shadow_infill = [&] {
        if (!params.border_radii.has_any_radius())
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
        DevicePixels offset_x = context.rounded_device_pixels(box_shadow_data.offset_x);
        DevicePixels offset_y = context.rounded_device_pixels(box_shadow_data.offset_y);
        DevicePixels blur_radius = context.rounded_device_pixels(box_shadow_data.blur_radius);
        DevicePixels spread_distance = context.rounded_device_pixels(box_shadow_data.spread_distance);

        DevicePixelRect device_content_rect;
        if (box_shadow_data.placement == ShadowPlacement::Inner) {
            device_content_rect = context.rounded_device_rect(borderless_content_rect);
        } else {
            device_content_rect = context.rounded_device_rect(bordered_content_rect);
        }

        auto params = PaintOuterBoxShadowParams {
            .painter = context.painter(),
            .content_rect = bordered_content_rect,
            .border_radii = border_radii,
            .box_shadow_data = box_shadow_data,
            .corner_radii = CornerRadii {
                .top_left = border_radii.top_left.as_corner(context),
                .top_right = border_radii.top_right.as_corner(context),
                .bottom_right = border_radii.bottom_right.as_corner(context),
                .bottom_left = border_radii.bottom_left.as_corner(context) },
            .offset_x = offset_x,
            .offset_y = offset_y,
            .blur_radius = blur_radius,
            .spread_distance = spread_distance,
            .device_content_rect = context.painter().state().translation.map(device_content_rect.to_type<int>()).to_type<DevicePixels>(),
        };

        if (box_shadow_data.placement == ShadowPlacement::Inner) {
            params.border_radii.shrink(borders_data.top.width, borders_data.right.width, borders_data.bottom.width, borders_data.left.width);
            ScopedCornerRadiusClip corner_clipper { context, device_content_rect, params.border_radii, CornerClip::Outside };
            context.painter().paint_inner_box_shadow_params(params);
        } else {
            ScopedCornerRadiusClip corner_clipper { context, device_content_rect, border_radii, CornerClip::Inside };
            context.painter().paint_outer_box_shadow_params(params);
        }
    }
}

void paint_text_shadow(PaintContext& context, Layout::LineBoxFragment const& fragment, Vector<ShadowData> const& shadow_layers)
{
    if (shadow_layers.is_empty() || fragment.text().is_empty())
        return;

    auto fragment_width = context.enclosing_device_pixels(fragment.width()).value();
    auto fragment_height = context.enclosing_device_pixels(fragment.height()).value();
    auto draw_rect = context.enclosing_device_rect(fragment.absolute_rect()).to_type<int>();
    auto text = Utf8View(fragment.text());
    auto& font = fragment.layout_node().scaled_font(context);
    auto fragment_baseline = context.rounded_device_pixels(fragment.baseline()).value();

    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (auto& layer : shadow_layers.in_reverse()) {
        int offset_x = context.rounded_device_pixels(layer.offset_x).value();
        int offset_y = context.rounded_device_pixels(layer.offset_y).value();
        int blur_radius = context.rounded_device_pixels(layer.blur_radius).value();

        // Space around the painted text to allow it to blur.
        // FIXME: Include spread in this once we use that.
        int margin = blur_radius * 2;
        Gfx::IntRect text_rect {
            margin, margin,
            fragment_width, fragment_height
        };
        Gfx::IntRect bounding_rect {
            0, 0,
            text_rect.width() + margin + margin,
            text_rect.height() + margin + margin
        };
        Gfx::IntPoint draw_location {
            draw_rect.x() + offset_x - margin,
            draw_rect.y() + offset_y - margin
        };

        context.painter().paint_text_shadow(blur_radius, bounding_rect, text_rect, text, font, layer.color, fragment_baseline, draw_location);
    }
}

}
