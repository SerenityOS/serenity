/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/CheckBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, CheckBox)

namespace GUI {

static const int s_box_width = 13;
static const int s_box_height = 13;
static const int s_horizontal_padding = 6;

CheckBox::CheckBox(String text)
    : AbstractButton(move(text))
{
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);

    set_min_width(32);
    set_fixed_height(22);
}

CheckBox::~CheckBox()
{
}

void CheckBox::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto text_rect = rect();
    text_rect.set_left(s_box_width + s_horizontal_padding);
    text_rect.set_width(font().width(text()));
    text_rect.set_top(height() / 2 - font().glyph_height() / 2);
    text_rect.set_height(font().glyph_height());

    if (fill_with_background_color())
        painter.fill_rect(rect(), palette().window());

    if (is_enabled() && is_hovered())
        painter.fill_rect(rect(), palette().hover_highlight());

    Gfx::IntRect box_rect {
        0, height() / 2 - s_box_height / 2 - 1,
        s_box_width, s_box_height
    };

    Gfx::StylePainter::paint_check_box(painter, box_rect, palette(), is_enabled(), is_checked(), is_being_pressed());

    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);

    if (is_focused())
        painter.draw_focus_rect(text_rect.inflated(6, 6), palette().focus_outline());
}

void CheckBox::click(unsigned)
{
    if (!is_enabled())
        return;
    set_checked(!is_checked());
}

void CheckBox::set_autosize(bool autosize)
{
    if (m_autosize == autosize)
        return;
    m_autosize = autosize;
    if (m_autosize)
        size_to_fit();
}

void CheckBox::size_to_fit()
{
    set_fixed_width(s_box_width + font().width(text()) + s_horizontal_padding * 2);
}

}
