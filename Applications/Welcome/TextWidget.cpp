/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "TextWidget.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

TextWidget::TextWidget(GUI::Widget* parent)
    : GUI::Frame(parent)
{
}

TextWidget::TextWidget(const StringView& text, GUI::Widget* parent)
    : GUI::Frame(parent)
    , m_text(text)
{
}

TextWidget::~TextWidget()
{
}

void TextWidget::set_text(const StringView& text)
{
    if (text == m_text)
        return;
    m_text = text;
    wrap_and_set_height();
    update();
}

void TextWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;

    for (int i = 0; i < m_lines.size(); i++) {
        auto& line = m_lines[i];

        auto text_rect = frame_inner_rect();
        text_rect.move_by(indent, i * m_line_height);
        if (!line.is_empty())
            text_rect.set_width(text_rect.width() - indent * 2);

        if (is_enabled()) {
            painter.draw_text(text_rect, line, m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::None);
        } else {
            painter.draw_text(text_rect.translated(1, 1), line, font(), text_alignment(), Color::White, Gfx::TextElision::Right);
            painter.draw_text(text_rect, line, font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right);
        }
    }
}

void TextWidget::resize_event(GUI::ResizeEvent& event)
{
    wrap_and_set_height();
    GUI::Widget::resize_event(event);
}

void TextWidget::wrap_and_set_height()
{
    Vector<String> words;
    Optional<size_t> start;
    for (size_t i = 0; i < m_text.length(); i++) {
        auto ch = m_text[i];

        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
            if (start.has_value())
                words.append(m_text.substring(start.value(), i - start.value()));
            start.clear();
        } else if (!start.has_value()) {
            start = i;
        }
    }
    if (start.has_value())
        words.append(m_text.substring(start.value(), m_text.length() - start.value()));

    auto rect = frame_inner_rect();
    if (frame_thickness() > 0)
        rect.set_width(rect.width() - font().glyph_width('x'));

    StringBuilder builder;
    Vector<String> lines;
    int line_width = 0;
    for (auto& word : words) {
        int word_width = font().width(word);
        if (line_width != 0)
            word_width += font().glyph_width('x');

        if (line_width + word_width > rect.width()) {
            lines.append(builder.to_string());
            builder.clear();
            line_width = 0;
        }

        if (line_width != 0)
            builder.append(' ');
        builder.append(word);
        line_width += word_width;
    }
    auto last_line = builder.to_string();
    if (!last_line.is_empty()) {
        lines.append(last_line);
    }

    m_lines = lines;

    set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    set_preferred_size(0, m_lines.size() * m_line_height + frame_thickness() * 2);
}
