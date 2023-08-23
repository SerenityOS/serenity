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
    const_cast<DOM::Document&>(layout_box.document()).register_viewport_client(*this);
}

void ImagePaintable::finalize()
{
    Base::finalize();

    // NOTE: We unregister from the document in finalize() to avoid trouble
    //       in the scenario where our Document has already been swept by GC.
    document().unregister_viewport_client(*this);
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
        auto image_rect = context.rounded_device_rect(absolute_rect());
        if (layout_box().renders_as_alt_text()) {
            auto& image_element = verify_cast<HTML::HTMLImageElement>(*dom_node());
            auto enclosing_rect = context.enclosing_device_rect(absolute_rect()).to_type<int>();
            context.painter().set_font(Platform::FontPlugin::the().default_font());
            Gfx::StylePainter::paint_frame(context.painter(), enclosing_rect, context.palette(), Gfx::FrameStyle::SunkenContainer);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_rect, alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = layout_box().image_provider().current_image_bitmap(image_rect.size().to_type<int>())) {
            ScopedCornerRadiusClip corner_clip { context, context.painter(), image_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };
            auto image_int_rect = image_rect.to_type<int>();
            auto bitmap_rect = bitmap->rect();
            auto scaling_mode = to_gfx_scaling_mode(computed_values().image_rendering(), bitmap_rect, image_int_rect);
            auto& dom_element = verify_cast<DOM::Element>(*dom_node());
            auto object_fit = dom_element.computed_css_values()->object_fit();
            auto bitmap_aspect_ratio = bitmap_rect.height() / bitmap_rect.width();
            auto image_aspect_ratio = (float)image_rect.height().value() / image_rect.width().value();

            auto scale_x = 0.0f;
            auto scale_y = 0.0f;
            Gfx::IntRect bitmap_intersect = bitmap_rect;

            auto object_fit_value = CSS::InitialValues::object_fit();

            if (object_fit.has_value())
                object_fit_value = object_fit.value();

            switch (object_fit_value) {
            case CSS::ObjectFit::Fill:
                scale_x = (float)image_int_rect.width() / bitmap_rect.width();
                scale_y = (float)image_int_rect.height() / bitmap_rect.height();
                bitmap_intersect = bitmap_rect;
                break;
            case CSS::ObjectFit::Contain:
                if (bitmap_aspect_ratio >= image_aspect_ratio) {
                    scale_x = (float)image_int_rect.height() / bitmap_rect.height();
                    scale_y = scale_x;
                } else {
                    scale_x = (float)image_int_rect.width() / bitmap_rect.width();
                    scale_y = scale_x;
                }
                break;
            case CSS::ObjectFit::Cover:
                if (bitmap_aspect_ratio >= image_aspect_ratio) {
                    scale_x = (float)image_int_rect.width() / bitmap_rect.width();
                    scale_y = scale_x;
                    bitmap_intersect.set_height(bitmap_rect.width() * image_aspect_ratio);
                } else {
                    scale_x = (float)image_int_rect.height() / bitmap_rect.height();
                    scale_y = scale_x;
                    bitmap_intersect.set_width(bitmap_rect.height() / image_aspect_ratio);
                }
                break;
            case CSS::ObjectFit::ScaleDown:
                // FIXME: Implement
            case CSS::ObjectFit::None:
                scale_x = 1;
                scale_y = 1;
                bitmap_intersect.set_size(image_int_rect.size());
            }

            bitmap_intersect.set_x((bitmap_rect.width() - bitmap_intersect.width()) / 2);
            bitmap_intersect.set_y((bitmap_rect.height() - bitmap_intersect.height()) / 2);

            auto offset_x = (image_int_rect.width() - bitmap_rect.width() * scale_x) / 2;
            auto offset_y = (image_int_rect.height() - bitmap_rect.height() * scale_y) / 2;

            Gfx::IntRect draw_rect = {
                image_int_rect.x() + offset_x,
                image_int_rect.y() + offset_y,
                bitmap_rect.width() * scale_x,
                bitmap_rect.height() * scale_y
            };

            context.painter().draw_scaled_bitmap(draw_rect.intersected(image_int_rect), *bitmap, bitmap_rect.intersected(bitmap_intersect), 1.f, scaling_mode);
        }
    }
}

void ImagePaintable::did_set_viewport_rect(CSSPixelRect const& viewport_rect)
{
    const_cast<Layout::ImageProvider&>(layout_box().image_provider()).set_visible_in_viewport(viewport_rect.intersects(absolute_rect()));
}

}
