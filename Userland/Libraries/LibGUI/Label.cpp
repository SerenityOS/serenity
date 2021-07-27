/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextLayout.h>
#include <LibGfx/TextWrapping.h>

REGISTER_WIDGET(GUI, Label)

namespace GUI {

Label::Label(String text)
    : m_text(move(text))
{
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text_alignment", text_alignment, set_text_alignment);

    set_frame_thickness(0);
    set_frame_shadow(Gfx::FrameShadow::Plain);
    set_frame_shape(Gfx::FrameShape::NoFrame);

    set_foreground_role(Gfx::ColorRole::WindowText);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);
}

Label::~Label()
{
}

void Label::set_autosize(bool autosize)
{
    if (m_autosize == autosize)
        return;
    m_autosize = autosize;
    if (m_autosize)
        size_to_fit();
}

void Label::set_icon(const Gfx::Bitmap* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    update();
}

void Label::set_text(String text)
{
    if (text == m_text)
        return;
    m_text = move(text);

    if (m_autosize)
        size_to_fit();
    update();
    did_change_text();
}

Gfx::IntRect Label::text_rect() const
{
    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;
    auto rect = frame_inner_rect();
    rect.set_width(rect.width() - indent * 2);
    return rect;
}

void Label::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_icon) {
        if (m_should_stretch_icon) {
            painter.draw_scaled_bitmap(frame_inner_rect(), *m_icon, m_icon->rect());
        } else {
            auto icon_location = frame_inner_rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
            painter.blit(icon_location, *m_icon, m_icon->rect());
        }
    }

    if (text().is_empty())
        return;

    auto text_rect = this->text_rect();
    if (is_enabled()) {
        painter.draw_text(text_rect, text(), m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
    } else {
        painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), Color::White, Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
        painter.draw_text(text_rect, text(), font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
    }
}

void Label::size_to_fit()
{
    set_fixed_width(font().width(m_text));
}

int Label::preferred_height() const
{
    // FIXME: The 4 is taken from Gfx::Painter and should be available as
    //        a constant instead.
    return Gfx::TextLayout(&font(), Utf8View { m_text }, text_rect()).bounding_rect(Gfx::TextWrapping::Wrap, 4).height();
}
}
