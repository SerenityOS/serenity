/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/ImagePaintable.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Painting {

NonnullRefPtr<ImagePaintable> ImagePaintable::create(Layout::ImageBox const& layout_box)
{
    return adopt_ref(*new ImagePaintable(layout_box));
}

ImagePaintable::ImagePaintable(Layout::ImageBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::ImageBox const& ImagePaintable::layout_box() const
{
    return static_cast<Layout::ImageBox const&>(layout_node());
}

void ImagePaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level. Also rect() does not include padding etc!
    if (!context.viewport_rect().intersects(enclosing_int_rect(absolute_rect())))
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        if (layout_box().renders_as_alt_text()) {
            auto& image_element = verify_cast<HTML::HTMLImageElement>(*dom_node());
            context.painter().set_font(Platform::FontPlugin::the().default_font());
            Gfx::StylePainter::paint_frame(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_int_rect(absolute_rect()), alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = layout_box().image_loader().bitmap(layout_box().image_loader().current_frame_index())) {
            auto image_rect = absolute_rect().to_rounded<int>();
            ScopedCornerRadiusClip corner_clip { context.painter(), image_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };
            context.painter().draw_scaled_bitmap(image_rect, *bitmap, bitmap->rect(), 1.0f, to_gfx_scaling_mode(computed_values().image_rendering()));
        }
    }
}

}
