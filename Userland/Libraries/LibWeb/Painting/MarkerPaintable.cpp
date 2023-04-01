/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>
#include <LibWeb/Painting/MarkerPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<MarkerPaintable> MarkerPaintable::create(Layout::ListItemMarkerBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<MarkerPaintable>(layout_box);
}

MarkerPaintable::MarkerPaintable(Layout::ListItemMarkerBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::ListItemMarkerBox const& MarkerPaintable::layout_box() const
{
    return static_cast<Layout::ListItemMarkerBox const&>(layout_node());
}

void MarkerPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (phase != PaintPhase::Foreground)
        return;

    // FIXME: All this does is round to the nearest whole CSS pixel, but it's goofy.
    CSSPixelRect enclosing = absolute_rect().to_type<float>().to_rounded<float>().to_type<CSSPixels>();
    auto device_enclosing = context.enclosing_device_rect(enclosing);

    CSSPixels marker_width = enclosing.height() / 2.0f;

    if (auto const* list_style_image = layout_box().list_style_image()) {
        CSSPixelRect image_rect {
            0, 0,
            list_style_image->natural_width().value_or(marker_width.value()),
            list_style_image->natural_height().value_or(marker_width.value())
        };
        image_rect.center_within(enclosing);

        auto device_image_rect = context.enclosing_device_rect(image_rect);
        list_style_image->resolve_for_size(layout_box(), image_rect.size());
        list_style_image->paint(context, device_image_rect, computed_values().image_rendering());
        return;
    }

    CSSPixelRect marker_rect { 0, 0, marker_width, marker_width };
    marker_rect.center_within(enclosing);
    auto device_marker_rect = context.enclosing_device_rect(marker_rect);

    auto color = computed_values().color();

    Gfx::AntiAliasingPainter aa_painter { context.painter() };

    switch (layout_box().list_style_type()) {
    case CSS::ListStyleType::Square:
        context.painter().fill_rect(device_marker_rect.to_type<int>(), color);
        break;
    case CSS::ListStyleType::Circle:
        aa_painter.draw_ellipse(device_marker_rect.to_type<int>(), color, 1);
        break;
    case CSS::ListStyleType::Disc:
        aa_painter.fill_ellipse(device_marker_rect.to_type<int>(), color);
        break;
    case CSS::ListStyleType::Decimal:
    case CSS::ListStyleType::DecimalLeadingZero:
    case CSS::ListStyleType::LowerAlpha:
    case CSS::ListStyleType::LowerLatin:
    case CSS::ListStyleType::LowerRoman:
    case CSS::ListStyleType::UpperAlpha:
    case CSS::ListStyleType::UpperLatin:
    case CSS::ListStyleType::UpperRoman:
        if (layout_box().text().is_null())
            break;
        // FIXME: This should use proper text layout logic!
        // This does not line up with the text in the <li> element which looks very sad :(
        context.painter().draw_text(device_enclosing.to_type<int>(), layout_box().text(), layout_box().scaled_font(context), Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::None:
        return;

    default:
        VERIFY_NOT_REACHED();
    }
}

}
