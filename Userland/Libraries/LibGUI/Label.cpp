/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextWrapping.h>

REGISTER_WIDGET(GUI, Label)

namespace GUI {

Label::Label(String text)
    : m_text(move(text))
{
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text_alignment", text_alignment, set_text_alignment);
    REGISTER_TEXT_WRAPPING_PROPERTY("text_wrapping", text_wrapping, set_text_wrapping);

    set_preferred_size({ SpecialDimension::OpportunisticGrow });
    set_min_size({ SpecialDimension::Shrink });
    set_frame_style(Gfx::FrameStyle::NoFrame);
    set_foreground_role(Gfx::ColorRole::WindowText);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);
}

void Label::set_autosize(bool autosize, size_t padding)
{
    if (m_autosize == autosize && m_autosize_padding == padding)
        return;
    m_autosize = autosize;
    m_autosize_padding = padding;
    if (m_autosize)
        size_to_fit();
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
    return frame_inner_rect().shrunken(frame_thickness() > 0 ? font().glyph_width('x') : 0, 0);
}

void Label::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (text().is_empty())
        return;

    auto text_rect = this->text_rect();
    if (is_enabled()) {
        painter.draw_text(text_rect, text(), text_alignment(), palette().color(foreground_role()), Gfx::TextElision::Right, text_wrapping());
    } else {
        painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), palette().disabled_text_back(), Gfx::TextElision::Right, text_wrapping());
        painter.draw_text(text_rect, text(), font(), text_alignment(), palette().disabled_text_front(), Gfx::TextElision::Right, text_wrapping());
    }
}

void Label::did_change_font()
{
    if (m_autosize)
        size_to_fit();
}

void Label::size_to_fit()
{
    set_fixed_width(text_calculated_preferred_width());
    set_fixed_height(text_calculated_preferred_height());
}

int Label::text_calculated_preferred_width() const
{
    return font().width_rounded_up(m_text) + m_autosize_padding * 2;
}

int Label::text_calculated_preferred_height() const
{
    return static_cast<int>(ceilf(font().preferred_line_height()) * m_text.bytes_as_string_view().count_lines());
}

Optional<UISize> Label::calculated_preferred_size() const
{
    return GUI::UISize(text_calculated_preferred_width(), text_calculated_preferred_height());
}

Optional<UISize> Label::calculated_min_size() const
{
    int frame = frame_thickness() * 2;
    int width = font().width_rounded_up("..."sv) + frame;
    int height = font().pixel_size_rounded_up() + frame;
    height = max(height, 22);

    return UISize(width, height);
}

}
