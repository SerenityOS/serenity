/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, RadioButton)

namespace GUI {

RadioButton::RadioButton(String text)
    : AbstractButton(move(text))
{
    set_exclusive(true);
    set_checkable(true);
    set_min_size(SpecialDimension::Shrink, SpecialDimension::Shrink);
    set_preferred_size(SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink);
}

int RadioButton::horizontal_padding()
{
    return 2;
}

Gfx::IntSize RadioButton::circle_size()
{
    return { 12, 12 };
}

void RadioButton::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (fill_with_background_color())
        painter.fill_rect(rect(), palette().window());

    if (is_enabled() && is_hovered())
        painter.fill_rect(rect(), palette().hover_highlight());

    Gfx::IntRect circle_rect { { horizontal_padding(), 0 }, circle_size() };
    circle_rect.center_vertically_within(rect());

    Gfx::StylePainter::paint_radio_button(painter, circle_rect, palette(), is_checked(), is_being_pressed());

    Gfx::IntRect text_rect { circle_rect.right() + 4 + horizontal_padding(), 0, font().width_rounded_up(text()), font().pixel_size_rounded_up() };
    text_rect.center_vertically_within(rect());
    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);

    if (is_focused())
        painter.draw_focus_rect(text_rect.inflated(6, 6), palette().focus_outline());
}

void RadioButton::click(unsigned)
{
    if (!is_enabled())
        return;
    set_checked(true);
}

Optional<UISize> RadioButton::calculated_min_size() const
{
    auto const& font = this->font();
    int width = horizontal_padding() * 2 + circle_size().width() + font.width_rounded_up(text());
    int height = max(22, max(font.pixel_size_rounded_up() + 8, circle_size().height()));
    return UISize(width, height);
}

}
