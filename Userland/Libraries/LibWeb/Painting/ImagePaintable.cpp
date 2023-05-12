/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/ImageRequest.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/ImagePaintable.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Painting {

JS::NonnullGCPtr<ImagePaintable> ImagePaintable::create(Layout::ImageBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<ImagePaintable>(layout_box);
}

ImagePaintable::ImagePaintable(Layout::ImageBox const& layout_box)
    : PaintableBox(layout_box)
{
    browsing_context().register_viewport_client(*this);
}

void ImagePaintable::finalize()
{
    Base::finalize();

    // NOTE: We unregister from the browsing context in finalize() to avoid trouble
    //       in the scenario where our BrowsingContext has already been swept by GC.
    browsing_context().unregister_viewport_client(*this);
}

Layout::ImageBox const& ImagePaintable::layout_box() const
{
    return static_cast<Layout::ImageBox const&>(layout_node());
}

void ImagePaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level.
    if (is_out_of_view(context))
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        if (layout_box().renders_as_alt_text()) {
            auto& image_element = verify_cast<HTML::HTMLImageElement>(*dom_node());
            auto enclosing_rect = context.enclosing_device_rect(absolute_rect()).to_type<int>();
            context.painter().set_font(Platform::FontPlugin::the().default_font());
            Gfx::StylePainter::paint_frame(context.painter(), enclosing_rect, context.palette(), Gfx::FrameStyle::SunkenContainer);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_rect, alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = layout_box().image_provider().current_image_bitmap()) {
            auto image_rect = context.rounded_device_rect(absolute_rect());
            ScopedCornerRadiusClip corner_clip { context, context.painter(), image_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };
            context.painter().draw_scaled_bitmap(image_rect.to_type<int>(), *bitmap, bitmap->rect(), 1.0f, to_gfx_scaling_mode(computed_values().image_rendering()));
        }
    }
}

void ImagePaintable::browsing_context_did_set_viewport_rect(CSSPixelRect const& viewport_rect)
{
    const_cast<Layout::ImageProvider&>(layout_box().image_provider()).set_visible_in_viewport(viewport_rect.intersects(absolute_rect()));
}

}
