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

NonnullRefPtr<MarkerPaintable> MarkerPaintable::create(Layout::ListItemMarkerBox const& layout_box)
{
    return adopt_ref(*new MarkerPaintable(layout_box));
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

    auto enclosing = enclosing_int_rect(absolute_rect());

    int marker_width = (int)enclosing.height() / 2;

    if (auto const* list_style_image = layout_box().list_style_image()) {
        Gfx::IntRect image_rect {
            0, 0,
            list_style_image->natural_width().value_or(marker_width),
            list_style_image->natural_height().value_or(marker_width)
        };
        image_rect.center_within(enclosing);
        list_style_image->resolve_for_size(layout_box(), image_rect.size().to_type<float>());
        list_style_image->paint(context, image_rect, computed_values().image_rendering());
        return;
    }

    Gfx::IntRect marker_rect { 0, 0, marker_width, marker_width };
    marker_rect.center_within(enclosing);

    auto color = computed_values().color();

    Gfx::AntiAliasingPainter aa_painter { context.painter() };

    switch (layout_box().list_style_type()) {
    case CSS::ListStyleType::Square:
        context.painter().fill_rect(marker_rect, color);
        break;
    case CSS::ListStyleType::Circle:
        aa_painter.draw_ellipse(marker_rect, color, 1);
        break;
    case CSS::ListStyleType::Disc:
        aa_painter.fill_ellipse(marker_rect, color);
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
        context.painter().draw_text(enclosing, layout_box().text(), Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::None:
        return;

    default:
        VERIFY_NOT_REACHED();
    }
}

}
