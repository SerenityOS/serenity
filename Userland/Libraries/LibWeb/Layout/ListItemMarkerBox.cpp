/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobi@tobyase.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>

namespace Web::Layout {

constexpr auto lower_alpha = "abcdefghijklmnopqrstuvwxyz";
constexpr auto upper_alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

ListItemMarkerBox::ListItemMarkerBox(DOM::Document& document, CSS::ListStyleType style_type, size_t index)
    : Box(document, nullptr, CSS::StyleProperties::create())
    , m_list_style_type(style_type)
    , m_index(index)
{
}

ListItemMarkerBox::~ListItemMarkerBox()
{
}

void ListItemMarkerBox::paint(PaintContext& context, PaintPhase phase)
{
    if (phase != PaintPhase::Foreground)
        return;

    // FIXME: It would be nicer to not have to go via the parent here to get our inherited style.
    auto color = parent()->computed_values().color();

    auto enclosing = enclosing_int_rect(absolute_rect());
    int marker_width = (int)enclosing.height() / 2;
    Gfx::IntRect marker_rect { 0, 0, marker_width, marker_width };
    marker_rect.center_within(enclosing);

    switch (m_list_style_type) {
    case CSS::ListStyleType::Square:
        context.painter().fill_rect(marker_rect, color);
        break;
    case CSS::ListStyleType::Circle:
        // For some reason for draw_ellipse() the ellipse is outside of the rect while for fill_ellipse() the ellipse is inside.
        // Scale the marker_rect with sqrt(2) to get an ellipse arc (circle) that appears as if it was inside of the marker_rect.
        marker_rect.set_height(marker_rect.height() / 1.41);
        marker_rect.set_width(marker_rect.width() / 1.41);
        marker_rect.center_within(enclosing);
        context.painter().draw_ellipse_intersecting(marker_rect, color);
        break;
    case CSS::ListStyleType::Decimal:
        context.painter().draw_text(enclosing, String::formatted("{}.", m_index), Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::Disc:
        context.painter().fill_ellipse(marker_rect, color);
        break;
    case CSS::ListStyleType::DecimalLeadingZero:
        // This is weird, but in accordance to spec.
        context.painter().draw_text(
            enclosing,
            m_index < 10 ? String::formatted("0{}.", m_index) : String::formatted("{}.", m_index),
            Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::LowerAlpha:
    case CSS::ListStyleType::LowerLatin:
        context.painter().draw_text(enclosing, String::bijective_base_from(m_index).to_lowercase(), Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::UpperAlpha:
    case CSS::ListStyleType::UpperLatin:
        context.painter().draw_text(enclosing, String::bijective_base_from(m_index), Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::None:
        return;

    default:
        VERIFY_NOT_REACHED();
    }
}

}
