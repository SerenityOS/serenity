/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

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
    REGISTER_BOOL_PROPERTY("word_wrap", is_word_wrap, set_word_wrap);
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

void Label::set_word_wrap(bool wrap)
{
    if (m_word_wrap == wrap)
        return;
    m_word_wrap = wrap;
    if (is_word_wrap())
        wrap_text();
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
    if (is_word_wrap())
        wrap_text();
    if (m_autosize)
        size_to_fit();
    update();
    did_change_text();
}

Gfx::IntRect Label::text_rect(size_t line) const
{
    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;
    auto rect = frame_inner_rect();
    rect.translate_by(indent, line * (font().glyph_height() + 1));
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

    if (is_word_wrap()) {
        wrap_text();
        for (size_t i = 0; i < m_lines.size(); i++) {
            auto& line = m_lines[i];
            auto text_rect = this->text_rect(i);
            if (is_enabled()) {
                painter.draw_text(text_rect, line, m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::None);
            } else {
                painter.draw_text(text_rect.translated(1, 1), line, font(), text_alignment(), Color::White, Gfx::TextElision::Right);
                painter.draw_text(text_rect, line, font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right);
            }
        }
    } else {
        auto text_rect = this->text_rect();
        if (is_enabled()) {
            painter.draw_text(text_rect, text(), m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right);
        } else {
            painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), Color::White, Gfx::TextElision::Right);
            painter.draw_text(text_rect, text(), font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right);
        }
    }
}

void Label::size_to_fit()
{
    set_fixed_width(font().width(m_text));
}

void Label::wrap_text()
{
    Vector<String> words;
    Optional<size_t> start;
    for (size_t i = 0; i < m_text.length(); i++) {
        switch (m_text[i]) {
        case '\n':
        case '\r':
        case '\t':
        case ' ': {
            if (start.has_value())
                words.append(m_text.substring(start.value(), i - start.value()));
            start.clear();
            continue;
        }
        default: {
            if (!start.has_value())
                start = i;
        }
        }
    }

    if (start.has_value())
        words.append(m_text.substring(start.value(), m_text.length() - start.value()));

    auto rect = frame_inner_rect();
    if (frame_thickness() > 0)
        rect.set_width(rect.width() - font().glyph_width('x'));

    Vector<String> lines;
    StringBuilder builder;
    int line_width = 0;
    for (auto& word : words) {
        int word_width = font().width(word);
        if (line_width > 0)
            word_width += font().glyph_width('x');
        if (line_width + word_width > rect.width()) {
            lines.append(builder.to_string());
            builder.clear();
            line_width = 0;
        }
        if (line_width > 0)
            builder.append(' ');
        builder.append(word);
        line_width += word_width;
    }

    auto last_line = builder.to_string();
    if (!last_line.is_empty())
        lines.append(last_line);

    m_lines = lines;
}

}
