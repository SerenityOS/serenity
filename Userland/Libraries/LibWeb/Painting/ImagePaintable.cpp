/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/CSS/StyleValues/EdgeStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
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

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto image_rect = context.rounded_device_rect(absolute_rect());
        if (layout_box().renders_as_alt_text()) {
            auto& image_element = verify_cast<HTML::HTMLImageElement>(*dom_node());
            auto enclosing_rect = context.enclosing_device_rect(absolute_rect()).to_type<int>();
            context.painter().set_font(Platform::FontPlugin::the().default_font());
            context.painter().paint_frame(enclosing_rect, context.palette(), Gfx::FrameStyle::SunkenContainer);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_rect, alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = layout_box().image_provider().current_image_bitmap(image_rect.size().to_type<int>())) {
            ScopedCornerRadiusClip corner_clip { context, image_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };
            auto image_int_rect = image_rect.to_type<int>();
            auto bitmap_rect = bitmap->rect();
            auto scaling_mode = to_gfx_scaling_mode(computed_values().image_rendering(), bitmap_rect, image_int_rect);
            auto& dom_element = verify_cast<DOM::Element>(*dom_node());
            auto object_fit = dom_element.computed_css_values()->object_fit();
            auto bitmap_aspect_ratio = (float)bitmap_rect.height() / bitmap_rect.width();
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

            auto scaled_bitmap_width = bitmap_rect.width() * scale_x;
            auto scaled_bitmap_height = bitmap_rect.height() * scale_y;

            auto residual_horizontal = image_int_rect.width() - scaled_bitmap_width;
            auto residual_vertical = image_int_rect.height() - scaled_bitmap_height;

            bitmap_intersect.set_x((bitmap_rect.width() - bitmap_intersect.width()) / 2);
            bitmap_intersect.set_y((bitmap_rect.height() - bitmap_intersect.height()) / 2);

            CSS::PositionStyleValue const& object_position = dom_element.computed_css_values()->object_position();

            auto offset_x = 0;
            auto const& horizontal = object_position.edge_x();
            if (horizontal->is_edge()) {
                auto const& horizontal_edge = horizontal->as_edge();
                auto const& offset = horizontal_edge.offset();
                if (horizontal_edge.edge() == CSS::PositionEdge::Left) {
                    if (offset.is_percentage())
                        offset_x = (double)(residual_horizontal)*offset.percentage().as_fraction();
                    else
                        offset_x = offset.length().to_px(layout_node()).to_int();

                    bitmap_intersect.set_x(0);
                } else if (horizontal_edge.edge() == CSS::PositionEdge::Right) {
                    if (offset.is_percentage())
                        offset_x = (double)residual_horizontal - (double)(residual_horizontal)*offset.percentage().as_fraction();
                    else
                        offset_x = residual_horizontal - offset.length().to_px(layout_node()).to_int();
                }
                if (image_int_rect.width() < scaled_bitmap_width)
                    bitmap_intersect.set_x(-(offset_x / scale_x));
            }

            auto offset_y = 0;
            auto const& vertical = object_position.edge_y();
            if (vertical->is_edge()) {
                auto const& vertical_edge = vertical->as_edge();
                auto const& offset = vertical_edge.offset();
                if (vertical_edge.edge() == CSS::PositionEdge::Top) {
                    if (offset.is_percentage())
                        offset_y = (double)(residual_vertical)*offset.percentage().as_fraction();
                    else
                        offset_y = offset.length().to_px(layout_node()).to_int();

                    bitmap_intersect.set_y(0);
                } else if (vertical_edge.edge() == CSS::PositionEdge::Bottom) {
                    if (offset.is_percentage())
                        offset_y = (double)residual_vertical - (double)(residual_vertical)*offset.percentage().as_fraction();
                    else
                        offset_y = residual_vertical - offset.length().to_px(layout_node()).to_int();
                }
                if (image_int_rect.height() < scaled_bitmap_height)
                    bitmap_intersect.set_y(-(offset_y / scale_y));
            }

            Gfx::IntRect draw_rect = {
                image_int_rect.x() + offset_x,
                image_int_rect.y() + offset_y,
                (int)scaled_bitmap_width,
                (int)scaled_bitmap_height
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
