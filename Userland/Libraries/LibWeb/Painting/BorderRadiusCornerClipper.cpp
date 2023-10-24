/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

ErrorOr<NonnullRefPtr<BorderRadiusCornerClipper>> BorderRadiusCornerClipper::create(CornerRadii const& corner_radii, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii, CornerClip corner_clip)
{
    VERIFY(border_radii.has_any_radius());

    auto top_left = corner_radii.top_left;
    auto top_right = corner_radii.top_right;
    auto bottom_right = corner_radii.bottom_right;
    auto bottom_left = corner_radii.bottom_left;

    DevicePixelSize corners_bitmap_size {
        max(
            max(
                top_left.horizontal_radius + top_right.horizontal_radius,
                top_left.horizontal_radius + bottom_right.horizontal_radius),
            max(
                bottom_left.horizontal_radius + top_right.horizontal_radius,
                bottom_left.horizontal_radius + bottom_right.horizontal_radius)),
        max(
            max(
                top_left.vertical_radius + bottom_left.vertical_radius,
                top_left.vertical_radius + bottom_right.vertical_radius),
            max(
                top_right.vertical_radius + bottom_left.vertical_radius,
                top_right.vertical_radius + bottom_right.vertical_radius))
    };

    RefPtr<Gfx::Bitmap> corner_bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, corners_bitmap_size.to_type<int>()));

    CornerData corner_data {
        .corner_radii = corner_radii,
        .page_locations = { .top_left = border_rect.top_left(), .top_right = border_rect.top_right().translated(-top_right.horizontal_radius, 0), .bottom_right = border_rect.bottom_right().translated(-bottom_right.horizontal_radius, -bottom_right.vertical_radius), .bottom_left = border_rect.bottom_left().translated(0, -bottom_left.vertical_radius) },
        .bitmap_locations = { .top_left = { 0, 0 }, .top_right = { corners_bitmap_size.width() - top_right.horizontal_radius, 0 }, .bottom_right = { corners_bitmap_size.width() - bottom_right.horizontal_radius, corners_bitmap_size.height() - bottom_right.vertical_radius }, .bottom_left = { 0, corners_bitmap_size.height() - bottom_left.vertical_radius } },
    };

    return try_make_ref_counted<BorderRadiusCornerClipper>(corner_data, corner_bitmap.release_nonnull(), corner_clip, border_rect);
}

void BorderRadiusCornerClipper::sample_under_corners(Gfx::Painter& page_painter)
{
    // Generate a mask for the corners:
    Gfx::Painter corner_painter { *m_corner_bitmap };
    Gfx::AntiAliasingPainter corner_aa_painter { corner_painter };
    corner_aa_painter.fill_rect_with_rounded_corners(m_corner_bitmap->rect(), Color::NamedColor::Black,
        m_data.corner_radii.top_left, m_data.corner_radii.top_right, m_data.corner_radii.bottom_right, m_data.corner_radii.bottom_left);

    auto copy_page_masked = [&](Gfx::IntRect const& mask_src, Gfx::IntPoint const& page_location) {
        for (int row = 0; row < mask_src.height(); ++row) {
            for (int col = 0; col < mask_src.width(); ++col) {
                auto corner_location = mask_src.location().translated(col, row);
                auto mask_pixel = m_corner_bitmap->get_pixel(corner_location);
                u8 mask_alpha = mask_pixel.alpha();
                if (m_corner_clip == CornerClip::Outside)
                    mask_alpha = ~mask_pixel.alpha();
                auto final_pixel = Color();
                if (mask_alpha > 0) {
                    auto page_pixel = page_painter.get_pixel(page_location.translated(col, row));
                    if (page_pixel.has_value())
                        final_pixel = page_pixel.value().with_alpha(mask_alpha);
                }
                m_corner_bitmap->set_pixel(corner_location, final_pixel);
            }
        }
    };

    // Copy the pixels under the corner mask (using the alpha of the mask):
    if (m_data.corner_radii.top_left)
        copy_page_masked(m_data.corner_radii.top_left.as_rect().translated(m_data.bitmap_locations.top_left.to_type<int>()), m_data.page_locations.top_left.to_type<int>());
    if (m_data.corner_radii.top_right)
        copy_page_masked(m_data.corner_radii.top_right.as_rect().translated(m_data.bitmap_locations.top_right.to_type<int>()), m_data.page_locations.top_right.to_type<int>());
    if (m_data.corner_radii.bottom_right)
        copy_page_masked(m_data.corner_radii.bottom_right.as_rect().translated(m_data.bitmap_locations.bottom_right.to_type<int>()), m_data.page_locations.bottom_right.to_type<int>());
    if (m_data.corner_radii.bottom_left)
        copy_page_masked(m_data.corner_radii.bottom_left.as_rect().translated(m_data.bitmap_locations.bottom_left.to_type<int>()), m_data.page_locations.bottom_left.to_type<int>());

    m_has_sampled = true;
}

void BorderRadiusCornerClipper::blit_corner_clipping(Gfx::Painter& painter)
{
    VERIFY(m_has_sampled);

    // Restore the corners:
    if (m_data.corner_radii.top_left)
        painter.blit(m_data.page_locations.top_left.to_type<int>(), *m_corner_bitmap, m_data.corner_radii.top_left.as_rect().translated(m_data.bitmap_locations.top_left.to_type<int>()));
    if (m_data.corner_radii.top_right)
        painter.blit(m_data.page_locations.top_right.to_type<int>(), *m_corner_bitmap, m_data.corner_radii.top_right.as_rect().translated(m_data.bitmap_locations.top_right.to_type<int>()));
    if (m_data.corner_radii.bottom_right)
        painter.blit(m_data.page_locations.bottom_right.to_type<int>(), *m_corner_bitmap, m_data.corner_radii.bottom_right.as_rect().translated(m_data.bitmap_locations.bottom_right.to_type<int>()));
    if (m_data.corner_radii.bottom_left)
        painter.blit(m_data.page_locations.bottom_left.to_type<int>(), *m_corner_bitmap, m_data.corner_radii.bottom_left.as_rect().translated(m_data.bitmap_locations.bottom_left.to_type<int>()));
}

ScopedCornerRadiusClip::ScopedCornerRadiusClip(PaintContext& context, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii, CornerClip corner_clip)
    : m_context(context)
{
    if (border_radii.has_any_radius()) {
        CornerRadii corner_radii {
            .top_left = border_radii.top_left.as_corner(context),
            .top_right = border_radii.top_right.as_corner(context),
            .bottom_right = border_radii.bottom_right.as_corner(context),
            .bottom_left = border_radii.bottom_left.as_corner(context)
        };
        auto clipper = BorderRadiusCornerClipper::create(corner_radii, context.painter().state().translation.map(border_rect.to_type<int>()).to_type<DevicePixels>(), border_radii, corner_clip);
        if (!clipper.is_error()) {
            m_corner_clipper = clipper.release_value();
            m_context.painter().sample_under_corners(*m_corner_clipper);
        }
    }
}

ScopedCornerRadiusClip::~ScopedCornerRadiusClip()
{
    if (m_corner_clipper) {
        m_context.painter().blit_corner_clipping(*m_corner_clipper);
    }
}

}
