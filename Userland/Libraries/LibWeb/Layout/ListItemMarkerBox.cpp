/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>

namespace Web::Layout {

ListItemMarkerBox::ListItemMarkerBox(DOM::Document& document, CSS::ListStyleType style_type, size_t index, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, nullptr, move(style))
    , m_list_style_type(style_type)
    , m_index(index)
{
    switch (m_list_style_type) {
    case CSS::ListStyleType::Square:
    case CSS::ListStyleType::Circle:
    case CSS::ListStyleType::Disc:
        break;
    case CSS::ListStyleType::Decimal:
        m_text = String::formatted("{}.", m_index);
        break;
    case CSS::ListStyleType::DecimalLeadingZero:
        // This is weird, but in accordance to spec.
        m_text = m_index < 10 ? String::formatted("0{}.", m_index) : String::formatted("{}.", m_index);
        break;
    case CSS::ListStyleType::LowerAlpha:
    case CSS::ListStyleType::LowerLatin:
        m_text = String::bijective_base_from(m_index - 1).to_lowercase();
        break;
    case CSS::ListStyleType::UpperAlpha:
    case CSS::ListStyleType::UpperLatin:
        m_text = String::bijective_base_from(m_index - 1);
        break;
    case CSS::ListStyleType::LowerRoman:
        m_text = String::roman_number_from(m_index).to_lowercase();
        break;
    case CSS::ListStyleType::UpperRoman:
        m_text = String::roman_number_from(m_index);
        break;
    case CSS::ListStyleType::None:
        break;

    default:
        VERIFY_NOT_REACHED();
    }
}

ListItemMarkerBox::~ListItemMarkerBox()
{
}

void ListItemMarkerBox::paint(PaintContext& context, PaintPhase phase)
{
    if (phase != PaintPhase::Foreground)
        return;

    auto enclosing = enclosing_int_rect(absolute_rect());

    if (auto const* list_style_image = list_style_image_bitmap()) {
        context.painter().blit(enclosing.location(), *list_style_image, list_style_image->rect());
        return;
    }

    auto color = computed_values().color();

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
    case CSS::ListStyleType::Disc:
        context.painter().fill_ellipse(marker_rect, color);
        break;
    case CSS::ListStyleType::Decimal:
    case CSS::ListStyleType::DecimalLeadingZero:
    case CSS::ListStyleType::LowerAlpha:
    case CSS::ListStyleType::LowerLatin:
    case CSS::ListStyleType::LowerRoman:
    case CSS::ListStyleType::UpperAlpha:
    case CSS::ListStyleType::UpperLatin:
    case CSS::ListStyleType::UpperRoman:
        if (m_text.is_null())
            break;
        context.painter().draw_text(enclosing, m_text, Gfx::TextAlignment::Center);
        break;
    case CSS::ListStyleType::None:
        return;

    default:
        VERIFY_NOT_REACHED();
    }
}

Gfx::Bitmap const* ListItemMarkerBox::list_style_image_bitmap() const
{
    return list_style_image() ? list_style_image()->bitmap() : nullptr;
}

}
