/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Filters/StackBlurFilter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/LineBoxFragment.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void paint_box_shadow(PaintContext& context, Gfx::IntRect const& content_rect, BorderRadiiData const& border_radii, Vector<ShadowData> const& box_shadow_layers)
{
    if (box_shadow_layers.is_empty())
        return;

    auto& painter = context.painter();

    auto top_left_corner = border_radii.top_left.as_corner();
    auto top_right_corner = border_radii.top_right.as_corner();
    auto bottom_right_corner = border_radii.bottom_right.as_corner();
    auto bottom_left_corner = border_radii.bottom_left.as_corner();

    Optional<BorderRadiusCornerClipper> corner_radius_clipper {};

    if (border_radii.has_any_radius()) {
        auto clipper = BorderRadiusCornerClipper::create(content_rect, border_radii, CornerClip::Inside);
        if (!clipper.is_error())
            corner_radius_clipper = clipper.release_value();
    }

    if (corner_radius_clipper.has_value())
        corner_radius_clipper->sample_under_corners(painter);

    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (auto& box_shadow_data : box_shadow_layers.in_reverse()) {
        // FIXME: Paint inset shadows.
        if (box_shadow_data.placement != ShadowPlacement::Outer)
            continue;

        auto fill_rect_masked = [](auto& painter, auto fill_rect, auto mask_rect, auto color) {
            Gfx::DisjointRectSet rect_set;
            rect_set.add(fill_rect);
            auto shattered = rect_set.shatter(mask_rect);
            for (auto& rect : shattered.rects())
                painter.fill_rect(rect, color);
        };

        // If there's no blurring, nor rounded corners, we can save a lot of effort.
        if (box_shadow_data.blur_radius == 0 && !border_radii.has_any_radius()) {
            fill_rect_masked(painter, content_rect.inflated(box_shadow_data.spread_distance, box_shadow_data.spread_distance, box_shadow_data.spread_distance, box_shadow_data.spread_distance).translated(box_shadow_data.offset_x, box_shadow_data.offset_y), content_rect, box_shadow_data.color);
            continue;
        }

        auto top_left_shadow_corner = top_left_corner;
        auto top_right_shadow_corner = top_right_corner;
        auto bottom_right_shadow_corner = bottom_right_corner;
        auto bottom_left_shadow_corner = bottom_left_corner;

        auto spread_corner = [&](auto& corner) {
            if (corner) {
                corner.horizontal_radius += box_shadow_data.spread_distance;
                corner.vertical_radius += box_shadow_data.spread_distance;
            }
        };

        spread_corner(top_left_shadow_corner);
        spread_corner(top_right_shadow_corner);
        spread_corner(bottom_right_shadow_corner);
        spread_corner(bottom_left_shadow_corner);

        auto expansion = box_shadow_data.spread_distance - (box_shadow_data.blur_radius * 2);
        Gfx::IntRect inner_bounding_rect = {
            content_rect.x() + box_shadow_data.offset_x - expansion,
            content_rect.y() + box_shadow_data.offset_y - expansion,
            content_rect.width() + 2 * expansion,
            content_rect.height() + 2 * expansion
        };

        // Calculating and blurring the box-shadow full size is expensive, and wasteful - aside from the corners,
        // all vertical strips of the shadow are identical, and the same goes for horizontal ones.
        // So instead, we generate a shadow bitmap that is just large enough to include the corners and 1px of
        // non-corner, and then we repeatedly blit sections of it. This is similar to a NinePatch on Android.
        auto double_radius = box_shadow_data.blur_radius * 2;
        auto blurred_edge_thickness = box_shadow_data.blur_radius * 4;

        auto default_corner_size = Gfx::IntSize { double_radius, double_radius };
        auto top_left_corner_size = top_left_shadow_corner ? top_left_shadow_corner.as_rect().size() : default_corner_size;
        auto top_right_corner_size = top_right_shadow_corner ? top_right_shadow_corner.as_rect().size() : default_corner_size;
        auto bottom_left_corner_size = bottom_left_shadow_corner ? bottom_left_shadow_corner.as_rect().size() : default_corner_size;
        auto bottom_right_corner_size = bottom_right_shadow_corner ? bottom_right_shadow_corner.as_rect().size() : default_corner_size;

        auto shadow_bitmap_rect = Gfx::IntRect(
            0, 0,
            max(
                top_left_corner_size.width() + top_right_corner_size.width(),
                bottom_left_corner_size.width() + bottom_right_corner_size.width())
                + 1 + blurred_edge_thickness,
            max(
                top_left_corner_size.height() + bottom_left_corner_size.height(),
                top_right_corner_size.height() + bottom_right_corner_size.height())
                + 1 + blurred_edge_thickness);

        auto top_left_corner_rect = Gfx::IntRect {
            0, 0,
            top_left_corner_size.width() + double_radius,
            top_left_corner_size.height() + double_radius
        };
        auto top_right_corner_rect = Gfx::IntRect {
            shadow_bitmap_rect.width() - (top_right_corner_size.width() + double_radius), 0,
            top_right_corner_size.width() + double_radius,
            top_right_corner_size.height() + double_radius
        };
        auto bottom_right_corner_rect = Gfx::IntRect {
            shadow_bitmap_rect.width() - (bottom_right_corner_size.width() + double_radius),
            shadow_bitmap_rect.height() - (bottom_right_corner_size.height() + double_radius),
            bottom_right_corner_size.width() + double_radius,
            bottom_right_corner_size.height() + double_radius
        };
        auto bottom_left_corner_rect = Gfx::IntRect {
            0, shadow_bitmap_rect.height() - (bottom_left_corner_size.height() + double_radius),
            bottom_left_corner_size.width() + double_radius,
            bottom_left_corner_size.height() + double_radius
        };

        Gfx::IntRect left_edge_rect { 0, top_left_corner_rect.height(), blurred_edge_thickness, 1 };
        Gfx::IntRect right_edge_rect { shadow_bitmap_rect.width() - blurred_edge_thickness, top_right_corner_rect.height(), blurred_edge_thickness, 1 };
        Gfx::IntRect top_edge_rect { top_left_corner_rect.width(), 0, 1, blurred_edge_thickness };
        Gfx::IntRect bottom_edge_rect { bottom_left_corner_rect.width(), shadow_bitmap_rect.height() - blurred_edge_thickness, 1, blurred_edge_thickness };

        auto shadows_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, shadow_bitmap_rect.size());
        if (shadows_bitmap.is_error()) {
            dbgln("Unable to allocate temporary bitmap for box-shadow rendering: {}", shadows_bitmap.error());
            return;
        }
        auto shadow_bitmap = shadows_bitmap.release_value();
        Gfx::Painter corner_painter { *shadow_bitmap };
        Gfx::AntiAliasingPainter aa_corner_painter { corner_painter };

        aa_corner_painter.fill_rect_with_rounded_corners(shadow_bitmap_rect.shrunken(double_radius, double_radius, double_radius, double_radius), box_shadow_data.color, top_left_shadow_corner, top_right_shadow_corner, bottom_right_shadow_corner, bottom_left_shadow_corner);
        Gfx::StackBlurFilter filter(*shadow_bitmap);
        filter.process_rgba(box_shadow_data.blur_radius);

        auto paint_shadow_infill = [&] {
            if (!border_radii.has_any_radius())
                return painter.fill_rect(inner_bounding_rect, box_shadow_data.color);

            auto top_left_inner_width = top_left_corner_rect.width() - blurred_edge_thickness;
            auto top_left_inner_height = top_left_corner_rect.height() - blurred_edge_thickness;
            auto top_right_inner_width = top_right_corner_rect.width() - blurred_edge_thickness;
            auto top_right_inner_height = top_right_corner_rect.height() - blurred_edge_thickness;
            auto bottom_right_inner_width = bottom_right_corner_rect.width() - blurred_edge_thickness;
            auto bottom_right_inner_height = bottom_right_corner_rect.height() - blurred_edge_thickness;
            auto bottom_left_inner_width = bottom_left_corner_rect.width() - blurred_edge_thickness;
            auto bottom_left_inner_height = bottom_left_corner_rect.height() - blurred_edge_thickness;

            Gfx::IntRect top_rect {
                inner_bounding_rect.x() + top_left_inner_width,
                inner_bounding_rect.y(),
                inner_bounding_rect.width() - top_left_inner_width - top_right_inner_width,
                top_left_inner_height
            };
            Gfx::IntRect right_rect {
                inner_bounding_rect.x() + inner_bounding_rect.width() - top_right_inner_width,
                inner_bounding_rect.y() + top_right_inner_height,
                top_right_inner_width,
                inner_bounding_rect.height() - top_right_inner_height - bottom_right_inner_height
            };
            Gfx::IntRect bottom_rect {
                inner_bounding_rect.x() + bottom_left_inner_width,
                inner_bounding_rect.y() + inner_bounding_rect.height() - bottom_right_inner_height,
                inner_bounding_rect.width() - bottom_left_inner_width - bottom_right_inner_width,
                bottom_right_inner_height
            };
            Gfx::IntRect left_rect {
                inner_bounding_rect.x(),
                inner_bounding_rect.y() + top_left_inner_height,
                bottom_left_inner_width,
                inner_bounding_rect.height() - top_left_inner_height - bottom_left_inner_height
            };
            Gfx::IntRect inner = {
                left_rect.x() + left_rect.width(),
                left_rect.y(),
                inner_bounding_rect.width() - left_rect.width() - right_rect.width(),
                inner_bounding_rect.height() - top_rect.height() - bottom_rect.height()
            };

            painter.fill_rect(top_rect, box_shadow_data.color);
            painter.fill_rect(right_rect, box_shadow_data.color);
            painter.fill_rect(bottom_rect, box_shadow_data.color);
            painter.fill_rect(left_rect, box_shadow_data.color);
            painter.fill_rect(inner, box_shadow_data.color);
        };

        auto left_start = inner_bounding_rect.left() - blurred_edge_thickness;
        auto right_start = inner_bounding_rect.left() + inner_bounding_rect.width();
        auto top_start = inner_bounding_rect.top() - blurred_edge_thickness;
        auto bottom_start = inner_bounding_rect.top() + inner_bounding_rect.height();

        // Note: The +1s in a few of the following translations are due to the -1s Gfx::Rect::right() and Gfx::Rect::bottom().
        auto top_left_corner_blit_pos = inner_bounding_rect.top_left().translated(-blurred_edge_thickness, -blurred_edge_thickness);
        auto top_right_corner_blit_pos = inner_bounding_rect.top_right().translated(-top_right_corner_size.width() + 1 + double_radius, -blurred_edge_thickness);
        auto bottom_left_corner_blit_pos = inner_bounding_rect.bottom_left().translated(-blurred_edge_thickness, -bottom_left_corner_size.height() + 1 + double_radius);
        auto bottom_right_corner_blit_pos = inner_bounding_rect.bottom_right().translated(-bottom_right_corner_size.width() + 1 + double_radius, -bottom_right_corner_size.height() + 1 + double_radius);

        auto paint_shadow = [&](Gfx::IntRect clip_rect) {
            Gfx::PainterStateSaver save { painter };
            painter.add_clip_rect(clip_rect);

            paint_shadow_infill();

            // Corners
            painter.blit(top_left_corner_blit_pos, shadow_bitmap, top_left_corner_rect);
            painter.blit(top_right_corner_blit_pos, shadow_bitmap, top_right_corner_rect);
            painter.blit(bottom_left_corner_blit_pos, shadow_bitmap, bottom_left_corner_rect);
            painter.blit(bottom_right_corner_blit_pos, shadow_bitmap, bottom_right_corner_rect);

            // Horizontal edges
            for (auto x = inner_bounding_rect.left() + (bottom_left_corner_size.width() - double_radius); x <= inner_bounding_rect.right() - (bottom_right_corner_size.width() - double_radius); ++x)
                painter.blit({ x, bottom_start }, shadow_bitmap, bottom_edge_rect);
            for (auto x = inner_bounding_rect.left() + (top_left_corner_size.width() - double_radius); x <= inner_bounding_rect.right() - (top_right_corner_size.width() - double_radius); ++x)
                painter.blit({ x, top_start }, shadow_bitmap, top_edge_rect);

            // Vertical edges
            for (auto y = inner_bounding_rect.top() + (top_right_corner_size.height() - double_radius); y <= inner_bounding_rect.bottom() - (bottom_right_corner_size.height() - double_radius); ++y)
                painter.blit({ right_start, y }, shadow_bitmap, right_edge_rect);
            for (auto y = inner_bounding_rect.top() + (top_left_corner_size.height() - double_radius); y <= inner_bounding_rect.bottom() - (bottom_left_corner_size.height() - double_radius); ++y)
                painter.blit({ left_start, y }, shadow_bitmap, left_edge_rect);
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

        // Everything above content_rect, including sides
        paint_shadow({ 0, 0, painter.target()->width(), content_rect.top() });

        // Everything below content_rect, including sides
        paint_shadow({ 0, content_rect.bottom() + 1, painter.target()->width(), painter.target()->height() });

        // Everything directly to the left of content_rect
        paint_shadow({ 0, content_rect.top(), content_rect.left(), content_rect.height() });

        // Everything directly to the right of content_rect
        paint_shadow({ content_rect.right() + 1, content_rect.top(), painter.target()->width(), content_rect.height() });

        if (top_left_corner) {
            // Inside the top left corner (the part outside the border radius)
            auto top_left = top_left_corner.as_rect().translated(content_rect.top_left());
            paint_shadow(top_left);
        }

        if (top_right_corner) {
            // Inside the top right corner (the part outside the border radius)
            auto top_right = top_right_corner.as_rect().translated(content_rect.top_right().translated(-top_right_corner.horizontal_radius + 1, 0));
            paint_shadow(top_right);
        }

        if (bottom_right_corner) {
            // Inside the bottom right corner (the part outside the border radius)
            auto bottom_right = bottom_right_corner.as_rect().translated(content_rect.bottom_right().translated(-bottom_right_corner.horizontal_radius + 1, -bottom_right_corner.vertical_radius + 1));
            paint_shadow(bottom_right);
        }

        if (bottom_left_corner) {
            // Inside the bottom left corner (the part outside the border radius)
            auto bottom_left = bottom_left_corner.as_rect().translated(content_rect.bottom_left().translated(0, -bottom_left_corner.vertical_radius + 1));
            paint_shadow(bottom_left);
        }
    }

    if (corner_radius_clipper.has_value())
        corner_radius_clipper->blit_corner_clipping(painter);
}

void paint_text_shadow(PaintContext& context, Layout::LineBoxFragment const& fragment, Vector<ShadowData> const& shadow_layers)
{
    if (shadow_layers.is_empty())
        return;

    auto& painter = context.painter();

    // Note: Box-shadow layers are ordered front-to-back, so we paint them in reverse
    for (auto& layer : shadow_layers.in_reverse()) {

        // Space around the painted text to allow it to blur.
        // FIXME: Include spread in this once we use that.
        auto margin = layer.blur_radius * 2;
        Gfx::IntRect text_rect {
            margin, margin,
            static_cast<int>(ceilf(fragment.width())),
            static_cast<int>(ceilf(fragment.height()))
        };
        Gfx::IntRect bounding_rect {
            0, 0,
            text_rect.width() + margin + margin,
            text_rect.height() + margin + margin
        };
        // FIXME: Figure out the maximum bitmap size for all shadows and then allocate it once and reuse it?
        auto maybe_shadow_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, bounding_rect.size());
        if (maybe_shadow_bitmap.is_error()) {
            dbgln("Unable to allocate temporary bitmap for box-shadow rendering: {}", maybe_shadow_bitmap.error());
            return;
        }
        auto shadow_bitmap = maybe_shadow_bitmap.release_value();

        Gfx::Painter shadow_painter { *shadow_bitmap };
        shadow_painter.set_font(context.painter().font());
        // FIXME: "Spread" the shadow somehow.
        Gfx::FloatPoint baseline_start(text_rect.x(), text_rect.y() + fragment.baseline());
        shadow_painter.draw_text_run(baseline_start, Utf8View(fragment.text()), context.painter().font(), layer.color);

        // Blur
        Gfx::StackBlurFilter filter(*shadow_bitmap);
        filter.process_rgba(layer.blur_radius);

        auto draw_rect = Gfx::enclosing_int_rect(fragment.absolute_rect());
        Gfx::IntPoint draw_location {
            draw_rect.x() + layer.offset_x - margin,
            draw_rect.y() + layer.offset_y - margin
        };
        painter.blit(draw_location, *shadow_bitmap, bounding_rect);
    }
}

}
