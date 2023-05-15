/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/GroupBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, GroupBox)

namespace GUI {

GroupBox::GroupBox(StringView title)
    : m_title(title)
{
    REGISTER_DEPRECATED_STRING_PROPERTY("title", title, set_title);
}

Margins GroupBox::content_margins() const
{
    return {
        (!m_title.is_empty() ? font().pixel_size_rounded_up() + 1 /*room for the focus rect*/ : 2),
        2,
        2,
        2
    };
}

void GroupBox::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::IntRect frame_rect {
        0, (!m_title.is_empty() ? font().pixel_size_rounded_up() / 2 : 0),
        width(), height() - (!m_title.is_empty() ? font().pixel_size_rounded_up() / 2 : 0)
    };
    Gfx::StylePainter::paint_frame(painter, frame_rect, palette(), Gfx::FrameStyle::SunkenBox);

    if (!m_title.is_empty()) {
        // Fill with button background behind the text (covering the frame).
        Gfx::IntRect text_background_rect { 6, 1, font().width_rounded_up(m_title) + 6, font().pixel_size_rounded_up() };
        painter.fill_rect(text_background_rect, palette().button());

        // Center text within button background rect to ensure symmetric padding on both sides.
        // Note that we don't use TextAlignment::Center here to avoid subpixel jitter.
        Gfx::IntRect text_rect { 0, 0, text_background_rect.width() - 6, text_background_rect.height() };
        text_rect.center_within(text_background_rect);
        painter.draw_text(text_rect, m_title, Gfx::TextAlignment::CenterLeft, palette().button_text());
    }
}

void GroupBox::fonts_change_event(FontsChangeEvent& event)
{
    Widget::fonts_change_event(event);
    layout_relevant_change_occurred();
}

void GroupBox::set_title(StringView title)
{
    if (m_title == title)
        return;
    m_title = title;
    update();
}

}
