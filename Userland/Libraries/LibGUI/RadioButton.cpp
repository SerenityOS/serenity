/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, RadioButton)

namespace GUI {

RadioButton::RadioButton(String text)
    : AbstractButton(move(text))
{
    set_exclusive(true);
    set_checkable(true);
    set_min_width(32);
    set_fixed_height(22);
}

RadioButton::~RadioButton()
{
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

    Gfx::IntRect circle_rect { { 2, 0 }, circle_size() };
    circle_rect.center_vertically_within(rect());

    Gfx::StylePainter::paint_radio_button(painter, circle_rect, palette(), is_checked(), is_being_pressed());

    Gfx::IntRect text_rect { circle_rect.right() + 7, 0, font().width(text()), font().glyph_height() };
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

}
